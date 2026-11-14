package com.cafesmart.app.data.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.purchase.NuevoProductor

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

// Como se elige el cliente en el paso 1: uno de la lista, "Sin registrar" o uno nuevo.
// enum class: una lista cerrada de opciones. Solo puede valer una de estas tres.
enum class ModoCliente { REGISTRADO, SIN_REGISTRAR, NUEVO }

// Lo que se va escribiendo en el formulario "Cliente nuevo". Tiene los mismos campos que "Productor nuevo"
// (tipo, documento, nombre y celular), asi que se reutiliza: typealias solo le da otro nombre.
typealias NuevoCliente = NuevoProductor
