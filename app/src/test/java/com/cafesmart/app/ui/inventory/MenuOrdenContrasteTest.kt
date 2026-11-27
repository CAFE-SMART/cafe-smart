package com.cafesmart.app.ui.inventory

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.resumirCalidades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Menú "Más reciente / Más antiguo" de Inventario: contraste WCAG AA de sus colores y el orden de las calidades.
class MenuOrdenContrasteTest {
    private fun contraste(a: Color, b: Color): Double {
        val (claro, oscuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05) / (oscuro + 0.05)
    }

    @Test fun textoNormalPasaAA() {
        val r = contraste(ColoresMenuOrden.texto, ColoresMenuOrden.fondo)
        assertTrue("contraste $r", r >= 4.5)
    }

    @Test fun opcionElegidaPasaAA() {
        val r = contraste(ColoresMenuOrden.textoElegido, ColoresMenuOrden.fondoElegido)
        assertTrue("contraste $r", r >= 4.5)
    }

    // El fondo es opaco (antes era el lila por defecto de Material) y la elegida se distingue del resto.
    @Test fun fondoOpacoYElegidaDistinta() {
        assertEquals(1f, ColoresMenuOrden.fondo.alpha)
        assertTrue(ColoresMenuOrden.fondoElegido != ColoresMenuOrden.fondo)
        assertTrue(ColoresMenuOrden.textoElegido != ColoresMenuOrden.texto)
    }

    // Los datos de la cuenta QA (café verde): buena 3-nov, regular 1-nov (y 28-oct), mala 25-oct.
    private val verde = listOf(
        grupo(1, Calidad.BUENA, sublote(1, 1, 60.0, Fecha(3, 11, 2026))),
        grupo(2, Calidad.REGULAR, sublote(2, 2, 120.0, Fecha(1, 11, 2026)), sublote(3, 2, 80.0, Fecha(28, 10, 2026))),
        grupo(3, Calidad.MALA, sublote(4, 3, 30.0, Fecha(25, 10, 2026))),
    )

    @Test fun masRecienteYMasAntiguoInviertenElOrdenSinCambiarLosDatos() {
        val recientes = resumirCalidades(verde, TipoCafe.VERDE, OrdenInventario.MAS_RECIENTE)
        val antiguos = resumirCalidades(verde, TipoCafe.VERDE, OrdenInventario.MAS_ANTIGUO)
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), recientes.map { it.lote.calidad })
        assertEquals(listOf(Calidad.MALA, Calidad.REGULAR, Calidad.BUENA), antiguos.map { it.lote.calidad })
        // Mismos kg y sublotes por calidad en los dos órdenes.
        assertEquals(recientes.associate { it.lote.calidad to (it.pesoKg to it.cantidadSublotes) },
            antiguos.associate { it.lote.calidad to (it.pesoKg to it.cantidadSublotes) })
        assertEquals(mapOf(Calidad.BUENA to (60.0 to 1), Calidad.REGULAR to (200.0 to 2), Calidad.MALA to (30.0 to 1)),
            recientes.associate { it.lote.calidad to (it.pesoKg to it.cantidadSublotes) })
    }

    private fun sublote(id: Long, lote: Long, kg: Double, f: Fecha) = Sublote(id, lote, 1, null, kg, kg, 14_000, f)
    private fun grupo(id: Long, c: Calidad, vararg s: Sublote) = LoteConSublotes(Lote(id, TipoCafe.VERDE, c), s.toList())
}
