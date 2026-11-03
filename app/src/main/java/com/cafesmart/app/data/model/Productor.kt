package com.cafesmart.app.data.model

import com.cafesmart.app.data.model.TipoDocumento

// PRODUCTOR: quien vende el café a la cooperativa o compraventa.
//
// Tomé como referencia el `Productor` en data/purchase/Purchase.kt, pero no tiene id.
// Esta versión es la que se guardará en la base de datos.
// La compra "Sin registrar" (Tony Café, RN-V-01) NO crea un productor: se guarda con Compra.productorId = null.
//
// Regla: `documento` es único (validarDocumento ya rechaza los repetidos).
data class Productor(
    val id: Long = 0,              // 0 = todavía no guardado; la BD le asigna el id al insertarlo
    val nombre: String,
    val documento: String,         // solo dígitos: cédula de 6 a 10 o NIT de 9 a 10
    val tipoDocumento: TipoDocumento = TipoDocumento.CC,
    val telefono: String = ""      // opcional; si se escribe: 10 dígitos y empieza por 3
)
