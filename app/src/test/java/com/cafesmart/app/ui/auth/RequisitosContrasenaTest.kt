package com.cafesmart.app.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Aviso en vivo de la contraseña de la cuenta (Crear cuenta · Paso 2): mínimo 8, con minúscula, mayúscula y número.
class RequisitosContrasenaTest {

    private fun cumplidos(p: String) = requisitosContrasena(p).filter { it.cumplido }.map { it.texto }

    @Test fun sinNadaEscritoNoCumpleNingunRequisito() {
        assertEquals(0, requisitosCumplidos(""))
        assertFalse(contrasenaValida(""))
        assertEquals(
            listOf("8 caracteres", "Una minúscula", "Una mayúscula", "Un número"),
            requisitosContrasena("").map { it.texto }
        )
    }

    @Test fun cadaRequisitoSeMarcaPorSeparado() {
        // Como el ejemplo del Figma: "Cafe12" cumple todo menos los 8 caracteres.
        assertEquals(listOf("Una minúscula", "Una mayúscula", "Un número"), cumplidos("Cafe12"))
        assertEquals(listOf("8 caracteres", "Una minúscula"), cumplidos("abcdefgh"))
        assertEquals(listOf("Un número"), cumplidos("123"))
    }

    @Test fun ochoCaracteresExactosYSieteNo() {
        assertTrue(contrasenaValida("Cafe1234"))
        assertFalse(contrasenaValida("Cafe123"))
        assertEquals(3, requisitosCumplidos("Cafe123"))
    }

    @Test fun lasCuatroReglasSonNecesarias() {
        assertFalse(contrasenaValida("cafe1234"))   // sin mayúscula
        assertFalse(contrasenaValida("CAFE1234"))   // sin minúscula
        assertFalse(contrasenaValida("CafeCafe"))   // sin número
        assertTrue(contrasenaValida("Cafe1234"))
    }

    @Test fun laCoincidenciaSeEvaluaMientrasSeEscribe() {
        assertEquals(Coincidencia.SIN_ESCRIBIR, coincidenciaContrasena("Cafe1234", ""))
        assertEquals(Coincidencia.NO_COINCIDEN, coincidenciaContrasena("Cafe1234", "Cafe12"))
        assertEquals(Coincidencia.COINCIDEN, coincidenciaContrasena("Cafe1234", "Cafe1234"))
        // Distinguen mayúsculas.
        assertEquals(Coincidencia.NO_COINCIDEN, coincidenciaContrasena("Cafe1234", "cafe1234"))
    }
}
