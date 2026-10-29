
package com.cafesmart.app.ui.screens

// Herramientas para crear animaciones en Jetpack Compose.
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

// Componentes para construir la interfaz.
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text

// Herramientas para manejar estados y ejecutar animaciones.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

// Propiedades visuales.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Permite ejecutar animaciones y pausas.
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Recursos del proyecto Café Smart.
import com.cafesmart.app.R

/*
 * TIPOGRAFÍA POPPINS
 *
 * Definimos una familia de fuentes para esta pantalla.
 *
 * Font relaciona un archivo .ttf con su peso.
 *
 * FontWeight.Normal: texto normal.
 * FontWeight.Bold: texto en negrilla.
 *
 * Poppins se utiliza únicamente en el Splash.
 * El resto de la aplicación conserva su tema global.
 */
private val Poppins = FontFamily(
    Font(
        R.font.poppins_regular,
        FontWeight.Normal
    ),
    Font(
        R.font.poppins_bold,
        FontWeight.Bold
    )
)

/*
 * PANTALLA DE CARGA
 *
 * @Composable indica que esta función construye una
 * interfaz de usuario con Jetpack Compose.
 *
 * onSplashFinished es un callback.
 *
 * Un callback es una función que se ejecuta cuando
 * ocurre un evento, en este caso cuando termina
 * toda la animación del Splash.
 *
 * () -> Unit significa que no recibe parámetros
 * y no devuelve ningún valor.
 *
 * = {} permite visualizar el Splash en Preview
 * aunque todavía no esté conectado a navegación.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit = {}
) {

    /*
     * DEGRADADO DEL FONDO
     *
     * Brush.verticalGradient combina varios colores
     * en dirección vertical.
     *
     * La parte superior comienza con azul claro,
     * pasa por un azul más oscuro y termina
     * en un tono casi negro.
     */
    val fondo = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3078EA),
            Color(0xFF1C397D),
            Color(0xFF030919)
        )
    )

    /*
     * ESTADOS DE LAS ANIMACIONES
     *
     * Animatable permite cambiar valores progresivamente.
     *
     * remember conserva los objetos durante
     * las recomposiciones de Jetpack Compose.
     *
     * Alpha controla la transparencia:
     * 0f = invisible.
     * 1f = completamente visible.
     */

    // Transparencia inicial del logo: invisible.
    val logoAlpha = remember {
        Animatable(0f)
    }

    /*
     * Escala inicial del logo.
     *
     * 0.8f significa que comienza al 80%
     * de su tamaño normal.
     *
     * Al llegar a 1f tendrá el 100%.
     */
    val logoScale = remember {
        Animatable(0.8f)
    }

    // El título comienza invisible.
    val tituloAlpha = remember {
        Animatable(0f)
    }

    /*
     * El título comienza desplazado hacia abajo.
     *
     * Luego se moverá hasta 0f,
     * que representa su posición normal.
     */
    val tituloOffset = remember {
        Animatable(30f)
    }

    // El eslogan comienza invisible.
    val esloganAlpha = remember {
        Animatable(0f)
    }

    /*
     * SECUENCIA DE ANIMACIONES
     *
     * LaunchedEffect(Unit) ejecuta este bloque
     * cuando el Splash entra en la composición.
     *
     * Las funciones animateTo cambian los valores
     * hasta alcanzar su objetivo.
     *
     * tween indica la duración de la transición
     * en milisegundos.
     *
     * launch permite ejecutar una animación
     * sin bloquear las siguientes instrucciones.
     */
    LaunchedEffect(Unit) {

        // 1. El logo aparece progresivamente.
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800
                )
            )
        }

        /*
         * 2. El logo aumenta suavemente de tamaño.
         *
         * FastOutSlowInEasing hace que el movimiento
         * empiece más rápido y termine lentamente.
         */
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )

        // Pausa breve antes de mostrar el título.
        delay(200)

        // 3. El título aparece progresivamente.
        launch {
            tituloAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500
                )
            )
        }

        // 4. El título sube hasta su posición original.
        tituloOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )

        // Pausa antes de mostrar el eslogan.
        delay(150)

        // 5. Aparece el eslogan.
        esloganAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 450
            )
        )

        // Dejamos visible el resultado final un momento.
        delay(400)

        /*
         * 6. AVISAMOS QUE FINALIZÓ EL SPLASH.
         *
         * Esta es la instrucción que permite
         * continuar hacia HomeScreen.
         *
         * No abre directamente HomeScreen.
         * Simplemente notifica que terminó la animación.
         *
         * MainActivity o el sistema de navegación
         * decidirá qué pantalla mostrar después.
         */
        onSplashFinished()
    }

    /*
     * CONTENEDOR PRINCIPAL
     *
     * Box permite colocar elementos dentro
     * de un mismo espacio.
     *
     * fillMaxSize utiliza toda la pantalla.
     *
     * background aplica el degradado azul.
     *
     * contentAlignment centra el contenido.
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {

        /*
         * Column organiza los elementos verticalmente:
         *
         * 1. Imagen central.
         * 2. Nombre Café Smart.
         * 3. Eslogan.
         */
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {

            /*
             * LOGO DE CAFÉ SMART
             *
             * painterResource carga la imagen guardada
             * en res/drawable.
             *
             * graphicsLayer permite aplicar
             * transparencia y escala.
             */
            Image(
                painter = painterResource(
                    id = R.drawable.splash_cafe
                ),
                contentDescription = "Logo de Café Smart",
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            /*
             * TÍTULO CAFÉ SMART
             *
             * Se muestra después de comenzar
             * la animación del logo.
             *
             * translationY modifica su posición vertical.
             */
            Text(
                text = "Café Smart",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = tituloAlpha.value
                    translationY = tituloOffset.value
                }
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * ESLOGAN
             *
             * Se muestra cuando termina
             * la entrada del título.
             *
             * Conserva la fuente Poppins Regular.
             */
            Text(
                text = "Compra, almacena y vende con claridad",
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = esloganAlpha.value
                }
            )
        }
    }
}

/*
 * VISTA PREVIA DE ANDROID STUDIO
 *
 * @Preview permite observar el diseño
 * sin ejecutar toda la aplicación.
 *
 * 390 x 844 dp es el tamaño de referencia
 * utilizado para comprobar el diseño móvil.
 *
 * Importante:
 * La vista previa estática no reproduce
 * necesariamente las animaciones de entrada.
 */
@Preview(
    name = "Splash - Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun SplashScreenPreview() {
    SplashScreen()
}
