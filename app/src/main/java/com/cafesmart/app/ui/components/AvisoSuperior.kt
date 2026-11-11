package com.cafesmart.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.SheetShadow
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessText
import kotlinx.coroutines.delay

enum class TipoAviso(val segundos: Double) {
    // Verde: el cambio se guardó. Se ve 2,5 s.
    EXITO(2.5),

    // Rojo: algo no funcionó. Se queda un poco más (4 s) para alcanzar a leerlo.
    ERROR(4.0)
}

/** Mensaje corto que se muestra arriba de la pantalla y con qué color. */
data class AvisoUi(val mensaje: String, val tipo: TipoAviso = TipoAviso.EXITO)

/**
 * Aviso arriba de la pantalla: barra semitransparente verde (éxito) o roja (error) con un ícono y el
 * mensaje ("Nombre actualizado", "No se pudo guardar"). Baja desde arriba, se queda unos segundos y se
 * oculta solo (guía del Design System: componente "Aviso superior").
 *
 * Se pone dentro de un Box que cubra la pantalla. Con [aviso] null no se ve; al terminar el tiempo llama
 * a [onOculto] para que quien lo muestra borre el aviso. Sirve en cualquier pantalla que guarde un cambio.
 *
 * [bajoBarraDeEstado] deja el aviso debajo de la barra de estado del celular (pantalla completa). En una hoja
 * inferior va en false: allí no hay barra de estado y el aviso se pone dentro de la hoja, porque una hoja
 * es una ventana aparte que se dibuja encima de la pantalla y oscurecería un aviso puesto detrás.
 */
@Composable
fun AvisoSuperior(
    aviso: AvisoUi?,
    onOculto: () -> Unit,
    modifier: Modifier = Modifier,
    bajoBarraDeEstado: Boolean = true,
) {
    // Guarda el último aviso para que se vea mientras la barra sale (aviso ya es null).
    var ultimo by remember { mutableStateOf(aviso ?: AvisoUi("")) }
    if (aviso != null) ultimo = aviso

    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay((aviso.tipo.segundos * 1000).toLong())
            onOculto()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (bajoBarraDeEstado) Modifier.statusBarsPadding() else Modifier)
            // En pantalla completa lleva margen lateral; dentro de una hoja ya lo da la hoja.
            .padding(horizontal = if (bajoBarraDeEstado) Spacing.s16 else 0.dp, vertical = Spacing.s8)
    ) {
        AnimatedVisibility(
            visible = aviso != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            val color = if (ultimo.tipo == TipoAviso.EXITO) SuccessText else ErrorText
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, AppShapes.medium, ambientColor = SheetShadow, spotColor = SheetShadow)
                    // Color del tema al 95 %: se nota un poco el fondo y el texto blanco conserva el contraste.
                    .background(color.copy(alpha = 0.95f), AppShapes.medium)
                    .padding(horizontal = Spacing.s16, vertical = Spacing.s12)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Icon(
                    imageVector = if (ultimo.tipo == TipoAviso.EXITO) AppIcons.Check else AppIcons.Warning,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = ultimo.mensaje,
                    style = AppTypography.bodyLg.copy(color = OnPrimary, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvisoSuperiorExitoPreview() {
    CafesmartTheme {
        AvisoSuperior(aviso = AvisoUi("Nombre actualizado"), onOculto = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AvisoSuperiorErrorPreview() {
    CafesmartTheme {
        AvisoSuperior(aviso = AvisoUi("No se pudo guardar. Inténtalo de nuevo.", TipoAviso.ERROR), onOculto = {})
    }
}
