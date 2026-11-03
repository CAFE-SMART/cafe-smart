
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite definir el borde de los botones
import androidx.compose.foundation.BorderStroke

// Componentes para manejar tamaños y espacios
import androidx.compose.foundation.layout.*

// Permite crear listas verticales desplazables
import androidx.compose.foundation.lazy.LazyColumn

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Permite configurar el tipo de teclado
import androidx.compose.foundation.text.KeyboardOptions

// Componentes visuales de Material Design 3
import androidx.compose.material3.*

// Herramientas para manejar los estados de Compose
import androidx.compose.runtime.*

// Conserva los campos ante recreaciones compatibles
import androidx.compose.runtime.saveable.rememberSaveable

// Permite modificar tamaño y apariencia
import androidx.compose.ui.Modifier

// Permite utilizar el teclado de correo electrónico
import androidx.compose.ui.text.input.KeyboardType

// Unidad de medida utilizada por Compose
import androidx.compose.ui.unit.dp

// Colores y tipografías de Café Smart
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. VALIDACIÓN DEL CORREO DE SOPORTE
// ============================================================

/**
 * Expresión regular que comprueba
 * el formato básico de un correo electrónico.
 *
 * Verifica que el correo:
 *
 * - Tenga texto antes del símbolo @.
 * - Contenga un dominio.
 * - Tenga un punto en el dominio.
 * - No contenga espacios en blanco.
 *
 * No comprueba si la dirección existe.
 *
 * Las tres comillas permiten escribir
 * la expresión regular sin duplicar
 * las barras invertidas.
 */
private val supportEmailRegex =
    Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")


// ============================================================
// 2. VENTANA DE AYUDA PARA INICIAR SESIÓN
// ============================================================

/**
 * LoginHelpDialog muestra instrucciones
 * para ingresar a una cuenta de Café Smart.
 *
 * Incluye:
 *
 * - Pasos para iniciar sesión.
 * - Opción de recuperar contraseña.
 * - Opción de contactar soporte.
 * - Botón para cerrar la ventana.
 *
 * Parámetros:
 *
 * onDismiss:
 * Cierra la ventana de ayuda.
 *
 * onRecovery:
 * Ejecuta la acción de recuperar contraseña.
 *
 * onSupport:
 * Ejecuta la acción para contactar soporte.
 */
