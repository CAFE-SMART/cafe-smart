
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.cafesmart.app.ui.theme.*

private val supportEmailRegex =
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

// VENTANA DE AYUDA
@Composable
fun LoginHelpDialog(
    onDismiss: () -> Unit,
    onRecovery: () -> Unit,
    onSupport: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,

        shape = RoundedCornerShape(20.dp),

        title = {
            Text(
                text = "Ayuda para ingresar",
                color = TextPrimary,
                style = AppTypography.titleModal
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "¿Cómo iniciar sesión?",
                    color = TextPrimary,
                    style = AppTypography.labelField
                )

                Text(
                    text =
                        "1. Ingresa tu correo registrado.\n" +
                                "2. Escribe tu contraseña.\n" +
                                "3. Presiona el botón Entrar.",

                    color = TextSecondary,
                    style = AppTypography.bodyMd
                )

                HorizontalDivider(
                    color = BorderDefault
                )

                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = TextPrimary,
                    style = AppTypography.labelField
                )

                OutlinedButton(
                    onClick = onRecovery,

                    modifier = Modifier.fillMaxWidth(),

                    border = BorderStroke(
                        width = 1.dp,
                        color = Primary
                    ),

                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = Primary
                        )
                ) {
                    Text(
                        text = "Recuperar contraseña",
                        color = Primary
                    )
                }

                HorizontalDivider(
                    color = BorderDefault
                )

                Text(
                    text = "¿Sigues teniendo problemas?",
                    color = TextPrimary,
                    style = AppTypography.labelField
                )

                OutlinedButton(
                    onClick = onSupport,

                    modifier = Modifier.fillMaxWidth(),

                    border = BorderStroke(
                        width = 1.dp,
                        color = Primary
                    ),

                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = Primary
                        )
                ) {
                    Text(
                        text = "Contactar soporte",
                        color = Primary
                    )
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss,

                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )
            ) {
                Text(
                    text = "Cerrar",
                    color = Primary
                )
            }
        }
    )
}

// VENTANA DE CONTACTAR SOPORTE
@Composable
fun ContactSupportDialog(
    onDismiss: () -> Unit,
    onPrepareEmail: (String, String) -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var description by rememberSaveable {
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
                text = "Contactar soporte",
                color = TextPrimary,
                style = AppTypography.titleModal
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),

                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "Cuéntanos qué problema tienes " +
                            "con Café Smart.",

                    color = TextSecondary,
                    style = AppTypography.bodyMd
                )

                OutlinedTextField(
                    value = email,

                    onValueChange = {
                        email = it
                        error = null
                    },

                    label = {
                        Text("Tu correo electrónico")
                    },

                    placeholder = {
                        Text(
                            text = "ejemplo@correo.com",
                            color = TextMuted
                        )
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),

                    singleLine = true,
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
                        cursorColor = Primary
                    )
                )

                OutlinedTextField(
                    value = description,

                    onValueChange = {
                        description = it
                        error = null
                    },

                    label = {
                        Text("Describe tu problema")
                    },

                    placeholder = {
                        Text(
                            text = "No puedo ingresar a mi cuenta...",
                            color = TextMuted
                        )
                    },

                    minLines = 3,
                    maxLines = 5,
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
                    text = "Se preparará un correo " +
                            "para que puedas revisarlo " +
                            "antes de enviarlo.",

                    color = TextMuted,
                    style = AppTypography.helper
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

                    when {

                        !supportEmailRegex.matches(
                            email.trim()
                        ) -> {

                            error =
                                "Ingresa un correo válido."
                        }

                        description.isBlank() -> {

                            error =
                                "Describe el problema."
                        }

                        else -> {

                            onPrepareEmail(
                                email.trim(),
                                description.trim()
                            )
                        }
                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                )
            ) {
                Text(
                    text = "Preparar correo",
                    color = OnPrimary
                )
            }
        }
    )
}
