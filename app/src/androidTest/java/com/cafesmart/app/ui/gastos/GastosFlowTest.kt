package com.cafesmart.app.ui.gastos

import android.graphics.Bitmap
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import android.widget.DatePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import android.view.View
import org.hamcrest.Matcher
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.gastos.*
import com.cafesmart.app.data.model.*
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Casos G01–G15 del enunciado actual. Los sublotes sintéticos solo existen en estas pruebas. */
class GastosFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var modelo: GastosViewModel

    /** Usa pantallas y hojas reales con repositorio inyectable para ejercitar el fallo. */
    private fun montar(lista: Boolean = false, sublotes: List<Sublote> = emptyList(), falla: Boolean = false) {
        modelo = if (falla) GastosViewModel(object : GastosRepository {
            override val gastos = MutableStateFlow<List<Gasto>>(emptyList())
            override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) { error("Fallo controlado de prueba") }
        }, sublotes) else GastosViewModel(sublotes = sublotes)
        compose.setContent {
            var mostrarLista by remember { mutableStateOf(lista) }
            val estado by modelo.uiState.collectAsState()
            CafesmartTheme {
                if (mostrarLista) ListaGastosScreen(estado, {}, { mostrarLista = false }, modelo::filtrar)
                else {
                    RegistrarGastoScreen(estado, modelo::editar, modelo::solicitarConfirmacion, { mostrarLista = true })
                    GastosResultado(estado, modelo::confirmar, modelo::volverAEditar, { mostrarLista = true }, modelo::nuevoRegistro)
                }
            }
        }
    }

    private fun texto(valor: String) = compose.onNodeWithText(valor)
    private fun campo(indice: Int) = compose.onAllNodes(hasSetTextAction())[indice]
    private fun llenar(monto: String = "12500") {
        campo(0).performTextInput("Transporte de prueba")
        campo(2).performTextInput(monto)
    }
    private fun guardar() { texto("Guardar gasto").performScrollTo().performClick() }
    private fun confirmar() { texto("Registrar gasto").performClick(); compose.waitUntil(15000) { modelo.uiState.value.resultado in listOf(ResultadoGasto.EXITO, ResultadoGasto.ERROR) } }
    private fun captura(nombre: String) {
        val carpeta = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "gastos-evidencias").apply { mkdirs() }
        // Las hojas viven en otra ventana: captura el sistema completo para incluirlas.
        compose.waitForIdle()
        val imagen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(carpeta, "$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun G01_abrirDesdeHome() {
        compose.setContent {
            val nav = rememberNavController()
            CafesmartTheme { Scaffold { padding -> AppNavHost(modifier = Modifier.padding(padding), navController = nav) } }
            // Solo se omite autenticación en la prueba; Home y la ruta de producción son reales.
            LaunchedEffect(Unit) {
                // Scaffold subcompone el NavHost: esperar su primera entrada evita navegar antes del grafo.
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        texto("Registrar gasto").performScrollTo().performClick()
        texto("Registro de Gastos").assertIsDisplayed()
        captura("G01-registro")
    }
    @Test fun G02_abrirDesdeLista() {
        compose.setContent {
            val nav = rememberNavController()
            CafesmartTheme { Scaffold { padding -> AppNavHost(modifier = Modifier.padding(padding), navController = nav) } }
            LaunchedEffect(Unit) {
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                nav.navigate(Routes.SETTINGS)
            }
        }
        texto("Gastos operativos").performClick()
        texto("Gastos generales").assertIsDisplayed(); captura("G02-lista-vacia")
        texto("Registrar gasto").performClick(); texto("Registro de Gastos").assertIsDisplayed()
    }
    @Test fun G03_conceptoVacio() {
        montar(); campo(0).performTextInput("   "); campo(2).performTextInput("1000"); guardar()
        texto("Escribe el concepto del gasto.").performScrollTo().assertIsDisplayed()
    }
    @Test fun G04_ceroYNegativo() {
        montar(); llenar("0"); guardar(); compose.runOnIdle { assertNotNull(modelo.uiState.value.errores.monto); modelo.editar { it.copy(monto = "-10") }; modelo.solicitarConfirmacion(); assertNotNull(modelo.uiState.value.errores.monto) }
    }
    @Test fun G05_formatoInvalido() {
        montar(); llenar("1e3"); guardar(); compose.runOnIdle { assertNotNull(modelo.uiState.value.errores.monto) }
        compose.runOnIdle { modelo.editar { it.copy(monto = "12.50") }; modelo.solicitarConfirmacion(); assertNotNull(modelo.uiState.value.errores.monto) }
    }
    @Test fun G06_seisCategorias() {
        montar(); TipoGasto.entries.forEach { tipo -> texto(tipo.etiqueta).performScrollTo().performClick(); compose.runOnIdle { assertEquals(tipo, modelo.uiState.value.formulario.tipo) } }
    }
    @Test fun G07_estadoPago() {
        montar(); texto("Pendiente").performScrollTo().performClick(); compose.runOnIdle { assertEquals(EstadoPago.PENDIENTE, modelo.uiState.value.formulario.estado) }
        texto("Pagado").performClick(); compose.runOnIdle { assertEquals(EstadoPago.PAGADO, modelo.uiState.value.formulario.estado) }
    }
    @Test fun G08_subloteDisponibleYReparto() {
        montar(sublotes = listOf(Sublote(7, 1, pesoInicialKg = 10.0, pesoDisponibleKg = 10.0, costoKg = 1000, fechaIngreso = Fecha(5,11,2026))))
        llenar(); texto("Asociar a sublotes").performScrollTo().performClick(); texto("Seleccionar…").performScrollTo().performClick(); texto("Sublote 7").performClick()
        campo(3).performScrollTo().performTextInput("12500"); guardar(); texto("¿Registrar este gasto?").assertIsDisplayed(); confirmar(); texto("Gasto registrado con éxito").assertIsDisplayed()
    }
    @Test fun G09_exigeSublote() {
        montar(); llenar(); texto("Asociar a sublotes").performScrollTo().performClick(); guardar()
        texto("Selecciona al menos un sublote.").performScrollTo().assertIsDisplayed()
    }
    @Test fun G10_confirmacion() {
        montar(); llenar(); guardar(); texto("¿Registrar este gasto?").assertIsDisplayed(); captura("G10-confirmacion")
        compose.runOnIdle { assertTrue(modelo.uiState.value.gastos.isEmpty()) }
    }
    @Test fun G11_regresarConserva() {
        montar(); llenar(); guardar(); texto("Regresar").performClick()
        campo(0).performScrollTo().assertTextEquals("Transporte de prueba")
        // El nodo también contiene el prefijo monetario "$" en su semántica accesible.
        campo(2).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("12500")))
    }
    @Test fun G12_guardarYMostrar() {
        montar(); llenar(); guardar(); confirmar(); texto("Gasto registrado con éxito").assertIsDisplayed(); captura("G12-exito")
        texto("Ver gastos").performClick(); texto("Transporte de prueba").assertIsDisplayed(); captura("G12-lista-registrada")
        compose.runOnIdle { assertEquals(1, modelo.uiState.value.gastos.size) }
    }
    @Test fun G13_filtros() {
        montar(); llenar(); guardar(); confirmar(); texto("Ver gastos").performClick()
        texto("Pendientes").performClick(); texto("No hay gastos en este estado").assertIsDisplayed()
        texto("Pagados").performClick(); texto("Transporte de prueba").assertIsDisplayed()
        texto("Todos").performClick(); texto("Transporte de prueba").assertIsDisplayed()
    }
    @Test fun G14_totalPendiente() {
        montar(); llenar(); texto("Pendiente").performScrollTo().performClick(); guardar(); confirmar(); texto("Ver gastos").performClick()
        compose.runOnIdle { assertEquals("12500", modelo.uiState.value.totalPendiente.toString()) }
        compose.onAllNodesWithText(dineroGasto(12500L))[0].assertIsDisplayed()
    }
    @Test fun G15_errorRealSinExito() {
        montar(falla = true); llenar(); guardar(); confirmar(); texto("Error al registrar").assertIsDisplayed(); captura("G15-error")
        texto("Gasto registrado con éxito").assertDoesNotExist(); compose.runOnIdle { assertTrue(modelo.uiState.value.gastos.isEmpty()) }
        texto("Regresar").performClick(); campo(0).performScrollTo().assertTextEquals("Transporte de prueba")
    }

    @Test fun G16_guardadoDesdeHomeEnNavegacionReal() {
        compose.setContent {
            val nav = rememberNavController()
            CafesmartTheme { Scaffold { padding -> AppNavHost(modifier = Modifier.padding(padding), navController = nav) } }
            LaunchedEffect(Unit) {
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        texto("Registrar gasto").performScrollTo().performClick(); llenar(); guardar()
        texto("Registrar gasto").performClick()
        texto("Ver gastos").assertIsDisplayed().performClick()
        texto("Gastos generales").assertIsDisplayed(); texto("Transporte de prueba").assertIsDisplayed()
        captura("G16-lista-navegacion-real")
        texto("Registrar gasto").performScrollTo().performClick()
        // El placeholder también es texto accesible; se comprueba solo el valor editable.
        campo(0).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    /** Comprueba el calendario nativo, el foco del campo y el acceso a las acciones con scroll. */
    @Test fun G17_calendarioTecladoYScroll() {
        montar()
        texto("Fecha").performScrollTo()
        compose.onNodeWithContentDescription("Seleccionar fecha").performClick()
        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Seleccionar 6 de noviembre de 2026 en el calendario nativo"
            override fun perform(uiController: UiController, view: View) {
                (view as DatePicker).updateDate(2026, 10, 6)
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
        compose.runOnIdle { assertEquals(Fecha(6, 11, 2026), modelo.uiState.value.formulario.fecha) }
        campo(0).performScrollTo().performClick().performTextInput("Prueba de teclado")
        compose.waitForIdle()
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val imagen = instrumentacion.uiAutomation.takeScreenshot()
        val archivo = File(instrumentacion.targetContext.getExternalFilesDir(null), "gastos-evidencias/G17-teclado-sistema.png")
        archivo.parentFile?.mkdirs()
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        texto("Guardar gasto").performScrollTo().assertIsDisplayed()
    }
}
