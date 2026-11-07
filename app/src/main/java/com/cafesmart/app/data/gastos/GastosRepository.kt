package com.cafesmart.app.data.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.Gasto
import com.cafesmart.app.data.model.GastoSublote
import com.cafesmart.app.data.sample.gastosEjemplo
// Flujos observables para publicar y leer el estado.
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ============================================================
// 2. CONTRATO DE LA FUENTE DE GASTOS
// ============================================================
/**
 * Puerto de persistencia de Gastos. El guardado debe completar o lanzar un error real.
 *
 * Una interfaz define qué necesita el ViewModel, sin obligarlo a conocer cómo se guardan
 * los datos. La fuente observable entrega listas y la operación suspend permite completar
 * un guardado o informar un fallo; suspend no significa por sí solo que exista una red.
 */
interface GastosRepository {
    // StateFlow expone el listado más reciente para que el ViewModel lo observe.
    val gastos: StateFlow<List<Gasto>>
    // ============================================================
    // 3. OPERACIÓN DEL CONTRATO
    // ============================================================
    /**
     * La implementación debe terminar correctamente o lanzar una excepción. El ViewModel
     * utiliza ese resultado real para publicar EXITO o ERROR.
     *
     * @param gasto Entidad oficial validada.
     * @param reparto Relaciones con sublotes cuya suma debe respetar las reglas del gasto.
     */
    suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>)
}

// ============================================================
// 4. IMPLEMENTACIÓN DE DEMOSTRACIÓN EN MEMORIA
// ============================================================
/**
 * Demostración en memoria, aislada de Firebase y de los datos de los demás módulos.
 * Vive con el ViewModel y se pierde al destruir su propietario o terminar el proceso.
 * Pendiente: sustituir por el repositorio permanente y una fuente real de inventario.
 *
 * registros conserva la lista observable y repartos conserva las asignaciones por ID.
 * Ambos viven solo en esta instancia: cerrar su sesión o terminar el proceso pierde los datos.
 * No usa Room, archivos, Firebase ni otro servicio de persistencia.
 */
class DemoGastosRepository : GastosRepository {
    // MutableStateFlow publica listas nuevas; el mapa asocia cada ID de gasto con su lista de relaciones.
    private val registros = MutableStateFlow<List<Gasto>>(emptyList())
    private val repartos = mutableMapOf<Long, List<GastoSublote>>()
    override val gastos: StateFlow<List<Gasto>> = registros

    /** Carga explícita para revisar el estado con datos, sin sobrescribir registros. */
    @Synchronized
    fun cargarEjemplos() {
        if (registros.value.isNotEmpty()) return
        gastosEjemplo().asReversed().forEach { registrar(it, emptyList()) }
    }

    // La operación es atómica en memoria. Un reintento del mismo ID y contenido no duplica.
    // ============================================================
    // 5. VALIDACIÓN E IDEMPOTENCIA DEL REGISTRO
    // ============================================================
    /**
     * @Synchronized serializa llamadas sobre esta instancia. require comprueba el ID, el reparto
     * y los sublotes repetidos. Si ya existe el mismo ID con el mismo contenido se devuelve
     * sin añadir otra fila; un contenido distinto para ese ID provoca un error.
     *
     * @param gasto Registro oficial que se intenta insertar.
     * @param reparto Asignaciones oficiales que se guardan junto al gasto.
     */
    @Synchronized
    private fun registrar(gasto: Gasto, reparto: List<GastoSublote>) {
        require(gasto.id > 0 && gasto.repartoEsValido(reparto))
        require(reparto.map { it.subloteId }.distinct().size == reparto.size)
        // La búsqueda por ID permite reconocer un reintento del mismo registro, en lugar de duplicarlo.
        val anterior = registros.value.find { it.id == gasto.id }
        if (anterior != null) {
            require(anterior == gasto && repartos[gasto.id] == reparto) { "El ID ya corresponde a otro gasto." }
            return
        }
        // Se copia el reparto y luego se publica la nueva lista, con el gasto más reciente al principio.
        // La emisión de StateFlow es lo que actualiza el listado que observa el ViewModel.
        repartos[gasto.id] = reparto.toList()
        registros.value = listOf(gasto) + registros.value
    }

    // ============================================================
    // 6. ADAPTACIÓN DEL CONTRATO AL ALMACENAMIENTO DEMO
    // ============================================================
    /**
     * Esta implementación delega en registrar. No hay una segunda escritura ni una consulta
     * a una base de datos oculta; la suspensión permite mantener el contrato del repositorio.
     *
     * @param gasto Gasto recibido desde el ViewModel.
     * @param reparto Reparto que ya pasó por la conversión y validación.
     */
    override suspend fun guardar(gasto: Gasto, reparto: List<GastoSublote>) = registrar(gasto, reparto)
}
