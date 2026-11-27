package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo

// Ayudas para las pruebas de Secado (no son pruebas).

// Hoy en el Figma de Secado: 3 de junio de 2026.
val HOY_FIGMA = Fecha(3, 6, 2026)

// Arma un sublote con su lote. Si no se dice, entro con lo mismo que tiene disponible (o 10 kg si esta agotado).
fun subloteDePrueba(
    id: Long,
    tipo: TipoCafe,
    calidad: Calidad,
    disponibleKg: Double,
    fecha: Fecha = HOY_FIGMA,
    inicialKg: Double = if (disponibleKg > 0) disponibleKg else 10.0
): SubloteConLote {
    val lote = Lote(id = tipo.ordinal * 10L + calidad.ordinal + 1, tipoCafe = tipo, calidad = calidad)
    val sublote = Sublote(
        id = id,
        loteId = lote.id,
        compraId = id,
        pesoInicialKg = inicialKg,
        pesoDisponibleKg = disponibleKg,
        costoKg = 10_000,
        fechaIngreso = fecha
    )
    return SubloteConLote(sublote, lote)
}

// El inventario del Figma "Secado - Paso 1": 4 sublotes verdes que suman 290 kg
// (3: buena 60 kg, 1: regular 120 kg, 2: regular 80 kg, 4: mala 30 kg), mas un sublote seco que NO se puede secar.
fun inventarioFigma(): List<SubloteConLote> = listOf(
    subloteDePrueba(1, TipoCafe.VERDE, Calidad.REGULAR, 120.0, fecha = Fecha(3, 6, 2026)),
    subloteDePrueba(2, TipoCafe.VERDE, Calidad.REGULAR, 80.0, fecha = Fecha(1, 6, 2026)),
    subloteDePrueba(3, TipoCafe.VERDE, Calidad.BUENA, 60.0, fecha = Fecha(31, 5, 2026)),
    subloteDePrueba(4, TipoCafe.VERDE, Calidad.MALA, 30.0, fecha = Fecha(29, 5, 2026)),
    subloteDePrueba(5, TipoCafe.SECO, Calidad.BUENA, 200.0)
)

fun repositorioDePrueba(inventario: List<SubloteConLote> = inventarioFigma()) = SecadoRepositoryEjemplo(inventario)
