package com.cafesmart.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.WarningDot

// Contraseña de la cuenta, con aviso en vivo de si está buena o no (Figma: "Cuenta · Crear cuenta · Paso 2 ·
// Contraseña con requisitos"). La regla es una sola: la usan este aviso y la validación del registro (AuthValidation).
// Mínimo 8 caracteres, con una minúscula, una mayúscula y un número.

const val MIN_CARACTERES_CONTRASENA = 8

data class RequisitoContrasena(val texto: String, val cumplido: Boolean)

/** Los cuatro requisitos y si la contraseña escrita cumple cada uno. */
fun requisitosContrasena(password: String): List<RequisitoContrasena> = listOf(
    RequisitoContrasena("$MIN_CARACTERES_CONTRASENA caracteres", password.length >= MIN_CARACTERES_CONTRASENA),
    RequisitoContrasena("Una minúscula", password.any(Char::isLowerCase)),
    RequisitoContrasena("Una mayúscula", password.any(Char::isUpperCase)),
    RequisitoContrasena("Un número", password.any(Char::isDigit)),
)

/** true si cumple los cuatro requisitos. */
fun contrasenaValida(password: String): Boolean = requisitosContrasena(password).all { it.cumplido }

/** Cuántos requisitos cumple (de 0 a 4): llena la barra de fuerza. */
fun requisitosCumplidos(password: String): Int = requisitosContrasena(password).count { it.cumplido }

enum class Coincidencia { SIN_ESCRIBIR, COINCIDEN, NO_COINCIDEN }

/** Compara la confirmación con la contraseña mientras se escribe. Con la confirmación vacía no dice nada. */
fun coincidenciaContrasena(password: String, confirmacion: String): Coincidencia = when {
    confirmacion.isEmpty() -> Coincidencia.SIN_ESCRIBIR
    confirmacion == password -> Coincidencia.COINCIDEN
    else -> Coincidencia.NO_COINCIDEN
}

/** Barra de fuerza y lista de requisitos que se van marcando en verde; se muestra mientras hay algo escrito. */
@Composable
internal fun RequisitosContrasena(password: String, modifier: Modifier = Modifier) {
    val cumplidos = requisitosCumplidos(password)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Barra: ámbar mientras falta algo, verde cuando cumple los cuatro.
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SurfaceSoft)) {
            Box(
                Modifier
                    .fillMaxWidth(cumplidos / 4f)
                    .height(6.dp)
                    .background(if (cumplidos == 4) SuccessIcon else WarningDot, RoundedCornerShape(3.dp))
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            requisitosContrasena(password).forEach { requisito ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (requisito.cumplido) {
                        Icon(AppIcons.Check, contentDescription = "Cumplido", tint = SuccessIcon, modifier = Modifier.size(18.dp))
                    } else {
                        Box(Modifier.size(18.dp).border(2.dp, BorderDefault, CircleShape))
                    }
                    Text(
                        text = requisito.texto,
                        style = AppTypography.helper.copy(color = if (requisito.cumplido) SuccessText else TextMuted)
                    )
                }
            }
        }
    }
}

/** "Las contraseñas coinciden." en verde o "Las contraseñas no coinciden." en rojo; nada si no hay confirmación. */
@Composable
internal fun MensajeCoincidencia(password: String, confirmacion: String) {
    when (coincidenciaContrasena(password, confirmacion)) {
        Coincidencia.COINCIDEN -> Text("Las contraseñas coinciden.", style = AppTypography.helper.copy(color = SuccessText))
        Coincidencia.NO_COINCIDEN -> Text("Las contraseñas no coinciden.", style = AppTypography.helper.copy(color = ErrorText))
        Coincidencia.SIN_ESCRIBIR -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun RequisitosPreview() {
    CafesmartTheme {
        Column(Modifier.size(width = 340.dp, height = 220.dp)) { RequisitosContrasena("Cafe12") }
    }
}
