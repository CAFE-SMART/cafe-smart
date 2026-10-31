
package com.cafesmart.app.ui.auth

// Importaciones para los elementos visuales y la distribución
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

// Componentes de Material Design 3
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// Herramientas para alineación, colores y modificadores
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Componentes reutilizables de Café Smart
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.SegmentedProgressBar

// Colores y tipografías definidos para la aplicación
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. CAMPO DE TEXTO PARA AUTENTICACIÓN Y REGISTRO
// ============================================================

/**
 * Componente reutilizable para los campos de los formularios.
 *
 * Permite ingresar información como nombres, correo,
 * teléfono y contraseña.
 *
 * También muestra mensajes de ayuda o errores de validación.
 */
@Composable
internal fun AuthField(
    title: String,                  // Nombre del campo
    value: String,                  // Texto ingresado
    onValueChange: (String) -> Unit, // Actualiza el texto
    hint: String,                   // Ejemplo dentro del campo
    help: String,                   // Mensaje de ayuda
    error: String?,                 // Mensaje de error
    modifier: Modifier = Modifier,

    // Define qué tipo de teclado se muestra
    keyboardType: KeyboardType = KeyboardType.Text,

    // Permite ocultar caracteres, como en contraseñas
    visualTransformation: VisualTransformation =
        VisualTransformation.None,

    // Elemento opcional al final del campo
    trailing: (@Composable () -> Unit)? = null,

    // Cantidad máxima de caracteres permitidos
    maxChars: Int? = null
) {

    // Organiza la etiqueta, el campo y el mensaje verticalmente
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // Nombre del campo que debe completar el usuario
        Text(
            text = title,
            style = AppTypography.labelField
        )

        // Campo donde el usuario escribe su información
        OutlinedTextField(
            value = value,

            // Controla los cambios y la longitud del texto
            onValueChange = { newValue ->

                // Si no hay límite o no lo supera,
                // se permite actualizar el contenido
                if (
                    maxChars == null ||
                    newValue.length <= maxChars
                ) {
                    onValueChange(newValue)
                }
            },

            // Texto de ejemplo cuando el campo está vacío
            placeholder = {
                Text(
                    text = hint,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            },

            // Evita que el campo tenga varias líneas
            singleLine = true,

            // Activa el estado de error si existe un mensaje
            isError = error != null,

            // Define si el texto se muestra u oculta
            visualTransformation = visualTransformation,

            // Configura el teclado según el dato solicitado
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType
            ),

            // Permite agregar un botón o ícono al final
            trailingIcon = trailing,

            // Bordes redondeados de 12 dp
            shape = RoundedCornerShape(12.dp),

            // Colores del campo según su estado
            colors = OutlinedTextFieldDefaults.colors(

                // Color del borde cuando está seleccionado
                focusedBorderColor = Primary,

                // Color del borde cuando no está seleccionado
                unfocusedBorderColor = BorderDefault,

                // Color del borde cuando existe un error
                errorBorderColor = ErrorText,

                // Fondo del campo en sus diferentes estados
                focusedContainerColor = SurfaceScreen,
                unfocusedContainerColor = SurfaceScreen,
                errorContainerColor = SurfaceScreen
            ),

            // Ocupa todo el ancho disponible
            modifier = Modifier.fillMaxWidth()
        )

        // Muestra un error o una indicación de ayuda
        Text(
            text = error ?: help,
            style = AppTypography.helper,

            // Los errores se muestran en rojo;
            // los mensajes normales usan un color secundario
            color = if (error != null) {
                ErrorText
            } else {
                TextMuted
            }
        )
    }
}


// ============================================================
// 2. ENCABEZADO DE LA PANTALLA CREAR CUENTA
// ============================================================

/**
 * Muestra el encabezado de registro.
 *
 * Contiene:
 * - Flecha para regresar.
 * - Título "Crear cuenta".
 * - Línea divisoria inferior.
 *
 * Respeta la zona de la barra de estado del dispositivo.
 */
@Composable
internal fun RegisterHeader(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()

            // Aplica el color de fondo del encabezado
            .background(SurfaceCard)

            // Evita que el contenido quede debajo
            // de la barra de estado del celular
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
    ) {

        // Contenedor principal del encabezado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            // Ubica la flecha de regreso a la izquierda
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {

                // Ejecuta la acción de volver
                BackArrow(onClick = onBack)
            }

            // Título centrado en el encabezado
            Text(
                text = "Crear cuenta",
                style = AppTypography.titleScreen,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }

        // Línea que separa el encabezado del formulario
        HorizontalDivider(
            color = BorderDefault,
            thickness = 1.dp
        )
    }
}


// ============================================================
// 3. INDICADOR DE PROGRESO DEL REGISTRO
// ============================================================

/**
 * Muestra en qué paso del registro se encuentra el usuario.
 *
 * Paso 1: Información del negocio.
 * Paso 2: Datos del administrador.
 *
 * Incluye una barra de progreso de dos segmentos.
 */
