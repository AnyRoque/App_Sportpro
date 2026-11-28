package pe.edu.esan.sportpro.presentation.stats

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.Secured
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext

/**
 * US-15: Módulo de navegación para la Consulta de Estadísticas Acumuladas (JUG / PAD / DT).
 */
fun NavGraphBuilder.statsGraph(sec: SecuredContext) {
    composable(Routes.STATS) {
        val viewModel: StatsViewModel = viewModel()
        sec.Secured("Estadísticas Acumuladas", allowed = setOf(Role.JUG, Role.PAD, Role.DT), showBack = true) { user ->
            StatsScreen(
                user = user,
                viewModel = viewModel,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }
}
