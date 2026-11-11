package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.auth.AuthField
import com.cafesmart.app.ui.auth.BusinessChoice
import com.cafesmart.app.ui.auth.BusinessType
import com.cafesmart.app.ui.components.AvisoSuperior
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.ButtonDisabled
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.settings.NegocioFormulario
import com.cafesmart.app.viewmodel.settings.NegocioInfoUiState

// Pantalla "Información de empresa" (Ajustes): completa o edita el nombre, el tipo y el teléfono del
// negocio. Usa los mismos campos y tarjetas de tipo del registro para que se vea y valide igual.
// Sin estado propio: recibe el estado y avisa con callbacks.

@Composable
fun NegocioInfoScreen(
    estado: NegocioInfoUiState,
    onBack: () -> Unit,
    onEditar: ((NegocioFormulario) -> NegocioFormulario) -> Unit,
    onGuardar: () -> Unit,
    onAvisoOculto: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(SurfaceScreen).imePadding()) {
            EncabezadoNegocio(onBack)

            when {
                estado.sinSesion -> Mensaje("Inicia sesión para guardar los datos de tu negocio.")
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                else -> Formulario(estado, onEditar, onGuardar)
            }
        }
        // Aviso rojo si no se pudo guardar (el verde de "guardado" sale en Ajustes, a donde se vuelve).
        AvisoSuperior(aviso = estado.aviso, onOculto = onAvisoOculto, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun Formulario(
    estado: NegocioInfoUiState,
    onEditar: ((NegocioFormulario) -> NegocioFormulario) -> Unit,
    onGuardar: () -> Unit,
) {
    val form = estado.form
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s20, vertical = Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Text(
            text = "Estos datos se guardan en este celular y aparecen en tus reportes.",
            style = AppTypography.bodyMd
        )

        AuthField(
            title = "Nombre del negocio",
            value = form.nombre,
            onValueChange = { valor -> onEditar { it.copy(nombre = valor) } },
            hint = "Ej: Café Los Alpes",
            help = "Aparecerá en tus reportes. ${form.nombre.length}/30",
            error = estado.errores["businessName"],
            maxChars = 30
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(text = "Tipo de negocio", style = AppTypography.labelField)
            BusinessType.entries.forEach { tipo ->
                BusinessChoice(
                    type = tipo,
                    selected = form.tipo == tipo,
                    onClick = { onEditar { it.copy(tipo = tipo) } }
                )
            }
            estado.errores["businessType"]?.let {
                Text(text = it, style = AppTypography.helper.copy(color = ErrorText))
            }
        }

        AuthField(
            title = "Teléfono",
            value = form.telefono,
            // Conserva solo números, como en el registro.
            onValueChange = { valor -> onEditar { it.copy(telefono = valor.filter(Char::isDigit)) } },
            hint = "3001234567",
            help = "Celular colombiano de 10 dígitos.",
            error = estado.errores["phone"],
            keyboardType = KeyboardType.Phone,
            maxChars = 10
        )

        Spacer(Modifier.height(Spacing.s4))
        Button(
            onClick = onGuardar,
            enabled = !estado.guardando,
            shape = AppShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = OnPrimary,
                disabledContainerColor = ButtonDisabled
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (estado.guardando) {
                CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(text = "Guardar cambios", style = AppTypography.buttonPrimary)
            }
        }
    }
}

@Composable
private fun Mensaje(texto: String) {
    Box(Modifier.fillMaxSize().padding(Spacing.s20), contentAlignment = Alignment.Center) {
        Text(text = texto, style = AppTypography.bodyLg)
    }
}

// Barra blanca con la flecha atrás (en círculo con borde) y el título centrado, igual que Ajustes.
@Composable
private fun EncabezadoNegocio(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceCard)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            contentAlignment = Alignment.Center
        ) {
            BackArrow(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(SurfaceCard, CircleShape)
                    .border(BorderStroke(1.dp, BorderDefault), CircleShape)
            )
            Text(text = "Información de empresa", style = AppTypography.titleScreen)
        }
        HorizontalDivider(color = BorderDefault)
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun NegocioInfoScreenPreview() {
    CafesmartTheme {
        NegocioInfoScreen(
            estado = NegocioInfoUiState(
                form = NegocioFormulario("Café La Esperanza", BusinessType.COMPRAVENTA, "3001234567"),
                cargando = false
            ),
            onBack = {},
            onEditar = {},
            onGuardar = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun NegocioInfoScreenErroresPreview() {
    CafesmartTheme {
        NegocioInfoScreen(
            estado = NegocioInfoUiState(
                cargando = false,
                errores = mapOf(
                    "businessName" to "Escribe el nombre del negocio.",
                    "businessType" to "Selecciona el tipo de negocio.",
                    "phone" to "Ingresa un celular de 10 dígitos que empiece por 3."
                )
            ),
            onBack = {},
            onEditar = {},
            onGuardar = {}
        )
    }
}
