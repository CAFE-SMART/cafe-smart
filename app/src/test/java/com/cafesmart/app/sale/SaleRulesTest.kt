package com.cafesmart.app.sale

import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.precioValido
import com.cafesmart.app.viewmodel.sale.cantidadDesdeTexto
import com.cafesmart.app.viewmodel.sale.cantidadValida
import com.cafesmart.app.viewmodel.sale.errorCantidad
import com.cafesmart.app.viewmodel.sale.mensajeCantidad
import com.cafesmart.app.viewmodel.sale.subtotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleRulesTest {

    // Sublote 5 del Figma: 200 kg disponibles
    private val disponibleKg = 200.0

    // ---------- Cantidad a vender ----------

    @Test
    fun cantidad_validaDentroDeLoDisponible() {
        assertTrue(cantidadValida(150.0, disponibleKg))
        assertTrue(cantidadValida(0.5, disponibleKg))
    }

    @Test
    fun cantidad_limites_todoLoDisponibleSiPeroUnPocoMasNo() {
        assertTrue(cantidadValida(200.0, disponibleKg))
        assertFalse(cantidadValida(200.5, disponibleKg))
        assertFalse(cantidadValida(0.0, disponibleKg))
    }

    @Test
    fun cantidad_invalidaSiEsNegativaOVacia() {
        assertFalse(cantidadValida(-10.0, disponibleKg))
        assertFalse(cantidadValida(null, disponibleKg))
    }

    @Test
    fun cantidad_mensajeIndicaLoDisponible() {
        assertEquals("Debe ser menor o igual a 200 kg.", mensajeCantidad(200.0))
        assertEquals("Debe ser menor o igual a 80,5 kg.", mensajeCantidad(80.5))
    }

    @Test
    fun cantidad_errorSoloAlPasarseDeLoDisponible() {
        assertNull(errorCantidad(150.0, disponibleKg))
        assertNull(errorCantidad(200.0, disponibleKg))
        assertNull(errorCantidad(0.0, disponibleKg))
        assertNull(errorCantidad(null, disponibleKg))
        assertEquals("Debe ser menor o igual a 200 kg.", errorCantidad(200.5, disponibleKg))
    }

    @Test
    fun cantidadDesdeTexto_aceptaComaPuntoYRechazaLoQueNoEsNumero() {
        assertEquals(25.5, cantidadDesdeTexto("25,5")!!, 0.0)
        assertEquals(25.5, cantidadDesdeTexto("25.5")!!, 0.0)
        assertEquals(150.0, cantidadDesdeTexto("150")!!, 0.0)
        assertNull(cantidadDesdeTexto(""))
        assertNull(cantidadDesdeTexto("."))
        assertNull(cantidadDesdeTexto("abc"))
    }

    // ---------- Precio por kg (mismas reglas de Compras: de $1.000 a $100.000) ----------

    @Test
    fun precio_limitesDelRango() {
        assertTrue(precioValido("1000"))
        assertTrue(precioValido("100000"))
        assertFalse(precioValido("999"))
        assertFalse(precioValido("100001"))
    }

    @Test
    fun precio_vacioEsInvalidoYElCampoConPuntosSeEntiende() {
        assertFalse(precioValido(""))
        assertTrue(precioValido("20.000"))
    }

    @Test
    fun precio_errorSoloCuandoSePasaDelMaximo() {
        assertNull(errorPrecio("20000"))
        assertNotNull(errorPrecio("100001"))
    }

    @Test
    fun precio_conMenosDe4DigitosNoMarcaErrorPeroTampocoEsValido() {
        // Puede estar escribiendo ("999" podria ser el inicio de "9990"), asi que no marca error,
        // pero "Continuar" sigue deshabilitado porque precioValido es falso.
        assertNull(errorPrecio("999"))
        assertFalse(precioValido("999"))
    }

    // ---------- Total ----------

    @Test
    fun subtotal_cantidadPorPrecio() {
        // Figma, Paso 3: 150 kg x $ 20.000 = $ 3.000.000
        assertEquals(3_000_000L, subtotal(150.0, 20_000.0))
        assertEquals(357_000L, subtotal(25.5, 14_000.0))
    }

    @Test
    fun subtotal_redondeaAlPesoMasCercano() {
        // 0,5 kg x $1.001 = 500,5 y el .5 sube
        assertEquals(501L, subtotal(0.5, 1_001.0))
    }

    @Test
    fun subtotal_sumaDeLosSublotesDelFigma() {
        // Figma, Paso 2 (todo): sublotes 5, 6, 7 y 8 con el precio de su calidad = $ 9.614.000
        val ventas = listOf(
            200.0 to 20_000.0,
            180.0 to 18_000.0,
            100.0 to 18_000.0,
            41.0 to 14_000.0
        )
        assertEquals(9_614_000L, ventas.sumOf { (kg, precio) -> subtotal(kg, precio) })
    }
}
