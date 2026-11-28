package pe.edu.esan.sportpro.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.navigation.LocalOpenDrawer
import pe.edu.esan.sportpro.presentation.navigation.Routes

private data class Shortcut(val title: String, val subtitle: String, val route: String, val icon: ImageVector)

private val Lime = Color(0xFF8CE32B)
private val GreenDark = Color(0xFF0B4D1E)
private val GreenMid = Color(0xFF2E7D32)
private val TextDark = Color(0xFF12301A)

/** Inicio segun rol: encabezado con fondo de cancha y accesos directos a las funciones autorizadas. */
@Composable
fun HomeScreen(user: UserModel, onNavigate: (String) -> Unit) {
    val trainingsShortcut = Shortcut("Entrenamientos", "Planifica y consulta las sesiones de entrenamiento", Routes.TRAININGS, Icons.Default.DateRange)
    val matchesShortcut = Shortcut(
        stringResource(R.string.home_matches_shortcut_title),
        stringResource(R.string.home_matches_shortcut_subtitle),
        Routes.MATCHES,
        Icons.Default.Place
    )
    val announcementsShortcut = Shortcut("Anuncios", "Avisos del equipo y de la academia", Routes.ANNOUNCEMENTS, Icons.Default.Notifications)
    val tryoutsShortcut = Shortcut("Pruebas y Convocatorias", "Avisos de pruebas de selección y convocatorias abiertas", Routes.TRYOUTS, Icons.Default.DateRange)
    val communityShortcut = Shortcut("Comunidad", "Publicaciones de tu equipo y la academia", Routes.COMMUNITY, Icons.Default.Favorite)
    val summariesShortcut = Shortcut("Resúmenes de partidos", "Crónicas generadas con IA y revisadas por el DT", Routes.SUMMARIES, Icons.Default.Star)
    val userAttendanceShortcut = Shortcut("Mi Asistencia", "Porcentaje de asistencia y desglose de sesiones", Routes.USER_ATTENDANCE_HISTORY, Icons.Default.CheckCircle)

    val shortcuts = when (user.roleEnum) {
        Role.ADM -> listOf(Shortcut("Validar equipos", "Aprueba o rechaza equipos registrados", Routes.TEAM_APPROVAL, Icons.Default.CheckCircle))
        Role.DT -> listOf(
            Shortcut("Mis equipos", "Registra equipos y gestiona tu plantel", Routes.MY_TEAMS, Icons.AutoMirrored.Filled.List),
            matchesShortcut, trainingsShortcut, tryoutsShortcut, announcementsShortcut, communityShortcut, summariesShortcut
        )
        Role.PAD -> listOf(
            Shortcut("Mis hijos", "Registra a tus hijos y vincúlalos a su equipo", Routes.MY_CHILDREN, Icons.Default.Face),
            tryoutsShortcut, matchesShortcut, trainingsShortcut, userAttendanceShortcut, announcementsShortcut, communityShortcut, summariesShortcut
        )
        Role.JUG -> listOf(
            tryoutsShortcut, matchesShortcut, trainingsShortcut, userAttendanceShortcut, announcementsShortcut, communityShortcut, summariesShortcut
        )
        null -> emptyList()
    }

    val openDrawer = LocalOpenDrawer.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F6))

    ) {
        HomeHeader(user, onMenu = openDrawer)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            shortcuts.forEach { sc -> ShortcutCard(sc) { onNavigate(sc.route) } }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Encabezado: fondo de cancha, menu, logo, avatar y saludo, con borde curvo en verde lima. */
@Composable
private fun HomeHeader(user: UserModel, onMenu: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(270.dp)) {
        Image(
            painter = painterResource(R.drawable.fondo_inicio),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xE6031A10), Color(0x66031A10), Color.Transparent))
            )
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Sport") }
                        withStyle(SpanStyle(color = Lime)) { append("Pro") }
                    },
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Lime, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = GreenDark, modifier = Modifier.size(46.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Hola, ${user.name}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(3.dp).height(20.dp).background(Lime))
                        Spacer(Modifier.width(8.dp))
                        Text(user.roleEnum?.label ?: "", color = Color.White, fontSize = 17.sp)
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(44.dp).align(Alignment.BottomCenter)) {
            val w = size.width
            val h = size.height
            val curve = Path().apply {
                moveTo(0f, h * 0.55f)
                cubicTo(w * 0.35f, h * 1.05f, w * 0.7f, h * 0.05f, w, h * 0.15f)
            }
            val fill = Path().apply {
                addPath(curve)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(fill, Color(0xFFF7F8F6))
            drawPath(curve, Lime, style = Stroke(width = 5.dp.toPx()))
        }
    }
}

@Composable
private fun ShortcutCard(sc: Shortcut, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(GreenMid, GreenDark), start = Offset.Zero, end = Offset.Infinite)),
                contentAlignment = Alignment.Center
            ) {
                Icon(sc.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(sc.title, color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(sc.subtitle, color = Color(0xFF3C4A40), fontSize = 13.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Lime, modifier = Modifier.size(30.dp))
        }
    }
}