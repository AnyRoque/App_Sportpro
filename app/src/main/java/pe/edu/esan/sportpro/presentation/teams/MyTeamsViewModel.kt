package pe.edu.esan.sportpro.presentation.teams

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.remote.CommunityImageStorage
import pe.edu.esan.sportpro.data.remote.TeamRepository

data class TeamListState(
    val loading: Boolean = true,
    val teams: List<TeamModel> = emptyList(),
    val error: String? = null,
    val savingEmblemId: String? = null,
    val failedEmblemId: String? = null
)

/** US-03: lista "Mis equipos" del DT, actualizada en tiempo real. */
class MyTeamsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TeamListState())
    val state: StateFlow<TeamListState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(dtUid: String) {
        if (job != null) return
        job = viewModelScope.launch {
            TeamRepository.observeTeamsByDt(dtUid)
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { list -> _state.update { it.copy(loading = false, teams = list) } }
        }
    }

    fun saveEmblem(context: Context, team: TeamModel, uri: Uri) {
        if (_state.value.savingEmblemId != null) return
        _state.update { it.copy(savingEmblemId = team.id, failedEmblemId = null) }
        viewModelScope.launch {
            try {
                val path = CommunityImageStorage.uploadTeamEmblem(context, uri, team.dtUid)
                TeamRepository.updateEmblem(team.id, path)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(failedEmblemId = team.id) }
            } finally {
                _state.update { it.copy(savingEmblemId = null) }
            }
        }
    }
}
