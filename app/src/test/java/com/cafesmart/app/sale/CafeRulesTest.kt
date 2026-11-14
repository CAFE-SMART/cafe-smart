package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.viewmodel.sale.MENSAJE_FECHA_VENTA
import com.cafesmart.app.viewmodel.sale.MILIS_POR_DIA
import com.cafesmart.app.viewmodel.sale.VENTA_MINIMA_KG
import com.cafesmart.app.viewmodel.sale.ayudaPrecio
import com.cafesmart.app.viewmodel.sale.costoPromedio
import com.cafesmart.app.viewmodel.sale.descontarVenta
import com.cafesmart.app.viewmodel.sale.diasDesde
import com.cafesmart.app.viewmodel.sale.errorFecha
import com.cafesmart.app.viewmodel.sale.errorTotalKg
import com.cafesmart.app.viewmodel.sale.resumenGrupo
import com.cafesmart.app.viewmodel.sale.sublotesDelTipo
import com.cafesmart.app.viewmodel.sale.textoComprado
import com.cafesmart.app.viewmodel.sale.textoSeleccionados
import com.cafesmart.app.viewmodel.sale.tiposParaVender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Reglas puras de los pasos 2 y 3 de Ventas (pestanas, dias, costo promedio, fecha y descuento del inventario).
class CafeRulesTest {

    private fun sublote(
        id: Long,
        tipo: TipoCafe = TipoCafe.SECO,
        calidad: Calidad = Calidad.BUENA,
        disponibleKg: Double = 100.0,
        costoKg: Long = 18_000,
        ingreso: Fecha = Fecha(1, 11, 2026)
    ) = SubloteConLote(
        Sublote(
            id = id, loteId = id, compraId = id,
            pesoInicialKg = maxOf(500.0, disponibleKg), pesoDisponibleKg = disponibleKg,
            costoKg = costoKg, fechaIngreso = ingreso
        ),
        Lote(id, tipo, calidad)
    )

    // ---------- Pestanas y sublotes del tipo ----------

