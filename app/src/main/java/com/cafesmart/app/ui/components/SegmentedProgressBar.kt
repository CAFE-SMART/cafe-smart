package com.cafesmart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing

@Composable
fun SegmentedProgressBar(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    val spacing = Spacing.s6
    val cornerRadius = 3.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(cornerRadius * 2)
            .semantics { contentDescription = "Paso $current de $total" }
    ) {
        val spacingPx = spacing.toPx()
        val segmentWidth = (size.width - spacingPx * (total - 1)) / total
        val cornerRadiusPx = cornerRadius.toPx()
        for (index in 0 until total) {
            val startX = index * (segmentWidth + spacingPx)
            drawRoundRect(
                color = if (index < current) Primary else BorderDefault,
                topLeft = Offset(startX, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedProgressBarPreview() {
    CafesmartTheme {
        SegmentedProgressBar(total = 4, current = 2)
    }
}
