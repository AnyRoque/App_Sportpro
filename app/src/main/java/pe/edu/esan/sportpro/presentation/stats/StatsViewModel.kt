package pe.edu.esan.sportpro.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.PlayerStatsModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.TeamStatsModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.StatsRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository

data class StatsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val teams: List<TeamModel> = emptyList(),
    val selectedTeam: TeamModel? = null,
    val teamStats: TeamStatsModel? = null,
    val playerStatsList: List<PlayerStatsModel> = emptyList(),
    val selectedTab: Int = 0, // 0: Jugadores, 1: Equipo
    val searchQuery: String = ""
)

/**
 * US-15: ViewModel para la pantalla de Consulta de Estadísticas Acumuladas.
 */
class StatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    fun loadForUser(user: UserModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            try {
                val teamsList = mutableListOf<TeamModel>()

                when (user.roleEnum) {
                    Role.DT -> {
                        val allApproved = TeamRepository.getApprovedTeams().getOrDefault(emptyList())
                        val dtApproved = allApproved.filter { it.dtUid == user.uid }
                        teamsList.addAll(if (dtApproved.isNotEmpty()) dtApproved else allApproved)
                    }
                    Role.JUG, Role.PAD -> {
                        val userPlayersSnap = if (user.roleEnum == Role.JUG) {
                            PlayerRepository.observePlayersByUser(user.uid)
                        } else {
                            PlayerRepository.observeChildrenByParent(user.uid)
                        }

                        userPlayersSnap.collect { players ->
                            val teamIds = players.map { it.teamId }.filter { it.isNotBlank() }.distinct()
                            for (tid in teamIds) {
                                val t = TeamRepository.getTeam(tid).getOrNull()
                                if (t != null && t.status == TeamStatus.APPROVED && teamsList.none { it.id == t.id }) {
                                    teamsList.add(t)
                                }
                            }
                            if (teamsList.isEmpty()) {
                                val allApproved = TeamRepository.getApprovedTeams().getOrDefault(emptyList())
                                teamsList.addAll(allApproved)
                            }
                            applyLoadedTeams(teamsList)
                            return@collect
                        }
                    }
                    else -> {
                        val allApproved = TeamRepository.getApprovedTeams().getOrDefault(emptyList())
                        teamsList.addAll(allApproved)
                    }
                }

                applyLoadedTeams(teamsList)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "Error al cargar los datos de estadísticas."
                )
            }
        }
    }

    private fun applyLoadedTeams(teamsList: List<TeamModel>) {
        val selected = teamsList.firstOrNull()
        _uiState.value = _uiState.value.copy(
            teams = teamsList,
            selectedTeam = selected,
            loading = selected == null
        )

        if (selected != null) {
            loadStatsForTeam(selected.id)
        } else {
            _uiState.value = _uiState.value.copy(
                loading = false,
                errorMessage = if (teamsList.isEmpty()) "No cuentas con ningún equipo registrado para consultar estadísticas." else null
            )
        }
    }

    fun selectTeam(team: TeamModel) {
        if (_uiState.value.selectedTeam?.id == team.id) return
        _uiState.value = _uiState.value.copy(selectedTeam = team, loading = true, errorMessage = null)
        loadStatsForTeam(team.id)
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun reload() {
        _uiState.value.selectedTeam?.let { loadStatsForTeam(it.id) }
    }

    private fun loadStatsForTeam(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val result = StatsRepository.getAccumulatedStats(teamId)
            result.fold(
                onSuccess = { (teamStats, playerStats) ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        teamStats = teamStats,
                        playerStatsList = playerStats
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = e.message ?: "Error al calcular las estadísticas del equipo."
                    )
                }
            )
        }
    }
}
