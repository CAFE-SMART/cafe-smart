package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.negocio.negocioDesdeRegistro
import com.cafesmart.app.ui.auth.AuthValidation
import com.cafesmart.app.ui.auth.BusinessType

// Reglas de "Información de empresa" (Ajustes). Usa las mismas validaciones del registro
// (AuthValidation) para que un negocio editado cumpla lo mismo que uno recién creado.

/** Formulario de la pantalla. tipo null = todavía no se eligió. */
data class NegocioFormulario(
    val nombre: String = "",
    val tipo: BusinessType? = null,
    val telefono: String = "",
)

/**
 * Errores del formulario por campo ("businessName", "businessType", "phone"), con los mismos
 * mensajes del registro. Vacío = se puede guardar.
 */
fun validarNegocio(form: NegocioFormulario): Map<String, String> =
    AuthValidation.stepOne(form.nombre, form.tipo) + buildMap {
        if (!AuthValidation.phoneRegex.matches(form.telefono.trim())) {
            put("phone", "Ingresa un celular de 10 dígitos que empiece por 3.")
        }
    }

/** Llena el formulario con el negocio guardado; sin negocio, el formulario queda vacío. */
fun formularioDesde(negocio: NegocioEntity?): NegocioFormulario =
    if (negocio == null) NegocioFormulario()
    else NegocioFormulario(
        nombre = negocio.nombre,
        tipo = BusinessType.entries.firstOrNull { it.name == negocio.tipo },
        telefono = negocio.telefono,
    )

/**
 * Negocio que se guarda. Si ya existía, conserva lo que esta pantalla no edita (capacidad de bodega,
 * peso por bulto y fecha de creación); si no, se crea como en el registro. Requiere el tipo elegido
 * (validarNegocio ya lo exige).
 */
fun negocioAGuardar(uid: String, form: NegocioFormulario, actual: NegocioEntity?, ahora: Long): NegocioEntity {
    val tipo = requireNotNull(form.tipo) { "Falta el tipo de negocio" }.name
    return actual?.copy(nombre = form.nombre.trim(), tipo = tipo, telefono = form.telefono.trim())
        ?: negocioDesdeRegistro(uid, form.nombre, tipo, form.telefono, ahora)
}
