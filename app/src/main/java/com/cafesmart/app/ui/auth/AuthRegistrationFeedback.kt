
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite crear componentes de interfaz con Jetpack Compose
import androidx.compose.runtime.Composable

// Permite visualizar la pantalla en Android Studio
// sin ejecutar la aplicación en un emulador
import androidx.compose.ui.tooling.preview.Preview

// Componente reutilizable que muestra una pantalla de éxito
import com.cafesmart.app.ui.components.AppSuccessScreen

// Modelo que permite mostrar información
// dentro del resumen del registro
import com.cafesmart.app.ui.components.FeedbackDetail

// Importa el tema visual de Café Smart
import com.cafesmart.app.ui.theme.CafesmartTheme


// ============================================================
// 1. PANTALLA DE REGISTRO EXITOSO
// ============================================================

/**
 * RegistrationSuccessScreen muestra el resultado
 * del proceso de creación de cuenta en Café Smart.
 *
 * Esta pantalla permite:
 *
 * - Informar si el registro fue exitoso.
 * - Diferenciar entre registro real y demostración.
 * - Mostrar un resumen de los datos registrados.
 * - Navegar a la pantalla de inicio.
 * - Regresar al formulario de registro.
 *
 * Utiliza AppSuccessScreen para mantener
 * un diseño reutilizable y consistente.
 *
 * Parámetros:
 *
 * registration:
 * Contiene la información del negocio
 * y del administrador registrado.
 *
 * isDemo:
 * Indica si el proceso es una demostración.
 *
 * onGoHome:
 * Acción para ir a la pantalla de inicio.
 *
 * onBackToRegister:
 * Acción para regresar al registro.
 */
@Composable
fun RegistrationSuccessScreen(

    // Información completa del registro
    registration: RegistrationData,

    // Indica si es un registro de demostración
    isDemo: Boolean,

    // Acción para ir al inicio de la aplicación
    onGoHome: () -> Unit,

    // Acción para regresar al formulario
    onBackToRegister: () -> Unit
) {


    // ========================================================
    // 2. COMPONENTE PRINCIPAL DE CONFIRMACIÓN
    // ========================================================

    /**
     * AppSuccessScreen es un componente reutilizable
     * que se encarga de presentar:
     *
     * - Título.
     * - Mensaje de confirmación.
     * - Resumen de información.
     * - Botón principal.
     * - Botón secundario.
     *
     * La organización visual y el desplazamiento
     * del contenido dependen de AppSuccessScreen.
     */
    AppSuccessScreen(


        // ====================================================
        // 3. TÍTULO DE LA PANTALLA
        // ====================================================

        /**
         * El título depende del tipo de registro.
         *
         * Si isDemo es true:
         * Informa que terminó un registro de prueba.
         *
         * Si isDemo es false:
         * Muestra un mensaje de cuenta creada.
         *
         * El componente no comprueba por sí mismo
         * si la cuenta existe en el servidor.
         */
        title =

            if (isDemo) {

                "Registro de prueba completado"

            } else {

                "¡Cuenta creada con éxito!"
            },


        // ====================================================
        // 4. MENSAJE DE CONFIRMACIÓN
        // ====================================================

        /**
         * Muestra un mensaje explicativo.
         *
         * En modo demostración:
         * Aclara que no se creó una cuenta real.
         *
         * En modo real:
         * Informa que el proceso se completó.
         */
        message =

            if (isDemo) {

                "Los datos del formulario son válidos. " +
                        "Esta es una demostración: " +
                        "no se creó una cuenta real."

            } else {

                "Tu cuenta se creó correctamente. " +
                        "Ya puedes continuar a Inicio."
            },


        // ====================================================
        // 5. TÍTULO DEL RESUMEN
        // ====================================================

        /**
         * Identifica la sección donde se muestran
         * los principales datos del registro.
         */
        summaryTitle = "Resumen del registro",


        // ====================================================
        // 6. LISTA DE DATOS DEL REGISTRO
        // ====================================================

        /**
         * Crea una lista con los datos que
         * deben aparecer en el resumen.
         *
         * Cada FeedbackDetail tiene:
         *
         * label:
         * Nombre del dato que se muestra.
         *
         * value:
         * Información correspondiente.
         *
         * No se muestra la contraseña porque
         * es información sensible.
         */
        details = listOf(


            // ------------------------------------------------
            // 7. NOMBRE DEL NEGOCIO
            // ------------------------------------------------

            FeedbackDetail(

                // Nombre del campo
                label = "Nombre del negocio",

                // Nombre ingresado en el paso 1
                value = registration.businessName
            ),


            // ------------------------------------------------
            // 8. TIPO DE NEGOCIO
            // ------------------------------------------------

            FeedbackDetail(

                label = "Tipo de negocio",

                /**
                 * Obtiene la etiqueta visible
                 * del tipo de negocio.
                 *
                 * Por ejemplo:
                 * - Compraventa.
                 * - Cooperativa.
                 * - Personalizado.
                 */
                value = registration.businessType.label
            ),


            // ------------------------------------------------
            // 9. NOMBRE DEL ADMINISTRADOR
            // ------------------------------------------------

            FeedbackDetail(

                label = "Administrador",

                /**
                 * Une el nombre y los apellidos
                 * del administrador.
                 *
                 * trim() elimina espacios sobrantes
                 * al comienzo y al final.
                 */
                value = (
                        registration.firstName + " " +
                                registration.lastName
                        ).trim()
            ),


            // ------------------------------------------------
            // 10. CORREO ELECTRÓNICO
            // ------------------------------------------------

            FeedbackDetail(

                label = "Correo electrónico",

                /**
                 * Muestra el correo que utilizará
                 * el administrador para iniciar sesión.
                 */
                value = registration.email
            )
        ),


        // ====================================================
        // 11. TEXTOS DE LOS BOTONES
        // ====================================================

        /**
         * Texto del botón principal.
         *
         * Permite continuar hacia el inicio.
         */
        primaryButtonText = "Ir a Inicio",

        /**
         * Texto del botón secundario.
         *
         * Permite regresar al formulario.
         */
        secondaryButtonText = "Volver al registro",


        // ====================================================
        // 12. ACCIONES DE LOS BOTONES
        // ====================================================

        /**
         * Ejecuta la acción correspondiente
         * cuando se pulsa el botón "Ir a Inicio".
         */
        onPrimaryClick = onGoHome,

        /**
         * Ejecuta la acción correspondiente
         * cuando se pulsa "Volver al registro".
         */
        onSecondaryClick = onBackToRegister
    )
}


