package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.viewmodel.drying.BOTON_CONFIRMAR_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.BOTON_DESCARTAR_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.CUERPO_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.MENSAJE_SIN_SECADOS_EN_CURSO
import com.cafesmart.app.viewmodel.drying.SecadosEnCursoViewModel
import com.cafesmart.app.viewmodel.drying.TITULO_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.textoLlevaDias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadosEnCursoViewModelTest {

    private val repositorio = repositorioDePrueba()
    private fun vm() = SecadosEnCursoViewModel(repositorio, HOY_FIGMA)

    private fun disponible(id: Long) = repositorio.inventario().first { it.sublote.id == id }.sublote.pesoDisponibleKg

    @Test
    fun sinSecados_diceQueNoHay() {
        val e = vm().uiState.value
        assertTrue(e.secados.isEmpty())
        assertEquals(MENSAJE_SIN_SECADOS_EN_CURSO, e.mensajeSinSecados)
        assertNull(e.elegido)
        assertNull(e.tarjetaElegida)
    }

    @Test
    fun conUnSoloSecado_vieneElegido() {
        val id = repositorio.iniciarSecado(Fecha(1, 6, 2026), listOf(EnvioASecar(1, 50.0))).id
        val e = vm().uiState.value
        assertEquals(id, e.elegido?.secado?.id)
        assertNull(e.mensajeSinSecados)
    }

    @Test
    fun conVarios_vieneElegidoElPrimeroQueEsElMasReciente() {
        repositorio.iniciarSecado(Fecha(1, 6, 2026), listOf(EnvioASecar(1, 10.0)))
        val reciente = repositorio.iniciarSecado(Fecha(2, 6, 2026), listOf(EnvioASecar(2, 10.0))).id
        assertEquals(reciente, vm().uiState.value.elegido?.secado?.id)
    }

    @Test
    fun laTarjetaTraeLosTextos() {
        repositorio.iniciarSecado(
            Fecha(1, 6, 2026),
            listOf(EnvioASecar(1, 120.0), EnvioASecar(2, 80.0), EnvioASecar(3, 60.0), EnvioASecar(4, 30.0))
        )
        val t = vm().uiState.value.tarjetaElegida!!
        assertEquals("Secado del 1 de junio", t.titulo)
        assertEquals("4 sublotes", t.sublotesTexto)
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), t.calidades)
        assertEquals("290 kg", t.kgEnviadosTexto)
        assertEquals("1 de junio de 2026", t.fechaInicioTexto)
        assertEquals("Lleva 2 días", t.diasTexto)
    }

    @Test
    fun laTarjetaSoloListaLasCalidadesEnviadas() {
        repositorio.iniciarSecado(HOY_FIGMA, listOf(EnvioASecar(1, 10.0)))
        val t = vm().uiState.value.tarjetaElegida!!
        assertEquals(listOf(Calidad.REGULAR), t.calidades)
        assertEquals("1 sublote", t.sublotesTexto)
        assertEquals("Empezó hoy", t.diasTexto)
    }

    @Test
    fun textoLlevaDias_singularYPlural() {
        assertEquals("Empezó hoy", textoLlevaDias(0))
        assertEquals("Lleva 1 día", textoLlevaDias(1))
        assertEquals("Lleva 5 días", textoLlevaDias(5))
    }

    @Test
    fun elegirSecado_cambiaElElegidoYIgnoraIdsDesconocidos() {
        val viejo = repositorio.iniciarSecado(Fecha(1, 6, 2026), listOf(EnvioASecar(1, 10.0))).id
        repositorio.iniciarSecado(Fecha(2, 6, 2026), listOf(EnvioASecar(2, 10.0)))
        val vm = vm()
        vm.elegirSecado(viejo)
        assertEquals(viejo, vm.uiState.value.elegido?.secado?.id)
        vm.elegirSecado(999)
        assertEquals(viejo, vm.uiState.value.elegido?.secado?.id)
    }

    @Test
    fun pedirYDescartarCancelar_abreYCierraElDialogoSinTocarNada() {
        repositorio.iniciarSecado(HOY_FIGMA, listOf(EnvioASecar(1, 50.0)))
        val vm = vm()
        vm.pedirCancelar()
        assertTrue(vm.uiState.value.confirmandoCancelacion)
        vm.descartarCancelar()
        assertFalse(vm.uiState.value.confirmandoCancelacion)
        assertEquals(1, vm.uiState.value.secados.size)
        assertEquals(70.0, disponible(1), 0.0)
    }

    @Test
    fun pedirCancelar_sinSecadosNoHaceNada() {
        val vm = vm()
        vm.pedirCancelar()
        assertFalse(vm.uiState.value.confirmandoCancelacion)
    }

    @Test
    fun confirmarCancelar_devuelveElCafeYRefrescaLaLista() {
        repositorio.iniciarSecado(HOY_FIGMA, listOf(EnvioASecar(1, 50.0), EnvioASecar(4, 30.0)))
        val vm = vm()
        vm.pedirCancelar()
        vm.confirmarCancelar()
        assertEquals(120.0, disponible(1), 0.0)
        assertEquals(30.0, disponible(4), 0.0)
        val e = vm.uiState.value
        assertTrue(e.secados.isEmpty())
        assertFalse(e.confirmandoCancelacion)
        assertEquals(MENSAJE_SIN_SECADOS_EN_CURSO, e.mensajeSinSecados)
    }

    @Test
    fun confirmarCancelar_conVariosCancelaSoloElElegido() {
        val viejo = repositorio.iniciarSecado(Fecha(1, 6, 2026), listOf(EnvioASecar(1, 10.0))).id
        val nuevo = repositorio.iniciarSecado(Fecha(2, 6, 2026), listOf(EnvioASecar(2, 10.0))).id
        val vm = vm()
        vm.elegirSecado(viejo)
        vm.pedirCancelar()
        vm.confirmarCancelar()
        assertEquals(listOf(nuevo), vm.uiState.value.secados.map { it.secado.id })
        assertEquals(nuevo, vm.uiState.value.elegido?.secado?.id)
        assertEquals(120.0, disponible(1), 0.0)
        assertEquals(70.0, disponible(2), 0.0)
    }

    @Test
    fun confirmarCancelar_sinPedirloAntesNoCancela() {
        repositorio.iniciarSecado(HOY_FIGMA, listOf(EnvioASecar(1, 50.0)))
        val vm = vm()
        vm.confirmarCancelar()
        assertEquals(70.0, disponible(1), 0.0)
        assertEquals(1, vm.uiState.value.secados.size)
    }

    @Test
    fun losTextosDelDialogoSonLosPropuestos() {
        assertEquals("¿Cancelar este secado?", TITULO_CANCELAR_SECADO)
        assertEquals("El café vuelve a tu inventario como estaba antes de enviarlo.", CUERPO_CANCELAR_SECADO)
        assertEquals("Sí, cancelar", BOTON_CONFIRMAR_CANCELAR_SECADO)
        assertEquals("No", BOTON_DESCARTAR_CANCELAR_SECADO)
    }
}
