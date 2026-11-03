package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.Fecha
import kotlin.math.roundToLong

// VENTA: una operación de venta
// Es importante resaltar que, una sola venta puede incluir varios sublotes.
// Cada sublote incluye cantidad y su precio por kg ("Precio por kg · Buena"), y la venta puede ser total o parcial
// Por tanto, el detalle va en VentaSublote y la relación es Venta 1 → N VentaSublote.
//
// totalRecibido: total a recibir.
// igual que Compra.totalPagado. clienteId = null significa venta "Sin registrar".
data class Venta(
    val id: Long = 0,
    val clienteId: Long? = null,
    val fecha: Fecha,
    val totalRecibido: Long
)

// Detalle de la venta: qué sublote se vendió, cuántos kg y a qué precio. Llave primaria: (ventaId, subloteId).
// Al confirmar la venta, pesoKg se descuenta de Sublote.pesoDisponibleKg y no puede superarse.
data class VentaSublote(
    val ventaId: Long,
    val subloteId: Long,
    val pesoKg: Double,
    val precioKg: Long
) {
    val subtotal: Long get() = (pesoKg * precioKg).roundToLong()
}

// Venta con su cliente y detalle: lo que muestra el historial de ventas.
data class VentaConDetalle(
    val venta: Venta,
    val cliente: Cliente?,
    val items: List<VentaSublote>
) {
    val totalKg: Double get() = items.sumOf { it.pesoKg }
    val totalCalculado: Long get() = items.sumOf { it.subtotal }
}
