package com.canchapp.movil.auth.componentes

/** Forma básica de un correo, la misma que valida el backend. Si la cuenta existe lo decide el backend. */
private val FORMATO_CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Quita los espacios que deja el autocompletado y pasa a minúsculas, como hace el backend. */
fun normalizarCorreo(email: String): String = email.trim().lowercase()

fun esCorreoValido(email: String): Boolean = FORMATO_CORREO.matches(email)
