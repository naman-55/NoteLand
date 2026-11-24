package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.repository.NoteRepository
import com.example.data.storage.AttachmentStorageManager
import com.example.security.SecurityManager
import com.example.ui.editor.components.NoteAttachmentsSection
import com.example.ui.editor.components.RichDocumentEditorPane
import com.example.ui.editor.components.RichEditorToolbar
import com.example.ui.theme.AppThemeMode
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.engine.RichSpanEngine
import com.example.ui.editor.model.CalloutTone
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.InlineSpan
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCellSelection
import com.example.ui.editor.model.UnderlineStyle
import com.example.ui.editor.serialization.RichNoteDocumentCodec
import com.example.ui.theme.NoteLandTheme
import com.example.util.SmartNoteUtils
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdvancedRichEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    // =========================================================================
    // 1. INLINE TEXT FORMATTING & MIXED FORMATTING
    // =========================================================================

    @Test
    fun `all 8 inline formatting actions apply, update, and toggle accurately`() {
        val text = "Hello Noteland Rich Editor"
        val len = text.length
        var spans = emptyList<InlineSpan>()

        // Bold on "Hello"
        spans = RichSpanEngine.toggleBold(spans, len, 0, 5)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(spans, len, 0, 5).bold)

        // Italic on "Noteland"
        spans = RichSpanEngine.toggleItalic(spans, len, 6, 14)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(spans, len, 6, 14).italic)

        // Strikethrough on "Rich"
        spans = RichSpanEngine.toggleStrikethrough(spans, len, 15, 19)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(spans, len, 15, 19).strikethrough)

        // Standard underline on "Editor"
        spans = RichSpanEngine.toggleUnderline(spans, len, 20, 26, UnderlineStyle.STANDARD)
        assertEquals(UnderlineStyle.STANDARD, RichSpanEngine.resolveActiveStyleAt(spans, len, 20, 26).underline)

        // Update previously applied underline to Wavy underline on "Editor"
        spans = RichSpanEngine.toggleUnderline(spans, len, 20, 26, UnderlineStyle.WAVY)
        assertEquals(UnderlineStyle.WAVY, RichSpanEngine.resolveActiveStyleAt(spans, len, 20, 26).underline)

        // Toggle wavy underline off again
        spans = RichSpanEngine.toggleUnderline(spans, len, 20, 26, UnderlineStyle.WAVY)
        assertEquals(UnderlineStyle.NONE, RichSpanEngine.resolveActiveStyleAt(spans, len, 20, 26).underline)

        // Apply Text Color, Background Color, and Highlight, then update previously applied colors
        val crimson = EditorColorPalette.textColors[1].hex!!
        val ocean = EditorColorPalette.textColors[6].hex!!
        val mintBg = EditorColorPalette.textBackgroundColors[4].hex!!
        val yellowHl = EditorColorPalette.highlightColors[1].hex!!

        spans = RichSpanEngine.setTextColor(spans, len, 0, 14, crimson)
        assertEquals(crimson, RichSpanEngine.resolveActiveStyleAt(spans, len, 0, 5).textColorHex)

        // Update text color on overlapping subrange [6..14]
        spans = RichSpanEngine.setTextColor(spans, len, 6, 14, ocean)
        assertEquals(crimson, RichSpanEngine.resolveActiveStyleAt(spans, len, 0, 5).textColorHex)
        assertEquals(ocean, RichSpanEngine.resolveActiveStyleAt(spans, len, 6, 14).textColorHex)

        // Apply text background color and highlight color
        spans = RichSpanEngine.setBackgroundColor(spans, len, 6, 14, mintBg)
        spans = RichSpanEngine.setHighlightColor(spans, len, 15, 26, yellowHl)

        val midStyle = RichSpanEngine.resolveActiveStyleAt(spans, len, 6, 14)
        assertTrue(midStyle.italic)
        assertEquals(ocean, midStyle.textColorHex)
        assertEquals(mintBg, midStyle.backgroundColorHex)
        assertNull(midStyle.highlightColorHex)

        val endStyle = RichSpanEngine.resolveActiveStyleAt(spans, len, 15, 19)
        assertTrue(endStyle.strikethrough)
        assertEquals(yellowHl, endStyle.highlightColorHex)

        // Render AnnotatedString and verify mixed styles + 1:1 text length
        val annotated = RichSpanEngine.buildStyledAnnotatedString(
            text = text,
            spans = spans,
            isDark = false,
            codeBg = Color(0xFFE5E7EB),
            linkColor = Color(0xFF3B82F6)
        )
        assertEquals(text, annotated.text)
        assertTrue(annotated.spanStyles.any { it.start == 0 && it.end == 5 && it.item.fontWeight == FontWeight.Bold })
        assertTrue(annotated.spanStyles.any { it.start == 6 && it.end == 14 && it.item.fontStyle == FontStyle.Italic })
        assertTrue(annotated.spanStyles.any { it.start == 15 && it.end == 19 && it.item.textDecoration == TextDecoration.LineThrough })
    }

    @Test
    fun `mixed formatting inside one paragraph preserves spans accurately across typing and deletion`() {
        val original = "Alpha Beta Gamma"
        var spans = RichSpanEngine.toggleBold(emptyList(), original.length, 0, 5)
        spans = RichSpanEngine.toggleItalic(spans, original.length, 6, 10)
        spans = RichSpanEngine.toggleUnderline(spans, original.length, 11, 16, UnderlineStyle.WAVY)

        // Insert " Super" after "Alpha" (index 5) using a highlight typingStyle
        val inserted = "Alpha Super Beta Gamma"
        val typingHighlight = InlineFormatStyle(highlightColorHex = EditorColorPalette.highlightColors[1].hex)
        val afterInsert = RichSpanEngine.applyTextDiff(original, inserted, spans, typingStyle = typingHighlight)

        // "Alpha" remains bold [0..5], "Super " gets highlight [6..12], "Beta" shifts to [12..16] italic, "Gamma" shifts to [17..22] wavy underline
        assertTrue(RichSpanEngine.resolveActiveStyleAt(afterInsert, inserted.length, 0, 5).bold)
        assertEquals(typingHighlight.highlightColorHex, RichSpanEngine.resolveActiveStyleAt(afterInsert, inserted.length, 6, 12).highlightColorHex)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(afterInsert, inserted.length, 12, 16).italic)
        assertEquals(UnderlineStyle.WAVY, RichSpanEngine.resolveActiveStyleAt(afterInsert, inserted.length, 17, 22).underline)

        // Delete " Super" back to original
        val afterDelete = RichSpanEngine.applyTextDiff(inserted, original, afterInsert, typingStyle = null)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(afterDelete, original.length, 0, 5).bold)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(afterDelete, original.length, 6, 10).italic)
        assertEquals(UnderlineStyle.WAVY, RichSpanEngine.resolveActiveStyleAt(afterDelete, original.length, 11, 16).underline)
    }

    // =========================================================================
    // 2. ALIGNMENT, INDENTATION, LISTS & INTERACTIVE CHECKLISTS
    // =========================================================================

    @Test
    fun `alignment, indentation, nested lists, checklists, enter split, and backspace merge work reliably`() {
        val b1 = RichBlock.TextBlock(id = "b1", text = "First item", kind = RichBlockKind.NUMBERED_LIST, indentLevel = 0)
        var doc = RichNoteDocument(blocks = listOf(b1))

        // Alignment Left -> Center -> Right
        doc = RichDocumentReducer.setAlignment(doc, "b1", RichTextAlignment.CENTER)
        assertEquals(RichTextAlignment.CENTER, (doc.blocks.first() as RichBlock.TextBlock).alignment)
        doc = RichDocumentReducer.setAlignment(doc, "b1", RichTextAlignment.RIGHT)
        assertEquals(RichTextAlignment.RIGHT, (doc.blocks.first() as RichBlock.TextBlock).alignment)
        doc = RichDocumentReducer.setAlignment(doc, "b1", RichTextAlignment.LEFT)
        assertEquals(RichTextAlignment.LEFT, (doc.blocks.first() as RichBlock.TextBlock).alignment)

        // Enter split continues numbered list and preserves spans across split
        doc = RichDocumentReducer.updateTextBlock(doc, "b1") {
            it.copy(spans = RichSpanEngine.toggleBold(emptyList(), it.text.length, 0, it.text.length))
        }
        val splitRes = RichDocumentReducer.splitBlockOnEnter(doc, "b1", 5) // "First" | " item"
        doc = splitRes.document
        assertEquals(2, doc.blocks.size)
        val firstBlock = doc.blocks[0] as RichBlock.TextBlock
        val secondBlock = doc.blocks[1] as RichBlock.TextBlock
        assertEquals("First", firstBlock.text)
        assertEquals(" item", secondBlock.text)
        assertEquals(RichBlockKind.NUMBERED_LIST, secondBlock.kind)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(secondBlock.spans, secondBlock.text.length, 0, secondBlock.text.length).bold)

        // Increase indent on second item -> hierarchical numbering (1., a.)
        doc = RichDocumentReducer.increaseIndent(doc, secondBlock.id)
        assertEquals(1, (doc.blocks[1] as RichBlock.TextBlock).indentLevel)
        assertEquals("1.", RichDocumentReducer.computeNumberedListLabel(doc.blocks, 0))
        assertEquals("a.", RichDocumentReducer.computeNumberedListLabel(doc.blocks, 1))

        // Nest one more level -> roman numeral (i.)
        doc = RichDocumentReducer.increaseIndent(doc, secondBlock.id)
        assertEquals("i.", RichDocumentReducer.computeNumberedListLabel(doc.blocks, 1))

        // Decrease indent twice back to 0, clamped at 0
        doc = RichDocumentReducer.decreaseIndent(doc, secondBlock.id)
        doc = RichDocumentReducer.decreaseIndent(doc, secondBlock.id)
        doc = RichDocumentReducer.decreaseIndent(doc, secondBlock.id)
        assertEquals(0, (doc.blocks[1] as RichBlock.TextBlock).indentLevel)
        assertEquals("2.", RichDocumentReducer.computeNumberedListLabel(doc.blocks, 1))

        // Convert to interactive checklist and toggle checked state
        doc = RichDocumentReducer.setBlockKind(doc, secondBlock.id, RichBlockKind.CHECKLIST)
        assertEquals(RichBlockKind.CHECKLIST, (doc.blocks[1] as RichBlock.TextBlock).kind)
        assertFalse((doc.blocks[1] as RichBlock.TextBlock).checked)
        doc = RichDocumentReducer.toggleChecklist(doc, secondBlock.id)
        assertTrue((doc.blocks[1] as RichBlock.TextBlock).checked)

        // Backspace at start of checklist converts to paragraph, then second backspace merges with previous block
        val unwrapRes = RichDocumentReducer.handleBackspaceAtStart(doc, secondBlock.id)
        doc = unwrapRes.document
        assertEquals(2, doc.blocks.size)
        assertEquals(RichBlockKind.PARAGRAPH, (doc.blocks[1] as RichBlock.TextBlock).kind)

        val mergeRes = RichDocumentReducer.handleBackspaceAtStart(doc, secondBlock.id)
        doc = mergeRes.document
        assertEquals(1, doc.blocks.size)
        assertEquals("First item", (doc.blocks[0] as RichBlock.TextBlock).text)
        assertEquals(5, mergeRes.focusCursorOffset)
    }

    // =========================================================================
    // 3. CONTENT BLOCKS: QUOTE, CALLOUT, DIVIDER, TABLE & SPREADSHEET
    // =========================================================================

    @Test
    fun `blockquotes, callouts, dividers, tables, and spreadsheets support full CRUD and formula evaluation`() {
        val quote = RichBlock.TextBlock(id = "q1", text = "Measure twice, cut once.", kind = RichBlockKind.BLOCKQUOTE)
        val callout = RichBlock.TextBlock(id = "c1", text = "Remember offline backup", kind = RichBlockKind.CALLOUT, calloutTone = CalloutTone.INFO)
        var doc = RichNoteDocument(blocks = listOf(quote, callout))

        // Cycle callout tone INFO -> TIP -> WARNING -> NOTE
        doc = RichDocumentReducer.cycleCalloutTone(doc, "c1")
        assertEquals(CalloutTone.TIP, (doc.blocks[1] as RichBlock.TextBlock).calloutTone)
        doc = RichDocumentReducer.cycleCalloutTone(doc, "c1")
        assertEquals(CalloutTone.WARNING, (doc.blocks[1] as RichBlock.TextBlock).calloutTone)

        // Insert horizontal divider
        val divRes = RichDocumentReducer.insertBlockAfter(doc, "c1", RichBlock.DividerBlock(id = "d1"))
        doc = divRes.document
        assertTrue(doc.blocks.any { it is RichBlock.DividerBlock && it.id == "d1" })

        // Insert editable table & test insert/delete rows and columns + editing cells
        val table = RichDocumentReducer.createDefaultTable(isSpreadsheet = false, rows = 2, cols = 2)
        doc = RichDocumentReducer.insertBlockAfter(doc, "d1", table).document

        doc = RichDocumentReducer.updateTableCell(doc, table.id, 0, 0) { it.copy(text = "Task") }
        doc = RichDocumentReducer.updateTableCell(doc, table.id, 0, 1) { it.copy(text = "Owner") }
        doc = RichDocumentReducer.updateTableCell(doc, table.id, 1, 0) { it.copy(text = "Editor") }
        doc = RichDocumentReducer.updateTableCell(doc, table.id, 1, 1) { it.copy(text = "Alex") }

        // Insert row at index 2 and column at index 2
        doc = RichDocumentReducer.insertTableRow(doc, table.id, atIndex = 2)
        doc = RichDocumentReducer.insertTableColumn(doc, table.id, atIndex = 2)
        var updatedTable = doc.blocks.first { it.id == table.id } as RichBlock.TableBlock
        assertEquals(3, updatedTable.rowCount)
        assertEquals(3, updatedTable.colCount)
        assertEquals("Editor", updatedTable.rows[1][0].text)

        // Delete inserted row and column and verify existing data preserved
        doc = RichDocumentReducer.deleteTableRow(doc, table.id, rowIndex = 2)
        doc = RichDocumentReducer.deleteTableColumn(doc, table.id, colIndex = 2)
        updatedTable = doc.blocks.first { it.id == table.id } as RichBlock.TableBlock
        assertEquals(2, updatedTable.rowCount)
        assertEquals(2, updatedTable.colCount)
        assertEquals("Alex", updatedTable.rows[1][1].text)

        // Insert spreadsheet block and test formulas (=SUM, =AVG, =MAX, =B2*2+5)
        val sheet = RichDocumentReducer.createDefaultTable(isSpreadsheet = true, rows = 4, cols = 3)
        doc = RichDocumentReducer.insertBlockAfter(doc, table.id, sheet).document
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 1, 1) { it.copy(text = "10") }
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 2, 1) { it.copy(text = "25") }
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 3, 1) { it.copy(text = "=SUM(B2:B3)") }
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 1, 2) { it.copy(text = "=AVG(B2:B3)") }
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 2, 2) { it.copy(text = "=B2*2+5") }
        doc = RichDocumentReducer.updateTableCell(doc, sheet.id, 3, 2) { it.copy(text = "=MAX(B2:B3)") }

        val evaluatedSheet = doc.blocks.first { it.id == sheet.id } as RichBlock.TableBlock
        assertEquals("35", RichDocumentReducer.evaluateSpreadsheetDisplay(evaluatedSheet.rows, evaluatedSheet.rows[3][1].text))
        assertEquals("17.5", RichDocumentReducer.evaluateSpreadsheetDisplay(evaluatedSheet.rows, evaluatedSheet.rows[1][2].text))
        assertEquals("25", RichDocumentReducer.evaluateSpreadsheetDisplay(evaluatedSheet.rows, evaluatedSheet.rows[2][2].text))
        assertEquals("25", RichDocumentReducer.evaluateSpreadsheetDisplay(evaluatedSheet.rows, evaluatedSheet.rows[3][2].text))
    }

    // =========================================================================
    // 4. SERIALIZATION, BACKWARD COMPATIBILITY & SYNC-LOSS PREVENTION
    // =========================================================================

    @Test
    fun `serialization round-trip preserves all formatting, blocks, and tables while staying backward-compatible`() {
        val originalBlock = RichBlock.TextBlock(
            id = "p1",
            text = "Important Meeting Summary",
            kind = RichBlockKind.HEADING_2,
            alignment = RichTextAlignment.CENTER,
            indentLevel = 1,
            spans = listOf(
                InlineSpan(
                    start = 0,
                    end = 9,
                    style = InlineFormatStyle(
                        bold = true,
                        underline = UnderlineStyle.WAVY,
                        textColorHex = "#DC2626",
                        highlightColorHex = "#FEF08A"
                    )
                ),
                InlineSpan(
                    start = 10,
                    end = 17,
                    style = InlineFormatStyle(
                        italic = true,
                        strikethrough = true,
                        backgroundColorHex = "#DCFCE7"
                    )
                )
            )
        )
        val sheet = RichDocumentReducer.createDefaultTable(isSpreadsheet = true, rows = 3, cols = 2)
        val doc = RichNoteDocument(
            blocks = listOf(
                originalBlock,
                RichBlock.DividerBlock(id = "div1"),
                sheet
            )
        )

        val plainMd = RichNoteDocumentCodec.toPlainMarkdown(doc)
        val synced = doc.copy(sourceContentHash = RichNoteDocumentCodec.computeContentHash(plainMd))
        val encoded = RichNoteDocumentCodec.encode(synced)

        // Reload via loadForNote (same content -> exact rich doc preserved without loss!)
        val loaded = RichNoteDocumentCodec.loadForNote(encoded, plainMd)
        assertEquals(3, loaded.blocks.size)
        val loadedText = loaded.blocks[0] as RichBlock.TextBlock
        assertEquals(RichBlockKind.HEADING_2, loadedText.kind)
        assertEquals(RichTextAlignment.CENTER, loadedText.alignment)
        assertEquals(1, loadedText.indentLevel)
        assertEquals(UnderlineStyle.WAVY, loadedText.spans[0].style.underline)
        assertEquals("#DC2626", loadedText.spans[0].style.textColorHex)
        assertEquals("#FEF08A", loadedText.spans[0].style.highlightColorHex)
        assertEquals("#DCFCE7", loadedText.spans[1].style.backgroundColorHex)
        assertTrue(loaded.blocks[1] is RichBlock.DividerBlock)
        assertTrue((loaded.blocks[2] as RichBlock.TableBlock).isSpreadsheet)

        // Corrupted JSON falls back cleanly without crashing
        val recovered = RichNoteDocumentCodec.loadForNote("{broken json", "**Recovered** note")
        val recoveredText = recovered.blocks.first() as RichBlock.TextBlock
        assertEquals("Recovered note", recoveredText.text)
        assertTrue(RichSpanEngine.resolveActiveStyleAt(recoveredText.spans, recoveredText.text.length, 0, 9).bold)

        // Existing legacy Markdown note (no richDocument JSON at all) parses cleanly with pipe table, checklist, quote, and divider
        val legacyMarkdown = """
            # Legacy Note
            This has **bold**, *italic*, ~~strike~~, and <u>underlined</u> text.
            > Quoted wisdom
            - [x] Done task
            - [ ] Pending task
            ---
            | Col A | Col B |
            | --- | --- |
            | Val 1 | Val 2 |
        """.trimIndent()
        val legacyDoc = RichNoteDocumentCodec.loadForNote("", legacyMarkdown)
        assertTrue(legacyDoc.blocks.any { it is RichBlock.TextBlock && it.kind == RichBlockKind.HEADING_1 && it.text == "Legacy Note" })
        assertTrue(legacyDoc.blocks.any { it is RichBlock.TextBlock && it.kind == RichBlockKind.BLOCKQUOTE && it.text == "Quoted wisdom" })
        assertTrue(legacyDoc.blocks.any { it is RichBlock.TextBlock && it.kind == RichBlockKind.CHECKLIST && it.checked && it.text == "Done task" })
        assertTrue(legacyDoc.blocks.any { it is RichBlock.DividerBlock })
        val parsedTable = legacyDoc.blocks.firstOrNull { it is RichBlock.TableBlock } as? RichBlock.TableBlock
        assertNotNull(parsedTable)
        assertEquals("Val 2", parsedTable!!.rows[1][1].text)
    }

    // =========================================================================
    // 5. ROOM PERSISTENCE, REOPENING, VAULT ENCRYPTION & FILE ATTACHMENTS
    // =========================================================================

    @Test
    fun `rich formatted note persists in Room, survives close and reopen, supports Vault encryption, and preserves attachments`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = NoteRepository(db, context)
        val storage = AttachmentStorageManager(context)
        val security = SecurityManager(context)

        try {
            // Build rich formatted document with mixed inline spans + spreadsheet table
            val textBlock = RichBlock.TextBlock(
                id = "t1",
                text = "Quarterly Budget Plan",
                kind = RichBlockKind.HEADING_1,
                alignment = RichTextAlignment.CENTER,
                spans = listOf(
                    InlineSpan(0, 9, InlineFormatStyle(bold = true, underline = UnderlineStyle.WAVY, textColorHex = "#2563EB")),
                    InlineSpan(10, 16, InlineFormatStyle(highlightColorHex = "#FEF08A"))
                )
            )
            val tableBlock = RichDocumentReducer.createDefaultTable(isSpreadsheet = true, rows = 3, cols = 2)
            val doc = RichNoteDocument(blocks = listOf(textBlock, tableBlock))
            val plainMd = RichNoteDocumentCodec.toPlainMarkdown(doc)
            val richJson = RichNoteDocumentCodec.encode(
                doc.copy(sourceContentHash = RichNoteDocumentCodec.computeContentHash(plainMd))
            )

            // Store inside canvasDataJson alongside existing metadata (blockOrder + texture)
            val canvasMeta = SmartNoteUtils.withRichDocumentJson(
                SmartNoteUtils.withNoteStyleMeta("", "dot_grid", ""),
                richJson
            )
            val note = NoteEntity(
                id = "rich-note-1",
                title = "Q4 Budget",
                content = plainMd,
                noteType = NoteType.STANDARD.name,
                canvasDataJson = canvasMeta
            )
            val saveRes = repository.saveOrUpdateNote(note)
            assertTrue(saveRes.isSuccess)

            // Reopen from Room DB
            val reopened = repository.getNoteByIdDirect("rich-note-1")
            assertNotNull(reopened)
            assertEquals("dot_grid", SmartNoteUtils.parseNoteTexture(reopened!!.canvasDataJson))
            val reopenedDoc = RichNoteDocumentCodec.loadForNote(
                SmartNoteUtils.parseRichDocumentJson(reopened.canvasDataJson),
                reopened.content
            )
            val reopenedHeading = reopenedDoc.blocks[0] as RichBlock.TextBlock
            assertEquals(RichTextAlignment.CENTER, reopenedHeading.alignment)
            assertEquals(UnderlineStyle.WAVY, reopenedHeading.spans[0].style.underline)
            assertEquals("#FEF08A", reopenedHeading.spans[1].style.highlightColorHex)
            assertTrue((reopenedDoc.blocks[1] as RichBlock.TableBlock).isSpreadsheet)

            // Vault encryption round-trip for richDocument payload
            val encryptedRich = security.encrypt(richJson)
            val lockedCanvas = SmartNoteUtils.withRichDocumentJson("", encryptedRich)
            val rawEncrypted = SmartNoteUtils.parseRichDocumentJson(lockedCanvas)
            assertTrue(rawEncrypted.isNotBlank())
            val decryptedRich = security.decrypt(rawEncrypted)
            val unlockedDoc = RichNoteDocumentCodec.loadForNote(decryptedRich, plainMd)
            assertEquals("Quarterly Budget Plan", (unlockedDoc.blocks[0] as RichBlock.TextBlock).text)

            // File Attachments: attach Image, PDF, and Text Document to the note
            val pdfResult = storage.saveAttachment(
                fileName = "budget_q4.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream("%PDF-1.4\n%%EOF".toByteArray()),
                noteId = note.id
            )
            val docResult = storage.saveAttachment(
                fileName = "summary.txt",
                mimeType = "text/plain",
                inputStream = ByteArrayInputStream("Revenue up 18% YoY".toByteArray()),
                noteId = note.id
            )
            val imgResult = storage.saveAttachment(
                fileName = "chart.png",
                mimeType = "image/png",
                inputStream = ByteArrayInputStream(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)),
                noteId = note.id
            )
            assertTrue(pdfResult.isSuccess && docResult.isSuccess && imgResult.isSuccess)
            repository.saveAttachment(pdfResult.getOrThrow())
            repository.saveAttachment(docResult.getOrThrow())
            repository.saveAttachment(imgResult.getOrThrow())

            val attached = repository.getAttachmentsForNote(note.id).first()
            assertEquals(3, attached.size)
            assertTrue(attached.any { it.fileName == "budget_q4.pdf" && it.mimeType == "application/pdf" })
            assertTrue(attached.any { it.fileName == "summary.txt" && it.sizeBytes > 0 })
            assertTrue(attached.any { it.fileName == "chart.png" && it.mimeType == "image/png" })

            // Download / export attachment bytes to destination file
            val srcFile = storage.getFileFromRelativePath(docResult.getOrThrow().relativePath)
            val downloadedFile = File(context.cacheDir, "downloaded_summary.txt")
            srcFile.inputStream().use { input ->
                downloadedFile.outputStream().use { output -> input.copyTo(output) }
            }
            assertEquals("Revenue up 18% YoY", downloadedFile.readText())
            downloadedFile.delete()

            // Remove one attachment and verify remaining attachments stay intact
            val pdfAtt = attached.first { it.fileName == "budget_q4.pdf" }
            storage.deleteFile(pdfAtt.relativePath)
            repository.deleteAttachment(pdfAtt.id)
            val afterRemoval = repository.getAttachmentsForNote(note.id).first()
            assertEquals(2, afterRemoval.size)
            assertFalse(storage.getFileFromRelativePath(pdfAtt.relativePath).exists())
        } finally {
            db.close()
        }
    }

    // =========================================================================
    // 6. COMPOSE UI INTEGRATION (TOOLBAR, RICH PANE, TABLE, ATTACHMENTS, DARK THEME)
    // =========================================================================

    @Test
    fun `RichEditorToolbar and RichDocumentEditorPane handle formatting, checklists, tables, and dark mode smoothly`() {
        val b1 = RichBlock.TextBlock(id = "b1", text = "Build Noteland", kind = RichBlockKind.PARAGRAPH)
        val b2 = RichBlock.TextBlock(id = "b2", text = "Ship rich editor", kind = RichBlockKind.CHECKLIST, checked = false)
        var doc by mutableStateOf(RichNoteDocument(blocks = listOf(b1, b2)))
        var activeBlockId by mutableStateOf<String?>("b1")
        val selections = mutableStateMapOf("b1" to TextRange(0, 5))
        var selectedCell by mutableStateOf<TableCellSelection?>(null)

        composeRule.setContent {
            NoteLandTheme(darkTheme = true) {
                val activeBlock = doc.blocks.firstOrNull { it.id == activeBlockId } as? RichBlock.TextBlock
                val activeStyle = activeBlock?.let {
                    val sel = selections[it.id] ?: TextRange(0)
                    RichSpanEngine.resolveActiveStyleAt(it.spans, it.text.length, sel.min, sel.max)
                } ?: InlineFormatStyle()

                Scaffold(
                    bottomBar = {
                        RichEditorToolbar(
                            plainTextForStats = RichNoteDocumentCodec.toPlainMarkdown(doc),
                            activeStyle = activeStyle,
                            activeBlockKind = activeBlock?.kind ?: RichBlockKind.PARAGRAPH,
                            activeAlignment = activeBlock?.alignment ?: RichTextAlignment.LEFT,
                            activeIndentLevel = activeBlock?.indentLevel ?: 0,
                            onToggleBold = {
                                val sel = selections["b1"] ?: TextRange(0, 5)
                                doc = RichDocumentReducer.updateTextBlock(doc, "b1") {
                                    it.copy(spans = RichSpanEngine.toggleBold(it.spans, it.text.length, sel.min, sel.max))
                                }
                            },
                            onToggleItalic = {},
                            onToggleUnderlineStandard = {},
                            onToggleUnderlineWavy = {
                                val sel = selections["b1"] ?: TextRange(0, 5)
                                doc = RichDocumentReducer.updateTextBlock(doc, "b1") {
                                    it.copy(spans = RichSpanEngine.toggleUnderline(it.spans, it.text.length, sel.min, sel.max, UnderlineStyle.WAVY))
                                }
                            },
                            onToggleStrikethrough = {},
                            onToggleInlineCode = {},
                            onClearFormatting = {},
                            onSelectTextColor = {},
                            onSelectTextBackgroundColor = {},
                            onSelectHighlightColor = {},
                            onSetAlignment = { align -> doc = RichDocumentReducer.setAlignment(doc, "b1", align) },
                            onIncreaseIndent = { doc = RichDocumentReducer.increaseIndent(doc, "b1") },
                            onDecreaseIndent = { doc = RichDocumentReducer.decreaseIndent(doc, "b1") },
                            onSetBlockKind = { kind -> doc = RichDocumentReducer.setBlockKind(doc, "b1", kind) },
                            onInsertDivider = {
                                doc = RichDocumentReducer.insertBlockAfter(doc, "b1", RichBlock.DividerBlock()).document
                            },
                            onInsertTable = {
                                val table = RichDocumentReducer.createDefaultTable(isSpreadsheet = false, rows = 2, cols = 2)
                                doc = RichDocumentReducer.insertBlockAfter(doc, "b2", table).document
                            },
                            onInsertSpreadsheet = {},
                            onOpenLinkDialog = {},
                            onAttachImage = {},
                            onAttachPdf = {},
                            onAttachDocument = {},
                            onPasteImage = {},
                            selectedNoteColorHex = "",
                            onSelectNoteColorHex = {},
                            onOpenNoteColorSheet = {},
                            onOpenUtilities = {},
                            onOpenDrawing = {},
                            onOpenEraser = {},
                            onStartVoiceDictation = {},
                            isDictating = false,
                            onToggleVoiceRecording = {},
                            isRecordingAudio = false,
                    currentAppThemeMode = AppThemeMode.LIGHT,
                    onSelectAppThemeMode = {},
                    hasInkStrokes = false
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RichDocumentEditorPane(
                            document = doc,
                            activeBlockId = activeBlockId,
                            blockSelections = selections,
                            pendingFocusBlockId = null,
                            pendingFocusOffset = null,
                            selectedTableCell = selectedCell,
                            isReadOnly = false,
                            baseFontSizeSp = 16f,
                            accentColor = Color(0xFFE07A5F),
                            isDark = true,
                            onBlockFocused = { activeBlockId = it },
                            onPendingFocusConsumed = {},
                            onTextBlockChange = { id, tfv ->
                                selections[id] = tfv.selection
                                doc = RichDocumentReducer.updateTextBlock(doc, id) { it.copy(text = tfv.text) }
                            },
                            onEnterSplit = { _, _ -> },
                            onBackspaceAtStart = {},
                            onToggleChecklist = { id -> doc = RichDocumentReducer.toggleChecklist(doc, id) },
                            onCycleCalloutTone = { id -> doc = RichDocumentReducer.cycleCalloutTone(doc, id) },
                            onUpdateDocument = { doc = it },
                            onSelectTableCell = { selectedCell = it }
                        )
                    }
                }
            }
        }

        // Toggle Bold and Wavy Underline on selected text "Build"
        composeRule.onNodeWithTag("format_bold_btn").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("format_wavy_underline_btn").assertIsDisplayed().performClick()

        val styledB1 = doc.blocks.first { it.id == "b1" } as RichBlock.TextBlock
        val style = RichSpanEngine.resolveActiveStyleAt(styledB1.spans, styledB1.text.length, 0, 5)
        assertTrue(style.bold)
        assertEquals(UnderlineStyle.WAVY, style.underline)

        // Toggle interactive checklist checkbox directly in the editor
        composeRule.onNodeWithTag("rich_checklist_toggle_b2").assertIsDisplayed().performClick()
        assertTrue((doc.blocks.first { it.id == "b2" } as RichBlock.TextBlock).checked)

        // Open Paragraph tray -> align center & indent
        composeRule.onNodeWithTag("format_paragraph_tray_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fmt_align_center").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("fmt_indent").performScrollTo().assertIsDisplayed().performClick()
        val alignedB1 = doc.blocks.first { it.id == "b1" } as RichBlock.TextBlock
        assertEquals(RichTextAlignment.CENTER, alignedB1.alignment)
        assertEquals(1, alignedB1.indentLevel)

        // Open Insert tray -> insert editable table and verify it renders
        composeRule.onNodeWithTag("format_blocks_tray_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insert_table_btn").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        val insertedTable = doc.blocks.firstOrNull { it is RichBlock.TableBlock } as? RichBlock.TableBlock
        assertNotNull(insertedTable)
        composeRule.onNodeWithTag("rich_table_${insertedTable!!.id}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `NoteAttachmentsSection renders attachment metadata and triggers download and remove callbacks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = AttachmentStorageManager(context)
        val saved = storage.saveAttachment(
            fileName = "architecture_spec.pdf",
            mimeType = "application/pdf",
            inputStream = ByteArrayInputStream("Spec Content".toByteArray()),
            noteId = "n1"
        ).getOrThrow()

        var downloadedName: String? = null
        var removedName: String? = null

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                NoteAttachmentsSection(
                    attachments = listOf(saved),
                    resolveFile = { storage.getFileFromRelativePath(it.relativePath) },
                    isReadOnly = false,
                    accentColor = Color(0xFFE07A5F),
                    isDark = false,
                    onOpenAttachment = {},
                    onDownloadAttachment = { downloadedName = it.fileName },
                    onRemoveAttachment = { removedName = it.fileName }
                )
            }
        }

        composeRule.onNodeWithTag("note_attachments_section").assertIsDisplayed()
        composeRule.onNodeWithText("architecture_spec.pdf").assertIsDisplayed()
        composeRule.onNodeWithTag("download_attachment_${saved.id}").performClick()
        composeRule.onNodeWithTag("remove_attachment_${saved.id}").performClick()

        assertEquals("architecture_spec.pdf", downloadedName)
        assertEquals("architecture_spec.pdf", removedName)
        storage.deleteFile(saved.relativePath)
    }
}
