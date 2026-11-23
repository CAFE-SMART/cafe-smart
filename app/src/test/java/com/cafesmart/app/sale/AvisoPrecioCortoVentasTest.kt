package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.viewmodel.sale.MENSAJE_REVISA_CAMPOS
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

// Un precio de 1 a 3 digitos no sale en rojo mientras se escribe, pero "Continuar" ya esta deshabilitado.
// Entonces el aviso no puede decir solo "revisa los campos": debe decir de que calidad es el precio.
// Sublote de ejemplo 5: calidad buena, 200 kg (la cantidad y el minimo de kg ya estan bien).
class AvisoPrecioCortoVentasTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    @Test
    fun precioCortoSinMarcar_elAvisoNombraLaCalidad() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        vm.cambiarPrecio(Calidad.BUENA, "500")
        assertEquals("Revisa el precio por kg de la calidad buena.", vm.uiState.value.mensajePendientePaso2)
    }

    @Test
    fun precioCortoDeUnaCalidad_noImportaQueOtraCalidadEstePendiente() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5, 8)
        vm.cambiarPrecio(Calidad.REGULAR, "50")
        assertEquals("Revisa el precio por kg de la calidad regular.", vm.uiState.value.mensajePendientePaso2)
    }

    @Test
    fun precioVacio_elCampoYaEstaEnRojo_elAvisoSigueSiendoGenerico() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.uiState.value.mensajePendientePaso2)
    }

    @Test
    fun precioMayorAlMaximo_elCampoYaEstaEnRojo_elAvisoSigueSiendoGenerico() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        vm.cambiarPrecio(Calidad.BUENA, "100001")
        assertEquals(MENSAJE_REVISA_CAMPOS, vm.uiState.value.mensajePendientePaso2)
    }

    @Test
    fun precioValido_noHayAviso() {
        val vm = viewModelDeVentasDePrueba()
        vm.elegir(5)
        vm.cambiarPrecio(Calidad.BUENA, "5000")
        assertNull(vm.uiState.value.mensajePendientePaso2)
    }
}
