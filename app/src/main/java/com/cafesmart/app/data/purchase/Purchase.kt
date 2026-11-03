package com.cafesmart.app.data.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento

// Modelo (MVVM): objetos que solo guardan datos. No tienen logica;
// la logica esta en el ViewModel (viewmodel/purchase).

// Como se elige el productor en el paso 1
// enum class: una lista cerrada de opciones. Solo puede valer una de estas tres.
enum class ModoProductor { REGISTRADO, SIN_REGISTRAR }

// Lo que se va escribiendo en el formulario "Productor nuevo"
// Todos los campos arrancan vacios porque el formulario empieza en blanco.
data class NuevoProductor(
    val tipo: TipoDocumento = TipoDocumento.CC,
    val documento: String = "",
    val nombre: String = "",
    val telefono: String = ""
)

// Lo que se va escribiendo en cada tarjeta "Café N" del paso 2
// String? significa que puede ser null: todavia no se ha elegido.
// Peso y precio son texto porque guardan lo que la persona va escribiendo.
data class CafeFormulario(
    val tipoCafe: String? = null,
    val calidad: Calidad? = null,
    val peso: String = "",
    val precio: String = ""
)

// Un cafe ya validado de la compra (antes "Sublote"; el Sublote real, con id, esta en data.model).
// A diferencia de CafeFormulario, aqui todo esta completo y los numeros ya son Double.
data class CafeValidado(
    val tipoCafe: String,
    val calidad: Calidad,
    val pesoKg: Double,
    val precioKg: Double
)

// Productor fijo que se usa cuando se elige "Sin registrar".
val productorSinRegistrar = Productor(nombre = "Sin registrar", documento = "Compra rápida")

// Productores de ejemplo (nombres del Figma) para ver las pantallas Buscar y Seleccionar.
// Temporal: se reemplazan por los datos reales cuando se incorpore Room.
// Va del mas antiguo al mas reciente; "Recientes" los muestra al reves.
val productoresDeEjemplo = listOf(
    Productor(nombre = "Finca La Esperanza", documento = "1020304050"),
    Productor(nombre = "Andy López", documento = "11140596"),
    Productor(nombre = "Pepito Pérez", documento = "890301886")
)

// Opciones de la pregunta "Tipo de café".
val tiposCafe = TipoCafe.entries.map { it.etiqueta }
