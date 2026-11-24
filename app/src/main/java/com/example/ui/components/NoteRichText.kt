package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Identity-offset VisualTransformation for the note editor so T (Headings), B (Bold),
 * I (Italic), U (Underline), Strikethrough, Highlight, Code, Quotes, and Links render
 * live in Edit Mode while preserving 1:1 cursor, selection, and click-to-position accuracy.
 */
class MarkdownVisualTransformation(
    private val baseFontSizeSp: Float,
    private val accentColor: Color,
    private val markerColor: Color,
    private val highlightBg: Color,
    private val codeBg: Color,
    private val linkColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val styled = buildAnnotatedString {
            append(raw)

            val markerStyle = SpanStyle(
                color = markerColor,
                fontSize = (baseFontSizeSp * 0.78f).coerceAtLeast(10f).sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                textDecoration = TextDecoration.None
            )

            // 1. Line-level styles (Headings & Quotes)
            var lineStart = 0
            while (lineStart <= raw.length) {
                val nextNewline = raw.indexOf('\n', lineStart)
                val lineEnd = if (nextNewline == -1) raw.length else nextNewline
                val line = raw.substring(lineStart, lineEnd)

                when {
                    line.startsWith("### ") -> {
                        addStyle(markerStyle, lineStart, lineStart + 4)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = (baseFontSizeSp * 1.15f).sp
                            ),
                            lineStart + 4,
                            lineEnd
                        )
                    }
                    line.startsWith("## ") -> {
                        addStyle(markerStyle, lineStart, lineStart + 3)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = (baseFontSizeSp * 1.28f).sp
                            ),
                            lineStart + 3,
                            lineEnd
                        )
                    }
                    line.startsWith("# ") -> {
                        addStyle(markerStyle, lineStart, lineStart + 2)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = (baseFontSizeSp * 1.46f).sp
                            ),
                            lineStart + 2,
                            lineEnd
                        )
                    }
                    line.startsWith("> ") -> {
                        addStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            ),
                            lineStart,
                            lineStart + 2
                        )
                        addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = markerColor.copy(alpha = 0.95f)
                            ),
                            lineStart + 2,
                            lineEnd
                        )
                    }
                    line.startsWith("- [x] ", ignoreCase = true) || line.startsWith("- [ ] ") -> {
                        addStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            ),
                            lineStart,
                            (lineStart + 6).coerceAtMost(lineEnd)
                        )
                    }
                    line.startsWith("• ") || line.startsWith("- ") -> {
                        addStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            ),
                            lineStart,
                            (lineStart + 2).coerceAtMost(lineEnd)
                        )
                    }
                }

                if (nextNewline == -1) break
                lineStart = nextNewline + 1
            }

            // 2. Inline Bold: **...** (balanced per-line so empty **** or trailing-space markers never bleed onto non-bold text)
            var boldSearchStart = 0
            while (boldSearchStart <= raw.length) {
                val lineEndRaw = raw.indexOf('\n', boldSearchStart)
                val lineEnd = if (lineEndRaw == -1) raw.length else lineEndRaw
                var cursor = boldSearchStart
                while (cursor + 1 < lineEnd) {
                    val openIdx = raw.indexOf("**", cursor)
                    if (openIdx == -1 || openIdx + 1 >= lineEnd) break

                    // Skip empty **** marker runs so they never pair with later **
                    if (openIdx + 3 < lineEnd && raw[openIdx + 2] == '*' && raw[openIdx + 3] == '*') {
                        addStyle(markerStyle, openIdx, openIdx + 4)
                        cursor = openIdx + 4
                        continue
                    }

                    val closeSearchFrom = if (openIdx + 2 < lineEnd && raw[openIdx + 2] == '*') {
                        openIdx + 3
                    } else {
                        openIdx + 2
                    }
                    val closeIdx = raw.indexOf("**", closeSearchFrom)
                    if (closeIdx == -1 || closeIdx + 1 >= lineEnd) {
                        cursor = openIdx + 2
                        continue
                    }

                    if (closeIdx > openIdx + 2 &&
                        !raw[openIdx + 2].isWhitespace() &&
                        !raw[closeIdx - 1].isWhitespace()
                    ) {
                        addStyle(markerStyle, openIdx, openIdx + 2)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black
                            ),
                            openIdx + 2,
                            closeIdx
                        )
                        addStyle(markerStyle, closeIdx, closeIdx + 2)
                    }
                    cursor = closeIdx + 2
                }
                if (lineEndRaw == -1) break
                boldSearchStart = lineEndRaw + 1
            }

            // 3. Inline Italic: *...* (not **)
            val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(?=\\S)(.+?)(?<=\\S)(?<!\\*)\\*(?!\\*)")
            italicRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(markerStyle, start, start + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start + 1, end - 1)
                addStyle(markerStyle, end - 1, end)
            }

            // 4. Inline Underline: <u>...</u>
            val underlineRegex = Regex("<u>(.*?)</u>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            underlineRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                if (end - start >= 7) {
                    addStyle(markerStyle, start, start + 3)
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start + 3, end - 4)
                    addStyle(markerStyle, end - 4, end)
                }
            }

            // 5. Strikethrough: ~~...~~
            val strikeRegex = Regex("~~(?=\\S)(.+?)(?<=\\S)~~")
            strikeRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(markerStyle, start, start + 2)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start + 2, end - 2)
                addStyle(markerStyle, end - 2, end)
            }

            // 6. Highlight: ==...==
            val highlightRegex = Regex("==(?=\\S)(.+?)(?<=\\S)==")
            highlightRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(markerStyle, start, start + 2)
                addStyle(SpanStyle(background = highlightBg), start + 2, end - 2)
                addStyle(markerStyle, end - 2, end)
            }

            // 7. Inline Code: `...`
            val codeRegex = Regex("`([^`\n]+)`")
            codeRegex.findAll(raw).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(markerStyle, start, start + 1)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg
                    ),
                    start + 1,
                    end - 1
                )
                addStyle(markerStyle, end - 1, end)
            }

            // 8. Wiki Links: [[Target]], [[Target|Alias]], [[Target#Anchor]]
            val wikiRegex = Regex("\\[\\[([^\\[\\]\n]+)]]")
            wikiRegex.findAll(raw).forEach { match ->
                val innerRange = match.groups[1]?.range ?: return@forEach
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(markerStyle, start, start + 2)
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        background = linkColor.copy(alpha = 0.12f),
                        textDecoration = TextDecoration.Underline
                    ),
                    innerRange.first,
                    innerRange.last + 1
                )
                addStyle(markerStyle, end - 2, end)
            }

            // 9. Markdown Links: [label](url)
            val linkRegex = Regex("(?<!\\[)\\[([^\\]\n]+)]\\(([^)\n]+)\\)")
            linkRegex.findAll(raw).forEach { match ->
                val labelGroup = match.groups[1] ?: return@forEach
                val urlStart = labelGroup.range.last + 2 // ](
                val fullEnd = match.range.last + 1
                addStyle(markerStyle, match.range.first, match.range.first + 1)
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    labelGroup.range.first,
                    labelGroup.range.last + 1
                )
                addStyle(markerStyle, urlStart, fullEnd)
            }
        }

        return TransformedText(styled, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MarkdownVisualTransformation) return false
        return baseFontSizeSp == other.baseFontSizeSp &&
            accentColor == other.accentColor &&
            markerColor == other.markerColor &&
            highlightBg == other.highlightBg &&
            codeBg == other.codeBg &&
            linkColor == other.linkColor
    }

    override fun hashCode(): Int {
        var result = baseFontSizeSp.hashCode()
        result = 31 * result + accentColor.hashCode()
        result = 31 * result + markerColor.hashCode()
        result = 31 * result + highlightBg.hashCode()
        result = 31 * result + codeBg.hashCode()
        result = 31 * result + linkColor.hashCode()
        return result
    }
}

