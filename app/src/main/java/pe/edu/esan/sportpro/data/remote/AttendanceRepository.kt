package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.PlayerAttendanceRecord
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Repositorio de Cloud Firestore para el Registro de Asistencia a Entrenamientos (US-13, US-14).
 */
object AttendanceRepository {

    private const val ATTENDANCE = "attendance"
    private const val TRAININGS = "trainings"
    private const val PLAYERS = "players"

    private val firestore = FirebaseFirestore.getInstance()
    private val ids = SportProIdGenerator(firestore)
    private val attendanceRef get() = firestore.collection(ATTENDANCE)
    private val trainingsRef get() = firestore.collection(TRAININGS)
    private val playersRef get() = firestore.collection(PLAYERS)

    /**
     * Guarda o actualiza el registro de asistencia en Firestore y marca la sesión como "REALIZADA".
     */
    suspend fun saveAttendanceRecord(record: AttendanceRecordModel): Result<Unit> {
        return try {
            val trainingId = record.trainingId.ifBlank { record.id }
            require(trainingId.isNotBlank()) { "Selecciona un entrenamiento" }
            val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
            val playerIds = record.items.map { it.playerId }
            require(playerIds.none { it.isBlank() } && playerIds.distinct().size == playerIds.size) {
                "Cada jugador debe figurar una sola vez en la asistencia"
            }
            ids.transact { tx ->
                val trainingRef = trainingsRef.document(trainingId)
                val training = tx.get(trainingRef)
                check(training.exists() && training.getString("teamId") == record.teamId) {
                    "El entrenamiento no pertenece al equipo seleccionado"
                }
                playerIds.forEach { id ->
                    val player = tx.get(playersRef.document(id))
                    check(player.exists() && player.getString("teamId") == record.teamId) {
                        "Hay un jugador que no pertenece al equipo"
                    }
                }

                val existingRef = attendanceRef.document(training.getString("attendanceId") ?: trainingId)
                val existing = tx.get(existingRef)
                val allocation = if (existing.exists()) null
                    else ids.allocate(tx, attendanceRef, SportProEntity.ATTENDANCE).first
                val target = allocation?.reference ?: existingRef
                val values = mapOf(
                    "trainingId" to trainingId,
                    "teamId" to record.teamId,
                    "teamName" to record.teamName,
                    "category" to record.category,
                    "dtUid" to record.dtUid,
                    "dtName" to record.dtName,
                    "date" to record.date,
                    "items" to record.items.map { item ->
                        mapOf(
                            "playerId" to item.playerId,
                            "playerName" to item.playerName,
                            "photoUrl" to item.photoUrl,
                            "shirtNumber" to item.shirtNumber,
                            "position" to item.position,
                            "status" to item.status,
                            "justificationReason" to item.justificationReason
                        )
                    },
                    "totalAttended" to record.totalAttended,
                    "totalLate" to record.totalLate,
                    "totalAbsent" to record.totalAbsent,
                    "totalJustified" to record.totalJustified,
                    "editedBy" to record.editedBy
                )
                val audit = ids.prepareAudit(tx, uid, target,
                    if (allocation == null) "UPDATE" else "CREATE",
                    if (allocation == null) ids.changedFields(existing, values).keys.sorted() else emptyList())

                if (allocation != null) {
                    allocation.commit(tx, values + ids.creationMetadata(uid))
                } else {
                    tx.update(target, values + mapOf(
                        "editedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedByUid" to uid
                    ))
                }
                tx.update(trainingRef, mapOf(
                    "attendanceId" to target.id,
                    "status" to TrainingStatus.COMPLETED,
                    "completedAt" to FieldValue.serverTimestamp()
                ))
                record.items.forEach { item ->
                    val history = playersRef.document(item.playerId)
                        .collection("attendanceHistory").document(trainingId)
                    tx.set(history, mapOf(
                        "attendanceId" to target.id,
                        "trainingId" to trainingId,
                        "teamId" to record.teamId,
                        "teamName" to record.teamName,
                        "date" to record.date,
                        "status" to item.status,
                        "justificationReason" to item.justificationReason,
                        "recordedAt" to FieldValue.serverTimestamp()
                    ))
                }
                audit()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el registro de asistencia de una sesión específica.
     */
    suspend fun getPlayerAttendanceForTraining(
        trainingId: String,
        player: PlayerModel
    ): Result<pe.edu.esan.sportpro.data.model.PlayerAttendanceItem?> = try {
        val entry = playersRef.document(player.id).collection("attendanceHistory").document(trainingId)
            .get().await().toObject(PlayerAttendanceRecord::class.java)
        Result.success(entry?.let {
            pe.edu.esan.sportpro.data.model.PlayerAttendanceItem(
                playerId = player.id, playerName = player.fullName, photoUrl = player.photoUrl,
                shirtNumber = player.shirtNumber, position = player.position,
                status = it.status, justificationReason = it.justificationReason
            )
        })
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun getAttendanceRecord(trainingId: String): Result<AttendanceRecordModel> {
        return try {
            val training = trainingsRef.document(trainingId).get().await()
            val doc = attendanceRef.document(training.getString("attendanceId") ?: trainingId).get().await()
            val model = doc.toObject(AttendanceRecordModel::class.java)
                ?: throw Exception("No se encontró registro de asistencia para esta sesión")
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el último registro de asistencia de un equipo para la función "Copiar asistencia previa".
     */
    suspend fun getLastAttendanceRecordForTeam(teamId: String): Result<AttendanceRecordModel?> {
        return try {
            val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
            val snapshot = attendanceRef
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("dtUid", uid)
                .get()
                .await()

            val lastRecord = snapshot.toObjects(AttendanceRecordModel::class.java)
                .sortedByDescending { Validators.parseDate(it.date) }
                .firstOrNull()

            Result.success(lastRecord)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los jugadores activos vinculados a un equipo específico.
     */
    suspend fun getActivePlayersForTeam(teamId: String, dtUid: String): Result<List<PlayerModel>> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("active", true)
                .get()
                .await()

            val players = snapshot.toObjects(PlayerModel::class.java)
                .sortedBy { it.lastName }

            Result.success(players)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la cuenta de jugador vinculada al UID de un usuario Jugador (JUG).
     */
    suspend fun getPlayerByUserUid(userUid: String): Result<PlayerModel> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("userUid", userUid)
                .get()
                .await()

            val player = snapshot.toObjects(PlayerModel::class.java).firstOrNull()
                ?: throw Exception("No se encontró perfil de jugador vinculado a esta cuenta.")

            Result.success(player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los hijos (jugadores) vinculados al UID de un Padre de familia (PAD).
     */
    suspend fun getChildrenByParentUid(parentUid: String): Result<List<PlayerModel>> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("parentUid", parentUid)
                .get()
                .await()

            val children = snapshot.toObjects(PlayerModel::class.java).sortedBy { it.name }
            Result.success(children)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real el historial personal de asistencia de un jugador (US-14).
     */
    fun observePlayerAttendanceHistory(playerId: String): Flow<List<PlayerAttendanceRecord>> = callbackFlow {
        if (playerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = playersRef
            .document(playerId)
            .collection("attendanceHistory")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(PlayerAttendanceRecord::class.java).orEmpty()
                trySend(list.sortedByDescending { Validators.parseDate(it.date) })
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observa el historial de asistencias de un equipo en tiempo real.
     */
    fun observeAttendanceHistoryByTeam(teamId: String): Flow<List<AttendanceRecordModel>> = callbackFlow {
        val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
        val registration = attendanceRef
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("dtUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AttendanceRecordModel::class.java).orEmpty()
                trySend(list.sortedByDescending { Validators.parseDate(it.date) })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Verifica si el registro de asistencia aún es editable (dentro del límite de 48 horas).
     */
    fun canEditAttendance(dateString: String): Boolean {
        return try {
            val recordDate = Validators.parseDate(dateString) ?: return false
            val today = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(recordDate, today)
            daysBetween in 0..2
        } catch (_: Exception) {
            false
        }
    }
}
