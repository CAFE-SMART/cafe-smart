package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MAX
import com.cafesmart.app.data.model.FACTOR_RENDIMIENTO_MIN
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.viewmodel.drying.FinalizarSecadoViewModel
import com.cafesmart.app.viewmodel.drying.MENSAJE_FACTOR_FUERA_DE_RANGO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FACTOR_NO_NUMERICO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FECHA_FIN_ANTES_DEL_INICIO
import com.cafesmart.app.viewmodel.drying.MENSAJE_FECHA_FIN_FUTURA
import com.cafesmart.app.viewmodel.drying.MENSAJE_KILOS_SIN_CAFE
import com.cafesmart.app.viewmodel.drying.formatoMermaPorcentaje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

class FinalizarSecadoViewModelTest {

    // Inventario del Figma con el verde a $ 14.000 por kg: la merma de 20 kg vale $ 280.000.
    private val repositorio: SecadoRepositoryEjemplo =
        repositorioDePrueba(inventarioFigma().map { it.copy(sublote = it.sublote.copy(costoKg = 14_000)) })

    private val inicio = Fecha(3, 6, 2026)
    private val hoy = Fecha(10, 6, 2026)

    // El ViewModel usa viewModelScope: se crea en @Before, cuando la regla ya puso el dispatcher de prueba.
    @get:Rule
    val main = MainDispatcherRule()

    private var secadoId: Long = 0
    private lateinit var vm: FinalizarSecadoViewModel

    @Before
    fun crearSecadoYViewModel() {
        runBlocking {
            secadoId = repositorio.iniciarSecado(
                inicio,
                listOf(EnvioASecar(1, 120.0), EnvioASecar(2, 80.0), EnvioASecar(3, 60.0), EnvioASecar(4, 30.0))
            ).id
        }
        vm = FinalizarSecadoViewModel(repositorio, secadoId, hoy)
    }
    private val estado get() = vm.uiState.value

    private fun llenarFigma() {
        vm.escribirKilos(Calidad.BUENA, "140")
        vm.escribirKilos(Calidad.REGULAR, "118")
        vm.escribirKilos(Calidad.MALA, "12")
        vm.escribirFactor("92")
    }

    // ---------- Estado inicial ----------

    @Test
    fun alEmpezar_fechaEsHoy_gastosNo_yNoSePuedeConfirmar() = runTest {
        assertEquals(secadoId, estado.secado?.secado?.id)
        assertEquals(hoy, estado.fechaFin)
        assertFalse(estado.huboGastos)
        assertNull(estado.resultado)
        assertFalse(estado.puedeConfirmar)
        assertNull(estado.resumen)
        assertNull(estado.errorKilos)
        assertNull(estado.errorFactor)
        assertEquals("Hoy, 10 de junio de 2026", estado.fechaFinTexto)
    }

    @Test
    fun secadoQueNoEstaEnCurso_noSePuedeConfirmar() = runTest {
        val vm = FinalizarSecadoViewModel(repositorio, 99, hoy)
        assertNull(vm.uiState.value.secado)
        vm.escribirKilos(Calidad.BUENA, "10")
        vm.escribirFactor("92")
        assertFalse(vm.uiState.value.puedeConfirmar)
        vm.confirmar()
        assertNull(vm.uiState.value.resultado)
    }

    // ---------- Kilos ----------

    @Test
    fun kilos_seLimpianComoElPesoDeCompras() = runTest {
        vm.escribirKilos(Calidad.BUENA, "12,5abc")
        assertEquals("12.5", estado.kilosBuena)
        assertEquals("12,5", estado.kilosEnPantalla(Calidad.BUENA))
        assertEquals(12.5, estado.kilosSecosTotalKg, 0.0)
    }

    @Test
    fun kilos_ceroEnTodosDiceQueEscribaAlMenosUnaCalidad() = runTest {
        vm.escribirKilos(Calidad.BUENA, "0")
        assertEquals(MENSAJE_KILOS_SIN_CAFE, estado.errorKilos)
        assertFalse(estado.kilosValidos)
    }

    @Test
    fun kilos_laSumaPasaDeLoEnviadoDiceElMaximo() = runTest {
        vm.escribirKilos(Calidad.BUENA, "150")
        vm.escribirKilos(Calidad.REGULAR, "100")
        vm.escribirKilos(Calidad.MALA, "41")
        assertEquals("Entre los tres, máximo 290 kg.", estado.errorKilos)
        assertEquals("Entre los tres, entre 232 y 290 kg.", estado.ayudaKilos)
        assertFalse(estado.puedeConfirmar.also { vm.escribirFactor("92") })
        assertNull(estado.resumen)
    }

