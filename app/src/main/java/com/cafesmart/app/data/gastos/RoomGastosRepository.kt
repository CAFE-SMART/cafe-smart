package com.cafesmart.app.data.gastos

import android.content.Context
import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.GastoDao
import com.cafesmart.app.data.local.GastoSubloteEntity
import com.cafesmart.app.data.model.EPSILON_KG
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.GastoSublote
import com.cafesmart.app.data.model.Sublote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Gastos guardados en Room: un gasto registrado queda al cerrar y abrir la app.
 *
 * [gastos] es un StateFlow que se alimenta de la base mientras viva [alcance]. En la app el alcance dura
 * lo que el proceso (ver [obtener]); en las pruebas se le pasa uno que ellas mismas cancelan.
 */
class RoomGastosRepository(
    private val dao: GastoDao,
    alcance: CoroutineScope,
) : GastosRepository {

    // Del gasto más reciente al más antiguo. Eagerly: empieza a leer al crear el repositorio, así el
    // listado ya está cargado cuando se abre la pantalla.
    override val gastos: StateFlow<List<Gasto>> =
        dao.observarGastos()
            .map { filas -> filas.map { it.aModelo() } }
            .stateIn(alcance, SharingStarted.Eagerly, emptyList())

    /**
     * Guarda el gasto y su reparto en una sola transacción. El id que trae [gasto] es solo el del
     * borrador: Room asigna el definitivo. Lanza una excepción si el reparto no cumple las reglas del
     * gasto o si apunta a un sublote que no existe; en ese caso no se guarda nada.
     */
    override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) {
        require(gasto.repartoEsValido(reparto)) { "El reparto no cumple las reglas del gasto." }
        require(reparto.map { it.subloteId }.distinct().size == reparto.size) { "Un sublote no puede repetirse en el reparto." }
        dao.guardarConReparto(
            gasto.aEntidad(),
            // El gastoId definitivo lo pone el DAO dentro de la transacción.
            reparto.map { GastoSubloteEntity(gastoId = 0, subloteId = it.subloteId, montoAsignado = it.montoAsignado) },
        )
    }

    override fun observarGastosSublote(subloteId: Long): Flow<List<GastoDelSublote>> =
        dao.observarDelSublote(subloteId).map { filas ->
            filas.map { GastoDelSublote(it.gasto.aModelo(), it.subloteId, it.montoAsignado) }
        }

    override fun observarSublotesDisponibles(): Flow<List<Sublote>> =
        dao.observarSublotesConExistencia(EPSILON_KG).map { filas -> filas.map { it.aModelo() } }

    override suspend fun buscarSublote(id: Long): Sublote? = dao.buscarSublote(id)?.aModelo()

    /**
     * TODOS los gastos (generales, de sublotes y de secado) como los lee el Resumen y el Historial financiero:
     * uno por gasto, con su monto completo. La clase OrigenFinancieroRoom la usa en gastos().
     */
    suspend fun gastosRegistrados(): List<GastoRegistrado> {
        val sublotesPorGasto = dao.todosLosRepartos().groupBy({ it.gastoId }, { it.subloteId })
        return dao.listarGastos().map { it.aModelo().aGastoRegistrado(sublotesPorGasto[it.id].orEmpty()) }
    }

    companion object {
        @Volatile
        private var instancia: RoomGastosRepository? = null

        /**
         * Una sola instancia para toda la app: así la lista, el registro y la pantalla del sublote
         * comparten el mismo listado en vez de abrir una lectura cada uno.
         */
        fun obtener(context: Context): RoomGastosRepository =
            instancia ?: synchronized(this) {
                instancia ?: RoomGastosRepository(
                    AppDatabase.obtener(context).gastoDao(),
                    CoroutineScope(SupervisorJob() + Dispatchers.Default),
                ).also { instancia = it }
            }
    }
}
