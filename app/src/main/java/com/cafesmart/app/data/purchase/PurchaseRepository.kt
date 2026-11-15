package com.cafesmart.app.data.purchase

import android.content.Context
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.CompraDao
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.local.LoteDao
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.ProductorDao
import com.cafesmart.app.data.local.ProductorEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.local.Transaccion
import com.cafesmart.app.data.local.TransaccionRoom
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.totalDe

// Acceso a los productores y a las compras guardadas. Es una interfaz para probar con JUnit sin Room
// (versión falsa en las pruebas), igual que NegocioRepository.

/** Resultado de guardar o modificar un productor. */
sealed interface ResultadoProductor {
    /** Quedó guardado; [productor] ya trae el id que asignó Room. */
    data class Guardado(val productor: Productor) : ResultadoProductor

    /** Otro productor ya tiene ese documento: no se guardó nada. */
    data object DocumentoRepetido : ResultadoProductor
}

interface PurchaseRepository {
    /** Todos los productores, del más antiguo al más nuevo (la pantalla muestra los recientes primero). */
    suspend fun listarProductores(): List<Productor>

    /** Guarda un productor nuevo. Si el documento ya existe devuelve [ResultadoProductor.DocumentoRepetido]. */
    suspend fun guardarProductor(productor: Productor): ResultadoProductor

    /** Modifica al productor con ese id. Su propio documento no cuenta como repetido; el de otro sí. */
    suspend fun actualizarProductor(productor: Productor): ResultadoProductor

    /**
     * Registra la compra completa en una sola transacción: la Compra, el Lote de cada café (se crea si no
     * existe) y un Sublote por cada café. [productorId] null = compra "Sin registrar".
     * Devuelve el id de la compra. Si algo falla lanza una excepción y no queda nada guardado.
     */
    suspend fun registrarCompra(productorId: Long?, fecha: Fecha, cafes: List<CafeValidado>): Long

    companion object {
        /** Versión real, sobre Room. */
        fun de(context: Context): PurchaseRepository {
            val base = AppDatabase.obtener(context)
            return RoomPurchaseRepository(
                productorDao = base.productorDao(),
                compraDao = base.compraDao(),
                loteDao = base.loteDao(),
                subloteDao = base.subloteDao(),
                transaccion = TransaccionRoom(base),
            )
        }
    }
}

class RoomPurchaseRepository(
    private val productorDao: ProductorDao,
    private val compraDao: CompraDao,
    private val loteDao: LoteDao,
    private val subloteDao: SubloteDao,
    private val transaccion: Transaccion,
) : PurchaseRepository {

    override suspend fun listarProductores(): List<Productor> =
        productorDao.listar().map { it.aModelo() }

    override suspend fun guardarProductor(productor: Productor): ResultadoProductor {
        // El índice único también lo rechazaría, pero con una falla de la base; así la pantalla puede avisar.
        if (productorDao.buscarPorDocumento(productor.documento) != null) return ResultadoProductor.DocumentoRepetido
        val id = productorDao.insertar(productor.aEntidad())
        return ResultadoProductor.Guardado(productor.copy(id = id))
    }

    override suspend fun actualizarProductor(productor: Productor): ResultadoProductor {
        val conEseDocumento = productorDao.buscarPorDocumento(productor.documento)
        if (conEseDocumento != null && conEseDocumento.id != productor.id) return ResultadoProductor.DocumentoRepetido
        productorDao.actualizar(productor.aEntidad())
        return ResultadoProductor.Guardado(productor)
    }

    override suspend fun registrarCompra(productorId: Long?, fecha: Fecha, cafes: List<CafeValidado>): Long {
        require(cafes.isNotEmpty()) { "una compra necesita al menos un café" }
        return transaccion.ejecutar {
            val fechaMilis = fechaAMilis(fecha)
            // totalPagado: la misma cuenta del paso 3 (peso x precio de cada café), en pesos enteros.
            val compraId = compraDao.insertar(
                CompraEntity(
                    productorId = productorId,
                    fecha = fechaMilis,
                    totalPagado = cafes.sumOf { totalDe(it) }.aPesos(),
                ),
            )
            for (cafe in cafes) {
                subloteDao.insertar(
                    SubloteEntity(
                        loteId = buscarOCrearLote(cafe),
                        compraId = compraId,
                        pesoInicialKg = cafe.pesoKg,
                        pesoDisponibleKg = cafe.pesoKg,
                        costoKg = cafe.precioKg.aPesos(),
                        fechaIngreso = fechaMilis,
                    ),
                )
            }
            compraId
        }
    }

    // Busca el lote de ese tipo y calidad y, si no existe, lo crea. Dos cafés iguales en la misma compra
    // comparten lote: el segundo ya lo encuentra porque todo ocurre dentro de la misma transacción.
    private suspend fun buscarOCrearLote(cafe: CafeValidado): Long {
        val tipo = requireNotNull(TipoCafe.desdeEtiqueta(cafe.tipoCafe)) { "tipo de café desconocido: ${cafe.tipoCafe}" }
        return loteDao.buscarPorTipoYCalidad(tipo.name, cafe.calidad.name)?.id
            ?: loteDao.insertar(LoteEntity(tipoCafe = tipo.name, calidad = cafe.calidad.name))
    }
}

private fun ProductorEntity.aModelo() = Productor(
    id = id,
    nombre = nombre,
    documento = documento,
    tipoDocumento = TipoDocumento.valueOf(tipoDocumento),
    telefono = telefono,
)

private fun Productor.aEntidad() = ProductorEntity(
    id = id,
    nombre = nombre,
    documento = documento,
    tipoDocumento = tipoDocumento.name,
    telefono = telefono,
)
