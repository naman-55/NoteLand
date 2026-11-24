package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.get
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A single continuous line highlight band in PDF bitmap pixel coordinates.
 */
data class PdfHighlightSegment(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerY: Float get() = (top + bottom) * 0.5f

    fun contains(point: Offset, tolerance: Float = 0f): Boolean {
        val l = min(left, right) - tolerance
        val r = max(left, right) + tolerance
        val t = min(top, bottom) - tolerance
        val b = max(top, bottom) + tolerance
        return point.x in l..r && point.y in t..b
    }
}

/**
 * Detected text line on a PDF page along with word-level spans in reading order.
 */
data class PdfTextLineSpan(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val words: List<PdfHighlightSegment>
) {
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
    val centerY: Float get() = (top + bottom) * 0.5f
}

/**
 * Cached text-line & word geometry for a rendered PDF page bitmap.
 */
data class PdfPageTextLayout(
    val pageWidth: Float,
    val pageHeight: Float,
    val lines: List<PdfTextLineSpan>
)

/**
 * Result of computing a phrase/line-aware text highlight from an anchor point to a focus point.
 */
data class PdfHighlightSelection(
    val segments: List<PdfHighlightSegment>,
    val startCaretTop: Offset? = null,
    val startCaretBottom: Offset? = null,
    val endCaretTop: Offset? = null,
    val endCaretBottom: Offset? = null,
    val snappedToText: Boolean = false
)

/**
 * Extracts text lines and word spans from a rendered PDF page bitmap so highlighting works
 * seamlessly across all Android API levels (24+) and both vector and scanned PDFs.
 */
