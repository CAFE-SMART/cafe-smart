package com.cafesmart.app.ui.settings

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class SeparadoresLimiteTest {
    @Test fun conservaValoresYPosicionDelCursor() {
        val casos = mapOf("1000" to "1.000", "10000" to "10.000", "99999" to "99.999",
            "100000" to "100.000", "900000" to "900.000", "50000000" to "50.000.000")
        casos.forEach { (crudo, esperado) ->
            val transformado = SeparadoresLimite.filter(AnnotatedString(crudo))
            assertEquals(esperado, transformado.text.text)
            for (offset in 0..crudo.length) {
                assertEquals(offset, transformado.offsetMapping.transformedToOriginal(transformado.offsetMapping.originalToTransformed(offset)))
            }
        }
    }
}