    @Test
    fun kilos_sumaIgualAlEnviadoEsValida() = runTest {
        vm.escribirKilos(Calidad.BUENA, "150")
        vm.escribirKilos(Calidad.REGULAR, "100")
        vm.escribirKilos(Calidad.MALA, "40")
        assertNull(estado.errorKilos)
        assertTrue(estado.kilosValidos)
        assertEquals("0 kg", estado.resumen!!.mermaKgTexto)
    }

    // ---------- Secado de 60 kg: rango de 48 a 60 kg ----------

    private suspend fun vmDe60Kg(): FinalizarSecadoViewModel {
        // Repositorio propio: el de la clase ya envio todos los sublotes verdes.
        val repo60 = repositorioDePrueba()
        val id = repo60.iniciarSecado(inicio, listOf(EnvioASecar(3, 60.0))).id
        return FinalizarSecadoViewModel(repo60, id, hoy)
    }

    @Test
    fun de60Kg_losCamposVienenVaciosYLaAyudaMuestraElRango() = runTest {
        val vm60 = vmDe60Kg()
        val e = vm60.uiState.value
        assertEquals("", e.kilosEnPantalla(Calidad.BUENA))
        assertEquals("", e.kilosEnPantalla(Calidad.REGULAR))
        assertEquals("", e.kilosEnPantalla(Calidad.MALA))
        assertEquals("Entre los tres, entre 48 y 60 kg.", e.ayudaKilos)
    }

    @Test
    fun de60Kg_40mas10mas5_confirmaConMermaDe5kg() = runTest {
        val vm60 = vmDe60Kg()
        vm60.escribirKilos(Calidad.BUENA, "40")
        vm60.escribirKilos(Calidad.REGULAR, "10")
        vm60.escribirKilos(Calidad.MALA, "5")
        vm60.escribirFactor("92")
        val e = vm60.uiState.value
        // 55 de 60 kg: no hace falta igualar lo enviado.
        assertNull(e.errorKilos)
        assertTrue(e.puedeConfirmar)
        assertEquals("5 kg", e.resumen!!.mermaKgTexto)
        assertEquals("8,3 %", e.resumen!!.mermaPorcentajeTexto)
        vm60.confirmar()
        assertTrue(vm60.uiState.value.finalizado)
    }

    @Test
    fun de60Kg_debajoDelMinimoNoSePuedeConfirmar() = runTest {
        val vm60 = vmDe60Kg()
        vm60.escribirKilos(Calidad.BUENA, "40")
        vm60.escribirKilos(Calidad.REGULAR, "5")
        vm60.escribirKilos(Calidad.MALA, "2,9")
        vm60.escribirFactor("92")
        val e = vm60.uiState.value
        assertEquals("Entre los tres, mínimo 48 kg.", e.errorKilos)
        assertFalse(e.puedeConfirmar)
        // Justo en el minimo (48 kg) si se puede.
        vm60.escribirKilos(Calidad.MALA, "3")
        assertTrue(vm60.uiState.value.puedeConfirmar)
    }

    // ---------- Factor ----------

    @Test
    fun factor_fronterasYTextos() = runTest {
        llenarFigma()
        listOf(
            (FACTOR_RENDIMIENTO_MIN - 1).toString() to false,
            FACTOR_RENDIMIENTO_MIN.toString() to true,
            FACTOR_RENDIMIENTO_MAX.toString() to true,
            (FACTOR_RENDIMIENTO_MAX + 1).toString() to false
        ).forEach { (texto, valido) ->
            vm.escribirFactor(texto)
            assertEquals(texto, valido, estado.puedeConfirmar)
            assertEquals(texto, if (valido) null else MENSAJE_FACTOR_FUERA_DE_RANGO, estado.errorFactor)
        }
        vm.escribirFactor("92,5")
        assertEquals(MENSAJE_FACTOR_NO_NUMERICO, estado.errorFactor)
        assertEquals("92,5", estado.factorEnPantalla)
        assertFalse(estado.puedeConfirmar)
        // Vacio es valido: el factor es opcional.
        vm.escribirFactor("")
        assertNull(estado.errorFactor)
        assertTrue(estado.puedeConfirmar)
        assertEquals("Opcional. Entre $FACTOR_RENDIMIENTO_MIN y $FACTOR_RENDIMIENTO_MAX.", estado.ayudaFactor)
    }

    @Test
    fun factor_nacaVacio() = runTest {
        assertEquals("", estado.factorTexto)
        assertEquals("", estado.factorEnPantalla)
    }

    @Test
    fun confirmar_sinFactorFinalizaYGuardaNull() = runTest {
        vm.escribirKilos(Calidad.BUENA, "140")
        vm.escribirKilos(Calidad.REGULAR, "118")
        vm.escribirKilos(Calidad.MALA, "12")
        assertTrue(estado.puedeConfirmar)
        vm.confirmar()
        assertTrue(estado.finalizado)
        assertNull(estado.resultado!!.secado.factorRendimiento)
        assertTrue(repositorio.inventario().filter { it.sublote.secadoId == secadoId }.all { it.sublote.factorRendimiento == null })
    }

