package com.cafesmart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.ui.theme.CafesmartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CafesmartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // padding aplica el espacio de las barras del sistema y consumeWindowInsets avisa a las
                    // pantallas que ya está aplicado. Sin consumirlo, cada Scaffold o safeDrawing interno
                    // lo sumaba otra vez y quedaba un hueco grande arriba y abajo.
                    AppNavHost(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }
            }
        }
    }
}
