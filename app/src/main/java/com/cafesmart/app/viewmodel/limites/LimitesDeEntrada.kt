package com.cafesmart.app.viewmodel.limites

import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoMiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ÚNICA fuente de los límites de dinero y de peso que se escriben en la app: Compras, Gastos (ExpenseRules y
// GastosValidation), Ventas y, cuando exista su pantalla, Secado. Los archivos de reglas de cada módulo usan estos
// valores en vez de repetirlos (sus nombres antiguos quedan como alias para no romper el código de cada módulo).
//
// Las constantes son los valores PREDETERMINADOS aprobados por el equipo. Los que valen en cada momento están en
// LimitesVigentes: Ajustes · "Límites de entrada" (data/limites/ConfiguracionLimites) los cambia, los guarda en el
// teléfono y los recupera al abrir la app. Las reglas de Compras, Ventas y Gastos consultan LimitesVigentes.
//
// Requisito de la tarjeta: solo los mensajes de mínimo y máximo de dinero dicen "pesos".

// ---------------- Peso (kg) ----------------

/** Peso mínimo de un café en una compra (y en una venta o un secado). Desde el PR #38: 1 kg. */
const val PESO_MINIMO_KG = 1

/** Peso máximo de un café: 5 enteros (99.999 kg). */
const val PESO_MAXIMO_KG = 99_999

/** Venta mínima (suma de lo que se vende en una venta). Regla aprobada de Ventas: 5 kg. */
const val VENTA_MINIMA_KG = 5.0

/** Dígitos enteros que admite el campo de peso: los del máximo vigente (99.999 son 5). */
val DIGITOS_PESO: Int get() = LimitesVigentes.actuales.pesoMaximoKg.toString().length

// ---------------- Precio por kg (COP, pesos enteros) ----------------

/** Precio mínimo por kg al comprar o vender. */
const val PRECIO_KG_MINIMO_COP = 1_000

/** Precio máximo por kg al comprar o vender. */
const val PRECIO_KG_MAXIMO_COP = 100_000

// ---------------- Valor de un gasto (COP, pesos enteros) ----------------

/** Dígitos que admite el campo de precio por kg: los del máximo vigente (100.000 son 6). */
val DIGITOS_PRECIO_KG: Int get() = LimitesVigentes.actuales.precioKgMaximoCop.toString().length

/** Valor mínimo de un gasto. */
const val GASTO_MINIMO_COP = 1_000L

/** Valor máximo de un gasto. */
const val GASTO_MAXIMO_COP = 50_000_000L

/**
 * Un juego completo de límites (los predeterminados o los que guardó el usuario en Ajustes). Rechaza combinaciones
 * imposibles (mínimo mayor que máximo o valores no positivos).
 */
data class LimitesDeEntrada(
    val pesoMinimoKg: Int = PESO_MINIMO_KG,
    val pesoMaximoKg: Int = PESO_MAXIMO_KG,
    val precioKgMinimoCop: Int = PRECIO_KG_MINIMO_COP,
    val precioKgMaximoCop: Int = PRECIO_KG_MAXIMO_COP,
    val gastoMinimoCop: Long = GASTO_MINIMO_COP,
    val gastoMaximoCop: Long = GASTO_MAXIMO_COP,
) {
    init {
        require(pesoMinimoKg >= 1) { "pesoMinimoKg debe ser mayor a 0" }
        require(pesoMaximoKg >= pesoMinimoKg) { "pesoMaximoKg no puede ser menor que pesoMinimoKg" }
        require(precioKgMinimoCop >= 1) { "precioKgMinimoCop debe ser mayor a 0" }
        require(precioKgMaximoCop >= precioKgMinimoCop) { "precioKgMaximoCop no puede ser menor que precioKgMinimoCop" }
        require(gastoMinimoCop >= 1) { "gastoMinimoCop debe ser mayor a 0" }
        require(gastoMaximoCop >= gastoMinimoCop) { "gastoMaximoCop no puede ser menor que gastoMinimoCop" }
    }

    fun pesoEnRango(kg: Double): Boolean = kg >= pesoMinimoKg && kg <= pesoMaximoKg
    fun precioEnRango(pesos: Double): Boolean = pesos >= precioKgMinimoCop && pesos <= precioKgMaximoCop
    fun gastoEnRango(pesos: Long): Boolean = pesos in gastoMinimoCop..gastoMaximoCop

    companion object {
        val PREDETERMINADOS = LimitesDeEntrada()
    }
}

/**
 * Los límites que valen ahora. Empiezan en los predeterminados; ConfiguracionLimites los reemplaza al abrir la app
 * (si el usuario guardó otros) y al guardar o restablecer en Ajustes. Lo consultan todas las reglas.
 */
object LimitesVigentes {
    private val estado = MutableStateFlow(LimitesDeEntrada.PREDETERMINADOS)
    val flujo: StateFlow<LimitesDeEntrada> = estado
    val actuales: LimitesDeEntrada get() = estado.value
    fun aplicar(limites: LimitesDeEntrada) { estado.value = limites }
}

// ---------------- Mensajes armados con los límites vigentes (con los predeterminados, el texto aprobado) ----------------

/** "Escribe un peso entre 1 y 99.999 kg." */
val MENSAJE_PESO_RANGO: String get() = LimitesVigentes.actuales.let {
    "Escribe un peso entre ${formatoMiles(it.pesoMinimoKg.toLong())} y ${formatoMiles(it.pesoMaximoKg.toLong())} kg."
}

/** "Entre 1 y 99.999 kg. Decimales con coma: 12,5" */
val AYUDA_PESO_RANGO: String get() = LimitesVigentes.actuales.let {
    "Entre ${formatoMiles(it.pesoMinimoKg.toLong())} y ${formatoMiles(it.pesoMaximoKg.toLong())} kg. Decimales con coma: 12,5"
}

/** "Escribe un valor entre $1.000 y $100.000 pesos colombianos." (error de rango de dinero: dice "pesos"). */
val MENSAJE_PRECIO_RANGO: String get() = LimitesVigentes.actuales.let {
    "Escribe un valor entre $${formatoMiles(it.precioKgMinimoCop.toLong())} y $${formatoMiles(it.precioKgMaximoCop.toLong())} pesos colombianos."
}

// ---------------- Mensajes de mínimo y máximo de dinero (los únicos que dicen "pesos") ----------------

/** "El valor mínimo es 1.000 pesos" */
fun mensajeValorMinimo(pesos: Long): String = "El valor mínimo es ${formatoMiles(pesos)} pesos"

/** "El valor máximo es 50.000.000 pesos" */
fun mensajeValorMaximo(pesos: Long): String = "El valor máximo es ${formatoMiles(pesos)} pesos"

/** Ayuda del campo precio por kg: "Entre $ 1.000 y $ 100.000." (sin la palabra "pesos": no es un error). */
fun ayudaPrecioKg(limites: LimitesDeEntrada = LimitesVigentes.actuales): String =
    "Entre ${formatoDinero(limites.precioKgMinimoCop.toDouble())} y ${formatoDinero(limites.precioKgMaximoCop.toDouble())}."
