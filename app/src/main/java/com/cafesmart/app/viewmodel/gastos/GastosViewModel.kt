package com.cafesmart.app.viewmodel.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// ViewModel y corrutinas ligadas a su ciclo de vida.
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Contrato del repositorio y fuente de demostración de Gastos.
import com.cafesmart.app.data.gastos.DemoGastosRepository
import com.cafesmart.app.data.gastos.GastosRepository
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.Sublote
// Corrutinas, cambios de hilo y manejo de cancelación.
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
// Flujos observables para publicar y leer el estado.
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
// Corrutinas, cambios de hilo y manejo de cancelación.
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// 2. VIEWMODEL Y PROPIEDAD DEL ESTADO
// ============================================================
/**
 * Único propietario del borrador, filtros y resultados. Las dos pantallas reciben estado
 * y emiten acciones; no escriben datos. Sobrevive a recomposiciones y rotaciones, no al proceso.
 * [repository] permite verificar fallos sin conectar servicios externos.
 * [sublotes] solo debe contener inventario real; por defecto no se fabrican opciones.
 *
 * ViewModel separa la lógica de presentación de las funciones Composable. Recibe eventos,
 * actualiza un estado y se comunica con el repositorio; no dibuja pantallas. El propietario HOME
 * permite compartir el borrador entre rutas y conservarlo durante cambios de configuración.
 *
 * @param repository Fuente de Gastos; por defecto es una demostración en memoria, sin guardado permanente.
 * @param sublotes Opciones disponibles para asociar; por defecto está vacío.
 */
