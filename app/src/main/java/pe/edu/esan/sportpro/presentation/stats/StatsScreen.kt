package pe.edu.esan.sportpro.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.esan.sportpro.data.model.PlayerStatsModel
import pe.edu.esan.sportpro.data.model.TeamStatsModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected
import java.util.Locale

/**
 * US-15: Pantalla para la Consulta de Estadísticas Acumuladas por Jugador y por Equipo.
 */
@Composable
fun StatsScreen(
    user: UserModel,
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(user.uid) {
        viewModel.loadForUser(user)
    }

    if (state.loading) {
        LoadingBox()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Selector de Equipo si cuenta con múltiples equipos
        if (state.teams.size > 1) {
            val teamNames = state.teams.map { "${it.name} (${it.category})" }
            val selectedName = state.selectedTeam?.let { "${it.name} (${it.category})" } ?: teamNames.first()

            DropdownField(
                label = "Seleccionar equipo",
                options = teamNames,
                selected = selectedName,
                onSelected = { label ->
                    val team = state.teams.find { "${it.name} (${it.category})" == label }
                    if (team != null) viewModel.selectTeam(team)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else if (state.selectedTeam != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.selectedTeam!!.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Categoría: ${state.selectedTeam!!.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar estadísticas", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.errorMessage != null && state.selectedTeam == null) {
            EmptyState(message = state.errorMessage!!)
            return
        }

        // Pestañas (0: Por Jugador, 1: Por Equipo)
        TabRow(selectedTabIndex = state.selectedTab) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { viewModel.setTab(0) },
                text = { Text("Estadísticas por Jugador", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { viewModel.setTab(1) },
                text = { Text("Estadísticas del Equipo", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (state.selectedTab) {
            0 -> PlayerStatsTab(
                playerStatsList = state.playerStatsList,
                searchQuery = state.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) }
            )
            1 -> TeamStatsTab(teamStats = state.teamStats)
        }
    }
}

@Composable
private fun PlayerStatsTab(
    playerStatsList: List<PlayerStatsModel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    val filteredList = playerStatsList.filter { p ->
        searchQuery.isBlank() ||
                p.playerName.contains(searchQuery, ignoreCase = true) ||
                p.position.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar jugador por nombre o posición...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            EmptyState(
                message = if (searchQuery.isBlank()) "No hay estadísticas de jugadores registradas aún." else "No se encontraron jugadores.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.playerId }) { playerStats ->
                    PlayerStatsCard(stats = playerStats)
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsCard(stats: PlayerStatsModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecera: Nombre y Posición
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.shirtNumber?.toString() ?: "#",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stats.playerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (stats.position.isNotBlank()) stats.position else "Sin posición",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Porcentaje de Asistencia a Entrenamientos
                val formattedAtt = String.format(Locale.getDefault(), "%.1f", stats.asistenciaEntrenamientosPorcentaje)
                Surface(
                    color = if (stats.asistenciaEntrenamientosPorcentaje >= 75f) StatusApproved else StatusPending,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$formattedAtt% Asist.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Grilla de las 9 Métricas de la US-15
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge("Convocado", stats.partidosConvocados.toString(), MaterialTheme.colorScheme.onSurfaceVariant)
                StatBadge("Jugados", stats.partidosJugados.toString(), MaterialTheme.colorScheme.primary)
                StatBadge("Titular", stats.partidosTitular.toString(), MaterialTheme.colorScheme.secondary)
                StatBadge("Minutos", "${stats.minutosJugados}'", MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge("⚽ Goles", stats.goles.toString(), StatusApproved)
                StatBadge("🟨 Amarillas", stats.tarjetasAmarillas.toString(), StatusPending)
                StatBadge("🟥 Rojas", stats.tarjetasRojas.toString(), StatusRejected)
                StatBadge("Faltas", stats.faltasCometidas.toString(), Color.Gray)
            }
        }
    }
}

@Composable
private fun TeamStatsTab(teamStats: TeamStatsModel?) {
    if (teamStats == null) {
        EmptyState(message = "No hay datos de equipo disponibles.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tarjeta Principal de Rendimiento del Equipo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rendimiento Global del Equipo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TeamMetricCard("PJ", teamStats.partidosJugados.toString(), MaterialTheme.colorScheme.onPrimaryContainer)
                    TeamMetricCard("PG", teamStats.partidosGanados.toString(), StatusApproved)
                    TeamMetricCard("PE", teamStats.partidosEmpatados.toString(), StatusPending)
                    TeamMetricCard("PP", teamStats.partidosPerdidos.toString(), StatusRejected)
                    TeamMetricCard("PTS", teamStats.puntos.toString(), MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Tarjeta de Marcador y Goles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Balance de Goles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Goles a favor (GF):", fontWeight = FontWeight.SemiBold)
                    Text("${teamStats.golesFavor}", fontWeight = FontWeight.Bold, color = StatusApproved)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Goles en contra (GC):", fontWeight = FontWeight.SemiBold)
                    Text("${teamStats.golesContra}", fontWeight = FontWeight.Bold, color = StatusRejected)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diferencia de goles (DG):", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (teamStats.diferenciaGoles > 0) "+${teamStats.diferenciaGoles}" else "${teamStats.diferenciaGoles}",
                        fontWeight = FontWeight.Bold,
                        color = if (teamStats.diferenciaGoles >= 0) StatusApproved else StatusRejected
                    )
                }
            }
        }

        // Tarjeta de Disciplina
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Registro Disciplinario del Equipo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TeamMetricCard("🟨 Amarillas", teamStats.tarjetasAmarillasTotales.toString(), StatusPending)
                    TeamMetricCard("🟥 Rojas", teamStats.tarjetasRojasTotales.toString(), StatusRejected)
                    TeamMetricCard("Faltas", teamStats.faltasTotales.toString(), Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
    }
}

@Composable
private fun TeamMetricCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.width(size).height(size)
