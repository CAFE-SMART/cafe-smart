package com.cafesmart.app.viewmodel.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.finanzas.AlmacenClaveFinanciera
import com.cafesmart.app.data.finanzas.ClaveFinanciera
import com.cafesmart.app.data.finanzas.ConfirmacionCuenta
import com.cafesmart.app.data.finanzas.ProveedorCuenta
import com.cafesmart.app.data.finanzas.conNuevoFallo
import com.cafesmart.app.data.finanzas.errorDeClaveNueva
import com.cafesmart.app.data.finanzas.errorDeConfirmacion
import com.cafesmart.app.data.finanzas.intentosRestantes
import com.cafesmart.app.data.finanzas.milisDeBloqueo
import com.cafesmart.app.data.finanzas.textoDeEspera
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Acceso a las métricas financieras con clave propia (Figma, sección NUEVAS de Flujos):
//   primer acceso  -> CREAR la clave;  siguientes -> PEDIR la clave (como un login);
//   "¿Olvidaste tu clave?" -> CONFIRMAR_IDENTIDAD con la cuenta -> NUEVA_CLAVE -> DENTRO.
// Mientras la clave no sea correcta no se pasa a DENTRO, así que la pantalla nunca muestra datos.

/** En qué paso del acceso está la persona. */
enum class PasoAcceso { CREAR, PEDIR, CONFIRMAR_IDENTIDAD, NUEVA_CLAVE, DENTRO }

const val ERROR_CLAVE_INCORRECTA = "La clave no es correcta."
const val ERROR_CONTRASENA_CUENTA_VACIA = "Escribe la contraseña de tu cuenta."
const val ERROR_CONTRASENA_CUENTA_INCORRECTA = "La contraseña de tu cuenta no es correcta. Inténtalo de nuevo."

/**
 * @property errorClave error bajo el campo de la clave; [errorConfirmacion] bajo el de repetirla;
 *   [errorCuenta] bajo la contraseña de la cuenta o el aviso de Google.
 * @property trabajando true mientras se calcula el hash o se espera a Firebase (no se deja tocar dos veces).
 */
data class AccesoUiState(
    val paso: PasoAcceso = PasoAcceso.PEDIR,
    val clave: String = "",
    val confirmacion: String = "",
    val contrasenaCuenta: String = "",
    val proveedor: ProveedorCuenta = ProveedorCuenta.CORREO,
    val errorClave: String? = null,
    val errorConfirmacion: String? = null,
    val errorCuenta: String? = null,
    val trabajando: Boolean = false,
) {
    val autorizado: Boolean get() = paso == PasoAcceso.DENTRO
}

