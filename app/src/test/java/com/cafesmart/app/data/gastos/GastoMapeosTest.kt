package com.cafesmart.app.data.gastos

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.local.GastoEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Paso entre las filas de Room y el modelo: no necesita Room ni emulador. */
class GastoMapeosTest {

    private val fecha = Fecha(dia = 9, mes = 11, anio = 2026)
    private fun gasto(
        tipo: TipoGasto = TipoGasto.TRANSPORTE,
        estado: EstadoPago = EstadoPago.PAGADO,
        aplicaA: AplicaGasto = AplicaGasto.GENERAL,
        secadoId: Long? = null,
    ) = Gasto(5, "Pago de jornaleros", "Cosecha de octubre", tipo, 125_000, fecha, estado, aplicaA, secadoId)

    @Test
    fun unGastoVuelveIgualDespuesDeIrYVenirDeRoom() {
        for (tipo in TipoGasto.entries) for (estado in EstadoPago.entries) {
            val original = gasto(tipo = tipo, estado = estado)
            assertEquals(original, original.aEntidad().aModelo())
        }
    }

    @Test
    fun losEnumsSeGuardanComoSuNombreYLaFechaEnMilisegundosUtc() {
        val fila = gasto(tipo = TipoGasto.DESCARGUE, estado = EstadoPago.PENDIENTE, aplicaA = AplicaGasto.SUBLOTES).aEntidad()
        assertEquals("DESCARGUE", fila.tipo)
        assertEquals("PENDIENTE", fila.estadoPago)
        assertEquals("SUBLOTES", fila.aplicaA)
        assertEquals(fechaAMilis(fecha), fila.fecha)
        assertEquals(125_000L, fila.monto)
    }

    @Test
    fun unGastoDeSecadoConservaSuSecadoId() {
        val original = gasto(tipo = TipoGasto.SECADO, aplicaA = AplicaGasto.SECADO, secadoId = 9)
        val fila = original.aEntidad()
        assertEquals(9L, fila.secadoId)
        assertEquals(original, fila.aModelo())
    }

    @Test
    fun unGastoGeneralNoTieneSecadoId() {
        assertNull(gasto().aEntidad().secadoId)
    }

    @Test
    fun unaFilaConUnTipoInventadoNoSeMuestraComoSiEstuvieraBien() {
        val fila = gasto().aEntidad().copy(tipo = "INVENTADO")
        assertThrows(IllegalArgumentException::class.java) { fila.aModelo() }
    }

    @Test
    fun unaFilaQueRompeLasReglasDelModeloLanzaError() {
        val montoCero = GastoEntity(1, "Transporte", "", "TRANSPORTE", 0, fechaAMilis(fecha), "PAGADO", "GENERAL")
        assertThrows(IllegalArgumentException::class.java) { montoCero.aModelo() }
        // secadoId con un gasto que no es de secado contradice la regla del modelo.
        val secadoSuelto = montoCero.copy(monto = 1000, secadoId = 3)
        assertThrows(IllegalArgumentException::class.java) { secadoSuelto.aModelo() }
    }

    // ---- Gasto -> GastoRegistrado (lo que lee el Resumen y el Historial financiero) ----

    @Test
    fun elResumenRecibeElMontoCompletoElConceptoYLaFecha() {
        assertEquals(
            GastoRegistrado(fecha, 125_000, "Pago de jornaleros", "Gasto general"),
            gasto().aGastoRegistrado(),
        )
    }

    @Test
    fun elDetalleDiceAQueSeAplicaElGasto() {
        assertEquals("Gasto general", gasto(aplicaA = AplicaGasto.GENERAL).aGastoRegistrado().detalle)
        assertEquals("Secado", gasto(tipo = TipoGasto.SECADO, aplicaA = AplicaGasto.SECADO, secadoId = 2).aGastoRegistrado().detalle)
        val deSublotes = gasto(aplicaA = AplicaGasto.SUBLOTES)
        assertEquals("Sublote 7", deSublotes.aGastoRegistrado(listOf(7)).detalle)
        assertEquals("2 sublotes", deSublotes.aGastoRegistrado(listOf(7, 8)).detalle)
        assertEquals("Sublotes", deSublotes.aGastoRegistrado(emptyList()).detalle)
    }

    @Test
    fun unGastoRepartidoCuentaCompletoUnaSolaVezEnElResumen() {
        // El monto es el del gasto (125.000), no lo que le toca a cada sublote.
        val registrado = gasto(aplicaA = AplicaGasto.SUBLOTES).aGastoRegistrado(listOf(1, 2, 3))
        assertEquals(125_000L, registrado.montoPesos)
    }

    @Test
    fun unSubloteConTodosSusDatosPasaAlModelo() {
        val fila = SubloteEntity(
            id = 3, loteId = 2, compraId = 7, pesoInicialKg = 120.5, pesoDisponibleKg = 80.25, costoKg = 14_000,
            fechaIngreso = fechaAMilis(fecha), humedadPorcentaje = 11.5, factorRendimiento = 95,
        )
        assertEquals(
            Sublote(3, 2, 7, null, 120.5, 80.25, 14_000, fecha, 11.5, 95),
            fila.aModelo(),
        )
    }

    @Test
    fun unSubloteSinDatosOpcionalesTieneNulos() {
        val sublote = SubloteEntity(
            id = 1, loteId = 1, pesoInicialKg = 10.0, pesoDisponibleKg = 10.0, costoKg = 1_000, fechaIngreso = fechaAMilis(fecha),
        ).aModelo()
        assertNull(sublote.compraId)
        assertNull(sublote.humedadPorcentaje)
        assertNull(sublote.factorRendimiento)
    }

    @Test
    fun unSubloteConMasKilosDisponiblesQueInicialesLanzaError() {
        val fila = SubloteEntity(
            id = 1, loteId = 1, pesoInicialKg = 10.0, pesoDisponibleKg = 11.0, costoKg = 1_000, fechaIngreso = fechaAMilis(fecha),
        )
        assertThrows(IllegalArgumentException::class.java) { fila.aModelo() }
    }
}
