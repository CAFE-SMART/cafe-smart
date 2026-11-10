package com.cafesmart.app.ui.screens.purchase

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.purchase.productorSinRegistrar
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.CancelarSheet
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.TrilladoBg
import com.cafesmart.app.ui.theme.TrilladoIcon
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.purchase.CafeEnResumen
import com.cafesmart.app.viewmodel.purchase.PurchaseUiState

// Paso 3: Confirma tu compra.
// Vista (MVVM): no tiene logica. Solo recuerda si el calendario o la hoja de cancelar
// estan abiertos, que es algo visual.
@Composable
fun PurchaseStep3Screen(
    estado: PurchaseUiState,
    onCambiarProductor: () -> Unit,
    onFechaChange: (Long) -> Unit,
    onEditarCafe: (Int) -> Unit,
    onConfirmar: () -> Unit,
    onCancelarCompra: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado visual: si el calendario esta abierto.
    var mostrarCalendario by remember { mutableStateOf(false) }
    // Estado visual: si la hoja de cancelar esta abierta.
    var mostrarCancelar by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                // Permite desplazar el contenido si no cabe en la pantalla.
                .verticalScroll(rememberScrollState())
        ) {
            PurchaseHeader(paso = 3, pregunta = "Confirma tu compra", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                DatosCompra(
                    productor = estado.productorTexto,
                    fecha = estado.fechaResumenTexto,
                    onCambiarProductor = onCambiarProductor,
                    // "Cambiar" fecha abre el calendario.
                    onCambiarFecha = { mostrarCalendario = true }
                )

                Text(
                    text = "Café que compras",
                    style = AppTypography.labelSection,
                    modifier = Modifier.padding(top = Spacing.s4)
                )

                // Una tarjeta por cada cafe de la compra.
                estado.cafesEnResumen.forEachIndexed { indice, cafe ->
                    TarjetaCafeResumen(cafe = cafe, onEditar = { onEditarCafe(indice) })
                }

                ResumenPago(totalKg = estado.totalKgTexto, totalPagar = estado.totalEstimadoTexto)
            }
        }

        PurchaseBottomBar {
            // Boton principal del equipo.
            ContinueButton(text = "Confirmar compra", onClick = onConfirmar)
            TextButton(
                // "Cancelar" no borra nada todavia: primero pregunta con la hoja.
                onClick = { mostrarCancelar = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Cancelar", style = AppTypography.labelSection)
            }
        }
    }

    if (mostrarCalendario) {
        PurchaseDatePickerDialog(
            fechaMilis = estado.fechaMilis,
            onFechaChange = onFechaChange,
            onCerrar = { mostrarCalendario = false }
        )
    }

    if (mostrarCancelar) {
        CancelarSheet(
            titulo = "¿Cancelar compra?",
            onCancelar = onCancelarCompra,
            // "Seguir editando" solo cierra la hoja.
            onSeguir = { mostrarCancelar = false }
        )
    }
}

// Tarjeta con el productor y la fecha, cada uno con su enlace "Cambiar"
@Composable
private fun DatosCompra(
    productor: String,
    fecha: String,
    onCambiarProductor: () -> Unit,
    onCambiarFecha: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(start = Spacing.s16, top = Spacing.s8, end = Spacing.s4, bottom = Spacing.s8)
    ) {
        FilaDato(etiqueta = "Productor", valor = productor, onCambiar = onCambiarProductor)
        HorizontalDivider(
            color = BorderDefault,
            modifier = Modifier.padding(end = Spacing.s12, top = Spacing.s4, bottom = Spacing.s4)
        )
        FilaDato(etiqueta = "Fecha", valor = fecha, onCambiar = onCambiarFecha)
    }
}

@Composable
// Una fila con etiqueta, valor y enlace "Cambiar".
private fun FilaDato(etiqueta: String, valor: String, onCambiar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // weight(1f): ocupa el espacio libre y empuja "Cambiar" a la derecha.
        Column(modifier = Modifier.weight(1f)) {
            Text(text = etiqueta, style = AppTypography.caption)
            Text(text = valor, style = AppTypography.valueStrong)
        }
        TextButton(onClick = onCambiar) {
            Text(text = "Cambiar", style = AppTypography.buttonLink)
        }
    }
}

// Un cafe de la compra con su cuenta: kg x precio = total
@Composable
private fun TarjetaCafeResumen(cafe: CafeEnResumen, onEditar: () -> Unit) {
    // Color del icono segun el tipo de cafe (solo apariencia)
    val fondoIcono = when (cafe.tipoCafe) {
        "Verde" -> SuccessBg
        "Seco" -> WarningBg
        "Trillado" -> TrilladoBg
        else -> SurfaceSoft
    }
    val colorIcono = when (cafe.tipoCafe) {
        "Verde" -> SuccessIcon
        "Seco" -> WarningText
        "Trillado" -> TrilladoIcon
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(fondoIcono, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Leaf,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = cafe.titulo, style = AppTypography.titleCard)
                Text(text = cafe.calidad, style = AppTypography.bodyMd)
            }
            OutlinedButton(
                onClick = onEditar,
                // CircleShape en un boton lo deja con forma de pastilla.
                shape = CircleShape,
                border = BorderStroke(1.5.dp, BorderField)
            ) {
                Icon(
                    imageVector = AppIcons.Pencil,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.s6))
                Text(text = "Editar", style = AppTypography.labelField.copy(color = PrimaryDark))
            }
        }
        HorizontalDivider(color = BorderDefault)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // La cuenta: "25 kg × $ 14.000" a la izquierda y el total a la derecha.
            Text(text = cafe.detalle, style = AppTypography.bodyMd)
            Text(text = cafe.total, style = AppTypography.valueStrongBold)
        }
    }
}

// Resumen azul con los totales
@Composable
private fun ResumenPago(totalKg: String, totalPagar: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSelected, AppShapes.medium)
            .border(1.dp, BorderSelectedSoft, AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8)
    ) {
        Text(text = "Resumen", style = AppTypography.labelSection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Total kg", style = AppTypography.bodyMd)
            Text(text = totalKg, style = AppTypography.titleModal.copy(color = PrimaryDark))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Total a pagar", style = AppTypography.bodyMd)
            // weight + TextAlign.End: si el valor es muy largo baja de linea en vez de cortarse
            Text(
                text = totalPagar,
                style = AppTypography.displayValorXl,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
// Preview con un cafe de ejemplo.
private fun PurchaseStep3Preview() {
    CafesmartTheme {
        PurchaseStep3Screen(
            estado = PurchaseUiState(
                paso = 3,
                productorDeLaCompra = productorSinRegistrar,
                cafes = listOf(CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14000"))
            ),
            onCambiarProductor = {},
            onFechaChange = {},
            onEditarCafe = {},
            onConfirmar = {},
            onCancelarCompra = {},
            onBack = {}
        )
    }
}
