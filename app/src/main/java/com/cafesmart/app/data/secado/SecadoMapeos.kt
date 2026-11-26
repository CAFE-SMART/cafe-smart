package com.cafesmart.app.data.secado

import com.cafesmart.app.data.local.SecadoEntity
import com.cafesmart.app.data.local.SecadoSubloteEntity
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Secado
import com.cafesmart.app.data.model.SecadoSublote
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.milisAFecha

// Paso entre las filas de Room y las data classes del modelo (igual que data/gastos/GastoMapeos.kt). Son funciones
// puras (sin Room ni Android) para probarlas con JUnit. Las data classes validan sus reglas al crearse: una fila
// dañada lanza un error en vez de mostrarse como si estuviera bien.

/** El id queda en 0 cuando el secado es nuevo: Room asigna el definitivo al insertarlo. */
internal fun Secado.aEntidad(): SecadoEntity = SecadoEntity(
    id = id,
    fechaInicio = fechaAMilis(fechaInicio),
    fechaFin = fechaFin?.let { fechaAMilis(it) },
    estado = estado.name,
    factorRendimiento = factorRendimiento,
)

internal fun SecadoEntity.aModelo(): Secado = Secado(
    id = id,
    fechaInicio = milisAFecha(fechaInicio),
    fechaFin = fechaFin?.let { milisAFecha(it) },
    estado = EstadoSecado.valueOf(estado),
    factorRendimiento = factorRendimiento,
)

internal fun SecadoSublote.aEntidad(): SecadoSubloteEntity = SecadoSubloteEntity(
    secadoId = secadoId,
    subloteId = subloteId,
    pesoEnviadoKg = pesoEnviadoKg,
)

internal fun SecadoSubloteEntity.aModelo(): SecadoSublote = SecadoSublote(
    secadoId = secadoId,
    subloteId = subloteId,
    pesoEnviadoKg = pesoEnviadoKg,
)
