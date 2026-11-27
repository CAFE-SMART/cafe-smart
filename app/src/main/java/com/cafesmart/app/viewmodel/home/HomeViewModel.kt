package com.cafesmart.app.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.finanzas.diasEntre
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.viewmodel.inventory.KG_POR_BULTO
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.ResumenInventario
import com.cafesmart.app.viewmodel.inventory.clave
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.inventory.resumirInventario
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.purchase.milisAFecha
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Mismo estilo que ERROR_INVENTARIO. Texto pendiente de confirmar con el equipo.
const val ERROR_INICIO = "No se pudo cargar el resumen. Inténtalo de nuevo."

/** Cuántos sublotes muestra "Sublotes más antiguos" (Figma: tres filas). */
const val SUBLOTES_EN_INICIO = 3

/** "Resumen del día": solo lo que se guardó con fecha de [hoy]. Las ventas aún no se guardan en Room. */
data class ComprasDelDia(
    val compras: Int,
    val kgComprados: Double,
    val productores: Int,
)

/** Una fila de "Sublotes más antiguos": un sublote real (con café) de la cuenta. */
data class SubloteInicio(
    val id: Long,
    val tipo: TipoCafe,
    val calidad: Calidad,
    val diasEnBodega: Int,
    val pesoKg: Double,
)

/**
 * Estado de Inicio. [inventario] sale de la MISMA función que usa Inventario (resumirInventario) con la misma
 * capacidad y peso del bulto del Negocio de la cuenta: el total, los bultos y el porcentaje de ocupación de las dos
 * pantallas no pueden diferir.
 */
data class HomeUiState(
    val inventario: ResumenInventario,
    val delDia: ComprasDelDia = ComprasDelDia(0, 0.0, 0),
    val sublotesAntiguos: List<SubloteInicio> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
) {
    /** Sublotes con café de toda la bodega. */
    val cantidadSublotes: Int get() = inventario.tipos.sumOf { it.cantidadSublotes }
}

/** Compras de [hoy]: cuántas, cuántos kg entraron (peso inicial de sus sublotes) y cuántos productores distintos. */
fun resumirDia(lotes: List<LoteConSublotes>, compras: List<CompraEntity>, hoy: Fecha): ComprasDelDia {
    val deHoy = compras.filter { milisAFecha(it.fecha) == hoy }
    val ids = deHoy.map { it.id }.toSet()
    val kg = lotes.flatMap { it.sublotes }.filter { it.compraId in ids }.sumOf { it.pesoInicialKg }
    return ComprasDelDia(
        compras = deHoy.size,
        kgComprados = redondearKg(kg),
        // "Sin registrar" (productorId null) no cuenta como productor.
        productores = deHoy.mapNotNull { it.productorId }.toSet().size,
    )
}

/** Los [cuantos] sublotes con café que llevan más tiempo en bodega (a igual fecha, el de menor id). */
fun sublotesMasAntiguos(lotes: List<LoteConSublotes>, hoy: Fecha, cuantos: Int = SUBLOTES_EN_INICIO): List<SubloteInicio> =
    lotes.flatMap { grupo -> grupo.sublotesConExistencia.map { grupo.lote to it } }
        .sortedWith(compareBy({ it.second.fechaIngreso.clave() }, { it.second.id }))
        .take(cuantos)
        .map { (lote, s) ->
            SubloteInicio(s.id, lote.tipoCafe, lote.calidad, maxOf(0, diasEntre(s.fechaIngreso, hoy)), s.pesoDisponibleKg)
        }

/**
 * Inicio con los datos de la cuenta activa: inventario y compras de su base Room y capacidad y peso del bulto de su
 * Negocio. Todo se actualiza solo (una compra nueva o un cambio en Ajustes). Sin Negocio, la capacidad queda en 0
 * ("sin configurar"), igual que en Inventario: nunca se usa una capacidad inventada.
 */
class HomeViewModel(
    private val inventario: InventoryRepository,
    private val compras: Flow<List<CompraEntity>>,
    private val negocios: NegocioRepository?,
    private val uid: String?,
    private val hoy: () -> Fecha = ::fechaDeHoy,
) : ViewModel() {

    private var lotes: List<LoteConSublotes> = emptyList()
    private var listaCompras: List<CompraEntity> = emptyList()
    private var capacidad = 0.0
    private var pesoBulto = KG_POR_BULTO
    private var lecturas: List<Job> = emptyList()

    private val _uiState = MutableStateFlow(HomeUiState(inventario = resumen()))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observar()
    }

    /** "Reintentar" del estado de error: vuelve a leer Room desde cero. */
    fun reintentar() {
        if (_uiState.value.error == null) return
        _uiState.update { it.copy(error = null, cargando = true) }
        observar()
    }

    private fun resumen() = resumirInventario(lotes, capacidad, OrdenInventario.MAS_RECIENTE, pesoBulto)

    private fun observar() {
        lecturas.forEach { it.cancel() }
        val l = viewModelScope.launch {
            protegido { inventario.observarLotes().collect { lotes = it; recalcular(cargando = false) } }
        }
        val c = viewModelScope.launch {
            protegido { compras.collect { listaCompras = it; recalcular() } }
        }
        val n = if (negocios != null && uid != null) viewModelScope.launch {
            protegido {
                negocios.observar(uid).collect { negocio ->
                    capacidad = negocio?.capacidadBodegaKg ?: 0.0
                    pesoBulto = negocio?.pesoBultoKg ?: KG_POR_BULTO
                    recalcular()
                }
            }
        } else null
        lecturas = listOfNotNull(l, c, n)
    }

    private suspend fun protegido(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lecturas.forEach { it.cancel() }
            _uiState.update { it.copy(cargando = false, error = ERROR_INICIO) }
        }
    }

    private fun recalcular(cargando: Boolean? = null) = _uiState.update {
        val fecha = hoy()
        it.copy(
            inventario = resumen(),
            delDia = resumirDia(lotes, listaCompras, fecha),
            sublotesAntiguos = sublotesMasAntiguos(lotes, fecha),
            cargando = cargando ?: it.cargando,
        )
    }
}
