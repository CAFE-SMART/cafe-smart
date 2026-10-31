
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.*

@Composable
fun RegisterStep1Screen(
    state: AuthUiState,
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onHelp: () -> Unit,
    onSupport: () -> Unit
) {

    Scaffold(
        containerColor = SurfaceCard,

        topBar = {
            RegisterHeader(onBack = onBack)
        },

        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    )
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 12.dp,
                        top = 12.dp
                    ),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ContinueButton(
                    text = "Continuar",
                    onClick = onContinue
                )

                Spacer(Modifier.height(14.dp))

                SupportLinks(
                    onHelp = onHelp,
                    onSupport = onSupport
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
        ) {

            RegistrationProgress(step = 1)

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Comencemos configurando tu negocio",
                style = AppTypography.titleCard
            )

            Spacer(Modifier.height(24.dp))

            AuthField(
                title = "Nombre del negocio",
                value = state.businessName,

                onValueChange = { value ->
                    onChange {
                        it.copy(businessName = value)
                    }
                },

                hint = "Ej: Café Los Alpes",
                help = "Aparecerá en tus reportes. ${state.businessName.length}/30",
                error = state.errors["businessName"],
                maxChars = 30
            )

            Spacer(Modifier.height(23.dp))

            Text(
                text = "Tipo de negocio",
                style = AppTypography.labelField
            )

            Spacer(Modifier.height(12.dp))

            BusinessType.entries.forEach { type ->

                BusinessChoice(
                    type = type,
                    selected = state.businessType == type,

                    onClick = {
                        onChange {
                            it.copy(businessType = type)
                        }
                    }
                )

                Spacer(Modifier.height(9.dp))
            }

            state.errors["businessType"]?.let {
                Text(
                    text = it,
                    color = ErrorText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BusinessChoice(
    type: BusinessType,
    selected: Boolean,
    onClick: () -> Unit
) {

    val accent = when (type) {
        BusinessType.COMPRAVENTA ->
            Color(0xFFEA580C)

        BusinessType.COOPERATIVA ->
            Color(0xFF2563EB)

        BusinessType.PERSONALIZADO ->
            Color(0xFFDB2777)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width =
                    if (selected) 2.dp else 1.dp,

                color =
                    if (selected)
                        Primary
                    else
                        BorderDefault,

                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    accent.copy(alpha = 0.10f),
                    RoundedCornerShape(8.dp)
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = when (type) {
                    BusinessType.COMPRAVENTA -> "▤"
                    BusinessType.COOPERATIVA -> "♧"
                    BusinessType.PERSONALIZADO -> "☷"
                },

                color = accent,
                fontSize = 23.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = type.label,

                style = AppTypography.labelField.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Text(
                text = type.subtitle,
                style = AppTypography.helper
            )
        }

        RadioButton(
            selected = selected,
            onClick = null,

            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = BorderField
            )
        )
    }
}

@Preview(
    name = "Registro Compraventa",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CompraventaPreview() {

    CafesmartTheme {
        RegisterStep1Screen(
            state = AuthUiState(
                page = AuthPage.REGISTER_STEP_1,
                businessType = BusinessType.COMPRAVENTA
            ),
            onChange = { _ -> },
            onBack = {},
            onContinue = {},
            onHelp = {},
            onSupport = {}
        )
    }
}

@Preview(
    name = "Registro Cooperativa",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CooperativaPreview() {

    CafesmartTheme {
        RegisterStep1Screen(
            state = AuthUiState(
                page = AuthPage.REGISTER_STEP_1,
                businessType = BusinessType.COOPERATIVA
            ),
            onChange = { _ -> },
            onBack = {},
            onContinue = {},
            onHelp = {},
            onSupport = {}
        )
    }
}
