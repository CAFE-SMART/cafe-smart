package com.cafesmart.app.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.finanzas.CLAVE_MAX_CARACTERES
import com.cafesmart.app.data.finanzas.CLAVE_MIN_CARACTERES
import com.cafesmart.app.data.finanzas.ProveedorCuenta
import com.cafesmart.app.ui.auth.AuthField
import com.cafesmart.app.ui.auth.PasswordEye
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.components.PieDeAccion
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.viewmodel.finance.AccesoUiState
import com.cafesmart.app.viewmodel.finance.PasoAcceso

// Acceso a las métricas financieras (Figma, sección NUEVAS de Flujos): crear la clave en el primer acceso, pedirla
// después como un login, y "Olvidé mi clave" en dos pasos simples (confirmar que eres tú y crear la clave nueva).
// Sin estado propio de negocio: recibe el estado y avisa con callbacks. El ícono y el título van centrados y las
// descripciones a la izquierda, como en el diseño.

@Composable
fun AccesoFinancieroScreen(
    estado: AccesoUiState,
    onBack: () -> Unit,
    onClave: (String) -> Unit,
    onConfirmacion: (String) -> Unit,
    onContrasenaCuenta: (String) -> Unit,
    onGuardarClave: () -> Unit,
    onIngresar: () -> Unit,
    onOlvide: () -> Unit,
    onCancelarOlvido: () -> Unit,
    onConfirmarContrasena: () -> Unit,
    onConfirmarGoogle: () -> Unit,
) {
    val olvido = estado.paso == PasoAcceso.CONFIRMAR_IDENTIDAD || estado.paso == PasoAcceso.NUEVA_CLAVE
    Column(Modifier.fillMaxSize().background(SurfaceScreen).imePadding()) {
        EncabezadoPantalla(
            titulo = if (olvido) "Olvidé mi clave" else "Resumen financiero",
            onBack = if (olvido) onCancelarOlvido else onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s28),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (estado.paso) {
                PasoAcceso.CREAR -> ContenidoCrear(estado, onClave, onConfirmacion)
                PasoAcceso.PEDIR -> ContenidoPedir(estado, onClave)
                PasoAcceso.CONFIRMAR_IDENTIDAD -> ContenidoConfirmar(estado, onContrasenaCuenta, onConfirmarContrasena, onConfirmarGoogle, onCancelarOlvido)
                PasoAcceso.NUEVA_CLAVE -> ContenidoNuevaClave(estado, onClave, onConfirmacion)
                PasoAcceso.DENTRO -> Unit
            }
        }
        when (estado.paso) {
            PasoAcceso.CREAR -> PieDeAccion(
                textoBoton = if (estado.trabajando) "Guardando…" else "Guardar clave",
                botonActivo = !estado.trabajando, cargando = estado.trabajando, onBoton = onGuardarClave
            )
            PasoAcceso.PEDIR -> PieDeAccion(
                textoBoton = if (estado.trabajando) "Comprobando…" else "Ver resumen financiero",
                botonActivo = !estado.trabajando, cargando = estado.trabajando, onBoton = onIngresar,
                textoEnlace = "¿Olvidaste tu clave?", onEnlace = onOlvide
            )
            PasoAcceso.NUEVA_CLAVE -> PieDeAccion(
                textoBoton = if (estado.trabajando) "Guardando…" else "Guardar clave nueva",
                botonActivo = !estado.trabajando, cargando = estado.trabajando, onBoton = onGuardarClave,
                textoEnlace = "Cancelar", onEnlace = onCancelarOlvido
            )
            else -> Unit
        }
    }
}

@Composable
private fun ContenidoCrear(estado: AccesoUiState, onClave: (String) -> Unit, onConfirmacion: (String) -> Unit) {
    Candado()
    Textos(
        titulo = "Crea tu clave financiera",
        descripcion = "Es una clave solo para ver tus finanzas. Te la pediremos cada vez que entres."
    )
    CampoClave("Clave nueva", estado.clave, onClave, "Escribe tu clave", "De $CLAVE_MIN_CARACTERES a $CLAVE_MAX_CARACTERES caracteres.", estado.errorClave)
    CampoClave("Confirmar clave", estado.confirmacion, onConfirmacion, "Repite tu clave", "", estado.errorConfirmacion)
    Nota("Si la olvidas, podrás crear otra confirmando con la contraseña de tu cuenta.")
}

@Composable
private fun ContenidoPedir(estado: AccesoUiState, onClave: (String) -> Unit) {
    Candado()
    Textos(titulo = "Ingresa tu clave financiera", descripcion = "Escríbela para ver ventas, compras y gastos.")
    CampoClave("Clave financiera", estado.clave, onClave, "Escribe tu clave", "", estado.errorClave)
}

