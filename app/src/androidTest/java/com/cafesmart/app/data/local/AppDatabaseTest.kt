package com.cafesmart.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Prueba con Room real (necesita emulador o celular; no corre en el CI). Usa una base en memoria:
// no toca cafesmart.db ni borra el Negocio guardado en el dispositivo.
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

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

    private fun lote() = LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name)

    private fun sublote(loteId: Long, compraId: Long? = null) = SubloteEntity(
        loteId = loteId,
        compraId = compraId,
        pesoInicialKg = 120.0,
        pesoDisponibleKg = 120.0,
        costoKg = 14_000,
        fechaIngreso = fechaAMilis(Fecha(dia = 10, mes = 11, anio = 2026)),
        humedadPorcentaje = 11.5,
        factorRendimiento = 95,
    )

    @Test
    fun insertaUnLoteYUnSubloteYLosLeeDeVuelta() = runBlocking {
        val loteId = db.loteDao().insertar(lote())
        val subloteId = db.subloteDao().insertar(sublote(loteId))

        assertEquals(lote().copy(id = loteId), db.loteDao().buscarPorId(loteId))
        assertEquals(sublote(loteId).copy(id = subloteId), db.subloteDao().buscarPorId(subloteId))
        assertEquals(listOf(loteId), db.loteDao().listar().map { it.id })
        assertEquals(listOf(subloteId), db.subloteDao().listar().map { it.id })
    }

    @Test
    fun buscarPorIdDevuelveNullSiNoExiste() = runBlocking {
        assertNull(db.loteDao().buscarPorId(99))
        assertNull(db.subloteDao().buscarPorId(99))
    }

    @Test
    fun unSubloteSinLoteExistenteNoSeGuarda() = runBlocking {
        try {
            db.subloteDao().insertar(sublote(loteId = 99))
            fail("Debía fallar: el lote 99 no existe")
        } catch (e: SQLiteConstraintException) {
            // Esperado: la llave foránea impide guardar un sublote huérfano.
        }
        assertEquals(emptyList<SubloteEntity>(), db.subloteDao().listar())
    }

    @Test
    fun noPermiteDosLotesConElMismoTipoYCalidad() = runBlocking {
        db.loteDao().insertar(lote())
        try {
            db.loteDao().insertar(lote())
            fail("Debía fallar: el lote Verde / Buena ya existe")
        } catch (e: SQLiteConstraintException) {
            // Esperado: (tipo_cafe, calidad) es único.
        }
        assertEquals(1, db.loteDao().listar().size)
    }

    @Test
    fun productorCompraYSubloteQuedanEnlazados() = runBlocking {
        val productorId = db.productorDao().insertar(
            ProductorEntity(
                nombre = "Juan Pérez",
                documento = "1112345678",
                tipoDocumento = TipoDocumento.CC.name,
                telefono = "3001234567",
            ),
        )
        val fecha = fechaAMilis(Fecha(dia = 10, mes = 11, anio = 2026))
        val compraId = db.compraDao().insertar(
            CompraEntity(productorId = productorId, fecha = fecha, totalPagado = 1_680_000),
        )
        val sinRegistrarId = db.compraDao().insertar(
            CompraEntity(productorId = null, fecha = fecha, totalPagado = 500_000),
        )
        val loteId = db.loteDao().insertar(lote())
        val subloteId = db.subloteDao().insertar(sublote(loteId, compraId))

        assertEquals(productorId, db.compraDao().buscarPorId(compraId)?.productorId)
        assertNull(db.compraDao().buscarPorId(sinRegistrarId)?.productorId)
        assertEquals(compraId, db.subloteDao().buscarPorId(subloteId)?.compraId)
        assertEquals("1112345678", db.productorDao().buscarPorId(productorId)?.documento)
        assertEquals(2, db.compraDao().listar().size)
    }

    @Test
    fun noPermiteDosProductoresConElMismoDocumento() = runBlocking {
        val productor = ProductorEntity(nombre = "Ana", documento = "52123456", tipoDocumento = TipoDocumento.CC.name)
        db.productorDao().insertar(productor)
        try {
            db.productorDao().insertar(productor.copy(nombre = "Otra Ana"))
            fail("Debía fallar: el documento ya existe")
        } catch (e: SQLiteConstraintException) {
            // Esperado: documento es único.
        }
        assertEquals(1, db.productorDao().listar().size)
    }
}
