package com.cafesmart.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Ventas en Room, con Room real (necesita emulador o celular; no corre en el CI):
// guardar la venta, descontar kg del sublote y las llaves foráneas / índice único.
@RunWith(AndroidJUnit4::class)
class VentasRoomTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private val fecha = 1_700_000_000_000L

    @Before
    fun abrirBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    // Crea un lote, una compra y un sublote con los kg dados; devuelve el id del sublote.
    private suspend fun crearSublote(kg: Double): Long {
        val loteId = db.loteDao().buscarPorTipoYCalidad(TipoCafe.SECO.name, Calidad.BUENA.name)?.id
            ?: db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.SECO.name, calidad = Calidad.BUENA.name))
        val compraId = db.compraDao().insertar(CompraEntity(fecha = fecha, totalPagado = 0))
        return db.subloteDao().insertar(
            SubloteEntity(
                loteId = loteId,
                compraId = compraId,
                pesoInicialKg = kg,
                pesoDisponibleKg = kg,
                costoKg = 14_000,
                fechaIngreso = fecha,
            ),
        )
    }

    private suspend fun crearCliente(documento: String) = db.clienteDao().insertar(
        ClienteEntity(nombre = "Trilladora Norte", documento = documento, tipoDocumento = TipoDocumento.NIT.name),
    )

    // ── Guardar la venta ───────────────────────────────────

    @Test
    fun guardaUnaVentaConCliente() = runBlocking {
        val clienteId = crearCliente("900123456")
        val subloteId = crearSublote(50.0)
        val ventaId = db.ventaDao().insertar(VentaEntity(clienteId = clienteId, fecha = fecha, totalRecibido = 300_000))
        db.ventaDao().insertarDetalle(VentaSubloteEntity(ventaId, subloteId, pesoKg = 20.0, precioKg = 15_000))

        val ventas = db.ventaDao().listar()
        assertEquals(1, ventas.size)
        assertEquals(clienteId, ventas[0].clienteId)
        assertEquals(300_000L, ventas[0].totalRecibido)
    }

    @Test
    fun guardaUnaVentaSinRegistrarConClienteNulo() = runBlocking {
        val subloteId = crearSublote(50.0)
        val ventaId = db.ventaDao().insertar(VentaEntity(clienteId = null, fecha = fecha, totalRecibido = 150_000))
        db.ventaDao().insertarDetalle(VentaSubloteEntity(ventaId, subloteId, pesoKg = 10.0, precioKg = 15_000))

        assertNull(db.ventaDao().listar().single().clienteId)
    }

    // ── SubloteDao.descontar ───────────────────────────────

    @Test
    fun descontarRestaYDejaTresDecimalesSinRuido() = runBlocking {
        val id = crearSublote(10.0)
        assertEquals(1, db.subloteDao().descontar(id, 2.5))
        assertEquals(7.5, db.subloteDao().buscarPorId(id)?.pesoDisponibleKg ?: -1.0, 0.0)

        // 0.1 + 0.2 en Double da 0.30000000000000004; al descontar queda exactamente 0.3.
        val otro = crearSublote(0.3)
        assertEquals(1, db.subloteDao().descontar(otro, 0.1))
        assertEquals(1, db.subloteDao().descontar(otro, 0.2))
        assertEquals(0.0, db.subloteDao().buscarPorId(otro)?.pesoDisponibleKg ?: -1.0, 0.0)

        val tercero = crearSublote(1.0)
        db.subloteDao().descontar(tercero, 0.1)
        db.subloteDao().descontar(tercero, 0.2)
        assertEquals(0.7, db.subloteDao().buscarPorId(tercero)?.pesoDisponibleKg ?: -1.0, 0.0)
    }

    @Test
    fun descontarMasDeLoDisponibleDevuelveCeroYNoCambiaElSublote() = runBlocking {
        val id = crearSublote(5.0)
        assertEquals(0, db.subloteDao().descontar(id, 5.001))
        assertEquals(5.0, db.subloteDao().buscarPorId(id)?.pesoDisponibleKg ?: -1.0, 0.0)
    }

    // ── Reglas de la tabla ─────────────────────────────────

    @Test
    fun noSePuedeBorrarUnSubloteVendido() = runBlocking {
        val subloteId = crearSublote(50.0)
        val ventaId = db.ventaDao().insertar(VentaEntity(clienteId = null, fecha = fecha, totalRecibido = 150_000))
        db.ventaDao().insertarDetalle(VentaSubloteEntity(ventaId, subloteId, pesoKg = 10.0, precioKg = 15_000))

        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM sublote WHERE id = $subloteId")
            fail("Debía fallar: el sublote ya tiene una venta (RESTRICT)")
        } catch (e: SQLiteConstraintException) {
            // Esperado.
        }
        assertEquals(1, db.subloteDao().listar().size)
    }

    @Test
    fun unDocumentoDeClienteRepetidoFalla() = runBlocking {
        crearCliente("900123456")
        try {
            crearCliente("900123456")
            fail("Debía fallar por el índice único del documento")
        } catch (e: SQLiteConstraintException) {
            // Esperado.
        }
        assertEquals(1, db.clienteDao().listar().size)
    }
}
