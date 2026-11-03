
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite configurar tamaños, espacios y márgenes
import androidx.compose.foundation.layout.*

// Permite crear contenido vertical desplazable
import androidx.compose.foundation.lazy.LazyColumn

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Permite configurar el teclado del formulario
import androidx.compose.foundation.text.KeyboardOptions

// Componentes visuales de Material Design 3
import androidx.compose.material3.*

// Permite crear funciones de interfaz
import androidx.compose.runtime.Composable

// Herramientas para administrar los estados
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Conserva el correo durante recreaciones compatibles
import androidx.compose.runtime.saveable.rememberSaveable

// Permite modificar la apariencia de los componentes
import androidx.compose.ui.Modifier

// Permite utilizar el teclado de correo electrónico
import androidx.compose.ui.text.input.KeyboardType

// Unidad de medida utilizada por Compose
import androidx.compose.ui.unit.dp

// Importa los colores y tipografías de Café Smart
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. VALIDACIÓN DEL CORREO ELECTRÓNICO
// ============================================================

/**
 * Expresión regular para comprobar el formato
 * básico de una dirección de correo electrónico.
 *
 * Verifica que:
 *
 * - Exista texto antes del símbolo @.
 * - Exista un dominio después del símbolo @.
 * - El dominio contenga un punto.
 * - No existan espacios en blanco.
 *
 * IMPORTANTE:
 * Esta validación no comprueba si el correo existe.
 *
 * Se utiliza una cadena de texto de tres comillas
 * para evitar problemas con las barras invertidas.
 */
private val recoveryEmailRegex =
    Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")


// ============================================================
// 2. VENTANA DE RECUPERACIÓN DE CONTRASEÑA
// ============================================================

/**
 * RecoveryPasswordDialog muestra una ventana
 * para solicitar la recuperación de contraseña.
 *
 * Permite:
 *
 * - Ingresar el correo electrónico.
 * - Validar el formato del correo.
 * - Mostrar mensajes de error.
 * - Cancelar la operación.
 * - Continuar con una solicitud válida.
 *
 * Parámetros:
 *
 * onDismiss:
 * Cierra la ventana de recuperación.
 *
 * onRequest:
 * Recibe el correo válido para procesar
 * la solicitud de recuperación.
 *
 * Este componente no envía correos directamente.
 */
