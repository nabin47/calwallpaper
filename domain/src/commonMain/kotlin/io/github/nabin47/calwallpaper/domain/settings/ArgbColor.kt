package io.github.nabin47.calwallpaper.domain.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

/**
 * A color as `0xAARRGGBB`. Stored in JSON as `"#AARRGGBB"` so exported settings are readable and editable.
 */
@Serializable(with = ArgbColorSerializer::class)
@JvmInline
value class ArgbColor(
    val argb: Int,
) {
    /** The color as `#AARRGGBB` (upper-case hex). */
    fun toHex(): String =
        "#" +
            argb
                .toUInt()
                .toString(HEX_RADIX)
                .uppercase()
                .padStart(HEX_DIGITS, '0')

    /** Parsing helpers. */
    companion object {
        private const val HEX_RADIX = 16
        private const val HEX_DIGITS = 8
        private const val OPAQUE_ALPHA = 0xFF000000.toInt()

        /**
         * Parses `#AARRGGBB` or `#RRGGBB` (opaque). Returns `null` for anything else, never throws.
         */
        fun parseOrNull(text: String): ArgbColor? {
            val hex = text.removePrefix("#")
            val value =
                hex
                    .takeIf { it.length == HEX_DIGITS || it.length == HEX_DIGITS - 2 }
                    ?.toUIntOrNull(HEX_RADIX)
                    ?.toInt()
            return value?.let { ArgbColor(if (hex.length == HEX_DIGITS) it else it or OPAQUE_ALPHA) }
        }
    }
}

/** Serializes [ArgbColor] as a `#AARRGGBB` string. Rejects malformed input with a [SerializationException]. */
object ArgbColorSerializer : KSerializer<ArgbColor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.github.nabin47.calwallpaper.ArgbColor", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ArgbColor,
    ) = encoder.encodeString(value.toHex())

    override fun deserialize(decoder: Decoder): ArgbColor {
        val text = decoder.decodeString()
        return ArgbColor.parseOrNull(text) ?: throw SerializationException("Invalid color: expected #AARRGGBB")
    }
}
