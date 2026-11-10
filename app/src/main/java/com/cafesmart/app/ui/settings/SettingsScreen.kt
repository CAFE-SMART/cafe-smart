package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ConfirmSheet
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.viewmodel.settings.NOMBRE_FALTANTE
import com.cafesmart.app.viewmodel.settings.PerfilUsuario
import kotlinx.coroutines.launch

// Pantalla "Cuenta · Ajustes" (Figma, página Flujos, marco 275:50): perfil de la cuenta, las filas de
// procesos, configuración y personas, la tarjeta "Ver resumen financiero" y "Cerrar sesión".
// Sin estado propio: recibe todo y avisa con callbacks. Las filas que aún no tienen pantalla muestran
// el aviso "próximamente" (onProximamente). El botón "Editar" del Figma no se incluye todavía.

// Borde rojo claro del botón "Cerrar sesión" (Figma).
private val BordeCerrarSesion = Color(0xFFFECACA)

/** Mensaje de las opciones que todavía no tienen pantalla. */
const val MENSAJE_PROXIMAMENTE = "Esta opción estará disponible próximamente."

@Composable
fun SettingsScreen(
    perfil: PerfilUsuario,
    mostrarConfirmacion: Boolean,
    onBack: () -> Unit,
    onGastosOperativos: () -> Unit,
    onInformacionEmpresa: () -> Unit,
    onPedirCerrarSesion: () -> Unit,
    onCancelarCierre: () -> Unit,
    onConfirmarCierre: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Reemplaza el aviso anterior si se tocan varias filas seguidas.
    val proximamente: () -> Unit = {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(MENSAJE_PROXIMAMENTE)
        }
    }

    Box(Modifier.fillMaxSize().background(SurfaceScreen)) {
        Column(Modifier.fillMaxSize()) {
            EncabezadoAjustes(onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Spacer(Modifier.height(Spacing.s4))
                TarjetaPerfil(perfil)

                TituloSeccion("Procesos operativos")
                FilaAjuste(IconoSecado, "Proceso de secado", "Tiempo y humedad", proximamente)
                FilaAjuste(IconoGastos, "Gastos operativos", "Listado y registro", onGastosOperativos)

                TituloSeccion("Configuración del negocio")
                FilaAjuste(
                    IconoEmpresa,
                    "Información de empresa",
                    // Si ya hay negocio guardado, se muestra su nombre; si no, se invita a completarlo.
                    perfil.nombreNegocio ?: "Completa los datos de tu negocio",
                    onInformacionEmpresa
                )
                FilaAjuste(AppIcons.Coffee, "Tipos de café", "Variedades registradas", proximamente)
                FilaAjuste(IconoCalidad, "Calidades de café", "Estándares de calidad", proximamente)
                FilaAjuste(AppIcons.Package, "Capacidad de bodega", "Límites de almacenamiento", proximamente)
                FilaAjuste(IconoLimites, "Límites de entrada", "Peso y precio máximo", proximamente)
                FilaAjuste(IconoGestionUsuarios, "Gestión de usuarios", "Próximamente", proximamente)

                TituloSeccion("Gestión de personas")
                FilaAjuste(IconoClientes, "Clientes registrados", "Base de datos de clientes", proximamente)
                FilaAjuste(IconoUsuariosSistema, "Usuarios del sistema", "Roles y permisos", proximamente)

                Spacer(Modifier.height(Spacing.s8))
                TarjetaResumenFinanciero(onAcceder = proximamente)

                Spacer(Modifier.height(Spacing.s8))
                BotonCerrarSesion(onClick = onPedirCerrarSesion)
                Spacer(Modifier.height(Spacing.s20))
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.s16)
        ) { datos -> AvisoProximamente(datos.visuals.message) }
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

// Barra blanca con la flecha atrás (en círculo blanco con borde) y el título centrado.
@Composable
private fun EncabezadoAjustes(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceCard)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            contentAlignment = Alignment.Center
        ) {
            BackArrow(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(SurfaceCard, CircleShape)
                    .border(BorderStroke(1.dp, BorderDefault), CircleShape)
            )
            Text(text = "Ajustes", style = AppTypography.titleScreen)
        }
        HorizontalDivider(color = BorderDefault)
    }
}

// Tarjeta de perfil en dos filas para no apilar todo en una columna:
//   [ícono] Usuario de la cuenta ........ correo
//           Nombre
//           ............................ [Compraventa]
// El correo se recorta por la mitad si no cabe ("usuariop…@correo.com") para que el dominio se siga viendo.
@Composable
private fun TarjetaPerfil(perfil: PerfilUsuario) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SurfaceSelected, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.User,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            // Fila superior: rótulo a la izquierda y correo a la derecha.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Usuario de la cuenta", style = AppTypography.caption)
                Spacer(Modifier.width(Spacing.s8))
                Text(
                    text = perfil.correo.orEmpty(),
                    style = AppTypography.caption.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            // Nombre debajo del rótulo; sin nombre se ve en gris para que no parezca un nombre real.
            Text(
                text = perfil.nombre ?: NOMBRE_FALTANTE,
                style = AppTypography.titleCard.copy(color = if (perfil.nombre == null) TextMuted else TextPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Tipo de negocio abajo a la derecha. Sin tipo no se muestra.
            perfil.tipoNegocio?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    EtiquetaTipoNegocio(it)
                }
            }
        }
    }
}