    // ---------- Fecha ----------

    @Test
    fun fecha_fronteras() = runTest {
        llenarFigma()
        vm.cambiarFecha(Fecha(2, 6, 2026))
        assertEquals(MENSAJE_FECHA_FIN_ANTES_DEL_INICIO, estado.errorFecha)
        assertFalse(estado.puedeConfirmar)
        vm.cambiarFecha(inicio)
        assertNull(estado.errorFecha)
        assertTrue(estado.puedeConfirmar)
        vm.cambiarFecha(hoy)
        assertNull(estado.errorFecha)
        assertTrue(estado.puedeConfirmar)
        vm.cambiarFecha(Fecha(11, 6, 2026))
        assertEquals(MENSAJE_FECHA_FIN_FUTURA, estado.errorFecha)
        assertFalse(estado.puedeConfirmar)
    }

    // ---------- Paso 5: resumen ----------

    @Test
    fun resumen_casoDelFigma() = runTest {
        llenarFigma()
        val r = estado.resumen!!
        assertEquals("290 kg", r.entradaTexto)
        assertEquals("270 kg", r.salidaSecaTexto)
        assertEquals("20 kg", r.mermaKgTexto)
        assertEquals("6,9 %", r.mermaPorcentajeTexto)
        assertEquals("$ 280.000", r.valorMermaTexto)
        assertEquals("270 kg", estado.kilosSecosTotalTexto)
        assertTrue(estado.puedeConfirmar)
    }

    @Test
    fun resumen_usaLosKilosDeLasTresCalidades() = runTest {
        vm.escribirKilos(Calidad.BUENA, "250")
        assertEquals("40 kg", estado.resumen!!.mermaKgTexto)
        vm.escribirKilos(Calidad.REGULAR, "10")
        assertEquals("30 kg", estado.resumen!!.mermaKgTexto)
        vm.escribirKilos(Calidad.MALA, "10")
        assertEquals("20 kg", estado.resumen!!.mermaKgTexto)
    }

    @Test
    fun formatoMermaPorcentaje_unDecimalConComa() = runTest {
        assertEquals("6,9 %", formatoMermaPorcentaje(6.896))
        assertEquals("0,0 %", formatoMermaPorcentaje(0.0))
    }

    // ---------- Confirmar y Paso 6 ----------

    @Test
    fun confirmar_finalizaYGuardaElResultado() = runTest {
        llenarFigma()
        vm.confirmar()
        val r = estado.resultado!!
        assertEquals(EstadoSecado.FINALIZADO, r.secado.estado)
        assertEquals(hoy, r.secado.fechaFin)
        assertEquals(92, r.secado.factorRendimiento)
        assertEquals(3, r.resultantes.size)
        assertTrue(estado.finalizado)
        assertTrue(repositorio.secadosEnCurso().isEmpty())
        assertEquals(3, repositorio.inventario().count { it.sublote.secadoId == secadoId })
        assertFalse(estado.puedeConfirmar)
    }

    @Test
    fun paso6_resumenFinalConCalidadesYFecha() = runTest {
        llenarFigma()
        vm.cambiarFecha(Fecha(5, 6, 2026))
        vm.confirmar()
        assertEquals(listOf("Calidad buena", "Calidad regular", "Calidad mala"), estado.calidadesSecadas.map { it.etiqueta })
        assertEquals(listOf("140 kg", "118 kg", "12 kg"), estado.calidadesSecadas.map { it.kgTexto })
        assertEquals("5 de junio de 2026", estado.fechaFinResumenTexto)
        val r = estado.resumenFinal!!
        assertEquals("290 kg", r.entradaTexto)
        assertEquals("270 kg", r.salidaSecaTexto)
        assertEquals("20 kg", r.mermaKgTexto)
        assertEquals("6,9 %", r.mermaPorcentajeTexto)
        assertEquals("$ 280.000", r.valorMermaTexto)
    }

    @Test
    fun paso6_soloMuestraLasCalidadesConKg() = runTest {
        vm.escribirKilos(Calidad.REGULAR, "250")
        vm.escribirFactor("100")
        vm.confirmar()
        assertEquals(listOf(Calidad.REGULAR), estado.calidadesSecadas.map { it.calidad })
    }

    @Test
    fun paso6_sinConfirmarNoHayResumenFinal() = runTest {
        assertNull(estado.resumenFinal)
        assertNull(estado.fechaFinResumenTexto)
        assertFalse(estado.finalizado)
    }

