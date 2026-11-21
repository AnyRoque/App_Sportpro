package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AccumulatesStatType
import pe.edu.esan.sportpro.data.model.EventCatalogModel
import java.util.UUID

/**
 * Repositorio para la Gestión Configurable del Catálogo de Eventos del Partido (US-09).
 * Incluye sincronización reactiva en tiempo real y respaldo local ante reglas de seguridad de Firestore.
 */
object EventCatalogRepository {

    private const val EVENT_CATALOG = "event_catalog"
    private const val MATCH_EVENTS = "match_events"

    private val firestore = FirebaseFirestore.getInstance()
    private val catalogCollection get() = firestore.collection(EVENT_CATALOG)

    // Lista por defecto de los 13 eventos base (Inician desactivados para que el ADM los active según reglas)
    private val defaultEventsBase = listOf(
        EventCatalogModel(id = "base_1", name = "Inicio", icon = "play_arrow", colorHex = "#4CAF50", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_2", name = "Fin del primer tiempo", icon = "pause", colorHex = "#FF9800", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_3", name = "Fin del partido", icon = "flag", colorHex = "#F44336", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_4", name = "Saque de meta", icon = "sports_soccer", colorHex = "#2196F3", requiresTeam = true, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_5", name = "Saque lateral", icon = "swap_horiz", colorHex = "#00BCD4", requiresTeam = true, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_6", name = "Tiro de esquina", icon = "navigation", colorHex = "#009688", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_7", name = "Falta", icon = "warning", colorHex = "#FF5722", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.FOULS, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_8", name = "Penal", icon = "gps_fixed", colorHex = "#E91E63", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.FOULS, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_9", name = "Gol", icon = "sports_soccer", colorHex = "#4CAF50", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = true, accumulatesStat = AccumulatesStatType.GOALS, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_10", name = "Tarjeta amarilla", icon = "style", colorHex = "#FFEB3B", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.CARDS, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_11", name = "Tarjeta roja", icon = "style", colorHex = "#D32F2F", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.CARDS, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_12", name = "Fuera de juego", icon = "block", colorHex = "#9C27B0", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true),
        EventCatalogModel(id = "base_13", name = "Cambio", icon = "cached", colorHex = "#3F51B5", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = true, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = false, isSystemDefault = true)
    )

    private val _localCatalog = MutableStateFlow<List<EventCatalogModel>>(defaultEventsBase)

    init {
        try {
            catalogCollection.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.toObjects(EventCatalogModel::class.java)
                    if (list.isNotEmpty()) {
                        _localCatalog.value = list.sortedBy { it.name }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        return try {
            val snapshot = catalogCollection.get().await()
            if (snapshot.isEmpty) {
                for (event in defaultEventsBase) {
                    val docRef = catalogCollection.document()
                    docRef.set(event.copy(id = docRef.id)).await()
                }
            } else {
                val list = snapshot.toObjects(EventCatalogModel::class.java)
                if (list.isNotEmpty()) {
                    _localCatalog.value = list.sortedBy { it.name }
                }
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    fun observeEventCatalog(): Flow<List<EventCatalogModel>> = _localCatalog.asStateFlow()

    suspend fun createEventType(item: EventCatalogModel): Result<String> {
        val cleanName = item.name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(Exception("Debe ingresar un nombre para el tipo de evento."))
        }

        val currentList = _localCatalog.value
        val exists = currentList.any { it.name.trim().equals(cleanName, ignoreCase = true) }
        if (exists) {
            return Result.failure(Exception("Ya existe un tipo de evento con el nombre '$cleanName'."))
        }

        val newId = UUID.randomUUID().toString()
        val toSave = item.copy(id = newId, name = cleanName)

        val updatedList = (currentList + toSave).sortedBy { it.name }
        _localCatalog.value = updatedList

        try {
            catalogCollection.document(newId).set(toSave).await()
        } catch (_: Exception) {}

        return Result.success(newId)
    }

    suspend fun updateEventType(item: EventCatalogModel): Result<Unit> {
        val cleanName = item.name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(Exception("Debe ingresar un nombre para el tipo de evento."))
        }

        val currentList = _localCatalog.value
        val exists = currentList.any { it.id != item.id && it.name.trim().equals(cleanName, ignoreCase = true) }
        if (exists) {
            return Result.failure(Exception("Ya existe otro tipo de evento con el nombre '$cleanName'."))
        }

        val updatedList = currentList.map {
            if (it.id == item.id) item.copy(name = cleanName) else it
        }.sortedBy { it.name }
        _localCatalog.value = updatedList

        try {
            catalogCollection.document(item.id).set(item.copy(name = cleanName)).await()
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    suspend fun toggleActiveStatus(id: String, active: Boolean): Result<Unit> {
        val currentList = _localCatalog.value
        val updatedList = currentList.map {
            if (it.id == id) it.copy(active = active) else it
        }
        _localCatalog.value = updatedList

        try {
            catalogCollection.document(id).update("active", active).await()
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    suspend fun deleteEventType(item: EventCatalogModel): Result<Unit> {
        val nameClean = item.name.trim()

        try {
            val eventsSnapshotByName = firestore.collection(MATCH_EVENTS)
                .whereEqualTo("type", nameClean)
                .get()
                .await()

            if (!eventsSnapshotByName.isEmpty) {
                return Result.failure(Exception("El tipo de evento '$nameClean' ya ha sido utilizado en partidos registrados. No se puede eliminar, únicamente desactivar."))
            }
        } catch (_: Exception) {}

        val currentList = _localCatalog.value
        _localCatalog.value = currentList.filter { it.id != item.id }

        try {
            catalogCollection.document(item.id).delete().await()
        } catch (_: Exception) {}

        return Result.success(Unit)
    }
}
