package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ConfirmSheet
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.drying.BOTON_CONFIRMAR_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.BOTON_DESCARTAR_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.CUERPO_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.SecadoEnCursoEnPantalla
import com.cafesmart.app.viewmodel.drying.TITULO_CANCELAR_SECADO
import com.cafesmart.app.viewmodel.drying.SecadosEnCursoUiState

// Paso 4 de Secado: Secados en curso (Figma "Secado · Paso 4 · En curso").
// Vista (MVVM): sin logica ni estado propio. Recibe el estado y avisa con callbacks.
@Composable
fun SecadoStep4Screen(
    estado: SecadosEnCursoUiState,
    onElegir: (Long) -> Unit,
    onFinalizar: () -> Unit,
    onPedirCancelar: () -> Unit,
    onConfirmarCancelar: () -> Unit,
    onDescartarCancelar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
    ) {
        EncabezadoPantalla(titulo = "Secados en curso", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            val mensajeVacio = estado.mensajeSinSecados
            if (mensajeVacio != null) {
                Text(
                    text = mensajeVacio,
                    style = AppTypography.bodyMd,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.s48)
                )
            }
            estado.tarjetas.forEach { tarjeta ->
                TarjetaSecadoEnCurso(
                    tarjeta = tarjeta,
                    // Con una sola tarjeta el Figma no la resalta; con varias se ve cual esta elegida.
                    resaltada = estado.tarjetas.size > 1 && tarjeta.detalle.secado.id == estado.elegido?.secado?.id,
                    onClick = { onElegir(tarjeta.detalle.secado.id) }
                )
            }
        }

        // Sin secados no hay nada que finalizar ni cancelar.
        if (estado.elegido != null) {
            PurchaseBottomBar {
                ContinueButton(text = "Finalizar secado", onClick = onFinalizar)
                TextButton(onClick = onPedirCancelar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = "Cancelar secado", style = AppTypography.buttonLink.copy(color = ErrorText))
                }
            }
        }
    }

    if (estado.confirmandoCancelacion) {
        // Hoja comun de confirmacion, con los textos del ViewModel y los colores de "cancelar" (rojo).
        ConfirmSheet(
            icon = AppIcons.Warning,
            iconBackground = ErrorBg,
            iconTint = ErrorIcon,
            title = TITULO_CANCELAR_SECADO,
            message = CUERPO_CANCELAR_SECADO,
            actionText = BOTON_CONFIRMAR_CANCELAR_SECADO,
            onAction = onConfirmarCancelar,
            linkText = BOTON_DESCARTAR_CANCELAR_SECADO,
            onLink = onDescartarCancelar,
            actionContainerColor = ErrorBg,
            actionContentColor = ErrorText
        )
    }
}

// Una tarjeta de secado: icono, titulo, "En secado", calidades y la fila Enviado / Inicio / Lleva.
@Composable
private fun TarjetaSecadoEnCurso(tarjeta: SecadoEnCursoEnPantalla, resaltada: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = AppShapes.medium,
        color = if (resaltada) SurfaceSelected else SurfaceCard,
        border = BorderStroke(
            width = if (resaltada) 2.dp else 1.dp,
            color = if (resaltada) Primary else BorderDefault
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                IconoDeSecado()
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = tarjeta.titulo, style = AppTypography.titleCard)
                    Text(text = tarjeta.sublotesTexto, style = AppTypography.bodyMd)
                }
                Text(
                    text = "En secado",
                    style = AppTypography.pill.copy(color = WarningText),
                    modifier = Modifier
                        .background(WarningBg, CircleShape)
                        .padding(horizontal = Spacing.s12, vertical = Spacing.s6)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                tarjeta.calidades.forEach { EtiquetaCalidad(calidad = it, texto = it.etiqueta) }
            }
            HorizontalDivider(color = BorderDefault)
            Row(modifier = Modifier.fillMaxWidth()) {
                DatoDeTarjeta(etiqueta = "Enviado", valor = tarjeta.kgEnviadosTexto, modifier = Modifier.weight(1f))
                DatoDeTarjeta(etiqueta = "Inició", valor = inicioCorto(tarjeta), modifier = Modifier.weight(1f))
                DatoDeTarjeta(etiqueta = "Lleva", valor = tarjeta.diasTexto.removePrefix("Lleva "), modifier = Modifier.weight(1f))
            }
        }
    }
}

// El Figma muestra "3 de junio" y el estado trae "3 de junio de 2026": se le quita el año.
// Solo presentacion; si el ViewModel llega a exponer la fecha corta, se usa directamente.
private fun inicioCorto(tarjeta: SecadoEnCursoEnPantalla): String =
    tarjeta.fechaInicioTexto.removeSuffix(" de ${tarjeta.detalle.secado.fechaInicio.anio}")

@Composable
private fun DatoDeTarjeta(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = etiqueta, style = AppTypography.caption)
        Text(text = valor, style = AppTypography.valueStrongBold)
    }
}

// Circulo verde con la hojita de las tarjetas de secado (pasos 4 y 5)
@Composable
fun IconoDeSecado() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(SuccessBg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = AppIcons.Leaf, contentDescription = null, tint = SuccessIcon, modifier = Modifier.size(22.dp))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso4Preview() {
    val repositorio = SecadoRepositoryEjemplo()
    val verde = repositorio.inventario().first { it.tipoCafe == TipoCafe.VERDE }
    repositorio.iniciarSecado(Fecha(3, 6, 2026), listOf(EnvioASecar(verde.sublote.id, 1.0)))
    CafesmartTheme {
        SecadoStep4Screen(
            estado = SecadosEnCursoUiState(
                secados = repositorio.secadosEnCurso(),
                inventario = repositorio.inventario(),
                hoy = Fecha(6, 6, 2026)
            ),
            onElegir = {}, onFinalizar = {}, onPedirCancelar = {},
            onConfirmarCancelar = {}, onDescartarCancelar = {}, onBack = {}
        )
    }
}
