package com.cafesmart.app.ui.compra

import com.cafesmart.app.data.Fecha
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Formatos para mostrar valores en pantalla (con String.format como en la Guia 1).
// Se usa Locale.US para que el resultado sea igual en cualquier celular,
// y luego se cambian los separadores al estilo colombiano.

// 25.0 -> "25 kg"   25.5 -> "25,5 kg"   25.125 -> "25,125 kg" (hasta 3 decimales, sin ceros de sobra)
fun formatoKg(kg: Double): String =
    if (kg % 1.0 == 0.0) {
        "${formatoMiles(kg.toLong())} kg"
    } else {
        String.format(Locale.US, "%.3f", kg).trimEnd('0').replace('.', ',') + " kg"
    }

// 14000 -> "14.000" (separador de miles con punto, como en Colombia)
fun formatoMiles(valor: Long): String = String.format(Locale.US, "%,d", valor).replace(',', '.')

// Lo que se ve en el campo de precio: "14000" -> "14.000" (el estado guarda solo los digitos)
fun precioEnPantalla(digitos: String): String =
    if (digitos.isEmpty()) "" else formatoMiles(digitos.toLong())

// 350000.0 -> "$ 350.000"
fun formatoDinero(valor: Double): String = "$ " + formatoMiles(valor.toLong())

// ---------------- Fechas (con Calendar, que existe en todas las versiones de Android) ----------------
// La fecha de hoy (fechaDeHoy) esta en la capa de dominio

private val meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

// "Hoy, 2 de junio de 2026" si es hoy, si no "2 de junio de 2026"
fun formatoFechaLarga(fecha: Fecha, hoy: Fecha): String {
    val texto = "${fecha.dia} de ${meses[fecha.mes - 1]} de ${fecha.anio}"
    return if (fecha == hoy) "Hoy, $texto" else texto
}

// El calendario (DatePicker) de Material trabaja en milisegundos en hora UTC

fun fechaAMilis(fecha: Fecha): Long {
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendario.clear()
    calendario.set(fecha.anio, fecha.mes - 1, fecha.dia)
    return calendario.timeInMillis
}

fun milisAFecha(millis: Long): Fecha {
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendario.timeInMillis = millis
    return Fecha(
        dia = calendario.get(Calendar.DAY_OF_MONTH),
        mes = calendario.get(Calendar.MONTH) + 1,
        anio = calendario.get(Calendar.YEAR)
    )
}
