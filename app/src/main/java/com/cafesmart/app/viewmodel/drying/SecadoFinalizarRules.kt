package com.cafesmart.app.viewmodel.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EPSILON_KG
import com.cafesmart.app.data.model.EnvioSecado
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MAX
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MIN
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.viewmodel.inventory.clave
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.sale.cantidadDesdeTexto
import java.math.BigDecimal
import java.math.RoundingMode

// Reglas de los pasos 4 a 6 de Secado (finalizar): kilos secos por calidad, factor de rendimiento,
// fecha de fin y costo del cafe seco. Son funciones puras (sin Compose ni Android) para probarlas con JUnit.

// Textos. El de "maximo" es del Figma; los demas son propuestos y faltan por confirmar con el equipo.
const val MENSAJE_KILOS_SIN_CAFE = "Escribe los kilos de al menos una calidad."
const val MENSAJE_KILOS_NEGATIVOS = "Los kilos no pueden ser negativos."
const val MENSAJE_FACTOR_NO_NUMERICO = "Escribe solo números."
const val MENSAJE_FACTOR_FUERA_DE_RANGO =
    "Recuerda: El factor de rendimiento que escribas debe estar entre $FACTOR_RENDIMIENTO_MIN y $FACTOR_RENDIMIENTO_MAX."
const val AYUDA_FACTOR = "Entre $FACTOR_RENDIMIENTO_MIN y $FACTOR_RENDIMIENTO_MAX."
const val MENSAJE_FECHA_FIN_ANTES_DEL_INICIO = "La fecha de finalización no puede ser anterior al inicio."
const val MENSAJE_FECHA_FIN_FUTURA = "La fecha de finalización no puede ser futura."

// ---------------- Kilos secos ----------------

// "Entre los tres, máximo 290 kg."
fun mensajeMaximoKilosSecos(enviadosKg: Double): String = "Entre los tres, máximo ${formatoKg(enviadosKg)}."

// Lo que se escribe en el campo de una calidad: vacio vale 0 (el campo es opcional).
fun kilosSecosDesdeTexto(texto: String): Double = cantidadDesdeTexto(limpiarPeso(texto)) ?: 0.0

// Suma de los kilos secos de las tres calidades (redondeada a gramos). Una calidad que falta en el mapa vale 0.
fun kilosSecosTotal(kilos: Map<Calidad, Double>): Double = redondearKg(kilos.values.sum())

// Error de los kilos secos o null si son validos: ninguno negativo, la suma mayor a 0 y no mayor a lo enviado.
// Es la unica validacion: la usan la pantalla y el repositorio.
fun errorKilosSecos(kilos: Map<Calidad, Double>, enviadosKg: Double): String? {
    if (kilos.values.any { !it.isFinite() || it < 0 }) return MENSAJE_KILOS_NEGATIVOS
    val total = kilosSecosTotal(kilos)
    if (total <= 0.0) return MENSAJE_KILOS_SIN_CAFE
    if (total > enviadosKg + EPSILON_KG) return mensajeMaximoKilosSecos(enviadosKg)
    return null
}

// ---------------- Factor de rendimiento ----------------

// El factor escrito, o null si el campo esta vacio o no son solo digitos.
fun factorDesdeTexto(texto: String): Int? {
    val limpio = texto.trim()
    if (limpio.isEmpty() || !limpio.all { it in '0'..'9' }) return null
    // Un numero tan grande que no cabe en Int tambien esta fuera de rango: se deja como "no valido" (Int.MAX_VALUE).
    return limpio.toIntOrNull() ?: Int.MAX_VALUE
}

// Error de un factor ya convertido a numero, o null si esta entre 80 y 120.
fun errorFactorEntero(factor: Int): String? =
    if (factor in FACTOR_RENDIMIENTO_MIN..FACTOR_RENDIMIENTO_MAX) null else MENSAJE_FACTOR_FUERA_DE_RANGO

// Error del campo. Vacio no muestra error (como errorCantidad): simplemente el factor es obligatorio y
// factorValido sigue en false.
fun errorFactor(texto: String): String? {
    if (texto.trim().isEmpty()) return null
    val factor = factorDesdeTexto(texto) ?: return MENSAJE_FACTOR_NO_NUMERICO
    return errorFactorEntero(factor)
}

// Obligatorio, entero y entre 80 y 120.
fun factorValido(texto: String): Boolean = factorDesdeTexto(texto)?.let { errorFactorEntero(it) == null } ?: false

// ---------------- Fecha de fin ----------------

// Error de la fecha de fin o null: no puede ser anterior al inicio (el mismo dia vale) ni posterior a hoy.
fun errorFechaFin(fechaInicio: Fecha, fechaFin: Fecha, hoy: Fecha): String? = when {
    fechaFin.clave() < fechaInicio.clave() -> MENSAJE_FECHA_FIN_ANTES_DEL_INICIO
    fechaFin.clave() > hoy.clave() -> MENSAJE_FECHA_FIN_FUTURA
    else -> null
}

// ---------------- Costo del cafe seco ----------------

// Costo por kg del cafe seco (igual para las tres calidades), en pesos enteros:
// Σ(kg enviados de cada sublote x su costo por kg) / kg secos totales, redondeado con HALF_UP.
// El costo de lo que se perdio en la merma se reparte entre los kg que si quedaron.
// Es la unica funcion que decide este costo: si el equipo cambia la regla, se cambia aqui.
fun costoKgSeco(enviados: List<EnvioSecado>, kgSecosTotal: Double): Long {
    require(kgSecosTotal > 0) { "kgSecosTotal debe ser mayor a 0" }
    val costoTotal = enviados.fold(BigDecimal.ZERO) { suma, e ->
        suma + BigDecimal.valueOf(e.envio.pesoEnviadoKg).multiply(BigDecimal.valueOf(e.sublote.costoKg))
    }
    return costoTotal.divide(BigDecimal.valueOf(kgSecosTotal), 0, RoundingMode.HALF_UP).toLong()
}
