package pe.edu.esan.sportpro.presentation.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
internal fun TeamEmblem(image: Any, modifier: Modifier = Modifier) {
    AsyncImage(
        model = image,
        contentDescription = "Emblema del equipo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
