package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.viewmodel.drying.MENSAJE_ELEGIR_SUBLOTE_SECADO
import com.cafesmart.app.viewmodel.drying.cafeListoParaSecar
import com.cafesmart.app.viewmodel.drying.calidadesDe
import com.cafesmart.app.viewmodel.drying.descontarEnvios
import com.cafesmart.app.viewmodel.drying.mensajeSinElegir
import com.cafesmart.app.viewmodel.drying.sublotesParaSecar
import com.cafesmart.app.viewmodel.drying.textoDeCantidad
import com.cafesmart.app.viewmodel.drying.textoSublotes
import com.cafesmart.app.viewmodel.drying.totalASecarKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadoRulesTest {

    private val inventario = inventarioFigma()

    // ---------- Que sublotes se pueden secar ----------

    @Test
    fun soloSePuedeSecarCafeVerde() {
        val ids = sublotesParaSecar(inventario).map { it.sublote.id }
        // El sublote 5 es seco: no aparece.
        assertEquals(listOf(3L, 1L, 2L, 4L), ids)
        assertTrue(sublotesParaSecar(inventario).all { it.tipoCafe == TipoCafe.VERDE })
    }

    @Test
    fun trilladoYPasillaNoSeSecan() {
        val otros = listOf(
            subloteDePrueba(6, TipoCafe.TRILLADO, Calidad.BUENA, 23.0),
            subloteDePrueba(7, TipoCafe.PASILLA, Calidad.REGULAR, 40.0)
        )
        assertTrue(sublotesParaSecar(otros).isEmpty())
    }

    @Test
    fun unSubloteVerdeAgotadoNoAparece() {
        val conAgotado = inventario + subloteDePrueba(8, TipoCafe.VERDE, Calidad.BUENA, 0.0)
        assertEquals(listOf(3L, 1L, 2L, 4L), sublotesParaSecar(conAgotado).map { it.sublote.id })
    }

    @Test
    fun sinInventarioNoHayNadaParaSecar() {
        assertTrue(sublotesParaSecar(emptyList()).isEmpty())
    }

    @Test
    fun losSublotesVanPorCalidadYDespuesPorNumero() {
        // Buena (3), regular (1 y 2) y mala (4), como en el Figma.
        val calidades = sublotesParaSecar(inventario).map { it.calidad }
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.REGULAR, Calidad.MALA), calidades)
    }

    // ---------- Cantidad ----------

    private fun elegido(indice: Int, cantidadKg: Double) = SubloteElegido(inventario[indice], cantidadKg)

    @Test
    fun cantidadValida_todoElSubloteOUnaParte() {
        // inventario[0] es el sublote 1, con 120 kg
        assertTrue(cafeListoParaSecar(listOf(elegido(0, 120.0))))
        assertTrue(cafeListoParaSecar(listOf(elegido(0, 50.0))))
        assertTrue(cafeListoParaSecar(listOf(elegido(0, 0.5))))
    }

    @Test
    fun cantidadInvalida_cero_negativaOMasDeLoDisponible() {
        assertFalse(cafeListoParaSecar(listOf(elegido(0, 0.0))))
        assertFalse(cafeListoParaSecar(listOf(elegido(0, -5.0))))
        assertFalse(cafeListoParaSecar(listOf(elegido(0, 120.5))))
    }

    @Test
    fun conUnaCantidadInvalidaNoSePuedeContinuarAunqueLasOtrasSirvan() {
        assertFalse(cafeListoParaSecar(listOf(elegido(0, 120.0), elegido(1, 81.0))))
    }

    @Test
    fun sinSubloteElegidoNoSePuedeContinuarYDiceQueFalta() {
        assertFalse(cafeListoParaSecar(emptyList()))
        assertEquals(MENSAJE_ELEGIR_SUBLOTE_SECADO, mensajeSinElegir(emptyList()))
    }

    @Test
    fun conCafeElegidoYaNoHayMensajePendiente() {
        assertNull(mensajeSinElegir(listOf(elegido(0, 120.0))))
    }

    // ---------- Totales y textos ----------

    @Test
    fun totalASecar_sumaLosKgElegidos() {
        // Los 4 sublotes del Figma: 120 + 80 + 60 + 30 = 290
        val todos = sublotesParaSecar(inventario).map { SubloteElegido(it, it.sublote.pesoDisponibleKg) }
        assertEquals(290.0, totalASecarKg(todos), 0.0)
    }

    @Test
    fun totalASecar_noDejaRuidoDeDecimales() {
        // 0,1 + 0,2 en Double da 0.30000000000000004
        val dos = listOf(
            SubloteElegido(subloteDePrueba(1, TipoCafe.VERDE, Calidad.BUENA, 1.0), 0.1),
            SubloteElegido(subloteDePrueba(2, TipoCafe.VERDE, Calidad.BUENA, 1.0), 0.2)
        )
        assertEquals(0.3, totalASecarKg(dos), 0.0)
    }

    @Test
    fun totalASecar_sinSublotesEsCero() {
        assertEquals(0.0, totalASecarKg(emptyList()), 0.0)
    }

    @Test
    fun calidadesDe_sonLasElegidasEnElOrdenBuenaRegularMala() {
        val todos = sublotesParaSecar(inventario).map { SubloteElegido(it, it.sublote.pesoDisponibleKg) }
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), calidadesDe(todos))
        // Solo regulares: no aparecen las otras calidades.
        assertEquals(listOf(Calidad.REGULAR), calidadesDe(listOf(elegido(0, 120.0), elegido(1, 80.0))))
        assertTrue(calidadesDe(emptyList()).isEmpty())
    }

    @Test
    fun textoSublotes_singularYPlural() {
        assertEquals("1 sublote", textoSublotes(1))
        assertEquals("4 sublotes", textoSublotes(4))
    }

    @Test
    fun textoDeCantidad_sinCerosDeSobraYConPuntoDecimal() {
        assertEquals("120", textoDeCantidad(120.0))
        assertEquals("12.5", textoDeCantidad(12.5))
        assertEquals("80.125", textoDeCantidad(80.125))
        assertEquals("0.001", textoDeCantidad(0.001))
    }

    // ---------- Descontar lo enviado ----------

    @Test
    fun descontarEnvios_restaLoEnviadoYNoTocaLosDemas() {
        val resultado = descontarEnvios(inventario, listOf(EnvioASecar(1, 50.0)))
        assertEquals(70.0, resultado.first { it.sublote.id == 1L }.sublote.pesoDisponibleKg, 0.0)
        assertEquals(80.0, resultado.first { it.sublote.id == 2L }.sublote.pesoDisponibleKg, 0.0)
        assertEquals(200.0, resultado.first { it.sublote.id == 5L }.sublote.pesoDisponibleKg, 0.0)
    }

    @Test
    fun descontarEnvios_todoElSubloteLoDejaEnCeroYAgotado() {
        val resultado = descontarEnvios(inventario, listOf(EnvioASecar(1, 120.0)))
        val sublote = resultado.first { it.sublote.id == 1L }.sublote
        assertEquals(0.0, sublote.pesoDisponibleKg, 0.0)
        assertTrue(sublote.estaAgotado)
    }

    @Test
    fun descontarEnvios_variosSublotesAlMismoTiempo() {
        val resultado = descontarEnvios(inventario, listOf(EnvioASecar(1, 20.0), EnvioASecar(4, 30.0)))
        assertEquals(100.0, resultado.first { it.sublote.id == 1L }.sublote.pesoDisponibleKg, 0.0)
        assertEquals(0.0, resultado.first { it.sublote.id == 4L }.sublote.pesoDisponibleKg, 0.0)
    }

    @Test
    fun descontarEnvios_conDecimalesNoDejaRuido() {
        val inv = listOf(subloteDePrueba(1, TipoCafe.VERDE, Calidad.BUENA, 0.3))
        val resultado = descontarEnvios(inv, listOf(EnvioASecar(1, 0.1)))
        // 0.3 - 0.1 en Double da 0.19999999999999998
        assertEquals(0.2, resultado.single().sublote.pesoDisponibleKg, 0.0)
    }

    @Test
    fun descontarEnvios_sinEnviosNoCambiaNada() {
        assertEquals(inventario, descontarEnvios(inventario, emptyList()))
    }
}
