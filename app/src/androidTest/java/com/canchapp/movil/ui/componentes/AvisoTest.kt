package com.canchapp.movil.ui.componentes

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canchapp.movil.ui.theme.CanchAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba de pantalla: se ejecuta en un emulador con `./gradlew connectedAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class AvisoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    @Test
    fun dadoUnError_cuandoSeMuestra_entoncesSeLeeElTexto() {
        reglaCompose.setContent {
            CanchAppTheme { Aviso("Correo o contraseña incorrectos") }
        }

        reglaCompose.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }
}
