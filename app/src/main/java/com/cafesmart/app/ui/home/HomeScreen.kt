
package com.cafesmart.app.ui.home

// ============================================================
// IMPORTACIONES NECESARIAS
// ============================================================

// Componentes para fondos, bordes y acciones de clic
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

// Componentes para organizar los elementos
import androidx.compose.foundation.layout.*

// Permite crear contenido vertical desplazable
import androidx.compose.foundation.lazy.LazyColumn

// Formas utilizadas en los componentes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Componentes de Material Design 3
import androidx.compose.material3.*

// Herramientas para manejar estados de Compose
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

// Permite alinear elementos
import androidx.compose.ui.Alignment

// Permite modificar la apariencia
import androidx.compose.ui.Modifier

// Permite recortar elementos con diferentes formas
import androidx.compose.ui.draw.clip

// Herramientas para manejar colores y degradados
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Herramientas para accesibilidad
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

// Configuración de textos
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

// Permite visualizar la pantalla en Android Studio
import androidx.compose.ui.tooling.preview.Preview

// Unidades de medida
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Componentes reutilizables de Café Smart
import com.cafesmart.app.ui.components.AppBottomBar
import com.cafesmart.app.ui.components.CafeIcon

// Tema de la aplicación
import com.cafesmart.app.ui.theme.CafesmartTheme

// Permite redondear números decimales
import kotlin.math.roundToInt


// ============================================================
// 1. COLORES UTILIZADOS EN LA PANTALLA
// ============================================================

/**
 * Estos colores definen la apariencia de Home.
 *
 * private significa que solamente pueden
 * utilizarse dentro de este archivo.
 */

// Azul principal de la aplicación
private val Azul = Color(0xFF2458D3)

// Azul utilizado para textos y botones destacados
private val AzulOscuro = Color(0xFF203D91)

// Azul claro para el degradado del encabezado
private val AzulClaro = Color(0xFF3889F8)

// Fondo general de la pantalla
private val Fondo = Color(0xFFF3F5FB)

// Color principal de los textos
private val Texto = Color(0xFF1D293F)

// Color secundario de los textos
private val Gris = Color(0xFF62738D)

// Color utilizado para estados positivos
private val Verde = Color(0xFF008767)

// Color de los bordes
private val Borde = Color(0xFFE5EBF3)


// ============================================================
// 2. MODELO DE DATOS PARA LOS SUBLOTES
// ============================================================

/**
 * SubloteEjemplo representa la información
 * de un sublote que aparece en Home.
 *
 * data class permite almacenar datos
 * dentro de un objeto.
 *
 * Propiedades:
 *
 * tipo: identifica el tipo de café.
 * dias: antigüedad del sublote.
 * peso: cantidad de café.
 * estado: estado o calidad del sublote.
 *
 * Los datos son de demostración.
 */
private data class SubloteEjemplo(

    val tipo: String,

    val dias: Int,

    val peso: String,

    val estado: String
)


// ============================================================
// 3. LISTA DE SUBLOTES DE EJEMPLO
// ============================================================

/**
 * listOf crea una lista de elementos.
 *
 * En este caso contiene tres sublotes
 * que se muestran en la pantalla principal.
 *
 * Posteriormente podrán reemplazarse
 * por información de la base de datos.
 */
private val sublotesEjemplo = listOf(

    // Primer sublote
    SubloteEjemplo(
        "Seco",
        26,
        "1 kg",
        "Regular"
    ),

    // Segundo sublote
    SubloteEjemplo(
        "Seco",
        25,
        "190 kg",
        "Buena"
    ),

    // Tercer sublote
    SubloteEjemplo(
        "Verde",
        15,
        "50 kg",
        "Buena"
    )
)


// ============================================================
// 4. PANTALLA PRINCIPAL HOME
// ============================================================

/**
 * HomeScreen representa la pantalla de inicio
 * de la aplicación Café Smart.
 *
 * Incluye las siguientes secciones:
 *
 * - Encabezado de bienvenida.
 * - Resumen del día.
 * - Capacidad de la bodega.
 * - Sublotes más antiguos.
 * - Barra de navegación inferior.
 *
 * También permite ejecutar acciones
 * relacionadas con cada sección.
 *
 * IMPORTANTE:
 *
 * Los parámetros onSalir, onActualizar,
 * onRegistrarGasto y los demás son callbacks.
 *
 * Un callback es una función que se ejecuta
 * cuando el usuario realiza una acción.
 *
 * () -> Unit representa una función
 * que no recibe parámetros.
 *
 * (Int) -> Unit recibe un número entero.
 *
 * (String) -> Unit recibe un texto.
 *
 * = {} significa que, por defecto,
 * la función no realiza ninguna acción.
 */
