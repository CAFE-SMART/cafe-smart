package com.cafesmart.app.viewmodel.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.viewmodel.inventory.redondearKg
import com.cafesmart.app.viewmodel.sale.cantidadValida
import com.cafesmart.app.viewmodel.sale.sublotesDelTipo
import java.math.BigDecimal

// Reglas de los pasos 1 a 3 de Secado (Figma "Secado - Paso 1 - Elegir cafe"): solo se seca cafe verde,
// se puede enviar todo un sublote o una parte, y la cantidad debe ser mayor a 0 y no pasar de lo disponible.
// Son funciones puras (no dependen de Compose ni de Android) para poder probarlas con JUnit.
// La cantidad usa las mismas reglas de Ventas (cantidadValida, errorCantidad, mensajeCantidad) para no repetirlas.

// Textos propuestos (no estan en el Figma; falta confirmarlos con el equipo).
const val MENSAJE_ELEGIR_SUBLOTE_SECADO = "Elige al menos un sublote para continuar."
const val MENSAJE_SIN_CAFE_VERDE = "No tienes café verde disponible para secar."

// Sublotes que se pueden secar: solo cafe verde y con cafe disponible,
// ordenados por calidad (buena, regular, mala) y dentro de cada calidad por numero de sublote.
fun sublotesParaSecar(inventario: List<SubloteConLote>): List<SubloteConLote> =
    sublotesDelTipo(inventario, TipoCafe.VERDE)

// Kg que se mandan a secar en total (redondeado a gramos: sumar decimales puede dejar 4.999999).
fun totalASecarKg(elegidos: List<SubloteElegido>): Double =
    redondearKg(elegidos.sumOf { it.cantidadKg })

// Calidades que hay entre los sublotes elegidos, en el orden buena, regular, mala.
fun calidadesDe(elegidos: List<SubloteElegido>): List<Calidad> =
    Calidad.entries.filter { calidad -> elegidos.any { it.calidad == calidad } }

// "1 sublote" / "4 sublotes"
fun textoSublotes(cantidad: Int): String = "$cantidad ${if (cantidad == 1) "sublote" else "sublotes"}"

// "Continuar" del paso 1: hay al menos un sublote y cada cantidad es mayor a 0 y no pasa de lo disponible.
fun cafeListoParaSecar(elegidos: List<SubloteElegido>): Boolean =
    elegidos.isNotEmpty() && elegidos.all { cantidadValida(it.cantidadKg, it.disponibleKg) }

// Texto sobre "Continuar" que dice que falta; es null justo cuando cafeListoParaSecar es true.
fun mensajeSinElegir(elegidos: List<SubloteElegido>): String? =
    if (elegidos.isEmpty()) MENSAJE_ELEGIR_SUBLOTE_SECADO else null

// Al iniciar el secado se le resta a cada sublote lo que se envio.
// redondearKg evita restos como 0.0000001 al restar decimales; el sublote nunca queda en negativo.
fun descontarEnvios(inventario: List<SubloteConLote>, envios: List<EnvioASecar>): List<SubloteConLote> =
    inventario.map { item ->
        val enviado = envios.firstOrNull { it.subloteId == item.sublote.id }?.pesoKg ?: return@map item
        item.copy(sublote = item.sublote.copy(pesoDisponibleKg = redondearKg(maxOf(0.0, item.sublote.pesoDisponibleKg - enviado))))
    }

// Cantidad como se escribe en el campo: sin ceros de sobra y con punto decimal ("120", "12.5").
// El campo la muestra con coma gracias a pesoEnPantalla.
fun textoDeCantidad(kg: Double): String =
    BigDecimal.valueOf(redondearKg(kg)).stripTrailingZeros().toPlainString()
