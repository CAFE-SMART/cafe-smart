package com.cafesmart.app.viewmodel.drying

/**
 * Calcula el porcentaje de merma del secado: (kgInicial - kgFinal) / kgInicial * 100.
 *
 * @throws IllegalArgumentException si kgInicial <= 0, kgFinal < 0 o kgFinal > kgInicial.
 */
fun calcularMermaPorcentaje(kgInicial: Double, kgFinal: Double): Double {
    require(kgInicial > 0) { "kgInicial debe ser mayor a 0" }
    require(kgFinal >= 0) { "kgFinal no puede ser negativo" }
    require(kgFinal <= kgInicial) { "kgFinal no puede ser mayor a kgInicial" }
    return (kgInicial - kgFinal) / kgInicial * 100
}
