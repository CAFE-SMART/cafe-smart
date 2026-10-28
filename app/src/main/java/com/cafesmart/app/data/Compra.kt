package com.cafesmart.app.data

// Como se elige el productor en el paso 1
enum class ModoProductor { REGISTRADO, SIN_REGISTRAR, NUEVO }

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

data class Productor(
    val nombre: String,
    val documento: String,
    val tipoDocumento: TipoDocumento = TipoDocumento.CC,
    val telefono: String = ""
)

// Lo que se va escribiendo en el formulario "Productor nuevo"
data class NuevoProductor(
    val tipo: TipoDocumento = TipoDocumento.CC,
    val documento: String = "",
    val nombre: String = "",
    val telefono: String = ""
)

// Lo que se va escribiendo en cada tarjeta "Café N" del paso 2
data class CafeFormulario(
    val tipoCafe: String? = null,
    val calidad: Calidad? = null,
    val peso: String = "",
    val precio: String = ""
)

// Un sublote es cada cafe ya validado de la compra (tipo + calidad + peso + precio)
data class Sublote(
    val tipoCafe: String,
    val calidad: Calidad,
    val pesoKg: Double,
    val precioKg: Double
) {
    val total: Double
        get() = pesoKg * precioKg
}

// Fecha simple: dia, mes (1 a 12) y anio.
// Se usa en vez de java.time.LocalDate porque LocalDate solo existe desde Android 8 (API 26)
// y el proyecto debe funcionar desde Android 7 (minSdk 24).
data class Fecha(val dia: Int, val mes: Int, val anio: Int)

val productorSinRegistrar = Productor(nombre = "Sin registrar", documento = "Compra rápida")

val tiposCafe = listOf("Verde", "Seco", "Pasilla", "Trillado")

const val PESO_MAX_KG = 99_999
const val PRECIO_MAX_KG = 100_000
