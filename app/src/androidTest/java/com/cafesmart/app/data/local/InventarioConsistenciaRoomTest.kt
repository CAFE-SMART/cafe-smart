package com.cafesmart.app.data.local

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.RoomPurchaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

// C3: Inventario leía lotes y sublotes con dos Flows juntados con combine() y, tras una compra, podía llegar el lote
// nuevo sin su sublote. observarConSublotes() lee las dos tablas en una transacción. Con Room real (emulador).
@RunWith(AndroidJUnit4::class)
class InventarioConsistenciaRoomTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Primero se esperan las lecturas en curso y después se cierra la base (cancel() + close() seguidos tumba el proceso).
    @After
    fun cerrar() {
        runBlocking { alcance.coroutineContext.job.cancelAndJoin() }
        db.close()
    }

    @Test
    fun laConsultaDeInventarioNuncaEntregaUnLoteSinSuSublote() = runBlocking {
        val compras = RoomPurchaseRepository(db.productorDao(), db.compraDao(), db.loteDao(), db.subloteDao(), TransaccionRoom(db))
        val inventario = RoomInventoryRepository(db.loteDao(), db.subloteDao())
        val nuevas = Collections.synchronizedList(mutableListOf<Pair<Int, Int>>())
        val anteriores = Collections.synchronizedList(mutableListOf<Pair<Int, Int>>())
        alcance.launch { inventario.observarLotes().collect { l -> nuevas += l.size to l.sumOf { it.sublotes.size } } }
        alcance.launch {
            combine(db.loteDao().observarTodos(), db.subloteDao().observarTodos()) { l, s -> l.size to s.size }
                .collect { anteriores += it }
        }
        withTimeout(5_000) { while (nuevas.isEmpty() || anteriores.isEmpty()) delay(20) }

        // 12 compras seguidas, cada una crea un lote nuevo con un sublote.
        for (tipo in TipoCafe.entries) for (calidad in Calidad.entries) {
            compras.registrarCompra(null, Fecha(10, 11, 2026), listOf(CafeValidado(tipo.etiqueta, calidad, 5.0, 14_000.0)))
        }
        withTimeout(10_000) { while (nuevas.last() != 12 to 12 || anteriores.last() != 12 to 12) delay(20) }

        val inconsistentesNuevas = nuevas.count { it.first != it.second }
        val inconsistentesAnteriores = anteriores.count { it.first != it.second }
        Log.i("InventarioC3", "nuevas=${nuevas.size} inconsistentes=$inconsistentesNuevas; " +
            "combine=${anteriores.size} inconsistentes=$inconsistentesAnteriores")
        assertEquals("la consulta nueva entregó lotes sin su sublote: $nuevas", 0, inconsistentesNuevas)
    }
}
