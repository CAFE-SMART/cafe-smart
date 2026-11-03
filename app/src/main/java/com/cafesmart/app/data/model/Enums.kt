package com.cafesmart.app.data.model

// Listas cerradas del modelo de datos (enum class: solo puede valer una de las opciones).

// Calidad del café. Cada calidad guarda el texto que se muestra en pantalla (etiqueta).
enum class Calidad(val etiqueta: String) {
    BUENA("Buena"),
    REGULAR("Regular"),
    MALA("Mala")
}

// Persona se registra con cédula (CC) y empresa con NIT.
enum class TipoDocumento(val etiqueta: String) {
    CC("Cédula"),
    NIT("NIT")
}

// Tipo de café al momento de entrada. Es la única fuente de las opciones "Tipo de café":
// data/purchase `tiposCafe` se arma a partir de este enum.
enum class TipoCafe(val etiqueta: String) {
    VERDE("Verde"),
    SECO("Seco"),
    PASILLA("Pasilla"),
    TRILLADO("Trillado");

    companion object {
        // Convierte el texto que usa hoy el formulario ("Verde", "Seco"...) al enum; null si no existe.
        fun desdeEtiqueta(etiqueta: String): TipoCafe? = entries.firstOrNull { it.etiqueta == etiqueta }
    }
}

// "Tipo de gasto" de la pantalla Registrar gasto.
enum class TipoGasto(val etiqueta: String) {
    TRANSPORTE("Transporte"),
    COMIDA("Comida"),
    SECADO("Secado"),
    CARGUE("Cargue"),
    DESCARGUE("Descargue"),
    OTROS("Otros")
}

// "Estado del pago" del gasto: segmentos Pagado / Pendiente del Figma.
enum class EstadoPago(val etiqueta: String) {
    PAGADO("Pagado"),
    PENDIENTE("Pendiente")
}

// "¿A qué aplica este gasto?": gasto general, a uno o varios sublotes, o a un secado.
enum class AplicaGasto {
    GENERAL,
    SUBLOTES,
    SECADO
}

// Estado de un proceso de secado (pasos "En curso", "Finalizar" y "Cancelar secado" del Figma).
enum class EstadoSecado(val etiqueta: String) {
    EN_CURSO("En secado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado")
}
