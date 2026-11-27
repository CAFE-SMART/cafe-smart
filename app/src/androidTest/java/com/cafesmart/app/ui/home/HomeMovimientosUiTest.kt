package com.cafesmart.app.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cafesmart.app.data.finanzas.MovimientoFinanciero
import com.cafesmart.app.data.finanzas.TipoMovimiento
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.home.HomeUiState
import com.cafesmart.app.viewmodel.inventory.resumirInventario
import org.junit.Rule
import org.junit.Test

class HomeMovimientosUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun muestraMovimientosRealesYSenalaVentasNoConectadas() {
        val estado = HomeUiState(
            inventario = resumirInventario(emptyList(), 0.0), cargando = false,
            movimientos = listOf(MovimientoFinanciero(TipoMovimiento.COMPRA, Fecha(27, 11, 2026), "Compra · Sin registrar", "Café verde · 12 kg", -168000))
        )
        compose.setContent { CafesmartTheme { HomeScreen(estado = estado) } }
        compose.onNodeWithTag("inicio_lista").performScrollToNode(hasText("Movimientos recientes"))
        compose.onNodeWithText("Compra · Sin registrar").assertIsDisplayed()
        compose.onNodeWithText("Café verde · 12 kg · 27/11/2026").assertExists()
        compose.onNodeWithText("Las ventas aparecerán cuando estén conectadas.").assertExists()
    }
}
