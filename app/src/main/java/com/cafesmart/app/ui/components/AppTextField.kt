package com.cafesmart.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.TextMuted

/**
 * Campo compartido del proyecto. [singleLine] y [minLines] permiten descripción opcional;
 * [compact] aplica la variante de 48 dp sin recortar los errores y [enabled] bloquea
 * la edición durante una operación. Los valores por defecto conservan los usos existentes.
 */
@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingText: String? = null,
    // Texto de ayuda debajo del campo (p. ej. "Máx. 99.999 kg")
    helper: String? = null,
    // Mensaje de error debajo del campo; null si el valor es valido. Reemplaza al helper.
    error: String? = null,
    // Variantes opcionales: mantienen la firma y el aspecto de los formularios existentes.
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    // Variante de 48 dp del Figma de Gastos. Los errores quedan fuera de la altura fija.
    compact: Boolean = false
) {
    // Etiqueta arriba y campo debajo.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s8)
    ) {
        Text(text = label, style = AppTypography.labelField)
        // El campo guarda lo escrito con la posición del cursor. Si el ViewModel devuelve otro texto (por ejemplo,
        // agrega el punto de miles: "1234" -> "1.234"), el cursor pasa al final; sin esto quedaba atrás y lo que se
        // escribía después entraba en otro lugar ("1.234,567" terminaba en "123,456").
        var campo by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
        val mostrado = if (campo.text == value) campo else TextFieldValue(value, TextRange(value.length))
        // Campo de texto de Material con borde.
        OutlinedTextField(
            value = mostrado,
            // Avisa cada letra escrita; el ViewModel limpia y devuelve el valor. Mover el cursor no avisa nada.
            onValueChange = { nuevo ->
                campo = nuevo
                if (nuevo.text != value) onValueChange(nuevo.text)
            },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            textStyle = AppTypography.bodyLg,
            // TextMuted (#64748B) tiene contraste 4,8:1 sobre blanco; la guia pide minimo 4,5:1 (RNF-27)
            placeholder = { Text(text = placeholder, style = AppTypography.bodyLg.copy(color = TextMuted)) },
            // Texto a la izquierda del campo, por ejemplo "$". null si no se pidio.
            leadingIcon = if (leadingText != null) {
                { Text(text = leadingText, style = AppTypography.valueStrongBold, modifier = Modifier.padding(start = Spacing.s4)) }
            } else {
                null
            },
            // Tipo de teclado que se abre.
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            // Si hay error, el borde se pone rojo.
            isError = error != null,
            // Debajo del campo: el error si existe; si no, la ayuda.
            supportingText = if (compact) null else if (error != null) {
                { Text(text = error, style = AppTypography.helper.copy(color = ErrorText)) }
            } else if (helper != null) {
                { Text(text = helper, style = AppTypography.helper) }
            } else {
                null
            },
            shape = AppShapes.medium,
            // Colores del tema del equipo.
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderField,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                errorBorderColor = ErrorDot,
                errorContainerColor = SurfaceCard
            ),
            modifier = Modifier.fillMaxWidth().then(if (compact && singleLine) Modifier.height(48.dp) else Modifier)
        )
        if (compact && (error != null || helper != null)) {
            Text(error ?: helper.orEmpty(), style = AppTypography.helper,
                color = if (error != null) ErrorText else TextMuted)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTextFieldPreview() {
    CafesmartTheme {
        AppTextField(
            label = "Precio x kg",
            value = "",
            onValueChange = {},
            placeholder = "ej. 14.000",
            leadingText = "$",
            modifier = Modifier.padding(Spacing.s16)
        )
    }
}
