package com.canchapp.movil.admin.canchas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.admin.canchas.data.BloqueoDto
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.DiaDto
import com.canchapp.movil.admin.canchas.data.TarifaDto
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** RF-05 · Secciones de horario, tarifas y bloqueos, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class HorariosSeccionesTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    @Test
    fun dadoLaPantallaDeCanchas_cuandoTocaLaPestanaTarifas_entoncesLaPide() {
        var elegida: PestanaCanchas? = null
        reglaCompose.setContent {
            CanchAppTheme {
                CanchasContenido(EstadoCanchas(cargando = false), AccionesCanchas(), alCambiarPestana = { elegida = it })
            }
        }

        reglaCompose.onNodeWithTag(EtiquetasCanchas.pestana(PestanaCanchas.TARIFAS)).performClick()

        assertEquals(PestanaCanchas.TARIFAS, elegida)
    }

    @Test
    fun dadoUnDiaCerrado_cuandoSeMuestra_entoncesDiceCerradoYNoPideHoras() {
        reglaCompose.setContent {
            CanchAppTheme {
                HorarioSeccion(
                    EstadoHorario(cargando = false, dias = listOf(DiaDto("DOMINGO", false), DiaDto("LUNES", true, "10:00", "24:00"))),
                    alEditarDia = { _, _ -> }, alGuardar = {}, alReintentar = {},
                )
            }
        }

        reglaCompose.onNodeWithText("Cerrado").assertIsDisplayed()
        reglaCompose.onNodeWithText("Cierra 24:00").assertIsDisplayed()
    }

    @Test
    fun dadoLasTarifasDelDercas_cuandoSeMuestran_entoncesSeLeeElPrecioYLaFranja() {
        reglaCompose.setContent {
            CanchAppTheme {
                TarifasSeccion(
                    EstadoTarifas(cargando = false, tarifas = listOf(TarifaDto(2, DIAS_API, "18:00", "24:00", "25.00"))),
                    AccionesTarifas(),
                )
            }
        }

        reglaCompose.onNodeWithText("$25.00 por hora").assertIsDisplayed()
        reglaCompose.onNodeWithText("18:00 a 24:00").assertIsDisplayed()
        reglaCompose.onNodeWithText("Todos los días").assertIsDisplayed()
    }

    @Test
    fun dadoReservasEnElHorario_cuandoSeMuestra_entoncesElDialogoDejaConfirmarElBloqueo() {
        var confirmo = false
        reglaCompose.setContent {
            CanchAppTheme {
                BloqueosSeccion(
                    EstadoBloqueos(
                        cargando = false,
                        bloqueos = listOf(BloqueoDto(5, 1, "2026-12-01", "10:00", "12:00", "Mantenimiento")),
                        canchas = listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, true)),
                        confirmacion = "Ese horario tiene 2 reservas activas.",
                    ),
                    AccionesBloqueos(alConfirmar = { confirmo = true }),
                    hoy = LocalDate.of(2026, 11, 28),
                )
            }
        }

        reglaCompose.onNodeWithText("Ese horario tiene 2 reservas activas.").assertIsDisplayed()
        reglaCompose.onNodeWithText("Bloquear").performClick()
        assertEquals(true, confirmo)
    }
}
