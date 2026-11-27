package com.cafesmart.app.ui.inventory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.cafesmart.app.viewmodel.inventory.ResumenCalidad
import com.cafesmart.app.data.model.Calidad
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.foundation.BorderStroke
import com.cafesmart.app.ui.theme.PrimaryDark
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.ui.components.AppBottomBar
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDashed
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Inter
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.TextStrong
import com.cafesmart.app.ui.theme.TrilladoIcon
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.inventory.EstadoBodega
import com.cafesmart.app.viewmodel.inventory.InventoryUiState
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.ResumenInventario
import com.cafesmart.app.viewmodel.inventory.ResumenTipo
import com.cafesmart.app.viewmodel.inventory.TIPOS_EN_INVENTARIO
import com.cafesmart.app.viewmodel.inventory.detallePeso
import com.cafesmart.app.viewmodel.inventory.etiquetaEstadoBodega
import com.cafesmart.app.viewmodel.inventory.formatoBultos
import com.cafesmart.app.viewmodel.inventory.formatoPorcentaje
import com.cafesmart.app.viewmodel.inventory.formatoSublotes
import com.cafesmart.app.viewmodel.inventory.resumirInventario
import com.cafesmart.app.viewmodel.purchase.formatoKg

// Pantalla "Inventario · Resumen (Todos)" y "Inventario · Vacío" del Figma (ruta `inventory`).
// Sin estado propio: recibe el estado y avisa con callbacks (igual que SettingsScreen). Los datos salen
// de InventoryViewModel, que los lee de Room (InventoryRepository) y se actualiza solo.
// Las pantallas siguientes (calidades de un tipo, sublotes, detalle) son otras tareas: aquí solo se avisa
// con onTipoClick cuál tipo se tocó.

// "Inventario" a 28 sp negrita (Figma). displayValor ya tiene ese tamaño pero en azul; aquí va en el texto principal.
private val TituloPantalla = AppTypography.displayValor.copy(color = TextPrimary)

// Estilos de 13 sp que el Figma usa y AppTypography no trae.
private val Texto13Medio = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp)
private val Texto14Menu = TextStyle(fontFamily = Inter, fontSize = 14.sp)

/**
 * Colores del menú "Más reciente / Más antiguo". Fondo blanco opaco con borde, texto oscuro y la opción elegida
 * en azul de la marca sobre azul muy claro (más un check). Contraste (WCAG): texto 17,9:1 sobre blanco y la
 * elegida 9,5:1 sobre su fondo; ambos pasan AA (4,5:1). Lo comprueba MenuOrdenContrasteTest.
 */
internal object ColoresMenuOrden {
    val fondo = SurfaceCard
    val borde = BorderDefault
    val texto = TextPrimary
    val fondoElegido = SurfaceSelected
    val textoElegido = PrimaryDark
}
private val Texto13Semi = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

private val FormaTarjetaTipo = RoundedCornerShape(14.dp)

