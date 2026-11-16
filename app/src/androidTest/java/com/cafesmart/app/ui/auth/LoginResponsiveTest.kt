package com.cafesmart.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.CafesmartTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI aislada: dimensiones del Login real, sin cuentas ni solicitudes Firebase. */
class LoginResponsiveTest {
    @get:Rule val compose = createComposeRule()
    private val acciones = mutableListOf<String>()

    private fun montar(ancho: Int, alto: Int, fuente: Float = 1f, enviando: Boolean = false, errores: Map<String, String> = emptyMap()) {
        compose.setContent {
            var estado by remember { mutableStateOf(AuthUiState(errors = errores)) }
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, fuente)) {
                CafesmartTheme {
                    Box(Modifier.requiredSize(ancho.dp, alto.dp)) {
                        LoginScreen(
                            state = estado, onChange = { estado = it(estado) },
                            onSubmit = { acciones += "entrar" }, onRegister = { acciones += "registro" },
                            onGoogle = { acciones += "google" }, onForgotPassword = { acciones += "recuperar" },
                            onHelp = { acciones += "ayuda" }, onSupport = { acciones += "soporte" },
                            onExit = { acciones += "salir" }, isSubmitting = enviando
                        )
                    }
                }
            }
        }
    }

    @Test fun formulario390MuestraAccionesSinScrollInicial() {
        montar(390, 844)
        compose.onAllNodesWithContentDescription("Logo Café Smart").assertCountEquals(1)
        compose.onNodeWithText("Entrar").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()
        compose.onNodeWithText("Contactar soporte").assertIsDisplayed()
    }

    @Test fun formulario360MantieneBotonPrincipalVisible() {
        montar(360, 800)
        compose.onNodeWithText("Entrar").assertIsDisplayed()
        compose.onNodeWithText("Contactar soporte").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Regístrate gratis").performScrollTo().assertIsDisplayed()
    }

    @Test fun fuenteAmpliadaConErroresPermiteTodasLasAcciones() {
        val error = "El correo no tiene un formato válido. Revisa la dirección e inténtalo de nuevo."
        montar(360, 640, fuente = 1.3f, errores = mapOf("loginEmail" to error))
        compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertExists()
        for (texto in listOf("Entrar", "¿Olvidaste tu contraseña?", "Continuar con Google", "Regístrate gratis", "Ver ayuda", "Contactar soporte", "Salir")) {
            compose.onNodeWithText(texto).performScrollTo().assertIsDisplayed().performClick()
        }
        assertEquals(listOf("entrar", "recuperar", "google", "registro", "ayuda", "soporte", "salir"), acciones)
        compose.onNodeWithText("Recordar mi cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Recordar mi cuenta").assertIsOff()
    }

    @Test fun enviandoMantieneIndicadorYBloqueaOtroEnvio() {
        montar(360, 800, fuente = 1.3f, enviando = true)
        compose.onNodeWithText("Ingresando...").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    }
}
