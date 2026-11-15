package com.cafesmart.app.data.seed

import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Revisa que los datos de ejemplo cumplan las reglas del modelo y sigan siendo los del Figma.
// Está en testDebug (no en test) porque la semilla solo existe en la variante debug.
class SemillaDeEjemploTest {

    private val cafes get() = COMPRAS_SEMILLA.flatMap { it.cafes }

    @Test
    fun losProductoresCumplenLasReglasDelFormulario() {
        val yaGuardados = mutableListOf<Productor>()
        for (p in PRODUCTORES_SEMILLA) {
            assertNull(p.nombre, validarNombre(p.nombre, p.tipoDocumento))
            assertNull(p.documento, validarDocumento(p.documento, p.tipoDocumento, yaGuardados))
            assertNull(p.telefono, validarTelefono(p.telefono))
            yaGuardados += Productor(nombre = p.nombre, documento = p.documento, tipoDocumento = p.tipoDocumento)
        }
    }

    @Test
    fun cadaCafeEsUnSubloteValidoDelModelo() {
        // Sublote valida en su init (pesos, costo...): si un dato está mal, lanza IllegalArgumentException.
        cafes.forEach {
            Sublote(
                loteId = 1,
                compraId = 1,
                pesoInicialKg = it.pesoInicialKg,
                pesoDisponibleKg = it.pesoDisponibleKg,
                costoKg = it.costoKg,
                fechaIngreso = COMPRAS_SEMILLA.first().fecha,
            )
        }
    }

    @Test
    fun lasComprasApuntanAProductoresQueExisten() {
        COMPRAS_SEMILLA.forEach { c ->
            c.productor?.let { assertTrue(it in PRODUCTORES_SEMILLA.indices) }
        }
        // Hay una compra "Sin registrar" y una compra con más de un café.
        assertTrue(COMPRAS_SEMILLA.any { it.productor == null })
        assertTrue(COMPRAS_SEMILLA.any { it.cafes.size > 1 })
    }

    @Test
    fun losKilosCoincidenConElFigmaYConLosDatosDeEjemploActuales() {
        fun kg(tipo: TipoCafe) = cafes.filter { it.tipo == tipo }.sumOf { it.pesoDisponibleKg }

        assertEquals(290.0, kg(TipoCafe.VERDE), 0.0)
        assertEquals(521.0, kg(TipoCafe.SECO), 0.0)
        assertEquals(23.0, kg(TipoCafe.TRILLADO), 0.0)
        assertEquals(0.0, kg(TipoCafe.PASILLA), 0.0)
        assertEquals(lotesEjemplo().sumOf { it.pesoDisponibleKg }, cafes.sumOf { it.pesoDisponibleKg }, 0.0)
        // 10 sublotes, 1 agotado: 9 con existencia (Verde 4, Seco 4, Trillado 1).
        assertEquals(10, cafes.size)
        assertEquals(9, cafes.count { it.pesoDisponibleKg > 0 })
    }
}
