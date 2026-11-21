package pe.edu.esan.sportpro.presentation.tryouts

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.navigation.Secured

/** Grafo de navegación para Convocatorias Abiertas y Pruebas (US-10). */
fun NavGraphBuilder.tryoutsGraph(sec: SecuredContext) {
    val nav = sec.navController
    composable(Routes.TRYOUTS) {
        sec.Secured("Pruebas y Convocatorias", allowed = setOf(Role.DT, Role.JUG, Role.PAD)) { u ->
            TryoutListScreen(user = u, onNavigate = { route -> nav.navigate(route) })
        }
    }
    composable(
        Routes.TRYOUT_FORM,
        arguments = listOf(navArgument("tryoutId") { type = NavType.StringType; nullable = true; defaultValue = null })
    ) { backStackEntry ->
        val tryoutId = backStackEntry.arguments?.getString("tryoutId")
        sec.Secured("Formulario de Convocatoria", allowed = setOf(Role.DT)) { u ->
            TryoutFormScreen(user = u, editTryoutId = tryoutId, onBack = { nav.popBackStack() })
        }
    }
    composable(
        Routes.TRYOUT_CANDIDATES,
        arguments = listOf(navArgument("tryoutId") { type = NavType.StringType })
    ) { backStackEntry ->
        val tryoutId = backStackEntry.arguments?.getString("tryoutId").orEmpty()
        sec.Secured("Candidatos Interesados", allowed = setOf(Role.DT)) { u ->
            TryoutCandidatesScreen(user = u, tryoutId = tryoutId, onBack = { nav.popBackStack() })
        }
    }
}
