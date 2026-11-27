package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Un lote con TODOS sus sublotes (también los agotados), leído en una sola transacción. No es una tabla: es el
 * resultado de [LoteDao.observarConSublotes]. El orden de [sublotes] no está garantizado: quien lo usa los ordena.
 */
data class LoteConSublotesFila(
    @Embedded val lote: LoteEntity,
    @Relation(parentColumn = "id", entityColumn = "lote_id") val sublotes: List<SubloteEntity>,
)

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

    /**
     * Los lotes con sus sublotes en UNA lectura: Room consulta las dos tablas dentro de la misma transacción y
     * vuelve a emitir cuando cambia cualquiera de ellas. Así nunca llega un lote nuevo sin el sublote que se guardó
     * con él (lo que sí podía pasar juntando observarTodos() de lotes y de sublotes con combine).
     */
    @Transaction
    @Query("SELECT * FROM lote ORDER BY id")
    fun observarConSublotes(): Flow<List<LoteConSublotesFila>>
}
