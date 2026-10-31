
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite crear funciones que muestran interfaces
// gráficas utilizando Jetpack Compose
import androidx.compose.runtime.Composable

// Permite obtener y conservar una instancia del ViewModel
// asociada al ciclo de vida de la pantalla
import androidx.lifecycle.viewmodel.compose.viewModel


// ============================================================
// 1. FLUJO PRINCIPAL DE AUTENTICACIÓN
// ============================================================

/**
 * AuthFlow controla la navegación entre las pantallas
 * de autenticación de Café Smart.
 *
 * Este componente conecta:
 *
 * - Pantalla de inicio de sesión.
 * - Paso 1 del registro: información del negocio.
 * - Paso 2 del registro: datos del administrador.
 *
 * También comunica las acciones del usuario
 * con el AuthViewModel.
 *
 * El ViewModel administra los datos del formulario,
 * las validaciones y la pantalla que debe mostrarse.
 *
 * IMPORTANTE:
 * Este componente no realiza directamente
 * la autenticación ni guarda usuarios.
 *
 * Las acciones reales son recibidas mediante
 * las funciones onLogin y onRegister.
 */
@Composable
fun AuthFlow(

    // Envía los datos de inicio de sesión
    // cuando el formulario se completa correctamente
    onLogin: (LoginCredentials) -> Unit,

    // Envía los datos del registro
    // cuando se confirma la creación de la cuenta
    onRegister: (RegistrationData) -> Unit,

    // Acción para continuar con una cuenta de Google
    onGoogle: () -> Unit,

    // Acción para recuperar una contraseña
    onForgotPassword: () -> Unit,

    // Abre la ayuda correspondiente a cada pantalla
    onHelp: (AuthHelpContext) -> Unit,

    // Acción para contactar al equipo de soporte
    onSupport: () -> Unit,

    // Acción para salir del flujo de autenticación
    onExit: () -> Unit,

    // Indica si se está procesando una solicitud.
    // Por defecto es false, es decir, no está enviando.
    isSubmitting: Boolean = false,

    // ViewModel encargado de administrar el estado
    // y las acciones de los formularios
    viewModel: AuthViewModel = viewModel()
) {

    // ========================================================
    // 2. OBTENER EL ESTADO ACTUAL
    // ========================================================

    /**
     * Obtiene el estado de la interfaz desde AuthViewModel.
     *
     * Este estado contiene la información necesaria
     * para mostrar el formulario correspondiente.
     *
     * Por ejemplo:
     * - Pantalla actual.
     * - Datos ingresados por el usuario.
     * - Errores de validación.
     */
    val state = viewModel.uiState


    // ========================================================
    // 3. CONTROL DE NAVEGACIÓN ENTRE PANTALLAS
    // ========================================================

    /**
     * La expresión when revisa qué pantalla
     * debe mostrarse según state.page.
     *
     * Esto permite cambiar entre inicio de sesión
     * y los dos pasos del registro sin duplicar
     * la lógica de navegación.
     */
    when (state.page) {


        // ====================================================
        // 4. PANTALLA DE INICIO DE SESIÓN
        // ====================================================

        /**
         * Se muestra cuando el estado de navegación
         * corresponde a LOGIN.
         */
        AuthPage.LOGIN -> {

            // Muestra el formulario de inicio de sesión
            LoginScreen(

                // Envía el estado actual al formulario
                state = state,

                // Actualiza los campos cuando el usuario
                // modifica su información
                onChange = viewModel::update,

                // --------------------------------------------
                // ACCIÓN PARA INICIAR SESIÓN
                // --------------------------------------------

                /**
                 * Cuando el usuario pulsa el botón
                 * para iniciar sesión, se ejecuta
                 * submitLogin del ViewModel.
                 *
                 * El ViewModel puede validar los datos
                 * antes de enviarlos mediante onLogin.
                 */
                onSubmit = {

                    viewModel.submitLogin(onLogin)
                },

                // --------------------------------------------
                // IR AL REGISTRO
                // --------------------------------------------

                // Permite navegar desde Login
                // hacia el primer paso del registro
                onRegister = viewModel::goToRegister,

                // --------------------------------------------
                // CONTINUAR CON GOOGLE
                // --------------------------------------------

                // Ejecuta la acción recibida para Google
                onGoogle = onGoogle,

                // --------------------------------------------
                // RECUPERAR CONTRASEÑA
                // --------------------------------------------

                // Ejecuta la acción de recuperación
                onForgotPassword = onForgotPassword,

                // --------------------------------------------
                // AYUDA DEL INICIO DE SESIÓN
                // --------------------------------------------

                /**
                 * Indica que la ayuda solicitada
                 * corresponde al inicio de sesión.
                 *
                 * Así se pueden mostrar instrucciones
                 * específicas para esta pantalla.
                 */
                onHelp = {

                    onHelp(AuthHelpContext.LOGIN)
                },

                // --------------------------------------------
                // SOPORTE Y SALIDA
                // --------------------------------------------

                // Permite contactar soporte
                onSupport = onSupport,

                // Permite salir de la autenticación
                onExit = onExit,

                // Informa si hay una solicitud en proceso
                isSubmitting = isSubmitting
            )
        }


        // ====================================================
        // 5. REGISTRO - PASO 1
        // ====================================================

        /**
         * Muestra la pantalla donde el usuario
         * registra la información de su negocio.
         *
         * En este paso se selecciona el modelo
         * de negocio y se completan sus datos.
         */
        AuthPage.REGISTER_STEP_1 -> {

            // Muestra el primer formulario de registro
            RegisterStep1Screen(

                // Estado actual de los campos
                state = state,

                // Actualiza la información ingresada
                onChange = viewModel::update,

                // --------------------------------------------
                // REGRESAR AL INICIO DE SESIÓN
                // --------------------------------------------

                /**
                 * Ejecuta la función back del ViewModel
                 * para regresar a la pantalla anterior.
                 */
                onBack = viewModel::back,

                // --------------------------------------------
                // CONTINUAR AL SEGUNDO PASO
                // --------------------------------------------

                /**
                 * Ejecuta nextStep del ViewModel.
                 *
                 * Esta función es responsable de
                 * controlar el avance al segundo paso,
                 * según la lógica definida en el ViewModel.
                 */
                onContinue = viewModel::nextStep,

                // --------------------------------------------
                // AYUDA DEL PRIMER PASO
                // --------------------------------------------

                /**
                 * Abre las instrucciones correspondientes
                 * a los datos del negocio.
                 */
                onHelp = {

                    onHelp(
                        AuthHelpContext.REGISTER_STEP_1
                    )
                },

                // --------------------------------------------
                // CONTACTAR SOPORTE
                // --------------------------------------------

                // Permite solicitar asistencia durante
                // el primer paso del registro
                onSupport = onSupport
            )
        }


        // ====================================================
        // 6. REGISTRO - PASO 2
        // ====================================================

        /**
         * Muestra el formulario donde se registran
         * los datos del administrador.
         *
         * En esta pantalla se recopila información
         * como nombres, teléfono, correo y contraseña.
         */
        AuthPage.REGISTER_STEP_2 -> {

            // Muestra el segundo formulario de registro
            RegisterStep2Screen(

                // Envía los datos actuales del formulario
                state = state,

                // Actualiza los campos que modifica
                // el usuario
                onChange = viewModel::update,

                // --------------------------------------------
                // REGRESAR AL PRIMER PASO
                // --------------------------------------------

                /**
                 * Permite regresar al paso anterior
                 * mediante la función back.
                 */
                onBack = viewModel::back,

                // --------------------------------------------
                // CREAR CUENTA
                // --------------------------------------------

                /**
                 * Se ejecuta cuando el usuario
                 * pulsa el botón Crear cuenta.
                 *
                 * Llama a submitRegistration,
                 * que procesa los datos del formulario
                 * y utiliza onRegister como acción final.
                 *
                 * La validación específica depende
                 * de la implementación de AuthViewModel.
                 */
                onCreateAccount = {

                    viewModel.submitRegistration(
                        onRegister
                    )
                },

                // --------------------------------------------
                // CONTINUAR CON GOOGLE
                // --------------------------------------------

                // Permite ejecutar la acción de Google
                // desde el segundo paso del registro
                onGoogle = onGoogle,

                // --------------------------------------------
                // AYUDA DEL SEGUNDO PASO
                // --------------------------------------------

                /**
                 * Abre las instrucciones relacionadas
                 * con los datos del administrador.
                 */
                onHelp = {

                    onHelp(
                        AuthHelpContext.REGISTER_STEP_2
                    )
                },

                // --------------------------------------------
                // SOPORTE
                // --------------------------------------------

                // Permite contactar soporte durante
                // el segundo paso del registro
                onSupport = onSupport,

                // --------------------------------------------
                // ESTADO DE ENVÍO
                // --------------------------------------------

                /**
                 * Informa a la pantalla si se está
                 * procesando la creación de la cuenta.
                 */
                isSubmitting = isSubmitting
            )
        }
    }
}
