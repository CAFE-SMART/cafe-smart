package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.purchase.errorPeso
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.limpiarPrecio
import com.cafesmart.app.viewmodel.purchase.pesoEnPantalla
import com.cafesmart.app.viewmodel.purchase.precioEnPantalla
import com.cafesmart.app.viewmodel.purchase.totalEstimado
import com.cafesmart.app.viewmodel.purchase.totalKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// QA de cantidades grandes en Compras: que los numeros no se rompan ni cambien de valor al crecer.
// Los textos largos que salen de aqui son los que la pantalla debe poder mostrar sin desbordarse.
class MontosGrandesComprasTest {

    private fun cafeMaximo() = CafeFormulario("Seco", Calidad.BUENA, peso = "99999", precio = "100000")

    private fun compraConCafes(cantidad: Int): PurchaseViewModel {
        val vm = viewModelDePrueba()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR)
        vm.continuarPaso1()
        repeat(cantidad) { i ->
            if (i > 0) vm.agregarCafe()
            vm.cambiarTipoCafe(i, "Seco")
            vm.cambiarCalidad(i, Calidad.BUENA)
            vm.cambiarPeso(i, "99999")
            vm.cambiarPrecio(i, "100000")
        }
        return vm
    }

    // ---------- Un solo cafe al maximo ----------

    @Test fun unCafeAlMaximo_totalEs9999900000() {
        assertEquals(9_999_900_000.0, totalEstimado(listOf(cafeMaximo())), 0.0)
    }

    @Test fun unCafeAlMaximo_seEscribeConPuntosDeMiles() {
        assertEquals("$ 9.999.900.000", formatoDinero(totalEstimado(listOf(cafeMaximo()))))
    }

    @Test fun unCafeAlMaximo_losKgSeEscribenConPuntos() {
        assertEquals("99.999 kg", formatoKg(totalKg(listOf(cafeMaximo()))))
    }

    @Test fun unCafeAlMaximo_noTieneErroresNiDePesoNiDePrecio() {
        val cafe = cafeMaximo()
        assertNull(errorPeso(cafe))
        assertNull(errorPrecio(cafe.precio))
    }

    // ---------- Varios cafes ----------

    @Test fun diezCafesAlMaximo_totalEs99999000000() {
        val vm = compraConCafes(10)
        assertEquals("$ 99.999.000.000", vm.uiState.value.totalEstimadoTexto)
        assertEquals("999.990 kg", vm.uiState.value.totalKgTexto)
    }

    @Test fun cienCafesAlMaximo_elTextoMideDieciochoCaracteres() {
        val vm = compraConCafes(100)
        assertEquals("$ 999.990.000.000", vm.uiState.value.totalEstimadoTexto)
    }

    @Test fun milCafesAlMaximo_sigueSiendoUnNumeroBienFormado() {
        val texto = formatoDinero(totalEstimado(List(1000) { cafeMaximo() }))
        assertEquals("$ 9.999.900.000.000", texto)
    }

    @Test fun conMuchosCafes_elBotonContinuarSigueHabilitado() {
        assertTrue(compraConCafes(10).uiState.value.cafesListos)
    }

    // ---------- El campo de precio ----------

    @Test fun elPrecioNuncaPasaDeSeisDigitos() {
        assertEquals("999999", limpiarPrecio("99999999999999999999"))
    }

    @Test fun elPrecioMostradoMasLargoEs999punto999() {
        assertEquals("999.999", precioEnPantalla(limpiarPrecio("99999999999999999999")))
    }

    @Test fun unPrecioDe999999dalError() {
        assertNotNull(errorPrecio("999999"))
    }

    @Test fun unPrecioDe100001dalError() {
        assertNotNull(errorPrecio("100001"))
    }

    @Test fun elPrecioDe100000esValido_yElDe99999tambien() {
        assertNull(errorPrecio("100000"))
        assertNull(errorPrecio("99999"))
    }

    @Test fun conPrecioPasadoDelMaximo_noSePuedeContinuar() {
        val vm = viewModelDePrueba()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR)
        vm.continuarPaso1()
        vm.cambiarTipoCafe(0, "Seco"); vm.cambiarCalidad(0, Calidad.BUENA)
        vm.cambiarPeso(0, "10"); vm.cambiarPrecio(0, "999999")
        assertFalse(vm.uiState.value.cafesListos)
    }

    // ---------- El campo de peso ----------

    @Test fun elPesoNuncaPasaDeCincoEnteros() {
        assertEquals("99999", limpiarPeso("99999999999"))
    }

    @Test fun elPesoMaximoConDecimales_son99999coma999() {
        val peso = limpiarPeso("99999,9999999")
        assertEquals("99999.999", peso)
        assertEquals("99.999,999", pesoEnPantalla(peso))
    }

    @Test fun pegarUnNumeroGigante_noRompeLaPantalla() {
        // 40 digitos: no debe lanzar excepcion al darle formato.
        val peso = limpiarPeso("9".repeat(40))
        assertEquals("99.999", pesoEnPantalla(peso))
        assertEquals("999.999", precioEnPantalla(limpiarPrecio("9".repeat(40))))
    }

    @Test fun pegarLetrasYSimbolosEnElPrecio_quedaVacio() {
        assertEquals("", limpiarPrecio("abc$%&"))
        assertEquals("", precioEnPantalla(limpiarPrecio("abc$%&")))
    }

    // ---------- Hallazgos de QA documentados (comportamiento actual) ----------

    // Si se escribe un cero de mas, el campo recorta el ultimo digito y el valor queda 10 veces menor SIN error:
    // "1000000" (un millon) se convierte en "100000" y es valido. Es una decision de diseno que Andres debe confirmar.
    @Test fun hallazgo_unPrecioDeUnMillonSeRecortaA100000SinAvisar() {
        val precio = limpiarPrecio("1000000")
        assertEquals("100000", precio)
        assertNull(errorPrecio(precio))
    }

    @Test fun hallazgo_unPesoDe100000SeRecortaA10000SinAvisar() {
        assertEquals("10000", limpiarPeso("100000"))
    }
}
