package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.components.PieDeAccion
import com.cafesmart.app.ui.screens.purchase.NewProducerForm
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.contactos.ContactosUiState
import com.cafesmart.app.viewmodel.contactos.ContactosViewModel
import com.cafesmart.app.viewmodel.contactos.TipoContacto

// Propuesta basada en Figma; la edición y la sección de clientes requieren revisión del equipo.
@Composable
fun ContactosScreen(estado: ContactosUiState, vm: ContactosViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SurfaceScreen)) {
        EncabezadoPantalla(titulo = "Clientes y productores", onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16),
        ) {
            Row(Modifier.fillMaxWidth().background(SurfaceSoft, AppShapes.medium).padding(4.dp)) {
                TipoContacto.entries.forEach { tipo ->
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp)
                            .background(if (estado.tipo == tipo) SurfaceCard else SurfaceSoft, AppShapes.medium)
                            .selectable(selected = estado.tipo == tipo, role = Role.Tab, onClick = { vm.elegirTipo(tipo) }),
                        contentAlignment = Alignment.Center,
                    ) { Text(tipo.etiqueta, style = AppTypography.labelField) }
                }
            }
            if (estado.tipo == TipoContacto.CLIENTES) {
                Text("Los clientes aún no están disponibles.", style = AppTypography.bodyMd.copy(color = TextSecondary))
            } else {
                AppTextField("Buscar un contacto", estado.busqueda, vm::buscar, placeholder = "Nombre o documento")
                when {
                    estado.cargando -> CircularProgressIndicator(color = Primary)
                    estado.error != null && estado.productores.isEmpty() -> {
                        Text(estado.error, style = AppTypography.bodyMd.copy(color = ErrorText))
                        TextButton(onClick = vm::cargar) { Text("Reintentar") }
                    }
                    else -> {
                        Text("${estado.visibles.size} productores", style = AppTypography.labelField.copy(color = TextMuted))
                        if (estado.visibles.isEmpty()) Text("No se encontraron productores.", style = AppTypography.bodyMd)
                        estado.visibles.forEach { productor ->
                            Row(
                                Modifier.fillMaxWidth().background(SurfaceCard, AppShapes.medium)
                                    .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
                                    .clickable(role = Role.Button, onClickLabel = "Editar ${productor.nombre}") { vm.editar(productor) }
                                    .padding(Spacing.s16),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s12), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val iniciales = productor.nombre.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                                Box(Modifier.size(44.dp).background(SurfaceSelected, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(iniciales, style = AppTypography.labelField.copy(color = Primary))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(productor.nombre, style = AppTypography.labelField)
                                    Text("${if (productor.tipoDocumento == TipoDocumento.NIT) "NIT" else "Cédula"} ${productor.documento}", style = AppTypography.helper.copy(color = TextMuted))
                                    if (productor.telefono.isNotBlank()) Text(productor.telefono, style = AppTypography.helper.copy(color = TextMuted))
                                }
                                Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
        if (estado.tipo == TipoContacto.PRODUCTORES && !estado.cargando && estado.error == null) {
            PieDeAccion("Agregar productor", botonActivo = true, onBoton = vm::agregarProductor)
        }
    }
    if (estado.enEdicion != null || estado.creando) {
        AppBottomSheet(onDismiss = { if (!estado.guardando) vm.cancelarEdicion() }, skipPartiallyExpanded = true) {
            Column {
                estado.error?.let { Text(it, color = ErrorText, modifier = Modifier.padding(horizontal = Spacing.s20)) }
                NewProducerForm(
                    titulo = if (estado.creando) "Agregar productor" else "Modificar productor",
                    esEmpresa = estado.datos.tipo == TipoDocumento.NIT,
                    documento = estado.datos.documento, nombre = estado.datos.nombre, telefono = estado.datos.telefono,
                    documentoError = estado.errores.documento, nombreError = estado.errores.nombre, telefonoError = estado.errores.telefono,
                    onTipoDocumentoChange = vm::cambiarTipoDocumento, onDocumentoChange = vm::cambiarDocumento,
                    onNombreChange = vm::cambiarNombre, onTelefonoChange = vm::cambiarTelefono,
                    textoGuardar = "Guardar productor", guardarHabilitado = !estado.guardando,
                    onGuardar = vm::guardar, onCancelar = { if (!estado.guardando) vm.cancelarEdicion() },
                )
            }
        }
    }
}
