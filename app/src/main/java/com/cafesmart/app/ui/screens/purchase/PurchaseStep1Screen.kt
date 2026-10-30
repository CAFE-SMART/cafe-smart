package com.cafesmart.app.ui.screens.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.Productor
import com.cafesmart.app.data.purchase.TipoDocumento
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.OptionCard
import com.cafesmart.app.ui.components.RadioCheck
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.viewmodel.purchase.ProductorEnPantalla
import com.cafesmart.app.viewmodel.purchase.PurchaseUiState

// Paso 1: ¿A quien le compras el cafe?
// Vista (MVVM): no tiene logica. Recibe el estado del ViewModel ya listo
// (true/false y textos) y avisa cada accion con una lambda.
@Composable
fun PurchaseStep1Screen(
    estado: PurchaseUiState,
    onModoChange: (ModoProductor) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onVerMas: () -> Unit,
    onTipoDocumentoChange: (TipoDocumento) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // imePadding(): cuando sale el teclado, la pantalla se encoge para que no tape
    // los campos ni el boton Continuar
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .imePadding()
    ) {
        // weight(1f): esta parte ocupa todo el alto que sobra y hace scroll;
        // asi la barra de abajo siempre queda pegada al final, en cualquier celular
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PurchaseHeader(paso = 1, pregunta = "¿A quién le compras el café?", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                OptionCard(
                    title = "Productor registrado",
                    description = "Elígelo de tu lista.",
                    icon = AppIcons.Search,
                    selected = estado.registradoSeleccionado,
                    onClick = { onModoChange(ModoProductor.REGISTRADO) }
                ) {
                    BuscadorProductor(
                        busqueda = estado.busqueda,
                        titulo = estado.tituloLista,
                        productores = estado.productoresEnPantalla,
                        mensajeListaVacia = estado.mensajeListaVacia,
                        hayMasProductores = estado.hayMasProductores,
                        onBusquedaChange = onBusquedaChange,
                        onProductorClick = onProductorClick,
                        onVerMas = onVerMas
                    )
                }

                OptionCard(
                    title = "Sin registrar",
                    description = "Compra rápida, sin datos.",
                    icon = AppIcons.User,
                    selected = estado.sinRegistrarSeleccionado,
                    onClick = { onModoChange(ModoProductor.SIN_REGISTRAR) }
                )

                OptionCard(
                    title = "Productor nuevo",
                    description = "Guarda sus datos.",
                    icon = AppIcons.UserPlus,
                    selected = estado.nuevoSeleccionado,
                    onClick = { onModoChange(ModoProductor.NUEVO) }
                ) {
                    NewProducerForm(
                        esEmpresa = estado.esEmpresa,
                        documento = estado.nuevoProductor.documento,
                        nombre = estado.nuevoProductor.nombre,
                        telefono = estado.nuevoProductor.telefono,
                        documentoError = estado.documentoError,
                        nombreError = estado.nombreError,
                        telefonoError = estado.telefonoError,
                        onTipoDocumentoChange = onTipoDocumentoChange,
                        onDocumentoChange = onDocumentoChange,
                        onNombreChange = onNombreChange,
                        onTelefonoChange = onTelefonoChange
                    )
                }
            }
        }

        PurchaseBottomBar {
            PendingMessage(mensaje = estado.mensajePendientePaso1)
            ContinueButton(
                text = "Continuar",
                enabled = estado.continuarPaso1Habilitado,
                onClick = onContinuar
            )
        }
    }
}

// Buscador + lista de productores, dentro de la tarjeta "Productor registrado"
@Composable
private fun BuscadorProductor(
    busqueda: String,
    titulo: String,
    productores: List<ProductorEnPantalla>,
    mensajeListaVacia: String?,
    hayMasProductores: Boolean,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onVerMas: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusquedaChange,
            singleLine = true,
            textStyle = AppTypography.bodyLg,
            placeholder = { Text(text = "Buscar por nombre o cédula", style = AppTypography.bodyLg.copy(color = TextMuted)) },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = AppShapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderField,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (mensajeListaVacia != null) {
            Text(text = mensajeListaVacia, style = AppTypography.bodyMd)
        } else {
            Text(text = titulo, style = AppTypography.labelField)
            productores.forEach { fila ->
                FilaProductor(
                    fila = fila,
                    onClick = { onProductorClick(fila.productor) }
                )
            }
            if (hayMasProductores) {
                OutlinedButton(
                    onClick = onVerMas,
                    shape = AppShapes.medium,
                    border = BorderStroke(1.5.dp, BorderField),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Ver más productores", style = AppTypography.buttonLink)
                }
            }
        }
    }
}

// Un productor de la lista: nombre, "Cédula 123..." y circulo de seleccion
@Composable
private fun FilaProductor(fila: ProductorEnPantalla, onClick: () -> Unit) {
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
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fila.nombre, style = AppTypography.valueStrong)
                Text(text = fila.documento, style = AppTypography.bodyMd)
            }
            RadioCheck(selected = fila.elegido)
        }
    }
}

// ---------------- Previews (con estados de ejemplo) ----------------

private val productoresDePrueba = listOf(
    Productor(nombre = "Pepito Pérez", documento = "890301886"),
    Productor(nombre = "Andy López", documento = "11140596")
)

@Composable
private fun Paso1Preview(estado: PurchaseUiState) {
    CafesmartTheme {
        PurchaseStep1Screen(
            estado = estado,
            onModoChange = {},
            onBusquedaChange = {},
            onProductorClick = {},
            onVerMas = {},
            onTipoDocumentoChange = {},
            onDocumentoChange = {},
            onNombreChange = {},
            onTelefonoChange = {},
            onContinuar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1SinElegirPreview() {
    Paso1Preview(PurchaseUiState())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1RegistradoPreview() {
    Paso1Preview(
        PurchaseUiState(
            modo = ModoProductor.REGISTRADO,
            productores = productoresDePrueba,
            productorElegido = productoresDePrueba[1]
        )
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1NuevoPreview() {
    Paso1Preview(PurchaseUiState(modo = ModoProductor.NUEVO))
}
