package com.example

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.example.ui.screens.PdfHighlightSegment
import com.example.ui.screens.PdfPageAnnotation
import com.example.ui.screens.PdfPageTextLayout
import com.example.ui.screens.PdfTextLineSpan
import com.example.ui.screens.analyzePdfPageBitmap
import com.example.ui.screens.computeTextHighlightSelection
import com.example.ui.screens.findTappedWordHighlight
import com.example.ui.screens.parsePdfAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfTextHighlightEngineTest {

    private fun fillRectPixels(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int
    ) {
        val clampedL = left.coerceIn(0, bitmap.width - 1)
        val clampedR = right.coerceIn(clampedL + 1, bitmap.width)
        val clampedT = top.coerceIn(0, bitmap.height - 1)
        val clampedB = bottom.coerceIn(clampedT + 1, bitmap.height)
        val w = clampedR - clampedL
        val h = clampedB - clampedT
        val buf = IntArray(w * h) { color }
        bitmap.setPixels(buf, 0, w, clampedL, clampedT, w, h)
    }

    /**
     * Paints a simulated word made of thin glyph strokes separated by 2px intra-word kerning,
     * mimicking real rasterized text glyphs on a page bitmap.
     */
    private fun paintSimulatedWord(
        bitmap: Bitmap,
        startX: Int,
        topY: Int,
        letterCount: Int,
        letterHeight: Int = 24,
        color: Int = Color.DKGRAY
    ): Int {
        var x = startX
        for (i in 0 until letterCount) {
            fillRectPixels(bitmap, x, topY, x + 5, topY + letterHeight, color)
            x += 7 // 5px stroke + 2px intra-word kerning
        }
        return x
    }

    private fun paintSimulatedLine(
        bitmap: Bitmap,
        startX: Int,
        topY: Int,
        wordLengths: List<Int>,
        wordSpacing: Int = 14,
        letterHeight: Int = 24
    ) {
        var x = startX
        for (len in wordLengths) {
            val endX = paintSimulatedWord(bitmap, x, topY, len, letterHeight)
            x = endX + wordSpacing
        }
    }

    @Test
    fun `analyzePdfPageBitmap detects separate text lines and words while ignoring divider and illustration`() {
        val bmp = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)

        // Thin 2px horizontal divider (should be filtered out)
        fillRectPixels(bmp, 100, 180, 1090, 182, Color.LTGRAY)

        // Three distinct body text lines with multiple words each
        paintSimulatedLine(bmp, startX = 100, topY = 240, wordLengths = listOf(4, 6, 3, 5, 8))
        paintSimulatedLine(bmp, startX = 100, topY = 282, wordLengths = listOf(5, 4, 7, 3, 6))
        paintSimulatedLine(bmp, startX = 100, topY = 324, wordLengths = listOf(7, 5, 9, 4))

        // Tall graphic block below (should not become a text line)
        fillRectPixels(bmp, 100, 480, 700, 720, Color.rgb(59, 130, 246))

        val layout = analyzePdfPageBitmap(bmp)

        assertEquals(3, layout.lines.size)
        assertEquals(5, layout.lines[0].words.size)
        assertEquals(5, layout.lines[1].words.size)
        assertEquals(4, layout.lines[2].words.size)
        assertTrue(layout.lines[0].bottom <= layout.lines[1].top + 2f)
        assertTrue(layout.lines[1].bottom <= layout.lines[2].top + 2f)
    }

    @Test
    fun `analyzePdfPageBitmap orders two-column pages down left column then right column`() {
        val bmp = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)

        // Left column (3 lines)
        paintSimulatedLine(bmp, startX = 80, topY = 200, wordLengths = listOf(4, 5, 4))
        paintSimulatedLine(bmp, startX = 80, topY = 244, wordLengths = listOf(5, 4, 5))
        paintSimulatedLine(bmp, startX = 80, topY = 288, wordLengths = listOf(4, 6, 3))

        // Right column overlapping vertically (3 lines)
        paintSimulatedLine(bmp, startX = 720, topY = 200, wordLengths = listOf(5, 4, 4))
        paintSimulatedLine(bmp, startX = 720, topY = 244, wordLengths = listOf(4, 5, 5))
        paintSimulatedLine(bmp, startX = 720, topY = 288, wordLengths = listOf(6, 4, 3))

        val layout = analyzePdfPageBitmap(bmp)

        assertEquals(6, layout.lines.size)
        // First 3 lines belong to left column, next 3 to right column
        assertTrue(layout.lines.take(3).all { it.right < 600f })
        assertTrue(layout.lines.drop(3).all { it.left > 600f })
    }

    @Test
    fun `computeTextHighlightSelection spans lines by phrase boundaries instead of rectangle marquee`() {
        val line1Words = listOf(
            PdfHighlightSegment(100f, 200f, 180f, 228f),
            PdfHighlightSegment(195f, 200f, 290f, 228f),
            PdfHighlightSegment(305f, 200f, 450f, 228f),
            PdfHighlightSegment(465f, 200f, 640f, 228f)
        )
        val line2Words = listOf(
            PdfHighlightSegment(100f, 242f, 220f, 270f),
            PdfHighlightSegment(235f, 242f, 390f, 270f),
            PdfHighlightSegment(405f, 242f, 680f, 270f)
        )
        val line3Words = listOf(
            PdfHighlightSegment(100f, 284f, 210f, 312f),
            PdfHighlightSegment(225f, 284f, 360f, 312f),
            PdfHighlightSegment(375f, 284f, 520f, 312f)
        )
        val layout = PdfPageTextLayout(
            pageWidth = 1190f,
            pageHeight = 1684f,
            lines = listOf(
                PdfTextLineSpan(100f, 200f, 640f, 228f, line1Words),
                PdfTextLineSpan(100f, 242f, 680f, 270f, line2Words),
                PdfTextLineSpan(100f, 284f, 520f, 312f, line3Words)
            )
        )

        // Drag forward from 3rd word on line 1 to 2nd word on line 3
        val selection = computeTextHighlightSelection(
            layout = layout,
            anchor = Offset(340f, 214f),
            focus = Offset(280f, 298f)
        )

        assertTrue(selection.snappedToText)
        assertEquals(3, selection.segments.size)

        // Line 1: starts at 3rd word left (305), runs to line 1 end (640)
        assertEquals(305f, selection.segments[0].left, 0.1f)
        assertEquals(640f, selection.segments[0].right, 0.1f)
        assertEquals(200f, selection.segments[0].top, 0.1f)
        assertEquals(228f, selection.segments[0].bottom, 0.1f)

        // Line 2: full intermediate line width (100 .. 680)
        assertEquals(100f, selection.segments[1].left, 0.1f)
        assertEquals(680f, selection.segments[1].right, 0.1f)

        // Line 3: starts at line 3 left (100), ends at 2nd word right (360)
        assertEquals(100f, selection.segments[2].left, 0.1f)
        assertEquals(360f, selection.segments[2].right, 0.1f)

        // Reverse drag (bottom-up from line 3 to line 1) produces identical reading-order segments
        val reverseSelection = computeTextHighlightSelection(
            layout = layout,
            anchor = Offset(280f, 298f),
            focus = Offset(340f, 214f)
        )
        assertEquals(selection.segments, reverseSelection.segments)

        // Tap on a single word highlights only that word
        val wordTap = findTappedWordHighlight(layout, Offset(250f, 214f))
        assertNotNull(wordTap)
        assertEquals(1, wordTap!!.segments.size)
        assertEquals(195f, wordTap.segments[0].left, 0.1f)
        assertEquals(290f, wordTap.segments[0].right, 0.1f)
    }

    @Test
    fun `multi-line highlight segments serialize roundtrip and hit-test per line accurately`() {
        val segs = listOf(
            PdfHighlightSegment(310f, 200f, 640f, 228f),
            PdfHighlightSegment(100f, 242f, 680f, 270f),
            PdfHighlightSegment(100f, 284f, 360f, 312f)
        )
        val original = PdfPageAnnotation(
            id = "hl-phrase-1",
            type = "HIGHLIGHT",
            pageNumber = 2,
            start = Offset(100f, 200f),
            end = Offset(680f, 312f),
            colorArgb = -256,
            strokeWidth = 2f,
            segments = segs
        )

        val entity = original.toEntity("pdf-att-1")
        val parsed = parsePdfAnnotation(entity)

        assertNotNull(parsed)
        assertEquals(3, parsed!!.segments.size)
        assertEquals(segs, parsed.segments)

        // Hitting inside line 2 returns true
        assertTrue(parsed.containsPoint(Offset(400f, 255f), tolerance = 10f))
        // Empty left margin before line 1 start (where line 1 wasn't highlighted) returns false!
        assertFalse(parsed.containsPoint(Offset(140f, 214f), tolerance = 10f))
        // Empty right margin after line 3 end returns false!
        assertFalse(parsed.containsPoint(Offset(580f, 298f), tolerance = 10f))
    }
}
