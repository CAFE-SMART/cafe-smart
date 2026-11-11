package com.cafesmart.app.data.sale

import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.TipoDocumento

// Clientes de ejemplo (nombres del Figma) para ver las pantallas Buscar y Seleccionar.
// Temporal: se reemplazan por los datos reales cuando se incorpore Room.
// Va del mas antiguo al mas reciente, igual que productoresDeEjemplo; "Recientes" los muestra al reves.
// El documento se guarda solo con digitos (el NIT del Figma "900.342.121-5" se escribe 9003421215).
val clientesDeEjemplo = listOf(
    Cliente(nombre = "Beneficiadora Las Palmas S.A.S.", documento = "900123456", tipoDocumento = TipoDocumento.NIT),
    Cliente(nombre = "Carlos Mejía", documento = "16780012"),
    Cliente(nombre = "Exportadora del Sur S.A.", documento = "9003421215", tipoDocumento = TipoDocumento.NIT)
)
