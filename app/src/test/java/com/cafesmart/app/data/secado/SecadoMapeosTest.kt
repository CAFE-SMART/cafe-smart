package com.cafesmart.app.data.secado

import com.cafesmart.app.data.local.SecadoEntity
import com.cafesmart.app.data.local.SecadoSubloteEntity
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Secado
import com.cafesmart.app.data.model.SecadoSublote
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SecadoMapeosTest {

    private val inicio = Fecha(3, 6, 2026)
    private val fin = Fecha(10, 6, 2026)

    @Test
    fun secadoEnCurso_guardaFechasEnMilisYSinFechaFinNiFactor() {
        val e = Secado(id = 7, fechaInicio = inicio).aEntidad()
        assertEquals(7L, e.id)
        assertEquals(fechaAMilis(inicio), e.fechaInicio)
        assertNull(e.fechaFin)
        assertEquals("EN_CURSO", e.estado)
        assertNull(e.factorRendimiento)
    }

    @Test
    fun secadoFinalizado_ida_y_vuelta() {
        val secado = Secado(id = 2, fechaInicio = inicio, fechaFin = fin, estado = EstadoSecado.FINALIZADO, factorRendimiento = 92)
        val entidad = secado.aEntidad()
        assertEquals(fechaAMilis(fin), entidad.fechaFin)
        assertEquals("FINALIZADO", entidad.estado)
        assertEquals(92, entidad.factorRendimiento)
        assertEquals(secado, entidad.aModelo())
    }

    @Test
    fun secadoCancelado_ida_y_vuelta() {
        val secado = Secado(id = 3, fechaInicio = inicio, estado = EstadoSecado.CANCELADO)
        assertEquals("CANCELADO", secado.aEntidad().estado)
        assertEquals(secado, secado.aEntidad().aModelo())
    }

    @Test
    fun losTresEstadosSeGuardanConElNombreDelEnum() {
        // La consulta del DAO usa 'EN_CURSO' y 'FINALIZADO' escritos a mano: este test avisa si el enum cambia de nombre.
        assertEquals(listOf("EN_CURSO", "FINALIZADO", "CANCELADO"), EstadoSecado.entries.map { it.name })
    }

    @Test
    fun estadoDesconocidoEnLaFilaFalla() {
        val fila = SecadoEntity(id = 1, fechaInicio = fechaAMilis(inicio), estado = "OTRO")
        assertThrows(IllegalArgumentException::class.java) { fila.aModelo() }
    }

    @Test
    fun factorFueraDeRangoEnLaFilaFalla() {
        val fila = SecadoEntity(id = 1, fechaInicio = fechaAMilis(inicio), estado = "EN_CURSO", factorRendimiento = 200)
        assertThrows(IllegalArgumentException::class.java) { fila.aModelo() }
    }

    @Test
    fun envio_ida_y_vuelta() {
        val envio = SecadoSublote(secadoId = 4, subloteId = 9, pesoEnviadoKg = 12.5)
        val fila = envio.aEntidad()
        assertEquals(SecadoSubloteEntity(secadoId = 4, subloteId = 9, pesoEnviadoKg = 12.5), fila)
        assertEquals(envio, fila.aModelo())
    }

    @Test
    fun envioConPesoCeroEnLaFilaFalla() {
        assertThrows(IllegalArgumentException::class.java) { SecadoSubloteEntity(1, 1, 0.0).aModelo() }
    }
}
