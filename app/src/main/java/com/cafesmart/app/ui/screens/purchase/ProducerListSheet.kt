package com.cafesmart.app.ui.screens.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.RadioCheck
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.viewmodel.purchase.ProductorEnPantalla
import com.cafesmart.app.viewmodel.purchase.PurchaseUiState

// Hoja "Ver más productores": busca, selecciona y modifica productores.
// Vista (MVVM): no tiene logica; todo llega del estado y cada accion sale por una lambda.
@Composable
fun ProducerListSheet(
    estado: PurchaseUiState,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onEditar: (Productor) -> Unit,
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
        // Si hay un productor en edicion muestra el formulario; si no, la lista
        if (estado.productorEnEdicion != null) {
            // El mismo formulario de "Productor nuevo"
            NewProducerForm(
                titulo = "Modificar productor",
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
            ListaProductores(
                estado = estado,
                onBusquedaChange = onBusquedaChange,
                onProductorClick = onProductorClick,
                onEditar = onEditar
            )
        }
    }
}

// Titulo, barra de busqueda y lista desplazable
@Composable
private fun ListaProductores(
    estado: PurchaseUiState,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onEditar: (Productor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // imePadding(): el teclado no tapa la lista mientras se busca
            .imePadding()
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Text(text = "Productores", style = AppTypography.titleModal)
        ProducerSearchField(busqueda = estado.busquedaLista, onBusquedaChange = onBusquedaChange)

        val mensajeSinResultados = estado.mensajeSinResultadosLista
        if (mensajeSinResultados != null) {
            Text(text = mensajeSinResultados, style = AppTypography.bodyMd)
        } else {
            // LazyColumn solo dibuja las filas visibles: sirve aunque haya muchos productores.
            // heightIn limita el alto para que la lista se desplace dentro de la hoja.
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.s8)
            ) {
                items(estado.productoresEnLista) { fila ->
                    ProducerRow(
                        fila = fila,
                        onClick = { onProductorClick(fila.productor) },
                        onEditar = { onEditar(fila.productor) }
                    )
                }
            }
        }
    }
}

// Barra de busqueda: la usan la tarjeta "Productor registrado" y esta hoja
@Composable
fun ProducerSearchField(busqueda: String, onBusquedaChange: (String) -> Unit) {
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusquedaChange,
        // Campo de una sola linea.
        singleLine = true,
        textStyle = AppTypography.bodyLg,
        placeholder = { Text(text = "Buscar por nombre, cédula o NIT", style = AppTypography.bodyLg.copy(color = TextMuted)) },
        // Lupa a la izquierda del campo.
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
}

// Un productor: nombre, "Cédula 123...", "Editar" (opcional) y circulo de seleccion
@Composable
fun ProducerRow(
    fila: ProductorEnPantalla,
    onClick: () -> Unit,
    // null: la fila no muestra "Editar"
    onEditar: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        // Fondo azul claro cuando el productor esta elegido.
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
            // weight(1f): el texto ocupa el espacio libre y deja el resto a la derecha
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fila.nombre, style = AppTypography.valueStrong)
                Text(text = fila.documento, style = AppTypography.bodyMd)
            }
            if (onEditar != null) {
                TextButton(onClick = onEditar) {
                    Text(text = "Editar", style = AppTypography.buttonLink)
                }
            }
            RadioCheck(selected = fila.elegido)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProducerRowPreview() {
    CafesmartTheme {
        ProducerRow(
            fila = ProductorEnPantalla(
                productor = Productor(nombre = "Pepito Pérez", documento = "890301886"),
                nombre = "Pepito Pérez",
                documento = "Cédula 890301886",
                elegido = true
            ),
            onClick = {},
            onEditar = {}
        )
    }
}
