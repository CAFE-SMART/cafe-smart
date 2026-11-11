package com.cafesmart.app.data.finanzas

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.viewmodel.finance.calcularUtilidad
import java.util.Calendar
import java.util.GregorianCalendar

// Datos de las métricas financieras (Resumen, y luego Historial y Gráficas, que reutilizan este repositorio).
// Hoy la implementación es de ejemplo; cuando existan las compras, ventas y gastos en Room solo cambia de dónde salen.

/** Periodo que se está viendo: solo hoy, o los últimos 7 días (hoy incluido). */
enum class PeriodoFinanciero(val etiqueta: String) {
    HOY("Hoy"),
    SEMANA("Esta semana"),
}

/**
 * Resumen de un periodo, en pesos enteros.
 * utilidad = ingresos - costo del café vendido - gastos (RN-G-28; los gastos restan).
 */
data class ResumenFinanciero(
    val ingresos: Long,
    val costoCafe: Long,
    val gastos: Long,
    val utilidad: Long,
    val mermaKg: Double,
    // Cuántas ventas hay en el periodo. Con 0 no hay nada que mostrar todavía y todo vale 0.
    val ventas: Int = 0,
) {
    /** Lo que salió: el costo del café vendido más los gastos. */
    val salio: Long get() = costoCafe + gastos
}

interface FinanzasRepository {
    suspend fun resumen(periodo: PeriodoFinanciero): ResumenFinanciero
}

/** Una venta ya registrada, con lo necesario para calcular su utilidad (gastos = los que le tocan a esa venta). */
data class VentaRegistrada(
    val fecha: Fecha,
    val kgVendidos: Double,
    val precioVentaKg: Double,
    val precioCompraKg: Double,
    val gastos: Double,
    val mermaKg: Double = 0.0,
)

/** Resumen de las ventas dadas en el periodo. Cada venta se calcula con calcularUtilidad y se suman. */
fun resumirVentas(ventas: List<VentaRegistrada>, periodo: PeriodoFinanciero, hoy: Fecha): ResumenFinanciero {
    val enPeriodo = ventas.filter { enElPeriodo(it.fecha, periodo, hoy) }
    val calculos = enPeriodo.map {
        calcularUtilidad(it.kgVendidos, it.precioVentaKg, it.precioCompraKg, it.gastos)
    }
    return ResumenFinanciero(
        ingresos = calculos.sumOf { it.ingresosPesos },
        costoCafe = calculos.sumOf { it.costoCompraPesos },
        gastos = calculos.sumOf { it.gastosPesos },
        utilidad = calculos.sumOf { it.utilidadPesos },
        mermaKg = enPeriodo.sumOf { it.mermaKg },
        ventas = enPeriodo.size,
    )
}

/** true si la fecha cae en el periodo: HOY = ese día; SEMANA = de hace 6 días hasta hoy. */
fun enElPeriodo(fecha: Fecha, periodo: PeriodoFinanciero, hoy: Fecha): Boolean {
    val dias = diasEntre(fecha, hoy)
    return when (periodo) {
        PeriodoFinanciero.HOY -> dias == 0
        PeriodoFinanciero.SEMANA -> dias in 0..6
    }
}

/** Días desde [desde] hasta [hasta] (positivo si [hasta] es posterior). */
fun diasEntre(desde: Fecha, hasta: Fecha): Int {
    fun milis(f: Fecha) = GregorianCalendar(f.anio, f.mes - 1, f.dia).timeInMillis
    // Se redondea para que un cambio de hora no deje un día de menos o de más.
    return Math.round((milis(hasta) - milis(desde)) / 86_400_000.0).toInt()
}

/** La fecha de [dias] días antes de [fecha]. */
fun restarDias(fecha: Fecha, dias: Int): Fecha {
    val c = GregorianCalendar(fecha.anio, fecha.mes - 1, fecha.dia).apply { add(Calendar.DAY_OF_MONTH, -dias) }
    return Fecha(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
}

/**
 * De dónde salen las ventas registradas. Hoy la app todavía no guarda ventas (las compras, ventas y gastos pasan a
 * Room en otras tareas), así que [SinVentasRegistradas] devuelve una lista vacía y las métricas valen 0, que es lo
 * real. Cuando existan en Room solo hay que escribir otra implementación de esta interfaz: ni el repositorio ni las
 * pantallas cambian.
 */
fun interface OrigenVentas {
    suspend fun ventas(): List<VentaRegistrada>
}

/** Todavía no hay ventas guardadas: no se inventa ninguna. */
object SinVentasRegistradas : OrigenVentas {
    override suspend fun ventas(): List<VentaRegistrada> = emptyList()
}

/** Calcula el resumen sobre las ventas que entregue el [origen]. [hoy] se inyecta para poder probarlo. */
class FinanzasLocal(
    private val origen: OrigenVentas,
    private val hoy: () -> Fecha,
) : FinanzasRepository {
    override suspend fun resumen(periodo: PeriodoFinanciero): ResumenFinanciero =
        resumirVentas(origen.ventas(), periodo, hoy())
}

/** La fecha de hoy según el reloj del celular. */
fun fechaDeHoy(): Fecha {
    val c = GregorianCalendar()
    return Fecha(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
}
