package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.TryoutCandidateModel
import pe.edu.esan.sportpro.data.model.TryoutModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Repositorio para Avisos de Pruebas y Convocatorias Abiertas (US-10).
 */
object TryoutRepository {

    private const val TRYOUTS = "tryouts"
    private const val CANDIDATES = "candidates"
    private const val TEAMS = "teams"
    private const val TRYOUT_REPORTS = "tryoutReports"

    private val firestore = FirebaseFirestore.getInstance()
    private val tryoutsCollection get() = firestore.collection(TRYOUTS)

    /**
     * Obtiene en solo lectura los equipos APROBADOS pertenecientes al DT.
     */
    suspend fun getApprovedTeamsByDt(dtUid: String): Result<List<TeamModel>> {
        return try {
            val snapshot = firestore.collection(TEAMS)
                .whereEqualTo("dtUid", dtUid)
                .whereEqualTo("status", TeamStatus.APPROVED)
                .get()
                .await()
            val teams = snapshot.toObjects(TeamModel::class.java)
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa convocatorias activas (cuyas fechas no hayan vencido).
     * Ordenadas por la fecha de prueba más próxima.
     */
    fun observeActiveTryouts(): Flow<List<TryoutModel>> = callbackFlow {
        val registration = tryoutsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TryoutModel::class.java).orEmpty()
            val activeList = list.filter { tryout ->
                !isDateExpired(tryout.registrationDeadline) && !isDateExpired(tryout.tryoutDate)
            }.sortedBy { it.tryoutDate }
            trySend(activeList)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observa convocatorias creadas por un DT específico.
     */
    fun observeTryoutsByDt(dtUid: String): Flow<List<TryoutModel>> = callbackFlow {
        val registration = tryoutsCollection.whereEqualTo("dtUid", dtUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(TryoutModel::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Crea un nuevo aviso de convocatoria/prueba (US-10).
     */
    suspend fun createTryout(tryout: TryoutModel): Result<String> {
        return try {
            if (tryout.title.isBlank()) return Result.failure(Exception("Debe ingresar un título para la convocatoria."))
            if (tryout.description.isBlank()) return Result.failure(Exception("Debe ingresar una descripción detallada."))
            if (tryout.teamId.isBlank()) return Result.failure(Exception("Debe seleccionar un equipo verificado."))
            if (tryout.category.isBlank()) return Result.failure(Exception("Debe especificar la categoría."))
            if (tryout.positionsSought.isBlank()) return Result.failure(Exception("Debe indicar las posiciones buscadas."))
            if (tryout.tryoutDate.isBlank() || tryout.tryoutTime.isBlank()) return Result.failure(Exception("Debe ingresar fecha y hora de la prueba."))
            if (tryout.address.isBlank() || tryout.district.isBlank()) return Result.failure(Exception("Debe ingresar la dirección y distrito."))
            if (tryout.slots <= 0) return Result.failure(Exception("Debe especificar una cantidad de cupos válida mayor a 0."))
            if (tryout.registrationDeadline.isBlank()) return Result.failure(Exception("Debe indicar la fecha límite de inscripción."))

            // Validar que la fecha de la prueba no sea anterior a hoy
            if (isDateBeforeToday(tryout.tryoutDate)) {
                return Result.failure(Exception("La fecha de la prueba no puede ser anterior a la fecha actual."))
            }

            // Validación política de convocatoria gratuita
            val textCheck = "${tryout.title} ${tryout.description} ${tryout.requirements}".lowercase()
            if (textCheck.contains("pago") || textCheck.contains("costo") || textCheck.contains("cobro") || textCheck.contains("precio")) {
                return Result.failure(Exception("Las convocatorias son gratuitas. Queda prohibida cualquier solicitud de pago."))
            }

            val docRef = tryoutsCollection.document()
            val tryoutToSave = tryout.copy(id = docRef.id, isFree = true, verifiedTeam = true)
            docRef.set(tryoutToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza una convocatoria existente.
     */
    suspend fun updateTryout(tryout: TryoutModel): Result<Unit> {
        return try {
            if (tryout.id.isBlank()) return Result.failure(Exception("ID de aviso no válido."))
            if (isDateBeforeToday(tryout.tryoutDate)) {
                return Result.failure(Exception("La fecha de la prueba no puede ser anterior a la fecha actual."))
            }

            tryoutsCollection.document(tryout.id).set(tryout).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un aviso de prueba.
     */
    suspend fun deleteTryout(tryoutId: String): Result<Unit> {
        return try {
            tryoutsCollection.document(tryoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra el interés de un JUG o PAD ("Me interesa").
     * Valida cupos disponibles y evita inscripciones duplicadas para la misma convocatoria.
     */
    suspend fun registerInterest(candidate: TryoutCandidateModel): Result<Unit> {
        return try {
            if (candidate.tryoutId.isBlank()) return Result.failure(Exception("Convocatoria no encontrada."))
            if (candidate.name.isBlank()) return Result.failure(Exception("Debe ingresar su nombre."))
            if (candidate.age <= 0) return Result.failure(Exception("Debe ingresar una edad válida."))
            if (candidate.position.isBlank()) return Result.failure(Exception("Debe indicar la posición a postular."))

            val tryoutDoc = tryoutsCollection.document(candidate.tryoutId).get().await()
            if (!tryoutDoc.exists()) return Result.failure(Exception("La convocatoria ya no está disponible."))

            val slots = tryoutDoc.getLong("slots")?.toInt() ?: 0
            val interestedCount = tryoutDoc.getLong("interestedCount")?.toInt() ?: 0

            // Validar si los cupos están agotados
            if (slots > 0 && interestedCount >= slots) {
                return Result.failure(Exception("Los cupos para esta convocatoria ya se encuentran agotados."))
            }

            val candidatesRef = tryoutsCollection.document(candidate.tryoutId).collection(CANDIDATES)

            // Validar que la persona no se haya inscrito previamente en la misma convocatoria
            if (candidate.userUid.isNotBlank()) {
                val existingByUser = candidatesRef.whereEqualTo("userUid", candidate.userUid).get().await()
                if (!existingByUser.isEmpty) {
                    return Result.failure(Exception("Ya te has inscrito previamente en esta convocatoria."))
                }
            }

            val existingByName = candidatesRef.whereEqualTo("name", candidate.name.trim()).get().await()
            if (!existingByName.isEmpty) {
                return Result.failure(Exception("Ya existe una inscripción registrada a nombre de ${candidate.name.trim()}."))
            }

            val docRef = candidatesRef.document()
            val candidateToSave = candidate.copy(id = docRef.id)
            docRef.set(candidateToSave).await()

            // Incrementar contador de interesados
            tryoutsCollection.document(candidate.tryoutId).update("interestedCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los candidatos interesados de una prueba.
     * NUNCA incluye teléfonos de menores para respetar estrictamente las normas de privacidad.
     */
    suspend fun getCandidatesByTryout(tryoutId: String): Result<List<TryoutCandidateModel>> {
        return try {
            val snapshot = tryoutsCollection.document(tryoutId).collection(CANDIDATES).get().await()
            val candidates = snapshot.toObjects(TryoutCandidateModel::class.java).sortedBy { it.name }
            Result.success(candidates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permite reportar un aviso (Integración US-24).
     */
    suspend fun reportTryout(
        tryoutId: String,
        tryoutTitle: String,
        authorUid: String,
        authorName: String,
        reporterUid: String,
        reason: String,
        detail: String
    ): Result<Unit> {
        return try {
            val reportData = hashMapOf(
                "tryoutId" to tryoutId,
                "postId" to tryoutId,
                "postText" to "[Aviso de Convocatoria] $tryoutTitle",
                "postAuthorUid" to authorUid,
                "postAuthorName" to authorName,
                "reason" to reason,
                "detail" to detail,
                "reporterUid" to reporterUid,
                "type" to "TRYOUT",
                "status" to "PENDIENTE",
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(TRYOUT_REPORTS).document("${tryoutId}_$reporterUid").set(reportData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isDateBeforeToday(dateStr: String): Boolean {
        if (dateStr.isBlank()) return false
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (fmtStr in formats) {
            try {
                val fmt = SimpleDateFormat(fmtStr, Locale.getDefault())
                val parsed = fmt.parse(dateStr.trim())
                if (parsed != null && parsed.before(nowCal.time)) {
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun isDateExpired(deadlineStr: String): Boolean {
        if (deadlineStr.isBlank()) return false
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (fmtStr in formats) {
            try {
                val fmt = SimpleDateFormat(fmtStr, Locale.getDefault())
                val parsed = fmt.parse(deadlineStr.trim())
                if (parsed != null) {
                    val endCal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    if (nowCal.time.after(endCal.time)) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }
}
