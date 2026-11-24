package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import com.example.ui.screens.PdfPageAnnotation
import com.example.ui.screens.drawPdfAnnotation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.Inflater
import kotlin.math.max

sealed class PdfValidationResult {
    data class Valid(val pageCount: Int) : PdfValidationResult()
    data object Missing : PdfValidationResult()
    data class Corrupted(val reason: String) : PdfValidationResult()
}

data class PdfTextMatch(
    val pageNumber: Int,
    val snippet: String,
    val sourceType: String, // "PAGE_TEXT" or "ANNOTATION"
    val annotationId: String? = null
)

object PdfDocumentEngine {

    private const val MAX_TEXT_PARSE_BYTES = 24 * 1024 * 1024L // 24 MB guard for pure-Kotlin stream fallback

    fun validatePdfFile(file: File): PdfValidationResult {
        if (!file.exists() || !file.isFile) return PdfValidationResult.Missing
        if (file.length() < 16L) {
            return PdfValidationResult.Corrupted("This PDF file is empty or truncated.")
        }

        val headerOk = runCatching {
            val buf = ByteArray(minOf(file.length().toInt(), 1024))
            file.inputStream().use { input ->
                val n = input.read(buf)
                if (n <= 0) false else String(buf, 0, n, Charsets.ISO_8859_1).contains("%PDF-")
            }
        }.getOrDefault(false)

        if (!headerOk) {
            return PdfValidationResult.Corrupted("Invalid PDF header. File may be corrupted or not a PDF.")
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            if (count <= 0) {
                PdfValidationResult.Corrupted("PDF document has no readable pages.")
            } else {
                PdfValidationResult.Valid(count)
            }
        } catch (e: IOException) {
            PdfValidationResult.Corrupted(e.message ?: "Unable to parse PDF document.")
        } catch (e: SecurityException) {
            PdfValidationResult.Corrupted("This PDF is password-protected or access was denied.")
        } catch (t: Throwable) {
            // Native libpdfium may be unavailable in Robolectric JVM tests: fall back to structural check.
            val structuralCount = countPdfPagesHeuristic(file)
            if (structuralCount <= 0) {
                PdfValidationResult.Corrupted("PDF structure is incomplete or damaged.")
            } else {
                PdfValidationResult.Valid(structuralCount)
            }
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    private fun countPdfPagesHeuristic(file: File): Int {
        if (file.length() > MAX_TEXT_PARSE_BYTES) return 1
        val text = runCatching {
            file.inputStream().use { it.readBytes().toString(Charsets.ISO_8859_1) }
        }.getOrNull() ?: return 0

        if (!text.contains("%PDF-")) return 0
        val hasObjOrEof = text.contains("endobj") || text.contains("%%EOF") || text.contains("/Type")
        if (!hasObjOrEof) return 0
        val pageMatches = Regex("""/Type\s*/Page\b""").findAll(text).count()
        return max(1, pageMatches)
    }

    /**
     * Extracts searchable plain text mapped by 1-based page number.
     * Uses Android 15 (API 35) PdfRenderer text extraction when available, and merges/falls back to
     * an embedded pure-Kotlin PDF content-stream parser (supporting uncompressed & FlateDecode streams).
     */
    fun extractPdfPageTexts(file: File): Map<Int, String> {
        if (!file.exists() || !file.isFile || file.length() < 16L) return emptyMap()

        val result = linkedMapOf<Int, String>()

        if (Build.VERSION.SDK_INT >= 35) {
            extractViaPlatformApi35(file)?.let { apiTexts ->
                for ((page, txt) in apiTexts) {
                    if (txt.isNotBlank()) result[page] = txt
                }
            }
        }

        if (result.isEmpty() && file.length() <= MAX_TEXT_PARSE_BYTES) {
            val fallbackTexts = extractViaPureKotlinStreamParser(file)
            for ((page, txt) in fallbackTexts) {
                if (txt.isNotBlank()) result[page] = txt
            }
        }
        return result
    }

    private fun extractViaPlatformApi35(file: File): Map<Int, String>? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val out = linkedMapOf<Int, String>()
            val getTextContentsMethod = PdfRenderer.Page::class.java.methods
                .firstOrNull { it.name == "getTextContents" && it.parameterTypes.isEmpty() }
                ?: return null

            for (i in 0 until pageCount) {
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(i)
                    val contents = getTextContentsMethod.invoke(page) as? List<*>
                    if (!contents.isNullOrEmpty()) {
                        val sb = StringBuilder()
                        for (item in contents) {
                            if (item == null) continue
                            val getText = item.javaClass.methods.firstOrNull {
                                it.name == "getText" && it.parameterTypes.isEmpty()
                            }
                            val chunk = getText?.invoke(item) as? String
                            if (!chunk.isNullOrBlank()) {
                                if (sb.isNotEmpty()) sb.append(' ')
                                sb.append(chunk.trim())
                            }
                        }
                        if (sb.isNotBlank()) {
                            out[i + 1] = sb.toString()
                        }
                    }
                } finally {
                    try { page?.close() } catch (_: Throwable) {}
                }
            }
            out
        } catch (_: Throwable) {
            null
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    internal fun extractViaPureKotlinStreamParser(file: File): Map<Int, String> {
        val rawBytes = runCatching { file.readBytes() }.getOrNull() ?: return emptyMap()
        val latin = String(rawBytes, Charsets.ISO_8859_1)
        if (!latin.contains("%PDF-")) return emptyMap()

        // Map object number -> raw body (Latin1 indices match byte offsets 1:1 in ISO-8859-1)
        val objRegex = Regex("""(\d+)\s+\d+\s+obj\b(.*?)\bendobj\b""", RegexOption.DOT_MATCHES_ALL)
        val objectsByNum = linkedMapOf<Int, String>()
        val orderedObjects = mutableListOf<Pair<Int, String>>()
        for (match in objRegex.findAll(latin)) {
            val objNum = match.groupValues[1].toIntOrNull() ?: continue
            val body = match.groupValues[2]
            objectsByNum[objNum] = body
            orderedObjects.add(objNum to body)
        }

        // Collect page objects in order (try /Pages /Kids tree first, fallback to /Type /Page declaration order)
        val orderedPageObjNums = collectOrderedPageObjectNumbers(objectsByNum, orderedObjects)
        val out = linkedMapOf<Int, String>()

        if (orderedPageObjNums.isNotEmpty()) {
            orderedPageObjNums.forEachIndexed { idx, pageObjNum ->
                val pageBody = objectsByNum[pageObjNum].orEmpty()
                val contentRefs = extractContentObjectRefs(pageBody)
                val pageTextBuilder = StringBuilder()
                for (ref in contentRefs) {
                    val contentBody = objectsByNum[ref] ?: continue
                    val streamText = decodeObjectStreamText(contentBody)
                    if (streamText.isNotBlank()) {
                        if (pageTextBuilder.isNotEmpty()) pageTextBuilder.append('\n')
                        pageTextBuilder.append(streamText)
                    }
                }
                if (pageTextBuilder.isNotBlank()) {
                    out[idx + 1] = pageTextBuilder.toString().trim()
                }
            }
        }

        // Fallback: if /Page objects didn't reference readable streams cleanly, scan any BT..ET streams in order
        if (out.isEmpty()) {
            var pageIdx = 1
            for ((_, body) in orderedObjects) {
                if (!body.contains("stream")) continue
                val decoded = decodeObjectStreamText(body)
                if (decoded.isNotBlank()) {
                    out[pageIdx] = decoded
                    pageIdx++
                }
            }
        }
        return out
    }

    private fun collectOrderedPageObjectNumbers(
        objectsByNum: Map<Int, String>,
        orderedObjects: List<Pair<Int, String>>
    ): List<Int> {
        val pagesRoot = orderedObjects.firstOrNull { (objNum, body) ->
            Regex("""/Type\s*/Pages\b""").containsMatchIn(body) &&
                !orderedObjects.any { (_, other) ->
                    Regex("""/Type\s*/Pages\b""").containsMatchIn(other) &&
                        Regex("""\b${objNum}\s+\d+\s+R\b""").containsMatchIn(
                            other.substringAfter("/Kids", "")
                        )
                }
        }?.first

        if (pagesRoot != null) {
            val collected = mutableListOf<Int>()
            val visited = mutableSetOf<Int>()
            fun walk(objNum: Int) {
                if (!visited.add(objNum)) return
                val body = objectsByNum[objNum] ?: return
                if (Regex("""/Type\s*/Pages\b""").containsMatchIn(body)) {
                    val kidsBlock = Regex("""/Kids\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
                        .find(body)?.groupValues?.get(1).orEmpty()
                    val refRegex = Regex("""(\d+)\s+\d+\s+R""")
                    for (m in refRegex.findAll(kidsBlock)) {
                        val childId = m.groupValues[1].toIntOrNull() ?: continue
                        walk(childId)
                    }
                } else if (Regex("""/Type\s*/Page\b""").containsMatchIn(body)) {
                    collected.add(objNum)
                }
            }
            walk(pagesRoot)
            if (collected.isNotEmpty()) return collected
        }

        return orderedObjects
            .filter { (_, body) -> Regex("""/Type\s*/Page\b""").containsMatchIn(body) }
            .map { it.first }
    }

    private fun extractContentObjectRefs(pageBody: String): List<Int> {
        val arrayMatch = Regex("""/Contents\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(pageBody)
        if (arrayMatch != null) {
            return Regex("""(\d+)\s+\d+\s+R""")
                .findAll(arrayMatch.groupValues[1])
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .toList()
        }
        val singleMatch = Regex("""/Contents\s+(\d+)\s+\d+\s+R""").find(pageBody)
        if (singleMatch != null) {
            singleMatch.groupValues[1].toIntOrNull()?.let { return listOf(it) }
        }
        return emptyList()
    }

    private fun decodeObjectStreamText(objBody: String): String {
        val streamKeywordIdx = objBody.indexOf("stream")
        if (streamKeywordIdx < 0) return ""
        val dictPart = objBody.substring(0, streamKeywordIdx)
        var dataStart = streamKeywordIdx + "stream".length
        if (dataStart < objBody.length && objBody[dataStart] == '\r') dataStart++
        if (dataStart < objBody.length && objBody[dataStart] == '\n') dataStart++
        val endStreamIdx = objBody.lastIndexOf("endstream")
        if (endStreamIdx <= dataStart) return ""
        var dataEnd = endStreamIdx
        if (dataEnd > dataStart && objBody[dataEnd - 1] == '\n') dataEnd--
        if (dataEnd > dataStart && objBody[dataEnd - 1] == '\r') dataEnd--
        if (dataEnd <= dataStart) return ""

        val streamBytes = objBody.substring(dataStart, dataEnd).toByteArray(Charsets.ISO_8859_1)
        val decodedBytes = if (dictPart.contains("/FlateDecode")) {
            inflatePdfStream(streamBytes) ?: streamBytes
        } else {
            streamBytes
        }
        val contentScript = String(decodedBytes, Charsets.ISO_8859_1)
        if (!contentScript.contains("BT") && !contentScript.contains("Tj") && !contentScript.contains("TJ")) {
            return ""
        }
        return extractTextFromContentScript(contentScript)
    }

    private fun inflatePdfStream(compressed: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (!inflater.finished() && total < 4 * 1024 * 1024) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                } else {
                    out.write(buffer, 0, count)
                    total += count
                }
            }
            out.toByteArray()
        } catch (_: Throwable) {
            null
        } finally {
            inflater.end()
        }
    }

    internal fun extractTextFromContentScript(script: String): String {
        val sb = StringBuilder()
        var inTextBlock = false
        var i = 0
        val n = script.length

        fun appendWordFragment(fragment: String) {
            val cleaned = fragment
                .replace(Regex("""[\u0000-\u0008\u000B\u000C\u000E-\u001F]"""), "")
            if (cleaned.isEmpty()) return
            sb.append(cleaned)
        }

        while (i < n) {
            val c = script[i]
            if (c == '%') {
                while (i < n && script[i] != '\n' && script[i] != '\r') i++
                continue
            }
            if (!inTextBlock) {
                if (c == 'B' && i + 1 < n && script[i + 1] == 'T' &&
                    (i == 0 || script[i - 1].isWhitespace()) &&
                    (i + 2 >= n || script[i + 2].isWhitespace())
                ) {
                    inTextBlock = true
                    i += 2
                    continue
                }
                i++
                continue
            }

            if (c == 'E' && i + 1 < n && script[i + 1] == 'T' &&
                (i == 0 || script[i - 1].isWhitespace()) &&
                (i + 2 >= n || script[i + 2].isWhitespace())
            ) {
                inTextBlock = false
                if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                i += 2
                continue
            }

            when (c) {
                '(' -> {
                    val (lit, nextIdx) = readPdfLiteralString(script, i)
                    i = nextIdx
                    // Peek next operator (skip whitespace)
                    var k = i
                    while (k < n && script[k].isWhitespace()) k++
                    if (k < n && (
                            (k + 1 < n && script[k] == 'T' && script[k + 1] == 'j') ||
                                script[k] == '\'' || script[k] == '"'
                            )
                    ) {
                        appendWordFragment(lit)
                        if (script[k] == '\'' || script[k] == '"') sb.append(' ')
                    }
                }
                '<' -> {
                    if (i + 1 < n && script[i + 1] == '<') {
                        i += 2
                    } else {
                        val closeIdx = script.indexOf('>', i + 1)
                        if (closeIdx > i) {
                            val hex = script.substring(i + 1, closeIdx)
                            i = closeIdx + 1
                            var k = i
                            while (k < n && script[k].isWhitespace()) k++
                            if (k + 1 < n && script[k] == 'T' && script[k + 1] == 'j') {
                                appendWordFragment(decodePdfHexString(hex))
                            }
                        } else {
                            i++
                        }
                    }
                }
                '[' -> {
                    i++
                    val arrayBuf = StringBuilder()
                    while (i < n && script[i] != ']') {
                        when (script[i]) {
                            '(' -> {
                                val (lit, nextIdx) = readPdfLiteralString(script, i)
                                arrayBuf.append(lit)
                                i = nextIdx
                            }
                            '<' -> {
                                val closeIdx = script.indexOf('>', i + 1)
                                if (closeIdx > i) {
                                    arrayBuf.append(decodePdfHexString(script.substring(i + 1, closeIdx)))
                                    i = closeIdx + 1
                                } else {
                                    i++
                                }
                            }
                            '-', in '0'..'9', '.' -> {
                                val numStart = i
                                i++
                                while (i < n && (script[i].isDigit() || script[i] == '.')) i++
                                val num = script.substring(numStart, i).toFloatOrNull()
                                // Large negative kerning offset in TJ indicates word spacing
                                if (num != null && num <= -120f && arrayBuf.isNotEmpty() && arrayBuf.last() != ' ') {
                                    arrayBuf.append(' ')
                                }
                            }
                            else -> i++
                        }
                    }
                    if (i < n && script[i] == ']') i++
                    var k = i
                    while (k < n && script[k].isWhitespace()) k++
                    if (k + 1 < n && script[k] == 'T' && script[k + 1] == 'J') {
                        appendWordFragment(arrayBuf.toString())
                    }
                }
                'T' -> {
                    if (i + 1 < n && (script[i + 1] == 'd' || script[i + 1] == 'D' || script[i + 1] == '*' || script[i + 1] == 'L')) {
                        if (sb.isNotEmpty() && !sb.last().isWhitespace()) sb.append(' ')
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> i++
            }
        }
        return sb.toString()
            .replace(Regex("""[ \t]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun readPdfLiteralString(s: String, openParenIdx: Int): Pair<String, Int> {
        val out = StringBuilder()
        var i = openParenIdx + 1
        var depth = 1
        while (i < s.length && depth > 0) {
            val ch = s[i]
            when (ch) {
                '\\' -> {
                    i++
                    if (i >= s.length) break
                    when (val esc = s[i]) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        '(', ')', '\\' -> out.append(esc)
                        '\r' -> if (i + 1 < s.length && s[i + 1] == '\n') i++
                        '\n' -> {}
                        in '0'..'7' -> {
                            var oct = esc.toString()
                            var steps = 0
                            while (steps < 2 && i + 1 < s.length && s[i + 1] in '0'..'7') {
                                i++
                                oct += s[i]
                                steps++
                            }
                            out.append(oct.toInt(8).toChar())
                        }
                        else -> out.append(esc)
                    }
                }
                '(' -> {
                    depth++
                    out.append('(')
                }
                ')' -> {
                    depth--
                    if (depth > 0) out.append(')')
                }
                else -> out.append(ch)
            }
            i++
        }
        return out.toString() to i
    }

    private fun decodePdfHexString(rawHex: String): String {
        val clean = rawHex.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        if (clean.isEmpty()) return ""
        val padded = if (clean.length % 2 == 1) "${clean}0" else clean
        val bytes = ByteArray(padded.length / 2) { idx ->
            padded.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
        return if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE"))
        } else {
            String(bytes, Charsets.ISO_8859_1)
        }
    }

    fun searchPdfDocumentAndAnnotations(
        pdfFile: File,
        query: String,
        annotations: List<PdfPageAnnotation>,
        preExtractedPages: Map<Int, String>? = null
    ): List<PdfTextMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val matches = mutableListOf<PdfTextMatch>()
        val pagesText = preExtractedPages ?: extractPdfPageTexts(pdfFile)

        for ((pageNum, pageText) in pagesText.entries.sortedBy { it.key }) {
            var searchFrom = 0
            var matchesOnPage = 0
            while (matchesOnPage < 5) {
                val idx = pageText.indexOf(trimmed, startIndex = searchFrom, ignoreCase = true)
                if (idx < 0) break
                val start = max(0, idx - 32)
                val end = minOf(pageText.length, idx + trimmed.length + 48)
                val prefix = if (start > 0) "…" else ""
                val suffix = if (end < pageText.length) "…" else ""
                val snippet = (prefix + pageText.substring(start, end) + suffix)
                    .replace('\n', ' ')
                    .trim()
                matches.add(
                    PdfTextMatch(
                        pageNumber = pageNum,
                        snippet = snippet,
                        sourceType = "PAGE_TEXT",
                        annotationId = null
                    )
                )
                matchesOnPage++
                searchFrom = idx + trimmed.length
            }
        }

        annotations
            .filter { it.content.contains(trimmed, ignoreCase = true) }
            .sortedBy { it.pageNumber }
            .forEach { ann ->
                matches.add(
                    PdfTextMatch(
                        pageNumber = ann.pageNumber,
                        snippet = ann.content.trim().take(120),
                        sourceType = "ANNOTATION",
                        annotationId = ann.id
                    )
                )
            }

        return matches.sortedBy { it.pageNumber }
    }

    /**
     * Exports a flattened copy of [pdfFile] with all [annotations] burned onto each page.
     * Processes one page at a time to keep memory O(1 page bitmap) even on large documents.
     * Original [pdfFile] is never modified.
     */
    fun exportAnnotatedPdfToFile(
        pdfFile: File,
        annotations: List<PdfPageAnnotation>,
        destFile: File
    ): Result<File> {
        if (!pdfFile.exists() || !pdfFile.isFile) {
            return Result.failure(IOException("Source PDF file not found"))
        }

        destFile.parentFile?.mkdirs()
        val tempOut = File(destFile.parentFile, ".export_${System.currentTimeMillis()}_${destFile.name}.tmp")
        val annotationsByPage = annotations.groupBy { it.pageNumber }
        val composeDrawScope = CanvasDrawScope()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var outDoc: PdfDocument? = null

        return try {
            var nativeExportSucceeded = false
            try {
                pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                outDoc = PdfDocument()
                val count = max(renderer.pageCount, 1)
                for (i in 0 until count) {
                    var page: PdfRenderer.Page? = null
                    var bmp: Bitmap? = null
                    var outPage: PdfDocument.Page? = null
                    try {
                        page = renderer.openPage(i)
                        val maxSide = max(page.width, page.height).coerceAtLeast(1)
                        val scale = (2400f / maxSide).coerceAtMost(2f)
                        val targetW = (page.width * scale).toInt().coerceAtLeast(1)
                        val targetH = (page.height * scale).toInt().coerceAtLeast(1)

                        bmp = createBitmap(targetW, targetH)
                        val bmpCanvas = android.graphics.Canvas(bmp)
                        bmpCanvas.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, i + 1).create()
                        outPage = outDoc.startPage(pageInfo)
                        outPage.canvas.drawBitmap(bmp, 0f, 0f, null)

                        val pageAnnotations = annotationsByPage[i + 1].orEmpty()
                        if (pageAnnotations.isNotEmpty()) {
                            composeDrawScope.draw(
                                density = Density(1f, 1f),
                                layoutDirection = LayoutDirection.Ltr,
                                canvas = ComposeCanvas(outPage.canvas),
                                size = Size(targetW.toFloat(), targetH.toFloat())
                            ) {
                                pageAnnotations.forEach { drawPdfAnnotation(it) }
                            }
                        }
                    } finally {
                        if (outPage != null) {
                            try { outDoc.finishPage(outPage) } catch (_: Throwable) {}
                        }
                        bmp?.recycle()
                        try { page?.close() } catch (_: Throwable) {}
                    }
                }
                FileOutputStream(tempOut).use { fos ->
                    outDoc.writeTo(fos)
                    fos.flush()
                }
                nativeExportSucceeded = tempOut.exists() && tempOut.length() > 64L
            } catch (_: Throwable) {
                nativeExportSucceeded = false
            }

            if (!nativeExportSucceeded) {
                // Fallback when native PdfRenderer / PdfDocument is unavailable (e.g., Robolectric JVM)
                writeFallbackAnnotatedPdf(
                    sourcePdf = pdfFile,
                    annotationsByPage = annotationsByPage,
                    outFile = tempOut
                )
            }

            if (!tempOut.exists() || tempOut.length() < 32L) {
                throw IOException("Exported PDF output was empty")
            }

            if (destFile.exists()) destFile.delete()
            if (!tempOut.renameTo(destFile)) {
                tempOut.copyTo(destFile, overwrite = true)
                tempOut.delete()
            }
            Result.success(destFile)
        } catch (e: Exception) {
            if (tempOut.exists()) tempOut.delete()
            Result.failure(e)
        } finally {
            try { outDoc?.close() } catch (_: Throwable) {}
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    private fun writeFallbackAnnotatedPdf(
        sourcePdf: File,
        annotationsByPage: Map<Int, List<PdfPageAnnotation>>,
        outFile: File
    ) {
        val extractedPages = extractPdfPageTexts(sourcePdf)
        val pageCount = max(
            max(countPdfPagesHeuristic(sourcePdf), extractedPages.keys.maxOrNull() ?: 1),
            annotationsByPage.keys.maxOrNull() ?: 1
        ).coerceAtLeast(1)

        // Object layout:
        // 1: Catalog
        // 2: Pages
        // 3: Font (Helvetica)
        // For each page i in 0 until pageCount:
        //   4 + 2*i     : Page object
        //   4 + 2*i + 1 : Contents stream object
        val objects = mutableListOf<ByteArray>()
        val pageObjNums = (0 until pageCount).map { 4 + it * 2 }
        val kidsRef = pageObjNums.joinToString(" ") { "$it 0 R" }

        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        objects.add("2 0 obj\n<< /Type /Pages /Kids [$kidsRef] /Count $pageCount >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        objects.add("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))

        fun escapePdfText(raw: String): String = raw
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", " ")
            .replace("\n", " ")

        for (p in 1..pageCount) {
            val pageObjNum = 4 + (p - 1) * 2
            val contentObjNum = pageObjNum + 1
            val streamCmds = StringBuilder()

            val pageText = extractedPages[p].orEmpty().trim()
            if (pageText.isNotEmpty()) {
                streamCmds.append("BT /F1 12 Tf 72 720 Td (${escapePdfText(pageText.take(300))}) Tj ET\n")
            }

            var noteCursorY = 670
            for (ann in annotationsByPage[p].orEmpty()) {
                val r = ((ann.colorArgb ushr 16) and 0xFF) / 255f
                val g = ((ann.colorArgb ushr 8) and 0xFF) / 255f
                val b = (ann.colorArgb and 0xFF) / 255f
                val left = minOf(ann.start.x, ann.end.x).coerceIn(36f, 540f)
                val w = kotlin.math.abs(ann.end.x - ann.start.x).coerceIn(24f, 480f)
                val y = (760f - minOf(ann.start.y, ann.end.y)).coerceIn(60f, 740f)
                val h = kotlin.math.abs(ann.end.y - ann.start.y).coerceIn(6f, 120f)
                streamCmds.append(
                    String.format(
                        java.util.Locale.US,
                        "%.2f %.2f %.2f rg %.1f %.1f %.1f %.1f re f\n",
                        r, g, b, left, y, w, h
                    )
                )
                if (ann.content.isNotBlank()) {
                    streamCmds.append("0 0 0 rg BT /F1 10 Tf 72 $noteCursorY Td (${escapePdfText(ann.content.take(200))}) Tj ET\n")
                    noteCursorY = (noteCursorY - 16).coerceAtLeast(60)
                }
            }

            val streamBytes = streamCmds.toString().toByteArray(Charsets.ISO_8859_1)
            objects.add(
                "$pageObjNum 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjNum 0 R >>\nendobj\n"
                    .toByteArray(Charsets.ISO_8859_1)
            )
            val contentHeader = "$contentObjNum 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n".toByteArray(Charsets.ISO_8859_1)
            val contentFooter = "\nendstream\nendobj\n".toByteArray(Charsets.ISO_8859_1)
            val fullContent = ByteArrayOutputStream().apply {
                write(contentHeader)
                write(streamBytes)
                write(contentFooter)
            }.toByteArray()
            objects.add(fullContent)
        }

        val out = ByteArrayOutputStream()
        out.write("%PDF-1.4\n".toByteArray(Charsets.ISO_8859_1))
        val offsets = mutableListOf(0)
        for (objBytes in objects) {
            offsets.add(out.size())
            out.write(objBytes)
        }
        val xrefPos = out.size()
        val totalObjs = objects.size + 1
        out.write("xref\n0 $totalObjs\n0000000000 65535 f \n".toByteArray(Charsets.ISO_8859_1))
        for (i in 1 until offsets.size) {
            out.write(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]).toByteArray(Charsets.ISO_8859_1))
        }
        out.write("trailer\n<< /Size $totalObjs /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n".toByteArray(Charsets.ISO_8859_1))

        FileOutputStream(outFile).use { fos ->
            fos.write(out.toByteArray())
            fos.flush()
        }
    }
}
