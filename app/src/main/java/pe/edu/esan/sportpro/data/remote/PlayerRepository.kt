package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.util.Validators

/** Jugadores: perfil gestionado por el DT (US-04) y registro de hijos por el PAD (US-02). */
object PlayerRepository {

    private const val PLAYERS = "players"
    /** dniIndex/{dni} -> { playerId }  garantiza DNI único sin exponer datos de otros jugadores */
    private const val DNI_INDEX = "dniIndex"

    private val firestore = FirebaseFirestore.getInstance()
    private val ids = SportProIdGenerator(firestore)
    private val players get() = firestore.collection(PLAYERS)

    private val privateFields = setOf(
        "dni", "birthDate", "phone", "email", "heightCm", "weightKg", "emergencyName",
        "emergencyRelation", "emergencyPhone", "parentRelation"
    )

    private fun DocumentReference.contactRef() = collection("private").document("contact")

    private suspend fun findExistingRosterEntry(player: PlayerModel): PlayerModel? {
        val user = FirebaseAuthManager.getUserProfile().getOrThrow()
        if (user.roleEnum !in setOf(Role.DT, Role.ADM)) {
            val field = if (user.roleEnum == Role.PAD) "parentUid" else "userUid"
            val name = Validators.normalize(player.fullName)
            return players.whereEqualTo(field, user.uid).get().await().toObjects(PlayerModel::class.java)
                .firstOrNull { field == "userUid" || Validators.normalize(it.fullName) == name }
        }
        val matches = linkedMapOf<String, PlayerModel>()
        suspend fun collect(query: com.google.firebase.firestore.Query) {
            query.get().await().toObjects(PlayerModel::class.java).forEach { matches[it.id] = it }
        }
        if (player.userUid.isNotBlank()) collect(players.whereEqualTo("userUid", player.userUid))
        if (player.dni.isNotBlank()) collect(players.whereEqualTo("dni", player.dni))
        if (player.email.isNotBlank()) collect(players.whereEqualTo("email", player.email.trim().lowercase()))

        val normalizedName = Validators.normalize(player.fullName)
        if (normalizedName.isNotBlank()) {
            players.get().await().toObjects(PlayerModel::class.java)
                .firstOrNull { Validators.normalize(it.fullName) == normalizedName }
                ?.let { matches.putIfAbsent(it.id, it) }
        }
        return matches.values.firstOrNull()
    }

    /** Crea un jugador. Si tiene DNI, lo reserva en dniIndex dentro de la misma transacción. */
    suspend fun createPlayer(player: PlayerModel): Result<String> {
        return try {
            var playerToSave = player
            if (player.email.isNotBlank() && player.userUid.isBlank() && player.parentUid.isBlank()) {
                val userMatch = findRegisteredUserByEmail(player.email).getOrNull()
                if (userMatch != null) {
                    playerToSave = if (userMatch.role == "JUG") {
                        player.copy(
                            name = userMatch.name.ifBlank { player.name },
                            lastName = userMatch.lastName.ifBlank { player.lastName },
                            birthDate = player.birthDate.ifBlank { userMatch.birthDate },
                            phone = userMatch.phone.ifBlank { player.phone },
                            email = userMatch.email.ifBlank { player.email },
                            userUid = userMatch.uid,
                            linkStatus = LinkStatus.ACCEPTED
                        )
                    } else if (userMatch.role == "PAD") {
                        player.copy(
                            parentUid = userMatch.uid,
                            linkStatus = LinkStatus.ACCEPTED
                        )
                    } else player
                }
            }

            val existingEntry = findExistingRosterEntry(playerToSave)
            if (existingEntry != null) {
                val firstTeam = existingEntry.teamName.ifBlank { "otro equipo" }
                throw Exception("Este jugador ya pertenece a $firstTeam. Se conserva el primer equipo al que ingresó.")
            }

            val fields = playerToSave.firestoreFields()
            val playerRef = ids.create(players, SportProEntity.PLAYER,
                fields - privateFields + ("privateProfile" to true)) { tx, ref ->
                if (playerToSave.teamId.isNotBlank()) {
                    check(tx.get(firestore.collection("teams").document(playerToSave.teamId)).exists()) {
                        "El equipo seleccionado no existe"
                    }
                }
                val dniRef = playerToSave.dni.takeIf { it.isNotBlank() }
                    ?.let { firestore.collection(DNI_INDEX).document(it) }
                check(dniRef == null || !tx.get(dniRef).exists()) { "El DNI ya está registrado en el sistema" }
                val writeIndex: () -> Unit = {
                    if (dniRef != null) tx.set(dniRef, mapOf("playerId" to ref.id))
                    tx.set(ref.contactRef(), fields.filterKeys { it in privateFields })
                }
                writeIndex
            }
            if (playerToSave.active && playerToSave.linkStatus == LinkStatus.ACCEPTED) {
                val participants = listOf(playerToSave.userUid, playerToSave.parentUid)
                    .filter(String::isNotBlank).distinct()
                if (participants.isNotEmpty()) {
                    try {
                        addAcceptedMembersToExistingMatches(playerToSave, participants)
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        android.util.Log.w("SportPro", "No se pudieron actualizar los partidos del jugador", error)
                    }
                }
            }
            Result.success(playerRef.id)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Las excepciones dentro de la transacción llegan envueltas
            Result.failure(Exception(e.cause?.message ?: e.message))
        }
    }

