package com.cafesmart.app.viewmodel.sale

import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.viewmodel.purchase.CANTIDAD_RECIENTES
import com.cafesmart.app.viewmodel.purchase.coincideBusqueda
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.normalizarEspacios
import com.cafesmart.app.viewmodel.purchase.productorValido
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarDocumento

// Reglas del registro de venta (validaciones y calculos), segun el Figma "Ventas - Paso 2 - Cafe":
// la cantidad a vender no pasa de lo disponible en el sublote y el total sale de cantidad x precio.
// Son funciones puras (no dependen de Compose ni de Android) para poder probarlas con JUnit.
// El precio por kg ($1.000 a $100.000) tiene las mismas reglas que en Compras: se usan
// precioValido y errorPrecio de PurchaseRules, para no repetirlas.
// Lo mismo pasa con el cliente: su formulario y su busqueda usan las reglas de PurchaseRules.

// ---------------- Paso 1: cliente ----------------

// Devuelve los clientes cuyo nombre o documento (cedula o NIT) contienen lo que se busco.
fun filtrarClientes(clientes: List<Cliente>, busqueda: String): List<Cliente> =
    if (busqueda.isBlank()) clientes else clientes.filter { coincideBusqueda(it.nombre, it.documento, busqueda) }

// Los clientes se guardan del mas antiguo al mas reciente; "Recientes" los muestra al reves.
// Sin busqueda muestra solo los ultimos; con busqueda, todos los que coinciden.
fun clientesVisibles(clientes: List<Cliente>, busqueda: String): List<Cliente> {
    val recientesPrimero = clientes.reversed()
    return if (busqueda.isNotBlank()) filtrarClientes(recientesPrimero, busqueda) else recientesPrimero.take(CANTIDAD_RECIENTES)
}

// true cuando hay que mostrar el boton "Ver más clientes".
fun hayMasClientes(clientes: List<Cliente>, busqueda: String): Boolean =
    busqueda.isBlank() && clientes.size > CANTIDAD_RECIENTES

// Lista de la hoja "Clientes": todos, del mas reciente al mas antiguo, segun la busqueda.
fun clientesDeLaLista(clientes: List<Cliente>, busqueda: String): List<Cliente> =
    filtrarClientes(clientes.reversed(), busqueda)

// Documentos ya registrados: un cliente nuevo no puede repetir ninguno.
fun documentosDe(clientes: List<Cliente>): List<String> = clientes.map { it.documento }

// El formulario "Cliente nuevo" es valido con las mismas reglas del productor (nombre, documento sin repetir y celular).
fun clienteValido(datos: NuevoCliente, clientes: List<Cliente>): Boolean =
    productorValido(datos, documentosDe(clientes))

// Convierte lo escrito en el formulario en un Cliente.
fun crearCliente(datos: NuevoCliente): Cliente =
    Cliente(
        nombre = normalizarEspacios(datos.nombre),
        documento = datos.documento,
        tipoDocumento = datos.tipo,
        telefono = datos.telefono
    )

// Continuar del paso 1 se activa segun la opcion elegida:
// uno de la lista pide haber elegido a alguien, "Sin registrar" siempre sirve y "Cliente nuevo" pide el formulario valido.
fun puedeContinuarPaso1(modo: ModoCliente?, elegido: Cliente?, nuevo: NuevoCliente, clientes: List<Cliente>): Boolean =
    when (modo) {
        ModoCliente.REGISTRADO -> elegido != null
        ModoCliente.SIN_REGISTRAR -> true
        ModoCliente.NUEVO -> clienteValido(nuevo, clientes)
        null -> false
    }

// Aviso general sobre "Continuar" cuando hay un campo marcado en rojo: el error exacto se ve solo en ese campo,
// asi lo que falta no se repite en dos lugares.
const val MENSAJE_REVISA_CAMPOS = "Por favor, revisa los campos que faltan por llenar."

