package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.purchase.formatoKg
import java.util.Locale
import kotlin.math.roundToInt

// Lógica de la pantalla Inventario (resumen). Son funciones puras: reciben los lotes con sus sublotes
// (el modelo de data/model) y devuelven lo que la vista muestra, ya calculado y con el texto listo.
// Hoy los lotes vienen de datos de ejemplo; cuando exista Room solo cambia de dónde salen.

// Kilos que pesa un bulto. Valor inicial definido en el ERS (RF-G-29 y parámetros generales): 40,7 kg.
// Con él salen las cifras del Figma (834 kg = 20,5 bultos, 290 kg = 7,1 bultos, 521 kg = 12,8 bultos...).
// TODO: el ERS dice que el administrador lo configura en Ajustes; cuando exista esa pantalla, este valor
// pasa a ser solo el inicial y las funciones de abajo lo reciben como parámetro.
const val KG_POR_BULTO = 40.7

// Orden de los tipos en pantalla (Figma: Verde, Seco, Trillado, Pasilla). No es el orden del enum TipoCafe.
val TIPOS_EN_INVENTARIO: List<TipoCafe> =
    listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA)

// Opciones del botón "Más reciente" de la lista.
enum class OrdenInventario(val etiqueta: String) {
    MAS_RECIENTE("Más reciente"),
    MAS_ANTIGUO("Más antiguo")
}

// Una fila de la lista: un tipo de café con lo que hay en bodega.
// ultimoIngreso es la fecha del sublote más reciente (null si el tipo no tiene sublotes con café).
data class ResumenTipo(
    val tipo: TipoCafe,
    val pesoKg: Double,
    val cantidadSublotes: Int,
    val ultimoIngreso: Fecha?
)

// Todo lo que muestra la pantalla: el total, la capacidad de la bodega y la lista por tipo.
data class ResumenInventario(
    val totalKg: Double,
    val capacidadKg: Double,
    val ocupacion: OcupacionBodega,
    val tipos: List<ResumenTipo>
) {
    // Vacío = ningún tipo tiene sublotes con café (los agotados no cuentan).
    val estaVacio: Boolean get() = tipos.all { it.cantidadSublotes == 0 }
}

// Agrupa los lotes por tipo de café. Solo cuentan los sublotes con existencia (LoteConSublotes ya lo filtra).
fun resumirInventario(
    lotes: List<LoteConSublotes>,
    capacidadKg: Double,
    orden: OrdenInventario = OrdenInventario.MAS_RECIENTE
): ResumenInventario {
    val tipos = TIPOS_EN_INVENTARIO.map { tipo ->
        val sublotes = lotes
            .filter { it.lote.tipoCafe == tipo }
            .flatMap { it.sublotesConExistencia }
        ResumenTipo(
            tipo = tipo,
            pesoKg = redondearKg(sublotes.sumOf { it.pesoDisponibleKg }),
            cantidadSublotes = sublotes.size,
            ultimoIngreso = sublotes.map { it.fechaIngreso }.maxByOrNull { it.clave() }
        )
    }
    val totalKg = redondearKg(tipos.sumOf { it.pesoKg })
    return ResumenInventario(
        totalKg = totalKg,
        capacidadKg = capacidadKg,
        ocupacion = calcularOcupacionBodega(totalKg, capacidadKg),
        tipos = ordenarTipos(tipos, orden)
    )
}

// Ordena por la fecha del último ingreso. Los tipos sin café van siempre al final.
// sortedBy es estable: si dos tipos empatan se queda el orden de TIPOS_EN_INVENTARIO.
fun ordenarTipos(tipos: List<ResumenTipo>, orden: OrdenInventario): List<ResumenTipo> {
    val (conCafe, sinCafe) = tipos.partition { it.ultimoIngreso != null }
    val ordenados = when (orden) {
        OrdenInventario.MAS_RECIENTE -> conCafe.sortedByDescending { it.ultimoIngreso?.clave() ?: 0 }
        OrdenInventario.MAS_ANTIGUO -> conCafe.sortedBy { it.ultimoIngreso?.clave() ?: 0 }
    }
    return ordenados + sinCafe
}

// Los pesos tienen hasta 3 decimales, pero al sumar varios Double puede quedar 289.99999999.
// Se redondea a 3 decimales (1 g) para que el total y su texto salgan limpios.
fun redondearKg(kg: Double): Double = Math.round(kg * 1000.0) / 1000.0

// Fecha como número para compararla: 2026-11-03 -> 20261103.
private fun Fecha.clave(): Int = anio * 10_000 + mes * 100 + dia

// ---------------- Textos que muestra la pantalla ----------------

fun kgABultos(kg: Double): Double = kg / KG_POR_BULTO

// 290.0 -> "7,1 bultos"
fun formatoBultos(kg: Double): String =
    String.format(Locale.US, "%.1f", kgABultos(kg)).replace('.', ',') + " bultos"

// 290.0 -> "290 kg · 7,1 bultos"
fun detallePeso(kg: Double): String = "${formatoKg(kg)} · ${formatoBultos(kg)}"

// 0 -> "Sin sublotes", 1 -> "1 sublote", 4 -> "4 sublotes"
fun formatoSublotes(cantidad: Int): String = when (cantidad) {
    0 -> "Sin sublotes"
    1 -> "1 sublote"
    else -> "$cantidad sublotes"
}

// 27.8 -> "28%" (puede pasar de 100 si la bodega está sobrepasada)
fun formatoPorcentaje(porcentaje: Double): String = "${porcentaje.roundToInt()}%"

// Texto junto al punto de color. Figma solo trae "Capacidad normal"; los otros dos son propuesta.
fun etiquetaEstadoBodega(estado: EstadoBodega): String = when (estado) {
    EstadoBodega.NORMAL -> "Capacidad normal"
    EstadoBodega.ADVERTENCIA -> "Capacidad alta"
    EstadoBodega.CRITICA -> "Capacidad crítica"
    EstadoBodega.SIN_CAPACIDAD -> MENSAJE_CAPACIDAD_NO_CONFIGURADA
}
