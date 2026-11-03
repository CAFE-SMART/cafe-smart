package com.cafesmart.app.data.model

// Fecha simple: día, mes (1 a 12) y año.
// Se usa en vez de java.time.LocalDate porque LocalDate solo existe desde Android 8 (API 26)
// y el proyecto debe funcionar desde Android 7 (minSdk 24).
// Para guardarla en Room se convierte con fechaAMilis / milisAFecha (viewmodel/purchase/Formatters.kt).
data class Fecha(val dia: Int, val mes: Int, val anio: Int)
