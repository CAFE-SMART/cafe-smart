
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite leer valores de estado usando "by"
import androidx.compose.runtime.getValue

// Crea estados observables por Jetpack Compose
import androidx.compose.runtime.mutableStateOf

// Permite modificar valores de estado usando "by"
import androidx.compose.runtime.setValue

// Clase base para implementar el patrón MVVM
import androidx.lifecycle.ViewModel


// ============================================================
// 1. PANTALLAS DISPONIBLES EN LA AUTENTICACIÓN
// ============================================================

/**
 * Define las tres pantallas del flujo de autenticación.
 *
 * LOGIN: iniciar sesión.
 * REGISTER_STEP_1: información del negocio.
 * REGISTER_STEP_2: datos del administrador.
 */
enum class AuthPage {

    LOGIN,

    REGISTER_STEP_1,

    REGISTER_STEP_2
}


// ============================================================
// 2. TIPOS DE NEGOCIO
// ============================================================

/**
 * Define los modelos de negocio disponibles
 * durante el registro de Café Smart.
 *
 * Cada opción contiene:
 *
 * label: nombre que se muestra al usuario.
 * subtitle: explicación del tipo de negocio.
 */
enum class BusinessType(
    val label: String,
    val subtitle: String
) {

    // Negocio que compra y revende café
    COMPRAVENTA(
        "Compraventa",
        "Si compras café y lo revendes."
    ),

    // Organización que comercializa café
    // proveniente de productores asociados
    COOPERATIVA(
        "Cooperativa",
        "Si vendes el café de tus asociados."
    ),

    // Negocio con un modelo diferente
    PERSONALIZADO(
        "Personalizado",
        "Si tu negocio es de otro tipo."
    )
}


// ============================================================
// 3. DATOS PARA INICIAR SESIÓN
// ============================================================

/**
 * Almacena las credenciales que se entregan
 * al procesar el inicio de sesión.
 *
 * email: correo electrónico.
 * password: contraseña.
 * rememberMe: preferencia de recordar la sesión.
 */
data class LoginCredentials(

    val email: String,

    val password: String,

    val rememberMe: Boolean
)


// ============================================================
// 4. DATOS PARA CREAR UNA CUENTA
// ============================================================

/**
 * Agrupa la información del negocio
 * y del administrador.
 *
 * Se utiliza para entregar los datos completos
 * cuando el formulario supera las validaciones.
 */
data class RegistrationData(

    // Nombre del negocio cafetero
    val businessName: String,

    // Tipo de negocio seleccionado
    val businessType: BusinessType,

    // Datos personales del administrador
    val firstName: String,
    val lastName: String,

    // Número de celular
    val phone: String,

    // Correo de la cuenta
    val email: String,

    // Contraseña definida durante el registro
    val password: String
)


// ============================================================
// 5. ESTADO DE LA INTERFAZ DE AUTENTICACIÓN
// ============================================================

/**
 * AuthUiState contiene toda la información
 * que necesita el flujo de autenticación.
 *
 * Sus valores iniciales permiten mostrar
 * los formularios vacíos al comenzar.
 *
 * La pantalla inicial es LOGIN.
 */
data class AuthUiState(

    // --------------------------------------------------------
    // PANTALLA ACTUAL
    // --------------------------------------------------------

    val page: AuthPage = AuthPage.LOGIN,


    // --------------------------------------------------------
    // DATOS DEL INICIO DE SESIÓN
    // --------------------------------------------------------

    // Correo ingresado para iniciar sesión
    val loginEmail: String = "",

    // Contraseña del inicio de sesión
    val loginPassword: String = "",

    // Preferencia inicial de recordar la sesión
    val rememberMe: Boolean = true,


    // --------------------------------------------------------
    // PASO 1: INFORMACIÓN DEL NEGOCIO
    // --------------------------------------------------------

    // Nombre del negocio
    val businessName: String = "",

    // Tipo de negocio; inicialmente no seleccionado
    val businessType: BusinessType? = null,


    // --------------------------------------------------------
    // PASO 2: DATOS DEL ADMINISTRADOR
    // --------------------------------------------------------

    // Nombre de la persona administradora
    val firstName: String = "",

    // Apellidos
    val lastName: String = "",

    // Teléfono celular
    val phone: String = "",

    // Correo electrónico del administrador
    val email: String = "",

    // Contraseña del registro
    val password: String = "",

    // Confirmación de la contraseña
    val passwordConfirmation: String = "",


    // --------------------------------------------------------
    // MENSAJES DE ERROR
    // --------------------------------------------------------

    /**
     * Guarda los errores de los campos.
     *
     * Cada error se identifica mediante una clave,
     * por ejemplo "email" o "businessName".
     */
    val errors: Map<String, String> = emptyMap(),

    // Almacena errores generales de una solicitud
    val requestError: String? = null
)