/**
 * Parses inline markdown + <u> underline tags into a clean AnnotatedString for View Mode,
 * stripping syntax delimiters and preserving clickable links and nested styles.
 */
fun parseRichMarkdownInline(
    raw: String,
    linkColor: Color,
    highlightBg: Color,
    codeBg: Color,
    onWikiLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
    if (raw.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        appendInlineMarkdown(
            builder = this,
            text = raw,
            linkColor = linkColor,
            highlightBg = highlightBg,
            codeBg = codeBg,
            onWikiLinkClick = onWikiLinkClick
        )
    }
}

private fun appendInlineMarkdown(
    builder: AnnotatedString.Builder,
    text: String,
    linkColor: Color,
    highlightBg: Color,
    codeBg: Color,
    onWikiLinkClick: ((String) -> Unit)? = null
) {
    var i = 0
    val len = text.length
    while (i < len) {
        // 0. Wiki Link [[Note Name]] / [[Note Name|Alias]] / [[Note Name#Section]]
        if (i + 1 < len && text[i] == '[' && text[i + 1] == '[') {
            val closeDouble = text.indexOf("]]", i + 2)
            val newlineIdx = text.indexOf('\n', i + 2)
            if (closeDouble != -1 && (newlineIdx == -1 || closeDouble < newlineIdx)) {
                val inner = text.substring(i + 2, closeDouble).trim()
                if (inner.isNotBlank()) {
                    val pipeIdx = inner.indexOf('|')
                    val leftPart = (if (pipeIdx >= 0) inner.substring(0, pipeIdx) else inner).trim()
                    val aliasPart = (if (pipeIdx >= 0) inner.substring(pipeIdx + 1) else "").trim()
                    val hashIdx = leftPart.indexOf('#')
                    val targetTitle = (if (hashIdx >= 0) leftPart.substring(0, hashIdx) else leftPart).trim()
                    val displayLabel = when {
                        aliasPart.isNotBlank() -> aliasPart
                        hashIdx >= 0 && leftPart.substring(hashIdx + 1).isNotBlank() ->
                            "$targetTitle · ${leftPart.substring(hashIdx + 1).trim()}"
                        else -> targetTitle
                    }.ifBlank { inner }

                    val wikiSpanStyle = SpanStyle(
                        color = linkColor,
                        background = linkColor.copy(alpha = 0.14f),
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                    val wikiLinkStyle = TextLinkStyles(style = wikiSpanStyle)
                    if (targetTitle.isNotBlank() && onWikiLinkClick != null) {
                        builder.pushLink(
                            LinkAnnotation.Clickable(
                                tag = "wiki:$targetTitle",
                                styles = wikiLinkStyle,
                                linkInteractionListener = { onWikiLinkClick(targetTitle) }
                            )
                        )
                        builder.append(displayLabel)
                        builder.pop()
                    } else {
                        builder.pushStyle(wikiSpanStyle)
                        builder.append(displayLabel)
                        builder.pop()
                    }
                    i = closeDouble + 2
                    continue
                }
            }
        }

        // 1. Markdown Link [label](url)
        if (text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val label = text.substring(i + 1, closeBracket)
                    val url = text.substring(closeBracket + 2, closeParen).trim()
                    val linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (url.isNotEmpty()) {
                        builder.pushLink(LinkAnnotation.Url(url = url, styles = linkStyle))
                        appendInlineMarkdown(builder, label, linkColor, highlightBg, codeBg, onWikiLinkClick)
                        builder.pop()
                    } else {
                        appendInlineMarkdown(builder, label, linkColor, highlightBg, codeBg, onWikiLinkClick)
                    }
                    i = closeParen + 1
                    continue
                }
            }
        }

        // 2. Underline <u>...</u>
        if (text.regionMatches(i, "<u>", 0, 3, ignoreCase = true)) {
            val closeIdx = text.indexOf("</u>", i + 3, ignoreCase = true)
            if (closeIdx != -1) {
                val inner = text.substring(i + 3, closeIdx)
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                builder.pop()
                i = closeIdx + 4
                continue
            }
        }

        // 3. Bold **...**
        if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
            // Skip empty **** marker runs so they never steal a subsequent ** opening
            if (i + 3 < len && text[i + 2] == '*' && text[i + 3] == '*') {
                i += 4
                continue
            }
            val searchFrom = if (i + 2 < len && text[i + 2] == '*') i + 3 else i + 2
            val closeIdx = text.indexOf("**", searchFrom)
            if (closeIdx > i + 2) {
                val newlineBeforeClose = text.indexOf('\n', i + 2)
                if (newlineBeforeClose == -1 || newlineBeforeClose > closeIdx) {
                    val inner = text.substring(i + 2, closeIdx)
                    val isTrimmed = inner.isNotBlank() &&
                        !inner.first().isWhitespace() &&
                        !inner.last().isWhitespace()
                    if (isTrimmed) {
                        builder.pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black
                            )
                        )
                        appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                        builder.pop()
                        i = closeIdx + 2
                        continue
                    } else if (inner.isNotEmpty()) {
                        appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                        i = closeIdx + 2
                        continue
                    }
                }
            }
        }

        // 4. Strikethrough ~~...~~
        if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
            val closeIdx = text.indexOf("~~", i + 2)
            if (closeIdx > i + 2) {
                val inner = text.substring(i + 2, closeIdx)
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                builder.pop()
                i = closeIdx + 2
                continue
            }
        }

        // 5. Highlight ==...==
        if (i + 1 < len && text[i] == '=' && text[i + 1] == '=') {
            val closeIdx = text.indexOf("==", i + 2)
            if (closeIdx > i + 2) {
                val inner = text.substring(i + 2, closeIdx)
                builder.pushStyle(SpanStyle(background = highlightBg))
                appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                builder.pop()
                i = closeIdx + 2
                continue
            }
        }

        // 6. Inline code `...`
        if (text[i] == '`') {
            val closeIdx = text.indexOf('`', i + 1)
            if (closeIdx > i + 1) {
                val inner = text.substring(i + 1, closeIdx)
                builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg))
                builder.append(inner)
                builder.pop()
                i = closeIdx + 1
                continue
            }
        }

        // 7. Italic *...* (single asterisk)
        if (text[i] == '*') {
            val closeIdx = findSingleAsteriskClose(text, i + 1)
            if (closeIdx > i + 1) {
                val inner = text.substring(i + 1, closeIdx)
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInlineMarkdown(builder, inner, linkColor, highlightBg, codeBg, onWikiLinkClick)
                builder.pop()
                i = closeIdx + 1
                continue
            }
        }

        builder.append(text[i])
        i++
    }
}

