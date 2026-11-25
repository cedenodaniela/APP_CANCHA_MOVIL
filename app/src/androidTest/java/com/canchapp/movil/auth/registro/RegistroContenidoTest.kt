package com.canchapp.movil.auth.registro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** RF-01 · Pantalla de registro, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class RegistroContenidoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    private fun mostrar(estado: EstadoRegistro, acciones: AccionesRegistro = AccionesRegistro()) {
        reglaCompose.setContent {
            CanchAppTheme { RegistroContenido(estado, acciones) }
        }
    }

    @Test
    fun dadoCamposConError_cuandoSeMuestra_entoncesExplicaCadaUno() {
        mostrar(EstadoRegistro(errores = CampoRegistro.entries.toSet()))

        listOf(
            "Ingresa tu nombre.",
            "Ingresa tu apellido.",
            "Ingresa un correo válido.",
            "Usa al menos 8 caracteres.",
            "Las contraseñas no coinciden.",
        ).forEach { texto ->
            reglaCompose.onNodeWithText(texto).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun dadoQueElBackendRechaza_cuandoSeMuestra_entoncesSeLeeSuMensaje() {
        mostrar(EstadoRegistro(error = MensajeError.DelServidor("CORREO_YA_REGISTRADO", "Ya existe una cuenta con ese correo.")))

        reglaCompose.onNodeWithText("Ya existe una cuenta con ese correo.").assertIsDisplayed()
    }

    @Test
    fun dadoQueEstaCreandoLaCuenta_cuandoSeMuestra_entoncesElBotonEstaDeshabilitado() {
        mostrar(EstadoRegistro(cargando = true))

        reglaCompose.onNodeWithTag(EtiquetasRegistro.CREAR).performScrollTo().assertIsNotEnabled()
        reglaCompose.onNodeWithText("Creando cuenta…").assertIsDisplayed()
    }

    @Test
    fun dadoElFormulario_cuandoPulsaCrearEIniciarSesion_entoncesAvisa() {
        var registro = false
        var iniciarSesion = false
        mostrar(EstadoRegistro(), AccionesRegistro(alRegistrar = { registro = true }, alIniciarSesion = { iniciarSesion = true }))

        reglaCompose.onNodeWithTag(EtiquetasRegistro.CREAR).performScrollTo().performClick()
        reglaCompose.onNodeWithText("Inicia sesión").performScrollTo().performClick()

        assertTrue(registro)
        assertTrue(iniciarSesion)
    }
}
