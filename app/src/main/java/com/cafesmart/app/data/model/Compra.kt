package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.Fecha

// COMPRA: Transacción para adquisicion de café (identificador único).
// Una compra puede traer varios cafés ("Café 1", "Café 2"... en el paso 2) y cada uno se convierte en un
// Sublote con compraId = esta compra. Por eso la relación es Compra 1 → N Sublote.
//
// totalPagado se guarda tal cúal una vez se hace la confirmación (el pago es inmediato).
// Aunque después se edite un sublote, la compra conserva lo que realmente se pagó. Al crearla se calcula con la misma cuenta del paso 3: suma de peso x precio de cada café.
// productorId = null significa compra "Sin registrar" (compraventa que no pide nombre ni cédula).
data class Compra(
    val id: Long = 0,
    val productorId: Long? = null,
    val fecha: Fecha,
    val totalPagado: Long
)

// Compra con su productor y sublotes: lo que muestra el historial de compras (RF-09).
data class CompraConSublotes(
    val compra: Compra,
    val productor: Productor?,
    val sublotes: List<Sublote>
) {
    val totalKg: Double get() = sublotes.sumOf { it.pesoInicialKg }
}
