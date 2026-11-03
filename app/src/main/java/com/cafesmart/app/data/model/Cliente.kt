package com.cafesmart.app.data.model

import com.cafesmart.app.data.purchase.TipoDocumento

// CLIENTE: a quien se le vende el café (trilladora, centro de acopio, otro comprador).
// Allí´se toma en cuenta Cliente Registrado en sección Ventas.
// Sin registrar / Cliente nuevo", así que la venta necesita esta entidad (igual que Compra necesita Productor)
// Por tanto "Sin registrar" se guarda con Venta.clienteId = null.
data class Cliente(
    val id: Long = 0,
    val nombre: String,
    /* Documento` es único */
    val documento: String,
    val tipoDocumento: TipoDocumento = TipoDocumento.CC,
    val telefono: String = ""
)
