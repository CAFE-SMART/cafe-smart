package com.cafesmart.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafesmart.app.R
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.*

/** Presentación adaptable; estado, validaciones y acciones siguen en el flujo de Auth. */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,
    onSubmit: () -> Unit,
    onRegister: () -> Unit,
    onGoogle: () -> Unit,
    onForgotPassword: () -> Unit,
    onHelp: () -> Unit,
    onSupport: () -> Unit,
    onExit: () -> Unit,
    isSubmitting: Boolean = false
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    // El botón compartido tiene 54 dp por defecto. La restricción exterior deja
    // crecer su altura con la fuente del sistema sin cambiar otros módulos.
    val actionHeight = maxOf(54.dp, with(LocalDensity.current) {
        AppTypography.buttonPrimary.fontSize.toDp() * 1.5f + 24.dp
    })
    Box(Modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding().imePadding()) {
        // Sin altura fija: scroll también para teclado, errores y letra ampliada.
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().align(Alignment.TopCenter)
                .testTag("login_scroll").verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(16.dp))
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Una sola marca: se elimina el segundo logo de 138 dp de la tarjeta.
                Image(painterResource(R.drawable.splash_cafe), "Logo Café Smart", Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Café Smart", style = AppTypography.titleScreen.copy(fontSize = 20.sp),
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = ErrorBg)) {
                    Text("Salir", color = ErrorText, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(14.dp))
                    .background(SurfaceCard, RoundedCornerShape(14.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Iniciar Sesión", style = AppTypography.titleExito.copy(fontSize = 22.sp),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("Bienvenido a Café Smart", style = AppTypography.bodyMd, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                AuthField(
                    title = "Correo electrónico", value = state.loginEmail,
                    onValueChange = { value -> onChange { it.copy(loginEmail = value) } },
                    hint = "ejemplo@correo.com", help = "El correo con el que te registraste.",
                    maxChars = MAX_CORREO, error = state.errors["loginEmail"],
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(8.dp))
                AuthField(
                    title = "Contraseña", value = state.loginPassword,
                    onValueChange = { value -> onChange { it.copy(loginPassword = value) } },
                    hint = "********", help = "La que creaste al registrarte.",
                    maxChars = MAX_CONTRASENA, error = state.errors["loginPassword"],
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = { PasswordEye(showPassword) { showPassword = !showPassword } }
                )
                LoginLink("¿Olvidaste tu contraseña?", onForgotPassword, Modifier.align(Alignment.End))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(value = state.rememberMe, role = Role.Checkbox,
                            onValueChange = { checked -> onChange { it.copy(rememberMe = checked) } }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = state.rememberMe, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Primary))
                    Text("Recordar mi cuenta", style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ContinueButton(
                    text = if (isSubmitting) "Ingresando..." else "Entrar",
                    enabled = !isSubmitting, cargando = isSubmitting, onClick = onSubmit,
                    modifier = Modifier.heightIn(min = actionHeight)
                )
                Spacer(Modifier.height(16.dp))
                GoogleDivider()
                Spacer(Modifier.height(8.dp))
                // Solo Login usa altura flexible; GoogleButton compartido no se modifica.
                OutlinedButton(onClick = onGoogle,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard)) {
                    Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4285F4))
                    Spacer(Modifier.width(12.dp))
                    Text("Continuar con Google", color = TextStrong,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                // Los enlaces pasan a otra fila si el ancho o la fuente lo requieren.
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("¿No tienes una cuenta?", style = AppTypography.bodyMd,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    LoginLink("Regístrate gratis", onRegister)
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    LoginLink("Ver ayuda", onHelp)
                    LoginLink("Contactar soporte", onSupport)
                }
            }
        }
    }
}

/** Zonas táctiles de al menos 48 dp; sin fijar la altura de textos que pueden crecer. */
@Composable
private fun LoginLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = AppTypography.buttonLink, textAlign = TextAlign.Center)
    }
}

@Preview(name = "Login 390 dp", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Login 360 dp", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Login fuente ampliada", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun LoginCafeSmartPreview() {
    CafesmartTheme {
        LoginScreen(AuthUiState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}