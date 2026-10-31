
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite mostrar imágenes y aplicar fondos
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

// Componentes para organizar la interfaz
import androidx.compose.foundation.layout.*

// Permite crear listas verticales desplazables
import androidx.compose.foundation.lazy.LazyColumn

// Permite hacer clic en toda una fila seleccionable
import androidx.compose.foundation.selection.toggleable

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Componentes de Material Design 3
import androidx.compose.material3.*

// Componentes y estados de Jetpack Compose
import androidx.compose.runtime.*

// Conserva valores durante recreaciones compatibles
import androidx.compose.runtime.saveable.rememberSaveable

// Permite alinear elementos
import androidx.compose.ui.Alignment

// Permite modificar tamaño y apariencia
import androidx.compose.ui.Modifier

// Permite aplicar sombras
import androidx.compose.ui.draw.shadow

// Permite cargar imágenes de los recursos
import androidx.compose.ui.res.painterResource

// Define roles de accesibilidad
import androidx.compose.ui.semantics.Role

// Permite configurar el grosor de la fuente
import androidx.compose.ui.text.font.FontWeight

// Tipos de teclado y visibilidad de contraseñas
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

// Permite centrar el texto
import androidx.compose.ui.text.style.TextAlign

// Permite visualizar la pantalla en Android Studio
import androidx.compose.ui.tooling.preview.Preview

// Unidades utilizadas en Compose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Recursos gráficos de Café Smart
import com.cafesmart.app.R

// Botón reutilizable de la aplicación
import com.cafesmart.app.ui.components.ContinueButton

// Colores y tipografías del proyecto
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. PANTALLA DE INICIO DE SESIÓN
// ============================================================

/**
 * LoginScreen representa la pantalla
 * de inicio de sesión de Café Smart.
 *
 * Permite:
 *
 * - Ingresar correo electrónico.
 * - Ingresar contraseña.
 * - Mostrar u ocultar la contraseña.
 * - Recordar la cuenta.
 * - Recuperar contraseña.
 * - Iniciar sesión.
 * - Continuar con Google.
 * - Crear una cuenta.
 * - Consultar ayuda y contactar soporte.
 * - Salir de la pantalla.
 *
 * Utiliza LazyColumn para permitir
 * el desplazamiento vertical del contenido.
 *
 * Los datos del formulario se reciben
 * mediante AuthUiState.
 */
