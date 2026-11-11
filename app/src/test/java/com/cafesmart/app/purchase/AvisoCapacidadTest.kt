package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.purchase.NivelAvisoCapacidad
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.purchase.avisoCapacidadCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Avisos de capacidad de bodega en Compras: informativos, nunca bloquean la compra.
class AvisoCapacidadTest {

    // ── Regla ──────────────────────────────────────────────

    @Test fun conMenosDeNoventaPorCientoNoHayAviso() {
        // 2.000 + 600 = 2.600 de 3.000 = 86,7 %
        assertNull(avisoCapacidadCompra(3000.0, 2000.0, 600.0))
    }

    @Test fun enElLimiteExactoDeNoventaPorCientoSiAvisa() {
        // 2.400 + 300 = 2.700 de 3.000 = 90 % justo.
        val a = avisoCapacidadCompra(3000.0, 2400.0, 300.0)
        assertNotNull(a)
        assertEquals(NivelAvisoCapacidad.CASI_LLENA, a!!.nivel)
        // Justo por debajo (89,9 %) no avisa.
        assertNull(avisoCapacidadCompra(3000.0, 2400.0, 299.0))
    }

    @Test fun casiLlenaDiceDeQuePorcientoAQuePorcientoYCuantoQueda() {
        val a = avisoCapacidadCompra(3000.0, 2550.0, 270.0)!!   // 85 % -> 94 %
        assertEquals(NivelAvisoCapacidad.CASI_LLENA, a.nivel)
        assertEquals(85, a.porcentajeAntes)
        assertEquals(94, a.porcentajeDespues)
        assertEquals("Tu bodega quedará casi llena", a.titulo)
        assertEquals("Pasa de 85 % a 94 %. Quedan 180 kg libres de 3.000 kg.", a.mensajePaso2)
        assertTrue(a.mensajePaso3.endsWith("Es solo un aviso: igual puedes registrar la compra."))
    }

    @Test fun cienPorCientoJustoSigueSiendoCasiLlenaYMasDeCienEsSuperada() {
        assertEquals(NivelAvisoCapacidad.CASI_LLENA, avisoCapacidadCompra(3000.0, 2900.0, 100.0)!!.nivel)
        val a = avisoCapacidadCompra(3000.0, 3125.0, 25.0)!!    // 3.150 de 3.000 = 105 %
        assertEquals(NivelAvisoCapacidad.SUPERADA, a.nivel)
        assertEquals(105, a.porcentajeDespues)
        assertEquals(150.0, a.kgExceso, 0.0)
        assertEquals("Esta compra supera tu capacidad", a.titulo)
        assertEquals("Te pasas 150 kg de los 3.000 kg que caben.", a.mensajePaso2)
    }

    @Test fun enCompraRegistradaAconsejaVenderCafeYNoConfigurarLaCapacidad() {
        val a = avisoCapacidadCompra(3000.0, 3125.0, 25.0)!!
        assertEquals("Tu bodega superó su capacidad", a.tituloRegistrada)
        assertEquals("Nuevo total: 3.150 kg de 3.000 kg. Te conviene vender café para liberar espacio.", a.mensajeRegistrada)
        assertFalse(a.mensajeRegistrada.contains("Ajustes"))
        assertFalse(a.mensajeRegistrada.contains("capacidad en"))
    }

    @Test fun sinCapacidadOSinKilosNoHayAvisoDeEsteTipo() {
        assertNull(avisoCapacidadCompra(0.0, 500.0, 100.0))
        assertNull(avisoCapacidadCompra(3000.0, 500.0, 0.0))
    }

    // ── Flujo de compra ────────────────────────────────────

    private fun compraDe(kg: String, vm: PurchaseViewModel = PurchaseViewModel()): PurchaseViewModel {
        vm.cambiarTipoCafe(0, "Verde"); vm.cambiarCalidad(0, Calidad.BUENA)
        vm.cambiarPeso(0, kg); vm.cambiarPrecio(0, "14000")
        return vm
    }

    @Test fun conCapacidadEnCeroElPaso1AvisaQueFaltaConfigurarYNoBloquea() {
        val vm = PurchaseViewModel()
        assertTrue(vm.uiState.value.avisoSinConfigurar)
        vm.actualizarBodega(capacidadKg = 3000.0, kgOcupados = 834.0)
        assertFalse(vm.uiState.value.avisoSinConfigurar)
    }

    @Test fun elAvisoSeActualizaAlEscribirElPeso() {
        val vm = PurchaseViewModel()
        vm.actualizarBodega(3000.0, 834.0)
        compraDe("25", vm)
        assertNull(vm.uiState.value.avisoCapacidad)           // 859 kg: 29 %
        vm.cambiarPeso(0, "2300")                              // 3.134 kg: 104 %
        assertEquals(NivelAvisoCapacidad.SUPERADA, vm.uiState.value.avisoCapacidad!!.nivel)
        // El aviso nunca bloquea: la compra sigue lista para confirmarse.
        assertTrue(vm.uiState.value.cafesListos)
    }

    @Test fun confirmarConAvisoRegistraLaCompraYConservaElAviso() {
        val vm = PurchaseViewModel()
        vm.actualizarBodega(3000.0, 834.0)
        compraDe("2300", vm)
        vm.confirmarCompra()
        val estado = vm.uiState.value
        assertTrue(estado.compraRegistrada)
        assertEquals(NivelAvisoCapacidad.SUPERADA, estado.avisoAlRegistrar!!.nivel)
        // Si la ocupacion cambia despues (la compra ya se guardo), el aviso de esta compra no cambia.
        vm.actualizarBodega(3000.0, 3134.0)
        assertEquals(3134.0, estado.avisoAlRegistrar!!.nuevoTotalKg, 0.0)
        assertEquals(834.0, vm.uiState.value.kgOcupados, 0.0)
    }

    @Test fun reiniciarConservaLaBodegaPeroBorraElAviso() {
        val vm = PurchaseViewModel()
        vm.actualizarBodega(3000.0, 834.0)
        compraDe("2300", vm)
        vm.confirmarCompra()
        vm.reiniciarCompra()
        assertNull(vm.uiState.value.avisoAlRegistrar)
        assertEquals(3000.0, vm.uiState.value.capacidadKg, 0.0)
        assertFalse(vm.uiState.value.compraRegistrada)
    }
}
