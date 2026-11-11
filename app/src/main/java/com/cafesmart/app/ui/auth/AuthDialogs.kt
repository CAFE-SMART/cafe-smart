package com.cafesmart.app.ui.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cafesmart.app.ui.components.AppErrorDialog
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary

// true: permite probar las pantallas sin backend (sin Firebase).
// false: login y registro reales con Firebase Authentication.
const val MODO_DEMOSTRACION = false

// true: simula un error en el primer registro.
const val SIMULAR_ERROR_REGISTRO = false

// TODO: definir el correo oficial de soporte del equipo.
const val CORREO_SOPORTE = ""

enum class AuthAuxDialog {
    SUPPORT
}

data class RegistrationFailure(
    val title: String,
    val message: String
)

/** Estado de los diálogos que se abren desde las pantallas de autenticación. */
private const val TITULO_INFO = "Información"

class AuthDialogsState {
    var help by mutableStateOf<AuthHelpContext?>(null)
    var active by mutableStateOf<AuthAuxDialog?>(null)
    var info by mutableStateOf<String?>(null)
    // Título del aviso; vuelve a "Información" al cerrarlo.
    var infoTitle by mutableStateOf(TITULO_INFO)
    var registrationError by mutableStateOf<RegistrationFailure?>(null)
    // Error de Firebase (login o registro): ver authErrorDialogData.
    var authError by mutableStateOf<AuthErrorDialogData?>(null)

    fun clear() {
        help = null
        active = null
        info = null
        infoTitle = TITULO_INFO
        registrationError = null
        authError = null
    }

    fun cerrarInfo() {
        info = null
        infoTitle = TITULO_INFO
    }
}

/** Dibuja los diálogos de ayuda, recuperación, soporte, error de registro y avisos. */
@Composable
fun AuthDialogs(
    state: AuthDialogsState,
    onRetryRegistration: () -> Unit,
    // Ejecuta lo que pide un botón del diálogo de error (reintentar, ir al login, etc.).
    onAuthErrorAction: (AuthErrorAction, AuthOrigin) -> Unit,
    // Abre la pantalla "Recuperar contraseña" (antes era un diálogo).
    onRecovery: () -> Unit
) {
    val context = LocalContext.current

    state.help?.let { helpContext ->
        AuthContextHelpDialog(
            context = helpContext,
            onDismiss = { state.help = null },
            onRecovery = {
                state.help = null
                onRecovery()
            },
            onSupport = {
                state.help = null
                state.active = AuthAuxDialog.SUPPORT
            }
        )
    }

    if (state.active == AuthAuxDialog.SUPPORT) {
        ContactSupportDialog(
            onDismiss = { state.active = null },
            onPrepareEmail = { email: String, description: String ->
                state.active = null

                val body = """
                    Solicitud de soporte Café Smart

                    Correo de contacto:
                    $email

                    Descripción del problema:
                    $description
                """.trimIndent()

                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$CORREO_SOPORTE")
                    putExtra(Intent.EXTRA_SUBJECT, "Soporte Café Smart")
                    putExtra(Intent.EXTRA_TEXT, body)
                }

                try {
                    context.startActivity(emailIntent)
                } catch (e: ActivityNotFoundException) {
                    state.info = "No se encontró una aplicación de correo instalada."
                }
            }
        )
    }

    state.authError?.let { datos ->
        AppErrorDialog(
            title = datos.title,
            message = datos.message,
            icon = if (datos.sinConexion) AppIcons.WifiOff else null,
            primaryButtonText = datos.primaryText,
            secondaryButtonText = datos.secondaryText,
            onRetry = {
                state.authError = null
                onAuthErrorAction(datos.primaryAction, datos.origin)
            },
            onBack = {
                state.authError = null
                onAuthErrorAction(datos.secondaryAction, datos.origin)
            },
            // La "x" y tocar fuera solo cierran: nunca navegan.
            onDismiss = { state.authError = null }
        )
    }

    state.registrationError?.let { error ->
        AppErrorDialog(
            title = error.title,
            message = error.message,
            primaryButtonText = if (MODO_DEMOSTRACION) "Reintentar" else "Volver al formulario",
            secondaryButtonText = "Regresar",
            onRetry = {
                state.registrationError = null
                if (MODO_DEMOSTRACION) onRetryRegistration()
            },
            onBack = { state.registrationError = null }
        )
    }

    state.info?.let { message ->
        AlertDialog(
            onDismissRequest = { state.cerrarInfo() },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(text = state.infoTitle, style = AppTypography.titleModal, color = TextPrimary)
            },
            text = {
                Text(text = message, style = AppTypography.bodyMd, color = TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = { state.cerrarInfo() }) {
                    Text(text = "Entendido", color = Primary)
                }
            }
        )
    }
}

/** Confirmación con botón destructivo (cerrar sesión, salir de la app). */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmText: String = "Salir",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(text = title, style = AppTypography.titleModal, color = TextPrimary)
        },
        text = {
            Text(text = message, style = AppTypography.bodyMd, color = TextSecondary)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = Primary)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorIcon,
                    contentColor = OnPrimary
                )
            ) {
                Text(text = confirmText, color = OnPrimary)
            }
        }
    )
}
