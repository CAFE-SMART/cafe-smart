package com.cafesmart.app.ui.home
import androidx.compose.foundation.BorderStroke
import com.cafesmart.app.ui.components.AppBottomBar
import com.cafesmart.app.ui.components.CafeIcon
// Importaciones para construir la interfaz.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Importamos los componentes que ya están separados.
// Así evitamos copiar la barra y los íconos nuevamente.
import com.cafesmart.app.ui.theme.CafesmartTheme

// Colores utilizados en esta pantalla.
private val Azul = Color(0xFF2458D3)
private val AzulOscuro = Color(0xFF203D91)
private val AzulClaro = Color(0xFF3889F8)
private val Fondo = Color(0xFFF3F5FB)
private val Texto = Color(0xFF1D293F)
private val Gris = Color(0xFF62738D)
private val Verde = Color(0xFF008767)
private val Borde = Color(0xFFE5EBF3)

/*
 * Clase para representar los sublotes de ejemplo.
 *
 * data class crea una clase destinada a almacenar datos.
 * Int representa un número entero.
 * String representa texto.
 *
 * private significa que solamente se utiliza en este archivo.
 */
private data class SubloteEjemplo(
    val tipo: String,
    val dias: Int,
    val peso: String,
    val estado: String
)

// listOf crea una lista con tres sublotes de ejemplo.
// Más adelante podrán reemplazarse por datos reales.
private val sublotesEjemplo = listOf(
    SubloteEjemplo("Seco", 26, "1 kg", "Regular"),
    SubloteEjemplo("Seco", 25, "190 kg", "Buena"),
    SubloteEjemplo("Verde", 15, "50 kg", "Buena")
)

/*
 * PANTALLA PRINCIPAL HOME
 *
 * @Composable indica que la función dibuja interfaz
 * utilizando Jetpack Compose.
 *
 * Los parámetros onSalir, onActualizar, etc. son callbacks.
 * Un callback es una función que se ejecuta cuando
 * ocurre una acción, por ejemplo al pulsar un botón.
 *
 * () -> Unit:
 * función que no recibe parámetros ni devuelve un resultado.
 *
 * (Int) -> Unit:
 * función que recibe un número entero.
 *
 * (String) -> Unit:
 * función que recibe un texto.
 *
 * = {} indica que, por defecto, no realiza ninguna acción.
 */
