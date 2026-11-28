package com.cafesmart.app.data.limites

import android.content.Context
import com.cafesmart.app.viewmodel.limites.LimitesDeEntrada
import com.cafesmart.app.viewmodel.limites.LimitesVigentes

// Ajustes · "Límites de entrada" (ciclo 3, Amelia): guardar en el teléfono los mínimos y máximos que el usuario
// cambie, recuperarlos al volver a abrir la app y poder volver a los predeterminados.
//
// La pantalla usa esta capa para validar, guardar, recuperar, restablecer y aplicar los valores.
// La presentación y el precio compartido de compra y venta están pendientes de revisión del equipo.

/** Dónde se guardan los límites del usuario. null = nunca los cambió (se usan los predeterminados). */
interface AlmacenLimites {
    fun leer(): LimitesDeEntrada?
    fun guardar(limites: LimitesDeEntrada)
    fun borrar()
}

/** En el teléfono (SharedPreferences, archivo propio): sobrevive a cerrar y abrir la app. */
class AlmacenLimitesPreferencias(context: Context) : AlmacenLimites {
    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(): LimitesDeEntrada? {
        if (!prefs.contains(PESO_MIN)) return null
        // Un dato dañado o fuera de las reglas (por ejemplo, de una versión anterior) no tumba la app: se ignora.
        return runCatching {
            LimitesDeEntrada(
                pesoMinimoKg = prefs.getInt(PESO_MIN, 0),
                pesoMaximoKg = prefs.getInt(PESO_MAX, 0),
                precioKgMinimoCop = prefs.getInt(PRECIO_MIN, 0),
                precioKgMaximoCop = prefs.getInt(PRECIO_MAX, 0),
                gastoMinimoCop = prefs.getLong(GASTO_MIN, 0),
                gastoMaximoCop = prefs.getLong(GASTO_MAX, 0),
            )
        }.getOrNull()
    }

    override fun guardar(limites: LimitesDeEntrada) {
        prefs.edit()
            .putInt(PESO_MIN, limites.pesoMinimoKg).putInt(PESO_MAX, limites.pesoMaximoKg)
            .putInt(PRECIO_MIN, limites.precioKgMinimoCop).putInt(PRECIO_MAX, limites.precioKgMaximoCop)
            .putLong(GASTO_MIN, limites.gastoMinimoCop).putLong(GASTO_MAX, limites.gastoMaximoCop)
            .apply()
    }

    override fun borrar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val ARCHIVO = "limites_de_entrada"
        const val PESO_MIN = "peso_min_kg"
        const val PESO_MAX = "peso_max_kg"
        const val PRECIO_MIN = "precio_kg_min_cop"
        const val PRECIO_MAX = "precio_kg_max_cop"
        const val GASTO_MIN = "gasto_min_cop"
        const val GASTO_MAX = "gasto_max_cop"
    }
}

/** Para pruebas y vistas previas: solo en memoria. */
class AlmacenLimitesMemoria(private var guardados: LimitesDeEntrada? = null) : AlmacenLimites {
    override fun leer(): LimitesDeEntrada? = guardados
    override fun guardar(limites: LimitesDeEntrada) { guardados = limites }
    override fun borrar() { guardados = null }
}

/** Los campos que la pantalla permitirá editar. */
enum class CampoLimite { PESO_MIN, PESO_MAX, PRECIO_MIN, PRECIO_MAX, GASTO_MIN, GASTO_MAX }

/** Lo que el usuario escribió en la pantalla, tal cual (texto). */
data class BorradorLimites(
    val pesoMin: String, val pesoMax: String,
    val precioMin: String, val precioMax: String,
    val gastoMin: String, val gastoMax: String,
) {
    companion object {
        fun de(l: LimitesDeEntrada) = BorradorLimites(
            l.pesoMinimoKg.toString(), l.pesoMaximoKg.toString(),
            l.precioKgMinimoCop.toString(), l.precioKgMaximoCop.toString(),
            l.gastoMinimoCop.toString(), l.gastoMaximoCop.toString(),
        )
    }
}

/** Resultado de validar: los límites listos para guardar o un error por campo. */
sealed interface ValidacionLimites {
    data class Valida(val limites: LimitesDeEntrada) : ValidacionLimites
    data class ConErrores(val errores: Map<CampoLimite, String>) : ValidacionLimites
}

