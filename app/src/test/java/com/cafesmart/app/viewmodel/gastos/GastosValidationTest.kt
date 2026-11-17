package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.model.*
import com.cafesmart.app.viewmodel.finance.LimitesGasto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

/** Reglas del modelo oficial: importes exactos, fechas, asignaciones e idempotencia demo. */
class GastosValidationTest {
    @Test fun descripcionOpcionalAceptaElMaximoExistenteYRechazaExcesoAlConvertir() {
        assertEquals(200, GastosValidation.DESCRIPCION_MAX_CARACTERES)
        for (texto in listOf("", "d".repeat(200))) {
            assertNull(GastosValidation.validar(valido.copy(descripcion = texto), emptyList()).descripcion)
        }
        val largo = valido.copy(descripcion = "d".repeat(201))
        val errores = GastosValidation.validar(largo, emptyList())
        assertNotNull(errores.descripcion)
        assertTrue(errores.hayErrores)
        assertThrows(IllegalArgumentException::class.java) { GastosValidation.convertir(largo, 1, emptyList()) }
    }

    @Test fun limitaPegadoYSaltosDeLineaSinPartirUnEmoji() {
        assertEquals("d".repeat(200), GastosValidation.limitarDescripcion("d".repeat(500)))
        assertEquals(200, GastosValidation.limitarDescripcion("\n".repeat(300)).length)
        val texto = "a".repeat(199) + "\uD83D\uDE00" + "fin"
        assertEquals("a".repeat(199), GastosValidation.limitarDescripcion(texto))
    }

    @Test fun etiquetasDeSeleccionRespetanSingularYPlural() {
        assertEquals("Seleccionar sublotes", etiquetaSeleccionSublotes(0))
        assertEquals("1 sublote seleccionado", etiquetaSeleccionSublotes(1))
        assertEquals("2 sublotes seleccionados", etiquetaSeleccionSublotes(2))
    }
    private val fecha = Fecha(5, 11, 2026)
    private val valido = GastoFormulario(concepto = "Transporte", monto = "12500", fecha = fecha)
    private val sublote = Sublote(7, 1, pesoInicialKg = 10.0, pesoDisponibleKg = 10.0, costoKg = 1000, fechaIngreso = fecha)

