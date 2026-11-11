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
 * utilidad = ingresos - costo del café vendido - gastos asociados a lo vendido (RN-G-28; los gastos restan).
 * costoVenta = lo que costó el café vendido (su compra) más los gastos asociados (secado, transporte, generales).
 * comprasPesos y gastosPeriodo son aparte: todo lo comprado y todo lo gastado en el periodo, se haya vendido o no.
 */
data class ResumenFinanciero(
    val ingresos: Long,
    val costoCafe: Long,
    val gastos: Long,
    val utilidad: Long,
    val mermaKg: Double,
    // Cuántas ventas hay en el periodo.
    val ventas: Int = 0,
    // Total pagado por las compras del periodo y cuántas fueron.
    val comprasPesos: Long = 0,
    val compras: Int = 0,
    // Todos los gastos registrados en el periodo.
    val gastosPeriodo: Long = 0,
) {
    /** Costo de venta: el costo de comprar el café vendido más sus gastos asociados. */
    val costoVenta: Long get() = costoCafe + gastos

    /** false cuando no hay ninguna venta, compra, gasto ni merma: todo vale 0 y se muestra el aviso vacío. */
    val hayDatos: Boolean get() = ventas > 0 || compras > 0 || gastosPeriodo > 0 || mermaKg > 0.0
}

interface FinanzasRepository {
    suspend fun resumen(periodo: PeriodoFinanciero): ResumenFinanciero
}

/**
 * Una venta ya registrada, con lo necesario para calcular su utilidad.
 * [precioCompraKg] es lo que costó comprar cada kg vendido (el costo del sublote del que salió).
 * [gastos] son los gastos asociados a esa venta, en pesos: los del sublote (transporte, cargue), los de su secado
 * y la parte que le toque de los generales.
 */
data class VentaRegistrada(
    val fecha: Fecha,
    val kgVendidos: Double,
    val precioVentaKg: Double,
    val precioCompraKg: Double,
    val gastos: Double,
)

/** Una compra confirmada: cuánto se pagó en total (en pesos) el día [fecha]. */
data class CompraRegistrada(val fecha: Fecha, val totalPesos: Long)

/** Un gasto registrado (de cualquier tipo: general, de sublotes o de secado) por [montoPesos] el día [fecha]. */
data class GastoRegistrado(val fecha: Fecha, val montoPesos: Long)

/** Café perdido al secar: [kg] de merma de un secado terminado el día [fecha]. */
data class MermaRegistrada(val fecha: Fecha, val kg: Double)

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
        mermaKg = 0.0,
        ventas = enPeriodo.size,
    )
}

/** Resumen completo del periodo: las ventas (utilidad), las compras, todos los gastos y la merma de secado. */
fun resumirPeriodo(
    periodo: PeriodoFinanciero,
    hoy: Fecha,
    ventas: List<VentaRegistrada> = emptyList(),
    compras: List<CompraRegistrada> = emptyList(),
    gastos: List<GastoRegistrado> = emptyList(),
    mermas: List<MermaRegistrada> = emptyList(),
): ResumenFinanciero {
    val comprasEnPeriodo = compras.filter { enElPeriodo(it.fecha, periodo, hoy) }
    return resumirVentas(ventas, periodo, hoy).copy(
        mermaKg = mermas.filter { enElPeriodo(it.fecha, periodo, hoy) }.sumOf { it.kg },
        comprasPesos = comprasEnPeriodo.sumOf { it.totalPesos },
        compras = comprasEnPeriodo.size,
        gastosPeriodo = gastos.filter { enElPeriodo(it.fecha, periodo, hoy) }.sumOf { it.montoPesos },
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
 * De dónde salen los datos del Resumen financiero (y luego del Historial y las Gráficas). Cada función es de quien
 * tiene esa parte en Room; las que aún no existen devuelven lista vacía y el Resumen las muestra en 0:
 * - [ventas]: ventas guardadas, con el costo por kg de su sublote y sus gastos asociados (Carlos, Ventas a Room).
 * - [compras]: compras confirmadas (Carlos, Compras a Room).
 * - [gastos]: todos los gastos registrados (Amelia, Gastos en Room).
 * - [mermas]: merma de cada secado terminado (Katherine, Secado en Room).
 * Para conectarlo basta escribir otra implementación y cambiarla en AppNavHost: ni el repositorio ni las pantallas cambian.
 */
interface OrigenFinanciero {
    suspend fun ventas(): List<VentaRegistrada> = emptyList()
    suspend fun compras(): List<CompraRegistrada> = emptyList()
    suspend fun gastos(): List<GastoRegistrado> = emptyList()
    suspend fun mermas(): List<MermaRegistrada> = emptyList()
}

/** Todavía no hay nada guardado: no se inventa ningún dato. */
object SinDatosFinancieros : OrigenFinanciero

/** Calcula el resumen sobre lo que entregue el [origen]. [hoy] se inyecta para poder probarlo. */
class FinanzasLocal(
    private val origen: OrigenFinanciero,
    private val hoy: () -> Fecha,
) : FinanzasRepository {
    override suspend fun resumen(periodo: PeriodoFinanciero): ResumenFinanciero =
        resumirPeriodo(periodo, hoy(), origen.ventas(), origen.compras(), origen.gastos(), origen.mermas())
}

/** La fecha de hoy según el reloj del celular. */
fun fechaDeHoy(): Fecha {
    val c = GregorianCalendar()
    return Fecha(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
}
