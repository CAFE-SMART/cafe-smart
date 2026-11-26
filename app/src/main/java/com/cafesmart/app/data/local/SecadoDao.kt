package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

/** Un secado con sus envíos (los sublotes verdes que se mandaron a secar y cuántos kg de cada uno). */
data class SecadoConEnviosFila(
    @Embedded val secado: SecadoEntity,
    @Relation(parentColumn = "id", entityColumn = "secado_id")
    val envios: List<SecadoSubloteEntity>,
)

// Clase abstracta (y no interfaz) para poder tener guardarConEnvios, que junta dos inserciones en una transacción,
// igual que GastoDao.guardarConReparto.
@Dao
abstract class SecadoDao {

    /** Inserta y devuelve el id que asignó Room. */
    @Insert
    abstract suspend fun insertar(secado: SecadoEntity): Long

    @Insert
    abstract suspend fun insertarEnvios(filas: List<SecadoSubloteEntity>)

    /**
     * Guarda el secado y sus envíos en una sola transacción: o entra todo o no entra nada (si un sublote
     * no existe, la llave foránea falla y el secado tampoco se guarda). El id lo asigna Room: se ignora el
     * que traiga [secado] y los envíos se amarran al nuevo id. Devuelve ese id.
     */
    @Transaction
    open suspend fun guardarConEnvios(secado: SecadoEntity, envios: List<SecadoSubloteEntity>): Long {
        val id = insertar(secado.copy(id = 0))
        if (envios.isNotEmpty()) insertarEnvios(envios.map { it.copy(secadoId = id) })
        return id
    }

    /**
     * Cambia el estado de un secado que sigue EN_CURSO y guarda su fecha de fin y su factor (null si no hay).
     * Devuelve cuántas filas cambió (0 o 1): 0 significa que el secado no existe o ya no estaba EN_CURSO,
     * así no se finaliza ni se cancela dos veces. "EN_CURSO" es el nombre de EstadoSecado.EN_CURSO.
     */
    @Query(
        "UPDATE secado SET estado = :estado, fecha_fin = :fechaFin, factor_rendimiento = :factor " +
            "WHERE id = :id AND estado = 'EN_CURSO'",
    )
    abstract suspend fun cerrar(id: Long, estado: String, fechaFin: Long?, factor: Int?): Int

    /** La fila con ese id, o null si no existe. */
    @Query("SELECT * FROM secado WHERE id = :id LIMIT 1")
    abstract suspend fun buscarPorId(id: Long): SecadoEntity?

    /** El secado con sus envíos, o null si no existe. */
    @Transaction
    @Query("SELECT * FROM secado WHERE id = :id LIMIT 1")
    abstract suspend fun buscarConEnvios(id: Long): SecadoConEnviosFila?

    /** Los secados EN_CURSO con sus envíos, el de inicio más reciente primero (a igual fecha, el último que se creó). */
    @Transaction
    @Query("SELECT * FROM secado WHERE estado = 'EN_CURSO' ORDER BY fecha_inicio DESC, id DESC")
    abstract suspend fun listarEnCurso(): List<SecadoConEnviosFila>

    /** Los secados FINALIZADOS con sus envíos, el de fin más reciente primero (a igual fecha, el último que se creó). */
    @Transaction
    @Query("SELECT * FROM secado WHERE estado = 'FINALIZADO' ORDER BY fecha_fin DESC, id DESC")
    abstract suspend fun listarFinalizados(): List<SecadoConEnviosFila>

    /** Los sublotes secos que resultaron de un secado (secado_id = ese secado); vacío si aún no terminó. */
    @Query("SELECT * FROM sublote WHERE secado_id = :secadoId ORDER BY id")
    abstract suspend fun resultantesDe(secadoId: Long): List<SubloteEntity>
}
