package com.cafesmart.app.data.borradores

import android.content.Context
import androidx.lifecycle.SavedStateHandle

// Borradores de operaciones a medio diligenciar (Compras hoy; Ventas cuando su flujo en Room esté integrado).
// Un borrador NO es una operación guardada: solo recuerda lo escrito para retomarlo. Se guarda:
// - en SavedStateHandle, que sobrevive a girar la pantalla, a recrear la actividad y a que Android elimine el proceso;
// - en el teléfono (SharedPreferences privadas de la app), que sobrevive además a cerrar y volver a abrir la app.
// Cada borrador va con una clave por tipo de operación Y por cuenta: una cuenta nunca ve el borrador de otra.
// No guarda contraseñas, tokens ni nada de la sesión: solo los campos del formulario.

/** Dónde se guarda el texto del borrador en el teléfono. */
interface AlmacenBorradores {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
    fun borrar(clave: String)
}

/** Preferencias privadas de la app (archivo propio): se conservan al cerrar la app. */
class AlmacenBorradoresPreferencias(context: Context) : AlmacenBorradores {
    private val prefs = context.applicationContext.getSharedPreferences("borradores_operaciones", Context.MODE_PRIVATE)
    override fun leer(clave: String): String? = prefs.getString(clave, null)
    override fun guardar(clave: String, valor: String) { prefs.edit().putString(clave, valor).apply() }
    override fun borrar(clave: String) { prefs.edit().remove(clave).apply() }
}

/** Para pruebas: solo en memoria. */
class AlmacenBorradoresMemoria : AlmacenBorradores {
    val guardados = mutableMapOf<String, String>()
    override fun leer(clave: String): String? = guardados[clave]
    override fun guardar(clave: String, valor: String) { guardados[clave] = valor }
    override fun borrar(clave: String) { guardados.remove(clave) }
}

/** Tipos de operación con borrador. */
enum class TipoOperacion(val prefijo: String) { COMPRA("compra"), VENTA("venta") }

/** "compra:<uid>" o "compra:sin_sesion". El uid identifica la cuenta, no es un secreto. */
fun claveBorrador(tipo: TipoOperacion, uid: String?): String = "${tipo.prefijo}:${uid ?: "sin_sesion"}"

/**
 * El borrador de UNA operación de UNA cuenta. Primero se lee el de SavedStateHandle (es el más reciente si Android
 * cerró el proceso) y, si no hay, el del teléfono. Guardar null lo borra de los dos lugares.
 */
class BorradorDeOperacion(
    private val almacen: AlmacenBorradores,
    private val clave: String,
    private val estadoGuardado: SavedStateHandle? = null,
) {
    fun leer(): String? = estadoGuardado?.get<String>(clave) ?: almacen.leer(clave)

    fun guardar(valor: String?) {
        estadoGuardado?.set(clave, valor)
        if (valor == null) almacen.borrar(clave) else almacen.guardar(clave, valor)
    }
}
