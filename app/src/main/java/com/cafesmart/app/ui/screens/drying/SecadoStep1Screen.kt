package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.barraDeDesplazamiento
import com.cafesmart.app.ui.screens.purchase.PendingMessage
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.DividerOnBlue
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.viewmodel.drying.AjusteEnPantalla
import com.cafesmart.app.viewmodel.drying.GrupoSecado
import com.cafesmart.app.viewmodel.drying.SecadoUiState
import com.cafesmart.app.viewmodel.drying.SubloteSecadoEnPantalla

// Paso 1 de Secado: ¿Qué café vas a secar?
// Vista (MVVM): no tiene logica. Todo llega calculado en el estado (textos, errores, true/false)
// y cada accion sale por una lambda.
@Composable
fun SecadoStep1Screen(
    estado: SecadoUiState,
    onAlternarTodos: () -> Unit,
    onAlternarSublote: (SubloteConLote) -> Unit,
    onCambiar: (Long) -> Unit,
    onAjusteChange: (String) -> Unit,
    onAjusteConfirmar: () -> Unit,
    onAjusteCancelar: () -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val desplazamiento = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                // Barrita a la derecha: avisa que la lista sigue hacia abajo cuando hay varios sublotes.
                .barraDeDesplazamiento(desplazamiento)
                .verticalScroll(desplazamiento)
        ) {
            EncabezadoSecado(paso = 1, pregunta = "¿Qué café vas a secar?", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                AvisoSoloVerde()

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
                        GrupoDeCalidad(grupo = grupo, onAlternarSublote = onAlternarSublote, onCambiar = onCambiar)
                    }
                }
            }
        }

        // Pie fijo: total a secar y "Continuar"
        PurchaseBottomBar {
            Column {
                Text(text = "Total a secar", style = AppTypography.caption)
                Text(text = estado.totalKgTexto, style = AppTypography.titleScreen.copy(color = PrimaryDark))
            }
            PendingMessage(mensaje = estado.mensajePendientePaso1)
            ContinueButton(text = "Continuar", enabled = estado.puedeContinuar, onClick = onContinuar)
        }
    }

    // Hoja "Ajustar cantidad": solo existe mientras se ajusta un sublote.
    estado.ajusteEnPantalla?.let { ajuste ->
        HojaAjustarCantidad(
            ajuste = ajuste,
            onCambio = onAjusteChange,
            onConfirmar = onAjusteConfirmar,
            onCancelar = onAjusteCancelar
        )
    }
}

// Aviso azul de arriba: este proceso es solo para cafe verde.
@Composable
private fun AvisoSoloVerde() {
    Surface(
        shape = AppShapes.medium,
        color = SurfaceSelected,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8)
        ) {
            Icon(imageVector = AppIcons.Leaf, contentDescription = null, tint = SuccessIcon, modifier = Modifier.size(20.dp))
            Text(
                text = "Este proceso es solo para café verde. Aquí solo aparecen sublotes de este tipo.",
                style = AppTypography.bodyMd,
                modifier = Modifier.weight(1f)
            )
        }
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

// Etiqueta de una calidad con su resumen y sus sublotes
@Composable
private fun GrupoDeCalidad(
    grupo: GrupoSecado,
    onAlternarSublote: (SubloteConLote) -> Unit,
    onCambiar: (Long) -> Unit
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
                onClick = { onAlternarSublote(fila.sublote) },
                onCambiar = { onCambiar(fila.sublote.sublote.id) }
            )
        }
    }
}

// Un sublote: al tocarlo se elige (y se manda todo lo disponible). Ya elegido muestra la cantidad con "Cambiar".
@Composable
private fun TarjetaSublote(
    fila: SubloteSecadoEnPantalla,
    onClick: () -> Unit,
    onCambiar: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Cantidad a secar", style = AppTypography.caption)
                        Text(text = fila.cantidadTexto, style = AppTypography.valueStrongBold)
                    }
                    TextButton(onClick = onCambiar) {
                        Text(text = "Cambiar", style = AppTypography.buttonLink)
                    }
                }
            }
        }
    }
}

// Hoja "Ajustar cantidad" (Figma "Secado - Modal - Ajustar cantidad a secar").
// "Confirmar" solo se activa cuando lo escrito sirve; cerrar la hoja o "Cancelar" descarta lo escrito.
@Composable
private fun HojaAjustarCantidad(
    ajuste: AjusteEnPantalla,
    onCambio: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AppBottomSheet(onDismiss = onCancelar, skipPartiallyExpanded = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            Text(text = "Ajustar cantidad", style = AppTypography.titleModal)

            // Sublote y lo que tiene disponible
            Surface(shape = AppShapes.medium, color = SurfaceSoft, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(Spacing.s16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Sublote", style = AppTypography.caption)
                        Text(text = ajuste.titulo, style = AppTypography.valueStrongBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Disponible", style = AppTypography.caption)
                        Text(text = ajuste.disponibleTexto, style = AppTypography.valueStrongBold.copy(color = PrimaryDark))
                    }
                }
            }

            AppTextField(
                label = "Cantidad a secar (kg)",
                value = ajuste.texto,
                onValueChange = onCambio,
                placeholder = "Ej. 60",
                keyboardType = KeyboardType.Decimal,
                helper = ajuste.ayuda,
                error = ajuste.error
            )

            ContinueButton(text = "Confirmar", enabled = ajuste.valido, onClick = onConfirmar)
            TextButton(onClick = onCancelar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = "Cancelar", style = AppTypography.buttonLink)
            }
        }
    }
}

// ---------------- Previews (con el inventario de ejemplo) ----------------

@Composable
private fun Paso1Preview(estado: SecadoUiState) {
    CafesmartTheme {
        SecadoStep1Screen(
            estado = estado,
            onAlternarTodos = {},
            onAlternarSublote = {},
            onCambiar = {},
            onAjusteChange = {},
            onAjusteConfirmar = {},
            onAjusteCancelar = {},
            onContinuar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1SinElegirPreview() {
    Paso1Preview(SecadoUiState(inventario = SecadoRepositoryEjemplo().inventario()))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1SinCafeVerdePreview() {
    Paso1Preview(SecadoUiState(inventario = emptyList()))
}
