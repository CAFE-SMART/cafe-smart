package com.cafesmart.app.data.finanzas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LimiteIntentosTest {

    private val minuto = 60_000L
    private val t0 = 10_000_000L

    @Test
    fun conMenosDeTresFallosNoHayBloqueo() {
        assertEquals(0, milisDeBloqueo(emptyList(), t0))
        assertEquals(0, milisDeBloqueo(listOf(t0), t0))
        assertEquals(0, milisDeBloqueo(listOf(t0 - minuto, t0), t0))
    }

    @Test
    fun tresFallosEnMenosDe15MinutosBloqueanPor5Minutos() {
        val fallos = listOf(t0 - 2 * minuto, t0 - minuto, t0)
        assertEquals(5 * minuto, milisDeBloqueo(fallos, t0))
        assertEquals(2 * minuto, milisDeBloqueo(fallos, t0 + 3 * minuto))
    }

    @Test
    fun cumplidoElBloqueoSeEmpiezaDeCeroConTresIntentosNuevos() {
        val fallos = listOf(t0 - 2 * minuto, t0 - minuto, t0)
        val despues = t0 + 5 * minuto
        assertEquals(0, milisDeBloqueo(fallos, despues))
        assertEquals(3, intentosRestantes(fallos, despues))
        // El primer fallo nuevo cuenta como el 1.º, no como el 4.º.
        assertEquals(listOf(despues), conNuevoFallo(fallos, despues))
    }

    @Test
    fun dosFallosNoBloqueanAunqueNoHayaPasadoLaVentana() {
        val fallos = listOf(t0 - minuto, t0)
        assertEquals(0, milisDeBloqueo(fallos, t0))
        assertEquals(1, intentosRestantes(fallos, t0))
    }

    @Test
    fun losFallosDeHaceMasDe15MinutosNoCuentan() {
        val fallos = listOf(t0 - 20 * minuto, t0 - 16 * minuto, t0)
        assertEquals(1, fallosVigentes(fallos, t0).size)
        assertEquals(0, milisDeBloqueo(fallos, t0))
        assertEquals(2, intentosRestantes(fallos, t0))
    }

    @Test
    fun conNuevoFalloDescartaLosVencidos() {
        val fallos = conNuevoFallo(listOf(t0 - 30 * minuto, t0 - minuto), t0)
        assertEquals(listOf(t0 - minuto, t0), fallos)
    }

    @Test
    fun intentosRestantesBajaDeTresACero() {
        assertEquals(3, intentosRestantes(emptyList(), t0))
        assertEquals(2, intentosRestantes(listOf(t0), t0))
        assertEquals(1, intentosRestantes(listOf(t0 - 1, t0), t0))
        assertEquals(0, intentosRestantes(listOf(t0 - 2, t0 - 1, t0), t0))
    }

    @Test
    fun elTextoDeEsperaRedondeaHaciaArribaYNuncaDiceCero() {
        assertEquals("1 minuto", textoDeEspera(1))
        assertEquals("1 minuto", textoDeEspera(60_000))
        assertEquals("2 minutos", textoDeEspera(60_001))
        assertEquals("15 minutos", textoDeEspera(15 * minuto))
    }

    @Test
    fun losDatosGuardadosDeFallosSobrevivenEnLaMemoria() {
        val almacen = AlmacenClaveFinancieraEnMemoria()
        assertTrue(almacen.leerFallos("u1").isEmpty())
        almacen.guardarFallos("u1", listOf(1L, 2L))
        assertEquals(listOf(1L, 2L), almacen.leerFallos("u1"))
        assertTrue(almacen.leerFallos("u2").isEmpty())
    }
}
