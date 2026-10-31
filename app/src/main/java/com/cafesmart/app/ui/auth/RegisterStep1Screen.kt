
package com.cafesmart.app.ui.auth

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Permite establecer fondos y bordes
import androidx.compose.foundation.background
import androidx.compose.foundation.border

// Componentes para organizar elementos en pantalla
import androidx.compose.foundation.layout.*

// Permite crear listas verticales desplazables
import androidx.compose.foundation.lazy.LazyColumn

// Permite agregar elementos de una colección a LazyColumn
import androidx.compose.foundation.lazy.items

// Permite seleccionar opciones de manera accesible
import androidx.compose.foundation.selection.selectable

// Permite crear esquinas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Componentes visuales de Material Design 3
import androidx.compose.material3.*

// Permite crear funciones de interfaz con Jetpack Compose
import androidx.compose.runtime.Composable

// Permite alinear los componentes
import androidx.compose.ui.Alignment

// Permite modificar tamaño, posición y apariencia
import androidx.compose.ui.Modifier

// Permite definir colores personalizados
import androidx.compose.ui.graphics.Color

// Identifica elementos seleccionables para accesibilidad
import androidx.compose.ui.semantics.Role

// Permite modificar el grosor de la tipografía
import androidx.compose.ui.text.font.FontWeight

// Permite visualizar las pantallas en Android Studio
import androidx.compose.ui.tooling.preview.Preview

// Unidades utilizadas en Compose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Botón reutilizable de Café Smart
import com.cafesmart.app.ui.components.ContinueButton

// Colores y estilos definidos para la aplicación
import com.cafesmart.app.ui.theme.*


// ============================================================
// 1. PANTALLA DEL PRIMER PASO DEL REGISTRO
// ============================================================

/**
 * RegisterStep1Screen muestra el primer paso
 * del formulario de creación de cuenta.
 *
 * Permite:
 *
 * - Ingresar el nombre del negocio.
 * - Seleccionar el tipo de negocio.
 * - Consultar información de ayuda.
 * - Contactar soporte.
 * - Continuar al segundo paso.
 * - Regresar a la pantalla anterior.
 *
 * Utiliza LazyColumn para organizar el contenido
 * desplazable del formulario.
 *
 * El estado y las validaciones son administrados
 * desde AuthViewModel.
 */
