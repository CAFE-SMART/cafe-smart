package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.model.LoteConSublotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// InventoryRepository falso para probar InventoryViewModel sin Room: entrega los lotes de `lotes` y vuelve a emitir
// cada vez que se les asigna un valor nuevo (como Room al guardarse una compra).
class InventarioFalso(inicial: List<LoteConSublotes> = emptyList()) : InventoryRepository {
    val lotes = MutableStateFlow(inicial)
    override fun observarLotes(): Flow<List<LoteConSublotes>> = lotes
}
