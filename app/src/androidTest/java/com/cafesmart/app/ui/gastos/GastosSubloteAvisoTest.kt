package com.cafesmart.app.ui.gastos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.GastosSubloteUiState
import org.junit.Rule
import org.junit.Test

// El aviso "Demostración… se pierden al cerrar la sesión" solo es cierto con el repositorio en memoria.
// Con Room los gastos sí se guardan, así que ahí no debe salir.
class GastosSubloteAvisoTest {
    @get:Rule val compose = createComposeRule()

    private fun mostrar(aviso: Boolean) = compose.setContent {
        CafesmartTheme { GastosSubloteScreen(GastosSubloteUiState(), {}, {}, mostrarAvisoDemostracion = aviso) }
    }

    @Test fun conRoomNoSaleElAvisoDeDemostracion() {
        mostrar(aviso = false)
        compose.onNodeWithTag("gastos_sublote_aviso_demo").assertDoesNotExist()
    }

    @Test fun conElRepositorioDeDemostracionSiSale() {
        mostrar(aviso = true)
        compose.onNodeWithTag("gastos_sublote_aviso_demo").assertExists()
    }
}