fun analyzePdfPageBitmap(bitmap: Bitmap): PdfPageTextLayout {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) {
        return PdfPageTextLayout(0f, 0f, emptyList())
    }

    val step = max(1, (min(w, h) + 399) / 650)
    val gw = max(1, (w + step - 1) / step)
    val gh = max(1, (h + step - 1) / step)

    // Estimate page background luminance from margin samples.
    val bgLuma = estimateBackgroundLuminance(bitmap, w, h)
    val inkContrastThreshold = 42

    val inkGrid = BooleanArray(gw * gh)
    val rowInkCounts = IntArray(gh)
    val stripPixels = IntArray(w * step)

    for (gy in 0 until gh) {
        val pyStart = gy * step
        val rowsToRead = min(step, h - pyStart)
        bitmap.getPixels(stripPixels, 0, w, 0, pyStart, w, rowsToRead)

        var rowCount = 0
        val rowOffset = gy * gw
        for (gx in 0 until gw) {
            val pxStart = gx * step
            val pxEnd = min(w, pxStart + step)
            var isInkCell = false
            var dy = 0
            while (dy < rowsToRead && !isInkCell) {
                val base = dy * w
                var px = pxStart
                while (px < pxEnd) {
                    val argb = stripPixels[base + px]
                    val r = (argb ushr 16) and 0xFF
                    val g = (argb ushr 8) and 0xFF
                    val b = argb and 0xFF
                    val luma = (r * 77 + g * 150 + b * 29) ushr 8
                    if (abs(luma - bgLuma) >= inkContrastThreshold) {
                        isInkCell = true
                        break
                    }
                    px++
                }
                dy++
            }
            if (isInkCell) {
                inkGrid[rowOffset + gx] = true
                rowCount++
            }
        }
        rowInkCounts[gy] = rowCount
    }

    // Detect horizontal bands with active ink rows.
    val minRowInk = 2
    val rawBands = mutableListOf<IntRange>()
    var bandStart = -1
    var emptyGap = 0
    val maxVerticalGapCells = if (step == 1) 1 else 0

    for (gy in 0 until gh) {
        if (rowInkCounts[gy] >= minRowInk) {
            if (bandStart < 0) bandStart = gy
            emptyGap = 0
        } else if (bandStart >= 0) {
            emptyGap++
            if (emptyGap > maxVerticalGapCells) {
                rawBands.add(bandStart..(gy - emptyGap))
                bandStart = -1
                emptyGap = 0
            }
        }
    }
    if (bandStart >= 0) {
        rawBands.add(bandStart..(gh - 1 - emptyGap))
    }

    // Refine tall Y-bands by splitting on deep row-ink valleys (e.g., tightly spaced lines).
    val refinedBands = mutableListOf<IntRange>()
    for (band in rawBands) {
        val bandPxH = (band.last - band.first + 1) * step
        if (bandPxH > 46) {
            refinedBands.addAll(splitBandAtInkValleys(band, rowInkCounts, step))
        } else {
            refinedBands.add(band)
        }
    }

    val rawLines = mutableListOf<PdfTextLineSpan>()

    for (band in refinedBands) {
        val bandTopPx = (band.first * step).toFloat()
        val bandBottomPx = min(h, (band.last + 1) * step).toFloat()
        val bandHeight = bandBottomPx - bandTopPx

        // Discard horizontal divider rules and large graphic illustrations.
        if (bandHeight < 9f || bandHeight > 108f) continue

        // Column projection inside this Y-band.
        val colInk = BooleanArray(gw)
        for (gx in 0 until gw) {
            var gy = band.first
            while (gy <= band.last) {
                if (inkGrid[gy * gw + gx]) {
                    colInk[gx] = true
                    break
                }
                gy++
            }
        }

        // Group contiguous X-runs into glyph clusters.
        val xRuns = mutableListOf<IntRange>()
        var runStart = -1
        for (gx in 0 until gw) {
            if (colInk[gx]) {
                if (runStart < 0) runStart = gx
            } else if (runStart >= 0) {
                xRuns.add(runStart until gx)
                runStart = -1
            }
        }
        if (runStart >= 0) xRuns.add(runStart until gw)
        if (xRuns.isEmpty()) continue

        // Separate into column segments first if there is a wide column gap.
        val columnGapThresholdPx = max(bandHeight * 2.1f, 64f)
        val columnGroups = mutableListOf<MutableList<IntRange>>()
        for (run in xRuns) {
            if (columnGroups.isEmpty()) {
                columnGroups.add(mutableListOf(run))
            } else {
                val prevGroup = columnGroups.last()
                val gapPx = (run.first - prevGroup.last().last - 1) * step.toFloat()
                if (gapPx >= columnGapThresholdPx) {
                    columnGroups.add(mutableListOf(run))
                } else {
                    prevGroup.add(run)
                }
            }
        }

        for (colRuns in columnGroups) {
            val colLeftGx = colRuns.first().first
            val colRightGx = colRuns.last().last

            // Tightly fit vertical top/bottom within this column run.
            var tightTopGy = band.last
            var tightBottomGy = band.first
            var totalInkCells = 0
            for (gy in band.first..band.last) {
                var rowHasInkInCol = false
                for (gx in colLeftGx..colRightGx) {
                    if (inkGrid[gy * gw + gx]) {
                        rowHasInkInCol = true
                        totalInkCells++
                    }
                }
                if (rowHasInkInCol) {
                    if (gy < tightTopGy) tightTopGy = gy
                    if (gy > tightBottomGy) tightBottomGy = gy
                }
            }

            val tightHeightPx = ((tightBottomGy - tightTopGy + 1) * step).toFloat()
            val tightWidthPx = ((colRightGx - colLeftGx + 1) * step).toFloat()
            if (tightHeightPx < 8f || tightHeightPx > 105f) continue
            if (tightWidthPx < 6f) continue

            // Filter out solid filled boxes/shapes (very high fill density + large area).
            val cellArea = (tightBottomGy - tightTopGy + 1) * (colRightGx - colLeftGx + 1)
            val fillRatio = totalInkCells.toFloat() / max(1, cellArea)
            if (tightHeightPx > 52f && tightWidthPx > 60f && fillRatio > 0.72f) continue

            val rawTopPx = (tightTopGy * step).toFloat()
            val rawBottomPx = min(h, (tightBottomGy + 1) * step).toFloat()
            val ascPad = max(3f, tightHeightPx * 0.14f)
            val descPad = max(3f, tightHeightPx * 0.16f)
            val lineTop = (rawTopPx - ascPad).coerceAtLeast(0f)
            val lineBottom = (rawBottomPx + descPad).coerceAtMost(h.toFloat())
            val normalizedBottom = max(lineBottom, (lineTop + 20f).coerceAtMost(h.toFloat()))

            // Merge glyph runs separated by intra-word kerning into word spans.
            val wordGapThresholdPx = (tightHeightPx * 0.18f).coerceIn(3.5f, 10f)
            val mergedWords = mutableListOf<IntRange>()
            for (run in colRuns) {
                if (mergedWords.isEmpty()) {
                    mergedWords.add(run)
                } else {
                    val prev = mergedWords.last()
                    val gapPx = (run.first - prev.last - 1) * step.toFloat()
                    if (gapPx < wordGapThresholdPx) {
                        mergedWords[mergedWords.lastIndex] = prev.first..run.last
                    } else {
                        mergedWords.add(run)
                    }
                }
            }

            val wordSegments = mergedWords.mapNotNull { wr ->
                val wl = (wr.first * step - 2f).coerceAtLeast(0f)
                val wrPx = (min(w, (wr.last + 1) * step) + 2f).coerceAtMost(w.toFloat())
                if (wrPx - wl >= 4f) {
                    PdfHighlightSegment(wl, lineTop, wrPx, normalizedBottom)
                } else null
            }

            if (wordSegments.isNotEmpty()) {
                rawLines.add(
                    PdfTextLineSpan(
                        left = wordSegments.first().left,
                        top = lineTop,
                        right = wordSegments.last().right,
                        bottom = normalizedBottom,
                        words = wordSegments
                    )
                )
            }
        }
    }

    val orderedLines = sortLinesInReadingOrder(rawLines, w.toFloat())
    val deOverlapped = clampVerticalLineOverlaps(orderedLines)
    return PdfPageTextLayout(
        pageWidth = w.toFloat(),
        pageHeight = h.toFloat(),
        lines = deOverlapped
    )
}

