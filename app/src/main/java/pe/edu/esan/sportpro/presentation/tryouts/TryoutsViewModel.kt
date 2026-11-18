package pe.edu.esan.sportpro.presentation.tryouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TryoutCandidateModel
import pe.edu.esan.sportpro.data.model.TryoutModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.TryoutRepository

data class TryoutUiState(
    val tryouts: List<TryoutModel> = emptyList(),
    val dtApprovedTeams: List<TeamModel> = emptyList(),
    val candidates: List<TryoutCandidateModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val filterCategory: String = "",
    val filterPosition: String = "",
    val filterDate: String = "",
    val filterDistrict: String = "",
    val selectedTryoutForInterest: TryoutModel? = null,
    val selectedTryoutForReport: TryoutModel? = null
)

class TryoutsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TryoutUiState())
    val uiState: StateFlow<TryoutUiState> = _uiState.asStateFlow()

    fun loadDataForUser(user: UserModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (user.roleEnum == Role.DT) {
                // Cargar equipos aprobados del DT
                val teamsRes = TryoutRepository.getApprovedTeamsByDt(user.uid)
                val approvedTeams = teamsRes.getOrDefault(emptyList())

                _uiState.value = _uiState.value.copy(dtApprovedTeams = approvedTeams)

                // Observar avisos creados por este DT
                TryoutRepository.observeTryoutsByDt(user.uid)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al cargar tus avisos: ${e.message}"
                        )
                    }
                    .collect { list ->
                        _uiState.value = _uiState.value.copy(
                            tryouts = list,
                            isLoading = false
                        )
                    }
            } else {
                // Para JUG y PAD: Observar convocatorias activas generales
                TryoutRepository.observeActiveTryouts()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al cargar avisos de convocatorias: ${e.message}"
                        )
                    }
                    .collect { list ->
                        _uiState.value = _uiState.value.copy(
                            tryouts = list,
                            isLoading = false
                        )
                    }
            }
        }
    }

    fun setFilterCategory(category: String) {
        _uiState.value = _uiState.value.copy(filterCategory = category)
    }

    fun setFilterPosition(position: String) {
        _uiState.value = _uiState.value.copy(filterPosition = position)
    }

    fun setFilterDate(date: String) {
        _uiState.value = _uiState.value.copy(filterDate = date)
    }

    fun setFilterDistrict(district: String) {
        _uiState.value = _uiState.value.copy(filterDistrict = district)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            filterCategory = "",
            filterPosition = "",
            filterDate = "",
            filterDistrict = ""
        )
    }

    fun createTryout(tryout: TryoutModel, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = TryoutRepository.createTryout(tryout)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Aviso de convocatoria publicado exitosamente."
                )
                onDone()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al publicar la convocatoria."
                )
            }
        }
    }

    fun updateTryout(tryout: TryoutModel, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = TryoutRepository.updateTryout(tryout)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Aviso actualizado correctamente."
                )
                onDone()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al actualizar aviso."
                )
            }
        }
    }

    fun deleteTryout(tryoutId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = TryoutRepository.deleteTryout(tryoutId)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Aviso eliminado correctamente."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al eliminar aviso."
                )
            }
        }
    }

    fun openInterestDialog(tryout: TryoutModel) {
        _uiState.value = _uiState.value.copy(selectedTryoutForInterest = tryout)
    }

    fun closeInterestDialog() {
        _uiState.value = _uiState.value.copy(selectedTryoutForInterest = null)
    }

    fun registerInterest(tryoutId: String, name: String, ageStr: String, position: String, user: UserModel) {
        val age = ageStr.toIntOrNull()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese su nombre completo.")
            return
        }
        if (age == null || age <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese una edad válida.")
            return
        }
        if (position.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese la posición preferida.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val candidate = TryoutCandidateModel(
                tryoutId = tryoutId,
                userUid = user.uid,
                name = name.trim(),
                age = age,
                position = position.trim(),
                role = user.role
            )

            val res = TryoutRepository.registerInterest(candidate)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedTryoutForInterest = null,
                    successMessage = "¡Te has registrado exitosamente en la lista de interesados!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al registrar interés."
                )
            }
        }
    }

    fun loadCandidates(tryoutId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = TryoutRepository.getCandidatesByTryout(tryoutId)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    candidates = res.getOrDefault(emptyList()),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al cargar lista de interesados."
                )
            }
        }
    }

    fun openReportDialog(tryout: TryoutModel) {
        _uiState.value = _uiState.value.copy(selectedTryoutForReport = tryout)
    }

    fun closeReportDialog() {
        _uiState.value = _uiState.value.copy(selectedTryoutForReport = null)
    }

    fun reportTryout(tryout: TryoutModel, reporterUid: String, reason: String, detail: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Seleccione un motivo de reporte.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = TryoutRepository.reportTryout(
                tryoutId = tryout.id,
                tryoutTitle = tryout.title,
                authorUid = tryout.dtUid,
                authorName = tryout.dtName,
                reporterUid = reporterUid,
                reason = reason,
                detail = detail
            )

            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedTryoutForReport = null,
                    successMessage = "Reporte enviado. Gracias por ayudar a mantener segura la comunidad."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al enviar el reporte."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
