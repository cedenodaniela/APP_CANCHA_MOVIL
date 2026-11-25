package com.canchapp.movil.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta institucional (app_cancha/AGENTS.md §6). Es la misma del panel web,
// para que un estado se vea igual en los dos clientes. Cada color cumple
// contraste 4.5:1 con texto blanco (RNF-05).

/** Verde: color principal y estado «Pagada». */
val Verde = Color(0xFF0F6B3E)

/** Azul: estado «En verificación». */
val Azul = Color(0xFF1D5FA8)

/** Naranja: estados pendientes («Pendiente de comprobante», «Confirmada»). */
val Naranja = Color(0xFFB4541A)

/** Rojo: errores y estados «Rechazada», «Expirada» y «No asistió». */
val Rojo = Color(0xFFB42318)

/** Fondo de la app. */
val Fondo = Color(0xFFF4F6F1)

val Superficie = Color(0xFFFFFFFF)
val Borde = Color(0xFFD5DCCF)
val Texto = Color(0xFF1C2521)
val TextoSuave = Color(0xFF4D5A52)

/** Fondos suaves de los avisos (con icono y texto, nunca solo color). */
val FondoError = Color(0xFFFDECEA)
val FondoExito = Color(0xFFE7F3EC)
