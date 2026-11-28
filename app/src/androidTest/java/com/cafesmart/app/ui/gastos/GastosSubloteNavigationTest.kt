package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.model.*
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Recorre AppNavHost real; los datos sintéticos se inyectan exclusivamente en pruebas. */
class GastosSubloteNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private val repo = DemoGastosRepository()

    private fun abrir(batchId: String) {
        compose.setContent {
            nav = rememberNavController()
            CafesmartTheme { Scaffold { AppNavHost(Modifier.padding(it), nav) } }
            LaunchedEffect(Unit) {
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        compose.onNodeWithText("Registrar gasto").assertExists()
        compose.runOnIdle {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = GastosViewModel(repo) as T
            }
            ViewModelProvider(nav.getBackStackEntry(Routes.HOME), factory)[GastosViewModel::class.java]
            nav.navigate(Routes.inventoryDetail(batchId))
        }
        // El detalle solo muestra "Ver gastos" si el sublote existe en Room; aquí los datos son sintéticos,
        // así que se navega directo a la ruta de gastos (el detalle queda debajo en la pila).
        compose.runOnIdle { nav.navigate(Routes.inventoryBatchExpenses(batchId)) }
    }

    @Test fun vacioYVolverConservanElDetalleYBatchId() {
        abrir("2")
        compose.onNodeWithText("Este sublote todavía no tiene gastos registrados.").assertIsDisplayed()
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(0))
        compose.runOnIdle { assertEquals("2", nav.currentBackStackEntry?.arguments?.getString(Routes.ARG_BATCH_ID)) }
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.runOnIdle {
            assertEquals(Routes.INVENTORY_DETAIL, nav.currentDestination?.route)
            assertEquals("2", nav.currentBackStackEntry?.arguments?.getString(Routes.ARG_BATCH_ID))
        }
    }

    @Test fun repartoFiltraYActualizaLaPantallaAbierta() {
        val gasto = Gasto(1, "Transporte compartido", "Entrega de prueba", TipoGasto.TRANSPORTE,
            50000, Fecha(9, 11, 2026), EstadoPago.PAGADO, AplicaGasto.SUBLOTES)
        runBlocking {
            repo.guardar(gasto, listOf(GastoSublote(1, 1, 30000), GastoSublote(1, 2, 20000)))
            repo.guardar(gasto.copy(id = 2, concepto = "Solo otro sublote"), listOf(GastoSublote(2, 2, 50000)))
        }
        abrir("1")
        compose.onNodeWithText("Transporte compartido").assertIsDisplayed()
        compose.onNodeWithText("Solo otro sublote").assertDoesNotExist()
        // La fila (diseño del Figma, tarea 16) muestra solo lo asignado a ESTE sublote, sin el prefijo "Valor
        // asignado:" del diseño anterior: $30.000 de los $50.000 del gasto, nunca el monto global.
        val lista = compose.onNodeWithTag("gastos_sublote_lista")
        lista.performScrollToNode(hasTestTag("gastos_sublote_fila_1"))
        compose.onNode(hasText(dineroGasto(30000)) and hasAnyAncestor(hasTestTag("gastos_sublote_fila_1"))).assertIsDisplayed()
        compose.onNode(hasText(dineroGasto(50000)) and hasAnyAncestor(hasTestTag("gastos_sublote_fila_1"))).assertDoesNotExist()
        lista.performScrollToNode(hasTestTag("gastos_sublote_total"))
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(30000))
        runBlocking { repo.guardar(gasto.copy(id = 3, concepto = "Segundo gasto", monto = 7000), listOf(GastoSublote(3, 1, 7000))) }
        lista.performScrollToNode(hasTestTag("gastos_sublote_total"))
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(37000))
        lista.performScrollToNode(hasText("Segundo gasto"))
        compose.onNodeWithText("Segundo gasto").assertIsDisplayed()
        compose.runOnIdle { nav.popBackStack(); nav.navigate(Routes.inventoryBatchExpenses("2")) }
        compose.onNodeWithTag("gastos_sublote_total").assertTextEquals(dineroGasto(70000))
        compose.onNodeWithText("Segundo gasto").assertDoesNotExist()
    }

    @Test fun identificadorInvalidoMuestraMensajeYPermiteVolver() {
        abrir("invalido")
        compose.onNodeWithText("El identificador del sublote no es válido.").assertIsDisplayed()
        // Verifica el botón real de la pantalla y su callback a Navigation. La inyección
        // de KEYCODE_BACK con Espresso exige una raíz con foco; no es lo que prueba este caso.
        compose.onNodeWithContentDescription("Volver").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(Routes.INVENTORY_DETAIL, nav.currentDestination?.route)
            assertEquals("invalido", nav.currentBackStackEntry?.arguments?.getString(Routes.ARG_BATCH_ID))
        }
        compose.onNodeWithTag("detalle_sublote_no_encontrado").assertIsDisplayed()
    }

    @Test fun errorPermiteReintentar() {
        var reintentos = 0
        compose.setContent { CafesmartTheme {
            GastosSubloteScreen(GastosSubloteUiState(error = "Consulta fallida", recuperable = true), {}, { reintentos++ })
        } }
        compose.onNodeWithText("Consulta fallida").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, reintentos) }
    }
}
