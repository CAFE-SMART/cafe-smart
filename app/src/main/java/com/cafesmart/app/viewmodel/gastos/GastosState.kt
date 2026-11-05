package com.cafesmart.app.viewmodel.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.*
// Aritmética entera de gran tamaño para sumas sin desbordamiento de Long.
import java.math.BigInteger
// Formato numérico, fechas, idioma y zona horaria compatibles con este proyecto.
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ============================================================
// 2. BORRADOR DEL FORMULARIO
// ============================================================
/**
 * Borrador de presentación: conserva el texto inválido para explicar el error sin transformarlo.
 *
 * Es un modelo de presentación, distinto de Gasto. Mantiene monto y asignaciones como
 * texto para no perder lo que escribió el usuario cuando hay un formato inválido. data class
 * aporta copy, que permite cambiar un campo conservando el resto sin mutar el objeto anterior.
 *
 * @param concepto Texto obligatorio que se valida al continuar.
 * @param descripcion Detalle opcional.
 * @param monto Texto del monto; todavía no se considera un Long válido.
 * @param fecha Fecha civil seleccionada; inicia con el día actual en Colombia.
 * @param tipo Categoría oficial elegida.
 * @param estado Estado de pago elegido.
 * @param aplicaA Destino general o sublotes del gasto.
 * @param asignaciones Relaciona ID de sublote con el texto del monto escrito por el usuario.
 */
data class GastoFormulario(
    val concepto: String = "",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: Fecha = hoyEnColombia(),
    val tipo: TipoGasto = TipoGasto.TRANSPORTE,
    val estado: EstadoPago = EstadoPago.PAGADO,
    val aplicaA: AplicaGasto = AplicaGasto.GENERAL,
    // El usuario decide cada asignación: no se infiere un reparto por peso ni partes iguales.
    val asignaciones: Map<Long, String> = emptyMap()
)

// ============================================================
// 3. MENSAJES DE VALIDACIÓN
// ============================================================
/**
 * Errores por sección del formulario; una validación correcta no contiene mensajes.
 *
 * Cada String? puede contener un mensaje o null. null significa que esa sección no
 * tiene un error informado; no es un gasto guardado ni el resultado de la persistencia.
 *
 * @param concepto Error del concepto.
 * @param monto Error del importe.
 * @param fecha Error de la fecha.
 * @param sublotes Error de selección, disponibilidad o suma del reparto.
 */
data class GastoErrores(
    val concepto: String? = null,
    val monto: String? = null,
    val fecha: String? = null,
    val sublotes: String? = null
) {
    // any devuelve true si al menos uno de los mensajes no es null.
    val hayErrores get() = listOf(concepto, monto, fecha, sublotes).any { it != null }
}

// ============================================================
// 4. OPCIONES DEL FILTRO
// ============================================================
/**
 * Filtros exclusivos de la lista; no cambian el total pendiente global.
 *
 * PENDIENTES y PAGADOS muestran un estado de pago; TODOS no lo restringe. Son opciones
 * de la interfaz, no nuevas categorías del modelo de datos.
 *
 * @param etiqueta Texto que aparece en el control segmentado.
 */
enum class FiltroGasto(val etiqueta: String) { PENDIENTES("Pendientes"), PAGADOS("Pagados"), TODOS("Todos") }

// ============================================================
// 5. ETAPAS DEL FLUJO DE REGISTRO
// ============================================================
/**
 * Etapas del registro; el guardado bloquea edición, cierre y doble envío.
 *
 * EDICION permite escribir; CONFIRMACION solicita aceptación; GUARDANDO bloquea el envío.
 * EXITO indica una operación terminada correctamente y ERROR permite regresar o reintentar.
 */
enum class ResultadoGasto { EDICION, CONFIRMACION, GUARDANDO, EXITO, ERROR }

// ============================================================
// 6. ESTADO COMPLETO DE LA INTERFAZ
// ============================================================
/**
 * Estado observable compartido por las rutas de Gastos durante la sesión de navegación.
 *
 * Agrupa los datos que publica el ViewModel mediante StateFlow. Sus propiedades derivadas
 * se calculan al leerlas: no almacenan otra lista ni cambian los gastos del repositorio.
 *
 * @param formulario Borrador actual.
 * @param errores Mensajes actuales del formulario.
 * @param gastos Registros recibidos del repositorio.
 * @param sublotes Opciones recibidas del inventario; una lista vacía no inventa ejemplos.
 * @param filtro Selección actual del listado.
 * @param resultado Etapa actual de edición o guardado.
 */
data class GastosUiState(
    val formulario: GastoFormulario = GastoFormulario(),
    val errores: GastoErrores = GastoErrores(),
    val gastos: List<Gasto> = emptyList(),
    val sublotes: List<Sublote> = emptyList(),
    val filtro: FiltroGasto = FiltroGasto.PENDIENTES,
    val resultado: ResultadoGasto = ResultadoGasto.EDICION
) {
    // filter crea la lista visible según el filtro, sin eliminar registros de gastos.
    val visibles get() = gastos.filter {
        when (filtro) {
            FiltroGasto.PENDIENTES -> it.estadoPago == EstadoPago.PENDIENTE
            FiltroGasto.PAGADOS -> it.estadoPago == EstadoPago.PAGADO
            FiltroGasto.TODOS -> true
        }
    }
    // BigInteger evita desbordar Long al sumar varios gastos individualmente válidos.
    // La suma usa la lista completa, no visibles. fold acumula solo pendientes en BigInteger
    // para que varios importes Long válidos no desborden al sumarlos.
    val totalPendiente get() = gastos.filter { it.estadoPago == EstadoPago.PENDIENTE }
        .fold(BigInteger.ZERO) { total, gasto -> total + BigInteger.valueOf(gasto.monto) }
}

// ============================================================
// 7. FECHA ACTUAL SIN JAVA.TIME
// ============================================================
/**
 * API 24: Calendar sustituye a java.time sin depender de desugaring.
 *
 * Calendar permite obtener el día en America/Bogota con API 24. Android cuenta el mes
 * desde cero; se suma uno para crear el modelo Fecha, cuyo mes va de 1 a 12.
 */
fun hoyEnColombia(): Fecha = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).let {
    Fecha(it.get(Calendar.DAY_OF_MONTH), it.get(Calendar.MONTH) + 1, it.get(Calendar.YEAR))
}

// ============================================================
// 8. FORMATO MONETARIO PARA LA PANTALLA
// ============================================================
/**
 * Representación COP sin centavos: el modelo oficial almacena pesos enteros.
 *
 * NumberFormat de es-CO organiza los separadores de miles. En este módulo recibe Long
 * o BigInteger ya calculados; no es la función que valida ni convierte el texto del formulario.
 *
 * @param valor Número que se prepara como texto COP sin centavos.
 */
fun dineroGasto(valor: Number): String = "$ ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(valor)}"

// ============================================================
// 9. FORMATO DE FECHA PARA LA PANTALLA
// ============================================================
/**
 * Fecha del modelo oficial, sin convertirla a UTC ni desplazar el día.
 *
 * String.format completa día/mes con dos cifras y año con cuatro. Locale.ROOT mantiene
 * el patrón dd/MM/yyyy; no analiza texto, valida fechas ni convierte zonas horarias.
 *
 * @param fecha Valor civil del modelo que se quiere representar.
 */
fun fechaGasto(fecha: Fecha): String = String.format(Locale.ROOT, "%02d/%02d/%04d", fecha.dia, fecha.mes, fecha.anio)
