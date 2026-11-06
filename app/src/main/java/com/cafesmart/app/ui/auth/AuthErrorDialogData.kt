package com.cafesmart.app.ui.auth

import com.cafesmart.app.data.auth.AuthError

// Decide qué dice y qué ofrece el diálogo de error de autenticación según la causa.
// Es Kotlin puro (sin Compose ni Firebase) para poder probarlo con JUnit.
//
// Sigue el patrón de los diálogos de error del Figma (ícono, título, descripción y botones
// "Reintentar" / "Regresar"). El Figma no tiene un frame propio para login ni para "sin conexión";
// conviene agregarlos con esta misma estructura.

/** Desde qué pantalla ocurrió el error. */
enum class AuthOrigin { LOGIN, REGISTRO }

/** Lo que hace un botón del diálogo. La ejecuta AppNavHost, que tiene el NavController. */
enum class AuthErrorAction {
    /** Repite la solicitud a Firebase con los mismos datos. */
    REINTENTAR,

    /** Solo cierra el diálogo y deja a la persona en el formulario. */
    CERRAR,

    /** Vuelve al inicio de sesión con el correo ya escrito. */
    IR_A_LOGIN,

    /** Abre el registro. */
    IR_A_REGISTRO
}

data class AuthErrorDialogData(
    val title: String,
    val message: String,
    /** true: el diálogo muestra el ícono de red tachada en lugar de la "x" roja. */
    val sinConexion: Boolean,
    val primaryText: String,
    val primaryAction: AuthErrorAction,
    /** null: el diálogo muestra un solo botón. */
    val secondaryText: String?,
    val secondaryAction: AuthErrorAction = AuthErrorAction.CERRAR,
    val origin: AuthOrigin
)

fun authErrorDialogData(error: AuthError, origin: AuthOrigin): AuthErrorDialogData {
    val titulo = when (origin) {
        AuthOrigin.LOGIN -> "No pudimos iniciar sesión"
        AuthOrigin.REGISTRO -> "No pudimos crear tu cuenta"
    }

    return when {
        // Sin Internet: reintentar sí tiene sentido (la red puede volver).
        error == AuthError.SIN_CONEXION -> AuthErrorDialogData(
            title = "Sin conexión",
            message = error.mensaje,
            sinConexion = true,
            primaryText = "Reintentar",
            primaryAction = AuthErrorAction.REINTENTAR,
            secondaryText = "Regresar",
            origin = origin
        )

        // Repetir con el mismo correo daría el mismo error: se ofrece entrar o cambiar el correo.
        origin == AuthOrigin.REGISTRO && error == AuthError.CORREO_YA_REGISTRADO -> AuthErrorDialogData(
            title = titulo,
            message = error.mensaje,
            sinConexion = false,
            primaryText = "Iniciar sesión",
            primaryAction = AuthErrorAction.IR_A_LOGIN,
            secondaryText = "Cambiar correo",
            origin = origin
        )

        // El mensaje no dice cuál de los dos datos falló (así no se revela si el correo existe).
        // El botón principal vuelve al formulario; el secundario ayuda a quien aún no tiene cuenta.
        origin == AuthOrigin.LOGIN && error == AuthError.CREDENCIALES_INCORRECTAS -> AuthErrorDialogData(
            title = titulo,
            message = error.mensaje,
            sinConexion = false,
            primaryText = "Reintentar",
            primaryAction = AuthErrorAction.CERRAR,
            secondaryText = "Crear cuenta",
            secondaryAction = AuthErrorAction.IR_A_REGISTRO,
            origin = origin
        )

        // Fallo inesperado: puede ser pasajero, así que se deja reintentar.
        error == AuthError.DESCONOCIDO -> AuthErrorDialogData(
            title = titulo,
            message = error.mensaje,
            sinConexion = false,
            primaryText = "Reintentar",
            primaryAction = AuthErrorAction.REINTENTAR,
            secondaryText = "Regresar",
            origin = origin
        )

        // Resto (contraseña débil, demasiados intentos, cuenta deshabilitada...): repetir no sirve,
        // así que hay un solo botón para volver.
        else -> AuthErrorDialogData(
            title = titulo,
            message = error.mensaje,
            sinConexion = false,
            primaryText = if (origin == AuthOrigin.REGISTRO) "Volver al formulario" else "Regresar",
            primaryAction = AuthErrorAction.CERRAR,
            secondaryText = null,
            origin = origin
        )
    }
}