@Composable
fun HomeScreen(

    // Acción para salir
    onSalir: () -> Unit = {},

    // Acción para actualizar la información
    onActualizar: () -> Unit = {},

    // Acción para registrar un gasto
    onRegistrarGasto: () -> Unit = {},

    // Acción para ajustar la capacidad de bodega
    onAjustarBodega: () -> Unit = {},

    // Acción para consultar todos los sublotes
    onVerSublotes: () -> Unit = {},

    // Acción para abrir un sublote determinado
    onAbrirSublote: (Int) -> Unit = {},

    // Acción para navegar a otras pantallas
    onNavegar: (String) -> Unit = {}
) {


    // ========================================================
    // 5. ESTADO DE LA NAVEGACIÓN INFERIOR
    // ========================================================

    /**
     * Almacena la sección seleccionada
     * dentro de la barra de navegación.
     *
     * rememberSaveable conserva el valor
     * durante recomposiciones y recreaciones
     * compatibles de la pantalla.
     *
     * mutableStateOf crea un estado observable.
     *
     * Inicialmente se selecciona "inicio".
     */
    var seccionSeleccionada by rememberSaveable {

        mutableStateOf("inicio")
    }


    // ========================================================
    // 6. ESTRUCTURA GENERAL DE LA PANTALLA
    // ========================================================

    /**
     * Scaffold organiza los componentes
     * principales de la interfaz.
     *
     * Permite definir:
     *
     * - Color de fondo.
     * - Barra inferior.
     * - Contenido principal.
     */
    Scaffold(

        // Color de fondo
        containerColor = Fondo,


        // ====================================================
        // 7. BARRA DE NAVEGACIÓN INFERIOR
        // ====================================================

        bottomBar = {

            /**
             * AppBottomBar es un componente
             * reutilizable del proyecto.
             *
             * Muestra las opciones de navegación
             * de la aplicación Café Smart.
             */
            AppBottomBar(

                // Ruta que está seleccionada
                selectedRoute = seccionSeleccionada,

                // Acción cuando se pulsa una opción
                onNavigate = { ruta ->

                    // Actualiza la opción seleccionada
                    seccionSeleccionada = ruta

                    // Informa a la aplicación
                    // sobre la ruta seleccionada
                    onNavegar(ruta)
                }
            )
        }

    ) { innerPadding ->


        // ====================================================
        // 8. CONTENIDO PRINCIPAL CON LAZYCOLUMN
        // ====================================================

        /**
         * LazyColumn organiza las diferentes
         * secciones verticalmente.
         *
         * A diferencia de Column con verticalScroll,
         * LazyColumn compone los elementos
         * cuando son necesarios.
         *
         * Esto facilita el desplazamiento
         * y permite ampliar la pantalla.
         *
         * innerPadding evita que el contenido
         * quede detrás de la barra inferior.
         *
         * item() agrega una sección a la lista.
         *
         * key identifica cada sección
         * mediante una clave estable.
         */
        LazyColumn(

            modifier = Modifier

                // Ocupa toda la pantalla
                .fillMaxSize()

                // Respeta el espacio del Scaffold
                .padding(innerPadding),

            // Agrega espacio al final del contenido
            contentPadding = PaddingValues(
                bottom = 24.dp
            )

        ) {


            // =================================================
            // 9. ENCABEZADO DE BIENVENIDA
            // =================================================

            item(key = "encabezado") {

                EncabezadoInicio(

                    onSalir = onSalir,

                    onActualizar = onActualizar
                )
            }


            // =================================================
            // 10. RESUMEN DEL DÍA
            // =================================================

            item(key = "resumen") {

                ResumenDelDia(

                    onRegistrarGasto = onRegistrarGasto
                )
            }


            // =================================================
            // 11. CAPACIDAD DE LA BODEGA
            // =================================================

            item(key = "bodega") {

                CapacidadBodega(

                    onAjustarBodega = onAjustarBodega
                )
            }


            // =================================================
            // 12. SUBLOTES MÁS ANTIGUOS
            // =================================================

            item(key = "sublotes") {

                SublotesAntiguos(

                    onVerSublotes = onVerSublotes,

                    onAbrirSublote = onAbrirSublote
                )
            }
        }
    }
}


