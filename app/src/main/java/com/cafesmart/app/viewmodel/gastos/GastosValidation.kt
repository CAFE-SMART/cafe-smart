package com.cafesmart.app.viewmodel.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.*
// Reglas oficiales de un gasto (concepto, valor, fecha, descripción): no se repiten aquí.
import com.cafesmart.app.viewmodel.finance.LARGO_CONCEPTO_GASTO_MAXIMO
import com.cafesmart.app.viewmodel.finance.LARGO_DESCRIPCION_GASTO_MAXIMO
import com.cafesmart.app.viewmodel.finance.LimitesGasto
import com.cafesmart.app.viewmodel.limites.LimitesVigentes
import com.cafesmart.app.viewmodel.finance.validarGasto
// Aritmética entera de gran tamaño para sumas sin desbordamiento de Long.
import java.math.BigInteger

// ============================================================
// 2. REGLAS DE VALIDACIÓN Y CONVERSIÓN
// ============================================================
/**
 * Reglas puras del registro, comprobables sin Android ni Compose.
 *
 * Los campos del gasto (concepto, valor, fecha y descripción) los valida [validarGasto] de
 * ExpenseRules, con sus mensajes y sus límites; aquí solo queda lo propio de este flujo:
 * los topes de escritura y que el reparto entre sublotes sume el monto del gasto.
 */
object GastosValidation {
    // Topes de escritura: salen de ExpenseRules para que escribir y validar usen el mismo número.
    const val CONCEPTO_MAX_CARACTERES = LARGO_CONCEPTO_GASTO_MAXIMO
    const val DESCRIPCION_MAX_CARACTERES = LARGO_DESCRIPCION_GASTO_MAXIMO

    /** Dígitos que caben en el valor máximo vigente (50.000.000 son 8). */
    val DIGITOS_MAX_VALOR: Int get() = LimitesVigentes.actuales.gastoMaximoCop.toString().length

    fun limitarConcepto(texto: String): String = recortar(texto, CONCEPTO_MAX_CARACTERES)

    fun limitarDescripcion(texto: String): String = recortar(texto, DESCRIPCION_MAX_CARACTERES)

    /** Recorta pegado o escritura sin dejar medio par sustituto UTF-16 (un emoji) al final. */
    private fun recortar(texto: String, maximo: Int): String {
        if (texto.length <= maximo) return texto
        val recorte = texto.take(maximo)
        return if (recorte.last().isHighSurrogate()) recorte.dropLast(1) else recorte
    }

    /** Solo dígitos y hasta [DIGITOS_MAX_VALOR], como el precio de Compras: "12abc" queda en "12". */
    fun limpiarValor(texto: String): String = texto.filter { it in '0'..'9' }.take(DIGITOS_MAX_VALOR)

    /** Aplica todos los topes de escritura al borrador: el estado nunca guarda texto que no cabe. */
    fun normalizar(form: GastoFormulario): GastoFormulario = form.copy(
        concepto = limitarConcepto(form.concepto),
        descripcion = limitarDescripcion(form.descripcion),
        monto = limpiarValor(form.monto),
        asignaciones = form.asignaciones.mapValues { limpiarValor(it.value) }
    )

    // ============================================================
    // 3. MONTO EN PESOS ENTEROS
    // ============================================================
    /**
     * Acepta dígitos en pesos enteros. Rechaza decimales, agrupadores y exponentes sin
     * redondear ni truncar. Solo interpreta el texto: el mínimo y el máximo del gasto los
     * juzga [validarGasto]; Long.MAX_VALUE es apenas el límite técnico del tipo.
     *
     * trim quita espacios exteriores. La expresión regular admite solo dígitos; toLongOrNull
     * rechaza valores que no caben en Long y takeIf exige un número positivo. El dominio guarda
     * pesos enteros, por eso no se aceptan centavos ni se redondea o trunca un decimal silenciosamente.
     *
     * @param texto Entrada original del usuario; se devuelve null cuando no es válida.
     */
    fun pesos(texto: String): Long? = texto.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toLongOrNull()?.takeIf { it > 0 }

    /** Valor que se le entrega a ExpenseRules: null si está vacío o no son solo dígitos (incluye el 0). */
    private fun valorEscrito(texto: String): Long? =
        texto.trim().takeIf { t -> t.isNotEmpty() && t.all { it in '0'..'9' } }?.toLongOrNull()

