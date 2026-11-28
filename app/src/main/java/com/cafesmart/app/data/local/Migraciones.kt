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

/**
 * 4 → 5: tablas de Secado y llave foránea de sublote.secado_id y gasto.secado_id hacia secado.
 *
 * Las columnas secado_id ya existían desde la versión 3, pero sin tabla secado ni llave foránea. Antes de la
 * versión 5 ningún código guardaba un valor ahí (la semilla registra el café seco como compra y Gastos no asigna
 * secado), así que en una base real deben estar vacías. Si alguna fila trae un valor, no apunta a ningún secado:
 * - se copia a migracion_5_secado_huerfano (tabla, fila_id, secado_id) para no perder el dato, y
 * - solo esa referencia queda en NULL (no se inventan secados). Qué hacer con ella es decisión del equipo.
 *
 * SQLite no agrega una llave foránea con ALTER TABLE: sublote y gasto se reconstruyen (crear nueva, copiar con
 * los mismos ids, borrar, renombrar, índices). Al final se revisan todas las llaves (PRAGMA foreign_key_check):
 * si alguna quedó rota se lanza una excepción y Room deshace la migración completa, sin tocar los datos.
 */
val MIGRACION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Si la conexión tuviera las llaves activas, borrar la tabla padre fallaría por las filas hijas
        // (venta_sublote, gasto_sublote): se difieren hasta el final de la transacción.
        db.execSQL("PRAGMA defer_foreign_keys = ON")

        db.execSQL("CREATE TABLE IF NOT EXISTS `secado` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha_inicio` INTEGER NOT NULL, `fecha_fin` INTEGER, `estado` TEXT NOT NULL, `factor_rendimiento` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `secado_sublote` (`secado_id` INTEGER NOT NULL, `sublote_id` INTEGER NOT NULL, `peso_enviado_kg` REAL NOT NULL, PRIMARY KEY(`secado_id`, `sublote_id`), FOREIGN KEY(`secado_id`) REFERENCES `secado`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`sublote_id`) REFERENCES `sublote`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_secado_sublote_sublote_id` ON `secado_sublote` (`sublote_id`)")

        // Referencias a un secado que no existe: se guardan antes de quitarlas.
        db.execSQL("CREATE TABLE IF NOT EXISTS `migracion_5_secado_huerfano` (`tabla` TEXT NOT NULL, `fila_id` INTEGER NOT NULL, `secado_id` INTEGER NOT NULL, PRIMARY KEY(`tabla`, `fila_id`))")
        db.execSQL("INSERT OR IGNORE INTO `migracion_5_secado_huerfano` SELECT 'sublote', `id`, `secado_id` FROM `sublote` WHERE `secado_id` IS NOT NULL AND `secado_id` NOT IN (SELECT `id` FROM `secado`)")
        db.execSQL("INSERT OR IGNORE INTO `migracion_5_secado_huerfano` SELECT 'gasto', `id`, `secado_id` FROM `gasto` WHERE `secado_id` IS NOT NULL AND `secado_id` NOT IN (SELECT `id` FROM `secado`)")

        val columnasSublote = "`id`, `lote_id`, `compra_id`, `secado_id`, `peso_inicial_kg`, `peso_disponible_kg`, `costo_kg`, `fecha_ingreso`, `humedad_porcentaje`, `factor_rendimiento`"
        db.execSQL("CREATE TABLE `_sublote_v5` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lote_id` INTEGER NOT NULL, `compra_id` INTEGER, `secado_id` INTEGER, `peso_inicial_kg` REAL NOT NULL, `peso_disponible_kg` REAL NOT NULL, `costo_kg` INTEGER NOT NULL, `fecha_ingreso` INTEGER NOT NULL, `humedad_porcentaje` REAL, `factor_rendimiento` INTEGER, FOREIGN KEY(`lote_id`) REFERENCES `lote`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`compra_id`) REFERENCES `compra`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`secado_id`) REFERENCES `secado`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        // Se copia todo igual; solo la referencia huérfana (ya respaldada) queda en NULL.
        db.execSQL("INSERT INTO `_sublote_v5` ($columnasSublote) SELECT `id`, `lote_id`, `compra_id`, CASE WHEN `secado_id` IN (SELECT `id` FROM `secado`) THEN `secado_id` ELSE NULL END, `peso_inicial_kg`, `peso_disponible_kg`, `costo_kg`, `fecha_ingreso`, `humedad_porcentaje`, `factor_rendimiento` FROM `sublote`")
        db.execSQL("DROP TABLE `sublote`")
        db.execSQL("ALTER TABLE `_sublote_v5` RENAME TO `sublote`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sublote_lote_id` ON `sublote` (`lote_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sublote_compra_id` ON `sublote` (`compra_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sublote_secado_id` ON `sublote` (`secado_id`)")

        db.execSQL("CREATE TABLE `_gasto_v5` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `concepto` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `tipo` TEXT NOT NULL, `monto` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `estado_pago` TEXT NOT NULL, `aplica_a` TEXT NOT NULL, `secado_id` INTEGER, FOREIGN KEY(`secado_id`) REFERENCES `secado`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        db.execSQL("INSERT INTO `_gasto_v5` (`id`, `concepto`, `descripcion`, `tipo`, `monto`, `fecha`, `estado_pago`, `aplica_a`, `secado_id`) SELECT `id`, `concepto`, `descripcion`, `tipo`, `monto`, `fecha`, `estado_pago`, `aplica_a`, CASE WHEN `secado_id` IN (SELECT `id` FROM `secado`) THEN `secado_id` ELSE NULL END FROM `gasto`")
        db.execSQL("DROP TABLE `gasto`")
        db.execSQL("ALTER TABLE `_gasto_v5` RENAME TO `gasto`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gasto_secado_id` ON `gasto` (`secado_id`)")

        // Ninguna llave rota (compra, venta, gasto_sublote, venta_sublote, secado): si hay, se deshace todo.
        db.query("PRAGMA foreign_key_check").use { c ->
            check(!c.moveToFirst()) { "Migración 4→5: llave foránea rota en la tabla ${c.getString(0)}; no se aplicó ningún cambio." }
        }
    }
}

/** Todas las migraciones, en orden. 3 → 5 encadena las dos. */
val MIGRACIONES: Array<Migration> = arrayOf(MIGRACION_3_4, MIGRACION_4_5)
