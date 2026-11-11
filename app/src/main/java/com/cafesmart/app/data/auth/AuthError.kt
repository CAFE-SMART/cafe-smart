package com.cafesmart.app.data.auth

// Errores de autenticación con el mensaje que ve el usuario.
// Se separan de Firebase para poder probarlos con JUnit sin el SDK.
enum class AuthError(val mensaje: String) {
    CORREO_INVALIDO("El correo no es válido. Revísalo e inténtalo de nuevo."),
    CREDENCIALES_INCORRECTAS("Correo o contraseña incorrectos. Revísalos e inténtalo de nuevo."),
    CORREO_YA_REGISTRADO("Ya existe una cuenta con este correo. Inicia sesión o usa otro correo."),
    CONTRASENA_DEBIL("La contraseña es muy débil. Usa mínimo 8 caracteres con mayúscula, minúscula y número."),
    CUENTA_DESHABILITADA("Esta cuenta está deshabilitada. Comunícate con soporte."),
    DEMASIADOS_INTENTOS("Hiciste demasiados intentos. Espera unos minutos e inténtalo de nuevo."),
    SIN_CONEXION("No hay conexión a Internet. Revisa tu red e inténtalo de nuevo."),
    CORREO_NO_REGISTRADO("No encontramos una cuenta con ese correo. Revísalo o crea una cuenta nueva."),
    METODO_NO_HABILITADO("El ingreso con correo y contraseña no está habilitado en Firebase. Avisa al equipo."),
    DESCONOCIDO("Algo salió mal. Inténtalo de nuevo.")
}

/** Traduce el errorCode de FirebaseAuthException (por ejemplo "ERROR_WRONG_PASSWORD") a un AuthError. */
fun authErrorDesdeCodigo(codigo: String?): AuthError = when (codigo) {
    "ERROR_INVALID_EMAIL", "ERROR_MISSING_EMAIL" -> AuthError.CORREO_INVALIDO
    // Por seguridad, correo inexistente y contraseña errada muestran el mismo mensaje.
    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> AuthError.CREDENCIALES_INCORRECTAS
    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.CORREO_YA_REGISTRADO
    "ERROR_WEAK_PASSWORD" -> AuthError.CONTRASENA_DEBIL
    "ERROR_USER_DISABLED" -> AuthError.CUENTA_DESHABILITADA
    "ERROR_TOO_MANY_REQUESTS" -> AuthError.DEMASIADOS_INTENTOS
    "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.SIN_CONEXION
    "ERROR_OPERATION_NOT_ALLOWED" -> AuthError.METODO_NO_HABILITADO
    else -> AuthError.DESCONOCIDO
}

/**
 * Igual que [authErrorDesdeCodigo], pero para pedir el enlace de recuperación: aquí un correo
 * inexistente sí se dice claro (no se está adivinando una contraseña). Si Firebase tiene activada
 * la protección contra enumeración de correos, no devuelve este código y se verá la confirmación.
 */
fun authErrorRecuperacionDesdeCodigo(codigo: String?): AuthError = when (codigo) {
    "ERROR_USER_NOT_FOUND" -> AuthError.CORREO_NO_REGISTRADO
    else -> authErrorDesdeCodigo(codigo)
}
