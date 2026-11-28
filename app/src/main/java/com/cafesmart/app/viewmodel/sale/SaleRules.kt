package com.cafesmart.app.viewmodel.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.NuevoCliente
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.purchase.CANTIDAD_RECIENTES
import com.cafesmart.app.viewmodel.purchase.MENSAJE_PRECIO
import com.cafesmart.app.viewmodel.limites.LimitesVigentes
import com.cafesmart.app.viewmodel.purchase.coincideBusqueda
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFecha
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.normalizarEspacios
import com.cafesmart.app.viewmodel.purchase.precioValido
import com.cafesmart.app.viewmodel.purchase.productorValido
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono

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
fun clientesVisibles(clientes: List<Cliente>, busqueda: String): List<Cliente> =
    if (busqueda.isNotBlank()) clientesDeLaLista(clientes, busqueda) else clientes.reversed().take(CANTIDAD_RECIENTES)

// true cuando hay que mostrar el boton "Ver más clientes".
fun hayMasClientes(clientes: List<Cliente>, busqueda: String): Boolean =
    busqueda.isBlank() && clientes.size > CANTIDAD_RECIENTES

// "Cédula 16780012" / "NIT 9003421215": tipo y número del documento de un cliente.
fun documentoTexto(cliente: Cliente): String = "${cliente.tipoDocumento.etiqueta} ${cliente.documento}"

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

// ---------------- Paso 2: elegir el cafe ----------------

// Orden de las pestanas del Figma (el enum TipoCafe tiene otro orden).
val tiposParaVender = listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA)

// Sublotes que se pueden vender de un tipo de cafe: los que todavia tienen cafe, agrupados por calidad
// (buena, regular, mala) y dentro de cada calidad por numero de sublote.
fun sublotesDelTipo(inventario: List<SubloteConLote>, tipo: TipoCafe): List<SubloteConLote> =
    inventario
        .filter { it.tipoCafe == tipo && !it.sublote.estaAgotado }
        .sortedWith(compareBy({ it.calidad }, { it.sublote.id }))

// "1 de 4 seleccionados"
fun textoSeleccionados(seleccionados: Int, total: Int): String = "$seleccionados de $total seleccionados"

const val MILIS_POR_DIA = 86_400_000L

// Dias entre la fecha en que entro el cafe y hoy.
fun diasDesde(fecha: Fecha, hoy: Fecha): Int = ((fechaAMilis(hoy) - fechaAMilis(fecha)) / MILIS_POR_DIA).toInt()

// "Comprado hoy" / "Comprado hace 1 día" / "Comprado hace 6 días"
fun textoComprado(dias: Int): String = when {
    dias <= 0 -> "Comprado hoy"
    dias == 1 -> "Comprado hace 1 día"
    else -> "Comprado hace $dias días"
}

// "1 sublote · 200 kg" / "2 sublotes · 280 kg"
fun resumenGrupo(cantidad: Int, kg: Double): String =
    "$cantidad ${if (cantidad == 1) "sublote" else "sublotes"} · ${formatoKg(kg)}"

// Costo promedio por kg de lo que se vende (promedio ponderado: pesa mas el sublote del que se vende mas).
// Es solo una referencia para poner el precio. Sin kg devuelve 0.
fun costoPromedio(elegidos: List<SubloteElegido>): Long {
    val kg = elegidos.sumOf { it.cantidadKg }
    if (kg <= 0.0) return 0
    return (elegidos.sumOf { it.cantidadKg * it.sublote.sublote.costoKg } / kg).aPesos()
}

// Ayuda del campo de precio: "Entre $ 1.000 y $ 100.000. Costo promedio: $ 18.000 por kg."
fun ayudaPrecio(costoPromedioKg: Long): String =
    "Entre ${formatoDinero(LimitesVigentes.actuales.precioKgMinimoCop.toDouble())} y " +
        "${formatoDinero(LimitesVigentes.actuales.precioKgMaximoCop.toDouble())}. " +
        "Costo promedio: ${formatoDinero(costoPromedioKg.toDouble())} por kg."

// Error del campo precio de una calidad elegida (null si ya es valido): vacio pide escribirlo y fuera de rango da el rango.
fun mensajePrecioPendiente(calidad: Calidad, texto: String): String? =
    when {
        precioValido(texto) -> null
        texto.isEmpty() -> "Escribe el precio por kg de la calidad ${calidad.etiqueta.lowercase()}."
        else -> MENSAJE_PRECIO
    }

// ---------------- Paso 3: confirmar ----------------

// Fechas permitidas para una venta: desde el 1 de enero de 2026 hasta hoy (misma regla de la version web).
val FECHA_MINIMA_VENTA = Fecha(1, 1, 2026)
const val MENSAJE_FECHA_VENTA = "Solo puedes registrar ventas desde 2026 hasta hoy."

// Ademas, la venta no puede ser de antes de que entrara el cafe que se vende.
// Devuelve el mensaje de error, o null si la fecha sirve.
fun errorFecha(fecha: Fecha, hoy: Fecha, elegidos: List<SubloteElegido>): String? {
    if (fechaAMilis(fecha) > fechaAMilis(hoy) || fechaAMilis(fecha) < fechaAMilis(FECHA_MINIMA_VENTA)) return MENSAJE_FECHA_VENTA
    val masReciente = elegidos.map { it.fechaIngreso }.maxByOrNull { fechaAMilis(it) } ?: return null
    return if (fechaAMilis(fecha) < fechaAMilis(masReciente)) {
        "La fecha no puede ser anterior a la entrada del café (${formatoFecha(masReciente)})."
    } else {
        null
    }
}

// Una venta no puede ser de menos de 5 kg en total (misma regla de la version web: PESO_MINIMO_KG).
// Sale de los límites unificados (viewmodel/limites); este nombre queda como alias.
const val VENTA_MINIMA_KG = com.cafesmart.app.viewmodel.limites.VENTA_MINIMA_KG

// El error aparece cuando ya hay cafe elegido pero la suma de lo que se vende no llega al minimo.
fun errorTotalKg(totalKg: Double, hayElegidos: Boolean): String? =
    if (hayElegidos && totalKg < VENTA_MINIMA_KG) "La cantidad total a vender debe ser mínimo ${formatoKg(VENTA_MINIMA_KG)}." else null

// Al confirmar la venta se descuenta de cada sublote lo que se vendio.
// redondearKg evita restos como 0.0000001 al restar decimales; el sublote nunca queda en negativo.
fun descontarVenta(inventario: List<SubloteConLote>, elegidos: List<SubloteElegido>): List<SubloteConLote> =
    inventario.map { item ->
        val vendido = elegidos.firstOrNull { it.id == item.sublote.id }?.cantidadKg ?: return@map item
        item.copy(sublote = item.sublote.copy(pesoDisponibleKg = redondearKg(maxOf(0.0, item.sublote.pesoDisponibleKg - vendido))))
    }
