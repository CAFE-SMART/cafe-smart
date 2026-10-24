package com.cafesmart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.AppBottomSheet
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.SegmentedProgressBar
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CafesmartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ComponentsShowcase(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ComponentsShowcase(modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(1) }
    var sheetVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s48),
        verticalArrangement = Arrangement.spacedBy(Spacing.s28)
    ) {
        BackArrow(onClick = { step = (step - 2).coerceAtLeast(1) })

        Text(text = "Componentes base", style = AppTypography.titleScreen)

        SegmentedProgressBar(
            total = 4,
            current = step,
            modifier = Modifier.fillMaxWidth()
        )

        ContinueButton(
            text = "Continuar",
            enabled = true,
            onClick = { step = (step + 1).coerceAtMost(4) }
        )

        ContinueButton(
            text = "Continuar",
            enabled = false,
            onClick = {}
        )

        ContinueButton(
            text = "Abrir hoja inferior",
            enabled = true,
            onClick = { sheetVisible = true }
        )
    }

    if (sheetVisible) {
        AppBottomSheet(onDismiss = { sheetVisible = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.s20),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Text(text = "Hoja inferior", style = AppTypography.titleModal)
                Text(text = "Contenido de ejemplo del AppBottomSheet.", style = AppTypography.bodyMd)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ComponentsShowcasePreview() {
    CafesmartTheme {
        ComponentsShowcase()
    }
}
