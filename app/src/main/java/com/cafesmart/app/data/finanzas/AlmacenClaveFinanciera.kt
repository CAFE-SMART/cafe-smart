package com.cafesmart.app.data.finanzas

import android.content.Context

// Dónde se guarda la clave financiera de cada cuenta (hash y sal, nunca la clave). Con una interfaz la lógica
// se prueba sin Android. La clave es del celular: si se reinstala la app se pierde y se vuelve a crear.

interface AlmacenClaveFinanciera {
    /** La clave guardada de esa cuenta, o null si todavía no creó una (primer acceso). */
    fun leer(uid: String): ClaveGuardada?

    fun guardar(uid: String, clave: ClaveGuardada)

    fun borrar(uid: String)

    /** Momentos (en milisegundos) de las claves incorrectas recientes de esa cuenta. Sobreviven a cerrar la app. */
    fun leerFallos(uid: String): List<Long>

    fun guardarFallos(uid: String, fallos: List<Long>)
}

/** Versión real, sobre SharedPreferences. */
class AlmacenClaveFinancieraLocal(context: Context) : AlmacenClaveFinanciera {

    private val prefs = context.applicationContext.getSharedPreferences("finanzas", Context.MODE_PRIVATE)

    override fun leer(uid: String): ClaveGuardada? {
        val hash = prefs.getString("$uid.hash", null) ?: return null
        val sal = prefs.getString("$uid.sal", null) ?: return null
        val algoritmo = prefs.getString("$uid.algoritmo", null) ?: return null
        return ClaveGuardada(algoritmo, prefs.getInt("$uid.iteraciones", 0), sal, hash)
    }

    override fun guardar(uid: String, clave: ClaveGuardada) {
        prefs.edit()
            .putString("$uid.algoritmo", clave.algoritmo)
            .putInt("$uid.iteraciones", clave.iteraciones)
            .putString("$uid.sal", clave.sal)
            .putString("$uid.hash", clave.hash)
            .apply()
    }

    override fun borrar(uid: String) {
        prefs.edit()
            .remove("$uid.algoritmo").remove("$uid.iteraciones").remove("$uid.sal").remove("$uid.hash")
            .apply()
    }

    override fun leerFallos(uid: String): List<Long> =
        prefs.getString("$uid.fallos", null)?.split(",")?.mapNotNull { it.toLongOrNull() }.orEmpty()

    override fun guardarFallos(uid: String, fallos: List<Long>) {
        prefs.edit().putString("$uid.fallos", fallos.joinToString(",")).apply()
    }
}

/** Versión en memoria para pruebas. */
class AlmacenClaveFinancieraEnMemoria : AlmacenClaveFinanciera {
    private val datos = mutableMapOf<String, ClaveGuardada>()
    override fun leer(uid: String): ClaveGuardada? = datos[uid]
    override fun guardar(uid: String, clave: ClaveGuardada) {
        datos[uid] = clave
    }
    override fun borrar(uid: String) {
        datos.remove(uid)
    }
    private val fallos = mutableMapOf<String, List<Long>>()
    override fun leerFallos(uid: String): List<Long> = fallos[uid].orEmpty()
    override fun guardarFallos(uid: String, fallos: List<Long>) {
        this.fallos[uid] = fallos
    }
}
