package com.cafesmart.app.data.finanzas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaveFinancieraTest {

    @Test
    fun laClaveCorrectaSeVerificaYLaIncorrectaNo() {
        val guardada = ClaveFinanciera.crear("MiClave123")
        assertTrue(ClaveFinanciera.verificar("MiClave123", guardada))
        assertFalse(ClaveFinanciera.verificar("miclave123", guardada))
        assertFalse(ClaveFinanciera.verificar("MiClave12", guardada))
        assertFalse(ClaveFinanciera.verificar("", guardada))
    }

    @Test
    fun nuncaSeGuardaLaClaveEnTextoPlano() {
        val guardada = ClaveFinanciera.crear("MiClave123")
        assertFalse(guardada.toString().contains("MiClave123"))
        assertFalse(guardada.hash.contains("MiClave123"))
        assertFalse(guardada.sal.contains("MiClave123"))
    }

    @Test
    fun laMismaClaveConOtraSalDaOtroHash() {
        val a = ClaveFinanciera.crear("MiClave123")
        val b = ClaveFinanciera.crear("MiClave123")
        assertNotEquals(a.sal, b.sal)
        assertNotEquals(a.hash, b.hash)
        // Cada una solo se verifica con su propia sal.
        assertTrue(ClaveFinanciera.verificar("MiClave123", a))
        assertTrue(ClaveFinanciera.verificar("MiClave123", b))
    }

    @Test
    fun elRegistroGuardaAlgoritmoVueltasSalYHash() {
        val guardada = ClaveFinanciera.crear("abc123")
        assertTrue(guardada.algoritmo.startsWith("PBKDF2"))
        assertTrue(guardada.iteraciones >= 100_000)
        assertEquals(32, guardada.sal.length)      // 16 bytes en hexadecimal
        assertEquals(64, guardada.hash.length)     // 256 bits en hexadecimal
    }

    @Test
    fun unHashAlteradoNoVerifica() {
        val guardada = ClaveFinanciera.crear("MiClave123")
        val alterado = guardada.copy(hash = "0".repeat(guardada.hash.length))
        assertFalse(ClaveFinanciera.verificar("MiClave123", alterado))
    }

    @Test
    fun admiteTildesYEspaciosEnLaClave() {
        val guardada = ClaveFinanciera.crear("café con leche")
        assertTrue(ClaveFinanciera.verificar("café con leche", guardada))
        assertFalse(ClaveFinanciera.verificar("cafe con leche", guardada))
    }

    @Test
    fun reglasDeLaClaveNueva() {
        assertEquals(ERROR_CLAVE_VACIA, errorDeClaveNueva(""))
        assertEquals(ERROR_CLAVE_CORTA, errorDeClaveNueva("12345"))
        assertNull(errorDeClaveNueva("123456"))
        assertNull(errorDeClaveNueva("una clave larga"))
    }

    @Test
    fun reglasDeLaConfirmacion() {
        assertEquals(ERROR_CONFIRMACION_VACIA, errorDeConfirmacion("123456", ""))
        assertEquals(ERROR_CLAVES_DISTINTAS, errorDeConfirmacion("123456", "123457"))
        assertNull(errorDeConfirmacion("123456", "123456"))
    }
}
