package com.cafesmart.app.ui.screens.sale

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.screens.purchase.ListaPersonas
import com.cafesmart.app.ui.screens.purchase.NewProducerForm
import com.cafesmart.app.ui.screens.purchase.ProducerRow
import com.cafesmart.app.viewmodel.sale.SaleUiState

// Hoja "Ver más clientes": busca, elige y modifica clientes registrados.
// Vista (MVVM): no tiene logica; todo llega del estado y cada accion sale por una lambda.
// Usa la misma lista, las mismas filas y el mismo formulario que la hoja "Productores" de Compras.
@Composable
fun ClientListSheet(
    estado: SaleUiState,
    onBusquedaChange: (String) -> Unit,
    onClienteClick: (Cliente) -> Unit,
    onEditar: (Cliente) -> Unit,
    onTipoEdicionChange: (TipoDocumento) -> Unit,
    onDocumentoEdicionChange: (String) -> Unit,
    onNombreEdicionChange: (String) -> Unit,
    onTelefonoEdicionChange: (String) -> Unit,
    onGuardarEdicion: () -> Unit,
    onCancelarEdicion: () -> Unit,
    onCerrar: () -> Unit
) {
    // Hoja del equipo, abierta completa: se cierra deslizando hacia abajo o tocando por fuera
    AppBottomSheet(onDismiss = onCerrar, skipPartiallyExpanded = true) {
        // Si hay un cliente en modificacion muestra el formulario; si no, la lista
        if (estado.clienteEnEdicion != null) {
            NewProducerForm(
                titulo = "Modificar cliente",
                esEmpresa = estado.edicionEsEmpresa,
                documento = estado.datosEdicion.documento,
                nombre = estado.datosEdicion.nombre,
                telefono = estado.datosEdicion.telefono,
                documentoError = estado.edicionDocumentoError,
                nombreError = estado.edicionNombreError,
                telefonoError = estado.edicionTelefonoError,
                onTipoDocumentoChange = onTipoEdicionChange,
                onDocumentoChange = onDocumentoEdicionChange,
                onNombreChange = onNombreEdicionChange,
                onTelefonoChange = onTelefonoEdicionChange,
                textoGuardar = "Guardar cambios",
                guardarHabilitado = estado.guardarEdicionHabilitado,
                onGuardar = onGuardarEdicion,
                onCancelar = onCancelarEdicion
            )
        } else {
            ListaPersonas(
                titulo = "Clientes",
                busqueda = estado.busquedaLista,
                onBusquedaChange = onBusquedaChange,
                mensajeSinResultados = estado.mensajeSinResultadosLista,
                placeholder = PLACEHOLDER_BUSQUEDA_CLIENTE
            ) {
                items(estado.clientesEnLista) { fila ->
                    ProducerRow(
                        nombre = fila.nombre,
                        documento = fila.documento,
                        elegido = fila.elegido,
                        onClick = { onClienteClick(fila.cliente) },
                        onEditar = { onEditar(fila.cliente) }
                    )
                }
            }
        }
    }
}
