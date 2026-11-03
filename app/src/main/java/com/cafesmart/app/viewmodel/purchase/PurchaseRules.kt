package com.cafesmart.app.viewmodel.purchase

import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.model.TipoDocumento
import java.util.Calendar

// Reglas del registro de compra (validaciones y calculos): son parte de la logica del ViewModel.
// Estan en un archivo aparte como funciones puras (no dependen de Compose ni de Android)
// para poder probarlas con JUnit.

// Limites de peso y precio por kg
const val PESO_MAX_KG = 99_999
const val PRECIO_MIN_KG = 1_000
const val PRECIO_MAX_KG = 100_000

// ---------------- Paso 1: productor ----------------

// El Figma muestra 2 recientes y el boton "Ver más productores"
// const val: valor fijo que se conoce al compilar.
const val CANTIDAD_RECIENTES = 2

// Devuelve los productores cuyo nombre o documento (cedula o NIT) contienen lo que se busco.
fun filtrarProductores(productores: List<Productor>, busqueda: String): List<Productor> =
    // isBlank: esta vacio o solo tiene espacios. Sin busqueda se devuelven todos.
    if (busqueda.isBlank()) {
        productores
    } else {
        // filter crea una lista nueva solo con los que cumplen la condicion.
        // it es cada productor; trim() quita espacios; ignoreCase ignora mayusculas.
        productores.filter {
            it.nombre.contains(busqueda.trim(), ignoreCase = true) ||
                it.documento.contains(busqueda.trim())
        }
    }

// Sin busqueda muestra solo los ultimos registrados ("Recientes"), salvo que se pida ver todos
// Decide que productores se ven: resultados, todos o solo los recientes.
fun productoresVisibles(productores: List<Productor>, busqueda: String): List<Productor> {
    // reversed() invierte la lista: los ultimos registrados quedan de primeros.
    val recientesPrimero = productores.reversed()
    return if (busqueda.isNotBlank()) {
        filtrarProductores(recientesPrimero, busqueda)
    } else {
        // take(n) toma solo los primeros n.
        recientesPrimero.take(CANTIDAD_RECIENTES)
    }
}

// true cuando hay que mostrar el boton "Ver más productores".
fun hayMasProductores(productores: List<Productor>, busqueda: String): Boolean =
    busqueda.isBlank() && productores.size > CANTIDAD_RECIENTES

// Lista de la hoja "Productores": todos, del mas reciente al mas antiguo, segun la busqueda.
fun productoresDeLaLista(productores: List<Productor>, busqueda: String): List<Productor> =
    filtrarProductores(productores.reversed(), busqueda)

// Validaciones y mensajes segun los requerimientos de Compras del equipo (RF-COM-01 a RF-COM-13).
// El diseno (etiquetas, ejemplos y ayudas de los campos) sigue el Figma.

const val NOMBRE_MAX = 30

// Quita espacios de mas: "  Juan   Perez " -> "Juan Perez"
fun normalizarEspacios(texto: String): String =
    // split parte el texto por espacios, filter quita los pedazos vacios y joinToString los une.
    texto.trim().split(" ").filter { it.isNotEmpty() }.joinToString(" ")

// RF-COM-05: solo letras y espacios
fun limpiarNombre(texto: String): String =
    // Deja solo letras y espacios, y corta a NOMBRE_MAX caracteres.
    texto.filter { it.isLetter() || it == ' ' }.take(NOMBRE_MAX)

// RF-COM-04: solo digitos, sin puntos ni espacios; cedula y NIT hasta 10 digitos
// Deja solo digitos, maximo 10.
fun limpiarDocumento(texto: String): String = texto.filter { it.isDigit() }.take(10)

// Deja solo digitos, maximo 10.
fun limpiarTelefono(texto: String): String = texto.filter { it.isDigit() }.take(10)

// Cada validacion devuelve el mensaje de error, o null si el valor es valido

