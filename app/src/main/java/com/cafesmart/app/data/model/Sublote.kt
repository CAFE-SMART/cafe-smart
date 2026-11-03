package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.Calidad
import com.cafesmart.app.data.purchase.Fecha
import kotlin.math.roundToLong

// Margen para decir que un sublote se agotó: el peso tiene hasta 3 decimales (1 g) y al restar
// varias ventas un Double puede quedar en 0.0000001 en vez de 0.
const val EPSILON_KG = 0.0005

// SUBLOTE: unidad real del inventario. Cada "Café N" de una compra crea un sublote con su propio
// tipo, calidad, peso, precio y fecha de entrada (HU-01). Sobre él se calculan gastos, merma y utilidad.
// El tipo y la calidad NO se repiten aquí: viven en el Lote (loteId) para que de esta manera no se contradigan.
// Para pantallas que necesitan todo junto se recomienda usar SubloteConLote.
//
//
// - compraId: el sublote nació de una compra.
// - secadoId: el sublote es café seco que resultó de un secado (viene de varios verdes).
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
    val humedadPorcentaje: Double? = null,   // opcional, entre 0 y 100 (Figma: "Datos de calidad")
    val factorRendimiento: Int? = null       // opcional, entre 80 y 120 (Figma: "Datos de calidad")
) {
    // Estos dos se calculan al leerlos; no son columnas.
    val estaAgotado: Boolean get() = pesoDisponibleKg <= EPSILON_KG

    // "Valor total en bodega" del detalle del sublote: kg disponibles x costo por kg.
    val valorEnBodega: Long get() = (pesoDisponibleKg * costoKg).roundToLong()
}

// Sublote con su lote: da acceso directo a tipo y calidad para las listas de inventario.
data class SubloteConLote(
    val sublote: Sublote,
    val lote: Lote
) {
    val tipoCafe: TipoCafe get() = lote.tipoCafe
    val calidad: Calidad get() = lote.calidad
}
