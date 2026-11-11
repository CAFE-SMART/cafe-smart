package com.cafesmart.app.ui.screens.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.purchase.productorSinRegistrar
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.AvisoCapacidadCard
import com.cafesmart.app.ui.components.EstiloAviso
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessSolid
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.viewmodel.purchase.PurchaseUiState

// Compra registrada: circulo verde con chulito blanco, titulo negro centrado y resumen.
// Vista (MVVM): no tiene logica ni estado propio.
@Composable
fun PurchaseSuccessScreen(
    estado: PurchaseUiState,
    onRegistrarNueva: () -> Unit,
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28)
    ) {
        Column(
            modifier = Modifier
                // weight(1f) y scroll: si el resumen no cabe se desplaza y los botones quedan abajo
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.s48),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circulo verde con el chulito blanco centrado.
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SuccessSolid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = "Compra registrada",
                // titleExito: titulo negro, Bold 24.
                style = AppTypography.titleExito,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s20)
            )
            Text(
                text = "La compra se guardó correctamente.",
                style = AppTypography.bodyMd,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s20)
            )
            // La compra dejo la bodega casi llena o la supero: se aconseja vender cafe para liberar espacio.
            estado.avisoAlRegistrar?.let {
                AvisoCapacidadCard(
                    titulo = it.tituloRegistrada, mensaje = it.mensajeRegistrada, estilo = EstiloAviso.ALERTA,
                    modifier = Modifier.padding(bottom = Spacing.s20)
                )
            }
            Text(
                text = "Resumen de compra",
                style = AppTypography.labelSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.s8)
            )
            ResumenCompra(
                productor = estado.productorTexto,
                fecha = estado.fechaResumenTexto,
                totalKg = estado.totalKgTexto,
                totalPagado = estado.totalEstimadoTexto
            )
        }

        Column(
            modifier = Modifier.padding(top = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            // Boton principal: empieza otra compra.
            ContinueButton(text = "Registrar nueva compra", onClick = onRegistrarNueva)
            Button(
                onClick = onIrAInventario,
                shape = AppShapes.medium,
                // Boton secundario con fondo azul suave.
                colors = ButtonDefaults.buttonColors(containerColor = ButtonSoft, contentColor = PrimaryDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(text = "Ir a inventario", style = AppTypography.buttonPrimary.copy(color = PrimaryDark))
            }
        }
    }
}

// Surface recorta el fondo azul de la ultima fila con las esquinas redondeadas de la tarjeta
@Composable
private fun ResumenCompra(productor: String, fecha: String, totalKg: String, totalPagado: String) {
    Surface(
        shape = AppShapes.medium,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Filas separadas por una linea.
            FilaResumen(etiqueta = "Productor", valor = productor)
            HorizontalDivider(color = BorderDefault)
            FilaResumen(etiqueta = "Fecha", valor = fecha)
            HorizontalDivider(color = BorderDefault)
            FilaResumen(etiqueta = "Total kg", valor = totalKg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSelected)
                    .padding(Spacing.s16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ultima fila resaltada en azul.
                Text(text = "Total pagado", style = AppTypography.labelField.copy(color = PrimaryDark))
                // weight + TextAlign.End: si el valor es muy largo baja de linea en vez de cortarse
                Text(
                    text = totalPagado,
                    style = AppTypography.displayValor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = etiqueta, style = AppTypography.bodyMd)
        Text(
            text = valor,
            style = AppTypography.valueStrong,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
// Preview con una compra de ejemplo.
private fun PurchaseSuccessPreview() {
    CafesmartTheme {
        PurchaseSuccessScreen(
            estado = PurchaseUiState(
                compraRegistrada = true,
                productorDeLaCompra = productorSinRegistrar,
                cafes = listOf(CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14000"))
            ),
            onRegistrarNueva = {},
            onIrAInventario = {}
        )
    }
}