    @Test
    fun pestanas_siguenElOrdenDelFigma() {
        assertEquals(listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA), tiposParaVender)
    }

    @Test
    fun pestanas_incluyenTodosLosTiposDeCafe() {
        assertEquals(TipoCafe.entries.toSet(), tiposParaVender.toSet())
    }

    @Test
    fun sublotesDelTipo_soloDevuelveLosDeEseTipo() {
        val inventario = listOf(sublote(1, TipoCafe.VERDE), sublote(2, TipoCafe.SECO), sublote(3, TipoCafe.SECO))
        assertEquals(listOf(2L, 3L), sublotesDelTipo(inventario, TipoCafe.SECO).map { it.sublote.id })
    }

    @Test
    fun sublotesDelTipo_dejaFueraLosAgotados() {
        val inventario = listOf(sublote(1, disponibleKg = 0.0), sublote(2, disponibleKg = 10.0))
        assertEquals(listOf(2L), sublotesDelTipo(inventario, TipoCafe.SECO).map { it.sublote.id })
    }

    @Test
    fun sublotesDelTipo_unRestoMinimoDePesoCuentaComoAgotado() {
        // 0.0001 kg es menor que EPSILON_KG (0.0005): ya no hay cafe que vender
        assertTrue(sublotesDelTipo(listOf(sublote(1, disponibleKg = 0.0001)), TipoCafe.SECO).isEmpty())
    }

    @Test
    fun sublotesDelTipo_ordenaPorCalidadYLuegoPorId() {
        val inventario = listOf(
            sublote(9, calidad = Calidad.MALA),
            sublote(7, calidad = Calidad.REGULAR),
            sublote(6, calidad = Calidad.BUENA),
            sublote(5, calidad = Calidad.BUENA)
        )
        assertEquals(listOf(5L, 6L, 7L, 9L), sublotesDelTipo(inventario, TipoCafe.SECO).map { it.sublote.id })
    }

    @Test
    fun sublotesDelTipo_tipoSinCafeDevuelveListaVacia() {
        assertTrue(sublotesDelTipo(listOf(sublote(1)), TipoCafe.PASILLA).isEmpty())
    }

    @Test
    fun sublotesDelTipo_inventarioVacio() {
        assertTrue(sublotesDelTipo(emptyList(), TipoCafe.SECO).isEmpty())
    }

    // ---------- Textos ----------

    @Test
    fun textoSeleccionados_cuentaLosElegidos() {
        assertEquals("0 de 4 seleccionados", textoSeleccionados(0, 4))
        assertEquals("1 de 4 seleccionados", textoSeleccionados(1, 4))
        assertEquals("4 de 4 seleccionados", textoSeleccionados(4, 4))
    }

    @Test
    fun resumenGrupo_singularYPlural() {
        assertEquals("1 sublote · 200 kg", resumenGrupo(1, 200.0))
        assertEquals("2 sublotes · 280 kg", resumenGrupo(2, 280.0))
    }

    @Test
    fun resumenGrupo_conDecimalesUsaComa() {
        assertEquals("2 sublotes · 25,5 kg", resumenGrupo(2, 25.5))
    }

    @Test
    fun resumenGrupo_miles() {
        assertEquals("3 sublotes · 1.250 kg", resumenGrupo(3, 1250.0))
    }

    // ---------- Dias desde la compra ----------

    @Test
    fun diasDesde_mismoDiaEsCero() {
        assertEquals(0, diasDesde(Fecha(3, 11, 2026), Fecha(3, 11, 2026)))
    }

    @Test
    fun diasDesde_dentroDelMismoMes() {
        assertEquals(6, diasDesde(Fecha(20, 10, 2026), Fecha(26, 10, 2026)))
    }

    @Test
    fun diasDesde_cruzandoDeMes() {
        // 28 de octubre al 3 de noviembre: 6 dias
        assertEquals(6, diasDesde(Fecha(28, 10, 2026), Fecha(3, 11, 2026)))
    }

    @Test
    fun diasDesde_cruzandoDeAnio() {
        assertEquals(3, diasDesde(Fecha(30, 12, 2025), Fecha(2, 1, 2026)))
    }

    @Test
    fun diasDesde_anioBisiesto() {
        // 2028 es bisiesto: del 28 de febrero al 1 de marzo hay 2 dias
        assertEquals(2, diasDesde(Fecha(28, 2, 2028), Fecha(1, 3, 2028)))
        // 2026 no lo es: solo 1
        assertEquals(1, diasDesde(Fecha(28, 2, 2026), Fecha(1, 3, 2026)))
    }

    @Test
    fun diasDesde_fechaFuturaDaNegativo() {
        assertEquals(-2, diasDesde(Fecha(5, 11, 2026), Fecha(3, 11, 2026)))
    }

    @Test
    fun milisPorDia_son24Horas() {
        assertEquals(24L * 60 * 60 * 1000, MILIS_POR_DIA)
    }

    @Test
    fun textoComprado_hoyAyerYVariosDias() {
        assertEquals("Comprado hoy", textoComprado(0))
        assertEquals("Comprado hace 1 día", textoComprado(1))
        assertEquals("Comprado hace 2 días", textoComprado(2))
        assertEquals("Comprado hace 6 días", textoComprado(6))
    }

    @Test
    fun textoComprado_unNegativoNoMuestraDiasNegativos() {
        assertEquals("Comprado hoy", textoComprado(-3))
    }

    // ---------- Costo promedio (ayuda del precio) ----------

    private fun elegido(id: Long, cantidadKg: Double, costoKg: Long, calidad: Calidad = Calidad.BUENA) =
        SubloteElegido(sublote(id, calidad = calidad, disponibleKg = 1000.0, costoKg = costoKg), cantidadKg)

    @Test
    fun costoPromedio_unSoloSubloteEsSuCosto() {
        assertEquals(18_000L, costoPromedio(listOf(elegido(1, 150.0, 18_000))))
    }

    @Test
    fun costoPromedio_pesaMasElSubloteDelQueSeVendeMas() {
        // (100 kg x 10.000 + 300 kg x 20.000) / 400 kg = 17.500
        assertEquals(17_500L, costoPromedio(listOf(elegido(1, 100.0, 10_000), elegido(2, 300.0, 20_000))))
    }

    @Test
    fun costoPromedio_conIgualCantidadEsElPromedioSimple() {
        assertEquals(16_000L, costoPromedio(listOf(elegido(1, 50.0, 14_000), elegido(2, 50.0, 18_000))))
    }

    @Test
    fun costoPromedio_usaLaCantidadQueSeVendeNoLaDisponible() {
        // Se venden solo 10 kg del primero (a 10.000) y 90 kg del segundo (a 20.000): 19.000
        assertEquals(19_000L, costoPromedio(listOf(elegido(1, 10.0, 10_000), elegido(2, 90.0, 20_000))))
    }

    @Test
    fun costoPromedio_redondeaAPesosEnteros() {
        // (1 kg x 10.000 + 2 kg x 10.001) / 3 = 10.000,67 -> 10.001
        assertEquals(10_001L, costoPromedio(listOf(elegido(1, 1.0, 10_000), elegido(2, 2.0, 10_001))))
    }

    @Test
    fun costoPromedio_sinSublotesEsCero() {
        assertEquals(0L, costoPromedio(emptyList()))
    }

    @Test
    fun ayudaPrecio_textoDelFigma() {
        assertEquals("Entre $ 1.000 y $ 100.000. Costo promedio: $ 18.000 por kg.", ayudaPrecio(18_000))
    }

    @Test
    fun ayudaPrecio_otroCosto() {
        assertEquals("Entre $ 1.000 y $ 100.000. Costo promedio: $ 12.500 por kg.", ayudaPrecio(12_500))
    }

    // ---------- Fecha de la venta ----------

    private val hoy = Fecha(10, 11, 2026)

    @Test
    fun errorFecha_hoyEsValida() {
        assertNull(errorFecha(hoy, hoy, listOf(elegido(1, 10.0, 18_000))))
    }

    @Test
    fun errorFecha_unaFechaFuturaNoSirve() {
        assertEquals(MENSAJE_FECHA_VENTA, errorFecha(Fecha(11, 11, 2026), hoy, emptyList()))
    }

    @Test
    fun errorFecha_futuraEnOtroAnio() {
        assertNotNull(errorFecha(Fecha(1, 1, 2027), hoy, emptyList()))
    }

    @Test
    fun errorFecha_ayerEsValidaSiElCafeYaHabiaEntrado() {
        assertNull(errorFecha(Fecha(9, 11, 2026), hoy, listOf(elegido(1, 10.0, 18_000))))
    }

    @Test
    fun errorFecha_noPuedeSerAnteriorALaEntradaDelCafe() {
        // El sublote entro el 1 de noviembre: vender el 31 de octubre no tiene sentido
        assertEquals(
            "La fecha no puede ser anterior a la entrada del café (1 de noviembre de 2026).",
            errorFecha(Fecha(31, 10, 2026), hoy, listOf(elegido(1, 10.0, 18_000)))
        )
    }

    @Test
    fun errorFecha_elMismoDiaDeLaEntradaEsValido() {
        assertNull(errorFecha(Fecha(1, 11, 2026), hoy, listOf(elegido(1, 10.0, 18_000))))
    }

    @Test
    fun errorFecha_conVariosSublotesManda_elQueEntroUltimo() {
        val viejo = SubloteElegido(sublote(1, ingreso = Fecha(1, 10, 2026)), 10.0)
        val nuevo = SubloteElegido(sublote(2, ingreso = Fecha(5, 11, 2026)), 10.0)
        assertNull(errorFecha(Fecha(6, 11, 2026), hoy, listOf(viejo, nuevo)))
        assertEquals(
            "La fecha no puede ser anterior a la entrada del café (5 de noviembre de 2026).",
            errorFecha(Fecha(3, 11, 2026), hoy, listOf(viejo, nuevo))
        )
    }

    @Test
    fun errorFecha_antesDeEnero2026NoSirve() {
        assertEquals("Solo puedes registrar ventas desde 2026 hasta hoy.", errorFecha(Fecha(31, 12, 2025), hoy, emptyList()))
    }

    @Test
    fun errorFecha_primeroDeEnero2026EsElLimite() {
        assertNull(errorFecha(Fecha(1, 1, 2026), hoy, emptyList()))
    }

    @Test
    fun errorFecha_sinSublotesSoloRevisaElRangoDeFechas() {
        assertNull(errorFecha(Fecha(15, 6, 2026), hoy, emptyList()))
    }

    // ---------- Minimo de una venta ----------

    @Test
    fun errorTotalKg_menosDeCincoKgMuestraError() {
        assertEquals("La cantidad total a vender debe ser mínimo 5 kg.", errorTotalKg(4.99, true))
        assertEquals("La cantidad total a vender debe ser mínimo 5 kg.", errorTotalKg(0.5, true))
    }

    @Test
    fun errorTotalKg_cincoKgExactosSirve() {
        assertNull(errorTotalKg(VENTA_MINIMA_KG, true))
        assertNull(errorTotalKg(5.0, true))
    }

    @Test
    fun errorTotalKg_masDeCincoKgSirve() {
        assertNull(errorTotalKg(150.0, true))
    }

    @Test
    fun errorTotalKg_sinNadaElegidoNoMuestraError() {
        assertNull(errorTotalKg(0.0, false))
    }

    // ---------- Descuento del inventario ----------

    @Test
    fun descontarVenta_restaLoVendidoDelSublote() {
        val resultado = descontarVenta(listOf(sublote(1, disponibleKg = 200.0)), listOf(elegido(1, 150.0, 18_000)))
        assertEquals(50.0, resultado.single().sublote.pesoDisponibleKg, 0.0001)
    }

    @Test
    fun descontarVenta_venderTodoLoDejaAgotado() {
        val resultado = descontarVenta(listOf(sublote(1, disponibleKg = 200.0)), listOf(elegido(1, 200.0, 18_000)))
        assertTrue(resultado.single().sublote.estaAgotado)
    }

    @Test
    fun descontarVenta_noTocaLosSublotesQueNoSeVendieron() {
        val otro = sublote(2, disponibleKg = 80.0)
        val resultado = descontarVenta(listOf(sublote(1, disponibleKg = 200.0), otro), listOf(elegido(1, 150.0, 18_000)))
        assertEquals(otro, resultado[1])
    }

    @Test
    fun descontarVenta_conservaElOrdenYLaCantidadDeSublotes() {
        val inventario = listOf(sublote(3), sublote(1), sublote(2))
        assertEquals(listOf(3L, 1L, 2L), descontarVenta(inventario, listOf(elegido(1, 10.0, 18_000))).map { it.sublote.id })
    }

    @Test
    fun descontarVenta_conDecimalesNoDejaRestosDeDouble() {
        // 0.3 - 0.1 en Double da 0.19999999999999998; debe quedar 0,2
        val resultado = descontarVenta(listOf(sublote(1, disponibleKg = 0.3)), listOf(elegido(1, 0.1, 18_000)))
        assertEquals(0.2, resultado.single().sublote.pesoDisponibleKg, 0.0)
    }

    @Test
    fun descontarVenta_nuncaQuedaNegativo() {
        val resultado = descontarVenta(listOf(sublote(1, disponibleKg = 10.0)), listOf(elegido(1, 50.0, 18_000)))
        assertEquals(0.0, resultado.single().sublote.pesoDisponibleKg, 0.0)
    }

    @Test
    fun descontarVenta_variosSublotesALaVez() {
        val resultado = descontarVenta(
            listOf(sublote(1, disponibleKg = 200.0), sublote(2, disponibleKg = 100.0)),
            listOf(elegido(1, 200.0, 18_000), elegido(2, 40.0, 18_000))
        )
        assertEquals(listOf(0.0, 60.0), resultado.map { it.sublote.pesoDisponibleKg })
    }

    @Test
    fun descontarVenta_sinNadaElegidoNoCambiaElInventario() {
        val inventario = listOf(sublote(1), sublote(2))
        assertEquals(inventario, descontarVenta(inventario, emptyList()))
    }

    @Test
    fun descontarVenta_noCambiaElSubloteOriginal() {
        val original = sublote(1, disponibleKg = 200.0)
        descontarVenta(listOf(original), listOf(elegido(1, 150.0, 18_000)))
        assertEquals(200.0, original.sublote.pesoDisponibleKg, 0.0)
    }
}
