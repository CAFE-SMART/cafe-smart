package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubloteDao {
    /** Inserta y devuelve el id que asignó Room. Falla si rompe una regla de la tabla (unicidad o llave foránea). */
    @Insert
    suspend fun insertar(sublote: SubloteEntity): Long

    /** Todas las filas, de la más antigua a la más nueva. */
    @Query("SELECT * FROM sublote ORDER BY id")
    suspend fun listar(): List<SubloteEntity>

    /** La fila con ese id, o null si no existe. */
    @Query("SELECT * FROM sublote WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): SubloteEntity?

    /** Todos los sublotes (también los agotados); emite de nuevo cada vez que la tabla cambia. */
    @Query("SELECT * FROM sublote ORDER BY id")
    fun observarTodos(): Flow<List<SubloteEntity>>
}
