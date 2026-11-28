package com.canchapp.movil.admin.ajustes.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** RF-03 · Contrato de `/api/v1/configuracion`. */
class ConfiguracionRepositoryTest {

    private val servidor = MockWebServer()
    private lateinit var repositorio: ConfiguracionRepository

    private val cuerpo = """
        {"complejo":{"nombre":"Complejo Deportivo Calceta","direccion":"Calceta, Manabí","telefono":"0969908865"},
         "datosBancarios":{"banco":"Banco Pichincha","tipoCuenta":"AHORROS","numeroCuenta":"2201234567","titular":"Complejo Deportivo Calceta"},
         "politica":{"minutosExpiracion":15,"horasPlazoCancelacion":3,"maxReservasEfectivo":1,"diasAnticipacion":3,"limitarTrasInasistencia":true},
         "actualizadaEn":"2026-11-27T15:00:00Z"}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor.start()
        // Misma configuración que core/red/ModuloRed.
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<ConfiguracionApi>()
        repositorio = ConfiguracionRepository(api)
    }

    @After
    fun cerrar() = servidor.close()

    @Test
    fun `dado la configuracion del backend, cuando se consulta, entonces se leen sus tres partes`() = runTest {
        servidor.enqueue(MockResponse.Builder().code(200).body(cuerpo).build())

        val configuracion = repositorio.consultar().getOrThrow()

        assertEquals("Complejo Deportivo Calceta", configuracion.complejo.nombre)
        assertEquals("2201234567", configuracion.datosBancarios.numeroCuenta)
        assertEquals(15, configuracion.politica.minutosExpiracion)
        val solicitud = servidor.takeRequest()
        assertEquals("GET", solicitud.method)
        assertEquals("/api/v1/configuracion", solicitud.url.encodedPath)
    }

    @Test
    fun `dado un numero vacio, cuando se guarda, entonces el campo no se envia y el backend lo explica`() = runTest {
        servidor.enqueue(
            MockResponse.Builder()
                .code(400)
                .body("""{"codigo":"CONFIGURACION_INVALIDA","mensaje":"El tiempo para subir el comprobante debe ser de al menos 1 minuto."}""")
                .build(),
        )

        val resultado = repositorio.guardar(
            ConfiguracionDto(
                ComplejoDto("Complejo", "Calceta", "0969908865"),
                DatosBancariosDto("Banco", "AHORROS", "1", "Titular"),
                PoliticaDto(null, 3, 1, 3, true),
            ),
        )

        assertTrue(resultado.isFailure)
        val solicitud = servidor.takeRequest()
        assertEquals("PUT", solicitud.method)
        val enviado = solicitud.body?.utf8().orEmpty()
        assertTrue(enviado.contains(""""diasAnticipacion":3"""))
        assertTrue(!enviado.contains("minutosExpiracion"))
    }
}
