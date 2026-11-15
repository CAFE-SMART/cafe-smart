package com.cafesmart.app.data.local

import androidx.room.RoomDatabase
import androidx.room.withTransaction

// Ejecuta un bloque como una sola transacción: si algo falla (lanza una excepción), no queda nada a medias.
// Es una interfaz para probar el repositorio con JUnit sin Room (en las pruebas se pasa una versión falsa).
interface Transaccion {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}

/** Versión real: la transacción de Room (se deshace sola si el bloque lanza una excepción). */
class TransaccionRoom(private val base: RoomDatabase) : Transaccion {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T = base.withTransaction(bloque)
}
