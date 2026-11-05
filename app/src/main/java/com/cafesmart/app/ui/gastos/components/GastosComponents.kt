package com.cafesmart.app.ui.gastos.components

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Anotaciones para identificar recursos gráficos.
import androidx.annotation.DrawableRes
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
// Layouts, distribución del espacio y listas desplazables.
import androidx.compose.foundation.layout.*
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.selection.selectableGroup
// Controles de Material 3: tarjetas, textos, botones o superficies.
import androidx.compose.material3.*
// Composable, estados observables y efectos del ciclo de composición.
import androidx.compose.runtime.Composable
// Modificadores, alineación, dibujo, tipografía, unidades y accesibilidad.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// Identificadores de los recursos locales de la aplicación.
import com.cafesmart.app.R
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.TipoGasto
// Componentes reutilizables del proyecto.
import com.cafesmart.app.ui.components.BackArrow
// Colores, formas, tipografía y espaciados del tema compartido.
import com.cafesmart.app.ui.theme.*

// ============================================================
// 2. ENCABEZADO COMÚN DE GASTOS
// ============================================================
/**
 * Encabezado de las dos pantallas; [accion] reserva el espacio derecho sin descentrar el título.
 *
 * Box permite superponer el título centrado y las acciones en los extremos. Se reutiliza
 * BackArrow para volver, sin implementar navegación dentro de este componente.
 *
 * @param titulo Texto del encabezado.
 * @param onBack Callback de la flecha Volver.
 * @param accion Contenido Composable opcional del extremo derecho, como Actualizar.
 */
@Composable
fun GastosHeader(titulo: String, onBack: () -> Unit, accion: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = Spacing.s16)) {
        Box(Modifier.align(Alignment.CenterStart)) { BackArrow(onClick = onBack) }
        Text(titulo, style = AppTypography.titleScreen, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) { accion() }
    }
}

// ============================================================
// 3. RECURSO GRÁFICO DE CADA CATEGORÍA
// ============================================================
/**
 * Iconos originales exportados de Figma. No se usan emojis ni URLs temporales en ejecución.
 *
 * @DrawableRes indica que el Int devuelto identifica un drawable local. when cubre los
 * valores del enum oficial; no descarga imágenes ni decide categorías nuevas.
 *
 * @param tipo Categoría oficial del gasto cuyo icono se quiere mostrar.
 */
@DrawableRes
fun iconoTipoGasto(tipo: TipoGasto): Int = when (tipo) {
    TipoGasto.TRANSPORTE -> R.drawable.gasto_transporte
    TipoGasto.COMIDA -> R.drawable.gasto_comida
    TipoGasto.SECADO -> R.drawable.gasto_secado
    TipoGasto.CARGUE -> R.drawable.gasto_cargue
    TipoGasto.DESCARGUE -> R.drawable.gasto_descargue
    TipoGasto.OTROS -> R.drawable.gasto_otros
}

// ============================================================
// 4. TARJETA DE SELECCIÓN
// ============================================================
/**
 * Tarjeta vertical de categoría o aplicación; [alto] respeta sus dos variantes de Figma.
 *
 * La selección llega del consumidor: esta función no guarda un estado paralelo. Surface
 * reacciona a selected y enabled; el borde y el color expresan visualmente la opción activa.
 *
 * @param texto Etiqueta de la opción.
 * @param icono Identificador drawable del icono local.
 * @param seleccionado Indica si la opción es la elegida.
 * @param onClick Comunica la pulsación al consumidor.
 * @param modifier Permite distribuir la tarjeta, por ejemplo con weight.
 * @param habilitado Permite o bloquea la interacción.
 * @param alto Altura mínima de la variante, expresada en dp.
 */
@Composable
fun GastoOpcionCard(
    texto: String, @DrawableRes icono: Int, seleccionado: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, habilitado: Boolean = true, alto: Int = 82
) {
    Surface(
        selected = seleccionado, onClick = onClick, enabled = habilitado,
        color = if (seleccionado) SurfaceSelected else SurfaceCard,
        shape = AppShapes.medium,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) Primary else BorderDefault),
        // heightIn conserva un mínimo sin limitar el crecimiento; semantics identifica una opción de selección para accesibilidad.
        modifier = modifier.heightIn(min = alto.dp).semantics { role = Role.RadioButton }
    ) {
        Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s12),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s8, Alignment.CenterVertically)) {
            Icon(painterResource(icono), null, tint = if (seleccionado) Primary else TextMuted, modifier = Modifier.size(24.dp))
            Text(texto, style = AppTypography.bodyMd, color = if (seleccionado) Primary else TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

// ============================================================
// 5. CONTROL SEGMENTADO GENÉRICO DEL MÓDULO
// ============================================================
/**
 * Control segmentado propio de Gastos, compartido entre filtros y estado de pago.
 *
 * T permite reutilizar la misma presentación con filtros o estados de pago. No filtra ni
 * guarda por sí mismo: únicamente indica qué opción se pulsa.
 *
 * @param opciones Elementos que se dibujan en el control.
 * @param elegido Valor seleccionado recibido del estado.
 * @param etiqueta Función que convierte cada opción en su texto visible.
 * @param onElegir Callback que recibe la opción pulsada.
 * @param habilitado Controla si se pueden pulsar los segmentos.
 */
@Composable
fun <T> GastosSegmentos(opciones: List<T>, elegido: T, etiqueta: (T) -> String, onElegir: (T) -> Unit, habilitado: Boolean = true) {
    // selectableGroup reúne los segmentos como un grupo. Cada Surface usa weight(1f)
    // para repartir el ancho y comunica su estado mediante selected y el rol RadioButton.
    Row(Modifier.fillMaxWidth().background(BorderDefault, AppShapes.medium).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        opciones.forEach { opcion ->
            Surface(selected = opcion == elegido, onClick = { onElegir(opcion) }, enabled = habilitado,
                color = if (opcion == elegido) SurfaceCard else BorderDefault, shape = AppShapes.medium,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).semantics { role = Role.RadioButton }) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(etiqueta(opcion), style = AppTypography.buttonLink, color = if (opcion == elegido) Primary else TextSecondary)
                }
            }
        }
    }
}
