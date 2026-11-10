package com.cafesmart.app.data.negocio

import android.content.Context
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.NegocioDao
import com.cafesmart.app.data.local.NegocioEntity
import kotlinx.coroutines.flow.Flow

// Acceso al negocio guardado. Es una interfaz para probar con JUnit sin Room (versión falsa en las pruebas).
interface NegocioRepository {
    suspend fun guardar(negocio: NegocioEntity)
    fun observar(uid: String): Flow<NegocioEntity?>

    companion object {
        /** Versión real, sobre Room. */
        fun de(context: Context): NegocioRepository =
            RoomNegocioRepository(AppDatabase.obtener(context).negocioDao())
    }
}

class RoomNegocioRepository(private val dao: NegocioDao) : NegocioRepository {
    override suspend fun guardar(negocio: NegocioEntity) = dao.guardar(negocio)
    override fun observar(uid: String): Flow<NegocioEntity?> = dao.observar(uid)
}
