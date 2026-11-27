package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadoRepositoryEjemploTest {

    private val fecha = Fecha(3, 6, 2026)

    private fun disponible(repositorio: SecadoRepositoryEjemplo, id: Long): Double =
        repositorio.inventario().first { it.sublote.id == id }.sublote.pesoDisponibleKg

    @Test
    fun iniciarSecado_devuelveUnSecadoEnCursoConSuFecha() {
        val secado = repositorioDePrueba().iniciarSecado(fecha, listOf(EnvioASecar(1, 50.0)))
        assertEquals(EstadoSecado.EN_CURSO, secado.estado)
        assertEquals(fecha, secado.fechaInicio)
        assertNull(secado.fechaFin)
    }

    @Test
    fun iniciarSecado_descuentaLoEnviadoDeCadaSublote() {
        val repositorio = repositorioDePrueba()
        repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 50.0), EnvioASecar(4, 30.0)))
        assertEquals(70.0, disponible(repositorio, 1), 0.0)
        assertEquals(0.0, disponible(repositorio, 4), 0.0)
        // Los que no se enviaron quedan igual.
        assertEquals(80.0, disponible(repositorio, 2), 0.0)
        assertEquals(60.0, disponible(repositorio, 3), 0.0)
        assertEquals(200.0, disponible(repositorio, 5), 0.0)
    }

    @Test
    fun iniciarSecado_cadaSecadoTieneSuPropioId() {
        val repositorio = repositorioDePrueba()
        val primero = repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 10.0)))
        val segundo = repositorio.iniciarSecado(fecha, listOf(EnvioASecar(2, 10.0)))
        assertEquals(1L, primero.id)
        assertEquals(2L, segundo.id)
    }

    @Test
    fun iniciarSecado_elSegundoSecadoVeLoQueDejoElPrimero() {
        val repositorio = repositorioDePrueba()
        repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 100.0)))
        // Quedan 20 kg: enviar 21 ya no se puede.
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 21.0)))
        }
        repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 20.0)))
        assertEquals(0.0, disponible(repositorio, 1), 0.0)
    }

    @Test
    fun iniciarSecado_conDecimalesNoDejaRuido() {
        val repositorio = repositorioDePrueba(listOf(subloteDePrueba(1, TipoCafe.VERDE, Calidad.BUENA, 0.3)))
        repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 0.1)))
        assertEquals(0.2, disponible(repositorio, 1), 0.0)
    }

    // ---------- Lo que no se puede ----------

    @Test
    fun sinSublotesNoSeInicia() {
        assertThrows(IllegalArgumentException::class.java) {
            repositorioDePrueba().iniciarSecado(fecha, emptyList())
        }
    }

    @Test
    fun unSubloteQueNoEsVerdeNoSeSeca() {
        val repositorio = repositorioDePrueba()
        // El sublote 5 es seco.
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(5, 50.0)))
        }
        assertEquals(200.0, disponible(repositorio, 5), 0.0)
    }

    @Test
    fun unSubloteQueNoExisteNoSeSeca() {
        assertThrows(IllegalArgumentException::class.java) {
            repositorioDePrueba().iniciarSecado(fecha, listOf(EnvioASecar(99, 10.0)))
        }
    }

    @Test
    fun cantidadCeroONegativaNoSeAcepta() {
        val repositorio = repositorioDePrueba()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 0.0)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, -5.0)))
        }
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
    }

    @Test
    fun cantidadMayorQueLoDisponibleNoSeAcepta() {
        val repositorio = repositorioDePrueba()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 120.5)))
        }
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
    }

    @Test
    fun unSubloteRepetidoNoSeAcepta() {
        val repositorio = repositorioDePrueba()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 10.0), EnvioASecar(1, 10.0)))
        }
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
    }

    @Test
    fun siUnEnvioFallaNoSeDescuentaNingunoDeLosOtros() {
        val repositorio = repositorioDePrueba()
        // El primero sirve y el segundo pasa de lo disponible: no se descuenta ni el primero.
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 50.0), EnvioASecar(2, 81.0)))
        }
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
        assertEquals(80.0, disponible(repositorio, 2), 0.0)
    }

    @Test
    fun unSecadoQueFallaNoGastaUnId() {
        val repositorio = repositorioDePrueba()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 500.0)))
        }
        assertEquals(1L, repositorio.iniciarSecado(fecha, listOf(EnvioASecar(1, 10.0))).id)
    }

    @Test
    fun elInventarioInicialEsElDeEjemploDeLaApp() {
        // Sin pasarle inventario usa los sublotes de ejemplo, y entre ellos hay cafe verde para secar.
        val verdes = SecadoRepositoryEjemplo().inventario().filter { it.tipoCafe == TipoCafe.VERDE }
        assertTrue(verdes.isNotEmpty())
    }
}
