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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.shadow
import com.cafesmart.app.ui.auth.AuthField
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.AvisoSuperior
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.SheetShadow
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.viewmodel.settings.EdicionPerfil
import com.cafesmart.app.viewmodel.settings.NOMBRE_PERFIL_MAX
import com.cafesmart.app.viewmodel.settings.inicialesDe
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.viewmodel.settings.PestanaAjustes
import kotlinx.coroutines.launch

// Pantalla "Cuenta · Ajustes" (Figma, sección NUEVAS de Flujos): tarjeta de perfil, las 4 agrupaciones
// (Negocio, Bodega, Calidades, Usuarios) con sus opciones, la tarjeta "Ver resumen financiero" y "Cerrar sesión".
// Sin estado propio: recibe todo y avisa con callbacks. Las opciones que aún no tienen pantalla muestran
// el aviso "próximamente"; las marcadas "Próximamente" en el Figma se ven apagadas.

/** Mensaje de las opciones que todavía no tienen pantalla. */
const val MENSAJE_PROXIMAMENTE = "Esta opción estará disponible próximamente."

@Composable
fun SettingsScreen(
    perfil: PerfilUsuario,
    mostrarConfirmacion: Boolean,
    onBack: () -> Unit,
    onGastosOperativos: () -> Unit,
    onInformacionEmpresa: () -> Unit,
    onResumenFinanciero: () -> Unit,
    onCapacidadBodega: () -> Unit = {},
    onPedirCerrarSesion: () -> Unit,
    onCancelarCierre: () -> Unit,
    onConfirmarCierre: () -> Unit,
    edicion: EdicionPerfil? = null,
    onEditarPerfil: () -> Unit = {},
    onNombreChange: (String) -> Unit = {},
    onGuardarNombre: () -> Unit = {},
    onCancelarEdicion: () -> Unit = {},
    // Mensaje de éxito arriba (barra verde); null = sin aviso.
    aviso: AvisoUi? = null,
    onAvisoOculto: () -> Unit = {},
    // Agrupación visible (Negocio, Bodega, Calidades, Usuarios) y el interruptor de notificaciones.
    pestana: PestanaAjustes = PestanaAjustes.NEGOCIO,
    onPestana: (PestanaAjustes) -> Unit = {},
    notificaciones: Boolean = true,
    onNotificaciones: (Boolean) -> Unit = {},
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
                    .padding(horizontal = Spacing.s20),
                verticalArrangement = Arrangement.spacedBy(Spacing.s16)
            ) {
                TarjetaPerfil(perfil, onClick = onEditarPerfil)
                PestanasAjustes(actual = pestana, onSeleccion = onPestana)

                when (pestana) {
                    PestanaAjustes.NEGOCIO -> OpcionesNegocio(
                        nombreNegocio = perfil.nombreNegocio,
                        notificaciones = notificaciones,
                        onNotificaciones = onNotificaciones,
                        onInformacionEmpresa = onInformacionEmpresa,
                        onGastosOperativos = onGastosOperativos,
                        proximamente = proximamente
                    )
                    PestanaAjustes.BODEGA -> OpcionesBodega(onCapacidadBodega, proximamente)
                    PestanaAjustes.CALIDADES -> OpcionesCalidades(proximamente)
                    PestanaAjustes.USUARIOS -> OpcionesUsuarios(proximamente)
                }

                TarjetaResumenFinanciero(onAcceder = onResumenFinanciero)
                BotonCerrarSesion(onClick = onPedirCerrarSesion)
                Spacer(Modifier.height(Spacing.s12))
            }
        }

        // Con la hoja "Editar perfil" abierta el aviso va dentro de la hoja (una hoja oscurece lo que tiene detrás).
        AvisoSuperior(
            aviso = if (edicion == null) aviso else null,
            onOculto = onAvisoOculto,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.s16)
        ) { datos -> AvisoProximamente(datos.visuals.message) }
    }

    if (edicion != null) {
        HojaEditarPerfil(
            edicion = edicion,
            aviso = aviso,
            onAvisoOculto = onAvisoOculto,
            correo = perfil.correo,
            onNombreChange = onNombreChange,
            onGuardar = onGuardarNombre,
            onCancelar = onCancelarEdicion
        )
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

// Encabezado del Figma (guía "Encabezado de pantalla"): flecha atrás a la izquierda y título centrado, sobre el fondo.
@Composable
private fun EncabezadoAjustes(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        contentAlignment = Alignment.Center
    ) {
        BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Text(text = "Ajustes", style = AppTypography.titleScreen)
    }
}