private fun estimateBackgroundLuminance(bitmap: Bitmap, w: Int, h: Int): Int {
    val xs = intArrayOf(
        4.coerceAtMost(w - 1),
        (w / 4).coerceIn(0, w - 1),
        (w / 2).coerceIn(0, w - 1),
        (w * 3 / 4).coerceIn(0, w - 1),
        (w - 5).coerceAtLeast(0)
    )
    val ys = intArrayOf(
        4.coerceAtMost(h - 1),
        (h / 2).coerceIn(0, h - 1),
        (h - 5).coerceAtLeast(0)
    )
    val samples = ArrayList<Int>(xs.size * ys.size)
    for (y in ys) {
        for (x in xs) {
            val argb = bitmap[x, y]
            val r = (argb ushr 16) and 0xFF
            val g = (argb ushr 8) and 0xFF
            val b = argb and 0xFF
            samples.add((r * 77 + g * 150 + b * 29) ushr 8)
        }
    }
    samples.sort()
    return samples[samples.size / 2]
}

private fun splitBandAtInkValleys(
    band: IntRange,
    rowInkCounts: IntArray,
    step: Int
): List<IntRange> {
    var peak = 0
    for (gy in band) {
        if (rowInkCounts[gy] > peak) peak = rowInkCounts[gy]
    }
    if (peak <= 6) return listOf(band)

    // Only split at genuine internal valleys where ink drops sharply between two denser regions.
    val valleyThreshold = max(2, (peak * 0.10f).toInt())
    val splitPoints = mutableListOf<Int>()
    var inValley = false
    var valleyMinGy = -1
    var valleyMinVal = Int.MAX_VALUE
    var sawHighBefore = false

    for (gy in band) {
        val count = rowInkCounts[gy]
        if (count > valleyThreshold * 2) {
            if (inValley && sawHighBefore && valleyMinGy >= 0) {
                splitPoints.add(valleyMinGy)
            }
            sawHighBefore = true
            inValley = false
            valleyMinGy = -1
            valleyMinVal = Int.MAX_VALUE
        } else if (sawHighBefore && count <= valleyThreshold) {
            inValley = true
            if (count < valleyMinVal) {
                valleyMinVal = count
                valleyMinGy = gy
            }
        }
    }

    if (splitPoints.isEmpty()) return listOf(band)

    val subBands = mutableListOf<IntRange>()
    var start = band.first
    for (sp in splitPoints) {
        if (sp - start >= 4) {
            subBands.add(start until sp)
        }
        start = sp + 1
    }
    if (band.last - start + 1 >= 4) {
        subBands.add(start..band.last)
    }
    return if (subBands.isEmpty()) listOf(band) else subBands
}

