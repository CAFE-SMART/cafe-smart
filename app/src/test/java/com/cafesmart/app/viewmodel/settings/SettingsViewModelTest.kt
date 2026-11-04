package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.session.SessionGateway
import com.cafesmart.app.data.session.UsuarioSesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Versión falsa de la sesión: permite probar sin Firebase y contar los cierres de sesión.
private class SesionFalsa(var usuario: UsuarioSesion?) : SessionGateway {
    var cierres = 0
    override fun usuarioActual(): UsuarioSesion? = usuario
    override fun cerrarSesion() {
        cierres++
        usuario = null
    }
}

class SettingsViewModelTest {

    private val andres = UsuarioSesion("Andrés Peña", "andres@correo.com", "Compraventa")

    @Test
    fun alEmpezarMuestraElPerfilDelUsuarioSinConfirmacion() {
        val vm = SettingsViewModel(SesionFalsa(andres))
        assertEquals(PerfilUsuario("Andrés Peña", "Compraventa"), vm.uiState.value.perfil)
        assertFalse(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun sinSesionMuestraElPerfilPorDefecto() {
        val vm = SettingsViewModel(SesionFalsa(null))
        assertEquals("Usuario", vm.uiState.value.perfil.nombre)
        assertNull(vm.uiState.value.perfil.tipoNegocio)
    }

    @Test
    fun pedirCerrarSesionAbreLaConfirmacionSinCerrarLaSesion() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion)
        vm.pedirCerrarSesion()
        assertTrue(vm.uiState.value.mostrarConfirmacion)
        assertEquals(0, sesion.cierres)
    }

    @Test
    fun cancelarCierraLaConfirmacionYNoCierraLaSesion() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion)
        vm.pedirCerrarSesion()
        vm.cancelarCierre()
        assertFalse(vm.uiState.value.mostrarConfirmacion)
        assertEquals(0, sesion.cierres)
        assertEquals("Andrés Peña", vm.uiState.value.perfil.nombre)
    }

    @Test
    fun confirmarCierraLaSesionUnaVezYOcultaLaConfirmacion() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion)
        vm.pedirCerrarSesion()
        vm.confirmarCierre()
        assertEquals(1, sesion.cierres)
        assertFalse(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun sePuedePedirCancelarYVolverAPedir() {
        val vm = SettingsViewModel(SesionFalsa(andres))
        vm.pedirCerrarSesion()
        vm.cancelarCierre()
        vm.pedirCerrarSesion()
        assertTrue(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun actualizarPerfilVuelveALeerElUsuario() {
        val sesion = SesionFalsa(UsuarioSesion(null, "ana@correo.com"))
        val vm = SettingsViewModel(sesion)
        assertEquals("ana", vm.uiState.value.perfil.nombre)
        sesion.usuario = UsuarioSesion("Ana López", "ana@correo.com", "Cooperativa")
        vm.actualizarPerfil()
        assertEquals(PerfilUsuario("Ana López", "Cooperativa"), vm.uiState.value.perfil)
    }

    @Test
    fun actualizarPerfilNoCierraLaConfirmacionAbierta() {
        val vm = SettingsViewModel(SesionFalsa(andres))
        vm.pedirCerrarSesion()
        vm.actualizarPerfil()
        assertTrue(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun elEstadoInicialPorDefectoNoTieneConfirmacion() {
        val estado = SettingsUiState()
        assertFalse(estado.mostrarConfirmacion)
        assertEquals("Usuario", estado.perfil.nombre)
    }
}