@Composable
internal fun RegistrationProgress(
    step: Int
) {

    // Fila que contiene el nombre y número del paso
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // Cambia el título dependiendo del paso actual
        Text(
            text = if (step == 1) {
                "Paso 1: Información del negocio"
            } else {
                "Paso 2: Datos del administrador"
            },

            style = AppTypography.caption
        )

        // Muestra el progreso numérico del registro
        Text(
            text = "$step de 2",
            style = AppTypography.caption
        )
    }

    // Espacio entre el texto y la barra de progreso
    Spacer(
        modifier = Modifier.height(8.dp)
    )

    // Barra visual que indica los pasos completados
    SegmentedProgressBar(
        total = 2,
        current = step,
        modifier = Modifier.fillMaxWidth()
    )
}


// ============================================================
// 4. ENLACE REUTILIZABLE
// ============================================================

/**
 * Crea un texto que funciona como enlace.
 *
 * Puede utilizarse para abrir ayuda,
 * contactar soporte o navegar a otra pantalla.
 */
@Composable
internal fun AuthLink(
    text: String,
    onClick: () -> Unit
) {

    Text(
        text = text,

        // Aplica el estilo definido para los enlaces
        style = AppTypography.buttonLink,

        // Permite ejecutar una acción al pulsar el texto
        modifier = Modifier.clickable(
            onClick = onClick
        )
    )
}


// ============================================================
// 5. ENLACES DE AYUDA Y SOPORTE
// ============================================================

/**
 * Muestra las opciones de ayuda durante el registro.
 *
 * Incluye:
 * - Ver ayuda.
 * - Contactar soporte.
 *
 * Las acciones se reciben desde la pantalla que lo utiliza.
 */
@Composable
internal fun SupportLinks(
    onHelp: () -> Unit,
    onSupport: () -> Unit
) {

    // Organiza los elementos verticalmente y centrados
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Pregunta que orienta al usuario
        Text(
            text = "¿Dudas con los datos del registro?",
            style = AppTypography.helper
        )

        // Espacio entre la pregunta y los enlaces
        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Organiza los dos enlaces horizontalmente
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                16.dp
            )
        ) {

            // Abre la sección de ayuda
            AuthLink(
                text = "Ver ayuda",
                onClick = onHelp
            )

            // Abre la opción de contactar soporte
            AuthLink(
                text = "Contactar soporte",
                onClick = onSupport
            )
        }
    }
}


// ============================================================
// 6. BOTÓN PARA CONTINUAR CON GOOGLE
// ============================================================

/**
 * Botón visual para iniciar sesión con Google.
 *
 * Recibe la acción onClick desde la pantalla principal.
 *
 * Nota: este componente solo construye la interfaz.
 * La autenticación real debe implementarse por separado.
 */
@Composable
internal fun GoogleButton(
    onClick: () -> Unit
) {

    OutlinedButton(

        // Acción que se ejecuta al pulsar el botón
        onClick = onClick,

        // Configuración del tamaño del botón
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),

        // Bordes redondeados
        shape = RoundedCornerShape(12.dp),

        // Color de fondo del botón
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceCard
        )
    ) {

        // Letra G utilizada como representación
        // temporal del identificador de Google
        Text(
            text = "G",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF4285F4)
        )

        // Separación entre la letra y el texto
        Spacer(
            modifier = Modifier.width(12.dp)
        )

        // Texto que describe la acción del botón
        Text(
            text = "Continuar con Google",
            color = TextStrong
        )
    }
}


// ============================================================
// 7. DIVISOR ENTRE MÉTODOS DE AUTENTICACIÓN
// ============================================================

/**
 * Muestra un separador horizontal con el texto
 * "O continúa con".
 *
 * Se utiliza para distinguir el inicio de sesión
 * con Google de los demás métodos.
 */
@Composable
internal fun GoogleDivider() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Línea horizontal izquierda
        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )

        // Texto central del separador
        Text(
            text = "O continúa con",

            modifier = Modifier.padding(
                horizontal = 12.dp
            ),

            style = AppTypography.caption
        )

        // Línea horizontal derecha
        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )
    }
}


// ============================================================
// 8. CONTROL PARA MOSTRAR U OCULTAR CONTRASEÑA
// ============================================================

/**
 * Permite cambiar la visibilidad de una contraseña.
 *
 * Si visible es true, muestra "Ocultar".
 * Si visible es false, muestra "Ver".
 *
 * El estado de visibilidad se administra
 * desde la pantalla que utiliza este componente.
 */
@Composable
internal fun PasswordEye(
    visible: Boolean,
    onClick: () -> Unit
) {

    // Botón de texto para cambiar la visibilidad
    TextButton(
        onClick = onClick
    ) {

        // Cambia el texto dependiendo del estado
        Text(
            text = if (visible) {
                "Ocultar"
            } else {
                "Ver"
            },

            fontSize = 12.sp
        )
    }
}
