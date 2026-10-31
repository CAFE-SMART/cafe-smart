
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite definir el grosor y color de los bordes
import androidx.compose.foundation.BorderStroke

// Elementos para organizar los componentes en pantalla
import androidx.compose.foundation.layout.*

// Permite recordar la posición del desplazamiento
import androidx.compose.foundation.rememberScrollState

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Habilita el desplazamiento vertical del contenido
import androidx.compose.foundation.verticalScroll

// Componentes visuales de Material Design 3
import androidx.compose.material3.*

// Permite declarar funciones de interfaz en Jetpack Compose
import androidx.compose.runtime.Composable

// Permite modificar tamaño, posición y apariencia
import androidx.compose.ui.Modifier

// Unidad de medida utilizada en Jetpack Compose
import androidx.compose.ui.unit.dp

// Importa los colores y estilos definidos en Café Smart
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. CONTEXTOS DE AYUDA PARA LA AUTENTICACIÓN
// ============================================================

/**
 * Define las pantallas desde las cuales se puede
 * solicitar ayuda.
 *
 * LOGIN: pantalla de inicio de sesión.
 * REGISTER_STEP_1: primer paso del registro.
 * REGISTER_STEP_2: segundo paso del registro.
 *
 * Esto permite mostrar información diferente
 * dependiendo de la pantalla donde esté el usuario.
 */
enum class AuthHelpContext {

    // Ayuda para iniciar sesión
    LOGIN,

    // Ayuda para registrar la información del negocio
    REGISTER_STEP_1,

    // Ayuda para registrar los datos del administrador
    REGISTER_STEP_2
}


// ============================================================
// 2. MODELO DE DATOS PARA EL CONTENIDO DE AYUDA
// ============================================================

/**
 * Almacena la información que aparecerá
 * dentro de la ventana de ayuda.
 *
 * title: título de la ventana.
 * description: explicación general.
 * instructions: lista de instrucciones para el usuario.
 *
 * Se declara como private porque solamente
 * se utiliza dentro de este archivo.
 */
private data class HelpContent(

    // Título que identifica el tipo de ayuda
    val title: String,

    // Descripción breve del proceso
    val description: String,

    // Lista de pasos que debe seguir el usuario
    val instructions: List<String>
)


// ============================================================
// 3. VENTANA DE AYUDA CONTEXTUAL
// ============================================================

/**
 * Componente que muestra una ventana emergente
 * con instrucciones según el proceso de autenticación.
 *
 * Parámetros:
 *
 * context:
 * Indica desde qué pantalla se solicita ayuda.
 *
 * onDismiss:
 * Acción utilizada para cerrar la ventana.
 *
 * onRecovery:
 * Acción para recuperar la contraseña.
 *
 * onSupport:
 * Acción para contactar al equipo de soporte.
 *
 * El componente no realiza directamente estas acciones.
 * Las recibe desde la pantalla que lo utiliza.
 */
