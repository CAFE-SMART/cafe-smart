package com.cafesmart.app.ui.compra

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.CafeFormulario
import com.cafesmart.app.data.Calidad
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.tiposCafe
import com.cafesmart.app.domain.fechaDeHoy
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ChoiceButton
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDashed
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorIcon
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningText

// Paso 2: ¿Que cafe compras?
// Pantalla sin estado de negocio: todo llega por parametros y cada accion sale por una lambda.
// Solo recuerda si el calendario esta abierto (estado visual).
@Composable
fun RegistroCompraPaso2Screen(
    cafes: List<CafeFormulario>,
    erroresPeso: List<String?>,
    erroresPrecio: List<String?>,
    fecha: Fecha,
    totalKg: Double,
    totalEstimado: Double,
    agregarHabilitado: Boolean,
    mensajeAgregar: String?,
    continuarHabilitado: Boolean,
    mensajePendiente: String?,
    onCafeChange: (Int, CafeFormulario) -> Unit,
    onQuitarCafe: (Int) -> Unit,
    onAgregarCafe: () -> Unit,
    onFechaChange: (Fecha) -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    // imePadding(): cuando sale el teclado, la pantalla se encoge para que no tape los campos
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CompraHeader(paso = 2, pregunta = "¿Qué café compras?", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                cafes.forEachIndexed { indice, cafe ->
                    TarjetaCafe(
                        numero = indice + 1,
                        cafe = cafe,
                        errorPeso = erroresPeso[indice],
                        errorPrecio = erroresPrecio[indice],
                        puedeQuitar = cafes.size > 1,
                        onCafeChange = { onCafeChange(indice, it) },
                        onQuitar = { onQuitarCafe(indice) }
                    )
                }

                BotonAgregarCafe(enabled = agregarHabilitado, onClick = onAgregarCafe)
                // Explica por que no se puede agregar otro cafe todavia
                if (mensajeAgregar != null) {
                    Text(text = mensajeAgregar, style = AppTypography.helper)
                }

                TarjetaFecha(fecha = fecha, onCambiar = { mostrarCalendario = true })
            }
        }

        CompraBottomBar {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total kg", style = AppTypography.caption)
                    Text(text = formatoKg(totalKg), style = AppTypography.titleModal.copy(color = PrimaryDark))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Total estimado", style = AppTypography.caption)
                    Text(text = formatoDinero(totalEstimado), style = AppTypography.titleModal.copy(color = PrimaryDark))
                }
            }
            MensajePendiente(mensaje = mensajePendiente)
            ContinueButton(
                text = "Continuar",
                enabled = continuarHabilitado,
                onClick = onContinuar
            )
        }
    }

    if (mostrarCalendario) {
        DialogoFecha(
            fecha = fecha,
            onFechaChange = onFechaChange,
            onCerrar = { mostrarCalendario = false }
        )
    }
}

