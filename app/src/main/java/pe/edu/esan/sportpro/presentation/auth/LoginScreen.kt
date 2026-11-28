package pe.edu.esan.sportpro.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.esan.sportpro.R

private val Lime = Color(0xFF8CE32B)
private val DarkText = Color(0xFF0B1F0E)

/** US-01: inicio de sesion sobre el fondo de SportPro (el logo viene en la imagen). */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onGoRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearMessages() }
    pe.edu.esan.sportpro.presentation.components.StatusBarIcons(white = true)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val topSpace = maxHeight * 0.30f
        Image(
            painter = painterResource(R.drawable.fondo_inicio),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topSpace))
            Text("Iniciar sesión", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            LoginField(email, { email = it }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)
            LoginField(
                password, { password = it }, "Contraseña", Icons.Default.Lock, KeyboardType.Password,
                visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Mostrar", color = Lime, fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            ui.errorMessage?.let {
                Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 6.dp))
            }

            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("¿Olvidaste tu contraseña?", color = Lime, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank() && !ui.loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lime, contentColor = DarkText,
                    disabledContainerColor = Lime.copy(alpha = 0.45f), disabledContentColor = DarkText.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(22.dp), color = DarkText, strokeWidth = 2.dp)
                else Text("Ingresar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("¿No tienes cuenta?", color = Color.White)
                TextButton(onClick = onGoRegister) {
                    Text("Crear cuenta", color = Lime, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** Campo transparente con linea inferior e icono, como en el diseno. */
@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.White.copy(alpha = 0.85f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
        trailingIcon = trailing,
        singleLine = true,
        visualTransformation = visual,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Lime,
            focusedIndicatorColor = Lime,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.8f)
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}