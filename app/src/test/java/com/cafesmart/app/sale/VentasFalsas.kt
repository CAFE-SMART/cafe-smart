package com.cafesmart.app.sale

import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.sale.ItemVenta
import com.cafesmart.app.data.sale.ResultadoCliente
import com.cafesmart.app.data.sale.SinCafeSuficienteException
import com.cafesmart.app.data.sale.VentaRepository
import com.cafesmart.app.data.sale.clientesDeEjemplo
import com.cafesmart.app.data.sale.sublotesParaVenderEjemplo
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Versiones falsas para probar SaleViewModel con JUnit, sin Room (como ComprasFalsas en Compras).

/** Repositorio en memoria: guarda clientes con id y anota las ventas que se registran. */
class RepositorioDeVentasFalso(clientes: List<Cliente> = emptyList()) : VentaRepository {

    data class VentaGuardada(val clienteId: Long?, val fecha: Fecha, val items: List<ItemVenta>)

    // Los clientes de ejemplo traen id 0 y se dejan asi, para que las pruebas los comparen con clientesDeEjemplo;
    // los que se guardan despues reciben el siguiente id, como en Room.
    val clientes = clientes.toMutableList()
    val ventas = mutableListOf<VentaGuardada>()

    // true = registrarVenta falla (por ejemplo, la base de datos no responde).
    var fallarAlRegistrar = false

    // true = leer, guardar o modificar clientes falla.
    var fallarAlListar = false
    var fallarAlGuardarCliente = false

    // Si se da, guardar o modificar un cliente se queda esperando hasta que se complete (un guardado lento),
    // y se cuentan los intentos que llegaron al repositorio. Sirve para probar el doble toque.
    var esperarAlGuardarCliente: CompletableDeferred<Unit>? = null
    var intentosDeGuardarCliente = 0
    var intentosDeActualizarCliente = 0

    override suspend fun listarClientes(): List<Cliente> {
        if (fallarAlListar) throw IllegalStateException("fallo simulado al listar")
        return clientes.toList()
    }

    override suspend fun guardarCliente(cliente: Cliente): ResultadoCliente {
        intentosDeGuardarCliente++
        esperarAlGuardarCliente?.await()
        if (fallarAlGuardarCliente) throw IllegalStateException("fallo simulado al guardar el cliente")
        if (clientes.any { it.documento == cliente.documento }) return ResultadoCliente.DocumentoRepetido
        val guardado = cliente.copy(id = (clientes.maxOfOrNull { it.id } ?: 0L) + 1)
        clientes += guardado
        return ResultadoCliente.Guardado(guardado)
    }

    override suspend fun actualizarCliente(cliente: Cliente): ResultadoCliente {
        intentosDeActualizarCliente++
        esperarAlGuardarCliente?.await()
        if (fallarAlGuardarCliente) throw IllegalStateException("fallo simulado al modificar el cliente")
        if (clientes.any { it.documento == cliente.documento && it.id != cliente.id }) return ResultadoCliente.DocumentoRepetido
        val i = clientes.indexOfFirst { it.id == cliente.id }
        if (i >= 0) clientes[i] = cliente
        return ResultadoCliente.Guardado(cliente)
    }

    // Si se da, registrarVenta se queda esperando hasta que se complete (un guardado lento) y se cuentan los intentos
    // que llegaron. Sirve para probar el doble toque mientras se guarda.
    var esperarAlRegistrar: CompletableDeferred<Unit>? = null
    var intentosDeRegistrar = 0

    // Inventario que esta venta descuenta (el mismo que lee el ViewModel). Sin inventario, solo anota la venta.
    var inventario: InventarioDeVentasFalso? = null

    // Imita a Room: todo o nada. Si algun sublote no alcanza lanza SinCafeSuficienteException y no queda nada
    // guardado ni descontado; si alcanza, anota la venta, resta los kg y el inventario vuelve a emitir.
    override suspend fun registrarVenta(clienteId: Long?, fecha: Fecha, items: List<ItemVenta>): Long {
        intentosDeRegistrar++
        esperarAlRegistrar?.await()
        if (fallarAlRegistrar) throw IllegalStateException("fallo simulado")
        inventario?.let { falso ->
            val disponible = falso.lotes.value.flatMap { it.sublotes }.associate { it.id to it.pesoDisponibleKg }
            items.firstOrNull { (disponible[it.subloteId] ?: 0.0) < it.pesoKg }
                ?.let { throw SinCafeSuficienteException(it.subloteId) }
            falso.lotes.value = falso.lotes.value.map { lote ->
                lote.copy(sublotes = lote.sublotes.map { sublote ->
                    val vendido = items.filter { it.subloteId == sublote.id }.sumOf { it.pesoKg }
                    if (vendido == 0.0) sublote
                    else sublote.copy(pesoDisponibleKg = redondearKg(maxOf(0.0, sublote.pesoDisponibleKg - vendido)))
                })
            }
        }
        ventas += VentaGuardada(clienteId, fecha, items)
        return ventas.size.toLong()
    }
}

/** Inventario en memoria: entrega los lotes de `lotes` y vuelve a emitir cada vez que se les asigna un valor nuevo. */
class InventarioDeVentasFalso(
    inicial: List<LoteConSublotes> = lotesParaVenderEjemplo(),
) : InventoryRepository {
    val lotes = MutableStateFlow(inicial)
    override fun observarLotes(): Flow<List<LoteConSublotes>> = lotes
}

/** Los sublotes de ejemplo para vender, agrupados otra vez por su lote (como los entrega Inventario). */
fun lotesParaVenderEjemplo(): List<LoteConSublotes> =
    sublotesParaVenderEjemplo().groupBy { it.lote }.map { (lote, items) -> LoteConSublotes(lote, items.map { it.sublote }) }

/**
 * Regla de JUnit para las pruebas de Ventas: antes de cada prueba pone el Main en un despachador que ejecuta al
 * instante (cada accion termina antes de la siguiente linea) y al terminar lo restaura con resetMain, igual que
 * el @Before / @After de PurchaseViewModelGuardadoTest. Cada clase de prueba la declara con @get:Rule.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrincipalDeVentasRule : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

/**
 * ViewModel de Ventas con los clientes y el inventario de ejemplo ya cargados, como los dejaria Room.
 * Necesita la regla [PrincipalDeVentasRule] en la clase de prueba (viewModelScope usa el Main).
 */
fun viewModelDeVentasDePrueba(
    repositorio: RepositorioDeVentasFalso = RepositorioDeVentasFalso(clientesDeEjemplo),
    inventario: InventarioDeVentasFalso = InventarioDeVentasFalso(),
): SaleViewModel {
    // El repositorio descuenta de este mismo inventario, como Room lo hace con las tablas que lee Inventario.
    if (repositorio.inventario == null) repositorio.inventario = inventario
    return SaleViewModel(repositorio, inventario).also {
        it.cargarClientes()
        it.observarInventario()
    }
}
