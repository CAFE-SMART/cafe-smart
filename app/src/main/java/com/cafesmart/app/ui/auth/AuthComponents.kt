
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.SegmentedProgressBar
import com.cafesmart.app.ui.theme.*

@Composable
internal fun AuthField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    help: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation =
        VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    maxChars: Int? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            style = AppTypography.labelField
        )

        OutlinedTextField(
            value = value,

            onValueChange = { newValue ->
                if (
                    maxChars == null ||
                    newValue.length <= maxChars
                ) {
                    onValueChange(newValue)
                }
            },

            placeholder = {
                Text(
                    hint,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            },

            singleLine = true,
            isError = error != null,

            visualTransformation = visualTransformation,

            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType
            ),

            trailingIcon = trailing,

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderDefault,
                errorBorderColor = ErrorText,
                focusedContainerColor = SurfaceScreen,
                unfocusedContainerColor = SurfaceScreen,
                errorContainerColor = SurfaceScreen
            ),

            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = error ?: help,
            style = AppTypography.helper,

            color =
                if (error != null)
                    ErrorText
                else
                    TextMuted
        )
    }
}

// ENCABEZADO DE REGISTRO CON ZONA SEGURA
@Composable
internal fun RegisterHeader(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                BackArrow(onClick = onBack)
            }

            Text(
                text = "Crear cuenta",
                style = AppTypography.titleScreen,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }

        HorizontalDivider(
            color = BorderDefault,
            thickness = 1.dp
        )
    }
}

@Composable
internal fun RegistrationProgress(
    step: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text =
                if (step == 1)
                    "Paso 1: Información del negocio"
                else
                    "Paso 2: Datos del administrador",

            style = AppTypography.caption
        )

        Text(
            text = "$step de 2",
            style = AppTypography.caption
        )
    }

    Spacer(Modifier.height(8.dp))

    SegmentedProgressBar(
        total = 2,
        current = step,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthLink(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = AppTypography.buttonLink,
        modifier = Modifier.clickable(
            onClick = onClick
        )
    )
}

@Composable
internal fun SupportLinks(
    onHelp: () -> Unit,
    onSupport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "¿Dudas con los datos del registro?",
            style = AppTypography.helper
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(
                16.dp
            )
        ) {
            AuthLink("Ver ayuda", onHelp)
            AuthLink("Contactar soporte", onSupport)
        }
    }
}

@Composable
internal fun GoogleButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,

        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),

        shape = RoundedCornerShape(12.dp),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceCard
        )
    ) {

        Text(
            "G",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF4285F4)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            "Continuar con Google",
            color = TextStrong
        )
    }
}

@Composable
internal fun GoogleDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "O continúa con",
            modifier = Modifier.padding(
                horizontal = 12.dp
            ),
            style = AppTypography.caption
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun PasswordEye(
    visible: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            if (visible) "Ocultar" else "Ver",
            fontSize = 12.sp
        )
    }
}
