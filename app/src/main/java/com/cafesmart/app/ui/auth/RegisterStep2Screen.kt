
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite crear fondos para los elementos
import androidx.compose.foundation.background

// Componentes para organizar el contenido de la pantalla
import androidx.compose.foundation.layout.*

// LazyColumn permite mostrar contenido vertical desplazable
// componiendo sus elementos según se necesitan
import androidx.compose.foundation.lazy.LazyColumn

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Componentes visuales de Material Design 3
import androidx.compose.material3.*

// Herramientas para administrar los estados de Compose
import androidx.compose.runtime.*

// Permite alinear los componentes
import androidx.compose.ui.Alignment

// Permite modificar tamaño, posición y apariencia
import androidx.compose.ui.Modifier

// Tipos de teclado y visibilidad de contraseñas
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

// Permite visualizar la pantalla desde Android Studio
import androidx.compose.ui.tooling.preview.Preview

// Unidades de medida
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Botón reutilizable del proyecto Café Smart
import com.cafesmart.app.ui.components.ContinueButton

// Colores y tipografías de la aplicación
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. PANTALLA DEL SEGUNDO PASO DEL REGISTRO
// ============================================================

/**
 * RegisterStep2Screen muestra el formulario
 * para registrar los datos del administrador.
 *
 * Incluye:
 *
 * - Nombre y apellidos.
 * - Teléfono celular.
 * - Correo electrónico.
 * - Contraseña.
 * - Confirmación de contraseña.
 * - Opciones de ayuda y soporte.
 * - Botón para crear cuenta.
 * - Opción para continuar con Google.
 *
 * Utiliza LazyColumn para organizar
 * y desplazar el formulario verticalmente.
 *
 * Los datos se administran mediante AuthUiState.
 */