// Tarjeta "Café N": tipo, calidad, peso y precio de un cafe
@Composable
private fun TarjetaCafe(
    numero: Int,
    cafe: CafeFormulario,
    errorPeso: String?,
    errorPrecio: String?,
    puedeQuitar: Boolean,
    onCafeChange: (CafeFormulario) -> Unit,
    onQuitar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Café $numero", style = AppTypography.titleScreen.copy(color = PrimaryDark))
            if (puedeQuitar) {
                TextButton(onClick = onQuitar) {
                    Text(text = "Quitar", style = AppTypography.buttonLink.copy(color = ErrorText))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(text = "Tipo de café", style = AppTypography.labelField)
            // chunked(2) parte la lista en filas de 2: [Verde, Seco], [Pasilla, Trillado]
            tiposCafe.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    fila.forEach { tipo ->
                        ChoiceButton(
                            text = tipo,
                            selected = cafe.tipoCafe == tipo,
                            onClick = { onCafeChange(cafe.copy(tipoCafe = tipo)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        SelectorCalidad(
            calidad = cafe.calidad,
            onCalidadChange = { onCafeChange(cafe.copy(calidad = it)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            AppTextField(
                label = "Peso (kg)",
                value = cafe.peso,
                onValueChange = { onCafeChange(cafe.copy(peso = it)) },
                placeholder = "Ej. 25",
                keyboardType = KeyboardType.Decimal,
                helper = "Máx. 99.999 kg",
                error = errorPeso,
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                label = "Precio por kg",
                // El estado guarda solo digitos ("14000"); en pantalla se ve "14.000"
                value = precioEnPantalla(cafe.precio),
                onValueChange = { onCafeChange(cafe.copy(precio = it)) },
                placeholder = "14.000",
                keyboardType = KeyboardType.Number,
                leadingText = "$",
                helper = "Entre $1.000 y $100.000/kg",
                error = errorPrecio,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Tres botones de calidad, cada uno con su icono en un circulo de color
@Composable
private fun SelectorCalidad(calidad: Calidad?, onCalidadChange: (Calidad) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(text = "Calidad", style = AppTypography.labelField)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Calidad.entries.forEach { opcion ->
                val activa = opcion == calidad
                val icono = when (opcion) {
                    Calidad.BUENA -> AppIcons.ThumbsUp
                    Calidad.REGULAR -> AppIcons.ArrowRight
                    Calidad.MALA -> AppIcons.ThumbsDown
                }
                val fondoIcono = when (opcion) {
                    Calidad.BUENA -> BorderSelectedSoft
                    Calidad.REGULAR -> WarningBg
                    Calidad.MALA -> ErrorBg
                }
                val colorIcono = when (opcion) {
                    Calidad.BUENA -> Primary
                    Calidad.REGULAR -> WarningText
                    Calidad.MALA -> ErrorIcon
                }
                Surface(
                    onClick = { onCalidadChange(opcion) },
                    selected = activa,
                    shape = AppShapes.medium,
                    color = if (activa) SurfaceSelected else SurfaceCard,
                    border = BorderStroke(
                        width = if (activa) 2.dp else 1.dp,
                        color = if (activa) Primary else BorderField
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.s8, Alignment.CenterVertically)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(fondoIcono, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icono,
                                contentDescription = null,
                                tint = colorIcono,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = opcion.etiqueta,
                            style = if (activa) AppTypography.labelField.copy(color = PrimaryDark) else AppTypography.labelField
                        )
                    }
                }
            }
        }
    }
}

// Boton con borde punteado: drawBehind dibuja un rectangulo redondeado con linea punteada
@Composable
private fun BotonAgregarCafe(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Primary else TextMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                drawRoundRect(
                    color = BorderDashed,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = AppIcons.Plus, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(Spacing.s8))
        Text(text = "Agregar otro café", style = AppTypography.buttonLink.copy(color = color))
    }
}

// Tarjeta de la fecha: icono, "Fecha de compra", la fecha y el enlace "Cambiar"
@Composable
private fun TarjetaFecha(fecha: Fecha, onCambiar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(start = Spacing.s16, top = Spacing.s8, bottom = Spacing.s8, end = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = AppIcons.Calendar, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(Spacing.s12))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Fecha de compra", style = AppTypography.caption)
            Text(text = formatoFechaLarga(fecha, fechaDeHoy()), style = AppTypography.valueStrong)
        }
        TextButton(onClick = onCambiar) {
            Text(text = "Cambiar", style = AppTypography.buttonLink)
        }
    }
}

@Composable
private fun DialogoFecha(fecha: Fecha, onFechaChange: (Fecha) -> Unit, onCerrar: () -> Unit) {
    // El DatePicker de Material trabaja en milisegundos: se convierte con fechaAMilis / milisAFecha
    val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMilis(fecha))
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                val millis = estado.selectedDateMillis
                if (millis != null) {
                    onFechaChange(milisAFecha(millis))
                }
                onCerrar()
            }) { Text(text = "Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(text = "Cancelar") }
        }
    ) {
        DatePicker(state = estado)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso2Preview() {
    CafesmartTheme {
        RegistroCompraPaso2Screen(
            cafes = listOf(CafeFormulario(tipoCafe = "Verde", calidad = Calidad.BUENA, peso = "25", precio = "14000")),
            erroresPeso = listOf(null),
            erroresPrecio = listOf(null),
            fecha = fechaDeHoy(),
            totalKg = 25.0,
            totalEstimado = 350000.0,
            agregarHabilitado = true,
            mensajeAgregar = null,
            continuarHabilitado = true,
            mensajePendiente = null,
            onCafeChange = { _, _ -> },
            onQuitarCafe = {},
            onAgregarCafe = {},
            onFechaChange = {},
            onContinuar = {},
            onBack = {}
        )
    }
}
