package com.cafesmart.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextStrong

// Tarjeta de opcion: icono + titulo + descripcion + circulo de seleccion.
// Cuando esta seleccionada muestra debajo su contenido extra (buscador, formulario...).
@Composable
fun OptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expandedContent: @Composable () -> Unit = {}
) {
    Card(
        onClick = onClick,
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Primary else BorderDefault
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = if (selected) Primary else SurfaceSoft, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) OnPrimary else TextStrong,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = AppTypography.titleCard)
                    Text(text = description, style = AppTypography.bodyMd)
                }
                RadioCheck(selected = selected)
            }
            if (selected) {
                expandedContent()
            }
        }
    }
}

// Circulo de seleccion: vacio, o azul con ✓ cuando esta elegido
@Composable
fun RadioCheck(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier = modifier
                .size(24.dp)
                .background(color = Primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = "Seleccionado",
                tint = OnPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .border(width = 1.5.dp, color = BorderField, shape = CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionCardPreview() {
    CafesmartTheme {
        Column(
            modifier = Modifier.padding(Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            OptionCard(
                title = "Productor registrado",
                description = "Elígelo de tu lista.",
                icon = AppIcons.Search,
                selected = false,
                onClick = {}
            )
            OptionCard(
                title = "Sin registrar",
                description = "Compra rápida, sin datos.",
                icon = AppIcons.User,
                selected = true,
                onClick = {}
            )
        }
    }
}
