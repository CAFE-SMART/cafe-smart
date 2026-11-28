package com.cafesmart.app.viewmodel.limites

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.viewmodel.finance.validarGasto
import com.cafesmart.app.viewmodel.gastos.GastosValidation
import com.cafesmart.app.viewmodel.purchase.AYUDA_PESO
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PESO
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PRECIO
import com.cafesmart.app.viewmodel.purchase.PESO_MAX_KG
import com.cafesmart.app.viewmodel.purchase.PESO_MIN_KG
import com.cafesmart.app.viewmodel.purchase.errorPeso
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.formatoMiles
import com.cafesmart.app.viewmodel.purchase.pesoValido
import com.cafesmart.app.viewmodel.purchase.precioValido
import com.cafesmart.app.viewmodel.sale.ayudaPrecio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Límites unificados (ciclo 3, tarea de Amelia, parte 1): una sola fuente para Compras, Gastos y Ventas, con los
// mismos valores y mensajes aprobados que había antes.
class LimitesDeEntradaTest {

    private val hoy = Fecha(23, 11, 2026)
    private fun cafe(peso: String, precio: String = "14000") = CafeFormulario("Verde", Calidad.BUENA, peso = peso, precio = precio)
    private fun errorValorGasto(valor: Long) = validarGasto("Flete", TipoGasto.TRANSPORTE, valor, hoy, hoy = hoy).valor

    // ── Valores aprobados ──────────────────────────────────

    @Test fun losValoresSonLosAprobados() {
        assertEquals(1, PESO_MINIMO_KG)
        assertEquals(99_999, PESO_MAXIMO_KG)
        assertEquals(1_000, PRECIO_KG_MINIMO_COP)
        assertEquals(100_000, PRECIO_KG_MAXIMO_COP)
        assertEquals(1_000L, GASTO_MINIMO_COP)
        assertEquals(50_000_000L, GASTO_MAXIMO_COP)
    }

    @Test fun losNombresAntiguosApuntanALaMismaFuente() {
        assertEquals(PESO_MINIMO_KG, PESO_MIN_KG)
        assertEquals(PESO_MAXIMO_KG, PESO_MAX_KG)
        // Los de dinero ya no tienen alias dispersos: Gastos usa los vigentes (por defecto, los predeterminados).
        assertEquals(GASTO_MINIMO_COP, com.cafesmart.app.viewmodel.finance.LimitesGasto().valorMinimoCop)
        assertEquals(GASTO_MAXIMO_COP, com.cafesmart.app.viewmodel.finance.LimitesGasto().valorMaximoCop)
        // El campo de monto de Gastos admite exactamente los dígitos del máximo (50.000.000 son 8).
        assertEquals(GASTO_MAXIMO_COP.toString().length, GastosValidation.DIGITOS_MAX_VALOR)
    }

    @Test fun losPredeterminadosSonLosValoresAprobados() {
        val p = LimitesDeEntrada.PREDETERMINADOS
        assertEquals(LimitesDeEntrada(1, 99_999, 1_000, 100_000, 1_000L, 50_000_000L), p)
    }

    // ── Mensajes: siguen siendo los aprobados y coinciden con los valores ──────────

    @Test fun losMensajesDeComprasCoincidenConLosLimites() {
        assertEquals("Escribe un peso entre $PESO_MINIMO_KG y ${formatoMiles(PESO_MAXIMO_KG.toLong())} kg.", MENSAJE_PESO)
        assertEquals("Entre $PESO_MINIMO_KG y ${formatoMiles(PESO_MAXIMO_KG.toLong())} kg. Decimales con coma: 12,5", AYUDA_PESO)
        assertEquals("Escribe un valor entre $${formatoMiles(PRECIO_KG_MINIMO_COP.toLong())} y " +
            "$${formatoMiles(PRECIO_KG_MAXIMO_COP.toLong())} pesos colombianos.", MENSAJE_PRECIO)
        assertEquals("Entre $ 1.000 y $ 100.000.", ayudaPrecioKg())
        assertTrue(ayudaPrecio(18_000).startsWith("Entre $ 1.000 y $ 100.000. "))
    }

