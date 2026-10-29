package com.cafesmart.app.domain

import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.PESO_MAX_KG
import com.cafesmart.app.data.PRECIO_MAX_KG
import com.cafesmart.app.data.PRECIO_MIN_KG
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.Sublote
import com.cafesmart.app.data.TipoDocumento
import java.util.Calendar

// Capa de Dominio: reglas del registro de compra (validaciones y calculos).
// Son funciones puras: no dependen de Compose ni de Android, por eso se prueban con JUnit.

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

// Validaciones y mensajes segun los requerimientos de Compras del equipo (RF-COM-01 a RF-COM-13).
// El diseno (etiquetas, ejemplos y ayudas de los campos) sigue el Figma.

const val NOMBRE_MAX = 30

// Quita espacios de mas: "  Juan   Perez " -> "Juan Perez"
fun normalizarEspacios(texto: String): String =
    texto.trim().split(" ").filter { it.isNotEmpty() }.joinToString(" ")

// RF-COM-05: solo letras y espacios
fun limpiarNombre(texto: String): String =
    texto.filter { it.isLetter() || it == ' ' }.take(NOMBRE_MAX)

// RF-COM-04: solo digitos, sin puntos ni espacios; cedula y NIT hasta 10 digitos
fun limpiarDocumento(texto: String): String = texto.filter { it.isDigit() }.take(10)

fun limpiarTelefono(texto: String): String = texto.filter { it.isDigit() }.take(10)

// Cada validacion devuelve el mensaje de error, o null si el valor es valido

// RF-COM-05: persona = nombre y apellido; empresa = su nombre
fun validarNombre(nombre: String, tipo: TipoDocumento): String? {
    val palabras = normalizarEspacios(nombre).split(" ").filter { it.isNotEmpty() }
    return when {
        tipo == TipoDocumento.NIT && palabras.isEmpty() -> "Escribe el nombre de la empresa."
        tipo == TipoDocumento.NIT -> null
        palabras.size < 2 -> "Escribe tu nombre y apellido."
        palabras.any { it.length < 2 } -> "Escribe tu nombre y apellido."
        else -> null
    }
}

// RF-COM-04: cedula de 6 a 10 digitos, NIT de 9 a 10, y que no este repetido
fun validarDocumento(documento: String, tipo: TipoDocumento, productores: List<Productor>): String? =
    when {
        documento.isEmpty() -> "Escribe solo números, sin puntos, comas ni espacios."
        tipo == TipoDocumento.CC && documento.length < 6 -> "La cédula debe tener entre 6 y 10 números."
        tipo == TipoDocumento.NIT && documento.length < 9 -> "El NIT debe tener 9 o 10 números."
        productores.any { it.documento == documento } -> "Este documento ya está registrado. Verifica en tu lista."
        else -> null
    }

// RF-COM-06: opcional; si se escribe, 10 digitos y empieza por 3
fun validarTelefono(telefono: String): String? =
    if (telefono.isEmpty() || (telefono.length == 10 && telefono.startsWith("3"))) {
        null
    } else {
        "Recuerda, tu celular debe tener 10 números y empezar por el 3."
    }

fun productorValido(datos: NuevoProductor, productores: List<Productor>): Boolean =
    validarNombre(datos.nombre, datos.tipo) == null &&
        validarDocumento(datos.documento, datos.tipo, productores) == null &&
        validarTelefono(datos.telefono) == null

fun crearProductor(datos: NuevoProductor): Productor =
    Productor(
        nombre = normalizarEspacios(datos.nombre),
        documento = datos.documento,
        tipoDocumento = datos.tipo,
        telefono = datos.telefono
    )

// Continuar del paso 1 se activa segun la opcion elegida
fun puedeContinuarPaso1(
    modo: ModoProductor?,
    elegido: Productor?,
    nuevo: NuevoProductor,
    productores: List<Productor>
): Boolean =
    when (modo) {
        ModoProductor.REGISTRADO -> elegido != null
        ModoProductor.SIN_REGISTRAR -> true
        ModoProductor.NUEVO -> productorValido(nuevo, productores)
        null -> false
    }

// Mensaje sobre "Continuar" que dice que falta (el boton sigue deshabilitado, como pide la guia).
// RF-COM-01 y RF-COM-02
fun mensajePendientePaso1(modo: ModoProductor?, elegido: Productor?, nuevo: NuevoProductor, productores: List<Productor>): String? =
    when {
        modo == null -> "Por favor, selecciona una opción para continuar el proceso."
        modo == ModoProductor.REGISTRADO && elegido == null -> "Elige un productor de la lista."
        modo == ModoProductor.NUEVO && !productorValido(nuevo, productores) -> "Completa los datos del productor nuevo."
        else -> null
    }

// RF-COM-02: cuando la busqueda no encuentra a nadie
const val SIN_RESULTADOS = "Lo siento, no pudimos encontrarlo. Prueba con otro nombre o cédula."

// ---------------- Paso 2: cafe ----------------

const val MENSAJE_PESO = "Escribe un peso mayor a 0 y hasta 99.999 kg."
const val MENSAJE_PRECIO = "Escribe un valor entre $1.000 y $100.000 pesos colombianos."

