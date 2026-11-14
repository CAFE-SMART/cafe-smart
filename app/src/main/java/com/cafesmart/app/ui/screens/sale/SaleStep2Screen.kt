package com.cafesmart.app.ui.screens.sale

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.BloqueDeTotales
import com.cafesmart.app.ui.components.barraDeDesplazamiento
import com.cafesmart.app.ui.screens.purchase.PurchaseHeader
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.DividerOnBlue
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextStrong
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.sale.GrupoCalidad
import com.cafesmart.app.viewmodel.sale.PrecioEnPantalla
import com.cafesmart.app.viewmodel.sale.SaleUiState
import com.cafesmart.app.viewmodel.sale.SubloteEnPantalla

// Paso 2 de Ventas: ¿Qué café vendes?
// Vista (MVVM): no tiene logica. Todo llega calculado en el estado (textos, errores, true/false)
// y cada accion sale por una lambda. Solo recuerda algo visual: cuales sublotes tienen abierto el campo "Cambiar".
@Composable
fun SaleStep2Screen(
    estado: SaleUiState,
    onTipoCafeChange: (TipoCafe) -> Unit,
    onAlternarTodos: () -> Unit,
    onAlternarSublote: (SubloteConLote) -> Unit,
    onCantidadChange: (Long, String) -> Unit,
    onPrecioChange: (Calidad, String) -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado visual: ids de los sublotes que tienen abierto el campo de cantidad.
    var editando by remember { mutableStateOf(setOf<Long>()) }
    val desplazamiento = rememberScrollState()

    // imePadding(): cuando sale el teclado, la pantalla se encoge para que no tape el campo ni el boton.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                // Barrita a la derecha: avisa que la lista sigue hacia abajo cuando hay varios cafes.
                .barraDeDesplazamiento(desplazamiento)
                .verticalScroll(desplazamiento)
        ) {
            PurchaseHeader(paso = 2, pregunta = "¿Qué café vendes?", onBack = onBack, titulo = "Registro de venta")

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                SelectorTipoCafe(tipos = estado.tipos, elegido = estado.tipoCafe, onTipoCafeChange = onTipoCafeChange)

                val mensajeSinCafe = estado.mensajeSinCafe
                if (mensajeSinCafe != null) {
                    Text(
                        text = mensajeSinCafe,
                        style = AppTypography.bodyMd,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.s20)
                    )
                } else {
                    TarjetaSeleccionarTodo(
                        texto = estado.textoSeleccionarTodo,
                        marcada = estado.todosElegidos,
                        aMedias = estado.algunosElegidos,
                        onClick = onAlternarTodos
                    )
                    estado.grupos.forEach { grupo ->
                        GrupoDeCalidad(
                            grupo = grupo,
                            editando = editando,
                            onAlternarSublote = {
                                // Al marcar o desmarcar, el campo "Cambiar" se cierra.
                                editando = editando - it.sublote.id
                                onAlternarSublote(it)
                            },
                            onEditar = { editando = editando + it },
                            onTerminarEdicion = { editando = editando - it },
                            onCantidadChange = onCantidadChange,
                            onPrecioChange = onPrecioChange
                        )
                    }
                }

                BloqueDeTotales(
                    etiquetaIzquierda = "Total a vender", valorIzquierdo = estado.totalKgTexto,
                    etiquetaDerecha = "Total a recibir", valorDerecho = estado.totalARecibirTexto,
                    color = if (estado.totalARecibir > 0) PrimaryDark else TextMuted,
                    habilitado = estado.puedeContinuar,
                    mensajePendiente = estado.mensajePendientePaso2,
                    onContinuar = onContinuar
                ) {
                    // Aviso cuando lo elegido suma menos del minimo de una venta (5 kg).
                    estado.errorDeTotal?.let { Text(text = it, style = AppTypography.helper.copy(color = ErrorText)) }
                }
            }
        }
    }
}

// Pestanas Verde / Seco / Trillado / Pasilla: la activa va en oscuro.
@Composable
private fun SelectorTipoCafe(tipos: List<TipoCafe>, elegido: TipoCafe, onTipoCafeChange: (TipoCafe) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSoft, AppShapes.medium)
            .padding(Spacing.s4)
    ) {
        tipos.forEach { tipo ->
            val activo = tipo == elegido
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (activo) TextPrimary else Color.Transparent, AppShapes.medium)
                    // selectable: ademas del clic, avisa a TalkBack cual pestana esta activa.
                    .selectable(selected = activo, role = Role.Tab, onClick = { onTipoCafeChange(tipo) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tipo.etiqueta,
                    style = AppTypography.labelField.copy(color = if (activo) OnPrimary else TextStrong)
                )
            }
        }
    }
}

// Casilla cuadrada: vacia, con ✓ (marcada) o con "-" (algunos elegidos).
@Composable
private fun Casilla(marcada: Boolean, aMedias: Boolean = false) {
    if (marcada || aMedias) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Primary, AppShapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (marcada) {
                Icon(imageVector = AppIcons.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
            } else {
                // La raya del "-"
                Box(modifier = Modifier.size(width = 12.dp, height = 2.dp).background(OnPrimary, CircleShape))
            }
        }
    } else {
        Box(modifier = Modifier.size(24.dp).border(1.5.dp, BorderField, AppShapes.small))
    }
}

@Composable
private fun TarjetaSeleccionarTodo(texto: String, marcada: Boolean, aMedias: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.s16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Seleccionar todo", style = AppTypography.titleCard)
                Text(text = texto, style = AppTypography.bodyMd)
            }
            Casilla(marcada = marcada, aMedias = aMedias)
        }
    }
}

