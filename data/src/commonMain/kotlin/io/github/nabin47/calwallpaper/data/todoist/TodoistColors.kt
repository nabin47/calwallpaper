package io.github.nabin47.calwallpaper.data.todoist

/**
 * Todoist's named project colors as `0xAARRGGBB`, from Doist's official SDK (todoist-api-typescript,
 * `src/utils/colors.ts`). Pure.
 */
internal object TodoistColors {
    private val HEX =
        mapOf(
            "berry_red" to 0xB8255F,
            "red" to 0xDB4035,
            "orange" to 0xFF9933,
            "yellow" to 0xFAD000,
            "olive_green" to 0xAFB83B,
            "lime_green" to 0x7ECC49,
            "green" to 0x299438,
            "mint_green" to 0x6ACCBC,
            "teal" to 0x158FAD,
            "sky_blue" to 0x14AAF5,
            "light_blue" to 0x96C3EB,
            "blue" to 0x4073FF,
            "grape" to 0x884DFF,
            "violet" to 0xAF38EB,
            "lavender" to 0xEB96EB,
            "magenta" to 0xE05194,
            "salmon" to 0xFF8D85,
            "charcoal" to 0x808080,
            "grey" to 0xB8B8B8,
            "taupe" to 0xCCAC93,
        )

    private const val OPAQUE = -0x1000000

    /** The opaque color for [name], or `null` for an unknown or missing name. */
    fun argb(name: String?): Int? = HEX[name]?.let { it or OPAQUE }
}