@Composable
fun InventoryScreen(
    estado: InventoryUiState,
    onTipoClick: (TipoCafe) -> Unit,
    onOrdenChange: (OrdenInventario) -> Unit,
    onAjustarBodega: () -> Unit,
    onRegistrarCompra: () -> Unit,
    onNavegar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReintentar: () -> Unit = {},
    // "Inventario · Lista · <tipo>" (Figma): la misma pantalla con la pestaña del tipo elegida y sus calidades.
    // null = "Resumen (Todos)".
    tipoSeleccionado: TipoCafe? = null,
    onTodosClick: () -> Unit = {},
    onCalidadClick: (ResumenCalidad) -> Unit = {},
    // Solo el café verde se manda a secar (botón "Iniciar secado" del Figma).
    onIniciarSecado: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = SurfaceScreen,
        // Misma barra inferior que Inicio; aquí la pestaña activa es Inventario (el botón central).
        bottomBar = { AppBottomBar(selectedRoute = "inventario", onNavigate = onNavegar) }
    ) { innerPadding ->
        if (estado.error != null) {
            // Room falló o trajo un dato que no se pudo leer: se avisa en vez de mostrar un inventario incompleto.
            EstadoError(
                mensaje = estado.error,
                onReintentar = onReintentar,
                modifier = Modifier.padding(innerPadding)
            )
        } else if (estado.cargando) {
            // Room todavía no entregó la primera lectura: no se muestra nada para que no parpadee el
            // estado vacío ("no tienes café") a quien sí tiene. Dura unos milisegundos.
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else if (estado.resumen.estaVacio) {
            EstadoVacio(
                onRegistrarCompra = onRegistrarCompra,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = Spacing.s20,
                    end = Spacing.s20,
                    top = Spacing.s48,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.s16)
            ) {
                item(key = "titulo") {
                    Text(text = "Inventario", style = TituloPantalla)
                }
                item(key = "resumen") {
                    ResumenBodega(resumen = estado.resumen, onAjustarBodega = onAjustarBodega)
                }
                item(key = "filtro") {
                    FiltroPorTipo(seleccionado = tipoSeleccionado, onTipoClick = onTipoClick, onTodosClick = onTodosClick)
                }
                if (tipoSeleccionado == null) {
                    item(key = "encabezado") {
                        EncabezadoLista(orden = estado.orden, onOrdenChange = onOrdenChange)
                    }
                    items(items = estado.resumen.tipos, key = { it.tipo.name }) { fila ->
                        TarjetaTipo(fila = fila, pesoBultoKg = estado.resumen.pesoBultoKg, onClick = { onTipoClick(fila.tipo) })
                    }
                } else {
                    if (tipoSeleccionado == TipoCafe.VERDE) item(key = "secado") {
                        BotonIniciarSecado(onIniciarSecado)
                    }
                    item(key = "encabezado_tipo") {
                        EncabezadoLista(
                            orden = estado.orden, onOrdenChange = onOrdenChange,
                            titulo = "Café ${tipoSeleccionado.etiqueta.lowercase()} por calidad",
                        )
                    }
                    val calidades = estado.calidadesDe(tipoSeleccionado)
                    if (calidades.isEmpty()) item(key = "sin_calidades") {
                        // Texto propuesto (pendiente del equipo): el tipo todavía no tiene compras.
                        Text(
                            text = "Aún no tienes café ${tipoSeleccionado.etiqueta.lowercase()} en inventario.",
                            style = AppTypography.bodyMd, color = TextSecondary,
                            modifier = Modifier.testTag("inventario_tipo_vacio"),
                        )
                    }
                    items(items = calidades, key = { it.lote.id }) { fila ->
                        TarjetaCalidad(fila = fila, pesoBultoKg = estado.resumen.pesoBultoKg, onClick = { onCalidadClick(fila) })
                    }
                }
            }
        }
    }
}

// ---------------- Resumen de la bodega ----------------

// Tarjeta blanca: total en bodega, anillo de capacidad y estado ("Capacidad normal" + "Ajustar bodega").
@Composable
private fun ResumenBodega(resumen: ResumenInventario, onAjustarBodega: () -> Unit) {
    val estadoBodega = resumen.ocupacion.estado
    val colorEstado = colorDelEstado(estadoBodega)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.large)
            .border(1.dp, BorderDefault, AppShapes.large)
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s16, bottom = Spacing.s8),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = "Total en bodega", style = Texto13Medio, color = TextMuted)
                Text(
                    text = formatoKg(resumen.totalKg),
                    style = AppTypography.displayValor.copy(fontSize = 32.sp, color = TextPrimary)
                )
                // Sin capacidad configurada no hay "de X kg": solo se muestran los bultos.
                val detalle = if (resumen.capacidadKg > 0) {
                    "de ${formatoKg(resumen.capacidadKg)} · ${formatoBultos(resumen.totalKg, resumen.pesoBultoKg)}"
                } else {
                    formatoBultos(resumen.totalKg, resumen.pesoBultoKg)
                }
                Text(text = detalle, style = AppTypography.bodyMd)
            }
            AnilloCapacidad(porcentaje = resumen.ocupacion.porcentaje, color = colorEstado)
        }

        HorizontalDivider(color = BorderDefault)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s8)
            ) {
                Box(modifier = Modifier.size(10.dp).background(colorEstado, CircleShape))
                Text(
                    text = etiquetaEstadoBodega(estadoBodega),
                    style = AppTypography.labelField.copy(color = TextPrimary)
                )
            }
            // Zona táctil de 44 dp de alto (guía de diseño), aunque el texto sea pequeño.
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clickable(role = Role.Button, onClick = onAjustarBodega)
                    .padding(start = Spacing.s12, end = Spacing.s4),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Ajustar bodega", style = AppTypography.buttonLink)
            }
        }
    }
}