// RF-COM-05: persona = nombre y apellido; empresa = su nombre
fun validarNombre(nombre: String, tipo: TipoDocumento): String? {
    // Separa el nombre en palabras para contar nombre y apellido.
    val palabras = normalizarEspacios(nombre).split(" ").filter { it.isNotEmpty() }
    // when revisa las condiciones en orden y devuelve la primera que se cumpla.
    return when {
        tipo == TipoDocumento.NIT && palabras.isEmpty() -> "Escribe el nombre de la empresa."
        tipo == TipoDocumento.NIT -> null
        palabras.size < 2 -> "Escribe tu nombre y apellido."
        // any: true si alguna palabra tiene menos de 2 letras (por ejemplo "Juan P").
        palabras.any { it.length < 2 } -> "Escribe tu nombre y apellido."
        else -> null
    }
}

// RF-COM-04: cedula de 6 a 10 digitos, NIT de 9 a 10, y que no este repetido
// Recibe la lista de productores para detectar documentos repetidos.
fun validarDocumento(documento: String, tipo: TipoDocumento, productores: List<Productor>): String? =
    when {
        documento.isEmpty() -> "Escribe solo números, sin puntos, comas ni espacios."
        tipo == TipoDocumento.CC && documento.length < 6 -> "La cédula debe tener entre 6 y 10 números."
        tipo == TipoDocumento.NIT && documento.length < 9 -> "El NIT debe tener 9 o 10 números."
        // any: true si algun productor ya tiene este documento.
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

// El formulario es valido cuando ninguna validacion devuelve mensaje de error.
fun productorValido(datos: NuevoProductor, productores: List<Productor>): Boolean =
    validarNombre(datos.nombre, datos.tipo) == null &&
        validarDocumento(datos.documento, datos.tipo, productores) == null &&
        validarTelefono(datos.telefono) == null

// Convierte lo escrito en el formulario en un Productor.
fun crearProductor(datos: NuevoProductor): Productor =
    Productor(
        nombre = normalizarEspacios(datos.nombre),
        documento = datos.documento,
        tipoDocumento = datos.tipo,
        telefono = datos.telefono
    )

// Continuar del paso 1 se activa segun la opcion elegida.
fun puedeContinuarPaso1(modo: ModoProductor?, elegido: Productor?): Boolean =
    when (modo) {
        ModoProductor.REGISTRADO -> elegido != null
        ModoProductor.SIN_REGISTRAR -> true
        null -> false
    }

// Mensaje sobre "Continuar" que dice que falta (RF-COM-01 y RF-COM-02).
fun mensajePendientePaso1(modo: ModoProductor?, elegido: Productor?): String? =
    when {
        modo == null -> "Por favor, selecciona una opción para continuar el proceso."
        modo == ModoProductor.REGISTRADO && elegido == null -> "Elige un productor de la lista."
        else -> null
    }

// RF-COM-02: cuando la busqueda no encuentra a nadie
const val SIN_RESULTADOS = "Lo siento, no pudimos encontrarlo. Prueba con otro nombre o cédula."

// ---------------- Paso 2: cafe ----------------

const val MENSAJE_PESO = "Escribe un peso mayor a 0 y hasta 99.999 kg."
const val MENSAJE_PRECIO = "Escribe un valor entre $1.000 y $100.000 pesos colombianos."

// RF-COM-09: solo digitos y un separador decimal (acepta coma o punto), maximo 3 decimales
// Primero cambia la coma por punto y deja solo digitos y puntos.
fun limpiarPeso(texto: String): String {
    val normalizado = texto.replace(',', '.').filter { it.isDigit() || it == '.' }
    // indexOf devuelve la posicion del primer punto, o -1 si no hay.
    val punto = normalizado.indexOf('.')
    return if (punto == -1) {
        normalizado.take(5)
    } else {
        // Parte entera: hasta 5 digitos (99.999 kg como maximo).
        val entero = normalizado.substring(0, punto).take(5)
        // Parte decimal: quita otros puntos y deja maximo 3 decimales.
        val decimales = normalizado.substring(punto + 1).replace(".", "").take(3)
        "$entero.$decimales"
    }
}

// RF-COM-10: precio por kg en pesos enteros, solo digitos
fun limpiarPrecio(texto: String): String = texto.filter { it.isDigit() }.take(6)

// toDoubleOrNull convierte el texto en numero; devuelve null si no se puede.
fun pesoKg(cafe: CafeFormulario): Double? = cafe.peso.toDoubleOrNull()

// Convierte el precio a numero despues de dejar solo digitos.
fun precioKg(cafe: CafeFormulario): Double? = limpiarPrecio(cafe.precio).toDoubleOrNull()

// Mayor a 0 y sin pasar del maximo.
fun pesoValido(cafe: CafeFormulario): Boolean {
    val peso = pesoKg(cafe)
    return peso != null && peso > 0 && peso <= PESO_MAX_KG
}

// Entre el precio minimo y el maximo.
fun precioValido(cafe: CafeFormulario): Boolean {
    val precio = precioKg(cafe)
    return precio != null && precio >= PRECIO_MIN_KG && precio <= PRECIO_MAX_KG
}

// El error del campo aparece cuando ya se escribio algo y esta fuera de rango
fun errorPeso(cafe: CafeFormulario): String? {
    // ?: return null: si el campo esta vacio no hay error que mostrar.
    val peso = pesoKg(cafe) ?: return null
    return if (peso <= 0 || peso > PESO_MAX_KG) MENSAJE_PESO else null
}

// El minimo ($1.000) se revisa cuando ya hay 4 digitos, para no marcar error mientras se escribe
fun errorPrecio(cafe: CafeFormulario): String? {
    // ?: return null: si el campo esta vacio no hay error que mostrar.
    val precio = precioKg(cafe) ?: return null
    val muyBajo = cafe.precio.length >= 4 && precio < PRECIO_MIN_KG
    return if (muyBajo || precio > PRECIO_MAX_KG) MENSAJE_PRECIO else null
}

// Convierte la tarjeta en un sublote, o null si falta algun dato o esta fuera de rango
fun crearSublote(cafe: CafeFormulario): CafeValidado? {
    val tipo = cafe.tipoCafe
    val calidad = cafe.calidad
    val peso = pesoKg(cafe)
    val precio = precioKg(cafe)
    // Solo crea el sublote si todos los datos estan completos y en rango.
    return if (tipo != null && calidad != null && peso != null && precio != null &&
        pesoValido(cafe) && precioValido(cafe)
    ) {
        CafeValidado(tipoCafe = tipo, calidad = calidad, pesoKg = peso, precioKg = precio)
    } else {
        null
    }
}

// Todas las tarjetas tienen sus datos completos y validos
// all: true si todas las tarjetas cumplen la condicion.
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
    // indexOfFirst: posicion de la primera tarjeta incompleta, o -1 si todas estan bien.
    val indice = cafes.indexOfFirst { loQueFaltaEnCafe(it) != null }
    // Si todas estan completas no hay nada que avisar.
    if (indice == -1) return null
    val falta = loQueFaltaEnCafe(cafes[indice])
    // Con varias tarjetas se aclara en cual falta el dato.
    return if (cafes.size > 1) "Café ${indice + 1}: $falta" else falta
}

