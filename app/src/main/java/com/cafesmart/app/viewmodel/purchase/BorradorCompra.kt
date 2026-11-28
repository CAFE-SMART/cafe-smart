package com.cafesmart.app.viewmodel.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.productorSinRegistrar

// Recuperar una compra interrumpida (ciclo 3, Amelia, orden 21): lo que hace falta para volver al mismo paso con lo
// que ya se escribió. Funciones puras, sin Android, para probarlas con JUnit. No guarda la compra: eso solo lo hace
// "Confirmar compra" (PurchaseRepository.registrarCompra).

/** Lo que se recuerda de una compra a medio hacer. El productor registrado se recuerda por su id de Room. */
data class BorradorCompra(
    val paso: Int,
    val modo: ModoProductor?,
    val productorId: Long?,
    val nuevoProductor: NuevoProductor,
    val cafes: List<CafeFormulario>,
    val cafeAbierto: Int,
    val fecha: Fecha,
) {
    /** Este borrador pide un productor registrado que todavía hay que buscar en la lista de Room. */
    val esperaProductor: Boolean get() = modo == ModoProductor.REGISTRADO && productorId != null

    companion object {
        /**
         * El borrador del estado actual, o null si no hay nada que recordar: formulario sin tocar o compra ya
         * registrada. Con null se borra el borrador guardado (así "Confirmar" y "Sí, cancelar" lo eliminan,
         * porque los dos terminan en un estado vacío o registrado).
         */
        fun de(estado: PurchaseUiState): BorradorCompra? {
            if (estado.compraRegistrada) return null
            val sinTocar = estado.paso == 1 && estado.modo == null && estado.productorElegido == null &&
                estado.nuevoProductor == NuevoProductor() && estado.cafes.all { it == CafeFormulario() }
            if (sinTocar) return null
            return BorradorCompra(
                paso = estado.paso.coerceIn(1, 3),
                modo = estado.modo,
                productorId = estado.productorElegido?.id?.takeIf { it != 0L },
                nuevoProductor = estado.nuevoProductor,
                cafes = estado.cafes.ifEmpty { listOf(CafeFormulario()) },
                cafeAbierto = estado.cafeAbierto,
                fecha = estado.fecha,
            )
        }
    }

    /**
     * Pone el borrador sobre [base]. Con un productor registrado, el paso 2 o 3 solo se recupera si ese productor
     * existe en [productores]: si no (la lista aún no llegó, o lo borraron), se queda en el paso 1 con el modo
     * elegido, para que nunca se confirme una compra sin su productor.
     */
    fun aplicar(base: PurchaseUiState, productores: List<Productor> = base.productores): PurchaseUiState {
        val elegido = productorId?.let { id -> productores.firstOrNull { it.id == id } }
        val delaCompra = when (modo) {
            ModoProductor.REGISTRADO -> elegido
            ModoProductor.SIN_REGISTRAR -> productorSinRegistrar
            null -> null
        }
        val pasoSeguro = if (paso > 1 && delaCompra == null) 1 else paso
        val cafesSeguros = cafes.ifEmpty { listOf(CafeFormulario()) }
        return base.copy(
            paso = pasoSeguro,
            modo = modo,
            productorElegido = elegido,
            productorDeLaCompra = if (pasoSeguro > 1) delaCompra else base.productorDeLaCompra,
            nuevoProductor = nuevoProductor,
            cafes = cafesSeguros,
            cafeAbierto = cafeAbierto.coerceIn(0, cafesSeguros.lastIndex),
            fecha = fecha,
        )
    }

    // ---------------- Texto para guardarlo (versión 1) ----------------

    fun codificar(): String {
        fun c(cafe: CafeFormulario) = listOf(cafe.tipoCafe.orEmpty(), cafe.calidad?.name.orEmpty(), cafe.peso, cafe.precio)
            .joinToString(",") { escapar(it) }
        return listOf(
            "v1", paso.toString(), modo?.name.orEmpty(), productorId?.toString().orEmpty(),
            escapar(nuevoProductor.tipo.name), escapar(nuevoProductor.documento), escapar(nuevoProductor.nombre),
            escapar(nuevoProductor.telefono), cafes.joinToString(";") { c(it) }, cafeAbierto.toString(),
            "${fecha.dia}-${fecha.mes}-${fecha.anio}",
        ).joinToString("|")
    }
}

/** Lee un borrador guardado. Si el texto está dañado o es de otra versión devuelve null (no tumba la app). */
fun decodificarBorradorCompra(texto: String?): BorradorCompra? = runCatching {
    val p = texto!!.split("|")
    require(p.size == 11 && p[0] == "v1")
    val cafes = if (p[8].isEmpty()) emptyList() else p[8].split(";").map { t ->
        val c = t.split(",").map { desescapar(it) }
        require(c.size == 4)
        CafeFormulario(c[0].ifEmpty { null }, c[1].ifEmpty { null }?.let { Calidad.valueOf(it) }, c[2], c[3])
    }
    val (dia, mes, anio) = p[10].split("-").map { it.toInt() }
    BorradorCompra(
        paso = p[1].toInt(),
        modo = p[2].ifEmpty { null }?.let { ModoProductor.valueOf(it) },
        productorId = p[3].ifEmpty { null }?.toLong(),
        nuevoProductor = NuevoProductor(
            TipoDocumento.valueOf(desescapar(p[4])), desescapar(p[5]), desescapar(p[6]), desescapar(p[7]),
        ),
        cafes = cafes,
        cafeAbierto = p[9].toInt(),
        fecha = Fecha(dia, mes, anio),
    )
}.getOrNull()

// Los separadores del formato (| ; ,) y el % se escriben como %7C %3B %2C %25 dentro de cada campo.
private fun escapar(t: String) = t.replace("%", "%25").replace("|", "%7C").replace(";", "%3B").replace(",", "%2C")
private fun desescapar(t: String) = t.replace("%7C", "|").replace("%3B", ";").replace("%2C", ",").replace("%25", "%")
