
package com.cafesmart.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class AuthPage {
    LOGIN,
    REGISTER_STEP_1,
    REGISTER_STEP_2
}

enum class BusinessType(
    val label: String,
    val subtitle: String
) {
    COMPRAVENTA(
        "Compraventa",
        "Si compras café y lo revendes."
    ),

    COOPERATIVA(
        "Cooperativa",
        "Si vendes el café de tus asociados."
    ),

    PERSONALIZADO(
        "Personalizado",
        "Si tu negocio es de otro tipo."
    )
}

data class LoginCredentials(
    val email: String,
    val password: String,
    val rememberMe: Boolean
)

data class RegistrationData(
    val businessName: String,
    val businessType: BusinessType,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val password: String
)

data class AuthUiState(
    val page: AuthPage = AuthPage.LOGIN,

    val loginEmail: String = "",
    val loginPassword: String = "",
    val rememberMe: Boolean = true,

    val businessName: String = "",
    val businessType: BusinessType? = null,

    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",

    val errors: Map<String, String> = emptyMap(),
    val requestError: String? = null
)

class AuthViewModel : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun update(
        transform: (AuthUiState) -> AuthUiState
    ) {
        uiState = transform(uiState).copy(
            errors = emptyMap(),
            requestError = null
        )
    }

    fun goToRegister() {
        uiState = uiState.copy(
            page = AuthPage.REGISTER_STEP_1,
            errors = emptyMap(),
            requestError = null
        )
    }

    fun goToLogin() {
        uiState = uiState.copy(
            page = AuthPage.LOGIN,
            errors = emptyMap(),
            requestError = null
        )
    }

    fun back() {

        when (uiState.page) {

            AuthPage.LOGIN -> Unit

            AuthPage.REGISTER_STEP_1 -> {
                goToLogin()
            }

            AuthPage.REGISTER_STEP_2 -> {
                uiState = uiState.copy(
                    page = AuthPage.REGISTER_STEP_1,
                    errors = emptyMap()
                )
            }
        }
    }

    fun nextStep() {

        val errors = AuthValidation.stepOne(
            uiState.businessName,
            uiState.businessType
        )

        if (errors.isNotEmpty()) {
            uiState = uiState.copy(
                errors = errors
            )
        } else {
            uiState = uiState.copy(
                page = AuthPage.REGISTER_STEP_2,
                errors = emptyMap()
            )
        }
    }

    fun submitLogin(
        onValid: (LoginCredentials) -> Unit
    ) {

        val errors = AuthValidation.login(
            uiState.loginEmail,
            uiState.loginPassword
        )

        if (errors.isNotEmpty()) {
            uiState = uiState.copy(
                errors = errors
            )
            return
        }

        onValid(
            LoginCredentials(
                email = uiState.loginEmail.trim(),
                password = uiState.loginPassword,
                rememberMe = uiState.rememberMe
            )
        )
    }

    fun submitRegistration(
        onValid: (RegistrationData) -> Unit
    ) {

        val errors = AuthValidation.stepTwo(
            uiState
        )

        if (errors.isNotEmpty()) {
            uiState = uiState.copy(
                errors = errors
            )
            return
        }

        val type = requireNotNull(
            uiState.businessType
        )

        onValid(
            RegistrationData(
                businessName = uiState.businessName.trim(),
                businessType = type,
                firstName = uiState.firstName.trim(),
                lastName = uiState.lastName.trim(),
                phone = uiState.phone.trim(),
                email = uiState.email.trim(),
                password = uiState.password
            )
        )
    }

    fun showRequestError(message: String) {
        uiState = uiState.copy(
            requestError = message
        )
    }

    // LIMPIAR LOS DATOS AL SALIR
    fun reset() {
        uiState = AuthUiState()
    }
}

object AuthValidation {

    private val emailRegex =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    private val phoneRegex =
        Regex("^3[0-9]{9}$")

    fun login(
        email: String,
        password: String
    ): Map<String, String> = buildMap {

        if (!emailRegex.matches(email.trim())) {

            put(
                "loginEmail",
                "Ingresa un correo electrónico válido."
            )
        }

        if (password.isBlank()) {

            put(
                "loginPassword",
                "Escribe tu contraseña."
            )
        }
    }

    fun stepOne(
        name: String,
        type: BusinessType?
    ): Map<String, String> = buildMap {

        if (name.isBlank()) {

            put(
                "businessName",
                "Escribe el nombre del negocio."
            )

        } else if (name.trim().length > 30) {

            put(
                "businessName",
                "Máximo 30 caracteres."
            )
        }

        if (type == null) {

            put(
                "businessType",
                "Selecciona el tipo de negocio."
            )
        }
    }

    fun stepTwo(
        state: AuthUiState
    ): Map<String, String> = buildMap {

        if (state.firstName.isBlank()) {

            put(
                "firstName",
                "Ingresa tu nombre."
            )
        }

        if (state.lastName.isBlank()) {

            put(
                "lastName",
                "Ingresa tus apellidos."
            )
        }

        if (!phoneRegex.matches(state.phone.trim())) {

            put(
                "phone",
                "Ingresa un celular de 10 dígitos " +
                        "que empiece por 3."
            )
        }

        if (!emailRegex.matches(state.email.trim())) {

            put(
                "email",
                "Ingresa un correo válido."
            )
        }

        val password = state.password

        if (
            password.length < 8 ||
            !password.any(Char::isUpperCase) ||
            !password.any(Char::isLowerCase) ||
            !password.any(Char::isDigit)
        ) {

            put(
                "password",
                "Mínimo 8 caracteres con mayúscula, " +
                        "minúscula y número."
            )
        }

        if (
            state.passwordConfirmation != password ||
            state.passwordConfirmation.isBlank()
        ) {

            put(
                "passwordConfirmation",
                "Las contraseñas deben coincidir."
            )
        }
    }
}
