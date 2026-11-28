package com.canchapp.movil.admin.ajustes

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

/** RF-03 · Pantalla de ajustes del complejo, con un emulador (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class AjustesContenidoTest {

    @get:Rule
    val reglaCompose = createComposeRule()

    private val formulario = FormularioAjustes(
        nombre = "Complejo Deportivo Calceta",
        direccion = "Calceta, Manabí",
        telefono = "0969908865",
        banco = "Banco Pichincha",
        numeroCuenta = "2201234567",
        titular = "Complejo Deportivo Calceta",
        minutosExpiracion = "15",
        horasPlazoCancelacion = "3",
        maxReservasEfectivo = "1",
        diasAnticipacion = "3",
        limitarTrasInasistencia = true,
    )

    private fun mostrar(estado: EstadoAjustes, acciones: AccionesAjustes = AccionesAjustes()) {
        reglaCompose.setContent { CanchAppTheme { AjustesContenido(estado, acciones) } }
    }

    @Test
    fun dadoLaConfiguracion_cuandoSeMuestra_entoncesSeVenSusTresSecciones() {
        mostrar(EstadoAjustes(cargando = false, formulario = formulario))

        reglaCompose.onNodeWithText("Datos del complejo").assertIsDisplayed()
        reglaCompose.onNodeWithText("Datos bancarios").performScrollTo().assertIsDisplayed()
        reglaCompose.onNodeWithText("Políticas de reserva").performScrollTo().assertIsDisplayed()
        reglaCompose.onNodeWithText("2201234567").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dadoQueElBackendRechaza_cuandoSeMuestra_entoncesSeLeeSuMensaje() {
        mostrar(
            EstadoAjustes(
                cargando = false,
                formulario = formulario.copy(diasAnticipacion = "0"),
                error = MensajeError.DelServidor("CONFIGURACION_INVALIDA", "Los días de anticipación deben ser al menos 1."),
            ),
        )

        reglaCompose.onNodeWithText("Los días de anticipación deben ser al menos 1.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dadoQueEstaGuardando_cuandoSeMuestra_entoncesElBotonEstaDeshabilitado() {
        mostrar(EstadoAjustes(cargando = false, formulario = formulario, guardando = true))

        reglaCompose.onNodeWithTag(EtiquetasAjustes.GUARDAR).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun dadoUnErrorDeCarga_cuandoPulsaReintentar_entoncesAvisa() {
        var reintento = false
        mostrar(
            EstadoAjustes(cargando = false, errorCarga = MensajeError.SinConexion),
            AccionesAjustes(alReintentar = { reintento = true }),
        )

        reglaCompose.onNodeWithTag(EtiquetasAjustes.REINTENTAR).performClick()

        assertTrue(reintento)
    }
}
