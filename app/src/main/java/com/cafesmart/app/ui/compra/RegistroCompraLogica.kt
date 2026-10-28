package com.cafesmart.app.ui.compra

import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.PESO_MAX_KG
import com.cafesmart.app.data.PRECIO_MAX_KG
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.Sublote
import com.cafesmart.app.data.TipoDocumento

// Funciones puras del registro de compra: no dependen de Compose y se prueban con JUnit

// ---------------- Paso 1: productor ----------------

const val CANTIDAD_RECIENTES = 3

fun filtrarProductores(productores: List<Productor>, busqueda: String): List<Productor> =
    if (busqueda.isBlank()) {
        productores
    } else {
        productores.filter {
            it.nombre.contains(busqueda.trim(), ignoreCase = true) ||
                it.documento.contains(busqueda.trim())
        }
    }

// Sin busqueda muestra solo los ultimos registrados ("Recientes"), salvo que se pida ver todos
fun productoresVisibles(productores: List<Productor>, busqueda: String, verTodos: Boolean): List<Productor> {
    val recientesPrimero = productores.reversed()
    return if (busqueda.isNotBlank()) {
        filtrarProductores(recientesPrimero, busqueda)
    } else if (verTodos) {
        recientesPrimero
    } else {
        recientesPrimero.take(CANTIDAD_RECIENTES)
    }
}

fun hayMasProductores(productores: List<Productor>, busqueda: String, verTodos: Boolean): Boolean =
    busqueda.isBlank() && !verTodos && productores.size > CANTIDAD_RECIENTES

// Mismas reglas que personValidation.ts de la version web

const val NOMBRE_MAX = 30

// Quita espacios de mas: "  Juan   Perez " -> "Juan Perez"
fun normalizarEspacios(texto: String): String =
    texto.trim().split(" ").filter { it.isNotEmpty() }.joinToString(" ")

// Mientras se escribe: deja solo los caracteres permitidos para cada tipo y corta al maximo
fun limpiarNombre(texto: String, tipo: TipoDocumento): String =
    when (tipo) {
        // Persona: letras, espacios y . ' -
        TipoDocumento.CC -> texto.filter { it.isLetter() || it == ' ' || it in ".'-" }
        // Empresa: ademas permite numeros y & /
        TipoDocumento.NIT -> texto.filter { it.isLetterOrDigit() || it == ' ' || it in ".'-&/" }
    }.take(NOMBRE_MAX)

fun limpiarDocumento(texto: String, tipo: TipoDocumento): String =
    when (tipo) {
        TipoDocumento.CC -> texto.filter { it.isDigit() }.take(10)
        TipoDocumento.NIT -> texto.filter { it.isDigit() || it == '-' }.take(17)
    }

fun limpiarTelefono(texto: String): String = texto.filter { it.isDigit() }.take(10)

// Cada validacion devuelve el mensaje de error, o null si el valor es valido
fun validarNombre(nombre: String, tipo: TipoDocumento): String? {
    val limpio = normalizarEspacios(nombre)
    val palabras = limpio.split(" ")
    return when {
        limpio.isEmpty() && tipo == TipoDocumento.NIT -> "Ingresa el nombre de la empresa para continuar."
        limpio.isEmpty() -> "Escribe el nombre y apellido para continuar."
        limpio.none { it.isLetter() } -> "Ingresa un nombre válido."
        tipo == TipoDocumento.NIT -> null
        palabras.size < 2 -> "Completa el nombre y apellido para continuar."
        palabras.any { palabra -> palabra.count { it.isLetter() } < 2 } -> "Completa el nombre y apellido para continuar."
        else -> null
    }
}

// NIT valido: solo numeros, o numeros + guion + un digito de verificacion (900123456-7)
fun nitBienEscrito(nit: String): Boolean {
    val partes = nit.split("-")
    return when (partes.size) {
        1 -> partes[0].isNotEmpty() && partes[0].all { it.isDigit() }
        2 -> partes[0].isNotEmpty() && partes[0].all { it.isDigit() } &&
            partes[1].length == 1 && partes[1][0].isDigit()
        else -> false
    }
}

