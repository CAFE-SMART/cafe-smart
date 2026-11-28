package com.cafesmart.app.data.gastos

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.GastoSublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Prueba con Room real (necesita emulador o celular; no corre en el CI). Casi todo usa una base en memoria:
// no toca cafesmart.db ni borra nada del dispositivo. La prueba de "cerrar y abrir" usa un archivo aparte
// (gastos-prueba.db) que borra al terminar.
@RunWith(AndroidJUnit4::class)
class GastosRoomTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var alcance: CoroutineScope
    private lateinit var repo: RoomGastosRepository

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = RoomGastosRepository(db.gastoDao(), alcance)
    }

    // Primero se espera a que termine la lectura de fondo del repositorio y después se cierra la base. Con
    // cancel() + close() seguidos, una consulta en curso caía sobre la base cerrada ("attempt to re-open an
    // already-closed object") y tumbaba el proceso de pruebas.
    @After
    fun cerrar() = runBlocking {
        alcance.coroutineContext.job.cancelAndJoin()
        db.close()
    }

    private suspend fun crearSublote(disponibleKg: Double = 100.0, base: AppDatabase = db): Long {
        // Tipo y calidad de un lote son únicos: todos los sublotes de la prueba cuelgan del mismo lote.
        val loteId = base.loteDao().listar().firstOrNull()?.id
            ?: base.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name))
        return base.subloteDao().insertar(
            SubloteEntity(
                loteId = loteId, pesoInicialKg = 100.0, pesoDisponibleKg = disponibleKg,
                costoKg = 14_000, fechaIngreso = fechaAMilis(Fecha(10, 11, 2026)),
            )
        )
    }

    private fun gasto(
        id: Long = 1,
        concepto: String = "Pago de jornaleros",
        monto: Long = 50_000,
        fecha: Fecha = Fecha(9, 11, 2026),
        aplicaA: AplicaGasto = AplicaGasto.GENERAL,
    ) = Gasto(id, concepto, "Cosecha de octubre", TipoGasto.TRANSPORTE, monto, fecha, EstadoPago.PENDIENTE, aplicaA)

    /** Espera (con tope) a que se cumpla algo que llega por un flujo. */
    private suspend fun esperar(condicion: () -> Boolean) = withTimeout(5_000) { while (!condicion()) delay(20) }

    @Test
    fun unGastoGeneralSeGuardaYSeLeeConTodosSusDatos() = runBlocking {
        repo.guardar(gasto(), emptyList())

        val lista = withTimeout(5_000) { repo.gastos.first { it.isNotEmpty() } }
        assertEquals(1, lista.size)
        assertEquals(gasto().copy(id = lista[0].id), lista[0])
    }

    @Test
    fun elIdDelBorradorNoSeUsaPorqueLoAsignaRoom() = runBlocking {
        repo.guardar(gasto(id = 777), emptyList())

        val lista = withTimeout(5_000) { repo.gastos.first { it.isNotEmpty() } }
        assertEquals(1L, lista[0].id)
    }

    @Test
    fun unGastoRepartidoSeLeePorSubloteConLoQueLeTocaACadaUno() = runBlocking {
        val uno = crearSublote()
        val dos = crearSublote()
        val repartido = gasto(monto = 50_000, aplicaA = AplicaGasto.SUBLOTES)
        repo.guardar(repartido, listOf(GastoSublote(1, uno, 30_000), GastoSublote(1, dos, 20_000)))

        val delUno = withTimeout(5_000) { repo.observarGastosSublote(uno).first { it.isNotEmpty() } }
        val delDos = withTimeout(5_000) { repo.observarGastosSublote(dos).first { it.isNotEmpty() } }
        assertEquals(30_000L, delUno.single().montoAsignado)
        assertEquals(20_000L, delDos.single().montoAsignado)
        // El gasto conserva su monto completo; solo la relación trae la parte de cada sublote.
        assertEquals(50_000L, delUno.single().gasto.monto)
        assertEquals(50_000L, db.gastoDao().repartoDe(delUno.single().gasto.id).sumOf { it.montoAsignado })
    }

    @Test
    fun siElRepartoApuntaAUnSubloteQueNoExisteNoSeGuardaNada() = runBlocking {
        val existente = crearSublote()
        val repartido = gasto(monto = 50_000, aplicaA = AplicaGasto.SUBLOTES)
        try {
            repo.guardar(repartido, listOf(GastoSublote(1, existente, 30_000), GastoSublote(1, 999, 20_000)))
            fail("Debe fallar por la llave foránea")
        } catch (_: SQLiteConstraintException) {
        }
        // Todo o nada: ni el gasto ni el reparto que sí era válido quedaron guardados.
        assertTrue(db.gastoDao().observarGastos().first().isEmpty())
        assertTrue(db.gastoDao().repartoDe(1).isEmpty())
        assertTrue(repo.gastos.value.isEmpty())
    }

    @Test
    fun unRepartoQueNoSumaElMontoSeRechazaSinTocarLaBase() = runBlocking {
        val uno = crearSublote()
        try {
            repo.guardar(gasto(monto = 50_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(1, uno, 10_000)))
            fail("Debe rechazar el reparto")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(db.gastoDao().observarGastos().first().isEmpty())
    }

    @Test
    fun unGastoConRepartoNoSePuedeBorrar() = runBlocking {
        val uno = crearSublote()
        repo.guardar(gasto(monto = 1_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(1, uno, 1_000)))
        val id = withTimeout(5_000) { repo.gastos.first { it.isNotEmpty() } }[0].id
        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM gasto WHERE id = $id")
            fail("RESTRICT debe impedir borrar un gasto con reparto")
        } catch (_: SQLiteConstraintException) {
        }
        assertEquals(1, repo.gastos.value.size)
    }

    @Test
    fun losGastosSalenDelMasNuevoAlMasViejo() = runBlocking {
        repo.guardar(gasto(1, "Cinco", fecha = Fecha(5, 11, 2026)), emptyList())
        repo.guardar(gasto(2, "Nueve", fecha = Fecha(9, 11, 2026)), emptyList())
        repo.guardar(gasto(3, "Siete", fecha = Fecha(7, 11, 2026)), emptyList())

        esperar { repo.gastos.value.size == 3 }
        assertEquals(listOf("Nueve", "Siete", "Cinco"), repo.gastos.value.map { it.concepto })
    }

    @Test
    fun alAsociarSoloSeOfrecenSublotesConCafeYElAgotadoSigueConsultable() = runBlocking {
        val conCafe = crearSublote(disponibleKg = 100.0)
        val agotado = crearSublote(disponibleKg = 0.0)

        assertEquals(listOf(conCafe), repo.observarSublotesDisponibles().first().map { it.id })
        assertEquals(agotado, repo.buscarSublote(agotado)?.id)
        assertNull(repo.buscarSublote(999))
    }

    @Test
    fun laListaDeUnSubloteSeActualizaSolaAlRegistrarUnGasto() = runBlocking {
        val uno = crearSublote()
        val recibidos = mutableListOf<Int>()
        val lectura = alcance.launch { repo.observarGastosSublote(uno).collect { recibidos += it.size } }
        esperar { recibidos.isNotEmpty() }
        assertEquals(0, recibidos.last())

        repo.guardar(gasto(monto = 1_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(1, uno, 1_000)))

        esperar { recibidos.last() == 1 }
        lectura.cancel()
    }

    @Test
    fun elResumenRecibeTodosLosGastosConSuMontoCompleto() = runBlocking {
        val uno = crearSublote()
        val dos = crearSublote()
        repo.guardar(gasto(1, "General", monto = 10_000), emptyList())
        repo.guardar(gasto(2, "Repartido", monto = 50_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(2, uno, 30_000), GastoSublote(2, dos, 20_000)))
        repo.guardar(gasto(3, "Uno solo", monto = 7_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(3, uno, 7_000)))

        val registrados = repo.gastosRegistrados()
        assertEquals(3, registrados.size)
        // Uno por gasto y con el monto completo, no la parte de cada sublote.
        assertEquals(67_000L, registrados.sumOf { it.montoPesos })
        assertEquals("2 sublotes", registrados.single { it.concepto == "Repartido" }.detalle)
        assertEquals("Sublote $uno", registrados.single { it.concepto == "Uno solo" }.detalle)
        assertEquals("Gasto general", registrados.single { it.concepto == "General" }.detalle)
    }

    @Test
    fun unGastoSigueAhiDespuesDeCerrarYAbrirLaBase() = runBlocking {
        val nombre = "gastos-prueba.db"
        contexto.deleteDatabase(nombre)
        try {
            val primera = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            val subloteId = crearSublote(base = primera)
            val alcance1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            RoomGastosRepository(primera.gastoDao(), alcance1)
                .guardar(gasto(monto = 8_000, aplicaA = AplicaGasto.SUBLOTES), listOf(GastoSublote(1, subloteId, 8_000)))
            alcance1.coroutineContext.job.cancelAndJoin()
            primera.close()

            val segunda = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            val guardados = segunda.gastoDao().observarGastos().first()
            val reparto = segunda.gastoDao().repartoDe(guardados.single().id)
            segunda.close()

            assertEquals("Pago de jornaleros", guardados.single().concepto)
            assertEquals(8_000L, reparto.single().montoAsignado)
            assertNotNull(reparto.single().subloteId)
        } finally {
            contexto.deleteDatabase(nombre)
        }
    }
}
