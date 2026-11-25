package com.canchapp.movil.auth.recuperacion

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.auth.login.EstadoLogin
import com.canchapp.movil.auth.login.EtiquetasLogin
import com.canchapp.movil.auth.login.LoginContenido
import com.canchapp.movil.core.navegacion.tokenDeEnlaceRecuperacion
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** RF-02 · Pantallas de recuperación de contraseña, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class RecuperacionContenidoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    @Test
    fun dadoElLogin_cuandoPulsaOlvideMiContrasena_entoncesAvisa() {
        var olvido = false
        reglaCompose.setContent {
            CanchAppTheme { LoginContenido(EstadoLogin(), {}, {}, {}, {}, {}, alOlvideContrasena = { olvido = true }) }
        }

        reglaCompose.onNodeWithTag(EtiquetasLogin.OLVIDE).performScrollTo().performClick()

        assertTrue(olvido)
    }

    @Test
    fun dadoQueCambioLaContrasena_cuandoVuelveAlLogin_entoncesLoConfirma() {
        reglaCompose.setContent {
            CanchAppTheme { LoginContenido(EstadoLogin(contrasenaRestablecida = true), {}, {}, {}, {}, {}) }
        }

        reglaCompose.onNodeWithText("Tu contraseña se cambió. Inicia sesión con la nueva.").assertIsDisplayed()
    }

    @Test
    fun dadoElEnlaceEnviado_cuandoSeMuestra_entoncesSeLeeElMensajeDelBackend() {
        reglaCompose.setContent {
            CanchAppTheme {
                RecuperarContenido(EstadoRecuperar(email = "ana@correo.com", enviado = "Revisa tu bandeja de entrada."), {}, {}, {})
            }
        }

        reglaCompose.onNodeWithText("Revisa tu bandeja de entrada.").assertIsDisplayed()
    }

    @Test
    fun dadoQueEstaEnviando_cuandoSeMuestra_entoncesElBotonEstaDeshabilitado() {
        reglaCompose.setContent {
            CanchAppTheme { RecuperarContenido(EstadoRecuperar(email = "ana@correo.com", cargando = true), {}, {}, {}) }
        }

        reglaCompose.onNodeWithTag(EtiquetasRecuperar.ENVIAR).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun dadoCamposConError_cuandoSeMuestraLaContrasenaNueva_entoncesExplicaCadaUno() {
        reglaCompose.setContent {
            CanchAppTheme {
                RestablecerContenido(EstadoRestablecer(errores = CampoRestablecer.entries.toSet()), AccionesRestablecer())
            }
        }

        reglaCompose.onNodeWithText("Usa al menos 8 caracteres.").performScrollTo().assertIsDisplayed()
        reglaCompose.onNodeWithText("Las contraseñas no coinciden.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dadoUnEnlaceVencido_cuandoPideOtro_entoncesAvisa() {
        var pidio = false
        reglaCompose.setContent {
            CanchAppTheme {
                RestablecerContenido(
                    EstadoRestablecer(error = MensajeError.DelServidor("ENLACE_INVALIDO", "El enlace no es válido o ya venció.")),
                    AccionesRestablecer(alPedirEnlace = { pidio = true }),
                )
            }
        }

        reglaCompose.onNodeWithText("El enlace no es válido o ya venció.").assertIsDisplayed()
        reglaCompose.onNodeWithText("Pide uno nuevo").performScrollTo().performClick()
        assertTrue(pidio)
    }

    @Test
    fun dadoEnlacesDistintos_cuandoSeLeen_entoncesSoloElDeRecuperacionDaToken() {
        assertEquals("abc", tokenDeEnlaceRecuperacion(Uri.parse("canchapp://restablecer-contrasena?token=abc")))
        assertNull(tokenDeEnlaceRecuperacion(Uri.parse("canchapp://restablecer-contrasena")))
        assertNull(tokenDeEnlaceRecuperacion(Uri.parse("canchapp://otra-cosa?token=abc")))
        assertNull(tokenDeEnlaceRecuperacion(Uri.parse("https://restablecer-contrasena?token=abc")))
    }
}
