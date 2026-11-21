package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    /**
     * El lote de ese tipo y calidad (nombres de los enums TipoCafe y Calidad), o null si no existe.
     * Al registrar una compra se busca antes de insertar: el índice único (tipo_cafe, calidad) impide duplicarlo.
     */
    @Query("SELECT * FROM lote WHERE tipo_cafe = :tipoCafe AND calidad = :calidad LIMIT 1")
    suspend fun buscarPorTipoYCalidad(tipoCafe: String, calidad: String): LoteEntity?

    /**
     * Todos los lotes; emite de nuevo cada vez que la tabla cambia (Inventario se actualiza sin reiniciar).
     * No es suspend: Room corre la consulta en su propio hilo y devuelve un Flow.
     */
    @Query("SELECT * FROM lote ORDER BY id")
    fun observarTodos(): Flow<List<LoteEntity>>
}
