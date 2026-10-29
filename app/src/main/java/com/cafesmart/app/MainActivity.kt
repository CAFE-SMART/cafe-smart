
package com.cafesmart.app

// Permite recuperar información cuando Android crea la actividad.
import android.os.Bundle

// Herramientas para utilizar Jetpack Compose en Android.
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// Elementos para organizar los componentes visuales.
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

// Componentes de Material Design.
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

// Herramientas para manejar los estados de Compose.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Permite modificar las propiedades de los componentes.
import androidx.compose.ui.Modifier

// Herramienta para visualizar pantallas sin ejecutar la app.
import androidx.compose.ui.tooling.preview.Preview

// Componentes reutilizables creados por el equipo.
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.SegmentedProgressBar

// Pantallas de Café Smart.
import com.cafesmart.app.ui.screens.SplashScreen
import com.cafesmart.app.ui.screens.HomeScreen

// Estilos y tema de la aplicación.
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing

/**
 * ACTIVIDAD PRINCIPAL DE CAFÉ SMART
 *
 * MainActivity es el punto de entrada de la aplicación.
 *
 * ComponentActivity permite crear actividades Android
 * que muestran interfaces desarrolladas con Jetpack Compose.
 *
 * En esta clase se controla la secuencia inicial:
 *
 * 1. SplashScreen: pantalla de carga.
 * 2. HomeScreen: pantalla principal.
 */
class MainActivity : ComponentActivity() {

    /**
     * onCreate se ejecuta cuando Android crea la actividad.
     *
     * savedInstanceState contiene información que
     * Android puede recuperar de una instancia anterior.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Ejecuta la inicialización de ComponentActivity.
        super.onCreate(savedInstanceState)

        /*
         * Permite que la aplicación aproveche el espacio
         * de la pantalla, incluyendo las zonas cercanas
         * a las barras del sistema.
         */
        enableEdgeToEdge()

        /*
         * setContent establece la interfaz principal
         * utilizando Jetpack Compose.
         */
        setContent {

            /*
             * Aplicamos el tema compartido de Café Smart.
             * Este tema define los estilos generales.
             */
            CafesmartTheme {

                /*
                 * ESTADO DE LA PANTALLA INICIAL
                 *
                 * remember conserva el valor entre
                 * recomposiciones de Jetpack Compose.
                 *
                 * mutableStateOf crea un estado observable.
                 *
                 * true:
                 * Mostrar la pantalla de carga.
                 *
                 * false:
                 * Mostrar la pantalla Inicio.
                 *
                 * by permite utilizar directamente
                 * la variable sin escribir .value.
                 */
                var mostrarSplash by remember {
                    mutableStateOf(true)
                }

                /*
                 * CONTROL DE LA SECUENCIA
                 *
                 * if comprueba el estado mostrarSplash.
                 *
                 * Si es true, muestra SplashScreen.
                 *
                 * Cuando termina la animación, el callback
                 * cambia mostrarSplash a false.
                 *
                 * Compose detecta el cambio y reemplaza
                 * la pantalla Splash por Home.
                 */
                if (mostrarSplash) {

                    /*
                     * PRIMERA PANTALLA: SPLASH
                     *
                     * La pantalla de carga contiene:
                     * - Logo de Café Smart.
                     * - Fondo degradado.
                     * - Animaciones.
                     * - Nombre y eslogan.
                     */
                    SplashScreen(

                        /*
                         * onSplashFinished es un callback.
                         *
                         * Se ejecuta cuando SplashScreen
                         * termina su secuencia de animación.
                         */
                        onSplashFinished = {

                            /*
                             * Al cambiar a false,
                             * Compose muestra HomeScreen.
                             */
                            mostrarSplash = false
                        }
                    )

                } else {

                    /*
                     * SEGUNDA PANTALLA: HOME
                     *
                     * HomeScreen muestra:
                     * - Encabezado de bienvenida.
                     * - Resumen del día.
                     * - Capacidad en bodega.
                     * - Sublotes más antiguos.
                     * - Barra inferior de navegación.
                     *
                     * HomeScreen ya tiene su propio Scaffold.
                     * No agregamos otro para evitar duplicar
                     * la estructura de la interfaz.
                     */
                    HomeScreen()
                }
            }
        }
    }
}

/**
 * PANTALLA DE PRUEBA DE COMPONENTES BASE
 *
 * Esta función pertenece al trabajo previo del equipo.
 *
 * La conservamos completamente para poder probar:
 * - BackArrow.
 * - SegmentedProgressBar.
 * - ContinueButton.
 * - AppBottomSheet.
 *
 * Ya no aparece automáticamente al abrir la app,
 * porque ahora la primera pantalla es SplashScreen.
 *
 * modifier permite personalizar el diseño
 * desde la función que utiliza este componente.
 */