@Composable
fun LoginHelpDialog(
    onDismiss: () -> Unit,
    onRecovery: () -> Unit,
    onSupport: () -> Unit
) {

    // ========================================================
    // 3. CREACIÓN DE LA VENTANA DE AYUDA
    // ========================================================

    /**
     * AlertDialog muestra una ventana emergente
     * sobre la pantalla de inicio de sesión.
     */
    AlertDialog(

        // Permite cerrar la ventana
        onDismissRequest = onDismiss,

        // Colores definidos en Café Smart
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,

        // Esquinas redondeadas del diálogo
        shape = RoundedCornerShape(20.dp),


        // ====================================================
        // 4. TÍTULO DEL DIÁLOGO
        // ====================================================

        title = {

            Text(
                text = "Ayuda para ingresar",
                color = TextPrimary,
                style = AppTypography.titleModal
            )
        },


        // ====================================================
        // 5. CONTENIDO DESPLAZABLE CON LAZYCOLUMN
        // ====================================================

        text = {

            /**
             * LazyColumn organiza verticalmente
             * las instrucciones y los botones.
             *
             * Reemplaza:
             *
             * Column + verticalScroll().
             *
             * heightIn establece una altura máxima
             * para el contenido desplazable.
             *
             * Los botones de ayuda se encuentran
             * dentro de la lista porque forman
             * parte de las instrucciones.
             */
            LazyColumn(

                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),

                // Separación entre los elementos
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {


                // ============================================
                // 6. ENCABEZADO DE INSTRUCCIONES
                // ============================================

                item(key = "login_help_title") {

                    Text(
                        text = "¿Cómo iniciar sesión?",
                        color = TextPrimary,
                        style = AppTypography.labelField
                    )
                }


                // ============================================
                // 7. PASOS PARA INICIAR SESIÓN
                // ============================================

                item(key = "login_help_steps") {

                    /**
                     * Presenta los pasos que debe seguir
                     * el usuario para ingresar a su cuenta.
                     */
                    Text(

                        text =
                            "1. Ingresa tu correo registrado.\n" +
                                    "2. Escribe tu contraseña.\n" +
                                    "3. Presiona el botón Entrar.",

                        color = TextSecondary,
                        style = AppTypography.bodyMd
                    )
                }


                // ============================================
                // 8. SEPARADOR VISUAL
                // ============================================

                item(key = "login_help_divider_1") {

                    HorizontalDivider(
                        color = BorderDefault
                    )
                }


                // ============================================
                // 9. RECUPERACIÓN DE CONTRASEÑA
                // ============================================

                item(key = "login_help_recovery_title") {

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = TextPrimary,
                        style = AppTypography.labelField
                    )
                }


                // ============================================
                // 10. BOTÓN RECUPERAR CONTRASEÑA
                // ============================================

                item(key = "login_help_recovery_button") {

                    /**
                     * Permite acceder al formulario
                     * para recuperar la contraseña.
                     *
                     * La acción real depende
                     * de onRecovery.
                     */
                    OutlinedButton(

                        onClick = onRecovery,

                        modifier = Modifier.fillMaxWidth(),

                        // Borde del botón
                        border = BorderStroke(
                            width = 1.dp,
                            color = Primary
                        ),

                        // Colores del tema
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = Primary
                            )
                    ) {

                        Text(
                            text = "Recuperar contraseña",
                            color = Primary
                        )
                    }
                }


                // ============================================
                // 11. SEGUNDO SEPARADOR VISUAL
                // ============================================

                item(key = "login_help_divider_2") {

                    HorizontalDivider(
                        color = BorderDefault
                    )
                }


                // ============================================
                // 12. SECCIÓN DE SOPORTE
                // ============================================

                item(key = "login_help_support_title") {

                    Text(
                        text = "¿Sigues teniendo problemas?",
                        color = TextPrimary,
                        style = AppTypography.labelField
                    )
                }


                // ============================================
                // 13. BOTÓN CONTACTAR SOPORTE
                // ============================================

                item(key = "login_help_support_button") {

                    /**
                     * Permite acceder al formulario
                     * para contactar soporte.
                     *
                     * Ejecuta la acción onSupport.
                     */
                    OutlinedButton(

                        onClick = onSupport,

                        modifier = Modifier.fillMaxWidth(),

                        // Borde del botón
                        border = BorderStroke(
                            width = 1.dp,
                            color = Primary
                        ),

                        // Colores del botón
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = Primary
                            )
                    ) {

                        Text(
                            text = "Contactar soporte",
                            color = Primary
                        )
                    }
                }
            }
        },


        // ====================================================
        // 14. BOTÓN PARA CERRAR LA AYUDA
        // ====================================================

        /**
         * El botón Cerrar permanece fuera
         * de LazyColumn.
         *
         * Esto evita que se desplace junto
         * con las instrucciones de ayuda.
         */
        confirmButton = {

            TextButton(

                onClick = onDismiss,

                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )
            ) {

                Text(
                    text = "Cerrar",
                    color = Primary
                )
            }
        }
    )
}


// ============================================================
// 15. VENTANA PARA CONTACTAR SOPORTE
// ============================================================

/**
 * ContactSupportDialog muestra un formulario
 * para solicitar ayuda relacionada con Café Smart.
 *
 * Permite:
 *
 * - Ingresar el correo del usuario.
 * - Describir el problema.
 * - Validar que los campos estén completos.
 * - Preparar un correo de soporte.
 * - Cancelar la operación.
 *
 * Parámetros:
 *
 * onDismiss:
 * Cierra el formulario.
 *
 * onPrepareEmail:
 * Recibe el correo y la descripción del problema.
 *
 * IMPORTANTE:
 * Este componente no envía correos directamente.
 * La acción real depende de onPrepareEmail.
 */
