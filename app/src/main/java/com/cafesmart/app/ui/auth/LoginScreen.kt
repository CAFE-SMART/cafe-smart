
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cafesmart.app.R
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.*

@Composable
fun LoginScreen(
    state: AuthUiState,
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,
    onSubmit: () -> Unit,
    onRegister: () -> Unit,
    onGoogle: () -> Unit,
    onForgotPassword: () -> Unit,
    onHelp: () -> Unit,
    onSupport: () -> Unit,
    onExit: () -> Unit,
    isSubmitting: Boolean = false
) {

    var showPassword by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // ENCABEZADO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    5.dp,
                    RoundedCornerShape(16.dp)
                )
                .background(
                    SurfaceCard,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(
                    R.drawable.splash_cafe
                ),
                contentDescription = "Logo Café Smart",
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Café Smart",
                style = AppTypography.titleScreen.copy(
                    fontSize = 20.sp
                ),
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onExit,

                colors = ButtonDefaults.textButtonColors(
                    containerColor = ErrorBg
                )
            ) {
                Text(
                    text = "Salir",
                    color = ErrorText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // TARJETA DE LOGIN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    8.dp,
                    RoundedCornerShape(14.dp)
                )
                .background(
                    SurfaceCard,
                    RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 22.dp,
                    vertical = 30.dp
                ),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(
                    R.drawable.splash_cafe
                ),

                contentDescription = "Logo de Café Smart",
                modifier = Modifier.size(138.dp)
            )

            Text(
                text = "Iniciar Sesión",
                style = AppTypography.titleExito,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Bienvenido a Café Smart",
                style = AppTypography.bodyMd
            )

            Spacer(Modifier.height(30.dp))

            // CORREO
            AuthField(
                title = "Correo electrónico",
                value = state.loginEmail,

                onValueChange = { value ->
                    onChange {
                        it.copy(loginEmail = value)
                    }
                },

                hint = "ejemplo@correo.com",
                help = "El correo con el que te registraste.",
                error = state.errors["loginEmail"],
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(22.dp))

            // CONTRASEÑA
            AuthField(
                title = "Contraseña",
                value = state.loginPassword,

                onValueChange = { value ->
                    onChange {
                        it.copy(loginPassword = value)
                    }
                },

                hint = "********",
                help = "La que creaste al registrarte.",
                error = state.errors["loginPassword"],
                keyboardType = KeyboardType.Password,

                visualTransformation =
                    if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),

                trailing = {
                    PasswordEye(
                        visible = showPassword,
                        onClick = {
                            showPassword = !showPassword
                        }
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                AuthLink(
                    text = "¿Olvidaste tu contraseña?",
                    onClick = onForgotPassword
                )
            }

            Spacer(Modifier.height(14.dp))

            // RECORDAR MI CUENTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = state.rememberMe,

                    onCheckedChange = { checked ->
                        onChange {
                            it.copy(rememberMe = checked)
                        }
                    },

                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary
                    )
                )

                Text(
                    "Recordar mi cuenta",
                    style = AppTypography.bodyMd
                )
            }

            Spacer(Modifier.height(18.dp))

            // BOTÓN ENTRAR
            ContinueButton(
                text =
                    if (isSubmitting)
                        "Ingresando..."
                    else
                        "Entrar",

                enabled = !isSubmitting,
                onClick = onSubmit
            )

            Spacer(Modifier.height(38.dp))

            GoogleDivider()

            Spacer(Modifier.height(22.dp))

            GoogleButton(
                onClick = onGoogle
            )

            Spacer(Modifier.height(28.dp))

            // CREAR CUENTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "¿No tienes una cuenta? ",
                    style = AppTypography.bodyMd
                )

                AuthLink(
                    text = "Regístrate gratis",
                    onClick = onRegister
                )
            }

            Spacer(Modifier.height(25.dp))

            Text(
                text = "¿Problemas para entrar?",
                style = AppTypography.caption.copy(
                    color = TextPrimary
                )
            )

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp
                )
            ) {

                AuthLink(
                    text = "Ver ayuda",
                    onClick = onHelp
                )

                AuthLink(
                    text = "Contactar soporte",
                    onClick = onSupport
                )
            }
        }
    }
}

@Preview(
    name = "Login Café Smart",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun LoginCafeSmartPreview() {

    CafesmartTheme {
        LoginScreen(
            state = AuthUiState(),
            onChange = { _ -> },
            onSubmit = {},
            onRegister = {},
            onGoogle = {},
            onForgotPassword = {},
            onHelp = {},
            onSupport = {},
            onExit = {}
        )
    }
}
