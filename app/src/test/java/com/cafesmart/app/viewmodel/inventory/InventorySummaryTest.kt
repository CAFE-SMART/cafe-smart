package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.viewmodel.purchase.formatoKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventorySummaryTest {

    // Sublote de prueba: por defecto queda lleno (disponible = inicial).
    private fun sublote(
        id: Long,
        loteId: Long,
        kg: Double,
        disponible: Double = kg,
        fecha: Fecha = Fecha(1, 11, 2026)
    ) = Sublote(
        id = id,
        loteId = loteId,
        compraId = id,
        pesoInicialKg = kg,
        pesoDisponibleKg = disponible,
        costoKg = 10_000,
        fechaIngreso = fecha
    )

    private fun lote(id: Long, tipo: TipoCafe, vararg sublotes: Sublote) =
        LoteConSublotes(Lote(id, tipo, Calidad.BUENA), sublotes.toList())

    private val ejemplo get() = resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG)

    @Test
    fun resumenDeEjemploReproduceLasCifrasDelFigma() {
        val r = ejemplo
        assertEquals(834.0, r.totalKg, 1e-9)
        assertEquals(
            listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA),
            r.tipos.map { it.tipo }
        )
        assertEquals(listOf(290.0, 521.0, 23.0, 0.0), r.tipos.map { it.pesoKg })
        assertEquals(listOf(4, 4, 1, 0), r.tipos.map { it.cantidadSublotes })
    }

    @Test
    fun ocupacionDeEjemploEs28PorCientoYNormal() {
        val ocupacion = ejemplo.ocupacion
        assertEquals(27.8, ocupacion.porcentaje, 0.01)
        assertEquals(EstadoBodega.NORMAL, ocupacion.estado)
        assertEquals("28%", formatoPorcentaje(ocupacion.porcentaje))
    }

    @Test
    fun textosDeLasTarjetasCoincidenConElFigma() {
        assertEquals("290 kg · 7,1 bultos", detallePeso(290.0))
        assertEquals("521 kg · 12,8 bultos", detallePeso(521.0))
        assertEquals("23 kg · 0,6 bultos", detallePeso(23.0))
        assertEquals("0 kg · 0,0 bultos", detallePeso(0.0))
        // Calidades del Figma "Lista · Verde".
        assertEquals("60 kg · 1,5 bultos", detallePeso(60.0))
        assertEquals("200 kg · 4,9 bultos", detallePeso(200.0))
        assertEquals("30 kg · 0,7 bultos", detallePeso(30.0))
    }

    @Test
    fun unBultoPesa40Coma7KilosSegunElErs() {
        assertEquals(40.7, KG_POR_BULTO, 0.0)
        assertEquals(1.0, kgABultos(40.7), 0.0001)
        // Ejemplo del ERS (RF-G-29): 163 kg son 4 bultos.
        assertEquals("4,0 bultos", formatoBultos(163.0))
    }

    @Test
    fun textoDelTotalYDeLaCapacidadCoincideConElFigma() {
        assertEquals("834 kg", formatoKg(ejemplo.totalKg))
        assertEquals("3.000 kg", formatoKg(ejemplo.capacidadKg))
        assertEquals("20,5 bultos", formatoBultos(ejemplo.totalKg))
    }

    @Test
    fun losSublotesAgotadosNoCuentanNiSuman() {
        val lotes = listOf(
            lote(1, TipoCafe.VERDE, sublote(1, 1, 50.0), sublote(2, 1, 30.0, disponible = 0.0))
        )
        val verde = resumirInventario(lotes, 1000.0).tipos.first { it.tipo == TipoCafe.VERDE }
        assertEquals(1, verde.cantidadSublotes)
        assertEquals(50.0, verde.pesoKg, 1e-9)
    }

    @Test
    fun unTipoSinLotesApareceConCeroKgYSinFecha() {
        val pasilla = ejemplo.tipos.first { it.tipo == TipoCafe.PASILLA }
        assertEquals(0.0, pasilla.pesoKg, 1e-9)
        assertEquals(0, pasilla.cantidadSublotes)
        assertNull(pasilla.ultimoIngreso)
    }

    @Test
    fun masRecienteOrdenaPorElUltimoIngreso() {
        val orden = resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG, OrdenInventario.MAS_RECIENTE)
            .tipos.map { it.tipo }
        assertEquals(listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA), orden)
    }

    @Test
    fun masAntiguoInvierteElOrdenYDejaLosVaciosAlFinal() {
        val orden = resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG, OrdenInventario.MAS_ANTIGUO)
            .tipos.map { it.tipo }
        assertEquals(listOf(TipoCafe.TRILLADO, TipoCafe.SECO, TipoCafe.VERDE, TipoCafe.PASILLA), orden)
    }

    @Test
    fun sinLotesElInventarioEstaVacio() {
        val r = resumirInventario(emptyList(), 3000.0)
        assertTrue(r.estaVacio)
        assertEquals(0.0, r.totalKg, 1e-9)
        assertEquals(EstadoBodega.NORMAL, r.ocupacion.estado)
    }

    @Test
    fun conCafeElInventarioNoEstaVacio() {
        assertFalse(ejemplo.estaVacio)
    }

    @Test
    fun capacidadCeroMarcaSinCapacidad() {
        val r = resumirInventario(lotesEjemplo(), 0.0)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, r.ocupacion.estado)
        assertEquals(MENSAJE_CAPACIDAD_NO_CONFIGURADA, etiquetaEstadoBodega(r.ocupacion.estado))
    }

    @Test
    fun bodegaSobrepasadaMuestraMasDe100PorCiento() {
        val r = resumirInventario(lotesEjemplo(), 500.0)
        assertEquals(EstadoBodega.CRITICA, r.ocupacion.estado)
        assertEquals("167%", formatoPorcentaje(r.ocupacion.porcentaje))
    }

    @Test
    fun etiquetasDelEstadoDeLaBodega() {
        assertEquals("Capacidad normal", etiquetaEstadoBodega(EstadoBodega.NORMAL))
        assertEquals("Capacidad alta", etiquetaEstadoBodega(EstadoBodega.ADVERTENCIA))
        assertEquals("Capacidad crítica", etiquetaEstadoBodega(EstadoBodega.CRITICA))
    }

    @Test
    fun formatoDeSublotes() {
        assertEquals("Sin sublotes", formatoSublotes(0))
        assertEquals("1 sublote", formatoSublotes(1))
        assertEquals("4 sublotes", formatoSublotes(4))
    }

    @Test
    fun elTotalNoArrastraRuidoDeDecimales() {
        // 0,1 + 0,2 en Double da 0.30000000000000004; el resumen lo redondea a gramos.
        val lotes = listOf(
            lote(1, TipoCafe.VERDE, sublote(1, 1, 0.1), sublote(2, 1, 0.2))
        )
        val r = resumirInventario(lotes, 1000.0)
        assertEquals(0.3, r.totalKg, 1e-12)
        assertEquals("0,3 kg", formatoKg(r.totalKg))
    }
}
