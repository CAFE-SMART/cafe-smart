package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.SecadoRepository
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Ayudas para las pruebas de Secado (no son pruebas).

// Hoy en el Figma de Secado: 3 de junio de 2026.
val HOY_FIGMA = Fecha(3, 6, 2026)

// Arma un sublote con su lote. Si no se dice, entro con lo mismo que tiene disponible (o 10 kg si esta agotado).
fun subloteDePrueba(
    id: Long,
    tipo: TipoCafe,
    calidad: Calidad,
    disponibleKg: Double,
    fecha: Fecha = HOY_FIGMA,
    inicialKg: Double = if (disponibleKg > 0) disponibleKg else 10.0
): SubloteConLote {
    val lote = Lote(id = tipo.ordinal * 10L + calidad.ordinal + 1, tipoCafe = tipo, calidad = calidad)
    val sublote = Sublote(
        id = id,
        loteId = lote.id,
        compraId = id,
        pesoInicialKg = inicialKg,
        pesoDisponibleKg = disponibleKg,
        costoKg = 10_000,
        fechaIngreso = fecha
    )
    return SubloteConLote(sublote, lote)
}

// El inventario del Figma "Secado - Paso 1": 4 sublotes verdes que suman 290 kg
// (3: buena 60 kg, 1: regular 120 kg, 2: regular 80 kg, 4: mala 30 kg), mas un sublote seco que NO se puede secar.
fun inventarioFigma(): List<SubloteConLote> = listOf(
    subloteDePrueba(1, TipoCafe.VERDE, Calidad.REGULAR, 120.0, fecha = Fecha(3, 6, 2026)),
    subloteDePrueba(2, TipoCafe.VERDE, Calidad.REGULAR, 80.0, fecha = Fecha(1, 6, 2026)),
    subloteDePrueba(3, TipoCafe.VERDE, Calidad.BUENA, 60.0, fecha = Fecha(31, 5, 2026)),
    subloteDePrueba(4, TipoCafe.VERDE, Calidad.MALA, 30.0, fecha = Fecha(29, 5, 2026)),
    subloteDePrueba(5, TipoCafe.SECO, Calidad.BUENA, 200.0)
)

fun repositorioDePrueba(inventario: List<SubloteConLote> = inventarioFigma()) = SecadoRepositoryEjemplo(inventario)

// Los ViewModels usan viewModelScope (Dispatchers.Main), que en JUnit no existe. Esta regla pone un dispatcher de prueba
// "sin cola" (Unconfined): lo que lanza viewModelScope.launch corre enseguida, asi los tests se leen igual que antes.
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

// Como assertThrows, pero para llamadas suspend al repositorio. Devuelve la excepcion para revisar su mensaje.
suspend fun assertFalla(bloque: suspend () -> Unit): IllegalArgumentException {
    try {
        bloque()
    } catch (e: IllegalArgumentException) {
        return e
    }
    throw AssertionError("Se esperaba IllegalArgumentException y no fallo.")
}

// Repositorio que no entrega inventario ni secados en curso hasta que se abre la puerta: simula a Room tardando.
// Sirve para probar el estado "cargando" de los ViewModels.
class RepositorioConPuerta(
    private val real: SecadoRepository,
    val puerta: CompletableDeferred<Unit> = CompletableDeferred()
) : SecadoRepository by real {
    override suspend fun inventario() = puerta.await().let { real.inventario() }
    override suspend fun secadosEnCurso() = puerta.await().let { real.secadosEnCurso() }
}
