
package com.cafesmart.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import com.cafesmart.app.ui.components.AppSuccessScreen
import com.cafesmart.app.ui.components.FeedbackDetail
import com.cafesmart.app.ui.theme.CafesmartTheme

@Composable
fun RegistrationSuccessScreen(
    registration: RegistrationData,
    isDemo: Boolean,
    onGoHome: () -> Unit,
    onBackToRegister: () -> Unit
) {

    AppSuccessScreen(
        title =
            if (isDemo)
                "Registro de prueba completado"
            else
                "¡Cuenta creada con éxito!",

        message =
            if (isDemo)
                "Los datos del formulario son válidos. " +
                        "Esta es una demostración: " +
                        "no se creó una cuenta real."
            else
                "Tu cuenta se creó correctamente. " +
                        "Ya puedes continuar a Inicio.",

        summaryTitle = "Resumen del registro",

        details = listOf(

            FeedbackDetail(
                label = "Nombre del negocio",
                value = registration.businessName
            ),

            FeedbackDetail(
                label = "Tipo de negocio",
                value = registration.businessType.label
            ),

            FeedbackDetail(
                label = "Administrador",
                value = (
                        registration.firstName + " " +
                                registration.lastName
                        ).trim()
            ),

            FeedbackDetail(
                label = "Correo electrónico",
                value = registration.email
            )
        ),

        primaryButtonText = "Ir a Inicio",
        secondaryButtonText = "Volver al registro",

        onPrimaryClick = onGoHome,
        onSecondaryClick = onBackToRegister
    )
}

@Preview(
    name = "Registro exitoso Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartRegistrationSuccessPreview() {

    CafesmartTheme {
        RegistrationSuccessScreen(

            registration = RegistrationData(
                businessName = "Café Los Alpes",
                businessType = BusinessType.COMPRAVENTA,
                firstName = "María",
                lastName = "Gómez",
                phone = "3001234567",
                email = "ejemplo@correo.com",
                password = "Ejemplo123"
            ),

            isDemo = true,
            onGoHome = {},
            onBackToRegister = {}
        )
    }
}
