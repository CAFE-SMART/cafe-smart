package com.cafesmart.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    error: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s8)
    ) {
        Text(text = label, style = AppTypography.labelField)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppTypography.bodyLg,
            // TextMuted (#64748B) tiene contraste 4,8:1 sobre blanco; la guia pide minimo 4,5:1 (RNF-27)
            placeholder = { Text(text = placeholder, style = AppTypography.bodyLg.copy(color = TextMuted)) },
            leadingIcon = if (leadingText != null) {
                { Text(text = leadingText, style = AppTypography.valueStrongBold, modifier = Modifier.padding(start = Spacing.s4)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error, style = AppTypography.helper.copy(color = ErrorText)) }
            } else if (helper != null) {
                { Text(text = helper, style = AppTypography.helper) }
            } else {
                null
            },
            shape = AppShapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderField,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                errorBorderColor = ErrorDot,
                errorContainerColor = SurfaceCard
            ),
            modifier = Modifier.fillMaxWidth()
        )
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
