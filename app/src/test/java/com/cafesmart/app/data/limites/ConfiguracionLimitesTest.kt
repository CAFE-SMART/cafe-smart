package com.cafesmart.app.data.limites

import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.viewmodel.finance.LimitesGasto
import com.cafesmart.app.viewmodel.gastos.GastosValidation
import com.cafesmart.app.viewmodel.gastos.GastoFormulario
import com.cafesmart.app.viewmodel.limites.LimitesDeEntrada
import com.cafesmart.app.viewmodel.limites.LimitesVigentes
import com.cafesmart.app.viewmodel.limites.ayudaPrecioKg
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PESO
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PRECIO
import com.cafesmart.app.viewmodel.purchase.errorPeso
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.precioValido
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ajustes · Límites de entrada (parte 2 de la tarjeta): validar, guardar, recuperar al reabrir, restablecer, y que
// Compras y Gastos consulten los límites vigentes. La pantalla todavía no existe (falta el diseño aprobado).
class ConfiguracionLimitesTest {

    @After fun volverALosPredeterminados() = LimitesVigentes.aplicar(LimitesDeEntrada.PREDETERMINADOS)

    private val predeterminados = BorradorLimites.de(LimitesDeEntrada.PREDETERMINADOS)

    private fun errores(b: BorradorLimites) = (validarLimites(b) as ValidacionLimites.ConErrores).errores

    // ---------------- Validación ----------------

    @Test fun losPredeterminadosSonValidos() {
        val r = validarLimites(predeterminados)
        assertEquals(ValidacionLimites.Valida(LimitesDeEntrada.PREDETERMINADOS), r)
    }

    @Test fun noAceptaNegativosVaciosNiCero() {
        val e = errores(predeterminados.copy(pesoMin = "-5", precioMax = "", gastoMin = "0", gastoMax = "12a"))
        assertEquals(ERROR_LIMITE_NEGATIVO, e[CampoLimite.PESO_MIN])
        assertEquals(ERROR_LIMITE_VACIO, e[CampoLimite.PRECIO_MAX])
        assertEquals(ERROR_LIMITE_CERO, e[CampoLimite.GASTO_MIN])
        assertEquals(ERROR_LIMITE_NO_NUMERO, e[CampoLimite.GASTO_MAX])
    }

    @Test fun elMinimoNoPuedeSuperarAlMaximo() {
        val e = errores(predeterminados.copy(pesoMin = "500", pesoMax = "100", precioMin = "200000", gastoMin = "60000000"))
        assertEquals(ERROR_LIMITE_MINIMO_MAYOR, e[CampoLimite.PESO_MIN])
        assertEquals(ERROR_LIMITE_MINIMO_MAYOR, e[CampoLimite.PRECIO_MIN])
        assertEquals(ERROR_LIMITE_MINIMO_MAYOR, e[CampoLimite.GASTO_MIN])
    }

    @Test fun minimoIgualAlMaximoEsValidoYAceptaPuntosDeMiles() {
        val r = validarLimites(predeterminados.copy(precioMin = "5.000", precioMax = "5.000"))
        assertEquals(5_000, (r as ValidacionLimites.Valida).limites.precioKgMinimoCop)
        assertEquals(5_000, r.limites.precioKgMaximoCop)
    }

    // ---------------- Guardar, recuperar y restablecer ----------------

    @Test fun guardaYSeRecuperaAlVolverAAbrir() {
        val telefono = AlmacenLimitesMemoria()
        val r = ConfiguracionLimites(telefono).guardar(predeterminados.copy(precioMin = "2000", gastoMax = "10000000"))
        assertTrue(r is ValidacionLimites.Valida)
        LimitesVigentes.aplicar(LimitesDeEntrada.PREDETERMINADOS) // "se cierra la app"
        val alAbrir = ConfiguracionLimites(telefono).cargar()     // "se vuelve a abrir"
        assertEquals(2_000, alAbrir.precioKgMinimoCop)
        assertEquals(10_000_000L, alAbrir.gastoMaximoCop)
        assertEquals(alAbrir, LimitesVigentes.actuales)
    }

    @Test fun conErroresNoGuardaNiCambiaLosVigentes() {
        val telefono = AlmacenLimitesMemoria()
        ConfiguracionLimites(telefono).guardar(predeterminados.copy(pesoMin = "-1"))
        assertNull(telefono.leer())
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, LimitesVigentes.actuales)
    }

    @Test fun sinNadaGuardadoUsaLosPredeterminados() =
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, ConfiguracionLimites(AlmacenLimitesMemoria()).cargar())

    @Test fun restablecerVuelveATodosLosPredeterminadosYBorraLoGuardado() {
        val telefono = AlmacenLimitesMemoria()
        val config = ConfiguracionLimites(telefono)
        config.guardar(predeterminados.copy(pesoMax = "500", precioMax = "90000", gastoMin = "5000"))
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, config.restablecer())
        assertNull(telefono.leer())
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, LimitesVigentes.actuales)
        assertEquals(LimitesDeEntrada.PREDETERMINADOS, ConfiguracionLimites(telefono).cargar())
    }

    // ---------------- Compras, Ventas y Gastos consultan los vigentes ----------------

    @Test fun comprasUsaLosLimitesVigentes() {
        val config = ConfiguracionLimites(AlmacenLimitesMemoria())
        assertEquals(true, precioValido("1500"))
        config.guardar(predeterminados.copy(precioMin = "2000", pesoMax = "500"))
        assertEquals(false, precioValido("1500"))
        assertEquals(MENSAJE_PRECIO, errorPrecio("1500"))
        assertTrue(MENSAJE_PRECIO.contains("$2.000") && MENSAJE_PRECIO.contains("pesos"))
        assertEquals(MENSAJE_PESO, errorPeso(CafeFormulario(peso = "600")))
        assertEquals("Escribe un peso entre 1 y 500 kg.", MENSAJE_PESO)
        assertEquals("Entre $ 2.000 y $ 100.000.", ayudaPrecioKg().replace(' ', ' '))
        config.restablecer()
        assertEquals(true, precioValido("1500"))
        assertNull(errorPeso(CafeFormulario(peso = "600")))
    }

    @Test fun gastosUsaLosLimitesVigentes() {
        val config = ConfiguracionLimites(AlmacenLimitesMemoria())
        config.guardar(predeterminados.copy(gastoMin = "5000", gastoMax = "900000"))
        assertEquals(5_000L, LimitesGasto().valorMinimoCop)
        assertEquals(900_000L, LimitesGasto().valorMaximoCop)
        val gasto = GastoFormulario(concepto = "Transporte", monto = "4000")
        assertTrue(GastosValidation.validar(gasto, emptyList()).monto != null)
        assertNull(GastosValidation.validar(gasto.copy(monto = "5000"), emptyList()).monto)
        assertEquals(6, GastosValidation.DIGITOS_MAX_VALOR)
        config.restablecer()
        assertEquals(8, GastosValidation.DIGITOS_MAX_VALOR)
    }
}
