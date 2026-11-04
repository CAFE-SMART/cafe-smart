package com.cafesmart.app.viewmodel.inventory

/** Estado de ocupación de la bodega. Nunca bloquea una compra: solo informa. */
enum class EstadoBodega {
    /** La capacidad es 0: el usuario nuevo aún no la ha configurado. Se le pide asignarla (ver MENSAJE_CAPACIDAD_NO_CONFIGURADA). */
    SIN_CAPACIDAD,

    /** Menos del umbral de advertencia (por defecto 80 %). Verde. */
    NORMAL,

    /** Desde el umbral de advertencia hasta antes del crítico (80 % a menos de 90 %). Amarillo, solo visual, sin mensajes. */
    ADVERTENCIA,

    /** Desde el umbral crítico (90 %) en adelante, incluso por encima del 100 %. Rojo en Inventario y aviso en el paso 3 de Compras. */
    CRITICA,
}

/** Resultado del cálculo: porcentaje de ocupación (puede superar 100) y su estado. */
data class OcupacionBodega(val porcentaje: Double, val estado: EstadoBodega)

const val MENSAJE_CAPACIDAD_NO_CONFIGURADA = "Falta configurar la capacidad de tu bodega"

const val UMBRAL_ADVERTENCIA_POR_DEFECTO = 80.0
const val UMBRAL_CRITICO_POR_DEFECTO = 90.0

/**
 * Calcula la ocupación de la bodega: kgOcupados * 100 / capacidadKg.
 *
 * El porcentaje no se limita a 100: una compra puede superar la capacidad y el sistema solo avisa.
 *
 * Capacidad 0 significa que el usuario aún no la ha configurado (usuario nuevo): no hay nada que
 * evaluar, así que devuelve 0 % y estado SIN_CAPACIDAD, sin error. No confundir con kgOcupados = 0
 * (bodega vacía), que con capacidad configurada es NORMAL.
 *
 * @throws IllegalArgumentException si capacidadKg < 0, kgOcupados < 0
 * o los umbrales no cumplen 0 < umbralAdvertencia < umbralCritico.
 */
fun calcularOcupacionBodega(
    kgOcupados: Double,
    capacidadKg: Double,
    umbralAdvertencia: Double = UMBRAL_ADVERTENCIA_POR_DEFECTO,
    umbralCritico: Double = UMBRAL_CRITICO_POR_DEFECTO,
): OcupacionBodega {
    require(capacidadKg >= 0) { "capacidadKg no puede ser negativa" }
    require(kgOcupados >= 0) { "kgOcupados no puede ser negativo" }
    require(umbralAdvertencia > 0) { "umbralAdvertencia debe ser mayor a 0" }
    require(umbralCritico > umbralAdvertencia) { "umbralCritico debe ser mayor a umbralAdvertencia" }

    if (capacidadKg == 0.0) return OcupacionBodega(0.0, EstadoBodega.SIN_CAPACIDAD)

    val porcentaje = kgOcupados * 100 / capacidadKg
    val estado = when {
        porcentaje >= umbralCritico -> EstadoBodega.CRITICA
        porcentaje >= umbralAdvertencia -> EstadoBodega.ADVERTENCIA
        else -> EstadoBodega.NORMAL
    }
    return OcupacionBodega(porcentaje, estado)
}
