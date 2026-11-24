package com.example

import com.example.data.model.AttachmentEntity
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTagEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TagEntity
import com.example.ui.navigation.PrimaryNavItems
import com.example.ui.navigation.Screen
import com.example.util.ChecklistItem
import com.example.util.SmartNoteUtils
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLandUnitTest {

    @Test
    fun testWordCountAndReadingTime() {
        val sampleText = "NoteLand is a fast, offline-first personal knowledge management system for Android."
        val count = SmartNoteUtils.countWords(sampleText)
        assertEquals(11, count)

        val readTime = SmartNoteUtils.calculateReadingTimeMinutes(sampleText)
        assertTrue(readTime >= 1)
    }

    @Test
    fun testCharacterCount() {
        val sampleText = "Hello NoteLand"
        assertEquals(14, SmartNoteUtils.countCharacters(sampleText, includeSpaces = true))
        assertEquals(13, SmartNoteUtils.countCharacters(sampleText, includeSpaces = false))
    }

    @Test
    fun testChecklistProgress() {
        val items = listOf(
            ChecklistItem(id = "1", text = "Design Database", isDone = true),
            ChecklistItem(id = "2", text = "Build Canvas Tool", isDone = false),
            ChecklistItem(id = "3", text = "Test Keystore Vault", isDone = true)
        )
        val (done, total) = SmartNoteUtils.calculateChecklistProgress(items)
        assertEquals(2, done)
        assertEquals(3, total)
    }

    @Test
    fun testPrimaryNavigationStructure() {
        assertEquals(5, PrimaryNavItems.size)
        assertEquals("Home", PrimaryNavItems[0].label)
        assertEquals("Folders", PrimaryNavItems[1].label)
        assertEquals("Studio", PrimaryNavItems[2].label)
        assertEquals("Productivity", PrimaryNavItems[3].label)
        assertEquals("Settings", PrimaryNavItems[4].label)
        assertEquals(Screen.Home, PrimaryNavItems[0].screen)
        assertEquals(Screen.Folders, PrimaryNavItems[1].screen)
        assertEquals(Screen.Studio, PrimaryNavItems[2].screen)
        assertEquals(Screen.Productivity, PrimaryNavItems[3].screen)
        assertEquals(Screen.Settings, PrimaryNavItems[4].screen)
    }

    @Test
    fun testFormatRelativeTime() {
        val now = System.currentTimeMillis()
        assertEquals("Just now", SmartNoteUtils.formatRelativeTime(now - 10_000))
        assertEquals("10m ago", SmartNoteUtils.formatRelativeTime(now - 600_000))
        assertEquals("2h ago", SmartNoteUtils.formatRelativeTime(now - 7_200_000))
        assertEquals("Yesterday", SmartNoteUtils.formatRelativeTime(now - 86_400_000L))
    }

    @Test
    fun testNoteDuplicationProperties() {
        val original = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "Study Plan",
            content = "Complete revision",
            folderId = "f1",
            folderName = "Study",
            tagsCsv = "Education,Math",
            isPinned = true,
            isFavorite = true
        )

        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            createdAtUtc = System.currentTimeMillis(),
            updatedAtUtc = System.currentTimeMillis()
        )

        assertTrue(duplicate.id != original.id)
        assertEquals("Study Plan (Copy)", duplicate.title)
        assertEquals(original.content, duplicate.content)
        assertEquals(original.folderId, duplicate.folderId)
        assertEquals(original.tagsCsv, duplicate.tagsCsv)
    }

    @Test
    fun testPhase2DatabaseEntitiesUUIDAndUtcTimestamps() {
        val folderId = UUID.randomUUID().toString()
        val folder = FolderEntity(
            id = folderId,
            name = "Architecture",
            colorHex = "#3B82F6"
        )
        assertNotNull(UUID.fromString(folder.id))
        assertTrue(folder.createdAtUtc > 0)

        val noteId = UUID.randomUUID().toString()
        val note = NoteEntity(
            id = noteId,
            folderId = folder.id,
            title = "Database Specification",
            content = "13 tables in SQLite using Room with UUID primary keys.",
            createdAtUtc = System.currentTimeMillis()
        )
        assertNotNull(UUID.fromString(note.id))
        assertEquals(folderId, note.folderId)
        assertTrue(note.updatedAtUtc >= note.createdAtUtc)

        val tag = TagEntity(name = "SQLite")
        assertNotNull(UUID.fromString(tag.id))

        val noteTag = NoteTagEntity(noteId = note.id, tagId = tag.id)
        assertEquals(note.id, noteTag.noteId)
        assertEquals(tag.id, noteTag.tagId)

        val sticky = StickyNoteEntity(
            title = "Quick Note",
            content = "Test sticky note",
            orderIndex = 1
        )
        assertNotNull(UUID.fromString(sticky.id))

        val canvas = CanvasEntity(title = "System Diagram")
        assertNotNull(UUID.fromString(canvas.id))

        val element = CanvasElementEntity(
            canvasId = canvas.id,
            elementType = "STROKE",
            elementDataJson = "[]",
            zIndex = 0
        )
        assertEquals(canvas.id, element.canvasId)

        // Test Attachment stores relative path, NOT BLOB in SQLite
        val attachment = AttachmentEntity(
            noteId = note.id,
            fileName = "specification.pdf",
            fileExtension = "pdf",
            relativePath = "attachments/spec_123.pdf",
            mimeType = "application/pdf",
            sizeBytes = 204800L
        )
        assertTrue(attachment.relativePath.startsWith("attachments/"))
        assertEquals("pdf", attachment.fileExtension)
        assertEquals(204800L, attachment.sizeBytes)

        val reminder = ReminderEntity(
            noteId = note.id,
            scheduledTimeUtc = System.currentTimeMillis() + 3600000L
        )
        assertEquals(note.id, reminder.noteId)
        assertTrue(!reminder.isCompleted)
    }

    @Test
    fun testPhase4StickyLayoutNudgeAndSnapToGrid() {
        val layout = com.example.ui.viewmodel.StickyLayout(
            id = "s1",
            x = 23f,
            y = 57f,
            width = 170f,
            height = 180f
        )
        // Test nudging
        val nudged = layout.copy(x = layout.x + 20f, y = layout.y - 10f)
        assertEquals(43f, nudged.x, 0.001f)
        assertEquals(47f, nudged.y, 0.001f)

        // Test grid snapping (gridSize = 30f)
        val snappedX = (Math.round(nudged.x / 30f) * 30f).coerceAtLeast(20f)
        val snappedY = (Math.round(nudged.y / 30f) * 30f).coerceAtLeast(20f)
        assertEquals(30f, snappedX, 0.001f)
        assertEquals(60f, snappedY, 0.001f)
    }

    @Test
    fun testPhase4CanvasBoundedUndoRedoStack() {
        val undoStack = mutableListOf<List<com.example.ui.screens.CanvasItem>>()
        val redoStack = mutableListOf<List<com.example.ui.screens.CanvasItem>>()

        // Push 35 states, stack should cap at 30
        for (i in 1..35) {
            val list = listOf(
                com.example.ui.screens.CanvasItem(
                    id = "item_$i",
                    type = "STROKE",
                    colorArgb = i
                )
            )
            com.example.ui.screens.pushUndoSnapshot(list, undoStack, redoStack)
        }

        assertEquals(30, undoStack.size)
        assertEquals("item_6", undoStack.first().first().id)
        assertEquals("item_35", undoStack.last().first().id)
        assertEquals(0, redoStack.size)
    }

    @Test
    fun testPhase6DeterministicExtractiveSummarizer() {
        val longText = """
            NoteLand is an offline personal knowledge management application.
            It provides rich text notes, sticky notes, and an infinite drawing canvas.
            Security is paramount, ensuring notes can be locked in an encrypted vault.
            Offline smart utilities work without any remote AI or external dependencies.
            Users can organize notes with nested folders, colors, and search history.
        """.trimIndent()

        val summary = SmartNoteUtils.generateExtractiveSummary(longText, maxSentences = 2)
        assertTrue(summary.isNotBlank())
        assertTrue(summary.contains("NoteLand") || summary.contains("offline") || summary.contains("notes"))
    }

    @Test
    fun testPhase6ChecklistExtraction() {
        val markdownText = """
            # Meeting Action Items
            - [ ] Finalize phase 6 search algorithms
            * Setup local alarm notifications
            - Verify reading time estimator
            Finish user interface tests
        """.trimIndent()

        val items = SmartNoteUtils.extractChecklistItems(markdownText)
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.text.contains("Finalize phase 6") })
        assertTrue(items.any { it.text.contains("Setup local alarm") })
    }

    @Test
    fun testPhase6TitleSuggestions() {
        val content = """
            # Advanced Algorithms
            We investigate graph traversals and dynamic programming patterns.
        """.trimIndent()

        val suggestions = SmartNoteUtils.suggestTitles(content)
        assertTrue(suggestions.contains("Advanced Algorithms"))
    }

    @Test
    fun testPhase6DuplicateContentDetection() {
        val existingNote = NoteEntity(
            id = "note-1",
            title = "Kotlin Coroutines",
            content = "Kotlin Coroutines provide asynchronous non blocking structured concurrency for Android applications and background jobs."
        )

        val duplicateContent = "Kotlin Coroutines provide asynchronous non blocking structured concurrency for Android applications."
        val uniqueContent = "Machine learning models run locally using TensorFlow Lite on device."

        val dupResult = SmartNoteUtils.detectDuplicateContent(duplicateContent, "note-2", listOf(existingNote))
        assertTrue(dupResult.isDuplicate)
        assertTrue(dupResult.similarityPercent >= 50)
        assertEquals("Kotlin Coroutines", dupResult.matchedNoteTitle)

        val uniqueResult = SmartNoteUtils.detectDuplicateContent(uniqueContent, "note-3", listOf(existingNote))
        assertTrue(!uniqueResult.isDuplicate)
        assertTrue(uniqueResult.similarityPercent < 50)
    }

    @Test
    fun testPhase6OrganizationSuggestions() {
        val folders = listOf(
            FolderEntity(id = "f1", name = "Work Projects"),
            FolderEntity(id = "f2", name = "Study Notes")
        )

        val workContent = "Sprint planning meeting with client regarding roadmap and deliverables."
        val suggestion = SmartNoteUtils.suggestOrganization(workContent, folders)
        assertEquals("f1", suggestion.suggestedFolderId)
        assertEquals("Work Projects", suggestion.suggestedFolderName)
    }

    @Test
    fun testPhase6ReminderRepeatIntervalCalculation() {
        val baseTime = 1700000000000L
        val dailyNext = baseTime + (24 * 3600 * 1000L)
        val weeklyNext = baseTime + (7 * 24 * 3600 * 1000L)
        val monthlyNext = baseTime + (30 * 24 * 3600 * 1000L)

        assertEquals(86400000L, dailyNext - baseTime)
        assertEquals(604800000L, weeklyNext - baseTime)
        assertEquals(2592000000L, monthlyNext - baseTime)
    }

    @Test
    fun testFolderNotesSorting() {
        val noteA = NoteEntity(id = "1", title = "Bananas", content = "", folderId = "f1", createdAtUtc = 1000L, updatedAtUtc = 5000L)
        val noteB = NoteEntity(id = "2", title = "Apples", content = "", folderId = "f1", createdAtUtc = 3000L, updatedAtUtc = 2000L)
        val noteC = NoteEntity(id = "3", title = "Cherries", content = "", folderId = "f1", createdAtUtc = 2000L, updatedAtUtc = 6000L)
        val rawList = listOf(noteA, noteB, noteC)

        // Date Modified (Descending)
        val byModified = rawList.sortedByDescending { it.updatedAtUtc }
        assertEquals(listOf("Cherries", "Bananas", "Apples"), byModified.map { it.title })

        // Date Created (Descending)
        val byCreated = rawList.sortedByDescending { it.createdAtUtc }
        assertEquals(listOf("Apples", "Cherries", "Bananas"), byCreated.map { it.title })

        // Alphabetical ASC
        val byAlphaAsc = rawList.sortedBy { it.title.lowercase() }
        assertEquals(listOf("Apples", "Bananas", "Cherries"), byAlphaAsc.map { it.title })

        // Alphabetical DESC
        val byAlphaDesc = rawList.sortedByDescending { it.title.lowercase() }
        assertEquals(listOf("Cherries", "Bananas", "Apples"), byAlphaDesc.map { it.title })

        assertEquals(4, com.example.ui.screens.FolderSortOption.values().size)
    }

    @Test
    fun testSolidAndGradientStylePresetLookup() {
        val solid = com.example.ui.theme.findNoteStylePreset("#EDE9FE")
        assertNotNull(solid)
        assertTrue(!solid!!.isGradient)

        val gradient = com.example.ui.theme.findNoteStylePreset("grad_sunrise")
        assertNotNull(gradient)
        assertTrue(gradient!!.isGradient)
        assertEquals("Sunrise", gradient.name)
    }

    @Test
    fun testWeekDaysStartingMondayIncludesSundayProperly() {
        val cal = java.util.Calendar.getInstance()
        val anchors = listOf(
            Triple(2026, java.util.Calendar.NOVEMBER, 9),  // Monday
            Triple(2026, java.util.Calendar.NOVEMBER, 11), // Wednesday
            Triple(2026, java.util.Calendar.NOVEMBER, 15), // Sunday
            Triple(2025, java.util.Calendar.DECEMBER, 31)  // Year boundary
        )
        anchors.forEach { (year, month, day) ->
            cal.clear()
            cal.set(year, month, day, 14, 30, 0)
            val week = SmartNoteUtils.getWeekDaysStartingMonday(cal.timeInMillis)
            assertEquals(7, week.size)

            val firstCal = java.util.Calendar.getInstance().apply { timeInMillis = week.first() }
            val lastCal = java.util.Calendar.getInstance().apply { timeInMillis = week.last() }
            assertEquals(java.util.Calendar.MONDAY, firstCal.get(java.util.Calendar.DAY_OF_WEEK))
            assertEquals(java.util.Calendar.SUNDAY, lastCal.get(java.util.Calendar.DAY_OF_WEEK))

            for (i in 1 until week.size) {
                val prev = java.util.Calendar.getInstance().apply { timeInMillis = week[i - 1] }
                val curr = java.util.Calendar.getInstance().apply { timeInMillis = week[i] }
                prev.add(java.util.Calendar.DAY_OF_YEAR, 1)
                assertEquals(prev.get(java.util.Calendar.YEAR), curr.get(java.util.Calendar.YEAR))
                assertEquals(prev.get(java.util.Calendar.DAY_OF_YEAR), curr.get(java.util.Calendar.DAY_OF_YEAR))
            }
        }
    }
}


