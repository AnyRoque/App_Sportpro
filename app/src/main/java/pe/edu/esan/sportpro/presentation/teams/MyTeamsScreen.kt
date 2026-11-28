package pe.edu.esan.sportpro.presentation.teams

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.remote.CommunityImageStorage
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/** US-03: "Mis equipos" con su estado (Pendiente / Aprobado / Rechazado). */
@Composable
fun MyTeamsScreen(
    dtUid: String,
    canEditEmblem: Boolean,
    onNewTeam: () -> Unit,
    onOpenRoster: (TeamModel) -> Unit,
    onFixTeam: (TeamModel) -> Unit,
    viewModel: MyTeamsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current.applicationContext
    var pickingForTeamId by rememberSaveable { mutableStateOf<String?>(null) }
    val pickEmblem = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val team = state.teams.firstOrNull { it.id == pickingForTeamId }
        pickingForTeamId = null
        if (canEditEmblem && uri != null && team != null) {
            viewModel.saveEmblem(context, team, uri)
        }
    }
    LaunchedEffect(dtUid) { viewModel.start(dtUid) }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingBox()
            state.error != null -> EmptyState("Error: ${state.error}")
            state.teams.isEmpty() -> EmptyState("Aún no registras equipos.\nPresiona “Nuevo equipo” para empezar.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.teams, key = { it.id }) { team ->
                    TeamCard(
                        team, onOpenRoster, onFixTeam,
                        onPickEmblem = if (canEditEmblem) {
                            {
                                pickingForTeamId = team.id
                                pickEmblem.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        } else null,
                        emblemActionEnabled = state.savingEmblemId == null,
                        savingEmblem = state.savingEmblemId == team.id,
                        emblemFailed = state.failedEmblemId == team.id
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewTeam,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Nuevo equipo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
private fun TeamCard(
    team: TeamModel,
    onOpenRoster: (TeamModel) -> Unit,
    onFixTeam: (TeamModel) -> Unit,
    onPickEmblem: (() -> Unit)?,
    emblemActionEnabled: Boolean,
    savingEmblem: Boolean,
    emblemFailed: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = team.status == TeamStatus.APPROVED) { onOpenRoster(team) }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (team.emblemPath.isNotBlank()) {
                    TeamEmblem(CommunityImageStorage.publicUrl(team.emblemPath), Modifier.size(44.dp))
                }
                Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                StatusChip(team.status)
            }
            Text("${team.category} · ${team.district}", style = MaterialTheme.typography.bodyMedium)
            Text("Campo: ${team.field}", style = MaterialTheme.typography.bodySmall)
            if (team.trainingDays.isNotEmpty())
                Text("Entrena: ${team.trainingDays.joinToString(", ")} ${team.trainingTime}", style = MaterialTheme.typography.bodySmall)

            if (onPickEmblem != null) {
                OutlinedButton(onClick = onPickEmblem, enabled = emblemActionEnabled) {
                    Text(when {
                        savingEmblem -> "Guardando emblema…"
                        team.emblemPath.isBlank() -> "Añadir emblema"
                        else -> "Cambiar emblema"
                    })
                }
                if (emblemFailed) {
                    Text("No se pudo guardar el emblema. Intenta de nuevo.", color = MaterialTheme.colorScheme.error)
                }
            }

            when (team.status) {
                TeamStatus.PENDING -> Text("En revisión por el administrador. Aún no puedes agregar jugadores.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                TeamStatus.APPROVED -> Text("Toca para ver el plantel", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                TeamStatus.REJECTED -> {
                    Text("Motivo: ${team.rejectReason}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { onFixTeam(team) }) { Text("Corregir y reenviar") }
                }
            }
        }
    }
}
