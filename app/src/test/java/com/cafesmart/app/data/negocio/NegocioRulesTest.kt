package com.cafesmart.app.data.negocio

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.ui.auth.BusinessType
import org.junit.Assert.assertEquals
import org.junit.Test

class NegocioRulesTest {

    @Test
    fun armaElNegocioConElUidYLosDatosDelRegistro() {
        val n = negocioDesdeRegistro("u1", "Café La Esperanza", BusinessType.COOPERATIVA.name, "3001234567", 1_000L)
        assertEquals(
            NegocioEntity(uid = "u1", nombre = "Café La Esperanza", tipo = "COOPERATIVA", telefono = "3001234567", creadoEn = 1_000L),
            n
        )
    }

    @Test
    fun quitaLosEspaciosDelNombreYDelTelefono() {
        val n = negocioDesdeRegistro("u1", "  Café La Esperanza ", "COMPRAVENTA", " 3001234567 ", 0L)
        assertEquals("Café La Esperanza", n.nombre)
        assertEquals("3001234567", n.telefono)
    }

    // Se guarda el nombre del enum, no la etiqueta: cambiar el texto visible no rompe lo guardado.
    @Test
    fun elTipoSeGuardaComoNombreDelEnum() {
        BusinessType.entries.forEach {
            assertEquals(it.name, negocioDesdeRegistro("u1", "X", it.name, "1", 0L).tipo)
        }
    }

    // Capacidad 0 = bodega sin configurar; el bulto empieza en 40,7 kg (se configurarán en Ajustes).
    @Test
    fun alRegistrarseLaBodegaQuedaSinConfigurarYElBultoEn40Coma7() {
        val n = negocioDesdeRegistro("u1", "X", "COMPRAVENTA", "1", 0L)
        assertEquals(0.0, n.capacidadBodegaKg, 0.0)
        assertEquals(40.7, n.pesoBultoKg, 0.0)
    }
}
