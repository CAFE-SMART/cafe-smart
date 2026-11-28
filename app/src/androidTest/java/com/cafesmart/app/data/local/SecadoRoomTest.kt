package com.cafesmart.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.RoomSecadoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Secado en Room, con Room real (necesita emulador o celular; no corre en el CI): guardar el secado con sus
// envíos, descontar y devolver kg del sublote, cerrar una sola vez y las llaves foráneas.
@RunWith(AndroidJUnit4::class)
class SecadoRoomTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private val fecha = 1_700_000_000_000L
    private val inicio = Fecha(3, 11, 2026)
    private val fin = Fecha(10, 11, 2026)

    @Before
    fun abrirBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    private fun repositorio() = RoomSecadoRepository(
        secadoDao = db.secadoDao(),
        subloteDao = db.subloteDao(),
        loteDao = db.loteDao(),
        transaccion = TransaccionRoom(db),
    )

    // Un sublote verde SIN compra (compra_id = null) con los kg dados; devuelve su id.
    private suspend fun crearVerde(kg: Double, calidad: Calidad = Calidad.BUENA): Long {
        val loteId = db.loteDao().buscarPorTipoYCalidad(TipoCafe.VERDE.name, calidad.name)?.id
            ?: db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = calidad.name))
        return db.subloteDao().insertar(
            SubloteEntity(
                loteId = loteId, pesoInicialKg = kg, pesoDisponibleKg = kg, costoKg = 14_000, fechaIngreso = fecha,
            ),
        )
    }

    private suspend fun disponible(id: Long) = db.subloteDao().buscarPorId(id)!!.pesoDisponibleKg

    // ── Llaves foráneas ────────────────────────────────────

    @Test
    fun unSubloteSinCompraSeGuardaConLasLlavesForaneasActivas() = runBlocking {
        val id = crearVerde(50.0)
        val sublote = db.subloteDao().buscarPorId(id)!!
        assertNull(sublote.compraId)
        assertNull(sublote.secadoId)
    }

    @Test
    fun guardarConEnviosSinSubloteExistenteNoGuardaElSecado() = runBlocking {
        try {
            db.secadoDao().guardarConEnvios(
                SecadoEntity(fechaInicio = fecha, estado = "EN_CURSO"),
                listOf(SecadoSubloteEntity(secadoId = 0, subloteId = 999, pesoEnviadoKg = 10.0)),
            )
            fail("Debía fallar: el sublote 999 no existe")
        } catch (e: SQLiteConstraintException) {
            // Se espera.
        }
        assertEquals(0, db.secadoDao().listarEnCurso().size)
    }

    // ── Consultas del DAO ──────────────────────────────────

    @Test
    fun cerrarSoloCambiaUnSecadoEnCurso() = runBlocking {
        val sublote = crearVerde(50.0)
        val id = db.secadoDao().guardarConEnvios(
            SecadoEntity(fechaInicio = fecha, estado = "EN_CURSO"),
            listOf(SecadoSubloteEntity(0, sublote, 20.0)),
        )
        assertEquals(1, db.secadoDao().cerrar(id, "FINALIZADO", fecha, 95))
        assertEquals(0, db.secadoDao().cerrar(id, "CANCELADO", null, null))
        val fila = db.secadoDao().buscarPorId(id)!!
        assertEquals("FINALIZADO", fila.estado)
        assertEquals(95, fila.factorRendimiento)
    }

    @Test
    fun devolverSumaKgSinPasarDelPesoInicialYDaCeroSiNoExiste() = runBlocking {
        val id = crearVerde(100.0)
        assertEquals(1, db.subloteDao().descontar(id, 60.0))
        assertEquals(40.0, disponible(id), 0.0)
        assertEquals(1, db.subloteDao().devolver(id, 0.1))
        assertEquals(40.1, disponible(id), 0.0)
        assertEquals(1, db.subloteDao().devolver(id, 500.0))
        assertEquals(100.0, disponible(id), 0.0)   // nunca más que lo que entró
        assertEquals(0, db.subloteDao().devolver(999, 5.0))
    }

    // ── El repositorio completo, con transacciones reales ──

    @Test
    fun iniciarFinalizarYCancelarConRoomReal() = runBlocking {
        val repositorio = repositorio()
        val a = crearVerde(100.0)
        val b = crearVerde(40.0, Calidad.REGULAR)

        // Si el segundo no alcanza, la transacción deshace el secado y el descuento del primero.
        try {
            repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 30.0), EnvioASecar(b, 41.0)))
            fail("Debía fallar por falta de café")
        } catch (e: IllegalArgumentException) {
            // Se espera.
        }
        assertEquals(100.0, disponible(a), 0.0)
        assertEquals(0, repositorio.secadosEnCurso().size)

        val primero = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 100.0)))
        assertEquals(0.0, disponible(a), 0.0)
        assertEquals(listOf(b), repositorio.inventario().map { it.sublote.id })   // a quedó en 0: no se lista

        val detalle = repositorio.finalizarSecado(primero.id, fin, mapOf(Calidad.BUENA to 90.0), 95)
        assertEquals(EstadoSecado.FINALIZADO, detalle.secado.estado)
        assertEquals(10.0, detalle.mermaKg, 0.0)
        val seco = db.secadoDao().resultantesDe(primero.id).single()
        assertEquals(90.0, seco.pesoDisponibleKg, 0.0)
        assertNull(seco.compraId)
        assertEquals(15_556L, seco.costoKg)   // 100 kg x $14.000 entre 90 kg secos

        val segundo = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(b, 40.0)))
        assertEquals(0.0, disponible(b), 0.0)
        repositorio.cancelarSecado(segundo.id)
        assertEquals(40.0, disponible(b), 0.0)
        assertTrue(repositorio.secadosEnCurso().isEmpty())
        try {
            repositorio.cancelarSecado(segundo.id)
            fail("Debía fallar: ya estaba cancelado")
        } catch (e: IllegalArgumentException) {
            assertEquals(40.0, disponible(b), 0.0)   // no se devolvió dos veces
        }
    }
}
