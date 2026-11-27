package com.cafesmart.app.ui.gastos

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.GastoFormulario
import com.cafesmart.app.viewmodel.gastos.GastosUiState
import com.cafesmart.app.viewmodel.gastos.ResultadoGasto
import org.junit.Rule
import org.junit.Test

// Los textos de "demostración / temporal" solo son ciertos con el repositorio en memoria. En el Xiaomi se comprobó
// que con Room los gastos persisten al cerrar y reabrir la app, así que con Room esos textos no deben aparecer.
class GastosTextosModoTest {
    @get:Rule val compose = createComposeRule()

    private fun mostrar(contenido: @Composable () -> Unit) = compose.setContent { CafesmartTheme { contenido() } }
    private val conConcepto = GastosUiState(formulario = GastoFormulario(concepto = "Flete", monto = "30000"))

    // 1. Formulario "Registrar gasto"
    @Test fun formularioConRoomNoDiceDemostracion() {
        mostrar { RegistrarGastoScreen(GastosUiState(), {}, {}, {}, demostracion = false) }
        compose.onNodeWithTag("gastos_nota_formulario").assertTextEquals("Valores en COP, sin centavos.")
    }
    @Test fun formularioEnDemostracionLoDice() {
        mostrar { RegistrarGastoScreen(GastosUiState(), {}, {}, {}, demostracion = true) }
        compose.onNodeWithTag("gastos_nota_formulario").assertTextEquals("Demostración temporal · valores en COP, sin centavos.")
    }

    // 2. Confirmación
    @Test fun confirmacionConRoomDiceQueSeGuarda() {
        mostrar { GastosResultado(conConcepto.copy(resultado = ResultadoGasto.CONFIRMACION), {}, {}, {}, {}, demostracion = false) }
        compose.onNodeWithText("Se guardará en los gastos de tu cuenta.", substring = true).assertExists()
        compose.onNodeWithText("temporalmente", substring = true).assertDoesNotExist()
    }
    @Test fun confirmacionEnDemostracionDiceTemporal() {
        mostrar { GastosResultado(conConcepto.copy(resultado = ResultadoGasto.CONFIRMACION), {}, {}, {}, {}, demostracion = true) }
        compose.onNodeWithText("Se guardará temporalmente en esta demostración.", substring = true).assertExists()
    }

    // 3. Éxito (solo se muestra cuando el repositorio terminó bien; un error usa otra hoja)
    @Test fun exitoConRoomNoDiceQueNoSeGuardo() {
        mostrar { GastosResultado(conConcepto.copy(resultado = ResultadoGasto.EXITO), {}, {}, {}, {}, demostracion = false) }
        compose.onNodeWithText("El gasto se guardó correctamente.").assertExists()
        compose.onNodeWithText("No se guardó en una base de datos", substring = true).assertDoesNotExist()
    }
    @Test fun exitoEnDemostracionDiceListaTemporal() {
        mostrar { GastosResultado(conConcepto.copy(resultado = ResultadoGasto.EXITO), {}, {}, {}, {}, demostracion = true) }
        compose.onNodeWithText("El gasto se agregó a la lista temporal. No se guardó en una base de datos.").assertExists()
    }
    @Test fun errorSigueDiciendoQueNoSeGuardo() {
        mostrar { GastosResultado(conConcepto.copy(resultado = ResultadoGasto.ERROR), {}, {}, {}, {}, demostracion = false) }
        compose.onNodeWithText("No se pudo guardar el gasto. Inténtalo de nuevo.").assertExists()
        compose.onNodeWithText("El gasto se guardó correctamente.").assertDoesNotExist()
    }

    // 4. Selector de sublotes
    @Test fun selectorConRoomHablaDeTuInventario() {
        mostrar { AsociarSublotesScreen(GastosUiState(), {}, {}, demostracion = false) }
        compose.onNodeWithTag("gastos_sublotes_origen").assertTextEquals("Sublotes con café de tu inventario.")
    }
    @Test fun selectorEnDemostracionHablaDeEjemplo() {
        mostrar { AsociarSublotesScreen(GastosUiState(), {}, {}, demostracion = true) }
        compose.onNodeWithTag("gastos_sublotes_origen").assertTextEquals("Sublotes de ejemplo del inventario.")
    }

    // 5. Lista general
    @Test fun listaConRoomNoTieneAvisoDeDemostracion() {
        mostrar { ListaGastosScreen(GastosUiState(), {}, {}, {}, demostracion = false) }
        compose.onNodeWithTag("gastos_lista_aviso_demo").assertDoesNotExist()
    }
    @Test fun listaEnDemostracionTieneElAviso() {
        mostrar { ListaGastosScreen(GastosUiState(), {}, {}, {}, demostracion = true) }
        compose.onNode(hasTestTag("gastos_lista")).performScrollToNode(hasTestTag("gastos_lista_aviso_demo"))
        compose.onNodeWithTag("gastos_lista_aviso_demo").assertExists()
    }
}
