package pe.edu.esan.sportpro.presentation.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Iconos de la barra de estado (hora, bateria, senal): blancos sobre fondos oscuros, oscuros sobre fondos claros. */
@Composable
fun StatusBarIcons(white: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !white
        }
    }
}