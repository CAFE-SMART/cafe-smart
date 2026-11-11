package com.cafesmart.app.data.session

import android.content.Context

// Interruptor "Notificaciones" de Ajustes > Negocio: si la persona quiere recibir avisos (por ejemplo, cuando un
// sublote lleva mucho tiempo en bodega). Aquí solo se guarda la elección; quien envíe los avisos la consultará.

interface PreferenciaNotificaciones {
    /** true si los avisos están activados. Sin dato guardado vale true (así aparece en el Figma). */
    fun activas(): Boolean

    fun guardar(activas: Boolean)
}

/** Versión real, sobre SharedPreferences. */
class PreferenciaNotificacionesLocal(context: Context) : PreferenciaNotificaciones {

    private val prefs = context.applicationContext.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    override fun activas(): Boolean = prefs.getBoolean(CLAVE, true)

    override fun guardar(activas: Boolean) {
        prefs.edit().putBoolean(CLAVE, activas).apply()
    }

    private companion object {
        const val CLAVE = "notificaciones_activas"
    }
}

/** Versión en memoria para pruebas y vistas previas. */
class PreferenciaNotificacionesEnMemoria(private var valor: Boolean = true) : PreferenciaNotificaciones {
    override fun activas(): Boolean = valor
    override fun guardar(activas: Boolean) {
        valor = activas
    }
}
