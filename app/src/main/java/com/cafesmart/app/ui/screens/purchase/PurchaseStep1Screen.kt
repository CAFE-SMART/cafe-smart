package com.cafesmart.app.ui.screens.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.AvisoCapacidadCard
import com.cafesmart.app.ui.components.EstiloAviso
import com.cafesmart.app.ui.components.OptionCard
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceScreen
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
    onBusquedaListaChange: (String) -> Unit,
    onEditarProductor: (Productor) -> Unit,
    onTipoEdicionChange: (TipoDocumento) -> Unit,
    onDocumentoEdicionChange: (String) -> Unit,
    onNombreEdicionChange: (String) -> Unit,
    onTelefonoEdicionChange: (String) -> Unit,
    onGuardarEdicion: () -> Unit,
    onCancelarEdicion: () -> Unit,
    onCerrarLista: () -> Unit,
    onGuardarNuevo: () -> Unit,
    onCancelarNuevo: () -> Unit,
    onTipoDocumentoChange: (TipoDocumento) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // "Configurar ahora" del aviso de capacidad sin configurar: abre Ajustes → Capacidad de bodega.
    onConfigurarCapacidad: () -> Unit = {}
) {
    // Estado visual: si la hoja "Productores" esta abierta
    var mostrarLista by remember { mutableStateOf(false) }
    // Estado visual: si la hoja "Productor nuevo" esta abierta
    var mostrarNuevo by remember { mutableStateOf(false) }

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
            // Encabezado comun: flecha, titulo, paso y pregunta.
            PurchaseHeader(paso = 1, pregunta = "¿A quién le compras el café?", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                // Lo que va entre llaves al final solo se muestra cuando la tarjeta esta seleccionada.
                OptionCard(
                    title = "Productor registrado",
                    description = "Elígelo de tu lista.",
                    icon = AppIcons.Search,
                    // El ViewModel dice si esta tarjeta esta seleccionada.
                    selected = estado.registradoSeleccionado,
                    // Avisa al ViewModel la opcion elegida.
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
                        onVerMas = { mostrarLista = true }
                    )
                }

                OptionCard(
                    title = "Sin registrar",
                    description = "Compra rápida, sin datos.",
                    icon = AppIcons.User,
                    selected = estado.sinRegistrarSeleccionado,
                    onClick = { onModoChange(ModoProductor.SIN_REGISTRAR) }
                )

                // No queda seleccionada: al tocarla se abre la hoja con el formulario
                OptionCard(
                    title = "Productor nuevo",
                    description = "Guarda sus datos.",
                    icon = AppIcons.UserPlus,
                    selected = false,
                    onClick = { mostrarNuevo = true }
                )

                // Informativo: la capacidad de la bodega aun no esta configurada. No bloquea nada.
                if (estado.avisoSinConfigurar) {
                    AvisoCapacidadCard(
                        titulo = "Falta configurar la capacidad de tu bodega",
                        mensaje = "Así te avisamos cuando se esté llenando. Es solo un aviso: puedes seguir con tu compra.",
                        estilo = EstiloAviso.SIN_CONFIGURAR,
                        textoAccion = "Configurar ahora ›",
                        onAccion = onConfigurarCapacidad
                    )
                }
            }
        }

        // Pie fijo abajo: mensaje de lo que falta y boton Continuar.
        PurchaseBottomBar {
            PendingMessage(mensaje = estado.mensajePendientePaso1)
            ContinueButton(
                text = "Continuar",
                // El boton solo se activa cuando el ViewModel lo permite.
                enabled = estado.continuarPaso1Habilitado,
                onClick = onContinuar
            )
        }
    }

    if (mostrarNuevo) {
        // Deslizar para cerrar conserva lo escrito; "Cancelar" lo borra
        AppBottomSheet(onDismiss = { mostrarNuevo = false }, skipPartiallyExpanded = true) {
            NewProducerForm(
                titulo = "Productor nuevo",
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
                onTelefonoChange = onTelefonoChange,
                textoGuardar = "Guardar productor",
                guardarHabilitado = estado.guardarNuevoHabilitado,
                onGuardar = {
                    onGuardarNuevo()
                    mostrarNuevo = false
                },
                onCancelar = {
                    onCancelarNuevo()
                    mostrarNuevo = false
                }
            )
        }
    }

    if (mostrarLista) {
        ProducerListSheet(
            estado = estado,
            onBusquedaChange = onBusquedaListaChange,
            // Al elegir un productor se marca y la hoja se cierra
            onProductorClick = {
                onProductorClick(it)
                onCerrarLista()
                mostrarLista = false
            },
            onEditar = onEditarProductor,
            onTipoEdicionChange = onTipoEdicionChange,
            onDocumentoEdicionChange = onDocumentoEdicionChange,
            onNombreEdicionChange = onNombreEdicionChange,
            onTelefonoEdicionChange = onTelefonoEdicionChange,
            onGuardarEdicion = onGuardarEdicion,
            onCancelarEdicion = onCancelarEdicion,
            onCerrar = {
                onCerrarLista()
                mostrarLista = false
            }
        )
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
        ProducerSearchField(busqueda = busqueda, onBusquedaChange = onBusquedaChange)

        // Si no hay productores muestra el mensaje; si hay, muestra la lista.
        if (mensajeListaVacia != null) {
            Text(text = mensajeListaVacia, style = AppTypography.bodyMd)
        } else {
            Text(text = titulo, style = AppTypography.labelField)
            // forEach dibuja una fila por cada productor.
            productores.forEach { fila ->
                ProducerRow(
                    fila = fila,
                    // Avisa al ViewModel cual productor se eligio.
                    onClick = { onProductorClick(fila.productor) }
                )
            }
            // El boton solo aparece si quedan productores por mostrar.
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

// ---------------- Previews (con estados de ejemplo) ----------------

// Datos solo para ver las Preview en Android Studio.
private val productoresDePrueba = listOf(
    Productor(nombre = "Pepito Pérez", documento = "890301886"),
    Productor(nombre = "Andy López", documento = "11140596")
)

@Composable
// Preview comun que recibe un estado distinto en cada caso.
private fun Paso1Preview(estado: PurchaseUiState) {
    CafesmartTheme {
        PurchaseStep1Screen(
            estado = estado,
            onModoChange = {},
            onBusquedaChange = {},
            onProductorClick = {},
            onBusquedaListaChange = {},
            onEditarProductor = {},
            onTipoEdicionChange = {},
            onDocumentoEdicionChange = {},
            onNombreEdicionChange = {},
            onTelefonoEdicionChange = {},
            onGuardarEdicion = {},
            onCancelarEdicion = {},
            onCerrarLista = {},
            onGuardarNuevo = {},
            onCancelarNuevo = {},
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
private fun Paso1SinRegistrarPreview() {
    Paso1Preview(PurchaseUiState(modo = ModoProductor.SIN_REGISTRAR))
}
