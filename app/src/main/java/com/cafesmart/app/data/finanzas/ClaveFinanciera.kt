package com.cafesmart.app.data.finanzas

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Clave propia para ver las métricas financieras (Ajustes > Ver resumen financiero).
// Nunca se guarda en texto plano: solo un hash con sal (PBKDF2). La sal es distinta en cada clave, así que
// dos personas con la misma clave tienen hashes distintos. Funciones puras: se prueban con JUnit sin Android.

/** Mínimo de caracteres de la clave financiera (propuesta del diseño; el equipo puede cambiarlo). */
const val CLAVE_MIN_CARACTERES = 6

const val ERROR_CLAVE_VACIA = "Escribe tu clave."
const val ERROR_CLAVE_CORTA = "La clave debe tener mínimo $CLAVE_MIN_CARACTERES caracteres."
const val ERROR_CONFIRMACION_VACIA = "Repite tu clave."
const val ERROR_CLAVES_DISTINTAS = "Las claves no son iguales. Revísalas e inténtalo de nuevo."

/** Lo que se guarda de una clave: el algoritmo, las vueltas, la sal y el hash (los dos en hexadecimal). */
data class ClaveGuardada(
    val algoritmo: String,
    val iteraciones: Int,
    val sal: String,
    val hash: String,
)

object ClaveFinanciera {

    private const val ITERACIONES = 120_000
    private const val BITS_DEL_HASH = 256
    private const val BYTES_DE_SAL = 16
    private const val SHA256 = "PBKDF2WithHmacSHA256"
    private const val SHA1 = "PBKDF2WithHmacSHA1"

    /** Crea el registro para guardar: sal nueva al azar y el hash de la clave con esa sal. */
    fun crear(clave: String, azar: SecureRandom = SecureRandom()): ClaveGuardada {
        val sal = ByteArray(BYTES_DE_SAL).also(azar::nextBytes)
        val algoritmo = algoritmoDisponible()
        val hash = derivar(clave, sal, ITERACIONES, algoritmo)
        return ClaveGuardada(algoritmo, ITERACIONES, sal.aHex(), hash.aHex())
    }

    /** true si la clave escrita corresponde a la guardada. La comparación tarda lo mismo acierte o no. */
    fun verificar(clave: String, guardada: ClaveGuardada): Boolean {
        val hash = derivar(clave, guardada.sal.deHex(), guardada.iteraciones, guardada.algoritmo)
        return MessageDigest.isEqual(hash, guardada.hash.deHex())
    }

    // Android 7 (API 24 y 25) no trae PBKDF2 con SHA-256; allí se usa SHA-1. El algoritmo usado queda guardado.
    private fun algoritmoDisponible(): String =
        try {
            SecretKeyFactory.getInstance(SHA256)
            SHA256
        } catch (e: NoSuchAlgorithmException) {
            SHA1
        }

    private fun derivar(clave: String, sal: ByteArray, iteraciones: Int, algoritmo: String): ByteArray {
        val especificacion = PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS_DEL_HASH)
        return try {
            SecretKeyFactory.getInstance(algoritmo).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }
}

/** Error de la clave nueva (vacía o muy corta), o null si sirve. */
fun errorDeClaveNueva(clave: String): String? = when {
    clave.isEmpty() -> ERROR_CLAVE_VACIA
    clave.length < CLAVE_MIN_CARACTERES -> ERROR_CLAVE_CORTA
    else -> null
}

/** Error de la confirmación (vacía o distinta de la clave), o null si coincide. */
fun errorDeConfirmacion(clave: String, confirmacion: String): String? = when {
    confirmacion.isEmpty() -> ERROR_CONFIRMACION_VACIA
    confirmacion != clave -> ERROR_CLAVES_DISTINTAS
    else -> null
}

private fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it) }

private fun String.deHex(): ByteArray = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
