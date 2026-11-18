package pe.edu.esan.sportpro.presentation.tryouts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import pe.edu.esan.sportpro.data.model.ReportReason
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TryoutModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryoutListScreen(
    user: UserModel,
    onNavigate: (String) -> Unit,
    viewModel: TryoutsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(user.uid) {
        viewModel.loadDataForUser(user)
    }

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

    // Filtrado de convocatorias
    val filteredTryouts = uiState.tryouts.filter { t ->
        val matchCat = uiState.filterCategory.isBlank() || t.category.contains(uiState.filterCategory.trim(), ignoreCase = true)
        val matchPos = uiState.filterPosition.isBlank() || t.positionsSought.contains(uiState.filterPosition.trim(), ignoreCase = true)
        val matchDate = uiState.filterDate.isBlank() || t.tryoutDate.contains(uiState.filterDate.trim(), ignoreCase = true)
        val matchDist = uiState.filterDistrict.isBlank() || t.district.contains(uiState.filterDistrict.trim(), ignoreCase = true) || t.address.contains(uiState.filterDistrict.trim(), ignoreCase = true)
        matchCat && matchPos && matchDate && matchDist
    }

    val isDt = user.roleEnum == Role.DT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera con título y botón para DT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pruebas y Convocatorias",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDt) "Tus publicaciones de selección" else "Convocatorias abiertas de equipos verificados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDt) {
                Button(
                    onClick = {
                        if (uiState.dtApprovedTeams.isEmpty()) {
                            Toast.makeText(context, "Debes tener un equipo APROBADO para publicar convocatorias.", Toast.LENGTH_LONG).show()
                        } else {
                            onNavigate(Routes.tryoutForm(null))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Publicar")
                }
            }
        }

        // Bloque de Filtros Combinables y Limpiables
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Filtros de Búsqueda",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.filterCategory,
                        onValueChange = { viewModel.setFilterCategory(it) },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.filterPosition,
                        onValueChange = { viewModel.setFilterPosition(it) },
                        label = { Text("Posición") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.filterDistrict,
                        onValueChange = { viewModel.setFilterDistrict(it) },
                        label = { Text("Distrito / Lugar") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.filterDate,
                        onValueChange = { viewModel.setFilterDate(it) },
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.filterCategory.isNotBlank() || uiState.filterPosition.isNotBlank() || uiState.filterDate.isNotBlank() || uiState.filterDistrict.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { viewModel.clearFilters() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Limpiar filtros")
                    }
                }
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
        } else if (filteredTryouts.isEmpty()) {
            // MENSAJE OBLIGATORIO EXACTO DE US-10
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron avisos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTryouts) { tryout ->
                    TryoutCard(
                        tryout = tryout,
                        currentUser = user,
                        onEdit = { onNavigate(Routes.tryoutForm(tryout.id)) },
                        onDelete = { viewModel.deleteTryout(tryout.id) },
                        onViewCandidates = { onNavigate(Routes.tryoutCandidates(tryout.id)) },
                        onRegisterInterest = { viewModel.openInterestDialog(tryout) },
                        onReport = { viewModel.openReportDialog(tryout) }
                    )
                }
            }
        }
    }

    // Modal "Me interesa"
    uiState.selectedTryoutForInterest?.let { tryout ->
        RegisterInterestDialog(
            tryout = tryout,
            user = user,
            onDismiss = { viewModel.closeInterestDialog() },
            onConfirm = { name, age, pos ->
                viewModel.registerInterest(tryout.id, name, age, pos, user)
            }
        )
    }

    // Modal "Reportar aviso" (Integración US-24)
    uiState.selectedTryoutForReport?.let { tryout ->
        ReportTryoutDialog(
            tryout = tryout,
            onDismiss = { viewModel.closeReportDialog() },
            onConfirm = { reason, detail ->
                viewModel.reportTryout(tryout, user.uid, reason, detail)
            }
        )
    }
}

@Composable
fun TryoutCard(
    tryout: TryoutModel,
    currentUser: UserModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewCandidates: () -> Unit,
    onRegisterInterest: () -> Unit,
    onReport: () -> Unit
) {
    val isOwner = currentUser.uid == tryout.dtUid

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tryout.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tryout.teamName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // INSIÑA OBLIGATORIA "Equipo verificado"
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Equipo verificado",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tryout.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Detalles clave: Categoría, Posiciones, Fecha/Hora, Ubicación, Cupos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Categoría: ${tryout.category}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(text = "Cupos: ${tryout.slots}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Text(text = "Posiciones buscadas: ${tryout.positionsSought}", style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Prueba: ${tryout.tryoutDate} - ${tryout.tryoutTime}", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${tryout.address} (${tryout.district})", style = MaterialTheme.typography.bodySmall)
            }

            if (tryout.requirements.isNotBlank()) {
                Text(
                    text = "Requisitos: ${tryout.requirements}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Inscripciones hasta: ${tryout.registrationDeadline}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Acciones según rol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Opción discreta de "Reportar aviso"
                IconButton(onClick = onReport, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Reportar aviso",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOwner) {
                        OutlinedButton(onClick = onViewCandidates) {
                            Text("Interesados (${tryout.interestedCount})")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(
                            onClick = onRegisterInterest,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Me interesa")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterInterestDialog(
    tryout: TryoutModel,
    user: UserModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user.fullName) }
    var ageStr by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }

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
                    text = "Registrar Interés",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tryout.title} (${tryout.teamName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Edad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Posición Preferida") },
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
                    Button(onClick = { onConfirm(name, ageStr, position) }) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTryoutDialog(
    tryout: TryoutModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.ALL.first()) }
    var detail by remember { mutableStateOf("") }
    var expandedReasonMenu by remember { mutableStateOf(false) }

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
                    text = "Reportar Aviso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Publicación: ${tryout.title}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Motivo del Reporte:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { expandedReasonMenu = true }
                        .padding(12.dp)
                ) {
                    Text(selectedReason, style = MaterialTheme.typography.bodyMedium)

                    DropdownMenu(
                        expanded = expandedReasonMenu,
                        onDismissRequest = { expandedReasonMenu = false }
                    ) {
                        ReportReason.ALL.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReason = reason
                                    expandedReasonMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Detalles adicionales (opcional)") },
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
                        onClick = { onConfirm(selectedReason, detail) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Enviar Reporte")
                    }
                }
            }
        }
    }
}
