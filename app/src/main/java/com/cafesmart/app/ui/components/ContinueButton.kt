package com.cafesmart.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.ButtonDisabled
import com.cafesmart.app.ui.theme.ButtonShadow
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.TextMuted

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
        shape = AppShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary,
            disabledContainerColor = ButtonDisabled,
            disabledContentColor = TextMuted
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .let {
                if (enabled) {
                    it.shadow(
                        elevation = 16.dp,
                        shape = AppShapes.medium,
                        ambientColor = ButtonShadow,
                        spotColor = ButtonShadow
                    )
                } else {
                    it
                }
            }
    ) {
        Text(
            text = text,
            style = AppTypography.buttonPrimary,
            color = if (enabled) OnPrimary else TextMuted
        )
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
