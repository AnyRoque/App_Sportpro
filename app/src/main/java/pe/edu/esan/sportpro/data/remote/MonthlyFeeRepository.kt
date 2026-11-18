package pe.edu.esan.sportpro.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.MonthlyFeeModel
import pe.edu.esan.sportpro.data.model.MonthlyFeeStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TeamModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Repositorio para la gestión de mensualidades (US-08).
 * Guarda las mensualidades dentro de la colección existente "players/{playerId}"
 * en el campo tipo Lista/Array denominado "monthlyFees".
 */
object MonthlyFeeRepository {

    private const val PLAYERS = "players"
    private const val TEAMS = "teams"
    private const val FIELD_MONTHLY_FEES = "monthlyFees"

    private val firestore = FirebaseFirestore.getInstance()
    private val playersCollection get() = firestore.collection(PLAYERS)

    /**
     * Lectura limpia (solo lectura) de los jugadores registrados en la colección "players".
     */
    suspend fun getActivePlayers(): Result<List<PlayerModel>> {
        return try {
            val snapshot = playersCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            val players = snapshot.toObjects(PlayerModel::class.java).sortedBy { it.lastName }
            Result.success(players)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lectura limpia (solo lectura) de los equipos registrados.
     */
    suspend fun getTeams(): Result<List<TeamModel>> {
        return try {
            val snapshot = firestore.collection(TEAMS).get().await()
            val teams = snapshot.toObjects(TeamModel::class.java).sortedBy { it.name }
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escucha en tiempo real todas las mensualidades almacenadas en el campo "monthlyFees" de los jugadores.
     */
    @Suppress("UNCHECKED_CAST")
    fun observeMonthlyFees(): Flow<List<MonthlyFeeModel>> = callbackFlow {
        val registration = playersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val allFees = mutableListOf<MonthlyFeeModel>()
            snapshot?.documents?.forEach { doc ->
                val playerId = doc.id
                val playerName = "${doc.getString("name").orEmpty()} ${doc.getString("lastName").orEmpty()}".trim()
                val teamId = doc.getString("teamId").orEmpty()
                val teamName = doc.getString("teamName").orEmpty()
                val category = doc.getString("category").orEmpty()

                val rawList = doc.get(FIELD_MONTHLY_FEES) as? List<Map<String, Any?>> ?: emptyList()
                rawList.forEach { map ->
                    val fee = MonthlyFeeModel.fromMap(
                        map = map,
                        fallbackPlayerId = playerId,
                        fallbackPlayerName = playerName,
                        fallbackTeamId = teamId,
                        fallbackTeamName = teamName,
                        fallbackCategory = category
                    )
                    allFees.add(fee)
                }
            }
            trySend(allFees)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Registra una nueva mensualidad agregándola al arreglo "monthlyFees" del documento del jugador.
     * Regla de Negocio: No permite más de una mensualidad para el mismo playerId y period.
     * Únicamente actualiza el campo "monthlyFees" sin alterar los demás campos del jugador.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun createMonthlyFee(fee: MonthlyFeeModel): Result<String> {
        return try {
            val cleanPlayerId = fee.playerId.trim()
            val cleanPeriod = fee.period.trim()
            val periodRegex = Regex("""^(0?[1-9]|1[0-2])/\d{4}$""")

            if (cleanPlayerId.isBlank()) {
                return Result.failure(Exception("Debe seleccionar un jugador válido."))
            }
            if (cleanPeriod.isBlank()) {
                return Result.failure(Exception("Debe ingresar un periodo válido (ej. MM/YYYY)."))
            }
            if (!cleanPeriod.matches(periodRegex)) {
                return Result.failure(Exception("El periodo debe tener el formato MM/YYYY (ejemplo: 11/2026)."))
            }
            if (fee.amount <= 0.0) {
                return Result.failure(Exception("El monto debe ser un valor positivo mayor a 0."))
            }
            if (fee.dueDate.isBlank()) {
                return Result.failure(Exception("Debe ingresar una fecha de vencimiento."))
            }

            val playerRef = playersCollection.document(cleanPlayerId)
            val docSnapshot = playerRef.get().await()

            if (!docSnapshot.exists()) {
                return Result.failure(Exception("El documento del jugador no existe en el sistema."))
            }

            val existingRawList = docSnapshot.get(FIELD_MONTHLY_FEES) as? List<Map<String, Any?>> ?: emptyList()
            val existingFees = existingRawList.map { MonthlyFeeModel.fromMap(it, fallbackPlayerId = cleanPlayerId) }

            // Validar si ya existe una mensualidad registrada para el mismo playerId y period
            val hasDuplicate = existingFees.any { it.period.equals(cleanPeriod, ignoreCase = true) }
            if (hasDuplicate) {
                return Result.failure(Exception("Ya existe una mensualidad registrada para este jugador en el periodo $cleanPeriod."))
            }

            val initialStatus = if (isDueDatePassed(fee.dueDate)) {
                MonthlyFeeStatus.EXPIRED
            } else {
                fee.status.ifBlank { MonthlyFeeStatus.PENDING }
            }

            val newId = UUID.randomUUID().toString()
            val feeToSave = fee.copy(
                id = newId,
                playerId = cleanPlayerId,
                period = cleanPeriod,
                status = initialStatus,
                createdAt = Timestamp.now()
            )

            val updatedListMaps = existingRawList.toMutableList().apply {
                add(feeToSave.toMap())
            }

            // Actualización quirúrgica únicamente del campo "monthlyFees" del jugador
            playerRef.update(FIELD_MONTHLY_FEES, updatedListMaps).await()
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el estado de una mensualidad específica dentro del arreglo "monthlyFees" del jugador.
     * Asigna automáticamente updatedAt (Timestamp actual) y updatedBy (email/uid del ADM).
     * Únicamente actualiza el campo "monthlyFees" sin alterar el resto del documento.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun updateFeeStatus(playerId: String, feeId: String, newStatus: String, updatedByAdmin: String): Result<Unit> {
        return try {
            val playerRef = playersCollection.document(playerId)
            val docSnapshot = playerRef.get().await()

            if (!docSnapshot.exists()) {
                return Result.failure(Exception("Jugador no encontrado."))
            }

            val existingRawList = docSnapshot.get(FIELD_MONTHLY_FEES) as? List<Map<String, Any?>> ?: emptyList()
            val updatedListMaps = existingRawList.map { map ->
                val currentId = map["id"] as? String ?: ""
                if (currentId == feeId) {
                    val mutableMap = map.toMutableMap()
                    mutableMap["status"] = newStatus
                    mutableMap["updatedBy"] = updatedByAdmin
                    if (newStatus == MonthlyFeeStatus.PAID) {
                        mutableMap["updatedAt"] = Timestamp.now()
                    }
                    mutableMap
                } else {
                    map
                }
            }

            // Actualizar solo el campo "monthlyFees"
            playerRef.update(FIELD_MONTHLY_FEES, updatedListMaps).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evalúa la fecha límite de mensualidades pendientes en la lista de jugadores y actualiza su estado a "Vencido".
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun autoCheckExpiredFees(fees: List<MonthlyFeeModel>): Result<Unit> {
        return try {
            val expiredPendingFees = fees.filter { it.status == MonthlyFeeStatus.PENDING && isDueDatePassed(it.dueDate) }
            val groupedByPlayer = expiredPendingFees.groupBy { it.playerId }

            for ((playerId, playerExpiredFees) in groupedByPlayer) {
                val playerRef = playersCollection.document(playerId)
                val docSnapshot = playerRef.get().await()
                if (!docSnapshot.exists()) continue

                val expiredIds = playerExpiredFees.map { it.id }.toSet()
                val existingRawList = docSnapshot.get(FIELD_MONTHLY_FEES) as? List<Map<String, Any?>> ?: emptyList()

                val updatedListMaps = existingRawList.map { map ->
                    val currentId = map["id"] as? String ?: ""
                    if (currentId in expiredIds) {
                        val mutableMap = map.toMutableMap()
                        mutableMap["status"] = MonthlyFeeStatus.EXPIRED
                        mutableMap["updatedBy"] = "SISTEMA_AUTO"
                        mutableMap
                    } else {
                        map
                    }
                }

                playerRef.update(FIELD_MONTHLY_FEES, updatedListMaps).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isDueDatePassed(dueDateStr: String): Boolean {
        if (dueDateStr.isBlank()) return false
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = nowCal.time

        for (fmtStr in formats) {
            try {
                val fmt = SimpleDateFormat(fmtStr, Locale.getDefault())
                val parsedDate = fmt.parse(dueDateStr.trim())
                if (parsedDate != null) {
                    val dueCal = Calendar.getInstance().apply {
                        time = parsedDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    if (todayStart.after(dueCal.time)) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }
}
