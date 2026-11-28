package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.UserModel

/**
 * Repositorio de Cloud Firestore para la gestión de Partidos (US-16).
 */
object MatchRepository {

    private const val MATCHES = "matches"
    private const val TEAMS = "teams"
    private const val PLAYERS = "players"
    private const val NOTIFICATIONS = "notifications"

    private val firestore = FirebaseFirestore.getInstance()
    private val ids = SportProIdGenerator(firestore)
    private val matches get() = firestore.collection(MATCHES)
    private val teams get() = firestore.collection(TEAMS)
    private val players get() = firestore.collection(PLAYERS)
    private val notifications get() = firestore.collection(NOTIFICATIONS)

    /**
     * Obtiene los equipos en estado APROBADO asignados al Director Técnico (DT).
     */
    suspend fun getApprovedTeamsByDt(dtUid: String): Result<List<TeamModel>> {
        return try {
            val snapshot = teams
                .whereEqualTo("dtUid", dtUid)
                .whereEqualTo("status", TeamStatus.APPROVED)
                .get()
                .await()
            val list = snapshot.toObjects(TeamModel::class.java).sortedBy { it.name }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los equipos aprobados del sistema para selección como rival.
     */
    suspend fun getAllApprovedTeams(): Result<List<TeamModel>> {
        return try {
            val snapshot = teams
                .whereEqualTo("status", TeamStatus.APPROVED)
                .get()
                .await()
            val list = snapshot.toObjects(TeamModel::class.java).sortedBy { it.name }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza la formación táctica del partido.
     */
    suspend fun updateFormation(matchId: String, formation: String): Result<Unit> {
        return try {
            matches.document(matchId).update("formation", formation).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cambia el estado de titularidad de un jugador convocado.
     */
    suspend fun updatePlayerStarterStatus(matchId: String, playerId: String, isStarter: Boolean): Result<Unit> {
        return try {
            ids.update(callUpRef(matchId, playerId), mapOf("isStarter" to isStarter))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los usuarios integrantes del equipo (DT y miembros vinculados) para selección de operador.
     */
    suspend fun getTeamMembers(teamId: String, dtUser: UserModel): Result<List<UserModel>> {
        return try {
            val list = mutableListOf(dtUser)
            val uids = fetchTeamMemberUids(teamId, dtUser.uid).filter { it != dtUser.uid }
            if (uids.isNotEmpty()) {
                val userDocs = firestore.collection("users")
                    .whereIn("uid", uids.take(10))
                    .get()
                    .await()
                list.addAll(userDocs.toObjects(UserModel::class.java))
            }
            Result.success(list.distinctBy { it.uid })
        } catch (_: Exception) {
            Result.success(listOf(dtUser))
        }
    }

    /**
     * Registra un nuevo partido programado y notifica a los integrantes del equipo local y al equipo contrincante (US-16).
     */
    suspend fun createMatch(match: MatchModel): Result<String> {
        return try {
            val localMemberUids = fetchTeamMemberUids(match.teamId, match.dtUid)
            val rivalTeam = findRivalTeam(match)
            val rivalMemberUids = rivalTeam?.let { fetchTeamMemberUids(it.id, it.dtUid) }.orEmpty()

            val toSave = match.copy(
                rivalTeamId = rivalTeam?.id.orEmpty(),
                operatorUid = match.operatorUid.ifBlank { match.dtUid },
                memberUids = (localMemberUids + rivalMemberUids).distinct(),
                status = MatchStatus.PROGRAMMED,
                cancelReason = ""
            )
            val docRef = ids.create(matches, SportProEntity.MATCH, toSave.firestoreFields()) { tx, _ ->
                requireTeam(tx, teams, match.teamId, match.dtUid)
                ({})
            }

            // 3. Notificar a los miembros del equipo local
            dispatchMatchNotifications(
                teamId = match.teamId,
                recipientUids = localMemberUids.filter { it != match.dtUid },
                title = "Nuevo Partido Programado",
                message = "Partido programado vs ${match.rival} el ${match.date} a las ${match.time} hs en ${match.location}.",
                matchId = docRef.id
            )

            // 4. Notificar al DT y miembros del equipo CONTRINCANTE
            if (rivalMemberUids.isNotEmpty()) {
                dispatchMatchNotifications(
                    teamId = match.teamId,
                    recipientUids = rivalMemberUids.distinct(),
                    title = "¡Nuevo Partido vs Tu Equipo!",
                    message = "El equipo ${match.teamName} ha programado un partido contra tu equipo (${match.rival}) el ${match.date} a las ${match.time} hs en ${match.location}.",
                    matchId = docRef.id
                )
            }

            Result.success(docRef.id)
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Result.failure(IllegalStateException(
                    "Firestore rechazó guardar el partido. Revisa el acceso del DT y las reglas publicadas de partidos y auditoría.",
                    error
                ))
            } else {
                Result.failure(error)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun findRivalTeam(match: MatchModel): TeamModel? =
        teams.whereEqualTo("name", match.rival.trim())
            .whereEqualTo("status", TeamStatus.APPROVED)
            .get().await().toObjects(TeamModel::class.java)
            .filter { it.id != match.teamId }
            .singleOrNull()

    /**
     * Actualiza la información de un partido existente.
     */
    suspend fun updateMatch(match: MatchModel): Result<Unit> {
        return try {
            val target = matches.document(match.id)
            val previous = target.get().await().toObject(MatchModel::class.java)
                ?: error("Partido no encontrado")
            check(previous.status == MatchStatus.PROGRAMMED) { "Solo puedes editar un partido programado" }
            val changedTeams = previous.teamId != match.teamId || previous.rival != match.rival
            check(!changedTeams || (!previous.hasCallUp && !previous.hasLineup)) {
                "El partido ya tiene convocatoria o alineación para los equipos seleccionados"
            }

            val rivalTeam = if (changedTeams || previous.rivalTeamId.isBlank()) findRivalTeam(match) else null
            val fields = match.copy(
                rivalTeamId = if (changedTeams) rivalTeam?.id.orEmpty()
                    else previous.rivalTeamId.ifBlank { rivalTeam?.id.orEmpty() }
            ).firestoreFields().filterKeys {
                it in setOf("teamId", "teamName", "category", "rival", "rivalTeamId", "matchType",
                    "condition", "modality", "periodMinutes", "date", "time", "matchDate",
                    "location", "notes", "operatorUid", "operatorName")
            }
            val members = if (changedTeams) {
                val local = fetchTeamMemberUids(match.teamId, match.dtUid)
                val rival = rivalTeam?.let { fetchTeamMemberUids(it.id, it.dtUid) }.orEmpty()
                mapOf("memberUids" to (local + rival).distinct())
            } else {
                emptyMap()
            }
            ids.update(target, fields + members)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancela un partido programado y notifica a los integrantes.
     */
    suspend fun cancelMatch(matchId: String, reason: String, dtUid: String): Result<Unit> {
        return try {
            val doc = matches.document(matchId).get().await()
            val match = doc.toObject(MatchModel::class.java) ?: throw Exception("Partido no encontrado")

            if (match.dtUid != dtUid) throw Exception("No tienes permisos para cancelar este partido")

            matches.document(matchId).update(
                mapOf(
                    "status" to MatchStatus.CANCELED,
                    "cancelReason" to reason.trim()
                )
            ).await()

            val recipientUids = match.memberUids.filter { it != dtUid }
            dispatchMatchNotifications(
                teamId = match.teamId,
                recipientUids = recipientUids,
                title = "Partido Cancelado",
                message = "El partido vs ${match.rival} programado para el ${match.date} ha sido cancelado. Motivo: ${reason.trim()}",
                matchId = matchId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el detalle de un partido específico.
     */
    suspend fun getMatch(matchId: String): Result<MatchModel> {
        return try {
            val doc = matches.document(matchId).get().await()
            val model = doc.toObject(MatchModel::class.java) ?: throw Exception("Partido no encontrado")
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real los partidos asignados al usuario (DT local, DT rival, Jugador o Padre) (US-16).
     */
    fun observeMatchesForUser(user: UserModel): Flow<List<MatchModel>> = callbackFlow {
        val registrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val matchesBySource = mutableMapOf<String, List<MatchModel>>()
        val observedQueryKeys = mutableSetOf<String>()

        fun publishMatches() {
            val combined = synchronized(matchesBySource) {
                matchesBySource.values.flatten().distinctBy { it.id }
            }
            trySend(combined)
        }

        fun observeQuery(key: String, query: Query, closeOnError: Boolean = false) {
            if (!observedQueryKeys.add(key)) return
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (closeOnError) close(error)
                    return@addSnapshotListener
                }
                synchronized(matchesBySource) {
                    matchesBySource[key] = snapshot?.toObjects(MatchModel::class.java).orEmpty()
                }
                publishMatches()
            }
            registrations.add(registration)
        }

        // Mantiene el acceso por UID para DTs, operadores y partidos con convocatorias existentes.
        observeQuery("member:${user.uid}", matches.whereArrayContains("memberUids", user.uid), closeOnError = true)

        if (user.roleEnum == Role.DT) {
            observeQuery("coach:${user.uid}", matches.whereEqualTo("dtUid", user.uid))
        }

        awaitClose { registrations.forEach { it.remove() } }
    }

    private suspend fun fetchTeamMemberUids(teamId: String, dtUid: String): List<String> {
        val set = mutableSetOf(dtUid)
        try {
            val docs = players.whereEqualTo("teamId", teamId).get().await()
            val list = docs.toObjects(PlayerModel::class.java)
                .filter { it.active && it.linkStatus == LinkStatus.ACCEPTED }
            list.forEach { p ->
                if (p.userUid.isNotBlank()) set.add(p.userUid)
                if (p.parentUid.isNotBlank()) set.add(p.parentUid)
            }
        } catch (_: Exception) {}
        return set.toList()
    }

    private suspend fun validateCallUpOwnership(
        callUpsRef: com.google.firebase.firestore.CollectionReference,
        playersToAdd: List<PlayerModel>,
        fallbackTeamId: String
    ) {
        val existing = callUpsRef.get().await().toObjects(CallUpItemModel::class.java)
        val claimedPlayers = mutableMapOf<String, String>()
        existing.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }.forEach { item ->
            val identity = item.userUid.ifBlank { "player:${item.playerId}" }
            claimedPlayers.putIfAbsent(identity, item.teamId.ifBlank { fallbackTeamId })
        }
        val identitiesInRequest = mutableSetOf<String>()
        playersToAdd.forEach { player ->
            val identity = player.userUid.ifBlank { "player:${player.id}" }
            val existingTeamId = claimedPlayers[identity]
            val newTeamId = player.teamId.ifBlank { fallbackTeamId }
            if (existingTeamId != null) {
                throw Exception("${player.fullName} ya fue convocado por otro equipo. Se mantiene la primera convocatoria.")
            }
            if (!identitiesInRequest.add(identity)) {
                throw Exception("${player.fullName} está repetido en la convocatoria.")
            }
            claimedPlayers[identity] = newTeamId
        }
    }

    suspend fun createCallUp(
        match: MatchModel,
        selectedPlayers: List<PlayerModel>,
        deadlineDate: String,
        deadlineTime: String
    ): Result<Unit> = saveCallUps(match, selectedPlayers, deadlineDate, deadlineTime, updateDeadline = true)

    suspend fun addPlayersToCallUp(
        match: MatchModel,
        additionalPlayers: List<PlayerModel>
    ): Result<Unit> = saveCallUps(match, additionalPlayers, match.callUpDeadlineDate,
        match.callUpDeadlineTime, updateDeadline = false)

    @Suppress("UNCHECKED_CAST")
    private suspend fun saveCallUps(
        match: MatchModel,
        selected: List<PlayerModel>,
        deadlineDate: String,
        deadlineTime: String,
        updateDeadline: Boolean
    ): Result<Unit> = try {
        val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
        val matchRef = matches.document(match.id)
        val collection = matchRef.collection("callups")
        validateCallUpOwnership(collection, selected, match.teamId)
        val recipients = ids.transact { tx ->
            val current = tx.get(matchRef)
            check(current.exists()) { "No se encontró el partido" }
            val existing = current.get("callupIds") as? Map<String, String> ?: emptyMap()
            val identities = current.get("callupUserIds") as? Map<String, String> ?: emptyMap()
            val roster = selected.map { input ->
                val player = tx.get(players.document(input.id)).toObject(PlayerModel::class.java)
                    ?: error("Un jugador ya no está disponible")
                check(player.teamId == input.teamId && player.teamId.isNotBlank()) {
                    "El jugador cambió de equipo. Recarga el plantel."
                }
                check(player.id !in existing && (player.userUid.isBlank() || player.userUid !in identities)) {
                    "El jugador ya fue convocado"
                }
                player
            }
            val (references, reserve) = ids.allocateCallUps(tx, collection, roster.size)
            val audit = ids.prepareAudit(tx, uid, matchRef, "UPDATE", listOf("callupIds"))
            val newKeys = roster.zip(references).associate { (player, ref) -> player.id to ref.id }
            val newAccounts = roster.filter { it.userUid.isNotBlank() }.associate { it.userUid to it.id }
            check(newAccounts.size == roster.count { it.userUid.isNotBlank() }) { "Hay una cuenta repetida en la convocatoria" }
            val recipients = roster.flatMap { listOf(it.userUid, it.parentUid) }.filter { it.isNotBlank() }.distinct()
            val updates = mutableMapOf<String, Any>(
                "hasCallUp" to true,
                "callupIds" to (existing + newKeys),
                "callupUserIds" to (identities + newAccounts),
                "memberUids" to FieldValue.arrayUnion(*recipients.toTypedArray()),
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedByUid" to uid
            )
            if (updateDeadline) {
                updates["callUpDeadlineDate"] = deadlineDate
                updates["callUpDeadlineTime"] = deadlineTime
            }
            reserve()
            roster.zip(references).forEach { (player, ref) ->
                val item = CallUpItemModel(
                    matchId = match.id,
                    teamId = player.teamId,
                    playerId = player.id,
                    playerName = player.fullName,
                    playerPhotoUrl = player.photoUrl,
                    position = player.position,
                    isMinor = player.parentUid.isNotBlank(),
                    userUid = player.userUid,
                    parentUid = player.parentUid,
                    responseStatus = CallUpStatus.PENDING,
                    deadlineDate = deadlineDate,
                    deadlineTime = deadlineTime
                )
                tx.set(ref, item.firestoreFields() + ids.creationMetadata(uid) + ("id" to ref.id))
            }
            tx.update(matchRef, updates)
            audit()
            recipients
        }
        dispatchMatchNotifications(
            teamId = match.teamId,
            recipientUids = recipients,
            title = "Nueva Convocatoria",
            message = "Has sido convocado para el partido vs ${match.rival}. Confirma tu asistencia antes del $deadlineDate $deadlineTime hs.",
            matchId = match.id
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun callUpRef(matchId: String, playerId: String): com.google.firebase.firestore.DocumentReference {
        val match = matches.document(matchId)
        val key = match.get().await().getString("callupIds.$playerId") ?: playerId
        return match.collection("callups").document(key)
    }

    /**
     * Responde a una convocatoria (JUG o PAD) y notifica al DT del partido en tiempo real (US-17).
     */
    suspend fun respondCallUp(
        matchId: String,
        playerId: String,
        status: String,
        reason: String = "",
        comment: String = ""
    ): Result<Unit> {
        return try {
            val ref = callUpRef(matchId, playerId)
            ids.update(ref, mapOf(
                "responseStatus" to status,
                "rejectReason" to reason.trim(),
                "rejectComment" to comment.trim()
            ))

            // Notificar al DT del partido
            try {
                val matchDoc = matches.document(matchId).get().await()
                val m = matchDoc.toObject(MatchModel::class.java)
                if (m != null && m.dtUid.isNotBlank()) {
                    val itemDoc = ref.get().await()
                    val item = itemDoc.toObject(CallUpItemModel::class.java)
                    val playerName = item?.playerName ?: "Un jugador"

                    val isConfirmed = (status == CallUpStatus.CONFIRMED)
                    val title = if (isConfirmed) "Asistencia Confirmada" else "Inasistencia Notificada"
                    val reasonText = if (reason.isNotBlank()) " Motivo: ${reason.trim()}" else ""
                    val commentText = if (comment.isNotBlank()) " ($comment)" else ""
                    val msg = if (isConfirmed) {
                        "$playerName ha CONFIRMADO su asistencia para el partido vs ${m.rival}."
                    } else {
                        "$playerName ha RECHAZADO la convocatoria para el partido vs ${m.rival}.$reasonText$commentText"
                    }

                    dispatchMatchNotifications(
                        teamId = m.teamId,
                        recipientUids = listOf(m.dtUid),
                        title = title,
                        message = msg,
                        matchId = matchId,
                        callUpId = ref.id
                    )
                }
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reenvía un recordatorio a los pendientes (US-17).
     */
    suspend fun resendCallUpReminders(
        match: MatchModel,
        pendingItems: List<CallUpItemModel>
    ): Result<Unit> {
        return try {
            val recipients = mutableListOf<String>()
            pendingItems.forEach { item ->
                if (item.userUid.isNotBlank()) recipients.add(item.userUid)
                if (item.parentUid.isNotBlank()) recipients.add(item.parentUid)
            }

            dispatchMatchNotifications(
                teamId = match.teamId,
                recipientUids = recipients.distinct(),
                title = "Recordatorio de Convocatoria",
                message = "Tienes una convocatoria pendiente para el partido vs ${match.rival}. Por favor confirma antes de la fecha límite.",
                matchId = match.id
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa la lista de convocatorias de un partido en tiempo real (US-17).
     */
    fun observeCallUpsByMatch(matchId: String): Flow<List<CallUpItemModel>> = callbackFlow {
        val registration = matches.document(matchId).collection("callups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(CallUpItemModel::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Guarda la alineación de un partido (US-18).
     */
    suspend fun saveLineup(matchId: String, lineup: LineupModel): Result<Unit> = try {
        val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
        val matchRef = matches.document(matchId)
        val teamId = lineup.teamId.ifBlank { "starting" }
        val collection = matchRef.collection("lineup")
        ids.transact { tx ->
            val match = tx.get(matchRef)
            check(match.exists()) { "El partido ya no está disponible" }
            val existing = collection.document(match.getString("lineupIds.$teamId") ?: teamId)
            val previous = tx.get(existing)
            val playerIds = lineup.positions.mapNotNull { it.playerId } + lineup.substitutePlayerIds
            check(playerIds.distinct().size == playerIds.size) { "La alineación contiene jugadores repetidos" }
            val actualTeam = lineup.teamId.ifBlank { match.getString("teamId").orEmpty() }
            playerIds.forEach { playerId ->
                val player = tx.get(players.document(playerId))
                check(player.exists() && player.getString("teamId") == actualTeam) {
                    "Hay un jugador que no pertenece al equipo"
                }
            }
            val data = lineup.copy(matchId = matchId).firestoreFields() +
                ("savedAt" to FieldValue.serverTimestamp())
            if (previous.exists()) {
                val audit = ids.prepareAudit(tx, uid, existing, "UPDATE",
                    ids.changedFields(previous, data).keys.sorted())
                tx.update(existing, data + mapOf(
                    "updatedAt" to FieldValue.serverTimestamp(), "updatedByUid" to uid
                ))
                tx.update(matchRef, "hasLineup", true)
                audit()
            } else {
                val (allocation, id) = ids.allocate(tx, collection, SportProEntity.LINEUP)
                val audit = ids.prepareAudit(tx, uid, allocation.reference, "CREATE", emptyList())
                allocation.commit(tx, data + ids.creationMetadata(uid))
                tx.update(matchRef, mapOf("lineupIds.$teamId" to id, "lastLineupTeam" to teamId, "hasLineup" to true))
                audit()
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLineup(matchId: String, teamId: String): Result<LineupModel?> = try {
        val ref = matches.document(matchId)
        val id = ref.get().await().getString("lineupIds.$teamId") ?: teamId
        val snapshot = ref.collection("lineup").document(id).get().await()
        Result.success(snapshot.toObject(LineupModel::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeLineup(matchId: String, teamId: String = "starting"): Flow<LineupModel?> {
        val ref = matches.document(matchId)
        return ref.observeLinkedDocument("lineupIds.$teamId", ref.collection("lineup"), teamId, LineupModel::class.java)
    }

    /**
     * Observa en tiempo real un partido específico y retorna el modelo junto con el indicador de caché sin conexión (US-21).
     */
    fun observeMatch(matchId: String): Flow<Pair<MatchModel, Boolean>> = callbackFlow {
        val registration = matches.document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val match = snapshot.toObject(MatchModel::class.java)
                    val isFromCache = snapshot.metadata.isFromCache
                    if (match != null) {
                        trySend(Pair(match.copy(id = snapshot.id), isFromCache))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    // ==========================================
    // REGISTRO DE EVENTOS EN TIEMPO REAL (US-19)
    // ==========================================

    /**
     * Observa en tiempo real todos los eventos registrados en el partido (US-19 / US-21).
     */
    fun observeMatchEvents(matchId: String): Flow<Pair<List<pe.edu.esan.sportpro.data.model.MatchEventModel>, Boolean>> = callbackFlow {
        val registration = matches.document(matchId).collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val isFromCache = snapshot?.metadata?.isFromCache ?: false
                val hasPendingWrites = snapshot?.metadata?.hasPendingWrites() ?: false
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val event = doc.toObject(pe.edu.esan.sportpro.data.model.MatchEventModel::class.java)
                    event?.copy(
                        id = doc.id,
                        pendingSync = hasPendingWrites || doc.metadata.hasPendingWrites()
                    )
                }.sortedBy { it.minute }
                trySend(Pair(list, isFromCache))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Agrega un nuevo evento al partido, recalcula marcador y envía notificación push interna de gol/roja (US-19 / US-21).
     */
    suspend fun addMatchEvent(event: pe.edu.esan.sportpro.data.model.MatchEventModel): Result<Unit> {
        return try {
            val entity = when (event.type) {
                "GOL" -> SportProEntity.GOAL
                "TARJETA_AMARILLA", "TARJETA_ROJA" -> SportProEntity.CARD
                "CAMBIO" -> SportProEntity.SUBSTITUTION
                else -> SportProEntity.EVENT
            }
            val matchRef = matches.document(event.matchId)
            ids.create(matchRef.collection("events"), entity, event.firestoreFields() +
                ("registeredAt" to FieldValue.serverTimestamp())) { tx, _ ->
                check(tx.get(matchRef).exists()) { "El partido no existe" }
                listOfNotNull(event.playerId, event.jugadorSaleId, event.jugadorEntraId)
                    .filter { it.isNotBlank() }.distinct().forEach { playerId ->
                        check(tx.get(players.document(playerId)).exists()) { "El jugador no existe" }
                    }
                ({})
            }

            // Recalcular marcador si el evento es un GOL activo
            recalculateMatchScore(event.matchId)

            // Disparar notificaciones en tiempo real para Gol y Tarjeta Roja (US-21)
            try {
                if (event.type == pe.edu.esan.sportpro.data.model.EventType.GOAL || event.type == pe.edu.esan.sportpro.data.model.EventType.RED_CARD) {
                    val matchDoc = matches.document(event.matchId).get().await()
                    val m = matchDoc.toObject(MatchModel::class.java)
                    if (m != null) {
                        val recipients = m.memberUids.filter { it != event.registeredBy }
                        if (recipients.isNotEmpty()) {
                            val notifTitle = if (event.type == pe.edu.esan.sportpro.data.model.EventType.GOAL) "⚽ ¡GOOOOL DE ${event.playerName}!" else "🟥 Tarjeta Roja en Vivo"
                            val notifMsg = if (event.type == pe.edu.esan.sportpro.data.model.EventType.GOAL) {
                                "${event.playerName} anotó en el minuto ${event.minute}' vs ${m.rival}."
                            } else {
                                "${event.playerName} fue expulsado en el minuto ${event.minute}' vs ${m.rival}."
                            }
                            dispatchMatchNotifications(m.teamId, recipients, notifTitle, notifMsg, m.id)
                        }
                    }
                }
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recalcula el marcador oficial del partido leyendo los eventos activos del partido.
     */
    suspend fun recalculateMatchScore(matchId: String): Result<Unit> {
        return try {
            val eventsSnap = matches.document(matchId).collection("events")
                .whereEqualTo("status", pe.edu.esan.sportpro.data.model.EventStatus.ACTIVE)
                .whereEqualTo("type", pe.edu.esan.sportpro.data.model.EventType.GOAL)
                .get()
                .await()

            val goals = eventsSnap.toObjects(pe.edu.esan.sportpro.data.model.MatchEventModel::class.java)
            val ownGoals = goals.count { it.team == pe.edu.esan.sportpro.data.model.EventTeam.OWN }
            val rivalGoals = goals.count { it.team == pe.edu.esan.sportpro.data.model.EventTeam.RIVAL }

            matches.document(matchId).update(
                "score", mapOf("own" to ownGoals, "rival" to rivalGoals)
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia el partido, cambia el estado a EN_CURSO y arranca el cronómetro oficial (US-19).
     */
    suspend fun startMatchTimer(matchId: String, user: UserModel): Result<Unit> {
        return try {
            val now = com.google.firebase.Timestamp.now()
            matches.document(matchId).update(
                mapOf(
                    "status" to MatchStatus.IN_PROGRESS,
                    "currentPeriod" to 1,
                    "timerIsRunning" to true,
                    "timerStartTime" to now,
                    "timerAccumulatedSeconds" to 0L,
                    "timerPauseTime" to null
                )
            ).await()

            addMatchEvent(
                pe.edu.esan.sportpro.data.model.MatchEventModel(
                    matchId = matchId,
                    type = pe.edu.esan.sportpro.data.model.EventType.PERIOD_START,
                    minute = 1,
                    observation = "Inicio del 1° Tiempo",
                    registeredBy = user.uid,
                    registeredByName = user.fullName
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pausa el cronómetro del partido guardando los segundos acumulados (US-19).
     */
    suspend fun pauseMatchTimer(matchId: String, accumulatedSeconds: Long): Result<Unit> {
        return try {
            val now = com.google.firebase.Timestamp.now()
            matches.document(matchId).update(
                mapOf(
                    "timerIsRunning" to false,
                    "timerAccumulatedSeconds" to accumulatedSeconds,
                    "timerPauseTime" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reanuda el cronómetro del partido desde la marca pausada (US-19).
     */
    suspend fun resumeMatchTimer(matchId: String): Result<Unit> {
        return try {
            val now = com.google.firebase.Timestamp.now()
            matches.document(matchId).update(
                mapOf(
                    "timerIsRunning" to true,
                    "timerStartTime" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finaliza el primer tiempo (US-19).
     */
    suspend fun endFirstHalf(matchId: String, accumulatedSeconds: Long, minute: Int, user: UserModel): Result<Unit> {
        return try {
            matches.document(matchId).update(
                mapOf(
                    "timerIsRunning" to false,
                    "timerAccumulatedSeconds" to accumulatedSeconds
                )
            ).await()

            addMatchEvent(
                pe.edu.esan.sportpro.data.model.MatchEventModel(
                    matchId = matchId,
                    type = pe.edu.esan.sportpro.data.model.EventType.PERIOD_END,
                    minute = minute,
                    observation = "Fin del 1° Tiempo",
                    registeredBy = user.uid,
                    registeredByName = user.fullName
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia el segundo tiempo del partido (US-19).
     */
    suspend fun startSecondHalf(matchId: String, periodMinutes: Int, user: UserModel): Result<Unit> {
        return try {
            val now = com.google.firebase.Timestamp.now()
            val startAccumulated = (periodMinutes * 60).toLong()
            matches.document(matchId).update(
                mapOf(
                    "currentPeriod" to 2,
                    "timerIsRunning" to true,
                    "timerStartTime" to now,
                    "timerAccumulatedSeconds" to startAccumulated
                )
            ).await()

            addMatchEvent(
                pe.edu.esan.sportpro.data.model.MatchEventModel(
                    matchId = matchId,
                    type = pe.edu.esan.sportpro.data.model.EventType.PERIOD_START,
                    minute = periodMinutes + 1,
                    observation = "Inicio del 2° Tiempo",
                    registeredBy = user.uid,
                    registeredByName = user.fullName
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia el Tiempo Extra del partido en caso de empate (US-19).
     */
    suspend fun startExtraTime(matchId: String, periodMinutes: Int, user: UserModel): Result<Unit> {
        return try {
            val now = com.google.firebase.Timestamp.now()
            val startAccumulated = (periodMinutes * 2 * 60).toLong()
            matches.document(matchId).update(
                mapOf(
                    "currentPeriod" to 4,
                    "timerIsRunning" to true,
                    "timerStartTime" to now,
                    "timerAccumulatedSeconds" to startAccumulated
                )
            ).await()

            addMatchEvent(
                pe.edu.esan.sportpro.data.model.MatchEventModel(
                    matchId = matchId,
                    type = pe.edu.esan.sportpro.data.model.EventType.PERIOD_START,
                    minute = (periodMinutes * 2) + 1,
                    observation = "Inicio del Tiempo Extra",
                    registeredBy = user.uid,
                    registeredByName = user.fullName
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finaliza el partido y consolida los resultados oficiales (US-19).
     */
    suspend fun finishMatch(matchId: String, accumulatedSeconds: Long, minute: Int, user: UserModel): Result<Unit> {
        return try {
            matches.document(matchId).update(
                mapOf(
                    "status" to MatchStatus.FINISHED,
                    "currentPeriod" to 3,
                    "timerIsRunning" to false,
                    "timerAccumulatedSeconds" to accumulatedSeconds
                )
            ).await()

            addMatchEvent(
                pe.edu.esan.sportpro.data.model.MatchEventModel(
                    matchId = matchId,
                    type = pe.edu.esan.sportpro.data.model.EventType.PERIOD_END,
                    minute = minute,
                    observation = "Final del Partido",
                    registeredBy = user.uid,
                    registeredByName = user.fullName
                )
            )

            // Recalcular marcador final
            recalculateMatchScore(matchId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================
    // EDICIÓN, ANULACIÓN Y TRAZABILIDAD DE EVENTOS (US-20)
    // ================================================================

    /**
     * Verifica si el partido finalizado aún se encuentra dentro de la ventana de 24 horas para edición (US-20).
     */
    fun isWithin24HoursOfFinish(match: MatchModel): Boolean {
        if (match.status != MatchStatus.FINISHED) return true
        val createdAt = match.createdAt ?: return true
        val finishSeconds = createdAt.seconds
        val nowSeconds = System.currentTimeMillis() / 1000L
        val hoursElapsed = (nowSeconds - finishSeconds) / 3600L
        return hoursElapsed < 24L
    }

    /**
     * Edita un evento de partido registrando la trazabilidad en la bitácora (batch atómico) (US-20).
     */
    suspend fun editMatchEvent(
        event: pe.edu.esan.sportpro.data.model.MatchEventModel,
        motivo: String,
        user: UserModel
    ): Result<Unit> {
        return try {
            if (motivo.isBlank()) throw Exception("El motivo de la modificación es obligatorio.")

            val eventRef = matches.document(event.matchId).collection("events").document(event.id)
            saveEventChange(eventRef, event.matchId, user, motivo, canceled = false,
                event.copy(minutoCorregido = true).firestoreFields() - setOf("registeredAt", "registeredBy", "matchId", "status"))

            // Recalcular marcador y estadísticas
            recalculateMatchScore(event.matchId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Anula un evento de partido (sin eliminarlo) registrando el motivo en la bitácora (US-20).
     */
    suspend fun cancelMatchEvent(
        matchId: String,
        eventId: String,
        motivo: String,
        user: UserModel
    ): Result<Unit> {
        return try {
            if (motivo.isBlank()) throw Exception("El motivo de la anulación es obligatorio.")

            val eventRef = matches.document(matchId).collection("events").document(eventId)
            saveEventChange(eventRef, matchId, user, motivo, canceled = true,
                mapOf("status" to pe.edu.esan.sportpro.data.model.EventStatus.CANCELED))

            // Recalcular marcador y estadísticas
            recalculateMatchScore(matchId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveEventChange(
        ref: com.google.firebase.firestore.DocumentReference,
        matchId: String,
        user: UserModel,
        reason: String,
        canceled: Boolean,
        updates: Map<String, Any?>
    ) {
        ids.transact { tx ->
            val snapshot = tx.get(ref)
            val old = snapshot.toObject(pe.edu.esan.sportpro.data.model.MatchEventModel::class.java)
                ?: error("El evento no existe")
            val fields = ids.changedFields(snapshot, updates)
            val (history, _) = ids.allocate(tx, ref.collection("history"), SportProEntity.EVENT_HISTORY)
            val audit = ids.prepareAudit(tx, user.uid, ref,
                if (canceled) "DEACTIVATE" else "UPDATE", fields.keys.sorted())
            val previous = "Min: ${old.minute}', Tipo: ${old.type}, Jugador: ${old.playerName}, Obs: ${old.observation}"
            val next = if (canceled) "Estado: ANULADO" else
                "Min: ${updates["minute"]}', Tipo: ${updates["type"]}, Jugador: ${updates["playerName"]}, Obs: ${updates["observation"]}"
            tx.update(ref, fields + mapOf(
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedByUid" to user.uid
            ))
            history.commit(tx, mapOf(
                "eventId" to ref.id,
                "matchId" to matchId,
                "tipoCambio" to if (canceled) "ANULACION" else "EDICION",
                "usuarioUid" to user.uid,
                "usuarioNombre" to user.fullName,
                "motivo" to reason.trim(),
                "valorOriginal" to previous,
                "valorNuevo" to next,
                "fechaHora" to FieldValue.serverTimestamp()
            ) + ids.creationMetadata(user.uid))
            audit()
        }
    }

    /**
     * Elimina permanentemente un evento previamente anulado del partido (US-20).
     */
    suspend fun deleteMatchEvent(
        matchId: String,
        eventId: String,
        user: UserModel
    ): Result<Unit> {
        return try {
            val eventRef = matches.document(matchId).collection("events").document(eventId)
            eventRef.delete().await()

            // Recalcular marcador y estadísticas
            recalculateMatchScore(matchId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa la bitácora de auditoría de un evento en tiempo real (US-20).
     */
    fun observeEventHistory(matchId: String, eventId: String): Flow<List<pe.edu.esan.sportpro.data.model.EventAuditModel>> = callbackFlow {
        val registration = matches.document(matchId)
            .collection("events")
            .document(eventId)
            .collection("history")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(pe.edu.esan.sportpro.data.model.EventAuditModel::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    private const val CANCELLATION_REQUESTS = "eventCancellationRequests"
    private val cancellationRequestsRef get() = firestore.collection(CANCELLATION_REQUESTS)

    /**
     * Envía una solicitud de anulación de sanción/evento al Administrador (US-20).
     */
    suspend fun requestEventCancellationToAdmin(
        request: pe.edu.esan.sportpro.data.model.EventCancellationRequestModel
    ): Result<Unit> {
        return try {
            if (request.justification.isBlank()) throw Exception("Debes ingresar el motivo de la justificación para la solicitud.")

            ids.create(cancellationRequestsRef, SportProEntity.EVENT_CANCELLATION, request.firestoreFields() - "resolvedAt")

            // Notificar a administradores
            val adminDocs = firestore.collection("users").whereEqualTo("role", "ADM").get().await()
            val adminUids = adminDocs.documents.map { it.id }
            dispatchMatchNotifications(
                teamId = "",
                recipientUids = adminUids,
                title = "Solicitud de Anulación de Evento",
                message = "${request.requestingDtName} (${request.requestingTeamName}) solicitó anular un evento en ${request.matchTitle}. Motivo: ${request.justification}",
                matchId = request.matchId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa solicitudes de anulación pendientes para el Administrador (US-20).
     */
    fun observePendingCancellationRequests(): Flow<List<pe.edu.esan.sportpro.data.model.EventCancellationRequestModel>> = callbackFlow {
        val registration = cancellationRequestsRef
            .whereEqualTo("status", pe.edu.esan.sportpro.data.model.RequestStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(pe.edu.esan.sportpro.data.model.EventCancellationRequestModel::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Resuelve una solicitud de anulación por parte del Administrador (US-20).
     */
    suspend fun resolveCancellationRequest(
        requestId: String,
        approve: Boolean,
        adminComment: String,
        adminUser: UserModel
    ): Result<Unit> {
        return try {
            val reqDoc = cancellationRequestsRef.document(requestId).get().await()
            val req = reqDoc.toObject(pe.edu.esan.sportpro.data.model.EventCancellationRequestModel::class.java)
                ?: throw Exception("Solicitud no encontrada")

            if (approve) {
                // Anular el evento registrando la bitácora
                cancelMatchEvent(
                    matchId = req.matchId,
                    eventId = req.eventId,
                    motivo = "Aprobado por ADM (${adminUser.fullName}). Justificación: ${req.justification}",
                    user = adminUser
                )

                cancellationRequestsRef.document(requestId).update(
                    mapOf(
                        "status" to pe.edu.esan.sportpro.data.model.RequestStatus.APPROVED,
                        "adminResponseComment" to adminComment.trim(),
                        "resolvedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                dispatchMatchNotifications(
                    teamId = "",
                    recipientUids = listOf(req.requestingDtUid),
                    title = "Solicitud Aceptada",
                    message = "El Administrador ha ACEPTADO la anulación del evento (${req.eventSummary}) en ${req.matchTitle}.",
                    matchId = req.matchId
                )
            } else {
                cancellationRequestsRef.document(requestId).update(
                    mapOf(
                        "status" to pe.edu.esan.sportpro.data.model.RequestStatus.REJECTED,
                        "adminResponseComment" to adminComment.trim(),
                        "resolvedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                dispatchMatchNotifications(
                    teamId = "",
                    recipientUids = listOf(req.requestingDtUid),
                    title = "Solicitud Rechazada",
                    message = "El Administrador ha RECHAZADO la solicitud de anulación del evento (${req.eventSummary}). Comentario: ${adminComment.ifBlank { "Sin comentario" }}",
                    matchId = req.matchId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun dispatchMatchNotifications(
        teamId: String,
        recipientUids: List<String>,
        title: String,
        message: String,
        matchId: String,
        callUpId: String? = null
    ) {
        try {
            recipientUids.forEach { uid ->
                val notif = hashMapOf(
                    "recipientUid" to uid,
                    "teamId" to teamId,
                    "title" to title,
                    "message" to message,
                    "type" to "MATCH",
                    "relatedId" to matchId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (callUpId != null) notif["callUpId"] = callUpId
                notifications.notifyBestEffort(notif)
            }
        } catch (_: Exception) {}
    }
}
