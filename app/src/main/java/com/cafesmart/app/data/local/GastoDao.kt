package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Un gasto con la parte que le toca a un sublote (fila de la pantalla "Gastos del sublote"). */
data class GastoDelSubloteFila(
    @Embedded val gasto: GastoEntity,
    @ColumnInfo(name = "sublote_id") val subloteId: Long,
    @ColumnInfo(name = "monto_asignado") val montoAsignado: Long,
)

// Clase abstracta (y no interfaz) para poder tener guardarConReparto, que junta dos inserciones en una transacción.
@Dao
abstract class GastoDao {

    /** Inserta y devuelve el id que asignó Room. */
    @Insert
    abstract suspend fun insertar(gasto: GastoEntity): Long

    @Insert
    abstract suspend fun insertarReparto(filas: List<GastoSubloteEntity>)

    /**
     * Guarda el gasto y su reparto en una sola transacción: o entra todo o no entra nada (si un sublote
     * del reparto no existe, la llave foránea falla y el gasto tampoco se guarda). El id del gasto lo
     * asigna Room: se ignora el que traiga [gasto] y las filas del reparto se amarran al nuevo id.
     */
    @Transaction
    open suspend fun guardarConReparto(gasto: GastoEntity, reparto: List<GastoSubloteEntity>): Long {
        val id = insertar(gasto.copy(id = 0))
        if (reparto.isNotEmpty()) insertarReparto(reparto.map { it.copy(gastoId = id) })
        return id
    }

    /** Todos los gastos, del más reciente al más antiguo (a igual fecha, el último que se registró primero). */
    @Query("SELECT * FROM gasto ORDER BY fecha DESC, id DESC")
    abstract fun observarGastos(): Flow<List<GastoEntity>>

    /** Lo mismo que [observarGastos], pero una sola vez (para el Resumen y el Historial financiero). */
    @Query("SELECT * FROM gasto ORDER BY fecha DESC, id DESC")
    abstract suspend fun listarGastos(): List<GastoEntity>

    /** Todas las filas de reparto, de todos los gastos. */
    @Query("SELECT * FROM gasto_sublote")
    abstract suspend fun todosLosRepartos(): List<GastoSubloteEntity>

    /** El reparto de un gasto, ordenado por sublote. */
    @Query("SELECT * FROM gasto_sublote WHERE gasto_id = :gastoId ORDER BY sublote_id")
    abstract suspend fun repartoDe(gastoId: Long): List<GastoSubloteEntity>

    /** Los gastos de un sublote con lo que le tocó a ese sublote (no el monto completo del gasto). */
    @Query(
        "SELECT g.*, gs.sublote_id AS sublote_id, gs.monto_asignado AS monto_asignado " +
            "FROM gasto_sublote gs INNER JOIN gasto g ON g.id = gs.gasto_id " +
            "WHERE gs.sublote_id = :subloteId ORDER BY g.fecha DESC, g.id DESC"
    )
    abstract fun observarDelSublote(subloteId: Long): Flow<List<GastoDelSubloteFila>>

    // Las dos consultas siguientes leen la tabla sublote desde aquí a propósito: así Gastos no depende
    // de SubloteDao, que es de Inventario y puede cambiar.

    /** Sublotes con café disponible (más de [minimoKg]), los que se pueden elegir al asociar un gasto. */
    @Query("SELECT * FROM sublote WHERE peso_disponible_kg > :minimoKg ORDER BY id")
    abstract fun observarSublotesConExistencia(minimoKg: Double): Flow<List<SubloteEntity>>

    /** Un sublote por su id, aunque esté agotado (sigue teniendo gastos que mostrar). */
    @Query("SELECT * FROM sublote WHERE id = :id LIMIT 1")
    abstract suspend fun buscarSublote(id: Long): SubloteEntity?
}
