package com.cafesmart.app.ui.screens.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.purchase.TipoDocumento
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ChoiceButton
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing

// Vista (MVVM): formulario que aparece dentro de la tarjeta "Productor nuevo".
// Solo muestra lo que recibe y avisa cada cambio; la limpieza y validacion las hace el ViewModel.
@Composable
fun NewProducerForm(
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(text = "Tipo de documento", style = AppTypography.labelField)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                ChoiceButton(
                    text = "Cédula",
                    selected = !esEmpresa,
                    onClick = { onTipoDocumentoChange(TipoDocumento.CC) },
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
            placeholder = if (esEmpresa) "Ej. 9001234567" else "Ej. 1114059684",
            keyboardType = KeyboardType.Number,
            helper = if (esEmpresa) "Solo números: 9 o 10." else "Solo números: de 6 a 10.",
            error = documentoError
        )
        AppTextField(
            label = if (esEmpresa) "Nombre de la empresa" else "Nombre completo",
            value = nombre,
            onValueChange = onNombreChange,
            placeholder = if (esEmpresa) "Ej. Café Los Alpes" else "Ej. Pepito Pérez",
            error = nombreError
        )
        AppTextField(
            label = "Celular (opcional)",
            value = telefono,
            onValueChange = onTelefonoChange,
            placeholder = "Ej. 3001234567",
            keyboardType = KeyboardType.Phone,
            error = telefonoError
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewProducerFormPreview() {
    CafesmartTheme {
        NewProducerForm(
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
            modifier = Modifier.padding(Spacing.s16)
        )
    }
}
