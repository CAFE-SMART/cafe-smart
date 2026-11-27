package com.cafesmart.app.data.finanzas

import android.content.Context
import com.cafesmart.app.data.gastos.RoomGastosRepository

/**
 * Datos reales del Resumen, el Historial y las Gráficas financieras, leídos de Room. Solo conecta la parte que ya
 * está en Room y es de Amelia: los GASTOS (todos: generales, de sublotes y de secado, cada uno una sola vez con su
 * monto completo, para no contarlos dos veces).
 *
 * Ventas (Katherine, PR #43), compras (Carlos) y merma de secado (Katherine, PR #44-#46) siguen en lista vacía
 * (valor por defecto de OrigenFinanciero) hasta que cada responsable conecte la suya aquí.
 */
class OrigenFinancieroRoom(private val gastosRoom: RoomGastosRepository) : OrigenFinanciero {
    override suspend fun gastos(): List<GastoRegistrado> = gastosRoom.gastosRegistrados()

    companion object {
        fun de(context: Context) = OrigenFinancieroRoom(RoomGastosRepository.obtener(context))
    }
}
