package com.cafesmart.app.data.model

// CLIENTE: a quien se le vende el café (trilladora, centro de acopio, otro comprador).
// En la sección Ventas hay "Cliente registrado" y "Sin registrar / Cliente nuevo", así que la venta
// necesita esta entidad (igual que Compra necesita Productor).
// Por tanto "Sin registrar" se guarda con Venta.clienteId = null.
data class Cliente(
    val id: Long = 0,
    val nombre: String,
    val documento: String,         // único
    val tipoDocumento: TipoDocumento = TipoDocumento.CC,
    val telefono: String = ""
)