private fun sortLinesInReadingOrder(
    lines: List<PdfTextLineSpan>,
    pageWidth: Float
): List<PdfTextLineSpan> {
    if (lines.size <= 1 || pageWidth <= 0f) return lines.sortedWith(compareBy({ it.top }, { it.left }))

    val midX = pageWidth * 0.5f
    val marginTol = pageWidth * 0.03f
    val leftLines = lines.filter { it.right <= midX + marginTol }
    val rightLines = lines.filter { it.left >= midX - marginTol }

    // Confirm genuine vertically-overlapping two-column pairs.
    var overlapPairs = 0
    var colMinY = Float.MAX_VALUE
    var colMaxY = Float.MIN_VALUE
    for (l in leftLines) {
        for (r in rightLines) {
            if (min(l.bottom, r.bottom) > max(l.top, r.top)) {
                overlapPairs++
                colMinY = min(colMinY, min(l.top, r.top))
                colMaxY = max(colMaxY, max(l.bottom, r.bottom))
            }
        }
    }

    if (overlapPairs < 2) {
        return lines.sortedWith(compareBy({ it.top }, { it.left }))
    }

    return lines.sortedWith(
        compareBy<PdfTextLineSpan> { line ->
            when {
                line.bottom <= colMinY + 2f -> 0
                line.top >= colMaxY - 2f -> 3
                line.right <= midX + marginTol -> 1
                line.left >= midX - marginTol -> 2
                else -> 0
            }
        }.thenBy { it.top }.thenBy { it.left }
    )
}

private fun clampVerticalLineOverlaps(lines: List<PdfTextLineSpan>): List<PdfTextLineSpan> {
    if (lines.size <= 1) return lines
    val result = lines.toMutableList()
    for (i in 0 until result.size - 1) {
        val a = result[i]
        val b = result[i + 1]
        val horizontallyAligned = min(a.right, b.right) > max(a.left, b.left)
        if (horizontallyAligned && b.top > a.top && a.bottom > b.top + 1f) {
            val mid = (a.bottom + b.top) * 0.5f
            val newABottom = max(a.top + 12f, mid - 1f)
            val newBTop = min(b.bottom - 12f, mid + 1f)
            result[i] = a.copy(
                bottom = newABottom,
                words = a.words.map { it.copy(bottom = newABottom) }
            )
            result[i + 1] = b.copy(
                top = newBTop,
                words = b.words.map { it.copy(top = newBTop) }
            )
        }
    }
    return result
}

private data class LineWordHit(
    val lineIndex: Int,
    val wordIndex: Int,
    val word: PdfHighlightSegment
)

/**
 * Finds the nearest text line & word to a touch point if within snap tolerance.
 */
private fun findNearestLineWord(
    layout: PdfPageTextLayout,
    point: Offset,
    maxVerticalDistance: Float,
    maxHorizontalDistance: Float
): LineWordHit? {
    if (layout.lines.isEmpty()) return null

    var bestLineIdx = -1
    var bestScore = Float.MAX_VALUE

    for (i in layout.lines.indices) {
        val line = layout.lines[i]
        val vDist = when {
            point.y < line.top -> line.top - point.y
            point.y > line.bottom -> point.y - line.bottom
            else -> 0f
        }
        if (vDist > maxVerticalDistance) continue

        val hDist = when {
            point.x < line.left -> line.left - point.x
            point.x > line.right -> point.x - line.right
            else -> 0f
        }
        if (hDist > maxHorizontalDistance) continue

        val centerYDist = abs(point.y - line.centerY)
        val score = vDist * 3.0f + hDist * 0.6f + centerYDist * 0.25f
        if (score < bestScore) {
            bestScore = score
            bestLineIdx = i
        }
    }

    if (bestLineIdx < 0) return null
    val line = layout.lines[bestLineIdx]
    var bestWordIdx = 0
    var bestWordDist = Float.MAX_VALUE
    for (wIdx in line.words.indices) {
        val word = line.words[wIdx]
        val d = when {
            point.x < word.left -> word.left - point.x
            point.x > word.right -> point.x - word.right
            else -> 0f
        }
        if (d < bestWordDist) {
            bestWordDist = d
            bestWordIdx = wIdx
        }
    }
    return LineWordHit(bestLineIdx, bestWordIdx, line.words[bestWordIdx])
}

/**
 * Finds a word highlight when the user taps directly on a word in Highlight mode.
 */
