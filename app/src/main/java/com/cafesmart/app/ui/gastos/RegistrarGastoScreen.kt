package com.cafesmart.app.ui.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.background
// Layouts, distribución del espacio y listas desplazables.
import androidx.compose.foundation.layout.*
// Fondos, formas, selección y acciones visuales de Compose.
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
// Controles de Material 3: tarjetas, textos, botones o superficies.
import androidx.compose.material3.*
// Composable, estados observables y efectos del ciclo de composición.
import androidx.compose.runtime.*
// Modificadores, alineación, dibujo, tipografía, unidades y accesibilidad.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Identificadores de los recursos locales de la aplicación.
import com.cafesmart.app.R
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.*
// Componentes reutilizables del proyecto.
import com.cafesmart.app.ui.components.*
// Componentes visuales específicos de Gastos.
import com.cafesmart.app.ui.gastos.components.*
// Colores, formas, tipografía y espaciados del tema compartido.
import com.cafesmart.app.ui.theme.*
// Estado, eventos y lógica de presentación del ViewModel.
import com.cafesmart.app.viewmodel.gastos.*
// Misma forma de mostrar un valor que Compras: "1500" se ve "1.500".
import com.cafesmart.app.viewmodel.purchase.precioEnPantalla

// ============================================================
// 2. FORMULARIO CONTROLADO POR EL VIEWMODEL
// ============================================================
/**
 * Registro a pantalla completa según Figma 4020:6 y 4020:113. No conoce el listado.
 * [onEditar] envía cambios del borrador al ViewModel; [onGuardar] solicita validación.
 * La confirmación y el resultado se presentan por el contenedor de navegación.
 *
 * Los campos muestran valores recibidos: no guardan un segundo borrador local. copy crea
 * un GastoFormulario nuevo con el cambio pedido y conserva los demás campos. Los errores
 * se muestran desde el estado, sin decidir aquí si un gasto puede persistirse.
 *
 * @param estado Incluye borrador, errores, sublotes disponibles y etapa del registro.
 * @param onEditar Recibe una transformación del borrador y la comunica al ViewModel.
 * @param onGuardar Solicita validar y abrir confirmación; no guarda directamente desde la pantalla.
 * @param onCancelar Solicita salir o cancelar el borrador según el contenedor.
 * @param modifier Ajustes externos de layout, incluidos los insets ya consumidos.
 */
