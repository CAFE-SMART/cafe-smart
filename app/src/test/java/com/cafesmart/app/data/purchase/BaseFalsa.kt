package com.cafesmart.app.data.purchase

import com.cafesmart.app.data.local.ClienteDao
import com.cafesmart.app.data.local.ClienteEntity
import com.cafesmart.app.data.local.CompraDao
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.local.LoteDao
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.ProductorDao
import com.cafesmart.app.data.local.ProductorEntity
import com.cafesmart.app.data.local.SecadoConEnviosFila
import com.cafesmart.app.data.local.SecadoDao
import com.cafesmart.app.data.local.SecadoEntity
import com.cafesmart.app.data.local.SecadoSubloteEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.local.Transaccion
import com.cafesmart.app.data.local.VentaDao
import com.cafesmart.app.data.local.VentaEntity
import com.cafesmart.app.data.local.VentaSubloteEntity
import com.cafesmart.app.data.sale.RoomVentaRepository
import com.cafesmart.app.data.secado.RoomSecadoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    var filas = listOf<CompraEntity>()
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

    // Imita el UPDATE real de SubloteDao.descontar: 0 si no existe o no alcanza (con 0.0005 de tolerancia),
    // y si alcanza deja el peso redondeado a 3 decimales (nunca negativo) y devuelve 1.
    override suspend fun descontar(id: Long, kg: Double): Int {
        val sublote = filas.firstOrNull { it.id == id } ?: return 0
        if (sublote.pesoDisponibleKg < kg - 0.0005) return 0
        val nuevo = maxOf(0.0, Math.round((sublote.pesoDisponibleKg - kg) * 1000.0) / 1000.0)
        filas = filas.map { if (it.id == id) it.copy(pesoDisponibleKg = nuevo) else it }
        return 1
    }

    // Imita el UPDATE real de SubloteDao.devolver: 0 si no existe; si existe suma, redondea a 3 decimales
    // y nunca pasa del peso inicial.
    override suspend fun devolver(id: Long, kg: Double): Int {
        val sublote = filas.firstOrNull { it.id == id } ?: return 0
        val nuevo = minOf(sublote.pesoInicialKg, Math.round((sublote.pesoDisponibleKg + kg) * 1000.0) / 1000.0)
        filas = filas.map { if (it.id == id) it.copy(pesoDisponibleKg = nuevo) else it }
        return 1
    }

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

class ClienteDaoFalso : ClienteDao {
    var filas = listOf<ClienteEntity>()
    private var siguienteId = 1L

    fun copiarEstado() = filas to siguienteId
    fun restaurar(estado: Pair<List<ClienteEntity>, Long>) { filas = estado.first; siguienteId = estado.second }

    override suspend fun insertar(cliente: ClienteEntity): Long {
        check(filas.none { it.documento == cliente.documento }) { "UNIQUE cliente.documento" }
        val id = siguienteId++
        filas = filas + cliente.copy(id = id)
        return id
    }
    override suspend fun listar() = filas.sortedBy { it.id }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override suspend fun buscarPorDocumento(documento: String) = filas.firstOrNull { it.documento == documento }
    override suspend fun actualizar(cliente: ClienteEntity) {
        filas = filas.map { if (it.id == cliente.id) cliente else it }
    }
}

class VentaDaoFalso : VentaDao {
    var filas = listOf<VentaEntity>()
    var detalles = listOf<VentaSubloteEntity>()
    private var siguienteId = 1L

    fun copiarEstado() = Triple(filas, detalles, siguienteId)
    fun restaurar(estado: Triple<List<VentaEntity>, List<VentaSubloteEntity>, Long>) {
        filas = estado.first; detalles = estado.second; siguienteId = estado.third
    }

    override suspend fun insertar(venta: VentaEntity): Long {
        val id = siguienteId++
        filas = filas + venta.copy(id = id)
        return id
    }
    // Imita la llave primaria compuesta (venta_id, sublote_id) de la tabla real.
    override suspend fun insertarDetalle(item: VentaSubloteEntity) {
        check(detalles.none { it.ventaId == item.ventaId && it.subloteId == item.subloteId }) { "UNIQUE venta_sublote" }
        detalles = detalles + item
    }
    override suspend fun listar() = filas.sortedBy { it.id }
}

