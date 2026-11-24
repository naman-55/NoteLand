package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTagEntity
import com.example.data.model.NoteType
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TagEntity
import com.example.data.repository.NoteRepository
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.parseLegacyCanvasNoteStrokes
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.BackupManager
import com.example.util.ChecklistItem
import com.example.util.RestoreStrategy
import com.example.util.SmartNoteUtils
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
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
class Phase0StabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun openTempPersistentDb(context: Context, name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .addCallback(object : RoomDatabaseForeignKeyCallback() {})
            .allowMainThreadQueries()
            .build()
    }

    private abstract class RoomDatabaseForeignKeyCallback : androidx.room.RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            db.setForeignKeyConstraintsEnabled(true)
        }
    }

    // =========================================================
    // PRIORITY 1 & 2: NOTE CREATE, UPDATE & RESTART PERSISTENCE
    // =========================================================

    @Test
    fun `priority 1 and 2 - new note saves, updates preserve flags, and survives DB reopen`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase0_note_restart_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()

        val noteId = UUID.randomUUID().toString()
        val initialCreated = 1_710_000_000_000L

        val db1 = openTempPersistentDb(context, dbName)
        val repo1 = NoteRepository(db1, context)
        val hiddenNote = NoteEntity(
            id = noteId,
            title = "Initial Secret Note",
            content = "First draft content",
            tagsCsv = "Phase0,Critical",
            isPinned = true,
            isHidden = true,
            createdAtUtc = initialCreated,
            updatedAtUtc = initialCreated
        )
        val insertRes = repo1.saveOrUpdateNote(hiddenNote)
        assertTrue("Initial save must succeed", insertRes.isSuccess)
        db1.close()

        // Reopen DB (simulating cold app restart) and update existing note
        val db2 = openTempPersistentDb(context, dbName)
        val repo2 = NoteRepository(db2, context)
        val reloaded = repo2.getNoteByIdDirect(noteId)
        assertNotNull("Newly created note must survive reopen", reloaded)
        assertEquals("Initial Secret Note", reloaded!!.title)
        assertTrue("isHidden must persist", reloaded.isHidden)
        assertTrue("isPinned must persist", reloaded.isPinned)

        val updatedNote = reloaded.copy(
            title = "Updated Title",
            content = "Updated content after restart",
            tagsCsv = "Phase0,Verified",
            updatedAtUtc = initialCreated + 50_000L
        )
        assertTrue(repo2.saveOrUpdateNote(updatedNote).isSuccess)
        db2.close()

        // Reopen DB a third time and inspect updated fields
        val db3 = openTempPersistentDb(context, dbName)
        val afterUpdate = db3.noteDao().getNoteByIdDirect(noteId)
        assertNotNull(afterUpdate)
        assertEquals("Updated Title", afterUpdate!!.title)
        assertEquals("Updated content after restart", afterUpdate.content)
        assertEquals("Phase0,Verified", afterUpdate.tagsCsv)
        assertEquals("isHidden must not be stripped on update", true, afterUpdate.isHidden)
        assertEquals("createdAtUtc must be preserved across updates", initialCreated, afterUpdate.createdAtUtc)

        db3.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // PRIORITY 3: TO-DO / CHECKLIST PERSISTENCE & UI
    // =========================================================

    @Test
    fun `priority 3 - checklist JSON preserves stable IDs and handles legacy or duplicate IDs`() {
        val legacyJson = """[{"text":"Legacy A","done":false},{"text":"Legacy B","done":true}]"""
        val parsed = SmartNoteUtils.parseChecklistJson(legacyJson)
        assertEquals(2, parsed.size)
        assertEquals("Legacy A", parsed[0].text)
        assertFalse(parsed[0].isDone)
        assertTrue(parsed[1].isDone)
        assertTrue("Unique fallback IDs required", parsed[0].id != parsed[1].id)

        val duplicateIdsJson = """[{"id":"dup","text":"One","done":false},{"id":"dup","text":"Two","done":true}]"""
        val parsedDup = SmartNoteUtils.parseChecklistJson(duplicateIdsJson)
        assertEquals(2, parsedDup.size)
        assertTrue("Duplicate IDs must be deduplicated", parsedDup[0].id != parsedDup[1].id)

        val encoded = SmartNoteUtils.encodeChecklistJson(
            listOf(ChecklistItem(id = "stable-id-99", text = "Ship Phase 0", isDone = true))
        )
        val reparsed = SmartNoteUtils.parseChecklistJson(encoded)
        assertEquals(1, reparsed.size)
        assertEquals("stable-id-99", reparsed[0].id)
        assertEquals("Ship Phase 0", reparsed[0].text)
        assertTrue(reparsed[0].isDone)
    }

    @Test
    fun `priority 3 - toggleNoteChecklistItem updates sequentially and skips locked notes`() = runBlocking<Unit> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)

        val unlockedId = "phase0-chk-unlocked-${UUID.randomUUID()}"
        val lockedId = "phase0-chk-locked-${UUID.randomUUID()}"
        val unlockedNote = NoteEntity(
            id = unlockedId,
            title = "Checklist Task",
            checklistJson = SmartNoteUtils.encodeChecklistJson(
                listOf(
                    ChecklistItem(id = "c1", text = "First", isDone = false),
                    ChecklistItem(id = "c2", text = "Second", isDone = false)
                )
            )
        )
        val lockedNote = NoteEntity(
            id = lockedId,
            title = "enc_title",
            content = "enc_body",
            isLocked = true,
            checklistJson = "enc_ciphertext_do_not_overwrite"
        )
        assertTrue(vm.saveNoteDirect(unlockedNote).isSuccess)
        assertTrue(vm.saveNoteDirect(lockedNote).isSuccess)

        // Rapidly toggle index 0 and then index 1 passing the same stale object snapshot
        vm.toggleNoteChecklistItem(unlockedNote, 0)
        vm.toggleNoteChecklistItem(unlockedNote, 1)
        vm.toggleNoteChecklistItem(lockedNote, 0)

        delay(400)

        val reloadedUnlocked = vm.getNoteByIdDirect(unlockedId)!!
        val items = SmartNoteUtils.parseChecklistJson(reloadedUnlocked.checklistJson)
        assertTrue("First checklist item toggle must stick", items[0].isDone)
        assertTrue("Second checklist item toggle must stick", items[1].isDone)

        val reloadedLocked = vm.getNoteByIdDirect(lockedId)!!
        assertEquals("Locked note ciphertext must remain untouched", "enc_ciphertext_do_not_overwrite", reloadedLocked.checklistJson)

        vm.deletePermanently(unlockedId)
        vm.deletePermanently(lockedId)
    }

    @Test
    fun `priority 3 - NoteEditorScreen checklist items add and persist cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val uniqueTitle = "Phase0 Checklist ${UUID.randomUUID()}"
        var navigatedBack = false

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                NoteEditorScreen(
                    noteId = "0",
                    initialNoteType = "CHECKLIST",
                    viewModel = vm,
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("note_title_input").performTextInput(uniqueTitle)
        composeRule.onNodeWithTag("new_checklist_input").performScrollTo().performTextInput("Buy milk")
        composeRule.onNodeWithTag("add_checklist_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_checklist_input").performScrollTo().performTextInput("Write Phase 0 tests")
        composeRule.onNodeWithTag("add_checklist_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Verify View mode renders both persisted checklist items
        composeRule.onNodeWithTag("preview_toggle_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runBlocking {
                vm.repository.getAllNotesDirect().any {
                    it.title == uniqueTitle &&
                        SmartNoteUtils.parseChecklistJson(it.checklistJson).size == 2
                }
            }
        }
        composeRule.waitForIdle()

        assertTrue(composeRule.onAllNodesWithText("Buy milk").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Write Phase 0 tests").fetchSemanticsNodes().isNotEmpty())

        composeRule.onNodeWithTag("note_editor_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) { navigatedBack }
        assertTrue(navigatedBack)

        val persisted = runBlocking {
            vm.repository.getAllNotesDirect().firstOrNull { it.title == uniqueTitle }
        }
        assertNotNull("Checklist note must be persisted in SQLite", persisted)
        val parsed = SmartNoteUtils.parseChecklistJson(persisted!!.checklistJson)
        assertEquals(2, parsed.size)
        assertEquals("Buy milk", parsed[0].text)
        assertEquals("Write Phase 0 tests", parsed[1].text)

        runBlocking { vm.repository.deletePermanently(persisted.id) }
    }

    // =========================================================
    // PRIORITY 4: BOARDS, LEGACY CANVAS & ZOOM/PAN
    // =========================================================

    @Test
    fun `priority 4 - CanvasScreen cold start loads direct DB metadata and zoom controls work`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val canvasId = "phase0-canvas-${UUID.randomUUID()}"
        val originalCreated = 1_700_123_456_000L

        runBlocking {
            vm.repository.saveCanvasWithElements(
                CanvasEntity(
                    id = canvasId,
                    title = "Architecture Board",
                    backgroundColorHex = "#FFFBEB",
                    width = 2400f,
                    height = 1600f,
                    createdAtUtc = originalCreated,
                    updatedAtUtc = originalCreated
                ),
                listOf(
                    CanvasElementEntity(
                        id = "el-1",
                        canvasId = canvasId,
                        elementType = "TEXT",
                        elementDataJson = """{"type":"TEXT","text":"Core Module","startX":120.0,"startY":140.0,"color":-16777216,"width":5.0}""",
                        zIndex = 0
                    )
                )
            )
        }

        var navigatedBack = false
        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                CanvasScreen(
                    noteId = canvasId,
                    viewModel = vm,
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Architecture Board").assertIsDisplayed()
        composeRule.onNodeWithText("Saved").assertIsDisplayed()

        composeRule.onNodeWithTag("canvas_zoom_in_btn").performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("125%").assertIsDisplayed()

        composeRule.onNodeWithTag("canvas_zoom_reset_btn").performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("100%").assertIsDisplayed()

        composeRule.onNodeWithTag("canvas_save_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_back_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(navigatedBack)

        val savedCanvas = runBlocking {
            delay(200)
            vm.getCanvasByIdDirect(canvasId)
        }
        assertNotNull(savedCanvas)
        assertEquals("Architecture Board", savedCanvas!!.title)
        assertEquals(originalCreated, savedCanvas.createdAtUtc)
        assertEquals(2400f, savedCanvas.width, 0.01f)
        assertEquals(1600f, savedCanvas.height, 0.01f)

        runBlocking { vm.repository.deleteCanvas(canvasId) }
    }

    @Test
    fun `priority 4 - parseLegacyCanvasNoteStrokes hydrates seeded NoteType CANVAS format`() {
        val raw = """[{"color":-16744193,"width":5.0,"points":[{"x":120.0,"y":200.0},{"x":180.0,"y":240.0}]},{"color":-3381760,"width":4.0,"points":[{"x":260.0,"y":220.0}]}]"""
        val parsed = parseLegacyCanvasNoteStrokes(raw)
        assertEquals(2, parsed.size)
        assertEquals("STROKE", parsed[0].type)
        assertEquals(2, parsed[0].points.size)
        assertEquals(120f, parsed[0].points[0].x, 0.01f)
        assertEquals(-3381760, parsed[1].colorArgb)
    }

    // =========================================================
    // PRIORITY 5: CALENDAR 7-DAY DISPLAY & PRIVACY
    // =========================================================

    @Test
    fun `priority 5 - getWeekDaysStartingMonday always returns 7 Mon-Sun days across DST and leap boundaries`() {
        val origTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val testDates = listOf(
                Triple(2024, Calendar.FEBRUARY, 29), // Leap day (Thursday)
                Triple(2024, Calendar.DECEMBER, 31), // Year boundary week Mon Dec 30 .. Sun Jan 5
                Triple(2025, Calendar.MARCH, 9),     // US DST Spring Forward Sunday
                Triple(2025, Calendar.NOVEMBER, 2)   // US DST Fall Back Sunday
            )
            for ((yr, mo, dy) in testDates) {
                val cal = Calendar.getInstance().apply {
                    set(yr, mo, dy, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val week = SmartNoteUtils.getWeekDaysStartingMonday(cal.timeInMillis)
                assertEquals("Week must always contain 7 days", 7, week.size)

                val cFirst = Calendar.getInstance().apply { timeInMillis = week.first() }
                val cLast = Calendar.getInstance().apply { timeInMillis = week.last() }
                assertEquals("Week must start on Monday", Calendar.MONDAY, cFirst.get(Calendar.DAY_OF_WEEK))
                assertEquals("Week must end on Sunday", Calendar.SUNDAY, cLast.get(Calendar.DAY_OF_WEEK))
                assertEquals("All 7 days must be distinct", 7, week.distinct().size)

                for (i in 0 until 6) {
                    val cCurr = Calendar.getInstance().apply { timeInMillis = week[i] }
                    val cNext = Calendar.getInstance().apply { timeInMillis = week[i + 1] }
                    cCurr.add(Calendar.DAY_OF_MONTH, 1)
                    assertEquals(cCurr.get(Calendar.YEAR), cNext.get(Calendar.YEAR))
                    assertEquals(cCurr.get(Calendar.DAY_OF_YEAR), cNext.get(Calendar.DAY_OF_YEAR))
                }
            }
        } finally {
            TimeZone.setDefault(origTz)
        }
    }

    @Test
    fun `priority 5 - CalendarScreen renders all 7 weekdays and masks locked note reminder titles`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val lockedId = "phase0-cal-locked-${UUID.randomUUID()}"
        val remId = "phase0-cal-rem-${UUID.randomUUID()}"
        val todayNoon = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis()) + 12 * 3600_000L

        runBlocking {
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = lockedId,
                    title = "ENCRYPTED_CIPHER_BLOB_XYZ",
                    content = "SECRET_CIPHER_BODY",
                    isLocked = true,
                    reminderTimestamp = todayNoon
                )
            )
            vm.repository.saveReminder(
                ReminderEntity(
                    id = remId,
                    noteId = lockedId,
                    scheduledTimeUtc = todayNoon,
                    isCompleted = false
                )
            )
        }

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                CalendarScreen(
                    viewModel = vm,
                    initialTab = 0,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        val expectedWeek = SmartNoteUtils.getWeekDaysStartingMonday(System.currentTimeMillis())
        val dayFmt = java.text.SimpleDateFormat("d", java.util.Locale.getDefault())
        expectedWeek.forEach { dayMs ->
            val dayLabel = dayFmt.format(java.util.Date(dayMs))
            composeRule.onNodeWithTag("calendar_day_$dayLabel").assertIsDisplayed()
        }

        assertTrue(composeRule.onAllNodesWithText("ENCRYPTED_CIPHER_BLOB_XYZ").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Locked Note Reminder").assertIsDisplayed()

        runBlocking {
            vm.repository.deleteReminder(remId)
            vm.repository.deletePermanently(lockedId)
        }
    }

    // =========================================================
    // PRIORITY 6: BACKUP & RESTORE FIDELITY + SAFE MERGE
    // =========================================================

    @Test
    fun `priority 6 - encrypted ZIP backup preserves all entities, timestamps, backgrounds, layouts, and safe MERGE`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase.setForeignKeyConstraintsEnabled(true)
        val repo = NoteRepository(db, context)
        val backupManager = BackupManager(context, repo)

        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        val previousLayouts = settingsPrefs.getString("sticky_board_layouts", null)
        val bgDir = File(context.filesDir, "note_backgrounds")
        val bgFile = File(bgDir, "phase0_bg_${UUID.randomUUID()}.bin")

        try {
            bgDir.mkdirs()
            val expectedBgBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
            bgFile.writeBytes(expectedBgBytes)

            val folderId = "f-phase0"
            val tagId = "t-phase0"
            val noteId = "n-phase0"
            val canvasId = "c-phase0"
            val attachId = "a-phase0"
            val annId = "ann-phase0"
            val stickyId = "s-phase0"
            val remId = "r-phase0"
            val bmId = "bm-phase0"

            repo.insertFolder(FolderEntity(id = folderId, name = "Knowledge", colorHex = "#FF0000", createdAtUtc = 111_000L))
            repo.insertTag(TagEntity(id = tagId, name = "Phase0Tag", colorHex = "#00FF00", createdAtUtc = 222_000L))
            repo.saveOrUpdateNote(
                NoteEntity(
                    id = noteId,
                    folderId = folderId,
                    folderName = "Knowledge",
                    title = "Backed Up Note",
                    content = "Body",
                    tagsCsv = "Phase0Tag",
                    isDeleted = true,
                    deletedAtUtc = 333_000L,
                    canvasDataJson = SmartNoteUtils.withNoteStyleMeta("", "linen", bgFile.absolutePath),
                    createdAtUtc = 100_000L,
                    updatedAtUtc = 200_000L
                )
            )
            repo.insertNoteTags(listOf(NoteTagEntity(noteId = noteId, tagId = tagId)))
            repo.insertStickyNote(
                StickyNoteEntity(
                    id = stickyId,
                    noteId = noteId,
                    title = "Sticky",
                    content = "Pinned",
                    createdAtUtc = 444_000L,
                    updatedAtUtc = 555_000L
                )
            )
            settingsPrefs.edit().putString(
                "sticky_board_layouts",
                """{"$stickyId":{"x":120.5,"y":240.5,"width":210.0,"height":220.0,"rotation":-15.0,"zIndex":3}}"""
            ).commit()

            repo.saveCanvasWithElements(
                CanvasEntity(
                    id = canvasId,
                    title = "Canvas Export",
                    backgroundColorHex = "#FFFFFF",
                    width = 2560f,
                    height = 1440f,
                    createdAtUtc = 666_000L,
                    updatedAtUtc = 777_000L
                ),
                listOf(
                    CanvasElementEntity(
                        id = "cel-1",
                        canvasId = canvasId,
                        elementType = "RECTANGLE",
                        elementDataJson = """{"type":"RECTANGLE"}""",
                        zIndex = 2,
                        createdAtUtc = 666_500L
                    )
                )
            )
            repo.saveAttachmentWithAnnotations(
                AttachmentEntity(
                    id = attachId,
                    noteId = noteId,
                    fileName = "doc.pdf",
                    fileExtension = "pdf",
                    relativePath = "attachments/doc.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 42L,
                    createdAtUtc = 888_000L
                ),
                listOf(
                    AnnotationEntity(
                        id = annId,
                        attachmentId = attachId,
                        annotationType = "INK",
                        pageNumber = 1,
                        coordinatesJson = "{}",
                        createdAtUtc = 888_500L
                    )
                )
            )
            repo.saveReminder(
                ReminderEntity(
                    id = remId,
                    noteId = noteId,
                    scheduledTimeUtc = 999_000L,
                    createdAtUtc = 998_000L
                )
            )
            repo.insertBookmark(BookmarkEntity(id = bmId, targetType = "NOTE", targetId = noteId, label = "Fav", createdAtUtc = 101_000L))

            // 1. Export encrypted ZIP & test wrong-password rejection
            val zipFile = backupManager.createBackupZip("StrongPass99!")
            val wrongPass = backupManager.restoreFromZip(zipFile, RestoreStrategy.REPLACE, "WrongPass")
            assertTrue("Wrong password must fail without wiping DB", wrongPass.isFailure)
            assertNotNull("Existing data untouched after failed restore", repo.getNoteByIdDirect(noteId))

            // 2. Wipe local state and restore via REPLACE
            repo.clearAllData()
            bgFile.delete()
            settingsPrefs.edit().remove("sticky_board_layouts").commit()
            assertNull(repo.getNoteByIdDirect(noteId))
            assertFalse(bgFile.exists())

            val replaceRes = backupManager.restoreFromZip(zipFile, RestoreStrategy.REPLACE, "StrongPass99!")
            assertTrue("REPLACE restore must succeed", replaceRes.isSuccess)

            val restoredNote = repo.getNoteByIdDirect(noteId)!!
            assertEquals(333_000L, restoredNote.deletedAtUtc)
            assertEquals(1, db.noteDao().getAllNoteTagsDirect().count { it.noteId == noteId })

            val restoredSticky = db.stickyNoteDao().getStickyNoteById(stickyId)!!
            assertEquals(noteId, restoredSticky.noteId)
            assertEquals(444_000L, restoredSticky.createdAtUtc)
            assertTrue(settingsPrefs.getString("sticky_board_layouts", "")!!.contains(stickyId))

            val restoredCanvas = repo.getCanvasByIdDirect(canvasId)!!
            assertEquals(2560f, restoredCanvas.width, 0.01f)
            assertEquals(1440f, restoredCanvas.height, 0.01f)
            assertEquals(666_000L, restoredCanvas.createdAtUtc)

            val restoredBgFile = File(SmartNoteUtils.parseNoteBackgroundImage(restoredNote.canvasDataJson))
            assertTrue("Background image file must be restored", restoredBgFile.exists())
            assertArrayEquals(expectedBgBytes, restoredBgFile.readBytes())

            // 3. Test MERGE preserves existing local children on colliding parent IDs (no ON DELETE CASCADE!)
            val localExtraReminderId = "r-local-extra"
            val localExtraElementId = "cel-local-extra"
            repo.saveReminder(ReminderEntity(id = localExtraReminderId, noteId = noteId, scheduledTimeUtc = 1_500_000L))
            db.canvasDao().insertElements(
                listOf(
                    CanvasElementEntity(
                        id = localExtraElementId,
                        canvasId = canvasId,
                        elementType = "TEXT",
                        elementDataJson = "{}",
                        zIndex = 5
                    )
                )
            )

            val mergeRes = backupManager.restoreFromZip(zipFile, RestoreStrategy.MERGE, "StrongPass99!")
            assertTrue("MERGE restore must succeed", mergeRes.isSuccess)
            assertNotNull("Local child reminder must NOT be cascade-deleted on MERGE", repo.getReminderById(localExtraReminderId))
            assertTrue(
                "Local canvas element must NOT be cascade-deleted on MERGE",
                repo.getElementsForCanvasDirect(canvasId).any { it.id == localExtraElementId }
            )

            // 4. Test JSON export & restore preserves full payload too
            val jsonFile = backupManager.saveBackupToFile()
            repo.clearAllData()
            val jsonRestoreRes = backupManager.restoreFromJson(jsonFile.readText())
            assertTrue("JSON restore must succeed", jsonRestoreRes.isSuccess)
            assertEquals(2560f, repo.getCanvasByIdDirect(canvasId)!!.width, 0.01f)
            assertEquals(1, db.noteDao().getAllNoteTagsDirect().count { it.noteId == noteId })

            zipFile.delete()
            jsonFile.delete()
        } finally {
            if (bgFile.exists()) bgFile.delete()
            if (previousLayouts != null) {
                settingsPrefs.edit().putString("sticky_board_layouts", previousLayouts).commit()
            } else {
                settingsPrefs.edit().remove("sticky_board_layouts").commit()
            }
            db.close()
        }
    }

    // =========================================================
    // DATABASE INTEGRITY & FOREIGN KEYS
    // =========================================================

    @Test
    fun `database integrity check passes and foreign keys enforce SET_NULL and CASCADE properly`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase.setForeignKeyConstraintsEnabled(true)
        val repo = NoteRepository(db, context)

        db.openHelper.writableDatabase.query("PRAGMA integrity_check").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("ok", c.getString(0))
        }
        db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { c ->
            assertFalse("No foreign key violations expected", c.moveToFirst())
        }

        val folder = FolderEntity(id = "fk-folder", name = "Work")
        repo.insertFolder(folder)
        val note = NoteEntity(id = "fk-note", folderId = folder.id, folderName = "Work", title = "FK Test")
        repo.saveOrUpdateNote(note)
        val rem = ReminderEntity(id = "fk-rem", noteId = note.id, scheduledTimeUtc = 100_000L)
        repo.saveReminder(rem)

        // Delete folder -> note survives with folderId = null (SET_NULL)
        repo.deleteFolder(folder.id)
        val afterFolderDelete = repo.getNoteByIdDirect(note.id)
        assertNotNull(afterFolderDelete)
        assertNull(afterFolderDelete!!.folderId)

        // Soft-delete -> tombstone recorded; Permanent delete -> cascades reminder
        repo.softDeleteNote(afterFolderDelete)
        val deletedItems = db.deletedItemDao().getAllDeletedItems().first()
        assertTrue(deletedItems.any { it.originalId == note.id })

        repo.deletePermanently(note.id)
        assertNull(repo.getNoteByIdDirect(note.id))
        assertNull(repo.getReminderById(rem.id))

        db.close()
    }
}
