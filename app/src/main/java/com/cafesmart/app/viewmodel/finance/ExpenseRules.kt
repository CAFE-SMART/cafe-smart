package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.formatoMiles

// Validaciones del registro de un gasto (ExpenseRules). Funciones puras, sin Compose ni Android,
// para probarlas con JUnit. Aún no se conectan a pantallas (se enchufan en el ciclo 3).
// No lanzan excepciones: devuelven un mensaje corto, en tuteo, por cada campo con error.
// Solo los mensajes de mínimos y máximos de dinero dicen "pesos" (por ejemplo, "El valor mínimo es 1.000 pesos").

// ---------------- Constantes fáciles de cambiar ----------------

// Tipos de gasto permitidos. Para quitar o agregar uno, se cambia el enum TipoGasto (data/model/Enums.kt)
// o se pasa otra lista a validarGasto.
val TIPOS_GASTO_PERMITIDOS: List<TipoGasto> = TipoGasto.values().toList()

// Valores por defecto de los límites. En Ajustes el usuario podrá cambiar los límites de todo lo que
// sea dinero (mínimo y máximo): por eso se reciben como parámetro (LimitesGasto) y estos son solo los iniciales.
// Convención de nombres para límites configurables: <CAMPO>_<MINIMO|MAXIMO>_<UNIDAD>, por ejemplo
// VALOR_GASTO_MINIMO_COP (COP = pesos colombianos). Los campos de LimitesGasto siguen la misma idea.
const val VALOR_GASTO_MINIMO_COP = 1_000L
const val VALOR_GASTO_MAXIMO_COP = 50_000_000L
const val LARGO_CONCEPTO_GASTO_MAXIMO = 60
const val LARGO_DESCRIPCION_GASTO_MAXIMO = 200

// Mensajes (tuteo, cortos).
const val MSG_CONCEPTO_OBLIGATORIO = "Escribe el concepto del gasto"
const val MSG_TIPO_OBLIGATORIO = "Elige el tipo de gasto"
const val MSG_VALOR_OBLIGATORIO = "Escribe el valor del gasto"
const val MSG_FECHA_INVALIDA = "Elige una fecha válida"
const val MSG_FECHA_FUTURA = "La fecha no puede ser futura"

/** Límites configurables de un gasto (los valores en pesos colombianos, COP). valorMinimoCop debe ser al menos 1. */
data class LimitesGasto(
    val valorMinimoCop: Long = VALOR_GASTO_MINIMO_COP,
    val valorMaximoCop: Long = VALOR_GASTO_MAXIMO_COP,
    val largoConceptoMaximo: Int = LARGO_CONCEPTO_GASTO_MAXIMO,
    val largoDescripcionMaximo: Int = LARGO_DESCRIPCION_GASTO_MAXIMO,
) {
    init {
        require(valorMinimoCop >= 1) { "valorMinimoCop debe ser mayor a 0" }
        require(valorMaximoCop >= valorMinimoCop) { "valorMaximoCop no puede ser menor que valorMinimoCop" }
        require(largoConceptoMaximo >= 1) { "largoConceptoMaximo debe ser mayor a 0" }
        require(largoDescripcionMaximo >= 0) { "largoDescripcionMaximo no puede ser negativo" }
    }
}

/** Un mensaje por campo con error; null si el campo está bien. */
data class ErroresGasto(
    val concepto: String? = null,
    val tipo: String? = null,
    val valor: String? = null,
    val fecha: String? = null,
    val descripcion: String? = null,
) {
    val esValido: Boolean get() = concepto == null && tipo == null && valor == null && fecha == null && descripcion == null
}

/**
 * Valida los campos de un gasto.
 *
 * - concepto: obligatorio (no vacío ni solo espacios) y de máximo limites.largoConceptoMaximo caracteres.
 * - tipo: obligatorio y dentro de tiposPermitidos.
 * - valor: obligatorio y entre limites.valorMinimoCop y limites.valorMaximoCop (ambos incluidos).
 * - fecha: válida (día y mes reales) y no futura; hoy sí es válida.
 * - descripcion: opcional, de máximo limites.largoDescripcionMaximo caracteres.
 *
 * @param valor en pesos; null si el campo está vacío.
 * @param hoy se recibe para poder probar la regla de la fecha; por defecto, el día actual.
 */
fun validarGasto(
    concepto: String,
    tipo: TipoGasto?,
    valor: Long?,
    fecha: Fecha,
    descripcion: String = "",
    hoy: Fecha = fechaDeHoy(),
    limites: LimitesGasto = LimitesGasto(),
    tiposPermitidos: List<TipoGasto> = TIPOS_GASTO_PERMITIDOS,
): ErroresGasto = ErroresGasto(
    concepto = errorConcepto(concepto, limites),
    tipo = errorTipo(tipo, tiposPermitidos),
    valor = errorValor(valor, limites),
    fecha = errorFecha(fecha, hoy),
    descripcion = errorDescripcion(descripcion, limites),
)

private fun errorConcepto(concepto: String, limites: LimitesGasto): String? = when {
    concepto.isBlank() -> MSG_CONCEPTO_OBLIGATORIO
    concepto.trim().length > limites.largoConceptoMaximo -> "El concepto puede tener máximo ${limites.largoConceptoMaximo} caracteres"
    else -> null
}

private fun errorTipo(tipo: TipoGasto?, permitidos: List<TipoGasto>): String? = when {
    tipo == null -> MSG_TIPO_OBLIGATORIO
    tipo !in permitidos -> MSG_TIPO_OBLIGATORIO
    else -> null
}

private fun errorValor(valor: Long?, limites: LimitesGasto): String? = when {
    valor == null -> MSG_VALOR_OBLIGATORIO
    valor <= 0 -> "El valor debe ser mayor que 0"
    valor < limites.valorMinimoCop -> "El valor mínimo es ${formatoMiles(limites.valorMinimoCop)} pesos"
    valor > limites.valorMaximoCop -> "El valor máximo es ${formatoMiles(limites.valorMaximoCop)} pesos"
    else -> null
}

private fun errorFecha(fecha: Fecha, hoy: Fecha): String? = when {
    !esFechaReal(fecha) -> MSG_FECHA_INVALIDA
    esPosterior(fecha, hoy) -> MSG_FECHA_FUTURA
    else -> null
}

private fun errorDescripcion(descripcion: String, limites: LimitesGasto): String? =
    if (descripcion.trim().length > limites.largoDescripcionMaximo) {
        "La descripción puede tener máximo ${limites.largoDescripcionMaximo} caracteres"
    } else {
        null
    }

private fun esPosterior(a: Fecha, b: Fecha): Boolean =
    if (a.anio != b.anio) a.anio > b.anio else if (a.mes != b.mes) a.mes > b.mes else a.dia > b.dia

private fun esBisiesto(anio: Int): Boolean = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0

private fun esFechaReal(f: Fecha): Boolean {
    if (f.anio < 1 || f.mes !in 1..12 || f.dia < 1) return false
    val diasDelMes = when (f.mes) {
        2 -> if (esBisiesto(f.anio)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return f.dia <= diasDelMes
}