@Composable
private fun ContenidoConfirmar(
    estado: AccesoUiState,
    onContrasenaCuenta: (String) -> Unit,
    onConfirmarContrasena: () -> Unit,
    onConfirmarGoogle: () -> Unit,
    onCancelar: () -> Unit,
) {
    PasoDe(1)
    Candado()
    if (estado.proveedor == ProveedorCuenta.GOOGLE) {
        Textos("Confirma que eres tú", "Entraste con Google. Vuelve a confirmar con tu cuenta de Google. Después crearás tu clave nueva.")
        BotonGoogle(onClick = onConfirmarGoogle, activo = !estado.trabajando)
        estado.errorCuenta?.let { Text(it, style = AppTypography.helper.copy(color = com.cafesmart.app.ui.theme.ErrorText)) }
    } else {
        Textos("Confirma que eres tú", "Escribe la contraseña con la que entras a la app. Después crearás tu clave nueva.")
        CampoClave(
            "Contraseña de tu cuenta", estado.contrasenaCuenta, onContrasenaCuenta, "Escribe tu contraseña",
            "Es la que usas para entrar a la app.", estado.errorCuenta
        )
        ContinueButton(
            text = if (estado.trabajando) "Comprobando…" else "Continuar",
            onClick = onConfirmarContrasena,
            enabled = !estado.trabajando,
            cargando = estado.trabajando
        )
    }
    TextButton(onClick = onCancelar, modifier = Modifier.height(44.dp)) {
        Text("Cancelar", style = AppTypography.buttonLink.copy(color = Primary))
    }
}

@Composable
private fun ContenidoNuevaClave(estado: AccesoUiState, onClave: (String) -> Unit, onConfirmacion: (String) -> Unit) {
    PasoDe(2)
    Candado()
    Textos("Crea tu clave nueva", "Será la que uses para ver tu resumen financiero.")
    CampoClave("Clave nueva", estado.clave, onClave, "Escribe tu clave", "De $CLAVE_MIN_CARACTERES a $CLAVE_MAX_CARACTERES caracteres. Anótala en un lugar seguro.", estado.errorClave)
    CampoClave("Repite la clave", estado.confirmacion, onConfirmacion, "Escribe tu clave otra vez", "", estado.errorConfirmacion)
}

@Composable
private fun PasoDe(numero: Int) {
    Text("Paso $numero de 2", style = AppTypography.caption.copy(color = TextMuted), textAlign = TextAlign.Center)
}

@Composable
private fun Candado() {
    Box(
        modifier = Modifier.size(64.dp).background(ButtonSoft, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(IconoCandado, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
    }
}

// Título centrado y descripción a la izquierda (diseño del Figma).
@Composable
private fun Textos(titulo: String, descripcion: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Text(text = titulo, style = AppTypography.titleModal, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(text = descripcion, style = AppTypography.bodyMd.copy(color = TextSecondary))
    }
}

// Campo de clave con el ojito para mostrarla u ocultarla (Figma "Campo de clave").
@Composable
private fun CampoClave(
    titulo: String,
    valor: String,
    onCambio: (String) -> Unit,
    hint: String,
    ayuda: String,
    error: String?,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    AuthField(
        title = titulo,
        value = valor,
        onValueChange = onCambio,
        hint = hint,
        help = ayuda,
        error = error,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = { PasswordEye(visible = visible, onClick = { visible = !visible }) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Nota(texto: String) {
    Text(
        text = texto,
        style = AppTypography.bodyMd,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSelected, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.medium)
            .padding(Spacing.s16)
    )
}

// Botón blanco con el logo de Google (Figma "Continuar con Google").
@Composable
private fun BotonGoogle(onClick: () -> Unit, activo: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.5.dp, BorderField), AppShapes.medium)
            .clickable(enabled = activo, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(LogoGoogle, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        Text("  Continuar con Google", style = AppTypography.buttonPrimary.copy(color = com.cafesmart.app.ui.theme.TextPrimary))
    }
}

// Candado (Lucide "lock"), privado para no editar AppIcons.kt, que comparte todo el equipo.
private val IconoCandado: ImageVector by lazy {
    ImageVector.Builder("Candado", 24.dp, 24.dp, 24f, 24f).apply {
        listOf("M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1Z", "M8 11V7a4 4 0 0 1 8 0v4").forEach {
            addPath(
                pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

// Logo de Google en sus 4 colores (marca registrada; se usa solo para identificar el botón).
private val LogoGoogle: ImageVector by lazy {
    ImageVector.Builder("Google", 24.dp, 24.dp, 48f, 48f).apply {
        listOf(
            0xFFEA4335 to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
            0xFF4285F4 to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
            0xFFFBBC05 to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
            0xFF34A853 to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
        ).forEach { (color, ruta) -> addPath(pathData = addPathNodes(ruta), fill = SolidColor(Color(color))) }
    }.build()
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AccesoCrearPreview() {
    CafesmartTheme {
        AccesoFinancieroScreen(AccesoUiState(paso = PasoAcceso.CREAR), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AccesoPedirPreview() {
    CafesmartTheme {
        AccesoFinancieroScreen(
            AccesoUiState(paso = PasoAcceso.PEDIR, errorClave = "La clave no es correcta. Inténtalo de nuevo."),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AccesoConfirmarGooglePreview() {
    CafesmartTheme {
        AccesoFinancieroScreen(
            AccesoUiState(paso = PasoAcceso.CONFIRMAR_IDENTIDAD, proveedor = ProveedorCuenta.GOOGLE),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