fun findTappedWordHighlight(
    layout: PdfPageTextLayout?,
    tapPoint: Offset
): PdfHighlightSelection? {
    if (layout == null || layout.lines.isEmpty()) return null
    val hit = findNearestLineWord(
        layout = layout,
        point = tapPoint,
        maxVerticalDistance = 18f,
        maxHorizontalDistance = 24f
    ) ?: return null
    val seg = hit.word
    return PdfHighlightSelection(
        segments = listOf(seg),
        startCaretTop = Offset(seg.left, seg.top),
        startCaretBottom = Offset(seg.left, seg.bottom),
        endCaretTop = Offset(seg.right, seg.top),
        endCaretBottom = Offset(seg.right, seg.bottom),
        snappedToText = true
    )
}

/**
 * Computes line-by-line & phrase-snapped highlight segments from [anchor] to [focus].
 * Supports single-word, intra-line phrase, and multi-line forward or backward drags.
 * When no text exists near the gesture, falls back to a clean horizontal line-strip marker
 * rather than a rectangular box-selection marquee.
 */
fun computeTextHighlightSelection(
    layout: PdfPageTextLayout?,
    anchor: Offset,
    focus: Offset
): PdfHighlightSelection {
    if (layout != null && layout.lines.isNotEmpty()) {
        val anchorHit = findNearestLineWord(
            layout = layout,
            point = anchor,
            maxVerticalDistance = 42f,
            maxHorizontalDistance = 120f
        )

        if (anchorHit != null) {
            val focusHit = findNearestLineWord(
                layout = layout,
                point = focus,
                maxVerticalDistance = 140f,
                maxHorizontalDistance = 220f
            ) ?: anchorHit

            val isForward = if (anchorHit.lineIndex != focusHit.lineIndex) {
                anchorHit.lineIndex < focusHit.lineIndex
            } else {
                anchorHit.wordIndex <= focusHit.wordIndex
            }

            val startHit = if (isForward) anchorHit else focusHit
            val endHit = if (isForward) focusHit else anchorHit

            val segments = mutableListOf<PdfHighlightSegment>()
            if (startHit.lineIndex == endHit.lineIndex) {
                val line = layout.lines[startHit.lineIndex]
                val segLeft = min(startHit.word.left, endHit.word.left)
                val segRight = max(startHit.word.right, endHit.word.right)
                if (segRight - segLeft >= 4f) {
                    segments.add(
                        PdfHighlightSegment(
                            left = segLeft,
                            top = line.top,
                            right = segRight,
                            bottom = line.bottom
                        )
                    )
                }
            } else {
                for (idx in startHit.lineIndex..endHit.lineIndex) {
                    val line = layout.lines[idx]
                    val segLeft = if (idx == startHit.lineIndex) startHit.word.left else line.left
                    val segRight = if (idx == endHit.lineIndex) endHit.word.right else line.right
                    if (segRight - segLeft >= 4f) {
                        segments.add(
                            PdfHighlightSegment(
                                left = segLeft,
                                top = line.top,
                                right = segRight,
                                bottom = line.bottom
                            )
                        )
                    }
                }
            }

            if (segments.isNotEmpty()) {
                val first = segments.first()
                val last = segments.last()
                return PdfHighlightSelection(
                    segments = segments,
                    startCaretTop = Offset(first.left, first.top),
                    startCaretBottom = Offset(first.left, first.bottom),
                    endCaretTop = Offset(last.right, last.top),
                    endCaretBottom = Offset(last.right, last.bottom),
                    snappedToText = true
                )
            }
        }
    }

    // Fallback when dragging over non-text space: draw a neat single-line marker strip, never a big box.
    val stripHalfH = 14f
    val left = min(anchor.x, focus.x)
    val right = max(anchor.x, focus.x)
    val seg = PdfHighlightSegment(
        left = left,
        top = anchor.y - stripHalfH,
        right = max(right, left + 4f),
        bottom = anchor.y + stripHalfH
    )
    return PdfHighlightSelection(
        segments = listOf(seg),
        startCaretTop = Offset(seg.left, seg.top),
        startCaretBottom = Offset(seg.left, seg.bottom),
        endCaretTop = Offset(seg.right, seg.top),
        endCaretBottom = Offset(seg.right, seg.bottom),
        snappedToText = false
    )
}
