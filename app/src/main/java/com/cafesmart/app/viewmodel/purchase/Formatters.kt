package com.cafesmart.app.viewmodel.purchase

import com.cafesmart.app.data.model.Fecha
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Formatos: convierten los datos en el texto que muestra la vista (con String.format como en la Guia 1).
// Estan en el ViewModel para que la vista no tenga logica: solo recibe el texto listo.
// Se usa Locale.US para que el resultado sea igual en cualquier celular,
// y luego se cambian los separadores al estilo colombiano.

// 25.0 -> "25 kg"   25.5 -> "25,5 kg"   25.125 -> "25,125 kg" (hasta 3 decimales, sin ceros de sobra)
fun formatoKg(kg: Double): String {
    // Al sumar varios pesos con decimales puede quedar 3.0000000000000004 y se escribiria "3, kg".
    // Se redondea a 3 decimales (1 g) antes de decidir si el numero es entero.
    val redondeado = Math.round(kg * 1000.0) / 1000.0
    // % 1.0 da la parte decimal: si es 0 el numero es entero y no lleva decimales.
    return if (redondeado % 1.0 == 0.0) {
        "${formatoMiles(redondeado.toLong())} kg"
    } else {
        // %.3f deja 3 decimales; trimEnd quita los ceros de sobra; replace pone la coma.
        String.format(Locale.US, "%.3f", redondeado).trimEnd('0').replace('.', ',') + " kg"
    }
}

// 14000 -> "14.000" (separador de miles con punto, como en Colombia)
// %,d agrega separador de miles (coma en Locale.US) y luego se cambia por punto.
fun formatoMiles(valor: Long): String = String.format(Locale.US, "%,d", valor).replace(',', '.')

// Lo que se ve en el campo de precio: "14000" -> "14.000" (el estado guarda solo los digitos)
fun precioEnPantalla(digitos: String): String =
    if (digitos.isEmpty()) "" else formatoMiles(digitos.toLong())

// 350000.0 -> "$ 350.000"
fun formatoDinero(valor: Double): String = "$ " + formatoMiles(valor.toLong())

// ---------------- Fechas (con Calendar, que existe en todas las versiones de Android) ----------------
// La fecha de hoy (fechaDeHoy) esta en PurchaseRules.kt

// private: solo se usa en este archivo.
private val meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

// "2 de junio de 2026"
// meses[fecha.mes - 1]: las listas empiezan en 0 y enero es el mes 1.
fun formatoFecha(fecha: Fecha): String = "${fecha.dia} de ${meses[fecha.mes - 1]} de ${fecha.anio}"

// "Hoy, 2 de junio de 2026" si es hoy, si no "2 de junio de 2026"
fun formatoFechaLarga(fecha: Fecha, hoy: Fecha): String =
    if (fecha == hoy) "Hoy, ${formatoFecha(fecha)}" else formatoFecha(fecha)

// El calendario (DatePicker) de Material trabaja en milisegundos en hora UTC

fun fechaAMilis(fecha: Fecha): Long {
    // Calendario en hora UTC, la misma que usa el DatePicker.
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    // clear() borra la hora para que quede en la medianoche del dia.
    calendario.clear()
    // Calendar cuenta los meses desde 0, por eso se resta 1.
    calendario.set(fecha.anio, fecha.mes - 1, fecha.dia)
    // timeInMillis: la fecha en milisegundos.
    return calendario.timeInMillis
}

fun milisAFecha(millis: Long): Fecha {
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    // Se carga la fecha que entrega el calendario para leer dia, mes y anio.
    calendario.timeInMillis = millis
    return Fecha(
        dia = calendario.get(Calendar.DAY_OF_MONTH),
        mes = calendario.get(Calendar.MONTH) + 1,
        anio = calendario.get(Calendar.YEAR)
    )
}
