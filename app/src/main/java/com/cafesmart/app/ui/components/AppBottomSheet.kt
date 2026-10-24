package com.cafesmart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ModalScrim
import com.cafesmart.app.ui.theme.SheetHandle
import com.cafesmart.app.ui.theme.SheetShadow
import com.cafesmart.app.ui.theme.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppShapes.large,
        containerColor = SurfaceCard,
        scrimColor = ModalScrim,
        tonalElevation = 0.dp,
        dragHandle = { AppSheetDragHandle() }
    ) {
        content()
    }
}

// El sheet se posiciona animando su offset, no su tamano, asi que el
// shadow directional (Y -4, blur 20) va en el asa: es el unico elemento
// de tamano fijo y queda pegado al borde superior redondeado del sheet.
@Composable
private fun AppSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 20.dp, shape = AppShapes.large, ambientColor = SheetShadow, spotColor = SheetShadow)
            .background(color = SurfaceCard, shape = AppShapes.large)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(color = SheetHandle, shape = RoundedCornerShape(2.dp))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomSheetPreview() {
    CafesmartTheme {
        AppBottomSheet(onDismiss = {}) {
            Text(text = "Contenido de la hoja", style = AppTypography.bodyLg)
        }
    }
}
