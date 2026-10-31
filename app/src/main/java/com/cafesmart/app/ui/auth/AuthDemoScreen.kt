
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// Esta documento se eliminara cuando se usen datos reales
// ============================================================

// Permite organizar los elementos de la interfaz
// y controlar el tamaño de los contenedores
import androidx.compose.foundation.layout.*

// Importa los componentes de Material Design 3,
// incluyendo SnackbarHost y SnackbarHostState
import androidx.compose.material3.*

// Herramientas de Compose para recordar estados
// y trabajar con funciones @Composable
import androidx.compose.runtime.*

// Permite alinear componentes dentro de un Box
import androidx.compose.ui.Alignment

// Permite modificar tamaño, posición y apariencia
import androidx.compose.ui.Modifier

// Permite visualizar la pantalla en Android Studio
// sin ejecutar la aplicación en el emulador
import androidx.compose.ui.tooling.preview.Preview

// Importa el tema visual de Café Smart
import com.cafesmart.app.ui.theme.CafesmartTheme

// Permite ejecutar corrutinas para mostrar mensajes
import kotlinx.coroutines.launch


// ============================================================
// 1. PANTALLA DE DEMOSTRACIÓN DE AUTENTICACIÓN
// ============================================================

/**
 * Pantalla utilizada para probar visualmente
 * el flujo de autenticación de Café Smart.
 *
 * Permite probar:
 * - Inicio de sesión.
 * - Registro de una cuenta.
 * - Continuar con Google.
 * - Recuperación de contraseña.
 * - Ayuda.
 * - Soporte.
 * - Salida del modo de prueba.
 *
 * IMPORTANTE:
 * Esta pantalla es una demostración.
 * No realiza autenticación real ni guarda
 * información en una base de datos.
 */
@Composable
fun AuthDemoScreen() {

    // ========================================================
    // 2. ESTADO PARA MOSTRAR MENSAJES
    // ========================================================

    /**
     * SnackbarHostState administra los mensajes
     * temporales que aparecen en la pantalla.
     *
     * remember permite conservar este estado
     * durante las recomposiciones de Compose.
     */
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    /**
     * Crea un alcance de corrutinas asociado
     * al ciclo de vida de este componente.
     *
     * Se utiliza para ejecutar showSnackbar,
     * que es una función suspendida.
     */
    val scope = rememberCoroutineScope()


    // ========================================================
    // 3. FUNCIÓN PARA MOSTRAR MENSAJES
    // ========================================================

    /**
     * Recibe un mensaje y lo muestra temporalmente
     * en la parte inferior de la pantalla.
     *
     * Parámetro:
     * mensaje: texto que verá el usuario.
     *
     * Esta función permite reutilizar la misma
     * lógica para todas las acciones de prueba.
     */
    fun mostrarMensaje(mensaje: String) {

        // Ejecuta una corrutina para mostrar el mensaje
        scope.launch {

            // Muestra el mensaje mediante un Snackbar
            snackbarHostState.showSnackbar(mensaje)
        }
    }


    // ========================================================
    // 4. CONTENEDOR PRINCIPAL DE LA PANTALLA
    // ========================================================

    /**
     * Box permite colocar componentes
     * dentro de un mismo espacio.
     *
     * En este caso contiene:
     * - El flujo completo de autenticación.
     * - El componente que muestra los mensajes.
     */
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ====================================================
        // 5. FLUJO DE AUTENTICACIÓN
        // ====================================================

        /**
         * AuthFlow contiene las pantallas
         * y acciones relacionadas con la autenticación.
         *
         * Cada parámetro recibe una función que
         * se ejecuta cuando ocurre una acción.
         *
         * En esta demostración las acciones
         * únicamente muestran mensajes informativos.
         */
        AuthFlow(

            // ------------------------------------------------
            // INICIO DE SESIÓN
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario intenta
             * iniciar sesión.
             *
             * En esta prueba solamente informa
             * que se realizó la acción.
             */
            onLogin = {

                mostrarMensaje(
                    "Inicio de sesión de prueba."
                )
            },


            // ------------------------------------------------
            // REGISTRO DE UNA CUENTA
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario
             * completa el formulario de registro.
             *
             * Muestra un mensaje de demostración.
             * No crea una cuenta real.
             */
            onRegister = {

                mostrarMensaje(
                    "Formulario validado. Registro de prueba."
                )
            },


            // ------------------------------------------------
            // AUTENTICACIÓN CON GOOGLE
            // ------------------------------------------------

            /**
             * Se ejecuta al seleccionar
             * la opción de continuar con Google.
             *
             * Actualmente informa que la
             * integración se encuentra pendiente.
             */
            onGoogle = {

                mostrarMensaje(
                    "Google pendiente de integración."
                )
            },


            // ------------------------------------------------
            // RECUPERACIÓN DE CONTRASEÑA
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario
             * solicita recuperar su contraseña.
             *
             * Muestra una indicación para
             * probar el proceso en la aplicación.
             */
            onForgotPassword = {

                mostrarMensaje(
                    "Abre la aplicación para probar recuperación."
                )
            },


            // ------------------------------------------------
            // OPCIÓN DE AYUDA
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario
             * selecciona la opción de ayuda.
             *
             * Muestra un mensaje indicando
             * dónde probar esa funcionalidad.
             */
            onHelp = {

                mostrarMensaje(
                    "Abre la aplicación para probar ayuda."
                )
            },


            // ------------------------------------------------
            // OPCIÓN DE SOPORTE
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario
             * selecciona contactar soporte.
             *
             * En esta demostración muestra
             * un mensaje informativo.
             */
            onSupport = {

                mostrarMensaje(
                    "Abre la aplicación para probar soporte."
                )
            },


            // ------------------------------------------------
            // SALIR DEL MODO DE PRUEBA
            // ------------------------------------------------

            /**
             * Se ejecuta cuando el usuario
             * solicita salir del flujo de prueba.
             *
             * Actualmente solo muestra un mensaje,
             * sin cerrar la aplicación.
             */
            onExit = {

                mostrarMensaje(
                    "Salir del modo de prueba."
                )
            }
        )


        // ====================================================
        // 6. COMPONENTE PARA MOSTRAR MENSAJES
        // ====================================================

        /**
         * SnackbarHost muestra los mensajes
         * generados por mostrarMensaje().
         *
         * Se ubica en la parte inferior y centrada
         * de la pantalla.
         */
        SnackbarHost(

            // Utiliza el estado definido anteriormente
            hostState = snackbarHostState,

            // Ubica el mensaje en la parte inferior
            modifier = Modifier.align(
                Alignment.BottomCenter
            )
        )
    }
}


// ============================================================
// 7. VISTA PREVIA PARA ANDROID STUDIO
// ============================================================

/**
 * Permite visualizar AuthDemoScreen
 * directamente en Android Studio.
 *
 * No necesita ejecutar la aplicación
 * en un emulador para mostrar el diseño.
 *
 * Configuración:
 * - Nombre: Autenticación Café Smart.
 * - Fondo visible.
 * - Ancho: 390 dp.
 * - Alto: 844 dp.
 *
 * Estas dimensiones permiten revisar
 * la distribución de la interfaz móvil.
 */
@Preview(
    name = "Autenticación Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartAuthenticationPreview() {

    // Aplica los colores y estilos de Café Smart
    CafesmartTheme {

        // Muestra la pantalla completa de demostración
        AuthDemoScreen()
    }
}
