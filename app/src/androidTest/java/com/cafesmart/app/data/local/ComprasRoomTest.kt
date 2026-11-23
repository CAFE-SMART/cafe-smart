package com.cafesmart.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.inventario.OrigenOcupacionRoom
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.data.purchase.RoomPurchaseRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Compras conectadas a Room, con Room real (necesita emulador o celular; no corre en el CI):
// las dos búsquedas nuevas de los DAOs, el guardado en una sola transacción y que la compra siga ahí al reabrir.
@RunWith(AndroidJUnit4::class)
class ComprasRoomTest {

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

    private fun repositorio(base: AppDatabase = db) = RoomPurchaseRepository(
        base.productorDao(), base.compraDao(), base.loteDao(), base.subloteDao(), TransaccionRoom(base),
    )

    private fun cafe(tipo: String, calidad: Calidad, kg: Double, precio: Double) = CafeValidado(tipo, calidad, kg, precio)

    // ── LoteDao.buscarPorTipoYCalidad ──────────────────────

    @Test
    fun buscarLoteExistente() = runBlocking {
        val id = db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name))
        val lote = db.loteDao().buscarPorTipoYCalidad(TipoCafe.VERDE.name, Calidad.BUENA.name)
        assertEquals(id, lote?.id)
    }

    @Test
    fun buscarLoteQueNoExisteDevuelveNull() = runBlocking {
        db.loteDao().insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name))
        // Mismo tipo con otra calidad, y otro tipo con la misma calidad: no es el mismo lote.
        assertNull(db.loteDao().buscarPorTipoYCalidad(TipoCafe.VERDE.name, Calidad.MALA.name))
        assertNull(db.loteDao().buscarPorTipoYCalidad(TipoCafe.SECO.name, Calidad.BUENA.name))
    }

    // ── ProductorDao.buscarPorDocumento ────────────────────

    @Test
    fun buscarProductorPorDocumentoExistenteYNoExistente() = runBlocking {
        val id = db.productorDao().insertar(
            ProductorEntity(nombre = "Ana Gómez", documento = "52123456", tipoDocumento = TipoDocumento.CC.name),
        )
        assertEquals(id, db.productorDao().buscarPorDocumento("52123456")?.id)
        assertNull(db.productorDao().buscarPorDocumento("99999999"))
    }

    @Test
    fun documentoRepetidoSeAvisaSinLlegarAlIndiceUnico() = runBlocking {
        val repo = repositorio()
        val primero = repo.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456"))
        assertTrue(primero is ResultadoProductor.Guardado)

        // El resultado es DocumentoRepetido (no una SQLiteConstraintException) y no se agrega otra fila.
        val repetido = repo.guardarProductor(Productor(nombre = "Otra Ana", documento = "52123456"))
        assertEquals(ResultadoProductor.DocumentoRepetido, repetido)
        assertEquals(1, db.productorDao().listar().size)
    }

    // Simula la carrera: la consulta previa no ve el documento (otro guardado aún no había terminado),
    // así que quien lo frena es el índice único de Room. Debe seguir dando DocumentoRepetido, no una excepción.
    private fun repositorioQueNoVeDocumentos() = RoomPurchaseRepository(
        object : ProductorDao by db.productorDao() {
            override suspend fun buscarPorDocumento(documento: String): ProductorEntity? = null
        },
        db.compraDao(), db.loteDao(), db.subloteDao(), TransaccionRoom(db),
    )

    @Test
    fun siSeCuelaUnDocumentoRepetidoAlGuardarElIndiceUnicoDevuelveDocumentoRepetido() = runBlocking {
        db.productorDao().insertar(
            ProductorEntity(nombre = "Ana Gómez", documento = "52123456", tipoDocumento = TipoDocumento.CC.name),
        )

        val resultado = repositorioQueNoVeDocumentos()
            .guardarProductor(Productor(nombre = "Otra Ana", documento = "52123456"))

        assertEquals(ResultadoProductor.DocumentoRepetido, resultado)
        assertEquals(1, db.productorDao().listar().size)
    }

    @Test
    fun siSeCuelaUnDocumentoRepetidoAlModificarElProductorQuedaIgual() = runBlocking {
        db.productorDao().insertar(
            ProductorEntity(nombre = "Ana Gómez", documento = "52123456", tipoDocumento = TipoDocumento.CC.name),
        )
        val bId = db.productorDao().insertar(
            ProductorEntity(nombre = "Beto Ruiz", documento = "80123456", tipoDocumento = TipoDocumento.CC.name),
        )

        val resultado = repositorioQueNoVeDocumentos().actualizarProductor(
            Productor(id = bId, nombre = "Beto Ruiz", documento = "52123456"),
        )

        assertEquals(ResultadoProductor.DocumentoRepetido, resultado)
        assertEquals("80123456", db.productorDao().buscarPorId(bId)?.documento)
    }

    @Test
    fun modificarUnProductorCambiaSuFilaSinCrearOtra() = runBlocking {
        val repo = repositorio()
        val ana = (repo.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456")) as ResultadoProductor.Guardado).productor
        repo.actualizarProductor(ana.copy(nombre = "Ana Gómez Ruiz"))
        assertEquals(listOf("Ana Gómez Ruiz"), db.productorDao().listar().map { it.nombre })
    }

    // ── Guardar la compra ──────────────────────────────────

    @Test
    fun laCompraGuardaSusSublotesYLosKilosEnBodegaSuman() = runBlocking {
        val repo = repositorio()
        val ana = (repo.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456")) as ResultadoProductor.Guardado).productor
        val compraId = repo.registrarCompra(
            productorId = ana.id,
            fecha = fecha,
            cafes = listOf(
                cafe("Verde", Calidad.BUENA, 25.0, 14_000.0),
                cafe("Verde", Calidad.BUENA, 10.0, 15_000.0),   // mismo lote que el anterior
                cafe("Seco", Calidad.REGULAR, 100.5, 20_000.0),
            ),
        )

        assertEquals(ana.id, db.compraDao().buscarPorId(compraId)?.productorId)
        assertEquals(350_000L + 150_000L + 2_010_000L, db.compraDao().buscarPorId(compraId)?.totalPagado)
        assertEquals(3, db.subloteDao().listar().size)
        assertEquals(2, db.loteDao().listar().size)
        assertEquals(135.5, OrigenOcupacionRoom(db.subloteDao()).kgOcupados(), 0.0)
    }

    @Test
    fun unaCompraSinRegistrarGuardaProductorNulo() = runBlocking {
        val compraId = repositorio().registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0, 14_000.0)))
        assertNull(db.compraDao().buscarPorId(compraId)?.productorId)
    }

    @Test
    fun siAlgoFallaNoQuedaNadaAMedias() = runBlocking {
        try {
            repositorio().registrarCompra(
                null, fecha,
                // El segundo café falla (tipo desconocido) después de guardar la compra y el primer sublote.
                listOf(cafe("Verde", Calidad.BUENA, 25.0, 14_000.0), cafe("Mojado", Calidad.BUENA, 5.0, 9_000.0)),
            )
            fail("Debía fallar por el tipo de café desconocido")
        } catch (e: IllegalArgumentException) {
            // Esperado: Room deshace la transacción completa.
        }
        assertEquals(0, db.compraDao().listar().size)
        assertEquals(0, db.loteDao().listar().size)
        assertEquals(0, db.subloteDao().listar().size)
    }

    // ── La compra sigue ahí al cerrar y abrir ──────────────

    @Test
    fun laCompraSigueAlCerrarYAbrirLaBase() = runBlocking {
        val nombre = "compras-prueba.db"
        contexto.deleteDatabase(nombre)
        try {
            var base = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            val compraId = repositorio(base).registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0, 14_000.0)))
            base.close()

            // Como al cerrar y volver a abrir la app: una instancia nueva sobre el mismo archivo.
            base = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            assertNotNull(base.compraDao().buscarPorId(compraId))
            assertEquals(1, base.subloteDao().listar().size)
            assertEquals(25.0, OrigenOcupacionRoom(base.subloteDao()).kgOcupados(), 0.0)
            base.close()
        } finally {
            contexto.deleteDatabase(nombre)
        }
    }
}
