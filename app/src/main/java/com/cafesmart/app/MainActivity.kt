
package com.cafesmart.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel

import com.cafesmart.app.ui.auth.*
import com.cafesmart.app.ui.components.*
import com.cafesmart.app.ui.home.HomeScreen
import com.cafesmart.app.ui.screens.SplashScreen
import com.cafesmart.app.ui.theme.*

// true: permite probar las pantallas sin backend.
private const val MODO_DEMOSTRACION = true

// true: simula un error en el primer registro.
private const val SIMULAR_ERROR_REGISTRO = false

// Pendiente definir el correo oficial del equipo.
private const val CORREO_SOPORTE = ""

private enum class AppScreen {
    SPLASH,
    LOGIN,
    REGISTER_SUCCESS,
    HOME
}

private enum class AuxDialog {
    RECOVERY,
    SUPPORT
}

private data class RegistrationFailure(
    val title: String,
    val message: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            CafesmartTheme {

                // PANTALLA ACTUAL
                var screen by remember {
                    mutableStateOf(AppScreen.SPLASH)
                }

                // VIEWMODEL DE AUTENTICACIÓN
                val authViewModel: AuthViewModel = viewModel()

                // RESULTADO DEL REGISTRO
                var registrationSuccess by remember {
                    mutableStateOf<RegistrationData?>(null)
                }

                // DATOS DEL ÚLTIMO INTENTO
                var pendingRegistration by remember {
                    mutableStateOf<RegistrationData?>(null)
                }

                // ERROR DEL REGISTRO
                var registrationError by remember {
                    mutableStateOf<RegistrationFailure?>(null)
                }

                // AYUDA SEGÚN LA PANTALLA ACTUAL
                var helpContext by remember {
                    mutableStateOf<AuthHelpContext?>(null)
                }

                // RECUPERACIÓN Y SOPORTE
                var activeDialog by remember {
                    mutableStateOf<AuxDialog?>(null)
                }

                // MENSAJE INFORMATIVO
                var informationMessage by remember {
                    mutableStateOf<String?>(null)
                }

                // CONFIRMACIÓN PARA SALIR DE HOME
                var showLogoutConfirmation by remember {
                    mutableStateOf(false)
                }

                // CONFIRMACIÓN PARA CERRAR LA APP
                var showExitAppConfirmation by remember {
                    mutableStateOf(false)
                }

                // ERROR SIMULADO
                var simulateErrorNext by remember {
                    mutableStateOf(SIMULAR_ERROR_REGISTRO)
                }

                // PROCESAR REGISTRO EN DEMOSTRACIÓN
                val processRegistration:
                            (RegistrationData) -> Unit = { data ->

                    pendingRegistration = data

                    when {

                        !MODO_DEMOSTRACION -> {

                            registrationError =
                                RegistrationFailure(
                                    title =
                                        "Registro no disponible",

                                    message =
                                        "El servicio de creación " +
                                                "de cuentas todavía no " +
                                                "está integrado. " +
                                                "No se ha creado ningún usuario."
                                )
                        }

                        simulateErrorNext -> {

                            simulateErrorNext = false

                            registrationError =
                                RegistrationFailure(
                                    title =
                                        "¡Ups! Hubo un problema " +
                                                "de conexión",

                                    message =
                                        "No pudimos completar " +
                                                "el registro de prueba. " +
                                                "Inténtalo nuevamente."
                                )
                        }

                        else -> {

                            registrationError = null
                            registrationSuccess = data

                            screen = AppScreen.REGISTER_SUCCESS
                        }
                    }
                }

                // PANTALLAS PRINCIPALES
                when (screen) {

                    // 1. SPLASH
                    AppScreen.SPLASH -> {

                        SplashScreen(
                            onSplashFinished = {
                                screen = AppScreen.LOGIN
                            }
                        )
                    }

                    // 2. LOGIN Y REGISTRO
                    AppScreen.LOGIN -> {

                        AuthFlow(
                            viewModel = authViewModel,

                            onLogin = { _ ->

                                if (MODO_DEMOSTRACION) {

                                    // Limpiar contraseña temporal
                                    authViewModel.reset()

                                    screen = AppScreen.HOME

                                } else {

                                    informationMessage =
                                        "El inicio de sesión real " +
                                                "está pendiente de integración."
                                }
                            },

                            onRegister = {
                                    data: RegistrationData ->

                                processRegistration(data)
                            },

                            onGoogle = {

                                informationMessage =
                                    "El inicio de sesión con Google " +
                                            "está pendiente de integración."
                            },

                            onForgotPassword = {

                                activeDialog = AuxDialog.RECOVERY
                            },

                            // SELECCIONAR AYUDA CONTEXTUAL
                            onHelp = {
                                    context: AuthHelpContext ->

                                helpContext = context
                            },

                            onSupport = {

                                activeDialog = AuxDialog.SUPPORT
                            },

                            // BOTÓN SALIR DEL LOGIN
                            onExit = {

                                showExitAppConfirmation = true
                            }
                        )
                    }

                    // 3. REGISTRO EXITOSO
                    AppScreen.REGISTER_SUCCESS -> {

                        registrationSuccess?.let { data ->

                            RegistrationSuccessScreen(
                                registration = data,
                                isDemo = MODO_DEMOSTRACION,

                                onGoHome = {

                                    registrationSuccess = null
                                    pendingRegistration = null

                                    authViewModel.reset()

                                    screen = AppScreen.HOME
                                },

                                onBackToRegister = {

                                    registrationSuccess = null

                                    // Regresa al formulario
                                    // conservando los campos.
                                    screen = AppScreen.LOGIN
                                }
                            )
                        }
                    }

                    // 4. HOME
                    AppScreen.HOME -> {

                        HomeScreen(

                            // BOTÓN SALIR DE HOME
                            onSalir = {

                                showLogoutConfirmation = true
                            }
                        )
                    }
                }

                // CONFIRMAR CIERRE DE SESIÓN EN HOME
                if (showLogoutConfirmation) {

                    AlertDialog(
                        onDismissRequest = {
                            showLogoutConfirmation = false
                        },

                        containerColor = SurfaceCard,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,

                        title = {
                            Text(
                                text = "Cerrar sesión",
                                style = AppTypography.titleModal,
                                color = TextPrimary
                            )
                        },

                        text = {
                            Text(
                                text =
                                    "¿Deseas salir de tu cuenta " +
                                            "y volver al inicio de sesión?",

                                style = AppTypography.bodyMd,
                                color = TextSecondary
                            )
                        },

                        dismissButton = {

                            TextButton(
                                onClick = {
                                    showLogoutConfirmation = false
                                }
                            ) {
                                Text(
                                    text = "Cancelar",
                                    color = Primary
                                )
                            }
                        },

                        confirmButton = {

                            Button(
                                onClick = {

                                    showLogoutConfirmation = false

                                    // Limpiar datos temporales
                                    authViewModel.reset()

                                    registrationSuccess = null
                                    pendingRegistration = null
                                    registrationError = null

                                    helpContext = null
                                    activeDialog = null
                                    informationMessage = null

                                    // REGRESAR A LOGIN
                                    screen = AppScreen.LOGIN
                                },

                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = ErrorIcon,
                                        contentColor = OnPrimary
                                    )
                            ) {
                                Text(
                                    text = "Salir",
                                    color = OnPrimary
                                )
                            }
                        }
                    )
                }

                // CONFIRMAR CIERRE DE LA APLICACIÓN
                if (showExitAppConfirmation) {

                    AlertDialog(
                        onDismissRequest = {
                            showExitAppConfirmation = false
                        },

                        containerColor = SurfaceCard,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,

                        title = {
                            Text(
                                text = "Salir de Café Smart",
                                style = AppTypography.titleModal,
                                color = TextPrimary
                            )
                        },

                        text = {
                            Text(
                                text =
                                    "¿Deseas cerrar la aplicación?",

                                style = AppTypography.bodyMd,
                                color = TextSecondary
                            )
                        },

                        dismissButton = {

                            TextButton(
                                onClick = {
                                    showExitAppConfirmation = false
                                }
                            ) {
                                Text(
                                    text = "Cancelar",
                                    color = Primary
                                )
                            }
                        },

                        confirmButton = {

                            Button(
                                onClick = {

                                    showExitAppConfirmation = false

                                    // Cierra la Activity
                                    finish()
                                },

                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = ErrorIcon,
                                        contentColor = OnPrimary
                                    )
                            ) {
                                Text(
                                    text = "Salir",
                                    color = OnPrimary
                                )
                            }
                        }
                    )
                }

                // AYUDA SEGÚN LOGIN, PASO 1 O PASO 2
                helpContext?.let { context ->

                    AuthContextHelpDialog(
                        context = context,

                        onDismiss = {
                            helpContext = null
                        },

                        onRecovery = {

                            helpContext = null
                            activeDialog = AuxDialog.RECOVERY
                        },

                        onSupport = {

                            helpContext = null
                            activeDialog = AuxDialog.SUPPORT
                        }
                    )
                }

                // RECUPERAR CONTRASEÑA
                if (activeDialog == AuxDialog.RECOVERY) {

                    RecoveryPasswordDialog(
                        onDismiss = {
                            activeDialog = null
                        },

                        onRequest = { correo: String ->

                            activeDialog = null

                            informationMessage =
                                "El correo $correo tiene un " +
                                        "formato válido.\n\n" +
                                        "El envío del enlace de recuperación " +
                                        "todavía no está integrado.\n\n" +
                                        "No se ha enviado ningún correo."
                        }
                    )
                }

                // CONTACTAR SOPORTE
                if (activeDialog == AuxDialog.SUPPORT) {

                    ContactSupportDialog(
                        onDismiss = {
                            activeDialog = null
                        },

                        onPrepareEmail = {
                                email: String,
                                description: String ->

                            activeDialog = null

                            val body = """
                                Solicitud de soporte Café Smart

                                Correo de contacto:
                                $email

                                Descripción del problema:
                                $description
                            """.trimIndent()

                            val emailIntent = Intent(
                                Intent.ACTION_SENDTO
                            ).apply {

                                data = Uri.parse(
                                    "mailto:$CORREO_SOPORTE"
                                )

                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Soporte Café Smart"
                                )

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    body
                                )
                            }

                            try {

                                startActivity(emailIntent)

                            } catch (
                                e: ActivityNotFoundException
                            ) {

                                informationMessage =
                                    "No se encontró una aplicación " +
                                            "de correo instalada."
                            }
                        }
                    )
                }

                // ERROR REUTILIZABLE DEL REGISTRO
                registrationError?.let { error ->

                    AppErrorDialog(
                        title = error.title,
                        message = error.message,

                        primaryButtonText =
                            if (MODO_DEMOSTRACION)
                                "Reintentar"
                            else
                                "Volver al formulario",

                        secondaryButtonText = "Regresar",

                        onRetry = {

                            registrationError = null

                            if (MODO_DEMOSTRACION) {

                                pendingRegistration?.let { data ->

                                    processRegistration(data)
                                }
                            }
                        },

                        onBack = {
                            registrationError = null
                        }
                    )
                }

                // AVISOS INFORMATIVOS
                informationMessage?.let { message ->

                    AlertDialog(
                        onDismissRequest = {
                            informationMessage = null
                        },

                        containerColor = SurfaceCard,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,

                        title = {
                            Text(
                                text = "Información",
                                style = AppTypography.titleModal,
                                color = TextPrimary
                            )
                        },

                        text = {
                            Text(
                                text = message,
                                style = AppTypography.bodyMd,
                                color = TextSecondary
                            )
                        },

                        confirmButton = {

                            TextButton(
                                onClick = {
                                    informationMessage = null
                                }
                            ) {
                                Text(
                                    text = "Entendido",
                                    color = Primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// COMPONENTES EXISTENTES DEL EQUIPO

@Composable
fun ComponentsShowcase(
    modifier: Modifier = Modifier
) {

    var step by remember {
        mutableStateOf(1)
    }

    var sheetVisible by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s48),

        verticalArrangement =
            Arrangement.spacedBy(Spacing.s28)
    ) {

        BackArrow(
            onClick = {
                step = (step - 2).coerceAtLeast(1)
            }
        )

        Text(
            text = "Componentes base",
            style = AppTypography.titleScreen
        )

        SegmentedProgressBar(
            total = 3,
            current = step,
            modifier = Modifier.fillMaxWidth()
        )

        ContinueButton(
            text = "Continuar",
            enabled = true,

            onClick = {
                step = (step + 1).coerceAtMost(3)
            }
        )

        ContinueButton(
            text = "Continuar",
            enabled = false,
            onClick = {}
        )

        ContinueButton(
            text = "Abrir hoja inferior",
            enabled = true,

            onClick = {
                sheetVisible = true
            }
        )
    }

    if (sheetVisible) {

        AppBottomSheet(
            onDismiss = {
                sheetVisible = false
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s20),

                verticalArrangement =
                    Arrangement.spacedBy(Spacing.s12)
            ) {

                Text(
                    text = "Hoja inferior",
                    style = AppTypography.titleModal
                )

                Text(
                    text = "Contenido de ejemplo.",
                    style = AppTypography.bodyMd
                )
            }
        }
    }
}

// VISTAS PREVIAS

@Preview(
    name = "Componentes Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun ComponentsShowcasePreview() {

    CafesmartTheme {
        ComponentsShowcase()
    }
}

@Preview(
    name = "Splash Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun SplashScreenPreview() {

    CafesmartTheme {
        SplashScreen()
    }
}

@Preview(
    name = "Home Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun HomeScreenPreview() {

    CafesmartTheme {
        HomeScreen()
    }
}