// ============================================================
// 13. VISTA PREVIA PARA ANDROID STUDIO
// ============================================================

/**
 * Permite visualizar RegistrationSuccessScreen
 * directamente desde Android Studio.
 *
 * No necesita realizar un registro real.
 *
 * Utiliza datos ficticios para mostrar
 * cómo se verá el resumen.
 *
 * Configuración:
 *
 * - Nombre: Registro exitoso Café Smart.
 * - Ancho: 390 dp.
 * - Alto: 844 dp.
 * - Fondo visible.
 */
@Preview(
    name = "Registro exitoso Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartRegistrationSuccessPreview() {


    // ========================================================
    // 14. APLICACIÓN DEL TEMA
    // ========================================================

    /**
     * Aplica los estilos, colores y tipografías
     * definidos en el proyecto Café Smart.
     */
    CafesmartTheme {


        // ====================================================
        // 15. VISUALIZACIÓN DEL REGISTRO DE PRUEBA
        // ====================================================

        RegistrationSuccessScreen(


            // ================================================
            // 16. DATOS FICTICIOS DEL REGISTRO
            // ================================================

            /**
             * RegistrationData almacena
             * la información del negocio
             * y del administrador.
             *
             * Estos datos únicamente se utilizan
             * para la vista previa.
             */
            registration = RegistrationData(

                // Nombre del negocio ficticio
                businessName = "Café Los Alpes",

                // Tipo de negocio seleccionado
                businessType = BusinessType.COMPRAVENTA,

                // Nombre del administrador
                firstName = "María",

                // Apellidos del administrador
                lastName = "Gómez",

                // Teléfono de ejemplo
                phone = "3001234567",

                // Correo de ejemplo
                email = "ejemplo@correo.com",

                // Contraseña ficticia utilizada
                // solamente para construir el modelo
                password = "Ejemplo123"
            ),


            // ================================================
            // 17. MODO DEMOSTRACIÓN
            // ================================================

            /**
             * true indica que el registro
             * es solamente una demostración.
             *
             * Por eso se mostrará el mensaje
             * "Registro de prueba completado".
             */
            isDemo = true,


            // ================================================
            // 18. ACCIONES DE LA VISTA PREVIA
            // ================================================

            /**
             * Se utilizan funciones vacías porque
             * la vista previa no necesita navegar
             * hacia otras pantallas.
             */
            onGoHome = {},

            onBackToRegister = {}
        )
    }
}
