package com.cafesmart.app.purchase

import com.cafesmart.app.data.inventario.OrigenOcupacion
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.PurchaseRepository
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.data.purchase.productoresDeEjemplo
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel

// Versiones falsas para probar PurchaseViewModel con JUnit, sin Room (como Negocios en NegocioInfoViewModelTest).

/** Kilos en bodega que se pueden cambiar desde la prueba. */
class OcupacionFalsa(var kg: Double = 0.0) : OrigenOcupacion {
    // true = leer los kilos falla (por ejemplo, la base no responde).
    var fallar = false

    override suspend fun kgOcupados(): Double {
        if (fallar) throw IllegalStateException("fallo simulado al leer los kilos")
        return kg
    }
}

/** Repositorio en memoria: guarda productores con id y anota las compras que se registran. */
class RepositorioDeComprasFalso(productores: List<Productor> = emptyList()) : PurchaseRepository {

    data class CompraGuardada(val productorId: Long?, val fecha: Fecha, val cafes: List<CafeValidado>)

    val productores = productores.mapIndexed { i, p -> p.copy(id = i + 1L) }.toMutableList()
    val compras = mutableListOf<CompraGuardada>()

    // true = registrarCompra falla (por ejemplo, la base de datos no responde).
    var fallarAlRegistrar = false

    // true = leer, guardar o modificar productores falla.
    var fallarAlListar = false
    var fallarAlGuardarProductor = false

    // Si se da, cada compra guardada suma sus kilos a la bodega, como pasaria con los sublotes en Room.
    var bodega: OcupacionFalsa? = null

    override suspend fun listarProductores(): List<Productor> {
        if (fallarAlListar) throw IllegalStateException("fallo simulado al listar")
        return productores.toList()
    }

    override suspend fun guardarProductor(productor: Productor): ResultadoProductor {
        if (fallarAlGuardarProductor) throw IllegalStateException("fallo simulado al guardar el productor")
        if (productores.any { it.documento == productor.documento }) return ResultadoProductor.DocumentoRepetido
        val guardado = productor.copy(id = (productores.maxOfOrNull { it.id } ?: 0L) + 1)
        productores += guardado
        return ResultadoProductor.Guardado(guardado)
    }

    override suspend fun actualizarProductor(productor: Productor): ResultadoProductor {
        if (fallarAlGuardarProductor) throw IllegalStateException("fallo simulado al modificar el productor")
        if (productores.any { it.documento == productor.documento && it.id != productor.id }) {
            return ResultadoProductor.DocumentoRepetido
        }
        val i = productores.indexOfFirst { it.id == productor.id }
        if (i >= 0) productores[i] = productor
        return ResultadoProductor.Guardado(productor)
    }

    override suspend fun registrarCompra(productorId: Long?, fecha: Fecha, cafes: List<CafeValidado>): Long {
        if (fallarAlRegistrar) throw IllegalStateException("fallo simulado")
        compras += CompraGuardada(productorId, fecha, cafes)
        bodega?.let { it.kg += cafes.sumOf { c -> c.pesoKg } }
        return compras.size.toLong()
    }
}

/** ViewModel con los 3 productores de ejemplo ya cargados, como los dejaria Room. */
fun viewModelDePrueba(
    repositorio: RepositorioDeComprasFalso = RepositorioDeComprasFalso(productoresDeEjemplo),
    ocupacion: OcupacionFalsa = OcupacionFalsa(),
): PurchaseViewModel = PurchaseViewModel(repositorio, ocupacion).also { it.cargarProductores() }
