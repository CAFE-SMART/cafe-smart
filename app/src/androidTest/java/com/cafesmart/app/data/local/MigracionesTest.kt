package com.cafesmart.app.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Migración 3 → 4 con una base de prueba (nunca la de la app): se arma la base vieja con el mismo SQL
// que generaba Room en esa versión, se le ponen compras, sublotes y gastos ficticios y se abre con la versión
// actual. Room valida que el esquema final sea idéntico al de las entidades; si no, la prueba falla.
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "migraciones-prueba-${System.nanoTime()}.db"


    private val esquemaV3 = listOf(
        "CREATE TABLE IF NOT EXISTS `negocio` (`uid` TEXT NOT NULL, `nombre` TEXT NOT NULL, `tipo` TEXT NOT NULL, `telefono` TEXT NOT NULL, `capacidad_bodega_kg` REAL NOT NULL, `peso_bulto_kg` REAL NOT NULL, `creado_en` INTEGER NOT NULL, PRIMARY KEY(`uid`))",
        "CREATE TABLE IF NOT EXISTS `productor` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `documento` TEXT NOT NULL, `tipo_documento` TEXT NOT NULL, `telefono` TEXT NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_productor_documento` ON `productor` (`documento`)",
        "CREATE TABLE IF NOT EXISTS `compra` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productor_id` INTEGER, `fecha` INTEGER NOT NULL, `total_pagado` INTEGER NOT NULL, FOREIGN KEY(`productor_id`) REFERENCES `productor`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_compra_productor_id` ON `compra` (`productor_id`)",
        "CREATE TABLE IF NOT EXISTS `lote` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tipo_cafe` TEXT NOT NULL, `calidad` TEXT NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_lote_tipo_cafe_calidad` ON `lote` (`tipo_cafe`, `calidad`)",
        "CREATE TABLE IF NOT EXISTS `sublote` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lote_id` INTEGER NOT NULL, `compra_id` INTEGER, `secado_id` INTEGER, `peso_inicial_kg` REAL NOT NULL, `peso_disponible_kg` REAL NOT NULL, `costo_kg` INTEGER NOT NULL, `fecha_ingreso` INTEGER NOT NULL, `humedad_porcentaje` REAL, `factor_rendimiento` INTEGER, FOREIGN KEY(`lote_id`) REFERENCES `lote`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`compra_id`) REFERENCES `compra`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_sublote_lote_id` ON `sublote` (`lote_id`)",
        "CREATE INDEX IF NOT EXISTS `index_sublote_compra_id` ON `sublote` (`compra_id`)",
        "CREATE INDEX IF NOT EXISTS `index_sublote_secado_id` ON `sublote` (`secado_id`)",
        "CREATE TABLE IF NOT EXISTS `gasto` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `concepto` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `tipo` TEXT NOT NULL, `monto` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `estado_pago` TEXT NOT NULL, `aplica_a` TEXT NOT NULL, `secado_id` INTEGER)",
        "CREATE INDEX IF NOT EXISTS `index_gasto_secado_id` ON `gasto` (`secado_id`)",
        "CREATE TABLE IF NOT EXISTS `gasto_sublote` (`gasto_id` INTEGER NOT NULL, `sublote_id` INTEGER NOT NULL, `monto_asignado` INTEGER NOT NULL, PRIMARY KEY(`gasto_id`, `sublote_id`), FOREIGN KEY(`gasto_id`) REFERENCES `gasto`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`sublote_id`) REFERENCES `sublote`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_gasto_sublote_sublote_id` ON `gasto_sublote` (`sublote_id`)",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
    )

    // Datos ficticios de una instalación con la versión 3: 834 kg en dos sublotes, una compra y un gasto repartido.
    private fun datosV3(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO negocio VALUES ('uid-qa', 'Negocio QA', 'COMPRAVENTA', '3000000000', 5000.0, 40.7, 1700000000000)")
        db.execSQL("INSERT INTO productor (id, nombre, documento, tipo_documento, telefono) VALUES (1, 'Productor QA', '1000000001', 'CC', '')")
        db.execSQL("INSERT INTO compra (id, productor_id, fecha, total_pagado) VALUES (1, 1, 1700000000000, 11676000)")
        db.execSQL("INSERT INTO lote (id, tipo_cafe, calidad) VALUES (1, 'VERDE', 'BUENA'), (2, 'PERGAMINO', 'BUENA')")
        db.execSQL("INSERT INTO sublote (id, lote_id, compra_id, peso_inicial_kg, peso_disponible_kg, costo_kg, fecha_ingreso, humedad_porcentaje, factor_rendimiento) VALUES (1, 1, 1, 500.0, 500.0, 14000, 1700000000000, 52.0, NULL), (2, 2, 1, 334.0, 334.0, 14000, 1700000000000, NULL, 92)")
        db.execSQL("INSERT INTO gasto (id, concepto, descripcion, tipo, monto, fecha, estado_pago, aplica_a, secado_id) VALUES (1, 'Transporte', '', 'TRANSPORTE', 120000, 1700000000000, 'PAGADO', 'SUBLOTES', NULL)")
        db.execSQL("INSERT INTO gasto_sublote (gasto_id, sublote_id, monto_asignado) VALUES (1, 1, 72000), (1, 2, 48000)")
    }

    private fun crearBase(version: Int, extra: (SQLiteDatabase) -> Unit = {}) {
        val db = SQLiteDatabase.openOrCreateDatabase(contexto.getDatabasePath(nombre).apply { parentFile?.mkdirs() }, null)
        esquemaV3.forEach(db::execSQL)
        datosV3(db)
        extra(db)
        db.version = version
        db.close()
    }

    private fun abrirActual() = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre)
        .addMigrations(MIGRACION_3_4)
        .allowMainThreadQueries()
        .build()

    private fun comprobarDatosV3(db: AppDatabase) = runBlocking {
        assertEquals(1, db.productorDao().listar().size)
        assertEquals(1, db.compraDao().listar().size)
        assertEquals(2, db.loteDao().listar().size)
        val kg = db.subloteDao().listar().sumOf { it.pesoDisponibleKg }
        assertEquals(834.0, kg, 0.0)
        assertEquals(1, db.gastoDao().listarGastos().size)
        // Integridad: ninguna llave foránea rota y PRAGMA integrity_check en ok.
        db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        db.openHelper.writableDatabase.query("PRAGMA integrity_check").use { it.moveToFirst(); assertEquals("ok", it.getString(0)) }
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM gasto_sublote").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
    }

    @Test
    fun de3a4ConservaDatosYAdmiteVentas() {
        crearBase(3)
        val db = abrirActual()
        try {
            comprobarDatosV3(db)
            assertEquals(4, db.openHelper.readableDatabase.version)
            val sql = db.openHelper.writableDatabase
            sql.execSQL("INSERT INTO cliente (id, nombre, documento, tipo_documento, telefono) VALUES (1, 'Cliente QA', '900000001', 'NIT', '')")
            sql.execSQL("INSERT INTO venta (id, cliente_id, fecha, total_recibido) VALUES (1, 1, 1700000100000, 300000)")
            sql.execSQL("INSERT INTO venta_sublote (venta_id, sublote_id, peso_kg, precio_kg) VALUES (1, 2, 20.0, 15000)")
            sql.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            sql.query("SELECT COUNT(*) FROM venta_sublote").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        } finally { db.close() }
    }
}
