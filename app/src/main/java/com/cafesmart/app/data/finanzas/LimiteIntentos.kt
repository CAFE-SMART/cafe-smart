package com.cafesmart.app.data.finanzas

// Límite de intentos de la clave financiera: 3 claves incorrectas en menos de 15 minutos bloquean el acceso
// durante 5 minutos; pasado ese tiempo se empieza de cero (otros 3 intentos). Funciones puras (el tiempo se
// recibe como parámetro): se prueban con JUnit.

const val MAX_INTENTOS_FALLIDOS = 3

/** Los fallos cuentan mientras tengan menos de 15 minutos. */
const val VENTANA_INTENTOS_MS = 15 * 60 * 1000L

/** Lo que dura el bloqueo tras el tercer fallo. */
const val DURACION_BLOQUEO_MS = 5 * 60 * 1000L

/**
 * Los fallos que cuentan ahora, del más reciente al más antiguo: los de los últimos 15 minutos. Si ya hubo 3 y
 * el bloqueo de 5 minutos ya se cumplió, se empieza de cero (lista vacía).
 */
fun fallosVigentes(fallos: List<Long>, ahora: Long): List<Long> {
    val enVentana = fallos.filter { ahora - it in 0 until VENTANA_INTENTOS_MS }.sortedDescending()
    val bloqueoCumplido = enVentana.size >= MAX_INTENTOS_FALLIDOS && ahora >= enVentana.first() + DURACION_BLOQUEO_MS
    return if (bloqueoCumplido) emptyList() else enVentana
}

/** Anota un fallo nuevo y descarta los que ya no cuentan. */
fun conNuevoFallo(fallos: List<Long>, ahora: Long): List<Long> = fallosVigentes(fallos, ahora) + ahora

/**
 * Cuántos milisegundos faltan para poder volver a intentar, o 0 si no está bloqueado.
 * Se bloquea con el tercer fallo y dura 5 minutos desde ese último fallo.
 */
fun milisDeBloqueo(fallos: List<Long>, ahora: Long): Long {
    val vigentes = fallosVigentes(fallos, ahora)
    if (vigentes.size < MAX_INTENTOS_FALLIDOS) return 0
    return (vigentes.first() + DURACION_BLOQUEO_MS - ahora).coerceAtLeast(0)
}

/** Intentos que le quedan antes de bloquearse (0 si ya está bloqueado). */
fun intentosRestantes(fallos: List<Long>, ahora: Long): Int =
    (MAX_INTENTOS_FALLIDOS - fallosVigentes(fallos, ahora).size).coerceAtLeast(0)

/** "1 minuto" / "5 minutos" (siempre redondea hacia arriba, nunca dice 0). */
fun textoDeEspera(milis: Long): String {
    val minutos = ((milis + 59_999) / 60_000).toInt().coerceAtLeast(1)
    return if (minutos == 1) "1 minuto" else "$minutos minutos"
}
