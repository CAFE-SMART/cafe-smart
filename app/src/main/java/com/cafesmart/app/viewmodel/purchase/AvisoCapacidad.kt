package com.cafesmart.app.viewmodel.purchase

import com.cafesmart.app.viewmodel.inventory.UMBRAL_CRITICO_POR_DEFECTO
import kotlin.math.roundToInt

// Avisos de capacidad de bodega en Compras (Figma, NUEVAS): informativos, nunca bloquean la compra.
// - Paso 1: la capacidad está sin configurar (capacidad 0) -> se invita a configurarla.
// - Pasos 2 y 3 y "Compra registrada": la compra deja la ocupación en 90 % o más (casi llena) o por encima de
//   100 % (supera la capacidad). Con menos de 90 % no hay aviso, igual que en Inventario.
// Son funciones puras: reciben los kilos y devuelven los textos ya armados.

enum class NivelAvisoCapacidad { CASI_LLENA, SUPERADA }

/**
 * Lo que dejaría esta compra en la bodega.
 * @property porcentajeAntes ocupación de hoy, sin la compra.
 * @property porcentajeDespues ocupación con la compra (puede pasar de 100).
 * @property nuevoTotalKg kilos en bodega con la compra incluida.
 */
data class AvisoCapacidad(
    val nivel: NivelAvisoCapacidad,
    val porcentajeAntes: Int,
    val porcentajeDespues: Int,
    val capacidadKg: Double,
    val nuevoTotalKg: Double,
) {
    val kgLibres: Double get() = (capacidadKg - nuevoTotalKg).coerceAtLeast(0.0)
    val kgExceso: Double get() = (nuevoTotalKg - capacidadKg).coerceAtLeast(0.0)

    // Paso 2 (compacto, sobre el botón Continuar) y paso 3 (tarjeta con barra).
    val titulo: String
        get() = if (nivel == NivelAvisoCapacidad.SUPERADA) "Esta compra supera tu capacidad" else "Tu bodega quedará casi llena"

    val mensajePaso2: String
        get() = if (nivel == NivelAvisoCapacidad.SUPERADA) {
            "Te pasas ${formatoKg(kgExceso)} de los ${formatoKg(capacidadKg)} que caben."
        } else {
            "Pasa de $porcentajeAntes % a $porcentajeDespues %. Quedan ${formatoKg(kgLibres)} libres de ${formatoKg(capacidadKg)}."
        }

    val mensajePaso3: String
        get() = if (nivel == NivelAvisoCapacidad.SUPERADA) {
            "Te pasas ${formatoKg(kgExceso)} de los ${formatoKg(capacidadKg)} que caben. Es solo un aviso: igual puedes registrar la compra."
        } else {
            "Con esta compra tu bodega pasa de $porcentajeAntes % a $porcentajeDespues % ocupada. Es solo un aviso: igual puedes registrar la compra."
        }

    // "Compra registrada": ya pasó, así que aconseja vender café (no tocar la capacidad, que puede no ser real).
    val tituloRegistrada: String
        get() = if (nivel == NivelAvisoCapacidad.SUPERADA) "Tu bodega superó su capacidad" else "Tu bodega quedó casi llena"

    val mensajeRegistrada: String
        get() = "Nuevo total: ${formatoKg(nuevoTotalKg)} de ${formatoKg(capacidadKg)}. Te conviene vender café para liberar espacio."
}

/**
 * Aviso para una compra de [kgCompra] kilos con [kgOcupados] ya en bodega y [capacidadKg] de capacidad.
 * Devuelve null si no hay nada que avisar: capacidad sin configurar, compra vacía o ocupación menor a 90 %.
 * (El aviso de "sin configurar" del paso 1 es aparte: basta mirar si la capacidad es 0.)
 */
fun avisoCapacidadCompra(capacidadKg: Double, kgOcupados: Double, kgCompra: Double): AvisoCapacidad? {
    if (capacidadKg <= 0.0 || kgCompra <= 0.0 || kgOcupados < 0.0) return null
    val nuevoTotal = kgOcupados + kgCompra
    val despues = nuevoTotal * 100 / capacidadKg
    if (despues < UMBRAL_CRITICO_POR_DEFECTO) return null
    return AvisoCapacidad(
        nivel = if (despues > 100.0) NivelAvisoCapacidad.SUPERADA else NivelAvisoCapacidad.CASI_LLENA,
        porcentajeAntes = (kgOcupados * 100 / capacidadKg).roundToInt(),
        porcentajeDespues = despues.roundToInt(),
        capacidadKg = capacidadKg,
        nuevoTotalKg = nuevoTotal,
    )
}
