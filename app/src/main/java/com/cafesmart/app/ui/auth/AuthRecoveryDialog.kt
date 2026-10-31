
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.cafesmart.app.ui.theme.*

private val recoveryEmailRegex =
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun RecoveryPasswordDialog(
    onDismiss: () -> Unit,
    onRequest: (String) -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,

        shape = RoundedCornerShape(20.dp),

        title = {
            Text(
                text = "Recuperar contraseña",
                color = TextPrimary,
                style = AppTypography.titleModal
            )
        },

        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "Ingresa el correo electrónico " +
                            "con el que registraste tu cuenta.",

                    style = AppTypography.bodyMd,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = email,

                    onValueChange = {
                        email = it
                        error = null
                    },

                    label = {
                        Text("Correo electrónico")
                    },

                    placeholder = {
                        Text(
                            text = "ejemplo@correo.com",
                            color = TextMuted
                        )
                    },

                    singleLine = true,
                    isError = error != null,

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),

                    modifier = Modifier.fillMaxWidth(),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceScreen,
                        unfocusedContainerColor = SurfaceScreen,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderDefault,
                        errorBorderColor = ErrorText,
                        cursorColor = Primary
                    )
                )

                error?.let { message: String ->

                    Text(
                        text = message,
                        color = ErrorText,
                        style = AppTypography.helper
                    )
                }

                Text(
                    text = "El enlace de recuperación " +
                            "podrá enviarse cuando se conecte " +
                            "el servicio de autenticación.",

                    style = AppTypography.helper,
                    color = TextMuted
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss,

                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )
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

                    val correo: String = email.trim()

                    if (!recoveryEmailRegex.matches(correo)) {

                        error = "Ingresa un correo válido."

                    } else {

                        onRequest(correo)
                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                )
            ) {
                Text(
                    text = "Continuar",
                    color = OnPrimary
                )
            }
        }
    )
}
