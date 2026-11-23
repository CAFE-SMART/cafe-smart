package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductorDao {
    /** Inserta y devuelve el id que asignó Room. Falla si rompe una regla de la tabla (unicidad o llave foránea). */
    @Insert
    suspend fun insertar(productor: ProductorEntity): Long

    /** Todas las filas, de la más antigua a la más nueva. */
    @Query("SELECT * FROM productor ORDER BY id")
    suspend fun listar(): List<ProductorEntity>

    /** La fila con ese id, o null si no existe. */
    @Query("SELECT * FROM productor WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ProductorEntity?

    /**
     * El productor con ese documento, o null si no hay. Se busca antes de guardar para que la pantalla
     * muestre "Este documento ya está registrado" y no una falla del índice único.
     */
    @Query("SELECT * FROM productor WHERE documento = :documento LIMIT 1")
    suspend fun buscarPorDocumento(documento: String): ProductorEntity?

    /** Reemplaza los datos del productor con ese id (la hoja "Productores" permite modificarlos). */
    @Update
    suspend fun actualizar(productor: ProductorEntity)
}
