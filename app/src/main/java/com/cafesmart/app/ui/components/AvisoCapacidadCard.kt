package com.cafesmart.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary

// Avisos de capacidad de bodega en Compras (Figma, NUEVAS). Informativos: nunca bloquean la compra.
//  - SIN_CONFIGURAR: azul suave, con acceso directo a configurar (paso 1).
//  - ALERTA: rojo, para "casi llena" (90 % o más) y "supera la capacidad" (pasos 2 y 3 y Compra registrada).
// Si se da [porcentaje], dibuja la barra de ocupación y el texto "105 % ocupada" (paso 3).

enum class EstiloAviso { SIN_CONFIGURAR, ALERTA }

@Composable
fun AvisoCapacidadCard(
    titulo: String,
    mensaje: String,
    estilo: EstiloAviso,
    modifier: Modifier = Modifier,
    porcentaje: Int? = null,
    textoAccion: String? = null,
    onAccion: () -> Unit = {},
    compacto: Boolean = false,
) {
    val alerta = estilo == EstiloAviso.ALERTA
    val fondo = if (alerta) ErrorBg else SurfaceSelected
    val borde = if (alerta) ErrorDot else BorderSelectedSoft
    val colorTitulo = if (alerta) ErrorText else PrimaryDark
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fondo, AppShapes.medium)
            .border(BorderStroke(1.dp, borde), AppShapes.medium)
            .padding(horizontal = 16.dp, vertical = if (compacto) 10.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compacto) 6.dp else 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!compacto) {
                Icon(
                    imageVector = if (alerta) AppIcons.Warning else IconoInformacion,
                    contentDescription = null,
                    tint = if (alerta) ErrorIcon else Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = AppTypography.labelField.copy(color = colorTitulo))
                Text(mensaje, style = AppTypography.bodyMd.copy(color = if (alerta) TextPrimary else TextSecondary))
                if (textoAccion != null) {
                    Text(
                        text = textoAccion,
                        style = AppTypography.labelField.copy(color = Primary),
                        modifier = Modifier.clickable(onClick = onAccion).padding(top = 2.dp)
                    )
                }
            }
        }
        if (porcentaje != null) {
            val relleno = (porcentaje.coerceIn(0, 100)) / 100f
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceSoft)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth(relleno)
                        .height(8.dp)
                        .background(ErrorDot, RoundedCornerShape(4.dp))
                ) {}
            }
            if (!compacto) Text("$porcentaje % ocupada", style = AppTypography.helper.copy(color = ErrorText))
        }
    }
}

// Círculo de información (Lucide "info"), privado para no editar AppIcons.kt, que comparte todo el equipo.
private val IconoInformacion: ImageVector by lazy {
    ImageVector.Builder("Informacion", 24.dp, 24.dp, 24f, 24f).apply {
        listOf("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20Z", "M12 16v-4", "M12 8h.01").forEach {
            addPath(
                pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

@Preview(showBackground = true)
@Composable
private fun AvisoCapacidadPreview() {
    CafesmartTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.Start) {
            AvisoCapacidadCard(
                "Falta configurar la capacidad de tu bodega",
                "Así te avisamos cuando se esté llenando. Es solo un aviso: puedes seguir con tu compra.",
                EstiloAviso.SIN_CONFIGURAR, textoAccion = "Configurar ahora ›"
            )
            AvisoCapacidadCard(
                "Esta compra supera tu capacidad",
                "Te pasas 150 kg de los 3.000 kg que caben. Es solo un aviso: igual puedes registrar la compra.",
                EstiloAviso.ALERTA, porcentaje = 105
            )
        }
    }
}
