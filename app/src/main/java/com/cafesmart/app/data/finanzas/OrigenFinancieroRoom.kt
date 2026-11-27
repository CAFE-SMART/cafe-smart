package com.cafesmart.app.data.finanzas

import android.content.Context
import com.cafesmart.app.data.gastos.RoomGastosRepository
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.CompraDao
import com.cafesmart.app.data.local.ProductorDao
import com.cafesmart.app.viewmodel.home.comprasRegistradas
import kotlinx.coroutines.flow.first

/**
 * Datos reales del Resumen, el Historial y las Gráficas financieras, leídos de Room:
 * - GASTOS (Amelia): todos (generales, de sublotes y de secado), cada uno una sola vez con su monto completo y su
 *   categoría, para no contarlos dos veces.
 * - COMPRAS (Compras en Room, ya en main): lo pagado en cada compra, con su productor y su café. Solo alimentan
 *   "Compras de café" y el Historial; no entran en la ganancia (la ganancia sale de lo VENDIDO).
 *
 * VENTAS (Katherine, PR #43) y merma de secado (Katherine, PR #44-#46) siguen sin conectar. Mientras no lo estén,
 * [ventasConectadas] es false: el Resumen muestra ingresos y ganancia como "No disponible", no como 0.
 */
class OrigenFinancieroRoom(
    private val gastosRoom: RoomGastosRepository,
    private val comprasDao: CompraDao? = null,
    private val productoresDao: ProductorDao? = null,
    private val inventario: InventoryRepository? = null,
) : OrigenFinanciero {
    override val ventasConectadas: Boolean get() = false

    override suspend fun gastos(): List<GastoRegistrado> = gastosRoom.gastosRegistrados()

    override suspend fun compras(): List<CompraRegistrada> {
        val dao = comprasDao ?: return emptyList()
        val nombres = productoresDao?.listar().orEmpty().associate { it.id to it.nombre }
        val lotes = inventario?.observarLotes()?.first().orEmpty()
        return comprasRegistradas(dao.listar(), lotes, nombres)
    }

    companion object {
        fun de(context: Context): OrigenFinancieroRoom {
            val base = AppDatabase.obtener(context)
            return OrigenFinancieroRoom(
                RoomGastosRepository.obtener(context),
                base.compraDao(),
                base.productorDao(),
                RoomInventoryRepository(base.loteDao(), base.subloteDao()),
            )
        }
    }
}
