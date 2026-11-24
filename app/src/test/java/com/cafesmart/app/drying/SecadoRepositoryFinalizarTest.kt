package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadoRepositoryFinalizarTest {

    private val inicio = Fecha(3, 6, 2026)
    private val fin = Fecha(10, 6, 2026)

    // El inventario del Figma con el cafe verde a $ 14.000 por kg (asi la merma de 20 kg vale $ 280.000).
    private fun inventario14000(): List<SubloteConLote> =
        inventarioFigma().map { it.copy(sublote = it.sublote.copy(costoKg = 14_000)) }

    private fun repositorio() = repositorioDePrueba(inventario14000())

    private fun enviarTodoElVerde(repositorio: SecadoRepositoryEjemplo): Long =
        repositorio.iniciarSecado(
            inicio,
            listOf(EnvioASecar(1, 120.0), EnvioASecar(2, 80.0), EnvioASecar(3, 60.0), EnvioASecar(4, 30.0))
        ).id

    private fun kilos(buena: Double = 0.0, regular: Double = 0.0, mala: Double = 0.0) =
        mapOf(Calidad.BUENA to buena, Calidad.REGULAR to regular, Calidad.MALA to mala)

    private fun disponible(repositorio: SecadoRepositoryEjemplo, id: Long): Double =
        repositorio.inventario().first { it.sublote.id == id }.sublote.pesoDisponibleKg

    // ---------- Finalizar ----------

    @Test
    fun finalizar_casoDelFigma() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        val detalle = repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)

        assertEquals(290.0, detalle.pesoEnviadoKg, 0.0)
        assertEquals(270.0, detalle.pesoSecoKg, 0.0)
        assertEquals(20.0, detalle.mermaKg, 0.0)
        assertEquals(6.9, detalle.mermaPorcentaje, 0.05)
        assertEquals(280_000L, detalle.valorMerma)
        assertEquals(EstadoSecado.FINALIZADO, detalle.secado.estado)
        assertEquals(fin, detalle.secado.fechaFin)
        assertEquals(92, detalle.secado.factorRendimiento)
        assertEquals(4, detalle.enviados.size)
    }

    @Test
    fun finalizar_creaUnSubloteSecoPorCalidadConLosDatosDelSecado() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        val detalle = repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)

        assertEquals(listOf(140.0, 118.0, 12.0), detalle.resultantes.map { it.pesoInicialKg })
        detalle.resultantes.forEach {
            assertEquals(it.pesoInicialKg, it.pesoDisponibleKg, 0.0)
            assertEquals(id, it.secadoId)
            assertNull(it.compraId)
            assertEquals(15_037L, it.costoKg)   // 4.060.000 / 270
            assertEquals(fin, it.fechaIngreso)
            assertEquals(92, it.factorRendimiento)
        }
        // Estan en el inventario, cada uno en un lote seco de su calidad.
        val secos = repositorio.inventario().filter { it.sublote.secadoId == id }
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), secos.map { it.calidad })
        assertTrue(secos.all { it.tipoCafe == TipoCafe.SECO })
        // Los ids no chocan con los que ya habia.
        val ids = repositorio.inventario().map { it.sublote.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun finalizar_soloCreaLasCalidadesConKgMayoresA0() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        val detalle = repositorio.finalizarSecado(id, fin, kilos(buena = 0.0, regular = 100.0, mala = 0.0), 100)
        assertEquals(1, detalle.resultantes.size)
        val seco = repositorio.inventario().filter { it.sublote.secadoId == id }
        assertEquals(listOf(Calidad.REGULAR), seco.map { it.calidad })
    }

    @Test
    fun finalizar_usaElLoteSecoQueYaExisteYCreaLosQueFaltan() {
        val repositorio = repositorio()
        val loteSecoBuena = repositorio.inventario().first { it.sublote.id == 5L }.lote
        val id = enviarTodoElVerde(repositorio)
        repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)

        val nuevos = repositorio.inventario().filter { it.sublote.secadoId == id }
        // Seco bueno ya existia (sublote 5): se reutiliza.
        assertEquals(loteSecoBuena, nuevos.first { it.calidad == Calidad.BUENA }.lote)
        // Seco regular y malo no existian: lotes nuevos, distintos entre si y de todos los anteriores.
        val loteRegular = nuevos.first { it.calidad == Calidad.REGULAR }.lote
        val loteMalo = nuevos.first { it.calidad == Calidad.MALA }.lote
        assertNotEquals(loteRegular.id, loteMalo.id)
        val anteriores = inventario14000().map { it.lote.id }
        assertFalse(loteRegular.id in anteriores)
        assertFalse(loteMalo.id in anteriores)
    }

    @Test
    fun finalizar_unSegundoSecadoReutilizaElLoteSecoQueCreoElPrimero() {
        val repositorio = repositorio()
        val primero = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 50.0))).id
        repositorio.finalizarSecado(primero, fin, kilos(regular = 20.0), 100)
        val segundo = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(2, 50.0))).id
        repositorio.finalizarSecado(segundo, fin, kilos(regular = 10.0), 100)

        val secosRegular = repositorio.inventario().filter { it.tipoCafe == TipoCafe.SECO && it.calidad == Calidad.REGULAR }
        assertEquals(2, secosRegular.size)
        assertEquals(1, secosRegular.map { it.lote.id }.toSet().size)
    }

    @Test
    fun finalizar_dosVecesFalla() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)
        val antes = repositorio.inventario()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)
        }
        assertEquals(antes, repositorio.inventario())
    }

    @Test
    fun finalizar_unSecadoCanceladoFalla() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        repositorio.cancelarSecado(id)
        val antes = repositorio.inventario()
        assertThrows(IllegalArgumentException::class.java) {
            repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)
        }
        assertEquals(antes, repositorio.inventario())
    }

    @Test
    fun finalizar_unSecadoQueNoExisteFalla() {
        assertThrows(IllegalArgumentException::class.java) {
            repositorio().finalizarSecado(99, fin, kilos(10.0), 92)
        }
    }

    @Test
    fun finalizar_unErrorNoDejaCambiosAMedias() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        val inventarioAntes = repositorio.inventario()
        val enCursoAntes = repositorio.secadosEnCurso()

        val invalidos = listOf<() -> Unit>(
            { repositorio.finalizarSecado(id, Fecha(2, 6, 2026), kilos(140.0, 118.0, 12.0), 92) },  // fecha anterior al inicio
            { repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 79) },                 // factor bajo
            { repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 121) },                // factor alto
            { repositorio.finalizarSecado(id, fin, kilos(), 92) },                                   // suma 0
            { repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 33.0), 92) },                 // pasa de lo enviado
            { repositorio.finalizarSecado(id, fin, kilos(140.0, -1.0, 12.0), 92) }                   // negativo
        )
        invalidos.forEach { intento ->
            assertThrows(IllegalArgumentException::class.java) { intento() }
            assertEquals(inventarioAntes, repositorio.inventario())
            assertEquals(enCursoAntes, repositorio.secadosEnCurso())
        }
        // Y el secado sigue pudiendose finalizar bien.
        assertEquals(EstadoSecado.FINALIZADO, repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92).secado.estado)
    }

    @Test
    fun finalizar_fechaIgualAlInicioEsValida() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        assertEquals(inicio, repositorio.finalizarSecado(id, inicio, kilos(100.0), 92).secado.fechaFin)
    }

    // ---------- Secados en curso ----------

    @Test
    fun secadosEnCurso_sinSecadosEstaVacio() {
        assertTrue(repositorio().secadosEnCurso().isEmpty())
    }

    @Test
    fun secadosEnCurso_noMuestraFinalizadosNiCancelados() {
        val repositorio = repositorio()
        val a = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 10.0))).id
        val b = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(2, 10.0))).id
        val c = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(3, 10.0))).id
        repositorio.finalizarSecado(a, fin, kilos(5.0), 100)
        repositorio.cancelarSecado(b)
        assertEquals(listOf(c), repositorio.secadosEnCurso().map { it.secado.id })
    }

    @Test
    fun secadosEnCurso_elMasRecientePrimeroYSinResultantes() {
        val repositorio = repositorio()
        val viejo = repositorio.iniciarSecado(Fecha(1, 6, 2026), listOf(EnvioASecar(1, 10.0))).id
        val nuevo = repositorio.iniciarSecado(Fecha(3, 6, 2026), listOf(EnvioASecar(2, 10.0))).id
        val medio = repositorio.iniciarSecado(Fecha(2, 6, 2026), listOf(EnvioASecar(3, 10.0))).id
        val mismoDiaQueElNuevo = repositorio.iniciarSecado(Fecha(3, 6, 2026), listOf(EnvioASecar(4, 10.0))).id

        val lista = repositorio.secadosEnCurso()
        // Por fecha de inicio; el mismo dia, el que se creo despues va primero.
        assertEquals(listOf(mismoDiaQueElNuevo, nuevo, medio, viejo), lista.map { it.secado.id })
        assertTrue(lista.all { it.resultantes.isEmpty() })
        assertTrue(lista.all { it.secado.estado == EstadoSecado.EN_CURSO })
    }

    @Test
    fun secadosEnCurso_guardaLosEnviosConElSubloteComoEstabaAlEnviarse() {
        val repositorio = repositorio()
        repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 50.0)))
        val envio = repositorio.secadosEnCurso().single().enviados.single()
        assertEquals(50.0, envio.envio.pesoEnviadoKg, 0.0)
        assertEquals(1L, envio.sublote.id)
        assertEquals(120.0, envio.sublote.pesoDisponibleKg, 0.0)   // la foto es de antes de descontar
        assertEquals(70.0, disponible(repositorio, 1), 0.0)
    }

    // ---------- Cancelar ----------

    @Test
    fun cancelar_devuelveElCafeAUnSubloteQueSeguiaEnElInventario() {
        val repositorio = repositorio()
        val id = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 50.0), EnvioASecar(2, 20.0))).id
        assertEquals(70.0, disponible(repositorio, 1), 0.0)
        repositorio.cancelarSecado(id)
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
        assertEquals(80.0, disponible(repositorio, 2), 0.0)
        assertEquals(inventario14000(), repositorio.inventario())
        assertTrue(repositorio.secadosEnCurso().isEmpty())
    }

    @Test
    fun cancelar_devuelveElCafeDeUnSubloteQueSeHabiaAgotado() {
        val repositorio = repositorio()
        val id = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(4, 30.0))).id
        assertEquals(0.0, disponible(repositorio, 4), 0.0)
        repositorio.cancelarSecado(id)
        assertEquals(30.0, disponible(repositorio, 4), 0.0)
        assertEquals(1, repositorio.inventario().count { it.sublote.id == 4L })
    }

    @Test
    fun cancelar_sumaLoDevueltoAlRestoQueSeguiaDisponible() {
        val repositorio = repositorio()
        val primero = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 100.0))).id
        val segundo = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(1, 20.0))).id
        repositorio.cancelarSecado(primero)
        assertEquals(100.0, disponible(repositorio, 1), 0.0)
        repositorio.cancelarSecado(segundo)
        assertEquals(120.0, disponible(repositorio, 1), 0.0)
    }

    @Test
    fun cancelar_marcaElSecadoCanceladoYNoSePuedeCancelarOtraVez() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        repositorio.cancelarSecado(id)
        val antes = repositorio.inventario()
        assertThrows(IllegalArgumentException::class.java) { repositorio.cancelarSecado(id) }
        assertEquals(antes, repositorio.inventario())   // no devuelve el cafe dos veces
    }

    @Test
    fun cancelar_unSecadoYaFinalizadoFalla() {
        val repositorio = repositorio()
        val id = enviarTodoElVerde(repositorio)
        repositorio.finalizarSecado(id, fin, kilos(140.0, 118.0, 12.0), 92)
        val antes = repositorio.inventario()
        assertThrows(IllegalArgumentException::class.java) { repositorio.cancelarSecado(id) }
        assertEquals(antes, repositorio.inventario())
    }

    @Test
    fun cancelar_unSecadoQueNoExisteFalla() {
        assertThrows(IllegalArgumentException::class.java) { repositorio().cancelarSecado(99) }
    }
}
