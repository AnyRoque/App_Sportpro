package pe.edu.esan.sportpro.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel

/** Autenticación y perfil de usuario (US-01). */
object FirebaseAuthManager {

    private const val USERS = "users"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val ids = SportProIdGenerator(firestore)

    val currentUid: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email

    /**
     * Registra un nuevo usuario en Firebase Authentication y guarda de forma sincronizada su documento en Firestore.
     */
    suspend fun registerUser(user: UserModel, password: String): Result<UserModel> {
        return try {
            val cleanEmail = user.email.trim().lowercase()

            // Seguridad: el rol ADM nunca se crea desde el registro público
            if (Role.fromOrNull(user.role) !in Role.selfRegister)
                throw Exception("Rol no permitido para registro")

            val authUser = try {
                auth.createUserWithEmailAndPassword(cleanEmail, password).await().user
            } catch (collision: FirebaseAuthUserCollisionException) {
                val existing = try {
                    auth.signInWithEmailAndPassword(cleanEmail, password).await().user
                } catch (_: FirebaseAuthInvalidCredentialsException) {
                    throw collision
                } catch (_: FirebaseAuthInvalidUserException) {
                    throw collision
                }
                val existingUid = existing?.uid ?: throw collision
                if (firestore.collection(USERS).document(existingUid)
                        .get(com.google.firebase.firestore.Source.SERVER).await().exists()) {
                    throw collision
                }
                existing
            }
            val uid = authUser?.uid ?: error("Usuario inválido")

            // 2. Sincronizar e inscribir en Firestore colección 'users'
            val savedUser = user.copy(
                uid = uid,
                email = cleanEmail,
                role = if (user.role.isNotBlank()) user.role else Role.JUG.name,
                active = true
            )
            Result.success(saveProfile(savedUser))
        } catch (e: FirebaseAuthUserCollisionException) {
            auth.signOut()
            Result.failure(Exception("El correo ya está registrado"))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(e)
        }
    }

    /**
     * Inicia sesión con Firebase Auth y obtiene de forma segura el perfil y rol del usuario desde Firestore.
     */
    suspend fun loginUser(email: String, password: String): Result<UserModel> {
        val cleanEmail = email.trim().lowercase()

        return try {
            auth.signInWithEmailAndPassword(cleanEmail, password).await()
            getUserProfile()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lee 'users/{uid}' en Firestore para identificar el rol del usuario.
     * Si el documento no existe (registro previo incompleto), lo crea automáticamente (Auto-Healing)
     * garantizando que el inicio de sesión no se interrumpa.
     */
    suspend fun getUserProfile(): Result<UserModel> {
        return try {
            val uid = currentUid ?: throw Exception("No hay sesión iniciada")
            val email = (currentUserEmail ?: "").trim().lowercase()

            // 2. Intentar consultar documento en Firestore users/{uid}
            val doc = firestore.collection(USERS).document(uid).get().await()
            val existingUser = doc.toObject(UserModel::class.java)

            if (existingUser != null) {
                if (!existingUser.active) {
                    auth.signOut()
                    throw Exception("Tu cuenta está desactivada")
                }
                // El rol SOLO se toma de Firestore (users/{uid}.role)
                return Result.success(existingUser.copy(uid = uid))
            }

            // 3. AUTO-REPARACIÓN: El documento no existe en Firestore (ej. usuario creado en consola o registro previo interrumpido)
            // Un perfil reparado nunca recibe rol ADM
            val defaultRole = Role.JUG.name
            val defaultName = auth.currentUser?.displayName.orEmpty().ifBlank {
                if (email.isNotBlank()) email.substringBefore("@").replaceFirstChar { it.uppercase() }
                else "Usuario"
            }

            val healedUser = UserModel(
                uid = uid,
                name = defaultName,
                lastName = "Usuario",
                email = email,
                role = defaultRole,
                acceptedTerms = true,
                active = true
            )

            Result.success(saveProfile(healedUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveProfile(user: UserModel): UserModel {
        val entity = if (user.role == Role.DT.name) SportProEntity.DIRECTOR else SportProEntity.USER
        val users = firestore.collection(USERS)
        return ids.transact { tx ->
            val existing = tx.get(users.document(user.uid)).toObject(UserModel::class.java)
            if (existing != null) {
                check(existing.active) { "Tu cuenta está desactivada" }
                return@transact existing.copy(uid = user.uid)
            }
            val (record, businessId) = ids.allocate(tx, users, entity, user.uid)
            val audit = ids.prepareAudit(tx, user.uid, record.reference, "CREATE", emptyList(), businessId)
            record.commit(tx, user.firestoreFields() + ids.creationMetadata(user.uid) + ("createdBy" to businessId), businessId)
            audit()
            user.copy(id = businessId, authUid = user.uid)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}
