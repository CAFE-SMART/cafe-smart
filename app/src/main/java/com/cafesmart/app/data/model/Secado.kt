package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.Fecha
import kotlin.math.roundToLong

// SECADO: proceso que convierte café verde en café seco.
// Aunque no aparecía explicitamente en la tarea de Trello, decidí colocarla para explicar de dónde sale el café seco y
// aprovechar que la app ya tiene calcularMermaPorcentaje.
//
// Cómo funciona:
//  - Se envían kg de uno o varios sublotes verdes (SecadoSublote) y se les resta a su pesoDisponibleKg.
//  - Al finalizar se crean sublotes SECOS (uno por calidad: bueno, regular, malo) con secadoId = este secado.
//  - La merma NO se guarda: = kg enviados - kg secos obtenidos (ver SecadoConDetalle).
data class Secado(
    val id: Long = 0,
    val fechaInicio: Fecha,
    val fechaFin: Fecha? = null,             // null mientras está EN_CURSO
    val estado: EstadoSecado = EstadoSecado.EN_CURSO,
    val factorRendimiento: Int? = null       // entre 80 y 120 (Paso 5)
)

// Tabla intermedia Secado <-> Sublote: cuántos kg de cada sublote verde se enviaron a secar.
// Llave primaria: (secadoId, subloteId).
data class SecadoSublote(
    val secadoId: Long,
    val subloteId: Long,
    val pesoEnviadoKg: Double
)

// Un sublote enviado a secar, junto con el sublote (para conocer su costo por kg).
data class EnvioSecado(
    val envio: SecadoSublote,
    val sublote: Sublote
)

// Secado con sus kg enviados y los sublotes secos que resultaron. Reproduce las cifras del Paso 5/6:
// 290 kg enviados, 270 kg secos -> merma 20 kg (6,9 %), valor de la merma $ 280.000.
data class SecadoConDetalle(
    val secado: Secado,
    val enviados: List<EnvioSecado>,
    val resultantes: List<Sublote>
) {
    val pesoEnviadoKg: Double get() = enviados.sumOf { it.envio.pesoEnviadoKg }
    val pesoSecoKg: Double get() = resultantes.sumOf { it.pesoInicialKg }
    val mermaKg: Double get() = pesoEnviadoKg - pesoSecoKg

    val mermaPorcentaje: Double
        get() = if (pesoEnviadoKg > 0) mermaKg / pesoEnviadoKg * 100 else 0.0

    // Merma en pesos: kg perdidos x costo promedio por kg del café que se envió (promedio ponderado por kg).
    val valorMerma: Long
        get() {
            if (pesoEnviadoKg <= 0) return 0
            val costoPromedioKg = enviados.sumOf { it.envio.pesoEnviadoKg * it.sublote.costoKg } / pesoEnviadoKg
            return (mermaKg * costoPromedioKg).roundToLong()
        }
}
