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

    /**
     * Resta kg al peso disponible del sublote y devuelve cuántas filas cambió (0 o 1).
     * 0 significa que no alcanzaba el café: el repositorio debe fallar para deshacer la transacción.
     *
     * Los kg son Double y restar decimales deja ruido (0.1 + 0.2 no es exacto 0.3). Por eso el resultado
     * se redondea a 3 decimales, que es la precisión del peso en la app. Por la misma razón se permite
     * 0.0005 de tolerancia (EPSILON_KG del modelo): si faltan menos de 0.0005 kg, es ruido y no falta café.
     * MAX(0.0, ...) evita que quede un valor negativo.
     */
    @Query(
        "UPDATE sublote SET peso_disponible_kg = MAX(0.0, ROUND(peso_disponible_kg - :kg, 3)) " +
            "WHERE id = :id AND peso_disponible_kg >= :kg - 0.0005",
    )
    suspend fun descontar(id: Long, kg: Double): Int
}
