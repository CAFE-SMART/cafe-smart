package com.cafesmart.app.ui.compra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.TipoDocumento
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ChoiceButton
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing

// Formulario que aparece dentro de la tarjeta "Productor nuevo".
// No guarda nada: cada cambio sale por onDatosChange con una copia de los datos.
@Composable
fun NuevoProductorForm(
    datos: NuevoProductor,
    documentoError: String?,
    nombreError: String?,
    telefonoError: String?,
    onDatosChange: (NuevoProductor) -> Unit,
    modifier: Modifier = Modifier
) {
    val esEmpresa = datos.tipo == TipoDocumento.NIT

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
                    onClick = { onDatosChange(datos.copy(tipo = TipoDocumento.CC)) },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    text = "NIT",
                    selected = esEmpresa,
                    onClick = { onDatosChange(datos.copy(tipo = TipoDocumento.NIT)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AppTextField(
            label = "Número de documento",
            value = datos.documento,
            onValueChange = { onDatosChange(datos.copy(documento = it)) },
            placeholder = if (esEmpresa) "Ej. 900123456-7" else "Ej. 1114059684",
            keyboardType = if (esEmpresa) KeyboardType.Phone else KeyboardType.Number,
            error = documentoError
        )
        AppTextField(
            label = if (esEmpresa) "Nombre de la empresa" else "Nombre completo",
            value = datos.nombre,
            onValueChange = { onDatosChange(datos.copy(nombre = it)) },
            placeholder = if (esEmpresa) "Ej. Café Los Alpes" else "Ej. Pepito Pérez",
            error = nombreError
        )
        AppTextField(
            label = "Celular (opcional)",
            value = datos.telefono,
            onValueChange = { onDatosChange(datos.copy(telefono = it)) },
            placeholder = "Ej. 3001234567",
            keyboardType = KeyboardType.Phone,
            error = telefonoError
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NuevoProductorFormPreview() {
    CafesmartTheme {
        NuevoProductorForm(
            datos = NuevoProductor(),
            documentoError = null,
            nombreError = null,
            telefonoError = null,
            onDatosChange = {},
            modifier = Modifier.padding(Spacing.s16)
        )
    }
}
