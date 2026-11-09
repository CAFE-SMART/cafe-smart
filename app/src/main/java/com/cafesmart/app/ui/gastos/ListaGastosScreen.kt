package com.cafesmart.app.ui.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
// Layouts, distribución del espacio y listas desplazables.
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.shape.CircleShape
// Controles de Material 3: tarjetas, textos, botones o superficies.
import androidx.compose.material3.*
// Composable, estados observables y efectos del ciclo de composición.
import androidx.compose.runtime.Composable
// Modificadores, alineación, dibujo, tipografía, unidades y accesibilidad.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Identificadores de los recursos locales de la aplicación.
import com.cafesmart.app.R
import com.cafesmart.app.data.sample.gastosEjemplo
// Componentes reutilizables del proyecto.
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.AppIcons
// Componentes visuales específicos de Gastos.
import com.cafesmart.app.ui.gastos.components.*
// Colores, formas, tipografía y espaciados del tema compartido.
import com.cafesmart.app.ui.theme.*
// Estado, eventos y lógica de presentación del ViewModel.
import com.cafesmart.app.viewmodel.gastos.*

// ============================================================
// 2. PRESENTACIÓN DEL LISTADO DE GASTOS
// ============================================================
/**
 * Lista de gastos: presenta resumen, filtros y filas; no contiene formulario ni persistencia.
 * [estado] procede del ViewModel y [onRegistrar] navega a la pantalla de registro.
 *
 * La pantalla no modifica el repositorio ni administra el formulario. Usa propiedades
 * derivadas de GastosUiState para mostrar el filtro y el total; las pulsaciones emiten eventos.
 *
 * @param estado Datos que recibe del ViewModel, incluidos visibles y totalPendiente.
 * @param onBack Acción para volver a la pantalla anterior.
 * @param onRegistrar Acción para abrir el formulario.
 * @param onFiltro Comunica el filtro elegido al ViewModel.
 * @param modifier Ajustes de tamaño o insets que aplica el contenedor.
 * @param onActualizar Solicita releer la fuente demo; no implica una sincronización remota.
 */
@Composable
fun ListaGastosScreen(estado: GastosUiState, onBack: () -> Unit, onRegistrar: () -> Unit,
    onFiltro: (FiltroGasto) -> Unit, modifier: Modifier = Modifier, onActualizar: () -> Unit = {},
    onCargarEjemplos: (() -> Unit)? = null) {
    // fillMaxSize ocupa el área recibida; background aplica el fondo y safeDrawingPadding
    // respeta los espacios del sistema que aún no haya consumido el contenedor.
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        GastosHeader("Gastos generales", onBack) {
            IconButton(onClick = onActualizar) {
                Icon(painterResource(R.drawable.gasto_refresh), "Actualizar gastos", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        // weight(1f) ocupa el espacio restante bajo el encabezado. LazyColumn compone las filas
        // según se necesitan y contentPadding separa sus tarjetas de los bordes.
        LazyColumn(Modifier.weight(1f).testTag("gastos_lista"), contentPadding = PaddingValues(horizontal = Spacing.s16, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s20)) {
            // El total usa todos los pendientes. Cambiar el filtro de filas no cambia esa suma.
            item {
                Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Text("Total acumulado", style = AppTypography.caption)
                        Text(dineroGasto(estado.totalPendiente), style = AppTypography.displayValor, color = Primary)
                        Text("Suma de gastos pendientes por pagar.", style = AppTypography.bodyMd)
                    }
                }
            }
            item { ContinueButton(text = "Registrar gasto", onClick = onRegistrar,
                leadingIcon = { Icon(AppIcons.Plus, null, modifier = Modifier.size(20.dp)) }) }
            item { GastosSegmentos(FiltroGasto.entries, estado.filtro, { it.etiqueta }, onFiltro) }
            // Se diferencia el vacío general del vacío causado por el filtro seleccionado.
            if (estado.visibles.isEmpty()) item { GastoVacio(estado.gastos.isEmpty()) }
            if (estado.gastos.isEmpty() && onCargarEjemplos != null) item {
                TextButton(onClick = onCargarEjemplos, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Ver datos de ejemplo")
                }
            }
            // La key usa el ID del gasto para identificar cada fila al insertar, filtrar o recomponer.
            // La columna con weight distribuye el espacio entre los textos y el resto del contenido.
            items(estado.visibles, key = { it.id }) { gasto ->
                Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                            Icon(painterResource(iconoTipoGasto(gasto.tipo)), null, tint = Primary, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(gasto.concepto, style = AppTypography.titleCard)
                                Text("${gasto.tipo.etiqueta} · ${fechaGasto(gasto.fecha)}", style = AppTypography.caption)
                            }
                        }
                        Text(dineroGasto(gasto.monto), style = AppTypography.valueStrongBold)
                        Text(gasto.estadoPago.etiqueta, style = AppTypography.bodyMd)
                        if (gasto.descripcion.isNotBlank()) Text(gasto.descripcion, style = AppTypography.bodyMd)
                    }
                }
            }
            // Este aviso recuerda el límite real del repositorio: los datos son temporales en memoria.
            item { Text("Demostración: los gastos son temporales y se pierden al cerrar la sesión o el proceso.", style = AppTypography.helper) }
        }
    }
}

// ============================================================
// 3. ESTADO VACÍO DEL LISTADO
// ============================================================
/**
 * Distingue una lista sin registros de un filtro que no contiene resultados.
 *
 * La misma tarjeta explica dos situaciones distintas, evitando que un filtro sin coincidencias
 * parezca una pérdida de datos.
 *
 * @param sinRegistros true si la lista completa está vacía; false si solo el filtro actual no tiene resultados.
 */
@Composable
private fun GastoVacio(sinRegistros: Boolean) {
    Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Box(Modifier.size(64.dp).background(SurfaceSelected, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.gasto_empty), null, tint = Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Spacing.s4))
            Text(if (sinRegistros) "Aún no has registrado gastos" else "No hay gastos en este estado", style = AppTypography.titleScreen, textAlign = TextAlign.Center)
            Text(if (sinRegistros) "Registra el primero para empezar a gestionarlos." else "Selecciona otro filtro para ver tus gastos.",
                style = AppTypography.bodyLg, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

// ============================================================
// 4. VISTA PREVIA DEL LISTADO
// ============================================================
/**
 * GastosUiState vacío permite revisar la composición en el IDE. No ejecuta las pruebas
 * del listado ni demuestra persistencia: sus callbacks de muestra no realizan acciones.
 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ListaGastosPreview() { CafesmartTheme { ListaGastosScreen(GastosUiState(), {}, {}, {}) } }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ListaGastosConDatosPreview() {
    CafesmartTheme {
        ListaGastosScreen(GastosUiState(gastos = gastosEjemplo(), filtro = FiltroGasto.TODOS), {}, {}, {})
    }
}