fun validarDocumento(documento: String, tipo: TipoDocumento): String? {
    val limpio = documento.trim()
    val digitos = limpio.count { it.isDigit() }
    return when {
        limpio.isEmpty() -> "Escribe el número de documento para continuar."
        tipo == TipoDocumento.CC && digitos < 6 -> "Verifica que la cédula esté completa (6 a 10 dígitos)."
        tipo == TipoDocumento.NIT && !nitBienEscrito(limpio) ->
            "El NIT lleva solo números y, si quieres, un guion con el dígito de verificación."
        tipo == TipoDocumento.NIT && (digitos < 5 || digitos > 15) -> "Verifica que el NIT esté completo."
        else -> null
    }
}

// El celular es opcional: vacio es valido
fun validarTelefono(telefono: String): String? =
    if (telefono.isEmpty() || (telefono.length == 10 && telefono.startsWith("3"))) {
        null
    } else {
        "Debe ser un celular de 10 dígitos que empieza por 3."
    }

fun productorValido(datos: NuevoProductor): Boolean =
    validarNombre(datos.nombre, datos.tipo) == null &&
        validarDocumento(datos.documento, datos.tipo) == null &&
        validarTelefono(datos.telefono) == null

fun crearProductor(datos: NuevoProductor): Productor =
    Productor(
        nombre = normalizarEspacios(datos.nombre),
        documento = datos.documento.trim(),
        tipoDocumento = datos.tipo,
        telefono = datos.telefono
    )

// Continuar del paso 1 se activa segun la opcion elegida
fun puedeContinuarPaso1(modo: ModoProductor?, elegido: Productor?, nuevo: NuevoProductor): Boolean =
    when (modo) {
        ModoProductor.REGISTRADO -> elegido != null
        ModoProductor.SIN_REGISTRAR -> true
        ModoProductor.NUEVO -> productorValido(nuevo)
        null -> false
    }

// ---------------- Paso 2: cafe ----------------

// Peso: solo digitos y un separador decimal (acepta coma o punto)
fun limpiarPeso(texto: String): String {
    val normalizado = texto.replace(',', '.').filter { it.isDigit() || it == '.' }
    val punto = normalizado.indexOf('.')
    val unSoloPunto = if (punto == -1) {
        normalizado
    } else {
        normalizado.substring(0, punto + 1) + normalizado.substring(punto + 1).replace(".", "")
    }
    return unSoloPunto.take(9)
}

// Precio por kg: solo digitos (los pesos no llevan decimales)
fun limpiarPrecio(texto: String): String = texto.filter { it.isDigit() }.take(6)

// Lo que se ve en el campo de precio: "14000" -> "14.000"
fun precioEnPantalla(texto: String): String {
    val digitos = limpiarPrecio(texto)
    return if (digitos.isEmpty()) "" else formatoMiles(digitos.toLong())
}

fun pesoKg(cafe: CafeFormulario): Double? = cafe.peso.toDoubleOrNull()

fun precioKg(cafe: CafeFormulario): Double? = limpiarPrecio(cafe.precio).toDoubleOrNull()

fun errorPeso(cafe: CafeFormulario): String? {
    val peso = pesoKg(cafe)
    return if (peso != null && peso > PESO_MAX_KG) "Máximo 99.999 kg" else null
}

fun errorPrecio(cafe: CafeFormulario): String? {
    val precio = precioKg(cafe)
    return if (precio != null && precio > PRECIO_MAX_KG) "Máximo $100.000 por kg" else null
}

// Convierte la tarjeta en un sublote, o null si falta algun dato o esta fuera de rango
fun crearSublote(cafe: CafeFormulario): Sublote? {
    val tipo = cafe.tipoCafe
    val calidad = cafe.calidad
    val peso = pesoKg(cafe)
    val precio = precioKg(cafe)
    return if (tipo != null && calidad != null &&
        peso != null && peso > 0 && peso <= PESO_MAX_KG &&
        precio != null && precio > 0 && precio <= PRECIO_MAX_KG
    ) {
        Sublote(tipoCafe = tipo, calidad = calidad, pesoKg = peso, precioKg = precio)
    } else {
        null
    }
}

// Todas las tarjetas tienen sus datos completos y validos
fun cafesCompletos(cafes: List<CafeFormulario>): Boolean = cafes.all { crearSublote(it) != null }

// Los totales suman lo que ya se ha escrito en todas las tarjetas
fun totalKg(cafes: List<CafeFormulario>): Double = cafes.sumOf { pesoKg(it) ?: 0.0 }

fun totalEstimado(cafes: List<CafeFormulario>): Double =
    cafes.sumOf { (pesoKg(it) ?: 0.0) * (precioKg(it) ?: 0.0) }
