package com.cafesmart.app.data.seed

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.ProductorEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Prueba con Room real (necesita emulador o celular; no corre en el CI). Base en memoria:
// no toca cafesmart.db. Corre sobre debug, donde existe la semilla.
@RunWith(AndroidJUnit4::class)
class SemillaDeEjemploTest {

    private lateinit var db: AppDatabase

    @Before
    fun abrirBase() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun enUnaBaseVaciaCargaProductoresComprasLotesYSublotes() = runBlocking {
        assertTrue(sembrarDatosDeEjemplo(db))

        assertEquals(PRODUCTORES_SEMILLA.size, db.productorDao().listar().size)
        assertEquals(COMPRAS_SEMILLA.size, db.compraDao().listar().size)
        assertEquals(7, db.loteDao().listar().size) // sin lote Pasilla: no tiene sublotes
        val sublotes = db.subloteDao().listar()
        assertEquals(10, sublotes.size)

        // Todos nacen de una compra que existe, y los kg en bodega son los del Figma (834).
        val idsCompra = db.compraDao().listar().map { it.id }.toSet()
        assertTrue(sublotes.all { it.compraId in idsCompra && it.secadoId == null })
        assertEquals(834.0, sublotes.sumOf { it.pesoDisponibleKg }, 0.0)

        // Hay una compra "Sin registrar" (productor nulo) y el total pagado de la compra de dos cafés es la suma.
        assertTrue(db.compraDao().listar().any { it.productorId == null })
        val dosCafes = db.compraDao().listar().first { c -> sublotes.count { it.compraId == c.id } == 2 }
        assertEquals(100 * 13_500L + 100 * 21_000L, dosCafes.totalPagado)
    }

    @Test
    fun noSiembraDosVeces() = runBlocking {
        assertTrue(sembrarDatosDeEjemplo(db))
        assertFalse(sembrarDatosDeEjemplo(db))

        assertEquals(10, db.subloteDao().listar().size)
    }

    @Test
    fun noPisaUnaBaseQueYaTieneDatos() = runBlocking {
        db.productorDao().insertar(ProductorEntity(nombre = "Real Productor", documento = "123456", tipoDocumento = "CC"))

        assertFalse(sembrarDatosDeEjemplo(db))

        assertEquals(1, db.productorDao().listar().size)
        assertTrue(db.subloteDao().listar().isEmpty())
    }
}
