package io.github.nabin47.calwallpaper.data.secure

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SealedSecretTest {
    @Test
    fun roundTrips() {
        val sealed = SealedSecret(ByteArray(12) { it.toByte() }, byteArrayOf(9, 8, 7, 6, 5))
        val decoded = assertNotNull(SealedSecret.decode(sealed.encode()))
        assertContentEquals(sealed.iv, decoded.iv)
        assertContentEquals(sealed.ciphertext, decoded.ciphertext)
    }

    @Test
    fun rejectsTruncatedOrForeignBytes() {
        val good = SealedSecret(ByteArray(12), byteArrayOf(1)).encode()
        assertNull(SealedSecret.decode(byteArrayOf()))
        assertNull(SealedSecret.decode(byteArrayOf(1)))
        assertNull(SealedSecret.decode(good.copyOf(good.size - 1)), "no ciphertext left")
        assertNull(SealedSecret.decode(byteArrayOf(2) + good.copyOfRange(1, good.size)), "unknown version")
        assertNull(SealedSecret.decode(byteArrayOf(1, 0, 5)), "zero IV length")
        assertNull(SealedSecret.decode(byteArrayOf(1, -1, 5)), "negative IV length")
    }
}