// ============================================================
// 13. ENCABEZADO DE LA PANTALLA
// ============================================================

/**
 * EncabezadoInicio muestra:
 *
 * - Mensaje de bienvenida.
 * - Nombre de Café Smart.
 * - Botón de actualizar.
 * - Botón de salir.
 * - Indicador visual de conexión.
 *
 * Utiliza un fondo con degradado azul.
 */
@Composable
private fun EncabezadoInicio(

    onSalir: () -> Unit,

    onActualizar: () -> Unit
) {

    // Organiza verticalmente el encabezado
    Column(

        modifier = Modifier

            // Ocupa todo el ancho
            .fillMaxWidth()

            // Redondea las esquinas inferiores
            .clip(

                RoundedCornerShape(

                    bottomStart = 22.dp,

                    bottomEnd = 22.dp
                )
            )

            // Aplica el degradado de colores
            .background(

                Brush.linearGradient(

                    listOf(
                        AzulOscuro,
                        AzulClaro,
                        Azul
                    )
                )
            )

            // Espacios internos del encabezado
            .padding(

                start = 16.dp,

                end = 16.dp,

                top = 30.dp,

                bottom = 22.dp
            )

    ) {


        // ====================================================
        // 14. FILA DE BIENVENIDA Y BOTONES
        // ====================================================

        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically
        ) {


            // =================================================
            // 15. TEXTO DE BIENVENIDA
            // =================================================

            Column(

                /**
                 * weight(1f) permite ocupar
                 * el espacio disponible y dejar
                 * lugar para los botones.
                 */
                modifier = Modifier.weight(1f)
            ) {

                Text(

                    text = "Bienvenido a",

                    color = Color.White.copy(
                        alpha = 0.8f
                    ),

                    fontSize = 13.sp
                )

                Text(

                    text = "Café Smart",

                    color = Color.White,

                    fontSize = 23.sp,

                    fontWeight = FontWeight.Bold
                )
            }


            // =================================================
            // 16. BOTÓN ACTUALIZAR
            // =================================================

            /**
             * Permite solicitar la actualización
             * de la información del panel.
             *
             * La carga real de datos depende
             * de la función onActualizar.
             */
            Box(

                modifier = Modifier

                    // Tamaño del botón
                    .size(42.dp)

                    // Forma circular
                    .clip(CircleShape)

                    // Fondo semitransparente
                    .background(

                        Color.White.copy(
                            alpha = 0.17f
                        )
                    )

                    // Descripción para accesibilidad
                    .semantics {
                        contentDescription = "Actualizar panel"
                    }

                    // Ejecuta la acción al pulsar
                    .clickable(
                        onClick = onActualizar
                    ),

                contentAlignment = Alignment.Center
            ) {

                // Ícono reutilizable del proyecto
                CafeIcon(

                    name = "actualizar",

                    color = Color.White,

                    modifier = Modifier.size(23.dp)
                )
            }


            // Separación entre botones
            Spacer(
                modifier = Modifier.width(8.dp)
            )


            // =================================================
            // 17. BOTÓN SALIR
            // =================================================

            /**
             * Permite ejecutar la acción de salida.
             *
             * La navegación real se controla
             * mediante onSalir.
             */
            Button(

                onClick = onSalir,

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(

                    containerColor = Color(0xFFFFE5E8)
                ),

                contentPadding = PaddingValues(

                    horizontal = 12.dp,

                    vertical = 8.dp
                )
            ) {

                CafeIcon(

                    name = "salir",

                    color = Color(0xFFDC2626),

                    modifier = Modifier.size(20.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(

                    text = "Salir",

                    color = Color(0xFFDC2626),

                    fontWeight = FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier = Modifier.height(14.dp)
        )


        // ====================================================
        // 18. INDICADOR VISUAL DE CONEXIÓN
        // ====================================================

        /**
         * Muestra una etiqueta con el estado
         * de conexión a internet.
         *
         * IMPORTANTE:
         *
         * Actualmente es información de ejemplo.
         * No comprueba la conexión real del dispositivo.
         */
        Row(

            modifier = Modifier

                // Forma redondeada
                .clip(
                    RoundedCornerShape(30.dp)
                )

                // Fondo transparente
                .background(

                    Color.White.copy(
                        alpha = 0.15f
                    )
                )

                // Espacio interno
                .padding(

                    horizontal = 12.dp,

                    vertical = 7.dp
                ),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {


            // ================================================
            // PUNTO VERDE
            // ================================================

            Box(

                modifier = Modifier

                    .size(8.dp)

                    .background(

                        Color(0xFF4ADE80),

                        CircleShape
                    )
            )


            // ================================================
            // TEXTO DE CONEXIÓN
            // ================================================

            Text(

                text = "Conectado a internet",

                color = Color.White,

                fontSize = 12.sp
            )
        }
    }
}


// ============================================================
// 19. TÍTULO REUTILIZABLE PARA LAS SECCIONES
// ============================================================

/**
 * TituloSeccion muestra el título
 * de una sección y una acción opcional.
 *
 * Se utiliza en:
 *
 * - Resumen del día.
 * - Capacidad en bodega.
 * - Sublotes más antiguos.
 *
 * Parámetros:
 *
 * titulo: nombre de la sección.
 * accion: texto que aparece a la derecha.
 * onAccion: función que se ejecuta al pulsar.
 *
 * Si onAccion es null, el elemento
 * funciona solamente como etiqueta.
 */
@Composable
private fun TituloSeccion(

    titulo: String,

    accion: String,

    onAccion: (() -> Unit)? = null
) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically
    ) {


        // ====================================================
        // 20. NOMBRE DE LA SECCIÓN
        // ====================================================

        Text(

            text = titulo,

            // Ocupa el espacio disponible
            modifier = Modifier.weight(1f),

            color = Texto,

            fontWeight = FontWeight.Bold,

            fontSize = 17.sp
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )


        // ====================================================
        // 21. ETIQUETA O BOTÓN DE ACCIÓN
        // ====================================================

        /**
         * Cuando onAccion tiene una función,
         * el elemento puede pulsarse.
         *
         * Cuando es null, solamente muestra
         * una etiqueta informativa.
         *
         * Esto evita crear botones que
         * aparentemente funcionan,
         * pero no ejecutan ninguna acción.
         */
        Box(

            modifier = Modifier

                .clip(
                    RoundedCornerShape(12.dp)
                )

                .background(
                    Color(0xFFDCEAFF)
                )

                .then(

                    onAccion?.let { accionReal ->

                        Modifier.clickable(
                            onClick = accionReal
                        )

                    } ?: Modifier
                )

                .padding(

                    horizontal = 12.dp,

                    vertical = 8.dp
                )
        ) {

            Text(

                text = accion,

                color = Azul,

                fontSize = 12.sp,

                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ============================================================
// 22. RESUMEN DEL DÍA
// ============================================================

/**
 * ResumenDelDia muestra los indicadores
 * principales del negocio.
 *
 * Incluye:
 *
 * - Compras.
 * - Ventas.
 * - Kilogramos comprados.
 * - Productores.
 *
 * También incluye un botón
 * para registrar gastos.
 *
 * Actualmente utiliza datos de ejemplo.
 */
@Composable
private fun ResumenDelDia(

    onRegistrarGasto: () -> Unit
) {

    Column(

        modifier = Modifier.padding(

            start = 16.dp,

            end = 16.dp,

            top = 24.dp
        )
    ) {


        // ====================================================
        // 23. TÍTULO DEL RESUMEN
        // ====================================================

        TituloSeccion(

            titulo = "Resumen del día",

            // Es una etiqueta informativa
            accion = "Hoy"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // ====================================================
        // 24. TARJETA CONTENEDORA DEL RESUMEN
        // ====================================================

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(

                containerColor = Color.White
            )
        ) {

            Column(

                modifier = Modifier.padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {


                // ============================================
                // 25. PRIMERA FILA DE INDICADORES
                // ============================================

                /**
                 * Muestra las compras y ventas
                 * en dos tarjetas del mismo ancho.
                 */
                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {


                    // ========================================
                    // COMPRAS
                    // ========================================

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        TarjetaResumen(

                            titulo = "Compras",

                            valor = "0",

                            icono = "compras",

                            fondoIcono = Color(0xFFDCEAFF),

                            colorIcono = Color(0xFF2563EB)
                        )
                    }


                    // ========================================
                    // VENTAS
                    // ========================================

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        TarjetaResumen(

                            titulo = "Ventas",

                            valor = "0",

                            icono = "ventas",

                            fondoIcono = Color(0xFFDCF9E8),

                            colorIcono = Color(0xFF16A34A)
                        )
                    }
                }


                // ============================================
                // 26. SEGUNDA FILA DE INDICADORES
                // ============================================

                /**
                 * Muestra kilogramos comprados
                 * y cantidad de productores.
                 */
                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {


                    // ========================================
                    // KILOGRAMOS COMPRADOS
                    // ========================================

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        TarjetaResumen(

                            titulo = "Kg comprados",

                            valor = "0 kg",

                            icono = "balanza",

                            fondoIcono = Color(0xFFFFF0C8),

                            colorIcono = Color(0xFFE99000)
                        )
                    }


                    // ========================================
                    // PRODUCTORES
                    // ========================================

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        TarjetaResumen(

                            titulo = "Productores",

                            valor = "2",

                            icono = "usuarios",

                            fondoIcono = Color(0xFFEEE7FF),

                            colorIcono = Color(0xFF7C3AED)
                        )
                    }
                }


                // ============================================
                // 27. BOTÓN REGISTRAR GASTO
                // ============================================

                /**
                 * Ejecuta onRegistrarGasto.
                 *
                 * Su función real depende
                 * de la navegación de la aplicación.
                 */
                OutlinedButton(

                    onClick = onRegistrarGasto,

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(48.dp),

                    shape = RoundedCornerShape(12.dp),

                    border = BorderStroke(

                        1.dp,

                        Color(0xFFB7D1FF)
                    )
                ) {

                    CafeIcon(

                        name = "recibo",

                        color = AzulOscuro,

                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(

                        text = "Registrar gasto",

                        color = AzulOscuro,

                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// ============================================================
// 28. TARJETA INDIVIDUAL DE RESUMEN
// ============================================================

/**
 * TarjetaResumen es un componente reutilizable.
 *
 * Se utiliza para mostrar los cuatro
 * indicadores del resumen del día.
 *
 * Recibe:
 *
 * titulo: nombre del indicador.
 * valor: cantidad que debe mostrar.
 * icono: nombre del ícono.
 * fondoIcono: fondo del ícono.
 * colorIcono: color del ícono.
 */
@Composable
private fun TarjetaResumen(

    titulo: String,

    valor: String,

    icono: String,

    fondoIcono: Color,

    colorIcono: Color
) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            // Altura mínima
            .heightIn(min = 68.dp)

            // Borde de la tarjeta
            .border(

                width = 1.dp,

                color = Borde,

                shape = RoundedCornerShape(12.dp)
            )

            // Espacio interno
            .padding(10.dp),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {


        // ====================================================
        // 29. ÍCONO DEL INDICADOR
        // ====================================================

        Box(

            modifier = Modifier

                .size(36.dp)

                .clip(
                    RoundedCornerShape(10.dp)
                )

                .background(
                    fondoIcono
                ),

            contentAlignment = Alignment.Center
        ) {

            CafeIcon(

                name = icono,

                color = colorIcono,

                modifier = Modifier.size(23.dp)
            )
        }


        // ====================================================
        // 30. INFORMACIÓN DEL INDICADOR
        // ====================================================

        Column(

            modifier = Modifier.weight(1f)
        ) {


            // ================================================
            // NOMBRE DEL INDICADOR
            // ================================================

            Text(

                text = titulo,

                color = Gris,

                fontSize = 11.sp,

                lineHeight = 13.sp,

                // Máximo dos líneas
                maxLines = 2,

                // Agrega puntos suspensivos si no cabe
                overflow = TextOverflow.Ellipsis
            )


            // ================================================
            // VALOR DEL INDICADOR
            // ================================================

            Text(

                text = valor,

                color = Texto,

                fontSize = 18.sp,

                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// 31. SECCIÓN DE CAPACIDAD DE BODEGA
// ============================================================

/**
 * CapacidadBodega muestra la información
 * relacionada con el espacio ocupado.
 *
 * Incluye:
 *
 * - Porcentaje de ocupación.
 * - Barra de progreso.
 * - Kilogramos utilizados.
 * - Capacidad total.
 * - Opción para ajustar bodega.
 *
 * Actualmente utiliza valores de ejemplo.
 */
@Composable
private fun CapacidadBodega(

    onAjustarBodega: () -> Unit
) {


    // ========================================================
    // 32. VALORES DE EJEMPLO DE LA BODEGA
    // ========================================================

    /**
     * Representan la cantidad almacenada
     * y la capacidad total.
     *
     * Posteriormente podrán obtenerse
     * desde el inventario real.
     */
    val kilosUsados = 559

    val kilosTotales = 3000


    // ========================================================
    // 33. CÁLCULO DE OCUPACIÓN
    // ========================================================

    /**
     * Calcula la proporción de ocupación.
     *
     * toFloat() convierte el número
     * a un valor decimal.
     *
     * Ejemplo:
     *
     * 559 / 3000 = 0.1863
     *
     * Lo que equivale a 18.63%.
     *
     * coerceIn(0f, 1f) evita que el valor
     * sea menor que cero o mayor que uno.
     *
     * También se comprueba que la capacidad
     * total sea mayor que cero para evitar
     * una división entre cero.
     */
    val ocupacion = if (kilosTotales > 0) {

        (
                kilosUsados.toFloat() / kilosTotales
                ).coerceIn(0f, 1f)

    } else {

        0f
    }


    // ========================================================
    // 34. PORCENTAJE DE OCUPACIÓN
    // ========================================================

    /**
     * Multiplica la proporción por 100.
     *
     * roundToInt() redondea el resultado
     * a un número entero.
     *
     * En este ejemplo muestra 19%.
     */
    val porcentaje = (
            ocupacion * 100
            ).roundToInt()


    // ========================================================
    // 35. CONTENEDOR DE LA SECCIÓN
    // ========================================================

    Column(

        modifier = Modifier.padding(

            start = 16.dp,

            end = 16.dp,

            top = 26.dp
        )
    ) {


        // ====================================================
        // 36. TÍTULO Y ACCIÓN DE BODEGA
        // ====================================================

        TituloSeccion(

            titulo = "Capacidad en bodega",

            accion = "Ajustar bodega ›",

            onAccion = onAjustarBodega
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // ====================================================
        // 37. TARJETA DE CAPACIDAD
        // ====================================================

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(

                containerColor = Color.White
            )
        ) {

            Column(

                modifier = Modifier.padding(20.dp)
            ) {


                // ============================================
                // 38. PORCENTAJE ACTUAL
                // ============================================

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(

                        text = "Ocupación actual",

                        color = Texto,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.SemiBold
                    )

                    Text(

                        // Muestra el porcentaje calculado
                        text = "$porcentaje%",

                        color = Verde,

                        fontSize = 22.sp,

                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                // ============================================
                // 39. BARRA DE PROGRESO
                // ============================================

                /**
                 * LinearProgressIndicator representa
                 * visualmente el espacio ocupado.
                 *
                 * progress recibe un valor
                 * entre 0 y 1.
                 *
                 * Utiliza la misma variable ocupacion
                 * que se empleó para calcular
                 * el porcentaje.
                 *
                 * drawStopIndicator = {} evita mostrar
                 * el indicador decorativo del extremo.
                 */
                LinearProgressIndicator(

                    progress = {
                        ocupacion
                    },

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(10.dp)

                        .clip(CircleShape),

                    color = Verde,

                    trackColor = Color(0xFFEAF0F7),

                    gapSize = 0.dp,

                    drawStopIndicator = {}
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                // ============================================
                // 40. INFORMACIÓN DE KILOGRAMOS
                // ============================================

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {


                    // ========================================
                    // KILOGRAMOS UTILIZADOS
                    // ========================================

                    Column {

                        Text(

                            text = "Usados",

                            fontSize = 12.sp,

                            color = Gris
                        )

                        Text(

                            text = "$kilosUsados kg",

                            fontSize = 13.sp,

                            fontWeight = FontWeight.SemiBold
                        )
                    }


                    // ========================================
                    // CAPACIDAD TOTAL
                    // ========================================

                    Column(

                        horizontalAlignment = Alignment.End
                    ) {

                        Text(

                            text = "Total",

                            fontSize = 12.sp,

                            color = Gris
                        )

                        Text(

                            // Capacidad de ejemplo
                            text = "3.000 kg",

                            fontSize = 13.sp,

                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// 41. SECCIÓN DE SUBLOTES MÁS ANTIGUOS
// ============================================================

/**
 * SublotesAntiguos muestra los sublotes
 * que aparecen en la pantalla Home.
 *
 * Incluye:
 *
 * - Título de la sección.
 * - Opción para ver todos los sublotes.
 * - Lista de los sublotes de ejemplo.
 *
 * Cada sublote permite ejecutar
 * una acción al ser seleccionado.
 */
@Composable
private fun SublotesAntiguos(

    onVerSublotes: () -> Unit,

    onAbrirSublote: (Int) -> Unit
) {

    Column(

        modifier = Modifier.padding(

            start = 16.dp,

            end = 16.dp,

            top = 26.dp
        )
    ) {


        // ====================================================
        // 42. TÍTULO DE LA SECCIÓN
        // ====================================================

        TituloSeccion(

            titulo = "Sublotes más antiguos",

            accion = "Ver todos ›",

            onAccion = onVerSublotes
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // ====================================================
        // 43. TARJETA CONTENEDORA DE SUBLOTES
        // ====================================================

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(

                containerColor = Color.White
            )
        ) {

            Column(

                modifier = Modifier.padding(

                    horizontal = 16.dp,

                    vertical = 4.dp
                )
            ) {


                // ============================================
                // 44. RECORRER LA LISTA DE SUBLOTES
                // ============================================

                /**
                 * forEachIndexed recorre los elementos
                 * de una lista.
                 *
                 * Proporciona:
                 *
                 * index: posición del elemento.
                 * sublote: información del sublote.
                 *
                 * Las posiciones comienzan en cero.
                 *
                 * lastIndex es el índice
                 * del último elemento.
                 *
                 * Aquí se utiliza Column porque
                 * solamente existen tres sublotes
                 * dentro de una tarjeta.
                 *
                 * La pantalla principal ya utiliza
                 * LazyColumn para el desplazamiento.
                 */
                sublotesEjemplo.forEachIndexed {
                        index, sublote ->


                    // ========================================
                    // 45. FILA DEL SUBLOTE
                    // ========================================

                    FilaSublote(

                        sublote = sublote,

                        onClick = {

                            // Informa cuál sublote
                            // fue seleccionado
                            onAbrirSublote(index)
                        }
                    )


                    // ========================================
                    // 46. SEPARADOR ENTRE SUBLOTES
                    // ========================================

                    /**
                     * Muestra una línea entre filas.
                     *
                     * No agrega un separador
                     * después del último sublote.
                     */
                    if (
                        index != sublotesEjemplo.lastIndex
                    ) {

                        HorizontalDivider(
                            color = Borde
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// 47. FILA INDIVIDUAL DE UN SUBLOTE
// ============================================================

/**
 * FilaSublote muestra la información
 * de un sublote dentro de la lista.
 *
 * Contiene:
 *
 * - Ícono del tipo de café.
 * - Tipo de café.
 * - Antigüedad en días.
 * - Peso del sublote.
 * - Estado de calidad.
 * - Flecha para consultar detalles.
 *
 * El color del ícono y el estado
 * dependen de los datos del sublote.
 */
@Composable
private fun FilaSublote(

    sublote: SubloteEjemplo,

    onClick: () -> Unit
) {


    // ========================================================
    // 48. IDENTIFICAR TIPO DE CAFÉ
    // ========================================================

    /**
     * == compara si dos valores son iguales.
     *
     * Si el tipo es "Verde",
     * esVerde tendrá el valor true.
     */
    val esVerde = sublote.tipo == "Verde"


    // ========================================================
    // 49. IDENTIFICAR ESTADO DE CALIDAD
    // ========================================================

    /**
     * Comprueba si el sublote
     * tiene el estado "Regular".
     */
    val regular = sublote.estado == "Regular"


    // ========================================================
    // 50. CONTENEDOR DEL SUBLOTE
    // ========================================================

    Row(

        modifier = Modifier

            // Ocupa todo el ancho
            .fillMaxWidth()

            // Permite seleccionar el sublote
            .clickable(
                onClick = onClick
            )

            // Espacio vertical
            .padding(
                vertical = 12.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {


        // ====================================================
        // 51. ÍCONO DEL TIPO DE CAFÉ
        // ====================================================

        Box(

            modifier = Modifier

                .size(40.dp)

                .clip(CircleShape)

                // Color según el tipo de café
                .background(

                    if (esVerde) {

                        Color(0xFFE7FAF1)

                    } else {

                        Color(0xFFFFF8E4)
                    }
                ),

            contentAlignment = Alignment.Center
        ) {

            CafeIcon(

                // Ícono según el tipo de café
                name =
                    if (esVerde) {

                        "hoja"

                    } else {

                        "sol"
                    },

                color =
                    if (esVerde) {

                        Verde

                    } else {

                        Color(0xFFD99A00)
                    },

                modifier = Modifier.size(23.dp)
            )
        }


        Spacer(
            modifier = Modifier.width(12.dp)
        )


        // ====================================================
        // 52. TIPO Y ANTIGÜEDAD DEL SUBLOTE
        // ====================================================

        Column(

            modifier = Modifier.weight(1f)
        ) {


            // ================================================
            // TIPO DE CAFÉ
            // ================================================

            Text(

                text = sublote.tipo,

                fontWeight = FontWeight.SemiBold,

                color = Texto,

                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )


            // ================================================
            // ANTIGÜEDAD EN DÍAS
            // ================================================

            Row(

                verticalAlignment = Alignment.CenterVertically
            ) {

                CafeIcon(

                    name = "calendario",

                    color = Gris,

                    modifier = Modifier.size(13.dp)
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                /**
                 * ${...} inserta un valor
                 * dentro de una cadena de texto.
                 *
                 * Por ejemplo:
                 *
                 * Si dias es 26,
                 * se mostrará "26 días".
                 */
                Text(

                    text = "${sublote.dias} días",

                    color = Gris,

                    fontSize = 12.sp
                )
            }
        }


        // ====================================================
        // 53. PESO Y ESTADO DEL SUBLOTE
        // ====================================================

        Column(

            horizontalAlignment = Alignment.End
        ) {


            // ================================================
            // PESO DEL SUBLOTE
            // ================================================

            Text(

                text = sublote.peso,

                color = AzulOscuro,

                fontSize = 14.sp,

                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )


            // ================================================
            // 54. ETIQUETA DE CALIDAD
            // ================================================

            /**
             * Muestra una etiqueta con el estado.
             *
             * Si el estado es Regular,
             * utiliza un fondo amarillo.
             *
             * En caso contrario,
             * utiliza un fondo verde.
             */
            Box(

                modifier = Modifier

                    .clip(
                        RoundedCornerShape(4.dp)
                    )

                    .background(

                        if (regular) {

                            Color(0xFFFFF0BC)

                        } else {

                            Color(0xFFCDF8E0)
                        }
                    )

                    .padding(

                        horizontal = 8.dp,

                        vertical = 3.dp
                    )
            ) {

                Text(

                    text = sublote.estado,

                    color =
                        if (regular) {

                            Color(0xFF946000)

                        } else {

                            Color(0xFF006747)
                        },

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Medium
                )
            }
        }


        Spacer(
            modifier = Modifier.width(10.dp)
        )


        // ====================================================
        // 55. FLECHA PARA CONSULTAR DETALLES
        // ====================================================

        /**
         * Representa visualmente que el usuario
         * puede abrir el detalle del sublote.
         */
        Text(

            text = "›",

            color = Gris,

            fontSize = 25.sp
        )
    }
}


// ============================================================
// 56. VISTA PREVIA PARA ANDROID STUDIO
// ============================================================

/**
 * HomeScreenPreview permite visualizar
 * la pantalla principal directamente
 * en Android Studio.
 *
 * No necesita ejecutar MainActivity.
 *
 * Configuración:
 *
 * - Nombre: Inicio - Panel principal.
 * - Fondo visible.
 * - Ancho: 390 dp.
 * - Alto: 844 dp.
 *
 * CafesmartTheme aplica el tema
 * visual compartido del proyecto.
 */
@Preview(

    name = "Inicio - Panel principal",

    showBackground = true,

    widthDp = 390,

    heightDp = 844
)
@Composable
private fun HomeScreenPreview() {

    CafesmartTheme {

        HomeScreen()
    }
}
