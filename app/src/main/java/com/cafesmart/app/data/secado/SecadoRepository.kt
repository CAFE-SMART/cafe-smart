package com.cafesmart.app.data.secado

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.EnvioSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.Secado
import com.cafesmart.app.data.model.SecadoConDetalle
import com.cafesmart.app.data.model.SecadoSublote
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.sublotesParaVenderEjemplo
import com.cafesmart.app.viewmodel.drying.costoKgSeco
import com.cafesmart.app.viewmodel.drying.descontarEnvios
import com.cafesmart.app.viewmodel.drying.errorFactorEntero
import com.cafesmart.app.viewmodel.drying.errorKilosSecos
import com.cafesmart.app.viewmodel.drying.kilosSecosTotal
import com.cafesmart.app.viewmodel.inventory.clave
import com.cafesmart.app.viewmodel.inventory.redondearKg
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

    // Secados EN_CURSO, el de inicio mas reciente primero. Sus resultantes van vacios (todavia no hay cafe seco).
    fun secadosEnCurso(): List<SecadoConDetalle>

    // Finaliza un secado: crea un sublote seco por cada calidad con kg > 0 y marca el secado FINALIZADO.
    // Valida todo antes de cambiar algo. Lanza IllegalArgumentException si el secado no existe o no esta EN_CURSO,
    // si la fecha de fin es anterior al inicio, si los kilos secos no son validos o si el factor (opcional; null = sin factor) no esta entre FACTOR_RENDIMIENTO_MIN y FACTOR_RENDIMIENTO_MAX.
    fun finalizarSecado(
        secadoId: Long,
        fechaFin: Fecha,
        kilosSecos: Map<Calidad, Double>,
        factorRendimiento: Int?
    ): SecadoConDetalle

    // Cancela un secado EN_CURSO y devuelve al inventario el cafe que se habia enviado.
    // Lanza IllegalArgumentException si el secado no existe o no esta EN_CURSO.
    fun cancelarSecado(secadoId: Long)
}

// Lo que guarda la version de ejemplo de cada secado: el secado, sus envios (con el sublote como estaba
// al enviarse, antes de descontar) y, si ya termino, los sublotes secos que resultaron.
private data class RegistroSecado(
    val secado: Secado,
    val enviados: List<EnvioSecado>,
    val resultantes: List<Sublote> = emptyList()
) {
    fun conDetalle() = SecadoConDetalle(secado, enviados, resultantes)
}

