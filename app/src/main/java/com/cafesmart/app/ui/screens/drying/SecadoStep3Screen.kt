package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessSolid
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.drying.SecadoUiState

// Paso 3 de Secado: Secado iniciado. Circulo verde con chulito, resumen y los botones
// "Ver secados en curso" / "Ir a inventario".
// Vista (MVVM): no tiene logica ni estado propio. Es como la pantalla final de Compras y Ventas,
// pero con la fila de calidades del Figma, por eso no usa PantallaRegistroExitoso.
@Composable
fun SecadoStep3Screen(
    estado: SecadoUiState,
    onVerEnCurso: () -> Unit,
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28)
    ) {
        Column(
            modifier = Modifier
                // weight(1f) y scroll: si el resumen no cabe se desplaza y los botones quedan abajo
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.s48),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SuccessSolid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = AppIcons.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(44.dp))
            }
            Text(
                text = "Secado iniciado",
                style = AppTypography.titleExito,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s20)
            )
            Text(
                text = "Cuando termine, registra el resultado.",
                style = AppTypography.bodyMd,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s20)
            )
            Text(
                text = "Resumen del secado",
                style = AppTypography.labelSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.s8)
            )
            ResumenDeSecado(
                calidades = estado.calidadesElegidas,
                sublotes = estado.sublotesElegidosTexto,
                fechaInicio = estado.fechaResumenTexto,
                etiquetaKg = "En secado",
                kg = estado.totalKgTexto
            )
        }

        Column(
            modifier = Modifier.padding(top = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            ContinueButton(text = "Ver secados en curso", onClick = onVerEnCurso)
            Button(
                onClick = onIrAInventario,
                shape = AppShapes.medium,
                // Boton secundario con fondo azul suave.
                colors = ButtonDefaults.buttonColors(containerColor = ButtonSoft, contentColor = PrimaryDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(text = "Ir a inventario", style = AppTypography.buttonPrimary.copy(color = PrimaryDark))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso3Preview() {
    val inventario = SecadoRepositoryEjemplo().inventario()
    val verdes = inventario.filter { it.tipoCafe.name == "VERDE" }
    CafesmartTheme {
        SecadoStep3Screen(
            estado = SecadoUiState(
                inventario = inventario,
                elegidos = verdes.map { SubloteElegido(it, it.sublote.pesoDisponibleKg) },
                secadoIniciado = true
            ),
            onVerEnCurso = {},
            onIrAInventario = {}
        )
    }
}
