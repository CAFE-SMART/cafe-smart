
package com.cafesmart.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.*

@Composable
fun RegisterStep2Screen(
    state: AuthUiState,
    onChange: ((AuthUiState) -> AuthUiState) -> Unit,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoogle: () -> Unit,
    onHelp: () -> Unit = {},
    onSupport: () -> Unit = {},
    isSubmitting: Boolean = false
) {

    var showPassword by remember {
        mutableStateOf(false)
    }

    var showConfirmation by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = SurfaceCard,

        topBar = {
            RegisterHeader(
                onBack = onBack
            )
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

            RegistrationProgress(step = 2)

            Spacer(Modifier.height(12.dp))

            // AYUDA DEL PASO 2
            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(onClick = onHelp) {
                    Text(
                        text = "Ver ayuda",
                        color = Primary
                    )
                }

                TextButton(onClick = onSupport) {
                    Text(
                        text = "Contactar soporte",
                        color = Primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // INFORMACIÓN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        SurfaceSelected,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(14.dp),

                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = "●",
                    color = Primary,
                    fontSize = 18.sp
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text =
                        "Este usuario administrará el negocio, " +
                                "el inventario y los reportes. " +
                                "Usa datos reales para recuperar el acceso " +
                                "si lo necesitas. Todos los campos " +
                                "son obligatorios.",

                    style = AppTypography.bodyMd.copy(
                        fontSize = 12.sp,
                        color = PrimaryDark
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // NOMBRE Y APELLIDOS
            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                AuthField(
                    title = "Nombre",
                    value = state.firstName,

                    onValueChange = { value ->
                        onChange {
                            it.copy(firstName = value)
                        }
                    },

                    hint = "Ingresa tu nombre",
                    help = "Tu nombre.",
                    error = state.errors["firstName"],
                    modifier = Modifier.weight(1f)
                )

                AuthField(
                    title = "Apellidos",
                    value = state.lastName,

                    onValueChange = { value ->
                        onChange {
                            it.copy(lastName = value)
                        }
                    },

                    hint = "Ingresa tus apellidos",
                    help = "Tus dos apellidos.",
                    error = state.errors["lastName"],
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(17.dp))

            // TELÉFONO
            AuthField(
                title = "Teléfono",
                value = state.phone,

                onValueChange = { value ->
                    onChange {
                        it.copy(
                            phone = value.filter(Char::isDigit)
                        )
                    }
                },

                hint = "3001234567",
                help = "Celular colombiano de 10 dígitos.",
                error = state.errors["phone"],
                keyboardType = KeyboardType.Phone,
                maxChars = 10
            )

            Spacer(Modifier.height(17.dp))

            // CORREO
            AuthField(
                title = "Correo electrónico",
                value = state.email,

                onValueChange = { value ->
                    onChange {
                        it.copy(email = value)
                    }
                },

                hint = "admin@empresa.com",
                help = "Con este correo entrarás a la aplicación.",
                error = state.errors["email"],
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(17.dp))

            // CONTRASEÑA
            AuthField(
                title = "Contraseña",
                value = state.password,

                onValueChange = { value ->
                    onChange {
                        it.copy(password = value)
                    }
                },

                hint = "********",
                help =
                    "Mínimo 8 caracteres, con mayúscula, " +
                            "minúscula y número.",

                error = state.errors["password"],
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

            Spacer(Modifier.height(17.dp))

            // CONFIRMAR CONTRASEÑA
            AuthField(
                title = "Confirmar contraseña",
                value = state.passwordConfirmation,

                onValueChange = { value ->
                    onChange {
                        it.copy(
                            passwordConfirmation = value
                        )
                    }
                },

                hint = "Repite tu contraseña",
                help = "Debe coincidir con la anterior.",
                error = state.errors["passwordConfirmation"],
                keyboardType = KeyboardType.Password,

                visualTransformation =
                    if (showConfirmation)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),

                trailing = {
                    PasswordEye(
                        visible = showConfirmation,

                        onClick = {
                            showConfirmation = !showConfirmation
                        }
                    )
                }
            )

            Spacer(Modifier.height(22.dp))

            state.requestError?.let { error ->

                Text(
                    text = error,
                    color = ErrorText,
                    style = AppTypography.bodyMd,

                    modifier = Modifier.padding(
                        bottom = 12.dp
                    )
                )
            }

            // CREAR CUENTA
            ContinueButton(
                text =
                    if (isSubmitting)
                        "Creando cuenta..."
                    else
                        "Crear cuenta",

                enabled = !isSubmitting,
                onClick = onCreateAccount
            )

            if (isSubmitting) {

                Spacer(Modifier.height(12.dp))

                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            GoogleDivider()

            Spacer(Modifier.height(16.dp))

            GoogleButton(
                onClick = onGoogle
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(
    name = "Crear cuenta - Paso 2",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun RegisterStep2HelpPreview() {

    CafesmartTheme {

        RegisterStep2Screen(
            state = AuthUiState(
                page = AuthPage.REGISTER_STEP_2
            ),

            onChange = { _ -> },
            onBack = {},
            onCreateAccount = {},
            onGoogle = {},
            onHelp = {},
            onSupport = {}
        )
    }
}
