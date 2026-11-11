package com.cafesmart.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.PieDeAccion
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessSolid
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextSecondary

// Pantalla "Cuenta · Recuperar contraseña" (Figma, sección NUEVAS de la página Flujos). Tiene dos estados:
//   1. Correo: se escribe el correo y se pide el enlace.
//   2. Enlace enviado: "Revisa tu correo", con aviso de spam y "Reenviar enlace".
// Sin estado propio: recibe el estado y avisa con callbacks. El ícono y el título van centrados y las
// descripciones a la izquierda, como en el diseño.

@Composable
fun RecuperarContrasenaScreen(
    estado: RecuperarContrasenaUiState,
    onBack: () -> Unit,
    onCorreoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onReenviar: () -> Unit,
    onVolverAlLogin: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SurfaceScreen).imePadding()) {
        Encabezado(onBack)
        if (estado.enviadoA == null) {
            FormularioCorreo(estado, onCorreoChange, onEnviar, onVolverAlLogin, Modifier.weight(1f))
        } else {
            EnlaceEnviado(estado, onReenviar, onVolverAlLogin, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FormularioCorreo(
    estado: RecuperarContrasenaUiState,
    onCorreoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onVolverAlLogin: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s28),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(ButtonSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(IconoCorreo, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = AppTypography.titleModal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Escribe el correo de tu cuenta. Te enviaremos un enlace para crear una contraseña nueva.",
                    style = AppTypography.bodyMd.copy(color = TextSecondary)
                )
            }
            AuthField(
                title = "Correo electrónico",
                value = estado.correo,
                onValueChange = onCorreoChange,
                hint = "ejemplo@correo.com",
                help = "Es el correo con el que creaste tu cuenta.",
                error = estado.error,
                keyboardType = KeyboardType.Email,
                maxChars = MAX_CORREO,
                modifier = Modifier.fillMaxWidth()
            )
        }
        PieDeAccion(
            textoBoton = if (estado.enviando) "Enviando…" else "Enviar enlace",
            botonActivo = !estado.enviando,
            cargando = estado.enviando,
            onBoton = onEnviar,
            textoEnlace = "Volver a iniciar sesión",
            onEnlace = onVolverAlLogin
        )
    }
}

@Composable
private fun EnlaceEnviado(
    estado: RecuperarContrasenaUiState,
    onReenviar: () -> Unit,
    onVolverAlLogin: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s28),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(SuccessSolid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(40.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Text(
                    text = "Revisa tu correo",
                    style = AppTypography.titleModal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // Con la protección de Firebase no se sabe si el correo existe: la frase es verdadera en ambos casos.
                Text(
                    text = "Si ${estado.enviadoA} tiene una cuenta, le enviamos un enlace. " +
                        "Ábrelo para crear tu contraseña nueva.",
                    style = AppTypography.bodyMd.copy(color = TextSecondary)
                )
            }
            Text(
                text = "¿No lo ves? Revisa la carpeta de spam. El enlace funciona solo una vez.",
                style = AppTypography.bodyMd,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSelected, AppShapes.medium)
                    .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.medium)
                    .padding(Spacing.s16)
            )
            estado.error?.let {
                Text(text = it, style = AppTypography.helper.copy(color = ErrorText))
            }
        }
        PieDeAccion(
            textoBoton = "Volver a iniciar sesión",
            botonActivo = true,
            onBoton = onVolverAlLogin,
            textoEnlace = if (estado.enviando) "Enviando…" else "Reenviar enlace",
            onEnlace = { if (!estado.enviando) onReenviar() }
        )
    }
}

// Barra blanca con la flecha atrás y el título centrado, igual que las demás pantallas.
@Composable
private fun Encabezado(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceCard)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            contentAlignment = Alignment.Center
        ) {
            BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
            Text(text = "Recuperar contraseña", style = AppTypography.titleScreen)
        }
        HorizontalDivider(color = BorderDefault)
    }
}

// Sobre (Lucide "mail"), privado para no editar AppIcons.kt, que comparte todo el equipo.
private val IconoCorreo: ImageVector by lazy {
    ImageVector.Builder(
        name = "Correo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(
            "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
            "M22 7l-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"
        ).forEach {
            addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecuperarContrasenaCorreoPreview() {
    CafesmartTheme {
        RecuperarContrasenaScreen(RecuperarContrasenaUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecuperarContrasenaErrorPreview() {
    CafesmartTheme {
        RecuperarContrasenaScreen(
            RecuperarContrasenaUiState(correo = "ana@", error = ERROR_CORREO_INVALIDO),
            {}, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecuperarContrasenaEnviadoPreview() {
    CafesmartTheme {
        RecuperarContrasenaScreen(
            RecuperarContrasenaUiState(correo = "ana@cafe.com", enviadoA = "ana@cafe.com"),
            {}, {}, {}, {}, {}
        )
    }
}
