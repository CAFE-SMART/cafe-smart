package com.cafesmart.app.data.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SesionInicialTest {

    @Test
    fun conSesionYRecordarEntraDirectoAInicio() {
        assertEquals(
            DecisionInicial(DestinoInicial.INICIO, cerrarSesion = false),
            decidirInicio(hayUsuario = true, recordar = true)
        )
    }

    @Test
    fun conSesionSinRecordarLaCierraYPideElLogin() {
        assertEquals(
            DecisionInicial(DestinoInicial.LOGIN, cerrarSesion = true),
            decidirInicio(hayUsuario = true, recordar = false)
        )
    }

    @Test
    fun sinSesionVaAlLoginAunqueRecordarEsteActivo() {
        assertEquals(
            DecisionInicial(DestinoInicial.LOGIN, cerrarSesion = false),
            decidirInicio(hayUsuario = false, recordar = true)
        )
        assertEquals(
            DecisionInicial(DestinoInicial.LOGIN, cerrarSesion = false),
            decidirInicio(hayUsuario = false, recordar = false)
        )
    }
}
