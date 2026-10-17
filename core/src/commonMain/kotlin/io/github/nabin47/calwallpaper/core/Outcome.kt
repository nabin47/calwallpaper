package io.github.nabin47.calwallpaper.core

/**
 * Result of an operation that can fail in an expected way (a source is unreachable, a permission is missing).
 *
 * Used instead of throwing across module boundaries so callers handle failure explicitly.
 * Immutable and safe to share between threads.
 */
sealed interface Outcome<out T> {
    /** The operation produced [value]. */
    data class Success<out T>(
        val value: T,
    ) : Outcome<T>

    /** The operation failed. [reason] is a short, non-sensitive description; never put user data in it. */
    data class Failure(
        val reason: String,
    ) : Outcome<Nothing>
}

/** Transforms the success value; a [Outcome.Failure] passes through unchanged. */
inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }

/** Returns the success value, or `null` for a [Outcome.Failure]. */
fun <T> Outcome<T>.getOrNull(): T? =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> null
    }
