package com.cafesmart.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocioDao {
    /** Inserta el negocio o lo reemplaza si ya existe uno con el mismo uid. */
    @Upsert
    suspend fun guardar(negocio: NegocioEntity)

    /** Negocio de la cuenta; emite de nuevo cada vez que cambia. null si no hay. */
    @Query("SELECT * FROM negocio WHERE uid = :uid LIMIT 1")
    fun observar(uid: String): Flow<NegocioEntity?>
}
