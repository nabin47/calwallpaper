package io.github.nabin47.calwallpaper.data.secure

/** Result of reading a stored token. [Present.toString] never prints the token. */
sealed interface TokenRead {
    /** No token stored. */
    data object Missing : TokenRead

    /** A token is stored but cannot be decrypted any more; the user must enter it again. */
    data object Unreadable : TokenRead

    /** The decrypted token. */
    class Present(
        val token: String,
    ) : TokenRead {
        override fun toString(): String = "TokenRead.Present(<redacted>)"
    }
}
