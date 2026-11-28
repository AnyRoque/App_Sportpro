package pe.edu.esan.sportpro.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pe.edu.esan.sportpro.data.model.AdminModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel

/**
 * Repositorio para la gestión dinámica de Administradores Autorizados.
 * Maneja la colección "Admins" (registro de auditoría) y el rol ADM en "users".
 */
object AdminRepository {

    private const val ADMINS_COLLECTION = "Admins"
    private const val USERS_COLLECTION = "users"
    private val firestore = FirebaseFirestore.getInstance()

    /** Lista predefinida de academias / clubes para la asignación de administradores. */
    val AVAILABLE_CLUBS = listOf(
        "Academia Deportiva Los Pinos",
        "Club Deportivo SportPro"
    )

    /**
     * Verifica si un correo ya pertenece a un administrador activo.
     * La fuente de verdad del rol es users/{uid}.role == "ADM" (US-06).
     */
    suspend fun isAdminAuthorized(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return false
        return withTimeoutOrNull(4000L) {
            try {
                !firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", cleanEmail)
                    .whereEqualTo("role", Role.ADM.name)
                    .get()
                    .await()
                    .isEmpty
            } catch (_: Exception) {
                false
            }
        } ?: false
    }

    /**
     * Registra un nuevo usuario directamente en Firebase Authentication usando createUserWithEmailAndPassword,
     * guarda los datos en Firestore (Admins y users) y lo autoriza inmediatamente.
     */
    suspend fun registerNewAdminWithAuth(
        admin: AdminModel,
        password: String,
        creatorEmail: String,
        context: Context
    ): Result<Unit> {
        return try {
            val cleanEmail = admin.email.trim().lowercase()

            if (cleanEmail.isBlank()) throw Exception("El correo electrónico es obligatorio")
            if (admin.name.isBlank()) throw Exception("El nombre es obligatorio")
            if (admin.lastName.isBlank()) throw Exception("El apellido es obligatorio")
            if (password.length < 6) throw Exception("La contraseña debe tener al menos 6 caracteres")
            if (admin.phone.isBlank()) throw Exception("El teléfono es obligatorio")

            val actorUid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Debes iniciar sesión")
            val users = firestore.collection(USERS_COLLECTION)
            val actor = users.document(actorUid).get(Source.SERVER).await().toObject(UserModel::class.java)
            check(actor?.active == true && actor.roleEnum == Role.ADM) {
                "Solo un administrador activo puede registrar otros administradores"
            }

            // Validar si ya está autorizado como admin
            if (isAdminAuthorized(cleanEmail)) {
                throw Exception("El correo '$cleanEmail' ya está registrado como administrador")
            }

            // Instancia secundaria de FirebaseApp para NO afectar la sesión del Administrador en curso
            val secondaryApp = try {
                FirebaseApp.getInstance("SecondaryAdminAuthApp")
            } catch (_: Exception) {
                val primaryOptions = FirebaseApp.getInstance().options
                val options = if (pe.edu.esan.sportpro.BuildConfig.USE_FIREBASE_EMULATOR) {
                    FirebaseOptions.Builder(primaryOptions).setApiKey("fake-api-key-admin").build()
                } else {
                    primaryOptions
                }
                FirebaseApp.initializeApp(context, options, "SecondaryAdminAuthApp").also { app ->
                    if (pe.edu.esan.sportpro.BuildConfig.USE_FIREBASE_EMULATOR) {
                        FirebaseAuth.getInstance(app).useEmulator("10.0.2.2", 9099)
                    }
                }
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            var newUid: String?

            try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
                newUid = authResult.user?.uid
                secondaryAuth.signOut()
            } catch (_: FirebaseAuthUserCollisionException) {
                throw Exception("El correo '$cleanEmail' ya está registrado en Firebase Authentication")
            } catch (e: Exception) {
                throw Exception("Error al crear usuario en Firebase Auth: ${e.message}")
            }

            val docId = newUid ?: throw Exception("No se pudo obtener el UID del nuevo administrador")

            val ids = SportProIdGenerator(firestore)
            val userModel = UserModel(
                uid = docId,
                name = admin.name,
                lastName = admin.lastName,
                email = cleanEmail,
                phone = admin.phone,
                role = Role.ADM.name,
                acceptedTerms = true,
                active = true
            )
            ids.transact { tx ->
                val userRef = users.document(docId)
                val actorId = tx.get(users.document(actorUid)).getString("id").orEmpty()
                check(!tx.get(userRef).exists()) { "La cuenta ya tiene un perfil" }
                val (record, businessId) = ids.allocate(tx, firestore.collection(ADMINS_COLLECTION), SportProEntity.ADMIN)
                val audit = ids.prepareAudit(tx, actorUid, record.reference, "CREATE", emptyList())
                record.commit(tx, mapOf(
                    "authUid" to docId,
                    "name" to admin.name,
                    "lastName" to admin.lastName,
                    "email" to cleanEmail,
                    "phone" to admin.phone,
                    "club" to admin.club,
                    "role" to Role.ADM.name,
                    "active" to true,
                    "createdBy" to actorId
                ) + ids.creationMetadata(actorUid))
                tx.set(userRef, userModel.firestoreFields() + ids.creationMetadata(actorUid) + ("id" to businessId))
                audit()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
