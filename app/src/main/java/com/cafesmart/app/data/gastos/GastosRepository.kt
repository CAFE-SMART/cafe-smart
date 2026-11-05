package com.cafesmart.app.data.gastos

import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.GastoSublote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Puerto de persistencia de Gastos. El guardado debe completar o lanzar un error real. */
interface GastosRepository {
    val gastos: StateFlow<List<Gasto>>
    suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>)
}

/**
 * Demostración en memoria, aislada de Firebase y de los datos de los demás módulos.
 * Vive con el ViewModel y se pierde al destruir su propietario o terminar el proceso.
 * Pendiente: sustituir por el repositorio permanente y una fuente real de inventario.
 */
class DemoGastosRepository : GastosRepository {
    private val registros = MutableStateFlow<List<Gasto>>(emptyList())
    private val repartos = mutableMapOf<Long, List<GastoSublote>>()
    override val gastos: StateFlow<List<Gasto>> = registros

    // La operación es atómica en memoria. Un reintento del mismo ID y contenido no duplica.
    @Synchronized
    private fun registrar(gasto: Gasto, reparto: List<GastoSublote>) {
        require(gasto.id > 0 && gasto.repartoEsValido(reparto))
        require(reparto.map { it.subloteId }.distinct().size == reparto.size)
        val anterior = registros.value.find { it.id == gasto.id }
        if (anterior != null) {
            require(anterior == gasto && repartos[gasto.id] == reparto) { "El ID ya corresponde a otro gasto." }
            return
        }
        repartos[gasto.id] = reparto.toList()
        registros.value = listOf(gasto) + registros.value
    }

    override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) = registrar(gasto, reparto)
}
