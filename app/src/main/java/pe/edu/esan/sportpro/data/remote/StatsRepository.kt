package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.PlayerStatsModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatsModel

/**
 * US-15: Repositorio para la consulta y cálculo de estadísticas acumuladas por equipo y jugador.
 */
object StatsRepository {

    private val firestore get() = FirebaseFirestore.getInstance()

    /**
     * Obtiene todos los equipos aprobados del sistema o asociados al usuario.
     */
    suspend fun getApprovedTeams(): Result<List<TeamModel>> = try {
        val snap = firestore.collection("teams")
            .whereEqualTo("status", "APROBADO")
            .get()
            .await()
        Result.success(snap.toObjects(TeamModel::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Calcula dinámicamente las estadísticas acumuladas de un equipo y sus jugadores.
     */
    suspend fun getAccumulatedStats(teamId: String): Result<Pair<TeamStatsModel, List<PlayerStatsModel>>> = try {
        // 1. Obtener datos del equipo y plantilla de jugadores
        val teamRes = TeamRepository.getTeam(teamId).getOrNull()
        val teamName = teamRes?.name ?: "Equipo"
        val category = teamRes?.category ?: ""

        val playersSnap = firestore.collection("players")
            .whereEqualTo("teamId", teamId)
            .get()
            .await()
        val players = playersSnap.toObjects(PlayerModel::class.java)

        // 2. Obtener partidos finalizados del equipo
        val matchesSnap = firestore.collection("matches")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("status", MatchStatus.FINISHED)
            .get()
            .await()
        val finishedMatches = matchesSnap.toObjects(MatchModel::class.java)

        // 3. Inicializar acumuladores por equipo
        var winCount = 0
        var drawCount = 0
        var lossCount = 0
        var gfSum = 0
        var gcSum = 0
        var teamYellowCards = 0
        var teamRedCards = 0
        var teamFouls = 0

        // Estructuras de acumulación por jugador (playerId -> Datos acumulados)
        val convocadosMap = mutableMapOf<String, Int>()
        val titularMap = mutableMapOf<String, Int>()
        val jugadosMap = mutableMapOf<String, Int>()
        val minutosMap = mutableMapOf<String, Int>()
        val golesMap = mutableMapOf<String, Int>()
        val amarillasMap = mutableMapOf<String, Int>()
        val rojasMap = mutableMapOf<String, Int>()
        val faltasMap = mutableMapOf<String, Int>()

        // 4. Procesar cada partido finalizado
        for (match in finishedMatches) {
            val own = match.ownScore
            val rival = match.rivalScore
            gfSum += own
            gcSum += rival
            when {
                own > rival -> winCount++
                own == rival -> drawCount++
                else -> lossCount++
            }

            val totalDuration = match.totalMatchDuration

            // Obtener convocatorias del partido
            val callUpsSnap = firestore.collection("matches").document(match.id).collection("callups").get().await()
            val callUps = callUpsSnap.toObjects(CallUpItemModel::class.java)
            callUps.forEach { c ->
                convocadosMap[c.playerId] = (convocadosMap[c.playerId] ?: 0) + 1
            }

            // Obtener alineación inicial del partido
            val lineupDoc = firestore.collection("matches").document(match.id)
                .collection("lineup").document("starting").get().await()
            val lineup = lineupDoc.toObject(LineupModel::class.java)
            val starterPlayerIds = lineup?.positions?.mapNotNull { it.playerId }?.toSet().orEmpty()
            starterPlayerIds.forEach { pid ->
                titularMap[pid] = (titularMap[pid] ?: 0) + 1
                jugadosMap[pid] = (jugadosMap[pid] ?: 0) + 1
            }

            // Obtener eventos activos del partido
            val eventsSnap = firestore.collection("matches").document(match.id).collection("events").get().await()
            val events = eventsSnap.toObjects(MatchEventModel::class.java)
                .filter { it.status == EventStatus.ACTIVE }

            // Procesar minutos y participación por sustituciones
            val minutesInMatch = mutableMapOf<String, Int>()
            val playersParticipated = mutableSetOf<String>()
            playersParticipated.addAll(starterPlayerIds)

            // Para titulares, minutos iniciales asumen la duración total del partido
            starterPlayerIds.forEach { pid ->
                minutesInMatch[pid] = totalDuration
            }

            events.forEach { e ->
                val pid = e.playerId
                when (e.type) {
                    EventType.GOAL -> {
                        if (pid != null && pid.isNotBlank()) {
                            golesMap[pid] = (golesMap[pid] ?: 0) + 1
                            playersParticipated.add(pid)
                        }
                    }
                    EventType.YELLOW_CARD -> {
                        teamYellowCards++
                        if (pid != null && pid.isNotBlank()) {
                            amarillasMap[pid] = (amarillasMap[pid] ?: 0) + 1
                            playersParticipated.add(pid)
                        }
                    }
                    EventType.RED_CARD -> {
                        teamRedCards++
                        if (pid != null && pid.isNotBlank()) {
                            rojasMap[pid] = (rojasMap[pid] ?: 0) + 1
                            playersParticipated.add(pid)
                        }
                    }
                    EventType.FOUL -> {
                        teamFouls++
                        if (pid != null && pid.isNotBlank()) {
                            faltasMap[pid] = (faltasMap[pid] ?: 0) + 1
                            playersParticipated.add(pid)
                        }
                    }
                    EventType.SUBSTITUTION -> {
                        val outId = e.jugadorSaleId
                        val inId = e.jugadorEntraId
                        val subMin = e.minute

                        if (outId != null && outId.isNotBlank()) {
                            minutesInMatch[outId] = subMin
                            playersParticipated.add(outId)
                        }
                        if (inId != null && inId.isNotBlank()) {
                            val playedIn = (totalDuration - subMin).coerceAtLeast(0)
                            minutesInMatch[inId] = (minutesInMatch[inId] ?: 0) + playedIn
                            playersParticipated.add(inId)
                            jugadosMap[inId] = (jugadosMap[inId] ?: 0) + 1
                        }
                    }
                }
            }

            playersParticipated.forEach { pid ->
                val m = minutesInMatch[pid] ?: totalDuration
                minutosMap[pid] = (minutosMap[pid] ?: 0) + m
            }
        }

        // 5. Obtener asistencia a entrenamientos del equipo
        val attendancesSnap = firestore.collection("attendance")
            .whereEqualTo("teamId", teamId)
            .get()
            .await()
        val attendanceRecords = attendancesSnap.toObjects(AttendanceRecordModel::class.java)

        val playerAttendanceStats = mutableMapOf<String, Pair<Int, Int>>() // playerId -> (atendido, total)
        for (rec in attendanceRecords) {
            rec.items.forEach { pr ->
                val (attended, total) = playerAttendanceStats[pr.playerId] ?: Pair(0, 0)
                val isAttended = pr.status == AttendanceStatus.ATTENDED || pr.status == AttendanceStatus.LATE || pr.status == AttendanceStatus.JUSTIFIED
                val newAttended = if (isAttended) attended + 1 else attended
                playerAttendanceStats[pr.playerId] = Pair(newAttended, total + 1)
            }
        }

        // 6. Construir modelos de resultado
        val teamStats = TeamStatsModel(
            teamId = teamId,
            teamName = teamName,
            category = category,
            partidosJugados = finishedMatches.size,
            partidosGanados = winCount,
            partidosEmpatados = drawCount,
            partidosPerdidos = lossCount,
            golesFavor = gfSum,
            golesContra = gcSum,
            tarjetasAmarillasTotales = teamYellowCards,
            tarjetasRojasTotales = teamRedCards,
            faltasTotales = teamFouls
        )

        val playerStatsList = players.map { p ->
            val (att, tot) = playerAttendanceStats[p.id] ?: Pair(0, 0)
            val attPct = if (tot > 0) (att.toFloat() / tot.toFloat()) * 100f else 0f

            PlayerStatsModel(
                playerId = p.id,
                playerName = p.fullName,
                teamId = teamId,
                teamName = teamName,
                category = p.category,
                shirtNumber = p.shirtNumber,
                position = p.position,
                partidosConvocados = convocadosMap[p.id] ?: 0,
                partidosJugados = jugadosMap[p.id] ?: 0,
                partidosTitular = titularMap[p.id] ?: 0,
                minutosJugados = minutosMap[p.id] ?: 0,
                goles = golesMap[p.id] ?: 0,
                tarjetasAmarillas = amarillasMap[p.id] ?: 0,
                tarjetasRojas = rojasMap[p.id] ?: 0,
                faltasCometidas = faltasMap[p.id] ?: 0,
                asistenciaEntrenamientosPorcentaje = attPct
            )
        }.sortedByDescending { it.goles }

        Result.success(Pair(teamStats, playerStatsList))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