    @Test fun soloLosMensajesDeMinimoYMaximoDicenPesos() {
        assertEquals("El valor mínimo es 1.000 pesos", errorValorGasto(999))
        assertEquals("El valor máximo es 50.000.000 pesos", errorValorGasto(50_000_001))
        assertEquals("El valor mínimo es 1.000 pesos", mensajeValorMinimo(GASTO_MINIMO_COP))
        assertEquals("El valor máximo es 50.000.000 pesos", mensajeValorMaximo(GASTO_MAXIMO_COP))
        // Las ayudas (no son errores de mínimo o máximo) no dicen "pesos".
        assertFalse(ayudaPrecioKg().contains("pesos"))
        assertFalse(AYUDA_PESO.contains("pesos"))
        assertFalse(validarGasto("", TipoGasto.TRANSPORTE, null, hoy, hoy = hoy).valor!!.contains("pesos"))
    }

    // ── Bordes y casos inválidos ──────────────────────────────

    @Test fun pesoEnLosBordes() {
        assertFalse(pesoValido(cafe("0")))
        assertTrue(pesoValido(cafe("1")))
        assertTrue(pesoValido(cafe("99999")))
        assertEquals(MENSAJE_PESO, errorPeso(cafe("0.5"))) // el peso se guarda con punto decimal (limpiarPeso)
        assertNull(errorPeso(cafe("99999")))
        assertTrue(LimitesDeEntrada.PREDETERMINADOS.pesoEnRango(1.0))
        assertFalse(LimitesDeEntrada.PREDETERMINADOS.pesoEnRango(99_999.001))
    }

    @Test fun precioEnLosBordes() {
        assertFalse(precioValido("999"))
        assertTrue(precioValido("1000"))
        assertTrue(precioValido("100000"))
        assertFalse(precioValido("100001"))
        // Con 3 dígitos aún se está escribiendo: no marca error. Con 4 o más, sí.
        assertNull(errorPrecio("999"))
        assertEquals(MENSAJE_PRECIO, errorPrecio("0999"))
        assertEquals(MENSAJE_PRECIO, errorPrecio("100001"))
    }

    @Test fun gastoEnLosBordes() {
        assertNull(errorValorGasto(1_000))
        assertNull(errorValorGasto(50_000_000))
        assertEquals("El valor debe ser mayor que 0", errorValorGasto(0))
        assertTrue(LimitesDeEntrada.PREDETERMINADOS.gastoEnRango(1_000))
        assertFalse(LimitesDeEntrada.PREDETERMINADOS.gastoEnRango(999))
    }

    @Test fun rechazaLimitesImposibles() {
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(pesoMinimoKg = 0) }
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(pesoMinimoKg = 10, pesoMaximoKg = 9) }
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(precioKgMinimoCop = 0) }
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(precioKgMinimoCop = 5_000, precioKgMaximoCop = 4_000) }
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(gastoMinimoCop = 0) }
        assertThrows(IllegalArgumentException::class.java) { LimitesDeEntrada(gastoMinimoCop = 2_000, gastoMaximoCop = 1_000) }
    }

    @Test fun ventaMinimaYDigitosSalenDeLaMismaFuente() {
        assertEquals(5.0, VENTA_MINIMA_KG, 0.0)
        assertEquals(VENTA_MINIMA_KG, com.cafesmart.app.viewmodel.sale.VENTA_MINIMA_KG, 0.0)
        assertEquals(5, DIGITOS_PESO)
        assertEquals(6, DIGITOS_PRECIO_KG)
        assertEquals("100000", com.cafesmart.app.viewmodel.purchase.limpiarPrecio("1.000.000"))
        assertEquals("12345", com.cafesmart.app.viewmodel.purchase.limpiarPeso("123456")) // los 5 primeros dígitos, como antes
    }

    @Test fun losMensajesSiguenSiendoLosAprobadosExactos() {
        assertEquals("Escribe un peso entre 1 y 99.999 kg.", MENSAJE_PESO)
        assertEquals("Escribe un valor entre $1.000 y $100.000 pesos colombianos.", MENSAJE_PRECIO)
        assertEquals("Entre 1 y 99.999 kg. Decimales con coma: 12,5", AYUDA_PESO)
    }

}
