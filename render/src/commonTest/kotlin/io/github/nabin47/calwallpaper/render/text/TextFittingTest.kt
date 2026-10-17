package io.github.nabin47.calwallpaper.render.text

import io.github.nabin47.calwallpaper.render.FakeMeasurer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextFittingTest {
    private val style = TextStyle(sizePx = 10f) // 5 px per character with the fake measurer

    @Test
    fun ellipsizeKeepsFittingText() {
        assertEquals("Hello", FakeMeasurer.ellipsize("Hello", style, 25f))
    }

    @Test
    fun ellipsizeCutsAndAppendsEllipsis() {
        val result = FakeMeasurer.ellipsize("Hello world", style, 30f)
        assertEquals("Hello$ELLIPSIS", result)
        assertTrue(FakeMeasurer.width(result, style) <= 30f)
    }

    @Test
    fun ellipsizeNeverSplitsSurrogatePairs() {
        val text = "ab😀cd" // a b 😀 c d
        val result = FakeMeasurer.ellipsize(text, style, 20f)
        assertTrue(result.none { it.isHighSurrogate() } || result.contains("😀"))
    }

    @Test
    fun ellipsizeReturnsEmptyWhenNothingFits() {
        assertEquals("", FakeMeasurer.ellipsize("Hello", style, 2f))
    }

    @Test
    fun wrapBreaksOnWordsAndEllipsizesTheLastLine() {
        val lines = FakeMeasurer.wrap("one two three four five six", style, 40f, maxLines = 2)
        assertEquals(listOf("one two", "three f$ELLIPSIS"), lines)
    }

    @Test
    fun wrapBreaksLongWordsByCharacter() {
        val lines = FakeMeasurer.wrap("abcdefghijklmnop", style, 25f, maxLines = 3)
        assertEquals("abcde", lines[0])
        assertEquals(3, lines.size)
    }
}
