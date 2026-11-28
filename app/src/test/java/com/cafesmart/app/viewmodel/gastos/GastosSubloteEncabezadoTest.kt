package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import org.junit.Assert.assertEquals
import org.junit.Test

// Encabezado de "Gastos del sublote" como en el Figma: "Sublote · Verde · Regular" (antes decía "Lote #6").
class GastosSubloteEncabezadoTest {
    private val sublote = Sublote(9, 6, 1, null, 120.0, 120.0, 14_000, Fecha(1, 11, 2026))
    private val lotes = listOf(
        LoteConSublotes(Lote(5, TipoCafe.SECO, Calidad.BUENA), emptyList()),
        LoteConSublotes(Lote(6, TipoCafe.VERDE, Calidad.REGULAR), listOf(sublote)),
    )

    @Test fun conElLoteMuestraTipoYCalidad() =
        assertEquals("Sublote #9 · Verde · Regular", encabezadoGastosSublote(sublote, lotes))

    @Test fun sinElLoteSoloElNumero() = assertEquals("Sublote #9", encabezadoGastosSublote(sublote, emptyList()))

    // Nunca toma el tipo o la calidad de otro lote.
    @Test fun usaElLoteDelSublote() =
        assertEquals("Sublote #9", encabezadoGastosSublote(sublote.copy(loteId = 77), lotes))
}
