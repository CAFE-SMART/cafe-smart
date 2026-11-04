package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.session.UsuarioSesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRulesTest {

    @Test
    fun usuarioCompletoMuestraNombreYTipoDeNegocio() {
        val p = construirPerfil(UsuarioSesion("Andrés Peña", "andres@correo.com", "Compraventa"))
        assertEquals(PerfilUsuario("Andrés Peña", "Compraventa"), p)
    }

    @Test
    fun sinSesionUsaNombrePorDefectoYSinTipo() {
        val p = construirPerfil(null)
        assertEquals("Usuario", p.nombre)
        assertNull(p.tipoNegocio)
    }

    @Test
    fun nombreNuloUsaLaParteDelCorreoAntesDelArroba() {
        assertEquals("andres.pena", construirPerfil(UsuarioSesion(null, "andres.pena@correo.com")).nombre)
    }

    @Test
    fun nombreVacioUsaElCorreo() {
        assertEquals("katherine", construirPerfil(UsuarioSesion("", "katherine@correo.com")).nombre)
    }

    @Test
    fun nombreSoloEspaciosUsaElCorreo() {
        assertEquals("amelia", construirPerfil(UsuarioSesion("   ", "amelia@correo.com")).nombre)
    }

    @Test
    fun sinNombreNiCorreoUsaNombrePorDefecto() {
        assertEquals(NOMBRE_POR_DEFECTO, construirPerfil(UsuarioSesion(null, null)).nombre)
    }

    @Test
    fun correoVacioYNombreVacioUsaNombrePorDefecto() {
        assertEquals(NOMBRE_POR_DEFECTO, construirPerfil(UsuarioSesion("", "  ")).nombre)
    }

    @Test
    fun correoQueEmpiezaPorArrobaUsaNombrePorDefecto() {
        assertEquals(NOMBRE_POR_DEFECTO, construirPerfil(UsuarioSesion(null, "@correo.com")).nombre)
    }

    @Test
    fun correoSinArrobaSeUsaCompleto() {
        assertEquals("sincorreo", construirPerfil(UsuarioSesion(null, "sincorreo")).nombre)
    }

    @Test
    fun seQuitanLosEspaciosDelNombre() {
        assertEquals("Carlos Arango", construirPerfil(UsuarioSesion("  Carlos Arango  ", null)).nombre)
    }

    @Test
    fun elNombreTienePrioridadSobreElCorreo() {
        assertEquals("Carlos", construirPerfil(UsuarioSesion("Carlos", "otro@correo.com")).nombre)
    }

    @Test
    fun tipoDeNegocioNuloNoSeMuestra() {
        assertNull(construirPerfil(UsuarioSesion("Ana", "ana@correo.com", null)).tipoNegocio)
    }

    @Test
    fun tipoDeNegocioVacioNoSeMuestra() {
        assertNull(construirPerfil(UsuarioSesion("Ana", "ana@correo.com", "   ")).tipoNegocio)
    }

    @Test
    fun seQuitanLosEspaciosDelTipoDeNegocio() {
        assertEquals("Cooperativa", construirPerfil(UsuarioSesion("Ana", null, "  Cooperativa ")).tipoNegocio)
    }

    @Test
    fun nombrePorDefectoEsUsuario() {
        assertEquals("Usuario", NOMBRE_POR_DEFECTO)
    }
}
