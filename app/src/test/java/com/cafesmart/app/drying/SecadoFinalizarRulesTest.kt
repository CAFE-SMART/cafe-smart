package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EnvioSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SecadoSublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.drying.AYUDA_FACTOR
import com.cafesmart.app.viewmodel.drying.MENSAJE_FACTOR_FUERA_DE_RANGO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FACTOR_NO_NUMERICO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FECHA_FIN_ANTES_DEL_INICIO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FECHA_FIN_FUTURA
import com.cafesmart.app.viewmodel.drying.MENSAJE_KILOS_NEGATIVOS
import com.cafesmart.app.viewmodel.drying.MENSAJE_KILOS_SIN_CAFE
import com.cafesmart.app.viewmodel.drying.costoKgSeco
import com.cafesmart.app.viewmodel.drying.errorFactor
import com.cafesmart.app.viewmodel.drying.errorFechaFin
import com.cafesmart.app.viewmodel.drying.errorKilosSecos
import com.cafesmart.app.viewmodel.drying.factorDesdeTexto
import com.cafesmart.app.viewmodel.drying.factorValido
import com.cafesmart.app.viewmodel.drying.kilosSecosDesdeTexto
import com.cafesmart.app.viewmodel.drying.kilosSecosTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadoFinalizarRulesTest {

    private fun kilos(buena: Double = 0.0, regular: Double = 0.0, mala: Double = 0.0) =
        mapOf(Calidad.BUENA to buena, Calidad.REGULAR to regular, Calidad.MALA to mala)

    // ---------- Kilos secos ----------

    @Test
    fun kilosSecosTotal_sumaLasTresCalidades() {
        assertEquals(270.0, kilosSecosTotal(kilos(140.0, 118.0, 12.0)), 0.0)
    }

    @Test
    fun kilosSecosTotal_unaCalidadQueFaltaVale0() {
        assertEquals(50.0, kilosSecosTotal(mapOf(Calidad.MALA to 50.0)), 0.0)
        assertEquals(0.0, kilosSecosTotal(emptyMap()), 0.0)
    }

    @Test
    fun kilosSecosTotal_noArrastraRestosDeDecimales() {
        assertEquals(0.3, kilosSecosTotal(kilos(0.1, 0.2)), 0.0)
    }

    @Test
    fun kilosSecosDesdeTexto_vacioVale0() {
        assertEquals(0.0, kilosSecosDesdeTexto(""), 0.0)
        assertEquals(118.0, kilosSecosDesdeTexto("118"), 0.0)
        assertEquals(12.5, kilosSecosDesdeTexto("12,5"), 0.0)
        assertEquals(1500.0, kilosSecosDesdeTexto("1.500"), 0.0)
    }

    @Test
    fun errorKilosSecos_casoDelFigmaEsValido() {
        assertNull(errorKilosSecos(kilos(140.0, 118.0, 12.0), 290.0))
    }

    @Test
    fun errorKilosSecos_camposVaciosValen0YBastaUnaCalidad() {
        assertNull(errorKilosSecos(kilos(mala = 5.0), 290.0))
    }

    @Test
    fun errorKilosSecos_sumaCeroPideAlMenosUnaCalidad() {
        assertEquals(MENSAJE_KILOS_SIN_CAFE, errorKilosSecos(kilos(), 290.0))
        assertEquals(MENSAJE_KILOS_SIN_CAFE, errorKilosSecos(emptyMap(), 290.0))
    }

    @Test
    fun errorKilosSecos_negativoEsInvalidoAunqueLaSumaAlcance() {
        assertEquals(MENSAJE_KILOS_NEGATIVOS, errorKilosSecos(kilos(100.0, -5.0, 0.0), 290.0))
    }

    @Test
    fun errorKilosSecos_sumaJustoIgualAlEnviadoEsValida() {
        assertNull(errorKilosSecos(kilos(150.0, 100.0, 40.0), 290.0))
    }

    @Test
    fun errorKilosSecos_sumaMayorDiceElMaximo() {
        assertEquals("Entre los tres, máximo 290 kg.", errorKilosSecos(kilos(150.0, 100.0, 41.0), 290.0))
        assertEquals("Entre los tres, máximo 12,5 kg.", errorKilosSecos(kilos(13.0), 12.5))
    }

    // ---------- Factor de rendimiento ----------

    @Test
    fun factor_80y120SonValidos() {
        assertNull(errorFactor("80"))
        assertNull(errorFactor("120"))
        assertTrue(factorValido("80"))
        assertTrue(factorValido("120"))
        assertTrue(factorValido("92"))
    }

    @Test
    fun factor_79y121EstanFueraDeRango() {
        assertEquals(MENSAJE_FACTOR_FUERA_DE_RANGO, errorFactor("79"))
        assertEquals(MENSAJE_FACTOR_FUERA_DE_RANGO, errorFactor("121"))
        assertFalse(factorValido("79"))
        assertFalse(factorValido("121"))
        assertEquals(MENSAJE_FACTOR_FUERA_DE_RANGO, errorFactor("99999999999999"))
    }

    @Test
    fun factor_textoNoNumericoDiceSoloNumeros() {
        listOf("abc", "92,5", "9 2", "-90", "9a").forEach {
            assertEquals(it, MENSAJE_FACTOR_NO_NUMERICO, errorFactor(it))
            assertFalse(it, factorValido(it))
        }
    }

    @Test
    fun factor_vacioEsObligatorioPeroSinErrorEnRojo() {
        assertNull(errorFactor(""))
        assertNull(errorFactor("  "))
        assertFalse(factorValido(""))
        assertNull(factorDesdeTexto(""))
    }

    @Test
    fun factor_losTextosSonLosAcordados() {
        assertEquals("Escribe solo números.", MENSAJE_FACTOR_NO_NUMERICO)
        assertEquals("Recuerda: El factor de rendimiento que escribas debe estar entre 80 y 120.", MENSAJE_FACTOR_FUERA_DE_RANGO)
        assertEquals("Entre 80 y 120.", AYUDA_FACTOR)
    }

    // ---------- Fecha de fin ----------

    private val inicio = Fecha(1, 6, 2026)
    private val hoy = HOY_FIGMA

    @Test
    fun fechaFin_anteriorAlInicioEsInvalida() {
        assertEquals(MENSAJE_FECHA_FIN_ANTES_DEL_INICIO, errorFechaFin(inicio, Fecha(31, 5, 2026), hoy))
    }

    @Test
    fun fechaFin_igualAlInicioEsValida() {
        assertNull(errorFechaFin(inicio, inicio, hoy))
    }

    @Test
    fun fechaFin_igualAHoyEsValida() {
        assertNull(errorFechaFin(inicio, hoy, hoy))
    }

    @Test
    fun fechaFin_futuraEsInvalida() {
        assertEquals(MENSAJE_FECHA_FIN_FUTURA, errorFechaFin(inicio, Fecha(4, 6, 2026), hoy))
        assertEquals(MENSAJE_FECHA_FIN_FUTURA, errorFechaFin(inicio, Fecha(1, 1, 2027), hoy))
    }

    // ---------- Costo del cafe seco ----------

    private fun envio(id: Long, kg: Double, costoKg: Long): EnvioSecado {
        val item = subloteDePrueba(id, TipoCafe.VERDE, Calidad.REGULAR, kg)
        return EnvioSecado(SecadoSublote(1, id, kg), item.sublote.copy(costoKg = costoKg))
    }

    @Test
    fun costoKgSeco_dosSublotesDeCostoDistintoYMermaRepartidaEntreLosSecos() {
        // 100 kg x 10.000 + 200 kg x 16.000 = 4.200.000; salen 280 kg secos -> 15.000 por kg.
        val enviados = listOf(envio(1, 100.0, 10_000), envio(2, 200.0, 16_000))
        assertEquals(15_000L, costoKgSeco(enviados, 280.0))
        // Con 270 kg secos: 15.555,56 -> 15.556.
        assertEquals(15_556L, costoKgSeco(enviados, 270.0))
    }

    @Test
    fun costoKgSeco_casoDelFigma() {
        // 290 kg a 14.000 y salen 270 kg: 4.060.000 / 270 = 15.037,04 -> 15.037.
        assertEquals(15_037L, costoKgSeco(listOf(envio(1, 290.0, 14_000)), 270.0))
    }

    @Test
    fun costoKgSeco_sinMermaEsElCostoDelEnviado() {
        assertEquals(14_000L, costoKgSeco(listOf(envio(1, 50.0, 14_000)), 50.0))
    }

    @Test
    fun costoKgSeco_redondeaLaMitadHaciaArriba() {
        // 5 / 2 = 2,5 -> 3
        assertEquals(3L, costoKgSeco(listOf(envio(1, 1.0, 5)), 2.0))
    }

    @Test
    fun costoKgSeco_exigeKilosSecosMayoresA0() {
        val enviados = listOf(envio(1, 10.0, 14_000))
        assertThrows(IllegalArgumentException::class.java) { costoKgSeco(enviados, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { costoKgSeco(enviados, -1.0) }
    }
}
