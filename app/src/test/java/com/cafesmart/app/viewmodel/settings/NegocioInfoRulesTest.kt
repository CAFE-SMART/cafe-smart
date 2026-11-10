package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.ui.auth.BusinessType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NegocioInfoRulesTest {

    private val valido = NegocioFormulario("Café La Esperanza", BusinessType.COMPRAVENTA, "3001234567")
    private val guardado = NegocioEntity(
        uid = "u1", nombre = "Viejo", tipo = "COOPERATIVA", telefono = "3110000000",
        capacidadBodegaKg = 3000.0, pesoBultoKg = 41.0, creadoEn = 5L
    )

    @Test fun formularioCompletoNoTieneErrores() = assertTrue(validarNegocio(valido).isEmpty())

    // Mismos mensajes que el registro.
    @Test fun formularioVacioMarcaLosTresCampos() {
        val e = validarNegocio(NegocioFormulario())
        assertEquals("Escribe el nombre del negocio.", e["businessName"])
        assertEquals("Selecciona el tipo de negocio.", e["businessType"])
        assertEquals("Ingresa un celular de 10 dígitos que empiece por 3.", e["phone"])
    }

    @Test fun nombreDeMasDe30CaracteresEsError() =
        assertEquals("Máximo 30 caracteres.", validarNegocio(valido.copy(nombre = "x".repeat(31)))["businessName"])

    @Test fun nombreDe30CaracteresEsValido() =
        assertTrue(validarNegocio(valido.copy(nombre = "x".repeat(30))).isEmpty())

    @Test fun telefonoQueNoEmpiezaPor3OConOtroLargoEsError() {
        listOf("2001234567", "300123456", "30012345678", "", "300123456a").forEach {
            assertTrue(it, validarNegocio(valido.copy(telefono = it)).containsKey("phone"))
        }
    }

    @Test fun sinNegocioGuardadoElFormularioQuedaVacio() = assertEquals(NegocioFormulario(), formularioDesde(null))

    @Test fun conNegocioGuardadoSeLlenaElFormulario() = assertEquals(
        NegocioFormulario("Viejo", BusinessType.COOPERATIVA, "3110000000"),
        formularioDesde(guardado)
    )

    @Test fun tipoGuardadoDesconocidoDejaElTipoSinElegir() =
        assertEquals(null, formularioDesde(guardado.copy(tipo = "OTRO")).tipo)

    // Editar no debe borrar la configuración que se hace en otras pantallas.
    @Test fun alEditarSeConservanCapacidadBultoYFechaDeCreacion() {
        val n = negocioAGuardar("u1", valido, guardado, ahora = 99L)
        assertEquals(guardado.copy(nombre = "Café La Esperanza", tipo = "COMPRAVENTA", telefono = "3001234567"), n)
    }

    @Test fun sinNegocioPrevioSeCreaComoEnElRegistro() {
        val n = negocioAGuardar("u1", valido.copy(nombre = " Café ", telefono = " 3001234567 "), null, ahora = 99L)
        assertEquals(
            NegocioEntity(uid = "u1", nombre = "Café", tipo = "COMPRAVENTA", telefono = "3001234567", creadoEn = 99L),
            n
        )
    }
}
