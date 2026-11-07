package com.cafesmart.app.data.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.SubloteConLote

// Modelo (MVVM): objetos que solo guardan datos. La logica esta en el ViewModel (viewmodel/sale).

// Un sublote que se eligio para vender y cuantos kg se venden de el.
// Al elegirlo se vende todo lo disponible; "Cambiar" lo ajusta.
data class SubloteElegido(
    val sublote: SubloteConLote,
    val cantidadKg: Double
) {
    val id: Long get() = sublote.sublote.id
    val calidad: Calidad get() = sublote.calidad
    val disponibleKg: Double get() = sublote.sublote.pesoDisponibleKg
}
