package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.session.UsuarioSesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRulesTest {

    private fun negocio(tipo: String = "COMPRAVENTA", nombre: String = "Café La Esperanza") =
        NegocioEntity(uid = "u1", nombre = nombre, tipo = tipo, telefono = "3001234567", creadoEn = 0L)

    @Test
    fun cuentaYNegocioCompletosLlenanTodoElPerfil() {
        val p = construirPerfil(UsuarioSesion("u1", "Andrés Peña", "andres@correo.com"), negocio())
        assertEquals(PerfilUsuario("Andrés Peña", "andres@correo.com", "Compraventa", "Café La Esperanza"), p)
    }

    @Test
    fun sinSesionTodoQuedaVacio() {
        assertEquals(PerfilUsuario(), construirPerfil(null))
    }

    // El caso del usuario de prueba creado en la consola de Firebase: sin displayName.
    @Test
    fun sinNombreNoSeInventaConElCorreoYElCorreoSeConserva() {
        val p = construirPerfil(UsuarioSesion("u1", null, "usuarioprueba@correo.com"))
        assertNull(p.nombre)
        assertEquals("usuarioprueba@correo.com", p.correo)
    }

    @Test
    fun nombreVacioOSoloEspaciosEsNull() {
        assertNull(construirPerfil(UsuarioSesion("u1", "", "a@correo.com")).nombre)
        assertNull(construirPerfil(UsuarioSesion("u1", "   ", "a@correo.com")).nombre)
    }

    @Test
    fun correoVacioEsNull() {
        assertNull(construirPerfil(UsuarioSesion("u1", "Ana", "  ")).correo)
        assertNull(construirPerfil(UsuarioSesion("u1", "Ana", null)).correo)
    }

    @Test
    fun seQuitanLosEspaciosDelNombreYDelCorreo() {
        val p = construirPerfil(UsuarioSesion("u1", "  Carlos Arango  ", " carlos@correo.com "))
        assertEquals("Carlos Arango", p.nombre)
        assertEquals("carlos@correo.com", p.correo)
    }

    @Test
    fun sinNegocioNoHayTipoNiNombreDeNegocio() {
        val p = construirPerfil(UsuarioSesion("u1", "Ana", "ana@correo.com"), null)
        assertNull(p.tipoNegocio)
        assertNull(p.nombreNegocio)
    }

    @Test
    fun cadaTipoGuardadoMuestraSuEtiqueta() {
        assertEquals("Compraventa", etiquetaTipoNegocio("COMPRAVENTA"))
        assertEquals("Cooperativa", etiquetaTipoNegocio("COOPERATIVA"))
        assertEquals("Personalizado", etiquetaTipoNegocio("PERSONALIZADO"))
    }

    @Test
    fun tipoDesconocidoOVacioNoMuestraEtiqueta() {
        assertNull(etiquetaTipoNegocio("OTRO"))
        assertNull(etiquetaTipoNegocio(""))
        assertNull(construirPerfil(UsuarioSesion("u1", "Ana", null), negocio(tipo = "compraventa")).tipoNegocio)
    }

    @Test
    fun nombreDeNegocioVacioEsNull() {
        assertNull(construirPerfil(UsuarioSesion("u1", "Ana", null), negocio(nombre = "  ")).nombreNegocio)
    }

    @Test
    fun textoCuandoFaltaElNombre() {
        assertEquals("Sin nombre", NOMBRE_FALTANTE)
    }
}
