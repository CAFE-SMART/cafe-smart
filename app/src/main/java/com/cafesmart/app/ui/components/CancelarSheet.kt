package com.cafesmart.app.ui.components

import androidx.compose.runtime.Composable
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText

// Hoja "¿Cancelar ...?" que comparten Compras y Ventas: solo cambia el titulo.
// Usa la hoja comun ConfirmSheet con los textos y colores de cancelar.
@Composable
fun CancelarSheet(
    titulo: String,
    onCancelar: () -> Unit,
    onSeguir: () -> Unit
) {
    ConfirmSheet(
        icon = AppIcons.Warning,
        iconBackground = ErrorBg,
        iconTint = ErrorIcon,
        title = titulo,
        message = "Se perderán los datos ingresados y tendrás que iniciar el proceso nuevamente.",
        actionText = "Sí, cancelar",
        onAction = onCancelar,
        linkText = "Seguir editando",
        onLink = onSeguir,
        actionContainerColor = ErrorBg,
        actionContentColor = ErrorText
    )
}
