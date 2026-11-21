package com.cafesmart.app.ui.gastos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cafesmart.app.data.gastos.RoomGastosRepository
import com.cafesmart.app.viewmodel.gastos.GastosViewModel

/**
 * Fábrica del ViewModel de Gastos sobre Room. La lista, el registro, el selector de sublotes y
 * "Gastos del sublote" le piden el mismo ViewModel a HOME: si ya existe uno (por ejemplo, el que arma
 * una prueba con un repositorio falso), se reutiliza y esta fábrica no crea otro.
 *
 * sublotes = null hace que el ViewModel lea los sublotes con café disponible desde la base.
 */
@Composable
fun rememberGastosViewModelFactory(): ViewModelProvider.Factory {
    val contexto = LocalContext.current.applicationContext
    return remember(contexto) {
        viewModelFactory {
            initializer { GastosViewModel(RoomGastosRepository.obtener(contexto), sublotes = null) }
        }
    }
}
