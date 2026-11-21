package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AccumulatesStatType
import pe.edu.esan.sportpro.data.model.EventCatalogModel

/**
 * Repositorio para la Gestión Configurable del Catálogo de Eventos del Partido (US-09).
 * Opera exclusivamente sobre la colección "event_catalog".
 */
object EventCatalogRepository {

    private const val EVENT_CATALOG = "event_catalog"
    private const val MATCH_EVENTS = "match_events"

    private val firestore = FirebaseFirestore.getInstance()
    private val catalogCollection get() = firestore.collection(EVENT_CATALOG)

    /**
     * Precarga los 13 tipos de eventos base obligatorios si el catálogo en Firestore está vacío.
     */
    suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        return try {
            val snapshot = catalogCollection.get().await()
            if (snapshot.isEmpty) {
                val defaultEvents = listOf(
                    EventCatalogModel(name = "Inicio", icon = "play_arrow", colorHex = "#4CAF50", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Fin del primer tiempo", icon = "pause", colorHex = "#FF9800", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Fin del partido", icon = "flag", colorHex = "#F44336", requiresTeam = false, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Saque de meta", icon = "sports_soccer", colorHex = "#2196F3", requiresTeam = true, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Saque lateral", icon = "swap_horiz", colorHex = "#00BCD4", requiresTeam = true, requiresPlayer = false, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Tiro de esquina", icon = "navigation", colorHex = "#009688", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Falta", icon = "warning", colorHex = "#FF5722", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.FOULS, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Penal", icon = "gps_fixed", colorHex = "#E91E63", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.FOULS, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Gol", icon = "sports_soccer", colorHex = "#4CAF50", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = true, accumulatesStat = AccumulatesStatType.GOALS, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Tarjeta amarilla", icon = "style", colorHex = "#FFEB3B", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.CARDS, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Tarjeta roja", icon = "style", colorHex = "#D32F2F", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.CARDS, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Fuera de juego", icon = "block", colorHex = "#9C27B0", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = false, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true),
                    EventCatalogModel(name = "Cambio", icon = "cached", colorHex = "#3F51B5", requiresTeam = true, requiresPlayer = true, requiresSecondPlayer = true, affectsScore = false, accumulatesStat = AccumulatesStatType.NONE, active = true, isSystemDefault = true)
                )

                for (event in defaultEvents) {
                    val docRef = catalogCollection.document()
                    docRef.set(event.copy(id = docRef.id)).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sincronización en tiempo real con el catálogo de eventos.
     */
    fun observeEventCatalog(): Flow<List<EventCatalogModel>> = callbackFlow {
        val registration = catalogCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(EventCatalogModel::class.java).orEmpty()
                .sortedBy { it.name }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Crea un nuevo tipo de evento configurado por el Administrador.
     * Criterio: El nombre del tipo de evento debe ser único.
     */
    suspend fun createEventType(item: EventCatalogModel): Result<String> {
        return try {
            val cleanName = item.name.trim()
            if (cleanName.isBlank()) {
                return Result.failure(Exception("Debe ingresar un nombre para el tipo de evento."))
            }

            // Validar unicidad del nombre
            val snapshot = catalogCollection.get().await()
            val exists = snapshot.documents.any {
                val name = it.getString("name").orEmpty().trim()
                name.equals(cleanName, ignoreCase = true)
            }

            if (exists) {
                return Result.failure(Exception("Ya existe un tipo de evento con el nombre '$cleanName'."))
            }

            val docRef = catalogCollection.document()
            val toSave = item.copy(id = docRef.id, name = cleanName)
            docRef.set(toSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza la configuración de un tipo de evento existente.
     */
    suspend fun updateEventType(item: EventCatalogModel): Result<Unit> {
        return try {
            val cleanName = item.name.trim()
            if (cleanName.isBlank()) {
                return Result.failure(Exception("Debe ingresar un nombre para el tipo de evento."))
            }

            // Validar unicidad del nombre excluyendo el ID actual
            val snapshot = catalogCollection.get().await()
            val exists = snapshot.documents.any {
                it.id != item.id && it.getString("name").orEmpty().trim().equals(cleanName, ignoreCase = true)
            }

            if (exists) {
                return Result.failure(Exception("Ya existe otro tipo de evento con el nombre '$cleanName'."))
            }

            catalogCollection.document(item.id).set(item.copy(name = cleanName)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activa o desactiva un tipo de evento.
     */
    suspend fun toggleActiveStatus(id: String, active: Boolean): Result<Unit> {
        return try {
            catalogCollection.document(id).update("active", active).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Intenta eliminar un tipo de evento.
     * Criterio: Los tipos ya utilizados en partidos no podrán eliminarse, solo desactivarse.
     */
    suspend fun deleteEventType(item: EventCatalogModel): Result<Unit> {
        return try {
            // Verificar si el evento ha sido utilizado en partidos registrados
            val eventsSnapshot = firestore.collection(MATCH_EVENTS)
                .whereEqualTo("type", item.name)
                .get()
                .await()

            if (!eventsSnapshot.isEmpty) {
                return Result.failure(Exception("El tipo de evento '${item.name}' ya ha sido utilizado en partidos registrados. No se puede eliminar, únicamente desactivar."))
            }

            catalogCollection.document(item.id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
