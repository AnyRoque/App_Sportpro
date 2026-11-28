package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TacticalLayouts
import pe.edu.esan.sportpro.data.model.TacticalPosition
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository

/** Estado táctico aislado por equipo dentro de un partido. */
class LineupViewModel : ViewModel() {
    private val _match = MutableStateFlow<MatchModel?>(null)
    val match: StateFlow<MatchModel?> = _match.asStateFlow()
    private val _confirmedCallUps = MutableStateFlow<List<CallUpItemModel>>(emptyList())
    private val allCallUps = MutableStateFlow<List<CallUpItemModel>>(emptyList())
    private var callUpsJob: Job? = null
    val confirmedCallUps: StateFlow<List<CallUpItemModel>> = _confirmedCallUps.asStateFlow()
    private val _positions = MutableStateFlow<List<TacticalPosition>>(emptyList())
    val positions: StateFlow<List<TacticalPosition>> = _positions.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isEditable = MutableStateFlow(false)
    val isEditable: StateFlow<Boolean> = _isEditable.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val validationError = MutableStateFlow<String?>(null)
    val formation = MutableStateFlow("4-3-3")
    private val _localTeam = MutableStateFlow<TeamModel?>(null)
    val localTeam: StateFlow<TeamModel?> = _localTeam.asStateFlow()
    private val _visitorTeam = MutableStateFlow<TeamModel?>(null)
    val visitorTeam: StateFlow<TeamModel?> = _visitorTeam.asStateFlow()
    private val _activeTeamId = MutableStateFlow("")
    val activeTeamId: StateFlow<String> = _activeTeamId.asStateFlow()
    private val _activeTeamName = MutableStateFlow("")
    val activeTeamName: StateFlow<String> = _activeTeamName.asStateFlow()
    private val _activeTeamRoster = MutableStateFlow<List<PlayerModel>>(emptyList())
    val activeTeamRoster: StateFlow<List<PlayerModel>> = _activeTeamRoster.asStateFlow()
    private var currentUser: UserModel? = null
    private var playerOwnerTeamById: Map<String, String> = emptyMap()
    private var savedByTeam = mutableMapOf<String, LineupModel>()
    private var lineupByTeam = mutableMapOf<String, LineupModel?>()

