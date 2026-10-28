package com.cafesmart.app.ui.compra

import com.cafesmart.app.data.Fecha
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Formatos para mostrar valores en pantalla (con String.format como en la Guia 1).
// Se usa Locale.US para que el resultado sea igual en cualquier celular,
// y luego se cambian los separadores al estilo colombiano.

// 25.0 -> "25 kg"   25.5 -> "25,5 kg"
fun formatoKg(kg: Double): String =
    if (kg % 1.0 == 0.0) {
        "${formatoMiles(kg.toLong())} kg"
    } else {
        String.format(Locale.US, "%.1f", kg).replace('.', ',') + " kg"
    }

// 14000 -> "14.000" (separador de miles con punto, como en Colombia)
fun formatoMiles(valor: Long): String = String.format(Locale.US, "%,d", valor).replace(',', '.')

// 350000.0 -> "$ 350.000"
fun formatoDinero(valor: Double): String = "$ " + formatoMiles(valor.toLong())

// ---------------- Fechas (con Calendar, que existe en todas las versiones de Android) ----------------

private val meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

// Calendar cuenta los meses desde 0 (enero = 0), por eso se suma o resta 1
fun fechaDeHoy(): Fecha {
    val calendario = Calendar.getInstance()
    return Fecha(
        dia = calendario.get(Calendar.DAY_OF_MONTH),
        mes = calendario.get(Calendar.MONTH) + 1,
        anio = calendario.get(Calendar.YEAR)
    )
}

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
