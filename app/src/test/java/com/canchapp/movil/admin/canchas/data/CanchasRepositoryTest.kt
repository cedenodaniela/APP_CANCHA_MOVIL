package com.canchapp.movil.admin.canchas.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** RF-04 · Contrato de `/api/v1/canchas`. */
class CanchasRepositoryTest {

    private val servidor = MockWebServer()
    private lateinit var repositorio: CanchasRepository

    @Before
    fun preparar() {
        servidor.start()
        // Misma configuración que core/red/ModuloRed.
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val base = servidor.url("/api/v1/")
        val api = Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<CanchasApi>()
        repositorio = CanchasRepository(api, base)
    }

    @After
    fun cerrar() = servidor.close()

    @Test
    fun `dado una cancha que se desactiva confirmando, cuando se envia, entonces va confirmar en true`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200)
                .body("""{"id":1,"nombre":"Cancha 1","tipoCesped":"Sintético","anchoMetros":30,"largoMetros":50,"activa":false}""")
                .build(),
        )

        val cancha = repositorio.cambiarEstado(1, activa = false, confirmar = true).getOrThrow()

        val solicitud = servidor.takeRequest()
        assertEquals("PATCH", solicitud.method)
        assertEquals("/api/v1/canchas/1/estado", solicitud.url.encodedPath)
        assertEquals("""{"activa":false,"confirmar":true}""", solicitud.body?.utf8())
        assertEquals(false, cancha.activa)
        assertNull(cancha.fotoVersion)
    }

    @Test
    fun `dado una foto preparada, cuando se sube, entonces va como parte foto en JPG`() = runTest {
        servidor.enqueue(
            MockResponse.Builder().code(200)
                .body("""{"id":1,"nombre":"Cancha 1","tipoCesped":"Sintético","anchoMetros":30,"largoMetros":50,"activa":true,"fotoVersion":7}""")
                .build(),
        )

        val cancha = repositorio.subirFoto(1, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())).getOrThrow()

        val solicitud = servidor.takeRequest()
        assertEquals("PUT", solicitud.method)
        assertEquals("/api/v1/canchas/1/foto", solicitud.url.encodedPath)
        assertTrue(solicitud.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        val cuerpo = solicitud.body?.utf8().orEmpty()
        assertTrue(cuerpo.contains("""name="foto"; filename="cancha.jpg""""))
        assertTrue(cuerpo.contains("Content-Type: image/jpeg"))
        assertEquals(7L, cancha.fotoVersion)
    }

    @Test
    fun `dado una cancha con foto, cuando se arma su url, entonces lleva la version para no usar la cache vieja`() {
        val repo = CanchasRepository(api = io.mockk.mockk(), urlApi = "http://10.0.2.2:8080/api/v1/".toHttpUrl())

        assertEquals(
            "http://10.0.2.2:8080/api/v1/canchas/2/foto?v=123",
            repo.urlFoto(CanchaDto(2, "Cancha 2", "Sintético", 30, 50, true, 123)),
        )
        assertNull(repo.urlFoto(CanchaDto(2, "Cancha 2", "Sintético", 30, 50, true)))
    }
}