// ============================================================
// 6. VIEWMODEL DE AUTENTICACIÓN
// ============================================================

/**
 * Administra el estado y las acciones de autenticación.
 *
 * Responsabilidades:
 *
 * - Actualizar los datos de los formularios.
 * - Controlar las pantallas del registro.
 * - Validar la información ingresada.
 * - Preparar los datos para iniciar sesión.
 * - Preparar los datos para registrar una cuenta.
 * - Administrar los mensajes de error.
 * - Restablecer los datos al salir.
 *
 * Sigue el patrón MVVM utilizado por Café Smart.
 */
class AuthViewModel : ViewModel() {


    // ========================================================
    // 7. ESTADO PRINCIPAL DEL VIEWMODEL
    // ========================================================

    /**
     * Guarda el estado completo del formulario.
     *
     * mutableStateOf permite que Compose observe
     * los cambios y actualice la interfaz.
     *
     * private set evita que otras clases
     * modifiquen directamente este estado.
     */
    var uiState by mutableStateOf(AuthUiState())
        private set


    // ========================================================
    // 8. ACTUALIZAR DATOS DEL FORMULARIO
    // ========================================================

    /**
     * Permite modificar los campos de AuthUiState.
     *
     * transform recibe el estado actual y devuelve
     * una nueva versión con los cambios.
     *
     * También limpia los errores anteriores.
     */
    fun update(
        transform: (AuthUiState) -> AuthUiState
    ) {

        uiState = transform(uiState).copy(

            // Limpia los errores de validación
            errors = emptyMap(),

            // Limpia el error general anterior
            requestError = null
        )
    }


    // ========================================================
    // 9. IR AL PRIMER PASO DEL REGISTRO
    // ========================================================

    /**
     * Cambia la pantalla actual desde Login
     * hacia el primer paso del registro.
     */
    fun goToRegister() {

        uiState = uiState.copy(

            // Establece la pantalla de registro
            page = AuthPage.REGISTER_STEP_1,

            // Reinicia los mensajes de error
            errors = emptyMap(),
            requestError = null
        )
    }


    // ========================================================
    // 10. REGRESAR AL INICIO DE SESIÓN
    // ========================================================

    /**
     * Cambia la pantalla actual a Login.
     *
     * No elimina los datos ingresados;
     * solamente actualiza la navegación
     * y limpia los errores.
     */
    fun goToLogin() {

        uiState = uiState.copy(

            page = AuthPage.LOGIN,

            errors = emptyMap(),

            requestError = null
        )
    }


    // ========================================================
    // 11. NAVEGACIÓN HACIA ATRÁS
    // ========================================================

    /**
     * Permite regresar a la pantalla anterior
     * dependiendo del paso actual.
     */
    fun back() {

        when (uiState.page) {

            // Si ya está en Login, no hace ningún cambio
            AuthPage.LOGIN -> Unit

            // Desde el paso 1 regresa al inicio de sesión
            AuthPage.REGISTER_STEP_1 -> {

                goToLogin()
            }

            // Desde el paso 2 regresa al paso 1
            AuthPage.REGISTER_STEP_2 -> {

                uiState = uiState.copy(

                    page = AuthPage.REGISTER_STEP_1,

                    // Limpia los errores de los campos
                    errors = emptyMap()
                )
            }
        }
    }


    // ========================================================
    // 12. AVANZAR AL SEGUNDO PASO DEL REGISTRO
    // ========================================================

    /**
     * Comprueba que el nombre y el tipo
     * de negocio sean válidos.
     *
     * Si encuentra errores, los muestra.
     *
     * Si no encuentra errores, permite
     * avanzar al segundo paso.
     */
    fun nextStep() {

        // Ejecuta las validaciones del primer paso
        val errors = AuthValidation.stepOne(
            uiState.businessName,
            uiState.businessType
        )

        // Si existen errores, no permite continuar
        if (errors.isNotEmpty()) {

            uiState = uiState.copy(
                errors = errors
            )

        } else {

            // Los campos son válidos y avanza al paso 2
            uiState = uiState.copy(

                page = AuthPage.REGISTER_STEP_2,

                errors = emptyMap()
            )
        }
    }


