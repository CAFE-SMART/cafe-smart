package com.cafesmart.app.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.cafesmart.app.data.local.AppDatabase
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.ProductorEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.model.aPesos
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Datos de ejemplo para demos y pruebas. SOLO EXISTE EN DEBUG: este archivo está en app/src/debug.
// En release hay otro archivo con el mismo nombre (app/src/release) cuya función no hace nada,
// así que la app publicada ni siquiera trae estos datos.
//
// Las cifras reproducen las de data/sample/InventarioEjemplo.kt (Figma "Inventario · Resumen (Todos)"):
//   Verde 290 kg (4 sublotes) · Seco 521 kg (4) · Trillado 23 kg (1) · Pasilla sin sublotes · total 834 kg.
// Hay un sublote agotado (no cuenta) y una compra con dos cafés (Compra 1 → N Sublote).
// No crea el Negocio: ese se crea al registrarse. Para el "de 3.000 kg" del Figma, poner capacidad 3000 en Ajustes.

private const val ARCHIVO_PREFS = "cafesmart_semilla_debug"
private const val CLAVE_YA_INTENTADA = "semilla_ya_intentada"
private const val TAG = "SemillaDeEjemplo"

internal data class ProductorSemilla(
    val nombre: String,
    val documento: String,
    val tipoDocumento: TipoDocumento,
    val telefono: String = "",
)

internal data class CafeSemilla(
    val tipo: TipoCafe,
    val calidad: Calidad,
    val pesoInicialKg: Double,
    val pesoDisponibleKg: Double,
    val costoKg: Long,
)

// productor = posición en PRODUCTORES_SEMILLA; null = compra "Sin registrar".
internal data class CompraSemilla(
    val productor: Int?,
    val fecha: Fecha,
    val cafes: List<CafeSemilla>,
)

// Nombres y documentos inventados: no son de personas reales.
internal val PRODUCTORES_SEMILLA = listOf(
    ProductorSemilla("María Fernanda Ospina", "1112345601", TipoDocumento.CC, "3001234501"),
    ProductorSemilla("Hernán Darío Cardona", "16234502", TipoDocumento.CC, "3102345602"),
    ProductorSemilla("Finca El Mirador SAS", "900456789", TipoDocumento.NIT, "3203456703"),
    ProductorSemilla("Luz Marina Valencia", "66789012", TipoDocumento.CC), // sin teléfono (es opcional)
)

// De la más antigua a la más nueva. Los seco también entran como compra (el formulario permite
// comprar café seco) para no depender de la tabla de Secado, que todavía no existe.
internal val COMPRAS_SEMILLA = listOf(
    CompraSemilla(3, Fecha(5, 9, 2026), listOf(
        CafeSemilla(TipoCafe.SECO, Calidad.BUENA, 90.0, 0.0, 20_000), // agotado
    )),
    CompraSemilla(2, Fecha(15, 10, 2026), listOf(
        CafeSemilla(TipoCafe.TRILLADO, Calidad.BUENA, 23.0, 23.0, 30_000),
    )),
    CompraSemilla(null, Fecha(18, 10, 2026), listOf(
        CafeSemilla(TipoCafe.SECO, Calidad.MALA, 80.0, 71.0, 15_000),
    )),
    CompraSemilla(3, Fecha(20, 10, 2026), listOf(
        CafeSemilla(TipoCafe.SECO, Calidad.BUENA, 250.0, 200.0, 21_000),
    )),
    CompraSemilla(1, Fecha(22, 10, 2026), listOf(
        CafeSemilla(TipoCafe.SECO, Calidad.REGULAR, 150.0, 150.0, 19_000),
    )),
    CompraSemilla(0, Fecha(25, 10, 2026), listOf(
        CafeSemilla(TipoCafe.VERDE, Calidad.MALA, 30.0, 30.0, 9_000),
    )),
    CompraSemilla(2, Fecha(28, 10, 2026), listOf( // una compra con dos cafés
        CafeSemilla(TipoCafe.VERDE, Calidad.REGULAR, 100.0, 80.0, 13_500),
        CafeSemilla(TipoCafe.SECO, Calidad.BUENA, 100.0, 100.0, 21_000),
    )),
    CompraSemilla(1, Fecha(1, 11, 2026), listOf(
        CafeSemilla(TipoCafe.VERDE, Calidad.REGULAR, 120.0, 120.0, 14_000),
    )),
    CompraSemilla(0, Fecha(3, 11, 2026), listOf(
        CafeSemilla(TipoCafe.VERDE, Calidad.BUENA, 60.0, 60.0, 16_000),
    )),
)