@Composable
fun RegistrarGastoScreen(estado: GastosUiState, onEditar: ((GastoFormulario) -> GastoFormulario) -> Unit,
    onGuardar: () -> Unit, onCancelar: () -> Unit, modifier: Modifier = Modifier,
    onSeleccionarSublotes: () -> Unit = {},
    // true solo con el repositorio de demostración (en memoria). Con Room los gastos sí se guardan.
    demostracion: Boolean = false) {
    // Se toma el borrador de presentación y solo se habilita la edición en EDICION.
    val form = estado.formulario
    val habilitado = estado.resultado == ResultadoGasto.EDICION
    // El controlador del teclado permite ocultarlo al pulsar Guardar, antes de solicitar la validación.
    val teclado = LocalSoftwareKeyboardController.current
    // El contenedor ocupa el tamaño disponible y respeta barras e IME (teclado).
    // La columna interior usa weight y verticalScroll para desplazar campos manteniendo el encabezado.
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding().imePadding()) {
        GastosHeader("Registro de Gastos", { if (habilitado) onCancelar() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.s20, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s20)) {
            // Topes de escritura de ExpenseRules: el concepto no pasa de 60 caracteres y su contador se ve debajo (12/60).
            AppTextField("Concepto del gasto", form.concepto,
                { texto -> onEditar { it.copy(concepto = GastosValidation.limitarConcepto(texto)) } },
                modifier = Modifier.testTag("gastos_concepto"),
                placeholder = "Ej: Pago de jornaleros - Cosecha oct", error = estado.errores.concepto, enabled = habilitado, compact = true,
                helper = "${form.concepto.length}/${GastosValidation.CONCEPTO_MAX_CARACTERES}")
            GastoDescripcion(form.descripcion, estado.errores.descripcion, habilitado) { texto ->
                onEditar { it.copy(descripcion = GastosValidation.limitarDescripcion(texto)) }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Como el precio de Compras: el estado guarda solo dígitos (hasta 8) y aquí se ven con punto de miles ("1.500").
                // KeyboardType.Number abre el teclado numérico; si se pega texto, limpiarValor deja solo los dígitos.
                val monto: @Composable (Modifier) -> Unit = { campoModifier ->
                    AppTextField("Monto ($)", precioEnPantalla(GastosValidation.limpiarValor(form.monto)),
                        { texto -> onEditar { it.copy(monto = GastosValidation.limpiarValor(texto)) } },
                        modifier = campoModifier.testTag("gastos_monto"), placeholder = "0", keyboardType = KeyboardType.Number,
                        leadingText = "$", enabled = habilitado, error = estado.errores.monto)
                }
                // El calendario común recibe Fecha y el formateador de Gastos. Su callback solo actualiza
                // la fecha aceptada; cancelar conserva el valor. El icono original se inserta mediante trailingIcon.
                val fecha: @Composable (Modifier) -> Unit = { campoModifier -> AppDatePickerField(
                    label = "Fecha", fecha = form.fecha,
                    onFechaChange = { fecha -> onEditar { it.copy(fecha = fecha) } },
                    formato = ::fechaGasto, enabled = habilitado, error = estado.errores.fecha,
                    modifier = campoModifier,
                    trailingIcon = {
                        Icon(painterResource(R.drawable.gasto_calendar), null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                ) }
                // El símbolo y "50.000.000" necesitan ancho: en móviles no se divide Monto con Fecha.
                if (maxWidth < 480.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                        monto(Modifier.fillMaxWidth())
                        fecha(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                        monto(Modifier.weight(1f))
                        fecha(Modifier.weight(1f))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Text("Tipo de gasto", style = AppTypography.labelField)
                // chunked(3) organiza las seis categorías oficiales en dos filas. El tipo elegido
                // sigue siendo un solo valor del borrador, aunque se dibuje en dos filas.
                TipoGasto.entries.chunked(3).forEach { fila ->
                    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        fila.forEach { tipo ->
                            GastoOpcionCard(tipo.etiqueta, iconoTipoGasto(tipo), form.tipo == tipo,
                                { onEditar { it.copy(tipo = tipo) } }, Modifier.weight(1f), habilitado)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Text("Estado del pago", style = AppTypography.labelField)
                // El control segmentado reutilizado comunica Pagado/Pendiente mediante onEditar.
                GastosSegmentos(EstadoPago.entries, form.estado, { it.etiqueta }, { estado -> onEditar { it.copy(estado = estado) } }, habilitado)
            }
            GastoAsociacionSelector(estado, onEditar, habilitado, onSeleccionarSublotes)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                ContinueButton("Guardar gasto", { teclado?.hide(); onGuardar() }, habilitado)
                TextButton(onClick = onCancelar, enabled = habilitado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Cancelar", style = AppTypography.valueStrong, color = TextSecondary)
                }
            }
            Text(if (demostracion) "Demostración temporal · valores en COP, sin centavos." else "Valores en COP, sin centavos.",
                style = AppTypography.helper, modifier = Modifier.testTag("gastos_nota_formulario"))
            Spacer(Modifier.height(Spacing.s12))
        }
    }
}

/** El máximo ya existe en ExpenseRules. El campo acota su alto y conserva scroll interno. */
@Composable
private fun GastoDescripcion(valor: String, error: String?, habilitado: Boolean, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text("Descripción breve", style = AppTypography.labelField)
        OutlinedTextField(
            value = valor, onValueChange = onChange, enabled = habilitado,
            modifier = Modifier.fillMaxWidth().testTag("gastos_descripcion"),
            textStyle = AppTypography.bodyLg, minLines = 3, maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            placeholder = { Text("Detalles adicionales…", color = TextMuted) },
            isError = error != null, shape = AppShapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = Primary, unfocusedBorderColor = BorderField,
                errorBorderColor = ErrorText, errorContainerColor = SurfaceCard
            )
        )
        Text("${valor.length}/${GastosValidation.DESCRIPCION_MAX_CARACTERES}",
            style = AppTypography.helper, color = if (error != null) ErrorText else TextSecondary,
            modifier = Modifier.align(Alignment.End).testTag("gastos_descripcion_contador"))
        Text(error ?: if (valor.length >= GastosValidation.DESCRIPCION_MAX_CARACTERES)
            "Límite alcanzado. Puedes editar o borrar el texto."
        else "Opcional. Máximo ${GastosValidation.DESCRIPCION_MAX_CARACTERES} caracteres.",
            color = if (error != null) ErrorText else TextMuted, style = AppTypography.helper)
    }
}

// ============================================================
// 3. APLICACIÓN GENERAL O ASOCIACIÓN A SUBLOTES
// ============================================================
/**
 * Selector y reparto manual: solo ofrece sublotes proporcionados por el inventario.
 *
 * La selección pertenece al formulario compartido y se edita en EXPENSE_LINK_BATCHES.
 * Las opciones proceden del ViewModel; no se reparten costos automáticamente. Para cada sublote elegido
 * se pide un monto y la validación externa comprueba después que la suma coincida.
 *
 * @param estado Fuente de sublotes disponibles, selección y mensajes del formulario.
 * @param onEditar Comunica cambios de aplicación o del mapa de asignaciones.
 * @param habilitado Impide editar cuando no se está en la etapa EDICION.
 */
@Composable
private fun GastoAsociacionSelector(estado: GastosUiState, onEditar: ((GastoFormulario) -> GastoFormulario) -> Unit,
    habilitado: Boolean, onSeleccionarSublotes: () -> Unit) {
    val form = estado.formulario
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text("¿A qué aplica este gasto?", style = AppTypography.labelField)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            GastoOpcionCard("Gasto general", R.drawable.gasto_general, form.aplicaA == AplicaGasto.GENERAL,
                { onEditar { it.copy(aplicaA = AplicaGasto.GENERAL) } }, Modifier.weight(1f), habilitado, 86)
            GastoOpcionCard("Asociar a sublotes", R.drawable.gasto_sublotes, form.aplicaA == AplicaGasto.SUBLOTES,
                { onEditar { it.copy(aplicaA = AplicaGasto.SUBLOTES) } }, Modifier.weight(1f), habilitado, 86)
        }
        if (form.aplicaA == AplicaGasto.SUBLOTES) {
            OutlinedButton(onSeleccionarSublotes, enabled = habilitado, shape = AppShapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (form.asignaciones.isEmpty()) SurfaceCard else SurfaceSelected,
                    contentColor = PrimaryDark, disabledContainerColor = ButtonDisabled,
                    disabledContentColor = TextSecondary),
                contentPadding = PaddingValues(horizontal = Spacing.s16, vertical = Spacing.s12),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("gastos_seleccionar_sublotes")) {
                // labelLarge del tema tiene color blanco; definir color explícito también en Text.
                Text(etiquetaSeleccionSublotes(form.asignaciones.size),
                    color = if (habilitado) PrimaryDark else TextSecondary,
                    style = AppTypography.valueStrong, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
            Text(if (estado.sublotes.isEmpty()) "El inventario todavía no proporciona sublotes. Puedes registrar un gasto general."
                else "Indica cuánto corresponde a cada sublote. La suma debe coincidir con el total.", style = AppTypography.helper)
            // Se dibuja un campo por ID seleccionado para que el usuario escriba el reparto explícitamente.
            form.asignaciones.forEach { (id, monto) ->
                AppTextField("Asignación al sublote $id", precioEnPantalla(GastosValidation.limpiarValor(monto)),
                    { texto -> onEditar { it.copy(asignaciones = it.asignaciones + (id to GastosValidation.limpiarValor(texto))) } },
                    keyboardType = KeyboardType.Number, leadingText = "$", enabled = habilitado)
            }
            estado.errores.sublotes?.let { Text(it, color = ErrorText, style = AppTypography.helper) }
        }
    }
}

// ============================================================
// 4. VISTA PREVIA DEL REGISTRO
// ============================================================
/**
 * La muestra utiliza el estado inicial y el tema. Permite inspeccionar campos y estilos
 * en Android Studio, pero sus callbacks vacíos no ejecutan un registro real ni una prueba funcional.
 */
@Preview(showBackground = true, widthDp = 390, heightDp = 1033)
@Composable
private fun RegistrarGastoPreview() { CafesmartTheme { RegistrarGastoScreen(GastosUiState(), {}, {}, {}) } }
