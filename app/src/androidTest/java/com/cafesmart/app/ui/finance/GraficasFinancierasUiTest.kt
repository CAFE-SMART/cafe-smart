package com.cafesmart.app.ui.finance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.finance.GraficasUiState
import com.cafesmart.app.viewmodel.finance.MesFinanciero
import org.junit.Rule
import org.junit.Test

// Gráficas financieras dibujadas con Canvas. Solo Compose con un estado armado: sin Room ni Firebase.
class GraficasFinancierasUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sinDatosMuestraElEstadoVacio() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(GraficasUiState(cargando = false, serie = List(6) { MesFinanciero(2026, it + 6, 0, 0, 0) })) } }
        compose.onNodeWithTag("graficas_vacias").assertIsDisplayed()
        compose.onNodeWithTag("graficas_canvas").assertDoesNotExist()
    }

    @Test fun conDatosDibujaLasBarrasConLeyendaYMeses() {
        val serie = listOf(MesFinanciero(2026, 10, 1_200_000, 300_000, 120_000), MesFinanciero(2026, 11, 3_000_000, 90_000, -60_000))
        compose.setContent { CafesmartTheme { GraficasFinancieras(GraficasUiState(cargando = false, serie = serie)) } }
        compose.onNodeWithTag("graficas_canvas").assertIsDisplayed()
        compose.onNodeWithText("Ingresos").assertIsDisplayed()
        compose.onNodeWithText("Utilidad").assertIsDisplayed()
        compose.onNodeWithText("Oct 2026").assertIsDisplayed()
        compose.onNodeWithText("Nov").assertIsDisplayed()
    }

    @Test fun origenParcialSoloMuestraGastos() {
        val serie = listOf(MesFinanciero(2026, 11, 0, 90000, 0))
        compose.setContent { CafesmartTheme { GraficasFinancieras(GraficasUiState(cargando = false, serie = serie), soloGastos = true) } }
        compose.onNodeWithTag("graficas_canvas").assertIsDisplayed()
        compose.onNodeWithText("Gastos").assertIsDisplayed()
        compose.onNodeWithText("Ingresos").assertDoesNotExist()
        compose.onNodeWithText("Utilidad").assertDoesNotExist()
    }
}
