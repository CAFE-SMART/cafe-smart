package com.cafesmart.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Conserva la información de la versión 3 y agrega las tablas de Ventas.
val MIGRACION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cliente` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `documento` TEXT NOT NULL, `tipo_documento` TEXT NOT NULL, `telefono` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cliente_documento` ON `cliente` (`documento`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `venta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cliente_id` INTEGER, `fecha` INTEGER NOT NULL, `total_recibido` INTEGER NOT NULL, FOREIGN KEY(`cliente_id`) REFERENCES `cliente`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_venta_cliente_id` ON `venta` (`cliente_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `venta_sublote` (`venta_id` INTEGER NOT NULL, `sublote_id` INTEGER NOT NULL, `peso_kg` REAL NOT NULL, `precio_kg` INTEGER NOT NULL, PRIMARY KEY(`venta_id`, `sublote_id`), FOREIGN KEY(`venta_id`) REFERENCES `venta`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`sublote_id`) REFERENCES `sublote`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_venta_sublote_venta_id` ON `venta_sublote` (`venta_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_venta_sublote_sublote_id` ON `venta_sublote` (`sublote_id`)")
    }
}
