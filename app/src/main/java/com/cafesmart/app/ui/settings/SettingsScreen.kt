package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ConfirmSheet
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.viewmodel.settings.PerfilUsuario

// Pantalla "Cuenta · Ajustes": tarjeta de perfil (nombre y tipo de negocio) y botón "Cerrar sesión"
// con hoja de confirmación. Sin estado propio: recibe todo y avisa con callbacks (igual que el resto
// de pantallas del proyecto). Las demás filas del Figma (secado, gastos, capacidad de bodega, límites...)
// son otras tareas.

// Borde rojo claro del botón "Cerrar sesión" (Figma).
private val BordeCerrarSesion = Color(0xFFFECACA)

@Composable
fun SettingsScreen(
    perfil: PerfilUsuario,
    mostrarConfirmacion: Boolean,
    onBack: () -> Unit,
    onPedirCerrarSesion: () -> Unit,
    onCancelarCierre: () -> Unit,
    onConfirmarCierre: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .padding(horizontal = Spacing.s20)
    ) {
        Spacer(Modifier.height(Spacing.s48))
        EncabezadoAjustes(onBack)
        Spacer(Modifier.height(Spacing.s20))
        TarjetaPerfil(perfil)
        // Empuja el botón al final de la pantalla.
        Spacer(Modifier.weight(1f))
        BotonCerrarSesion(onClick = onPedirCerrarSesion)
        Spacer(Modifier.height(Spacing.s28))
    }

    if (mostrarConfirmacion) {
        ConfirmSheet(
            icon = IconoSalir,
            iconBackground = ErrorBg,
            iconTint = ErrorIcon,
            title = "¿Cerrar sesión?",
            message = "Saldrás de tu cuenta y volverás al inicio de sesión.",
            actionText = "Sí, cerrar sesión",
            onAction = onConfirmarCierre,
            linkText = "Cancelar",
            onLink = onCancelarCierre,
            actionContainerColor = ErrorBg,
            actionContentColor = ErrorText
        )
    }
}

// Flecha atrás a la izquierda y el título centrado.
@Composable
private fun EncabezadoAjustes(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Text(text = "Ajustes", style = AppTypography.titleScreen)
    }
}

// Tarjeta con el ícono de persona, "Usuario de la cuenta", el nombre y la etiqueta del tipo de negocio.
@Composable
private fun TarjetaPerfil(perfil: PerfilUsuario) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SurfaceSelected, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.User,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = "Usuario de la cuenta", style = AppTypography.caption)
            Text(text = perfil.nombre, style = AppTypography.titleScreen)
            // Sin tipo de negocio no se muestra la etiqueta.
            perfil.tipoNegocio?.let { EtiquetaTipoNegocio(it) }
        }
    }
}

@Composable
private fun EtiquetaTipoNegocio(texto: String) {
    Text(
        text = texto,
        style = AppTypography.pill.copy(color = Primary),
        modifier = Modifier
            .background(SurfaceSelected, RoundedCornerShape(percent = 50))
            .padding(horizontal = Spacing.s12, vertical = Spacing.s4)
    )
}

// Botón blanco con borde y texto rojos: "Cerrar sesión".
@Composable
private fun BotonCerrarSesion(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = AppShapes.medium,
        border = BorderStroke(1.dp, BordeCerrarSesion),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = ErrorText),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(imageVector = IconoSalir, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(Spacing.s8))
        Text(text = "Cerrar sesión", style = AppTypography.buttonPrimary.copy(color = ErrorText), textAlign = TextAlign.Center)
    }
}

// Ícono de salida (puerta con flecha), de línea 24x24 como los de AppIcons. Es privado de esta pantalla
// para no editar AppIcons.kt, que comparte todo el equipo.
private val IconoSalir: ImageVector by lazy {
    ImageVector.Builder(
        name = "Salir",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        listOf(
            "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4",
            "M16 17l5-5-5-5",
            "M21 12H9"
        ).forEach { trazo ->
            addPath(
                pathData = addPathNodes(trazo),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SettingsScreenPreview() {
    CafesmartTheme {
        SettingsScreen(
            perfil = PerfilUsuario("Andrés Mauricio Peña López", "Compraventa"),
            mostrarConfirmacion = false,
            onBack = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SettingsScreenSinTipoPreview() {
    CafesmartTheme {
        SettingsScreen(
            perfil = PerfilUsuario("Usuario", null),
            mostrarConfirmacion = false,
            onBack = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SettingsScreenConfirmacionPreview() {
    CafesmartTheme {
        SettingsScreen(
            perfil = PerfilUsuario("Andrés Mauricio Peña López", "Compraventa"),
            mostrarConfirmacion = true,
            onBack = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}
