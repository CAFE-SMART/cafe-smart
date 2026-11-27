package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.purchase.BaseFalsa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Inventario cuando Room falla o entrega un dato que no se puede leer: la pantalla debe quedar en un estado de
// error con "Reintentar", nunca cerrar la app ni mostrar un inventario a medias.
class InventoryViewModelErroresTest {

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val loteVerde = LoteConSublotes(
        Lote(id = 1, tipoCafe = TipoCafe.VERDE, calidad = Calidad.BUENA),
        listOf(
            Sublote(
                id = 1, loteId = 1, compraId = 1, pesoInicialKg = 50.0, pesoDisponibleKg = 50.0,
                costoKg = 15_000, fechaIngreso = Fecha(15, 11, 2026),
            )
        ),
    )

    // Repositorio que falla las primeras [fallos] veces que se abre la lectura y después entrega [lotes].
    private class InventarioQueFalla(
        private var fallos: Int,
        private val lotes: List<LoteConSublotes> = emptyList(),
    ) : InventoryRepository {
        var aperturas = 0
        override fun observarLotes(): Flow<List<LoteConSublotes>> = flow {
            aperturas++
            if (fallos > 0) {
                fallos--
                throw IllegalStateException("Room no pudo abrir la base")
            }
            emit(lotes)
        }
    }

    private class NegociosQueFallan : NegocioRepository {
        override suspend fun guardar(negocio: NegocioEntity) = Unit
        override fun observar(uid: String): Flow<NegocioEntity?> = flow { throw IllegalStateException("negocio") }
    }

    @Test
    fun siRoomFallaAlEmpezarMuestraElErrorYNoSeQuedaCargando() {
        val vm = InventoryViewModel(InventarioQueFalla(fallos = 1))

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun siRoomFallaDespuesDeHaberMostradoCafeTambienMuestraElError() {
        val vm = InventoryViewModel(
            object : InventoryRepository {
                override fun observarLotes(): Flow<List<LoteConSublotes>> = flow {
                    emit(listOf(loteVerde))
                    throw IllegalStateException("Room falló al volver a leer")
                }
            }
        )

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun unTipoDeCafeDesconocidoEnLaBaseMuestraElErrorEnVezDeCerrarLaApp() {
        val base = BaseFalsa()
        base.lotes.filas = listOf(LoteEntity(id = 1, tipoCafe = "AZUL", calidad = "BUENA"))

        val vm = InventoryViewModel(RoomInventoryRepository(base.lotes, base.sublotes))

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
    }

    @Test
    fun unSubloteQueNoPasaLasValidacionesDelModeloMuestraElError() {
        val base = BaseFalsa()
        base.lotes.filas = listOf(LoteEntity(id = 1, tipoCafe = "VERDE", calidad = "BUENA"))
        // Más café disponible que el que entró: Sublote lo rechaza al crearse.
        base.sublotes.filas = listOf(
            SubloteEntity(
                id = 1, loteId = 1, pesoInicialKg = 10.0, pesoDisponibleKg = 20.0, costoKg = 1_000, fechaIngreso = 0L,
            )
        )

        val vm = InventoryViewModel(RoomInventoryRepository(base.lotes, base.sublotes))

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
    }

    @Test
    fun siFallaLaLecturaDelNegocioTambienMuestraElError() {
        val vm = InventoryViewModel(
            InventarioQueFalla(fallos = 0, lotes = listOf(loteVerde)),
            negocios = NegociosQueFallan(),
            uid = "uid-1",
        )

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
    }

    @Test
    fun reintentarVuelveALeerYSiFuncionaQuitaElError() {
        val repositorio = InventarioQueFalla(fallos = 1, lotes = listOf(loteVerde))
        val vm = InventoryViewModel(repositorio)
        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)

        vm.reintentar()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
        assertEquals(50.0, vm.uiState.value.resumen.totalKg, 1e-9)
        assertEquals(2, repositorio.aperturas)
    }

    @Test
    fun reintentarQueVuelveAFallarSigueMostrandoElError() {
        val repositorio = InventarioQueFalla(fallos = 2)
        val vm = InventoryViewModel(repositorio)

        vm.reintentar()

        assertEquals(ERROR_INVENTARIO, vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
        assertEquals(2, repositorio.aperturas)
    }

    @Test
    fun reintentarSinErrorNoAbreOtraLectura() {
        val repositorio = InventarioQueFalla(fallos = 0, lotes = listOf(loteVerde))
        val vm = InventoryViewModel(repositorio)

        vm.reintentar()

        assertNull(vm.uiState.value.error)
        assertEquals(1, repositorio.aperturas)
    }

    @Test
    fun despuesDeReintentarSigueRecibiendoLosCambiosDeRoom() {
        // Primera apertura falla; la segunda queda escuchando un flujo vivo, como Room.
        val vivos = MutableStateFlow(emptyList<LoteConSublotes>())
        var aperturas = 0
        val vm = InventoryViewModel(
            object : InventoryRepository {
                override fun observarLotes(): Flow<List<LoteConSublotes>> = flow {
                    if (aperturas++ == 0) throw IllegalStateException("primera lectura")
                    vivos.collect { emit(it) }
                }
            }
        )
        vm.reintentar()
        assertEquals(0.0, vm.uiState.value.resumen.totalKg, 1e-9)

        vivos.value = listOf(loteVerde)

        assertEquals(50.0, vm.uiState.value.resumen.totalKg, 1e-9)
        assertNull(vm.uiState.value.error)
    }
}
