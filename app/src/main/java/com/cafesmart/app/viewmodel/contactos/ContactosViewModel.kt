package com.cafesmart.app.viewmodel.contactos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.PurchaseRepository
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.viewmodel.purchase.conDocumento
import com.cafesmart.app.viewmodel.purchase.conNombre
import com.cafesmart.app.viewmodel.purchase.conTelefono
import com.cafesmart.app.viewmodel.purchase.conTipo
import com.cafesmart.app.viewmodel.purchase.crearProductor
import com.cafesmart.app.viewmodel.purchase.normalizarEspacios
import com.cafesmart.app.viewmodel.purchase.validarDocumento
import com.cafesmart.app.viewmodel.purchase.validarNombre
import com.cafesmart.app.viewmodel.purchase.validarTelefono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

// Contactos registrados (ciclo 3, Amelia, orden 16): ver, buscar y editar productores y clientes.
// Lógica sin pantalla: la pantalla espera el diseño aprobado por Andrés (Figma nodo 4355-1081) y los clientes
// esperan Ventas en Room (PR #43, Katherine). Mientras tanto los CLIENTES quedan marcados como no disponibles.
// Los productores usan el repositorio de Compras que ya existe (Room): no hay otra base ni otra entidad, y editar
// conserva el id, así que las compras ya registradas siguen apuntando al mismo productor.
// Reutiliza las reglas de Compras (limpiar, validar y "documento repetido"): mismos mensajes en las dos pantallas.

enum class TipoContacto(val etiqueta: String) { PRODUCTORES("Productores"), CLIENTES("Clientes") }

const val ERROR_CARGAR_CONTACTOS = "No se pudieron cargar los contactos. Inténtalo de nuevo."
const val ERROR_GUARDAR_CONTACTO = "No se pudo guardar el contacto. Inténtalo de nuevo."
const val MENSAJE_DOCUMENTO_REPETIDO = "Este documento ya está registrado. Verifica en tu lista."

/** Sin tildes, en minúsculas y con un solo espacio: "  José   PÉREZ" → "jose perez". */
fun normalizarBusqueda(texto: String): String =
    Normalizer.normalize(normalizarEspacios(texto).lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/**
 * Busca por nombre (sin importar mayúsculas ni tildes) o por documento (solo dígitos, sin puntos: "900.123" encuentra
 * "900123456"). Texto vacío = todos, en el mismo orden.
 */
fun buscarContactos(productores: List<Productor>, texto: String): List<Productor> {
    val consulta = normalizarBusqueda(texto)
    if (consulta.isEmpty()) return productores
    val digitos = texto.filter { it.isDigit() }
    return productores.filter { p ->
        normalizarBusqueda(p.nombre).contains(consulta) ||
            (digitos.isNotEmpty() && digitos.length == texto.count { !it.isWhitespace() && it != '.' && it != '-' } &&
                p.documento.contains(digitos))
    }
}

/** Errores por campo del formulario de edición. Todos null = se puede guardar. */
data class ErroresContacto(val nombre: String? = null, val documento: String? = null, val telefono: String? = null) {
    val hayErrores: Boolean get() = nombre != null || documento != null || telefono != null
}

/** Valida la edición de [id]: su propio documento actual NO cuenta como repetido; el de otro contacto sí. */
fun validarContacto(id: Long, datos: NuevoProductor, todos: List<Productor>): ErroresContacto =
    ErroresContacto(
        nombre = validarNombre(datos.nombre, datos.tipo),
        documento = validarDocumento(datos.documento, datos.tipo, todos.filter { it.id != id }.map { it.documento }),
        telefono = validarTelefono(datos.telefono),
    )

data class ContactosUiState(
    val tipo: TipoContacto = TipoContacto.PRODUCTORES,
    val busqueda: String = "",
    val productores: List<Productor> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val enEdicion: Productor? = null,
    val creando: Boolean = false,
    val datos: NuevoProductor = NuevoProductor(),
    val errores: ErroresContacto = ErroresContacto(),
    val guardando: Boolean = false,
) {
    /** Clientes: llegan con Ventas en Room (PR #43). Hasta entonces la lista no existe (no se muestra vacía como real). */
    val clientesDisponibles: Boolean get() = false

    val visibles: List<Productor>
        get() = if (tipo == TipoContacto.PRODUCTORES) buscarContactos(productores, busqueda) else emptyList()
}

class ContactosViewModel(private val repositorio: PurchaseRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ContactosUiState())
    val estado: StateFlow<ContactosUiState> = _estado

    init { cargar() }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = repositorio.listarProductores()
                _estado.update { it.copy(productores = lista, cargando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = ERROR_CARGAR_CONTACTOS) }
            }
        }
    }

    fun elegirTipo(tipo: TipoContacto) = _estado.update { it.copy(tipo = tipo) }
    fun buscar(texto: String) = _estado.update { it.copy(busqueda = texto) }

    fun editar(productor: Productor) = _estado.update {
        it.copy(
            enEdicion = productor,
            creando = false,
            error = null,
            datos = NuevoProductor(productor.tipoDocumento, productor.documento, productor.nombre, productor.telefono),
            errores = ErroresContacto(),
        )
    }

    fun agregarProductor() = _estado.update {
        it.copy(enEdicion = null, creando = true, datos = NuevoProductor(), errores = ErroresContacto(), error = null)
    }

    fun cambiarTipoDocumento(tipo: TipoDocumento) = _estado.update { it.copy(datos = it.datos.conTipo(tipo)) }
    fun cambiarDocumento(texto: String) = _estado.update { it.copy(datos = it.datos.conDocumento(texto)) }
    fun cambiarNombre(texto: String) = _estado.update { it.copy(datos = it.datos.conNombre(texto)) }
    fun cambiarTelefono(texto: String) = _estado.update { it.copy(datos = it.datos.conTelefono(texto)) }
    fun cancelarEdicion() = _estado.update { it.copy(enEdicion = null, creando = false, errores = ErroresContacto()) }

    /** Valida y guarda en Room. Con errores no guarda nada y los muestra por campo. */
    fun guardar() {
        val estado = _estado.value
        val original = estado.enEdicion
        if (original == null && !estado.creando) return
        if (estado.guardando) return
        val errores = validarContacto(original?.id ?: -1L, estado.datos, estado.productores)
        if (errores.hayErrores) { _estado.update { it.copy(errores = errores) }; return }
        val editado = original?.copy(
            nombre = normalizarEspacios(estado.datos.nombre),
            documento = estado.datos.documento,
            tipoDocumento = estado.datos.tipo,
            telefono = estado.datos.telefono,
        ) ?: crearProductor(estado.datos)
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                when (val r = if (original == null) repositorio.guardarProductor(editado) else repositorio.actualizarProductor(editado)) {
                    is ResultadoProductor.Guardado -> _estado.update { s ->
                        s.copy(
                            productores = if (original == null) s.productores + r.productor else s.productores.map { if (it.id == r.productor.id) r.productor else it },
                            enEdicion = null, creando = false, guardando = false, errores = ErroresContacto(),
                        )
                    }
                    ResultadoProductor.DocumentoRepetido -> _estado.update {
                        it.copy(guardando = false, errores = ErroresContacto(documento = MENSAJE_DOCUMENTO_REPETIDO))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, error = ERROR_GUARDAR_CONTACTO) }
            }
        }
    }
}