@Composable
fun LoginScreen(

    // Estado actual del formulario
    state: AuthUiState,

    // Actualiza la información ingresada
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,

    // Acción para iniciar sesión
    onSubmit: () -> Unit,

    // Acción para ir al registro
    onRegister: () -> Unit,

    // Acción para continuar con Google
    onGoogle: () -> Unit,

    // Acción para recuperar contraseña
    onForgotPassword: () -> Unit,

    // Acción para mostrar ayuda
    onHelp: () -> Unit,

    // Acción para contactar soporte
    onSupport: () -> Unit,

    // Acción para salir
    onExit: () -> Unit,

    // Indica si se está procesando el inicio de sesión
    isSubmitting: Boolean = false
) {


    // ========================================================
    // 2. ESTADO DE VISIBILIDAD DE LA CONTRASEÑA
    // ========================================================

    /**
     * Controla si la contraseña se muestra
     * como texto normal o como caracteres ocultos.
     *
     * rememberSaveable conserva este valor durante
     * recomposiciones y recreaciones compatibles.
     *
     * Inicialmente la contraseña está oculta.
     */
    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }


    // ========================================================
    // 3. CONTENEDOR PRINCIPAL CON LAZYCOLUMN
    // ========================================================

    /**
     * LazyColumn organiza el contenido
     * de forma vertical y desplazable.
     *
     * Reemplaza el uso anterior de:
     * Column + verticalScroll().
     *
     * Se mantienen dos secciones principales:
     *
     * 1. Encabezado de Café Smart.
     * 2. Tarjeta del formulario.
     *
     * Esto conserva el diseño original.
     */
    LazyColumn(

        modifier = Modifier

            // Ocupa toda la pantalla
            .fillMaxSize()

            // Aplica el fondo de la aplicación
            .background(SurfaceScreen)

            // Respeta las barras del sistema Android
            .windowInsetsPadding(WindowInsets.safeDrawing)

            // Agrega espacio cuando aparece el teclado
            .imePadding(),

        // Márgenes del contenido
        contentPadding = PaddingValues(20.dp),

        // Separación entre encabezado y formulario
        verticalArrangement =
            Arrangement.spacedBy(26.dp)

    ) {


        // ====================================================
        // 4. ENCABEZADO DE CAFÉ SMART
        // ====================================================

        item(key = "login_header") {

            /**
             * Muestra la identidad de Café Smart:
             *
             * - Logo.
             * - Nombre de la aplicación.
             * - Botón para salir.
             */
            Row(

                modifier = Modifier

                    // Ocupa todo el ancho
                    .fillMaxWidth()

                    // Aplica una sombra suave
                    .shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Fondo blanco o definido por el tema
                    .background(
                        SurfaceCard,
                        RoundedCornerShape(16.dp)
                    )

                    // Espacio interno
                    .padding(12.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                // ============================================
                // 5. LOGO DEL ENCABEZADO
                // ============================================

                Image(

                    // Imagen almacenada en drawable
                    painter = painterResource(
                        R.drawable.splash_cafe
                    ),

                    contentDescription = "Logo Café Smart",

                    // Tamaño del logo
                    modifier = Modifier.size(32.dp)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )


                // ============================================
                // 6. NOMBRE DE LA APLICACIÓN
                // ============================================

                Text(

                    text = "Café Smart",

                    style = AppTypography.titleScreen.copy(
                        fontSize = 20.sp
                    ),

                    // Utiliza el espacio restante
                    modifier = Modifier.weight(1f)
                )


                // ============================================
                // 7. BOTÓN SALIR
                // ============================================

                /**
                 * Ejecuta la función onExit.
                 *
                 * La navegación o salida real
                 * depende de la pantalla que lo utiliza.
                 */
                TextButton(

                    onClick = onExit,

                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = ErrorBg
                        )
                ) {

                    Text(

                        text = "Salir",

                        color = ErrorText,

                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }


        // ====================================================
        // 8. TARJETA PRINCIPAL DE INICIO DE SESIÓN
        // ====================================================

        item(key = "login_form") {

            /**
             * Esta tarjeta agrupa todos los controles
             * del formulario de inicio de sesión.
             *
             * Se conserva como un único elemento
             * para mantener el fondo, las esquinas
             * y la sombra del diseño original.
             */
            Column(

                modifier = Modifier

                    // Ocupa el ancho disponible
                    .fillMaxWidth()

                    // Sombra de la tarjeta
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Fondo de la tarjeta
                    .background(
                        SurfaceCard,
                        RoundedCornerShape(14.dp)
                    )

                    // Márgenes internos
                    .padding(
                        horizontal = 22.dp,
                        vertical = 30.dp
                    ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                // ============================================
                // 9. LOGO PRINCIPAL
                // ============================================

                /**
                 * Muestra el logo principal
                 * de Café Smart dentro del formulario.
                 */
                Image(

                    painter = painterResource(
                        R.drawable.splash_cafe
                    ),

                    contentDescription = "Logo de Café Smart",

                    modifier = Modifier.size(138.dp)
                )


                // ============================================
                // 10. TÍTULO DEL FORMULARIO
                // ============================================

                Text(

                    text = "Iniciar Sesión",

                    style = AppTypography.titleExito,

                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                // ============================================
                // 11. MENSAJE DE BIENVENIDA
                // ============================================

                Text(

                    text = "Bienvenido a Café Smart",

                    style = AppTypography.bodyMd,

                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )


                // ============================================
                // 12. CAMPO DE CORREO ELECTRÓNICO
                // ============================================

                /**
                 * Permite ingresar el correo
                 * con el cual se registró el usuario.
                 *
                 * La validación se realiza
                 * mediante AuthViewModel.
                 */
                AuthField(

                    title = "Correo electrónico",

                    // Correo actual
                    value = state.loginEmail,

                    // Actualiza el correo
                    onValueChange = { value ->

                        onChange { currentState ->

                            currentState.copy(
                                loginEmail = value
                            )
                        }
                    },

                    // Texto de ejemplo
                    hint = "ejemplo@correo.com",

                    // Ayuda del campo
                    help = "El correo con el que te registraste.",

                    // Error de validación
                    error = state.errors["loginEmail"],

                    // Teclado adecuado para correos
                    keyboardType = KeyboardType.Email
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )


                // ============================================
                // 13. CAMPO DE CONTRASEÑA
                // ============================================

                /**
                 * Permite ingresar la contraseña.
                 *
                 * Por defecto, los caracteres
                 * permanecen ocultos.
                 *
                 * PasswordEye permite mostrar
                 * u ocultar la contraseña.
                 */
                AuthField(

                    title = "Contraseña",

                    // Contraseña actual
                    value = state.loginPassword,

                    // Actualiza la contraseña
                    onValueChange = { value ->

                        onChange { currentState ->

                            currentState.copy(
                                loginPassword = value
                            )
                        }
                    },

                    hint = "********",

                    help = "La que creaste al registrarte.",

                    // Muestra los errores del formulario
                    error = state.errors["loginPassword"],

                    keyboardType = KeyboardType.Password,


                    // ========================================
                    // 14. VISIBILIDAD DE CONTRASEÑA
                    // ========================================

                    visualTransformation =

                        if (showPassword) {

                            // Muestra los caracteres
                            VisualTransformation.None

                        } else {

                            // Oculta los caracteres
                            PasswordVisualTransformation()
                        },


                    // ========================================
                    // 15. BOTÓN MOSTRAR / OCULTAR
                    // ========================================

                    trailing = {

                        PasswordEye(

                            visible = showPassword,

                            onClick = {

                                // Invierte la visibilidad
                                showPassword = !showPassword
                            }
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                // ============================================
                // 16. ENLACE RECUPERAR CONTRASEÑA
                // ============================================

                /**
                 * Coloca el enlace alineado
                 * a la derecha del formulario.
                 */
                Box(

                    modifier = Modifier.fillMaxWidth(),

                    contentAlignment = Alignment.CenterEnd
                ) {

                    AuthLink(

                        text = "¿Olvidaste tu contraseña?",

                        onClick = onForgotPassword
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // ============================================
                // 17. RECORDAR MI CUENTA
                // ============================================

                /**
                 * Permite activar o desactivar
                 * la preferencia de recordar la cuenta.
                 *
                 * La fila completa se puede pulsar.
                 *
                 * La persistencia de esta preferencia
                 * depende de la implementación externa.
                 */
                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        // Permite pulsar el texto o la casilla
                        .toggleable(

                            value = state.rememberMe,

                            role = Role.Checkbox,

                            onValueChange = { checked ->

                                onChange { currentState ->

                                    currentState.copy(
                                        rememberMe = checked
                                    )
                                }
                            }
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Checkbox(

                        checked = state.rememberMe,

                        // La fila controla el clic
                        // para evitar acciones duplicadas
                        onCheckedChange = null,

                        colors = CheckboxDefaults.colors(
                            checkedColor = Primary
                        )
                    )

                    Text(

                        text = "Recordar mi cuenta",

                        style = AppTypography.bodyMd
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                // ============================================
                // 18. BOTÓN INICIAR SESIÓN
                // ============================================

                /**
                 * Ejecuta onSubmit cuando
                 * el usuario intenta iniciar sesión.
                 *
                 * Si isSubmitting es true:
                 *
                 * - Cambia el texto del botón.
                 * - Deshabilita nuevos intentos.
                 */
                ContinueButton(

                    text =

                        if (isSubmitting) {

                            "Ingresando..."

                        } else {

                            "Entrar"
                        },

                    enabled = !isSubmitting,

                    onClick = onSubmit
                )


                // ============================================
                // 19. INDICADOR DE CARGA
                // ============================================

                /**
                 * Muestra un indicador mientras
                 * se procesa el inicio de sesión.
                 */
                if (isSubmitting) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    CircularProgressIndicator(
                        color = Primary
                    )
                }

                Spacer(
                    modifier = Modifier.height(38.dp)
                )


                // ============================================
                // 20. SEPARADOR PARA GOOGLE
                // ============================================

                /**
                 * Divide visualmente el formulario
                 * tradicional de la opción Google.
                 */
                GoogleDivider()

                Spacer(
                    modifier = Modifier.height(22.dp)
                )


                // ============================================
                // 21. BOTÓN CONTINUAR CON GOOGLE
                // ============================================

                /**
                 * Ejecuta la acción de Google
                 * cuando el usuario pulsa el botón.
                 *
                 * La conexión real depende
                 * de la implementación de onGoogle.
                 */
                GoogleButton(

                    onClick = onGoogle
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )


                // ============================================
                // 22. ENLACE PARA CREAR UNA CUENTA
                // ============================================

                /**
                 * Permite navegar desde el inicio
                 * de sesión hacia el registro.
                 *
                 * Se utiliza Column para evitar
                 * que el texto y el enlace se corten
                 * en pantallas de poco ancho.
                 */
                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(

                        text = "¿No tienes una cuenta?",

                        style = AppTypography.bodyMd,

                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    AuthLink(

                        text = "Regístrate gratis",

                        onClick = onRegister
                    )
                }

                Spacer(
                    modifier = Modifier.height(25.dp)
                )


                // ============================================
                // 23. SECCIÓN DE AYUDA
                // ============================================

                Text(

                    text = "¿Problemas para entrar?",

                    style = AppTypography.caption.copy(
                        color = TextPrimary
                    )
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // ============================================
                // 24. ENLACES DE AYUDA Y SOPORTE
                // ============================================

                /**
                 * Presenta las opciones para:
                 *
                 * - Consultar instrucciones.
                 * - Contactar soporte.
                 */
                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.CenterHorizontally
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    AuthLink(

                        text = "Ver ayuda",

                        onClick = onHelp
                    )

                    AuthLink(

                        text = "Contactar soporte",

                        onClick = onSupport
                    )
                }
            }
        }
    }
}


// ============================================================
// 25. VISTA PREVIA PARA ANDROID STUDIO
// ============================================================

/**
 * Permite visualizar la pantalla
 * de inicio de sesión en Android Studio.
 *
 * Configuración:
 *
 * - Nombre: Login Café Smart.
 * - Ancho: 390 dp.
 * - Alto: 844 dp.
 * - Fondo visible.
 *
 * Utiliza un estado inicial con los campos vacíos.
 */
@Preview(
    name = "Login Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun LoginCafeSmartPreview() {

    // Aplica el tema visual de Café Smart
    CafesmartTheme {

        LoginScreen(

            // Estado inicial
            state = AuthUiState(),

            // Acciones vacías para la vista previa
            onChange = { _ -> },

            onSubmit = {},

            onRegister = {},

            onGoogle = {},

            onForgotPassword = {},

            onHelp = {},

            onSupport = {},

            onExit = {}
        )
    }
}
