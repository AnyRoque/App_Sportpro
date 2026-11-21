package pe.edu.esan.sportpro.presentation.admin.monthly_fees

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.MonthlyFeeModel
import pe.edu.esan.sportpro.data.model.MonthlyFeeStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyFeeScreen(
    adminEmail: String,
    viewModel: MonthlyFeeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Manejo de mensajes Toast
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

    // Filtrar lista localmente según la selección
    val filteredFees = uiState.fees.filter { fee ->
        val matchTeam = uiState.filterTeam == "Todos" || fee.teamName == uiState.filterTeam || fee.teamId == uiState.filterTeam
        val matchPeriod = uiState.filterPeriod.isBlank() || fee.period.contains(uiState.filterPeriod.trim(), ignoreCase = true)
        val matchStatus = uiState.filterStatus == "Todos" || fee.status == uiState.filterStatus
        matchTeam && matchPeriod && matchStatus
    }.sortedByDescending { it.createdAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // LEYENDA ESTÁTICA OBLIGATORIA (US-08)
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Registro simulado – no se procesan pagos reales",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Fila de acción principal: Botón "Registrar Mensualidad"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Mensualidades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.openCreateDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar")
            }
        }

        // Filtros (Equipo, Periodo, Estado)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Filtros de Búsqueda", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filtro Periodo
                OutlinedTextField(
                    value = uiState.filterPeriod,
                    onValueChange = { viewModel.setFilterPeriod(it) },
                    label = { Text("Filtrar por Periodo (ej. 11/2026)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chips para filtro de Estado
                Text("Estado:", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val statusOptions = listOf("Todos", MonthlyFeeStatus.PENDING, MonthlyFeeStatus.PAID, MonthlyFeeStatus.EXPIRED)
                    items(statusOptions) { status ->
                        FilterChip(
                            selected = uiState.filterStatus == status,
                            onClick = { viewModel.setFilterStatus(status) },
                            label = { Text(status) }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredFees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay mensualidades registradas con los filtros seleccionados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredFees) { fee ->
                    MonthlyFeeCard(
                        fee = fee,
                        onMarkPaid = { viewModel.markAsPaid(fee.id, adminEmail) },
                        onViewHistory = {
                            val p = uiState.players.find { it.id == fee.playerId }
                            if (p != null) {
                                viewModel.openHistoryForPlayer(p)
                            } else {
                                Toast.makeText(context, "Historial de ${fee.playerName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal para "Registrar Nueva Mensualidad"
    if (uiState.showCreateDialog) {
        CreateMonthlyFeeDialog(
            players = uiState.players,
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { player, period, amountStr, dueDate ->
                viewModel.createMonthlyFee(player, period, amountStr, dueDate, adminEmail)
            }
        )
    }

    // Modal para "Historial de Mensualidades por Jugador"
    uiState.historyPlayer?.let { player ->
        PlayerHistoryDialog(
            player = player,
            allFees = uiState.fees,
            onDismiss = { viewModel.closeHistory() }
        )
    }
}

@Composable
fun MonthlyFeeCard(
    fee: MonthlyFeeModel,
    onMarkPaid: () -> Unit,
    onViewHistory: () -> Unit
) {
    val (statusBg, statusColor) = when (fee.status) {
        MonthlyFeeStatus.PAID -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        MonthlyFeeStatus.EXPIRED -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = fee.playerName.ifBlank { "Jugador desconocido" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (fee.teamName.isNotBlank() || fee.category.isNotBlank()) {
                            Text(
                                text = "${fee.teamName} ${if (fee.category.isNotBlank()) "(${fee.category})" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Badge de Estado
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = fee.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Periodo: ${fee.period}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Monto: S/ ${String.format(Locale.US, "%.2f", fee.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vence: ${fee.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fee.isExpired) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (fee.isPaid && fee.updatedBy.isNotBlank()) {
                    Text(
                        text = "Pagado a: ${fee.updatedBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewHistory) {
                    Text("Ver Historial")
                }

                if (!fee.isPaid) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Marcar Pagado")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMonthlyFeeDialog(
    players: List<PlayerModel>,
    onDismiss: () -> Unit,
    onConfirm: (PlayerModel?, String, String, String) -> Unit
) {
    val defaultPeriod = remember {
        val cal = java.util.Calendar.getInstance()
        val m = String.format(Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
        val y = cal.get(java.util.Calendar.YEAR)
        "$m/$y"
    }
    val defaultDueDate = remember {
        val cal = java.util.Calendar.getInstance()
        val y = cal.get(java.util.Calendar.YEAR)
        val m = String.format(Locale.US, "%02d", cal.get(java.util.Calendar.MONTH) + 1)
        val lastDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        "$y-$m-$lastDay"
    }

    var selectedPlayer by remember { mutableStateOf<PlayerModel?>(null) }
    var period by remember { mutableStateOf(defaultPeriod) }
    var amountStr by remember { mutableStateOf("150.00") }
    var dueDate by remember { mutableStateOf(defaultDueDate) }

    var expandedPlayerMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Registrar Mensualidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Jugador
                Text("Jugador:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { expandedPlayerMenu = true }
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedPlayer?.let { "${it.fullName} (${it.teamName})" } ?: "Seleccionar Jugador...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedPlayer != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DropdownMenu(
                        expanded = expandedPlayerMenu,
                        onDismissRequest = { expandedPlayerMenu = false }
                    ) {
                        if (players.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay jugadores disponibles") },
                                onClick = { expandedPlayerMenu = false }
                            )
                        } else {
                            players.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.fullName} - ${p.teamName} (${p.category})") },
                                    onClick = {
                                        selectedPlayer = p
                                        expandedPlayerMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Periodo
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    label = { Text("Periodo (ej. 11/2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Monto
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Fecha Vencimiento
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Fecha Vencimiento (ej. 2026-11-30)") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        onClick = { onConfirm(selectedPlayer, period, amountStr, dueDate) }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerHistoryDialog(
    player: PlayerModel,
    allFees: List<MonthlyFeeModel>,
    onDismiss: () -> Unit
) {
    val playerFees = allFees.filter { it.playerId == player.id }.sortedByDescending { it.period }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Historial de Mensualidades",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player.fullName} (${player.teamName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (playerFees.isEmpty()) {
                    Text(
                        text = "El jugador no registra historial de mensualidades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(playerFees) { fee ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Periodo: ${fee.period}", fontWeight = FontWeight.Bold)
                                        Text("S/ ${fee.amount} - Vence: ${fee.dueDate}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = fee.status,
                                        fontWeight = FontWeight.Bold,
                                        color = when (fee.status) {
                                            MonthlyFeeStatus.PAID -> Color(0xFF2E7D32)
                                            MonthlyFeeStatus.EXPIRED -> Color(0xFFC62828)
                                            else -> Color(0xFFF57F17)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}
