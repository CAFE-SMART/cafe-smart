package com.cafesmart.app.ui.screens.purchase

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calendario de Material: trabaja en milisegundos; la conversion a fecha la hace el ViewModel
@Composable
fun PurchaseDatePickerDialog(
    fechaMilis: Long,
    onFechaChange: (Long) -> Unit,
    onCerrar: () -> Unit
) {
    // El tema del proyecto fija colores en estos estilos (labelLarge es blanco, para botones azules).
    // El calendario los usa sobre fondo claro, asi que aqui se quitan esos colores
    // para que Material use el color correcto de cada elemento y todo se vea.
    val tipografia = MaterialTheme.typography
    val tipografiaSinColor = tipografia.copy(
        labelLarge = tipografia.labelLarge.copy(color = Color.Unspecified),
        bodyLarge = tipografia.bodyLarge.copy(color = Color.Unspecified),
        bodyMedium = tipografia.bodyMedium.copy(color = Color.Unspecified),
        titleLarge = tipografia.titleLarge.copy(color = Color.Unspecified),
        titleMedium = tipografia.titleMedium.copy(color = Color.Unspecified)
    )

    MaterialTheme(typography = tipografiaSinColor) {
        // Guarda el dia elegido y abre el calendario en la fecha actual de la compra.
        val estadoCalendario = rememberDatePickerState(initialSelectedDateMillis = fechaMilis)
        DatePickerDialog(
            // Se cierra al tocar por fuera del calendario.
            onDismissRequest = onCerrar,
            confirmButton = {
                TextButton(onClick = {
                    // selectedDateMillis es null si no hay un dia elegido.
                    val millis = estadoCalendario.selectedDateMillis
                    if (millis != null) {
                        onFechaChange(millis)
                    }
                    onCerrar()
                }) { Text(text = "Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = onCerrar) { Text(text = "Cancelar") }
            }
        ) {
            // El calendario de Material dentro del dialogo.
            DatePicker(state = estadoCalendario)
        }
    }
}
