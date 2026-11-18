package com.cafesmart.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.RoomPurchaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Inventario conectado a Room, con Room real (necesita emulador o celular; no corre en el CI):
// observarTodos() de los dos DAOs y que una compra confirmada llegue sola a quien está escuchando.
@RunWith(AndroidJUnit4::class)
class InventarioRoomTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private val fecha = Fecha(dia = 10, mes = 11, anio = 2026)

    @Before
    fun abrirBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    private fun compras() = RoomPurchaseRepository(
        db.productorDao(), db.compraDao(), db.loteDao(), db.subloteDao(), TransaccionRoom(db),
    )

    private fun inventario() = RoomInventoryRepository(db.loteDao(), db.subloteDao())

    private fun cafe(tipo: String, calidad: Calidad, kg: Double) = CafeValidado(tipo, calidad, kg, 14_000.0)

    @Test
    fun sinNadaGuardadoEmiteUnaListaVacia() = runBlocking {
        val lotes = withTimeout(5_000) { inventario().observarLotes().first() }
        assertTrue(lotes.isEmpty())
    }

    @Test
    fun observarTodosDevuelveLasFilasOrdenadasPorId() = runBlocking {
        val verde = db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name))
        val seco = db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.SECO.name, calidad = Calidad.MALA.name))

        val lotes = withTimeout(5_000) { db.loteDao().observarTodos().first() }
        assertEquals(listOf(verde, seco), lotes.map { it.id })
    }

    @Test
    fun losDoceTiposYCalidadesSeLeenBienDespuesDeGuardarlosConCompras() = runBlocking {
        // Compras guarda el nombre del enum; Inventario lo lee con valueOf. Si alguno no coincidiera, fallaría aquí.
        for (tipo in TipoCafe.entries) {
            for (calidad in Calidad.entries) {
                compras().registrarCompra(null, fecha, listOf(cafe(tipo.etiqueta, calidad, 10.0)))
            }
        }

        val lotes = withTimeout(5_000) { inventario().observarLotes().first() }
        assertEquals(TipoCafe.entries.size * Calidad.entries.size, lotes.size)
        assertTrue(lotes.all { it.cantidadSublotes == 1 && it.pesoDisponibleKg == 10.0 })
    }

    @Test
    fun unaCompraConfirmadaLlegaSolaAQuienEstaEscuchando() = runBlocking {
        val recibidos = mutableListOf<List<LoteConSublotes>>()
        val escucha = launch { inventario().observarLotes().toList(recibidos) }

        // Primera lectura: la base está vacía.
        withTimeout(5_000) { while (recibidos.isEmpty()) delay(10) }
        assertTrue(recibidos.first().isEmpty())

        // Se confirma una compra con la escucha abierta, sin volver a pedir nada.
        compras().registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0)))

        // Room avisa por separado de la tabla lote y de la tabla sublote, así que puede llegar antes una lectura
        // con el lote y sin sublotes. Se espera la que ya trae el sublote.
        val conSublote = { lotes: List<LoteConSublotes> -> lotes.any { it.sublotes.isNotEmpty() } }
        withTimeout(5_000) { while (recibidos.none(conSublote)) delay(10) }
        escucha.cancel()

        val grupo = recibidos.last(conSublote).single()
        assertEquals(TipoCafe.VERDE, grupo.lote.tipoCafe)
        assertEquals(Calidad.BUENA, grupo.lote.calidad)
        assertEquals(25.0, grupo.pesoDisponibleKg, 0.0)
        assertEquals(fecha, grupo.sublotes.single().fechaIngreso)
    }
}
