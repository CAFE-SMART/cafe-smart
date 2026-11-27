package com.cafesmart.app.data.secado

import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Secado
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.sublotesParaVenderEjemplo
import com.cafesmart.app.viewmodel.drying.descontarEnvios
import com.cafesmart.app.viewmodel.sale.cantidadValida

// Cuantos kg de un sublote se mandan a secar.
data class EnvioASecar(
    val subloteId: Long,
    val pesoKg: Double
)

// Repositorio (MVVM): de donde salen los datos de Secado y donde se guardan los cambios.
// Hoy solo existe la version de ejemplo (en memoria). Cuando exista la tarjeta "Secado conectado a Room",
// se agrega otra clase que cumpla esta misma interfaz y el ViewModel no cambia.
interface SecadoRepository {
    // Sublotes con cafe disponible, de todos los tipos. La pantalla se queda solo con el verde.
    fun inventario(): List<SubloteConLote>

    // Inicia un secado: le resta a cada sublote lo que se envia y devuelve el secado EN_CURSO.
    // Lanza IllegalArgumentException si el envio no es valido (sin sublotes, repetido, no es verde o pasa de lo disponible).
    fun iniciarSecado(fechaInicio: Fecha, envios: List<EnvioASecar>): Secado
}

// Version de ejemplo: guarda el inventario en memoria, asi el segundo secado ya ve lo que quedo del primero.
// Se pierde al cerrar la app (para eso es la tarjeta de Room).
class SecadoRepositoryEjemplo(
    inventarioInicial: List<SubloteConLote> = sublotesParaVenderEjemplo()
) : SecadoRepository {

    private var inventario: List<SubloteConLote> = inventarioInicial
    private var ultimoId = 0L

    override fun inventario(): List<SubloteConLote> = inventario

    override fun iniciarSecado(fechaInicio: Fecha, envios: List<EnvioASecar>): Secado {
        require(envios.isNotEmpty()) { "Elige al menos un sublote para secar." }
        require(envios.map { it.subloteId }.toSet().size == envios.size) { "Un sublote no puede repetirse en el secado." }
        envios.forEach { envio ->
            val item = inventario.firstOrNull { it.sublote.id == envio.subloteId }
            requireNotNull(item) { "El sublote ${envio.subloteId} no existe." }
            require(item.tipoCafe == TipoCafe.VERDE) { "Solo se seca cafe verde." }
            require(cantidadValida(envio.pesoKg, item.sublote.pesoDisponibleKg)) {
                "La cantidad del sublote ${envio.subloteId} debe ser mayor a 0 y no pasar de lo disponible."
            }
        }
        inventario = descontarEnvios(inventario, envios)
        ultimoId += 1
        return Secado(id = ultimoId, fechaInicio = fechaInicio)
    }
}
