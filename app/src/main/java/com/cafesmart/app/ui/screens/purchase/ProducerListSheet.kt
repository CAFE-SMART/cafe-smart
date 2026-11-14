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
import androidx.compose.foundation.lazy.LazyListScope
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

// Titulo, barra de busqueda y lista desplazable de productores
@Composable
private fun ListaProductores(
    estado: PurchaseUiState,
    onBusquedaChange: (String) -> Unit,
    onProductorClick: (Productor) -> Unit,
    onEditar: (Productor) -> Unit
) {
    ListaPersonas(
        titulo = "Productores",
        busqueda = estado.busquedaLista,
        onBusquedaChange = onBusquedaChange,
        mensajeSinResultados = estado.mensajeSinResultadosLista
    ) {
        items(estado.productoresEnLista) { fila ->
            ProducerRow(
                nombre = fila.nombre,
                documento = fila.documento,
                elegido = fila.elegido,
                onClick = { onProductorClick(fila.productor) },
                onEditar = { onEditar(fila.productor) }
            )
        }
    }
}

// Contenido de las hojas "Productores" y "Clientes": titulo, barra de busqueda y la lista desplazable.
// Cada hoja pone sus filas en [filas]; el resto es igual para las dos.
@Composable
fun ListaPersonas(
    titulo: String,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    // null cuando hay filas que mostrar
    mensajeSinResultados: String?,
    placeholder: String = PLACEHOLDER_BUSQUEDA,
    filas: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // imePadding(): el teclado no tapa la lista mientras se busca
            .imePadding()
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Text(text = titulo, style = AppTypography.titleModal)
        ProducerSearchField(busqueda = busqueda, onBusquedaChange = onBusquedaChange, placeholder = placeholder)

        if (mensajeSinResultados != null) {
            Text(text = mensajeSinResultados, style = AppTypography.bodyMd)
        } else {
            // LazyColumn solo dibuja las filas visibles: sirve aunque haya muchas.
            // heightIn limita el alto para que la lista se desplace dentro de la hoja.
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.s8),
                content = filas
            )
        }
    }
}

const val PLACEHOLDER_BUSQUEDA = "Buscar por nombre, cédula o NIT"

// Barra de busqueda: la usan las tarjetas "Productor registrado" y "Cliente registrado" y sus hojas
@Composable
fun ProducerSearchField(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    placeholder: String = PLACEHOLDER_BUSQUEDA
) {
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusquedaChange,
        // Campo de una sola linea.
        singleLine = true,
        textStyle = AppTypography.bodyLg,
        placeholder = { Text(text = placeholder, style = AppTypography.bodyLg.copy(color = TextMuted)) },
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

// Una persona (productor o cliente): nombre, "Cédula 123...", "Editar" (opcional) y circulo de seleccion
@Composable
fun ProducerRow(
    nombre: String,
    documento: String,
    elegido: Boolean,
    onClick: () -> Unit,
    // null: la fila no muestra "Editar"
    onEditar: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        // Fondo azul claro cuando el productor esta elegido.
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
            // weight(1f): el texto ocupa el espacio libre y deja el resto a la derecha
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nombre, style = AppTypography.valueStrong)
                Text(text = documento, style = AppTypography.bodyMd)
            }
            if (onEditar != null) {
                TextButton(onClick = onEditar) {
                    Text(text = "Editar", style = AppTypography.buttonLink)
                }
            }
            RadioCheck(selected = elegido)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProducerRowPreview() {
    CafesmartTheme {
        ProducerRow(
            nombre = "Pepito Pérez",
            documento = "Cédula 890301886",
            elegido = true,
            onClick = {},
            onEditar = {}
        )
    }
}