// Version de ejemplo: guarda el inventario en memoria, asi el segundo secado ya ve lo que quedo del primero.
// Se pierde al cerrar la app (para eso es la tarjeta de Room).
class SecadoRepositoryEjemplo(
    inventarioInicial: List<SubloteConLote> = sublotesParaVenderEjemplo()
) : SecadoRepository {

    private var inventario: List<SubloteConLote> = inventarioInicial
    private var ultimoId = 0L
    private var ultimoSubloteId = inventarioInicial.maxOfOrNull { it.sublote.id } ?: 0L
    private var ultimoLoteId = inventarioInicial.maxOfOrNull { it.lote.id } ?: 0L
    private var secados: List<RegistroSecado> = emptyList()

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
        val id = ultimoId + 1
        // Foto de cada sublote tal como estaba al enviarse (antes de descontar).
        val enviados = envios.map { envio ->
            val sublote = inventario.first { it.sublote.id == envio.subloteId }.sublote
            EnvioSecado(SecadoSublote(secadoId = id, subloteId = envio.subloteId, pesoEnviadoKg = envio.pesoKg), sublote)
        }
        inventario = descontarEnvios(inventario, envios)
        ultimoId = id
        val secado = Secado(id = id, fechaInicio = fechaInicio)
        secados = secados + RegistroSecado(secado, enviados)
        return secado
    }

    override fun secadosEnCurso(): List<SecadoConDetalle> =
        secados
            .filter { it.secado.estado == EstadoSecado.EN_CURSO }
            .sortedWith(compareByDescending<RegistroSecado> { it.secado.fechaInicio.clave() }.thenByDescending { it.secado.id })
            .map { it.conDetalle() }

    override fun finalizarSecado(
        secadoId: Long,
        fechaFin: Fecha,
        kilosSecos: Map<Calidad, Double>,
        factorRendimiento: Int?
    ): SecadoConDetalle {
        // Primero se valida todo; mientras no pase, no se modifica nada.
        val registro = enCurso(secadoId)
        require(fechaFin.clave() >= registro.secado.fechaInicio.clave()) {
            "La fecha de finalizacion no puede ser anterior al inicio."
        }
        val enviadoKg = redondearKg(registro.enviados.sumOf { it.envio.pesoEnviadoKg })
        errorKilosSecos(kilosSecos, enviadoKg)?.let { throw IllegalArgumentException(it) }
        factorRendimiento?.let { errorFactorEntero(it) }?.let { throw IllegalArgumentException(it) }

        val kgSecosTotal = kilosSecosTotal(kilosSecos)
        val costoKg = costoKgSeco(registro.enviados, kgSecosTotal)
        var nuevoInventario = inventario
        var subloteId = ultimoSubloteId
        var loteId = ultimoLoteId
        val resultantes = Calidad.entries.mapNotNull { calidad ->
            val kg = redondearKg(kilosSecos[calidad] ?: 0.0)
            if (kg <= 0.0) return@mapNotNull null
            // Un solo lote por (tipo, calidad): se usa el seco que ya exista; si no, se crea.
            val lote = nuevoInventario.firstOrNull { it.tipoCafe == TipoCafe.SECO && it.calidad == calidad }?.lote
                ?: Lote(id = ++loteId, tipoCafe = TipoCafe.SECO, calidad = calidad)
            val sublote = Sublote(
                id = ++subloteId,
                loteId = lote.id,
                secadoId = secadoId,
                pesoInicialKg = kg,
                pesoDisponibleKg = kg,
                costoKg = costoKg,
                fechaIngreso = fechaFin,
                factorRendimiento = factorRendimiento
            )
            nuevoInventario = nuevoInventario + SubloteConLote(sublote, lote)
            sublote
        }

        val finalizado = registro.secado.copy(
            fechaFin = fechaFin,
            estado = EstadoSecado.FINALIZADO,
            factorRendimiento = factorRendimiento
        )
        val nuevo = registro.copy(secado = finalizado, resultantes = resultantes)
        inventario = nuevoInventario
        ultimoSubloteId = subloteId
        ultimoLoteId = loteId
        secados = secados.map { if (it.secado.id == secadoId) nuevo else it }
        return nuevo.conDetalle()
    }

    override fun cancelarSecado(secadoId: Long) {
        val registro = enCurso(secadoId)
        // Se valida todo antes de cambiar nada: cada sublote enviado debe seguir en el inventario.
        registro.enviados.forEach { e ->
            require(inventario.any { it.sublote.id == e.sublote.id }) {
                "El sublote ${e.sublote.id} ya no esta en el inventario; no se puede cancelar el secado $secadoId."
            }
        }
        inventario = inventario.map { item ->
            val enviado = registro.enviados.firstOrNull { it.sublote.id == item.sublote.id }?.envio?.pesoEnviadoKg
                ?: return@map item
            item.copy(sublote = item.sublote.copy(pesoDisponibleKg = redondearKg(item.sublote.pesoDisponibleKg + enviado)))
        }
        secados = secados.map {
            if (it.secado.id == secadoId) it.copy(secado = it.secado.copy(estado = EstadoSecado.CANCELADO)) else it
        }
    }

    // El secado, si existe y sigue EN_CURSO.
    private fun enCurso(secadoId: Long): RegistroSecado {
        val registro = secados.firstOrNull { it.secado.id == secadoId }
        requireNotNull(registro) { "El secado $secadoId no existe." }
        require(registro.secado.estado == EstadoSecado.EN_CURSO) { "El secado $secadoId ya no esta en curso." }
        return registro
    }
}
