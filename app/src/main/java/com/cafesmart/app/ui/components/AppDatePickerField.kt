package com.cafesmart.app.ui.components

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Calendario nativo de Android.
import android.app.DatePickerDialog
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.BorderStroke
// Layouts, distribución del espacio y listas desplazables.
import androidx.compose.foundation.layout.*
// Controles de Material 3: tarjetas, textos, botones o superficies.
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Composable, estados observables y efectos del ciclo de composición.
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// Modificadores, alineación, dibujo, tipografía, unidades y accesibilidad.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.Fecha
// Colores, formas, tipografía y espaciados del tema compartido.
import com.cafesmart.app.ui.theme.*

// ============================================================
// 2. CAMPO REUTILIZABLE Y CALENDARIO NATIVO
// ============================================================
/**
 * Campo controlado de fecha con calendario nativo, compatible con API 24.
 *
 * [fecha] usa el valor civil día/mes/año de [Fecha], sin convertir milisegundos ni zonas horarias.
 * [formato] pertenece a la presentación del consumidor; [onFechaChange] se emite solo al aceptar.
 * Cancelar no cambia el valor. [error] muestra el resultado de validaciones externas: aquí no
 * se prohíben fechas futuras ni se aplican reglas de Gastos, Compras o Secado.
 * [trailingIcon] permite conservar recursos existentes sin acoplar el campo a ningún módulo.
 * La apertura se guarda en Compose y el diálogo se destruye al abandonar/recrear su contexto.
 *
 * El valor pertenece al consumidor: este campo no crea un ViewModel ni conoce entidades
 * de Gastos o Compras. Recibe Fecha, un dato civil compartido, y devuelve otra Fecha al aceptar.
 * La presentación puede reutilizarse en otros formularios pasando sus callbacks y su formato.
 *
 * @param label Etiqueta visible del campo.
 * @param fecha Fecha civil válida recibida del formulario.
 * @param onFechaChange Callback llamado solo cuando el usuario confirma una selección.
 * @param formato Función que convierte Fecha al texto visible; no valida reglas de negocio.
 * @param modifier Ajustes de layout del consumidor.
 * @param enabled Permite o bloquea abrir el calendario.
 * @param error Mensaje opcional producido por la validación externa.
 * @param calendarDescription Descripción de la acción para accesibilidad.
 * @param trailingIcon Contenido Composable del icono; permite reutilizar recursos del consumidor.
 */
@Composable
fun AppDatePickerField(
    label: String,
    fecha: Fecha,
    onFechaChange: (Fecha) -> Unit,
    formato: (Fecha) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    calendarDescription: String = "Seleccionar fecha",
    trailingIcon: @Composable () -> Unit = {
        Icon(AppIcons.Calendar, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
) {
    // LocalContext proporciona el contexto Android con el que se crea la ventana nativa.
    val contexto = LocalContext.current
    // mutableStateOf hace observable abierto. rememberSaveable conserva esa apertura mediante
    // el estado guardable de Compose; no guarda la fecha en una base de datos. by simplifica su lectura y escritura.
    var abierto by rememberSaveable { mutableStateOf(false) }
    // rememberUpdatedState conserva la referencia más reciente del callback y de la fecha
    // sin reconstruir el diálogo en cada recomposición del consumidor.
    val notificarCambio by rememberUpdatedState(onFechaChange)
    val fechaActual by rememberUpdatedState(fecha)
    // La función de formato la decide el formulario: aquí no se fuerza COP, UTC ni un patrón de otro módulo.
    val texto = formato(fecha)

    // Este efecto se ejecuta al cambiar enabled y cierra una apertura pendiente si se deshabilita el campo.
    LaunchedEffect(enabled) { if (!enabled) abierto = false }
    // Un único efecto crea la ventana: dos pulsaciones no construyen dos DatePickerDialog.
    // DisposableEffect crea recursos al cambiar sus claves y los libera en onDispose.
    // Si abierto sigue en true al recomponer, no se añade otra ventana por ese simple redibujo.
    DisposableEffect(abierto, contexto, enabled) {
        val dialogo = if (abierto && enabled) {
            DatePickerDialog(contexto, { _, anio, mes, dia ->
                abierto = false
                // Android cuenta los meses desde cero; el modelo de fecha usa 1 a 12.
                notificarCambio(Fecha(dia, mes + 1, anio))
            }, fechaActual.anio, fechaActual.mes - 1, fechaActual.dia).apply {
                // Cancelar o cerrar la ventana solo cambia abierto. No se llama a onFechaChange,
                // por lo que el valor anterior del formulario permanece intacto.
                setOnDismissListener { abierto = false }
                show()
            }
        } else null
        onDispose {
            // Evita ventanas retenidas y callbacks de cierre sobre una composición destruida.
            dialogo?.setOnDismissListener(null)
            dialogo?.dismiss()
        }
    }

    // Column coloca etiqueta, campo y error uno debajo de otro usando el espaciado del tema.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(label, style = AppTypography.labelField)
        Surface(
            onClick = { abierto = true }, enabled = enabled,
            shape = AppShapes.medium, color = SurfaceCard,
            border = BorderStroke(1.dp, if (error == null) BorderField else ErrorDot),
            // Estas propiedades describen acción, fecha, rol y error para servicios de accesibilidad;
            // no alteran la fecha ni las reglas de validación.
            modifier = Modifier.semantics {
                contentDescription = calendarDescription
                stateDescription = texto
                role = Role.Button
                if (error != null) this.error(error)
            }
        ) {
            // fillMaxWidth aprovecha el ancho; heightIn conserva un mínimo táctil de 48 dp.
            // weight(1f) deja espacio al icono y padding separa el texto de los bordes.
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = Spacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(texto, style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
                trailingIcon()
            }
        }
        // El mensaje solo se dibuja cuando existe. La compatibilidad API 24 se mantiene
        // usando DatePickerDialog y día/mes/año, sin introducir java.time ni conversiones de milisegundos.
        error?.let { Text(it, color = ErrorText, style = AppTypography.helper) }
    }
}
