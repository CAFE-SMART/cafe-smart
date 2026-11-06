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
private fun lineIcon(name: String, vararg paths: String, strokeWidth: Float = 2f): ImageVector {
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
            strokeLineWidth = strokeWidth,
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
    // Hoja (tipo de cafe)
    val Leaf: ImageVector by lazy {
        lineIcon(
            "Leaf",
            "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8c0 5.5-4.78 10-10 10Z",
            "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"
        )
    }
    // Lapiz (editar)
    val Pencil: ImageVector by lazy {
        lineIcon(
            "Pencil",
            "M21.17 6.81a2.82 2.82 0 0 0-3.99-3.99L3.84 16.17a2 2 0 0 0-.5.83l-1.32 4.35a.5.5 0 0 0 .62.62l4.35-1.32a2 2 0 0 0 .83-.5Z",
            "M15 5l4 4"
        )
    }
    // Triangulo de advertencia
    val Warning: ImageVector by lazy {
        lineIcon(
            "Warning",
            "M21.73 18l-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z",
            "M12 9v4",
            "M12 17h.01"
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

    // Flecha hacia abajo (menu desplegable)
    val ChevronDown: ImageVector by lazy {
        lineIcon("ChevronDown", "M6 9l6 6l6-6")
    }
    // Flecha hacia la derecha (ir al detalle)
    val ChevronRight: ImageVector by lazy {
        lineIcon("ChevronRight", "M9 18l6-6l-6-6")
    }
    // Brote (cafe verde)
    val Sprout: ImageVector by lazy {
        lineIcon(
            "Sprout",
            "M7 20h10",
            "M10 20c5.5-2.5.8-6.4 3-10",
            "M9.5 9.4c1.1.8 1.8 2.2 2.3 3.7-2 .4-3.5.4-4.8-.3-1.2-.6-2.3-1.9-3-4.2 2.8-.5 4.4 0 5.5.8Z",
            "M14.1 6a7 7 0 0 0-1.1 4c1.9-.1 3.3-.6 4.3-1.4 1-1 1.6-2.3 1.7-4.6-2.7.1-4 1-4.9 2Z"
        )
    }
    // Sol (cafe seco)
    val Sun: ImageVector by lazy {
        lineIcon(
            "Sun",
            "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0Z",
            "M12 2v2",
            "M12 20v2",
            "M4.93 4.93l1.41 1.41",
            "M17.66 17.66l1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "M6.34 17.66l-1.41 1.41",
            "M19.07 4.93l-1.41 1.41"
        )
    }
    // Taza (cafe trillado)
    val Coffee: ImageVector by lazy {
        lineIcon(
            "Coffee",
            "M10 2v2",
            "M14 2v2",
            "M6 2v2",
            "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"
        )
    }
    private const val PACKAGE_BOX =
        "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73Z"
    // Caja (cafe pasilla)
    val Package: ImageVector by lazy {
        lineIcon("Package", PACKAGE_BOX, "M12 22V12", "M3.3 7l7.703 4.734a2 2 0 0 0 1.994 0L20.7 7", "M7.5 4.27l9 5.15")
    }
    // Caja de trazo fino: se dibuja grande en el estado vacio del inventario
    val PackageFine: ImageVector by lazy {
        lineIcon(
            "PackageFine", PACKAGE_BOX, "M12 22V12", "M3.3 7l7.703 4.734a2 2 0 0 0 1.994 0L20.7 7", "M7.5 4.27l9 5.15",
            strokeWidth = 0.75f
        )
    }
    // Carrito de compras
    val ShoppingCart: ImageVector by lazy {
        lineIcon(
            "ShoppingCart",
            "M9 21a1 1 0 1 1-2 0a1 1 0 1 1 2 0Z",
            "M20 21a1 1 0 1 1-2 0a1 1 0 1 1 2 0Z",
            "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"
        )
    }
}
