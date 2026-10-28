package com.cafesmart.app.ui.compra

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
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.Productor
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

// Paso 1: ¿A quien le compras el cafe?
// Pantalla sin estado propio: todo llega por parametros y cada accion sale por una lambda.
@Composable
fun RegistroCompraPaso1Screen(
    modo: ModoProductor?,
    busqueda: String,
    productores: List<Productor>,
    hayMasProductores: Boolean,
    productorElegido: Productor?,
    nuevoProductor: NuevoProductor,
    documentoError: String?,
    nombreError: String?,
    telefonoError: String?,
    continuarHabilitado: Boolean,
    onModoChange: (ModoProductor) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onVerMas: () -> Unit,
    onNuevoProductorChange: (NuevoProductor) -> Unit,
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
            CompraHeader(paso = 1, pregunta = "¿A quién le compras el café?", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                OptionCard(
                    title = "Productor registrado",
                    description = "Elígelo de tu lista.",
                    icon = AppIcons.Search,
                    selected = modo == ModoProductor.REGISTRADO,
                    onClick = { onModoChange(ModoProductor.REGISTRADO) }
                ) {
                    BuscadorProductor(
                        busqueda = busqueda,
                        productores = productores,
                        hayMasProductores = hayMasProductores,
                        productorElegido = productorElegido,
                        onBusquedaChange = onBusquedaChange,
                        onProductorClick = onProductorClick,
                        onVerMas = onVerMas
                    )
                }

                OptionCard(
                    title = "Sin registrar",
                    description = "Compra rápida, sin datos.",
                    icon = AppIcons.User,
                    selected = modo == ModoProductor.SIN_REGISTRAR,
                    onClick = { onModoChange(ModoProductor.SIN_REGISTRAR) }
                )

                OptionCard(
                    title = "Productor nuevo",
                    description = "Guarda sus datos.",
                    icon = AppIcons.UserPlus,
                    selected = modo == ModoProductor.NUEVO,
                    onClick = { onModoChange(ModoProductor.NUEVO) }
                ) {
                    NuevoProductorForm(
                        datos = nuevoProductor,
                        documentoError = documentoError,
                        nombreError = nombreError,
                        telefonoError = telefonoError,
                        onDatosChange = onNuevoProductorChange
                    )
                }
            }
        }

        CompraBottomBar {
            ContinueButton(
                text = "Continuar",
                enabled = continuarHabilitado,
                onClick = onContinuar
            )
        }
    }
}

// Buscador + lista de productores, dentro de la tarjeta "Productor registrado"
@Composable
private fun BuscadorProductor(
    busqueda: String,
    productores: List<Productor>,
    hayMasProductores: Boolean,
    productorElegido: Productor?,
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

        if (productores.isEmpty()) {
            Text(
                text = if (busqueda.isBlank()) {
                    "Aún no tienes productores registrados. Usa \"Productor nuevo\" para crear uno."
                } else {
                    "No hay productores con ese nombre o documento."
                },
                style = AppTypography.bodyMd
            )
        } else {
            Text(text = if (busqueda.isBlank()) "Recientes" else "Resultados", style = AppTypography.labelField)
            productores.forEach { productor ->
                FilaProductor(
                    productor = productor,
                    elegido = productor == productorElegido,
                    onClick = { onProductorClick(productor) }
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
private fun FilaProductor(productor: Productor, elegido: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        color = if (elegido) SurfaceSelected else SurfaceCard,
        border = BorderStroke(
            width = if (elegido) 2.dp else 1.dp,
            color = if (elegido) Primary else BorderDefault
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = productor.nombre, style = AppTypography.valueStrong)
                Text(text = "${productor.tipoDocumento.etiqueta} ${productor.documento}", style = AppTypography.bodyMd)
            }
            RadioCheck(selected = elegido)
        }
    }
}

private val productoresDePrueba = listOf(
    Productor(nombre = "Pepito Pérez", documento = "890301886"),
    Productor(nombre = "Andy López", documento = "11140596")
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1SinElegirPreview() {
    CafesmartTheme {
        RegistroCompraPaso1Screen(
            modo = null,
            busqueda = "",
            productores = emptyList(),
            hayMasProductores = false,
            productorElegido = null,
            nuevoProductor = NuevoProductor(),
            documentoError = null,
            nombreError = null,
            telefonoError = null,
            continuarHabilitado = false,
            onModoChange = {},
            onBusquedaChange = {},
            onProductorClick = {},
            onVerMas = {},
            onNuevoProductorChange = {},
            onContinuar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1RegistradoPreview() {
    CafesmartTheme {
        RegistroCompraPaso1Screen(
            modo = ModoProductor.REGISTRADO,
            busqueda = "",
            productores = productoresDePrueba,
            hayMasProductores = true,
            productorElegido = productoresDePrueba[1],
            nuevoProductor = NuevoProductor(),
            documentoError = null,
            nombreError = null,
            telefonoError = null,
            continuarHabilitado = true,
            onModoChange = {},
            onBusquedaChange = {},
            onProductorClick = {},
            onVerMas = {},
            onNuevoProductorChange = {},
            onContinuar = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1NuevoPreview() {
    CafesmartTheme {
        RegistroCompraPaso1Screen(
            modo = ModoProductor.NUEVO,
            busqueda = "",
            productores = emptyList(),
            hayMasProductores = false,
            productorElegido = null,
            nuevoProductor = NuevoProductor(),
            documentoError = null,
            nombreError = null,
            telefonoError = null,
            continuarHabilitado = false,
            onModoChange = {},
            onBusquedaChange = {},
            onProductorClick = {},
            onVerMas = {},
            onNuevoProductorChange = {},
            onContinuar = {},
            onBack = {}
        )
    }
}
