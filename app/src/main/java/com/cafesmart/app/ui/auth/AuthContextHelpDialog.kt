
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.cafesmart.app.ui.theme.*

enum class AuthHelpContext {
    LOGIN,
    REGISTER_STEP_1,
    REGISTER_STEP_2
}

private data class HelpContent(
    val title: String,
    val description: String,
    val instructions: List<String>
)

@Composable
fun AuthContextHelpDialog(
    context: AuthHelpContext,
    onDismiss: () -> Unit,
    onRecovery: () -> Unit,
    onSupport: () -> Unit
) {

    val content = when (context) {

        AuthHelpContext.LOGIN -> HelpContent(
            title = "Ayuda para iniciar sesión",

            description =
                "Aquí encontrarás información para " +
                        "ingresar a tu cuenta de Café Smart.",

            instructions = listOf(
                "Ingresa el correo electrónico " +
                        "con el que registraste tu cuenta.",

                "Escribe tu contraseña y comprueba " +
                        "que esté escrita correctamente.",

                "Presiona Entrar para continuar.",

                "Si todavía no tienes cuenta, " +
                        "selecciona Regístrate gratis."
            )
        )

        AuthHelpContext.REGISTER_STEP_1 -> HelpContent(
            title = "Ayuda: Paso 1",

            description =
                "En este paso debes registrar " +
                        "la información de tu negocio.",

            instructions = listOf(
                "Nombre del negocio: escribe el nombre " +
                        "con el que se identifica tu negocio. " +
                        "El máximo es de 30 caracteres.",

                "Compraventa: selecciona esta opción " +
                        "si compras café para venderlo.",

                "Cooperativa: selecciona esta opción " +
                        "si administras el café de tus asociados.",

                "Personalizado: selecciona esta opción " +
                        "si tu negocio funciona de otra manera.",

                "Después de completar los campos " +
                        "obligatorios, presiona Continuar."
            )
        )

        AuthHelpContext.REGISTER_STEP_2 -> HelpContent(
            title = "Ayuda: Paso 2",

            description =
                "En este paso registrarás los " +
                        "datos del administrador del negocio.",

            instructions = listOf(
                "Nombre y apellidos: escribe los datos " +
                        "de la persona que administrará la cuenta.",

                "Teléfono: ingresa un celular colombiano " +
                        "de 10 dígitos que empiece por 3.",

                "Correo electrónico: escribe un correo válido. " +
                        "Lo utilizarás para iniciar sesión.",

                "Contraseña: escribe al menos 8 caracteres, " +
                        "incluyendo mayúscula, minúscula y número.",

                "Confirmar contraseña: repite exactamente " +
                        "la contraseña anterior.",

                "Cuando los campos estén completos, " +
                        "presiona Crear cuenta."
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,

        shape = RoundedCornerShape(20.dp),

        title = {
            Text(
                text = content.title,
                style = AppTypography.titleModal,
                color = TextPrimary
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = content.description,
                    style = AppTypography.bodyMd,
                    color = TextSecondary
                )

                HorizontalDivider(
                    color = BorderDefault
                )

                content.instructions.forEachIndexed {
                        index, instruction ->

                    Text(
                        text = "${index + 1}. $instruction",
                        style = AppTypography.bodyMd,
                        color = TextPrimary
                    )
                }

                HorizontalDivider(
                    color = BorderDefault
                )

                if (context == AuthHelpContext.LOGIN) {

                    OutlinedButton(
                        onClick = onRecovery,
                        modifier = Modifier.fillMaxWidth(),

                        border = BorderStroke(
                            1.dp,
                            Primary
                        ),

                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = Primary
                            )
                    ) {
                        Text(
                            "Recuperar contraseña",
                            color = Primary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSupport,
                    modifier = Modifier.fillMaxWidth(),

                    border = BorderStroke(1.dp, Primary),

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
                    text = "Entendido",
                    color = Primary
                )
            }
        }
    )
}
