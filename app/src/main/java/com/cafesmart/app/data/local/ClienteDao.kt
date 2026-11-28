package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClienteDao {
    /** Inserta y devuelve el id que asignó Room. Falla si rompe una regla de la tabla (unicidad o llave foránea). */
    @Insert
    suspend fun insertar(cliente: ClienteEntity): Long

    /** Todas las filas, de la más antigua a la más nueva. */
    @Query("SELECT * FROM cliente ORDER BY id")
    suspend fun listar(): List<ClienteEntity>

    /** La fila con ese id, o null si no existe. */
    @Query("SELECT * FROM cliente WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ClienteEntity?

    /**
     * El cliente con ese documento, o null si no hay. Se busca antes de guardar para que la pantalla
     * muestre "Este documento ya está registrado" y no una falla del índice único.
     */
    @Query("SELECT * FROM cliente WHERE documento = :documento LIMIT 1")
    suspend fun buscarPorDocumento(documento: String): ClienteEntity?

    /** Reemplaza los datos del cliente con ese id. */
    @Update
    suspend fun actualizar(cliente: ClienteEntity)
}
