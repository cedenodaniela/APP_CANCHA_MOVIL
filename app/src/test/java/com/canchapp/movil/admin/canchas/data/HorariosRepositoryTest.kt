package com.canchapp.movil.admin.canchas.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** RF-05 · Contrato de `/horario`, `/tarifas` y `/bloqueos`. */
class HorariosRepositoryTest {

    private val servidor = MockWebServer()
    private lateinit var repositorio: HorariosRepository

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
            .create<HorariosApi>()
        repositorio = HorariosRepository(api)
    }

    @After
    fun cerrar() = servidor.close()

    @Test
    fun `dado las tarifas del backend, cuando se leen, entonces el precio llega como texto con 2 decimales`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200)
                .body("""[{"id":2,"dias":["LUNES","MARTES"],"inicio":"18:00","fin":"24:00","precioHora":"25.00"}]""")
                .build(),
        )

        val tarifas = repositorio.tarifas().getOrThrow()

        assertEquals("25.00", tarifas[0].precioHora)
        assertEquals("24:00", tarifas[0].fin)
    }

    @Test
    fun `dado una tarifa existente, cuando se guarda, entonces se edita con PUT y su id`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200)
                .body("""{"id":3,"dias":["LUNES"],"inicio":"10:00","fin":"18:00","precioHora":"21.50"}""")
                .build(),
        )

        repositorio.guardarTarifa(3, SolicitudTarifa(listOf("LUNES"), "10:00", "18:00", "21.50")).getOrThrow()

        val solicitud = servidor.takeRequest()
        assertEquals("PUT", solicitud.method)
        assertEquals("/api/v1/tarifas/3", solicitud.url.encodedPath)
        assertEquals("""{"dias":["LUNES"],"inicio":"10:00","fin":"18:00","precioHora":"21.50"}""", solicitud.body?.utf8())
    }

    @Test
    fun `dado un bloqueo de todo el complejo sin confirmar, cuando se crea, entonces no se envian cancha ni confirmar`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(201)
                .body("""{"id":5,"fecha":"2026-12-25","inicio":"10:00","fin":"24:00","motivo":"Navidad"}""")
                .build(),
        )

        val bloqueo = repositorio.crearBloqueo(SolicitudBloqueo(null, "2026-12-25", "10:00", "24:00", "Navidad")).getOrThrow()

        val solicitud = servidor.takeRequest()
        assertEquals("/api/v1/bloqueos", solicitud.url.encodedPath)
        assertEquals(
            // confirmar = false no viaja: kotlinx no envía valores por defecto y el backend lo lee como false.
            """{"fecha":"2026-12-25","inicio":"10:00","fin":"24:00","motivo":"Navidad"}""",
            solicitud.body?.utf8(),
        )
        assertEquals(null, bloqueo.canchaId)
    }
}