    fun load(matchId: String, user: UserModel) {
        if (_match.value?.id == matchId && currentUser?.uid == user.uid) return
        currentUser = user
        viewModelScope.launch {
            _isLoading.value = true
            savedByTeam.clear()
            lineupByTeam.clear()
            val matchRes = MatchRepository.getMatch(matchId)
            matchRes.onSuccess { m ->
                _match.value = m
                callUpsJob?.cancel()
                callUpsJob = viewModelScope.launch {
                    MatchRepository.observeCallUpsByMatch(matchId).collect { callUps ->
                        allCallUps.value = callUps
                        updateConfirmedPlayers()
                    }
                }
                val local = TeamModel(id = m.teamId, name = m.teamName, dtUid = m.dtUid)
                _localTeam.value = local
                val rivalTeam = TeamRepository.getApprovedTeams().getOrNull()
                    ?.firstOrNull { it.name.trim().equals(m.rival.trim(), ignoreCase = true) }
                _visitorTeam.value = rivalTeam

                // Resolver convocatorias antiguas usando la plantilla aceptada de cada lado.
                val playerOwners = mutableMapOf<String, String>()
                listOfNotNull(local, rivalTeam).forEach { team ->
                    val members = try {
                        PlayerRepository.observePlayersByTeam(team.id, team.dtUid).first()
                            .filter { it.active && it.linkStatus == LinkStatus.ACCEPTED }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    members.forEach { member ->
                        playerOwners.putIfAbsent(member.id, team.id)
                        if (member.userUid.isNotBlank()) playerOwners.putIfAbsent("uid:${member.userUid}", team.id)
                        if (member.parentUid.isNotBlank()) playerOwners.putIfAbsent("parent:${member.parentUid}", team.id)
                    }
                }
                playerOwnerTeamById = playerOwners

                val initialTeam = if (rivalTeam?.dtUid == user.uid) rivalTeam else local
                selectTeamInternal(initialTeam, user, m)
            }.onFailure { _error.value = it.message ?: "No se pudo cargar el partido" }
            _isLoading.value = false
        }
    }

    fun selectTeam(team: TeamModel, user: UserModel) {
        val m = _match.value ?: return
        if (team.id.isBlank() || team.id == _activeTeamId.value) return
        viewModelScope.launch {
            _isLoading.value = true
            selectTeamInternal(team, user, m)
            _isLoading.value = false
        }
    }

    private suspend fun selectTeamInternal(team: TeamModel, user: UserModel, match: MatchModel) {
        _activeTeamId.value = team.id
        _activeTeamName.value = team.name
        _isEditable.value = user.roleEnum == Role.DT &&
            team.dtUid == user.uid && match.status == MatchStatus.PROGRAMMED
        try {
            val players = PlayerRepository.observePlayersByTeam(team.id, team.dtUid).first()
                .filter { it.active && it.linkStatus == LinkStatus.ACCEPTED }
            _activeTeamRoster.value = players
            allCallUps.value = MatchRepository.observeCallUpsByMatch(match.id).first()
            updateConfirmedPlayers()
            val saved = MatchRepository.getLineup(match.id, team.id).getOrNull()
                ?: if (team.id == match.teamId) MatchRepository.getLineup(match.id, "starting").getOrNull() else null
            lineupByTeam[team.id] = saved

            updateConfirmedPlayers()
            val confirmedIds = _confirmedCallUps.value.map { it.playerId }.toSet()
            val defaultForm = if (match.modality == 7) "2-3-1" else if (match.modality == 9) "3-3-2" else "4-3-3"
            val activeForm = saved?.formation?.takeIf { it.isNotBlank() } ?: defaultForm
            formation.value = activeForm
            val layout = TacticalLayouts.generateDefaultPositions(activeForm, match.modality)
            val savedMap = saved?.positions.orEmpty().associateBy { it.slotIndex }
            val restoredNames = mutableSetOf<String>()
            _positions.value = layout.map { position ->
                val old = savedMap[position.slotIndex]
                val normalizedName = old?.playerName?.trim()?.lowercase().orEmpty()
                if (old != null && old.playerId in confirmedIds && normalizedName.isNotBlank() && restoredNames.add(normalizedName)) {
                    position.copy(playerId = old.playerId, playerName = old.playerName, shirtNumber = old.shirtNumber)
                } else position
            }
            validationError.value = null
            _error.value = null
        } catch (e: Exception) {
            _activeTeamRoster.value = emptyList()
            _confirmedCallUps.value = emptyList()
            _error.value = "No se pudo cargar la plantilla de ${team.name}: ${e.message}"
        }
    }

    private fun updateConfirmedPlayers() {
        val roster = _activeTeamRoster.value

        fun playerId(item: CallUpItemModel): String = item.playerId.ifBlank { item.id }
        fun identity(item: CallUpItemModel): String = when {
            item.userUid.isNotBlank() -> "uid:${item.userUid}"
            item.parentUid.isNotBlank() -> "parent:${item.parentUid}"
            else -> "player:${playerId(item)}"
        }
        fun ownerTeam(item: CallUpItemModel): String {
            val id = playerId(item)
            return playerOwnerTeamById[id]
                ?: item.userUid.takeIf(String::isNotBlank)?.let { playerOwnerTeamById["uid:$it"] }
                ?: item.parentUid.takeIf(String::isNotBlank)?.let { playerOwnerTeamById["parent:$it"] }
                ?: item.teamId.ifBlank { _match.value?.teamId.orEmpty() }
        }

        val firstTeamByPlayer = mutableMapOf<String, String>()
        allCallUps.value
            .sortedWith(compareBy<CallUpItemModel> { it.createdAt?.seconds ?: Long.MAX_VALUE }.thenBy { it.teamId })
            .forEach { callUp ->
                val owner = ownerTeam(callUp)
                if (owner.isNotBlank()) firstTeamByPlayer.putIfAbsent(identity(callUp), owner)
            }

        _confirmedCallUps.value = allCallUps.value.mapNotNull { callUp ->
            if (callUp.responseStatus != CallUpStatus.CONFIRMED) return@mapNotNull null
            val resolvedOwner = ownerTeam(callUp)
            val playerKey = identity(callUp)
            if (resolvedOwner != _activeTeamId.value || firstTeamByPlayer[playerKey] != resolvedOwner) return@mapNotNull null

            // La convocatoria confirmada habilita al jugador; el roster aporta su ID y datos canónicos.
            val member = roster.firstOrNull { player ->
                player.id == playerId(callUp) ||
                    (callUp.userUid.isNotBlank() && (player.userUid == callUp.userUid || player.parentUid == callUp.userUid)) ||
                    (callUp.parentUid.isNotBlank() && (player.parentUid == callUp.parentUid || player.userUid == callUp.parentUid))
            }
            if (member == null) callUp.copy(playerId = playerId(callUp)) else callUp.copy(
                playerId = member.id,
                playerName = member.fullName,
                playerPhotoUrl = member.photoUrl,
                position = member.position
            )
        }.distinctBy { identity(it) }
            .distinctBy { playerId(it) }

        val confirmedIds = _confirmedCallUps.value.map { it.playerId }.toSet()
        val seenNames = mutableSetOf<String>()
        _positions.value = _positions.value.map { position ->
            val name = position.playerName?.trim()?.lowercase().orEmpty()
            if (!position.playerId.isNullOrBlank() &&
                (position.playerId !in confirmedIds || name.isBlank() || !seenNames.add(name))) {
                position.copy(playerId = null, playerName = null, shirtNumber = null)
            } else position
        }
    }

    fun changeFormation(newFormation: String) {
        val m = _match.value ?: return
        formation.value = newFormation
        val old = _positions.value.associateBy { it.slotIndex }
        _positions.value = TacticalLayouts.generateDefaultPositions(newFormation, m.modality).map { position ->
            old[position.slotIndex]?.takeIf { !it.playerId.isNullOrBlank() }?.let {
                position.copy(playerId = it.playerId, playerName = it.playerName, shirtNumber = it.shirtNumber)
            } ?: position
        }
        validationError.value = null
    }

    fun assignPlayerToPosition(slotIndex: Int, player: CallUpItemModel) {
        validationError.value = null
        val playerNameKey = player.playerName.trim().lowercase()
        val existing = _positions.value.firstOrNull { position ->
            position.slotIndex != slotIndex &&
                (position.playerId == player.playerId || position.playerName?.trim()?.lowercase() == playerNameKey)
        }
        if (existing != null) {
            validationError.value = "Un jugador no puede ocupar dos posiciones y el nombre no puede repetirse"
            return
        }
        _positions.value = _positions.value.map { pos ->
            if (pos.slotIndex == slotIndex) pos.copy(
                playerId = player.playerId, playerName = player.playerName, shirtNumber = null
            ) else pos
        }
    }

    fun unassignPlayerFromPosition(slotIndex: Int) {
        _positions.value = _positions.value.map { pos ->
            if (pos.slotIndex == slotIndex) pos.copy(playerId = null, playerName = null, shirtNumber = null) else pos
        }
        validationError.value = null
    }

    fun saveLineup(user: UserModel, onDone: () -> Unit) {
        validationError.value = null
        val m = _match.value ?: return
        val teamId = _activeTeamId.value
        if (!_isEditable.value || teamId.isBlank()) {
            validationError.value = "Solo el DT de este equipo puede guardar su alineación"
            return
        }
        val assigned = _positions.value.mapNotNull { it.playerId }
        if (assigned.size < m.modality) {
            validationError.value = "Faltan ${m.modality - assigned.size} titulares para completar la alineación"
            return
        }
        if (assigned.toSet().size != assigned.size) {
            validationError.value = "Un jugador no puede ocupar dos posiciones"
            return
        }
        val assignedNames = _positions.value.mapNotNull { it.playerName?.trim()?.lowercase()?.takeIf(String::isNotBlank) }
        if (assignedNames.toSet().size != assignedNames.size) {
            validationError.value = "El nombre de un jugador no puede repetirse en el equipo"
            return
        }
        val allowedIds = _confirmedCallUps.value.map { it.playerId }.toSet()
        if (assigned.any { it !in allowedIds }) {
            validationError.value = "La alineación contiene jugadores de otro equipo"
            return
        }
        val goalkeeper = _positions.value.any {
            !it.playerId.isNullOrBlank() && (it.positionName.contains("Portero", true) || it.slotIndex == 0)
        }
        if (!goalkeeper) {
            validationError.value = "Debe asignar un portero"
            return
        }
        val substitutes = allowedIds - assigned.toSet()
        viewModelScope.launch {
            _isLoading.value = true
            val lineup = LineupModel(
                id = teamId,
                matchId = m.id,
                teamId = teamId,
                teamName = _activeTeamName.value,
                formation = formation.value,
                modality = m.modality,
                positions = _positions.value,
                substitutePlayerIds = substitutes.toList(),
                savedBy = user.uid
            )
            MatchRepository.saveLineup(m.id, lineup).onSuccess {
                lineupByTeam[teamId] = lineup
                onDone()
            }.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null; validationError.value = null }
}
