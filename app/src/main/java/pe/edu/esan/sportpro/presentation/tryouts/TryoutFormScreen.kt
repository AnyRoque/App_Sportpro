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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TryoutModel
import pe.edu.esan.sportpro.data.model.UserModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryoutFormScreen(
    user: UserModel,
    editTryoutId: String? = null,
    onBack: () -> Unit,
    viewModel: TryoutsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val existingTryout = remember(editTryoutId, uiState.tryouts) {
        if (!editTryoutId.isNullOrBlank()) {
            uiState.tryouts.find { it.id == editTryoutId }
        } else null
    }

    var selectedTeam by remember { mutableStateOf<TeamModel?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var positionsSought by remember { mutableStateOf("") }
    var tryoutDate by remember { mutableStateOf("") }
    var tryoutTime by remember { mutableStateOf("10:00 AM") }
    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var slotsStr by remember { mutableStateOf("15") }
    var registrationDeadline by remember { mutableStateOf("") }

    var expandedTeamMenu by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) {
        viewModel.loadDataForUser(user)
    }

    LaunchedEffect(existingTryout) {
        existingTryout?.let { t ->
            title = t.title
            description = t.description
            category = t.category
            positionsSought = t.positionsSought
            tryoutDate = t.tryoutDate
            tryoutTime = t.tryoutTime
            address = t.address
            district = t.district
            requirements = t.requirements
            slotsStr = t.slots.toString()
            registrationDeadline = t.registrationDeadline
        }
    }

    // Llenar por defecto equipo seleccionado si existe en la lista
    LaunchedEffect(uiState.dtApprovedTeams) {
        if (selectedTeam == null && uiState.dtApprovedTeams.isNotEmpty()) {
            val matchedTeam = if (existingTryout != null) {
                uiState.dtApprovedTeams.find { it.id == existingTryout.teamId }
            } else null
            selectedTeam = matchedTeam ?: uiState.dtApprovedTeams.first()
        }
    }

    // Valores por defecto para fechas si están vacíos
    val defaultDates = remember {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val d = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))

        // Fecha de prueba en 5 días
        cal.add(Calendar.DAY_OF_MONTH, 5)
        val ty = cal.get(Calendar.YEAR)
        val tm = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val td = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))

        Pair("$y-$m-$d", "$ty-$tm-$td")
    }

    LaunchedEffect(Unit) {
        if (registrationDeadline.isBlank()) registrationDeadline = defaultDates.first
        if (tryoutDate.isBlank()) tryoutDate = defaultDates.second
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (editTryoutId == null) "Nueva Convocatoria de Selección" else "Editar Convocatoria",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AVISO PREVENTIVO OBLIGATORIO (US-10)
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
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
                    text = "Convocatoria gratuita - Se prohíbe cualquier cobro o solicitud de pago para participar en las pruebas.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.dtApprovedTeams.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No tienes equipos aprobados para publicar convocatorias. Solo un DT con equipo validado por el Administrador puede publicar.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Selector de Equipo Aprobado
        Text("Equipo Verificado:", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = uiState.dtApprovedTeams.isNotEmpty()) { expandedTeamMenu = true }
                .padding(12.dp)
        ) {
            Text(
                text = selectedTeam?.let { "${it.name} (${it.category})" } ?: "Seleccionar Equipo Aprobado...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedTeam != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            DropdownMenu(
                expanded = expandedTeamMenu,
                onDismissRequest = { expandedTeamMenu = false }
            ) {
                uiState.dtApprovedTeams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text("${team.name} (${team.category})") },
                        onClick = {
                            selectedTeam = team
                            category = team.category
                            expandedTeamMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Título
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título del Aviso") },
            placeholder = { Text("ej. Pruebas Abiertas de Selección Sub-17") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Descripción
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción del Evento") },
            placeholder = { Text("Detalla el propósito de la prueba y la metodología...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categoría y Posiciones Buscadas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoría") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = slotsStr,
                onValueChange = { slotsStr = it },
                label = { Text("Cupos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = positionsSought,
            onValueChange = { positionsSought = it },
            label = { Text("Posiciones Buscadas") },
            placeholder = { Text("ej. Portero, Defensa central, Delantero") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fecha y Hora de Prueba
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tryoutDate,
                onValueChange = { tryoutDate = it },
                label = { Text("Fecha Prueba (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = tryoutTime,
                onValueChange = { tryoutTime = it },
                label = { Text("Hora (ej. 09:30 AM)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dirección y Distrito
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección / Campo") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = district,
                onValueChange = { district = it },
                label = { Text("Distrito") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fecha Límite de Inscripción
        OutlinedTextField(
            value = registrationDeadline,
            onValueChange = { registrationDeadline = it },
            label = { Text("Fecha Límite Inscripción (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Requisitos (Opcional)
        OutlinedTextField(
            value = requirements,
            onValueChange = { requirements = it },
            label = { Text("Requisitos (Opcional)") },
            placeholder = { Text("ej. Traer DNI físico, indumentaria deportiva blanca") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Cancelar")
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    val team = selectedTeam
                    if (team == null) {
                        Toast.makeText(context, "Debe seleccionar un equipo aprobado", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val slots = slotsStr.toIntOrNull() ?: 0

                    val tryoutToSave = (existingTryout ?: TryoutModel()).copy(
                        teamId = team.id,
                        teamName = team.name,
                        verifiedTeam = true,
                        title = title.trim(),
                        description = description.trim(),
                        category = category.trim(),
                        positionsSought = positionsSought.trim(),
                        tryoutDate = tryoutDate.trim(),
                        tryoutTime = tryoutTime.trim(),
                        address = address.trim(),
                        district = district.trim(),
                        requirements = requirements.trim(),
                        slots = slots,
                        registrationDeadline = registrationDeadline.trim(),
                        isFree = true,
                        dtUid = user.uid,
                        dtName = user.fullName
                    )

                    if (editTryoutId == null) {
                        viewModel.createTryout(tryoutToSave, onDone = onBack)
                    } else {
                        viewModel.updateTryout(tryoutToSave, onDone = onBack)
                    }
                },
                enabled = uiState.dtApprovedTeams.isNotEmpty()
            ) {
                Text(if (editTryoutId == null) "Publicar Convocatoria" else "Guardar Cambios")
            }
        }
    }
}
