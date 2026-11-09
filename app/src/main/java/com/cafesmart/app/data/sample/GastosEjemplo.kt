package com.cafesmart.app.data.sample

import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.TipoGasto

/** Datos sintéticos opcionales, separados de los registros ingresados por el usuario. */
fun gastosEjemplo(): List<Gasto> = listOf(
    Gasto(1001, "Transporte de café (ejemplo)", tipo = TipoGasto.TRANSPORTE,
        monto = 45000, fecha = Fecha(6, 11, 2026), estadoPago = EstadoPago.PENDIENTE),
    Gasto(1002, "Cargue en bodega (ejemplo)", tipo = TipoGasto.CARGUE,
        monto = 20000, fecha = Fecha(5, 11, 2026), estadoPago = EstadoPago.PAGADO)
)
