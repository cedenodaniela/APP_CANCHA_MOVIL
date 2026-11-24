package com.canchapp.movil.core.sesion

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Cifra la sesión antes de guardarla en el celular (app_cancha/AGENTS.md §6). */
interface CifradorToken {
    fun cifrar(texto: String): String
    fun descifrar(cifrado: String): String
}

/**
 * Cifrador con una clave AES-256-GCM que vive en el Android Keystore: la clave nunca sale
 * del hardware seguro del celular, así que una copia de los datos de la app no sirve en otro equipo.
 */
@Singleton
class CifradorKeystore @Inject constructor() : CifradorToken {

    private val keyStore: KeyStore = KeyStore.getInstance(PROVEEDOR).apply { load(null) }

    override fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, clave())
        val datos = cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    override fun descifrar(cifrado: String): String {
        val datos = Base64.decode(cifrado, Base64.NO_WRAP)
        val iv = datos.copyOfRange(0, LONGITUD_IV)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(LONGITUD_TAG_BITS, iv))
        return String(cipher.doFinal(datos, LONGITUD_IV, datos.size - LONGITUD_IV), Charsets.UTF_8)
    }

    private fun clave(): SecretKey =
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: crearClave()

    private fun crearClave(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()

    private companion object {
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "canchapp_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val LONGITUD_IV = 12
        const val LONGITUD_TAG_BITS = 128
    }
}
