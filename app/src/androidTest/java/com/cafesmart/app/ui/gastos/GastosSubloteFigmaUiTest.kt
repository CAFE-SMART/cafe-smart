package com.cafesmart.app.ui.gastos

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cafesmart.app.data.gastos.GastoDelSublote
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.GastosSubloteUiState
import com.cafesmart.app.viewmodel.gastos.dineroGasto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Gastos del sublote según el Figma: chips, pestañas, "Registrar gasto" y "Actualizar".
class GastosSubloteFigmaUiTest {
    @get:Rule val compose = createComposeRule()

    private fun fila(id: Long, estado: EstadoPago, monto: Long) = GastoDelSublote(
        Gasto(id, "Gasto $id", "", TipoGasto.CARGUE, monto, Fecha(3, 6, 2026), estado, AplicaGasto.SUBLOTES), 1, monto)
    private val estado = GastosSubloteUiState(
        sublote = Sublote(1, 1, 1, null, 120.0, 120.0, 14_000, Fecha(1, 6, 2026)),
        gastos = listOf(fila(1, EstadoPago.PENDIENTE, 15_000), fila(2, EstadoPago.PAGADO, 65_000)),
    )

    @Test fun chipsPestanasYAcciones() {
        var registrar = 0
        var actualizar = 0
        compose.setContent {
            CafesmartTheme {
                GastosSubloteScreen(estado, {}, {}, onRegistrarGasto = { registrar++ }, onActualizar = { actualizar++ })
            }
        }
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(80_000))
        compose.onNodeWithTag("gastos_sublote_total_pagado", useUnmergedTree = true).assertTextEquals("Pagado ${dineroGasto(65_000)}")
        compose.onNodeWithTag("gastos_sublote_total_pendiente", useUnmergedTree = true).assertTextEquals("Pendiente ${dineroGasto(15_000)}")
        compose.onNodeWithTag("gastos_sublote_fila_1").assertExists()
        compose.onNodeWithTag("gastos_sublote_fila_2").assertExists()

        compose.onNodeWithText("Pendientes").performClick()
        compose.onNodeWithTag("gastos_sublote_fila_2").assertDoesNotExist()
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(80_000)) // el total no cambia

        compose.onNodeWithTag("gastos_sublote_registrar").performClick()
        compose.onNodeWithTag("gastos_sublote_actualizar").performClick()
        compose.runOnIdle { assertEquals(1, registrar); assertEquals(1, actualizar) }
    }

    @Test fun pestanaSinGastosMuestraSuEstadoVacio() {
        compose.setContent {
            CafesmartTheme { GastosSubloteScreen(estado.copy(gastos = listOf(fila(2, EstadoPago.PAGADO, 1_000))), {}, {}) }
        }
        compose.onNodeWithText("Pendientes").performClick()
        compose.onNodeWithTag("gastos_sublote_vacio").assertTextEquals("Este sublote no tiene gastos pendientes.")
    }
}
