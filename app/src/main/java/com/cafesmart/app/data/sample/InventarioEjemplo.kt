package com.cafesmart.app.data.sample

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe

// Datos de ejemplo del Inventario. Usan las mismas clases del modelo que luego llenará Room
// (Lote, Sublote, LoteConSublotes), así que cuando exista la base de datos solo cambia de dónde salen.
// Las cifras reproducen el Figma "Inventario · Resumen (Todos)":
//   Verde 290 kg (4 sublotes) · Seco 521 kg (4) · Trillado 23 kg (1) · Pasilla 0 kg (sin sublotes) · total 834 kg.
// Pasilla no tiene lotes a propósito, para mostrar el caso "Sin sublotes".

// Capacidad de la bodega del ejemplo (Figma: "de 3.000 kg").
const val CAPACIDAD_EJEMPLO_KG = 3000.0

// Atajo para armar un sublote de ejemplo. El que nace de un secado (café seco) lleva secadoId; el resto, compraId.
private fun sublote(
    id: Long,
    loteId: Long,
    pesoInicialKg: Double,
    pesoDisponibleKg: Double,
    costoKg: Long,
    fecha: Fecha,
    deSecado: Boolean = false
) = Sublote(
    id = id,
    loteId = loteId,
    compraId = if (deSecado) null else id,
    secadoId = if (deSecado) 1L else null,
    pesoInicialKg = pesoInicialKg,
    pesoDisponibleKg = pesoDisponibleKg,
    costoKg = costoKg,
    fechaIngreso = fecha
)

fun lotesEjemplo(): List<LoteConSublotes> = listOf(
    // ── Verde ────────────────────────────────────────────
    LoteConSublotes(
        Lote(1, TipoCafe.VERDE, Calidad.BUENA),
        listOf(sublote(1, 1, 60.0, 60.0, 16_000, Fecha(3, 11, 2026)))
    ),
    LoteConSublotes(
        Lote(2, TipoCafe.VERDE, Calidad.REGULAR),
        listOf(
            sublote(2, 2, 120.0, 120.0, 14_000, Fecha(1, 11, 2026)),
            sublote(3, 2, 100.0, 80.0, 13_500, Fecha(28, 10, 2026))
        )
    ),
    LoteConSublotes(
        Lote(3, TipoCafe.VERDE, Calidad.MALA),
        listOf(sublote(4, 3, 30.0, 30.0, 9_000, Fecha(25, 10, 2026)))
    ),
    // ── Seco (viene de un secado) ────────────────────────
    LoteConSublotes(
        Lote(4, TipoCafe.SECO, Calidad.BUENA),
        listOf(
            sublote(5, 4, 250.0, 200.0, 21_000, Fecha(20, 10, 2026), deSecado = true),
            sublote(6, 4, 100.0, 100.0, 21_000, Fecha(28, 10, 2026), deSecado = true),
            // Agotado: no cuenta como sublote ni suma kg (ver LoteConSublotes.sublotesConExistencia).
            sublote(7, 4, 90.0, 0.0, 20_000, Fecha(5, 9, 2026), deSecado = true)
        )
    ),
    LoteConSublotes(
        Lote(5, TipoCafe.SECO, Calidad.REGULAR),
        listOf(sublote(8, 5, 150.0, 150.0, 19_000, Fecha(22, 10, 2026), deSecado = true))
    ),
    LoteConSublotes(
        Lote(6, TipoCafe.SECO, Calidad.MALA),
        listOf(sublote(9, 6, 80.0, 71.0, 15_000, Fecha(18, 10, 2026), deSecado = true))
    ),
    // ── Trillado ─────────────────────────────────────────
    LoteConSublotes(
        Lote(7, TipoCafe.TRILLADO, Calidad.BUENA),
        listOf(sublote(10, 7, 23.0, 23.0, 30_000, Fecha(15, 10, 2026)))
    )
)
