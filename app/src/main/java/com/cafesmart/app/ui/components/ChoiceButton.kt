package com.cafesmart.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceSelected

// Chip de opcion para elegir una entre varias (Cedula / NIT, Verde / Seco...).
// Guia: alto 48, radio 12; seleccionado con borde 2 px primary y fondo surface/selected
@Composable
fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        selected = selected,
        shape = AppShapes.medium,
        color = if (selected) SurfaceSelected else SurfaceCard,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Primary else BorderField
        ),
        modifier = modifier.height(48.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = if (selected) AppTypography.titleCard.copy(color = PrimaryDark) else AppTypography.titleCard
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChoiceButtonPreview() {
    CafesmartTheme {
        Row(
            modifier = Modifier.padding(Spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            ChoiceButton(text = "Cédula", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            ChoiceButton(text = "NIT", selected = false, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}