// Punto y anillo del estado: verde normal, amarillo alto, rojo crítico, gris sin capacidad configurada.
private fun colorDelEstado(estado: EstadoBodega): Color = when (estado) {
    EstadoBodega.NORMAL -> SuccessDot
    EstadoBodega.ADVERTENCIA -> WarningDot
    EstadoBodega.CRITICA -> ErrorDot
    EstadoBodega.SIN_CAPACIDAD -> TextMuted
}

// Anillo de 84 dp: pista gris y arco de color que empieza arriba y avanza en sentido horario.
// El arco se limita a 100 %, pero el número muestra el porcentaje real (puede pasar de 100).
@Composable
private fun AnilloCapacidad(porcentaje: Double, color: Color, modifier: Modifier = Modifier) {
    val texto = formatoPorcentaje(porcentaje)
    Box(
        modifier = modifier
            .size(84.dp)
            // Lector de pantalla: una sola frase en vez del "28%" suelto.
            .clearAndSetSemantics { contentDescription = "Bodega ocupada al $texto" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 8.dp.toPx()
            val diametro = size.minDimension - grosor
            val esquina = Offset(grosor / 2, grosor / 2)
            val area = Size(diametro, diametro)

            // Pista (círculo completo).
            drawArc(
                color = BorderDefault,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = esquina,
                size = area,
                style = Stroke(width = grosor)
            )
            // Progreso: -90° es arriba (las 12 en punto).
            val barrido = (porcentaje.coerceIn(0.0, 100.0) / 100.0 * 360.0).toFloat()
            if (barrido > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = barrido,
                    useCenter = false,
                    topLeft = esquina,
                    size = area,
                    style = Stroke(width = grosor, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = texto,
            style = AppTypography.titleScreen.copy(fontWeight = FontWeight.Bold)
        )
    }
}

// ---------------- Filtro por tipo ----------------

// Fila de pestañas "Todos · Verde · Seco · Trillado · Pasilla". En el Figma cada una abre la lista de ese
// tipo (otra pantalla), por eso solo avisa con onTipoClick. `seleccionado = null` marca "Todos".
@Composable
private fun FiltroPorTipo(
    seleccionado: TipoCafe?,
    onTipoClick: (TipoCafe) -> Unit,
    onTodosClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSoft, AppShapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OpcionFiltro(
            texto = "Todos",
            activa = seleccionado == null,
            onClick = onTodosClick,
            modifier = Modifier.weight(1f)
        )
        TIPOS_EN_INVENTARIO.forEach { tipo ->
            OpcionFiltro(
                texto = tipo.etiqueta,
                activa = seleccionado == tipo,
                onClick = { onTipoClick(tipo) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OpcionFiltro(
    texto: String,
    activa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(forma)
            .background(if (activa) TextPrimary else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = if (activa) Texto13Semi else Texto13Medio,
            color = if (activa) OnPrimary else TextSecondary,
            maxLines = 1
        )
    }
}

// ---------------- Lista por tipo ----------------

// "Por tipo de café" a la izquierda y el botón de orden a la derecha.
@Composable
private fun EncabezadoLista(
    orden: OrdenInventario,
    onOrdenChange: (OrdenInventario) -> Unit,
    titulo: String = "Por tipo de café",
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = AppTypography.labelSection,
            modifier = Modifier.weight(1f)
        )
        Box {
            val forma = RoundedCornerShape(10.dp)
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(forma)
                    .background(SurfaceCard)
                    .border(1.dp, BorderField, forma)
                    .clickable(role = Role.Button) { menuAbierto = true }
                    .padding(horizontal = Spacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s6)
            ) {
                Text(text = orden.etiqueta, style = Texto13Medio, color = TextStrong)
                Icon(
                    imageVector = AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = TextStrong,
                    modifier = Modifier.size(14.dp)
                )
            }
            // El Figma solo muestra el botón cerrado; las opciones del menú son propuesta del equipo de código.
            // Colores propios a propósito: con los del tema el menú salía ilegible (fondo lila y texto blanco),
            // porque Material usa surfaceContainer (no definido en el tema: queda el lila por defecto) y
            // typography.labelLarge (= buttonPrimary, que trae color blanco fijo). Ver ColoresMenuOrden.
            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false },
                shape = forma,
                containerColor = ColoresMenuOrden.fondo,
                border = BorderStroke(1.dp, ColoresMenuOrden.borde),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("inventario_orden_menu"),
            ) {
                OrdenInventario.entries.forEach { opcion ->
                    val elegida = opcion == orden
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opcion.etiqueta,
                                style = Texto14Menu,
                                fontWeight = if (elegida) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (elegida) ColoresMenuOrden.textoElegido else ColoresMenuOrden.texto,
                                maxLines = 1,
                            )
                        },
                        trailingIcon = if (elegida) {
                            { Icon(AppIcons.Check, contentDescription = null, tint = ColoresMenuOrden.textoElegido, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            menuAbierto = false
                            onOrdenChange(opcion)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = ColoresMenuOrden.texto,
                            trailingIconColor = ColoresMenuOrden.textoElegido,
                        ),
                        modifier = Modifier
                            .background(if (elegida) ColoresMenuOrden.fondoElegido else ColoresMenuOrden.fondo)
                            .semantics { selected = elegida }
                            .testTag("inventario_orden_${opcion.name}"),
                    )
                }
            }
        }
    }
}

