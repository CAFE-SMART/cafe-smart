package com.cafesmart.app.ui.gastos

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.R
import com.cafesmart.app.data.model.*
import com.cafesmart.app.ui.components.*
import com.cafesmart.app.ui.gastos.components.*
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*

/**
 * Registro a pantalla completa según Figma 4020:6 y 4020:113. No conoce el listado.
 * [onEditar] envía cambios del borrador al ViewModel; [onGuardar] solicita validación.
 * La confirmación y el resultado se presentan por el contenedor de navegación.
 */
@Composable
fun RegistrarGastoScreen(estado: GastosUiState, onEditar: ((GastoFormulario) -> GastoFormulario) -> Unit,
    onGuardar: () -> Unit, onCancelar: () -> Unit, modifier: Modifier = Modifier) {
    val form = estado.formulario
    val habilitado = estado.resultado == ResultadoGasto.EDICION
    val teclado = LocalSoftwareKeyboardController.current
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding().imePadding()) {
        GastosHeader("Registro de Gastos", { if (habilitado) onCancelar() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.s20, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s20)) {
            AppTextField("Concepto del gasto", form.concepto, { texto -> onEditar { it.copy(concepto = texto) } },
                placeholder = "Ej: Pago de jornaleros - Cosecha oct", error = estado.errores.concepto, enabled = habilitado, compact = true)
            AppTextField("Descripción breve", form.descripcion, { texto -> onEditar { it.copy(descripcion = texto) } },
                placeholder = "Detalles adicionales…", singleLine = false, minLines = 3, enabled = habilitado)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                AppTextField("Monto ($)", form.monto, { texto -> onEditar { it.copy(monto = texto) } },
                    modifier = Modifier.weight(1f), placeholder = "0", keyboardType = KeyboardType.Number, leadingText = "$", enabled = habilitado, compact = true)
                FechaGastoCampo(form.fecha, { fecha -> onEditar { it.copy(fecha = fecha) } }, habilitado, Modifier.weight(1f))
            }
            if (estado.errores.monto != null) Text(estado.errores.monto, color = ErrorText, style = AppTypography.helper)
            if (estado.errores.fecha != null) Text(estado.errores.fecha, color = ErrorText, style = AppTypography.helper)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Text("Tipo de gasto", style = AppTypography.labelField)
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
                GastosSegmentos(EstadoPago.entries, form.estado, { it.etiqueta }, { estado -> onEditar { it.copy(estado = estado) } }, habilitado)
            }
            GastoAsociacionSelector(estado, onEditar, habilitado)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                ContinueButton("Guardar gasto", { teclado?.hide(); onGuardar() }, habilitado)
                TextButton(onClick = onCancelar, enabled = habilitado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Cancelar", style = AppTypography.valueStrong, color = TextSecondary)
                }
            }
            Text("Demostración temporal · COP en pesos enteros, sin centavos ni separadores.", style = AppTypography.helper)
            Spacer(Modifier.height(Spacing.s12))
        }
    }
}

/** DatePicker nativo: el mes se convierte de base cero al modelo Fecha y funciona desde API 24. */
@Composable
private fun FechaGastoCampo(fecha: Fecha, onFecha: (Fecha) -> Unit, habilitado: Boolean, modifier: Modifier) {
    val contexto = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text("Fecha", style = AppTypography.labelField)
        Surface(onClick = {
            DatePickerDialog(contexto, { _, anio, mes, dia -> onFecha(Fecha(dia, mes + 1, anio)) }, fecha.anio, fecha.mes - 1, fecha.dia).show()
        }, enabled = habilitado, shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderField)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = Spacing.s12),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(fechaGasto(fecha), style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.gasto_calendar), "Seleccionar fecha", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Selector y reparto manual: solo ofrece sublotes proporcionados por el inventario. */
@Composable
private fun GastoAsociacionSelector(estado: GastosUiState, onEditar: ((GastoFormulario) -> GastoFormulario) -> Unit, habilitado: Boolean) {
    val form = estado.formulario
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text("¿A qué aplica este gasto?", style = AppTypography.labelField)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            GastoOpcionCard("Gasto general", R.drawable.gasto_general, form.aplicaA == AplicaGasto.GENERAL,
                { onEditar { it.copy(aplicaA = AplicaGasto.GENERAL) } }, Modifier.weight(1f), habilitado, 86)
            GastoOpcionCard("Asociar a sublotes", R.drawable.gasto_sublotes, form.aplicaA == AplicaGasto.SUBLOTES,
                { onEditar { it.copy(aplicaA = AplicaGasto.SUBLOTES) } }, Modifier.weight(1f), habilitado, 86)
        }
        if (form.aplicaA == AplicaGasto.SUBLOTES) {
            Text("Seleccionar sublotes", style = AppTypography.labelField)
            Box {
                OutlinedButton({ abierto = true }, enabled = habilitado, shape = AppShapes.medium, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (form.asignaciones.isEmpty()) "Seleccionar…" else "${form.asignaciones.size} sublote(s) seleccionado(s)")
                }
                DropdownMenu(abierto, { abierto = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                    if (estado.sublotes.isEmpty()) DropdownMenuItem(text = { Text("No hay sublotes disponibles") }, onClick = { abierto = false })
                    estado.sublotes.forEach { sublote ->
                        DropdownMenuItem(text = { Text("Sublote ${sublote.id}") }, leadingIcon = {
                            Checkbox(sublote.id in form.asignaciones, onCheckedChange = null)
                        }, onClick = {
                            onEditar { actual -> actual.copy(asignaciones = if (sublote.id in actual.asignaciones)
                                actual.asignaciones - sublote.id else actual.asignaciones + (sublote.id to "")) }
                            abierto = false
                        })
                    }
                }
            }
            Text(if (estado.sublotes.isEmpty()) "El inventario todavía no proporciona sublotes. Puedes registrar un gasto general."
                else "Indica cuánto corresponde a cada sublote. La suma debe coincidir con el total.", style = AppTypography.helper)
            form.asignaciones.forEach { (id, monto) ->
                AppTextField("Asignación al sublote $id", monto, { texto -> onEditar { it.copy(asignaciones = it.asignaciones + (id to texto)) } },
                    keyboardType = KeyboardType.Number, leadingText = "$", enabled = habilitado)
            }
            estado.errores.sublotes?.let { Text(it, color = ErrorText, style = AppTypography.helper) }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1033)
@Composable
private fun RegistrarGastoPreview() { CafesmartTheme { RegistrarGastoScreen(GastosUiState(), {}, {}, {}) } }
