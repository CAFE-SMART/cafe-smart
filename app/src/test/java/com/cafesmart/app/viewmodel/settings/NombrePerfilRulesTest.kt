package com.cafesmart.app.viewmodel.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NombrePerfilRulesTest {

    @Test
    fun nombreYApellidoValidos() {
        assertNull(errorDeNombrePerfil("Juan Pérez"))
        assertNull(errorDeNombrePerfil("  Ana María   Rojas Díaz "))
    }

    @Test
    fun vacioOSoloEspaciosPideElNombre() {
        assertEquals(ERROR_NOMBRE_VACIO, errorDeNombrePerfil(""))
        assertEquals(ERROR_NOMBRE_VACIO, errorDeNombrePerfil("    "))
    }

    @Test
    fun unaSolaPalabraPideLosApellidos() {
        assertEquals(ERROR_APELLIDOS_VACIOS, errorDeNombrePerfil("Juan"))
        assertEquals(ERROR_APELLIDOS_VACIOS, errorDeNombrePerfil("  Juan  "))
    }

    @Test
    fun conMasDe70CaracteresMuestraErrorDeLargo() {
        val largo = "Juan " + "P".repeat(NOMBRE_PERFIL_MAX)
        assertEquals(ERROR_NOMBRE_LARGO, errorDeNombrePerfil(largo))
        val justo = "Juan " + "P".repeat(NOMBRE_PERFIL_MAX - 5)
        assertNull(errorDeNombrePerfil(justo))
    }

    @Test
    fun normalizarQuitaEspaciosSobrantes() {
        assertEquals("Juan Pérez", normalizarNombre("  Juan    Pérez "))
        assertEquals("", normalizarNombre("   "))
    }

    @Test
    fun inicialesSonLasDeLasDosPrimerasPalabras() {
        assertEquals("JP", inicialesDe("Juan Pérez"))
        assertEquals("AM", inicialesDe("ana maría rojas"))
        assertEquals("J", inicialesDe("Juan"))
    }

    @Test
    fun sinNombreNoHayIniciales() {
        assertNull(inicialesDe(null))
        assertNull(inicialesDe("   "))
    }
}
