package com.canchapp.movil.ui.componentes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Menú del administrador y confirmación al salir, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class MenuYSalidaTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    private fun mostrarMenu(alIrA: (SeccionAdministrador) -> Unit = {}, alSalir: () -> Unit = {}) {
        reglaCompose.setContent {
            CanchAppTheme {
                MenuAdministrador(SeccionAdministrador.AGENDA, "Daniela", alIrA, alSalir) { abrirMenu ->
                    PantallaPendiente(titulo = "Agenda", requerimiento = "RF-15", alAbrirMenu = abrirMenu)
                }
            }
        }
        reglaCompose.onNodeWithContentDescription("Abrir menú").performClick()
    }

    @Test
    fun dadoElMenu_cuandoSeAbre_entoncesMuestraLasSeccionesYMarcaLaActual() {
        mostrarMenu()

        reglaCompose.onNodeWithText("Daniela").assertIsDisplayed()
        reglaCompose.onNodeWithTag(EtiquetasMenu.seccion(SeccionAdministrador.AGENDA)).assertIsSelected()
        reglaCompose.onNodeWithText("Canchas").assertIsDisplayed()
        reglaCompose.onNodeWithText("Ajustes del complejo").assertIsDisplayed()
    }

    @Test
    fun dadoElMenu_cuandoEligeCanchas_entoncesNavegaAEsaSeccion() {
        var elegida: SeccionAdministrador? = null
        mostrarMenu(alIrA = { elegida = it })

        reglaCompose.onNodeWithTag(EtiquetasMenu.seccion(SeccionAdministrador.CANCHAS)).performClick()

        assertEquals(SeccionAdministrador.CANCHAS, elegida)
    }

    @Test
    fun dadoElMenu_cuandoPulsaSalirYRespondeNo_entoncesNoCierraLaSesion() {
        var salio = false
        mostrarMenu(alSalir = { salio = true })

        reglaCompose.onNodeWithTag(EtiquetasMenu.SALIR).performClick()
        reglaCompose.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        reglaCompose.onNodeWithTag(EtiquetasCerrarSesion.CANCELAR).performClick()

        assertFalse(salio)
    }

    @Test
    fun dadoElMenu_cuandoPulsaSalirYConfirma_entoncesCierraLaSesion() {
        var salio = false
        mostrarMenu(alSalir = { salio = true })

        reglaCompose.onNodeWithTag(EtiquetasMenu.SALIR).performClick()
        reglaCompose.onNodeWithTag(EtiquetasCerrarSesion.CONFIRMAR).performClick()

        assertTrue(salio)
    }

    @Test
    fun dadoElCliente_cuandoPulsaSalir_entoncesPrimeroPreguntaSiEstaSeguro() {
        var salio = false
        reglaCompose.setContent {
            CanchAppTheme { PantallaPendiente(titulo = "Reservar", requerimiento = "RF-06", alSalir = { salio = true }) }
        }

        reglaCompose.onNodeWithTag(EtiquetasPantallaPendiente.SALIR).performClick()
        assertFalse(salio)
        reglaCompose.onNodeWithText("Sí, salir").performClick()

        assertTrue(salio)
    }
}
