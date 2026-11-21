package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object AccumulatesStatType {
    const val NONE = "Ninguno"
    const val GOALS = "Goles"
    const val CARDS = "Tarjetas"
    const val FOULS = "Faltas"
    val ALL = listOf(NONE, GOALS, CARDS, FOULS)
}

/**
 * Modelo para el Catálogo Configurable de Eventos del Partido (US-09 - Rol ADM).
 * Almacenado en la colección Firestore "event_catalog".
 */
data class EventCatalogModel(
    @DocumentId var id: String = "",
    val name: String = "", // Nombre único del tipo de evento (ej. "Gol", "Falta")
    val icon: String = "sports_soccer", // Ícono representativo
    val colorHex: String = "#2196F3", // Color en hexadecimal
    val requiresTeam: Boolean = true,
    val requiresPlayer: Boolean = true,
    val requiresSecondPlayer: Boolean = false, // ej. Para Cambio (sale/entra)
    val affectsScore: Boolean = false,
    val accumulatesStat: String = AccumulatesStatType.NONE, // "Goles", "Tarjetas", "Faltas", "Ninguno"
    val active: Boolean = true, // Activo / Desactivo
    val isSystemDefault: Boolean = false, // Indica si pertenece a los 13 tipos base
    @ServerTimestamp val createdAt: Timestamp? = null
)
