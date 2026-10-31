
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.cafesmart.app.ui.theme.CafesmartTheme

import kotlinx.coroutines.launch

@Composable
fun AuthDemoScreen() {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    fun mostrarMensaje(mensaje: String) {
        scope.launch {
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AuthFlow(
            onLogin = {
                mostrarMensaje(
                    "Inicio de sesión de prueba."
                )
            },

            onRegister = {
                mostrarMensaje(
                    "Formulario validado. Registro de prueba."
                )
            },

            onGoogle = {
                mostrarMensaje(
                    "Google pendiente de integración."
                )
            },

            onForgotPassword = {
                mostrarMensaje(
                    "Abre la aplicación para probar recuperación."
                )
            },

            onHelp = {
                mostrarMensaje(
                    "Abre la aplicación para probar ayuda."
                )
            },

            onSupport = {
                mostrarMensaje(
                    "Abre la aplicación para probar soporte."
                )
            },

            onExit = {
                mostrarMensaje(
                    "Salir del modo de prueba."
                )
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,

            modifier = Modifier.align(
                Alignment.BottomCenter
            )
        )
    }
}

@Preview(
    name = "Autenticación Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartAuthenticationPreview() {

    CafesmartTheme {
        AuthDemoScreen()
    }
}
