package pe.edu.esan.sportpro.presentation.admin.event_catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.EventCatalogModel
import pe.edu.esan.sportpro.data.remote.EventCatalogRepository

data class EventCatalogUiState(
    val catalogItems: List<EventCatalogModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedItemForEdit: EventCatalogModel? = null,
    val showCreateEditDialog: Boolean = false
)

class EventCatalogViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventCatalogUiState())
    val uiState: StateFlow<EventCatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Seed 13 base default events if Firestore collection is empty
            EventCatalogRepository.seedDefaultsIfEmpty()

            // Observe catalog items in real time
            EventCatalogRepository.observeEventCatalog()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar el catálogo de eventos: ${e.message}"
                    )
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(
                        catalogItems = list,
                        isLoading = false
                    )
                }
        }
    }

    fun toggleActive(id: String, currentActive: Boolean) {
        viewModelScope.launch {
            val res = EventCatalogRepository.toggleActiveStatus(id, !currentActive)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (!currentActive) "Tipo de evento activado" else "Tipo de evento desactivado"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al cambiar estado"
                )
            }
        }
    }

    fun saveEventType(item: EventCatalogModel, isEdit: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = if (isEdit) {
                EventCatalogRepository.updateEventType(item)
            } else {
                EventCatalogRepository.createEventType(item)
            }

            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showCreateEditDialog = false,
                    selectedItemForEdit = null,
                    successMessage = if (isEdit) "Tipo de evento actualizado correctamente" else "Nuevo tipo de evento creado exitosamente"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al guardar el tipo de evento"
                )
            }
        }
    }

    fun deleteEventType(item: EventCatalogModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = EventCatalogRepository.deleteEventType(item)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Tipo de evento eliminado del catálogo"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "No se pudo eliminar el tipo de evento"
                )
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            selectedItemForEdit = null,
            showCreateEditDialog = true
        )
    }

    fun openEditDialog(item: EventCatalogModel) {
        _uiState.value = _uiState.value.copy(
            selectedItemForEdit = item,
            showCreateEditDialog = true
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(
            selectedItemForEdit = null,
            showCreateEditDialog = false
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
