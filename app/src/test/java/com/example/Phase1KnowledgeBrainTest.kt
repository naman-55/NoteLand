package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AttachmentEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteLinkEntity
import com.example.data.model.NoteTemplateEntity
import com.example.data.model.NoteType
import com.example.data.model.TagEntity
import com.example.data.repository.NoteRepository
import com.example.ui.screens.PinnedNotesScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.BackupManager
import com.example.util.KnowledgeBrainEngine
import com.example.util.RestoreStrategy
import com.example.util.SmartNoteUtils
import java.util.UUID
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
class Phase1KnowledgeBrainTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun openIsolatedDb(context: Context, name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            })
            .allowMainThreadQueries()
            .build()
    }

    // =========================================================
    // 1. SAFE ROOM MIGRATION 3 -> 4 & SCHEMA INTEGRITY
    // =========================================================

    @Test
    fun `safe MIGRATION_3_4 preserves existing v3 data, builds indexed Brain tables, and seeds templates`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_migration_v3_v4_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)

        // Build authentic v3 schema using pre-Phase-1 SQL statements and seed a v3 folder, tag, and note
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        AppDatabase.MIGRATION_1_2.migrate(db)
                        AppDatabase.MIGRATION_2_3.migrate(db)

                        db.execSQL("INSERT INTO `folders` VALUES ('f-v3', NULL, 'Legacy Research', '#4F46E5', 'folder', 1000, 1000)")
                        db.execSQL("INSERT INTO `tags` VALUES ('t-v3', 'Architecture', '#10B981', 1000)")
                        db.execSQL(
                            "INSERT INTO `notes` (`id`, `folderId`, `folderName`, `title`, `content`, `noteType`, `colorHex`, `tagsCsv`, `isPinned`, `isArchived`, `isDeleted`, `isLocked`, `isHidden`, `isFavorite`, `reminderTimestamp`, `canvasDataJson`, `checklistJson`, `imageUri`, `createdAtUtc`, `updatedAtUtc`, `deletedAtUtc`) " +
                                "VALUES ('n-v3', 'f-v3', 'Legacy Research', 'V3 Existing Note', 'Links to [[V3 Target]]', 'STANDARD', '#FFFFFF', 'Architecture', 1, 0, 0, 0, 0, 0, NULL, '', '[]', '', 111111, 222222, NULL)"
                        )
                        db.execSQL("INSERT INTO `app_settings` VALUES ('db_initialized', 'true', 1000)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        // Open with Room v4 and MIGRATION_3_4
        val v4Db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(v4Db)

        // Verify v3 note & folder survived intact
        val preservedNote = v4Db.noteDao().getNoteByIdDirect("n-v3")
        assertNotNull("v3 note must survive migration 3->4", preservedNote)
        assertEquals("V3 Existing Note", preservedNote!!.title)
        assertEquals("f-v3", preservedNote.folderId)
        assertEquals(111111L, preservedNote.createdAtUtc)

        // Verify Phase 1 tables and built-in templates exist
        val templates = v4Db.noteTemplateDao().getAllTemplatesDirect()
        assertEquals("5 built-in templates must be seeded on upgraded DB", 5, templates.size)
        assertTrue(templates.all { it.isBuiltIn })

        // Verify SQLite integrity & foreign key checks pass cleanly
        val sqlite = v4Db.openHelper.writableDatabase
        sqlite.query("PRAGMA integrity_check").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("ok", c.getString(0))
        }
        sqlite.query("PRAGMA foreign_key_check").use { c ->
            assertFalse("No FK violations permitted after MIGRATION_3_4", c.moveToFirst())
        }

        // Verify indices exist on note_links and note_versions
        val linkIndices = mutableSetOf<String>()
        sqlite.query("PRAGMA index_list(`note_links`)").use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) linkIndices.add(c.getString(nameIdx))
        }
        assertTrue(linkIndices.contains("index_note_links_sourceNoteId"))
        assertTrue(linkIndices.contains("index_note_links_targetNoteId"))
        assertTrue(linkIndices.contains("index_note_links_sourceNoteId_normalizedTargetTitle"))

        v4Db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 2. WIKI LINK PARSING, RESOLUTION, BACKLINKS & RELATED NOTES
    // =========================================================

    @Test
    fun `wiki link parser handles standard, aliased, anchored links and ignores fenced code`() {
        val markdown = """
            Core spec connects [[Graph Theory]] and [[Clean Architecture|Clean Arch]].
            Jump to [[Storage Engine#Indexing|Index Section]].
            ```kotlin
            val example = "[[Code Block Should Be Ignored]]"
            ```
            Also ignore duplicate [[graph theory]] mentions in the same note.
        """.trimIndent()

        val spans = KnowledgeBrainEngine.extractWikiLinkSpans(markdown)
        assertEquals(4, spans.size)
        assertEquals("Graph Theory", spans[0].targetTitle)
        assertEquals("Clean Architecture", spans[1].targetTitle)
        assertEquals("Clean Arch", spans[1].displayAlias)
        assertEquals("Storage Engine", spans[2].targetTitle)
        assertEquals("Indexing", spans[2].anchorHeading)
        assertEquals("Index Section", spans[2].displayAlias)

        val unique = KnowledgeBrainEngine.extractUniqueWikiLinks(markdown)
        assertEquals(3, unique.size)
        assertEquals(listOf("graph theory", "clean architecture", "storage engine"), unique.map { it.normalizedTitle })
    }

    @Test
    fun `automatic link detection resolves forward references, backlinks, and related notes`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_links_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db, context)

        // Note A links to [[Architecture Guide]] BEFORE Architecture Guide exists (unresolved initially)
        val noteA = NoteEntity(
            id = "note-a",
            folderId = "folder-core",
            folderName = "Engineering",
            title = "API Gateway",
            content = "See [[Architecture Guide]] for routing rules.",
            tagsCsv = "Backend,Design"
        )
        val noteC = NoteEntity(
            id = "note-c",
            folderId = "folder-other",
            folderName = "Notes",
            title = "Caching Strategy",
            content = "References [[Architecture Guide|Arch]] and shares Backend tag.",
            tagsCsv = "Backend,Performance"
        )
        repo.insertFolder(FolderEntity(id = "folder-core", name = "Engineering"))
        repo.insertFolder(FolderEntity(id = "folder-other", name = "Notes"))
        repo.saveOrUpdateNote(noteA)
        repo.saveOrUpdateNote(noteC)

        val unresolvedLinks = repo.getAllNoteLinksDirect()
        assertEquals(2, unresolvedLinks.size)
        assertTrue("Target should be unresolved before target note creation", unresolvedLinks.all { it.targetNoteId == null })

        // Now create target note "Architecture Guide"; existing incoming links from A and C must auto-resolve!
        val noteB = NoteEntity(
            id = "note-b",
            folderId = "folder-core",
            folderName = "Engineering",
            title = "Architecture Guide",
            content = "Main hub note referencing [[API Gateway]].",
            tagsCsv = "Design"
        )
        repo.saveOrUpdateNote(noteB)

        val backlinksToB = repo.getBacklinksForNote("note-b").first()
        assertEquals("Both note A and note C must appear as backlinks for note B", 2, backlinksToB.size)
        assertEquals(setOf("note-a", "note-c"), backlinksToB.map { it.sourceNoteId }.toSet())

        // Related notes for noteA: B (direct bidirectional + shared tag + folder) and C (2-hop via B + shared tag)
        val allNotes = repo.getAllNotesDirect()
        val allLinks = repo.getAllNoteLinksDirect()
        val relatedToA = KnowledgeBrainEngine.findRelatedNotes(noteA, allNotes, allLinks)
        assertEquals(2, relatedToA.size)
        assertEquals("note-b", relatedToA[0].note.id)
        assertTrue(relatedToA[0].reasons.any { it.contains("Bidirectional", ignoreCase = true) })
        assertEquals("note-c", relatedToA[1].note.id)
        assertTrue(relatedToA[1].reasons.any { it.contains("Connected via [[Architecture Guide]]") })
        assertTrue(relatedToA[1].sharedTags.contains("Backend"))

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 3. SECURITY & VISIBILITY RULES (LOCKED / HIDDEN / ARCHIVED)
    // =========================================================

    @Test
    fun `locked, hidden, archived, and deleted notes never leak into links, graph, versions, search, or templates`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_security_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db, context)

        val publicNote = NoteEntity(
            id = "pub-1",
            title = "Public Knowledge",
            content = "Normal note mentioning [[Secret Plan]] and [[Hidden Plan]].",
            tagsCsv = "PublicTag"
        )
        val lockedNote = NoteEntity(
            id = "lock-1",
            title = "Secret Plan",
            content = "Top secret vault text mentioning [[Public Knowledge]]",
            tagsCsv = "VaultOnlyTag",
            isLocked = true
        )
        val hiddenNote = NoteEntity(
            id = "hid-1",
            title = "Hidden Plan",
            content = "Hidden note referencing [[Public Knowledge]]",
            tagsCsv = "HiddenOnlyTag",
            isHidden = true
        )
        val archivedNote = NoteEntity(
            id = "arch-1",
            title = "Archived Plan",
            content = "Archived note referencing [[Public Knowledge]]",
            tagsCsv = "ArchivedOnlyTag",
            isArchived = true
        )
        val deletedNote = NoteEntity(
            id = "del-1",
            title = "Deleted Plan",
            content = "Deleted note referencing [[Public Knowledge]]",
            tagsCsv = "DeletedOnlyTag",
            isDeleted = true
        )

        repo.saveOrUpdateNote(publicNote)
        repo.saveOrUpdateNote(lockedNote)
        repo.saveOrUpdateNote(hiddenNote)
        repo.saveOrUpdateNote(archivedNote)
        repo.saveOrUpdateNote(deletedNote)

        // 1. No links should resolve to or originate from locked/hidden/archived/deleted notes
        val allLinks = repo.getAllNoteLinksDirect()
        assertTrue("Locked/hidden/archived/deleted notes must not own outgoing links", allLinks.all { it.sourceNoteId == "pub-1" })
        assertTrue("Locked/hidden/archived/deleted notes must not be resolved as targetNoteId", allLinks.all { it.targetNoteId == null })
        assertTrue("Public note must have 0 backlinks from locked/hidden/archived/deleted notes", repo.getBacklinksForNote("pub-1").first().isEmpty())

        // 2. Graph must contain ONLY publicNote
        val allNotes = repo.getAllNotesDirect()
        val graph = KnowledgeBrainEngine.buildKnowledgeGraph(allNotes, allLinks)
        assertEquals(1, graph.nodes.size)
        assertEquals("pub-1", graph.nodes[0].noteId)
        assertEquals(0, graph.edges.size)

        // 3. Universal search notes & tags must never surface locked/hidden/archived/deleted data
        assertTrue(KnowledgeBrainEngine.universalSearchNotes("vault", allNotes, allLinks).isEmpty())
        assertTrue(KnowledgeBrainEngine.universalSearchNotes("Hidden note referencing", allNotes, allLinks).isEmpty())
        assertTrue(KnowledgeBrainEngine.universalSearchNotes("HiddenOnlyTag", allNotes, allLinks).isEmpty())
        assertTrue(KnowledgeBrainEngine.universalSearchNotes("Archived Plan", allNotes, allLinks).isEmpty())
        val tagMatches = KnowledgeBrainEngine.universalSearchTags("", allNotes, emptyList())
        assertEquals(listOf("PublicTag"), tagMatches.map { it.name })

        // 4. Locked notes must NEVER store plaintext version history
        assertTrue(repo.getVersionsForNoteDirect("lock-1").isEmpty())

        // 5. Locking an existing unlocked note MUST purge all existing plaintext versions & incoming link targets
        val toBeLocked = NoteEntity(
            id = "was-unlocked",
            title = "Becoming Confidential",
            content = "Plaintext v1 before locking"
        )
        repo.saveOrUpdateNote(toBeLocked)
        repo.saveOrUpdateNote(toBeLocked.copy(content = "Plaintext v2 before locking"))
        assertEquals(2, repo.getVersionsForNoteDirect("was-unlocked").size)

        val linker = NoteEntity(
            id = "linker",
            title = "Linker",
            content = "Points to [[Becoming Confidential]]"
        )
        repo.saveOrUpdateNote(linker)
        assertEquals(1, repo.getBacklinksForNote("was-unlocked").first().size)

        // Now lock the note!
        repo.toggleLock(repo.getNoteByIdDirect("was-unlocked")!!)
        assertTrue(
            "Locking a note must purge all plaintext note_versions",
            repo.getVersionsForNoteDirect("was-unlocked").isEmpty()
        )
        assertTrue(
            "Locking a note must clear incoming link target resolution",
            repo.getBacklinksForNote("was-unlocked").first().isEmpty()
        )
        val linkerOut = repo.getOutgoingLinksForNote("linker").first()
        assertEquals(1, linkerOut.size)
        assertNull(linkerOut[0].targetNoteId)

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 4. DAILY JOURNAL DATE-BASED ENTRIES & REUSABLE TEMPLATES
    // =========================================================

    @Test
    fun `daily journal opens or creates deterministic date-based entries without duplicates`() = runBlocking<Unit> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)

        suspend fun awaitJournalId(dateKey: String): String {
            var resolvedId: String? = null
            vm.openOrCreateJournalForDate(dateKey) { id, _ -> resolvedId = id }
            repeat(200) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                resolvedId?.let { return it }
                kotlinx.coroutines.delay(25)
            }
            error("Timed out waiting for journal dateKey=$dateKey")
        }

        val dateKey = "2026-11-16"
        val firstId = awaitJournalId(dateKey)

        val created = vm.repository.getNoteByIdDirect(firstId)
        assertNotNull(created)
        assertEquals(dateKey, KnowledgeBrainEngine.extractJournalDateKey(created!!))
        assertTrue(KnowledgeBrainEngine.isDailyJournalNote(created))

        // Calling again for same dateKey must reuse the exact same note ID
        val secondId = awaitJournalId(dateKey)
        assertEquals("Reopening journal for the same date must not duplicate entries", firstId, secondId)

        // Calling for a different date creates a distinct entry
        val otherId = awaitJournalId("2026-11-15")
        assertTrue(otherId != firstId)

        vm.repository.deletePermanently(firstId)
        vm.repository.deletePermanently(otherId)
    }

    @Test
    fun `templates expand placeholders, create notes, increment usage, and protect built-ins`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_templates_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db, context)

        repo.ensureBuiltInTemplatesSeeded()
        val seeded = repo.noteTemplates.first()
        assertEquals(5, seeded.size)

        // Built-in templates cannot be deleted
        val delBuiltIn = repo.deleteTemplate("template_builtin_daily_journal")
        assertTrue(delBuiltIn.isFailure)

        // Custom template lifecycle
        val customTpl = NoteTemplateEntity(
            id = "tpl-custom-1",
            name = "Sprint Retro",
            category = "Work",
            titlePattern = "Retro: {{title}} ({{iso_date}})",
            contentTemplate = "# {{title}}\n### Went Well\n- ",
            defaultTagsCsv = "Retro,Team",
            isBuiltIn = false
        )
        assertTrue(repo.saveTemplate(customTpl).isSuccess)
        repo.incrementTemplateUsage("tpl-custom-1")
        val reloadedTpl = db.noteTemplateDao().getTemplateByIdDirect("tpl-custom-1")!!
        assertEquals(1, reloadedTpl.usageCount)

        val expanded = KnowledgeBrainEngine.expandTemplatePattern(reloadedTpl.titlePattern, "Sprint 14", 1_763_294_400_000L)
        assertTrue(expanded.startsWith("Retro: Sprint 14 ("))

        // Locked note cannot be saved as a template via NoteViewModel guard
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val lockedResult = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        vm.saveNoteAsTemplate(
            name = "Leaked Template",
            description = "",
            category = "Custom",
            note = NoteEntity(id = "lck", title = "Secret", content = "Plain", isLocked = true)
        ) { lockedResult.complete(it) }
        assertTrue(lockedResult.await().isFailure)

        assertTrue(repo.deleteTemplate("tpl-custom-1").isSuccess)
        assertNull(db.noteTemplateDao().getTemplateByIdDirect("tpl-custom-1"))

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 5. VERSION HISTORY CAPTURE, PRUNING & RESTORATION
    // =========================================================

    @Test
    fun `version history deduplicates unchanged saves, prunes at 25, and restores older revision cleanly`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_versions_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db, context)

        val noteId = "versioned-note"
        val createdTime = 1_700_000_000_000L
        repo.saveOrUpdateNote(
            NoteEntity(
                id = noteId,
                title = "Draft v1",
                content = "Initial body text",
                tagsCsv = "Draft",
                createdAtUtc = createdTime
            )
        )
        // Duplicate unchanged save should NOT create another version
        repo.saveOrUpdateNote(repo.getNoteByIdDirect(noteId)!!)
        assertEquals(1, repo.getVersionsForNoteDirect(noteId).size)

        // Create 27 additional distinct edits -> retention cap should keep latest 25
        for (i in 2..28) {
            val cur = repo.getNoteByIdDirect(noteId)!!
            repo.saveOrUpdateNote(
                cur.copy(
                    title = "Draft v$i",
                    content = "Body iteration $i with enough words",
                    tagsCsv = "Draft,Iter$i"
                )
            )
        }
        val cappedVersions = repo.getVersionsForNoteDirect(noteId)
        assertEquals("Version history must prune to 25 newest snapshots", 25, cappedVersions.size)
        assertEquals(28, cappedVersions.first().versionNumber)

        // Restore an earlier version in the retained window (e.g., v10)
        val v10 = cappedVersions.first { it.versionNumber == 10 }
        val restoreRes = repo.restoreNoteVersion(noteId, v10.id)
        assertTrue(restoreRes.isSuccess)
        val restoredNote = repo.getNoteByIdDirect(noteId)!!
        assertEquals("Draft v10", restoredNote.title)
        assertEquals("Body iteration 10 with enough words", restoredNote.content)
        assertEquals("Draft,Iter10", restoredNote.tagsCsv)
        assertEquals("Original createdAtUtc must remain immutable on restore", createdTime, restoredNote.createdAtUtc)

        // New head version records the restore summary
        val latestAfterRestore = repo.getVersionsForNoteDirect(noteId).first()
        assertEquals("Restored from v10", latestAfterRestore.changeSummary)

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 6. BACKUP v4 EXPORT & RESTORE FIDELITY
    // =========================================================

    @Test
    fun `backup v4 preserves wiki links, custom templates, and version history without leaking locked notes`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase1_backup_v4_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db, context)
        val backupManager = BackupManager(context, repo)

        val targetNote = NoteEntity(id = "bk-target", title = "Knowledge Hub", content = "Root concept")
        val sourceNote = NoteEntity(id = "bk-src", title = "Spoke Note", content = "Links to [[Knowledge Hub]]")
        val lockedNote = NoteEntity(id = "bk-locked", title = "Vault Cipher", content = "enc", isLocked = true)

        repo.saveOrUpdateNote(targetNote)
        repo.saveOrUpdateNote(sourceNote)
        repo.saveOrUpdateNote(sourceNote.copy(content = "Links to [[Knowledge Hub]] — edited"))
        repo.saveOrUpdateNote(lockedNote)

        repo.saveTemplate(
            NoteTemplateEntity(
                id = "bk-tpl",
                name = "Research Brief",
                category = "Knowledge",
                titlePattern = "Brief: {{title}}",
                contentTemplate = "# {{title}}\n- [[Knowledge Hub]]"
            )
        )

        val previewZip = backupManager.createBackupZip("VaultPass123!")
        val preview = backupManager.validateAndPreview(previewZip, "VaultPass123!").getOrThrow()
        assertEquals(BackupManager.CURRENT_SCHEMA_VERSION, preview.schemaVersion)

        repo.clearAllData()
        assertTrue(repo.getAllNotesDirect().isEmpty())
        assertTrue(repo.getAllNoteLinksDirect().isEmpty())

        val restoreRes = backupManager.restoreFromZip(previewZip, RestoreStrategy.REPLACE, "VaultPass123!")
        assertTrue(restoreRes.isSuccess)

        val restoredLinks = repo.getBacklinksForNote("bk-target").first()
        assertEquals(1, restoredLinks.size)
        assertEquals("bk-src", restoredLinks[0].sourceNoteId)

        val restoredTemplate = db.noteTemplateDao().getTemplateByIdDirect("bk-tpl")
        assertNotNull("Custom template must survive v4 backup restore", restoredTemplate)
        assertEquals("Research Brief", restoredTemplate!!.name)

        assertTrue(
            "Note version history for unlocked note must survive v4 restore",
            repo.getVersionsForNoteDirect("bk-src").size >= 2
        )
        assertTrue(
            "Locked note must still have zero plaintext versions after restore",
            repo.getVersionsForNoteDirect("bk-locked").isEmpty()
        )

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================
    // 7. ROBOLECTRIC COMPOSE UI: BRAIN SCREEN, UNIVERSAL SEARCH & EDITOR
    // =========================================================

    @Test
    fun `PinnedNotesScreen displays pinned notes directly and opens selected note`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val pinnedId = "ui-pinned-${UUID.randomUUID()}"
        val unpinnedId = "ui-unpinned-${UUID.randomUUID()}"
        var openedNoteId: String? = null

        runBlocking {
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = pinnedId,
                    title = "Pinned Release Checklist",
                    content = "Verify light and dark modes",
                    isPinned = true
                )
            )
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = unpinnedId,
                    title = "Regular Draft Note",
                    content = "Should not appear in pinned notes",
                    isPinned = false
                )
            )
        }

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                PinnedNotesScreen(
                    viewModel = vm,
                    onNavigateToEditor = { id, _ -> openedNoteId = id },
                    onNavigateToCanvas = { id -> openedNoteId = id },
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Pinned Release Checklist").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pinned Release Checklist").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Regular Draft Note").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText("Pinned Release Checklist").performClick()
        composeRule.waitForIdle()
        assertEquals(pinnedId, openedNoteId)

        runBlocking {
            vm.repository.deletePermanently(pinnedId)
            vm.repository.deletePermanently(unpinnedId)
        }
    }

    @Test
    fun `SearchScreen universal search supports Tags and Links filters while hiding locked notes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val alphaId = "srch-alpha-${UUID.randomUUID()}"
        val betaId = "srch-beta-${UUID.randomUUID()}"
        val lockedId = "srch-locked-${UUID.randomUUID()}"
        val uniqueTag = "QuantumTag99"

        runBlocking {
            vm.repository.saveOrUpdateNote(
                NoteEntity(id = alphaId, title = "Quantum Target", content = "Target concepts", tagsCsv = uniqueTag)
            )
            vm.repository.saveOrUpdateNote(
                NoteEntity(id = betaId, title = "Bridge Note", content = "Mentions [[Quantum Target]] directly", tagsCsv = "Other")
            )
            vm.repository.saveOrUpdateNote(
                NoteEntity(id = lockedId, title = "Secret Quantum", content = "Locked secret", tagsCsv = "VaultOnly99", isLocked = true)
            )
            vm.setSearchQuery("")
            vm.setSearchFilterType("ALL")
        }

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                SearchScreen(
                    viewModel = vm,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Select TAGS filter -> public uniqueTag is clickable, VaultOnly99 must NOT exist
        composeRule.onNodeWithTag("filter_chip_TAGS").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("search_tag_chip_$uniqueTag").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag("search_tag_chip_VaultOnly99").fetchSemanticsNodes().isEmpty())

        // Select LINKS filter and query "Quantum Target" -> Bridge Note appears with link reason badge
        composeRule.onNodeWithTag("filter_chip_LINKS").performClick()
        composeRule.onNodeWithTag("global_search_input").performTextInput("Quantum Target")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Bridge Note").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText("Secret Quantum").fetchSemanticsNodes().isEmpty())

        runBlocking {
            vm.repository.deletePermanently(alphaId)
            vm.repository.deletePermanently(betaId)
            vm.repository.deletePermanently(lockedId)
        }
    }

    @Test
    fun `NoteEditorScreen displays Backlinks, Related Notes, Wiki Autocomplete, and Version History restore`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val hubId = "ed-hub-${UUID.randomUUID()}"
        val refId = "ed-ref-${UUID.randomUUID()}"

        runBlocking {
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = hubId,
                    title = "Versioned Hub",
                    content = "Original version 1 body",
                    tagsCsv = "Knowledge"
                )
            )
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = hubId,
                    title = "Versioned Hub",
                    content = "Updated version 2 body",
                    tagsCsv = "Knowledge"
                )
            )
            vm.repository.saveOrUpdateNote(
                NoteEntity(
                    id = refId,
                    title = "Referencing Concept",
                    content = "References [[Versioned Hub]] in full detail.",
                    tagsCsv = "Knowledge"
                )
            )
        }

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                NoteEditorScreen(
                    noteId = hubId,
                    initialNoteType = NoteType.STANDARD.name,
                    viewModel = vm,
                    onNavigateBack = {},
                    onNavigateToNote = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Existing notes open in View Mode showing Backlinks & Related Notes panels
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("backlink_item_$refId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("backlink_item_$refId").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("related_note_item_$refId").performScrollTo().assertIsDisplayed()

        // Open More Options -> Version History and verify both versions are listed and restorable
        composeRule.onNodeWithContentDescription("More Options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("more_menu_version_history").performClick()
        composeRule.waitForIdle()

        val versions = runBlocking { vm.repository.getVersionsForNoteDirect(hubId) }
        val v1 = versions.first { it.versionNumber == 1 }
        composeRule.onNodeWithTag("restore_version_${v1.id}").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runBlocking {
                vm.repository.getNoteByIdDirect(hubId)?.content == "Original version 1 body"
            }
        }

        // Enter Edit Mode and type "[[" to verify live wiki link autocomplete bar
        composeRule.onNodeWithTag("preview_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_content_input").performScrollTo().performTextInput("\n[[Ref")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("wiki_link_autocomplete_bar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("wiki_suggestion_$refId").assertIsDisplayed()

        runBlocking {
            vm.repository.deletePermanently(hubId)
            vm.repository.deletePermanently(refId)
        }
    }
}
