package com.cafesmart.app.viewmodel.contactos

import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.purchase.RepositorioDeComprasFalso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Contactos registrados (ciclo 3, orden 16): buscar y editar productores con el repositorio de Compras (sin pantalla).
class ContactosTest {
    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val ana = Productor(nombre = "José Pérez Ruiz", documento = "1234567", telefono = "3001234567")
    private val finca = Productor(nombre = "Finca El Mirador", documento = "900123456", tipoDocumento = TipoDocumento.NIT)
    private val luz = Productor(nombre = "Luz Marina Valencia", documento = "66789012")
    private val repo = RepositorioDeComprasFalso(listOf(ana, finca, luz))
    private val lista get() = repo.productores.toList()

    @Test fun buscaPorNombreSinTildesNiMayusculas() {
        assertEquals(listOf("José Pérez Ruiz"), buscarContactos(lista, "  jose   PEREZ ").map { it.nombre })
        assertEquals(listOf("Finca El Mirador"), buscarContactos(lista, "mirador").map { it.nombre })
        assertEquals(3, buscarContactos(lista, "").size)
        assertTrue(buscarContactos(lista, "nadie").isEmpty())
    }

    @Test fun buscaPorDocumentoConOSinPuntos() {
        assertEquals(listOf("Finca El Mirador"), buscarContactos(lista, "900.123").map { it.nombre })
        assertEquals(listOf("Luz Marina Valencia"), buscarContactos(lista, "6678").map { it.nombre })
        // Un texto con letras no se toma como documento.
        assertTrue(buscarContactos(lista, "12a").isEmpty())
    }

    @Test fun normalizaElTexto() = assertEquals("jose perez", normalizarBusqueda("  JOSÉ   Pérez "))

    @Test fun suPropioDocumentoNoEsRepetidoPeroElDeOtroSi() {
        val id = lista[0].id
        assertNull(validarContacto(id, NuevoProductor(TipoDocumento.CC, "1234567", "José Pérez Ruiz", ""), lista).documento)
        assertEquals(MENSAJE_DOCUMENTO_REPETIDO,
            validarContacto(id, NuevoProductor(TipoDocumento.CC, "66789012", "José Pérez Ruiz", ""), lista).documento)
    }

    @Test fun camposInvalidosConErrorPorCampo() {
        val e = validarContacto(lista[0].id, NuevoProductor(TipoDocumento.CC, "123", "José", "123"), lista)
        assertEquals("Escribe tu nombre y apellido.", e.nombre)
        assertEquals("La cédula debe tener entre 6 y 10 números.", e.documento)
        assertEquals("Recuerda, tu celular debe tener 10 números y empezar por el 3.", e.telefono)
        assertTrue(e.hayErrores)
    }

    @Test fun editarGuardaEnElRepositorioYConservaElId() {
        val vm = ContactosViewModel(repo)
        val original = vm.estado.value.productores[0]
        vm.editar(original)
        vm.cambiarNombre("José Pérez Gómez"); vm.cambiarTelefono("3109876543")
        vm.guardar()
        val guardado = repo.productores.first { it.id == original.id }
        assertEquals("José Pérez Gómez", guardado.nombre)
        assertEquals("3109876543", guardado.telefono)
        assertNull(vm.estado.value.enEdicion)
        assertEquals("José Pérez Gómez", vm.estado.value.productores.first { it.id == original.id }.nombre)
    }

    @Test fun conErroresNoGuarda() {
        val vm = ContactosViewModel(repo)
        val original = vm.estado.value.productores[0]
        vm.editar(original); vm.cambiarDocumento("66789012") // documento de Luz
        vm.guardar()
        assertEquals(MENSAJE_DOCUMENTO_REPETIDO, vm.estado.value.errores.documento)
        assertEquals(original, repo.productores.first { it.id == original.id })
    }

    @Test fun losClientesTodaviaNoEstanDisponibles() {
        val vm = ContactosViewModel(repo)
        vm.elegirTipo(TipoContacto.CLIENTES)
        assertFalse(vm.estado.value.clientesDisponibles)
        assertTrue(vm.estado.value.visibles.isEmpty())
    }

    @Test fun agregarProductorGuardaYLoIncluyeEnLaLista() {
        val vm = ContactosViewModel(repo)
        vm.agregarProductor()
        vm.cambiarNombre("Ana María Gómez")
        vm.cambiarDocumento("9876543")
        vm.cambiarTelefono("3109876543")
        vm.guardar()
        val guardado = repo.productores.single { it.documento == "9876543" }
        assertEquals("Ana María Gómez", guardado.nombre)
        assertTrue(vm.estado.value.productores.any { it.id == guardado.id })
        assertFalse(vm.estado.value.creando)
    }

    @Test fun agregarConDocumentoExistenteNoCreaOtroProductor() {
        val vm = ContactosViewModel(repo)
        val cantidad = repo.productores.size
        vm.agregarProductor()
        vm.cambiarNombre("Ana María Gómez")
        vm.cambiarDocumento("66789012")
        vm.guardar()
        assertEquals(cantidad, repo.productores.size)
        assertEquals(MENSAJE_DOCUMENTO_REPETIDO, vm.estado.value.errores.documento)
        assertTrue(vm.estado.value.creando)
    }
}
