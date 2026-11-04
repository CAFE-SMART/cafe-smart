package com.cafesmart.app.viewmodel.finance

import kotlin.math.roundToLong

/**
 * Resultado del cálculo de utilidad de una venta, en pesos enteros (COP).
 *
 * La utilidad se redondea una sola vez al final, a partir de los valores sin redondear, así que
 * puede diferir en 1 peso de ingresosPesos - costoCompraPesos - gastosPesos.
 */
data class ResultadoUtilidad(
    val ingresosPesos: Long,
    val costoCompraPesos: Long,
    val gastosPesos: Long,
    val utilidadPesos: Long,
) {
    val esGanancia: Boolean get() = utilidadPesos > 0
    val esPerdida: Boolean get() = utilidadPesos < 0
}

/**
 * Calcula la utilidad de una venta (RN-G-28 del ERS, apartado 2.3):
 *
 *     utilidad = ingresos - costo de compra del café vendido - gastos
 *
 * - ingresos = kgVendidos * precioVentaKg
 * - costo de compra = kgVendidos * precioCompraKg
 * - gastos: monto total de gastos ya prorrateados a esta venta (no es por kg)
 *
 * Los gastos se restan (no se suman). El resultado va en pesos enteros, redondeado al final.
 * Una pérdida da una utilidad negativa.
 *
 * @throws IllegalArgumentException si algún valor es negativo, NaN o infinito.
 */
fun calcularUtilidad(
    kgVendidos: Double,
    precioVentaKg: Double,
    precioCompraKg: Double,
    gastos: Double = 0.0,
): ResultadoUtilidad {
    require(kgVendidos.isFinite() && kgVendidos >= 0) { "kgVendidos no puede ser negativo" }
    require(precioVentaKg.isFinite() && precioVentaKg >= 0) { "precioVentaKg no puede ser negativo" }
    require(precioCompraKg.isFinite() && precioCompraKg >= 0) { "precioCompraKg no puede ser negativo" }
    require(gastos.isFinite() && gastos >= 0) { "gastos no puede ser negativo" }

    val ingresos = kgVendidos * precioVentaKg
    val costoCompra = kgVendidos * precioCompraKg
    val utilidad = ingresos - costoCompra - gastos

    return ResultadoUtilidad(
        ingresosPesos = ingresos.roundToLong(),
        costoCompraPesos = costoCompra.roundToLong(),
        gastosPesos = gastos.roundToLong(),
        utilidadPesos = utilidad.roundToLong(),
    )
}
