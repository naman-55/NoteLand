package com.example.ui.editor.serialization

import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.engine.RichSpanEngine
import com.example.ui.editor.model.CalloutTone
import com.example.ui.editor.model.DividerStyle
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.InlineSpan
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCell
import com.example.ui.editor.model.UnderlineStyle
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Safe, backward-compatible serializer/deserializer for structured RichNoteDocument.
 * Avoids fragile HTML persistence and seamlessly converts legacy Markdown notes on open.
 */
object RichNoteDocumentCodec {

    fun computeContentHash(content: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            bytes.take(8).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            content.hashCode().toString()
        }
    }

    fun encode(document: RichNoteDocument, syncedPlainContent: String? = null): String {
        val root = JSONObject()
        root.put("v", document.version)
        val effectiveHash = syncedPlainContent?.let { computeContentHash(it) } ?: document.sourceContentHash
        if (effectiveHash.isNotBlank()) {
            root.put("h", effectiveHash)
        }
        val blocksArr = JSONArray()
        document.blocks.forEach { block ->
            val bObj = JSONObject()
            bObj.put("id", block.id)
            bObj.put("kind", block.kind.name)
            when (block) {
                is RichBlock.TextBlock -> {
                    bObj.put("text", block.text)
                    if (block.alignment != RichTextAlignment.LEFT) {
                        bObj.put("align", block.alignment.name)
                    }
                    if (block.indentLevel > 0) {
                        bObj.put("indent", block.indentLevel.coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL))
                    }
                    if (block.kind == RichBlockKind.CHECKLIST) {
                        bObj.put("checked", block.checked)
                    }
                    if (block.kind == RichBlockKind.CALLOUT) {
                        bObj.put("tone", block.calloutTone.name)
                    }
                    val normalizedSpans = RichSpanEngine.normalizeSpans(block.spans, block.text.length)
                    if (normalizedSpans.isNotEmpty()) {
                        val spansArr = JSONArray()
                        normalizedSpans.forEach { span ->
                            val sObj = JSONObject()
                            sObj.put("s", span.start)
                            sObj.put("e", span.end)
                            val st = span.style
                            if (st.bold) sObj.put("b", true)
                            if (st.italic) sObj.put("i", true)
                            if (st.strikethrough) sObj.put("st", true)
                            if (st.underline != UnderlineStyle.NONE) sObj.put("u", st.underline.name)
                            st.textColorHex?.let { sObj.put("fg", it) }
                            st.backgroundColorHex?.let { sObj.put("bg", it) }
                            st.highlightColorHex?.let { sObj.put("hl", it) }
                            if (st.inlineCode) sObj.put("code", true)
                            spansArr.put(sObj)
                        }
                        bObj.put("spans", spansArr)
                    }
                }

                is RichBlock.DividerBlock -> {
                    bObj.put("divStyle", block.style.name)
                }

                is RichBlock.TableBlock -> {
                    if (block.title.isNotBlank()) bObj.put("title", block.title)
                    bObj.put("sheet", block.isSpreadsheet)
                    bObj.put("header", block.hasHeaderRow)
                    val rowsArr = JSONArray()
                    block.rows.forEach { row ->
                        val rArr = JSONArray()
                        row.forEach { cell ->
                            val cObj = JSONObject()
                            cObj.put("t", cell.text)
                            if (cell.alignment != RichTextAlignment.LEFT) cObj.put("a", cell.alignment.name)
                            if (cell.isBold) cObj.put("b", true)
                            cell.bgColorHex?.takeIf { it.isNotBlank() }?.let { cObj.put("bg", it) }
                            rArr.put(cObj)
                        }
                        rowsArr.put(rArr)
                    }
                    bObj.put("rows", rowsArr)
                }
            }
            blocksArr.put(bObj)
        }
        root.put("blocks", blocksArr)
        return root.toString()
    }

    fun decode(jsonStr: String): RichNoteDocument? {
        if (jsonStr.isBlank()) return null
        return try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("v", RichNoteDocument.CURRENT_SCHEMA_VERSION)
            val hash = root.optString("h", "")
            val blocksArr = root.optJSONArray("blocks") ?: return null
            val blocks = mutableListOf<RichBlock>()

            for (i in 0 until blocksArr.length()) {
                val bObj = blocksArr.optJSONObject(i) ?: continue
                val id = bObj.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                val kind = runCatching {
                    RichBlockKind.valueOf(bObj.optString("kind", RichBlockKind.PARAGRAPH.name))
                }.getOrDefault(RichBlockKind.PARAGRAPH)

                when (kind) {
                    RichBlockKind.DIVIDER -> {
                        val divStyle = runCatching {
                            DividerStyle.valueOf(bObj.optString("divStyle", DividerStyle.SOLID.name))
                        }.getOrDefault(DividerStyle.SOLID)
                        blocks.add(RichBlock.DividerBlock(id = id, style = divStyle))
                    }

                    RichBlockKind.TABLE, RichBlockKind.SPREADSHEET -> {
                        val title = bObj.optString("title", "")
                        val isSheet = bObj.optBoolean("sheet", kind == RichBlockKind.SPREADSHEET)
                        val hasHeader = bObj.optBoolean("header", !isSheet)
                        val rowsArr = bObj.optJSONArray("rows")
                        val parsedRows = mutableListOf<List<TableCell>>()
                        if (rowsArr != null) {
                            for (r in 0 until rowsArr.length()) {
                                val rArr = rowsArr.optJSONArray(r) ?: continue
                                val cells = mutableListOf<TableCell>()
                                for (c in 0 until rArr.length()) {
                                    val cObj = rArr.optJSONObject(c)
                                    if (cObj != null) {
                                        cells.add(
                                            TableCell(
                                                text = cObj.optString("t", ""),
                                                alignment = runCatching {
                                                    RichTextAlignment.valueOf(cObj.optString("a", RichTextAlignment.LEFT.name))
                                                }.getOrDefault(RichTextAlignment.LEFT),
                                                isBold = cObj.optBoolean("b", false),
                                                bgColorHex = cObj.optString("bg", "").takeIf { it.isNotBlank() }
                                            )
                                        )
                                    } else {
                                        cells.add(TableCell(text = rArr.optString(c, "")))
                                    }
                                }
                                if (cells.isNotEmpty()) parsedRows.add(cells)
                            }
                        }
                        // Normalize column counts safely across rows
                        val maxCols = parsedRows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 2
                        val normalizedRows = if (parsedRows.isEmpty()) {
                            listOf(List(maxCols) { TableCell() })
                        } else {
                            parsedRows.map { row ->
                                if (row.size < maxCols) row + List(maxCols - row.size) { TableCell() } else row
                            }
                        }
                        blocks.add(
                            RichBlock.TableBlock(
                                id = id,
                                title = title,
                                isSpreadsheet = isSheet,
                                hasHeaderRow = hasHeader,
                                rows = normalizedRows
                            )
                        )
                    }

                    else -> {
                        val text = bObj.optString("text", "")
                        val align = runCatching {
                            RichTextAlignment.valueOf(bObj.optString("align", RichTextAlignment.LEFT.name))
                        }.getOrDefault(RichTextAlignment.LEFT)
                        val indent = bObj.optInt("indent", 0).coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL)
                        val checked = bObj.optBoolean("checked", false)
                        val tone = runCatching {
                            CalloutTone.valueOf(bObj.optString("tone", CalloutTone.NOTE.name))
                        }.getOrDefault(CalloutTone.NOTE)
                        val spansArr = bObj.optJSONArray("spans")
                        val spans = mutableListOf<InlineSpan>()
                        if (spansArr != null) {
                            for (j in 0 until spansArr.length()) {
                                val sObj = spansArr.optJSONObject(j) ?: continue
                                val s = sObj.optInt("s", 0).coerceIn(0, text.length)
                                val e = sObj.optInt("e", 0).coerceIn(0, text.length)
                                if (e <= s) continue
                                val underline = runCatching {
                                    UnderlineStyle.valueOf(sObj.optString("u", UnderlineStyle.NONE.name))
                                }.getOrDefault(UnderlineStyle.NONE)
                                val style = InlineFormatStyle(
                                    bold = sObj.optBoolean("b", false),
                                    italic = sObj.optBoolean("i", false),
                                    strikethrough = sObj.optBoolean("st", false),
                                    underline = underline,
                                    textColorHex = sObj.optString("fg", "").takeIf { it.isNotBlank() },
                                    backgroundColorHex = sObj.optString("bg", "").takeIf { it.isNotBlank() },
                                    highlightColorHex = sObj.optString("hl", "").takeIf { it.isNotBlank() },
                                    inlineCode = sObj.optBoolean("code", false)
                                )
                                if (!style.isEmpty()) {
                                    spans.add(InlineSpan(s, e, style))
                                }
                            }
                        }
                        blocks.add(
                            RichBlock.TextBlock(
                                id = id,
                                kind = kind,
                                text = text,
                                spans = RichSpanEngine.normalizeSpans(spans, text.length),
                                alignment = align,
                                indentLevel = indent,
                                checked = checked,
                                calloutTone = tone
                            )
                        )
                    }
                }
            }

            RichNoteDocument(
                version = version,
                sourceContentHash = hash,
                blocks = blocks
            ).ensureNonEmpty()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the authoritative RichNoteDocument when opening a note:
     * - Uses saved structured richDocument JSON when valid and in sync with note.content.
     * - Falls back cleanly to parsing legacy Markdown when opening pre-existing notes
     *   or notes modified outside the rich editor (preventing sync loss).
     */
    fun loadForNote(richDocumentJson: String, plainContent: String): RichNoteDocument {
        val decoded = decode(richDocumentJson)
        if (decoded != null) {
            val currentHash = computeContentHash(plainContent)
            if (decoded.sourceContentHash.isBlank() || decoded.sourceContentHash == currentHash) {
                return decoded
            }
        }
        return fromLegacyMarkdown(plainContent)
    }

    /**
     * Parses legacy Markdown notes into structured RichBlocks + InlineSpans with 100% backward compatibility.
     */
    fun fromLegacyMarkdown(rawContent: String): RichNoteDocument {
        if (rawContent.isEmpty()) {
            return RichNoteDocument.empty().copy(sourceContentHash = computeContentHash(""))
        }
        val lines = rawContent.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val blocks = mutableListOf<RichBlock>()
        val numberedRegex = Regex("^\\d+\\.\\s+(.*)$")
        val dividerRegex = Regex("^([-*_])\\1{2,}$")
        val calloutRegex = Regex("^>\\s*\\[!(NOTE|INFO|TIP|WARNING|IMPORTANT)]\\s*(.*)$", RegexOption.IGNORE_CASE)

        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            // Markdown pipe table detection: | A | B |
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
                val tableLines = mutableListOf<String>()
                var j = i
                while (j < lines.size && lines[j].trim().startsWith("|") && lines[j].trim().endsWith("|")) {
                    tableLines.add(lines[j].trim())
                    j++
                }
                if (tableLines.isNotEmpty()) {
                    val parsedRows = mutableListOf<List<TableCell>>()
                    var hasHeader = false
                    tableLines.forEachIndexed { rowIdx, tLine ->
                        val cellsRaw = tLine.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                        val isSeparator = cellsRaw.all { it.matches(Regex("^:?-{2,}:?$")) }
                        if (isSeparator && rowIdx == 1) {
                            hasHeader = true
                        } else {
                            parsedRows.add(
                                cellsRaw.map { cText ->
                                    TableCell(text = cText, isBold = rowIdx == 0 && tableLines.size > 1)
                                }
                            )
                        }
                    }
                    if (parsedRows.isNotEmpty()) {
                        val maxCols = parsedRows.maxOf { it.size }.coerceAtLeast(1)
                        val normalized = parsedRows.map { r ->
                            if (r.size < maxCols) r + List(maxCols - r.size) { TableCell() } else r
                        }
                        blocks.add(
                            RichBlock.TableBlock(
                                hasHeaderRow = hasHeader,
                                isSpreadsheet = false,
                                rows = normalized
                            )
                        )
                        i = j
                        continue
                    }
                }
            }

            if (dividerRegex.matches(trimmed)) {
                blocks.add(RichBlock.DividerBlock(style = DividerStyle.SOLID))
                i++
                continue
            }

            val leadingSpaces = rawLine.takeWhile { it == ' ' || it == '\t' }
                .sumOf { if (it == '\t') 2 else 1 }
            val indentLevel = (leadingSpaces / 2).coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL)
            val bodyLine = rawLine.trimStart()

            val calloutMatch = calloutRegex.matchEntire(bodyLine)
            val numberedMatch = numberedRegex.matchEntire(bodyLine)

            when {
                calloutMatch != null -> {
                    val toneTag = calloutMatch.groupValues[1].uppercase()
                    val tone = when (toneTag) {
                        "INFO" -> CalloutTone.INFO
                        "TIP" -> CalloutTone.TIP
                        "WARNING", "IMPORTANT" -> CalloutTone.WARNING
                        else -> CalloutTone.NOTE
                    }
                    val (plain, spans) = parseInlineMarkdownToSpans(calloutMatch.groupValues[2])
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.CALLOUT,
                            text = plain,
                            spans = spans,
                            calloutTone = tone,
                            indentLevel = indentLevel
                        )
                    )
                }

                bodyLine.startsWith("### ") -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.removePrefix("### "))
                    blocks.add(RichBlock.TextBlock(kind = RichBlockKind.HEADING_3, text = plain, spans = spans))
                }

                bodyLine.startsWith("## ") -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.removePrefix("## "))
                    blocks.add(RichBlock.TextBlock(kind = RichBlockKind.HEADING_2, text = plain, spans = spans))
                }

                bodyLine.startsWith("# ") -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.removePrefix("# "))
                    blocks.add(RichBlock.TextBlock(kind = RichBlockKind.HEADING_1, text = plain, spans = spans))
                }

                bodyLine.startsWith("> ") -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.removePrefix("> "))
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.BLOCKQUOTE,
                            text = plain,
                            spans = spans,
                            indentLevel = indentLevel
                        )
                    )
                }

                bodyLine.startsWith("- [x] ", ignoreCase = true) -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.drop(6))
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.CHECKLIST,
                            text = plain,
                            spans = spans,
                            checked = true,
                            indentLevel = indentLevel
                        )
                    )
                }

                bodyLine.startsWith("- [ ] ") -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine.drop(6))
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.CHECKLIST,
                            text = plain,
                            spans = spans,
                            checked = false,
                            indentLevel = indentLevel
                        )
                    )
                }

                bodyLine.startsWith("• ") || bodyLine.startsWith("- ") || bodyLine.startsWith("* ") -> {
                    val stripped = when {
                        bodyLine.startsWith("• ") -> bodyLine.removePrefix("• ")
                        bodyLine.startsWith("- ") -> bodyLine.removePrefix("- ")
                        else -> bodyLine.removePrefix("* ")
                    }
                    val (plain, spans) = parseInlineMarkdownToSpans(stripped)
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.BULLET_LIST,
                            text = plain,
                            spans = spans,
                            indentLevel = indentLevel
                        )
                    )
                }

                numberedMatch != null -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(numberedMatch.groupValues[1])
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.NUMBERED_LIST,
                            text = plain,
                            spans = spans,
                            indentLevel = indentLevel
                        )
                    )
                }

                else -> {
                    val (plain, spans) = parseInlineMarkdownToSpans(bodyLine)
                    blocks.add(
                        RichBlock.TextBlock(
                            kind = RichBlockKind.PARAGRAPH,
                            text = plain,
                            spans = spans,
                            indentLevel = indentLevel
                        )
                    )
                }
            }
            i++
        }

        return RichNoteDocument(
            version = RichNoteDocument.CURRENT_SCHEMA_VERSION,
            sourceContentHash = computeContentHash(rawContent),
            blocks = blocks
        ).ensureNonEmpty()
    }

    /**
     * Converts structured RichNoteDocument to clean readable Markdown for NoteEntity.content
     * (enables search, previews, widgets, Knowledge Brain [[backlinks]], and Markdown view).
     */
    fun toPlainMarkdown(document: RichNoteDocument): String {
        val lines = mutableListOf<String>()
        document.blocks.forEachIndexed { idx, block ->
            when (block) {
                is RichBlock.TextBlock -> {
                    val indent = "  ".repeat(block.indentLevel.coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL))
                    val inlineMd = renderInlineMarkdown(block.text, block.spans)
                    val formatted = when (block.kind) {
                        RichBlockKind.HEADING_1 -> "# $inlineMd"
                        RichBlockKind.HEADING_2 -> "## $inlineMd"
                        RichBlockKind.HEADING_3 -> "### $inlineMd"
                        RichBlockKind.BULLET_LIST -> "$indent- $inlineMd"
                        RichBlockKind.NUMBERED_LIST -> {
                            val num = RichDocumentReducer.computeNumberedListLabel(document.blocks, idx)
                            "$indent${if (num.firstOrNull()?.isDigit() == true) num else "1."} $inlineMd"
                        }
                        RichBlockKind.CHECKLIST -> {
                            val box = if (block.checked) "- [x] " else "- [ ] "
                            "$indent$box$inlineMd"
                        }
                        RichBlockKind.BLOCKQUOTE -> "$indent> $inlineMd"
                        RichBlockKind.CALLOUT -> "$indent> [!${block.calloutTone.name}] $inlineMd"
                        else -> "$indent$inlineMd"
                    }
                    lines.add(formatted)
                }

                is RichBlock.DividerBlock -> {
                    lines.add("---")
                }

                is RichBlock.TableBlock -> {
                    if (block.rows.isNotEmpty()) {
                        block.rows.forEachIndexed { rIdx, row ->
                            val rowLine = row.joinToString(" | ", prefix = "| ", postfix = " |") { cell ->
                                val display = if (block.isSpreadsheet) {
                                    RichDocumentReducer.evaluateSpreadsheetDisplay(block.rows, cell.text)
                                } else {
                                    cell.text
                                }
                                display.replace("|", "\\|").replace("\n", " ")
                            }
                            lines.add(rowLine)
                            if (rIdx == 0 && (block.hasHeaderRow || block.rows.size > 1)) {
                                lines.add(row.joinToString(" | ", prefix = "| ", postfix = " |") { "---" })
                            }
                        }
                    }
                }
            }
        }
        return lines.joinToString("\n")
    }

    private fun renderInlineMarkdown(text: String, spans: List<InlineSpan>): String {
        if (text.isEmpty() || spans.isEmpty()) return text
        val normalized = RichSpanEngine.normalizeSpans(spans, text.length)
        if (normalized.isEmpty()) return text

        val sb = StringBuilder()
        var cursor = 0
        for (span in normalized) {
            if (span.start > cursor) {
                sb.append(text.substring(cursor, span.start))
            }
            var segment = text.substring(span.start, span.end)
            val st = span.style
            if (st.inlineCode) segment = "`$segment`"
            if (st.bold) segment = "**$segment**"
            if (st.italic) segment = "*$segment*"
            if (st.strikethrough) segment = "~~$segment~~"
            if (st.underline != UnderlineStyle.NONE) segment = "<u>$segment</u>"
            if (!st.highlightColorHex.isNullOrBlank()) segment = "==$segment=="
            sb.append(segment)
            cursor = span.end
        }
        if (cursor < text.length) {
            sb.append(text.substring(cursor))
        }
        return sb.toString()
    }

    /**
     * Parses inline legacy Markdown (**bold**, *italic*, <u>underline</u>, ~~strike~~, ==highlight==, `code`)
     * into clean plain text + structured InlineSpans.
     */
    fun parseInlineMarkdownToSpans(raw: String): Pair<String, List<InlineSpan>> {
        if (raw.isEmpty()) return "" to emptyList()
        val out = StringBuilder()
        val spans = mutableListOf<InlineSpan>()
        parseInlineRecursive(raw, InlineFormatStyle(), out, spans)
        val plain = out.toString()
        return plain to RichSpanEngine.normalizeSpans(spans, plain.length)
    }

    private fun parseInlineRecursive(
        text: String,
        inherited: InlineFormatStyle,
        out: StringBuilder,
        spans: MutableList<InlineSpan>
    ) {
        var i = 0
        val len = text.length
        while (i < len) {
            // <u>...</u>
            if (text.regionMatches(i, "<u>", 0, 3, ignoreCase = true)) {
                val close = text.indexOf("</u>", i + 3, ignoreCase = true)
                if (close != -1) {
                    val inner = text.substring(i + 3, close)
                    parseInlineRecursive(inner, inherited.copy(underline = UnderlineStyle.STANDARD), out, spans)
                    i = close + 4
                    continue
                }
            }
            // **...**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val close = text.indexOf("**", i + 2)
                if (close > i + 2) {
                    val inner = text.substring(i + 2, close)
                    parseInlineRecursive(inner, inherited.copy(bold = true), out, spans)
                    i = close + 2
                    continue
                }
            }
            // ~~...~~
            if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
                val close = text.indexOf("~~", i + 2)
                if (close > i + 2) {
                    val inner = text.substring(i + 2, close)
                    parseInlineRecursive(inner, inherited.copy(strikethrough = true), out, spans)
                    i = close + 2
                    continue
                }
            }
            // ==...==
            if (i + 1 < len && text[i] == '=' && text[i + 1] == '=') {
                val close = text.indexOf("==", i + 2)
                if (close > i + 2) {
                    val inner = text.substring(i + 2, close)
                    parseInlineRecursive(inner, inherited.copy(highlightColorHex = "#FEF08A"), out, spans)
                    i = close + 2
                    continue
                }
            }
            // `...`
            if (text[i] == '`') {
                val close = text.indexOf('`', i + 1)
                if (close > i + 1) {
                    val startPos = out.length
                    val inner = text.substring(i + 1, close)
                    out.append(inner)
                    val endPos = out.length
                    if (endPos > startPos) {
                        spans.add(InlineSpan(startPos, endPos, inherited.copy(inlineCode = true)))
                    }
                    i = close + 1
                    continue
                }
            }
            // *...*
            if (text[i] == '*') {
                var close = -1
                var scan = i + 1
                while (scan < len) {
                    if (text[scan] == '*' &&
                        (scan + 1 >= len || text[scan + 1] != '*') &&
                        text[scan - 1] != '*'
                    ) {
                        close = scan
                        break
                    }
                    scan++
                }
                if (close > i + 1) {
                    val inner = text.substring(i + 1, close)
                    parseInlineRecursive(inner, inherited.copy(italic = true), out, spans)
                    i = close + 1
                    continue
                }
            }

            val startPos = out.length
            out.append(text[i])
            if (!inherited.isEmpty()) {
                spans.add(InlineSpan(startPos, startPos + 1, inherited))
            }
            i++
        }
    }
}
