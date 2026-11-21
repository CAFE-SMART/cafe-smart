package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.viewmodel.purchase.NOMBRE_MAX
import com.cafesmart.app.viewmodel.purchase.coincideBusqueda
import com.cafesmart.app.viewmodel.purchase.conDocumento
import com.cafesmart.app.viewmodel.purchase.conNombre
import com.cafesmart.app.viewmodel.purchase.conTelefono
import com.cafesmart.app.viewmodel.purchase.conTipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Lo que pasa con cada campo del formulario de una persona (productor o cliente) mientras se escribe.
// Estas funciones las usan Compras y Ventas, por eso se prueban aqui, aparte de cada ViewModel.
class FormularioPersonaTest {

    private val cedula = NuevoProductor(tipo = TipoDocumento.CC)
    private val nit = NuevoProductor(tipo = TipoDocumento.NIT)

    // ---------- Nombre ----------

    @Test
    fun nombreConCedula_noPermiteNumeros() {
        assertEquals("Juan Pérez", cedula.conNombre("Juan123 Pérez").nombre)
        assertEquals("", cedula.conNombre("123456").nombre)
    }

    @Test
    fun nombreConCedula_noPermiteCaracteresRaros() {
        assertEquals("Juan Pérez", cedula.conNombre("Juan Pérez!!@#").nombre)
        assertEquals("JuanPérez", cedula.conNombre("Juan.Pérez&").nombre)
        assertEquals("", cedula.conNombre("!@#$%^&*()").nombre)
        assertTrue(cedula.conNombre("Ana 😀 Ríos").nombre.all { it.isLetter() || it == ' ' })
    }

    @Test
    fun nombreConCedula_conservaTildesYEnie() {
        assertEquals("Muñoz Núñez", cedula.conNombre("Muñoz Núñez").nombre)
        assertEquals("María José", cedula.conNombre("María José").nombre)
    }

    @Test
    fun nombreConNit_permiteNumerosPuntoYAmpersand() {
        assertEquals("Café 2000 S.A.S.", nit.conNombre("Café 2000 S.A.S.").nombre)
        assertEquals("Hermanos López & Cía", nit.conNombre("Hermanos López & Cía").nombre)
    }

    @Test
    fun nombreConNit_noPermiteOtrosSimbolos() {
        assertEquals("Café SAS", nit.conNombre("Café #SAS!@").nombre)
        assertEquals("", nit.conNombre("###").nombre)
    }

    @Test
    fun nombre_nuncaPasaDeLaLongitudMaxima() {
        assertEquals(NOMBRE_MAX, cedula.conNombre("A".repeat(80)).nombre.length)
        assertEquals(NOMBRE_MAX, nit.conNombre("1".repeat(80)).nombre.length)
        assertEquals(30, NOMBRE_MAX)
    }

    @Test
    fun nombre_conLaLongitudExactaSeConserva() {
        assertEquals("A".repeat(30), cedula.conNombre("A".repeat(30)).nombre)
    }

    // ---------- Tipo de documento ----------

    @Test
    fun alPasarDeNitACedula_elNombreSeLimpia() {
        val formulario = NuevoProductor(tipo = TipoDocumento.NIT, nombre = "Café 2000 S.A.S.")
        val nuevo = formulario.conTipo(TipoDocumento.CC)
        assertEquals(TipoDocumento.CC, nuevo.tipo)
        assertEquals("Café SAS", nuevo.nombre)
    }

    @Test
    fun alPasarDeCedulaANit_elNombreSeConserva() {
        val formulario = NuevoProductor(tipo = TipoDocumento.CC, nombre = "Juan Pérez")
        val nuevo = formulario.conTipo(TipoDocumento.NIT)
        assertEquals(TipoDocumento.NIT, nuevo.tipo)
        assertEquals("Juan Pérez", nuevo.nombre)
    }

