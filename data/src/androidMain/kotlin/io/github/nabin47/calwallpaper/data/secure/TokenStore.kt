package io.github.nabin47.calwallpaper.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores one secret (the Todoist API token) encrypted with an AES-256-GCM key that never leaves the Android
 * Keystore (CLAUDE.md §7; not the deprecated `security-crypto`). The ciphertext lives in `noBackupFilesDir`, so
 * it is never backed up: a restored backup could not decrypt it anyway, because Keystore keys do not travel.
 *
 * The plain token exists only in memory, only while it is needed. It is never logged, and no error message
 * contains it. Disk and Keystore work runs on [Dispatchers.IO].
 */
class TokenStore(
    context: Context,
    private val keyAlias: String,
    fileName: String,
) {
    private val file = File(context.applicationContext.noBackupFilesDir, fileName)

    /** True if a token is stored (it may still fail to decrypt; see [read]). */
    fun exists(): Boolean = file.exists()

    /**
     * The stored token, `null` if none is stored, or [TokenRead.Unreadable] if it can no longer be decrypted
     * (for example after the user removed the device's screen lock, which can delete Keystore keys).
     */
    suspend fun read(): TokenRead =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext TokenRead.Missing
            val sealed = runCatching { file.readBytes() }.getOrNull()?.let(SealedSecret::decode)
            val plain = sealed?.let(::decrypt)
            if (plain == null) TokenRead.Unreadable else TokenRead.Present(plain)
        }

    /** Encrypts and stores [token], replacing any previous one. Returns false if the Keystore refused. */
    suspend fun write(token: String): Boolean =
        withContext(Dispatchers.IO) {
            val sealed = encrypt(token) ?: return@withContext false
            runCatching {
                val temp = File(file.parentFile, file.name + ".tmp")
                temp.writeBytes(sealed.encode())
                temp.renameTo(file)
            }.getOrDefault(false)
        }

    /** Deletes the stored token and its key. */
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            file.delete()
            runCatching { keyStore().deleteEntry(keyAlias) }
        }
    }

    private fun encrypt(token: String): SealedSecret? =
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            SealedSecret(cipher.iv, cipher.doFinal(token.encodeToByteArray()))
        } catch (_: GeneralSecurityException) {
            null
        }

    private fun decrypt(sealed: SealedSecret): String? =
        try {
            val key = keyStore().getKey(keyAlias, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed.iv))
            cipher.doFinal(sealed.ciphertext).decodeToString()
        } catch (_: GeneralSecurityException) {
            null
        }

    /** The existing key, or a new one. The Keystore generates the IV for every encryption. */
    private fun key(): SecretKey {
        (keyStore().getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
    }
}
