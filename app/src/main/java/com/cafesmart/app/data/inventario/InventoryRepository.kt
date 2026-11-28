package com.cafesmart.app.data.inventario

import android.content.Context
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.LoteDao
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.purchase.milisAFecha
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Lectura del inventario guardado en Room: los lotes con sus sublotes. Es una interfaz para probar con JUnit
// sin Room (DAOs falsos), igual que NegocioRepository y PurchaseRepository.
//
// Es solo de lectura: los sublotes los crea Compras (PurchaseRepository.registrarCompra) y más adelante los
// modificarán Ventas y Secado. Aquí no se escribe nada.

interface InventoryRepository {
    /**
     * Todos los lotes con sus sublotes, incluidos los sublotes agotados (LoteConSublotes ya sabe cuáles
     * tienen café: sublotesConExistencia). Va emitiendo la lista completa de nuevo cada vez que cambia una
     * compra, una venta o un secado, así la pantalla se actualiza sola sin reiniciar la app.
     * Un lote sin sublotes aparece con la lista vacía.
     */
    fun observarLotes(): Flow<List<LoteConSublotes>>

    /**
     * El sublote con ese id junto con su lote (tipo y calidad), o null si no existe. Se vuelve a emitir cuando
     * cambia el inventario (una venta o un ajuste bajan su peso). Incluye sublotes agotados: el detalle debe
     * poder abrirse aunque ya no quede café. Sale de [observarLotes], así que no necesita otra consulta a Room.
     */
    fun observarSublote(id: Long): Flow<SubloteConLote?> =
        observarLotes().map { lotes ->
            lotes.firstNotNullOfOrNull { grupo ->
                grupo.sublotes.firstOrNull { it.id == id }?.let { SubloteConLote(it, grupo.lote) }
            }
        }

    companion object {
        /** Versión real, sobre Room. */
        fun de(context: Context): InventoryRepository {
            val base = AppDatabase.obtener(context)
            return RoomInventoryRepository(loteDao = base.loteDao(), subloteDao = base.subloteDao())
        }
    }
}

class RoomInventoryRepository(
    private val loteDao: LoteDao,
    private val subloteDao: SubloteDao,
) : InventoryRepository {

    // Se observan las dos tablas y se vuelven a juntar cada vez que cualquiera cambia.
    // Al confirmar una compra, Room guarda el lote y los sublotes en una sola transacción y avisa después
    // de terminarla, así que lo normal es que ambas listas lleguen completas. Si por un instante llegara
    // una a medias, no se rompe nada: un lote sin sublotes no cuenta café, y un sublote cuyo lote aún no
    // llegó se ignora hasta que llegue.
    override fun observarLotes(): Flow<List<LoteConSublotes>> =
        combine(loteDao.observarTodos(), subloteDao.observarTodos()) { lotes, sublotes ->
            val sublotesPorLote = sublotes.map { it.aModelo() }.groupBy { it.loteId }
            lotes.map { lote -> LoteConSublotes(lote.aModelo(), sublotesPorLote[lote.id].orEmpty()) }
        }
}

// Entidad de Room → modelo de la app. El tipo y la calidad se guardan como el nombre del enum
// ("VERDE", "BUENA"), que es lo que escribe PurchaseRepository. valueOf lanza una excepción si el texto no
// existe: es mejor que un dato corrupto se note a que Inventario muestre un tipo equivocado.

private fun LoteEntity.aModelo() = Lote(
    id = id,
    tipoCafe = TipoCafe.valueOf(tipoCafe),
    calidad = Calidad.valueOf(calidad),
)

private fun SubloteEntity.aModelo() = Sublote(
    id = id,
    loteId = loteId,
    compraId = compraId,
    secadoId = secadoId,
    pesoInicialKg = pesoInicialKg,
    pesoDisponibleKg = pesoDisponibleKg,
    costoKg = costoKg,
    // La fecha se guarda en milisegundos UTC (la misma conversión que usa Compras al guardarla).
    fechaIngreso = milisAFecha(fechaIngreso),
    humedadPorcentaje = humedadPorcentaje,
    factorRendimiento = factorRendimiento,
)
