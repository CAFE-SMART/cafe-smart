package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LoteDao {
    /** Inserta y devuelve el id que asignó Room. Falla si rompe una regla de la tabla (unicidad o llave foránea). */
    @Insert
    suspend fun insertar(lote: LoteEntity): Long

    /** Todas las filas, de la más antigua a la más nueva. */
    @Query("SELECT * FROM lote ORDER BY id")
    suspend fun listar(): List<LoteEntity>

    /** La fila con ese id, o null si no existe. */
    @Query("SELECT * FROM lote WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): LoteEntity?
}
