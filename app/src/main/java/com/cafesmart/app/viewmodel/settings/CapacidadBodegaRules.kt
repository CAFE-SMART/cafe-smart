package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.viewmodel.purchase.formatoKg
import java.util.Locale

// Reglas de "Capacidad de bodega" (Ajustes): cuántos kilos caben y cuánto pesa un bulto.
// - La capacidad la define cada negocio: no hay un máximo. Solo debe ser mayor a 0 (0 significa "sin
//   configurar" y no se puede volver a ese estado) y no menor al café que ya hay guardado (bug-6 de QA).
// - El peso del bulto va entre 10 y 100 kg; el valor inicial es 40,7 y la persona puede cambiarlo.

const val PESO_BULTO_MIN_KG = 10.0
const val PESO_BULTO_MAX_KG = 100.0

// Tope técnico de dígitos al escribir la capacidad, solo para que el número quepa en el campo y en el cálculo.
const val DIGITOS_MAX_CAPACIDAD = 9

const val ERROR_CAPACIDAD_VACIA = "Escribe cuántos kilos caben en tu bodega."
const val ERROR_CAPACIDAD_INVALIDA = "Escribe solo números, por ejemplo 3000."
const val ERROR_CAPACIDAD_CERO = "La capacidad debe ser mayor a 0 kg."
const val ERROR_BULTO_VACIO = "Escribe cuánto pesa un bulto."
const val ERROR_BULTO_INVALIDO = "Escribe un número, por ejemplo 40,7."
const val ERROR_BULTO_RANGO = "Un bulto pesa entre 10 y 100 kg."

/** Lo que está escrito en los dos campos. */
data class CapacidadFormulario(
    val capacidad: String = "",
    val pesoBulto: String = "",
)

/** Errores por campo ("capacidad", "bulto"). Vacío = se puede guardar. */
fun validarCapacidadBodega(form: CapacidadFormulario, kgOcupados: Double): Map<String, String> = buildMap {
    val capacidad = kgDesdeTexto(form.capacidad)
    when {
        form.capacidad.isBlank() -> put("capacidad", ERROR_CAPACIDAD_VACIA)
        capacidad == null -> put("capacidad", ERROR_CAPACIDAD_INVALIDA)
        capacidad <= 0.0 -> put("capacidad", ERROR_CAPACIDAD_CERO)
        capacidad < kgOcupados ->
            put("capacidad", "La capacidad no puede ser menor al café que ya tienes (${formatoKg(kgOcupados)}).")
    }
    val bulto = kgDesdeTexto(form.pesoBulto)
    when {
        form.pesoBulto.isBlank() -> put("bulto", ERROR_BULTO_VACIO)
        bulto == null -> put("bulto", ERROR_BULTO_INVALIDO)
        bulto < PESO_BULTO_MIN_KG || bulto > PESO_BULTO_MAX_KG -> put("bulto", ERROR_BULTO_RANGO)
    }
}

/**
 * Número que escribió la persona: acepta "3000", "3.000" (punto de miles) y "40,7" (coma decimal).
 * Devuelve null si no es un número.
 */
fun kgDesdeTexto(texto: String): Double? {
    val limpio = texto.trim().replace(" ", "")
    if (limpio.isEmpty()) return null
    // Con coma, los puntos son de miles: "1.234,5". Sin coma, un punto seguido de 3 dígitos también es de miles.
    val normal = when {
        limpio.contains(',') -> limpio.replace(".", "").replace(',', '.')
        Regex("""^\d{1,3}(\.\d{3})+$""").matches(limpio) -> limpio.replace(".", "")
        else -> limpio
    }
    return normal.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/** Para mostrar un valor guardado en el campo: 3000.0 -> "3000", 40.7 -> "40,7". */
fun textoDeKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else String.format(Locale.US, "%s", kg).replace('.', ',')

/** El negocio con la capacidad y el peso por bulto nuevos; lo demás no cambia. Los valores ya deben estar validados. */
fun negocioConCapacidad(actual: com.cafesmart.app.data.local.NegocioEntity, form: CapacidadFormulario) =
    actual.copy(
        capacidadBodegaKg = requireNotNull(kgDesdeTexto(form.capacidad)),
        pesoBultoKg = requireNotNull(kgDesdeTexto(form.pesoBulto)),
    )
