package com.cafesmart.app.ui.components

import android.view.View
import android.widget.DatePicker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.ui.theme.CafesmartTheme
import org.hamcrest.Matcher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Contrato del campo común: sin modelos de gastos, compras ni reglas de negocio. */
class AppDatePickerFieldTest {
    @get:Rule val compose = createComposeRule()
    private val fecha = mutableStateOf(Fecha(5, 11, 2026))
    private val revision = mutableIntStateOf(0)
    private val visible = mutableStateOf(true)
    private val enabled = mutableStateOf(true)
    private var avisos = 0
    private var versionNotificada = -1

    private fun montar(error: String? = null) {
        compose.setContent {
            val version = revision.intValue
            CafesmartTheme {
                Box(Modifier.fillMaxWidth().padding(20.dp)) {
                    if (visible.value) AppDatePickerField(
                        label = "Fecha de operación", fecha = fecha.value,
                        onFechaChange = { fecha.value = it; avisos++; versionNotificada = version },
                        formato = { String.format(Locale.ROOT, "%02d/%02d/%04d", it.dia, it.mes, it.anio) },
                        enabled = enabled.value, error = error
                    )
                }
            }
        }
    }

    private fun abrir() { compose.onNodeWithContentDescription("Seleccionar fecha").performClick() }
    private fun elegir(dia: Int) {
        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Elegir día $dia de noviembre de 2026"
            override fun perform(uiController: UiController, view: View) {
                (view as DatePicker).updateDate(2026, 10, dia)
                uiController.loopMainThreadUntilIdle()
            }
        })
    }

    @Test fun C01_fechaInicialYConfirmacion() {
        montar(); abrir()
        onView(isAssignableFrom(DatePicker::class.java)).check { vista, error ->
            if (error != null) throw error
            val picker = vista as DatePicker
            assertEquals(2026, picker.year); assertEquals(10, picker.month); assertEquals(5, picker.dayOfMonth)
        }
        elegir(6); onView(withId(android.R.id.button1)).perform(click())
        compose.onNodeWithText("06/11/2026").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Fecha(6, 11, 2026), fecha.value); assertEquals(1, avisos) }
    }

    @Test fun C02_cancelarNoNotificaNiModifica() {
        montar(); abrir(); elegir(9); onView(withId(android.R.id.button2)).perform(click())
        compose.onNodeWithText("05/11/2026").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Fecha(5, 11, 2026), fecha.value); assertEquals(0, avisos) }
    }

    @Test fun C03_recomposicionConDialogoAbierto() {
        montar(); abrir(); elegir(7)
        compose.runOnIdle { revision.intValue++ }
        // Si se duplican ventanas, Espresso falla por coincidencias ambiguas; la selección no se reinicia.
        onView(isAssignableFrom(DatePicker::class.java)).check { vista, error ->
            if (error != null) throw error
            assertEquals(7, (vista as DatePicker).dayOfMonth)
        }
        onView(withId(android.R.id.button1)).perform(click())
        compose.runOnIdle { assertEquals(1, avisos); assertEquals(1, versionNotificada) }
    }

    @Test fun C04_deshabilitadoYMensajeAccesible() {
        enabled.value = false; montar("Fecha no permitida por el formulario")
        val campo = compose.onNodeWithContentDescription("Seleccionar fecha")
        campo.assertIsNotEnabled()
        campo.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        campo.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "05/11/2026"))
        campo.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Fecha no permitida por el formulario"))
        compose.onNodeWithText("Fecha no permitida por el formulario").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, avisos) }
    }

    @Test fun C05_retirarCampoCierraVentana() {
        montar(); abrir(); compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        onView(withId(android.R.id.button1)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
        compose.runOnIdle { assertEquals(0, avisos) }
    }

    @Test fun C06_areaTactilYAperturasSucesivas() {
        montar()
        compose.onNodeWithContentDescription("Seleccionar fecha").assertHeightIsAtLeast(48.dp)
        repeat(2) { abrir(); onView(isAssignableFrom(DatePicker::class.java)).check(matches(isDisplayed())); onView(withId(android.R.id.button2)).perform(click()) }
        compose.runOnIdle { assertEquals(0, avisos); assertEquals(Fecha(5, 11, 2026), fecha.value) }
    }
}