    @Test
    fun alCambiarDeTipo_seQuitanLosEspaciosDeMas() {
        val formulario = NuevoProductor(tipo = TipoDocumento.NIT, nombre = "  Café   2000 ")
        assertEquals("Café", formulario.conTipo(TipoDocumento.CC).nombre)
    }

    @Test
    fun alCambiarDeTipo_noSeTocanElDocumentoNiElCelular() {
        val formulario = NuevoProductor(documento = "1114059684", telefono = "3001234567")
        val nuevo = formulario.conTipo(TipoDocumento.NIT)
        assertEquals("1114059684", nuevo.documento)
        assertEquals("3001234567", nuevo.telefono)
    }

    // ---------- Documento ----------

    @Test
    fun documento_soloLlevaNumeros() {
        assertEquals("1234567", cedula.conDocumento("1.234-567 abc").documento)
        assertEquals("", cedula.conDocumento("abcdef").documento)
        assertEquals("", cedula.conDocumento("!@#").documento)
    }

    @Test
    fun documento_noTomaLosPuntosNiLosGuiones() {
        assertEquals("1114059684", cedula.conDocumento("1.114.059.684").documento)
        assertEquals("9003421215", nit.conDocumento("900.342.121-5").documento)
    }

    @Test
    fun documento_nuncaPasaDeDiezDigitos() {
        assertEquals(10, cedula.conDocumento("1".repeat(30)).documento.length)
        assertEquals(10, nit.conDocumento("9".repeat(30)).documento.length)
        assertEquals("1234567890", cedula.conDocumento("12345678901234").documento)
    }

    @Test
    fun documento_conDiezDigitosExactosSeConserva() {
        assertEquals("1234567890", cedula.conDocumento("1234567890").documento)
    }

    // ---------- Celular ----------

    @Test
    fun celular_noPermiteLetras() {
        assertEquals("", cedula.conTelefono("abcdefghij").telefono)
        assertEquals("30012345", cedula.conTelefono("300-123 45abc").telefono)
    }

    @Test
    fun celular_soloLlevaNumeros() {
        assertEquals("3001234567", cedula.conTelefono("(300) 123-4567").telefono)
        assertEquals("", cedula.conTelefono("+-()").telefono)
    }

    @Test
    fun celular_nuncaPasaDeDiezDigitos() {
        assertEquals(10, cedula.conTelefono("3".repeat(30)).telefono.length)
        assertEquals("5730012345", cedula.conTelefono("+57 300 123 4567").telefono)
    }

    // ---------- Los demas campos no se tocan ----------

    @Test
    fun cambiarUnCampo_noCambiaLosOtros() {
        val formulario = NuevoProductor(TipoDocumento.NIT, "9003421215", "Café Los Alpes", "3001234567")
        assertEquals(formulario.copy(documento = "123456"), formulario.conDocumento("123456"))
        assertEquals(formulario.copy(nombre = "Otra Finca"), formulario.conNombre("Otra Finca"))
        assertEquals(formulario.copy(telefono = "3109876543"), formulario.conTelefono("3109876543"))
    }

    // ---------- Busqueda ----------

    @Test
    fun busqueda_porNombreSinImportarMayusculas() {
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "carlos"))
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "MEJÍA"))
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "los me"))
    }

    @Test
    fun busqueda_porDocumento_completoOParcial() {
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "16780012"))
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "7800"))
    }

    @Test
    fun busqueda_ignoraLosEspaciosDeLasPuntas() {
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", "  carlos  "))
        assertTrue(coincideBusqueda("Carlos Mejía", "16780012", " 16780012 "))
    }

    @Test
    fun busqueda_sinCoincidencia() {
        assertFalse(coincideBusqueda("Carlos Mejía", "16780012", "zzz"))
        assertFalse(coincideBusqueda("Carlos Mejía", "16780012", "99999"))
    }

    @Test
    fun busqueda_noMiraElTipoDeDocumento() {
        // "Cédula" o "NIT" son solo la etiqueta de la fila: no se buscan.
        assertFalse(coincideBusqueda("Carlos Mejía", "16780012", "cédula"))
    }
}
