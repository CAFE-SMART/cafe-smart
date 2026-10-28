package com.cafesmart.app.ui.compra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.ModoProductor
import com.cafesmart.app.data.NuevoProductor
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.Sublote
import com.cafesmart.app.data.productorSinRegistrar
import com.cafesmart.app.ui.theme.CafesmartTheme

// El estado del registro vive aqui (state hoisting): las pantallas solo lo muestran
// y avisan cada cambio por lambdas, igual que MainScreen(onThemeToggle) del ejemplo de clase.
@Composable
fun RegistroCompraFlow(
    onSalir: () -> Unit,
    // Entrega productor, cafes y fecha al paso 3 (Finalizar registro), que se hace en otra rama
    onFinalizar: (Productor, List<Sublote>, Fecha) -> Unit,
    modifier: Modifier = Modifier
) {
    var paso by remember { mutableStateOf(1) }

    // Paso 1: productor
    var modo by remember { mutableStateOf<ModoProductor?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var verTodos by remember { mutableStateOf(false) }
    // Lista en memoria hasta que se incorpore Room
    var productores by remember { mutableStateOf(listOf<Productor>()) }
    var productorElegido by remember { mutableStateOf<Productor?>(null) }
    var nuevoProductor by remember { mutableStateOf(NuevoProductor()) }
    // El productor con el que se hace la compra (sale del paso 1)
    var productorDeLaCompra by remember { mutableStateOf<Productor?>(null) }

    // Paso 2: cafes (una tarjeta "Café N" por cada uno) y fecha
    var cafes by remember { mutableStateOf(listOf(CafeFormulario())) }
    var fecha by remember { mutableStateOf(fechaDeHoy()) }

    if (paso == 1) {
        RegistroCompraPaso1Screen(
            modo = modo,
            busqueda = busqueda,
            productores = productoresVisibles(productores, busqueda, verTodos),
            hayMasProductores = hayMasProductores(productores, busqueda, verTodos),
            productorElegido = productorElegido,
            nuevoProductor = nuevoProductor,
            // El error solo se muestra cuando el campo ya tiene algo escrito
            documentoError = if (nuevoProductor.documento.isEmpty()) null else validarDocumento(nuevoProductor.documento, nuevoProductor.tipo),
            nombreError = if (nuevoProductor.nombre.isEmpty()) null else validarNombre(nuevoProductor.nombre, nuevoProductor.tipo),
            telefonoError = validarTelefono(nuevoProductor.telefono),
            continuarHabilitado = puedeContinuarPaso1(modo, productorElegido, nuevoProductor),
            onModoChange = { modo = it },
            onBusquedaChange = { busqueda = it },
            onProductorClick = { productorElegido = it },
            onVerMas = { verTodos = true },
            onNuevoProductorChange = { datos ->
                nuevoProductor = datos.copy(
                    documento = limpiarDocumento(datos.documento, datos.tipo),
                    nombre = limpiarNombre(datos.nombre, datos.tipo),
                    telefono = limpiarTelefono(datos.telefono)
                )
            },
            onContinuar = {
                if (modo == ModoProductor.REGISTRADO) {
                    productorDeLaCompra = productorElegido
                } else if (modo == ModoProductor.SIN_REGISTRAR) {
                    productorDeLaCompra = productorSinRegistrar
                } else if (modo == ModoProductor.NUEVO) {
                    // Se guarda en la lista y queda elegido por si vuelve al paso 1
                    val nuevo = crearProductor(nuevoProductor)
                    productores = productores + nuevo
                    productorElegido = nuevo
                    productorDeLaCompra = nuevo
                    nuevoProductor = NuevoProductor()
                    modo = ModoProductor.REGISTRADO
                }
                paso = 2
            },
            onBack = onSalir,
            modifier = modifier
        )
    } else {
        RegistroCompraPaso2Screen(
            cafes = cafes,
            erroresPeso = cafes.map { errorPeso(it) },
            erroresPrecio = cafes.map { errorPrecio(it) },
            fecha = fecha,
            totalKg = totalKg(cafes),
            totalEstimado = totalEstimado(cafes),
            agregarHabilitado = cafesCompletos(cafes),
            continuarHabilitado = cafesCompletos(cafes),
            onCafeChange = { indice, cafe ->
                val limpio = cafe.copy(peso = limpiarPeso(cafe.peso), precio = precioEnPantalla(cafe.precio))
                // Reemplaza solo la tarjeta que cambio
                cafes = cafes.mapIndexed { i, actual -> if (i == indice) limpio else actual }
            },
            onQuitarCafe = { indice -> cafes = cafes.filterIndexed { i, _ -> i != indice } },
            onAgregarCafe = { cafes = cafes + CafeFormulario() },
            onFechaChange = { fecha = it },
            onContinuar = {
                val productor = productorDeLaCompra
                if (productor != null) {
                    onFinalizar(productor, cafes.mapNotNull { crearSublote(it) }, fecha)
                }
            },
            onBack = { paso = 1 },
            modifier = modifier
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RegistroCompraFlowPreview() {
    CafesmartTheme {
        RegistroCompraFlow(onSalir = {}, onFinalizar = { _, _, _ -> })
    }
}
