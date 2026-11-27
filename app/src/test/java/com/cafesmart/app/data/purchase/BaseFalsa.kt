package com.cafesmart.app.data.purchase

import com.cafesmart.app.data.local.CompraDao
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.local.LoteConSublotesFila
import com.cafesmart.app.data.local.LoteDao
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.ProductorDao
import com.cafesmart.app.data.local.ProductorEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.local.Transaccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// DAOs falsos en memoria para probar los repositorios con JUnit, sin Room ni emulador.
// Imitan lo que importa de la base real: ids que asigna la base y los índices únicos (productor.documento y
// lote(tipo_cafe, calidad)), que aquí lanzan IllegalStateException al repetirse.
// Los DAOs reales y sus consultas se prueban con Room real en androidTest (BusquedasDaoTest).
// LoteDaoFalso y SubloteDaoFalso también imitan observarTodos(): emiten de nuevo cada vez que cambia `filas`,
// como hace Room tras guardar, para probar que Inventario se actualiza solo.

class ProductorDaoFalso : ProductorDao {
    var filas = listOf<ProductorEntity>()
    private var siguienteId = 1L

    fun copiarEstado() = filas to siguienteId
    fun restaurar(estado: Pair<List<ProductorEntity>, Long>) { filas = estado.first; siguienteId = estado.second }

    override suspend fun insertar(productor: ProductorEntity): Long {
        check(filas.none { it.documento == productor.documento }) { "UNIQUE productor.documento" }
        val id = siguienteId++
        filas = filas + productor.copy(id = id)
        return id
    }
    override suspend fun listar() = filas.sortedBy { it.id }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override suspend fun buscarPorDocumento(documento: String) = filas.firstOrNull { it.documento == documento }
    override suspend fun actualizar(productor: ProductorEntity) {
        filas = filas.map { if (it.id == productor.id) productor else it }
    }
}

class CompraDaoFalso : CompraDao {
    private val flujo = MutableStateFlow(listOf<CompraEntity>())
    var filas: List<CompraEntity>
        get() = flujo.value
        set(valor) { flujo.value = valor }
    private var siguienteId = 1L

    fun copiarEstado() = filas to siguienteId
    fun restaurar(estado: Pair<List<CompraEntity>, Long>) { filas = estado.first; siguienteId = estado.second }

    override suspend fun insertar(compra: CompraEntity): Long {
        val id = siguienteId++
        filas = filas + compra.copy(id = id)
        return id
    }
    override suspend fun listar() = filas.sortedBy { it.id }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override fun observarTodas(): Flow<List<CompraEntity>> = flujo.map { lista -> lista.sortedBy { it.id } }
}

class LoteDaoFalso : LoteDao {
    private val flujo = MutableStateFlow(listOf<LoteEntity>())
    var filas: List<LoteEntity>
        get() = flujo.value
        set(valor) { flujo.value = valor }
    private var siguienteId = 1L

    fun copiarEstado() = filas to siguienteId
    fun restaurar(estado: Pair<List<LoteEntity>, Long>) { filas = estado.first; siguienteId = estado.second }

    override suspend fun insertar(lote: LoteEntity): Long {
        check(filas.none { it.tipoCafe == lote.tipoCafe && it.calidad == lote.calidad }) { "UNIQUE lote(tipo_cafe, calidad)" }
        val id = siguienteId++
        filas = filas + lote.copy(id = id)
        return id
    }
    override suspend fun listar() = filas.sortedBy { it.id }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override suspend fun buscarPorTipoYCalidad(tipoCafe: String, calidad: String) =
        filas.firstOrNull { it.tipoCafe == tipoCafe && it.calidad == calidad }
    override fun observarTodos(): Flow<List<LoteEntity>> = flujo.map { lista -> lista.sortedBy { it.id } }

    // Los sublotes de la misma BaseFalsa (para observarConSublotes). Room lee las dos tablas en una transacción;
    // aquí se juntan los dos flujos, lo que basta para las pruebas de JVM. La garantía real se prueba en androidTest.
    var sublotes: SubloteDaoFalso? = null
    override fun observarConSublotes(): Flow<List<LoteConSublotesFila>> =
        combine(flujo, checkNotNull(sublotes) { "LoteDaoFalso sin sublotes" }.observarTodos()) { lotes, subs ->
            lotes.sortedBy { it.id }.map { lote -> LoteConSublotesFila(lote, subs.filter { it.loteId == lote.id }) }
        }
}

class SubloteDaoFalso : SubloteDao {
    private val flujo = MutableStateFlow(listOf<SubloteEntity>())
    var filas: List<SubloteEntity>
        get() = flujo.value
        set(valor) { flujo.value = valor }
    private var siguienteId = 1L
    // Número de inserción (1, 2, 3...) que debe fallar; null = no falla. Sirve para probar que no queda nada a medias.
    var fallarEnInsercion: Int? = null
    private var inserciones = 0

    fun copiarEstado() = filas to siguienteId
    fun restaurar(estado: Pair<List<SubloteEntity>, Long>) { filas = estado.first; siguienteId = estado.second }

    override suspend fun insertar(sublote: SubloteEntity): Long {
        inserciones++
        if (inserciones == fallarEnInsercion) throw IllegalStateException("fallo simulado al guardar el sublote $inserciones")
        val id = siguienteId++
        filas = filas + sublote.copy(id = id)
        return id
    }
    override suspend fun listar() = filas.sortedBy { it.id }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override fun observarTodos(): Flow<List<SubloteEntity>> = flujo.map { lista -> lista.sortedBy { it.id } }
}

// Los cuatro DAOs falsos juntos y una transacción falsa: si el bloque lanza una excepción, devuelve todas las
// tablas a como estaban (lo que hace Room de verdad con withTransaction).
class BaseFalsa {
    val productores = ProductorDaoFalso()
    val compras = CompraDaoFalso()
    val lotes = LoteDaoFalso()
    val sublotes = SubloteDaoFalso()

    init { lotes.sublotes = sublotes }

    val transaccion = object : Transaccion {
        override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
            val p = productores.copiarEstado()
            val c = compras.copiarEstado()
            val l = lotes.copiarEstado()
            val s = sublotes.copiarEstado()
            try {
                return bloque()
            } catch (e: Throwable) {
                productores.restaurar(p); compras.restaurar(c); lotes.restaurar(l); sublotes.restaurar(s)
                throw e
            }
        }
    }

    fun repositorio() = RoomPurchaseRepository(productores, compras, lotes, sublotes, transaccion)
}