@Composable
fun ComponentsShowcase(
    modifier: Modifier = Modifier
) {

    /*
     * ESTADO DEL PASO ACTUAL
     *
     * step almacena el paso seleccionado.
     *
     * Inicia en 1.
     *
     * mutableStateOf permite que Compose actualice
     * la interfaz cuando cambia su valor.
     */
    var step by remember {
        mutableStateOf(1)
    }

    /*
     * ESTADO DEL MODAL INFERIOR
     *
     * false: el modal permanece oculto.
     * true: el modal aparece.
     */
    var sheetVisible by remember {
        mutableStateOf(false)
    }

    /*
     * CONTENEDOR VERTICAL
     *
     * Column organiza los elementos uno debajo del otro.
     *
     * fillMaxSize ocupa el espacio disponible.
     *
     * padding crea separaciones internas.
     *
     * Spacing contiene medidas reutilizables
     * definidas en el tema del proyecto.
     */
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s48),

        /*
         * spacedBy establece la separación vertical
         * entre los componentes de esta columna.
         */
        verticalArrangement = Arrangement.spacedBy(
            Spacing.s28
        )
    ) {

        /*
         * BOTÓN PARA REGRESAR
         *
         * BackArrow es un componente reutilizable.
         *
         * Al pulsarlo, disminuye el paso actual.
         *
         * coerceAtLeast(1) impide que el resultado
         * sea inferior a 1.
         *
         * Conservamos la operación original step - 2
         * para no cambiar la lógica previa del equipo.
         */
        BackArrow(
            onClick = {
                step = (step - 2).coerceAtLeast(1)
            }
        )

        /*
         * TÍTULO DE LA PANTALLA
         *
         * Utiliza un estilo tipográfico
         * definido en AppTypography.
         */
        Text(
            text = "Componentes base",
            style = AppTypography.titleScreen
        )

        /*
         * BARRA DE PROGRESO
         *
         * total = número de segmentos.
         * current = segmento activo.
         *
         * fillMaxWidth ocupa todo el ancho disponible.
         */
        SegmentedProgressBar(
            total = 3,
            current = step,
            modifier = Modifier.fillMaxWidth()
        )

        /*
         * BOTÓN CONTINUAR HABILITADO
         *
         * enabled = true permite pulsar el botón.
         *
         * Al pulsarlo, aumenta el paso actual.
         *
         * coerceAtMost(3) impide superar el paso 3.
         */
        ContinueButton(
            text = "Continuar",
            enabled = true,
            onClick = {
                step = (step + 1).coerceAtMost(3)
            }
        )

        /*
         * BOTÓN CONTINUAR DESHABILITADO
         *
         * enabled = false impide interactuar.
         *
         * onClick = {} representa una función
         * que no realiza ninguna acción.
         */
        ContinueButton(
            text = "Continuar",
            enabled = false,
            onClick = {}
        )

        /*
         * BOTÓN PARA ABRIR LA HOJA INFERIOR
         *
         * Cambia sheetVisible a true.
         *
         * Compose detecta el cambio y muestra
         * el componente AppBottomSheet.
         */
        ContinueButton(
            text = "Abrir hoja inferior",
            enabled = true,
            onClick = {
                sheetVisible = true
            }
        )
    }

    /*
     * MODAL INFERIOR
     *
     * if verifica si sheetVisible es true.
     *
     * Si se cumple, se muestra AppBottomSheet.
     */
    if (sheetVisible) {

        AppBottomSheet(

            /*
             * onDismiss se ejecuta cuando
             * el usuario cierra el modal.
             */
            onDismiss = {
                sheetVisible = false
            }

        ) {

            /*
             * CONTENIDO DEL MODAL
             *
             * Column organiza su contenido verticalmente.
             */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s20),

                verticalArrangement = Arrangement.spacedBy(
                    Spacing.s12
                )
            ) {

                // Título de la hoja inferior.
                Text(
                    text = "Hoja inferior",
                    style = AppTypography.titleModal
                )

                // Descripción de ejemplo.
                Text(
                    text = "Contenido de ejemplo del AppBottomSheet.",
                    style = AppTypography.bodyMd
                )
            }
        }
    }
}

/**
 * VISTA PREVIA DE COMPONENTES BASE
 *
 * Permite seguir comprobando los componentes
 * existentes sin cambiar MainActivity.
 */
@Preview(
    name = "Componentes base",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun ComponentsShowcasePreview() {

    CafesmartTheme {
        ComponentsShowcase()
    }
}

/**
 * VISTA PREVIA DE SPLASHSCREEN
 *
 * Permite observar la pantalla de carga
 * directamente desde Android Studio.
 *
 * Las animaciones pueden requerir
 * una vista previa interactiva o ejecución real.
 */
@Preview(
    name = "Splash - Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun SplashScreenPreview() {

    CafesmartTheme {
        SplashScreen()
    }
}

/**
 * VISTA PREVIA DE HOMESCREEN
 *
 * Permite observar el panel principal
 * sin modificar la navegación de la aplicación.
 */
@Preview(
    name = "Inicio - Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun HomeScreenPreview() {

    CafesmartTheme {
        HomeScreen()
    }
}