@Composable
fun RecoveryPasswordDialog(

    // Acción para cerrar la ventana
    onDismiss: () -> Unit,

    // Acción para procesar el correo válido
    onRequest: (String) -> Unit

) {


    // ========================================================
    // 3. ESTADO DEL CORREO ELECTRÓNICO
    // ========================================================

    /**
     * Almacena el correo escrito por el usuario.
     *
     * rememberSaveable permite conservar el valor
     * durante recomposiciones y recreaciones
     * compatibles de la interfaz.
     */
    var email by rememberSaveable {
        mutableStateOf("")
    }


    // ========================================================
    // 4. ESTADO DE LOS ERRORES
    // ========================================================

    /**
     * Almacena el mensaje de error del formulario.
     *
     * Si el valor es null, significa que no existe
     * un error que deba mostrarse.
     */
    var error by remember {
        mutableStateOf<String?>(null)
    }


    // ========================================================
    // 5. CREACIÓN DE LA VENTANA EMERGENTE
    // ========================================================

    /**
     * AlertDialog crea una ventana emergente
     * sobre la pantalla actual.
     *
     * Se utiliza para mostrar el formulario
     * de recuperación sin salir de Login.
     */
    AlertDialog(

        // Permite cerrar la ventana
        onDismissRequest = onDismiss,

        // Fondo de la ventana
        containerColor = SurfaceCard,

        // Color predeterminado del título
        titleContentColor = TextPrimary,

        // Color predeterminado del contenido
        textContentColor = TextSecondary,

        // Esquinas redondeadas de 20 dp
        shape = RoundedCornerShape(20.dp),


        // ====================================================
        // 6. TÍTULO DE LA VENTANA
        // ====================================================

        title = {

            Text(

                text = "Recuperar contraseña",

                color = TextPrimary,

                style = AppTypography.titleModal
            )
        },


        // ====================================================
        // 7. CONTENIDO DESPLAZABLE CON LAZYCOLUMN
        // ====================================================

        text = {

            /**
             * LazyColumn reemplaza el uso de Column.
             *
             * Organiza los elementos de forma vertical
             * y permite desplazarse si el contenido
             * no cabe completamente en pantalla.
             *
             * Cada item representa una sección
             * independiente del formulario.
             *
             * heightIn establece una altura máxima
             * para evitar que el contenido ocupe
             * demasiado espacio en el diálogo.
             */
            LazyColumn(

                modifier = Modifier

                    // Ocupa el ancho disponible
                    .fillMaxWidth()

                    // Limita la altura del contenido
                    .heightIn(max = 240.dp),

                // Espacio entre las secciones
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)

            ) {


                // ============================================
                // 8. INSTRUCCIONES PARA EL USUARIO
                // ============================================

                item(key = "recovery_description") {

                    /**
                     * Informa qué correo debe ingresar
                     * el usuario para recuperar su acceso.
                     */
                    Text(

                        text =
                            "Ingresa el correo electrónico " +
                                    "con el que registraste tu cuenta.",

                        style = AppTypography.bodyMd,

                        color = TextSecondary
                    )
                }


                // ============================================
                // 9. CAMPO DE CORREO ELECTRÓNICO
                // ============================================

                item(key = "recovery_email") {

                    /**
                     * OutlinedTextField permite escribir
                     * el correo electrónico.
                     *
                     * El valor se conserva en email.
                     *
                     * Cuando el usuario escribe,
                     * se limpia el error anterior.
                     */
                    OutlinedTextField(

                        // Valor actual del correo
                        value = email,


                        // ====================================
                        // 10. ACTUALIZAR EL CORREO
                        // ====================================

                        onValueChange = { value ->

                            // Actualiza el contenido del campo
                            email = value

                            // Elimina el error mientras corrige
                            error = null
                        },


                        // ====================================
                        // 11. ETIQUETA DEL CAMPO
                        // ====================================

                        label = {

                            Text(
                                text = "Correo electrónico"
                            )
                        },


                        // ====================================
                        // 12. EJEMPLO DE CORREO
                        // ====================================

                        placeholder = {

                            Text(

                                text = "ejemplo@correo.com",

                                color = TextMuted
                            )
                        },


                        // ====================================
                        // 13. CONFIGURACIÓN DEL CAMPO
                        // ====================================

                        // Permite una sola línea de texto
                        singleLine = true,

                        // Marca el campo cuando existe un error
                        isError = error != null,

                        // Muestra un teclado adecuado
                        // para escribir correos electrónicos
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),

                        // El campo ocupa todo el ancho
                        modifier = Modifier.fillMaxWidth(),


                        // ====================================
                        // 14. COLORES DEL CAMPO
                        // ====================================

                        colors = OutlinedTextFieldDefaults.colors(

                            // Color del texto
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,

                            // Color del fondo
                            focusedContainerColor = SurfaceScreen,
                            unfocusedContainerColor = SurfaceScreen,

                            // Color de la etiqueta
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextSecondary,

                            // Color de los bordes
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDefault,

                            // Color del borde cuando existe error
                            errorBorderColor = ErrorText,

                            // Color del cursor
                            cursorColor = Primary
                        )
                    )
                }


                // ============================================
                // 15. MENSAJE DE ERROR
                // ============================================

                /**
                 * Si el correo tiene un formato incorrecto,
                 * aparece un mensaje debajo del campo.
                 *
                 * Cuando error es null, no se agrega
                 * este elemento a LazyColumn.
                 */
                error?.let { message ->

                    item(key = "recovery_error") {

                        Text(

                            text = message,

                            color = ErrorText,

                            style = AppTypography.helper
                        )
                    }
                }


                // ============================================
                // 16. MENSAJE INFORMATIVO
                // ============================================

                item(key = "recovery_information") {

                    /**
                     * Informa que la recuperación
                     * depende de conectar el servicio
                     * real de autenticación.
                     *
                     * Esto evita indicar que se enviará
                     * un correo cuando la integración
                     * todavía no está disponible.
                     */
                    Text(

                        text =
                            "El enlace de recuperación " +
                                    "podrá enviarse cuando se conecte " +
                                    "el servicio de autenticación.",

                        style = AppTypography.helper,

                        color = TextMuted
                    )
                }
            }
        },


        // ====================================================
        // 17. BOTÓN CANCELAR
        // ====================================================

        dismissButton = {

            /**
             * Este botón permanece fuera de LazyColumn
             * para que no se desplace con el contenido.
             *
             * Permite cerrar la ventana sin procesar
             * ninguna solicitud.
             */
            TextButton(

                onClick = onDismiss,

                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )

            ) {

                Text(

                    text = "Cancelar",

                    color = Primary
                )
            }
        },


        // ====================================================
        // 18. BOTÓN CONTINUAR
        // ====================================================

        confirmButton = {

            /**
             * Comprueba el correo antes de ejecutar
             * la función de recuperación.
             *
             * Si el formato es inválido:
             * muestra un mensaje de error.
             *
             * Si el formato es válido:
             * ejecuta onRequest con el correo.
             */
            Button(

                onClick = {


                    // ========================================
                    // 19. LIMPIAR EL CORREO
                    // ========================================

                    /**
                     * trim() elimina espacios
                     * al inicio y al final del correo.
                     */
                    val correo = email.trim()


                    // ========================================
                    // 20. VALIDAR EL CORREO
                    // ========================================

                    if (!recoveryEmailRegex.matches(correo)) {

                        // Muestra el error de validación
                        error = "Ingresa un correo válido."

                    } else {

                        // ====================================
                        // 21. PROCESAR SOLICITUD VÁLIDA
                        // ====================================

                        /**
                         * Entrega el correo a onRequest.
                         *
                         * El envío real del enlace
                         * debe implementarse fuera
                         * de este componente.
                         */
                        onRequest(correo)
                    }
                },


                // ============================================
                // 22. COLORES DEL BOTÓN
                // ============================================

                colors = ButtonDefaults.buttonColors(

                    containerColor = Primary,

                    contentColor = OnPrimary
                )

            ) {

                Text(

                    text = "Continuar",

                    color = OnPrimary
                )
            }
        }
    )
}