// Las 4 agrupaciones (Figma "Ajustes / Pestañas"): Negocio, Bodega, Calidades y Usuarios. La activa va en azul.
@Composable
private fun PestanasAjustes(actual: PestanaAjustes, onSeleccion: (PestanaAjustes) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        PestanaAjustes.entries.forEach { pestana ->
            val activa = pestana == actual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (activa) Primary else SurfaceCard)
                    .border(BorderStroke(1.dp, if (activa) Primary else BorderField), RoundedCornerShape(percent = 50))
                    .selectable(selected = activa, role = Role.Tab, onClick = { onSeleccion(pestana) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pestana.etiqueta,
                    style = AppTypography.labelField.copy(color = if (activa) OnPrimary else TextMuted),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// ── Opciones de cada agrupación (textos y orden del Figma) ──

@Composable
private fun OpcionesNegocio(
    nombreNegocio: String?,
    notificaciones: Boolean,
    onNotificaciones: (Boolean) -> Unit,
    onInformacionEmpresa: () -> Unit,
    onGastosOperativos: () -> Unit,
    proximamente: () -> Unit,
) {
    TituloSeccion("Configuración del negocio")
    // Si ya hay negocio guardado, se muestra su nombre; si no, se invita a completarlo.
    FilaAjuste(
        IconoEmpresa, "Información de empresa",
        nombreNegocio ?: "Nombre, tipo y teléfono de tu negocio",
        onInformacionEmpresa
    )
    FilaAjuste(IconoGota, "Proceso de secado", "Registra el secado de tu café", proximamente)
    FilaAjuste(IconoAjustes, "Límites de entrada", "Lo mínimo y lo máximo al registrar", proximamente)
    FilaAjuste(IconoGastos, "Gastos operativos", "Mira y registra tus gastos", onGastosOperativos)
    FilaAjuste(
        IconoCampana, "Notificaciones", "Avisos cuando un sublote lleva mucho tiempo en bodega", {},
        modo = ModoFila.INTERRUPTOR, interruptor = notificaciones, onInterruptor = onNotificaciones
    )
}

@Composable
private fun OpcionesBodega(onCapacidadBodega: () -> Unit, proximamente: () -> Unit) {
    TituloSeccion("Tu bodega")
    FilaAjuste(IconoArchivo, "Capacidad de bodega", "Cuántos kilos caben y cuánto pesa un bulto", onCapacidadBodega)
    FilaAjuste(IconoEtiqueta, "Zonas de bodega", "Organiza tu espacio", proximamente, modo = ModoFila.PROXIMAMENTE)
}

@Composable
private fun OpcionesCalidades(proximamente: () -> Unit) {
    TituloSeccion("Más adelante podrás cambiar")
    FilaAjuste(AppIcons.Coffee, "Tipos de café", "Elige los tipos de café que manejas", proximamente, modo = ModoFila.PROXIMAMENTE)
    FilaAjuste(IconoEstrella, "Calidades del café", "Define los niveles de calidad", proximamente, modo = ModoFila.PROXIMAMENTE)
}

@Composable
private fun OpcionesUsuarios(proximamente: () -> Unit) {
    TituloSeccion("Gestión de personas")
    FilaAjuste(IconoClientes, "Permisos y roles", "Qué puede hacer cada trabajador", proximamente, modo = ModoFila.PROXIMAMENTE)
    FilaAjuste(AppIcons.User, "Personal", "Tus trabajadores", proximamente, modo = ModoFila.PROXIMAMENTE)
    // Contactos todavía no tiene pantalla: por ahora muestra el aviso de "próximamente".
    FilaAjuste(IconoClientes, "Clientes y productores", "Directorio de tus contactos", proximamente)
}

// Tarjeta de perfil (Figma, sección NUEVAS de Flujos): avatar con iniciales y lápiz, nombre, correo completo
// (se ajusta en varias líneas, nunca se corta) y tipo de negocio. Toda la tarjeta se toca para abrir
// "Editar perfil"; la flecha ">" de la derecha lo indica.
@Composable
private fun TarjetaPerfil(perfil: PerfilUsuario, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = AppShapes.large, ambientColor = SheetShadow, spotColor = SheetShadow)
            .background(SurfaceCard, AppShapes.large)
            .clickable(role = Role.Button, onClickLabel = "Editar perfil", onClick = onClick)
            .padding(start = Spacing.s20, top = Spacing.s20, end = Spacing.s16, bottom = Spacing.s20),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        AvatarConLapiz(inicialesDe(perfil.nombre))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            // Sin nombre se ve en gris para que no parezca un nombre real.
            Text(
                text = perfil.nombre ?: NOMBRE_FALTANTE,
                style = AppTypography.titleCard.copy(color = if (perfil.nombre == null) TextMuted else TextPrimary)
            )
            perfil.correo?.let { Text(text = it, style = AppTypography.bodyMd.copy(color = TextSecondary)) }
            perfil.tipoNegocio?.let {
                Spacer(Modifier.height(Spacing.s4))
                EtiquetaTipoNegocio(it)
            }
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

// Círculo azul claro con las iniciales (o el ícono de persona si no hay nombre) y una insignia con lápiz.
@Composable
private fun AvatarConLapiz(iniciales: String?) {
    Box(Modifier.size(68.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SurfaceSelected, CircleShape)
                .border(BorderStroke(2.dp, BorderSelectedSoft), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iniciales != null) {
                Text(text = iniciales, style = AppTypography.titleExito.copy(color = Primary))
            } else {
                Icon(AppIcons.User, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .background(Primary, CircleShape)
                .border(BorderStroke(2.dp, SurfaceCard), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(AppIcons.Pencil, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(12.dp))
        }
    }
}

// Hoja "Editar perfil": solo el nombre se cambia. El correo se muestra en una caja gris con candado
// (no se toca) y el teléfono se edita en "Información de empresa".
@Composable
private fun HojaEditarPerfil(
    edicion: EdicionPerfil,
    aviso: AvisoUi?,
    onAvisoOculto: () -> Unit,
    correo: String?,
    onNombreChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AppBottomSheet(onDismiss = onCancelar, skipPartiallyExpanded = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            // Aviso rojo (o verde) dentro de la hoja, sin la barra de estado.
            AvisoSuperior(aviso = aviso, onOculto = onAvisoOculto, bajoBarraDeEstado = false)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Text(text = "Editar perfil", style = AppTypography.titleModal)
                Text(
                    text = "Cambia el nombre con el que apareces en la app.",
                    style = AppTypography.bodyMd.copy(color = TextSecondary)
                )
            }
            AuthField(
                title = "Nombre completo",
                value = edicion.nombre,
                onValueChange = onNombreChange,
                hint = "Ej: Juan Pérez",
                help = "Máximo $NOMBRE_PERFIL_MAX caracteres.",
                error = edicion.error,
                maxChars = NOMBRE_PERFIL_MAX,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                Text(text = "Correo electrónico", style = AppTypography.labelField.copy(color = TextMuted))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(SurfaceSoft, AppShapes.medium)
                        .padding(horizontal = Spacing.s16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
                ) {
                    Text(
                        text = correo.orEmpty(),
                        style = AppTypography.bodyLg.copy(color = TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(IconoCandado, contentDescription = "No se puede cambiar", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Text(text = "Este dato no se puede cambiar.", style = AppTypography.helper.copy(color = TextMuted))
            }
            ContinueButton(
                text = if (edicion.guardando) "Guardando…" else "Guardar perfil",
                onClick = onGuardar,
                enabled = !edicion.guardando,
                cargando = edicion.guardando
            )
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Text(text = "Cancelar", style = AppTypography.buttonLink.copy(color = Primary))
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
    Text(text = texto, style = AppTypography.labelSection.copy(color = TextMuted))
}

/** Cómo se ve una fila de opción: normal (con flecha), "Próximamente" (apagada) o con interruptor. */
private enum class ModoFila { NORMAL, PROXIMAMENTE, INTERRUPTOR }

// Fila de opción (Figma "Ajustes / Fila de opción"): mosaico con ícono, título, descripción (puede ocupar dos
// líneas) y al final una flecha, la etiqueta "Próximamente" o un interruptor. Toda la fila se toca.
@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    modo: ModoFila = ModoFila.NORMAL,
    interruptor: Boolean = false,
    onInterruptor: (Boolean) -> Unit = {},
) {
    val apagada = modo == ModoFila.PROXIMAMENTE
    val forma = AppShapes.medium
    val gestos = if (modo == ModoFila.INTERRUPTOR) {
        Modifier.toggleable(value = interruptor, role = Role.Switch, onValueChange = onInterruptor)
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (apagada) SurfaceSoft else SurfaceCard, forma)
            .border(BorderStroke(1.dp, BorderDefault), forma)
            .then(gestos)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (apagada) SurfaceCard else SurfaceSoft, forma),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (apagada) TextMuted else Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = titulo, style = AppTypography.labelField.copy(color = if (apagada) TextMuted else TextPrimary))
            Text(text = subtitulo, style = AppTypography.helper.copy(color = TextMuted))
        }
        when (modo) {
            ModoFila.NORMAL -> Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            ModoFila.PROXIMAMENTE -> Text(
                text = "Próximamente",
                style = AppTypography.pill.copy(color = TextMuted),
                modifier = Modifier
                    .background(BorderDefault, RoundedCornerShape(percent = 50))
                    .padding(horizontal = Spacing.s8 + Spacing.s4 / 2, vertical = Spacing.s4)
            )
            ModoFila.INTERRUPTOR -> Switch(
                checked = interruptor,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceCard,
                    checkedTrackColor = Primary,
                    checkedBorderColor = Primary,
                    uncheckedThumbColor = SurfaceCard,
                    uncheckedTrackColor = BorderField,
                    uncheckedBorderColor = BorderField
                )
            )
        }
    }
}

// Tarjeta azul claro con candado: "Ver resumen financiero" y el botón "Acceder ahora".
@Composable
private fun TarjetaResumenFinanciero(onAcceder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSelected, AppShapes.large)
            .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.large)
            .padding(Spacing.s20),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s8)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(ButtonSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = IconoCandado, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Text(text = "Ver resumen financiero", style = AppTypography.titleCard)
        Text(
            text = "Consulta ventas, compras y gastos con tu clave financiera.",
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

// Botón rojo suave (guía: ícono rojo y botón rojo suave): "Cerrar sesión".
@Composable
private fun BotonCerrarSesion(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = AppShapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorBg, contentColor = ErrorText),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(imageVector = IconoSalir, contentDescription = null, tint = ErrorIcon, modifier = Modifier.size(20.dp))
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

// Círculo con "i" (aviso).
// Íconos de las filas nuevas (Lucide): gota, ajustes (controles), caja de archivo, etiqueta, estrella y campana.
private val IconoGota by lazy {
    iconoLinea("Gota", "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
}

private val IconoAjustes by lazy {
    iconoLinea("Ajustes", "M21 4h-7", "M10 4H3", "M21 12h-9", "M8 12H3", "M21 20h-5", "M12 20H3", "M14 2v4", "M8 10v4", "M16 18v4")
}

private val IconoArchivo by lazy {
    iconoLinea("Archivo", "M21 8v13H3V8", "M1 3h22v5H1z", "M10 12h4")
}

private val IconoEtiqueta by lazy {
    iconoLinea(
        "Etiqueta",
        "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z",
        "M7.5 7.5h.01"
    )
}

private val IconoEstrella by lazy {
    iconoLinea("Estrella", "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
}

private val IconoCampana by lazy {
    iconoLinea("Campana", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
}

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
            onResumenFinanciero = {},
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
            onResumenFinanciero = {},
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
            onResumenFinanciero = {},
            onPedirCerrarSesion = {},
            onCancelarCierre = {},
            onConfirmarCierre = {}
        )
    }
}
