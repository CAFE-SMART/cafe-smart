package com.cafesmart.app.data.secado

import android.content.Context
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.LoteDao
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SecadoConEnviosFila
import com.cafesmart.app.data.local.SecadoDao
import com.cafesmart.app.data.local.SecadoSubloteEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.local.Transaccion
import com.cafesmart.app.data.local.TransaccionRoom
import com.cafesmart.app.data.gastos.aModelo
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
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
// Todas las funciones son suspend (como las de Room): los ViewModels las llaman dentro de viewModelScope.launch.
// Hay dos versiones: la de ejemplo (en memoria, para previews y pruebas) y RoomSecadoRepository (la real, sobre
// Room). Las dos cumplen esta misma interfaz, asi que los ViewModels no cambian.
interface SecadoRepository {
    // Sublotes con cafe disponible, de todos los tipos. La pantalla se queda solo con el verde.
    suspend fun inventario(): List<SubloteConLote>

    // Inicia un secado: le resta a cada sublote lo que se envia y devuelve el secado EN_CURSO.
    // Lanza IllegalArgumentException si el envio no es valido (sin sublotes, repetido, no es verde o pasa de lo disponible).
    suspend fun iniciarSecado(fechaInicio: Fecha, envios: List<EnvioASecar>): Secado

    // Secados EN_CURSO, el de inicio mas reciente primero. Sus resultantes van vacios (todavia no hay cafe seco).
    suspend fun secadosEnCurso(): List<SecadoConDetalle>

    // Finaliza un secado: crea un sublote seco por cada calidad con kg > 0 y marca el secado FINALIZADO.
    // Valida todo antes de cambiar algo. Lanza IllegalArgumentException si el secado no existe o no esta EN_CURSO,
    // si la fecha de fin es anterior al inicio, si los kilos secos no son validos o si el factor (opcional; null = sin factor) no esta entre FACTOR_RENDIMIENTO_MIN y FACTOR_RENDIMIENTO_MAX.
    suspend fun finalizarSecado(
        secadoId: Long,
        fechaFin: Fecha,
        kilosSecos: Map<Calidad, Double>,
        factorRendimiento: Int?
    ): SecadoConDetalle

    // Cancela un secado EN_CURSO y devuelve al inventario el cafe que se habia enviado.
    // Lanza IllegalArgumentException si el secado no existe o no esta EN_CURSO.
    suspend fun cancelarSecado(secadoId: Long)

    companion object {
        /** Versión real, sobre Room. */
        fun de(context: Context): SecadoRepository {
            val base = AppDatabase.obtener(context)
            return RoomSecadoRepository(
                secadoDao = base.secadoDao(),
                subloteDao = base.subloteDao(),
                loteDao = base.loteDao(),
                transaccion = TransaccionRoom(base),
            )
        }
    }
}

