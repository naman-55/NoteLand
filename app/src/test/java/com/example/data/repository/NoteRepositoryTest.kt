package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.NoteDao
import com.example.data.model.NoteEntity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoteRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
        repository = NoteRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertNoteViaDaoReturnsExpectedRowIdAndImmediatelyRetrievableById() = runBlocking {
        // Arrange
        val expectedNoteId = UUID.randomUUID().toString()
        val expectedTitle = "Architecture Design Notes"
        val expectedContent = "Verified database persistence and immediate retrieval by primary key."
        val note = NoteEntity(
            id = expectedNoteId,
            title = expectedTitle,
            content = expectedContent,
            noteType = "TEXT",
            tagsCsv = "architecture,sqlite,room",
            createdAtUtc = System.currentTimeMillis(),
            updatedAtUtc = System.currentTimeMillis()
        )

        // Act: Insert the note directly via the DAO
        val generatedRowId = noteDao.insertNote(note)

        // Assert: Verify that inserting returns a valid SQLite row ID (> 0)
        assertTrue("Expected insertNote to return a positive row ID (> 0), but was $generatedRowId", generatedRowId > 0L)

        // Assert: Verify that the note can be immediately retrieved by its string ID
        val retrievedNote = noteDao.getNoteByIdDirect(expectedNoteId)
        assertNotNull("Inserted note must be immediately retrievable by ID", retrievedNote)
        assertEquals("Retrieved note ID must match inserted note ID", expectedNoteId, retrievedNote?.id)
        assertEquals("Retrieved note title must match inserted title", expectedTitle, retrievedNote?.title)
        assertEquals("Retrieved note content must match inserted content", expectedContent, retrievedNote?.content)

        // Verify retrieval via Repository as well
        val retrievedFromRepo = repository.getNoteByIdDirect(expectedNoteId)
        assertNotNull("Note must also be retrievable via repository", retrievedFromRepo)
        assertEquals(expectedNoteId, retrievedFromRepo?.id)
        assertEquals(expectedTitle, retrievedFromRepo?.title)

        // Verify Flow-based retrieval emits the expected note immediately
        val flowEmittedNote = noteDao.getNoteById(expectedNoteId).first()
        assertNotNull("Flow must immediately emit the inserted note", flowEmittedNote)
        assertEquals(expectedNoteId, flowEmittedNote?.id)
    }

    @Test
    fun testSaveOrUpdateNoteInsertsAndRetrievesImmediately() = runBlocking {
        val noteId = UUID.randomUUID().toString()
        val note = NoteEntity(
            id = noteId,
            title = "Repository Save Note",
            content = "Testing saveOrUpdateNote via repository"
        )

        val result = repository.saveOrUpdateNote(note)
        assertTrue("saveOrUpdateNote should succeed", result.isSuccess)
        assertEquals(noteId, result.getOrThrow())

        val retrieved = repository.getNoteByIdDirect(noteId)
        assertNotNull(retrieved)
        assertEquals("Repository Save Note", retrieved?.title)
    }
}
