package com.cafesmart.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Gasto y su reparto entre sublotes. Versión de Room de data/model/Gasto.kt (las data classes siguen
// validando las reglas; aquí solo se describen las tablas y sus llaves foráneas).

/**
 * - tipo, estadoPago y aplicaA: el nombre del enum como texto (TipoGasto, EstadoPago, AplicaGasto).
 * - monto: pesos enteros. fecha: milisegundos UTC (fechaAMilis / milisAFecha).
 * - secadoId: el secado al que pertenece el gasto (null si no es de secado). Con RESTRICT no se puede
 *   borrar un secado mientras tenga gastos (igual que sublote.secado_id).
 */
@Entity(
    tableName = "gasto",
    foreignKeys = [
        ForeignKey(
            entity = SecadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["secado_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["secado_id"])],
)
data class GastoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val descripcion: String = "",
    val tipo: String,
    val monto: Long,
    val fecha: Long,
    @ColumnInfo(name = "estado_pago") val estadoPago: String,
    @ColumnInfo(name = "aplica_a") val aplicaA: String,
    @ColumnInfo(name = "secado_id") val secadoId: Long? = null,
)

/**
 * Tabla intermedia Gasto <-> Sublote. La llave primaria es (gasto_id, sublote_id): un sublote aparece
 * una sola vez en el reparto de un gasto. montoAsignado es la parte del gasto que le toca a ese sublote.
 * Con RESTRICT no se puede borrar un gasto ni un sublote mientras tengan reparto, así no se pierde el historial.
 * El índice de sublote_id permite listar rápido los gastos de un sublote ("Gastos del sublote").
 */
@Entity(
    tableName = "gasto_sublote",
    primaryKeys = ["gasto_id", "sublote_id"],
    foreignKeys = [
        ForeignKey(
            entity = GastoEntity::class,
            parentColumns = ["id"],
            childColumns = ["gasto_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SubloteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sublote_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["sublote_id"])],
)
data class GastoSubloteEntity(
    @ColumnInfo(name = "gasto_id") val gastoId: Long,
    @ColumnInfo(name = "sublote_id") val subloteId: Long,
    @ColumnInfo(name = "monto_asignado") val montoAsignado: Long,
)
