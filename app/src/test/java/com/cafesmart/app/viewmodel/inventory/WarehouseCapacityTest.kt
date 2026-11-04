package com.cafesmart.app.viewmodel.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class WarehouseCapacityTest {

    private fun estado(kg: Double, capacidad: Double = 1000.0) =
        calcularOcupacionBodega(kg, capacidad).estado

    @Test
    fun calculaElPorcentaje() {
        assertEquals(18.63, calcularOcupacionBodega(559.0, 3000.0).porcentaje, 0.01)
    }

    @Test
    fun bodegaVaciaEsNormal() {
        val r = calcularOcupacionBodega(0.0, 1000.0)
        assertEquals(0.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.NORMAL, r.estado)
    }

    @Test
    fun justoAntesDel80EsNormal() {
        assertEquals(EstadoBodega.NORMAL, estado(799.0)) // 79,9 %
    }

    @Test
    fun exactamente80EsAdvertencia() {
        assertEquals(EstadoBodega.ADVERTENCIA, estado(800.0))
    }

    @Test
    fun justoAntesDel90EsAdvertencia() {
        assertEquals(EstadoBodega.ADVERTENCIA, estado(899.0)) // 89,9 %
    }

    @Test
    fun exactamente90EsCritica() {
        assertEquals(EstadoBodega.CRITICA, estado(900.0))
    }

    @Test
    fun exactamente100EsCritica() {
        assertEquals(EstadoBodega.CRITICA, estado(1000.0))
    }

    @Test
    fun superarLaCapacidadNoSeLimitaA100() {
        val r = calcularOcupacionBodega(1200.0, 1000.0)
        assertEquals(120.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.CRITICA, r.estado)
    }

    @Test
    fun umbralesPersonalizados() {
        val r = calcularOcupacionBodega(700.0, 1000.0, umbralAdvertencia = 70.0, umbralCritico = 85.0)
        assertEquals(EstadoBodega.ADVERTENCIA, r.estado)
    }

    // --- Capacidad 0: usuario nuevo que aún no configura la capacidad de su bodega ---

    @Test
    fun capacidadCeroSinCafeEsSinCapacidad() {
        val r = calcularOcupacionBodega(0.0, 0.0)
        assertEquals(0.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, r.estado)
    }

    @Test
    fun capacidadCeroConCafeEsSinCapacidadYNoDivideEntreCero() {
        val r = calcularOcupacionBodega(500.0, 0.0)
        assertEquals(0.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, r.estado)
    }

    @Test
    fun capacidadCeroConUmbralesPersonalizadosEsSinCapacidad() {
        val r = calcularOcupacionBodega(500.0, 0.0, umbralAdvertencia = 10.0, umbralCritico = 20.0)
        assertEquals(EstadoBodega.SIN_CAPACIDAD, r.estado)
    }

    @Test
    fun mensajeDeCapacidadNoConfigurada() {
        assertEquals("Falta configurar la capacidad de tu bodega", MENSAJE_CAPACIDAD_NO_CONFIGURADA)
    }

    @Test
    fun capacidadConfiguradaYBodegaVaciaNoEsSinCapacidad() {
        // kg en 0 con capacidad > 0 es NORMAL; no se confunde con capacidad sin configurar.
        assertEquals(EstadoBodega.NORMAL, calcularOcupacionBodega(0.0, 1.0).estado)
    }

    @Test(expected = IllegalArgumentException::class)
    fun capacidadCeroConKgNegativosLanzaExcepcion() {
        calcularOcupacionBodega(-1.0, 0.0)
    }

    // --- Bodega vaciada tras vender todo ---

    @Test
    fun vendiTodoElCafeLaBodegaVaciaEsNormal() {
        val r = calcularOcupacionBodega(0.0, 3000.0)
        assertEquals(0.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.NORMAL, r.estado)
    }

    // --- Porcentaje y decimales ---

    @Test
    fun porcentajeConDecimales() {
        assertEquals(33.333, calcularOcupacionBodega(1.0, 3.0).porcentaje, 0.001)
    }

    @Test
    fun kgConDecimales() {
        val r = calcularOcupacionBodega(799.9, 1000.0)
        assertEquals(79.99, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.NORMAL, r.estado)
    }

    @Test
    fun justoSobreElUmbralCriticoConDecimales() {
        assertEquals(EstadoBodega.CRITICA, estado(900.1))
    }

    @Test
    fun capacidadDistintaDeMil() {
        assertEquals(EstadoBodega.ADVERTENCIA, estado(2400.0, 3000.0)) // 80 %
        assertEquals(EstadoBodega.CRITICA, estado(2700.0, 3000.0)) // 90 %
        assertEquals(EstadoBodega.NORMAL, estado(2399.0, 3000.0)) // 79,97 %
    }

    @Test
    fun capacidadMuyPequena() {
        assertEquals(EstadoBodega.CRITICA, estado(1.0, 1.0))
    }

    @Test
    fun cantidadesGrandes() {
        val r = calcularOcupacionBodega(5_000_000.0, 10_000_000.0)
        assertEquals(50.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.NORMAL, r.estado)
    }

    @Test
    fun muchoMasDelDobleDeLaCapacidadSigueSiendoCritica() {
        val r = calcularOcupacionBodega(5000.0, 1000.0)
        assertEquals(500.0, r.porcentaje, 1e-9)
        assertEquals(EstadoBodega.CRITICA, r.estado)
    }

    // --- Casos de compra: antes y después de comprar ---

    @Test
    fun unaCompraPuedeCambiarElEstadoDeNormalACritica() {
        val antes = calcularOcupacionBodega(700.0, 1000.0)
        val despues = calcularOcupacionBodega(700.0 + 250.0, 1000.0)
        assertEquals(EstadoBodega.NORMAL, antes.estado)
        assertEquals(EstadoBodega.CRITICA, despues.estado)
    }

    @Test
    fun unaVentaPuedeBajarElEstadoDeCriticaANormal() {
        val antes = calcularOcupacionBodega(950.0, 1000.0)
        val despues = calcularOcupacionBodega(950.0 - 400.0, 1000.0)
        assertEquals(EstadoBodega.CRITICA, antes.estado)
        assertEquals(EstadoBodega.NORMAL, despues.estado)
    }

    @Test
    fun cambiarLaCapacidadEnAjustesCambiaElEstado() {
        assertEquals(EstadoBodega.CRITICA, estado(950.0, 1000.0))
        assertEquals(EstadoBodega.NORMAL, estado(950.0, 2000.0))
    }

    // --- Umbrales ---

    @Test
    fun valoresPorDefectoDeLosUmbrales() {
        assertEquals(80.0, UMBRAL_ADVERTENCIA_POR_DEFECTO, 0.0)
        assertEquals(90.0, UMBRAL_CRITICO_POR_DEFECTO, 0.0)
    }

    @Test
    fun umbralesPersonalizadosEnLosLimites() {
        fun e(kg: Double) = calcularOcupacionBodega(kg, 1000.0, umbralAdvertencia = 70.0, umbralCritico = 85.0).estado
        assertEquals(EstadoBodega.NORMAL, e(699.0))
        assertEquals(EstadoBodega.ADVERTENCIA, e(700.0))
        assertEquals(EstadoBodega.ADVERTENCIA, e(849.0))
        assertEquals(EstadoBodega.CRITICA, e(850.0))
    }

    @Test
    fun soloSePersonalizaElUmbralCritico() {
        val r = calcularOcupacionBodega(950.0, 1000.0, umbralCritico = 98.0)
        assertEquals(EstadoBodega.ADVERTENCIA, r.estado)
    }

    @Test
    fun resultadoEsComparablePorValor() {
        assertEquals(OcupacionBodega(50.0, EstadoBodega.NORMAL), calcularOcupacionBodega(500.0, 1000.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun umbralesIgualesLanzaExcepcion() {
        calcularOcupacionBodega(10.0, 1000.0, umbralAdvertencia = 80.0, umbralCritico = 80.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun umbralAdvertenciaCeroLanzaExcepcion() {
        calcularOcupacionBodega(10.0, 1000.0, umbralAdvertencia = 0.0, umbralCritico = 90.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun umbralAdvertenciaNegativoLanzaExcepcion() {
        calcularOcupacionBodega(10.0, 1000.0, umbralAdvertencia = -5.0, umbralCritico = 90.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun capacidadNegativaLanzaExcepcion() {
        calcularOcupacionBodega(10.0, -5.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun kgNegativosLanzaExcepcion() {
        calcularOcupacionBodega(-1.0, 1000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun umbralCriticoMenorOIgualQueAdvertenciaLanzaExcepcion() {
        calcularOcupacionBodega(10.0, 1000.0, umbralAdvertencia = 90.0, umbralCritico = 80.0)
    }
}