private fun findSingleAsteriskClose(text: String, fromIndex: Int): Int {
    var idx = fromIndex
    while (idx < text.length) {
        if (text[idx] == '*') {
            val isDouble = (idx + 1 < text.length && text[idx + 1] == '*') ||
                (idx > 0 && text[idx - 1] == '*')
            if (!isDouble) return idx
        }
        idx++
    }
    return -1
}

/**
 * Applies heading, line-prefix, or inline formatting cleanly while preserving
 * relative cursor/selection offsets and allowing toggling on/off.
 */
fun applyEditorialFormatting(
    current: TextFieldValue,
    prefix: String,
    suffix: String
): TextFieldValue {
    val text = current.text
    val start = current.selection.min.coerceIn(0, text.length)
    val end = current.selection.max.coerceIn(0, text.length)

    // 1. Heading prefixes ("# ", "## ", "### ") operate at the start of the current line
    if (prefix in listOf("# ", "## ", "### ") && suffix.isEmpty()) {
        val lastNewlineBefore = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
        val lineStart = if (start == 0 || lastNewlineBefore == -1) 0 else lastNewlineBefore + 1
        val lineEndRaw = text.indexOf('\n', lineStart)
        val lineEnd = if (lineEndRaw == -1) text.length else lineEndRaw
        val lineText = text.substring(lineStart, lineEnd)

        val existingPrefix = when {
            lineText.startsWith("### ") -> "### "
            lineText.startsWith("## ") -> "## "
            lineText.startsWith("# ") -> "# "
            else -> ""
        }

        val replacementPrefix = if (existingPrefix == prefix) "" else prefix
        val newText = text.replaceRange(lineStart, lineStart + existingPrefix.length, replacementPrefix)
        val delta = replacementPrefix.length - existingPrefix.length
        val newStart = (start + delta).coerceIn(lineStart, newText.length)
        val newEnd = (end + delta).coerceIn(lineStart, newText.length)
        return TextFieldValue(
            text = newText,
            selection = TextRange(newStart, newEnd)
        )
    }

    // 2. Block/line prefixes ("\n• ", "\n1. ", "\n- [ ] ", "\n> ")
    val isLinePrefix = prefix in listOf("\n• ", "\n1. ", "\n- [ ] ", "\n> ") && suffix.isEmpty()
    if (isLinePrefix) {
        val cleanPrefix = prefix.removePrefix("\n")
        val lastNewlineBefore = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
        val lineStart = if (start == 0 || lastNewlineBefore == -1) 0 else lastNewlineBefore + 1
        val lineEndRaw = text.indexOf('\n', lineStart)
        val lineEnd = if (lineEndRaw == -1) text.length else lineEndRaw
        val currentLine = text.substring(lineStart, lineEnd)

        // If cursor is on an empty line or already at line start, toggle prefix cleanly on this line
        if (start == lineStart || currentLine.startsWith(cleanPrefix)) {
            return if (currentLine.startsWith(cleanPrefix)) {
                val newText = text.replaceRange(lineStart, lineStart + cleanPrefix.length, "")
                val newPos = (start - cleanPrefix.length).coerceIn(lineStart, newText.length)
                TextFieldValue(text = newText, selection = TextRange(newPos))
            } else {
                val newText = text.replaceRange(lineStart, lineStart, cleanPrefix)
                val newPos = (start + cleanPrefix.length).coerceIn(lineStart, newText.length)
                TextFieldValue(text = newText, selection = TextRange(newPos))
            }
        }

        val newText = text.replaceRange(start, end, prefix)
        return TextFieldValue(
            text = newText,
            selection = TextRange(start + prefix.length)
        )
    }

    // 3. Inline paired markers (**...**, *...*, <u>...</u>, ~~...~~, ==...==, `...`)
    if (suffix.isNotEmpty()) {
        val selectedText = text.substring(start, end)
        if (start != end) {
            // Case A: selection itself starts & ends with prefix/suffix
            if (selectedText.startsWith(prefix) &&
                selectedText.endsWith(suffix) &&
                selectedText.length >= prefix.length + suffix.length
            ) {
                val unwrapped = selectedText.removePrefix(prefix).removeSuffix(suffix)
                val newText = text.replaceRange(start, end, unwrapped)
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(start, start + unwrapped.length)
                )
            }

            // Case B: selection is immediately surrounded by prefix...suffix
            if (start >= prefix.length &&
                end + suffix.length <= text.length &&
                text.substring(start - prefix.length, start) == prefix &&
                text.substring(end, end + suffix.length) == suffix
            ) {
                val newText = text.replaceRange(start - prefix.length, end + suffix.length, selectedText)
                val newStart = start - prefix.length
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(newStart, newStart + selectedText.length)
                )
            }

            // Case C: trim leading/trailing whitespace outside inline markers so bold/italic stay valid
            val leadingWs = selectedText.indexOfFirst { !it.isWhitespace() }.let { if (it == -1) 0 else it }
            val trailingWs = if (selectedText.isBlank()) 0 else selectedText.length - 1 - selectedText.indexOfLast { !it.isWhitespace() }
            if (selectedText.isNotBlank() && (leadingWs > 0 || trailingWs > 0)) {
                val coreStart = start + leadingWs
                val coreEnd = end - trailingWs
                val core = text.substring(coreStart, coreEnd)
                if (coreStart >= prefix.length &&
                    coreEnd + suffix.length <= text.length &&
                    text.substring(coreStart - prefix.length, coreStart) == prefix &&
                    text.substring(coreEnd, coreEnd + suffix.length) == suffix
                ) {
                    val newText = text.replaceRange(coreStart - prefix.length, coreEnd + suffix.length, core)
                    val newCoreStart = coreStart - prefix.length
                    return TextFieldValue(
                        text = newText,
                        selection = TextRange(newCoreStart, newCoreStart + core.length)
                    )
                }
                val wrappedCore = "$prefix$core$suffix"
                val newText = text.replaceRange(coreStart, coreEnd, wrappedCore)
                val innerStart = coreStart + prefix.length
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(innerStart, innerStart + core.length)
                )
            }

            // Case D: wrap selected text and keep inner text selected so styles can stack
            val wrapped = "$prefix$selectedText$suffix"
            val newText = text.replaceRange(start, end, wrapped)
            val innerStart = start + prefix.length
            return TextFieldValue(
                text = newText,
                selection = TextRange(innerStart, innerStart + selectedText.length)
            )
        } else {
            val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
                .let { if (start == 0 || it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }

            // Collapsed cursor right before suffix (e.g. **word|** or **word |** or **|**)
            if (start + suffix.length <= text.length && text.substring(start, start + suffix.length) == suffix) {
                var wsStart = start
                while (wsStart > lineStart && text[wsStart - 1].isWhitespace()) {
                    wsStart--
                }
                if (wsStart < start && wsStart > lineStart) {
                    val openIdx = findEnclosingOpenMarkerOnLine(text, lineStart, wsStart, prefix, suffix)
                    if (openIdx != -1 && openIdx + prefix.length < wsStart) {
                        val trailingSpaces = text.substring(wsStart, start)
                        val newText = text.replaceRange(wsStart, start + suffix.length, "$suffix$trailingSpaces")
                        return TextFieldValue(
                            text = newText,
                            selection = TextRange(wsStart + suffix.length + trailingSpaces.length)
                        )
                    }
                }
                return TextFieldValue(
                    text = text,
                    selection = TextRange(start + suffix.length)
                )
            }

            // Collapsed cursor inside an active prefix...suffix span on the same line: unwrap to toggle off
            val openIdx = findEnclosingOpenMarkerOnLine(text, lineStart, start, prefix, suffix)
            if (openIdx != -1) {
                val closeIdx = findEnclosingCloseMarkerOnLine(text, start, lineEnd, prefix, suffix)
                if (closeIdx != -1 && openIdx + prefix.length <= closeIdx) {
                    val inner = text.substring(openIdx + prefix.length, closeIdx)
                    val newText = text.replaceRange(openIdx, closeIdx + suffix.length, inner)
                    val newCursor = (start - prefix.length).coerceIn(openIdx, openIdx + inner.length)
                    return TextFieldValue(
                        text = newText,
                        selection = TextRange(newCursor)
                    )
                }
            }

            val inserted = "$prefix$suffix"
            val newText = text.replaceRange(start, end, inserted)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length)
            )
        }
    }

    // Fallback plain insertion
    val newText = text.replaceRange(start, end, prefix)
    return TextFieldValue(
        text = newText,
        selection = TextRange(start + prefix.length)
    )
}