// Versión de Room. Cada función que escribe corre en una sola transacción: si algo falla (lanza una excepción)
// no queda nada a medias. Los errores de reglas son IllegalArgumentException, igual que en la versión de ejemplo,
// que es lo único que capturan los ViewModels.
class RoomSecadoRepository(
    private val secadoDao: SecadoDao,
    private val subloteDao: SubloteDao,
    private val loteDao: LoteDao,
    private val transaccion: Transaccion,
) : SecadoRepository {

    override suspend fun inventario(): List<SubloteConLote> {
        val lotes = loteDao.listar().associate { it.id to it.aModelo() }
        return subloteDao.listar()
            .filter { it.pesoDisponibleKg > 0.0 }
            .mapNotNull { sublote -> lotes[sublote.loteId]?.let { SubloteConLote(sublote.aModelo(), it) } }
    }

    override suspend fun iniciarSecado(fechaInicio: Fecha, envios: List<EnvioASecar>): Secado {
        require(envios.isNotEmpty()) { "Elige al menos un sublote para secar." }
        require(envios.map { it.subloteId }.toSet().size == envios.size) { "Un sublote no puede repetirse en el secado." }
        return transaccion.ejecutar {
            envios.forEach { envio ->
                val sublote = subloteDao.buscarPorId(envio.subloteId)
                requireNotNull(sublote) { "El sublote ${envio.subloteId} no existe." }
                require(loteDao.buscarPorId(sublote.loteId)?.tipoCafe == TipoCafe.VERDE.name) { "Solo se seca cafe verde." }
                require(cantidadValida(envio.pesoKg, sublote.pesoDisponibleKg)) {
                    "La cantidad del sublote ${envio.subloteId} debe ser mayor a 0 y no pasar de lo disponible."
                }
            }
            val secado = Secado(fechaInicio = fechaInicio)
            // El secadoId 0 se reemplaza por el id nuevo dentro de guardarConEnvios.
            val id = secadoDao.guardarConEnvios(
                secado.aEntidad(),
                envios.map { SecadoSubloteEntity(secadoId = 0, subloteId = it.subloteId, pesoEnviadoKg = it.pesoKg) },
            )
            envios.forEach { envio ->
                // 0 filas cambiadas = ya no alcanzaba el cafe: se lanza para que Room deshaga todo el secado.
                require(subloteDao.descontar(envio.subloteId, envio.pesoKg) > 0) {
                    "La cantidad del sublote ${envio.subloteId} debe ser mayor a 0 y no pasar de lo disponible."
                }
            }
            secado.copy(id = id)
        }
    }

    override suspend fun secadosEnCurso(): List<SecadoConDetalle> =
        secadoDao.listarEnCurso().map { fila -> SecadoConDetalle(fila.secado.aModelo(), enviosDe(fila), emptyList()) }

    override suspend fun finalizarSecado(
        secadoId: Long,
        fechaFin: Fecha,
        kilosSecos: Map<Calidad, Double>,
        factorRendimiento: Int?
    ): SecadoConDetalle = transaccion.ejecutar {
        // Primero se valida todo; mientras no pase, no se escribe nada.
        val fila = enCurso(secadoId)
        val secado = fila.secado.aModelo()
        require(fechaFin.clave() >= secado.fechaInicio.clave()) {
            "La fecha de finalizacion no puede ser anterior al inicio."
        }
        val enviados = enviosDe(fila)
        val enviadoKg = redondearKg(enviados.sumOf { it.envio.pesoEnviadoKg })
        errorKilosSecos(kilosSecos, enviadoKg)?.let { throw IllegalArgumentException(it) }
        factorRendimiento?.let { errorFactorEntero(it) }?.let { throw IllegalArgumentException(it) }

        // Cierra primero: si otro toque ya lo finalizo o cancelo, no cambia ninguna fila y no se crea nada.
        require(secadoDao.cerrar(secadoId, EstadoSecado.FINALIZADO.name, fechaAMilis(fechaFin), factorRendimiento) > 0) {
            "El secado $secadoId ya no esta en curso."
        }
        val costoKg = costoKgSeco(enviados, kilosSecosTotal(kilosSecos))
        val resultantes = Calidad.entries.mapNotNull { calidad ->
            val kg = redondearKg(kilosSecos[calidad] ?: 0.0)
            if (kg <= 0.0) return@mapNotNull null
            // Un solo lote por (tipo, calidad): se usa el seco que ya exista; si no, se crea.
            val loteId = loteDao.buscarPorTipoYCalidad(TipoCafe.SECO.name, calidad.name)?.id
                ?: loteDao.insertar(LoteEntity(tipoCafe = TipoCafe.SECO.name, calidad = calidad.name))
            val entidad = SubloteEntity(
                loteId = loteId,
                secadoId = secadoId,
                pesoInicialKg = kg,
                pesoDisponibleKg = kg,
                costoKg = costoKg,
                fechaIngreso = fechaAMilis(fechaFin),
                factorRendimiento = factorRendimiento,
            )
            entidad.copy(id = subloteDao.insertar(entidad)).aModelo()
        }
        val finalizado = secado.copy(fechaFin = fechaFin, estado = EstadoSecado.FINALIZADO, factorRendimiento = factorRendimiento)
        SecadoConDetalle(finalizado, enviados, resultantes)
    }

    override suspend fun cancelarSecado(secadoId: Long) {
        transaccion.ejecutar {
            val fila = enCurso(secadoId)
            require(secadoDao.cerrar(secadoId, EstadoSecado.CANCELADO.name, null, null) > 0) {
                "El secado $secadoId ya no esta en curso."
            }
            fila.envios.forEach { envio ->
                // 0 filas cambiadas = el sublote ya no existe: se lanza para que Room deshaga la cancelacion.
                require(subloteDao.devolver(envio.subloteId, envio.pesoEnviadoKg) > 0) {
                    "El sublote ${envio.subloteId} ya no esta en el inventario; no se puede cancelar el secado $secadoId."
                }
            }
        }
    }

    // El secado con sus envios, si existe y sigue EN_CURSO.
    private suspend fun enCurso(secadoId: Long): SecadoConEnviosFila {
        val fila = secadoDao.buscarConEnvios(secadoId)
        requireNotNull(fila) { "El secado $secadoId no existe." }
        require(fila.secado.estado == EstadoSecado.EN_CURSO.name) { "El secado $secadoId ya no esta en curso." }
        return fila
    }

    // Los envios con el sublote actual (de ahi sale el costo por kg), en el orden de los sublotes.
    private suspend fun enviosDe(fila: SecadoConEnviosFila): List<EnvioSecado> =
        fila.envios.sortedBy { it.subloteId }.map { envio ->
            val sublote = checkNotNull(subloteDao.buscarPorId(envio.subloteId)) { "El sublote ${envio.subloteId} no existe." }
            EnvioSecado(envio.aModelo(), sublote.aModelo())
        }
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

    override suspend fun inventario(): List<SubloteConLote> = inventario

    override suspend fun iniciarSecado(fechaInicio: Fecha, envios: List<EnvioASecar>): Secado {
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

    override suspend fun secadosEnCurso(): List<SecadoConDetalle> =
        secados
            .filter { it.secado.estado == EstadoSecado.EN_CURSO }
            .sortedWith(compareByDescending<RegistroSecado> { it.secado.fechaInicio.clave() }.thenByDescending { it.secado.id })
            .map { it.conDetalle() }

    override suspend fun finalizarSecado(
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

    override suspend fun cancelarSecado(secadoId: Long) {
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