    // ============================================================
    // 4. VALIDACIÓN COMPLETA DEL BORRADOR
    // ============================================================
    /**
     * Valida los campos con ExpenseRules y comprueba disponibilidad y suma exacta del reparto.
     *
     * Los mensajes de concepto, valor, fecha y descripción son los de [validarGasto], sin
     * reescribirlos. El tipo no se revisa aquí: el formulario siempre trae uno de los oficiales.
     * Los errores se devuelven por sección, para que la pantalla explique qué debe corregirse.
     *
     * @param form Borrador de presentación aún no convertido al dominio.
     * @param disponibles Sublotes que pueden elegirse; se comprueban sus IDs antes de aceptar el reparto.
     * @param hoy Día de referencia para la fecha futura; por defecto el de Colombia, como el formulario.
     * @param limites Mínimo, máximo y largos del gasto; por defecto los de ExpenseRules (Ajustes los podrá cambiar).
     */
    fun validar(
        form: GastoFormulario,
        disponibles: List<Sublote>,
        hoy: Fecha = hoyEnColombia(),
        limites: LimitesGasto = LimitesGasto()
    ): GastoErrores {
        val campos = validarGasto(
            concepto = form.concepto, tipo = form.tipo, valor = valorEscrito(form.monto),
            fecha = form.fecha, descripcion = form.descripcion, hoy = hoy, limites = limites
        )
        val monto = pesos(form.monto)
        // toSet permite comprobar si cada ID seleccionado sigue en las opciones recibidas.
        val disponiblesIds = disponibles.map { it.id }.toSet()
        // GENERAL no exige sublotes; SUBLOTES exige selección, IDs disponibles y montos positivos.
        // SECADO se remite a su flujo propio. No se calcula un reparto automático.
        val repartoError = when {
            form.aplicaA == AplicaGasto.SECADO -> "Los gastos de secado se registran desde su propio flujo."
            form.aplicaA != AplicaGasto.SUBLOTES -> null
            form.asignaciones.isEmpty() -> "Selecciona al menos un sublote."
            form.asignaciones.keys.any { it !in disponiblesIds } -> "Un sublote seleccionado ya no está disponible."
            form.asignaciones.values.any { pesos(it) == null } -> "Escribe un valor mayor que cero para cada sublote."
            // La suma se compara en BigInteger solo después de validar cada asignación. El !! de
            // pesos(texto) es seguro en esta rama porque la condición anterior descartó los null.
            monto != null && form.asignaciones.values.fold(BigInteger.ZERO) { suma, texto ->
                suma + BigInteger.valueOf(pesos(texto)!!)
            } != BigInteger.valueOf(monto) -> "Las asignaciones deben sumar el monto del gasto."
            else -> null
        }
        // Cada condición produce un mensaje o null. El ViewModel decide después si puede abrir confirmación.
        return GastoErrores(
            concepto = campos.concepto, monto = campos.valor, fecha = campos.fecha,
            sublotes = repartoError, descripcion = campos.descripcion
        )
    }

    // ============================================================
    // 5. TRANSFORMACIÓN A LOS MODELOS OFICIALES
    // ============================================================
    /**
     * Conversión explícita al dominio; solo se llama después de validar todo el borrador.
     *
     * require exige que la validación completa haya pasado. Se recortan los textos y se crean
     * Gasto y GastoSublote con pesos exactos. El operador to forma el Pair que recibe el ViewModel;
     * repartoEsValido vuelve a comprobar la regla del dominio antes de entregar el resultado.
     *
     * @param form Borrador que debe ser válido.
     * @param id Identificador local elegido por el ViewModel para este registro.
     * @param disponibles Opciones contra las que se valida la asociación.
     */
    fun convertir(form: GastoFormulario, id: Long, disponibles: List<Sublote>): Pair<Gasto, List<GastoSublote>> {
        require(!validar(form, disponibles).hayErrores)
        val gasto = Gasto(id, form.concepto.trim(), form.descripcion.trim(), form.tipo,
            pesos(form.monto)!!, form.fecha, form.estado, form.aplicaA)
        // Para un gasto general se devuelve una lista vacía; para sublotes, una relación oficial por asignación.
        val reparto = if (form.aplicaA == AplicaGasto.SUBLOTES) form.asignaciones.map { (sublote, valor) ->
            GastoSublote(id, sublote, pesos(valor)!!)
        } else emptyList()
        require(gasto.repartoEsValido(reparto))
        return gasto to reparto
    }
}
