package com.cafesmart.app.data.session

import android.content.Context

// "Recordar mi cuenta" (RF-G-04). Firebase guarda la sesión en el celular por sí solo; lo que hay que
// recordar es si la persona quiso conservarla. Esa preferencia se guarda aparte y el Splash decide con ella.

enum class DestinoInicial { INICIO, LOGIN }

/**
 * Lo que hace el Splash al abrir la app.
 * [cerrarSesion] es true cuando hay una sesión de Firebase pero la persona no pidió recordarla.
 */
data class DecisionInicial(val destino: DestinoInicial, val cerrarSesion: Boolean)

/**
 * Regla del Splash:
 * - Hay sesión y se pidió recordarla -> Inicio.
 * - Hay sesión y NO se pidió recordarla -> se cierra y va al login.
 * - No hay sesión -> login.
 */
fun decidirInicio(hayUsuario: Boolean, recordar: Boolean): DecisionInicial = when {
    hayUsuario && recordar -> DecisionInicial(DestinoInicial.INICIO, cerrarSesion = false)
    hayUsuario -> DecisionInicial(DestinoInicial.LOGIN, cerrarSesion = true)
    else -> DecisionInicial(DestinoInicial.LOGIN, cerrarSesion = false)
}

/** Dónde se guarda si se pidió recordar la cuenta. Es una interfaz para probar la lógica sin Android. */
interface PreferenciaSesion {
    /** true si la persona pidió recordar la cuenta. Sin dato guardado vale true, igual que la casilla del login. */
    fun recordar(): Boolean

    fun guardarRecordar(valor: Boolean)

    /** Borra la preferencia (al cerrar sesión). */
    fun olvidar()
}

/** Versión real, sobre SharedPreferences. */
class PreferenciaSesionLocal(context: Context) : PreferenciaSesion {

    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun recordar(): Boolean = prefs.getBoolean(CLAVE_RECORDAR, true)

    override fun guardarRecordar(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE_RECORDAR, valor).apply()
    }

    override fun olvidar() {
        prefs.edit().remove(CLAVE_RECORDAR).apply()
    }

    private companion object {
        const val ARCHIVO = "sesion"
        const val CLAVE_RECORDAR = "recordar_cuenta"
    }
}
