package com.cafesmart.app.data.model

import com.cafesmart.app.data.model.Fecha

// GASTO: dinero que cuesta operar (transporte, cargue, comida, secado...)
// A qué aplica (aplicaA):
//  - GENERAL: no se asocia a ningún sublote.
//  - SUBLOTES: se asocia a uno o varios sublotes mediante GastoSublote.
//  - SECADO: es un gasto de un secado (Secado · Paso 7); entonces secadoId tiene valor.
// Regla: secadoId != null si y solo si aplicaA == SECADO.
data class Gasto(
    val id: Long = 0,
    val concepto: String,                    // "Concepto del gasto" (Ej: Pago de jornaleros)
    val descripcion: String = "",            // "Descripción breve", opcional
    val tipo: TipoGasto,
    val monto: Long,                         // en pesos, mayor a 0
    val fecha: Fecha,
    val estadoPago: EstadoPago,
    val aplicaA: AplicaGasto = AplicaGasto.GENERAL,
    val secadoId: Long? = null
) {
    init {
        require(concepto.isNotBlank()) { "concepto no puede estar vacío" }
        require(monto > 0) { "monto debe ser mayor a 0" }
        require((secadoId != null) == (aplicaA == AplicaGasto.SECADO)) {
            "secadoId debe tener valor si y solo si aplicaA == SECADO"
        }
    }

    // El reparto entre sublotes solo tiene sentido si el gasto aplica a SUBLOTES: la suma de
    // montoAsignado debe ser igual al monto. Para GENERAL y SECADO no debe haber reparto.
    fun repartoEsValido(reparto: List<GastoSublote>): Boolean =
        when (aplicaA) {
            AplicaGasto.SUBLOTES ->
                reparto.isNotEmpty() &&
                    reparto.all { it.gastoId == id } &&
                    reparto.sumOf { it.montoAsignado } == monto
            else -> reparto.isEmpty()
        }
}

// Tabla intermedia Gasto <-> Sublote (relación muchos a muchos: un gasto puede repartirse entre varios
// sublotes y un sublote puede tener varios gastos). Llave primaria: (gastoId, subloteId).
//
// montoAsignado es la parte del gasto que le toca a ese sublote. Guardarla evita la duda de "¿el total
// del gasto cuenta completo en cada sublote?": la suma de montoAsignado de un gasto debe ser igual a
// Gasto.monto. Por defecto se reparte en proporción a los kg; esa regla está por confirmar con el equipo.
data class GastoSublote(
    val gastoId: Long,
    val subloteId: Long,
    val montoAsignado: Long
) {
    init {
        require(montoAsignado > 0) { "montoAsignado debe ser mayor a 0" }
    }
}
