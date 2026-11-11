package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.viewmodel.purchase.NOMBRE_MAX
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.purchase.ayudaNombre
import com.cafesmart.app.viewmodel.purchase.limpiarNombre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Topes de escritura y caracteres permitidos en los nombres (productor de Compras y registro de la cuenta).
class LimitesDeCamposTest {

    @Test fun unaPersonaSoloLlevaLetrasYEspacios() {
        assertEquals("Juan Perez", limpiarNombre("Juan123 Perez!!@"))
        assertEquals("María José", limpiarNombre("María José"))      // tildes sí
        assertEquals("Juan Perez", limpiarNombre("Juan Perez", TipoDocumento.CC))
    }

    @Test fun unaEmpresaPuedeLlevarNumerosPuntoYAmpersand() {
        assertEquals("Café 2000 S.A.S.", limpiarNombre("Café 2000 S.A.S.", TipoDocumento.NIT))
        assertEquals("Hermanos López & Cía", limpiarNombre("Hermanos López & Cía", TipoDocumento.NIT))
        // Otros signos siguen fuera.
        assertEquals("Café SAS", limpiarNombre("Café #SAS!", TipoDocumento.NIT))
    }

    @Test fun nuncaPasaDeTreintaCaracteres() {
        assertEquals(NOMBRE_MAX, limpiarNombre("A".repeat(80)).length)
        assertEquals(NOMBRE_MAX, limpiarNombre("1".repeat(80), TipoDocumento.NIT).length)
    }

    @Test fun laAyudaDiceElMaximoYLoQueLlevaEscrito() {
        assertEquals("Solo letras. 10/30", ayudaNombre("Juan Perez", TipoDocumento.CC))
        assertEquals("Letras, números, punto y &. 16/30", ayudaNombre("Café 2000 S.A.S.", TipoDocumento.NIT))
    }

    @Test fun alPasarDeNitACedulaElNombreSeLimpia() {
        val vm = PurchaseViewModel()
        vm.cambiarTipoDocumento(TipoDocumento.NIT)
        vm.cambiarNombre("Café 2000 S.A.S.")
        assertEquals("Café 2000 S.A.S.", vm.uiState.value.nuevoProductor.nombre)
        vm.cambiarTipoDocumento(TipoDocumento.CC)
        val nombre = vm.uiState.value.nuevoProductor.nombre
        assertTrue(nombre.all { it.isLetter() || it == ' ' })
        // Sin espacios dobles donde estaban los números y puntos.
        assertEquals("Café SAS", nombre)
    }
}