@Composable
fun RegisterStep1Screen(

    // Estado que contiene los datos del formulario
    state: AuthUiState,

    // Función para actualizar los datos del usuario
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,

    // Acción para regresar a la pantalla anterior
    onBack: () -> Unit,

    // Acción para continuar al segundo paso
    onContinue: () -> Unit,

    // Acción para mostrar las instrucciones de ayuda
    onHelp: () -> Unit,

    // Acción para contactar soporte
    onSupport: () -> Unit
) {

    // ========================================================
    // 2. ESTRUCTURA PRINCIPAL DE LA PANTALLA
    // ========================================================

    /**
     * Scaffold organiza las áreas principales
     * de la interfaz.
     *
     * Contiene:
     *
     * topBar: encabezado superior.
     * bottomBar: botón Continuar y enlaces de ayuda.
     * content: formulario de registro.
     *
     * Esto permite que el formulario se desplace
     * sin mover los botones inferiores.
     */
    Scaffold(

        // Color de fondo de la pantalla
        containerColor = SurfaceCard,


        // ====================================================
        // 3. ENCABEZADO SUPERIOR
        // ====================================================

        topBar = {

            /**
             * Muestra el encabezado "Crear cuenta"
             * y la flecha para regresar.
             */
            RegisterHeader(
                onBack = onBack
            )
        },


        // ====================================================
        // 4. BARRA INFERIOR DE ACCIONES
        // ====================================================

        bottomBar = {

            /**
             * Esta sección permanece separada
             * del contenido desplazable.
             *
             * Contiene el botón Continuar
             * y los enlaces de ayuda y soporte.
             */
            Column(

                modifier = Modifier

                    // Ocupa todo el ancho
                    .fillMaxWidth()

                    // Aplica el color de fondo
                    .background(SurfaceCard)

                    // Respeta la zona de navegación
                    // del sistema Android
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )

                    // Márgenes internos
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 12.dp,
                        top = 12.dp
                    ),

                // Centra horizontalmente los elementos
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                // ============================================
                // 5. BOTÓN CONTINUAR
                // ============================================

                /**
                 * Ejecuta onContinue cuando el usuario
                 * intenta avanzar al segundo paso.
                 *
                 * AuthViewModel.nextStep() es responsable
                 * de validar los datos antes de avanzar.
                 */
                ContinueButton(

                    text = "Continuar",

                    onClick = onContinue
                )

                // Espacio entre el botón y los enlaces
                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // ============================================
                // 6. ENLACES DE AYUDA Y SOPORTE
                // ============================================

                /**
                 * Muestra las opciones:
                 *
                 * - Ver ayuda.
                 * - Contactar soporte.
                 *
                 * Utiliza el componente reutilizable
                 * SupportLinks de Café Smart.
                 */
                SupportLinks(

                    onHelp = onHelp,

                    onSupport = onSupport
                )
            }
        }

    ) { paddingValues ->


        // ====================================================
        // 7. CONTENIDO DEL FORMULARIO CON LAZYCOLUMN
        // ====================================================

        /**
         * LazyColumn organiza verticalmente
         * las secciones del formulario.
         *
         * Ventajas:
         *
         * - Permite desplazar el contenido.
         * - Compone los elementos según se necesitan.
         * - Facilita agregar nuevas secciones.
         * - Evita utilizar verticalScroll().
         *
         * Cada item representa una sección.
         *
         * paddingValues evita que el contenido
         * quede debajo del encabezado o de
         * la barra inferior del Scaffold.
         */
        LazyColumn(

            modifier = Modifier

                // Ocupa el espacio disponible
                .fillMaxSize()

                // Respeta los espacios del Scaffold
                .padding(paddingValues),

            // Márgenes internos del formulario
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            )

        ) {


            // =================================================
            // 8. INDICADOR DE PROGRESO
            // =================================================

            item(key = "registration_progress") {

                /**
                 * Muestra el indicador del primer paso.
                 *
                 * Se utiliza el componente compartido
                 * RegistrationProgress.
                 */
                RegistrationProgress(
                    step = 1
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )


                // =============================================
                // 9. TÍTULO DEL FORMULARIO
                // =============================================

                Text(

                    text = "Comencemos configurando tu negocio",

                    style = AppTypography.titleCard
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }


            // =================================================
            // 10. CAMPO DEL NOMBRE DEL NEGOCIO
            // =================================================

            item(key = "business_name") {

                /**
                 * Permite ingresar el nombre del negocio.
                 *
                 * El campo tiene un límite de 30 caracteres
                 * y muestra la cantidad escrita.
                 */
                AuthField(

                    // Título del campo
                    title = "Nombre del negocio",

                    // Obtiene el valor actual
                    value = state.businessName,


                    // =========================================
                    // ACTUALIZACIÓN DEL NOMBRE
                    // =========================================

                    onValueChange = { value ->

                        /**
                         * Actualiza solamente businessName.
                         *
                         * copy() conserva los demás valores
                         * del estado actual.
                         */
                        onChange { currentState ->

                            currentState.copy(
                                businessName = value
                            )
                        }
                    },

                    // Ejemplo para orientar al usuario
                    hint = "Ej: Café Los Alpes",

                    // Muestra el contador de caracteres
                    help =
                        "Aparecerá en tus reportes. " +
                                "${state.businessName.length}/30",

                    // Muestra el error de validación
                    error = state.errors["businessName"],

                    // Límite máximo de caracteres
                    maxChars = 30
                )

                Spacer(
                    modifier = Modifier.height(23.dp)
                )
            }


            // =================================================
            // 11. TÍTULO DEL TIPO DE NEGOCIO
            // =================================================

            item(key = "business_type_title") {

                /**
                 * Indica al usuario que debe seleccionar
                 * el modelo de negocio correspondiente.
                 */
                Text(

                    text = "Tipo de negocio",

                    style = AppTypography.labelField
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }


            // =================================================
            // 12. LISTA DE TIPOS DE NEGOCIO
            // =================================================

            /**
             * items() recorre los valores del enum
             * BusinessType.
             *
             * Las opciones disponibles son:
             *
             * - COMPRAVENTA
             * - COOPERATIVA
             * - PERSONALIZADO
             *
             * Cada opción se representa mediante
             * el componente BusinessChoice.
             *
             * La clave estable permite identificar
             * cada elemento de la lista.
             */
            items(

                items = BusinessType.entries,

                key = { type ->
                    type.name
                }

            ) { type ->


                // =============================================
                // 13. OPCIÓN INDIVIDUAL DEL NEGOCIO
                // =============================================

                BusinessChoice(

                    // Tipo de negocio correspondiente
                    type = type,

                    // Comprueba si esta opción está seleccionada
                    selected = state.businessType == type,

                    // Actualiza el negocio seleccionado
                    onClick = {

                        onChange { currentState ->

                            currentState.copy(
                                businessType = type
                            )
                        }
                    }
                )

                // Separación entre las opciones
                Spacer(
                    modifier = Modifier.height(9.dp)
                )
            }


            // =================================================
            // 14. MENSAJE DE ERROR DEL TIPO DE NEGOCIO
            // =================================================

            /**
             * Si no se seleccionó un tipo de negocio
             * y existe un error, se muestra debajo
             * de las opciones.
             */
            state.errors["businessType"]?.let { error ->

                item(key = "business_type_error") {

                    Text(

                        text = error,

                        color = ErrorText,

                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


// ============================================================
// 15. COMPONENTE PARA SELECCIONAR EL TIPO DE NEGOCIO
// ============================================================

/**
 * BusinessChoice representa una tarjeta seleccionable
 * para cada modelo de negocio.
 *
 * Contiene:
 *
 * - Un símbolo representativo.
 * - Nombre del modelo de negocio.
 * - Descripción breve.
 * - Botón de selección.
 *
 * Parámetros:
 *
 * type: tipo de negocio que representa.
 * selected: indica si está seleccionado.
 * onClick: acción que selecciona la opción.
 */
@Composable
private fun BusinessChoice(

    type: BusinessType,

    selected: Boolean,

    onClick: () -> Unit
) {


    // ========================================================
    // 16. COLOR IDENTIFICADOR DEL TIPO DE NEGOCIO
    // ========================================================

    /**
     * Cada modelo de negocio utiliza
     * un color visual diferente.
     *
     * Estos colores ayudan a distinguir
     * las opciones de la interfaz.
     */
    val accent = when (type) {

        // Naranja para compraventa
        BusinessType.COMPRAVENTA ->
            Color(0xFFEA580C)

        // Azul para cooperativa
        BusinessType.COOPERATIVA ->
            Color(0xFF2563EB)

        // Rosa para personalizado
        BusinessType.PERSONALIZADO ->
            Color(0xFFDB2777)
    }


    // ========================================================
    // 17. CONTENEDOR DE LA OPCIÓN
    // ========================================================

    /**
     * Row organiza horizontalmente:
     *
     * - Símbolo.
     * - Información.
     * - RadioButton.
     *
     * Toda la tarjeta se puede seleccionar.
     */
    Row(

        modifier = Modifier

            // Ocupa el ancho disponible
            .fillMaxWidth()


            // ================================================
            // BORDE SEGÚN EL ESTADO DE SELECCIÓN
            // ================================================

            .border(

                // Borde más grueso cuando está seleccionado
                width =
                    if (selected) 2.dp else 1.dp,

                // Color del borde según el estado
                color =
                    if (selected) {
                        Primary
                    } else {
                        BorderDefault
                    },

                // Esquinas redondeadas
                shape = RoundedCornerShape(12.dp)
            )


            // ================================================
            // SELECCIÓN ACCESIBLE
            // ================================================

            /**
             * selectable permite seleccionar
             * la opción pulsando cualquier parte
             * de la tarjeta.
             *
             * Role.RadioButton informa a las tecnologías
             * de accesibilidad que se trata de una
             * opción de selección única.
             */
            .selectable(

                selected = selected,

                onClick = onClick,

                role = Role.RadioButton
            )

            // Espacio interno de la tarjeta
            .padding(12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {


        // ====================================================
        // 18. SÍMBOLO DEL TIPO DE NEGOCIO
        // ====================================================

        /**
         * Muestra un símbolo diferente
         * para cada modelo de negocio.
         */
        Box(

            modifier = Modifier

                // Tamaño del contenedor
                .size(36.dp)

                // Fondo con baja opacidad
                .background(

                    accent.copy(alpha = 0.10f),

                    RoundedCornerShape(8.dp)
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(

                // Selecciona el símbolo según el negocio
                text = when (type) {

                    BusinessType.COMPRAVENTA -> "▤"

                    BusinessType.COOPERATIVA -> "♧"

                    BusinessType.PERSONALIZADO -> "☷"
                },

                color = accent,

                fontSize = 23.sp
            )
        }


        // Separación entre el símbolo y el texto
        Spacer(
            modifier = Modifier.width(12.dp)
        )


        // ====================================================
        // 19. INFORMACIÓN DEL TIPO DE NEGOCIO
        // ====================================================

        /**
         * Muestra el nombre y la descripción
         * del modelo de negocio seleccionado.
         */
        Column(

            // Ocupa el espacio restante
            modifier = Modifier.weight(1f)
        ) {


            // ================================================
            // NOMBRE DEL NEGOCIO
            // ================================================

            Text(

                text = type.label,

                style = AppTypography.labelField.copy(

                    fontWeight = FontWeight.Bold,

                    color = TextPrimary
                )
            )


            // ================================================
            // DESCRIPCIÓN DEL NEGOCIO
            // ================================================

            Text(

                text = type.subtitle,

                style = AppTypography.helper
            )
        }


        // ====================================================
        // 20. INDICADOR DE SELECCIÓN
        // ====================================================

        /**
         * RadioButton indica visualmente
         * cuál de las opciones está seleccionada.
         *
         * onClick es null porque la selección
         * la administra la tarjeta completa
         * mediante Modifier.selectable.
         */
        RadioButton(

            selected = selected,

            onClick = null,

            colors = RadioButtonDefaults.colors(

                selectedColor = Primary,

                unselectedColor = BorderField
            )
        )
    }
}


// ============================================================
// 21. VISTA PREVIA DEL REGISTRO COMPRAVENTA
// ============================================================

/**
 * Permite visualizar el formulario
 * con Compraventa seleccionada.
 *
 * Utiliza las dimensiones:
 * 390 x 844 dp.
 */
@Preview(
    name = "Registro Compraventa",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CompraventaPreview() {

    // Aplica el tema de Café Smart
    CafesmartTheme {

        RegisterStep1Screen(

            // Estado de ejemplo
            state = AuthUiState(

                page = AuthPage.REGISTER_STEP_1,

                businessType = BusinessType.COMPRAVENTA
            ),

            // Acciones vacías para la vista previa
            onChange = { _ -> },

            onBack = {},

            onContinue = {},

            onHelp = {},

            onSupport = {}
        )
    }
}


// ============================================================
// 22. VISTA PREVIA DEL REGISTRO COOPERATIVA
// ============================================================

/**
 * Permite visualizar el formulario
 * con Cooperativa seleccionada.
 *
 * Sirve para comprobar el aspecto visual
 * de ambas variantes de registro.
 */
@Preview(
    name = "Registro Cooperativa",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CooperativaPreview() {

    // Aplica el tema de Café Smart
    CafesmartTheme {

        RegisterStep1Screen(

            // Estado de ejemplo
            state = AuthUiState(

                page = AuthPage.REGISTER_STEP_1,

                businessType = BusinessType.COOPERATIVA
            ),

            // Acciones vacías para la vista previa
            onChange = { _ -> },

            onBack = {},

            onContinue = {},

            onHelp = {},

            onSupport = {}
        )
    }
}