// Tarjeta de un tipo de café: ícono, nombre, "290 kg · 7,1 bultos", etiqueta de sublotes y flecha.
@Composable
private fun TarjetaTipo(fila: ResumenTipo, pesoBultoKg: Double, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaTarjetaTipo)
            .background(SurfaceCard)
            .border(1.dp, BorderDefault, FormaTarjetaTipo)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceSoft, AppShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconoDelTipo(fila.tipo),
                contentDescription = null,
                tint = colorDelTipo(fila.tipo),
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = fila.tipo.etiqueta, style = AppTypography.titleCard)
            Text(text = detallePeso(fila.pesoKg, pesoBultoKg), style = AppTypography.bodyMd)
        }
        EtiquetaSublotes(texto = formatoSublotes(fila.cantidadSublotes))
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// Tarjeta de una calidad (Figma "Lista · Verde"): punto de color, "Calidad buena", "60 kg · 1,5 bultos", sublotes y flecha.
@Composable
private fun TarjetaCalidad(fila: ResumenCalidad, pesoBultoKg: Double, onClick: () -> Unit) {
    val punto = when (fila.calidad) {
        Calidad.BUENA -> SuccessDot
        Calidad.REGULAR -> WarningDot
        Calidad.MALA -> ErrorDot
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaTarjetaTipo)
            .background(SurfaceCard)
            .border(1.dp, BorderDefault, FormaTarjetaTipo)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("inventario_calidad_${fila.calidad.name}")
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                Box(Modifier.size(8.dp).background(punto, CircleShape))
                Text(text = "Calidad ${fila.calidad.etiqueta.lowercase()}", style = Texto13Semi, color = TextStrong)
            }
            Text(text = detallePeso(fila.pesoKg, pesoBultoKg), style = AppTypography.titleCard)
        }
        EtiquetaSublotes(texto = formatoSublotes(fila.cantidadSublotes))
        Icon(imageVector = AppIcons.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

// Botón azul "Iniciar secado" (Figma "Lista · Verde").
@Composable
private fun BotonIniciarSecado(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(AppShapes.medium)
            .background(Primary)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("inventario_iniciar_secado"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = AppIcons.Sun, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
        Text(text = "Iniciar secado", style = AppTypography.valueStrong, color = OnPrimary, modifier = Modifier.padding(start = Spacing.s8))
    }
}

// Etiqueta gris "4 sublotes" / "1 sublote" / "Sin sublotes".
@Composable
private fun EtiquetaSublotes(texto: String) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .background(SurfaceSoft, AppShapes.medium)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = AppTypography.helper.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary),
            maxLines = 1
        )
    }
}

private fun iconoDelTipo(tipo: TipoCafe): ImageVector = when (tipo) {
    TipoCafe.VERDE -> AppIcons.Sprout
    TipoCafe.SECO -> AppIcons.Sun
    TipoCafe.TRILLADO -> AppIcons.Coffee
    TipoCafe.PASILLA -> AppIcons.Package
}

