package com.cafesmart.app.ui.screens.purchase

import com.cafesmart.app.viewmodel.purchase.ayudaNombre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ChoiceButton
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing

// Vista (MVVM): formulario de las hojas "Productor nuevo" y "Modificar productor".
// Solo muestra lo que recibe y avisa cada cambio; la limpieza y validacion las hace el ViewModel.
@Composable
fun NewProducerForm(
    titulo: String,
    esEmpresa: Boolean,
    documento: String,
    nombre: String,
    telefono: String,
    documentoError: String?,
    nombreError: String?,
    telefonoError: String?,
    onTipoDocumentoChange: (TipoDocumento) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    textoGuardar: String,
    guardarHabilitado: Boolean,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // imePadding y scroll: el formulario sube con el teclado y se desplaza si no cabe
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Text(text = titulo, style = AppTypography.titleModal)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(text = "Tipo de documento", style = AppTypography.labelField)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                ChoiceButton(
                    text = "Cédula",
                    // ! niega: Cedula esta seleccionada cuando no es empresa.
                    selected = !esEmpresa,
                    // Avisa al ViewModel el tipo elegido.
                    onClick = { onTipoDocumentoChange(TipoDocumento.CC) },
                    // weight(1f): los dos botones se reparten el ancho por igual.
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    text = "NIT",
                    selected = esEmpresa,
                    onClick = { onTipoDocumentoChange(TipoDocumento.NIT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AppTextField(
            label = "Número de documento",
            value = documento,
            onValueChange = onDocumentoChange,
            // El ejemplo cambia segun el tipo de documento.
            placeholder = if (esEmpresa) "Ej. 9001234567" else "Ej. 1114059684",
            // Abre el teclado numerico.
            keyboardType = KeyboardType.Number,
            helper = if (esEmpresa) "Solo números: 9 o 10." else "Solo números: de 6 a 10.",
            error = documentoError
        )
        AppTextField(
            // La etiqueta cambia segun el tipo de documento.
            label = if (esEmpresa) "Nombre de la empresa" else "Nombre completo",
            value = nombre,
            onValueChange = onNombreChange,
            placeholder = if (esEmpresa) "Ej. Café Los Alpes" else "Ej. Pepito Pérez",
            // El máximo se ve mientras se escribe ("Solo letras. 12/30").
            helper = ayudaNombre(nombre, if (esEmpresa) TipoDocumento.NIT else TipoDocumento.CC),
            error = nombreError
        )
        AppTextField(
            label = "Celular (opcional)",
            value = telefono,
            onValueChange = onTelefonoChange,
            placeholder = "Ej. 3001234567",
            helper = "10 números, empieza por 3.",
            // Abre el teclado de telefono.
            keyboardType = KeyboardType.Phone,
            error = telefonoError
        )
        // Guardar solo se activa cuando todos los campos son validos
        ContinueButton(text = textoGuardar, enabled = guardarHabilitado, onClick = onGuardar)
        // Cancelar queda centrado debajo del boton principal
        TextButton(onClick = onCancelar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = "Cancelar", style = AppTypography.labelSection)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewProducerFormPreview() {
    CafesmartTheme {
        NewProducerForm(
            titulo = "Productor nuevo",
            esEmpresa = false,
            documento = "",
            nombre = "",
            telefono = "",
            documentoError = null,
            nombreError = null,
            telefonoError = null,
            onTipoDocumentoChange = {},
            onDocumentoChange = {},
            onNombreChange = {},
            onTelefonoChange = {},
            textoGuardar = "Guardar productor",
            guardarHabilitado = false,
            onGuardar = {},
            onCancelar = {}
        )
    }
}