    // ========================================================
    // 13. PROCESAR EL INICIO DE SESIÓN
    // ========================================================

    /**
     * Valida los campos del inicio de sesión.
     *
     * onValid se ejecuta únicamente cuando
     * los datos superan las validaciones locales.
     *
     * IMPORTANTE:
     * Validar el formulario no significa que
     * las credenciales sean correctas en el servidor.
     */
    fun submitLogin(
        onValid: (LoginCredentials) -> Unit
    ) {

        // Valida correo y contraseña
        val errors = AuthValidation.login(
            uiState.loginEmail,
            uiState.loginPassword
        )

        // Comprueba si existen errores
        if (errors.isNotEmpty()) {

            // Guarda los errores para mostrarlos
            uiState = uiState.copy(
                errors = errors
            )

            // Detiene el proceso
            return
        }

        // Si no hay errores, entrega las credenciales
        onValid(

            LoginCredentials(

                // Elimina espacios alrededor del correo
                email = uiState.loginEmail.trim(),

                // Conserva la contraseña escrita
                password = uiState.loginPassword,

                // Incluye la preferencia de recordar sesión
                rememberMe = uiState.rememberMe
            )
        )
    }


    // ========================================================
    // 14. PROCESAR EL REGISTRO DE UNA CUENTA
    // ========================================================

    /**
     * Valida los datos del segundo paso.
     *
     * Si son válidos, construye un objeto
     * RegistrationData y lo entrega a onValid.
     *
     * Este método no crea directamente
     * la cuenta en el servidor.
     */
    fun submitRegistration(
        onValid: (RegistrationData) -> Unit
    ) {

        // Ejecuta las validaciones del segundo paso
        val errors = AuthValidation.stepTwo(
            uiState
        )

        // Si existen errores, detiene el registro
        if (errors.isNotEmpty()) {

            uiState = uiState.copy(
                errors = errors
            )

            return
        }

        /**
         * Obtiene el tipo de negocio seleccionado.
         *
         * requireNotNull exige que el valor no sea null.
         *
         * Si fuera null, generaría una excepción.
         */
        val type = requireNotNull(
            uiState.businessType
        )

        // Entrega todos los datos del registro
        onValid(

            RegistrationData(

                // Información del negocio
                businessName = uiState.businessName.trim(),
                businessType = type,

                // Información personal
                firstName = uiState.firstName.trim(),
                lastName = uiState.lastName.trim(),

                // Información de contacto
                phone = uiState.phone.trim(),
                email = uiState.email.trim(),

                // Contraseña de la cuenta
                password = uiState.password
            )
        )
    }


    // ========================================================
    // 15. MOSTRAR ERROR GENERAL DE SOLICITUD
    // ========================================================

    /**
     * Guarda un mensaje cuando ocurre un error
     * durante una solicitud.
     *
     * Por ejemplo, un error de autenticación
     * que se informe desde otra capa.
     */
    fun showRequestError(message: String) {

        uiState = uiState.copy(

            requestError = message
        )
    }


    // ========================================================
    // 16. LIMPIAR LOS DATOS DE AUTENTICACIÓN
    // ========================================================

    /**
     * Restablece todos los datos a sus valores iniciales.
     *
     * Limpia:
     * - Correo y contraseña de Login.
     * - Información del negocio.
     * - Datos del administrador.
     * - Errores de validación.
     *
     * También regresa a LOGIN.
     */
    fun reset() {

        uiState = AuthUiState()
    }
}


// ============================================================
// 17. VALIDACIONES DE LOS FORMULARIOS
// ============================================================

/**
 * AuthValidation contiene las reglas utilizadas
 * para comprobar los datos ingresados.
 *
 * Se utiliza desde AuthViewModel.
 *
 * Todas las funciones devuelven un mapa de errores.
 *
 * Si el mapa está vacío, no se detectaron
 * errores según las reglas correspondientes.
 */
object AuthValidation {


    // ========================================================
    // 18. EXPRESIONES REGULARES
    // ========================================================

    /**
     * Comprueba el formato básico de un correo.
     *
     * No verifica que el correo exista.
     */
    private val emailRegex =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /**
     * Comprueba que el celular:
     *
     * - Empiece por 3.
     * - Contenga exactamente 10 dígitos.
     */
    private val phoneRegex =
        Regex("^3[0-9]{9}$")


