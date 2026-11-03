package com.cafesmart.app.data.model

// Listas cerradas del modelo de datos (enum class: solo puede valer una de las opciones).
//
// Calidad del café (BUENA, REGULAR, MALA)

// Tipo de café al momento de entrada, actualmente la app lo guarda como String en `tiposCafe`.
enum class TipoCafe(val etiqueta: String) {
    VERDE("Verde"),
    SECO("Seco"),
    PASILLA("Pasilla"),
    TRILLADO("Trillado")
}

// Tipo de gasto" de la pantalla Registrar gasto.
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