@Composable
fun HomeScreen(
    onSalir: () -> Unit = {},
    onActualizar: () -> Unit = {},
    onRegistrarGasto: () -> Unit = {},
    onAjustarBodega: () -> Unit = {},
    onVerSublotes: () -> Unit = {},
    onAbrirSublote: (Int) -> Unit = {},
    onNavegar: (String) -> Unit = {}
) {

    /*
     * rememberSaveable conserva el valor seleccionado
     * cuando Compose recompone la pantalla y en ciertas
     * recreaciones, como al girar el dispositivo.
     *
     * mutableStateOf crea un valor observable.
     *
     * by permite utilizar la variable directamente
     * sin escribir .value.
     */
    var seccionSeleccionada by rememberSaveable {
        mutableStateOf("inicio")
    }

    /*
     * Scaffold organiza las partes principales
     * de una pantalla.
     *
     * containerColor: color del fondo.
     * bottomBar: componente que aparece abajo.
     */
    Scaffold(
        containerColor = Fondo,

        bottomBar = {
            // Reutilizamos la barra definida en components.
            AppBottomBar(
                selectedRoute = seccionSeleccionada,

                onNavigate = { ruta ->

                    // Actualiza visualmente el botón seleccionado.
                    seccionSeleccionada = ruta

                    // Informa a la aplicación qué ruta se pulsó.
                    onNavegar(ruta)
                }
            )
        }

    ) { innerPadding ->

        /*
         * innerPadding es el espacio que Scaffold reserva
         * para evitar que el contenido se superponga
         * con la barra inferior.
         *
         * LazyColumn organiza elementos verticalmente.
         * También permite desplazamiento vertical (scroll).
         *
         * item agrega una sección dentro de la lista.
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // Primera sección: encabezado azul.
            item {
                EncabezadoInicio(
                    onSalir = onSalir,
                    onActualizar = onActualizar
                )
            }

            // Segunda sección: resumen del día.
            item {
                ResumenDelDia(
                    onRegistrarGasto = onRegistrarGasto
                )
            }

            // Tercera sección: capacidad de la bodega.
            item {
                CapacidadBodega(
                    onAjustarBodega = onAjustarBodega
                )
            }

            // Cuarta sección: sublotes más antiguos.
            item {
                SublotesAntiguos(
                    onVerSublotes = onVerSublotes,
                    onAbrirSublote = onAbrirSublote
                )
            }
        }
    }
}

/*
 * ENCABEZADO DE HOME
 *
 * Muestra:
 * - Bienvenido a Café Smart.
 * - Botón Actualizar.
 * - Botón Salir.
 * - Indicador Conectado a internet.
 */
@Composable
private fun EncabezadoInicio(
    onSalir: () -> Unit,
    onActualizar: () -> Unit
) {

    Column(
        modifier = Modifier

            // Ocupa todo el ancho disponible.
            .fillMaxWidth()

            // Redondea solamente las esquinas inferiores.
            .clip(
                RoundedCornerShape(
                    bottomStart = 22.dp,
                    bottomEnd = 22.dp
                )
            )

            // Brush permite crear degradados de colores.
            .background(
                Brush.linearGradient(
                    listOf(AzulOscuro, AzulClaro, Azul)
                )
            )

            // Espacio interior del encabezado.
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 30.dp,
                bottom = 22.dp
            )
    ) {

        // Row organiza los elementos horizontalmente.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                // weight(1f) utiliza el espacio disponible
                // y deja lugar para los botones.
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    "Bienvenido a",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                Text(
                    "Café Smart",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón circular de actualizar.
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = 0.17f)
                    )

                    // clickable ejecuta una acción al tocar.
                    .clickable(onClick = onActualizar),

                contentAlignment = Alignment.Center
            ) {

                // Ícono reutilizado desde AppBottomBar.kt.
                CafeIcon(
                    name = "actualizar",
                    color = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }

            // Separación horizontal de 8 dp.
            Spacer(Modifier.width(8.dp))

            // Botón para salir.
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

                Spacer(Modifier.width(6.dp))

                Text(
                    "Salir",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Indicador visual de conexión.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Color.White.copy(alpha = 0.15f)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Punto verde.
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        Color(0xFF4ADE80),
                        CircleShape
                    )
            )

            // Estado de ejemplo, no verifica internet realmente.
            Text(
                "Conectado a internet",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

/*
 * TÍTULO DE SECCIÓN
 *
 * Se utiliza tres veces:
 * - Resumen del día.
 * - Capacidad en bodega.
 * - Sublotes más antiguos.
 *
 * Recibe el título, el texto del botón y su acción.
 */
@Composable
private fun TituloSeccion(
    titulo: String,
    accion: String,
    onAccion: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            titulo,
            modifier = Modifier.weight(1f),
            color = Texto,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.width(8.dp))

        // Botón pequeño con fondo azul claro.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDCEAFF))
                .clickable(onClick = onAccion)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
        ) {

            Text(
                accion,
                color = Azul,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/*
 * RESUMEN DEL DÍA
 *
 * Contiene cuatro tarjetas con datos de ejemplo.
 * Dos tarjetas por fila.
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

        TituloSeccion(
            titulo = "Resumen del día",
            accion = "Hoy",
            onAccion = {}
        )

        Spacer(Modifier.height(12.dp))

        // Tarjeta blanca que agrupa todo el resumen.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp),

                // Coloca 12 dp de separación entre elementos.
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                // Primera fila: compras y ventas.
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Box(Modifier.weight(1f)) {
                        TarjetaResumen(
                            "Compras",
                            "0",
                            "compras",
                            Color(0xFFDCEAFF),
                            Color(0xFF2563EB)
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        TarjetaResumen(
                            "Ventas",
                            "0",
                            "ventas",
                            Color(0xFFDCF9E8),
                            Color(0xFF16A34A)
                        )
                    }
                }

                // Segunda fila: kilogramos y productores.
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Box(Modifier.weight(1f)) {
                        TarjetaResumen(
                            "Kg comprados",
                            "0 kg",
                            "balanza",
                            Color(0xFFFFF0C8),
                            Color(0xFFE99000)
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        TarjetaResumen(
                            "Productores",
                            "2",
                            "usuarios",
                            Color(0xFFEEE7FF),
                            Color(0xFF7C3AED)
                        )
                    }
                }

                // Botón para registrar un gasto.
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

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Registrar gasto",
                        color = AzulOscuro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/*
 * TARJETA DE RESUMEN
 *
 * Esta misma función construye las cuatro tarjetas.
 * Solo cambian los parámetros recibidos.
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

            // Altura mínima, puede crecer si el contenido lo exige.
            .heightIn(min = 68.dp)

            // Borde fino y esquinas redondeadas.
            .border(
                1.dp,
                Borde,
                RoundedCornerShape(12.dp)
            )

            .padding(10.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Fondo de color del ícono.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(fondoIcono),

            contentAlignment = Alignment.Center
        ) {

            CafeIcon(
                name = icono,
                color = colorIcono,
                modifier = Modifier.size(23.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {

            Text(
                titulo,
                color = Gris,
                fontSize = 11.sp,
                lineHeight = 13.sp,

                // Máximo dos líneas de texto.
                maxLines = 2,

                // Si no cabe, termina con puntos suspensivos.
                overflow = TextOverflow.Ellipsis
            )

            Text(
                valor,
                color = Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/*
 * CAPACIDAD EN BODEGA
 *
 * Muestra cuánto espacio está ocupado.
 * Los valores actuales son datos de ejemplo.
 */
@Composable
private fun CapacidadBodega(
    onAjustarBodega: () -> Unit
) {

    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 26.dp
        )
    ) {

        TituloSeccion(
            titulo = "Capacidad en bodega",
            accion = "Ajustar bodega ›",
            onAccion = onAjustarBodega
        )

        Spacer(Modifier.height(12.dp))

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

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "Ocupación actual",
                        color = Texto,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "19%",
                        color = Verde,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                /*
                 * Barra de progreso.
                 *
                 * 559f / 3000f calcula la proporción ocupada.
                 * La f indica que los números son Float.
                 *
                 * Float permite trabajar con decimales.
                 * El resultado es aproximadamente 0.1863,
                 * equivalente a 18.63%, redondeado a 19%.
                 *
                 * drawStopIndicator = {} evita mostrar
                 * el indicador decorativo del extremo.
                 */
                LinearProgressIndicator(
                    progress = { 559f / 3000f },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),

                    color = Verde,
                    trackColor = Color(0xFFEAF0F7),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )

                Spacer(Modifier.height(12.dp))

                // Información inferior de la bodega.
                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {
                        Text(
                            "Usados",
                            fontSize = 12.sp,
                            color = Gris
                        )

                        Text(
                            "559 kg",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            "Total",
                            fontSize = 12.sp,
                            color = Gris
                        )

                        Text(
                            "3.000 kg",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/*
 * SUBLOTES MÁS ANTIGUOS
 *
 * Recorre los tres sublotes de ejemplo.
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

        TituloSeccion(
            titulo = "Sublotes más antiguos",
            accion = "Ver todos ›",
            onAccion = onVerSublotes
        )

        Spacer(Modifier.height(12.dp))

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

                /*
                 * forEachIndexed recorre la lista y entrega:
                 *
                 * index: posición del elemento.
                 * sublote: objeto que contiene sus datos.
                 *
                 * lastIndex es la última posición de la lista.
                 * Si hay tres elementos, lastIndex es 2,
                 * porque las posiciones comienzan en 0.
                 */
                sublotesEjemplo.forEachIndexed { index, sublote ->

                    FilaSublote(
                        sublote = sublote,

                        onClick = {
                            // Envía la posición del sublote pulsado.
                            onAbrirSublote(index)
                        }
                    )

                    // Agrega un separador entre filas,
                    // pero no después de la última.
                    if (index != sublotesEjemplo.lastIndex) {
                        HorizontalDivider(color = Borde)
                    }
                }
            }
        }
    }
}