private fun colorDelTipo(tipo: TipoCafe): Color = when (tipo) {
    TipoCafe.VERDE -> SuccessIcon
    TipoCafe.SECO -> WarningText
    TipoCafe.TRILLADO -> TrilladoIcon
    TipoCafe.PASILLA -> TextStrong
}

// ---------------- Estado vacío ----------------

// "Inventario · Vacío": título arriba y, en el centro del resto de la pantalla, ilustración, mensaje y botón.
// Mismo patrón que el error de Resumen financiero: mensaje en rojo y "Reintentar".
@Composable
private fun EstadoError(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s48, bottom = Spacing.s16)
    ) {
        Text(text = "Inventario", style = TituloPantalla)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mensaje,
                    style = AppTypography.bodyMd.copy(color = ErrorText),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onReintentar) {
                    Text("Reintentar", style = AppTypography.buttonLink.copy(color = Primary))
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio(onRegistrarCompra: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s48, bottom = Spacing.s16)
    ) {
        Text(text = "Inventario", style = TituloPantalla)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IlustracionVacia()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.s8)
                ) {
                    Text(
                        text = "Aún no tienes café en inventario",
                        style = AppTypography.titleModal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.s16)
                    )
                    Text(
                        text = "Registra tu primera compra para empezar a ver tu café.",
                        style = AppTypography.bodyMd.copy(color = TextMuted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.s28)
                    )
                }
                ContinueButton(
                    text = "Registrar compra",
                    onClick = onRegistrarCompra,
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

// Ilustración de 220 x 200 dp: anillo punteado, círculo azul claro, caja y puntos decorativos.
// Es una versión simplificada del Figma (allí la caja está abierta y hay un grano de café fantasma).
@Composable
private fun IlustracionVacia() {
    Box(modifier = Modifier.size(width = 220.dp, height = 200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centro = Offset(110.dp.toPx(), 100.dp.toPx())

            // Anillo punteado (200 dp de diámetro).
            drawCircle(
                color = BorderDashed,
                radius = 99.dp.toPx(),
                center = centro,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                )
            )
            // Fondo azul claro (176 dp).
            drawCircle(color = SurfaceSelected, radius = 88.dp.toPx(), center = centro)
            // Puntos decorativos.
            drawCircle(color = BorderDashed, radius = 4.dp.toPx(), center = Offset(44.dp.toPx(), 44.dp.toPx()))
            drawCircle(color = BorderDashed, radius = 3.dp.toPx(), center = Offset(179.dp.toPx(), 61.dp.toPx()))
            drawCircle(color = BorderDashed, radius = 4.dp.toPx(), center = Offset(188.dp.toPx(), 132.dp.toPx()))
            drawCircle(color = BorderDashed, radius = 3.dp.toPx(), center = Offset(33.dp.toPx(), 143.dp.toPx()))
        }
        Icon(
            imageVector = AppIcons.PackageFine,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .offset(x = 62.dp, y = 70.dp)
                .size(96.dp)
        )
    }
}

// ---------------- Vistas previas ----------------

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InventoryScreenPreview() {
    CafesmartTheme {
        InventoryScreen(
            estado = InventoryUiState(resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG)),
            onTipoClick = {},
            onOrdenChange = {},
            onAjustarBodega = {},
            onRegistrarCompra = {},
            onNavegar = {}
        )
    }
}

// Bodega casi llena: el punto y el anillo se ponen rojos.
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InventoryScreenCriticaPreview() {
    CafesmartTheme {
        InventoryScreen(
            estado = InventoryUiState(resumirInventario(lotesEjemplo(), capacidadKg = 900.0)),
            onTipoClick = {},
            onOrdenChange = {},
            onAjustarBodega = {},
            onRegistrarCompra = {},
            onNavegar = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InventoryScreenVaciaPreview() {
    CafesmartTheme {
        InventoryScreen(
            estado = InventoryUiState(resumirInventario(emptyList(), CAPACIDAD_EJEMPLO_KG)),
            onTipoClick = {},
            onOrdenChange = {},
            onAjustarBodega = {},
            onRegistrarCompra = {},
            onNavegar = {}
        )
    }
}
