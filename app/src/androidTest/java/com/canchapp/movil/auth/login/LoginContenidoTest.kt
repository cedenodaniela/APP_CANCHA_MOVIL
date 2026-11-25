package com.canchapp.movil.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

/** RF-01 · Pantalla de inicio de sesión, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class LoginContenidoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    private fun mostrar(estado: EstadoLogin, alIngresar: () -> Unit = {}, alCrearCuenta: () -> Unit = {}) {
        reglaCompose.setContent {
            CanchAppTheme { LoginContenido(estado, {}, {}, {}, alIngresar, alCrearCuenta) }
        }
    }

    @Test
    fun dadoQueElBackendRechaza_cuandoSeMuestra_entoncesSeLeeSuMensaje() {
        mostrar(EstadoLogin(error = MensajeError.DelServidor("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos")))

        reglaCompose.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun dadoCamposInvalidos_cuandoSeMuestra_entoncesExplicaCadaError() {
        mostrar(EstadoLogin(emailInvalido = true, passwordVacia = true))

        reglaCompose.onNodeWithText("Ingresa un correo válido.").assertIsDisplayed()
        reglaCompose.onNodeWithText("Ingresa tu contraseña.").assertIsDisplayed()
    }

    @Test
    fun dadoQueEstaIngresando_cuandoSeMuestra_entoncesElBotonEstaDeshabilitado() {
        mostrar(EstadoLogin(cargando = true))

        reglaCompose.onNodeWithTag(EtiquetasLogin.INGRESAR).assertIsNotEnabled()
        reglaCompose.onNodeWithText("Ingresando…").assertIsDisplayed()
    }

    @Test
    fun dadoLaContrasenaOculta_cuandoPulsaMostrar_entoncesCambiaElBoton() {
        reglaCompose.setContent {
            var estado by remember { mutableStateOf(EstadoLogin(password = "secreta")) }
            CanchAppTheme {
                LoginContenido(estado, {}, {}, { estado = estado.copy(passwordVisible = !estado.passwordVisible) }, {}, {})
            }
        }

        reglaCompose.onNodeWithContentDescription("Mostrar contraseña").performClick()

        reglaCompose.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
    }

    @Test
    fun dadoElFormulario_cuandoPulsaIngresarYCrearCuenta_entoncesAvisa() {
        var ingreso = false
        var crearCuenta = false
        mostrar(EstadoLogin(), alIngresar = { ingreso = true }, alCrearCuenta = { crearCuenta = true })

        reglaCompose.onNodeWithTag(EtiquetasLogin.INGRESAR).performScrollTo().performClick()
        reglaCompose.onNodeWithText("Crear cuenta").performScrollTo().performClick()

        assertTrue(ingreso)
        assertTrue(crearCuenta)
    }
}
