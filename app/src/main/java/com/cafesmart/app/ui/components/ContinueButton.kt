package com.cafesmart.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.ButtonDisabled
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary

@Composable
fun ContinueButton(
    text: String = "Continuar",
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary,
            disabledContainerColor = ButtonDisabled,
            disabledContentColor = OnPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text = text, style = AppTypography.buttonPrimary)
    }
}

@Preview(showBackground = true)
@Composable
private fun ContinueButtonPreview() {
    CafesmartTheme {
        ContinueButton(onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ContinueButtonDisabledPreview() {
    CafesmartTheme {
        ContinueButton(onClick = {}, enabled = false)
    }
}
