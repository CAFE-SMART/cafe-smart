package com.cafesmart.app.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cafesmart.app.data.limites.AlmacenLimitesMemoria
import com.cafesmart.app.data.limites.ConfiguracionLimites
import com.cafesmart.app.data.limites.AlmacenLimitesPreferencias
import com.cafesmart.app.data.limites.BorradorLimites
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.limites.LimitesDeEntrada
import com.cafesmart.app.viewmodel.limites.LimitesVigentes
import org.junit.After
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextRange
import org.junit.Rule
import org.junit.Test

class LimitesEntradaUiTest {
    @get:Rule val compose = createComposeRule()
    @After fun limpiarEstado() = LimitesVigentes.aplicar(LimitesDeEntrada.PREDETERMINADOS)

    @Test fun guardarValidarYRestablecer() {
        val almacen = AlmacenLimitesMemoria()
        compose.setContent { CafesmartTheme { LimitesEntradaScreen(ConfiguracionLimites(almacen)) {} } }
        compose.onAllNodes(hasSetTextAction())[0].performTextClearance()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("200000")
        compose.onNodeWithText("Guardar límites").performClick()
        compose.onNodeWithText("El mínimo no puede ser mayor que el máximo.").assertExists()
        assertEquals(null, almacen.leer())
        compose.onAllNodes(hasSetTextAction())[0].performTextClearance()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("-1")
        compose.onNodeWithText("Guardar límites").performClick()
        compose.onNodeWithText("No se permiten números negativos.").assertExists()
        assertEquals(null, almacen.leer())
        compose.onAllNodes(hasSetTextAction())[0].performTextClearance()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("2")
        compose.onNodeWithText("Guardar límites").performClick()
        assertEquals(2, almacen.leer()?.pesoMinimoKg)
        compose.onNodeWithText("Volver a los valores originales").performScrollTo().performClick()
        assertEquals(null, almacen.leer())
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, LimitesVigentes.actuales)
    }

    // Valores con puntos de miles en pantalla, guardado con dígitos y aviso superior visible al guardar.
    @Test fun formatoDeMilesYAvisoAlGuardar() {
        val almacen = AlmacenLimitesMemoria()
        compose.setContent { CafesmartTheme { LimitesEntradaScreen(ConfiguracionLimites(almacen)) {} } }
        compose.onNodeWithText("99.999").assertExists()
        compose.onNodeWithText("50.000.000").performScrollTo().assertExists()
        compose.onAllNodes(hasSetTextAction())[2].performTextClearance()
        compose.onAllNodes(hasSetTextAction())[2].performTextInput("2500")
        compose.onNodeWithText("2.500").assertExists()
        compose.onNodeWithText("Guardar límites").performClick()
        compose.onNodeWithText("Límites guardados.").assertIsDisplayed()
        assertEquals(2_500, almacen.leer()?.precioKgMinimoCop)
    }

    @Test fun preferenciasPersistenYSeRestablecen() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val config = ConfiguracionLimites(AlmacenLimitesPreferencias(contexto))
        try {
            config.restablecer()
            config.guardar(BorradorLimites.de(LimitesDeEntrada.PREDETERMINADOS).copy(gastoMin = "5000"))
            LimitesVigentes.aplicar(LimitesDeEntrada.PREDETERMINADOS)
            assertEquals(5000L, ConfiguracionLimites(AlmacenLimitesPreferencias(contexto)).cargar().gastoMinimoCop)
            config.restablecer()
            assertEquals(LimitesDeEntrada.PREDETERMINADOS, ConfiguracionLimites(AlmacenLimitesPreferencias(contexto)).cargar())
        } finally { config.restablecer() }
    }

    @Test fun camposLargosSeEditanConFuenteAmpliada() {
        val almacen = AlmacenLimitesMemoria()
        compose.setContent {
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, fontScale = 1.5f)) {
                CafesmartTheme { LimitesEntradaScreen(ConfiguracionLimites(almacen)) {} }
            }
        }
        val casos = listOf("1000" to "1.000", "10000" to "10.000", "99999" to "99.999",
            "100000" to "100.000", "900000" to "900.000", "50000000" to "50.000.000")
        casos.forEach { (crudo, mostrado) ->
            compose.onAllNodes(hasSetTextAction())[5].performScrollTo().performTextReplacement(crudo)
            compose.onAllNodes(hasSetTextAction())[5].assertIsDisplayed().assertTextEquals(mostrado)
        }
        compose.onAllNodes(hasSetTextAction())[5].performTextReplacement("")
        compose.onAllNodes(hasSetTextAction())[5].assertTextEquals("")
        compose.onAllNodes(hasSetTextAction())[5].performTextReplacement("50000000")
        compose.onAllNodes(hasSetTextAction())[5].performTextInputSelection(TextRange(2))
        compose.onAllNodes(hasSetTextAction())[5].performTextInput("9")
        compose.onAllNodes(hasSetTextAction())[5].assertTextEquals("509.000.000")
        compose.onAllNodes(hasSetTextAction())[5].performTextReplacement("50000000")
        compose.onNodeWithText("Guardar límites").performClick()
        assertEquals(50_000_000L, almacen.leer()?.gastoMaximoCop)
    }
}
