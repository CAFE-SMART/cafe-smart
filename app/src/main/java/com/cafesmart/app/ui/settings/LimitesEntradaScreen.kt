package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.limites.*
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.AvisoSuperior
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import com.cafesmart.app.viewmodel.purchase.formatoMiles
import androidx.compose.ui.Alignment
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.components.PieDeAccion
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.limites.LimitesDeEntrada

// Propuesta pendiente de revisión: el precio es compartido y Gastos no figura en el Figma.
@Composable
fun LimitesEntradaScreen(configuracion: ConfiguracionLimites, onBack: () -> Unit) {
    var borrador by remember { mutableStateOf(BorradorLimites.de(configuracion.cargar())) }
    var errores by remember { mutableStateOf<Map<CampoLimite, String>>(emptyMap()) }
    // Aviso superior del sistema de la app (verde al guardar o restablecer, rojo si hay errores). Se anuncia a
    // TalkBack (liveRegion) y queda visible arriba aunque el formulario esté desplazado.
    var aviso by remember { mutableStateOf<AvisoUi?>(null) }
    fun editar(nuevo: BorradorLimites) { borrador = nuevo; errores = emptyMap(); aviso = null }

  Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(SurfaceScreen).imePadding()) {
        EncabezadoPantalla(titulo = "Límites de entrada", onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            Text("Aquí decides cuánto puedes registrar. Si escribes algo fuera de estos límites, la app te avisa.", style = AppTypography.bodyLg.copy(color = TextSecondary))
            GrupoLimites("Peso de cada compra", "Los kilos que puedes registrar en una sola compra.", WarningBg,
                borrador.pesoMin, borrador.pesoMax, "kg", errores[CampoLimite.PESO_MIN], errores[CampoLimite.PESO_MAX],
                { editar(borrador.copy(pesoMin = it)) }, { editar(borrador.copy(pesoMax = it)) })
            GrupoLimites("Precio por kg de compra y venta", "El mismo límite se aplica a compras y ventas.", SurfaceSelected,
                borrador.precioMin, borrador.precioMax, "$ por kg", errores[CampoLimite.PRECIO_MIN], errores[CampoLimite.PRECIO_MAX],
                { editar(borrador.copy(precioMin = it)) }, { editar(borrador.copy(precioMax = it)) })
            GrupoLimites("Valor de cada gasto", "Lo mínimo y lo máximo para registrar un gasto.", SurfaceSoft,
                borrador.gastoMin, borrador.gastoMax, "pesos", errores[CampoLimite.GASTO_MIN], errores[CampoLimite.GASTO_MAX],
                { editar(borrador.copy(gastoMin = it)) }, { editar(borrador.copy(gastoMax = it)) })
            val p = LimitesDeEntrada.PREDETERMINADOS
            Text("Valores originales: peso ${mostrarLimite(p.pesoMinimoKg.toString())}–${mostrarLimite(p.pesoMaximoKg.toString())} kg; " +
                "precio ${mostrarLimite(p.precioKgMinimoCop.toString())}–${mostrarLimite(p.precioKgMaximoCop.toString())} pesos/kg; " +
                "gasto ${mostrarLimite(p.gastoMinimoCop.toString())}–${mostrarLimite(p.gastoMaximoCop.toString())} pesos.",
                style = AppTypography.helper.copy(color = TextSecondary))
            OutlinedButton(onClick = {
                borrador = BorradorLimites.de(configuracion.restablecer())
                errores = emptyMap()
                aviso = AvisoUi("Se restablecieron los valores originales.")
            }, modifier = Modifier.fillMaxWidth()) { Text("Volver a los valores originales", color = TextPrimary) }
        }
        PieDeAccion(textoBoton = "Guardar límites", botonActivo = true, onBoton = {
            when (val resultado = configuracion.guardar(borrador)) {
                is ValidacionLimites.Valida -> { errores = emptyMap(); aviso = AvisoUi("Límites guardados.") }
                is ValidacionLimites.ConErrores -> { errores = resultado.errores; aviso = AvisoUi("Revisa los campos indicados.", TipoAviso.ERROR) }
            }
        })
    }
    AvisoSuperior(aviso = aviso, onOculto = { aviso = null }, modifier = Modifier.align(Alignment.TopCenter))
  }
}

/**
 * Lo escrito en un campo de límite, tal como se guarda en el borrador: solo dígitos (sin puntos de miles), con el
 * "-" inicial si lo hay para que la validación pueda avisar "No se permiten números negativos.".
 */
internal fun limpiarLimite(texto: String): String {
    val negativo = texto.trimStart().startsWith("-")
    val digitos = texto.filter { it.isDigit() }.take(12)
    val sinCeros = digitos.trimStart('0').ifEmpty { if (digitos.isNotEmpty()) "0" else "" }
    return if (negativo) "-$sinCeros" else sinCeros
}

/** Cómo se ve en pantalla un valor del borrador: con puntos de miles ("99999" → "99.999"). No cambia lo guardado. */
internal fun mostrarLimite(crudo: String): String {
    val negativo = crudo.startsWith("-")
    val digitos = crudo.filter { it.isDigit() }
    if (digitos.isEmpty()) return if (negativo) "-" else ""
    return (if (negativo) "-" else "") + formatoMiles(digitos.toLong())
}

@Composable
private fun GrupoLimites(
    titulo: String, descripcion: String, fondo: Color, minimo: String, maximo: String, unidad: String,
    errorMinimo: String?, errorMaximo: String?, onMinimo: (String) -> Unit, onMaximo: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(SurfaceCard, AppShapes.medium).border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)) {
        Column(Modifier.fillMaxWidth().background(fondo, AppShapes.medium).padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            Text(titulo, style = AppTypography.titleCard)
            Text(descripcion, style = AppTypography.bodyMd.copy(color = TextSecondary))
        }
        Row(Modifier.fillMaxWidth().padding(Spacing.s16), horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            AppTextField("Mínimo ($unidad)", mostrarLimite(minimo), { onMinimo(limpiarLimite(it)) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, error = errorMinimo, compact = true)
            AppTextField("Máximo ($unidad)", mostrarLimite(maximo), { onMaximo(limpiarLimite(it)) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, error = errorMaximo, compact = true)
        }
    }
}
