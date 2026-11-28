package pe.edu.esan.sportpro.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R

val SportProLime = Color(0xFF8BC53F)

/** Pantalla de inicio con el fondo de SportPro mientras se verifica la sesion. */
@Composable
fun SplashScreen() {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(900)) }
    pe.edu.esan.sportpro.presentation.components.StatusBarIcons(white = true)

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fondo_inicio),
            contentDescription = "SportPro",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).alpha(alpha.value)
        ) {
            Text(
                "Conectamos entrenadores, jugadores y familias",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            CircularProgressIndicator(
                color = SportProLime,
                strokeWidth = 3.dp,
                modifier = Modifier.padding(top = 24.dp).size(32.dp)
            )
        }
    }
}