@Composable
fun ContactSupportDialog(
    onDismiss: () -> Unit,
    onPrepareEmail: (String, String) -> Unit
) {


    // ========================================================
    // 16. ESTADO DEL CORREO ELECTRÓNICO
    // ========================================================

    /**
     * Guarda el correo escrito por el usuario.
     *
     * rememberSaveable conserva el valor
     * durante recomposiciones y recreaciones
     * compatibles de la pantalla.
     */
    var email by rememberSaveable {
        mutableStateOf("")
    }


    // ========================================================
    // 17. ESTADO DE LA DESCRIPCIÓN
    // ========================================================

    /**
     * Almacena el problema que describe
     * el usuario en el formulario.
     */
    var description by rememberSaveable {
        mutableStateOf("")
    }


    // ========================================================
    // 18. ESTADO DEL MENSAJE DE ERROR
    // ========================================================

    /**
     * Almacena los errores de validación.
     *
     * Si es null, no se muestra ningún mensaje.
     */
    var error by remember {
        mutableStateOf<String?>(null)
    }


    // ========================================================
    // 19. CREACIÓN DE LA VENTANA DE SOPORTE
    // ========================================================

    AlertDialog(

        // Permite cerrar el formulario
        onDismissRequest = onDismiss,

        // Colores del diálogo
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,

        // Esquinas redondeadas
        shape = RoundedCornerShape(20.dp),


        // ====================================================
        // 20. TÍTULO DEL FORMULARIO
        // ====================================================

        title = {

            Text(
                text = "Contactar soporte",
                color = TextPrimary,
                style = AppTypography.titleModal
            )
        },


        // ====================================================
        // 21. FORMULARIO DESPLAZABLE CON LAZYCOLUMN
        // ====================================================

        text = {

            /**
             * LazyColumn organiza los campos
             * del formulario verticalmente.
             *
             * Permite desplazarse cuando
             * aparece el teclado o cuando
             * el contenido no cabe en pantalla.
             *
             * El límite de altura evita
             * que el contenido ocupe
             * demasiado espacio.
             */
            LazyColumn(

                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),

                // Separación entre secciones
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)

            ) {


                // ============================================
                // 22. DESCRIPCIÓN DEL FORMULARIO
                // ============================================

                item(key = "support_description") {

                    Text(

                        text =
                            "Cuéntanos qué problema tienes " +
                                    "con Café Smart.",

                        color = TextSecondary,
                        style = AppTypography.bodyMd
                    )
                }


                // ============================================
                // 23. CAMPO DE CORREO ELECTRÓNICO
                // ============================================

                item(key = "support_email_field") {

                    /**
                     * Permite ingresar el correo
                     * del usuario que solicita soporte.
                     *
                     * El teclado se configura
                     * para direcciones de correo.
                     */
                    OutlinedTextField(

                        // Correo actual
                        value = email,

                        // Actualiza el valor del campo
                        onValueChange = { value ->

                            email = value

                            // Limpia errores anteriores
                            error = null
                        },

                        // Etiqueta del campo
                        label = {
                            Text("Tu correo electrónico")
                        },

                        // Ejemplo de correo
                        placeholder = {

                            Text(
                                text = "ejemplo@correo.com",
                                color = TextMuted
                            )
                        },

                        // Configura el teclado de correo
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),

                        // Permite escribir en una sola línea
                        singleLine = true,

                        modifier = Modifier.fillMaxWidth(),

                        // Colores del campo
                        colors = OutlinedTextFieldDefaults.colors(

                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,

                            focusedContainerColor = SurfaceScreen,
                            unfocusedContainerColor = SurfaceScreen,

                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextSecondary,

                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDefault,

                            cursorColor = Primary
                        )
                    )
                }


                // ============================================
                // 24. CAMPO PARA DESCRIBIR EL PROBLEMA
                // ============================================

                item(key = "support_problem_field") {

                    /**
                     * Permite describir el problema
                     * presentado en Café Smart.
                     *
                     * El campo puede mostrar
                     * entre 3 y 5 líneas de texto.
                     */
                    OutlinedTextField(

                        // Descripción actual
                        value = description,

                        // Actualiza la descripción
                        onValueChange = { value ->

                            description = value

                            // Limpia el error anterior
                            error = null
                        },

                        // Nombre del campo
                        label = {
                            Text("Describe tu problema")
                        },

                        // Texto de ejemplo
                        placeholder = {

                            Text(
                                text = "No puedo ingresar a mi cuenta...",
                                color = TextMuted
                            )
                        },

                        // Altura mínima en líneas
                        minLines = 3,

                        // Altura máxima en líneas
                        maxLines = 5,

                        modifier = Modifier.fillMaxWidth(),

                        // Colores del campo
                        colors = OutlinedTextFieldDefaults.colors(

                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,

                            focusedContainerColor = SurfaceScreen,
                            unfocusedContainerColor = SurfaceScreen,

                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextSecondary,

                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDefault,

                            cursorColor = Primary
                        )
                    )
                }


                // ============================================
                // 25. MENSAJE DE ERROR
                // ============================================

                /**
                 * Si el usuario ingresa datos incorrectos,
                 * muestra el mensaje correspondiente.
                 *
                 * Cuando error es null,
                 * no agrega este elemento a LazyColumn.
                 */
                error?.let { message ->

                    item(key = "support_error") {

                        Text(
                            text = message,
                            color = ErrorText,
                            style = AppTypography.helper
                        )
                    }
                }


                // ============================================
                // 26. MENSAJE INFORMATIVO
                // ============================================

                item(key = "support_information") {

                    /**
                     * Explica que el correo será preparado
                     * para que el usuario pueda revisarlo
                     * antes de enviarlo.
                     */
                    Text(

                        text =
                            "Se preparará un correo " +
                                    "para que puedas revisarlo " +
                                    "antes de enviarlo.",

                        color = TextMuted,
                        style = AppTypography.helper
                    )
                }
            }
        },


        // ====================================================
        // 27. BOTÓN CANCELAR
        // ====================================================

        /**
         * Permite cerrar el formulario
         * sin preparar ningún correo.
         *
         * Se encuentra fuera de LazyColumn
         * para permanecer visible.
         */
        dismissButton = {

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
        // 28. BOTÓN PREPARAR CORREO
        // ====================================================

        confirmButton = {

            /**
             * Verifica los campos y entrega
             * los datos cuando son válidos.
             */
            Button(

                onClick = {


                    // ========================================
                    // 29. LIMPIAR LOS DATOS
                    // ========================================

                    /**
                     * trim() elimina espacios
                     * al principio y al final.
                     */
                    val correo = email.trim()

                    val problema = description.trim()


                    // ========================================
                    // 30. VALIDAR EL FORMULARIO
                    // ========================================

                    /**
                     * when comprueba:
                     *
                     * 1. Que el correo tenga formato válido.
                     * 2. Que la descripción no esté vacía.
                     *
                     * Si todo es correcto,
                     * ejecuta onPrepareEmail.
                     */
                    when {


                        // ------------------------------------
                        // VALIDACIÓN DEL CORREO
                        // ------------------------------------

                        !supportEmailRegex.matches(correo) -> {

                            error = "Ingresa un correo válido."
                        }


                        // ------------------------------------
                        // VALIDACIÓN DE LA DESCRIPCIÓN
                        // ------------------------------------

                        problema.isBlank() -> {

                            error = "Describe el problema."
                        }


                        // ------------------------------------
                        // DATOS VÁLIDOS
                        // ------------------------------------

                        else -> {

                            /**
                             * Entrega el correo y el problema
                             * a la función encargada
                             * de preparar el mensaje.
                             *
                             * No envía automáticamente
                             * ningún correo electrónico.
                             */
                            onPrepareEmail(
                                correo,
                                problema
                            )
                        }
                    }
                },

                // Colores del botón principal
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                )
            ) {

                Text(
                    text = "Preparar correo",
                    color = OnPrimary
                )
            }
        }
    )
}
