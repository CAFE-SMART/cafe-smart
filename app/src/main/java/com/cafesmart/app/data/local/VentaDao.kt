package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VentaDao {
    /** Inserta y devuelve el id que asignó Room. Falla si rompe una regla de la tabla (llave foránea). */
    @Insert
    suspend fun insertar(venta: VentaEntity): Long

    /** Inserta un renglón del detalle (un sublote vendido). Falla si repite (venta, sublote) o rompe una llave foránea. */
    @Insert
    suspend fun insertarDetalle(item: VentaSubloteEntity)

    /** Todas las ventas, de la más antigua a la más nueva. */
    @Query("SELECT * FROM venta ORDER BY id")
    suspend fun listar(): List<VentaEntity>
}