// Imita SecadoDao: ids que asigna la base, la llave primaria (secado_id, sublote_id), la llave foránea del sublote
// y el UPDATE de cerrar, que solo cambia un secado que sigue EN_CURSO. guardarConEnvios es el real (hereda de SecadoDao).
class SecadoDaoFalso(private val sublotes: SubloteDaoFalso) : SecadoDao() {
    var filas = listOf<SecadoEntity>()
    var envios = listOf<SecadoSubloteEntity>()
    private var siguienteId = 1L

    fun copiarEstado() = Triple(filas, envios, siguienteId)
    fun restaurar(estado: Triple<List<SecadoEntity>, List<SecadoSubloteEntity>, Long>) {
        filas = estado.first; envios = estado.second; siguienteId = estado.third
    }

    override suspend fun insertar(secado: SecadoEntity): Long {
        val id = siguienteId++
        filas = filas + secado.copy(id = id)
        return id
    }
    override suspend fun insertarEnvios(filas: List<SecadoSubloteEntity>) {
        filas.forEach { fila ->
            check(sublotes.filas.any { it.id == fila.subloteId }) { "FOREIGN KEY secado_sublote.sublote_id" }
            check(envios.none { it.secadoId == fila.secadoId && it.subloteId == fila.subloteId }) { "UNIQUE secado_sublote" }
            envios = envios + fila
        }
    }
    override suspend fun cerrar(id: Long, estado: String, fechaFin: Long?, factor: Int?): Int {
        val secado = filas.firstOrNull { it.id == id && it.estado == "EN_CURSO" } ?: return 0
        filas = filas.map { if (it.id == id) secado.copy(estado = estado, fechaFin = fechaFin, factorRendimiento = factor) else it }
        return 1
    }
    override suspend fun buscarPorId(id: Long) = filas.firstOrNull { it.id == id }
    override suspend fun buscarConEnvios(id: Long) = buscarPorId(id)?.let { conEnvios(it) }
    override suspend fun listarEnCurso() = filas.filter { it.estado == "EN_CURSO" }
        .sortedWith(compareByDescending<SecadoEntity> { it.fechaInicio }.thenByDescending { it.id }).map { conEnvios(it) }
    override suspend fun listarFinalizados() = filas.filter { it.estado == "FINALIZADO" }
        .sortedWith(compareByDescending<SecadoEntity> { it.fechaFin }.thenByDescending { it.id }).map { conEnvios(it) }
    override suspend fun resultantesDe(secadoId: Long) = sublotes.filas.filter { it.secadoId == secadoId }.sortedBy { it.id }

    private fun conEnvios(secado: SecadoEntity) =
        SecadoConEnviosFila(secado, envios.filter { it.secadoId == secado.id })
}

// Los DAOs falsos juntos y una transacción falsa: si el bloque lanza una excepción, devuelve todas las
// tablas a como estaban (lo que hace Room de verdad con withTransaction).
class BaseFalsa {
    val productores = ProductorDaoFalso()
    val compras = CompraDaoFalso()
    val lotes = LoteDaoFalso()
    val sublotes = SubloteDaoFalso()
    val clientes = ClienteDaoFalso()
    val ventas = VentaDaoFalso()
    val secados = SecadoDaoFalso(sublotes)

    val transaccion = object : Transaccion {
        override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
            val p = productores.copiarEstado()
            val c = compras.copiarEstado()
            val l = lotes.copiarEstado()
            val s = sublotes.copiarEstado()
            val cl = clientes.copiarEstado()
            val v = ventas.copiarEstado()
            val se = secados.copiarEstado()
            try {
                return bloque()
            } catch (e: Throwable) {
                productores.restaurar(p); compras.restaurar(c); lotes.restaurar(l); sublotes.restaurar(s)
                clientes.restaurar(cl); ventas.restaurar(v); secados.restaurar(se)
                throw e
            }
        }
    }

    fun repositorio() = RoomPurchaseRepository(productores, compras, lotes, sublotes, transaccion)

    fun repositorioVentas() = RoomVentaRepository(clientes, ventas, sublotes, transaccion)

    fun repositorioSecado() = RoomSecadoRepository(secados, sublotes, lotes, transaccion)
}
