package com.cafesmart.app.viewmodel.sale

import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.viewmodel.purchase.formatoKg

// Reglas del registro de venta (validaciones y calculos), segun el Figma "Ventas - Paso 2 - Cafe":
// la cantidad a vender no pasa de lo disponible en el sublote y el total sale de cantidad x precio.
// Son funciones puras (no dependen de Compose ni de Android) para poder probarlas con JUnit.
// El precio por kg ($1.000 a $100.000) tiene las mismas reglas que en Compras: se usan
// precioValido y errorPrecio de PurchaseRules, para no repetirlas.

// ---------------- Cantidad a vender ----------------

// Texto de ayuda y de error del campo "Cantidad a vender": "Debe ser menor o igual a 200 kg."
fun mensajeCantidad(disponibleKg: Double): String =
    "Debe ser menor o igual a ${formatoKg(disponibleKg)}."

// Valida la cantidad de un sublote: mayor que 0 y sin pasar de lo que queda disponible.
fun cantidadValida(cantidadKg: Double?, disponibleKg: Double): Boolean =
    cantidadKg != null && cantidadKg > 0 && cantidadKg <= disponibleKg

// El error aparece solo cuando la cantidad pasa de lo disponible.
// Con el campo vacio o en 0 no hay error: simplemente "Confirmar" sigue deshabilitado.
fun errorCantidad(cantidadKg: Double?, disponibleKg: Double): String? =
    if (cantidadKg != null && cantidadKg > disponibleKg) mensajeCantidad(disponibleKg) else null

// ---------------- Total ----------------

// Valor de un sublote: kg a vender x precio por kg, en pesos enteros.
// aPesos es la regla unica del modelo para redondear dinero (los .5 suben).
// Ejemplo (Figma, Paso 3): 150 kg x $20.000 = $3.000.000
fun subtotal(cantidadKg: Double, precioKg: Double): Long = (cantidadKg * precioKg).aPesos()