// Etiqueta de una calidad con su resumen, los sublotes y, si hay alguno elegido, el precio de esa calidad.
@Composable
private fun GrupoDeCalidad(
    grupo: GrupoCalidad,
    editando: Set<Long>,
    onAlternarSublote: (SubloteConLote) -> Unit,
    onEditar: (Long) -> Unit,
    onTerminarEdicion: (Long) -> Unit,
    onCantidadChange: (Long, String) -> Unit,
    onPrecioChange: (Calidad, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Row(
            modifier = Modifier.padding(top = Spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8)
        ) {
            EtiquetaCalidad(calidad = grupo.calidad, texto = grupo.etiqueta)
            Text(text = grupo.resumen, style = AppTypography.bodyMd)
        }
        grupo.sublotes.forEach { fila ->
            TarjetaSublote(
                fila = fila,
                editando = fila.sublote.sublote.id in editando,
                onClick = { onAlternarSublote(fila.sublote) },
                onEditar = { onEditar(fila.sublote.sublote.id) },
                onTerminarEdicion = { onTerminarEdicion(fila.sublote.sublote.id) },
                onCantidadChange = { onCantidadChange(fila.sublote.sublote.id, it) }
            )
        }
        grupo.precio?.let { TarjetaPrecio(precio = it, onPrecioChange = { texto -> onPrecioChange(grupo.calidad, texto) }) }
    }
}

// Pastilla "Calidad buena" con su punto de color (solo apariencia)
@Composable
private fun EtiquetaCalidad(calidad: Calidad, texto: String) {
    val (fondo, punto, color) = when (calidad) {
        Calidad.BUENA -> Triple(SuccessBg, SuccessDot, SuccessText)
        Calidad.REGULAR -> Triple(WarningBg, WarningDot, WarningText)
        Calidad.MALA -> Triple(ErrorBg, ErrorDot, ErrorText)
    }
    Row(
        modifier = Modifier
            .background(fondo, CircleShape)
            .padding(horizontal = Spacing.s12, vertical = Spacing.s6),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(punto, CircleShape))
        Spacer(modifier = Modifier.width(Spacing.s6))
        Text(text = texto, style = AppTypography.pill.copy(color = color))
    }
}

// Un sublote: al tocarlo se elige (y se vende todo lo disponible). Ya elegido muestra la cantidad con "Cambiar".
@Composable
private fun TarjetaSublote(
    fila: SubloteEnPantalla,
    editando: Boolean,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onTerminarEdicion: () -> Unit,
    onCantidadChange: (String) -> Unit
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        color = if (fila.elegido) SurfaceSelected else SurfaceCard,
        border = BorderStroke(
            width = if (fila.elegido) 2.dp else 1.dp,
            color = if (fila.elegido) Primary else BorderDefault
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = fila.titulo, style = AppTypography.titleCard)
                    Text(text = fila.detalle, style = AppTypography.bodyMd)
                }
                Casilla(marcada = fila.elegido)
            }
            if (fila.elegido) {
                HorizontalDivider(color = DividerOnBlue)
                if (editando) {
                    AppTextField(
                        label = "Cantidad a vender (kg)",
                        value = fila.cantidadCampo,
                        onValueChange = onCantidadChange,
                        placeholder = "Ej. 150",
                        keyboardType = KeyboardType.Decimal,
                        helper = fila.ayudaCantidad,
                        error = fila.errorCantidad
                    )
                    // "Listo" solo se activa cuando lo escrito sirve: asi la tarjeta nunca muestra una cantidad distinta a la del campo.
                    TextButton(onClick = onTerminarEdicion, enabled = fila.cantidadValida, modifier = Modifier.align(Alignment.End)) {
                        Text(text = "Listo", style = AppTypography.buttonLink)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cantidad a vender", style = AppTypography.caption)
                            Text(text = fila.cantidadTexto, style = AppTypography.valueStrongBold)
                        }
                        TextButton(onClick = onEditar) {
                            Text(text = "Cambiar", style = AppTypography.buttonLink)
                        }
                    }
                }
            }
        }
    }
}

// Precio por kg de una calidad, con su ayuda (rango y costo promedio) o su error
@Composable
private fun TarjetaPrecio(precio: PrecioEnPantalla, onPrecioChange: (String) -> Unit) {
    Surface(
        shape = AppShapes.medium,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        AppTextField(
            label = precio.titulo,
            // El ViewModel entrega el precio ya con punto de miles.
            value = precio.texto,
            onValueChange = onPrecioChange,
            placeholder = "20.000",
            keyboardType = KeyboardType.Number,
            leadingText = "$",
            helper = precio.ayuda,
            error = precio.error,
            modifier = Modifier.padding(Spacing.s16)
        )
    }
}

// ---------------- Previews (con estados de ejemplo) ----------------

@Composable
private fun Paso2Preview(estado: SaleUiState) {
    CafesmartTheme {
        SaleStep2Screen(
            estado = estado,
            onTipoCafeChange = {},
            onAlternarTodos = {},
            onAlternarSublote = {},
            onCantidadChange = { _, _ -> },
            onPrecioChange = { _, _ -> },
            onContinuar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso2SinElegirPreview() {
    Paso2Preview(SaleUiState())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun Paso2UnaPartePreview() {
    val estado = SaleUiState()
    val sublote5 = estado.inventario.first { it.sublote.id == 5L }
    Paso2Preview(
        estado.copy(
            elegidos = listOf(SubloteElegido(sublote5, 150.0)),
            precios = mapOf(Calidad.BUENA to "20000")
        )
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso2SinCafePreview() {
    Paso2Preview(SaleUiState(tipoCafe = TipoCafe.PASILLA))
}
