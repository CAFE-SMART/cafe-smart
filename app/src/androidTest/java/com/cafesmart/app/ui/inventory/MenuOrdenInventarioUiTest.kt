package com.cafesmart.app.ui.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.inventory.InventoryUiState
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.resumirInventario
import com.cafesmart.app.viewmodel.purchase.formatoKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Menú "Más reciente / Más antiguo" de Inventario con el tema real de la app. Solo dibuja la pantalla con un
// estado armado a mano: no usa Room, Firebase ni la navegación (los 834 kg de la cuenta QA no se tocan).
class MenuOrdenInventarioUiTest {
    @get:Rule val compose = createComposeRule()

    private fun sublote(id: Long, lote: Long, kg: Double, f: Fecha) = Sublote(id, lote, 1, null, kg, kg, 14_000, f)
    private val lotes = listOf(
        LoteConSublotes(Lote(1, TipoCafe.VERDE, Calidad.BUENA), listOf(sublote(1, 1, 60.0, Fecha(3, 11, 2026)))),
        LoteConSublotes(Lote(2, TipoCafe.VERDE, Calidad.REGULAR),
            listOf(sublote(2, 2, 120.0, Fecha(1, 11, 2026)), sublote(3, 2, 80.0, Fecha(28, 10, 2026)))),
        LoteConSublotes(Lote(3, TipoCafe.VERDE, Calidad.MALA), listOf(sublote(4, 3, 30.0, Fecha(25, 10, 2026)))),
        LoteConSublotes(Lote(4, TipoCafe.SECO, Calidad.BUENA), listOf(sublote(5, 4, 521.0, Fecha(5, 11, 2026)))),
    )

    private fun montar(tipo: TipoCafe?) = compose.setContent {
        var orden by remember { mutableStateOf(OrdenInventario.MAS_RECIENTE) }
        CafesmartTheme {
            InventoryScreen(
                estado = InventoryUiState(resumirInventario(lotes, 0.0, orden), orden = orden, lotes = lotes),
                onTipoClick = {}, onOrdenChange = { orden = it }, onAjustarBodega = {}, onRegistrarCompra = {},
                onNavegar = {}, tipoSeleccionado = tipo,
            )
        }
    }

    private fun abrir(etiquetaActual: String) {
        compose.onNodeWithText(etiquetaActual).performClick()
        compose.onNodeWithTag("inventario_orden_menu").assertIsDisplayed()
    }

    private fun elegida(opcion: OrdenInventario, si: Boolean) =
        compose.onNodeWithTag("inventario_orden_${opcion.name}").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, si))

    private fun y(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
    private fun yTexto(texto: String) = compose.onNodeWithText(texto, substring = true).fetchSemanticsNode().positionInRoot.y

    // Orden en pantalla de las tarjetas de calidad (de arriba abajo).
    private fun ordenCalidades(): List<String> =
        Calidad.entries.map { it.name }.sortedBy { y("inventario_calidad_$it") }

    @Test fun ordenaYMarcaLaOpcionElegida() {
        montar(TipoCafe.VERDE)
        assertEquals(listOf("BUENA", "REGULAR", "MALA"), ordenCalidades())

        abrir("Más reciente")
        elegida(OrdenInventario.MAS_RECIENTE, true)
        elegida(OrdenInventario.MAS_ANTIGUO, false)
        compose.onNodeWithTag("inventario_orden_MAS_ANTIGUO").performClick()
        compose.onNodeWithTag("inventario_orden_menu").assertDoesNotExist()

        // El selector muestra la opción elegida y las calidades se invierten; kg y sublotes no cambian.
        compose.onNodeWithText("Más antiguo").assertIsDisplayed()
        assertEquals(listOf("MALA", "REGULAR", "BUENA"), ordenCalidades())
        compose.onNodeWithText(formatoKg(200.0), substring = true).assertExists()
        compose.onNodeWithText("2 sublotes").assertExists()

        abrir("Más antiguo")
        elegida(OrdenInventario.MAS_ANTIGUO, true)
        compose.onNodeWithTag("inventario_orden_MAS_RECIENTE").performClick()
        assertEquals(listOf("BUENA", "REGULAR", "MALA"), ordenCalidades())
    }

    // Abrir y cerrar varias veces (con "Atrás") no cambia el orden ni deja el menú abierto.
    @Test fun seAbreYSeCierraVariasVeces() {
        montar(TipoCafe.VERDE)
        repeat(3) {
            abrir("Más reciente")
            Espresso.pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("inventario_orden_menu").assertDoesNotExist()
        }
        assertEquals(listOf("BUENA", "REGULAR", "MALA"), ordenCalidades())
    }

    // Píxeles reales del menú con el tema de la app. Antes: fondo lila (#F3EDF7) y texto blanco. Ahora: fondo claro
    // y opaco y texto oscuro, con contraste AA medido sobre los píxeles dibujados. (No se exige blanco exacto:
    // en el Xiaomi el blanco de cualquier tarjeta se captura como #F7F7F7.)
    @Test fun elMenuSeDibujaLegibleConElTemaReal() {
        montar(TipoCafe.VERDE)
        abrir("Más reciente")
        for (tag in listOf("inventario_orden_MAS_ANTIGUO", "inventario_orden_MAS_RECIENTE")) {
            val imagen = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            val colores = (0 until imagen.width step 2).flatMap { x -> (0 until imagen.height step 2).map { y -> imagen[x, y] } }
            val fondo = colores.groupingBy { it }.eachCount().maxBy { it.value }.key
            assertEquals("$tag: fondo opaco", 1f, fondo.alpha)
            assertTrue("$tag: fondo claro ($fondo)", fondo.luminance() > 0.85f)
            assertTrue("$tag: no es el lila por defecto ($fondo)", fondo.green >= fondo.red - 0.01f)
            val texto = colores.minBy { it.luminance() }
            val contraste = (fondo.luminance() + 0.05) / (texto.luminance() + 0.05)
            assertTrue("$tag: contraste $contraste (texto $texto, fondo $fondo)", contraste >= 4.5)
        }
    }

    // El mismo componente en "Resumen (Todos)", que ordena los tipos de café (Seco 5-nov es más reciente que Verde 3-nov).
    @Test fun funcionaTambienEnTodos() {
        montar(null)
        assertTrue(yTexto(formatoKg(521.0)) < yTexto(formatoKg(290.0)))
        abrir("Más reciente")
        compose.onNodeWithTag("inventario_orden_MAS_ANTIGUO").performClick()
        compose.onNodeWithText("Más antiguo").assertIsDisplayed()
        assertTrue(yTexto(formatoKg(290.0)) < yTexto(formatoKg(521.0)))
    }
}
