package com.example.ui.editor.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.example.ui.editor.model.CalloutTone
import com.example.ui.editor.model.DividerStyle
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.InlineSpan
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCell
import com.example.ui.editor.model.UnderlineStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

object RichSpanEngine {

    const val MAX_INDENT_LEVEL = 4

    /**
     * Normalizes spans into sorted, non-overlapping, clamped segments with adjacent identical styles merged.
     */
    fun normalizeSpans(spans: List<InlineSpan>, textLength: Int): List<InlineSpan> {
        if (textLength <= 0 || spans.isEmpty()) return emptyList()
        val valid = spans.mapNotNull { span ->
            val s = span.start.coerceIn(0, textLength)
            val e = span.end.coerceIn(0, textLength)
            val style = span.style.normalized()
            if (e > s && !style.isEmpty()) InlineSpan(s, e, style) else null
        }
        if (valid.isEmpty()) return emptyList()

        // Collect all unique boundary points to resolve overlaps deterministically (later spans override earlier)
        val boundaries = sortedSetOf<Int>()
        valid.forEach {
            boundaries.add(it.start)
            boundaries.add(it.end)
        }
        val points = boundaries.toList()
        val merged = mutableListOf<InlineSpan>()

        for (i in 0 until points.size - 1) {
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segEnd <= segStart) continue

            // Combine styles of all spans covering [segStart, segEnd)
            var activeStyle: InlineFormatStyle? = null
            for (span in valid) {
                if (span.start <= segStart && span.end >= segEnd) {
                    activeStyle = if (activeStyle == null) {
                        span.style
                    } else {
                        combineStyles(activeStyle, span.style)
                    }
                }
            }
            val finalStyle = activeStyle?.normalized() ?: continue
            if (finalStyle.isEmpty()) continue

            val prev = merged.lastOrNull()
            if (prev != null && prev.end == segStart && prev.style == finalStyle) {
                merged[merged.size - 1] = prev.copy(end = segEnd)
            } else {
                merged.add(InlineSpan(segStart, segEnd, finalStyle))
            }
        }
        return merged
    }

    private fun combineStyles(base: InlineFormatStyle, overlay: InlineFormatStyle): InlineFormatStyle {
        return InlineFormatStyle(
            bold = base.bold || overlay.bold,
            italic = base.italic || overlay.italic,
            strikethrough = base.strikethrough || overlay.strikethrough,
            underline = if (overlay.underline != UnderlineStyle.NONE) overlay.underline else base.underline,
            textColorHex = overlay.textColorHex ?: base.textColorHex,
            backgroundColorHex = overlay.backgroundColorHex ?: base.backgroundColorHex,
            highlightColorHex = overlay.highlightColorHex ?: base.highlightColorHex,
            inlineCode = base.inlineCode || overlay.inlineCode
        )
    }

    /**
     * Adjusts inline spans accurately after any user text insertion, deletion, or replacement.
     */
    fun applyTextDiff(
        oldText: String,
        newText: String,
        oldSpans: List<InlineSpan>,
        typingStyle: InlineFormatStyle? = null
    ): List<InlineSpan> {
        if (oldText == newText) return normalizeSpans(oldSpans, newText.length)
        if (newText.isEmpty()) return emptyList()

        val oldLen = oldText.length
        val newLen = newText.length

        var prefix = 0
        val minLen = minOf(oldLen, newLen)
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }

        var oldSuffix = oldLen
        var newSuffix = newLen
        while (oldSuffix > prefix && newSuffix > prefix && oldText[oldSuffix - 1] == newText[newSuffix - 1]) {
            oldSuffix--
            newSuffix--
        }

        val changeStart = prefix
        val deletedEnd = oldSuffix
        val insertedLen = newSuffix - prefix
        val deletedLen = deletedEnd - changeStart
        val delta = insertedLen - deletedLen

        val adjusted = mutableListOf<InlineSpan>()

        // Preserve existing spans outside the edited region
        for (span in normalizeSpans(oldSpans, oldLen)) {
            when {
                // Entirely before edit
                span.end <= changeStart -> {
                    adjusted.add(span)
                }
                // Entirely after edit
                span.start >= deletedEnd -> {
                    adjusted.add(span.copy(start = span.start + delta, end = span.end + delta))
                }
                // Spans across edit
                else -> {
                    if (span.start < changeStart) {
                        adjusted.add(span.copy(start = span.start, end = changeStart))
                    }
                    if (span.end > deletedEnd) {
                        val afterStart = changeStart + insertedLen
                        val afterEnd = span.end + delta
                        if (afterEnd > afterStart) {
                            adjusted.add(span.copy(start = afterStart, end = afterEnd))
                        }
                    }
                }
            }
        }

        // Apply typingStyle (or inherited style) to newly inserted text
        if (insertedLen > 0) {
            val effectiveInsertStyle = when {
                typingStyle != null -> typingStyle.normalized()
                else -> {
                    // Inherit style from character immediately preceding insertion, or encompassing deleted span
                    val leftNeighbor = oldSpans.firstOrNull { it.start < changeStart && it.end >= changeStart }
                        ?: oldSpans.firstOrNull { it.start <= changeStart && it.end > changeStart }
                    leftNeighbor?.style ?: InlineFormatStyle()
                }
            }
            if (!effectiveInsertStyle.isEmpty()) {
                adjusted.add(InlineSpan(changeStart, changeStart + insertedLen, effectiveInsertStyle))
            }
        }

        return normalizeSpans(adjusted, newLen)
    }

    /**
     * Applies a style transformation over [selStart, selEnd), splitting intersecting spans cleanly
     * without affecting text outside the selection.
     */
    fun applyInlineFormat(
        spans: List<InlineSpan>,
        textLength: Int,
        selStart: Int,
        selEnd: Int,
        transform: (InlineFormatStyle) -> InlineFormatStyle
    ): List<InlineSpan> {
        val start = selStart.coerceIn(0, textLength)
        val end = selEnd.coerceIn(0, textLength)
        if (end <= start) return normalizeSpans(spans, textLength)

        val normalized = normalizeSpans(spans, textLength)
        val result = mutableListOf<InlineSpan>()

        // Keep parts outside [start, end)
        for (span in normalized) {
            if (span.end <= start || span.start >= end) {
                result.add(span)
            } else {
                if (span.start < start) {
                    result.add(span.copy(end = start))
                }
                if (span.end > end) {
                    result.add(span.copy(start = end))
                }
            }
        }

        // Partition [start, end) using existing span boundaries inside it so mixed styles are preserved!
        val cuts = sortedSetOf(start, end)
        for (span in normalized) {
            if (span.start in (start + 1) until end) cuts.add(span.start)
            if (span.end in (start + 1) until end) cuts.add(span.end)
        }
        val pts = cuts.toList()
        for (i in 0 until pts.size - 1) {
            val segStart = pts[i]
            val segEnd = pts[i + 1]
            val existing = normalized.firstOrNull { it.start <= segStart && it.end >= segEnd }?.style
                ?: InlineFormatStyle()
            val updated = transform(existing).normalized()
            if (!updated.isEmpty()) {
                result.add(InlineSpan(segStart, segEnd, updated))
            }
        }

        return normalizeSpans(result, textLength)
    }

    /**
     * Checks if an attribute predicate holds across the entire range [selStart, selEnd).
     */
    fun isAttributeActiveAcrossRange(
        spans: List<InlineSpan>,
        textLength: Int,
        selStart: Int,
        selEnd: Int,
        predicate: (InlineFormatStyle) -> Boolean
    ): Boolean {
        val start = selStart.coerceIn(0, textLength)
        val end = selEnd.coerceIn(0, textLength)
        if (end <= start) return false
        val normalized = normalizeSpans(spans, textLength)
        var cursor = start
        for (span in normalized) {
            if (span.end <= cursor) continue
            if (span.start > cursor) {
                // Gap with default empty style
                if (!predicate(InlineFormatStyle())) return false
                cursor = minOf(span.start, end)
            }
            if (cursor >= end) break
            val coverEnd = minOf(span.end, end)
            if (coverEnd > cursor && !predicate(span.style)) return false
            cursor = coverEnd
            if (cursor >= end) break
        }
        if (cursor < end && !predicate(InlineFormatStyle())) return false
        return true
    }

    fun toggleBold(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int): List<InlineSpan> {
        val allBold = isAttributeActiveAcrossRange(spans, textLength, start, end) { it.bold }
        return applyInlineFormat(spans, textLength, start, end) { it.copy(bold = !allBold) }
    }

    fun toggleItalic(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int): List<InlineSpan> {
        val allItalic = isAttributeActiveAcrossRange(spans, textLength, start, end) { it.italic }
        return applyInlineFormat(spans, textLength, start, end) { it.copy(italic = !allItalic) }
    }

    fun toggleStrikethrough(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int): List<InlineSpan> {
        val allStrike = isAttributeActiveAcrossRange(spans, textLength, start, end) { it.strikethrough }
        return applyInlineFormat(spans, textLength, start, end) { it.copy(strikethrough = !allStrike) }
    }

    fun toggleUnderline(
        spans: List<InlineSpan>,
        textLength: Int,
        start: Int,
        end: Int,
        target: UnderlineStyle
    ): List<InlineSpan> {
        val allSame = isAttributeActiveAcrossRange(spans, textLength, start, end) { it.underline == target }
        val next = if (allSame) UnderlineStyle.NONE else target
        return applyInlineFormat(spans, textLength, start, end) { it.copy(underline = next) }
    }

    fun setTextColor(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int, hex: String?): List<InlineSpan> {
        return applyInlineFormat(spans, textLength, start, end) { it.copy(textColorHex = hex) }
    }

    fun setBackgroundColor(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int, hex: String?): List<InlineSpan> {
        return applyInlineFormat(spans, textLength, start, end) { it.copy(backgroundColorHex = hex) }
    }

    fun setHighlightColor(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int, hex: String?): List<InlineSpan> {
        return applyInlineFormat(spans, textLength, start, end) { it.copy(highlightColorHex = hex) }
    }

    fun toggleInlineCode(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int): List<InlineSpan> {
        val allCode = isAttributeActiveAcrossRange(spans, textLength, start, end) { it.inlineCode }
        return applyInlineFormat(spans, textLength, start, end) { it.copy(inlineCode = !allCode) }
    }

    fun clearInlineFormatting(spans: List<InlineSpan>, textLength: Int, start: Int, end: Int): List<InlineSpan> {
        return applyInlineFormat(spans, textLength, start, end) { InlineFormatStyle() }
    }

    /**
     * Resolves the effective InlineFormatStyle at a collapsed cursor or across a selection.
     */
    fun resolveActiveStyleAt(
        spans: List<InlineSpan>,
        textLength: Int,
        selStart: Int,
        selEnd: Int
    ): InlineFormatStyle {
        val start = selStart.coerceIn(0, textLength)
        val end = selEnd.coerceIn(0, textLength)
        val normalized = normalizeSpans(spans, textLength)
        if (start == end) {
            if (textLength == 0) return InlineFormatStyle()
            // Prefer character right before cursor, else character at cursor
            val probe = if (start > 0) start - 1 else 0
            return normalized.firstOrNull { probe >= it.start && probe < it.end }?.style
                ?: InlineFormatStyle()
        }
        val firstSpan = normalized.firstOrNull { it.end > start && it.start < end }?.style
        return InlineFormatStyle(
            bold = isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.bold },
            italic = isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.italic },
            strikethrough = isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.strikethrough },
            underline = when {
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.WAVY } -> UnderlineStyle.WAVY
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.DOUBLE } -> UnderlineStyle.DOUBLE
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.DOUBLE_WAVY } -> UnderlineStyle.DOUBLE_WAVY
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.DOTTED } -> UnderlineStyle.DOTTED
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.DASHED } -> UnderlineStyle.DASHED
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.DASH_DOT } -> UnderlineStyle.DASH_DOT
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.underline == UnderlineStyle.STANDARD } -> UnderlineStyle.STANDARD
                else -> UnderlineStyle.NONE
            },
            textColorHex = firstSpan?.textColorHex?.takeIf { hex ->
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.textColorHex.equals(hex, ignoreCase = true) }
            },
            backgroundColorHex = firstSpan?.backgroundColorHex?.takeIf { hex ->
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.backgroundColorHex.equals(hex, ignoreCase = true) }
            },
            highlightColorHex = firstSpan?.highlightColorHex?.takeIf { hex ->
                isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.highlightColorHex.equals(hex, ignoreCase = true) }
            },
            inlineCode = isAttributeActiveAcrossRange(normalized, textLength, start, end) { it.inlineCode }
        )
    }

    /**
     * Splits spans cleanly into (leftSpans, rightSpans) at [splitOffset] when splitting a block on Enter.
     */
    fun splitSpansAt(spans: List<InlineSpan>, textLength: Int, splitOffset: Int): Pair<List<InlineSpan>, List<InlineSpan>> {
        val offset = splitOffset.coerceIn(0, textLength)
        val normalized = normalizeSpans(spans, textLength)
        val left = mutableListOf<InlineSpan>()
        val right = mutableListOf<InlineSpan>()
        for (span in normalized) {
            if (span.end <= offset) {
                left.add(span)
            } else if (span.start >= offset) {
                right.add(span.copy(start = span.start - offset, end = span.end - offset))
            } else {
                left.add(span.copy(end = offset))
                right.add(span.copy(start = 0, end = span.end - offset))
            }
        }
        return normalizeSpans(left, offset) to normalizeSpans(right, textLength - offset)
    }

    /**
     * Merges two span lists when joining two blocks together (e.g., Backspace at start of block).
     */
    fun mergeSpans(
        leftSpans: List<InlineSpan>,
        leftLength: Int,
        rightSpans: List<InlineSpan>,
        rightLength: Int
    ): List<InlineSpan> {
        val shiftedRight = rightSpans.map { it.copy(start = it.start + leftLength, end = it.end + leftLength) }
        return normalizeSpans(leftSpans + shiftedRight, leftLength + rightLength)
    }

    /**
     * Builds a rich Compose AnnotatedString with 1:1 character offsets for live editing and viewing.
     */
    fun buildStyledAnnotatedString(
        text: String,
        spans: List<InlineSpan>,
        isDark: Boolean,
        codeBg: Color,
        linkColor: Color
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val normalized = normalizeSpans(spans, text.length)
        return buildAnnotatedString {
            append(text)
            for (span in normalized) {
                val st = span.style
                val decorations = mutableListOf<TextDecoration>()
                if (st.underline != UnderlineStyle.NONE && st.underline != UnderlineStyle.WAVY && st.underline != UnderlineStyle.DOUBLE_WAVY) decorations.add(TextDecoration.Underline)
                if (st.strikethrough) decorations.add(TextDecoration.LineThrough)

                val resolvedHighlight = EditorColorPalette.resolveHighlightColor(st.highlightColorHex, isDark)
                val resolvedBg = EditorColorPalette.resolveBackgroundColor(st.backgroundColorHex, isDark)
                val effectiveBg = resolvedHighlight ?: resolvedBg ?: if (st.inlineCode) codeBg else Color.Unspecified
                val resolvedFg = EditorColorPalette.resolveTextColor(st.textColorHex, isDark) ?: Color.Unspecified

                addStyle(
                    style = SpanStyle(
                        fontWeight = if (st.bold) FontWeight.Bold else null,
                        fontStyle = if (st.italic) FontStyle.Italic else null,
                        textDecoration = when {
                            decorations.isEmpty() -> null
                            decorations.size == 1 -> decorations.first()
                            else -> TextDecoration.combine(decorations)
                        },
                        color = resolvedFg,
                        background = effectiveBg,
                        fontFamily = if (st.inlineCode) FontFamily.Monospace else null
                    ),
                    start = span.start,
                    end = span.end
                )
            }

            // Highlight [[wiki-links]] subtly while keeping exact 1:1 offset mapping
            val wikiRegex = Regex("\\[\\[([^\\[\\]\n]+)]]")
            wikiRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        background = linkColor.copy(alpha = 0.12f),
                        textDecoration = TextDecoration.Underline
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}

data class DocumentEditResult(
    val document: RichNoteDocument,
    val focusBlockId: String? = null,
    val focusCursorOffset: Int? = null
)

object RichDocumentReducer {

    fun updateTextBlock(
        document: RichNoteDocument,
        blockId: String,
        transform: (RichBlock.TextBlock) -> RichBlock.TextBlock
    ): RichNoteDocument {
        val updated = document.blocks.map { block ->
            if (block is RichBlock.TextBlock && block.id == blockId) transform(block) else block
        }
        return document.copy(blocks = updated).ensureNonEmpty()
    }

    fun setBlockKind(
        document: RichNoteDocument,
        blockId: String,
        targetKind: RichBlockKind
    ): RichNoteDocument {
        return updateTextBlock(document, blockId) { block ->
            val nextKind = if (block.kind == targetKind && targetKind != RichBlockKind.PARAGRAPH) {
                RichBlockKind.PARAGRAPH
            } else {
                targetKind
            }
            block.copy(
                kind = nextKind,
                checked = if (nextKind == RichBlockKind.CHECKLIST) block.checked else false
            )
        }
    }

    fun setAlignment(
        document: RichNoteDocument,
        blockId: String,
        alignment: RichTextAlignment
    ): RichNoteDocument {
        return updateTextBlock(document, blockId) { it.copy(alignment = alignment) }
    }

    fun increaseIndent(document: RichNoteDocument, blockId: String): RichNoteDocument {
        return updateTextBlock(document, blockId) {
            it.copy(indentLevel = (it.indentLevel + 1).coerceAtMost(RichSpanEngine.MAX_INDENT_LEVEL))
        }
    }

    fun decreaseIndent(document: RichNoteDocument, blockId: String): RichNoteDocument {
        return updateTextBlock(document, blockId) {
            it.copy(indentLevel = (it.indentLevel - 1).coerceAtLeast(0))
        }
    }

    fun toggleChecklist(document: RichNoteDocument, blockId: String): RichNoteDocument {
        return updateTextBlock(document, blockId) { it.copy(checked = !it.checked) }
    }

    fun cycleCalloutTone(document: RichNoteDocument, blockId: String): RichNoteDocument {
        val tones = CalloutTone.entries
        return updateTextBlock(document, blockId) { block ->
            val nextIdx = (tones.indexOf(block.calloutTone) + 1) % tones.size
            block.copy(calloutTone = tones[nextIdx])
        }
    }

    /**
     * Handles Enter key cleanly: continues lists/checklists with matching indent,
     * outdents/exits empty list items, and splits rich paragraphs preserving inline spans on both halves.
     */
    fun splitBlockOnEnter(
        document: RichNoteDocument,
        blockId: String,
        cursorOffset: Int
    ): DocumentEditResult {
        val idx = document.blocks.indexOfFirst { it.id == blockId }
        if (idx == -1) return DocumentEditResult(document)
        val block = document.blocks[idx] as? RichBlock.TextBlock ?: return DocumentEditResult(document)

        val isListKind = block.kind == RichBlockKind.BULLET_LIST ||
            block.kind == RichBlockKind.NUMBERED_LIST ||
            block.kind == RichBlockKind.CHECKLIST

        // Pressing Enter on an empty list item decreases indent or exits list mode
        if (isListKind && block.text.isBlank()) {
            val nextBlock = if (block.indentLevel > 0) {
                block.copy(indentLevel = block.indentLevel - 1)
            } else {
                block.copy(kind = RichBlockKind.PARAGRAPH, checked = false)
            }
            val updated = document.blocks.toMutableList().apply { set(idx, nextBlock) }
            return DocumentEditResult(document.copy(blocks = updated), nextBlock.id, 0)
        }

        val safeCursor = cursorOffset.coerceIn(0, block.text.length)
        val leftText = block.text.substring(0, safeCursor)
        val rightText = block.text.substring(safeCursor)
        val (leftSpans, rightSpans) = RichSpanEngine.splitSpansAt(block.spans, block.text.length, safeCursor)

        val nextKind = when (block.kind) {
            RichBlockKind.HEADING_1, RichBlockKind.HEADING_2, RichBlockKind.HEADING_3 -> RichBlockKind.PARAGRAPH
            RichBlockKind.BLOCKQUOTE, RichBlockKind.CALLOUT -> if (rightText.isEmpty() && leftText.isBlank()) RichBlockKind.PARAGRAPH else block.kind
            else -> block.kind
        }

        val updatedFirst = block.copy(text = leftText, spans = leftSpans)
        val createdSecond = RichBlock.TextBlock(
            id = UUID.randomUUID().toString(),
            kind = nextKind,
            text = rightText,
            spans = rightSpans,
            alignment = block.alignment,
            indentLevel = if (nextKind == RichBlockKind.PARAGRAPH && !isListKind) 0 else block.indentLevel,
            checked = false,
            calloutTone = block.calloutTone
        )

        val updatedBlocks = document.blocks.toMutableList().apply {
            set(idx, updatedFirst)
            add(idx + 1, createdSecond)
        }
        return DocumentEditResult(
            document = document.copy(blocks = updatedBlocks),
            focusBlockId = createdSecond.id,
            focusCursorOffset = 0
        )
    }

    /**
     * Handles Backspace at start of a block: outdents, converts special blocks to paragraph, or merges with previous text block.
     */
    fun handleBackspaceAtStart(document: RichNoteDocument, blockId: String): DocumentEditResult {
        val idx = document.blocks.indexOfFirst { it.id == blockId }
        if (idx == -1) return DocumentEditResult(document)
        val block = document.blocks[idx] as? RichBlock.TextBlock ?: return DocumentEditResult(document)

        if (block.indentLevel > 0) {
            val updated = decreaseIndent(document, blockId)
            return DocumentEditResult(updated, blockId, 0)
        }
        if (block.kind != RichBlockKind.PARAGRAPH) {
            val updated = updateTextBlock(document, blockId) {
                it.copy(kind = RichBlockKind.PARAGRAPH, checked = false)
            }
            return DocumentEditResult(updated, blockId, 0)
        }
        if (idx > 0) {
            val prev = document.blocks[idx - 1]
            if (prev is RichBlock.TextBlock) {
                val mergeOffset = prev.text.length
                val mergedText = prev.text + block.text
                val mergedSpans = RichSpanEngine.mergeSpans(
                    leftSpans = prev.spans,
                    leftLength = prev.text.length,
                    rightSpans = block.spans,
                    rightLength = block.text.length
                )
                val mergedPrev = prev.copy(text = mergedText, spans = mergedSpans)
                val updatedBlocks = document.blocks.toMutableList().apply {
                    set(idx - 1, mergedPrev)
                    removeAt(idx)
                }
                return DocumentEditResult(
                    document = document.copy(blocks = updatedBlocks),
                    focusBlockId = mergedPrev.id,
                    focusCursorOffset = mergeOffset
                )
            } else if (prev is RichBlock.DividerBlock) {
                val updatedBlocks = document.blocks.toMutableList().apply { removeAt(idx - 1) }
                return DocumentEditResult(
                    document = document.copy(blocks = updatedBlocks),
                    focusBlockId = blockId,
                    focusCursorOffset = 0
                )
            }
        }
        return DocumentEditResult(document, blockId, 0)
    }

    fun insertBlockAfter(
        document: RichNoteDocument,
        afterBlockId: String?,
        newBlock: RichBlock,
        ensureTrailingParagraph: Boolean = false
    ): DocumentEditResult {
        val mutable = document.blocks.toMutableList()
        val insertIndex = if (afterBlockId != null) {
            val found = mutable.indexOfFirst { it.id == afterBlockId }
            if (found >= 0) found + 1 else mutable.size
        } else {
            mutable.size
        }
        mutable.add(insertIndex, newBlock)

        var focusId = if (newBlock is RichBlock.TextBlock) newBlock.id else null
        if (ensureTrailingParagraph && (insertIndex == mutable.lastIndex || mutable.getOrNull(insertIndex + 1) !is RichBlock.TextBlock)) {
            val trailing = RichBlock.TextBlock()
            mutable.add(insertIndex + 1, trailing)
            if (focusId == null) focusId = trailing.id
        }
        return DocumentEditResult(
            document = document.copy(blocks = mutable).ensureNonEmpty(),
            focusBlockId = focusId,
            focusCursorOffset = 0
        )
    }

    fun deleteBlock(document: RichNoteDocument, blockId: String): RichNoteDocument {
        val remaining = document.blocks.filterNot { it.id == blockId }
        return document.copy(blocks = remaining).ensureNonEmpty()
    }

    fun createDefaultTable(
        isSpreadsheet: Boolean = false,
        rows: Int = 3,
        cols: Int = 3
    ): RichBlock.TableBlock {
        val safeRows = rows.coerceAtLeast(1)
        val safeCols = cols.coerceAtLeast(1)
        val grid = List(safeRows) { r ->
            List(safeCols) { c ->
                if (!isSpreadsheet && r == 0) {
                    TableCell(text = "Column ${c + 1}", isBold = true)
                } else {
                    TableCell(text = "")
                }
            }
        }
        return RichBlock.TableBlock(
            id = UUID.randomUUID().toString(),
            title = if (isSpreadsheet) "Sheet" else "",
            isSpreadsheet = isSpreadsheet,
            hasHeaderRow = !isSpreadsheet,
            rows = grid
        )
    }

    fun updateTableBlock(
        document: RichNoteDocument,
        blockId: String,
        transform: (RichBlock.TableBlock) -> RichBlock.TableBlock
    ): RichNoteDocument {
        val updated = document.blocks.map { block ->
            if (block is RichBlock.TableBlock && block.id == blockId) transform(block) else block
        }
        return document.copy(blocks = updated)
    }

    fun updateTableCell(
        document: RichNoteDocument,
        blockId: String,
        rowIndex: Int,
        colIndex: Int,
        transform: (TableCell) -> TableCell
    ): RichNoteDocument {
        return updateTableBlock(document, blockId) { table ->
            if (rowIndex !in table.rows.indices) return@updateTableBlock table
            val updatedRows = table.rows.mapIndexed { r, row ->
                if (r != rowIndex) row else row.mapIndexed { c, cell ->
                    if (c == colIndex) transform(cell) else cell
                }
            }
            table.copy(rows = updatedRows)
        }
    }

    fun insertTableRow(document: RichNoteDocument, blockId: String, atIndex: Int): RichNoteDocument {
        return updateTableBlock(document, blockId) { table ->
            val cols = table.colCount.coerceAtLeast(1)
            val newRow = List(cols) { TableCell() }
            val mutable = table.rows.toMutableList()
            mutable.add(atIndex.coerceIn(0, mutable.size), newRow)
            table.copy(rows = mutable)
        }
    }

    fun deleteTableRow(document: RichNoteDocument, blockId: String, rowIndex: Int): RichNoteDocument {
        return updateTableBlock(document, blockId) { table ->
            if (table.rows.size <= 1 || rowIndex !in table.rows.indices) return@updateTableBlock table
            val mutable = table.rows.toMutableList().apply { removeAt(rowIndex) }
            table.copy(rows = mutable)
        }
    }

    fun insertTableColumn(document: RichNoteDocument, blockId: String, atIndex: Int): RichNoteDocument {
        return updateTableBlock(document, blockId) { table ->
            val safeIndex = atIndex.coerceIn(0, table.colCount)
            val updatedRows = table.rows.mapIndexed { r, row ->
                val mutable = row.toMutableList()
                val cell = if (table.hasHeaderRow && !table.isSpreadsheet && r == 0) {
                    TableCell(text = "Col ${safeIndex + 1}", isBold = true)
                } else {
                    TableCell()
                }
                mutable.add(safeIndex.coerceIn(0, mutable.size), cell)
                mutable
            }
            table.copy(rows = updatedRows)
        }
    }

    fun deleteTableColumn(document: RichNoteDocument, blockId: String, colIndex: Int): RichNoteDocument {
        return updateTableBlock(document, blockId) { table ->
            if (table.colCount <= 1) return@updateTableBlock table
            val updatedRows = table.rows.map { row ->
                if (colIndex in row.indices && row.size > 1) {
                    row.toMutableList().apply { removeAt(colIndex) }
                } else {
                    row
                }
            }
            table.copy(rows = updatedRows)
        }
    }

    fun toggleTableHeaderRow(document: RichNoteDocument, blockId: String): RichNoteDocument {
        return updateTableBlock(document, blockId) { it.copy(hasHeaderRow = !it.hasHeaderRow) }
    }

    fun toggleSpreadsheetMode(document: RichNoteDocument, blockId: String): RichNoteDocument {
        return updateTableBlock(document, blockId) { it.copy(isSpreadsheet = !it.isSpreadsheet) }
    }

    /**
     * Computes visual marker for nested bullet lists or hierarchical numbered lists.
     */
    fun computeBulletGlyph(indentLevel: Int): String = when (indentLevel.coerceAtLeast(0)) {
        0 -> "•"
        1 -> "◦"
        2 -> "▪"
        else -> "▸"
    }

    fun computeNumberedListLabel(blocks: List<RichBlock>, index: Int): String {
        val current = blocks.getOrNull(index) as? RichBlock.TextBlock ?: return "1."
        if (current.kind != RichBlockKind.NUMBERED_LIST) return "1."
        val level = current.indentLevel
        var count = 1
        var i = index - 1
        while (i >= 0) {
            val prev = blocks[i]
            if (prev is RichBlock.TextBlock && prev.kind == RichBlockKind.NUMBERED_LIST) {
                if (prev.indentLevel == level) {
                    count++
                } else if (prev.indentLevel < level) {
                    break
                }
            } else {
                break
            }
            i--
        }
        return when (level % 3) {
            0 -> "$count."
            1 -> "${('a' + ((count - 1) % 26))}."
            else -> "${toLowerRoman(count)}."
        }
    }

    private fun toLowerRoman(number: Int): String {
        if (number <= 0 || number > 39) return number.toString()
        val pairs = listOf(10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i")
        var n = number
        val sb = StringBuilder()
        for ((v, sym) in pairs) {
            while (n >= v) {
                sb.append(sym)
                n -= v
            }
        }
        return sb.toString()
    }

    fun columnIndexToLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    /**
     * Lightweight offline spreadsheet formula evaluator supporting:
     * =SUM(A1:A5), =AVG(A1:B2), =MIN(...), =MAX(...), =COUNT(...), and simple cell arithmetic (=A1+B1, =A1*2).
     */
    fun evaluateSpreadsheetDisplay(
        rows: List<List<TableCell>>,
        rawText: String,
        depth: Int = 0
    ): String {
        val trimmed = rawText.trim()
        if (!trimmed.startsWith("=") || depth > 5) return rawText
        val expr = trimmed.removePrefix("=").trim().uppercase(Locale.US)
        if (expr.isEmpty()) return rawText

        return try {
            val funcMatch = Regex("^(SUM|AVG|AVERAGE|MIN|MAX|COUNT)\\(([^)]+)\\)$").matchEntire(expr)
            if (funcMatch != null) {
                val fn = funcMatch.groupValues[1]
                val args = funcMatch.groupValues[2].split(",").flatMap { token ->
                    expandCellRange(rows, token.trim(), depth + 1)
                }
                val result = when (fn) {
                    "SUM" -> args.sum()
                    "AVG", "AVERAGE" -> if (args.isEmpty()) 0.0 else args.average()
                    "MIN" -> args.minOrNull() ?: 0.0
                    "MAX" -> args.maxOrNull() ?: 0.0
                    "COUNT" -> args.size.toDouble()
                    else -> return rawText
                }
                formatNumber(result)
            } else {
                // Basic binary/chained expression with cell references like A1+B1 or A1*10
                val substituted = Regex("[A-Z]+[0-9]+").replace(expr) { m ->
                    val v = resolveSingleCellNumeric(rows, m.value, depth + 1) ?: 0.0
                    v.toString()
                }
                val evaluated = evalSimpleArithmetic(substituted) ?: return rawText
                formatNumber(evaluated)
            }
        } catch (_: Exception) {
            "#ERR"
        }
    }

    private fun expandCellRange(rows: List<List<TableCell>>, token: String, depth: Int): List<Double> {
        if (token.contains(":")) {
            val parts = token.split(":")
            if (parts.size != 2) return emptyList()
            val start = parseCellRef(parts[0]) ?: return emptyList()
            val end = parseCellRef(parts[1]) ?: return emptyList()
            val rMin = minOf(start.first, end.first)
            val rMax = maxOf(start.first, end.first)
            val cMin = minOf(start.second, end.second)
            val cMax = maxOf(start.second, end.second)
            val nums = mutableListOf<Double>()
            for (r in rMin..rMax) {
                for (c in cMin..cMax) {
                    val raw = rows.getOrNull(r)?.getOrNull(c)?.text ?: continue
                    val valueStr = evaluateSpreadsheetDisplay(rows, raw, depth)
                    valueStr.replace(",", "").toDoubleOrNull()?.let { nums.add(it) }
                }
            }
            return nums
        } else {
            val v = resolveSingleCellNumeric(rows, token, depth)
            return if (v != null) listOf(v) else emptyList()
        }
    }

    private fun resolveSingleCellNumeric(rows: List<List<TableCell>>, ref: String, depth: Int): Double? {
        ref.toDoubleOrNull()?.let { return it }
        val (r, c) = parseCellRef(ref) ?: return null
        val raw = rows.getOrNull(r)?.getOrNull(c)?.text ?: return null
        val display = evaluateSpreadsheetDisplay(rows, raw, depth)
        return display.replace(",", "").toDoubleOrNull()
    }

    private fun parseCellRef(ref: String): Pair<Int, Int>? {
        val m = Regex("^([A-Z]+)([0-9]+)$").matchEntire(ref.trim()) ?: return null
        val letters = m.groupValues[1]
        val rowNum = m.groupValues[2].toIntOrNull() ?: return null
        if (rowNum <= 0) return null
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (rowNum - 1) to (col - 1)
    }

    private fun evalSimpleArithmetic(expr: String): Double? {
        val clean = expr.replace(" ", "")
        if (clean.isEmpty()) return null
        // Split by + and - keeping operators, then evaluate * and /
        val tokens = mutableListOf<String>()
        var i = 0
        val buf = StringBuilder()
        while (i < clean.length) {
            val ch = clean[i]
            if ((ch == '+' || ch == '-' || ch == '*' || ch == '/') && buf.isNotEmpty()) {
                tokens.add(buf.toString())
                buf.clear()
                tokens.add(ch.toString())
            } else {
                buf.append(ch)
            }
            i++
        }
        if (buf.isNotEmpty()) tokens.add(buf.toString())
        if (tokens.isEmpty()) return null

        // First pass: * and /
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val t = tokens[idx]
            if ((t == "*" || t == "/") && pass1.isNotEmpty() && idx + 1 < tokens.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: return null
                val right = tokens[idx + 1].toDoubleOrNull() ?: return null
                val res = if (t == "*") left * right else {
                    if (abs(right) < 1e-12) throw ArithmeticException("Divide by zero")
                    left / right
                }
                pass1.add(res.toString())
                idx += 2
            } else {
                pass1.add(t)
                idx++
            }
        }
        // Second pass: + and -
        var acc = pass1.first().toDoubleOrNull() ?: return null
        idx = 1
        while (idx + 1 < pass1.size) {
            val op = pass1[idx]
            val rhs = pass1[idx + 1].toDoubleOrNull() ?: return null
            acc = when (op) {
                "+" -> acc + rhs
                "-" -> acc - rhs
                else -> return null
            }
            idx += 2
        }
        return acc
    }

    private fun formatNumber(value: Double): String {
        return if (abs(value - value.toLong().toDouble()) < 1e-9) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