class AccesoFinancieroViewModel(
    private val uid: String,
    private val almacen: AlmacenClaveFinanciera,
    private val cuenta: ConfirmacionCuenta,
    // El hash tarda un poco: se calcula fuera del hilo principal (en las pruebas se pasa uno de prueba).
    private val trabajo: CoroutineDispatcher = Dispatchers.Default,
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _estado = MutableStateFlow(estadoInicial())
    val estado: StateFlow<AccesoUiState> = _estado

    // Si ya se agotaron los intentos (por ejemplo, la persona cerró y volvió a abrir), se avisa desde el principio.
    private fun estadoInicial(): AccesoUiState {
        if (almacen.leer(uid) == null) return AccesoUiState(paso = PasoAcceso.CREAR)
        val espera = milisDeBloqueo(almacen.leerFallos(uid), reloj())
        return AccesoUiState(paso = PasoAcceso.PEDIR, errorClave = if (espera > 0) mensajeDeBloqueo(espera) else null)
    }

    fun cambiarClave(valor: String) = _estado.update { it.copy(clave = valor, errorClave = null) }

    fun cambiarConfirmacion(valor: String) = _estado.update { it.copy(confirmacion = valor, errorConfirmacion = null) }

    fun cambiarContrasenaCuenta(valor: String) = _estado.update { it.copy(contrasenaCuenta = valor, errorCuenta = null) }

    /** Primer acceso o clave nueva tras olvidarla: valida, guarda el hash y entra. */
    fun guardarClave() {
        val actual = _estado.value
        if (actual.trabajando) return
        if (actual.paso != PasoAcceso.CREAR && actual.paso != PasoAcceso.NUEVA_CLAVE) return
        val errorClave = errorDeClaveNueva(actual.clave)
        val errorConfirmacion = if (errorClave == null) errorDeConfirmacion(actual.clave, actual.confirmacion) else null
        if (errorClave != null || errorConfirmacion != null) {
            _estado.update { it.copy(errorClave = errorClave, errorConfirmacion = errorConfirmacion) }
            return
        }
        _estado.update { it.copy(trabajando = true) }
        viewModelScope.launch {
            val guardada = withContext(trabajo) { ClaveFinanciera.crear(actual.clave) }
            almacen.guardar(uid, guardada)
            // Una clave nueva (por ejemplo, tras olvidarla) empieza sin intentos fallidos.
            almacen.guardarFallos(uid, emptyList())
            _estado.value = AccesoUiState(paso = PasoAcceso.DENTRO)
        }
    }

    /**
     * Acceso siguiente: compara la clave escrita con el hash guardado. Si no coincide, no pasa. Tras 3 claves
     * incorrectas en menos de 15 minutos se bloquea hasta que pase el tiempo (o se crea una clave nueva).
     */
    fun ingresar() {
        val actual = _estado.value
        if (actual.trabajando || actual.paso != PasoAcceso.PEDIR) return
        val espera = milisDeBloqueo(almacen.leerFallos(uid), reloj())
        if (espera > 0) {
            // Bloqueado: ni siquiera se comprueba la clave.
            _estado.update { it.copy(clave = "", errorClave = mensajeDeBloqueo(espera)) }
            return
        }
        if (actual.clave.isEmpty()) {
            _estado.update { it.copy(errorClave = ERROR_CLAVE_VACIA_ACCESO) }
            return
        }
        val guardada = almacen.leer(uid)
        if (guardada == null) {
            // La clave desapareció (por ejemplo, datos de la app borrados): se vuelve a crear.
            _estado.value = AccesoUiState(paso = PasoAcceso.CREAR)
            return
        }
        _estado.update { it.copy(trabajando = true) }
        viewModelScope.launch {
            val correcta = withContext(trabajo) { ClaveFinanciera.verificar(actual.clave, guardada) }
            if (correcta) {
                almacen.guardarFallos(uid, emptyList())
                _estado.value = AccesoUiState(paso = PasoAcceso.DENTRO)
            } else {
                val ahora = reloj()
                val fallos = conNuevoFallo(almacen.leerFallos(uid), ahora)
                almacen.guardarFallos(uid, fallos)
                val esperaAhora = milisDeBloqueo(fallos, ahora)
                _estado.value = actual.copy(
                    clave = "",
                    trabajando = false,
                    errorClave = if (esperaAhora > 0) mensajeDeBloqueo(esperaAhora) else mensajeDeIntento(intentosRestantes(fallos, ahora))
                )
            }
        }
    }

    /** "¿Olvidaste tu clave?": pasa a confirmar la identidad con la cuenta. */
    fun olvideMiClave() {
        if (_estado.value.paso != PasoAcceso.PEDIR) return
        _estado.value = AccesoUiState(paso = PasoAcceso.CONFIRMAR_IDENTIDAD, proveedor = cuenta.proveedor())
    }

    /** Vuelve al paso de pedir la clave (cancelar el olvido). Si nunca creó una, vuelve a crearla. */
    fun cancelarOlvido() {
        _estado.value = AccesoUiState(paso = if (almacen.leer(uid) == null) PasoAcceso.CREAR else PasoAcceso.PEDIR)
    }

    /** Cuenta de correo: confirma con la contraseña de la cuenta de Firebase. Si falla, no cambia nada. */
    fun confirmarConContrasena() {
        val actual = _estado.value
        if (actual.trabajando || actual.paso != PasoAcceso.CONFIRMAR_IDENTIDAD) return
        if (actual.contrasenaCuenta.isEmpty()) {
            _estado.update { it.copy(errorCuenta = ERROR_CONTRASENA_CUENTA_VACIA) }
            return
        }
        _estado.update { it.copy(trabajando = true, errorCuenta = null) }
        viewModelScope.launch {
            resolverConfirmacion(cuenta.confirmarConContrasena(actual.contrasenaCuenta))
        }
    }

    /** Cuenta de Google: vuelve a autenticar con Google. */
    fun confirmarConGoogle() {
        val actual = _estado.value
        if (actual.trabajando || actual.paso != PasoAcceso.CONFIRMAR_IDENTIDAD) return
        _estado.update { it.copy(trabajando = true, errorCuenta = null) }
        viewModelScope.launch {
            resolverConfirmacion(cuenta.confirmarConGoogle())
        }
    }

    // Confirmación correcta -> paso de crear la clave nueva. Incorrecta -> se queda aquí con su mensaje.
    private fun resolverConfirmacion(error: AuthError?) {
        _estado.value = if (error == null) {
            AccesoUiState(paso = PasoAcceso.NUEVA_CLAVE, proveedor = _estado.value.proveedor)
        } else {
            _estado.value.copy(
                trabajando = false,
                contrasenaCuenta = "",
                errorCuenta = if (error == AuthError.CREDENCIALES_INCORRECTAS) ERROR_CONTRASENA_CUENTA_INCORRECTA else error.mensaje
            )
        }
    }
}

const val ERROR_CLAVE_VACIA_ACCESO = "Escribe tu clave."

/** "La clave no es correcta. Te quedan 2 intentos." */
fun mensajeDeIntento(restantes: Int): String =
    "$ERROR_CLAVE_INCORRECTA Te " + if (restantes == 1) "queda 1 intento." else "quedan $restantes intentos."

/** Aviso cuando se agotaron los 3 intentos: cuánto esperar y qué hacer mientras tanto. */
fun mensajeDeBloqueo(milis: Long): String =
    "Por seguridad, espera ${textoDeEspera(milis)} para volver a intentar. " +
        "Si olvidaste tu clave, toca \"¿Olvidaste tu clave?\" para crear una nueva."