class GastosViewModel(
    private val repository: GastosRepository = DemoGastosRepository(),
    sublotes: List<Sublote> = emptyList()
) : ViewModel() {
    // MutableStateFlow permite publicar cambios. uiState se expone como StateFlow de lectura
    // para que las pantallas observen el estado sin escribir directamente en este contenedor.
    private val mutableState = MutableStateFlow(GastosUiState(sublotes = sublotes))
    val uiState: StateFlow<GastosUiState> = mutableState
    // El contador y el ID del borrador viven en esta instancia; no se guardan en disco.
    private var siguienteId = 1L
    private var idBorrador = siguienteId

    // La corrutina observa las listas emitidas por el repositorio. Cada emisión crea una copia
    // del estado con esos gastos; collectAsState, en la ruta, hace que la UI lea la lista actual.
    init {
        viewModelScope.launch {
            repository.gastos.collect { gastos -> mutableState.update { it.copy(gastos = gastos) } }
        }
    }

    // ============================================================
    // 3. EVENTOS DE LOS CAMPOS
    // ============================================================
    /**
     * Conserva el texto escrito; no borra caracteres inválidos para ocultar errores.
     *
     * Solo permite cambios en EDICION. update aplica la transformación recibida y elimina
     * los errores anteriores; los valores se comprobarán de nuevo al solicitar confirmación.
     *
     * @param cambio Función que recibe el borrador actual y devuelve una copia con los campos editados.
     */
    fun editar(cambio: (GastoFormulario) -> GastoFormulario) {
        if (uiState.value.resultado != ResultadoGasto.EDICION) return
        mutableState.update { it.copy(formulario = cambio(it.formulario), errores = GastoErrores()) }
    }

    // ============================================================
    // 4. CAMBIO DEL FILTRO DEL LISTADO
    // ============================================================
    /**
     * Solo actualiza la selección visual. GastosUiState.visibles aplica el filtro y
     * totalPendiente sigue calculándose sobre todos los registros pendientes.
     *
     * @param filtro Pendientes, pagados o todos según la opción pulsada.
     */
    fun filtrar(filtro: FiltroGasto) { mutableState.update { it.copy(filtro = filtro) } }

    // ============================================================
    // 5. ACTUALIZACIÓN DE LA FUENTE DEMO
    // ============================================================
    /**
     * Relee la fuente demo observable; no simula una sincronización remota inexistente.
     *
     * Lee el valor actual del StateFlow del repositorio. No hace una petición de red
     * ni convierte la demostración en una base de datos permanente.
     */
    fun actualizar() { mutableState.update { it.copy(gastos = repository.gastos.value) } }

    // ============================================================
    // 6. VALIDACIÓN ANTES DE CONFIRMAR
    // ============================================================
    /**
     * Confirma disponibilidad y reparto antes de mostrar la hoja, sin guardar todavía.
     *
     * Se valida el mismo borrador que ve la pantalla. Con errores se mantiene EDICION y
     * se publican mensajes; sin errores se pasa a CONFIRMACION. Todavía no se llama a guardar.
     */
    fun solicitarConfirmacion() {
        if (uiState.value.resultado != ResultadoGasto.EDICION) return
        val estado = uiState.value
        // Las reglas están en GastosValidation; la pantalla no decide por sí sola si los datos son válidos.
        val errores = GastosValidation.validar(estado.formulario, estado.sublotes)
        mutableState.update { it.copy(errores = errores, resultado =
            if (errores.hayErrores) ResultadoGasto.EDICION else ResultadoGasto.CONFIRMACION) }
    }

    // ============================================================
    // 7. REGRESO AL FORMULARIO
    // ============================================================
    /**
     * Cambia únicamente la etapa del flujo y conserva los campos. Durante GUARDANDO
     * no permite salir de la operación, para no iniciar otro envío al mismo tiempo.
     */
    fun volverAEditar() {
        if (uiState.value.resultado != ResultadoGasto.GUARDANDO) {
            mutableState.update { it.copy(resultado = ResultadoGasto.EDICION) }
        }
    }

    // ============================================================
    // 8. GUARDADO, CORRUTINAS Y ERRORES
    // ============================================================
    /**
     * Bloquea el doble toque antes de lanzar la corrutina. El ID se mantiene al reintentar;
     * un error nunca pasa por EXITO. La cancelación de la sesión se propaga correctamente.
     *
     * El paso a GUARDANDO es inmediato, antes de lanzar la corrutina: un segundo toque
     * no encuentra una etapa aceptada para reenviar. viewModelScope liga el trabajo a la vida
     * del ViewModel; withContext(IO) ejecuta el repositorio fuera del hilo de interfaz.
     * La idempotencia del repositorio complementa este bloqueo usando el mismo ID al reintentar.
     */
    fun confirmar() {
        val estado = uiState.value
        if (estado.resultado !in listOf(ResultadoGasto.CONFIRMACION, ResultadoGasto.ERROR)) return
        mutableState.update { it.copy(resultado = ResultadoGasto.GUARDANDO) }
        viewModelScope.launch {
            try {
                val (gasto, reparto) = GastosValidation.convertir(estado.formulario, idBorrador, estado.sublotes)
                withContext(Dispatchers.IO) { repository.guardar(gasto, reparto) }
                // EXITO se publica después de terminar guardar. TODOS permite ver también un gasto recién pagado.
                mutableState.update { it.copy(resultado = ResultadoGasto.EXITO, filtro = FiltroGasto.TODOS) }
            // La cancelación del ciclo de vida se propaga. Las demás excepciones producen ERROR,
            // sin mostrar un éxito falso ni borrar el borrador que puede corregirse o reintentarse.
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                mutableState.update { it.copy(resultado = ResultadoGasto.ERROR) }
            }
        }
    }

    // ============================================================
    // 9. NUEVO BORRADOR E IDENTIFICADOR LOCAL
    // ============================================================
    /**
     * Nuevo ID solo al descartar o terminar el borrador anterior. El listado permanece.
     *
     * El siguiente ID se elige con el contador y los IDs ya publicados en esta demostración.
     * Se reinician formulario, errores y resultado; copy conserva gastos, sublotes y filtro.
     * Este identificador local no sustituye una estrategia futura de IDs de base de datos.
     */
    fun nuevoRegistro() {
        if (uiState.value.resultado == ResultadoGasto.GUARDANDO) return
        siguienteId = maxOf(siguienteId, uiState.value.gastos.maxOfOrNull { it.id } ?: 0L) + 1
        idBorrador = siguienteId
        mutableState.update { it.copy(formulario = GastoFormulario(), errores = GastoErrores(), resultado = ResultadoGasto.EDICION) }
    }
}
