package io.github.nabin47.calwallpaper.data.secure

/**
 * An encrypted secret as stored on disk: the cipher's [iv] and the [ciphertext] (which includes the GCM tag).
 * The file format is `version | ivLength | iv | ciphertext`, so a future format change can be detected.
 * Holds no key and nothing readable; pure.
 */
class SealedSecret(
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    init {
        require(iv.size in 1..MAX_IV_LENGTH) { "Bad IV length" }
        require(ciphertext.isNotEmpty()) { "Empty ciphertext" }
    }

    /** The on-disk bytes. */
    fun encode(): ByteArray = byteArrayOf(FORMAT_VERSION, iv.size.toByte()) + iv + ciphertext

    /** Parsing. */
    companion object {
        private const val FORMAT_VERSION: Byte = 1
        private const val HEADER_LENGTH = 2
        private const val MAX_IV_LENGTH = 32

        /** The secret in [bytes], or `null` if they are not a valid version-1 record. */
        fun decode(bytes: ByteArray): SealedSecret? {
            val ivLength = bytes.getOrNull(1)?.toInt() ?: return null
            val bodyStart = HEADER_LENGTH + ivLength
            val valid = bytes[0] == FORMAT_VERSION && ivLength in 1..MAX_IV_LENGTH && bytes.size > bodyStart
            return if (valid) {
                SealedSecret(bytes.copyOfRange(HEADER_LENGTH, bodyStart), bytes.copyOfRange(bodyStart, bytes.size))
            } else {
                null
            }
        }
    }
}
