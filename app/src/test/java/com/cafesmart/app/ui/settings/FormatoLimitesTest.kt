package com.cafesmart.app.ui.settings

import com.cafesmart.app.data.limites.BorradorLimites
import com.cafesmart.app.data.limites.ERROR_LIMITE_CERO
import com.cafesmart.app.data.limites.ERROR_LIMITE_NEGATIVO
import com.cafesmart.app.data.limites.CampoLimite
import com.cafesmart.app.data.limites.ValidacionLimites
import com.cafesmart.app.data.limites.validarLimites
import com.cafesmart.app.viewmodel.limites.LimitesDeEntrada
import org.junit.Assert.assertEquals
import org.junit.Test

// Límites de entrada: se ven con puntos de miles, pero el borrador (lo que se valida y se guarda) sigue en dígitos.
class FormatoLimitesTest {
    @Test fun seMuestraConPuntosDeMiles() {
        assertEquals("99.999", mostrarLimite("99999"))
        assertEquals("50.000.000", mostrarLimite("50000000"))
        assertEquals("1", mostrarLimite("1"))
        assertEquals("", mostrarLimite(""))
        assertEquals("-1.500", mostrarLimite("-1500"))
    }

    @Test fun alEditarSoloQuedanDigitos() {
        assertEquals("1500", limpiarLimite("1.500"))      // el usuario edita sobre el texto con puntos
        assertEquals("12500", limpiarLimite("1.2500"))     // al agregar un dígito el punto se recoloca
        assertEquals("7", limpiarLimite("007"))
        assertEquals("0", limpiarLimite("0"))
        assertEquals("", limpiarLimite("abc"))
        assertEquals("-1", limpiarLimite("-1"))           // el signo se conserva para avisar "negativo"
    }

    @Test fun ida_y_vuelta_noCambiaElValor() {
        for (v in listOf("1", "999", "1000", "99999", "100000", "50000000"))
            assertEquals(v, limpiarLimite(mostrarLimite(v)))
    }

    @Test fun lasValidacionesSiguenIgual() {
        val base = BorradorLimites.de(LimitesDeEntrada.PREDETERMINADOS)
        val e = (validarLimites(base.copy(pesoMin = limpiarLimite("-1"), gastoMin = limpiarLimite("0"))) as ValidacionLimites.ConErrores).errores
        assertEquals(ERROR_LIMITE_NEGATIVO, e[CampoLimite.PESO_MIN])
        assertEquals(ERROR_LIMITE_CERO, e[CampoLimite.GASTO_MIN])
        val ok = validarLimites(base.copy(precioMin = limpiarLimite("2.000")))
        assertEquals(2_000, (ok as ValidacionLimites.Valida).limites.precioKgMinimoCop)
    }
}
