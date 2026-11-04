package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseRulesTest {

    private val hoy = Fecha(dia = 15, mes = 11, anio = 2026)

    private fun validar(
        concepto: String = "Pago de jornaleros",
        tipo: TipoGasto? = TipoGasto.TRANSPORTE,
        valor: Long? = 100_000L,
        fecha: Fecha = hoy,
        descripcion: String = "",
        limites: LimitesGasto = LimitesGasto(),
        tiposPermitidos: List<TipoGasto> = TIPOS_GASTO_PERMITIDOS,
    ) = validarGasto(concepto, tipo, valor, fecha, descripcion, hoy, limites, tiposPermitidos)

    // --- Casos válidos ---

    @Test
    fun gastoValidoNoTieneErrores() {
        val e = validar()
        assertTrue(e.esValido)
        assertEquals(ErroresGasto(), e)
    }

    @Test
    fun cadaTipoDeGastoEsValido() {
        TipoGasto.values().forEach { assertTrue("$it", validar(tipo = it).esValido) }
    }

    @Test
    fun lasConstantesPorDefectoSonLasEsperadas() {
        assertEquals(1_000L, VALOR_GASTO_MINIMO_COP)
        assertEquals(50_000_000L, VALOR_GASTO_MAXIMO_COP)
        assertEquals(60, LARGO_CONCEPTO_GASTO_MAXIMO)
        assertEquals(200, LARGO_DESCRIPCION_GASTO_MAXIMO)
    }

    @Test
    fun losTiposPermitidosSonLosSeisDelProyecto() {
        assertEquals(
            listOf(TipoGasto.TRANSPORTE, TipoGasto.COMIDA, TipoGasto.SECADO, TipoGasto.CARGUE, TipoGasto.DESCARGUE, TipoGasto.OTROS),
            TIPOS_GASTO_PERMITIDOS,
        )
    }

    // --- Concepto (obligatorio) ---

    @Test
    fun conceptoVacioEsError() {
        val e = validar(concepto = "")
        assertEquals("Escribe el concepto del gasto", e.concepto)
        assertFalse(e.esValido)
    }

    @Test
    fun conceptoSoloEspaciosEsError() {
        assertEquals(MSG_CONCEPTO_OBLIGATORIO, validar(concepto = "   ").concepto)
    }

    @Test
    fun conceptoDeUnCaracterEsValido() {
        assertNull(validar(concepto = "A").concepto)
    }

    @Test
    fun conceptoDeSesentaCaracteresEsValido() {
        assertNull(validar(concepto = "a".repeat(60)).concepto)
    }

    @Test
    fun conceptoDeSesentaYUnCaracteresEsError() {
        assertEquals("El concepto puede tener máximo 60 caracteres", validar(concepto = "a".repeat(61)).concepto)
    }

    @Test
    fun losEspaciosDeLosExtremosNoCuentanEnElConcepto() {
        assertNull(validar(concepto = "  " + "a".repeat(60) + "  ").concepto)
    }

    // --- Tipo (obligatorio) ---

    @Test
    fun tipoVacioEsError() {
        assertEquals("Elige el tipo de gasto", validar(tipo = null).tipo)
    }

    @Test
    fun tipoFueraDeLosPermitidosEsError() {
        val e = validar(tipo = TipoGasto.COMIDA, tiposPermitidos = listOf(TipoGasto.TRANSPORTE, TipoGasto.OTROS))
        assertEquals(MSG_TIPO_OBLIGATORIO, e.tipo)
    }

    @Test
    fun tipoDentroDeUnaListaPersonalizadaEsValido() {
        assertNull(validar(tipo = TipoGasto.OTROS, tiposPermitidos = listOf(TipoGasto.OTROS)).tipo)
    }

    // --- Valor (mayor que 0, con máximo) ---

    @Test
    fun valorVacioEsError() {
        assertEquals("Escribe el valor del gasto", validar(valor = null).valor)
    }

    @Test
    fun valorCeroEsError() {
        assertEquals("El valor debe ser mayor que 0", validar(valor = 0L).valor)
    }

    @Test
    fun valorNegativoEsError() {
        assertEquals("El valor debe ser mayor que 0", validar(valor = -500L).valor)
    }

    @Test
    fun valorMinimoDeMilPesosEsValido() {
        assertNull(validar(valor = 1_000L).valor)
    }

    @Test
    fun valorPorDebajoDelMinimoEsError() {
        assertEquals("El valor mínimo es 1.000 pesos", validar(valor = 999L).valor)
        assertEquals("El valor mínimo es 1.000 pesos", validar(valor = 1L).valor)
    }

    @Test
    fun valorMaximoExactoEsValido() {
        assertNull(validar(valor = 50_000_000L).valor)
    }

    @Test
    fun valorUnPesoPorEncimaDelMaximoEsError() {
        assertEquals("El valor máximo es 50.000.000 pesos", validar(valor = 50_000_001L).valor)
    }

    @Test
    fun valorMuyGrandeEsError() {
        assertEquals("El valor máximo es 50.000.000 pesos", validar(valor = Long.MAX_VALUE).valor)
    }

    // --- Límites configurables desde Ajustes ---

    @Test
    fun limiteMaximoPersonalizado() {
        val limites = LimitesGasto(valorMaximoCop = 1_000_000L)
        assertNull(validar(valor = 1_000_000L, limites = limites).valor)
        assertEquals("El valor máximo es 1.000.000 pesos", validar(valor = 1_000_001L, limites = limites).valor)
    }

    @Test
    fun limiteMinimoPersonalizado() {
        val limites = LimitesGasto(valorMinimoCop = 5_000L)
        assertNull(validar(valor = 5_000L, limites = limites).valor)
        assertEquals("El valor mínimo es 5.000 pesos", validar(valor = 4_999L, limites = limites).valor)
    }

    @Test
    fun conLimiteMinimoPersonalizadoCeroSigueDiciendoMayorQueCero() {
        assertEquals("El valor debe ser mayor que 0", validar(valor = 0L, limites = LimitesGasto(valorMinimoCop = 5_000L)).valor)
    }

    @Test
    fun limitesDeTextoPersonalizados() {
        val limites = LimitesGasto(largoConceptoMaximo = 10, largoDescripcionMaximo = 20)
        assertNull(validar(concepto = "a".repeat(10), descripcion = "b".repeat(20), limites = limites).concepto)
        assertEquals("El concepto puede tener máximo 10 caracteres", validar(concepto = "a".repeat(11), limites = limites).concepto)
        assertEquals("La descripción puede tener máximo 20 caracteres", validar(descripcion = "b".repeat(21), limites = limites).descripcion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limiteMinimoCeroLanzaExcepcion() {
        LimitesGasto(valorMinimoCop = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limiteMaximoMenorQueMinimoLanzaExcepcion() {
        LimitesGasto(valorMinimoCop = 10_000L, valorMaximoCop = 5_000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limiteDeConceptoCeroLanzaExcepcion() {
        LimitesGasto(largoConceptoMaximo = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limiteDeDescripcionNegativoLanzaExcepcion() {
        LimitesGasto(largoDescripcionMaximo = -1)
    }

    // --- Fecha (no futura) ---

    @Test
    fun fechaDeHoyEsValida() {
        assertNull(validar(fecha = hoy).fecha)
    }

    @Test
    fun fechaDeAyerEsValida() {
        assertNull(validar(fecha = Fecha(14, 11, 2026)).fecha)
    }

    @Test
    fun fechaDeMananaEsError() {
        assertEquals("La fecha no puede ser futura", validar(fecha = Fecha(16, 11, 2026)).fecha)
    }

    @Test
    fun fechaFuturaEnOtroMesEsError() {
        assertEquals(MSG_FECHA_FUTURA, validar(fecha = Fecha(1, 12, 2026)).fecha)
    }

    @Test
    fun fechaFuturaEnOtroAnioEsError() {
        assertEquals(MSG_FECHA_FUTURA, validar(fecha = Fecha(1, 1, 2027)).fecha)
    }

    @Test
    fun fechaDeUnAnioAnteriorEsValida() {
        assertNull(validar(fecha = Fecha(31, 12, 2025)).fecha)
    }

    @Test
    fun fechaConMesFueraDeRangoEsInvalida() {
        assertEquals("Elige una fecha válida", validar(fecha = Fecha(1, 13, 2026)).fecha)
        assertEquals(MSG_FECHA_INVALIDA, validar(fecha = Fecha(1, 0, 2026)).fecha)
    }

    @Test
    fun fechaConDiaFueraDeRangoEsInvalida() {
        assertEquals(MSG_FECHA_INVALIDA, validar(fecha = Fecha(0, 5, 2026)).fecha)
        assertEquals(MSG_FECHA_INVALIDA, validar(fecha = Fecha(31, 4, 2026)).fecha)
        assertEquals(MSG_FECHA_INVALIDA, validar(fecha = Fecha(29, 2, 2026)).fecha)
    }

    @Test
    fun veintinueveDeFebreroEnAnioBisiestoEsValido() {
        assertNull(validar(fecha = Fecha(29, 2, 2024)).fecha)
    }

    // --- Descripción (opcional, con máximo) ---

    @Test
    fun descripcionVaciaEsValida() {
        assertNull(validar(descripcion = "").descripcion)
    }

    @Test
    fun descripcionDeDoscientosCaracteresEsValida() {
        assertNull(validar(descripcion = "d".repeat(200)).descripcion)
    }

    @Test
    fun descripcionDeDoscientosUnoCaracteresEsError() {
        assertEquals("La descripción puede tener máximo 200 caracteres", validar(descripcion = "d".repeat(201)).descripcion)
    }

    // --- Varios errores a la vez ---

    @Test
    fun formularioVacioMuestraUnErrorPorCampoObligatorio() {
        val e = validar(concepto = "", tipo = null, valor = null)
        assertEquals(MSG_CONCEPTO_OBLIGATORIO, e.concepto)
        assertEquals(MSG_TIPO_OBLIGATORIO, e.tipo)
        assertEquals(MSG_VALOR_OBLIGATORIO, e.valor)
        assertNull(e.fecha)
        assertNull(e.descripcion)
        assertFalse(e.esValido)
    }

    @Test
    fun todosLosCamposConErrorAlMismoTiempo() {
        val e = validar(concepto = " ", tipo = null, valor = 0L, fecha = Fecha(1, 1, 2030), descripcion = "d".repeat(201))
        assertFalse(e.esValido)
        assertEquals(5, listOf(e.concepto, e.tipo, e.valor, e.fecha, e.descripcion).count { it != null })
    }

    @Test
    fun uncampoConErrorNoAfectaLosDemas() {
        val e = validar(valor = 0L)
        assertNull(e.concepto)
        assertNull(e.tipo)
        assertNull(e.fecha)
        assertNull(e.descripcion)
        assertFalse(e.esValido)
    }

    @Test
    fun valorDefectoDeHoyUsaLaFechaDelSistema() {
        // Sin pasar "hoy", una fecha lejana en el futuro siempre es futura.
        val e = validarGasto("Gasolina", TipoGasto.TRANSPORTE, 50_000L, Fecha(1, 1, 2999))
        assertEquals(MSG_FECHA_FUTURA, e.fecha)
    }
}
