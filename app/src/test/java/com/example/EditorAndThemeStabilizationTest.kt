package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.core.app.ApplicationProvider
import com.example.data.storage.AttachmentStorageManager
import com.example.security.SecurityManager
import com.example.ui.components.MarkdownVisualTransformation
import com.example.ui.components.VoiceDictationController
import com.example.ui.components.applyEditorialFormatting
import com.example.ui.components.parseRichMarkdownInline
import com.example.util.InkSample
import com.example.util.InkStroke
import com.example.util.NoteBlockType
import com.example.util.SmartNoteUtils
import com.example.util.VoiceRecordingClip
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorAndThemeStabilizationTest {

    @Test
    fun `blockOrder persists alongside style, ink, and voice recordings`() {
        val customOrder = listOf(
            NoteBlockType.TEXT,
            NoteBlockType.CHECKLIST,
            NoteBlockType.IMAGES,
            NoteBlockType.VOICE
        )
        val styledJson = SmartNoteUtils.withNoteStyleMeta(
            existingJson = "",
            textureKey = "dot_grid",
            backgroundImagePath = "/tmp/bg.jpg"
        )
        val withInk = SmartNoteUtils.withHandwritingStrokes(
            existingJson = styledJson,
            strokes = listOf(
                InkStroke(
                    id = "s1",
                    colorArgb = 0,
                    points = listOf(InkSample(10f, 20f, 0.5f), InkSample(30f, 40f, 0.7f))
                )
            )
        )
        val withVoice = SmartNoteUtils.withVoiceRecordings(
            existingJson = withInk,
            clips = listOf(
                VoiceRecordingClip(
                    id = "v1",
                    filePath = "voice/clip.m4a",
                    title = "Meeting Memo",
                    durationMs = 4200L
                )
            )
        )
        val finalJson = SmartNoteUtils.withBlockOrder(withVoice, customOrder)

        assertEquals(customOrder, SmartNoteUtils.parseBlockOrder(finalJson))
        assertEquals("dot_grid", SmartNoteUtils.parseNoteTexture(finalJson))
        assertEquals("/tmp/bg.jpg", SmartNoteUtils.parseNoteBackgroundImage(finalJson))
        assertEquals(1, SmartNoteUtils.parseHandwritingStrokes(finalJson).size)
        assertEquals(1, SmartNoteUtils.parseVoiceRecordings(finalJson).size)
        assertEquals(SmartNoteUtils.DEFAULT_NOTE_BLOCK_ORDER, SmartNoteUtils.parseBlockOrder(""))
    }

    @Test
    fun `applyEditorialFormatting handles headings, bold, italic, and underline toggles cleanly`() {
        // Heading toggle on single line
        val initialLine = TextFieldValue("My Heading", TextRange(5))
        val h1 = applyEditorialFormatting(initialLine, "# ", "")
        assertEquals("# My Heading", h1.text)
        assertEquals(TextRange(7), h1.selection)

        val h2 = applyEditorialFormatting(h1, "## ", "")
        assertEquals("## My Heading", h2.text)
        assertEquals(TextRange(8), h2.selection)

        val clearedHeading = applyEditorialFormatting(h2, "## ", "")
        assertEquals("My Heading", clearedHeading.text)
        assertEquals(TextRange(5), clearedHeading.selection)

        // Bold wrap + stack underline + unwrap bold
        val base = TextFieldValue("hello world", TextRange(6, 11))
        val bolded = applyEditorialFormatting(base, "**", "**")
        assertEquals("hello **world**", bolded.text)
        assertEquals(TextRange(8, 13), bolded.selection)

        val underlinedAndBold = applyEditorialFormatting(bolded, "<u>", "</u>")
        assertEquals("hello **<u>world</u>**", underlinedAndBold.text)
        assertEquals(TextRange(11, 16), underlinedAndBold.selection)

        val unwrappedUnderline = applyEditorialFormatting(underlinedAndBold, "<u>", "</u>")
        assertEquals("hello **world**", unwrappedUnderline.text)
        assertEquals(TextRange(8, 13), unwrappedUnderline.selection)

        // Italic on collapsed cursor places cursor inside markers, then steps out
        val collapsed = TextFieldValue("note ", TextRange(5))
        val italicInserted = applyEditorialFormatting(collapsed, "*", "*")
        assertEquals("note **", italicInserted.text)
        assertEquals(TextRange(6), italicInserted.selection)
        val steppedOut = applyEditorialFormatting(italicInserted, "*", "*")
        assertEquals(TextRange(7), steppedOut.selection)
    }

    @Test
    fun `MarkdownVisualTransformation uses identity offset mapping and styles headings, bold, italic, and underline`() {
        val raw = "# Title\n**Bold** *Italic* <u>Under</u>"
        val transformation = MarkdownVisualTransformation(
            baseFontSizeSp = 15f,
            accentColor = Color.Cyan,
            markerColor = Color.Gray,
            highlightBg = Color.Yellow,
            codeBg = Color.DarkGray,
            linkColor = Color.Blue
        )
        val result = transformation.filter(AnnotatedString(raw))
        assertEquals(OffsetMapping.Identity, result.offsetMapping)
        assertEquals(raw, result.text.text)

        val spanStyles = result.text.spanStyles
        assertTrue(spanStyles.any { (it.item.fontWeight?.weight ?: 0) >= FontWeight.Black.weight })
        assertTrue(spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun `parseRichMarkdownInline strips syntax markers cleanly for View Mode`() {
        val rendered = parseRichMarkdownInline(
            raw = "**Bold** *Italic* <u>Under</u> [Docs](https://example.com)",
            linkColor = Color.Cyan,
            highlightBg = Color.Yellow,
            codeBg = Color.Gray
        )
        assertEquals("Bold Italic Under Docs", rendered.text)
        assertTrue(rendered.spanStyles.any { (it.item.fontWeight?.weight ?: 0) >= FontWeight.Black.weight })
        assertTrue(rendered.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(rendered.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun `bold formatting never bleeds onto non-bold text across empty markers or trailing spaces`() {
        val transformation = MarkdownVisualTransformation(
            baseFontSizeSp = 16f,
            accentColor = Color.Cyan,
            markerColor = Color.Gray,
            highlightBg = Color.Yellow,
            codeBg = Color.DarkGray,
            linkColor = Color.Blue
        )

        // Case 1: Empty **** before non-bold text followed by **bold**
        val rawEmptyBefore = "**** non bold **bold**"
        val transformed1 = transformation.filter(AnnotatedString(rawEmptyBefore)).text
        val nonBoldStart1 = rawEmptyBefore.indexOf("non bold")
        val nonBoldEnd1 = nonBoldStart1 + "non bold".length
        val boldStart1 = rawEmptyBefore.lastIndexOf("bold")
        val boldEnd1 = boldStart1 + "bold".length

        assertFalse(
            transformed1.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Bold.weight &&
                    span.start < nonBoldEnd1 && span.end > nonBoldStart1
            }
        )
        assertTrue(
            transformed1.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Black.weight &&
                    span.start <= boldStart1 && span.end >= boldEnd1
            }
        )

        val preview1 = parseRichMarkdownInline(rawEmptyBefore, Color.Blue, Color.Yellow, Color.Gray)
        assertEquals(" non bold bold", preview1.text)
        val p1NonBoldStart = preview1.text.indexOf("non bold")
        val p1NonBoldEnd = p1NonBoldStart + "non bold".length
        assertFalse(
            preview1.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Bold.weight &&
                    span.start < p1NonBoldEnd && span.end > p1NonBoldStart
            }
        )
        val p1BoldStart = preview1.text.lastIndexOf("bold")
        assertTrue(
            preview1.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Black.weight &&
                    span.start <= p1BoldStart && span.end >= p1BoldStart + 4
            }
        )

        // Case 2: Trailing-space **first ** must never bridge bold across non-bold to **second**
        val rawSpaceMarker = "**first ** non bold **second**"
        val transformed2 = transformation.filter(AnnotatedString(rawSpaceMarker)).text
        val nonBoldStart2 = rawSpaceMarker.indexOf("non bold")
        val nonBoldEnd2 = nonBoldStart2 + "non bold".length
        val secondStart2 = rawSpaceMarker.indexOf("second")
        assertFalse(
            transformed2.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Bold.weight &&
                    span.start < nonBoldEnd2 && span.end > nonBoldStart2
            }
        )
        assertTrue(
            transformed2.spanStyles.any { span ->
                (span.item.fontWeight?.weight ?: 0) >= FontWeight.Black.weight &&
                    span.start <= secondStart2 && span.end >= secondStart2 + "second".length
            }
        )
    }

    @Test
    fun `applyEditorialFormatting trims trailing selection whitespace and toggles bold off cleanly`() {
        // Selection with trailing space wraps core word only
        val selectedWithSpace = TextFieldValue("hello world", TextRange(0, 6))
        val wrapped = applyEditorialFormatting(selectedWithSpace, "**", "**")
        assertEquals("**hello** world", wrapped.text)
        assertEquals(TextRange(2, 7), wrapped.selection)

        // Collapsed cursor after trailing space inside **hello |** moves space outside **hello** |
        val typingWithSpace = TextFieldValue("**hello **", TextRange(8))
        val toggledOff = applyEditorialFormatting(typingWithSpace, "**", "**")
        assertEquals("**hello** ", toggledOff.text)
        assertEquals(TextRange(10), toggledOff.selection)

        // Collapsed cursor inside **hel|lo** unwraps bold cleanly
        val midBold = TextFieldValue("**hello**", TextRange(5))
        val unwrapped = applyEditorialFormatting(midBold, "**", "**")
        assertEquals("hello", unwrapped.text)
        assertEquals(TextRange(3), unwrapped.selection)

        // Active style detector reports bold inside **...** and false on plain text
        assertTrue(com.example.ui.components.detectActiveEditorialStyles("**hello** world", TextRange(4)).isBold)
        assertFalse(com.example.ui.components.detectActiveEditorialStyles("**hello** world", TextRange(12)).isBold)
    }

    @Test
    fun `overlapping sticky notes are detected accurately and Move to Front or Back persists zIndex`() {
        val noteA = com.example.ui.viewmodel.StickyLayout(
            id = "a",
            x = 20f,
            y = 20f,
            width = 170f,
            height = 180f,
            zIndex = 0
        )
        val noteB = com.example.ui.viewmodel.StickyLayout(
            id = "b",
            x = 120f,
            y = 100f,
            width = 170f,
            height = 180f,
            zIndex = 1
        )
        val noteFar = com.example.ui.viewmodel.StickyLayout(
            id = "far",
            x = 420f,
            y = 420f,
            width = 170f,
            height = 180f,
            zIndex = 2
        )
        val noteTouching = com.example.ui.viewmodel.StickyLayout(
            id = "touch",
            x = 190f,
            y = 20f,
            width = 170f,
            height = 180f,
            zIndex = 3
        )
        val rotatedCornerClear = com.example.ui.viewmodel.StickyLayout(
            id = "rot_clear",
            x = 175f,
            y = 170f,
            width = 170f,
            height = 180f,
            rotationDeg = 45f,
            zIndex = 4
        )
        val rotatedOverlap = com.example.ui.viewmodel.StickyLayout(
            id = "rot_overlap",
            x = 140f,
            y = 135f,
            width = 170f,
            height = 180f,
            rotationDeg = 45f,
            zIndex = 5
        )

        assertTrue(com.example.ui.screens.doStickyNotesOverlap(noteA, noteB))
        assertFalse(com.example.ui.screens.doStickyNotesOverlap(noteA, noteFar))
        assertFalse(com.example.ui.screens.doStickyNotesOverlap(noteA, noteTouching))
        assertFalse(com.example.ui.screens.doStickyNotesOverlap(noteA, rotatedCornerClear))
        assertTrue(com.example.ui.screens.doStickyNotesOverlap(noteA, rotatedOverlap))

        val layoutsMap = mapOf(
            "a" to noteA,
            "b" to noteB,
            "far" to noteFar
        )
        assertEquals(setOf("b"), com.example.ui.screens.findOverlappingStickyIds("a", layoutsMap))
        assertEquals(emptySet<String>(), com.example.ui.screens.findOverlappingStickyIds("far", layoutsMap))

        // Verify ViewModel Move to Front and Move to Back + persistence
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val vm = com.example.ui.viewmodel.NoteViewModel(app)
        vm.updateStickyPosition("a", 20f, 20f, zIndex = 0)
        vm.updateStickyPosition("b", 100f, 90f, zIndex = 1)
        vm.updateStickyPosition("c", 130f, 110f, zIndex = 2)

        vm.moveStickyToFront("a", listOf("a", "b", "c"))
        val afterFront = vm.stickyLayouts.value
        assertTrue(afterFront.getValue("a").zIndex > afterFront.getValue("b").zIndex)
        assertTrue(afterFront.getValue("a").zIndex > afterFront.getValue("c").zIndex)

        vm.moveStickyToBack("a", listOf("a", "b", "c"))
        val afterBack = vm.stickyLayouts.value
        assertEquals(0, afterBack.getValue("a").zIndex)
        assertTrue(afterBack.getValue("a").zIndex < afterBack.getValue("b").zIndex)
        assertTrue(afterBack.getValue("a").zIndex < afterBack.getValue("c").zIndex)

        // Recreate ViewModel to verify zIndex persisted cleanly
        val vmReloaded = com.example.ui.viewmodel.NoteViewModel(app)
        assertEquals(0, vmReloaded.stickyLayouts.value.getValue("a").zIndex)
        assertTrue(vmReloaded.stickyLayouts.value.getValue("b").zIndex > 0)
    }

    @Test
    fun `VoiceDictationController does not auto-start or enable continuous loop by default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = VoiceDictationController(
            context = context,
            onFinalTranscript = { _, _ -> },
            onFallbackIntentRequested = {}
        )
        assertFalse(controller.isListening)
        assertFalse(controller.isContinuous)
        controller.destroy()
    }

    @Test
    fun `AttachmentStorageManager persists pasted image stream into private storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = AttachmentStorageManager(context)
        val sourceFile = File(context.cacheDir, "clipboard_sample.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }
        val sourceUri = Uri.fromFile(sourceFile)

        val result = storage.saveImageAttachmentFromUri(sourceUri)
        assertTrue(result.isSuccess)
        val savedPath = result.getOrThrow()
        assertTrue(savedPath.contains("attachments/pasted_"))
        val savedFile = File(savedPath)
        assertTrue(savedFile.exists())
        assertTrue(savedFile.length() == sourceFile.length())
        savedFile.delete()
        sourceFile.delete()
    }

    @Test
    fun `SecurityManager enforces 4-digit default PIN cleanly and handles empty PIN or salt without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("noteland_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val security = SecurityManager(context)
        assertFalse(security.verifyPin(""))
        assertEquals(0, security.getFailedAttempts())
        assertTrue(security.verifyPin("1234"))

        // Calling setVaultPin with empty string must be a safe no-op
        security.setVaultPin("")
        assertTrue(security.verifyPin("1234"))

        // Corrupted/empty salt in SharedPreferences must fail safely without InvalidKeySpecException
        prefs.edit().putString("vault_pin_salt", "").commit()
        assertFalse(security.verifyPin("1234"))

        prefs.edit().putString("vault_pin_salt", "%%%not-base64%%%").commit()
        assertFalse(security.verifyPin("1234"))
    }

    @Test
    fun `SafeAndroidTextToolbar guards against double ActionMode finish during paste and hide`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var finishCount = 0
        var lastCallback: android.view.ActionMode.Callback? = null
        val fakeMode = object : android.view.ActionMode() {
            private val testMenu = android.widget.PopupMenu(context, android.view.View(context)).menu
            override fun setTitle(title: CharSequence?) {}
            override fun setTitle(resId: Int) {}
            override fun setSubtitle(subtitle: CharSequence?) {}
            override fun setSubtitle(resId: Int) {}
            override fun setCustomView(view: android.view.View?) {}
            override fun invalidate() {}
            override fun finish() {
                finishCount++
                lastCallback?.onDestroyActionMode(this)
            }
            override fun getMenu(): android.view.Menu = testMenu
            override fun getTitle(): CharSequence? = null
            override fun getSubtitle(): CharSequence? = null
            override fun getCustomView(): android.view.View? = null
            override fun getMenuInflater(): android.view.MenuInflater = android.view.MenuInflater(context)
        }
        val fakeView = object : android.view.View(context) {
            override fun startActionMode(callback: android.view.ActionMode.Callback?, type: Int): android.view.ActionMode {
                lastCallback = callback
                callback?.onCreateActionMode(fakeMode, fakeMode.menu)
                return fakeMode
            }
        }

        val toolbar = com.example.ui.components.SafeAndroidTextToolbar(fakeView)
        toolbar.showMenu(
            rect = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 40f),
            onCopyRequested = null,
            onPasteRequested = {
                // Simulates BasicTextField calling textToolbar.hide() inside onPasteRequested
                toolbar.hide()
            },
            onCutRequested = null,
            onSelectAllRequested = null
        )

        val pasteItem = fakeMode.menu.getItem(0)
        lastCallback?.onActionItemClicked(fakeMode, pasteItem)
        toolbar.hide()
        toolbar.hide()

        assertEquals(1, finishCount)
        assertEquals(androidx.compose.ui.platform.TextToolbarStatus.Hidden, toolbar.status)
    }

    @Test
    fun `setAutoLockTimeoutWithPin rejects wrong PIN and updates timeout only on valid PIN`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.getSharedPreferences("noteland_vault_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val viewModel = com.example.ui.viewmodel.NoteViewModel(app)

        val initialTimeout = viewModel.autoLockTimeoutMs.value
        assertFalse(viewModel.verifyVaultPin("9999"))
        assertFalse(viewModel.setAutoLockTimeoutWithPin("9999", 300_000L))
        assertEquals(initialTimeout, viewModel.autoLockTimeoutMs.value)

        assertTrue(viewModel.verifyVaultPin("1234"))
        assertTrue(viewModel.setAutoLockTimeoutWithPin("1234", 300_000L))
        assertEquals(300_000L, viewModel.autoLockTimeoutMs.value)
        assertEquals(300_000L, viewModel.securityManager.getAutoLockTimeout())
    }

    @Test
    fun `light and dark Material You design system palettes and section identities match specification`() {
        // Light Theme Surface & Typography Palette
        assertEquals(Color(0xFFF8FAF9), com.example.ui.theme.NoteBackgroundLight)
        assertEquals(Color(0xFFFFFFFF), com.example.ui.theme.NoteSurfaceLight)
        assertEquals(Color(0xFFF1F5F4), com.example.ui.theme.NoteSurfaceVariantLight)
        assertEquals(Color(0xFFDCE5E0), com.example.ui.theme.NoteOutlineLight)
        assertEquals(Color(0xFF172B25), com.example.ui.theme.NoteOnBackgroundLight)
        assertEquals(Color(0xFF65766F), com.example.ui.theme.NoteOnSurfaceVariantLight)
        assertEquals(Color(0xFF168A4A), com.example.ui.theme.LightHomeAccent)
        assertEquals(Color(0xFF2563EB), com.example.ui.theme.LightFoldersAccent)
        assertEquals(Color(0xFF8B5CF6), com.example.ui.theme.LightStudioAccent)
        assertEquals(Color(0xFF15803D), com.example.ui.theme.LightProductivityAccent)
        assertEquals(Color(0xFF0E9A85), com.example.ui.theme.LightSettingsAccent)

        // Dark Theme Surface & Typography Palette (never pure black #000000)
        assertEquals(Color(0xFF111815), com.example.ui.theme.NoteBackgroundDark)
        assertEquals(Color(0xFF1B2420), com.example.ui.theme.NoteSurfaceDark)
        assertEquals(Color(0xFF252F2A), com.example.ui.theme.NoteSurfaceVariantDark)
        assertEquals(Color(0xFF303B35), com.example.ui.theme.NoteSurfaceContainerHighDark)
        assertEquals(Color(0xFF46544C), com.example.ui.theme.NoteOutlineDark)
        assertEquals(Color(0xFFF1F5F2), com.example.ui.theme.NoteOnBackgroundDark)
        assertEquals(Color(0xFFB5C1BA), com.example.ui.theme.NoteOnSurfaceVariantDark)
        assertEquals(Color(0xFF6BBF80), com.example.ui.theme.DarkHomeAccent)
        assertEquals(Color(0xFF6BA4F9), com.example.ui.theme.DarkFoldersAccent)
        assertEquals(Color(0xFFB694F6), com.example.ui.theme.DarkStudioAccent)
        assertEquals(Color(0xFF7BD68D), com.example.ui.theme.DarkProductivityAccent)
        assertEquals(Color(0xFF5DC3B0), com.example.ui.theme.DarkSettingsAccent)
        assertEquals(Color(0xFF111815), com.example.ui.theme.EditorialBackdropDarkBottom)

        // Section Color Palette parity across all 5 primary sections
        com.example.ui.theme.AppSection.entries.forEach { section ->
            val light = com.example.ui.theme.SectionColorPalette.forSection(section, isDark = false)
            val dark = com.example.ui.theme.SectionColorPalette.forSection(section, isDark = true)
            assertEquals(section, light.section)
            assertEquals(section, dark.section)
        }
    }

    @Test
    fun `parseMarkdownBlocks parses headings, quotes, checklists, lists, code blocks, and paragraphs`() {
        val raw = """
            # Main Title
            ## Subheading
            > Important quote
            - [ ] Pending task
            - [x] Done task
            - Bullet item
            1. Numbered step
            ```
            val x = 42
            ```
            Normal **bold** text
        """.trimIndent()

        val blocks = com.example.ui.components.parseMarkdownBlocks(raw)
        assertTrue(blocks[0] is com.example.ui.components.MarkdownBlock.Heading)
        assertEquals(1, (blocks[0] as com.example.ui.components.MarkdownBlock.Heading).level)
        assertEquals(2, (blocks[1] as com.example.ui.components.MarkdownBlock.Heading).level)
        assertEquals("Important quote", (blocks[2] as com.example.ui.components.MarkdownBlock.Quote).text)
        assertFalse((blocks[3] as com.example.ui.components.MarkdownBlock.Checklist).checked)
        assertTrue((blocks[4] as com.example.ui.components.MarkdownBlock.Checklist).checked)
        assertEquals("Bullet item", (blocks[5] as com.example.ui.components.MarkdownBlock.Bullet).text)
        assertEquals("1.", (blocks[6] as com.example.ui.components.MarkdownBlock.Numbered).marker)
        assertEquals("val x = 42", (blocks[7] as com.example.ui.components.MarkdownBlock.CodeBlock).code)
        assertEquals("Normal **bold** text", (blocks[8] as com.example.ui.components.MarkdownBlock.Paragraph).text)
        assertEquals(3, com.example.ui.components.EditorMarkdownMode.entries.size)
    }

    @Test
    fun `power optimization helpers compute exact hour boundaries, zero-alloc tag matches, and auto-lock delays`() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 14, 45, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val delayMs = com.example.util.millisUntilNextHour(cal.timeInMillis)
        // 15 minutes until 15:00:00.050 -> ~900_050ms
        assertTrue(delayMs in 900_000L..900_100L)

        assertTrue(com.example.util.noteHasTag("Work, Diary , Personal", "Diary"))
        assertTrue(com.example.util.noteHasTag("Work,Diary", "Work"))
        assertTrue(com.example.util.noteHasTag("Work,Diary", "Diary"))
        assertFalse(com.example.util.noteHasTag("Work,Diary", "Dia"))
        assertFalse(com.example.util.noteHasTag("", "Work"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("noteland_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val security = com.example.security.SecurityManager(context)
        assertEquals(0L, security.getRemainingAutoLockDelayMs())

        assertTrue(security.verifyPin("1234"))
        security.setAutoLockTimeout(60_000L)
        security.recordActivity()
        val remaining = security.getRemainingAutoLockDelayMs()
        assertTrue(remaining in 55_000L..60_000L)

        security.setAutoLockTimeout(0L)
        assertEquals(Long.MAX_VALUE, security.getRemainingAutoLockDelayMs())
    }
}
