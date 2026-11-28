package pe.edu.esan.sportpro.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/** Permite abrir el menu lateral desde una pantalla sin barra superior (ej. Inicio). */
val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }