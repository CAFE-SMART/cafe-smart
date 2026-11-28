package com.cafesmart.app.ui.screens.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.screens.purchase.PendingMessage
import com.cafesmart.app.ui.components.OptionCard
import com.cafesmart.app.ui.screens.purchase.BuscadorPersonas
import com.cafesmart.app.ui.screens.purchase.CamposPersona
import com.cafesmart.app.ui.screens.purchase.ProducerRow
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.screens.purchase.PurchaseHeader
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.sale.SaleUiState

// Buscador de los clientes: el Figma de Ventas dice "documento" en lugar de "cédula o NIT".
const val PLACEHOLDER_BUSQUEDA_CLIENTE = "Buscar por nombre o documento"

// Paso 1 de Ventas: ¿A quién le vendes?
// Vista (MVVM): no tiene logica. Recibe el estado del ViewModel ya listo (true/false y textos)
// y avisa cada accion con una lambda. Usa los mismos componentes que el paso 1 de Compras.
@Composable
fun SaleStep1Screen(
    estado: SaleUiState,
    onModoChange: (ModoCliente) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onClienteClick: (Cliente) -> Unit,
    onBusquedaListaChange: (String) -> Unit,
    onCerrarLista: () -> Unit,
    onEditarCliente: (Cliente) -> Unit,
    onTipoEdicionChange: (TipoDocumento) -> Unit,
    onDocumentoEdicionChange: (String) -> Unit,
    onNombreEdicionChange: (String) -> Unit,
    onTelefonoEdicionChange: (String) -> Unit,
    onGuardarEdicion: () -> Unit,
    onCancelarEdicion: () -> Unit,
    onTipoDocumentoChange: (TipoDocumento) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado visual: si la hoja "Clientes" esta abierta
    var mostrarLista by rememberSaveable { mutableStateOf(false) }

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
            PurchaseHeader(
                paso = 1,
                pregunta = "¿A quién le vendes?",
                onBack = onBack,
                titulo = "Registro de venta"
            )

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                // Lo que va entre llaves al final solo se muestra cuando la tarjeta esta seleccionada.
                OptionCard(
                    title = "Cliente registrado",
                    description = "Elígelo de tu lista.",
                    icon = AppIcons.Search,
                    // El ViewModel dice si esta tarjeta esta seleccionada.
                    selected = estado.registradoSeleccionado,
                    // Avisa al ViewModel la opcion elegida.
                    onClick = { onModoChange(ModoCliente.REGISTRADO) }
                ) {
                    BuscadorPersonas(
                        busqueda = estado.busqueda,
                        titulo = estado.tituloLista,
                        mensajeListaVacia = estado.mensajeListaVacia,
                        hayMas = estado.hayMasClientes,
                        textoVerMas = "Ver más clientes",
                        onBusquedaChange = onBusquedaChange,
                        onVerMas = { mostrarLista = true },
                        placeholder = PLACEHOLDER_BUSQUEDA_CLIENTE
                    ) {
                        // forEach dibuja una fila por cada cliente.
                        estado.clientesEnPantalla.forEach { fila ->
                            ProducerRow(
                                nombre = fila.nombre,
                                documento = fila.documento,
                                elegido = fila.elegido,
                                // Avisa al ViewModel cual cliente se eligio.
                                onClick = { onClienteClick(fila.cliente) }
                            )
                        }
                    }
                }

                OptionCard(
                    title = "Sin registrar",
                    description = "Venta rápida, sin datos.",
                    icon = AppIcons.User,
                    selected = estado.sinRegistrarSeleccionado,
                    onClick = { onModoChange(ModoCliente.SIN_REGISTRAR) }
                )

                // Aqui el formulario va dentro de la tarjeta; "Continuar" registra al cliente y sigue.
                OptionCard(
                    title = "Cliente nuevo",
                    description = "Guarda sus datos.",
                    icon = AppIcons.UserPlus,
                    selected = estado.nuevoSeleccionado,
                    onClick = { onModoChange(ModoCliente.NUEVO) }
                ) {
                    CamposPersona(
                        esEmpresa = estado.esEmpresa,
                        documento = estado.nuevoCliente.documento,
                        nombre = estado.nuevoCliente.nombre,
                        telefono = estado.nuevoCliente.telefono,
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

        // Pie fijo abajo con el boton Continuar.
        PurchaseBottomBar {
            // Si no se pudo leer o guardar un cliente en Room (texto propuesto, falta confirmarlo con el equipo)
            estado.errorCliente?.let {
                Text(
                    text = it,
                    style = AppTypography.caption.copy(color = ErrorText),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            PendingMessage(mensaje = estado.mensajePendientePaso1)
            ContinueButton(
                text = "Continuar",
                // El boton solo se activa cuando el ViewModel lo permite, y no mientras se guarda el cliente.
                enabled = estado.continuarPaso1Habilitado && !estado.guardandoCliente,
                onClick = onContinuar
            )
        }
    }

    if (mostrarLista) {
        ClientListSheet(
            estado = estado,
            onBusquedaChange = onBusquedaListaChange,
            // Al elegir un cliente se marca y la hoja se cierra
            onClienteClick = {
                onClienteClick(it)
                onCerrarLista()
                mostrarLista = false
            },
            onEditar = onEditarCliente,
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

// ---------------- Previews (con estados de ejemplo) ----------------

@Composable
// Preview comun que recibe un estado distinto en cada caso.
private fun Paso1Preview(estado: SaleUiState) {
    CafesmartTheme {
        SaleStep1Screen(
            estado = estado,
            onModoChange = {},
            onBusquedaChange = {},
            onClienteClick = {},
            onBusquedaListaChange = {},
            onCerrarLista = {},
            onEditarCliente = {},
            onTipoEdicionChange = {},
            onDocumentoEdicionChange = {},
            onNombreEdicionChange = {},
            onTelefonoEdicionChange = {},
            onGuardarEdicion = {},
            onCancelarEdicion = {},
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
    Paso1Preview(SaleUiState())
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1RegistradoPreview() {
    Paso1Preview(SaleUiState(modo = ModoCliente.REGISTRADO))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1ClienteNuevoPreview() {
    Paso1Preview(SaleUiState(modo = ModoCliente.NUEVO))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso1SinRegistrarPreview() {
    Paso1Preview(SaleUiState(modo = ModoCliente.SIN_REGISTRAR))
}
