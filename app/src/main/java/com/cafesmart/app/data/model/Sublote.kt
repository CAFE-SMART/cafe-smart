package com.cafesmart.app.data.model

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha

// Margen para decir que un sublote se agotó: el peso tiene hasta 3 decimales (1 g) y al restar
// varias ventas un Double puede quedar en 0.0000001 en vez de 0.
const val EPSILON_KG = 0.0005

// Rangos de los "Datos de calidad" (Figma), definidos por el equipo.
const val HUMEDAD_MIN_PORCENTAJE = 8.0
const val HUMEDAD_MAX_PORCENTAJE = 15.0
const val FACTOR_RENDIMIENTO_MIN = 70
const val FACTOR_RENDIMIENTO_MAX = 140

// SUBLOTE: unidad real del inventario. Cada "Café N" de una compra crea un sublote con su propio
// tipo, calidad, peso, precio y fecha de entrada (HU-01). Sobre él se calculan gastos, merma y utilidad.
// El tipo y la calidad NO se repiten aquí: viven en el Lote (loteId) para que de esta manera no se contradigan.
// Para pantallas que necesitan todo junto se recomienda usar SubloteConLote.
//
// - compraId: el sublote nació de una compra.
// - secadoId: el sublote es café seco que resultó de un secado (viene de varios verdes).
// Solo uno de los dos puede tener valor (se valida en init).
//
// Dinero en pesos enteros (Long): los precios de la app son pesos sin decimales (limpiarPrecio solo
// deja dígitos), y con Long los totales no pierden centavos como pasaría con Double.
data class Sublote(
    val id: Long = 0,
    val loteId: Long,
    val compraId: Long? = null,
    val secadoId: Long? = null,
    val pesoInicialKg: Double,         // kg con que entró; no cambia (sirve para merma e historial)
    val pesoDisponibleKg: Double,      // kg que quedan: baja con ventas y con kg enviados a secar
    val costoKg: Long,                 // $/kg. Comprado: precio de compra. Seco de secado: costo trasladado
    val fechaIngreso: Fecha,
    val humedadPorcentaje: Double? = null,   // opcional, entre 8 y 15 (Figma: "Datos de calidad")
    val factorRendimiento: Int? = null       // opcional, entre 70 y 140 (Figma: "Datos de calidad")
) {
    init {
        require(compraId == null || secadoId == null) {
            "un sublote nace de una compra o de un secado, no de ambos"
        }
        require(pesoInicialKg > 0) { "pesoInicialKg debe ser mayor a 0" }
        require(pesoDisponibleKg >= 0) { "pesoDisponibleKg no puede ser negativo" }
        require(pesoDisponibleKg <= pesoInicialKg + EPSILON_KG) {
            "pesoDisponibleKg no puede superar a pesoInicialKg"
        }
        require(costoKg >= 0) { "costoKg no puede ser negativo" }
        humedadPorcentaje?.let {
            require(it in HUMEDAD_MIN_PORCENTAJE..HUMEDAD_MAX_PORCENTAJE) {
                "humedad debe estar entre $HUMEDAD_MIN_PORCENTAJE y $HUMEDAD_MAX_PORCENTAJE"
            }
        }
        factorRendimiento?.let {
            require(it in FACTOR_RENDIMIENTO_MIN..FACTOR_RENDIMIENTO_MAX) {
                "factor de rendimiento debe estar entre $FACTOR_RENDIMIENTO_MIN y $FACTOR_RENDIMIENTO_MAX"
            }
        }
    }

    // Estos dos se calculan al leerlos; no son columnas.
    val estaAgotado: Boolean get() = pesoDisponibleKg <= EPSILON_KG

    // "Valor total en bodega" del detalle del sublote: kg disponibles x costo por kg.
    val valorEnBodega: Long get() = (pesoDisponibleKg * costoKg).aPesos()
}

// Sublote con su lote: da acceso directo a tipo y calidad para las listas de inventario.
data class SubloteConLote(
    val sublote: Sublote,
    val lote: Lote
) {
    val tipoCafe: TipoCafe get() = lote.tipoCafe
    val calidad: Calidad get() = lote.calidad
}