// Mensaje sobre "Continuar" del paso 1; es null justo cuando puedeContinuarPaso1 es true.
// Solo dice algo especifico cuando no hay un campo que marcar (no se eligio opcion o cliente).
fun mensajePendientePaso1(modo: ModoCliente?, elegido: Cliente?, nuevo: NuevoCliente, clientes: List<Cliente>): String? =
    when (modo) {
        null -> "Por favor, selecciona una opción para continuar el proceso."
        ModoCliente.REGISTRADO -> if (elegido == null) "Elige un cliente de la lista." else null
        ModoCliente.SIN_REGISTRAR -> null
        ModoCliente.NUEVO -> if (loQueFaltaEnClienteNuevo(nuevo, clientes) != null) MENSAJE_REVISA_CAMPOS else null
    }

// Mensajes de un campo del formulario "Cliente nuevo" que todavia esta vacio.
const val MENSAJE_DOCUMENTO_VACIO = "Escribe el documento del cliente."

// "Escribe el nombre de la empresa." para un NIT; para una persona, "Escribe el nombre del cliente."
fun mensajeNombreVacio(tipo: TipoDocumento): String =
    if (tipo == TipoDocumento.NIT) "Escribe el nombre de la empresa." else "Escribe el nombre del cliente."

// Error del campo documento: vacio pide escribirlo (solo si el formulario esta abierto); escrito mal usa su error.
fun errorDocumentoNuevo(nuevo: NuevoCliente, clientes: List<Cliente>, formularioAbierto: Boolean): String? =
    when {
        nuevo.documento.isEmpty() -> if (formularioAbierto) MENSAJE_DOCUMENTO_VACIO else null
        else -> validarDocumento(nuevo.documento, nuevo.tipo, documentosDe(clientes))
    }

// Error del campo nombre. Vacio solo se marca cuando el documento ya esta bien: asi se marca un solo campo a la vez,
// el primero que falta.
fun errorNombreNuevo(nuevo: NuevoCliente, clientes: List<Cliente>, formularioAbierto: Boolean): String? =
    when {
        nuevo.nombre.isEmpty() ->
            if (formularioAbierto && errorDocumentoNuevo(nuevo, clientes, true) == null) mensajeNombreVacio(nuevo.tipo) else null
        else -> validarNombre(nuevo.nombre, nuevo.tipo)
    }

// Lo primero que falta en el formulario "Cliente nuevo", en el orden en que se llena: documento, nombre y celular.
// Es el mismo error que se ve en rojo en su campo.
fun loQueFaltaEnClienteNuevo(nuevo: NuevoCliente, clientes: List<Cliente>): String? =
    errorDocumentoNuevo(nuevo, clientes, true)
        ?: errorNombreNuevo(nuevo, clientes, true)
        ?: validarTelefono(nuevo.telefono)

// ---------------- Cantidad a vender ----------------

// Texto de ayuda y de error del campo "Cantidad a vender": "Debe ser menor o igual a 200 kg."
fun mensajeCantidad(disponibleKg: Double): String =
    "Debe ser menor o igual a ${formatoKg(disponibleKg)}."

// Valida la cantidad de un sublote: mayor que 0 y sin pasar de lo que queda disponible.
fun cantidadValida(cantidadKg: Double?, disponibleKg: Double): Boolean =
    cantidadKg != null && cantidadKg > 0 && cantidadKg <= disponibleKg

// Pasa el texto del campo a numero. Acepta coma o punto como separador decimal; devuelve null si esta vacio
// o no es un numero.
fun cantidadDesdeTexto(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()

// El error aparece solo cuando la cantidad pasa de lo disponible.
// Con el campo vacio o en 0 no hay error: simplemente "Confirmar" sigue deshabilitado.
fun errorCantidad(cantidadKg: Double?, disponibleKg: Double): String? =
    if (cantidadKg != null && cantidadKg > disponibleKg) mensajeCantidad(disponibleKg) else null

// ---------------- Total ----------------

// Valor de un sublote: kg a vender x precio por kg, en pesos enteros.
// aPesos es la regla unica del modelo para redondear dinero (los .5 suben).
// Ejemplo (Figma, Paso 3): 150 kg x $20.000 = $3.000.000
fun subtotal(cantidadKg: Double, precioKg: Double): Long = (cantidadKg * precioKg).aPesos()