// Textos PROPUESTOS (falta aprobarlos con el equipo junto con el diseño de la pantalla).
const val ERROR_LIMITE_VACIO = "Escribe un valor."
const val ERROR_LIMITE_NEGATIVO = "No se permiten números negativos."
const val ERROR_LIMITE_NO_NUMERO = "Escribe solo números."
const val ERROR_LIMITE_CERO = "Debe ser mayor que 0."
const val ERROR_LIMITE_MINIMO_MAYOR = "El mínimo no puede ser mayor que el máximo."

/** Valida lo escrito: números enteros, sin negativos, mayores que 0 y cada mínimo menor o igual a su máximo. */
fun validarLimites(b: BorradorLimites): ValidacionLimites {
    val errores = linkedMapOf<CampoLimite, String>()
    fun numero(campo: CampoLimite, texto: String): Long? {
        val t = texto.trim().replace(".", "")
        val error = when {
            t.isEmpty() -> ERROR_LIMITE_VACIO
            t.startsWith("-") -> ERROR_LIMITE_NEGATIVO
            !t.all { it.isDigit() } || t.length > 12 -> ERROR_LIMITE_NO_NUMERO
            t.toLong() == 0L -> ERROR_LIMITE_CERO
            else -> null
        }
        if (error != null) errores[campo] = error
        return if (error == null) t.toLong() else null
    }
    val pesoMin = numero(CampoLimite.PESO_MIN, b.pesoMin)
    val pesoMax = numero(CampoLimite.PESO_MAX, b.pesoMax)
    val precioMin = numero(CampoLimite.PRECIO_MIN, b.precioMin)
    val precioMax = numero(CampoLimite.PRECIO_MAX, b.precioMax)
    val gastoMin = numero(CampoLimite.GASTO_MIN, b.gastoMin)
    val gastoMax = numero(CampoLimite.GASTO_MAX, b.gastoMax)
    fun par(min: Long?, max: Long?, campo: CampoLimite) {
        if (min != null && max != null && min > max) errores[campo] = ERROR_LIMITE_MINIMO_MAYOR
    }
    par(pesoMin, pesoMax, CampoLimite.PESO_MIN)
    par(precioMin, precioMax, CampoLimite.PRECIO_MIN)
    par(gastoMin, gastoMax, CampoLimite.GASTO_MIN)
    // Peso y precio se guardan como Int: un número más grande no cabe y se trata como inválido.
    listOf(CampoLimite.PESO_MIN to pesoMin, CampoLimite.PESO_MAX to pesoMax,
        CampoLimite.PRECIO_MIN to precioMin, CampoLimite.PRECIO_MAX to precioMax).forEach { (c, v) ->
        if (v != null && v > Int.MAX_VALUE) errores[c] = ERROR_LIMITE_NO_NUMERO
    }
    if (errores.isNotEmpty()) return ValidacionLimites.ConErrores(errores)
    return ValidacionLimites.Valida(
        LimitesDeEntrada(
            pesoMinimoKg = pesoMin!!.toInt(), pesoMaximoKg = pesoMax!!.toInt(),
            precioKgMinimoCop = precioMin!!.toInt(), precioKgMaximoCop = precioMax!!.toInt(),
            gastoMinimoCop = gastoMin!!, gastoMaximoCop = gastoMax!!,
        )
    )
}

/**
 * Lo que usará la pantalla de Ajustes: cargar al abrir la app, guardar lo editado y restablecer. Cada operación
 * deja los límites aplicados en [LimitesVigentes], que es lo que consultan Compras, Ventas y Gastos.
 */
class ConfiguracionLimites(private val almacen: AlmacenLimites) {

    /** Al abrir la app: los guardados o, si no hay (o están dañados), los predeterminados. */
    fun cargar(): LimitesDeEntrada {
        val limites = almacen.leer() ?: LimitesDeEntrada.PREDETERMINADOS
        LimitesVigentes.aplicar(limites)
        return limites
    }

    /** Valida y, si todo está bien, guarda y aplica. Si hay errores no cambia nada. */
    fun guardar(borrador: BorradorLimites): ValidacionLimites {
        val resultado = validarLimites(borrador)
        if (resultado is ValidacionLimites.Valida) {
            almacen.guardar(resultado.limites)
            LimitesVigentes.aplicar(resultado.limites)
        }
        return resultado
    }

    /** Vuelve a los predeterminados y borra lo guardado. */
    fun restablecer(): LimitesDeEntrada {
        almacen.borrar()
        LimitesVigentes.aplicar(LimitesDeEntrada.PREDETERMINADOS)
        return LimitesDeEntrada.PREDETERMINADOS
    }
}
