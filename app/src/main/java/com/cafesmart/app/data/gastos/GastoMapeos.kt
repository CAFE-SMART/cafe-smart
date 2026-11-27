package com.cafesmart.app.data.gastos

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.local.GastoEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.milisAFecha

// Paso entre las filas de Room y las data classes del modelo. Son funciones puras (sin Room ni Android)
// para probarlas con JUnit. Las data classes validan sus reglas al crearse: una fila dañada lanza un
// error en vez de mostrarse como si estuviera bien.

/** El id queda en 0 cuando el gasto es nuevo: Room asigna el definitivo al insertarlo. */
internal fun Gasto.aEntidad(): GastoEntity = GastoEntity(
    id = id,
    concepto = concepto,
    descripcion = descripcion,
    tipo = tipo.name,
    monto = monto,
    fecha = fechaAMilis(fecha),
    estadoPago = estadoPago.name,
    aplicaA = aplicaA.name,
    secadoId = secadoId,
)

internal fun GastoEntity.aModelo(): Gasto = Gasto(
    id = id,
    concepto = concepto,
    descripcion = descripcion,
    tipo = TipoGasto.valueOf(tipo),
    monto = monto,
    fecha = milisAFecha(fecha),
    estadoPago = EstadoPago.valueOf(estadoPago),
    aplicaA = AplicaGasto.valueOf(aplicaA),
    secadoId = secadoId,
)

/**
 * Un gasto como lo leen el Resumen y el Historial financiero: el monto COMPLETO del gasto (no lo que le toca
 * a un sublote), su concepto y a qué se aplica: "Gasto general", "Sublote 7", "2 sublotes" o "Secado".
 * [subloteIds] son los sublotes de su reparto.
 */
internal fun Gasto.aGastoRegistrado(subloteIds: List<Long> = emptyList()): GastoRegistrado = GastoRegistrado(
    fecha = fecha,
    montoPesos = monto,
    concepto = concepto,
    tipo = tipo,
    detalle = when (aplicaA) {
        AplicaGasto.GENERAL -> "Gasto general"
        AplicaGasto.SECADO -> "Secado"
        AplicaGasto.SUBLOTES -> when (subloteIds.size) {
            0 -> "Sublotes"
            1 -> "Sublote ${subloteIds[0]}"
            else -> "${subloteIds.size} sublotes"
        }
    },
)

internal fun SubloteEntity.aModelo(): Sublote = Sublote(
    id = id,
    loteId = loteId,
    compraId = compraId,
    secadoId = secadoId,
    pesoInicialKg = pesoInicialKg,
    pesoDisponibleKg = pesoDisponibleKg,
    costoKg = costoKg,
    fechaIngreso = milisAFecha(fechaIngreso),
    humedadPorcentaje = humedadPorcentaje,
    factorRendimiento = factorRendimiento,
)
