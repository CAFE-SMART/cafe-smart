package com.cafesmart.app.data.model

import kotlin.math.roundToLong

// El modelo guarda el dinero en pesos enteros (Long); el formulario de compra todavía trabaja con
// Double (CafeValidado.precioKg). Regla única para convertir: redondear al peso más cercano
// (los .5 suben). Usar siempre esta función al pasar de Double a Long, para que todos redondeen igual.
fun Double.aPesos(): Long = roundToLong()
