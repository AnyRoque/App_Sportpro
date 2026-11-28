package pe.edu.esan.sportpro.data.model

/**
 * US-15: Modelo para estadísticas acumuladas por jugador a lo largo de la temporada.
 */
data class PlayerStatsModel(
    val playerId: String = "",
    val playerName: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val shirtNumber: Int? = null,
    val position: String = "",
    val partidosConvocados: Int = 0,
    val partidosJugados: Int = 0,
    val partidosTitular: Int = 0,
    val minutosJugados: Int = 0,
    val goles: Int = 0,
    val tarjetasAmarillas: Int = 0,
    val tarjetasRojas: Int = 0,
    val faltasCometidas: Int = 0,
    val asistenciaEntrenamientosPorcentaje: Float = 0f
)

/**
 * US-15: Modelo para estadísticas acumuladas por equipo a lo largo de la temporada.
 */
data class TeamStatsModel(
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val partidosJugados: Int = 0,
    val partidosGanados: Int = 0,
    val partidosEmpatados: Int = 0,
    val partidosPerdidos: Int = 0,
    val golesFavor: Int = 0,
    val golesContra: Int = 0,
    val tarjetasAmarillasTotales: Int = 0,
    val tarjetasRojasTotales: Int = 0,
    val faltasTotales: Int = 0
) {
    val diferenciaGoles: Int get() = golesFavor - golesContra
    val puntos: Int get() = (partidosGanados * 3) + partidosEmpatados
}