@Composable
private fun EtiquetaTipoNegocio(texto: String) {
    Row(
        modifier = Modifier
            .background(SurfaceSelected, RoundedCornerShape(percent = 50))
            .padding(horizontal = Spacing.s8, vertical = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4)
    ) {
        Icon(imageVector = IconoEmpresa, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
        Text(text = texto, style = AppTypography.pill.copy(color = Primary))
    }
}

// Aviso de "próximamente": fondo azul oscuro de la marca y texto blanco en negrita (contraste alto,
// unas 10:1), con ícono de información. Reemplaza al Snackbar por defecto, que dejaba texto gris sobre oscuro.
@Composable
private fun AvisoProximamente(mensaje: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, AppShapes.medium)
            .padding(horizontal = Spacing.s16, vertical = Spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Icon(imageVector = IconoInfo, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
        Text(text = mensaje, style = AppTypography.bodyMd.copy(color = OnPrimary, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(text = texto, style = AppTypography.labelSection, modifier = Modifier.padding(top = Spacing.s8))
}

// Fila blanca: ícono en cuadro azul claro, título y subtítulo. Toda la fila es tocable.
@Composable
private fun FilaAjuste(icono: ImageVector, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceSelected, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = titulo, style = AppTypography.valueStrong)
            Text(text = subtitulo, style = AppTypography.helper, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Tarjeta azul claro con candado: "Ver resumen financiero" y el botón "Acceder ahora".
@Composable
private fun TarjetaResumenFinanciero(onAcceder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSelected, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.medium)
            .padding(Spacing.s20),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s8)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = IconoCandado, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Text(text = "Ver resumen financiero", style = AppTypography.titleCard)
        Text(
            text = "Consulta ventas, compras y gastos con contraseña de administrador.",
            style = AppTypography.bodyMd,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.s4))
        Button(
            onClick = onAcceder,
            shape = AppShapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Acceder ahora", style = AppTypography.buttonPrimary)
        }
    }
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

// ── Íconos de línea 24x24 de esta pantalla ─────────────────
// Privados para no editar AppIcons.kt, que comparte todo el equipo. Mismo estilo: trazo 2, puntas redondas.

private fun iconoLinea(nombre: String, vararg trazos: String): ImageVector =
    ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        trazos.forEach { trazo ->
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

// Puerta con flecha.
private val IconoSalir by lazy {
    iconoLinea("Salir", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9")
}

// Viento (secado).
private val IconoSecado by lazy {
    iconoLinea(
        "Secado",
        "M17.7 7.7a2.5 2.5 0 1 1 1.8 4.3H2",
        "M9.6 4.6A2 2 0 1 1 11 8H2",
        "M12.6 19.4A2 2 0 1 0 14 16H2"
    )
}

// Recibo (gastos).
private val IconoGastos by lazy {
    iconoLinea(
        "Gastos",
        "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z",
        "M16 8H8",
        "M16 12H8",
        "M13 16H8"
    )
}

// Tienda (empresa y tipo de negocio).
private val IconoEmpresa by lazy {
    iconoLinea("Empresa", "M3 9l1.5-5h15L21 9", "M3 9h18", "M5 9v11h14V9", "M10 20v-5h4v5")
}

// Círculo con visto (calidades).
private val IconoCalidad by lazy {
    iconoLinea("Calidad", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M8.5 12l2.5 2.5 4.5-5")
}

// Líneas con una X (límites de entrada).
private val IconoLimites by lazy {
    iconoLinea("Limites", "M3 7h10", "M3 12h10", "M3 17h10", "M16 9l5 5", "M21 9l-5 5")
}

// Persona con engranaje (gestión de usuarios).
private val IconoGestionUsuarios by lazy {
    iconoLinea(
        "GestionUsuarios",
        "M10 3a4 4 0 1 1 0 8a4 4 0 1 1 0-8Z",
        "M2 21v-1a6 6 0 0 1 9-5.2",
        "M18 15a3 3 0 1 1 0 6a3 3 0 1 1 0-6Z",
        "M18 13v1", "M18 22v1", "M14 18h1", "M21 18h1"
    )
}

// Dos personas (clientes).
private val IconoClientes by lazy {
    iconoLinea(
        "Clientes",
        "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
        "M9 3a4 4 0 1 1 0 8a4 4 0 1 1 0-8Z",
        "M22 21v-2a4 4 0 0 0-3-3.87",
        "M16 3.13a4 4 0 0 1 0 7.75"
    )
}

// Carné (usuarios del sistema).
private val IconoUsuariosSistema by lazy {
    iconoLinea(
        "UsuariosSistema",
        "M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
        "M12 8a2 2 0 1 1 0 4a2 2 0 1 1 0-4Z",
        "M8 17a4 4 0 0 1 8 0"
    )
}

// Círculo con "i" (aviso).
private val IconoInfo by lazy {
    iconoLinea("Info", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M12 16v-4", "M12 8h.01")
}

// Candado (resumen financiero).
private val IconoCandado by lazy {
    iconoLinea("Candado", "M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1Z", "M8 11V7a4 4 0 0 1 8 0v4")
}

// ── Vistas previas ─────────────────────────────────────────

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SettingsScreenPreview() {
    CafesmartTheme {
        SettingsScreen(
            perfil = PerfilUsuario("Andrés Mauricio Peña López", "andres@correo.com", "Compraventa", "Café La Esperanza"),
            mostrarConfirmacion = false,
            onBack = {},
            onGastosOperativos = {},
            onInformacionEmpresa = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SettingsScreenSinNombrePreview() {
    CafesmartTheme {
        SettingsScreen(
            perfil = PerfilUsuario(nombre = null, correo = "usuarioprueba@correo.com"),
            mostrarConfirmacion = false,
            onBack = {},
            onGastosOperativos = {},
            onInformacionEmpresa = {},
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
            perfil = PerfilUsuario("Andrés Mauricio Peña López", "andres@correo.com", "Compraventa"),
            mostrarConfirmacion = true,
            onBack = {},
            onGastosOperativos = {},
            onInformacionEmpresa = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}