    // ========================================================
    // 19. VALIDACIÓN DEL INICIO DE SESIÓN
    // ========================================================

    /**
     * Valida el correo y la contraseña de Login.
     *
     * Devuelve un mapa con los errores encontrados.
     */
    fun login(
        email: String,
        password: String
    ): Map<String, String> = buildMap {

        // Valida el formato del correo
        if (!emailRegex.matches(email.trim())) {

            put(
                "loginEmail",
                "Ingresa un correo electrónico válido."
            )
        }

        // Comprueba que la contraseña no esté vacía
        if (password.isBlank()) {

            put(
                "loginPassword",
                "Escribe tu contraseña."
            )
        }
    }


    // ========================================================
    // 20. VALIDACIÓN DEL REGISTRO - PASO 1
    // ========================================================

    /**
     * Comprueba el nombre y el modelo de negocio.
     *
     * El nombre es obligatorio y debe tener
     * máximo 30 caracteres después de trim().
     *
     * También es obligatorio seleccionar
     * un tipo de negocio.
     */
    fun stepOne(
        name: String,
        type: BusinessType?
    ): Map<String, String> = buildMap {

        // Comprueba que el nombre no esté vacío
        if (name.isBlank()) {

            put(
                "businessName",
                "Escribe el nombre del negocio."
            )

        } else if (name.trim().length > 30) {

            // Evita nombres superiores a 30 caracteres
            put(
                "businessName",
                "Máximo 30 caracteres."
            )
        }

        // Comprueba que se haya elegido el tipo de negocio
        if (type == null) {

            put(
                "businessType",
                "Selecciona el tipo de negocio."
            )
        }
    }


    // ========================================================
    // 21. VALIDACIÓN DEL REGISTRO - PASO 2
    // ========================================================

    /**
     * Valida los datos del administrador.
     *
     * Revisa:
     *
     * - Nombre.
     * - Apellidos.
     * - Teléfono.
     * - Correo electrónico.
     * - Contraseña.
     * - Confirmación de contraseña.
     */
    fun stepTwo(
        state: AuthUiState
    ): Map<String, String> = buildMap {


        // ----------------------------------------------------
        // VALIDAR NOMBRE
        // ----------------------------------------------------

        // Comprueba que el nombre no esté vacío
        if (state.firstName.isBlank()) {

            put(
                "firstName",
                "Ingresa tu nombre."
            )
        }


        // ----------------------------------------------------
        // VALIDAR APELLIDOS
        // ----------------------------------------------------

        // Comprueba que se hayan ingresado los apellidos
        if (state.lastName.isBlank()) {

            put(
                "lastName",
                "Ingresa tus apellidos."
            )
        }


        // ----------------------------------------------------
        // VALIDAR TELÉFONO
        // ----------------------------------------------------

        // Comprueba que sea un celular colombiano
        // de 10 dígitos que comience por 3
        if (!phoneRegex.matches(state.phone.trim())) {

            put(
                "phone",
                "Ingresa un celular de 10 dígitos " +
                        "que empiece por 3."
            )
        }


        // ----------------------------------------------------
        // VALIDAR CORREO ELECTRÓNICO
        // ----------------------------------------------------

        // Comprueba que tenga un formato de correo válido
        if (!emailRegex.matches(state.email.trim())) {

            put(
                "email",
                "Ingresa un correo válido."
            )
        }


        // ----------------------------------------------------
        // VALIDAR CONTRASEÑA
        // ----------------------------------------------------

        // Obtiene la contraseña escrita por el usuario
        val password = state.password

        /**
         * La contraseña debe cumplir:
         *
         * - Mínimo 8 caracteres.
         * - Al menos una letra mayúscula.
         * - Al menos una letra minúscula.
         * - Al menos un número.
         */
        if (
            password.length < 8 ||

            // Comprueba si existe una mayúscula
            !password.any(Char::isUpperCase) ||

            // Comprueba si existe una minúscula
            !password.any(Char::isLowerCase) ||

            // Comprueba si existe un número
            !password.any(Char::isDigit)
        ) {

            put(
                "password",
                "Mínimo 8 caracteres con mayúscula, " +
                        "minúscula y número."
            )
        }


        // ----------------------------------------------------
        // VALIDAR CONFIRMACIÓN DE CONTRASEÑA
        // ----------------------------------------------------

        /**
         * Comprueba que:
         *
         * - La confirmación no esté vacía.
         * - La confirmación sea igual a la contraseña.
         */
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
