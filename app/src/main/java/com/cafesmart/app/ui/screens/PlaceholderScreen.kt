package com.cafesmart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.TextMuted

/** Botón de navegación que se muestra dentro de un placeholder. */
data class PlaceholderAction(val label: String, val onClick: () -> Unit)

/**
 * Pantalla temporal. Se reemplaza por la pantalla real cuando se implemente
 * (basta con cambiar el contenido del `composable(...)` en AppNavHost).
 */
@Composable
fun PlaceholderScreen(
    title: String,
    route: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: List<PlaceholderAction> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s16, bottom = Spacing.s20),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        if (onBack != null) {
            BackArrow(onClick = onBack)
        }
        Text(text = title, style = AppTypography.titleScreen)
        Text(text = "Ruta: $route", style = AppTypography.caption, color = TextMuted)
        Text(text = "Pantalla pendiente de implementar.", style = AppTypography.bodyMd)

        actions.forEach { action ->
            ContinueButton(text = action.label, onClick = action.onClick)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlaceholderScreenPreview() {
    CafesmartTheme {
        PlaceholderScreen(
            title = "Inventario",
            route = "inventory",
            onBack = {},
            actions = listOf(PlaceholderAction("Ver lista") {})
        )
    }
}
