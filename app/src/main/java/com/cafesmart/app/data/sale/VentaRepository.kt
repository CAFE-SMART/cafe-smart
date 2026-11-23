package com.cafesmart.app.data.sale

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.ClienteDao
import com.cafesmart.app.data.local.ClienteEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.Transaccion
import com.cafesmart.app.data.local.TransaccionRoom
import com.cafesmart.app.data.local.VentaDao
import com.cafesmart.app.data.local.VentaEntity
import com.cafesmart.app.data.local.VentaSubloteEntity
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.sale.subtotal

// Acceso a los clientes y a las ventas guardadas. Es una interfaz para probar con JUnit sin Room
// (versión falsa en las pruebas), igual que PurchaseRepository.

/** Resultado de guardar o modificar un cliente. */
sealed interface ResultadoCliente {
    /** Quedó guardado; [cliente] ya trae el id que asignó Room. */
    data class Guardado(val cliente: Cliente) : ResultadoCliente

    /** Otro cliente ya tiene ese documento: no se guardó nada. */
    data object DocumentoRepetido : ResultadoCliente
}

/** Un sublote que se vende: cuántos kg y a qué precio por kg (pesos enteros). */
data class ItemVenta(val subloteId: Long, val pesoKg: Double, val precioKg: Long)

/** El sublote ya no tiene los kg que se quieren vender. Al lanzarse, la transacción se deshace. */
class SinCafeSuficienteException(val subloteId: Long) :
    Exception("No hay café suficiente en el sublote $subloteId para esta venta.")

interface VentaRepository {
    /** Todos los clientes, del más antiguo al más nuevo (la pantalla muestra los recientes primero). */
    suspend fun listarClientes(): List<Cliente>

    /** Guarda un cliente nuevo. Si el documento ya existe devuelve [ResultadoCliente.DocumentoRepetido]. */
    suspend fun guardarCliente(cliente: Cliente): ResultadoCliente

    /** Modifica al cliente con ese id. Su propio documento no cuenta como repetido; el de otro sí. */
    suspend fun actualizarCliente(cliente: Cliente): ResultadoCliente

    /**
     * Registra la venta completa en una sola transacción: la Venta, el detalle de cada sublote y el descuento
     * de sus kg. [clienteId] null = venta "Sin registrar". Devuelve el id de la venta.
     * Si algo falla (por ejemplo [SinCafeSuficienteException]) no queda nada guardado.
     */
    suspend fun registrarVenta(clienteId: Long?, fecha: Fecha, items: List<ItemVenta>): Long

    companion object {
        /** Versión real, sobre Room. */
        fun de(context: Context): VentaRepository {
            val base = AppDatabase.obtener(context)
            return RoomVentaRepository(
                clienteDao = base.clienteDao(),
                ventaDao = base.ventaDao(),
                subloteDao = base.subloteDao(),
                transaccion = TransaccionRoom(base),
            )
        }
    }
}

class RoomVentaRepository(
    private val clienteDao: ClienteDao,
    private val ventaDao: VentaDao,
    private val subloteDao: SubloteDao,
    private val transaccion: Transaccion,
) : VentaRepository {

    override suspend fun listarClientes(): List<Cliente> =
        clienteDao.listar().map { it.aModelo() }

    override suspend fun guardarCliente(cliente: Cliente): ResultadoCliente {
        // El índice único también lo rechazaría, pero con una falla de la base; así la pantalla puede avisar.
        if (clienteDao.buscarPorDocumento(cliente.documento) != null) return ResultadoCliente.DocumentoRepetido
        return try {
            val id = clienteDao.insertar(cliente.aEntidad())
            ResultadoCliente.Guardado(cliente.copy(id = id))
        } catch (e: SQLiteConstraintException) {
            // Otro guardado del mismo documento se coló entre la consulta y el insert: lo frenó el índice único.
            ResultadoCliente.DocumentoRepetido
        }
    }

    override suspend fun actualizarCliente(cliente: Cliente): ResultadoCliente {
        val conEseDocumento = clienteDao.buscarPorDocumento(cliente.documento)
        if (conEseDocumento != null && conEseDocumento.id != cliente.id) return ResultadoCliente.DocumentoRepetido
        return try {
            clienteDao.actualizar(cliente.aEntidad())
            ResultadoCliente.Guardado(cliente)
        } catch (e: SQLiteConstraintException) {
            // Mismo caso que al guardar: otro cliente tomó ese documento después de la consulta.
            ResultadoCliente.DocumentoRepetido
        }
    }

    override suspend fun registrarVenta(clienteId: Long?, fecha: Fecha, items: List<ItemVenta>): Long {
        require(items.isNotEmpty()) { "una venta necesita al menos un sublote" }
        items.forEach {
            require(it.pesoKg > 0) { "el peso a vender debe ser mayor a 0" }
            require(it.precioKg > 0) { "el precio por kg debe ser mayor a 0" }
        }
        return transaccion.ejecutar {
            // totalRecibido: la misma cuenta del paso 3 (subtotal de SaleRules): cada item se redondea a pesos
            // enteros y luego se suman.
            val ventaId = ventaDao.insertar(
                VentaEntity(
                    clienteId = clienteId,
                    fecha = fechaAMilis(fecha),
                    totalRecibido = items.sumOf { subtotal(it.pesoKg, it.precioKg.toDouble()) },
                ),
            )
            for (item in items) {
                ventaDao.insertarDetalle(VentaSubloteEntity(ventaId, item.subloteId, item.pesoKg, item.precioKg))
                // 0 filas cambiadas = no alcanzaba el café: se lanza para que Room deshaga toda la venta.
                if (subloteDao.descontar(item.subloteId, item.pesoKg) == 0) {
                    throw SinCafeSuficienteException(item.subloteId)
                }
            }
            ventaId
        }
    }
}

private fun ClienteEntity.aModelo() = Cliente(
    id = id,
    nombre = nombre,
    documento = documento,
    tipoDocumento = TipoDocumento.valueOf(tipoDocumento),
    telefono = telefono,
)

private fun Cliente.aEntidad() = ClienteEntity(
    id = id,
    nombre = nombre,
    documento = documento,
    tipoDocumento = tipoDocumento.name,
    telefono = telefono,
)
