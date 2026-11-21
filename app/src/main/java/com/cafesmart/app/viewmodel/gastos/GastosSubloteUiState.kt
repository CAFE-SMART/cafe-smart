package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.gastos.GastoDelSublote
import com.cafesmart.app.data.model.Sublote
import java.math.BigInteger

data class GastosSubloteUiState(
    val sublote: Sublote? = null,
    val gastos: List<GastoDelSublote> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val recuperable: Boolean = false
) {
    val total: BigInteger get() = gastos.fold(BigInteger.ZERO) { total, fila ->
        total + BigInteger.valueOf(fila.montoAsignado)
    }
}
