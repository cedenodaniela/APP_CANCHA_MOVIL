package com.canchapp.movil.admin.canchas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Convierte la foto que eligió la persona (cámara o galería) en un JPG liviano antes de subirla.
 *
 * Una foto de celular puede pesar más de 5 MB o venir en HEIC; así siempre llega un JPG de hasta
 * [LADO_MAXIMO] px, que el backend acepta y convierte a WebP. No es una regla de negocio: solo
 * evita subir megas que el backend igual reduciría.
 */
@Singleton
class PreparadorFoto @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) {

    /** @return el JPG, o `null` si el archivo no se pudo leer como imagen */
    suspend fun preparar(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { decodificar(uri)?.let(::comprimir) }.getOrNull()
    }

    private fun decodificar(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder respeta la orientación EXIF de las fotos de la cámara.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.contentResolver, uri)) { decodificador, info, _ ->
                val escala = escala(info.size.width, info.size.height)
                decodificador.setTargetSize((info.size.width * escala).toInt(), (info.size.height * escala).toInt())
                decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var muestra = 1
            while (max(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        }

    private fun comprimir(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
            salida.toByteArray()
        }

    private fun escala(ancho: Int, alto: Int): Float = minOf(1f, LADO_MAXIMO.toFloat() / max(ancho, alto))

    private companion object {
        const val LADO_MAXIMO = 1600
        const val CALIDAD = 85
    }
}
