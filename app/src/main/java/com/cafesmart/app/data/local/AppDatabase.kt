package com.cafesmart.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Base de datos local de la app. Tiene Negocio, Productor, Compra, Lote y Sublote. Las demás
// entidades (Gasto, Secado, Venta...) se agregan aquí mismo, en la lista entities, con su DAO abstracto.
// Al agregar entidades o cambiar columnas hay que subir version. Mientras la app no esté publicada,
// basta con desinstalarla en el emulador; después habrá que escribir una migración.

@Database(
    entities = [
        NegocioEntity::class,
        ProductorEntity::class,
        CompraEntity::class,
        LoteEntity::class,
        SubloteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun negocioDao(): NegocioDao
    abstract fun productorDao(): ProductorDao
    abstract fun compraDao(): CompraDao
    abstract fun loteDao(): LoteDao
    abstract fun subloteDao(): SubloteDao

    companion object {
        private const val NOMBRE = "cafesmart.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /** Una sola instancia para toda la app (abrir la base es costoso). */
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NOMBRE)
                    .build()
                    .also { instancia = it }
            }
    }
}
