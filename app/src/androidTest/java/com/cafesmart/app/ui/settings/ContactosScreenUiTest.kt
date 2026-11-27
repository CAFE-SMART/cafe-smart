package com.cafesmart.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.PurchaseRepository
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.contactos.ContactosViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class ProductoresFalsos : PurchaseRepository {
    var datos = listOf(Productor(id = 1, nombre = "Ana Pérez", documento = "1234567"),
        Productor(id = 2, nombre = "Luis Rojas", documento = "9876543"))
    override suspend fun listarProductores() = datos
    override suspend fun guardarProductor(productor: Productor): ResultadoProductor = error("No se usa")
    override suspend fun actualizarProductor(productor: Productor): ResultadoProductor {
        datos = datos.map { if (it.id == productor.id) productor else it }
        return ResultadoProductor.Guardado(productor)
    }
    override suspend fun registrarCompra(productorId: Long?, fecha: Fecha, cafes: List<CafeValidado>): Long = error("No se usa")
}

class ContactosScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buscaEditaYExplicaClientesNoDisponibles() {
        val repo = ProductoresFalsos()
        val vm = ContactosViewModel(repo)
        compose.setContent {
            val estado by vm.estado.collectAsState()
            CafesmartTheme { ContactosScreen(estado, vm, {}) }
        }
        compose.onNodeWithText("Ana Pérez").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("1234567")
        compose.onNodeWithText("Ana Pérez").assertIsDisplayed()
        compose.onNodeWithText("Luis Rojas").assertDoesNotExist()
        compose.onNodeWithText("Ana Pérez").performClick()
        compose.onNode(hasSetTextAction() and hasText("Ana Pérez")).performTextReplacement("Ana María Pérez")
        compose.onNodeWithText("Guardar productor").performScrollTo().performClick()
        compose.waitUntil(5000) { repo.datos.first().nombre == "Ana María Pérez" }
        assertEquals("Ana María Pérez", repo.datos.first().nombre)
        compose.onNodeWithText("Clientes").performClick()
        compose.onNodeWithText("Los clientes aún no están disponibles.").assertIsDisplayed()
    }
}