    @Test fun conceptoEnBlanco() { assertNotNull(GastosValidation.validar(valido.copy(concepto = "  "), emptyList()).concepto) }
    @Test fun descripcionOpcional() { assertFalse(GastosValidation.validar(valido, emptyList()).hayErrores) }
    @Test fun ceroYNegativos() { listOf("0", "-1", "-500").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun letrasYExponentes() { listOf("abc", "12a", "1e3", "+10", "", "1 000").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun noTruncaDecimales() { listOf("12.50", "12,50", "12.00", "12,00").forEach { assertNull(GastosValidation.pesos(it)) } }
    @Test fun agrupadoresAmbiguosSeRechazan() { listOf("1.000", "1,000", "$1000").forEach { assertNull(GastosValidation.pesos(it)) } }
    // pesos() solo interpreta el texto; el mínimo y el máximo del gasto los juzga ExpenseRules (ver más abajo).
    @Test fun limiteTecnicoLong() { assertEquals(Long.MAX_VALUE, GastosValidation.pesos(Long.MAX_VALUE.toString())); assertNull(GastosValidation.pesos("9223372036854775808")) }

    // ---- Validaciones de ExpenseRules (concepto, valor, fecha, descripción): se muestran sus mensajes sin reescribirlos ----
    private val hoy = Fecha(17, 11, 2026)
    private fun errores(form: GastoFormulario) = GastosValidation.validar(form, emptyList(), hoy)

    @Test fun valorMinimoYMaximoMuestranElMensajeDeExpenseRules() {
        assertEquals("El valor mínimo es 1.000 pesos", errores(valido.copy(monto = "500")).monto)
        assertNull(errores(valido.copy(monto = "1000")).monto)
        assertNull(errores(valido.copy(monto = "50000000")).monto)
        assertEquals("El valor máximo es 50.000.000 pesos", errores(valido.copy(monto = "50000001")).monto)
        assertEquals("Escribe el valor del gasto", errores(valido.copy(monto = "")).monto)
        assertEquals("El valor debe ser mayor que 0", errores(valido.copy(monto = "0")).monto)
    }

    @Test fun conceptoObligatorioYDeMaximo60() {
        assertEquals("Escribe el concepto del gasto", errores(valido.copy(concepto = "  ")).concepto)
        assertNull(errores(valido.copy(concepto = "c".repeat(60))).concepto)
        assertEquals("El concepto puede tener máximo 60 caracteres", errores(valido.copy(concepto = "c".repeat(61))).concepto)
    }

    @Test fun fechaFuturaOInexistenteSeRechaza() {
        assertEquals("La fecha no puede ser futura", errores(valido.copy(fecha = Fecha(18, 11, 2026))).fecha)
        assertNull(errores(valido.copy(fecha = hoy)).fecha)
        assertEquals("Elige una fecha válida", errores(valido.copy(fecha = Fecha(31, 2, 2026))).fecha)
        assertNull(errores(valido.copy(fecha = Fecha(29, 2, 2024))).fecha)
    }

    @Test fun losLimitesSePuedenCambiarDesdeAjustes() {
        val limites = LimitesGasto(valorMinimoCop = 5_000, valorMaximoCop = 20_000)
        assertEquals("El valor mínimo es 5.000 pesos", GastosValidation.validar(valido.copy(monto = "4999"), emptyList(), hoy, limites).monto)
        assertEquals("El valor máximo es 20.000 pesos", GastosValidation.validar(valido.copy(monto = "20001"), emptyList(), hoy, limites).monto)
    }

    @Test fun soloLosMensajesDeMinimoYMaximoDicenPesos() {
        val sublotes = valido.copy(aplicaA = AplicaGasto.SUBLOTES)
        val mensajes = listOf("", "0", "500", "50000001").mapNotNull { errores(valido.copy(monto = it)).monto } + listOfNotNull(
            GastosValidation.validar(sublotes.copy(asignaciones = mapOf(7L to "")), listOf(sublote), hoy).sublotes,
            GastosValidation.validar(sublotes.copy(asignaciones = mapOf(7L to "100")), listOf(sublote), hoy).sublotes,
            GastosValidation.validar(sublotes, listOf(sublote), hoy).sublotes
        )
        assertEquals(7, mensajes.size)
        mensajes.filter { "pesos" in it }.forEach { assertTrue(it, it.startsWith("El valor mínimo") || it.startsWith("El valor máximo")) }
    }

    // ---- Topes de escritura: lo que no cabe no entra al borrador ----
    @Test fun elValorSoloAceptaDigitosHastaOchoYSeVeConPuntoDeMiles() {
        assertEquals(8, GastosValidation.DIGITOS_MAX_VALOR)
        assertEquals("1500", GastosValidation.limpiarValor("1.500"))
        assertEquals("12", GastosValidation.limpiarValor("12abc"))
        assertEquals("", GastosValidation.limpiarValor("abc"))
        assertEquals("50000000", GastosValidation.limpiarValor("500000001234"))
        assertEquals("1.500", com.cafesmart.app.viewmodel.purchase.precioEnPantalla(GastosValidation.limpiarValor("1500")))
    }

    @Test fun elConceptoNoPasaDe60NiPartePorLaMitadUnEmoji() {
        assertEquals("c".repeat(60), GastosValidation.limitarConcepto("c".repeat(90)))
        assertEquals("a".repeat(59), GastosValidation.limitarConcepto("a".repeat(59) + "😀" + "fin"))
    }

    @Test fun normalizarAplicaTodosLosTopesAlBorrador() {
        val sucio = valido.copy(concepto = "c".repeat(70), descripcion = "d".repeat(250), monto = "12abc",
            asignaciones = mapOf(7L to "5.0x00", 8L to "9".repeat(12)))
        val limpio = GastosValidation.normalizar(sucio)
        assertEquals("c".repeat(60), limpio.concepto)
        assertEquals("d".repeat(200), limpio.descripcion)
        assertEquals("12", limpio.monto)
        assertEquals(mapOf(7L to "5000", 8L to "99999999"), limpio.asignaciones)
    }

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
        // El total se calcula con BigInteger aunque el modelo reciba un monto enorme (aquí armado a mano:
        // desde el formulario ya no pasa del máximo de ExpenseRules).
        val gasto = Gasto(1, "Transporte", "", TipoGasto.TRANSPORTE, Long.MAX_VALUE, fecha, EstadoPago.PENDIENTE, AplicaGasto.GENERAL)
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
