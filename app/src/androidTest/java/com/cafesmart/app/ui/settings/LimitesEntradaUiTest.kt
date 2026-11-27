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
}
