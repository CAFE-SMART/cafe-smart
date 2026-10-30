package com.cafesmart.app.purchase

import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.purchase.Calidad
import com.cafesmart.app.data.purchase.Fecha
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.Productor
import com.cafesmart.app.data.purchase.TipoDocumento
import com.cafesmart.app.viewmodel.purchase.cafesCompletos
import com.cafesmart.app.viewmodel.purchase.crearSublote
import com.cafesmart.app.viewmodel.purchase.errorPeso
import com.cafesmart.app.viewmodel.purchase.errorPrecio
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.filtrarProductores
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import com.cafesmart.app.viewmodel.purchase.formatoFechaLarga
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.hayMasProductores
import com.cafesmart.app.viewmodel.purchase.limpiarDocumento
import com.cafesmart.app.viewmodel.purchase.limpiarNombre
import com.cafesmart.app.viewmodel.purchase.limpiarPeso
import com.cafesmart.app.viewmodel.purchase.limpiarPrecio
import com.cafesmart.app.viewmodel.purchase.mensajeAgregarCafe
import com.cafesmart.app.viewmodel.purchase.mensajePendientePaso1
import com.cafesmart.app.viewmodel.purchase.mensajePendientePaso2
import com.cafesmart.app.viewmodel.purchase.milisAFecha
import com.cafesmart.app.viewmodel.purchase.precioEnPantalla
import com.cafesmart.app.viewmodel.purchase.productoresVisibles
import com.cafesmart.app.viewmodel.purchase.puedeContinuarPaso1
import com.cafesmart.app.viewmodel.purchase.totalDe
import com.cafesmart.app.viewmodel.purchase.totalEstimado
import com.cafesmart.app.viewmodel.purchase.totalKg
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseRulesTest {

    private val productores = listOf(
        Productor(nombre = "Juan Pérez", documento = "123456789"),
        Productor(nombre = "Ana Gómez", documento = "900123456"),
        Productor(nombre = "Pepito Pérez", documento = "890301886"),
        Productor(nombre = "Andy López", documento = "11140596")
    )

    private val cafeCompleto = CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14000")

    // ---------- Paso 1 ----------

    @Test
    fun filtrarProductores_porNombreSinImportarMayusculas() {
        assertEquals(listOf(productores[1]), filtrarProductores(productores, "ana"))
    }

    @Test
    fun filtrarProductores_porDocumento() {
        assertEquals(listOf(productores[0]), filtrarProductores(productores, "1234567"))
    }

    @Test
    fun recientes_muestraLosUltimos3PrimeroElMasNuevo() {
        val visibles = productoresVisibles(productores, "", verTodos = false)
        assertEquals(listOf(productores[3], productores[2], productores[1]), visibles)
        assertTrue(hayMasProductores(productores, "", verTodos = false))
        assertEquals(4, productoresVisibles(productores, "", verTodos = true).size)
        assertFalse(hayMasProductores(productores, "", verTodos = true))
    }

    @Test
    fun continuarPaso1_segunLaOpcionElegida() {
        val nuevoValido = NuevoProductor(TipoDocumento.CC, "1114059684", "Pepito Pérez", "")
        assertFalse(puedeContinuarPaso1(null, null, NuevoProductor(), productores))
        assertTrue(puedeContinuarPaso1(ModoProductor.SIN_REGISTRAR, null, NuevoProductor(), productores))
        assertFalse(puedeContinuarPaso1(ModoProductor.REGISTRADO, null, NuevoProductor(), productores))
        assertTrue(puedeContinuarPaso1(ModoProductor.REGISTRADO, productores[0], NuevoProductor(), productores))
        assertFalse(puedeContinuarPaso1(ModoProductor.NUEVO, null, NuevoProductor(), productores))
        assertTrue(puedeContinuarPaso1(ModoProductor.NUEVO, null, nuevoValido, productores))
    }

    @Test
    fun mensajesPendientesPaso1_RF_COM_01_y_02() {
        assertEquals(
            "Por favor, selecciona una opción para continuar el proceso.",
            mensajePendientePaso1(null, null, NuevoProductor(), productores)
        )
        assertEquals(
            "Elige un productor de la lista.",
            mensajePendientePaso1(ModoProductor.REGISTRADO, null, NuevoProductor(), productores)
        )
        assertNull(mensajePendientePaso1(ModoProductor.SIN_REGISTRAR, null, NuevoProductor(), productores))
    }

    @Test
    fun nombre_soloLetrasYEspacios_RF_COM_05() {
        assertNull(validarNombre("Juan Pérez", TipoDocumento.CC))
        assertEquals("Escribe tu nombre y apellido.", validarNombre("Juan", TipoDocumento.CC))
        assertNotNull(validarNombre("Juan P", TipoDocumento.CC))
        assertEquals("Juan Pérez", limpiarNombre("Juan2 Pérez!"))
        assertNull(validarNombre("Cafe Los Alpes", TipoDocumento.NIT))
        assertNotNull(validarNombre("", TipoDocumento.NIT))
    }

    @Test
    fun cedula_de6a10Digitos_RF_COM_04() {
        assertNull(validarDocumento("1234567890", TipoDocumento.CC, productores))
        assertNotNull(validarDocumento("12345", TipoDocumento.CC, productores))
        assertEquals("1234567890", limpiarDocumento("1.234-567 890123"))
    }

    @Test
    fun nit_de9a10Digitos_RF_COM_04() {
        assertNull(validarDocumento("9001234567", TipoDocumento.NIT, productores))
        assertNull(validarDocumento("900123457", TipoDocumento.NIT, productores))
        assertNotNull(validarDocumento("90012345", TipoDocumento.NIT, productores))
    }

    @Test
    fun documentoRepetido_RF_COM_04() {
        assertEquals(
            "Este documento ya está registrado. Verifica en tu lista.",
            validarDocumento("123456789", TipoDocumento.CC, productores)
        )
    }

    @Test
    fun celular_opcionalODe10QueEmpiezaPor3() {
        assertNull(validarTelefono(""))
        assertNull(validarTelefono("3001234567"))
        assertNotNull(validarTelefono("6012345678"))
        assertNotNull(validarTelefono("300123"))
    }

    // ---------- Paso 2 ----------

    @Test
    fun limpiarPeso_aceptaComaYUnSoloPunto() {
        assertEquals("25.5", limpiarPeso("25,5"))
        assertEquals("25.55", limpiarPeso("25.5.5"))
        assertEquals("30", limpiarPeso("30kg"))
        // RF-COM-09: maximo 3 decimales
        assertEquals("25.125", limpiarPeso("25.12567"))
    }

    @Test
    fun precio_seGuardaConDigitosYSeMuestraConPuntoDeMiles() {
        assertEquals("140005", limpiarPrecio("14.0005"))
        assertEquals("14.000", precioEnPantalla("14000"))
        assertEquals("", precioEnPantalla(""))
    }

    @Test
    fun crearSublote_conDatosCompletos() {
        val sublote = crearSublote(cafeCompleto)
        assertNotNull(sublote)
        assertEquals(350000.0, totalDe(sublote!!), 0.001)
    }

    @Test
    fun crearSublote_faltaDatoOFueraDeRango_devuelveNull() {
        assertNull(crearSublote(cafeCompleto.copy(tipoCafe = null)))
        assertNull(crearSublote(cafeCompleto.copy(calidad = null)))
        assertNull(crearSublote(cafeCompleto.copy(peso = "0")))
        assertNull(crearSublote(cafeCompleto.copy(peso = "100000")))
        assertNull(crearSublote(cafeCompleto.copy(precio = "100001")))
        // RF-COM-10: minimo $1.000
        assertNull(crearSublote(cafeCompleto.copy(precio = "999")))
    }

    @Test
    fun errores_soloCuandoSePasaDelMaximo() {
        assertNull(errorPeso(cafeCompleto))
        assertNotNull(errorPeso(cafeCompleto.copy(peso = "100000")))
        assertNull(errorPrecio(cafeCompleto.copy(precio = "100000")))
        assertNotNull(errorPrecio(cafeCompleto.copy(precio = "100001")))
        assertNotNull(errorPeso(cafeCompleto.copy(peso = "0")))
        // Mientras escribe "14" no marca error; con 4 digitos ya revisa el minimo
        assertNull(errorPrecio(cafeCompleto.copy(precio = "14")))
        assertNull(errorPrecio(cafeCompleto.copy(precio = "1000")))
    }

    @Test
    fun mensajesPaso2_RF_COM_07_08_11() {
        assertEquals("Elige el tipo de café que comprarás.", mensajePendientePaso2(listOf(CafeFormulario())))
        assertEquals(
            "Elige la calidad del café que te interesa.",
            mensajePendientePaso2(listOf(CafeFormulario(tipoCafe = "Verde")))
        )
        assertNull(mensajePendientePaso2(listOf(cafeCompleto)))
        assertEquals(
            "Recuerda terminar de diligenciar los datos de Café 2 antes de agregar otro.",
            mensajeAgregarCafe(listOf(cafeCompleto, CafeFormulario()))
        )
    }

    @Test
    fun totales_sumanTodasLasTarjetas() {
        val cafes = listOf(cafeCompleto, CafeFormulario(peso = "10", precio = "1000"))
        assertEquals(35.0, totalKg(cafes), 0.001)
        assertEquals(360000.0, totalEstimado(cafes), 0.001)
        assertFalse(cafesCompletos(cafes))
        assertTrue(cafesCompletos(listOf(cafeCompleto)))
    }

    // ---------- Formatos y fechas ----------

    @Test
    fun formatos_estiloColombiano() {
        assertEquals("25 kg", formatoKg(25.0))
        assertEquals("25,5 kg", formatoKg(25.5))
        assertEquals("25,125 kg", formatoKg(25.125))
        assertEquals("1.200 kg", formatoKg(1200.0))
        assertEquals("$ 350.000", formatoDinero(350000.0))
    }

    @Test
    fun fechaLarga_diceHoySiEsHoy() {
        val fecha = Fecha(dia = 2, mes = 6, anio = 2026)
        assertEquals("Hoy, 2 de junio de 2026", formatoFechaLarga(fecha, hoy = fecha))
        assertEquals("2 de junio de 2026", formatoFechaLarga(fecha, hoy = Fecha(3, 6, 2026)))
    }

    @Test
    fun fecha_idaYVueltaConMilisegundosDelCalendario() {
        val fecha = Fecha(dia = 31, mes = 12, anio = 2026)
        assertEquals(fecha, milisAFecha(fechaAMilis(fecha)))
    }
}