@Composable
fun AuthContextHelpDialog(
    context: AuthHelpContext,
    onDismiss: () -> Unit,
    onRecovery: () -> Unit,
    onSupport: () -> Unit
) {

    // ========================================================
    // 4. SELECCIÓN DEL CONTENIDO SEGÚN LA PANTALLA
    // ========================================================

    /**
     * La expresión when permite seleccionar
     * qué información mostrar según el contexto.
     *
     * De esta manera se reutiliza una sola ventana
     * para los diferentes pasos de autenticación.
     */
    val content = when (context) {

        // ----------------------------------------------------
        // AYUDA PARA INICIAR SESIÓN
        // ----------------------------------------------------

        AuthHelpContext.LOGIN -> HelpContent(

            // Título de la ventana
            title = "Ayuda para iniciar sesión",

            // Explicación sobre el inicio de sesión
            description =
                "Aquí encontrarás información para " +
                        "ingresar a tu cuenta de Café Smart.",

            // Instrucciones para ingresar a la cuenta
            instructions = listOf(

                // Primera instrucción
                "Ingresa el correo electrónico " +
                        "con el que registraste tu cuenta.",

                // Segunda instrucción
                "Escribe tu contraseña y comprueba " +
                        "que esté escrita correctamente.",

                // Tercera instrucción
                "Presiona Entrar para continuar.",

                // Cuarta instrucción
                "Si todavía no tienes cuenta, " +
                        "selecciona Regístrate gratis."
            )
        )

        // ----------------------------------------------------
        // AYUDA PARA EL PASO 1 DEL REGISTRO
        // ----------------------------------------------------

        AuthHelpContext.REGISTER_STEP_1 -> HelpContent(

            // Título correspondiente al primer paso
            title = "Ayuda: Paso 1",

            // Explica qué información debe registrarse
            description =
                "En este paso debes registrar " +
                        "la información de tu negocio.",

            // Instrucciones para completar el primer paso
            instructions = listOf(

                // Explicación del nombre del negocio
                "Nombre del negocio: escribe el nombre " +
                        "con el que se identifica tu negocio. " +
                        "El máximo es de 30 caracteres.",

                // Explicación del modelo compraventa
                "Compraventa: selecciona esta opción " +
                        "si compras café para venderlo.",

                // Explicación del modelo cooperativa
                "Cooperativa: selecciona esta opción " +
                        "si administras el café de tus asociados.",

                // Explicación del modelo personalizado
                "Personalizado: selecciona esta opción " +
                        "si tu negocio funciona de otra manera.",

                // Indicación para avanzar al siguiente paso
                "Después de completar los campos " +
                        "obligatorios, presiona Continuar."
            )
        )

        // ----------------------------------------------------
        // AYUDA PARA EL PASO 2 DEL REGISTRO
        // ----------------------------------------------------

        AuthHelpContext.REGISTER_STEP_2 -> HelpContent(

            // Título correspondiente al segundo paso
            title = "Ayuda: Paso 2",

            // Explica qué información debe completar el usuario
            description =
                "En este paso registrarás los " +
                        "datos del administrador del negocio.",

            // Instrucciones del formulario de administrador
            instructions = listOf(

                // Datos personales del administrador
                "Nombre y apellidos: escribe los datos " +
                        "de la persona que administrará la cuenta.",

                // Reglas para ingresar el número celular
                "Teléfono: ingresa un celular colombiano " +
                        "de 10 dígitos que empiece por 3.",

                // Correo utilizado para ingresar al sistema
                "Correo electrónico: escribe un correo válido. " +
                        "Lo utilizarás para iniciar sesión.",

                // Requisitos para crear la contraseña
                "Contraseña: escribe al menos 8 caracteres, " +
                        "incluyendo mayúscula, minúscula y número.",

                // Verificación de la contraseña
                "Confirmar contraseña: repite exactamente " +
                        "la contraseña anterior.",

                // Acción final del proceso de registro
                "Cuando los campos estén completos, " +
                        "presiona Crear cuenta."
            )
        )
    }


    // ========================================================
    // 5. CREACIÓN DE LA VENTANA EMERGENTE
    // ========================================================

    /**
     * AlertDialog muestra una ventana sobre
     * la pantalla actual.
     *
     * Incluye un título, contenido y botones
     * para ejecutar diferentes acciones.
     */
    AlertDialog(

        // Acción que permite cerrar la ventana
        // al pulsar fuera de ella o solicitar salir
        onDismissRequest = onDismiss,

        // Color de fondo de la ventana
        containerColor = SurfaceCard,

        // Color predeterminado de los títulos
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

                // El título cambia según el contexto
                text = content.title,

                // Estilo definido para títulos de modales
                style = AppTypography.titleModal,

                // Color principal del texto
                color = TextPrimary
            )
        },


        // ====================================================
        // 7. CONTENIDO PRINCIPAL DE LA VENTANA
        // ====================================================

        text = {

            /**
             * Column organiza verticalmente
             * las instrucciones y los botones.
             *
             * El desplazamiento permite consultar
             * todo el contenido en pantallas pequeñas.
             */
            Column(

                modifier = Modifier

                    // Altura máxima del contenido desplazable
                    .heightIn(max = 430.dp)

                    // Habilita el desplazamiento vertical
                    .verticalScroll(rememberScrollState()),

                // Separación entre los elementos
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                // ============================================
                // 8. DESCRIPCIÓN DEL PROCESO
                // ============================================

                // Muestra la explicación del contexto actual
                Text(
                    text = content.description,

                    // Estilo de texto mediano
                    style = AppTypography.bodyMd,

                    // Color secundario para la descripción
                    color = TextSecondary
                )

                // Línea divisoria antes de las instrucciones
                HorizontalDivider(
                    color = BorderDefault
                )


                // ============================================
                // 9. INSTRUCCIONES NUMERADAS
                // ============================================

                /**
                 * Recorre todas las instrucciones.
                 *
                 * index representa la posición.
                 * instruction contiene el texto.
                 *
                 * Se suma 1 al índice porque
                 * las listas en Kotlin comienzan en 0.
                 */
                content.instructions.forEachIndexed {
                        index, instruction ->

                    // Muestra cada instrucción numerada
                    Text(

                        // Ejemplo: "1. Ingresa tu correo..."
                        text = "${index + 1}. $instruction",

                        // Estilo del contenido
                        style = AppTypography.bodyMd,

                        // Color principal para facilitar la lectura
                        color = TextPrimary
                    )
                }

                // Línea que separa las instrucciones
                // de los botones de ayuda
                HorizontalDivider(
                    color = BorderDefault
                )


                // ============================================
                // 10. BOTÓN PARA RECUPERAR CONTRASEÑA
                // ============================================

                /**
                 * Este botón solamente aparece
                 * cuando el usuario está en LOGIN.
                 *
                 * No es necesario mostrarlo durante
                 * el proceso de creación de cuenta.
                 */
                if (context == AuthHelpContext.LOGIN) {

                    OutlinedButton(

                        // Ejecuta la acción de recuperación
                        onClick = onRecovery,

                        // El botón ocupa todo el ancho
                        modifier = Modifier.fillMaxWidth(),

                        // Define el grosor y color del borde
                        border = BorderStroke(
                            1.dp,
                            Primary
                        ),

                        // Colores del botón
                        colors =
                            ButtonDefaults.outlinedButtonColors(

                                // Color de fondo
                                containerColor = SurfaceCard,

                                // Color del contenido
                                contentColor = Primary
                            )
                    ) {

                        // Texto del botón de recuperación
                        Text(
                            text = "Recuperar contraseña",
                            color = Primary
                        )
                    }
                }


                // ============================================
                // 11. BOTÓN PARA CONTACTAR SOPORTE
                // ============================================

                /**
                 * Este botón se muestra en todos los contextos.
                 *
                 * Permite solicitar ayuda adicional
                 * cuando las instrucciones no son suficientes.
                 */
                OutlinedButton(

                    // Ejecuta la acción de soporte
                    onClick = onSupport,

                    // Ocupa todo el ancho disponible
                    modifier = Modifier.fillMaxWidth(),

                    // Borde del botón
                    border = BorderStroke(
                        1.dp,
                        Primary
                    ),

                    // Configuración de los colores
                    colors =
                        ButtonDefaults.outlinedButtonColors(

                            // Fondo del botón
                            containerColor = SurfaceCard,

                            // Color del contenido
                            contentColor = Primary
                        )
                ) {

                    // Texto que identifica el botón
                    Text(
                        text = "Contactar soporte",
                        color = Primary
                    )
                }
            }
        },


        // ====================================================
        // 12. BOTÓN PARA CERRAR LA VENTANA
        // ====================================================

        /**
         * confirmButton define el botón principal
         * que aparece en la parte inferior del diálogo.
         *
         * En este caso permite cerrar la ayuda
         * y regresar a la pantalla anterior.
         */
        confirmButton = {

            TextButton(

                // Cierra la ventana de ayuda
                onClick = onDismiss,

                // Define el color del botón
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Primary
                    )
            ) {

                // Texto que permite confirmar la lectura
                Text(
                    text = "Entendido",
                    color = Primary
                )
            }
        }
    )
}
