package com.cafesmart.app.data.purchase

// Modelo (MVVM): objetos que solo guardan datos. No tienen logica;
// la logica esta en el ViewModel (viewmodel/purchase).

// Como se elige el productor en el paso 1
// enum class: una lista cerrada de opciones. Solo puede valer una de estas tres.
enum class ModoProductor { REGISTRADO, SIN_REGISTRAR }

// Cada calidad guarda el texto que se muestra en pantalla (etiqueta).
enum class Calidad(val etiqueta: String) {
    BUENA("Buena"),
    REGULAR("Regular"),
    MALA("Mala")
}

// Persona se registra con cedula (CC) y empresa con NIT
enum class TipoDocumento(val etiqueta: String) {
    CC("Cédula"),
    NIT("NIT")
}

// data class: clase cuyo unico trabajo es guardar datos.
// val = el dato no cambia; para "cambiarlo" se crea una copia con copy().
data class Productor(
    val nombre: String,
    val documento: String,
    // = TipoDocumento.CC es el valor por defecto si no se indica otro.
    val tipoDocumento: TipoDocumento = TipoDocumento.CC,
    val telefono: String = ""
)

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

// Un sublote es cada cafe ya validado de la compra
// A diferencia de CafeFormulario, aqui todo esta completo y los numeros ya son Double.
data class Sublote(
    val tipoCafe: String,
    val calidad: Calidad,
    val pesoKg: Double,
    val precioKg: Double
)

// Fecha simple: dia, mes (1 a 12) y anio.
// Se usa en vez de java.time.LocalDate porque LocalDate solo existe desde Android 8 (API 26)
// y el proyecto debe funcionar desde Android 7 (minSdk 24).
data class Fecha(val dia: Int, val mes: Int, val anio: Int)

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
val tiposCafe = listOf("Verde", "Seco", "Pasilla", "Trillado")
