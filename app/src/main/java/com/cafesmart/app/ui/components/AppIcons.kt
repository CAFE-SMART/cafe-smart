package com.cafesmart.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Iconos de linea (24x24, trazo 2) construidos con ImageVector.Builder, igual que
// ArrowLeftIcon de BackArrow, porque el proyecto no tiene la libreria material-icons.
// Cada texto es el trazo del icono en notacion SVG sobre una cuadricula de 24x24:
//   M x y = mover el lapiz, l / h / v / L = linea, a = arco (circulos), Z = cerrar figura.
// Ejemplo: "M5 12h14" = empieza en (5,12) y traza una linea horizontal de 14 -> "—".
// El color no va aqui: lo pone el parametro tint de Icon.
private fun lineIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    paths.forEach { pathData ->
        builder.addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}

object AppIcons {
    // Lupa
    val Search: ImageVector by lazy {
        lineIcon("Search", "M19 11a8 8 0 1 1-16 0a8 8 0 1 1 16 0Z", "M21 21l-4.3-4.3")
    }
    // Persona
    val User: ImageVector by lazy {
        lineIcon("User", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M16 7a4 4 0 1 1-8 0a4 4 0 1 1 8 0Z")
    }
    // Persona con "+"
    val UserPlus: ImageVector by lazy {
        lineIcon(
            "UserPlus",
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
            "M13 7a4 4 0 1 1-8 0a4 4 0 1 1 8 0Z",
            "M19 8v6",
            "M22 11h-6"
        )
    }
    // Chulito ✓
    val Check: ImageVector by lazy {
        lineIcon("Check", "M20 6L9 17l-5-5")
    }
    // Signo +
    val Plus: ImageVector by lazy {
        lineIcon("Plus", "M5 12h14", "M12 5v14")
    }
    // Flecha →
    val ArrowRight: ImageVector by lazy {
        lineIcon("ArrowRight", "M5 12h14", "M12 5l7 7l-7 7")
    }
    // Calendario
    val Calendar: ImageVector by lazy {
        lineIcon(
            "Calendar",
            "M8 2v4",
            "M16 2v4",
            "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
            "M3 10h18"
        )
    }
    // Pulgar arriba (calidad buena)
    val ThumbsUp: ImageVector by lazy {
        lineIcon(
            "ThumbsUp",
            "M7 10v12",
            "M15 5.88L14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z"
        )
    }
    // Pulgar abajo (calidad mala)
    val ThumbsDown: ImageVector by lazy {
        lineIcon(
            "ThumbsDown",
            "M17 14V2",
            "M9 18.12L10 14H4.17a2 2 0 0 1-1.92-2.56l2.33-8A2 2 0 0 1 6.5 2H20a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-2.76a2 2 0 0 0-1.79 1.11L12 22a3.13 3.13 0 0 1-3-3.88Z"
        )
    }
}
