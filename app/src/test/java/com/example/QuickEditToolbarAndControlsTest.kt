package com.example

import android.app.Application
import android.content.Context
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.DictationTarget
import com.example.ui.components.HandwritingOverlayToolbar
import com.example.ui.components.INK_ADAPTIVE_ARGB
import com.example.ui.components.RichFormatBar
import com.example.util.NoteInkTool
import com.example.ui.components.VoiceDictationBar
import com.example.ui.components.VoiceDictationController
import com.example.ui.components.formatSpeechErrorMessage
import com.example.ui.components.mergeDictatedSegment

import com.example.ui.editor.components.RichEditorToolbar
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.engine.RichEditorHistoryManager
import com.example.ui.editor.engine.RichEditorHistorySnapshot
import com.example.ui.editor.engine.RichSpanEngine
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.QuickEditConfigReducer
import com.example.ui.editor.model.QuickEditTool
import com.example.ui.editor.model.QuickEditToolbarConfig
import com.example.ui.editor.model.QuickEditToolbarConfigCodec
import com.example.ui.editor.model.QuickEditToolbarPreset
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCellSelection
import com.example.ui.editor.model.UnderlineStyle
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
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
class QuickEditToolbarAndControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    // =========================================================================
    // 1. QUICK EDIT TOOLBAR CUSTOMIZATION REDUCER
    // =========================================================================

    @Test
    fun `customization reducer supports add remove reorder frequent colors underline and restore`() {
        var config = QuickEditToolbarConfig.default()

        // Default config includes core requested quick-edit tools
        assertTrue(QuickEditTool.BOLD in config.enabledTools)
        assertTrue(QuickEditTool.TEXT_COLOR in config.enabledTools)
        assertTrue(QuickEditTool.TEXT_BG_COLOR in config.enabledTools)
        assertTrue(QuickEditTool.HIGHLIGHT in config.enabledTools)
        assertTrue(QuickEditTool.UNDERLINE in config.enabledTools)
        assertTrue(QuickEditTool.WAVY_UNDERLINE in config.enabledTools)
        assertTrue(QuickEditTool.ALIGNMENT in config.enabledTools)
        assertTrue(QuickEditTool.BULLET_LIST in config.enabledTools)
        assertTrue(QuickEditTool.NUMBERED_LIST in config.enabledTools)
        assertTrue(QuickEditTool.INDENT in config.enabledTools)
        assertTrue(QuickEditTool.QUOTE in config.enabledTools)
        assertTrue(QuickEditTool.CALLOUT in config.enabledTools)
        assertTrue(QuickEditTool.DIVIDER in config.enabledTools)
        assertTrue(QuickEditTool.TABLE in config.enabledTools)
        assertTrue(QuickEditTool.ATTACHMENT in config.enabledTools)

        // Add tool not enabled by default
        assertFalse(QuickEditTool.SPREADSHEET in config.enabledTools)
        config = QuickEditConfigReducer.addTool(config, QuickEditTool.SPREADSHEET)
        assertTrue(QuickEditTool.SPREADSHEET in config.enabledTools)

        // Adding same tool twice is idempotent
        val sizeAfterAdd = config.enabledTools.size
        config = QuickEditConfigReducer.addTool(config, QuickEditTool.SPREADSHEET)
        assertEquals(sizeAfterAdd, config.enabledTools.size)

        // Remove a tool (also clears it from frequentTools)
        config = QuickEditConfigReducer.toggleFrequentTool(config, QuickEditTool.DIVIDER)
        assertTrue(QuickEditTool.DIVIDER in config.frequentTools)
        config = QuickEditConfigReducer.removeTool(config, QuickEditTool.DIVIDER)
        assertFalse(QuickEditTool.DIVIDER in config.enabledTools)
        assertFalse(QuickEditTool.DIVIDER in config.frequentTools)

        // Rearrange tool positions: move ITALIC from index 1 to index 0
        assertEquals(QuickEditTool.BOLD, config.enabledTools[0])
        assertEquals(QuickEditTool.ITALIC, config.enabledTools[1])
        config = QuickEditConfigReducer.moveTool(config, 1, 0)
        assertEquals(QuickEditTool.ITALIC, config.enabledTools[0])
        assertEquals(QuickEditTool.BOLD, config.enabledTools[1])

        // Configure default colors & preferred underline style
        val customTextHex = EditorColorPalette.textColors[6].hex!!
        val customBgHex = EditorColorPalette.textBackgroundColors[2].hex!!
        val customHlHex = EditorColorPalette.highlightColors[3].hex!!
        config = QuickEditConfigReducer.setDefaultTextColor(config, customTextHex)
        config = QuickEditConfigReducer.setDefaultBackgroundColor(config, customBgHex)
        config = QuickEditConfigReducer.setDefaultHighlightColor(config, customHlHex)
        config = QuickEditConfigReducer.setPreferredUnderlineStyle(config, UnderlineStyle.WAVY)

        assertEquals(customTextHex, config.defaultTextColorHex)
        assertEquals(customBgHex, config.defaultBackgroundColorHex)
        assertEquals(customHlHex, config.defaultHighlightColorHex)
        assertEquals(UnderlineStyle.WAVY, config.preferredUnderlineStyle)

        // Guard: cannot remove the very last tool
        var singleToolConfig = config.copy(enabledTools = listOf(QuickEditTool.BOLD))
        singleToolConfig = QuickEditConfigReducer.removeTool(singleToolConfig, QuickEditTool.BOLD)
        assertEquals(listOf(QuickEditTool.BOLD), singleToolConfig.enabledTools)

        // Restore defaults
        val restored = QuickEditConfigReducer.restoreDefaults()
        assertEquals(QuickEditToolbarConfig.default(), restored)
        assertEquals(UnderlineStyle.STANDARD, restored.preferredUnderlineStyle)
    }

    @Test
    fun `preset reducer switches cleanly between Simple Writing and Full while preserving defaults`() {
        val base = QuickEditToolbarConfig.default().copy(
            defaultTextColorHex = "#2563EB",
            defaultHighlightColorHex = "#FBCFE8",
            preferredUnderlineStyle = UnderlineStyle.WAVY
        )

        val simple = QuickEditConfigReducer.applyPreset(base, QuickEditToolbarPreset.SIMPLE)
        assertEquals(QuickEditToolbarPreset.SIMPLE.tools, simple.enabledTools)
        assertEquals(7, simple.enabledTools.size)
        assertEquals("#2563EB", simple.defaultTextColorHex)
        assertEquals("#FBCFE8", simple.defaultHighlightColorHex)
        assertEquals(UnderlineStyle.WAVY, simple.preferredUnderlineStyle)

        val writing = QuickEditConfigReducer.applyPreset(simple, QuickEditToolbarPreset.WRITING)
        assertEquals(QuickEditToolbarPreset.WRITING.tools, writing.enabledTools)
        assertEquals(11, writing.enabledTools.size)
        assertEquals(UnderlineStyle.WAVY, writing.preferredUnderlineStyle)

        val full = QuickEditConfigReducer.applyPreset(writing, QuickEditToolbarPreset.FULL)
        assertEquals(QuickEditToolbarConfig.DEFAULT_TOOLS, full.enabledTools)
        assertEquals("#2563EB", full.defaultTextColorHex)
    }

    // =========================================================================
    // 2. PERSISTENCE & APP RESTART SAFETY
    // =========================================================================

    @Test
    fun `config codec round-trips cleanly and survives future unknown tools or malformed json`() {
        val custom = QuickEditToolbarConfig(
            enabledTools = listOf(
                QuickEditTool.HIGHLIGHT,
                QuickEditTool.BOLD,
                QuickEditTool.TABLE,
                QuickEditTool.SPREADSHEET
            ),
            frequentTools = setOf(QuickEditTool.HIGHLIGHT, QuickEditTool.TABLE),
            defaultTextColorHex = "#2563EB",
            defaultBackgroundColorHex = "#E0F2FE",
            defaultHighlightColorHex = "#FBCFE8",
            preferredUnderlineStyle = UnderlineStyle.WAVY
        )

        val encoded = QuickEditToolbarConfigCodec.encode(custom)
        val decoded = QuickEditToolbarConfigCodec.decode(encoded)
        assertEquals(custom, decoded)

        // Forward compatibility: future schema version + unknown tool IDs never reset valid user prefs
        val futureJson = """
            {
              "v": 3,
              "enabled": ["quote", "future_ai_magic_tool", "table", "future_chart_tool"],
              "frequent": ["quote", "future_ai_magic_tool"],
              "defaultTextColor": "#9333EA",
              "defaultBgColor": "#F3E8FF",
              "defaultHighlightColor": "#BAE6FD",
              "underlineStyle": "WAVY"
            }
        """.trimIndent()
        val futureDecoded = QuickEditToolbarConfigCodec.decode(futureJson)
        assertEquals(listOf(QuickEditTool.QUOTE, QuickEditTool.TABLE), futureDecoded.enabledTools)
        assertEquals(setOf(QuickEditTool.QUOTE), futureDecoded.frequentTools)
        assertEquals("#9333EA", futureDecoded.defaultTextColorHex)
        assertEquals("#F3E8FF", futureDecoded.defaultBackgroundColorHex)
        assertEquals("#BAE6FD", futureDecoded.defaultHighlightColorHex)
        assertEquals(UnderlineStyle.WAVY, futureDecoded.preferredUnderlineStyle)

        // Corrupted payload gracefully falls back to defaults instead of crashing
        assertEquals(QuickEditToolbarConfig.default(), QuickEditToolbarConfigCodec.decode("{not-valid-json"))
        assertEquals(QuickEditToolbarConfig.default(), QuickEditToolbarConfigCodec.decode(""))
    }

    @Test
    fun `NoteViewModel persists custom quick edit config across simulated app restarts`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val firstSessionVm = NoteViewModel(app)
        val customized = QuickEditToolbarConfig(
            enabledTools = listOf(
                QuickEditTool.UNDERLINE,
                QuickEditTool.HIGHLIGHT,
                QuickEditTool.CALLOUT,
                QuickEditTool.TABLE,
                QuickEditTool.ATTACHMENT
            ),
            frequentTools = setOf(QuickEditTool.UNDERLINE, QuickEditTool.CALLOUT),
            defaultTextColorHex = "#0D9488",
            defaultBackgroundColorHex = "#CCFBF1",
            defaultHighlightColorHex = "#BBF7D0",
            preferredUnderlineStyle = UnderlineStyle.WAVY
        )

        firstSessionVm.updateQuickEditConfig(customized)
        assertEquals(customized, firstSessionVm.quickEditConfig.value)

        // Simulate full application restart with a brand-new ViewModel instance
        val restartedVm = NoteViewModel(app)
        assertEquals(customized, restartedVm.quickEditConfig.value)

        // Reset to defaults and verify restart picks up defaults cleanly
        restartedVm.resetQuickEditConfig()
        assertEquals(QuickEditToolbarConfig.default(), restartedVm.quickEditConfig.value)
        val afterResetVm = NoteViewModel(app)
        assertEquals(QuickEditToolbarConfig.default(), afterResetVm.quickEditConfig.value)
    }

    // =========================================================================
    // 3. MICROPHONE PERMISSIONS, PROCESSING STATE & CURSOR-ACCURATE INSERTION
    // =========================================================================

    @Test
    fun `voice dictation handles permissions errors unavailable service and exact cursor insertion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Verify platform error messages including permission denied
        assertEquals(
            "Microphone permission required",
            formatSpeechErrorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        )
        assertEquals(
            "Network speech service unavailable",
            formatSpeechErrorMessage(SpeechRecognizer.ERROR_NETWORK)
        )
        assertEquals(
            "No speech detected",
            formatSpeechErrorMessage(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        )

        // Controller reports permission / unavailable service errors safely
        val controller = VoiceDictationController(
            context = context,
            onFinalTranscript = { _, _ -> },
            onFallbackIntentRequested = {}
        )
        controller.reportError("Microphone permission denied — allow mic access to use voice input")
        assertFalse(controller.isListening)
        assertFalse(controller.isProcessing)
        assertEquals(
            "Microphone permission denied — allow mic access to use voice input",
            controller.errorMessage
        )
        controller.clearError()
        assertNull(controller.errorMessage)

        // Exact cursor insertion: start of empty text (auto-capitalizes)
        val atStart = mergeDictatedSegment(TextFieldValue("", TextRange(0)), "hello world")
        assertEquals("Hello world", atStart.text)
        assertEquals(TextRange(11), atStart.selection)

        // Exact cursor insertion: middle of sentence with smart spacing
        val inMiddle = mergeDictatedSegment(TextFieldValue("Meet at 5pm", TextRange(4)), "Sarah")
        assertEquals("Meet Sarah at 5pm", inMiddle.text)
        assertEquals(TextRange(10), inMiddle.selection)

        // Exact cursor insertion: replacing selected words
        val replaced = mergeDictatedSegment(TextFieldValue("Draft old plan now", TextRange(6, 9)), "updated")
        assertEquals("Draft updated plan now", replaced.text)
        assertEquals(TextRange(13), replaced.selection)

        // Sentence boundary capitalization after period
        val afterSentence = mergeDictatedSegment(TextFieldValue("First line.", TextRange(11)), "second thought")
        assertEquals("First line. Second thought", afterSentence.text)
        assertEquals(TextRange(26), afterSentence.selection)
    }

    @Test
    fun `VoiceDictationBar displays processing state listening state and error retry state`() {
        var isListening by mutableStateOf(true)
        var isProcessing by mutableStateOf(true)
        var errorMsg by mutableStateOf<String?>(null)
        var retryClicks = 0
        var stopClicks = 0

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                VoiceDictationBar(
                    isListening = isListening,
                    isReady = false,
                    isProcessing = isProcessing,
                    isContinuous = false,
                    target = DictationTarget.BODY,
                    partialTranscript = "",
                    normalizedLevel = 0.2f,
                    errorMessage = errorMsg,
                    onToggleContinuous = {},
                    onToggleTarget = {},
                    onRetry = { retryClicks++ },
                    onStop = { stopClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        // Processing state shows spinner + "Processing speech…"
        composeRule.onNodeWithTag("voice_processing_indicator").assertIsDisplayed()
        composeRule.onNodeWithText("Processing speech…").assertIsDisplayed()

        // Switch to error state (e.g. microphone permission denied or unavailable service)
        composeRule.runOnIdle {
            isListening = false
            isProcessing = false
            errorMsg = "Speech recognition is not available on this device"
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Speech recognition is not available on this device").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_retry_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retryClicks)
    }

    // =========================================================================
    // 4. UNDO / REDO ACROSS TEXT, FORMATTING, BLOCKS & TABLE EDITS
    // =========================================================================

    @Test
    fun `RichEditorHistoryManager undoes and redoes text formatting block and table edits preserving selection`() {
        val history = RichEditorHistoryManager(maxEntries = 50)
        val initialBlock = RichBlock.TextBlock(id = "b1", text = "Initial note")
        var current = RichEditorHistorySnapshot(
            title = "Roadmap",
            document = RichNoteDocument(blocks = listOf(initialBlock)),
            activeBlockId = "b1",
            blockSelections = mapOf("b1" to TextRange(0, 7)),
            selectedTableCell = null
        )

        // Step 0: Title edit
        history.recordBeforeChange(current)
        current = current.copy(title = "Roadmap Q4")

        // Step 1: Text edit
        history.recordBeforeChange(current)
        current = current.copy(
            document = RichDocumentReducer.updateTextBlock(current.document, "b1") { it.copy(text = "Initial note updated") },
            blockSelections = mapOf("b1" to TextRange(20))
        )

        // Step 2: Inline formatting (Bold + Wavy Underline on "Initial")
        history.recordBeforeChange(current)
        val b1AfterText = current.document.blocks.first() as RichBlock.TextBlock
        val formattedSpans = RichSpanEngine.toggleUnderline(
            RichSpanEngine.toggleBold(b1AfterText.spans, b1AfterText.text.length, 0, 7),
            b1AfterText.text.length,
            0,
            7,
            UnderlineStyle.WAVY
        )
        current = current.copy(
            document = RichDocumentReducer.updateTextBlock(current.document, "b1") { it.copy(spans = formattedSpans) },
            blockSelections = mapOf("b1" to TextRange(0, 7))
        )

        // Step 3: Insert Table block
        history.recordBeforeChange(current)
        val tableBlock = RichDocumentReducer.createDefaultTable(isSpreadsheet = false, rows = 2, cols = 2)
        val insertRes = RichDocumentReducer.insertBlockAfter(current.document, "b1", tableBlock)
        current = current.copy(
            document = insertRes.document,
            activeBlockId = tableBlock.id,
            selectedTableCell = TableCellSelection(tableBlock.id, 1, 1)
        )

        // Step 4: Edit table cell (1, 1)
        history.recordBeforeChange(current)
        current = current.copy(
            document = RichDocumentReducer.updateTableCell(current.document, tableBlock.id, 1, 1) {
                it.copy(text = "Q4 Target")
            },
            selectedTableCell = TableCellSelection(tableBlock.id, 1, 1)
        )
        assertEquals(5, history.undoDepth)
        assertFalse(history.canRedo)

        // Undo Step 4 -> cell returns to empty, table selection preserved
        current = history.undo(current)!!
        val undoneTable = current.document.blocks.filterIsInstance<RichBlock.TableBlock>().single()
        assertEquals("", undoneTable.rows[1][1].text)
        assertEquals(TableCellSelection(tableBlock.id, 1, 1), current.selectedTableCell)

        // Undo Step 3 -> table removed, formatted spans still intact on b1 with [0..7] selection
        current = history.undo(current)!!
        assertTrue(current.document.blocks.none { it is RichBlock.TableBlock })
        val b1Formatted = current.document.blocks.first() as RichBlock.TextBlock
        assertTrue(RichSpanEngine.resolveActiveStyleAt(b1Formatted.spans, b1Formatted.text.length, 0, 7).bold)
        assertEquals(
            UnderlineStyle.WAVY,
            RichSpanEngine.resolveActiveStyleAt(b1Formatted.spans, b1Formatted.text.length, 0, 7).underline
        )
        assertEquals(TextRange(0, 7), current.blockSelections["b1"])

        // Undo Step 2 -> formatting removed, text still "Initial note updated"
        current = history.undo(current)!!
        val b1Plain = current.document.blocks.first() as RichBlock.TextBlock
        assertEquals("Initial note updated", b1Plain.text)
        assertFalse(RichSpanEngine.resolveActiveStyleAt(b1Plain.spans, b1Plain.text.length, 0, 7).bold)

        // Undo Step 1 -> back to "Initial note" with original selection [0..7] while title stays "Roadmap Q4"
        current = history.undo(current)!!
        assertEquals("Initial note", (current.document.blocks.first() as RichBlock.TextBlock).text)
        assertEquals("Roadmap Q4", current.title)
        assertEquals(TextRange(0, 7), current.blockSelections["b1"])

        // Undo Step 0 -> title restored back to "Roadmap"
        current = history.undo(current)!!
        assertEquals("Roadmap", current.title)
        assertFalse(history.canUndo)
        assertEquals(5, history.redoDepth)

        // Redo all 5 steps back to edited table cell
        repeat(5) {
            current = history.redo(current)!!
        }
        assertEquals("Roadmap Q4", current.title)
        val redoneTable = current.document.blocks.filterIsInstance<RichBlock.TableBlock>().single()
        assertEquals("Q4 Target", redoneTable.rows[1][1].text)
        assertFalse(history.canRedo)

        // Verify snapshot sanitization clamps out-of-bounds selections & table coordinates safely
        val corruptedSnapshot = RichEditorHistorySnapshot(
            title = "Safe",
            document = RichNoteDocument(blocks = listOf(RichBlock.TextBlock(id = "b1", text = "Hi"))),
            activeBlockId = "missing_block",
            blockSelections = mapOf("b1" to TextRange(0, 999), "ghost" to TextRange(0, 5)),
            selectedTableCell = TableCellSelection("missing_table", 9, 9)
        )
        val safe = history.sanitizeSnapshot(corruptedSnapshot)
        assertEquals("b1", safe.activeBlockId)
        assertEquals(TextRange(0, 2), safe.blockSelections["b1"])
        assertFalse("ghost" in safe.blockSelections)
        assertNull(safe.selectedTableCell)
    }

    // =========================================================================
    // 5. BOTTOM TOOLBAR, QUICK EDIT STRIP, THEME CONTROLS & CUSTOMIZATION UI
    // =========================================================================

    @Test
    fun `RichEditorToolbar renders custom tools toggles Quick Edit applies defaults switches themes and launches Pen`() {
        var wavyTriggered = false
        var selectedTextHex: String? = null
        var selectedBgHex: String? = null
        var selectedHlHex: String? = null
        var switchedTheme: AppThemeMode? = null
        var penOpened = false
        var eraserOpened = false

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                RichEditorToolbar(
                    plainTextForStats = "Quick edit toolbar test note",
                    activeStyle = InlineFormatStyle(),
                    activeBlockKind = RichBlockKind.PARAGRAPH,
                    activeAlignment = RichTextAlignment.LEFT,
                    activeIndentLevel = 0,
                    onToggleBold = {},
                    onToggleItalic = {},
                    onToggleUnderlineStandard = {},
                    onToggleUnderlineWavy = { wavyTriggered = true },
                    onToggleStrikethrough = {},
                    onToggleInlineCode = {},
                    onClearFormatting = {},
                    onSelectTextColor = { selectedTextHex = it },
                    onSelectTextBackgroundColor = { selectedBgHex = it },
                    onSelectHighlightColor = { selectedHlHex = it },
                    onSetAlignment = {},
                    onIncreaseIndent = {},
                    onDecreaseIndent = {},
                    onSetBlockKind = {},
                    onInsertDivider = {},
                    onInsertTable = {},
                    onInsertSpreadsheet = {},
                    onOpenLinkDialog = {},
                    onAttachImage = {},
                    onAttachPdf = {},
                    onAttachDocument = {},
                    onPasteImage = {},
                    selectedNoteColorHex = "#FFFFFF",
                    onSelectNoteColorHex = {},
                    onOpenNoteColorSheet = {},
                    onOpenUtilities = {},
                    onOpenDrawing = { penOpened = true },
                    onOpenEraser = { eraserOpened = true },
                    currentAppThemeMode = AppThemeMode.LIGHT,
                    onSelectAppThemeMode = { switchedTheme = it },
                    hasInkStrokes = true,
                    onStartVoiceDictation = {},
                    isDictating = false,
                    onToggleVoiceRecording = {},
                    isRecordingAudio = false
                )
            }
        }
        composeRule.waitForIdle()

        // Quick Edit strip is visible and respects preferred underline + default colors
        composeRule.onNodeWithTag("quick_edit_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("format_underline_btn").performClick()
        composeRule.onNodeWithTag("quick_tool_text_color").performClick()
        composeRule.onNodeWithTag("quick_tool_text_bg_color").performClick()
        composeRule.onNodeWithTag("quick_tool_highlight").performClick()
        composeRule.waitForIdle()

        assertTrue(wavyTriggered)
        assertEquals("#2563EB", selectedTextHex)
        assertEquals("#E0F2FE", selectedBgHex)
        assertEquals("#FBCFE8", selectedHlHex)

        // Toggle Quick Edit strip collapsed & back expanded
        composeRule.onNodeWithTag("quick_edit_toggle_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quick_edit_toggle_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quick_edit_strip").assertIsDisplayed()

        // Open Theme controls tray from bottom toolbar, switch to Dark theme, and dismiss tray via Close button
        composeRule.onNodeWithTag("note_style_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_active_tray").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_mode_dark_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.DARK, switchedTheme)
        composeRule.onNodeWithTag("close_editor_tray_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_active_tray").assertDoesNotExist()

        // Pen, Eraser, and Customize buttons in bottom toolbar
        composeRule.onNodeWithTag("note_draw_overlay_button").performClick()
        composeRule.onNodeWithTag("note_eraser_button").performClick()
        composeRule.waitForIdle()

        assertTrue(penOpened)
        assertTrue(eraserOpened)
    }



    // =========================================================================
    // 6. EXISTING PEN INTERACTIONS REGRESSION CHECK
    // =========================================================================

    @Test
    fun `HandwritingOverlayToolbar preserves all pen highlighter eraser and palm rejection controls`() {
        var activeTool by mutableStateOf(NoteInkTool.PRESSURE_PEN)
        var stylusOnly by mutableStateOf(false)

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                HandwritingOverlayToolbar(
                    activeTool = activeTool,
                    activeColorArgb = INK_ADAPTIVE_ARGB,
                    activeWidthDp = 4f,
                    stylusOnlyMode = stylusOnly,
                    stylusDetected = false,
                    hasStrokes = true,
                    onSelectTool = { activeTool = it },
                    onSelectColorArgb = {},
                    onSelectWidthDp = {},
                    onToggleStylusOnly = { stylusOnly = !stylusOnly },
                    onClearAll = {},
                    onDone = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ink_tool_fine_pen").performClick()
        composeRule.waitForIdle()
        assertEquals(NoteInkTool.FINE_PEN, activeTool)

        composeRule.onNodeWithTag("ink_tool_highlighter").performClick()
        composeRule.waitForIdle()
        assertEquals(NoteInkTool.HIGHLIGHTER, activeTool)

        composeRule.onNodeWithTag("ink_tool_eraser").performClick()
        composeRule.waitForIdle()
        assertEquals(NoteInkTool.ERASER, activeTool)

        composeRule.onNodeWithTag("ink_stylus_only_toggle").performClick()
        composeRule.waitForIdle()
        assertTrue(stylusOnly)
    }

    @Test
    fun `Markdown mode RichFormatBar exposes theme switching and pen actions`() {
        var selectedTheme: AppThemeMode? = null
        var penClicked = false

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                RichFormatBar(
                    content = "Markdown note",
                    selection = TextRange(0),
                    onInsertMarkdown = { _, _ -> },
                    onOpenDrawing = { penClicked = true },
                    onOpenUtilities = {},
                    currentAppThemeMode = AppThemeMode.LIGHT,
                    onSelectAppThemeMode = { selectedTheme = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("format_palette_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_mode_ivory_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.IVORY, selectedTheme)

        composeRule.onNodeWithTag("format_draw_overlay_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(penClicked)
    }
}