data class ActiveEditorialStyles(
    val isHeading: Boolean = false,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

fun detectActiveEditorialStyles(text: String, selection: TextRange): ActiveEditorialStyles {
    if (text.isEmpty()) return ActiveEditorialStyles()
    val cursor = selection.min.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        .let { if (cursor == 0 || it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val isHeading = line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")
    val isBold = isCursorInsideInlinePair(text, lineStart, lineEnd, cursor, "**", "**")
    val isItalic = isCursorInsideSingleAsterisk(text, lineStart, lineEnd, cursor)
    val isUnderline = isCursorInsideInlinePair(text, lineStart, lineEnd, cursor, "<u>", "</u>")

    return ActiveEditorialStyles(
        isHeading = isHeading,
        isBold = isBold,
        isItalic = isItalic,
        isUnderline = isUnderline
    )
}

private fun isCursorInsideInlinePair(
    text: String,
    lineStart: Int,
    lineEnd: Int,
    cursor: Int,
    prefix: String,
    suffix: String
): Boolean {
    val openIdx = findEnclosingOpenMarkerOnLine(text, lineStart, cursor, prefix, suffix)
    if (openIdx == -1) return false
    val closeIdx = findEnclosingCloseMarkerOnLine(text, cursor, lineEnd, prefix, suffix)
    return closeIdx != -1 && cursor >= openIdx + prefix.length && cursor <= closeIdx
}

private fun isCursorInsideSingleAsterisk(
    text: String,
    lineStart: Int,
    lineEnd: Int,
    cursor: Int
): Boolean {
    var open = -1
    var i = lineStart
    while (i < lineEnd) {
        if (text[i] == '*') {
            val isDouble = (i + 1 < lineEnd && text[i + 1] == '*') || (i > lineStart && text[i - 1] == '*')
            if (!isDouble) {
                if (open == -1) {
                    open = i
                } else {
                    if (cursor in (open + 1)..i) return true
                    open = -1
                }
            }
        }
        i++
    }
    return false
}

private fun findEnclosingOpenMarkerOnLine(
    text: String,
    lineStart: Int,
    cursor: Int,
    prefix: String,
    suffix: String
): Int {
    val safeCursor = cursor.coerceIn(lineStart, text.length)
    if (prefix == "*" && suffix == "*") {
        var openIdx = -1
        var i = lineStart
        while (i < safeCursor) {
            if (text[i] == '*') {
                val isDouble = (i + 1 < safeCursor && text[i + 1] == '*') ||
                    (i + 1 < text.length && text[i + 1] == '*') ||
                    (i > lineStart && text[i - 1] == '*')
                if (!isDouble) {
                    openIdx = if (openIdx == -1) i else -1
                }
            }
            i++
        }
        return openIdx
    }
    if (prefix == suffix) {
        var openIdx = -1
        var i = lineStart
        while (i + prefix.length <= safeCursor) {
            if (text.regionMatches(i, prefix, 0, prefix.length, ignoreCase = false)) {
                openIdx = if (openIdx == -1) i else -1
                i += prefix.length
                continue
            }
            i++
        }
        return openIdx
    } else {
        val lastOpen = text.lastIndexOf(prefix, (safeCursor - prefix.length).coerceAtLeast(lineStart))
        if (lastOpen < lineStart) return -1
        val intermediateClose = text.lastIndexOf(suffix, (safeCursor - suffix.length).coerceAtLeast(lineStart))
        return if (intermediateClose >= lastOpen) -1 else lastOpen
    }
}

private fun findEnclosingCloseMarkerOnLine(
    text: String,
    cursor: Int,
    lineEnd: Int,
    prefix: String,
    suffix: String
): Int {
    if (prefix == "*" && suffix == "*") {
        var i = cursor.coerceAtLeast(0)
        while (i < lineEnd) {
            if (text[i] == '*') {
                val isDouble = (i + 1 < lineEnd && text[i + 1] == '*') || (i > 0 && text[i - 1] == '*')
                if (!isDouble) return i
            }
            i++
        }
        return -1
    }
    val idx = text.indexOf(suffix, cursor.coerceAtLeast(0))
    if (idx == -1 || idx + suffix.length > lineEnd) return -1
    if (prefix != suffix) {
        val intermediateOpen = text.indexOf(prefix, cursor.coerceAtLeast(0))
        if (intermediateOpen != -1 && intermediateOpen < idx) return -1
    }
    return idx
}
