package com.cafesmart.app.viewmodel.gastos

import com.cafesmart.app.data.model.*
import java.math.BigInteger
import java.util.GregorianCalendar

/** Reglas puras del registro, comprobables sin Android ni Compose. */
object GastosValidation {
    /**
     * Acepta dígitos en pesos enteros. Rechaza decimales, agrupadores y exponentes sin
     * redondear ni truncar. Long.MAX_VALUE es el límite técnico del modelo, no comercial.
     */
    fun pesos(texto: String): Long? = texto.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toLongOrNull()?.takeIf { it > 0 }

    /** Calendar estricto detecta días inexistentes; compatible con minSdk 24. */
    fun fechaValida(fecha: Fecha): Boolean = fecha.anio in 1..9999 && runCatching {
        GregorianCalendar().apply {
            isLenient = false
            clear()
            set(fecha.anio, fecha.mes - 1, fecha.dia)
        }.timeInMillis
    }.isSuccess

    /** Comprueba disponibilidad y suma exacta del reparto escrito por el usuario. */
    fun validar(form: GastoFormulario, disponibles: List<Sublote>): GastoErrores {
        val monto = pesos(form.monto)
        val disponiblesIds = disponibles.map { it.id }.toSet()
        val repartoError = when {
            form.aplicaA == AplicaGasto.SECADO -> "Los gastos de secado se registran desde su propio flujo."
            form.aplicaA != AplicaGasto.SUBLOTES -> null
            form.asignaciones.isEmpty() -> "Selecciona al menos un sublote."
            form.asignaciones.keys.any { it !in disponiblesIds } -> "Un sublote seleccionado ya no está disponible."
            form.asignaciones.values.any { pesos(it) == null } -> "Escribe pesos enteros mayores que cero para cada sublote."
            monto != null && form.asignaciones.values.fold(BigInteger.ZERO) { suma, texto ->
                suma + BigInteger.valueOf(pesos(texto)!!)
            } != BigInteger.valueOf(monto) -> "Las asignaciones deben sumar el monto del gasto."
            else -> null
        }
        return GastoErrores(
            concepto = if (form.concepto.isBlank()) "Escribe el concepto del gasto." else null,
            monto = if (monto == null) "Ingresa un monto válido en pesos enteros mayores que cero, sin puntos ni comas." else null,
            fecha = if (!fechaValida(form.fecha)) "Selecciona una fecha válida." else null,
            sublotes = repartoError
        )
    }

    /** Conversión explícita al dominio; solo se llama después de validar todo el borrador. */
    fun convertir(form: GastoFormulario, id: Long, disponibles: List<Sublote>): Pair<Gasto, List<GastoSublote>> {
        require(!validar(form, disponibles).hayErrores)
        val gasto = Gasto(id, form.concepto.trim(), form.descripcion.trim(), form.tipo,
            pesos(form.monto)!!, form.fecha, form.estado, form.aplicaA)
        val reparto = if (form.aplicaA == AplicaGasto.SUBLOTES) form.asignaciones.map { (sublote, valor) ->
            GastoSublote(id, sublote, pesos(valor)!!)
        } else emptyList()
        require(gasto.repartoEsValido(reparto))
        return gasto to reparto
    }
}
