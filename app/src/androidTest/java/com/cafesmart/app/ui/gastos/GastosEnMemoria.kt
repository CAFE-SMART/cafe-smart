package com.cafesmart.app.ui.gastos

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.viewmodel.gastos.GastosViewModel

/**
 * Las rutas reales de Gastos guardan en Room, o sea en la base del dispositivo. Las pruebas que recorren
 * esas rutas registran antes, en la entrada de HOME, el ViewModel de demostración (en memoria): así lo que
 * registren no queda guardado en el celular ni depende de lo que ya haya en él.
 * Se llama cuando HOME ya existe y antes de abrir cualquier pantalla de Gastos.
 */
internal fun NavHostController.usarGastosEnMemoria(): GastosViewModel =
    ViewModelProvider(getBackStackEntry(Routes.HOME))[GastosViewModel::class.java]