/*
 * FILA INDIVIDUAL DE SUBLOTE
 *
 * Cambia el color según el tipo de café y el estado.
 */
@Composable
private fun FilaSublote(
    sublote: SubloteEjemplo,
    onClick: () -> Unit
) {

    // == compara si ambos valores son iguales.
    val esVerde = sublote.tipo == "Verde"
    val regular = sublote.estado == "Regular"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        // Círculo que contiene el ícono del tipo de café.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (esVerde) Color(0xFFE7FAF1)
                    else Color(0xFFFFF8E4)
                ),

            contentAlignment = Alignment.Center
        ) {

            CafeIcon(
                name = if (esVerde) "hoja" else "sol",
                color = if (esVerde) Verde
                else Color(0xFFD99A00),
                modifier = Modifier.size(23.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Tipo y antigüedad.
        Column(modifier = Modifier.weight(1f)) {

            Text(
                sublote.tipo,
                fontWeight = FontWeight.SemiBold,
                color = Texto,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                CafeIcon(
                    name = "calendario",
                    color = Gris,
                    modifier = Modifier.size(13.dp)
                )

                Spacer(Modifier.width(4.dp))

                /*
                 * ${...} inserta el valor de una expresión
                 * dentro de un texto.
                 *
                 * Ejemplo:
                 * si sublote.dias es 26, muestra "26 días".
                 */
                Text(
                    "${sublote.dias} días",
                    color = Gris,
                    fontSize = 12.sp
                )
            }
        }

        // Peso y estado de calidad.
        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                sublote.peso,
                color = AzulOscuro,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (regular) Color(0xFFFFF0BC)
                        else Color(0xFFCDF8E0)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 3.dp
                    )
            ) {

                Text(
                    sublote.estado,

                    color = if (regular)
                        Color(0xFF946000)
                    else Color(0xFF006747),

                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Flecha de acceso al detalle.
        Text(
            "›",
            color = Gris,
            fontSize = 25.sp
        )
    }
}

/*
 * PREVIEW
 *
 * Permite comprobar Home sin modificar MainActivity.
 *
 * widthDp y heightDp establecen el tamaño
 * de referencia del diseño.
 *
 * CafesmartTheme aplica el tema compartido del proyecto.
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
