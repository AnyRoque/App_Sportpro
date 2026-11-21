package pe.edu.esan.sportpro.presentation.admin.event_catalog

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.AccumulatesStatType
import pe.edu.esan.sportpro.data.model.EventCatalogModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCatalogScreen(
    viewModel: EventCatalogViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera principal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Catálogo de Eventos del Partido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configuración de reglas y tipos de eventos (US-09)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.openCreateDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Evento")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Los eventos activos se sincronizan en tiempo real con las consolas de registro de los partidos. Los tipos usados no se eliminan, solo se desactivan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.catalogItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay tipos de eventos configurados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.catalogItems) { item ->
                    EventCatalogItemCard(
                        item = item,
                        onToggleActive = { viewModel.toggleActive(item.id, item.active) },
                        onEdit = { viewModel.openEditDialog(item) },
                        onDelete = { viewModel.deleteEventType(item) }
                    )
                }
            }
        }
    }

    // Modal de Creación / Edición
    if (uiState.showCreateEditDialog) {
        CreateEditEventDialog(
            initialItem = uiState.selectedItemForEdit,
            onDismiss = { viewModel.closeDialog() },
            onConfirm = { eventToSave ->
                viewModel.saveEventType(eventToSave, isEdit = uiState.selectedItemForEdit != null)
            }
        )
    }
}

@Composable
fun EventCatalogItemCard(
    item: EventCatalogModel,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (item.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val colorParsed = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = colorParsed,
                        shape = CircleShape,
                        modifier = Modifier.size(16.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isSystemDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Base",
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.active) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.active) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = item.active,
                        onCheckedChange = { onToggleActive() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Etiquetas de configuración
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (item.requiresTeam) ConfigTag(label = "Equipo")
                if (item.requiresPlayer) ConfigTag(label = "Jugador")
                if (item.requiresSecondPlayer) ConfigTag(label = "2do Jugador (Sale/Entra)")
                if (item.affectsScore) ConfigTag(label = "+ Marcador", color = Color(0xFF2E7D32))
            }

            if (item.accumulatesStat != AccumulatesStatType.NONE) {
                Text(
                    text = "Acumula estadística: ${item.accumulatesStat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ConfigTag(label: String, color: Color = MaterialTheme.colorScheme.secondary) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEventDialog(
    initialItem: EventCatalogModel?,
    onDismiss: () -> Unit,
    onConfirm: (EventCatalogModel) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var colorHex by remember { mutableStateOf(initialItem?.colorHex ?: "#2196F3") }
    var requiresTeam by remember { mutableStateOf(initialItem?.requiresTeam ?: true) }
    var requiresPlayer by remember { mutableStateOf(initialItem?.requiresPlayer ?: true) }
    var requiresSecondPlayer by remember { mutableStateOf(initialItem?.requiresSecondPlayer ?: false) }
    var affectsScore by remember { mutableStateOf(initialItem?.affectsScore ?: false) }
    var accumulatesStat by remember { mutableStateOf(initialItem?.accumulatesStat ?: AccumulatesStatType.NONE) }

    var expandedStatMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialItem == null) "Nuevo Tipo de Evento" else "Editar Tipo de Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Evento (Único)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Color Hexadecimal (ej. #4CAF50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Checkboxes de configuración
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requiresTeam, onCheckedChange = { requiresTeam = it })
                    Text("Requiere equipo", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requiresPlayer, onCheckedChange = { requiresPlayer = it })
                    Text("Requiere jugador principal", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requiresSecondPlayer, onCheckedChange = { requiresSecondPlayer = it })
                    Text("Requiere 2do jugador (ej. Cambio: sale/entra)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = affectsScore, onCheckedChange = { affectsScore = it })
                    Text("Afecta el marcador del partido (+1 Gol)", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Estadística que acumula:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { expandedStatMenu = true }
                        .padding(12.dp)
                ) {
                    Text(accumulatesStat, style = MaterialTheme.typography.bodyMedium)

                    DropdownMenu(
                        expanded = expandedStatMenu,
                        onDismissRequest = { expandedStatMenu = false }
                    ) {
                        AccumulatesStatType.ALL.forEach { stat ->
                            DropdownMenuItem(
                                text = { Text(stat) },
                                onClick = {
                                    accumulatesStat = stat
                                    expandedStatMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val eventToSave = (initialItem ?: EventCatalogModel()).copy(
                                name = name.trim(),
                                colorHex = colorHex.trim(),
                                requiresTeam = requiresTeam,
                                requiresPlayer = requiresPlayer,
                                requiresSecondPlayer = requiresSecondPlayer,
                                affectsScore = affectsScore,
                                accumulatesStat = accumulatesStat,
                                active = initialItem?.active ?: true
                            )
                            onConfirm(eventToSave)
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
