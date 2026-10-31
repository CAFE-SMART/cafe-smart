
package com.cafesmart.app.ui.components

// Herramientas para dibujar, organizar y dar estilo a la interfaz.
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Funciones matemáticas utilizadas para dibujar el sol.
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Colores de la barra.
// private evita que estas variables se utilicen fuera del archivo.
private val Azul = Color(0xFF2458D3)
private val Gris = Color(0xFF62738D)

/**
 * BARRA DE NAVEGACIÓN INFERIOR
 *
 * @Composable indica que la función construye una interfaz.
 *
 * selectedRoute: identifica la sección seleccionada.
 *
 * onNavigate: callback que recibe el nombre de la ruta.
 * Por ejemplo, si se pulsa Compras, recibe "compras".
 *
 * (String) -> Unit significa:
 * recibe un String y no devuelve un valor.
 *
 * modifier permite personalizar el componente desde
 * cualquier pantalla que lo reutilice.
 */
@Composable
fun AppBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    // listOf crea una lista con las cinco secciones.
    val rutas = listOf(
        "inicio",
        "compras",
        "inventario",
        "ventas",
        "ajustes"
    )

    // Box permite colocar elementos uno encima de otro.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

        // BASE BLANCA DE LA BARRA FLOTANTE
        Row(
            modifier = Modifier
                .fillMaxWidth()

                // Separación respecto a los bordes de la pantalla.
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
                .height(59.dp)

                // Sombra y esquinas redondeadas.
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(30.dp)
                )
                .background(
                    Color.White,
                    RoundedCornerShape(30.dp)
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * forEach recorre los elementos de la lista.
             *
             * En cada repetición, ruta contiene una sección:
             * "inicio", "compras", etc.
             */
            rutas.forEach { ruta ->

                /*
                 * El botón de Inventario es diferente:
                 * es circular y está elevado.
                 *
                 * Por eso reservamos su espacio
                 * en el centro de la barra.
                 */
                if (ruta == "inventario") {

                    Spacer(
                        Modifier.weight(1f)
                    )

                } else {

                    // Dibuja cada botón normal.
                    OpcionBarra(
                        ruta = ruta,

                        // == comprueba si las rutas son iguales.
                        seleccionada = selectedRoute == ruta,

                        // weight(1f) distribuye el espacio disponible.
                        modifier = Modifier.weight(1f),

                        onClick = {
                            onNavigate(ruta)
                        }
                    )
                }
            }
        }

        // BOTÓN CENTRAL DE INVENTARIO
        Box(
            modifier = Modifier
                .align(Alignment.Center)

                // Desplaza el botón 10 dp hacia arriba.
                .offset(y = (-10).dp)

                .size(62.dp)

                // Forma circular con sombra.
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)

                // Borde blanco alrededor del círculo azul.
                .padding(5.dp)
                .clip(CircleShape)

                // Degradado azul del botón.
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF3789FA),
                            Color(0xFF1744B7)
                        )
                    )
                )

                // Informa que se pulsó Inventario.
                .clickable {
                    onNavigate("inventario")
                },

            contentAlignment = Alignment.Center
        ) {

            CafeIcon(
                name = "inventario",
                color = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

/**
 * OPCIÓN INDIVIDUAL DE LA BARRA
 *
 * Esta función se reutiliza para:
 * Inicio, Compras, Ventas y Ajustes.
 *
 * seleccionado es Boolean:
 * true = activo.
 * false = inactivo.
 *
 * () -> Unit representa una función sin argumentos.
 */
@Composable
private fun OpcionBarra(
    ruta: String,
    seleccionada: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    /*
     * when es parecido a switch en otros lenguajes.
     *
     * Dependiendo de la ruta, determina el nombre
     * que aparecerá debajo del ícono.
     *
     * else se ejecuta si no coincide ninguna opción.
     */
    val nombre = when (ruta) {
        "inicio" -> "Inicio"
        "compras" -> "Compras"
        "ventas" -> "Ventas"
        "ajustes" -> "Ajustes"
        else -> ruta
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))

            // Si está seleccionado, muestra fondo azul claro.
            .background(
                if (seleccionada) {
                    Color(0xFFDDEBFF)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Ícono de la sección.
        CafeIcon(
            name = ruta,

            color = if (seleccionada) Azul else Gris,

            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.height(2.dp))

        // Nombre de la sección.
        Text(
            text = nombre,
            fontSize = 9.sp,
            maxLines = 1,

            // Cambia los estilos según el estado.
            color = if (seleccionada) Azul else Gris,

            fontWeight = if (seleccionada) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

/**
 * FUNCIÓN PÚBLICA DE ÍCONOS
 *
 * Se utiliza tanto en AppBottomBar.kt como en HomeScreen.kt.
 *
 * No debe tener la palabra private, porque HomeScreen
 * necesita importarla desde otro paquete.
 *
 * name: identifica el dibujo.
 * color: determina el color de las líneas.
 * modifier: permite establecer tamaño y otros atributos.
 *
 * Canvas es una superficie donde podemos dibujar
 * mediante líneas, círculos, curvas y rectángulos.
 */
@Composable
fun CafeIcon(
    name: String,
    color: Color,
    modifier: Modifier = Modifier
) {

    Canvas(modifier = modifier) {

        /*
         * withTransform permite transformar los dibujos.
         *
         * Los íconos se diseñaron utilizando una cuadrícula
         * imaginaria de 24 x 24.
         *
         * scaleX y scaleY ajustan esa cuadrícula al tamaño
         * real que se asignó al ícono.
         *
         * Offset.Zero representa el punto (0, 0).
         */
        withTransform({
            scale(
                scaleX = size.width / 24f,
                scaleY = size.height / 24f,
                pivot = Offset.Zero
            )
        }) {

            /*
             * Stroke define cómo se dibujan los contornos.
             *
             * width = grosor de la línea.
             * cap = forma de los extremos.
             *
             * Round hace que los extremos sean redondeados.
             */
            val trazo = Stroke(
                width = 1.8f,
                cap = StrokeCap.Round
            )

            /*
             * FUNCIÓN AUXILIAR PARA DIBUJAR LÍNEAS
             *
             * x1, y1: posición donde comienza.
             * x2, y2: posición donde termina.
             *
             * Offset representa una coordenada (X, Y).
             *
             * La letra f indica que el número es Float.
             * Float permite representar decimales.
             */
            fun linea(
                x1: Float,
                y1: Float,
                x2: Float,
                y2: Float
            ) {
                drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
            }

            /*
             * FUNCIÓN AUXILIAR PARA DIBUJAR RECTÁNGULOS
             *
             * x, y: posición inicial.
             * ancho, alto: dimensiones.
             * radio: redondeo de las esquinas.
             *
             * Size representa ancho y alto.
             * CornerRadius representa el radio de las esquinas.
             *
             * radio = 2f es un valor predeterminado.
             */
            fun rectangulo(
                x: Float,
                y: Float,
                ancho: Float,
                alto: Float,
                radio: Float = 2f
            ) {

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(ancho, alto),
                    cornerRadius = CornerRadius(radio),
                    style = trazo
                )
            }

            // Elige qué ícono dibujar según su nombre.
            when (name) {

                // ÍCONO INICIO: CASA
                "inicio" -> {

                    /*
                     * Path permite construir figuras
                     * uniendo varios puntos.
                     *
                     * moveTo mueve el punto de dibujo
                     * sin trazar una línea.
                     *
                     * lineTo dibuja una línea hasta
                     * la coordenada indicada.
                     */
                    val casa = Path().apply {

                        // Techo.
                        moveTo(3f, 11f)
                        lineTo(12f, 3f)
                        lineTo(21f, 11f)

                        // Paredes.
                        moveTo(5f, 10f)
                        lineTo(5f, 21f)
                        lineTo(19f, 21f)
                        lineTo(19f, 10f)

                        // Puerta.
                        moveTo(10f, 21f)
                        lineTo(10f, 14f)
                        lineTo(14f, 14f)
                        lineTo(14f, 21f)
                    }

                    // Pinta la figura construida.
                    drawPath(
                        casa,
                        color,
                        style = trazo
                    )
                }

                // ÍCONO COMPRAS: CARRITO
                "compras" -> {

                    // Mango y estructura del carrito.
                    linea(2f, 4f, 5f, 4f)
                    linea(5f, 4f, 8f, 17f)
                    linea(8f, 17f, 19f, 17f)

                    // Canasta del carrito.
                    linea(7f, 8f, 21f, 8f)
                    linea(21f, 8f, 19f, 15f)
                    linea(19f, 15f, 8f, 15f)

                    // Ruedas.
                    drawCircle(
                        color,
                        1.5f,
                        Offset(9f, 21f)
                    )

                    drawCircle(
                        color,
                        1.5f,
                        Offset(18f, 21f)
                    )
                }

                // ÍCONO VENTAS: BILLETE
                "ventas" -> {

                    // Contorno del billete.
                    rectangulo(2f, 6f, 20f, 12f)

                    // Círculo central.
                    drawCircle(
                        color,
                        3f,
                        Offset(12f, 12f),
                        style = trazo
                    )

                    // Detalles laterales.
                    drawCircle(
                        color,
                        1f,
                        Offset(5.5f, 12f)
                    )

                    drawCircle(
                        color,
                        1f,
                        Offset(18.5f, 12f)
                    )
                }

                // ÍCONO INVENTARIO: CAJA
                "inventario" -> {

                    // Tapa de la caja.
                    rectangulo(3f, 4f, 18f, 5f, 1f)

                    // Cuerpo.
                    rectangulo(5f, 9f, 14f, 12f, 1f)

                    // Línea central.
                    linea(10f, 13f, 14f, 13f)
                }

                // ÍCONO AJUSTES: ENGRANAJE
                "ajustes" -> {

                    // Círculo central.
                    drawCircle(
                        color,
                        4f,
                        Offset(12f, 12f),
                        style = trazo
                    )

                    /*
                     * Pair permite guardar dos valores juntos.
                     *
                     * Aquí representan las coordenadas X e Y.
                     *
                     * Pair(12f, 2f):
                     * first = 12f
                     * second = 2f
                     *
                     * listOf agrupa los cuatro puntos.
                     */
                    val puntos = listOf(
                        Pair(12f, 2f),  // Superior
                        Pair(12f, 22f), // Inferior
                        Pair(2f, 12f),  // Izquierda
                        Pair(22f, 12f)  // Derecha
                    )

                    /*
                     * forEach recorre cada Pair.
                     *
                     * punto representa el elemento actual.
                     */
                    puntos.forEach { punto ->

                        // Extraemos las coordenadas.
                        val x = punto.first
                        val y = punto.second

                        /*
                         * dx indica cuánto se mueve en X.
                         *
                         * Si x está a la izquierda del centro,
                         * el desplazamiento es positivo.
                         *
                         * Si está a la derecha, es negativo.
                         */
                        val dx = when {
                            x < 12f -> 2f
                            x > 12f -> -2f
                            else -> 0f
                        }

                        /*
                         * dy hace lo mismo para el eje Y.
                         *
                         * En Canvas, Y aumenta hacia abajo.
                         */
                        val dy = when {
                            y < 12f -> 2f
                            y > 12f -> -2f
                            else -> 0f
                        }

                        // Dibuja una línea hacia el centro.
                        linea(
                            x,
                            y,
                            x + dx,
                            y + dy
                        )
                    }

                    // Dientes diagonales del engranaje.
                    linea(5f, 5f, 7f, 7f)
                    linea(19f, 5f, 17f, 7f)
                    linea(5f, 19f, 7f, 17f)
                    linea(19f, 19f, 17f, 17f)
                }

                // ÍCONO BALANZA
                "balanza" -> {

                    // Base y soporte.
                    linea(12f, 3f, 12f, 21f)
                    linea(5f, 7f, 19f, 7f)
                    linea(8f, 4f, 16f, 4f)

                    // Plato izquierdo.
                    linea(7f, 7f, 4f, 15f)
                    linea(7f, 7f, 10f, 15f)

                    // Plato derecho.
                    linea(17f, 7f, 14f, 15f)
                    linea(17f, 7f, 20f, 15f)

                    // Bordes inferiores.
                    linea(3f, 15f, 11f, 15f)
                    linea(13f, 15f, 21f, 15f)
                    linea(7f, 21f, 17f, 21f)
                }

                // ÍCONO PRODUCTORES: PERSONAS
                "usuarios" -> {

                    // Cabeza de la primera persona.
                    drawCircle(
                        color,
                        3f,
                        Offset(10f, 7f),
                        style = trazo
                    )

                    /*
                     * drawArc dibuja un arco.
                     *
                     * startAngle: ángulo inicial.
                     * sweepAngle: cuántos grados recorre.
                     * useCenter = false: no une con el centro.
                     */
                    drawArc(
                        color,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(3f, 12f),
                        size = Size(14f, 9f),
                        style = trazo
                    )

                    // Segunda persona.
                    drawArc(
                        color,
                        startAngle = 265f,
                        sweepAngle = 190f,
                        useCenter = false,
                        topLeft = Offset(14f, 4f),
                        size = Size(6f, 6f),
                        style = trazo
                    )

                    linea(19f, 14f, 21f, 19f)
                }

                // ÍCONO ACTUALIZAR
                "actualizar" -> {

                    // Flecha circular.
                    drawArc(
                        color,
                        startAngle = 30f,
                        sweepAngle = 300f,
                        useCenter = false,
                        topLeft = Offset(4f, 4f),
                        size = Size(16f, 16f),
                        style = trazo
                    )

                    // Punta de la flecha.
                    linea(19f, 5f, 20f, 11f)
                    linea(20f, 11f, 14f, 10f)
                }

                // ÍCONO SALIR
                "salir" -> {

                    // Marco de una puerta.
                    linea(12f, 4f, 4f, 4f)
                    linea(4f, 4f, 4f, 20f)
                    linea(4f, 20f, 12f, 20f)

                    // Flecha hacia afuera.
                    linea(10f, 12f, 21f, 12f)
                    linea(17f, 8f, 21f, 12f)
                    linea(21f, 12f, 17f, 16f)
                }

                // ÍCONO REGISTRAR GASTO
                "recibo" -> {

                    // Contorno del documento.
                    rectangulo(5f, 3f, 14f, 18f)

                    // Líneas interiores.
                    linea(9f, 8f, 15f, 8f)
                    linea(9f, 12f, 15f, 12f)
                    linea(9f, 16f, 13f, 16f)
                }

                // ÍCONO SOL: CAFÉ SECO
                "sol" -> {

                    // Círculo central.
                    drawCircle(
                        color,
                        4f,
                        Offset(12f, 12f),
                        style = trazo
                    )

                    /*
                     * Este ciclo dibuja ocho rayos.
                     *
                     * 0..7 incluye ambos extremos:
                     * 0, 1, 2, 3, 4, 5, 6 y 7.
                     *
                     * PI representa el número pi.
                     *
                     * cos y sin calculan posiciones
                     * alrededor de una circunferencia.
                     *
                     * toFloat convierte el resultado a Float.
                     */
                    for (i in 0..7) {

                        // Ángulo del rayo actual.
                        val a = i * PI / 4.0

                        // Punto inicial del rayo.
                        val x1 = 12f + 7f * cos(a).toFloat()
                        val y1 = 12f + 7f * sin(a).toFloat()

                        // Punto final del rayo.
                        val x2 = 12f + 10f * cos(a).toFloat()
                        val y2 = 12f + 10f * sin(a).toFloat()

                        // Dibuja el rayo.
                        linea(x1, y1, x2, y2)
                    }
                }

                // ÍCONO HOJA: CAFÉ VERDE
                "hoja" -> {

                    /*
                     * cubicTo crea una curva de Bézier.
                     *
                     * Utiliza puntos de control para
                     * definir la forma de la curva.
                     */
                    val hoja = Path().apply {

                        moveTo(5f, 16f)

                        cubicTo(
                            3f, 7f,
                            12f, 3f,
                            20f, 4f
                        )

                        cubicTo(
                            21f, 14f,
                            13f, 21f,
                            5f, 16f
                        )
                    }

                    drawPath(
                        hoja,
                        color,
                        style = trazo
                    )

                    // Nervadura de la hoja.
                    linea(5f, 21f, 17f, 9f)
                }

                // ÍCONO CALENDARIO
                "calendario" -> {

                    // Contorno.
                    rectangulo(3f, 5f, 18f, 16f)

                    // División del encabezado.
                    linea(3f, 10f, 21f, 10f)

                    // Argollas superiores.
                    linea(8f, 3f, 8f, 7f)
                    linea(16f, 3f, 16f, 7f)
                }
            }
        }
    }
}

/**
 * VISTA PREVIA DE LA BARRA
 *
 * @Preview permite ver el componente en Android Studio.
 *
 * rememberSaveable recuerda la opción seleccionada.
 *
 * mutableStateOf crea un estado que Compose observa.
 *
 * Cuando cambia seleccion, la barra se actualiza.
 */
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 120
)
@Composable
private fun AppBottomBarPreview() {

    var seleccion by rememberSaveable {
        mutableStateOf("inicio")
    }

    AppBottomBar(
        selectedRoute = seleccion,

        onNavigate = { ruta ->
            // Guarda la nueva selección de la vista previa.
            seleccion = ruta
        }
    )
}