private val candado = Mutex()

/**
 * Carga los datos de ejemplo la primera vez que se abre la app (instalación limpia).
 * Una sola vez: queda marcado aunque la base tuviera datos, para no volver a sembrar si luego se vacía.
 */
suspend fun cargarSemillaSiEsPrimeraVez(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences(ARCHIVO_PREFS, Context.MODE_PRIVATE)
    // El candado evita sembrar dos veces si la Activity se recrea (por ejemplo, al rotar) mientras carga.
    candado.withLock {
        if (prefs.getBoolean(CLAVE_YA_INTENTADA, false)) return
        try {
            val sembro = sembrarDatosDeEjemplo(AppDatabase.obtener(context))
            Log.i(TAG, if (sembro) "Datos de ejemplo cargados" else "La base ya tenía datos: no se cargó nada")
            prefs.edit().putBoolean(CLAVE_YA_INTENTADA, true).apply()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si falla no se marca, así se reintenta en la próxima apertura. No debe tumbar la app de demo.
            Log.e(TAG, "No se pudieron cargar los datos de ejemplo", e)
        }
    }
}

/**
 * Inserta productores, compras, lotes y sublotes en una sola transacción.
 * Solo siembra si la base no tiene productores ni sublotes (nunca pisa datos reales).
 * Devuelve true si sembró.
 */
suspend fun sembrarDatosDeEjemplo(db: AppDatabase): Boolean {
    if (db.productorDao().listar().isNotEmpty() || db.subloteDao().listar().isNotEmpty()) return false

    db.withTransaction {
        val idsProductor = PRODUCTORES_SEMILLA.map {
            db.productorDao().insertar(
                ProductorEntity(
                    nombre = it.nombre,
                    documento = it.documento,
                    tipoDocumento = it.tipoDocumento.name,
                    telefono = it.telefono,
                )
            )
        }
        // Un lote por (tipo, calidad), como pide el índice único; solo se crean los que tienen sublotes.
        val idsLote = mutableMapOf<Pair<TipoCafe, Calidad>, Long>()

        for (compra in COMPRAS_SEMILLA) {
            val millis = fechaAMilis(compra.fecha)
            val compraId = db.compraDao().insertar(
                CompraEntity(
                    productorId = compra.productor?.let { idsProductor[it] },
                    fecha = millis,
                    // Misma cuenta del paso 3 de Compras: suma de peso x precio de cada café.
                    totalPagado = compra.cafes.sumOf { it.pesoInicialKg * it.costoKg }.aPesos(),
                )
            )
            for (cafe in compra.cafes) {
                val loteId = idsLote.getOrPut(cafe.tipo to cafe.calidad) {
                    db.loteDao().insertar(LoteEntity(tipoCafe = cafe.tipo.name, calidad = cafe.calidad.name))
                }
                db.subloteDao().insertar(
                    SubloteEntity(
                        loteId = loteId,
                        compraId = compraId,
                        pesoInicialKg = cafe.pesoInicialKg,
                        pesoDisponibleKg = cafe.pesoDisponibleKg,
                        costoKg = cafe.costoKg,
                        fechaIngreso = millis,
                    )
                )
            }
        }
    }
    return true
}
