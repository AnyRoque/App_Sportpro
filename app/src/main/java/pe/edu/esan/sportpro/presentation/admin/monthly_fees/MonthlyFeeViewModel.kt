package pe.edu.esan.sportpro.presentation.admin.monthly_fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.MonthlyFeeModel
import pe.edu.esan.sportpro.data.model.MonthlyFeeStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.remote.MonthlyFeeRepository

data class MonthlyFeeUiState(
    val fees: List<MonthlyFeeModel> = emptyList(),
    val players: List<PlayerModel> = emptyList(),
    val teams: List<TeamModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val filterTeam: String = "Todos",
    val filterPeriod: String = "",
    val filterStatus: String = "Todos",
    val historyPlayer: PlayerModel? = null,
    val showCreateDialog: Boolean = false
)

class MonthlyFeeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyFeeUiState())
    val uiState: StateFlow<MonthlyFeeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Carga limpia de jugadores y equipos
            val playersRes = MonthlyFeeRepository.getActivePlayers()
            val teamsRes = MonthlyFeeRepository.getTeams()

            val loadedPlayers = playersRes.getOrDefault(emptyList())
            val loadedTeams = teamsRes.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                players = loadedPlayers,
                teams = loadedTeams
            )

            // Observar mensualidades en tiempo real
            MonthlyFeeRepository.observeMonthlyFees()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar mensualidades: ${e.message}"
                    )
                }
                .collect { feesList ->
                    // Evaluar auto-vencimiento en segundo plano
                    viewModelScope.launch {
                        MonthlyFeeRepository.autoCheckExpiredFees(feesList)
                    }

                    _uiState.value = _uiState.value.copy(
                        fees = feesList,
                        isLoading = false
                    )
                }
        }
    }

    fun createMonthlyFee(
        selectedPlayer: PlayerModel?,
        period: String,
        amountStr: String,
        dueDate: String,
        adminEmail: String
    ) {
        if (selectedPlayer == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Debe seleccionar un jugador de la lista")
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingrese un monto numérico válido mayor a 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val newFee = MonthlyFeeModel(
                playerId = selectedPlayer.id,
                playerName = selectedPlayer.fullName,
                teamId = selectedPlayer.teamId,
                teamName = selectedPlayer.teamName,
                category = selectedPlayer.category,
                period = period.trim(),
                amount = amount,
                dueDate = dueDate.trim(),
                status = MonthlyFeeStatus.PENDING,
                createdBy = adminEmail
            )

            val res = MonthlyFeeRepository.createMonthlyFee(newFee)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showCreateDialog = false,
                    successMessage = "Mensualidad registrada exitosamente para ${selectedPlayer.fullName}"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al registrar la mensualidad"
                )
            }
        }
    }

    fun markAsPaid(feeId: String, adminEmail: String) {
        val targetFee = _uiState.value.fees.find { it.id == feeId }
        if (targetFee == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No se encontró la mensualidad especificada")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = MonthlyFeeRepository.updateFeeStatus(
                playerId = targetFee.playerId,
                feeId = feeId,
                newStatus = MonthlyFeeStatus.PAID,
                updatedByAdmin = adminEmail
            )
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Estado actualizado a PAGADO correctamente"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al actualizar estado"
                )
            }
        }
    }

    fun setFilterTeam(team: String) {
        _uiState.value = _uiState.value.copy(filterTeam = team)
    }

    fun setFilterPeriod(period: String) {
        _uiState.value = _uiState.value.copy(filterPeriod = period)
    }

    fun setFilterStatus(status: String) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun openHistoryForPlayer(player: PlayerModel) {
        _uiState.value = _uiState.value.copy(historyPlayer = player)
    }

    fun closeHistory() {
        _uiState.value = _uiState.value.copy(historyPlayer = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