    /** Actualiza un perfil existente (el DNI no se modifica). */
    suspend fun updatePlayer(player: PlayerModel): Result<Unit> {
        return try {
            val ref = players.document(player.id)
            val uid = FirebaseAuthManager.currentUid ?: error("Debes iniciar sesión")
            val fields = player.firestoreFields() - "createdAt" - "createdBy"
            ids.transact { tx ->
                val current = tx.get(ref)
                check(current.exists()) { "El jugador no existe" }
                val contact = tx.get(ref.contactRef())
                val contactChanges = ids.changedFields(contact, fields.filterKeys { it in privateFields } - "dni")
                val rosterChanges = ids.changedFields(current, fields - privateFields)
                if (contactChanges.isNotEmpty() || rosterChanges.isNotEmpty()) {
                    val auditFields = rosterChanges.keys.toMutableList()
                    val metadata = mutableMapOf<String, Any>(
                        "updatedAt" to FieldValue.serverTimestamp(), "updatedByUid" to uid
                    )
                    if (contactChanges.isNotEmpty()) {
                        auditFields.add("privateUpdatedAt")
                        metadata["privateUpdatedAt"] = FieldValue.serverTimestamp()
                    }
                    val audit = ids.prepareAudit(tx, uid, ref, "UPDATE", auditFields.sorted())
                    if (contactChanges.isNotEmpty()) tx.set(ref.contactRef(), contactChanges, SetOptions.merge())
                    tx.update(ref, rosterChanges + metadata)
                    audit()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayer(playerId: String): Result<PlayerModel> {
        return try {
            val doc = players.document(playerId).get().await()
            val roster = doc.toObject(PlayerModel::class.java) ?: throw Exception("Jugador no encontrado")
            val contact = try {
                doc.reference.contactRef().get().await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                null
            }
            val player = if (contact?.exists() == true) roster.copy(
                birthDate = contact.getString("birthDate") ?: roster.birthDate,
                dni = contact.getString("dni") ?: roster.dni,
                phone = contact.getString("phone") ?: roster.phone,
                email = contact.getString("email") ?: roster.email,
                heightCm = if (contact.contains("heightCm")) contact.getLong("heightCm")?.toInt() else roster.heightCm,
                weightKg = if (contact.contains("weightKg")) contact.getLong("weightKg")?.toInt() else roster.weightKg,
                emergencyName = contact.getString("emergencyName") ?: roster.emergencyName,
                emergencyRelation = contact.getString("emergencyRelation") ?: roster.emergencyRelation,
                emergencyPhone = contact.getString("emergencyPhone") ?: roster.emergencyPhone,
                parentRelation = contact.getString("parentRelation") ?: roster.parentRelation
            ) else roster
            val user = if (doc.getBoolean("privateProfile") == true || contact?.exists() == true) null
            else if (player.userUid.isNotBlank()) {
                try {
                    firestore.collection("users").document(player.userUid).get().await()
                        .toObject(UserModel::class.java)
                } catch (e: FirebaseFirestoreException) {
                    if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                    null
                }
            } else {
                findRegisteredUserByEmail(player.email).getOrNull()?.takeIf { it.role == Role.JUG.name }
            }
            val enriched = if (user == null) player else player.copy(
                name = player.name.ifBlank { user.name },
                lastName = player.lastName.ifBlank { user.lastName },
                birthDate = player.birthDate.ifBlank { user.birthDate },
                phone = player.phone.ifBlank { user.phone },
                email = player.email.ifBlank { user.email }
            )
            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plantel del equipo. Se filtra también por dtUid para que la consulta cumpla
     * las reglas de seguridad (solo el DT del equipo puede leer sus jugadores).
     */
    fun observePlayersByTeam(teamId: String, dtUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("teamId", teamId).whereEqualTo("dtUid", dtUid))

    fun observeChildrenByParent(parentUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("parentUid", parentUid))

    fun observePlayersByUser(userUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("userUid", userUid))

    private fun observe(query: Query): Flow<List<PlayerModel>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(PlayerModel::class.java).orEmpty().sortedBy { it.lastName })
        }
        awaitClose { registration.remove() }
    }

    suspend fun acceptLink(playerId: String): Result<Unit> {
        val res = updateField(playerId, "linkStatus", LinkStatus.ACCEPTED)
        if (res.isSuccess) {
            try {
                val player = getPlayer(playerId).getOrNull()
                if (player != null) {
                    val recipients = listOf(player.userUid, player.parentUid)
                        .filter(String::isNotBlank)
                        .distinct()
                    if (recipients.isNotEmpty()) {
                        addAcceptedMembersToExistingMatches(player, recipients)
                        val notif = hashMapOf(
                            "recipientUid" to recipients.first(),
                            "teamId" to player.teamId,
                            "title" to "Solicitud Aceptada",
                            "message" to "¡Tu solicitud para unirte al equipo ${player.teamName} ha sido ACEPTADA!",
                            "type" to "TEAM_ACCEPT",
                            "relatedId" to player.teamId,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        firestore.collection("notifications").notifyBestEffort(notif)
                    }
                }
            } catch (_: Exception) {}
        }
        return res
    }

    /**
     * Añade al jugador aceptado a partidos ya programados antes de su ingreso al plantel.
     * Los UIDs también se agregan a partidos donde su equipo figura como rival.
     */
    private suspend fun addAcceptedMembersToExistingMatches(player: PlayerModel, recipientUids: List<String>) {
        if (player.teamId.isBlank()) return
        val matchCollection = firestore.collection("matches")
        val localMatches = matchCollection.whereEqualTo("teamId", player.teamId).limit(500).get().await()
        val rivalMatches = if (player.teamName.isNotBlank()) {
            matchCollection.whereEqualTo("rival", player.teamName).limit(500).get().await()
        } else null
        val documents = (localMatches.documents + rivalMatches?.documents.orEmpty()).distinctBy { it.id }
        if (documents.isEmpty()) return

        val batch = firestore.batch()
        documents.forEach { document ->
            batch.update(
                document.reference,
                "memberUids",
                FieldValue.arrayUnion(*recipientUids.toTypedArray())
            )
        }
        batch.commit().await()
    }

    suspend fun rejectLink(playerId: String): Result<Unit> {
        val res = updateField(playerId, "linkStatus", LinkStatus.REJECTED)
        if (res.isSuccess) {
            try {
                val p = getPlayer(playerId).getOrNull()
                if (p != null) {
                    val recipient = p.userUid.ifBlank { p.parentUid }
                    if (recipient.isNotBlank()) {
                        val notif = hashMapOf(
                            "recipientUid" to recipient,
                            "teamId" to p.teamId,
                            "title" to "Solicitud Rechazada",
                            "message" to "Tu solicitud para unirte al equipo ${p.teamName} ha sido rechazada.",
                            "type" to "TEAM_REJECT",
                            "relatedId" to p.teamId,
                            "read" to false,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        firestore.collection("notifications").notifyBestEffort(notif)
                    }
                }
            } catch (_: Exception) {}
        }
        return res
    }

    /** Dar de baja / reactivar sin borrar historial. */
    suspend fun setActive(playerId: String, active: Boolean): Result<Unit> = updateField(playerId, "active", active)

    private suspend fun updateField(playerId: String, field: String, value: Any): Result<Unit> {
        return try {
            ids.update(players.document(playerId), mapOf(field to value),
                if (field == "active" && value == false) "DEACTIVATE" else "UPDATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findRegisteredUserByEmail(email: String): Result<UserModel?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) return Result.success(null)
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .get()
                .await()
            val user = snapshot.toObjects(UserModel::class.java).firstOrNull()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRegisteredUserToTeam(user: UserModel, teamId: String, teamName: String, dtUid: String): Result<String> {
        return try {
            val isJug = user.roleEnum == Role.JUG
            val isPad = user.roleEnum == Role.PAD

            val newPlayer = PlayerModel(
                teamId = teamId,
                teamName = teamName,
                dtUid = dtUid,
                name = user.name,
                lastName = user.lastName,
                birthDate = if (isJug) user.birthDate else "",
                email = user.email,
                phone = user.phone,
                userUid = if (isJug) user.uid else "",
                parentUid = if (isPad) user.uid else "",
                linkStatus = LinkStatus.ACCEPTED,
                active = true,
                createdBy = dtUid
            )
            createPlayer(newPlayer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestToJoinTeam(user: UserModel, team: TeamModel, position: String = ""): Result<String> {
        return try {
            val isJug = user.roleEnum == Role.JUG
            val isPad = user.roleEnum == Role.PAD

            val requestPlayer = PlayerModel(
                teamId = team.id,
                teamName = team.name,
                dtUid = team.dtUid,
                category = team.category,
                name = user.name,
                lastName = user.lastName,
                birthDate = if (isJug) user.birthDate else "",
                email = user.email,
                phone = user.phone,
                position = position,
                userUid = if (isJug) user.uid else "",
                parentUid = if (isPad) user.uid else "",
                linkStatus = LinkStatus.PENDING,
                active = true,
                createdBy = user.uid
            )

            val docIdResult = createPlayer(requestPlayer)
            if (docIdResult.isFailure) return docIdResult

            val notif = hashMapOf(
                "recipientUid" to team.dtUid,
                "teamId" to team.id,
                "title" to "Nueva Solicitud de Ingreso",
                "message" to "${user.fullName} ha solicitado unirse a tu equipo ${team.name}.",
                "type" to "TEAM_REQUEST",
                "relatedId" to team.id,
                "read" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("notifications").notifyBestEffort(notif)

            docIdResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