// RF-COM-09: solo digitos y un separador decimal (acepta coma o punto), maximo 3 decimales
fun limpiarPeso(texto: String): String {
    val normalizado = texto.replace(',', '.').filter { it.isDigit() || it == '.' }
    val punto = normalizado.indexOf('.')
    return if (punto == -1) {
        normalizado.take(5)
    } else {
        val entero = normalizado.substring(0, punto).take(5)
        val decimales = normalizado.substring(punto + 1).replace(".", "").take(3)
        "$entero.$decimales"
    }
}

// RF-COM-10: precio por kg en pesos enteros, solo digitos
fun limpiarPrecio(texto: String): String = texto.filter { it.isDigit() }.take(6)

fun pesoKg(cafe: CafeFormulario): Double? = cafe.peso.toDoubleOrNull()

fun precioKg(cafe: CafeFormulario): Double? = limpiarPrecio(cafe.precio).toDoubleOrNull()

fun pesoValido(cafe: CafeFormulario): Boolean {
    val peso = pesoKg(cafe)
    return peso != null && peso > 0 && peso <= PESO_MAX_KG
}

fun precioValido(cafe: CafeFormulario): Boolean {
    val precio = precioKg(cafe)
    return precio != null && precio >= PRECIO_MIN_KG && precio <= PRECIO_MAX_KG
}

// El error del campo aparece cuando ya se escribio algo y esta fuera de rango
fun errorPeso(cafe: CafeFormulario): String? {
    val peso = pesoKg(cafe) ?: return null
    return if (peso <= 0 || peso > PESO_MAX_KG) MENSAJE_PESO else null
}

// El minimo ($1.000) se revisa cuando ya hay 4 digitos, para no marcar error mientras se escribe
fun errorPrecio(cafe: CafeFormulario): String? {
    val precio = precioKg(cafe) ?: return null
    val muyBajo = cafe.precio.length >= 4 && precio < PRECIO_MIN_KG
    return if (muyBajo || precio > PRECIO_MAX_KG) MENSAJE_PRECIO else null
}

// Convierte la tarjeta en un sublote, o null si falta algun dato o esta fuera de rango
fun crearSublote(cafe: CafeFormulario): Sublote? {
    val tipo = cafe.tipoCafe
    val calidad = cafe.calidad
    val peso = pesoKg(cafe)
    val precio = precioKg(cafe)
    return if (tipo != null && calidad != null && peso != null && precio != null &&
        pesoValido(cafe) && precioValido(cafe)
    ) {
        Sublote(tipoCafe = tipo, calidad = calidad, pesoKg = peso, precioKg = precio)
    } else {
        null
    }
}

// Todas las tarjetas tienen sus datos completos y validos
fun cafesCompletos(cafes: List<CafeFormulario>): Boolean = cafes.all { crearSublote(it) != null }

// Que le falta a una tarjeta "Café N" (RF-COM-07 a RF-COM-10)
fun loQueFaltaEnCafe(cafe: CafeFormulario): String? =
    when {
        cafe.tipoCafe == null -> "Elige el tipo de café que comprarás."
        cafe.calidad == null -> "Elige la calidad del café que te interesa."
        !pesoValido(cafe) -> MENSAJE_PESO
        !precioValido(cafe) -> MENSAJE_PRECIO
        else -> null
    }

// Mensaje sobre "Continuar" del paso 2: lo que falta en la primera tarjeta incompleta
fun mensajePendientePaso2(cafes: List<CafeFormulario>): String? {
    val indice = cafes.indexOfFirst { loQueFaltaEnCafe(it) != null }
    if (indice == -1) return null
    val falta = loQueFaltaEnCafe(cafes[indice])
    return if (cafes.size > 1) "Café ${indice + 1}: $falta" else falta
}

// RF-COM-11: solo se agrega otro cafe si los anteriores estan completos
fun mensajeAgregarCafe(cafes: List<CafeFormulario>): String? {
    val indice = cafes.indexOfFirst { crearSublote(it) == null }
    return if (indice == -1) null else "Recuerda terminar de diligenciar los datos de Café ${indice + 1} antes de agregar otro."
}

// RF-COM-13: los totales suman lo que ya se ha escrito en todas las tarjetas
fun totalKg(cafes: List<CafeFormulario>): Double = cafes.sumOf { pesoKg(it) ?: 0.0 }

fun totalEstimado(cafes: List<CafeFormulario>): Double =
    cafes.sumOf { (pesoKg(it) ?: 0.0) * (precioKg(it) ?: 0.0) }

// ---------------- Fecha ----------------

// Fecha de hoy con Calendar (existe en todas las versiones de Android).
// Calendar cuenta los meses desde 0 (enero = 0), por eso se suma 1
fun fechaDeHoy(): Fecha {
    val calendario = Calendar.getInstance()
    return Fecha(
        dia = calendario.get(Calendar.DAY_OF_MONTH),
        mes = calendario.get(Calendar.MONTH) + 1,
        anio = calendario.get(Calendar.YEAR)
    )
}
