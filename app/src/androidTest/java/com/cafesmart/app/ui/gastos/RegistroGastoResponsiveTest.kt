package com.cafesmart.app.ui.gastos

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Formulario real con borrador aislado: no guarda datos ni llama a Firebase. */
class RegistroGastoResponsiveTest {
    @get:Rule val compose = createComposeRule()

    private fun montar(ancho: Int, seleccion: Boolean = false) {
        compose.setContent {
            var estado by remember { mutableStateOf(GastosUiState(
                formulario = GastoFormulario(aplicaA = AplicaGasto.SUBLOTES,
                    asignaciones = if (seleccion) mapOf(2L to "12000") else emptyMap()),
                sublotes = lotesEjemplo().flatMap { it.sublotesConExistencia }
            )) }
            CafesmartTheme {
                Box(Modifier.requiredSize(ancho.dp, 800.dp).testTag("registro_root")) {
                    RegistrarGastoScreen(estado, { cambio -> estado = estado.copy(formulario = cambio(estado.formulario)) },
                        {}, {}, onSeleccionarSublotes = {
                            val mapa = when (estado.formulario.asignaciones.size) {
                                0 -> mapOf(2L to "12000")
                                1 -> mapOf(1L to "5000", 2L to "7000")
                                else -> emptyMap()
                            }
                            estado = estado.copy(formulario = estado.formulario.copy(asignaciones = mapa))
                        })
                }
            }
        }
    }

    private fun monto() = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("gastos_monto")))

    private fun evidenciar(nombre: String) {
        // Sin archivos en el dispositivo: el ejecutor privado decodifica el status en el host.
        val bytes = ByteArrayOutputStream()
        compose.onNodeWithTag("registro_root").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, bytes)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("cafesmartScreenshot", nombre + "|" + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
        })
    }

    @Test fun descripcion360LimitaPegadoYExponeContador() {
        montar(360)
        val campo = compose.onNodeWithTag("gastos_descripcion")
        campo.performScrollTo().performTextReplacement("d".repeat(250))
        campo.assertTextEquals("d".repeat(200))
        compose.onNodeWithTag("gastos_descripcion_contador").performScrollTo().assertTextEquals("200/200 caracteres")
        compose.onNodeWithText("Límite alcanzado. Puedes editar o borrar el texto.").assertExists()
        evidenciar("360-descripcion-limite")
    }

    @Test fun descripcion390AcotaSaltosDeLineaYMantieneAccesoAMontoYGuardar() {
        montar(390, seleccion = true)
        val campo = compose.onNodeWithTag("gastos_descripcion")
        campo.performScrollTo().performTextReplacement("linea\n".repeat(100))
        assertTrue("La descripción desplaza excesivamente el formulario", campo.getUnclippedBoundsInRoot().height <= 160.dp)
        monto().performScrollTo().performTextReplacement(Long.MAX_VALUE.toString())
        monto().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(Long.MAX_VALUE.toString())))
        compose.onNodeWithTag("gastos_monto").assertWidthIsAtLeast(300.dp)
        evidenciar("390-monto-largo")
        compose.onNodeWithText("Guardar gasto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 sublote seleccionado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Asignación al sublote 2").assertExists()
    }

    @Test fun monto360ConservaDiecinueveDigitosYFocoConTeclado() {
        montar(360)
        monto().performScrollTo().performTextReplacement(Long.MAX_VALUE.toString())
        monto().performClick().assertIsFocused()
        monto().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(Long.MAX_VALUE.toString())))
        compose.onNodeWithTag("gastos_monto").assertWidthIsAtLeast(300.dp)
        compose.onNode(hasText("$") and hasAnyAncestor(hasTestTag("gastos_monto")), useUnmergedTree = true).assertExists()
        evidenciar("360-monto-largo")
    }

    @Test fun selectorTieneTextoOscuroYContrasteEnCeroUnoYVarios() {
        montar(360)
        val boton = compose.onNodeWithTag("gastos_seleccionar_sublotes")
        for (cantidad in 0..2) {
            boton.performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(etiquetaSeleccionSublotes(cantidad)).assertIsDisplayed()
            val mapa = boton.captureToImage().toPixelMap()
            var pixelesTexto = 0
            var pixelesFondo = 0
            var luminanciaFondo = 0f
            for (y in 0 until mapa.height) for (x in 0 until mapa.width) {
                if (mapa[x,y].luminance() <= PrimaryDark.luminance() + .01f) pixelesTexto++
                if (mapa[x,y].luminance() > .85f) {
                    pixelesFondo++
                    luminanciaFondo += mapa[x,y].luminance()
                }
            }
            assertTrue("Texto oscuro ausente en captura", pixelesTexto > 20)
            assertTrue("Fondo esperado ausente en captura", pixelesFondo > 100)
            val luminanciaRealFondo = luminanciaFondo / pixelesFondo
            assertTrue("Contraste inferior a 4.5:1", (luminanciaRealFondo+.05f)/(PrimaryDark.luminance()+.05f) >= 4.5f)
            evidenciar("360-selector-$cantidad")
            boton.performClick()
        }
    }
}
