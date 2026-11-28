package com.canchapp.movil.admin.canchas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** RF-04 · Pantallas de canchas, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class CanchasContenidoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    private val canchas = listOf(
        CanchaItem(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, true), null),
        CanchaItem(CanchaDto(2, "Cancha 2", "Sintético", 30, 50, false), null),
    )

    @Test
    fun dadoLasCanchas_cuandoSeMuestran_entoncesSeLeeSuEstadoConTexto() {
        reglaCompose.setContent { CanchAppTheme { CanchasContenido(EstadoCanchas(cargando = false, canchas = canchas), AccionesCanchas()) } }

        reglaCompose.onNodeWithText("Cancha 1").assertIsDisplayed()
        reglaCompose.onNodeWithText("Activa").assertIsDisplayed()
        reglaCompose.onNodeWithText("Inactiva").assertIsDisplayed()
    }

    @Test
    fun dadoUnaCanchaActiva_cuandoTocaSuInterruptor_entoncesPideDesactivarla() {
        var pedido: Pair<Long, Boolean>? = null
        reglaCompose.setContent {
            CanchAppTheme {
                CanchasContenido(
                    EstadoCanchas(cargando = false, canchas = canchas),
                    AccionesCanchas(alCambiarEstado = { id, activa -> pedido = id to activa }),
                )
            }
        }

        reglaCompose.onNodeWithTag(EtiquetasCanchas.estado(1)).performClick()

        assertEquals(1L to false, pedido)
    }

    @Test
    fun dadoReservasActivas_cuandoSeMuestra_entoncesElDialogoDiceCuantasYDejaConfirmar() {
        var confirmo = false
        reglaCompose.setContent {
            CanchAppTheme {
                CanchasContenido(
                    EstadoCanchas(
                        cargando = false,
                        canchas = canchas,
                        confirmacion = ConfirmacionDesactivar(1, "Cancha 1", "La cancha tiene 2 reservas activas."),
                    ),
                    AccionesCanchas(alConfirmarDesactivar = { confirmo = true }),
                )
            }
        }

        reglaCompose.onNodeWithText("¿Desactivar Cancha 1?").assertIsDisplayed()
        reglaCompose.onNodeWithText("La cancha tiene 2 reservas activas.").assertIsDisplayed()
        reglaCompose.onNodeWithText("Desactivar").performClick()
        assertTrue(confirmo)
    }

    @Test
    fun dadoUnNombreRepetido_cuandoSeMuestraElFormulario_entoncesSeLeeElMensajeYNoSeGuardaDosVeces() {
        reglaCompose.setContent {
            CanchAppTheme {
                CanchaFormularioContenido(
                    EstadoFormularioCancha(
                        nombre = "Cancha 1",
                        error = MensajeError.DelServidor("CANCHA_REPETIDA", "Ya existe una cancha con ese nombre."),
                        guardando = true,
                    ),
                    AccionesFormularioCancha(),
                )
            }
        }

        reglaCompose.onNodeWithText("Nueva cancha").assertIsDisplayed()
        reglaCompose.onNodeWithText("Ya existe una cancha con ese nombre.").performScrollTo().assertIsDisplayed()
        reglaCompose.onNodeWithTag(EtiquetasFormularioCancha.GUARDAR).performScrollTo().assertIsNotEnabled()
    }
}
