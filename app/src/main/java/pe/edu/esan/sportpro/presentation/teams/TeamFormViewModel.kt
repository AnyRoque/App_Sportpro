package pe.edu.esan.sportpro.presentation.teams

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.CommunityImageStorage
import pe.edu.esan.sportpro.data.remote.TeamRepository
import pe.edu.esan.sportpro.util.FormValidators

data class TeamFormState(
    val editingId: String? = null,
    val name: String = "",
    val category: String = "",
    val field: String = "",
    val district: String = "",
    val days: Set<String> = emptySet(),
    val time: String = "",
    val emblemUri: Uri? = null,
    val emblemPath: String = "",
    val errors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null
)

/** US-03: registrar equipo (queda Pendiente) o corregir y reenviar uno rechazado. */
class TeamFormViewModel : ViewModel() {

    private val _state = MutableStateFlow(TeamFormState())
    val state: StateFlow<TeamFormState> = _state.asStateFlow()
    private var original: TeamModel? = null

    fun load(teamId: String?) {
        if (teamId == null || original != null) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            TeamRepository.getTeam(teamId)
                .onSuccess { t ->
                    original = t
                    _state.value = TeamFormState(
                        editingId = t.id, name = t.name, category = t.category, field = t.field,
                        district = t.district, days = t.trainingDays.toSet(), time = t.trainingTime
                    )
                }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.message) } }
        }
    }

    fun onChange(transform: (TeamFormState) -> TeamFormState) = _state.update { current ->
        if (current.saving || current.saved) current else transform(current)
    }

    fun selectEmblem(uri: Uri?) {
        _state.update { if (it.saving) it else it.copy(emblemUri = uri, emblemPath = "", message = null) }
    }

    fun save(dt: UserModel, context: Context) {
        val s = _state.value
        if (s.saving || s.saved) return
        val errors = FormValidators.validateTeam(s.name, s.category, s.field, s.district)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(saving = true, errors = emptyMap(), message = null) }
        viewModelScope.launch {
            val emblemPath = try {
                if (s.editingId == null && s.emblemUri != null && s.emblemPath.isBlank()) {
                    CommunityImageStorage.uploadTeamEmblem(context, s.emblemUri, dt.uid).also { path ->
                        _state.update { it.copy(emblemPath = path) }
                    }
                } else {
                    s.emblemPath
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update {
                    it.copy(saving = false,
                        message = "No se pudo subir el emblema. Intenta de nuevo o quita la imagen para registrar el equipo.")
                }
                return@launch
            }
            val team = (original ?: TeamModel()).copy(
                name = s.name.trim(),
                category = s.category,
                field = s.field.trim(),
                district = s.district.trim(),
                trainingDays = s.days.toList(),
                trainingTime = s.time.trim(),
                dtUid = dt.uid,
                dtName = dt.fullName
            )
            val result = if (s.editingId == null) TeamRepository.createTeam(team.copy(emblemPath = emblemPath))
                         else TeamRepository.updateAndResubmit(team)
            result
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, message = e.message) } }
        }
    }
}