@Composable
fun RegisterStep2Screen(

    // Estado actual del formulario
    state: AuthUiState,

    // Función utilizada para actualizar los campos
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,

    // Acción para regresar al paso anterior
    onBack: () -> Unit,

    // Acción para procesar la creación de la cuenta
    onCreateAccount: () -> Unit,

    // Acción para continuar con Google
    onGoogle: () -> Unit,

    // Acción para mostrar la ayuda
    onHelp: () -> Unit = {},

    // Acción para contactar soporte
    onSupport: () -> Unit = {},

    // Indica si se está procesando el registro
    isSubmitting: Boolean = false
) {

    // ========================================================
    // 2. ESTADOS DE VISIBILIDAD DE CONTRASEÑAS
    // ========================================================

    /**
     * Controla si la contraseña principal
     * se muestra u oculta.
     *
     * Inicialmente permanece oculta.
     */
    var showPassword by remember {
        mutableStateOf(false)
    }

    /**
     * Controla si la confirmación
     * de contraseña se muestra u oculta.
     *
     * Se administra independientemente
     * de la contraseña principal.
     */
    var showConfirmation by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // 3. ESTRUCTURA PRINCIPAL DE LA PANTALLA
    // ========================================================

    /**
     * Scaffold organiza la pantalla y permite
     * separar el encabezado del contenido.
     *
     * topBar contiene el encabezado del registro.
     */
    Scaffold(

        // Color de fondo de la pantalla
        containerColor = SurfaceCard,

        // Encabezado superior
        topBar = {

            RegisterHeader(
                onBack = onBack
            )
        }

    ) { paddingValues ->


        // ====================================================
        // 4. CONTENIDO DESPLAZABLE CON LAZYCOLUMN
        // ====================================================

        /**
         * LazyColumn organiza el formulario
         * en elementos verticales desplazables.
         *
         * A diferencia de Column con verticalScroll,
         * LazyColumn compone los elementos que se
         * necesitan para mostrar el contenido.
         *
         * Cada item representa una sección
         * del formulario.
         *
         * Esto permite mantener el encabezado
         * separado del contenido desplazable.
         */
        LazyColumn(

            modifier = Modifier

                // Ocupa el espacio disponible
                .fillMaxSize()

                // Respeta el espacio del Scaffold
                .padding(paddingValues),

            // Márgenes internos del formulario
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            )
        ) {


            // =================================================
            // 5. INDICADOR DE PROGRESO Y AYUDA
            // =================================================

            item {

                /**
                 * Indica que el usuario está
                 * en el segundo paso del registro.
                 */
                RegistrationProgress(step = 2)

                // Separación vertical
                Spacer(Modifier.height(12.dp))


                // =============================================
                // 6. ENLACES DE AYUDA Y SOPORTE
                // =============================================

                /**
                 * Muestra las opciones de ayuda
                 * alineadas a la derecha.
                 */
                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // Abre la ayuda del paso 2
                    TextButton(
                        onClick = onHelp
                    ) {

                        Text(
                            text = "Ver ayuda",
                            color = Primary
                        )
                    }

                    // Abre la opción de soporte
                    TextButton(
                        onClick = onSupport
                    ) {

                        Text(
                            text = "Contactar soporte",
                            color = Primary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }


            // =================================================
            // 7. MENSAJE INFORMATIVO DEL ADMINISTRADOR
            // =================================================

            item {

                /**
                 * Presenta información sobre el papel
                 * de la persona administradora.
                 *
                 * Se utiliza un fondo destacado
                 * para diferenciarlo del formulario.
                 */
                Row(

                    modifier = Modifier
                        .fillMaxWidth()

                        // Fondo y esquinas redondeadas
                        .background(
                            SurfaceSelected,
                            RoundedCornerShape(10.dp)
                        )

                        // Espacio interno del mensaje
                        .padding(14.dp),

                    verticalAlignment = Alignment.Top
                ) {

                    // Indicador visual del mensaje
                    Text(
                        text = "●",
                        color = Primary,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.width(10.dp))

                    // Explicación para el usuario
                    Text(

                        text =
                            "Este usuario administrará el negocio, " +
                                    "el inventario y los reportes. " +
                                    "Usa datos reales para recuperar el acceso " +
                                    "si lo necesitas. Todos los campos " +
                                    "son obligatorios.",

                        style = AppTypography.bodyMd.copy(
                            fontSize = 12.sp,
                            color = PrimaryDark
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))
            }


            // =================================================
            // 8. CAMPOS DE NOMBRE Y APELLIDOS
            // =================================================

            item {

                /**
                 * Se utiliza Row para mostrar
                 * nombre y apellidos horizontalmente.
                 *
                 * Cada campo utiliza weight(1f)
                 * para compartir el espacio disponible.
                 */
                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {


                    // =========================================
                    // CAMPO DE NOMBRE
                    // =========================================

                    AuthField(

                        title = "Nombre",

                        // Obtiene el nombre del estado
                        value = state.firstName,

                        // Actualiza el nombre ingresado
                        onValueChange = { value ->

                            onChange {

                                it.copy(
                                    firstName = value
                                )
                            }
                        },

                        // Instrucciones del campo
                        hint = "Ingresa tu nombre",
                        help = "Tu nombre.",

                        // Muestra el error si existe
                        error = state.errors["firstName"],

                        // Ocupa la mitad del espacio disponible
                        modifier = Modifier.weight(1f)
                    )


                    // =========================================
                    // CAMPO DE APELLIDOS
                    // =========================================

                    AuthField(

                        title = "Apellidos",

                        value = state.lastName,

                        // Actualiza los apellidos
                        onValueChange = { value ->

                            onChange {

                                it.copy(
                                    lastName = value
                                )
                            }
                        },

                        hint = "Ingresa tus apellidos",
                        help = "Tus dos apellidos.",

                        error = state.errors["lastName"],

                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(17.dp))
            }


            // =================================================
            // 9. CAMPO DE TELÉFONO
            // =================================================

            item {

                /**
                 * Permite ingresar un número celular.
                 *
                 * Filtra los caracteres para conservar
                 * únicamente los dígitos.
                 *
                 * El formato se valida posteriormente
                 * mediante AuthValidation.
                 */
                AuthField(

                    title = "Teléfono",

                    value = state.phone,

                    // Actualiza el teléfono
                    onValueChange = { value ->

                        onChange {

                            it.copy(

                                // Conserva solamente números
                                phone = value.filter(
                                    Char::isDigit
                                )
                            )
                        }
                    },

                    hint = "3001234567",

                    help = "Celular colombiano de 10 dígitos.",

                    // Error de validación del teléfono
                    error = state.errors["phone"],

                    // Muestra teclado telefónico
                    keyboardType = KeyboardType.Phone,

                    // Limita el campo a 10 caracteres
                    maxChars = 10
                )

                Spacer(Modifier.height(17.dp))
            }


            // =================================================
            // 10. CAMPO DE CORREO ELECTRÓNICO
            // =================================================

            item {

                /**
                 * Permite registrar el correo
                 * que utilizará el administrador
                 * para iniciar sesión.
                 */
                AuthField(

                    title = "Correo electrónico",

                    value = state.email,

                    // Actualiza el correo electrónico
                    onValueChange = { value ->

                        onChange {

                            it.copy(
                                email = value
                            )
                        }
                    },

                    hint = "admin@empresa.com",

                    help =
                        "Con este correo entrarás a la aplicación.",

                    error = state.errors["email"],

                    // Utiliza teclado para correo electrónico
                    keyboardType = KeyboardType.Email
                )

                Spacer(Modifier.height(17.dp))
            }


            // =================================================
            // 11. CAMPO DE CONTRASEÑA
            // =================================================

            item {

                /**
                 * Permite crear una contraseña.
                 *
                 * El usuario puede mostrarla
                 * u ocultarla mediante PasswordEye.
                 *
                 * Las reglas de seguridad se verifican
                 * posteriormente en AuthValidation.
                 */
                AuthField(

                    title = "Contraseña",

                    value = state.password,

                    // Actualiza la contraseña
                    onValueChange = { value ->

                        onChange {

                            it.copy(
                                password = value
                            )
                        }
                    },

                    hint = "********",

                    // Requisitos de la contraseña
                    help =
                        "Mínimo 8 caracteres, con mayúscula, " +
                                "minúscula y número.",

                    error = state.errors["password"],

                    // Teclado adecuado para contraseñas
                    keyboardType = KeyboardType.Password,


                    // =========================================
                    // CONTROL DE VISIBILIDAD
                    // =========================================

                    /**
                     * Si showPassword es true,
                     * muestra el texto normalmente.
                     *
                     * Si es false, oculta los caracteres.
                     */
                    visualTransformation =
                        if (showPassword) {

                            VisualTransformation.None

                        } else {

                            PasswordVisualTransformation()
                        },


                    // =========================================
                    // BOTÓN PARA MOSTRAR U OCULTAR
                    // =========================================

                    trailing = {

                        PasswordEye(

                            visible = showPassword,

                            // Cambia el estado de visibilidad
                            onClick = {

                                showPassword = !showPassword
                            }
                        )
                    }
                )

                Spacer(Modifier.height(17.dp))
            }


            // =================================================
            // 12. CAMPO PARA CONFIRMAR CONTRASEÑA
            // =================================================

            item {

                /**
                 * Permite repetir la contraseña.
                 *
                 * La confirmación se valida
                 * comparándola con la contraseña
                 * ingresada anteriormente.
                 */
                AuthField(

                    title = "Confirmar contraseña",

                    value = state.passwordConfirmation,

                    // Actualiza la confirmación
                    onValueChange = { value ->

                        onChange {

                            it.copy(
                                passwordConfirmation = value
                            )
                        }
                    },

                    hint = "Repite tu contraseña",

                    help = "Debe coincidir con la anterior.",

                    error =
                        state.errors["passwordConfirmation"],

                    keyboardType = KeyboardType.Password,


                    // =========================================
                    // VISIBILIDAD DE LA CONFIRMACIÓN
                    // =========================================

                    visualTransformation =
                        if (showConfirmation) {

                            VisualTransformation.None

                        } else {

                            PasswordVisualTransformation()
                        },


                    // =========================================
                    // BOTÓN PARA MOSTRAR U OCULTAR
                    // =========================================

                    trailing = {

                        PasswordEye(

                            visible = showConfirmation,

                            onClick = {

                                showConfirmation = !showConfirmation
                            }
                        )
                    }
                )

                Spacer(Modifier.height(22.dp))
            }


            // =================================================
            // 13. MENSAJE DE ERROR GENERAL
            // =================================================

            /**
             * Solamente se agrega este contenido
             * cuando existe un error de solicitud.
             *
             * Este error puede provenir de un proceso
             * externo al formulario.
             */
            state.requestError?.let { error ->

                item {

                    Text(

                        text = error,

                        color = ErrorText,

                        style = AppTypography.bodyMd,

                        modifier = Modifier.padding(
                            bottom = 12.dp
                        )
                    )
                }
            }


            // =================================================
            // 14. BOTÓN PARA CREAR CUENTA
            // =================================================

            item {

                /**
                 * Permite procesar el registro.
                 *
                 * Cuando isSubmitting es true:
                 *
                 * - Cambia el texto del botón.
                 * - Deshabilita el botón.
                 * - Muestra un indicador de carga.
                 */
                ContinueButton(

                    text =
                        if (isSubmitting) {

                            "Creando cuenta..."

                        } else {

                            "Crear cuenta"
                        },

                    // Evita nuevos envíos durante el proceso
                    enabled = !isSubmitting,

                    // Ejecuta la acción de registro
                    onClick = onCreateAccount
                )


                // =============================================
                // 15. INDICADOR DE CARGA
                // =============================================

                /**
                 * Muestra un indicador circular
                 * mientras se procesa la solicitud.
                 */
                if (isSubmitting) {

                    Spacer(Modifier.height(12.dp))

                    // Box permite centrar el indicador
                    Box(

                        modifier = Modifier.fillMaxWidth(),

                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }
                }

                Spacer(Modifier.height(20.dp))
            }


            // =================================================
            // 16. SEPARADOR DE AUTENTICACIÓN
            // =================================================

            item {

                /**
                 * Muestra una separación visual
                 * entre el formulario tradicional
                 * y la autenticación con Google.
                 */
                GoogleDivider()

                Spacer(Modifier.height(16.dp))
            }


            // =================================================
            // 17. BOTÓN DE GOOGLE
            // =================================================

            item {

                /**
                 * Permite ejecutar la acción
                 * para continuar con Google.
                 *
                 * La autenticación real depende
                 * de la implementación de onGoogle.
                 */
                GoogleButton(
                    onClick = onGoogle
                )

                // Espacio inferior del formulario
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}


// ============================================================
// 18. VISTA PREVIA PARA ANDROID STUDIO
// ============================================================

/**
 * Permite visualizar el segundo paso del registro
 * directamente en Android Studio.
 *
 * Configuración:
 *
 * - Nombre: Crear cuenta - Paso 2.
 * - Fondo visible.
 * - Ancho: 390 dp.
 * - Alto: 844 dp.
 *
 * Los campos se muestran inicialmente vacíos
 * utilizando AuthUiState.
 */
@Preview(
    name = "Crear cuenta - Paso 2",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun RegisterStep2HelpPreview() {

    // Aplica el tema visual de Café Smart
    CafesmartTheme {

        // Muestra la pantalla de registro
        RegisterStep2Screen(

            // Estado inicial del segundo paso
            state = AuthUiState(
                page = AuthPage.REGISTER_STEP_2
            ),

            // Funciones vacías utilizadas
            // exclusivamente para la vista previa
            onChange = { _ -> },

            onBack = {},

            onCreateAccount = {},

            onGoogle = {},

            onHelp = {},

            onSupport = {}
        )
    }
}
