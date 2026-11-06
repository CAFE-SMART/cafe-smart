package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

/** Reglas del modelo oficial: importes exactos, fechas, asignaciones e idempotencia demo. */
class GastosValidationTest {
    private val fecha = Fecha(5, 11, 2026)
    private val valido = GastoFormulario(concepto = "Transporte", monto = "12500", fecha = fecha)
    private val sublote = Sublote(7, 1, pesoInicialKg = 10.0, pesoDisponibleKg = 10.0, costoKg = 1000, fechaIngreso = fecha)

    @Test fun conceptoEnBlanco() { assertNotNull(GastosValidation.validar(valido.copy(concepto = "  "), emptyList()).concepto) }
    @Test fun descripcionOpcional() { assertFalse(GastosValidation.validar(valido, emptyList()).hayErrores) }
    @Test fun ceroYNegativos() { listOf("0", "-1", "-500").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun letrasYExponentes() { listOf("abc", "12a", "1e3", "+10", "", "1 000").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun noTruncaDecimales() { listOf("12.50", "12,50", "12.00", "12,00").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun agrupadoresAmbiguosSeRechazan() { listOf("1.000", "1,000", "$1000").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun limiteTecnicoLong() { assertEquals(Long.MAX_VALUE, GastosValidation.pesos(Long.MAX_VALUE.toString())); assertNull(GastosValidation.pesos("9223372036854775808")) }
    @Test fun noInventaMaximoComercial() { assertEquals(100000000000L, GastosValidation.pesos("100000000000")) }
    @Test fun fechaEstrictaYBisiesto() { assertFalse(GastosValidation.fechaValida(Fecha(31, 2, 2026))); assertTrue(GastosValidation.fechaValida(Fecha(29, 2, 2024))); assertFalse(GastosValidation.fechaValida(Fecha(1, 13, 2026))) }
    @Test fun generalSinSublote() { assertNull(GastosValidation.validar(valido, emptyList()).sublotes) }
    @Test fun exigeSeleccion() { assertNotNull(GastosValidation.validar(valido.copy(aplicaA = AplicaGasto.SUBLOTES), listOf(sublote)).sublotes) }
    @Test fun rechazaSubloteInexistente() { assertNotNull(GastosValidation.validar(valido.copy(aplicaA = AplicaGasto.SUBLOTES, asignaciones = mapOf(9L to "12500")), listOf(sublote)).sublotes) }
    @Test fun exigeSumaExacta() { assertNotNull(GastosValidation.validar(valido.copy(aplicaA = AplicaGasto.SUBLOTES, asignaciones = mapOf(7L to "12000")), listOf(sublote)).sublotes) }
    @Test fun convierteDominioYRepartoSinRedondear() {
        val (gasto, reparto) = GastosValidation.convertir(valido.copy(concepto = " Transporte ", aplicaA = AplicaGasto.SUBLOTES, asignaciones = mapOf(7L to "12500")), 42, listOf(sublote))
        assertEquals("Transporte", gasto.concepto); assertEquals(12500L, gasto.monto); assertEquals(listOf(GastoSublote(42, 7, 12500)), reparto); assertTrue(gasto.repartoEsValido(reparto))
    }
    @Test fun admiteRepartoManualMultiple() {
        val form = valido.copy(aplicaA = AplicaGasto.SUBLOTES, asignaciones = mapOf(7L to "5000", 8L to "7500"))
        assertFalse(GastosValidation.validar(form, listOf(sublote, sublote.copy(id = 8))).hayErrores)
    }
    @Test fun rechazaDesbordeEnSumaAsignaciones() {
        val form = valido.copy(monto = Long.MAX_VALUE.toString(), aplicaA = AplicaGasto.SUBLOTES,
            asignaciones = mapOf(7L to Long.MAX_VALUE.toString(), 8L to Long.MAX_VALUE.toString()))
        assertNotNull(GastosValidation.validar(form, listOf(sublote, sublote.copy(id = 8))).sublotes)
    }
    @Test fun totalPendienteIgnoraFiltroYNoDesborda() {
        val (gasto, _) = GastosValidation.convertir(valido.copy(monto = Long.MAX_VALUE.toString(), estado = EstadoPago.PENDIENTE), 1, emptyList())
        val estado = GastosUiState(gastos = listOf(gasto, gasto.copy(id = 2), gasto.copy(id = 3, estadoPago = EstadoPago.PAGADO)), filtro = FiltroGasto.PAGADOS)
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE) * BigInteger.TWO, estado.totalPendiente); assertEquals(1, estado.visibles.size)
    }
    @Test fun guardadoIdempotente() = runBlocking {
        val repository = DemoGastosRepository(); val (gasto, reparto) = GastosValidation.convertir(valido, 1, emptyList())
        repository.guardar(gasto, reparto); repository.guardar(gasto, reparto); assertEquals(1, repository.gastos.value.size)
    }
    @Test fun falloNoPublicaGasto() = runBlocking {
        val repository = DemoGastosRepository(); val (gasto, _) = GastosValidation.convertir(valido.copy(aplicaA = AplicaGasto.SUBLOTES, asignaciones = mapOf(7L to "12500")), 1, listOf(sublote))
        try { repository.guardar(gasto, emptyList()); fail("Debe rechazar un reparto vacío") } catch (_: IllegalArgumentException) { }
        assertTrue(repository.gastos.value.isEmpty())
    }
}
