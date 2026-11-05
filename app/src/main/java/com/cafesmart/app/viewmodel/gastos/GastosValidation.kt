package com.cafesmart.app.viewmodel.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.*
// Aritmética entera de gran tamaño para sumas sin desbordamiento de Long.
import java.math.BigInteger
// Formato numérico, fechas, idioma y zona horaria compatibles con este proyecto.
import java.util.GregorianCalendar

// ============================================================
// 2. REGLAS DE VALIDACIÓN Y CONVERSIÓN
// ============================================================
/**
 * Reglas puras del registro, comprobables sin Android ni Compose.
 *
 * Estas funciones trabajan con datos y devuelven resultados sin dibujar componentes.
 * Separarlas permite probarlas con JUnit sin abrir Android ni mezclar reglas con colores o layouts.
 */
object GastosValidation {
    // ============================================================
    // 3. MONTO EN PESOS ENTEROS
    // ============================================================
    /**
     * Acepta dígitos en pesos enteros. Rechaza decimales, agrupadores y exponentes sin
     * redondear ni truncar. Long.MAX_VALUE es el límite técnico del modelo, no comercial.
     *
     * trim quita espacios exteriores. La expresión regular admite solo dígitos; toLongOrNull
     * rechaza valores que no caben en Long y takeIf exige un número positivo. El dominio guarda
     * pesos enteros, por eso no se aceptan centavos ni se redondea o trunca un decimal silenciosamente.
     *
     * @param texto Entrada original del usuario; se devuelve null cuando no es válida.
     */
    fun pesos(texto: String): Long? = texto.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toLongOrNull()?.takeIf { it > 0 }

    // ============================================================
    // 4. FECHA CIVIL VÁLIDA
    // ============================================================
    /**
     * Calendar estricto detecta días inexistentes; compatible con minSdk 24.
     *
     * isLenient=false impide normalizar fechas imposibles, como 31 de febrero. clear elimina
     * campos previos y timeInMillis obliga al calendario a comprobar la fecha. runCatching evita
     * que una fecha inválida cierre la pantalla. Aquí no se prohíben fechas futuras.
     *
     * @param fecha Día, mes y año que se comprueban con GregorianCalendar compatible con API 24.
     */
    fun fechaValida(fecha: Fecha): Boolean = fecha.anio in 1..9999 && runCatching {
        GregorianCalendar().apply {
            isLenient = false
            clear()
            set(fecha.anio, fecha.mes - 1, fecha.dia)
        }.timeInMillis
    }.isSuccess

    // ============================================================
    // 5. VALIDACIÓN COMPLETA DEL BORRADOR
    // ============================================================
    /**
     * Comprueba disponibilidad y suma exacta del reparto escrito por el usuario.
     *
     * isBlank detecta concepto vacío o compuesto solo por espacios. La descripción es opcional.
     * Los errores se devuelven por sección, para que la pantalla explique qué debe corregirse
     * sin borrar ni transformar el borrador.
     *
     * @param form Borrador de presentación aún no convertido al dominio.
     * @param disponibles Sublotes que pueden elegirse; se comprueban sus IDs antes de aceptar el reparto.
     */
    fun validar(form: GastoFormulario, disponibles: List<Sublote>): GastoErrores {
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
            form.asignaciones.values.any { pesos(it) == null } -> "Escribe pesos enteros mayores que cero para cada sublote."
            // La suma se compara en BigInteger solo después de validar cada asignación. El !! de
            // pesos(texto) es seguro en esta rama porque la condición anterior descartó los null.
            monto != null && form.asignaciones.values.fold(BigInteger.ZERO) { suma, texto ->
                suma + BigInteger.valueOf(pesos(texto)!!)
            } != BigInteger.valueOf(monto) -> "Las asignaciones deben sumar el monto del gasto."
            else -> null
        }
        // Cada condición produce un mensaje o null. El ViewModel decide después si puede abrir confirmación.
        return GastoErrores(
            concepto = if (form.concepto.isBlank()) "Escribe el concepto del gasto." else null,
            monto = if (monto == null) "Ingresa un monto válido en pesos enteros mayores que cero, sin puntos ni comas." else null,
            fecha = if (!fechaValida(form.fecha)) "Selecciona una fecha válida." else null,
            sublotes = repartoError
        )
    }

    // ============================================================
    // 6. TRANSFORMACIÓN A LOS MODELOS OFICIALES
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