// RF-COM-11: solo se agrega otro cafe si los anteriores estan completos
fun mensajeAgregarCafe(cafes: List<CafeFormulario>): String? {
    val indice = cafes.indexOfFirst { crearSublote(it) == null }
    return if (indice == -1) null else "Recuerda terminar de diligenciar los datos de Café ${indice + 1} antes de agregar otro."
}

// Valor de un sublote: peso x precio por kg
fun totalDe(sublote: CafeValidado): Double = sublote.pesoKg * sublote.precioKg

// RF-COM-13: los totales suman lo que ya se ha escrito en todas las tarjetas
// sumOf suma el valor de cada tarjeta; ?: 0.0 usa cero si el peso aun no se ha escrito.
fun totalKg(cafes: List<CafeFormulario>): Double = cafes.sumOf { pesoKg(it) ?: 0.0 }

// Suma de peso x precio de cada tarjeta.
fun totalEstimado(cafes: List<CafeFormulario>): Double =
    cafes.sumOf { (pesoKg(it) ?: 0.0) * (precioKg(it) ?: 0.0) }

// ---------------- Fecha ----------------

// Fecha de hoy con Calendar (existe en todas las versiones de Android).
// Calendar cuenta los meses desde 0 (enero = 0), por eso se suma 1
// Calendar.getInstance() trae la fecha actual del celular.
fun fechaDeHoy(): Fecha {
    val calendario = Calendar.getInstance()
    return Fecha(
        dia = calendario.get(Calendar.DAY_OF_MONTH),
        mes = calendario.get(Calendar.MONTH) + 1,
        anio = calendario.get(Calendar.YEAR)
    )
}