    @Test
    fun confirmarDosVeces_soloFinalizaUna() = runTest {
        llenarFigma()
        vm.confirmar()
        val primero = estado.resultado
        val inventario = repositorio.inventario()
        vm.confirmar()
        assertEquals(primero, estado.resultado)
        assertEquals(inventario, repositorio.inventario())
        assertEquals(3, repositorio.inventario().count { it.sublote.secadoId == secadoId })
    }

    @Test
    fun confirmar_incompletoNoHaceNada() = runTest {
        vm.escribirKilos(Calidad.BUENA, "140")
        vm.confirmar()   // los kilos no llegan al minimo
        assertNull(estado.resultado)
        assertEquals(1, repositorio.secadosEnCurso().size)
        assertFalse(estado.bloqueado)
    }

    @Test
    fun confirmar_conFactorFueraDeRangoNoFinaliza() = runTest {
        llenarFigma()
        vm.escribirFactor((FACTOR_RENDIMIENTO_MAX + 1).toString())
        vm.confirmar()
        assertNull(estado.resultado)
        assertEquals(1, repositorio.secadosEnCurso().size)
    }

    @Test
    fun confirmar_conFechaFuturaNoFinaliza() = runTest {
        // El repositorio no valida "no futura": esa regla es de este ViewModel.
        llenarFigma()
        vm.cambiarFecha(Fecha(11, 6, 2026))
        vm.confirmar()
        assertNull(estado.resultado)
        assertEquals(1, repositorio.secadosEnCurso().size)
    }

    @Test
    fun gastos_noCambianElResultado() = runTest {
        llenarFigma()
        vm.elegirGastos(true)
        assertTrue(estado.huboGastos)
        vm.confirmar()
        assertNotNull(estado.resultado)
        assertTrue(estado.huboGastos)
        assertEquals(EstadoSecado.FINALIZADO, estado.resultado!!.secado.estado)
    }

    @Test
    fun despuesDeConfirmarNoSePuedeEditar() = runTest {
        llenarFigma()
        vm.confirmar()
        vm.escribirKilos(Calidad.BUENA, "1")
        vm.escribirFactor("100")
        vm.cambiarFecha(inicio)
        vm.elegirGastos(true)
        assertEquals("140", estado.kilosBuena)
        assertEquals("92", estado.factorTexto)
        assertEquals(hoy, estado.fechaFin)
        assertFalse(estado.huboGastos)
    }

    // ---------- Estado cargando y callbacks (el repositorio es suspend) ----------

    @Test
    fun alCrear_empiezaCargandoYTerminaAlLlegarElSecado() = runTest {
        val lento = RepositorioConPuerta(repositorio)
        val vmLento = FinalizarSecadoViewModel(lento, secadoId, hoy)
        // Mientras llega, el secado es null pero NO esta "ya no en curso": la pantalla espera.
        assertTrue(vmLento.uiState.value.cargando)
        assertNull(vmLento.uiState.value.secado)
        lento.puerta.complete(Unit)
        assertFalse(vmLento.uiState.value.cargando)
        assertEquals(secadoId, vmLento.uiState.value.secado?.secado?.id)
    }

    @Test
    fun secadoQueNoExiste_terminaDeCargarConSecadoNull() = runTest {
        val otro = FinalizarSecadoViewModel(repositorio, 99, hoy)
        assertFalse(otro.uiState.value.cargando)
        assertNull(otro.uiState.value.secado)
    }

    @Test
    fun confirmar_llamaOnConfirmadoUnaVezSiSaleBien() = runTest {
        llenarFigma()
        var llamadas = 0
        var finalizadoAlAvisar = false
        vm.confirmar {
            llamadas++
            finalizadoAlAvisar = estado.finalizado
        }
        assertEquals(1, llamadas)
        // Cuando avisa, el resultado ya esta en el estado.
        assertTrue(finalizadoAlAvisar)
        // Un segundo toque no vuelve a finalizar ni a llamar.
        vm.confirmar { llamadas++ }
        assertEquals(1, llamadas)
    }

    @Test
    fun confirmar_noLlamaOnConfirmadoSiElRepositorioRechaza() = runTest {
        llenarFigma()
        // El secado se cancela por otro lado: el repositorio ya no lo puede finalizar.
        repositorio.cancelarSecado(secadoId)
        var llamadas = 0
        vm.confirmar { llamadas++ }
        assertEquals(0, llamadas)
        assertFalse(estado.finalizado)
        assertFalse(estado.bloqueado)
        assertNotNull(estado.errorAlConfirmar)
    }

    @Test
    fun confirmar_incompletoNoLlamaOnConfirmado() = runTest {
        var llamadas = 0
        vm.confirmar { llamadas++ }
        assertEquals(0, llamadas)
    }
}
