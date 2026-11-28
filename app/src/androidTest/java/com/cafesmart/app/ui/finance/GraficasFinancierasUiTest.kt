package com.cafesmart.app.ui.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.ResumenFinanciero
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.finance.DiaBalance
import com.cafesmart.app.viewmodel.finance.GraficasUiState
import com.cafesmart.app.viewmodel.finance.ResumenUiState
import com.cafesmart.app.viewmodel.finance.diasDeLaSemana
import com.cafesmart.app.viewmodel.finance.distribucionGastos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

// Resumen financiero rediseñado (ciclo 3): gráficas y tarjetas con datos ficticios SOLO de prueba.
class GraficasFinancierasUiTest {
    @get:Rule val compose = createComposeRule()

    private val hoy = Fecha(27, 11, 2026)
    private val semana = diasDeLaSemana(hoy)
    private val balance = semana.mapIndexed { i, f -> DiaBalance(f, if (i == 6) 350_000 else i * 100_000L, if (i == 6) 120_000 else i * 40_000L) }
    private val distribucion = distribucionGastos(
        listOf(GastoRegistrado(hoy, 60_000, tipo = TipoGasto.TRANSPORTE), GastoRegistrado(hoy, 40_000, tipo = TipoGasto.CARGUE)),
        PeriodoFinanciero.SEMANA, hoy,
    )
    private fun semanaConDatos(b: List<DiaBalance> = balance) =
        GraficasUiState(PeriodoFinanciero.SEMANA, false, distribucion, b, true, semana = semana)

    private fun tooltip() = compose.onNodeWithTag("balance_tooltip", useUnmergedTree = true)
    private fun barra(dia: Int, ingresos: Boolean) =
        compose.onNodeWithTag("barra_${dia}_${if (ingresos) "ingresos" else "costos"}", useUnmergedTree = true)

    @Test fun gb03_gb04_gb05_tocarBarrasMuestraUnSoloValor() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(semanaConDatos()) } }
        barra(6, true).performTouchInput { click(center) }
        tooltip().assertContentDescriptionEquals("Valor seleccionado $350.000")
        barra(6, true).assertIsSelected()
        barra(6, false).performTouchInput { click(center) }
        tooltip().assertContentDescriptionEquals("Valor seleccionado $120.000")
        assertEquals(1, compose.onAllNodesWithTagCount("balance_tooltip"))
        // GB-06: barra del primer día (borde izquierdo).
        barra(1, true).performTouchInput { click(center) }
        tooltip().assertContentDescriptionEquals("Valor seleccionado $100.000")
    }

    @Test fun gb14_tocarFueraDeLasBarrasOcultaElTooltip() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(semanaConDatos()) } }
        barra(3, true).performTouchInput { click(center) }
        tooltip().assertExists()
        // Zona del eje vertical (a la izquierda de las barras).
        compose.onNodeWithTag("balance_grafica").performTouchInput { click(Offset(4f, centerY)) }
        tooltip().assertDoesNotExist()
    }

    @Test fun gb15_rf03_cambiarDePeriodoLimpiaLaSeleccion() {
        var estado by mutableStateOf(semanaConDatos())
        compose.setContent { CafesmartTheme { GraficasFinancieras(estado) } }
        barra(6, true).performTouchInput { click(center) }
        tooltip().assertExists()
        estado = GraficasUiState(PeriodoFinanciero.HOY, false, distribucion, null, true, semana = semana)
        compose.waitForIdle()
        compose.onNodeWithTag("graficas_balance").assertDoesNotExist()       // GB-01: con Hoy no existe
        estado = semanaConDatos(balance.map { it.copy(ingresosPesos = it.ingresosPesos + 1) })
        compose.waitForIdle()
        tooltip().assertDoesNotExist()
    }

    @Test fun gb16_gb17_sinControlesExtraYConTalkBack() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(semanaConDatos()) } }
        // Lo único que se puede tocar son las 14 barras (sin botones de días, ingresos o costos).
        assertEquals(14, compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes().size)
        barra(6, false).assertContentDescriptionEquals("viernes 27 de noviembre, costos y gastos $120.000")
        // TalkBack: la acción de la barra selecciona y muestra el valor, aunque la barra sea pequeña.
        barra(1, false).performSemanticsAction(SemanticsActions.OnClick)
        tooltip().assertContentDescriptionEquals("Valor seleccionado $40.000")
    }

    @Test fun gd_donaConTotalYCategorias() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(semanaConDatos()) } }
        compose.onNodeWithText("Transporte", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("60 %", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("$100.000", useUnmergedTree = true).assertExists()
    }

    @Test fun gd04_sinGastosEstadoVacio() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(GraficasUiState(PeriodoFinanciero.HOY, false, distribucionGastos(emptyList(), PeriodoFinanciero.HOY, hoy))) } }
        compose.onNodeWithText(TEXTO_SIN_GASTOS).assertExists()
    }

    @Test fun gb12_sinVentasConectadasBalanceNoDisponible() {
        compose.setContent { CafesmartTheme { GraficasFinancieras(GraficasUiState(PeriodoFinanciero.SEMANA, false, distribucion, null, false, semana = semana)) } }
        compose.onNodeWithTag("balance_no_disponible").assertExists()
        compose.onNodeWithTag("balance_grafica").assertDoesNotExist()
    }

    @Test fun rf07_resumenSinVentasMuestraNoDisponible() {
        val resumen = ResumenFinanciero(0, 0, 0, 0, 0.0, compras = 2, comprasPesos = 805_000, gastosPeriodo = 50_000, ventasDisponibles = false)
        compose.setContent { CafesmartTheme { ResumenFinancieroScreen(ResumenUiState(resumen = resumen, cargando = false, actualizando = false), {}, {}, {}) } }
        compose.onNodeWithText("Tu ganancia de hoy").assertExists()
        assertFalse(compose.onAllNodesWithText(NO_DISPONIBLE).fetchSemanticsNodes().isEmpty())
        compose.onAllNodesWithText("$ 0").fetchSemanticsNodes().let { assertEquals(0, it.size) }
        compose.onNodeWithText("$ 805.000").performScrollTo()
    }

    @Test fun movil_320dpFuenteGrandeYValoresGrandesNoSeCortan() {
        val resumen = ResumenFinanciero(987_654_321, 600_000_000, 50_000_000, 337_654_321, 12.0, ventas = 3)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 1.5f)) {
                CafesmartTheme {
                    Box(Modifier.width(320.dp)) {
                        ResumenFinancieroScreen(ResumenUiState(PeriodoFinanciero.SEMANA, resumen, false, false), {}, {}, {}, graficas = { GraficasFinancieras(semanaConDatos()) })
                    }
                }
            }
        }
        listOf("$ 337.654.321", "$ 987.654.321", "$ 650.000.000").forEach { texto ->
            val nodo = compose.onAllNodesWithText(texto, useUnmergedTree = true)[0]
            nodo.performScrollTo()
            assertFalse("$texto se corta", desborda(nodo))
        }
        compose.onNodeWithText("Tu ganancia de esta semana").assertExists()
        compose.onNodeWithTag("graficas_balance").performScrollTo()
    }

    private fun desborda(nodo: SemanticsNodeInteraction): Boolean {
        val resultados = mutableListOf<TextLayoutResult>()
        nodo.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(resultados)
        return resultados.firstOrNull()?.hasVisualOverflow ?: false
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size
}
