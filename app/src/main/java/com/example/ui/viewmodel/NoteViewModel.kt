package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AttachmentEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.FocusSessionStatus
import com.example.data.model.FocusSessionType
import com.example.data.model.FolderEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteLinkEntity
import com.example.data.model.NoteTemplateEntity
import com.example.data.model.NoteType
import com.example.data.model.NoteVersionEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.data.repository.NoteRepository
import com.example.data.storage.AttachmentStorageManager
import com.example.security.SecurityManager
import com.example.ui.editor.model.QuickEditToolbarConfig
import com.example.ui.editor.model.QuickEditToolbarConfigCodec
import com.example.util.BackupManager
import com.example.util.BackupPreview
import com.example.util.ChecklistItem
import com.example.util.DailyProgressReport
import com.example.util.DashboardWidgetItem
import com.example.util.DashboardWidgetType
import com.example.util.FocusStatisticsSummary
import com.example.util.FolderHierarchyUtils
import com.example.util.HabitStreakSummary
import com.example.util.KnowledgeBrainEngine
import com.example.util.KnowledgeGraphData
import com.example.util.PomodoroRuntimeState
import com.example.util.ProductivityEngine
import com.example.util.RelatedNoteRecommendation
import com.example.util.ReminderNotificationManager
import com.example.util.RestoreResult
import com.example.util.RestoreStrategy
import com.example.util.SmartNoteUtils
import com.example.util.UniversalNoteMatch
import com.example.util.UniversalTagMatch
import com.example.util.WeeklyProgressReport
import com.example.worker.PomodoroCompletionWorker
import com.example.worker.ProductivityWorkScheduler
import java.io.File
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class BacklinkItem(
    val sourceNote: NoteEntity,
    val link: NoteLinkEntity
)

data class SearchFilterCriteria(
    val typeFilter: NoteType? = null,
    val vaultUnlocked: Boolean = false,
    val sortOrder: String = "UPDATED_DESC",
    val pinnedOnly: Boolean = false,
    val hasChecklistOnly: Boolean = false,
    val favoritesOnly: Boolean = false
)

data class GoalItem(
    val id: String,
    val title: String,
    val targetValue: Int = 1,
    val currentValue: Int = 0,
    val unit: String = "",
    val isCompleted: Boolean = false,
    val deadlineUtc: Long? = null,
    val milestones: List<String> = emptyList(),
    val linkedTaskIds: List<String> = emptyList()
)

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = NoteRepository(database, application)
    val securityManager = SecurityManager(application)
    val backupManager = BackupManager(application, repository)
    val attachmentStorageManager = AttachmentStorageManager(application)

    private val pomodoroPrefs = application.getSharedPreferences(PomodoroCompletionWorker.PREFS_POMODORO, Context.MODE_PRIVATE)
    private var pomodoroTickerJob: Job? = null

    // Repository flows
    val allActiveNotes: StateFlow<List<NoteEntity>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<NoteEntity>> = repository.deletedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNotes: StateFlow<List<NoteEntity>> = repository.favoriteNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderNotes: StateFlow<List<NoteEntity>> = repository.reminderNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStickyNotes = repository.stickyNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStickyBoard = MutableStateFlow("Main Wall")
    val stickyBoards = MutableStateFlow<List<String>>(listOf("Main Wall"))

    val stickyNotes = combine(allStickyNotes, currentStickyBoard) { notes, board ->
        notes.filter { it.boardName == board }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canvases = repository.canvases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments = repository.attachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedAttachments: StateFlow<List<AttachmentEntity>> = repository.deletedAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAttachments: StateFlow<List<AttachmentEntity>> = repository.favoriteAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedAttachments: StateFlow<List<AttachmentEntity>> = repository.archivedAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedAttachments: StateFlow<List<AttachmentEntity>> = repository.lockedAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenNotes: StateFlow<List<NoteEntity>> = repository.hiddenNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedNotes: StateFlow<List<NoteEntity>> = repository.lockedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteLinks: StateFlow<List<NoteLinkEntity>> = repository.noteLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteTemplates: StateFlow<List<NoteTemplateEntity>> = repository.noteTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // PHASE 2: PRODUCTIVITY WORKSPACE FLOWS
    // ==========================================
    val currentKanbanBoard = MutableStateFlow("default")
    val kanbanBoards = MutableStateFlow<List<String>>(listOf("default"))

    private val allKanbanColumns: StateFlow<List<KanbanColumnEntity>> = repository.kanbanColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kanbanColumns: StateFlow<List<KanbanColumnEntity>> = combine(
        allKanbanColumns, currentKanbanBoard
    ) { columns, boardId ->
        columns.filter { it.boardId == boardId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allActiveTasks: StateFlow<List<TaskEntity>> = repository.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TaskEntity>> = combine(
        allActiveTasks, kanbanColumns
    ) { tasks, columns ->
        val validColIds = columns.map { it.id }.toSet()
        tasks.filter { it.columnId in validColIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskDependencies: StateFlow<List<TaskDependencyEntity>> = repository.taskDependencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabits: StateFlow<List<HabitEntity>> = repository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLogEntity>> = repository.habitLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.focusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitStreaks: StateFlow<Map<String, HabitStreakSummary>> = combine(
        activeHabits,
        habitLogs
    ) { habits, logs ->
        val logsByHabit = logs.groupBy { it.habitId }
        val now = System.currentTimeMillis()
        habits.associate { habit ->
            habit.id to ProductivityEngine.computeHabitStreak(
                habit = habit,
                logsForHabit = logsByHabit[habit.id].orEmpty(),
                nowUtc = now
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val focusStatistics: StateFlow<FocusStatisticsSummary> = focusSessions
        .map { sessions -> ProductivityEngine.computeFocusStatistics(sessions) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusStatisticsSummary(0, 0, 0, 0, 0, 0, 0, 0)
        )

    val dailyProgressReport: StateFlow<DailyProgressReport> = combine(
        combine(activeTasks, allReminders, allActiveNotes) { t, r, n -> Triple(t, r, n) },
        combine(activeHabits, habitLogs, focusSessions) { h, l, f -> Triple(h, l, f) }
    ) { part1, part2 ->
        ProductivityEngine.buildDailyProgressReport(
            tasks = part1.first,
            reminders = part1.second,
            notes = part1.third,
            habits = part2.first,
            habitLogs = part2.second,
            focusSessions = part2.third
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProductivityEngine.buildDailyProgressReport(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
    )

    val weeklyProgressReport: StateFlow<WeeklyProgressReport> = combine(
        activeTasks,
        allReminders,
        activeHabits,
        habitLogs,
        focusSessions
    ) { tasks, reminders, habits, logs, sessions ->
        ProductivityEngine.buildWeeklyProgressReport(
            tasks = tasks,
            reminders = reminders,
            habits = habits,
            habitLogs = logs,
            focusSessions = sessions
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProductivityEngine.buildWeeklyProgressReport(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
    )

    val pomodoroState = MutableStateFlow(PomodoroRuntimeState())
    val pomodoroRemainingSeconds = MutableStateFlow(FocusSessionType.WORK.defaultDurationSec)

    val knowledgeGraphTagFilter = MutableStateFlow<String?>(null)
    val knowledgeGraphIncludeOrphans = MutableStateFlow(true)

    val knowledgeGraphState: StateFlow<KnowledgeGraphData> = combine(
        allActiveNotes,
        noteLinks,
        knowledgeGraphTagFilter,
        knowledgeGraphIncludeOrphans
    ) { notes, links, tagFilter, includeOrphans ->
        KnowledgeBrainEngine.buildKnowledgeGraph(
            notes = notes,
            links = links,
            filterTag = tagFilter,
            includeOrphans = includeOrphans
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KnowledgeGraphData(emptyList(), emptyList(), 0, 0)
    )

    val dailyJournalNotes: StateFlow<List<NoteEntity>> = allActiveNotes
        .map { notes ->
            notes
                .filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) && KnowledgeBrainEngine.isDailyJournalNote(it) }
                .sortedWith(
                    compareByDescending<NoteEntity> {
                        KnowledgeBrainEngine.extractJournalDateKey(it)
                            ?: KnowledgeBrainEngine.journalDateKeyFromMillis(it.createdAtUtc)
                    }.thenByDescending { it.updatedAtUtc }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state filters
    val searchQuery = MutableStateFlow("")
    val searchFilterType = MutableStateFlow("ALL") // "ALL", "NOTES", "FOLDERS", "ATTACHMENTS"
    val searchSortOrder = MutableStateFlow("UPDATED_DESC") // "UPDATED_DESC", "CREATED_DESC", "TITLE_ASC", "TITLE_DESC"
    val searchPinnedOnly = MutableStateFlow(false)
    val searchHasChecklistOnly = MutableStateFlow(false)
    val searchFavoritesOnly = MutableStateFlow(false)
    private val settingsPrefs = application.getSharedPreferences("noteland_settings_prefs", android.content.Context.MODE_PRIVATE)

    fun initStickyBoards() {
        val savedBoard = settingsPrefs.getString("current_sticky_board", "Main Wall") ?: "Main Wall"
        currentStickyBoard.value = savedBoard
        
        val savedBoardsStr = settingsPrefs.getString("sticky_boards_list", "[\"Main Wall\"]")
        val parsedList = try {
            if (savedBoardsStr != null) {
                val arr = org.json.JSONArray(savedBoardsStr)
                List(arr.length()) { i -> arr.getString(i) }
            } else listOf("Main Wall")
        } catch (e: Exception) {
            listOf("Main Wall")
        }
        stickyBoards.value = if (parsedList.isEmpty()) listOf("Main Wall") else parsedList
    }

    fun initKanbanBoards() {
        val savedBoard = settingsPrefs.getString("current_kanban_board", "default") ?: "default"
        currentKanbanBoard.value = savedBoard
        
        val savedBoardsStr = settingsPrefs.getString("kanban_boards_list", "[\"default\"]")
        val parsedList = try {
            if (savedBoardsStr != null) {
                val arr = org.json.JSONArray(savedBoardsStr)
                List(arr.length()) { i -> arr.getString(i) }
            } else listOf("default")
        } catch (e: Exception) {
            listOf("default")
        }
        kanbanBoards.value = if (parsedList.isEmpty()) listOf("default") else parsedList
    }

    init {
        initStickyBoards()
        initKanbanBoards()
    }

    fun setCurrentKanbanBoard(boardId: String) {
        currentKanbanBoard.value = boardId
        settingsPrefs.edit { putString("current_kanban_board", boardId) }
    }

    fun addKanbanBoard(boardId: String) {
        val current = kanbanBoards.value.toMutableList()
        if (!current.contains(boardId)) {
            current.add(boardId)
            kanbanBoards.value = current
            settingsPrefs.edit { putString("kanban_boards_list", org.json.JSONArray(current).toString()) }
            setCurrentKanbanBoard(boardId)
        }
    }

    fun setCurrentStickyBoard(boardName: String) {
        currentStickyBoard.value = boardName
        settingsPrefs.edit { putString("current_sticky_board", boardName) }
    }

    fun addStickyBoard(boardName: String) {
        val current = stickyBoards.value.toMutableList()
        if (!current.contains(boardName)) {
            current.add(boardName)
            stickyBoards.value = current
            settingsPrefs.edit { putString("sticky_boards_list", org.json.JSONArray(current).toString()) }
            setCurrentStickyBoard(boardName)
        }
    }

    fun deleteStickyBoard(boardName: String) {
        val current = stickyBoards.value.toMutableList()
        if (current.size > 1 && current.contains(boardName)) {
            current.remove(boardName)
            stickyBoards.value = current
            settingsPrefs.edit { putString("sticky_boards_list", org.json.JSONArray(current).toString()) }
            if (currentStickyBoard.value == boardName) {
                setCurrentStickyBoard(current.first())
            }
            viewModelScope.launch(Dispatchers.IO) {
                allStickyNotes.value.filter { it.boardName == boardName }.forEach {
                    repository.deleteStickyNote(it.id)
                }
            }
        }
    }

    fun renameStickyBoard(oldName: String, newName: String) {
        val current = stickyBoards.value.toMutableList()
        val idx = current.indexOf(oldName)
        if (idx != -1 && !current.contains(newName)) {
            current[idx] = newName
            stickyBoards.value = current
            settingsPrefs.edit { putString("sticky_boards_list", org.json.JSONArray(current).toString()) }
            if (currentStickyBoard.value == oldName) {
                setCurrentStickyBoard(newName)
            }
            viewModelScope.launch(Dispatchers.IO) {
                allStickyNotes.value.filter { it.boardName == oldName }.forEach {
                    repository.updateStickyNote(it.copy(boardName = newName))
                }
            }
        }
    }
    val dashboardWidgets = MutableStateFlow(
        ProductivityEngine.parseDashboardWidgets(settingsPrefs.getString("dashboard_widgets_config", null))
    )
    val searchHistory = MutableStateFlow<List<String>>(loadSearchHistory())

    val selectedFolderId = MutableStateFlow<String?>(null)
    val pendingWidgetFolderId = MutableStateFlow<String?>(null)
    val pendingWidgetCreateFolder = MutableStateFlow(false)
    val selectedTag = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<NoteType?>(null)
    val userProfileName = MutableStateFlow(settingsPrefs.getString("user_name", "") ?: "")
    val userProfileEmail = MutableStateFlow(settingsPrefs.getString("user_email", "") ?: "")
    val isGridView = MutableStateFlow(settingsPrefs.getBoolean("home_grid_view", true))
    val editorFontSizeSp = MutableStateFlow(settingsPrefs.getFloat("editor_font_size_sp", 16.5f))
    val notificationsEnabled = MutableStateFlow(settingsPrefs.getBoolean("notifications_enabled", true))
    val isVaultUnlocked = MutableStateFlow(false)
    val vaultSessionEpoch = MutableStateFlow(0L)
    val vaultLockoutSeconds = MutableStateFlow(securityManager.getRemainingLockoutSeconds())
    val failedUnlockAttempts = MutableStateFlow(securityManager.getFailedAttempts())
    val autoLockTimeoutMs = MutableStateFlow(securityManager.getAutoLockTimeout())
    val biometricsEnabled = MutableStateFlow(securityManager.isBiometricsPreferenceEnabled())
    val themeMode = MutableStateFlow(settingsPrefs.getString("theme_mode", "light") ?: "light")
    val dynamicColorEnabled = MutableStateFlow(settingsPrefs.getBoolean("dynamic_color_enabled", true))
    val trashRetentionDays = MutableStateFlow(settingsPrefs.getInt(KEY_TRASH_RETENTION_DAYS, 30))
    val quickEditConfig = MutableStateFlow(
        QuickEditToolbarConfigCodec.decode(settingsPrefs.getString(KEY_QUICK_EDIT_TOOLBAR_CONFIG, null))
    )
    val goals = MutableStateFlow<List<GoalItem>>(loadGoals())

    init {
        // Sync from Room AppSettings if SharedPreferences has no value yet (e.g. after backup restore)
        if (!settingsPrefs.contains(KEY_QUICK_EDIT_TOOLBAR_CONFIG)) {
            viewModelScope.launch {
                val stored = repository.getSetting(KEY_QUICK_EDIT_TOOLBAR_CONFIG)
                if (!stored.isNullOrBlank()) {
                    val decoded = QuickEditToolbarConfigCodec.decode(stored)
                    quickEditConfig.value = decoded
                    settingsPrefs.edit { putString(KEY_QUICK_EDIT_TOOLBAR_CONFIG, stored) }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            attachmentStorageManager.clearTempVaultViewCache()
            val retention = trashRetentionDays.value
            if (retention > 0) {
                repository.purgeExpiredTrashItems(retention)
            }
        }
    }

    private fun loadSearchHistory(): List<String> {
        val json = settingsPrefs.getString("search_history_json", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addSearchQueryToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        // Never record search history if vault is currently unlocked (prevents leaking private/vault search terms)
        if (isVaultUnlocked.value) return
        val current = searchHistory.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val capped = current.take(15)
        searchHistory.value = capped
        val arr = org.json.JSONArray(capped)
        settingsPrefs.edit { putString("search_history_json", arr.toString()) }
    }

    fun removeSearchHistoryItem(query: String) {
        val current = searchHistory.value.toMutableList()
        current.remove(query)
        searchHistory.value = current
        val arr = org.json.JSONArray(current)
        settingsPrefs.edit { putString("search_history_json", arr.toString()) }
    }

    fun clearSearchHistory() {
        searchHistory.value = emptyList()
        settingsPrefs.edit { remove("search_history_json") }
    }

    private val searchCriteriaFlow: Flow<SearchFilterCriteria> = combine(
        combine(selectedTypeFilter, isVaultUnlocked) { t, u -> Pair(t, u) },
        combine(searchSortOrder, searchPinnedOnly) { s, p -> Pair(s, p) },
        combine(searchHasChecklistOnly, searchFavoritesOnly) { c, f -> Pair(c, f) }
    ) { p1, p2, p3 ->
        SearchFilterCriteria(
            typeFilter = p1.first,
            vaultUnlocked = p1.second,
            sortOrder = p2.first,
            pinnedOnly = p2.second,
            hasChecklistOnly = p3.first,
            favoritesOnly = p3.second
        )
    }

    // Filtered Notes: respects debounced search query, hierarchical folder subtree, tags, note type, filters, and strictly excludes locked notes
    private val selectedFolderScopeFlow: Flow<Pair<String?, Set<String>>> = combine(
        selectedFolderId,
        folders
    ) { folderId, allFolders ->
        if (folderId == null) {
            null to emptySet()
        } else {
            folderId to FolderHierarchyUtils.getSubtreeFolderIds(allFolders, folderId)
        }
    }

    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allActiveNotes,
        searchQuery,
        selectedFolderScopeFlow,
        selectedTag,
        searchCriteriaFlow
    ) { notes: List<NoteEntity>, query: String, folderScope: Pair<String?, Set<String>>, tag: String?, criteria: SearchFilterCriteria ->
        val folderId = folderScope.first
        val subtreeFolderIds = folderScope.second
        val normalizedQuery = query.trim()
        val hasQuery = normalizedQuery.isNotEmpty()
        var list = notes.filter { note ->
            // Always exclude locked, hidden, archived, and deleted notes from ordinary browsing and search
            if (!KnowledgeBrainEngine.isNoteKnowledgeEligible(note)) return@filter false
            // Imported PDF documents live in PDF Reader / Media, not in the Notes list
            if (criteria.typeFilter == null && note.noteType == NoteType.PDF_DOC.name) return@filter false
            if (criteria.pinnedOnly && !note.isPinned) return@filter false
            if (criteria.favoritesOnly && !note.isFavorite) return@filter false
            if (criteria.hasChecklistOnly && (note.checklistJson.isBlank() || note.checklistJson == "[]")) return@filter false

            val matchesQuery = !hasQuery ||
                note.title.contains(normalizedQuery, ignoreCase = true) ||
                note.content.contains(normalizedQuery, ignoreCase = true) ||
                note.tagsCsv.contains(normalizedQuery, ignoreCase = true) ||
                note.folderName.contains(normalizedQuery, ignoreCase = true)

            val matchesFolder = folderId == null || (note.folderId != null && note.folderId in subtreeFolderIds)
            val matchesTag = tag == null || com.example.util.noteHasTag(note.tagsCsv, tag)
            val matchesType = criteria.typeFilter == null || note.noteType == criteria.typeFilter.name

            matchesQuery && matchesFolder && matchesTag && matchesType
        }

        list = when (criteria.sortOrder) {
            "CREATED_DESC" -> list.sortedByDescending { it.createdAtUtc }
            "TITLE_ASC" -> list.sortedBy { it.title.lowercase() }
            "TITLE_DESC" -> list.sortedByDescending { it.title.lowercase() }
            else -> list.sortedByDescending { it.updatedAtUtc }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val universalNoteMatches: StateFlow<List<UniversalNoteMatch>> = combine(
        allActiveNotes,
        noteLinks,
        searchQuery,
        searchCriteriaFlow
    ) { notes, links, query, criteria ->
        KnowledgeBrainEngine.universalSearchNotes(
            query = query,
            allNotes = notes,
            allLinks = links,
            typeFilter = criteria.typeFilter,
            pinnedOnly = criteria.pinnedOnly,
            hasChecklistOnly = criteria.hasChecklistOnly,
            favoritesOnly = criteria.favoritesOnly,
            sortOrder = criteria.sortOrder
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val universalTagMatches: StateFlow<List<UniversalTagMatch>> = combine(
        searchQuery,
        allActiveNotes,
        tags
    ) { query, notes, tagEntities ->
        KnowledgeBrainEngine.universalSearchTags(
            query = query,
            allNotes = notes,
            tagEntities = tagEntities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSearchFilterType(filterType: String) {
        searchFilterType.value = filterType
    }

    fun setSearchSortOrder(order: String) {
        searchSortOrder.value = order
    }

    fun toggleSearchPinned() {
        searchPinnedOnly.value = !searchPinnedOnly.value
    }

    fun toggleSearchChecklist() {
        searchHasChecklistOnly.value = !searchHasChecklistOnly.value
    }

    fun toggleSearchFavorites() {
        searchFavoritesOnly.value = !searchFavoritesOnly.value
    }

    fun setSelectedFolder(folderId: String?) {
        selectedFolderId.value = folderId
    }

    fun requestOpenFolderFromWidget(folderId: String) {
        pendingWidgetFolderId.value = folderId
    }

    fun requestNewFolderFromWidget() {
        pendingWidgetCreateFolder.value = true
    }

    fun consumePendingFolderRequest() {
        pendingWidgetFolderId.value = null
        pendingWidgetCreateFolder.value = false
    }

    fun openOrCreateDailyDiary(onReady: (String, String) -> Unit) {
        val todayKey = KnowledgeBrainEngine.journalDateKeyFromMillis(System.currentTimeMillis())
        openOrCreateJournalForDate(todayKey, onReady)
    }

    fun setKnowledgeGraphTagFilter(tag: String?) {
        knowledgeGraphTagFilter.value = tag?.trim()?.removePrefix("#")?.takeIf { it.isNotBlank() }
    }

    fun setKnowledgeGraphIncludeOrphans(include: Boolean) {
        knowledgeGraphIncludeOrphans.value = include
    }

    fun observeBacklinks(noteId: String): Flow<List<BacklinkItem>> {
        return combine(
            repository.getBacklinksForNote(noteId),
            allActiveNotes
        ) { links, notes ->
            val currentNote = notes.firstOrNull { it.id == noteId }
            if (currentNote != null && !KnowledgeBrainEngine.isNoteKnowledgeEligible(currentNote)) {
                return@combine emptyList()
            }
            val eligibleById = notes
                .filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }
                .associateBy { it.id }
            links.mapNotNull { link ->
                val source = eligibleById[link.sourceNoteId] ?: return@mapNotNull null
                if (source.id == noteId) return@mapNotNull null
                BacklinkItem(sourceNote = source, link = link)
            }
        }
    }

    fun observeRelatedNotes(noteId: String): Flow<List<RelatedNoteRecommendation>> {
        return combine(allActiveNotes, noteLinks) { notes, links ->
            val anchor = notes.firstOrNull { it.id == noteId } ?: return@combine emptyList()
            KnowledgeBrainEngine.findRelatedNotes(
                anchorNote = anchor,
                allNotes = notes,
                allLinks = links,
                maxResults = 8
            )
        }
    }

    fun observeNoteVersions(noteId: String): Flow<List<NoteVersionEntity>> {
        return combine(
            repository.getVersionsForNote(noteId),
            allActiveNotes
        ) { versions, notes ->
            val target = notes.firstOrNull { it.id == noteId }
            if (target != null && target.isLocked) {
                emptyList()
            } else {
                versions
            }
        }
    }

    fun restoreNoteVersion(
        noteId: String,
        versionId: String,
        onResult: (Result<NoteEntity>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.restoreNoteVersion(noteId, versionId)
            onResult(res)
        }
    }

    fun openOrCreateJournalForDate(
        dateKey: String,
        onReady: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedKey = dateKey.trim().ifBlank {
                KnowledgeBrainEngine.journalDateKeyFromMillis(System.currentTimeMillis())
            }
            val notes = withContext(Dispatchers.IO) { repository.getAllNotesDirect() }
            val existing = notes.firstOrNull { note ->
                KnowledgeBrainEngine.isNoteKnowledgeEligible(note) &&
                    KnowledgeBrainEngine.isDailyJournalNote(note) &&
                    KnowledgeBrainEngine.extractJournalDateKey(note) == normalizedKey
            }
            if (existing != null) {
                onReady(existing.id, existing.noteType)
            } else {
                val targetMillis = KnowledgeBrainEngine.millisFromJournalDateKey(normalizedKey)
                    ?: System.currentTimeMillis()
                val displayTitle = KnowledgeBrainEngine.journalDisplayTitleForDateKey(normalizedKey)
                val dailyTpl = withContext(Dispatchers.IO) {
                    repository.getAllNoteTemplatesDirect()
                        .firstOrNull { it.id == "template_builtin_daily_journal" }
                }
                val expandedContent = if (dailyTpl != null) {
                    KnowledgeBrainEngine.expandTemplatePattern(
                        pattern = dailyTpl.contentTemplate,
                        titleInput = displayTitle,
                        nowMillis = targetMillis
                    )
                } else {
                    "# $displayTitle\n\n### Morning Intentions\n- \n\n### Key Insights\n- \n\n### Evening Reflection\n- "
                }
                val newDiaryId = UUID.randomUUID().toString()
                val diaryNote = NoteEntity(
                    id = newDiaryId,
                    title = displayTitle,
                    content = expandedContent,
                    noteType = NoteType.STANDARD.name,
                    tagsCsv = dailyTpl?.defaultTagsCsv?.ifBlank { "Diary" } ?: "Diary",
                    colorHex = dailyTpl?.defaultColorHex?.ifBlank { "#FFEDD5" } ?: "#FFEDD5",
                    checklistJson = dailyTpl?.checklistTemplateJson ?: "",
                    canvasDataJson = SmartNoteUtils.withJournalDateKey("", normalizedKey),
                    createdAtUtc = System.currentTimeMillis(),
                    updatedAtUtc = System.currentTimeMillis()
                )
                saveNoteDirect(diaryNote)
                if (dailyTpl != null) {
                    repository.incrementTemplateUsage(dailyTpl.id)
                }
                onReady(newDiaryId, NoteType.STANDARD.name)
            }
        }
    }

    fun createNoteFromTemplate(
        template: NoteTemplateEntity,
        customTitle: String = "",
        folderId: String? = null,
        onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val resolvedTitle = KnowledgeBrainEngine.expandTemplatePattern(
                pattern = template.titlePattern,
                titleInput = customTitle,
                nowMillis = now
            ).ifBlank { template.name }
            val resolvedBody = KnowledgeBrainEngine.expandTemplatePattern(
                pattern = template.contentTemplate,
                titleInput = customTitle.ifBlank { resolvedTitle },
                nowMillis = now
            )
            val folderName = if (!folderId.isNullOrBlank()) {
                folders.value.firstOrNull { it.id == folderId }?.name.orEmpty()
            } else {
                ""
            }
            val isJournalTpl = template.category.equals("Journal", ignoreCase = true) ||
                com.example.util.noteHasTag(template.defaultTagsCsv, "Diary")
            val canvasJson = if (isJournalTpl) {
                SmartNoteUtils.withJournalDateKey("", KnowledgeBrainEngine.journalDateKeyFromMillis(now))
            } else {
                ""
            }
            val newId = UUID.randomUUID().toString()
            val note = NoteEntity(
                id = newId,
                title = resolvedTitle,
                content = resolvedBody,
                noteType = NoteType.STANDARD.name,
                folderId = folderId?.takeIf { it.isNotBlank() },
                folderName = folderName,
                tagsCsv = template.defaultTagsCsv,
                colorHex = template.defaultColorHex,
                checklistJson = template.checklistTemplateJson,
                canvasDataJson = canvasJson,
                createdAtUtc = now,
                updatedAtUtc = now
            )
            val res = saveNoteDirect(note)
            if (res.isSuccess) {
                repository.incrementTemplateUsage(template.id)
                onCreated(res.getOrDefault(newId), NoteType.STANDARD.name)
            }
        }
    }

    fun saveCustomTemplate(
        name: String,
        description: String,
        category: String,
        titlePattern: String,
        contentTemplate: String,
        defaultTagsCsv: String = "",
        defaultColorHex: String = "",
        checklistTemplateJson: String = "",
        onResult: (Result<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val template = NoteTemplateEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim().ifBlank { "Custom Template" },
                description = description.trim(),
                category = category.trim().ifBlank { "Custom" },
                titlePattern = titlePattern.ifBlank { "{{title}}" },
                contentTemplate = contentTemplate,
                defaultTagsCsv = defaultTagsCsv,
                defaultColorHex = defaultColorHex,
                checklistTemplateJson = checklistTemplateJson,
                isBuiltIn = false,
                usageCount = 0
            )
            val res = repository.saveTemplate(template)
            onResult(res)
        }
    }

    fun saveNoteAsTemplate(
        name: String,
        description: String,
        category: String,
        note: NoteEntity,
        onResult: (Result<String>) -> Unit = {}
    ) {
        if (note.isLocked) {
            onResult(Result.failure(SecurityException("Locked notes cannot be saved as plaintext templates.")))
            return
        }
        saveCustomTemplate(
            name = name,
            description = description.ifBlank { "Saved from \"${note.title.ifBlank { "Untitled" }}\"" },
            category = category,
            titlePattern = note.title.ifBlank { "{{title}}" },
            contentTemplate = note.content,
            defaultTagsCsv = note.tagsCsv,
            defaultColorHex = note.colorHex,
            checklistTemplateJson = note.checklistJson,
            onResult = onResult
        )
    }

    fun deleteTemplate(id: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteTemplate(id)
            onResult(res)
        }
    }

    fun openOrCreateWikiLink(
        targetTitle: String,
        onReady: (String, String) -> Unit
    ) {
        val cleanTarget = targetTitle.trim()
        if (cleanTarget.isBlank()) return
        viewModelScope.launch {
            val notes = withContext(Dispatchers.IO) { repository.getAllNotesDirect() }
            val eligible = notes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }
            val existing = KnowledgeBrainEngine.resolveTargetNote(cleanTarget, eligible)
            if (existing != null) {
                onReady(existing.id, existing.noteType)
            } else {
                val now = System.currentTimeMillis()
                val newId = UUID.randomUUID().toString()
                val newNote = NoteEntity(
                    id = newId,
                    title = cleanTarget,
                    content = "# $cleanTarget\n\n",
                    noteType = NoteType.STANDARD.name,
                    createdAtUtc = now,
                    updatedAtUtc = now
                )
                saveNoteDirect(newNote)
                onReady(newId, NoteType.STANDARD.name)
            }
        }
    }

    fun setSelectedTag(tag: String?) {
        selectedTag.value = tag
    }

    fun setSelectedTypeFilter(type: NoteType?) {
        selectedTypeFilter.value = type
    }

    fun toggleGridView() {
        val next = !isGridView.value
        isGridView.value = next
        settingsPrefs.edit { putBoolean("home_grid_view", next) }
    }

    fun setEditorFontSizeSp(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(13f, 22f)
        editorFontSizeSp.value = clamped
        settingsPrefs.edit { putFloat("editor_font_size_sp", clamped) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        settingsPrefs.edit { putBoolean("notifications_enabled", enabled) }
    }

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        settingsPrefs.edit { putString("theme_mode", mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        dynamicColorEnabled.value = enabled
        settingsPrefs.edit { putBoolean("dynamic_color_enabled", enabled) }
    }

    fun updateQuickEditConfig(config: QuickEditToolbarConfig) {
        val normalized = config.copy(
            enabledTools = config.orderedEnabledTools().ifEmpty { QuickEditToolbarConfig.DEFAULT_TOOLS },
            frequentTools = config.frequentTools.intersect(config.enabledTools.toSet())
        )
        quickEditConfig.value = normalized
        val encoded = QuickEditToolbarConfigCodec.encode(normalized)
        settingsPrefs.edit { putString(KEY_QUICK_EDIT_TOOLBAR_CONFIG, encoded) }
        viewModelScope.launch {
            repository.setSetting(KEY_QUICK_EDIT_TOOLBAR_CONFIG, encoded)
        }
    }

    fun resetQuickEditConfig() {
        updateQuickEditConfig(QuickEditToolbarConfig.default())
    }

    fun updateUserProfile(name: String, email: String) {
        userProfileName.value = name.trim()
        userProfileEmail.value = email.trim()
        settingsPrefs.edit {
            putString("user_name", userProfileName.value)
            putString("user_email", userProfileEmail.value)
        }
    }

    fun duplicateNote(note: NoteEntity, onDuplicated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = if (note.isLocked) {
                val plainTitle = securityManager.decrypt(note.title).ifBlank { "Untitled" }
                val encryptedCopyTitle = securityManager.encrypt("$plainTitle (Copy)")
                val newId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val duplicated = note.copy(
                    id = newId,
                    title = encryptedCopyTitle,
                    createdAtUtc = now,
                    updatedAtUtc = now
                )
                repository.insertNote(duplicated)
            } else {
                repository.duplicateNote(note)
            }
            res.getOrNull()?.let { onDuplicated(it) }
        }
    }

    fun updateFolder(folder: FolderEntity, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.updateFolder(folder)
            onResult(res)
        }
    }

    fun moveFolder(folderId: String, newParentId: String?, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.moveFolder(folderId, newParentId)
            onResult(res)
        }
    }

    fun moveNoteToFolder(noteId: String, folder: FolderEntity?, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.moveNoteToFolder(noteId, folder)
            onResult(res)
        }
    }

    fun createMediaNote(title: String, mediaUri: String, isPdf: Boolean, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val noteId = UUID.randomUUID().toString()
            val note = NoteEntity(
                id = noteId,
                title = title.ifBlank { if (isPdf) "Document PDF" else "Photo Note" },
                content = if (isPdf) "Imported PDF document: $title" else "Imported image attachment",
                noteType = if (isPdf) NoteType.PDF_DOC.name else NoteType.STANDARD.name,
                imageUri = mediaUri,
                colorHex = if (isPdf) "#FFE4E6" else "#EFF6FF",
                createdAtUtc = System.currentTimeMillis(),
                updatedAtUtc = System.currentTimeMillis()
            )
            repository.insertNote(note)
            onSaved(noteId)
        }
    }

    // Vault Actions
    fun refreshLockoutStatus() {
        vaultLockoutSeconds.value = securityManager.getRemainingLockoutSeconds()
        failedUnlockAttempts.value = securityManager.getFailedAttempts()
    }

    fun unlockVault(pin: String): Boolean {
        refreshLockoutStatus()
        if (vaultLockoutSeconds.value > 0) return false
        val success = securityManager.verifyPin(pin)
        refreshLockoutStatus()
        if (success) {
            isVaultUnlocked.value = true
            vaultSessionEpoch.value += 1L
        }
        return success
    }

    fun unlockVaultWithBiometrics() {
        securityManager.markBiometricUnlockSuccess()
        isVaultUnlocked.value = true
        vaultSessionEpoch.value += 1L
        refreshLockoutStatus()
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        securityManager.setBiometricsEnabled(enabled)
        biometricsEnabled.value = enabled
    }

    fun lockVault() {
        securityManager.lockVault()
        isVaultUnlocked.value = false
        attachmentStorageManager.clearTempVaultViewCache()
    }

    fun updateVaultPin(currentPin: String, newPin: String): Boolean {
        refreshLockoutStatus()
        return if (securityManager.verifyPin(currentPin)) {
            securityManager.setVaultPin(newPin)
            refreshLockoutStatus()
            true
        } else {
            refreshLockoutStatus()
            false
        }
    }

    fun verifyVaultPin(pin: String): Boolean {
        refreshLockoutStatus()
        if (vaultLockoutSeconds.value > 0) return false
        val success = securityManager.verifyPin(pin)
        refreshLockoutStatus()
        return success
    }
    
    fun isVaultInitialized(): Boolean {
        return securityManager.isVaultInitialized()
    }

    fun setAutoLockTimeoutWithPin(pin: String, timeoutMs: Long): Boolean {
        if (!verifyVaultPin(pin)) return false
        setAutoLockTimeout(timeoutMs)
        return true
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        securityManager.setAutoLockTimeout(timeoutMs)
        autoLockTimeoutMs.value = timeoutMs
        if (isVaultUnlocked.value) {
            securityManager.recordActivity()
            vaultSessionEpoch.value += 1L
        }
    }

    fun handleVaultExitOrBackground() {
        if (!isVaultUnlocked.value) return
        if (autoLockTimeoutMs.value <= 0L) {
            lockVault()
        } else {
            securityManager.recordActivity()
            vaultSessionEpoch.value += 1L
        }
    }

    fun getRemainingAutoLockDelayMs(): Long = securityManager.getRemainingAutoLockDelayMs()

    fun checkAutoLockExpiration() {
        if (isVaultUnlocked.value && !securityManager.isVaultSessionUnlocked()) {
            lockVault()
        }
    }

    // Serialized Save Queue: Guarantees FIFO execution and prevents older snapshots from overwriting newer edits
    private val editorSaveMutex = Mutex()
    private val latestSavedRevisions = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun saveNoteSerialized(note: NoteEntity, revision: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            editorSaveMutex.withLock {
                val lastSaved = latestSavedRevisions[note.id] ?: 0L
                if (revision > 0 && revision < lastSaved) {
                    // Stale snapshot: an equal or newer revision has already been persisted to SQLite
                    return@withLock Result.success(note.id)
                }
                val result = repository.saveOrUpdateNote(note)
                if (result.isSuccess) {
                    val effectiveId = result.getOrThrow()
                    latestSavedRevisions[effectiveId] = revision
                    latestSavedRevisions[note.id] = revision
                    Result.success(effectiveId)
                } else {
                    result
                }
            }
        }
    }

    // Note operations
    fun saveNote(note: NoteEntity, revision: Long = System.currentTimeMillis(), onSaved: (Result<String>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = saveNoteSerialized(note, revision)
            withContext(Dispatchers.Main) {
                onSaved(result)
            }
        }
    }

    fun flushSaveDirect(note: NoteEntity, revision: Long): Result<String> {
        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500L) {
                    saveNoteSerialized(note, revision)
                }
            } ?: run {
                viewModelScope.launch(Dispatchers.IO) {
                    saveNoteSerialized(note, revision)
                }
                Result.success(note.id)
            }
        } catch (e: Throwable) {
            Log.e("NoteViewModel", "flushSaveDirect failed", e)
            viewModelScope.launch(Dispatchers.IO) {
                saveNoteSerialized(note, revision)
            }
            Result.failure(e)
        }
    }

    suspend fun saveNoteDirect(note: NoteEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            saveNoteSerialized(note, System.currentTimeMillis())
        }
    }

    suspend fun getNoteByIdDirect(id: String): NoteEntity? {
        return withContext(Dispatchers.IO) {
            repository.getNoteByIdDirect(id)
        }
    }

    fun softDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.softDeleteNote(note)
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreNote(note)
        }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch {
            repository.deletePermanently(id)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }

    fun setTrashRetentionDays(days: Int) {
        trashRetentionDays.value = days
        settingsPrefs.edit { putInt(KEY_TRASH_RETENTION_DAYS, days) }
        if (days > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.purgeExpiredTrashItems(days)
            }
        }
    }

    suspend fun purgeExpiredTrashItemsDirect(): Result<Int> {
        return repository.purgeExpiredTrashItems(trashRetentionDays.value)
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note)
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(note)
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleArchive(note)
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleLock(note)
        }
    }

    fun toggleHideNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleHide(note)
        }
    }

    fun createFolder(
        name: String,
        colorHex: String = "#D85C37",
        icon: String = "folder",
        parentId: String? = null,
        onCreated: (FolderEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val folder = FolderEntity(
                parentId = parentId?.takeIf { it.isNotBlank() },
                name = name.trim(),
                colorHex = colorHex,
                iconName = icon
            )
            repository.insertFolder(folder)
            onCreated(folder)
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            if (selectedFolderId.value == id) {
                selectedFolderId.value = null
            }
            repository.deleteFolder(id)
        }
    }

    fun createTag(name: String, colorHex: String = "#6366F1") {
        viewModelScope.launch {
            repository.insertTag(TagEntity(name = name.trim().removePrefix("#"), colorHex = colorHex))
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch {
            repository.deleteTag(id)
        }
    }

    fun exportBackup(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val file = backupManager.saveBackupToFile()
            onComplete(file)
        }
    }

    fun restoreBackup(json: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.restoreFromJson(json)
            if (res.isSuccess) {
                refreshStickyLayouts()
            }
            onResult(res)
        }
    }

    fun exportBackupZip(
        password: String? = null,
        onError: (Throwable) -> Unit = {},
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { backupManager.createBackupZip(password) }
            }
            res.onSuccess(onComplete).onFailure { err ->
                Log.e("NoteViewModel", "Failed to create backup ZIP", err)
                onError(err)
            }
        }
    }

    fun exportBackupZipToUri(
        destinationUri: android.net.Uri,
        password: String? = null,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val file = backupManager.createBackupZip(password)
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(destinationUri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    } ?: throw java.io.IOException("Unable to open destination file for writing")
                    file.length()
                }
            }
            onResult(res)
        }
    }

    fun importBackupZipFromUri(
        sourceUri: android.net.Uri,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val backupDir = File(app.filesDir, "backups")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    val importedFile = File(backupDir, "NoteLand_Imported_$stamp.zip")
                    app.contentResolver.openInputStream(sourceUri)?.use { input ->
                        importedFile.outputStream().use { out ->
                            val copied = input.copyTo(out)
                            if (copied <= 0L) {
                                throw java.io.IOException("Selected backup file is empty")
                            }
                        }
                    } ?: throw java.io.IOException("Unable to read selected backup file")
                    // Quick ZIP header verification (PK\x03\x04)
                    val header = ByteArray(2)
                    val readCount = importedFile.inputStream().use { it.read(header) }
                    if (readCount < 2 || header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                        importedFile.delete()
                        throw IllegalArgumentException("Selected file is not a valid .zip backup archive")
                    }
                    importedFile
                }
            }
            onResult(res)
        }
    }

    fun previewBackupZip(file: File, password: String? = null, onResult: (Result<BackupPreview>) -> Unit) {
        viewModelScope.launch {
            val preview = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                backupManager.validateAndPreview(file, password)
            }
            onResult(preview)
        }
    }

    fun restoreBackupZip(
        file: File,
        strategy: RestoreStrategy,
        password: String? = null,
        onResult: (Result<RestoreResult>) -> Unit
    ) {
        viewModelScope.launch {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                backupManager.restoreFromZip(file, strategy, password)
            }
            if (res.isSuccess) {
                refreshStickyLayouts()
            }
            onResult(res)
        }
    }

    fun listLocalBackups(): List<File> = backupManager.listLocalBackups()

    // ==========================================
    // STICKY NOTES BOARD OPERATIONS
    // ==========================================
    val stickyLayouts = MutableStateFlow<Map<String, StickyLayout>>(loadStickyLayouts())

    fun refreshStickyLayouts() {
        stickyLayouts.value = loadStickyLayouts()
    }

    private fun loadStickyLayouts(): Map<String, StickyLayout> {
        val jsonStr = settingsPrefs.getString("sticky_board_layouts", "") ?: ""
        val map = mutableMapOf<String, StickyLayout>()
        if (jsonStr.isNotBlank()) {
            try {
                val obj = org.json.JSONObject(jsonStr)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = obj.getJSONObject(key)
                    map[key] = StickyLayout(
                        id = key,
                        x = item.optDouble("x", 20.0).toFloat(),
                        y = item.optDouble("y", 20.0).toFloat(),
                        width = StickyLayout.clampWidth(item.optDouble("width", StickyLayout.DEFAULT_WIDTH.toDouble()).toFloat()),
                        height = StickyLayout.clampHeight(item.optDouble("height", StickyLayout.DEFAULT_HEIGHT.toDouble()).toFloat()),
                        rotationDeg = StickyLayout.normalizeAngle(item.optDouble("rotation", 0.0).toFloat()),
                        zIndex = item.optInt("zIndex", 0)
                    )
                }
            } catch (e: Exception) {
                Log.w("NoteViewModel", "Failed to load sticky board layouts", e)
            }
        }
        return map
    }

    private fun persistStickyLayouts(layouts: Map<String, StickyLayout>) {
        try {
            val obj = org.json.JSONObject()
            for ((key, layout) in layouts) {
                val item = org.json.JSONObject()
                item.put("x", layout.x.toDouble())
                item.put("y", layout.y.toDouble())
                item.put("width", layout.width.toDouble())
                item.put("height", layout.height.toDouble())
                item.put("rotation", layout.rotationDeg.toDouble())
                item.put("zIndex", layout.zIndex)
                obj.put(key, item)
            }
            settingsPrefs.edit { putString("sticky_board_layouts", obj.toString()) }
        } catch (e: Exception) {
            Log.w("NoteViewModel", "Failed to persist sticky board layouts", e)
        }
    }

    fun saveStickyNote(sticky: com.example.data.model.StickyNoteEntity) {
        viewModelScope.launch {
            repository.insertStickyNote(sticky)
        }
    }

    fun duplicateStickyNote(sticky: com.example.data.model.StickyNoteEntity) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val duplicated = sticky.copy(
                id = newId,
                title = if (sticky.title.isNotBlank()) "${sticky.title} (Copy)" else "Sticky (Copy)",
                createdAtUtc = System.currentTimeMillis(),
                updatedAtUtc = System.currentTimeMillis()
            )
            repository.insertStickyNote(duplicated)
            val currentLayout = stickyLayouts.value[sticky.id]
            val newX = (currentLayout?.x ?: 30f) + 30f
            val newY = (currentLayout?.y ?: 30f) + 30f
            val currentMap = stickyLayouts.value.toMutableMap()
            val nextZ = (currentMap.values.maxOfOrNull { it.zIndex } ?: -1) + 1
            currentMap[newId] = StickyLayout(
                id = newId,
                x = newX.coerceAtLeast(0f),
                y = newY.coerceAtLeast(0f),
                width = currentLayout?.width ?: StickyLayout.DEFAULT_WIDTH,
                height = currentLayout?.height ?: StickyLayout.DEFAULT_HEIGHT,
                rotationDeg = currentLayout?.rotationDeg ?: 0f,
                zIndex = nextZ
            )
            stickyLayouts.value = currentMap
            persistStickyLayouts(currentMap)
        }
    }

    fun deleteStickyNote(id: String) {
        viewModelScope.launch {
            repository.deleteStickyNote(id)
            val updated = stickyLayouts.value.toMutableMap()
            updated.remove(id)
            stickyLayouts.value = updated
            persistStickyLayouts(updated)
        }
    }

    fun toggleStickyPin(sticky: com.example.data.model.StickyNoteEntity) {
        viewModelScope.launch {
            repository.updateStickyNote(sticky.copy(isPinned = !sticky.isPinned, updatedAtUtc = System.currentTimeMillis()))
        }
    }

    fun updateStickyPosition(id: String, x: Float, y: Float, zIndex: Int? = null) {
        val current = stickyLayouts.value.toMutableMap()
        val existing = current[id]
        val resolvedZ = zIndex ?: existing?.zIndex ?: ((current.values.maxOfOrNull { it.zIndex } ?: -1) + 1)
        current[id] = (existing ?: StickyLayout(id, x, y, zIndex = resolvedZ)).copy(
            x = x.coerceAtLeast(0f),
            y = y.coerceAtLeast(0f),
            zIndex = resolvedZ
        )
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    fun moveStickyToFront(id: String, orderedBoardIds: List<String> = emptyList()) {
        val current = stickyLayouts.value.toMutableMap()
        orderedBoardIds.forEachIndexed { idx, noteId ->
            if (!current.containsKey(noteId)) {
                current[noteId] = StickyLayout(noteId, 24f, 24f, zIndex = idx)
            }
        }
        if (!current.containsKey(id)) {
            current[id] = StickyLayout(id, 24f, 24f)
        }
        val sortedOthers = current.values
            .filter { it.id != id }
            .sortedBy { it.zIndex }
        sortedOthers.forEachIndexed { idx, layout ->
            current[layout.id] = layout.copy(zIndex = idx)
        }
        current[id] = current.getValue(id).copy(zIndex = sortedOthers.size)
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    fun moveStickyToBack(id: String, orderedBoardIds: List<String> = emptyList()) {
        val current = stickyLayouts.value.toMutableMap()
        orderedBoardIds.forEachIndexed { idx, noteId ->
            if (!current.containsKey(noteId)) {
                current[noteId] = StickyLayout(noteId, 24f, 24f, zIndex = idx)
            }
        }
        if (!current.containsKey(id)) {
            current[id] = StickyLayout(id, 24f, 24f)
        }
        val sortedOthers = current.values
            .filter { it.id != id }
            .sortedBy { it.zIndex }
        current[id] = current.getValue(id).copy(zIndex = 0)
        sortedOthers.forEachIndexed { idx, layout ->
            current[layout.id] = layout.copy(zIndex = idx + 1)
        }
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    fun updateStickySize(id: String, width: Float, height: Float) {
        val current = stickyLayouts.value.toMutableMap()
        val existing = current[id] ?: StickyLayout(id, 20f, 20f)
        current[id] = existing.copy(
            width = StickyLayout.clampWidth(width),
            height = StickyLayout.clampHeight(height)
        )
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    fun updateStickyRotation(id: String, rotationDeg: Float) {
        val current = stickyLayouts.value.toMutableMap()
        val existing = current[id] ?: StickyLayout(id, 20f, 20f)
        current[id] = existing.copy(rotationDeg = StickyLayout.normalizeAngle(rotationDeg))
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    fun nudgeSticky(id: String, deltaX: Float, deltaY: Float) {
        val current = stickyLayouts.value[id] ?: StickyLayout(id, 20f, 20f)
        updateStickyPosition(id, (current.x + deltaX).coerceAtLeast(0f), (current.y + deltaY).coerceAtLeast(0f))
    }

    fun alignStickiesToGrid(gridSize: Float = 40f) {
        val current = stickyLayouts.value.toMutableMap()
        for ((key, layout) in current) {
            val snappedX = (Math.round(layout.x / gridSize) * gridSize).coerceAtLeast(20f)
            val snappedY = (Math.round(layout.y / gridSize) * gridSize).coerceAtLeast(20f)
            current[key] = layout.copy(x = snappedX, y = snappedY)
        }
        stickyLayouts.value = current
        persistStickyLayouts(current)
    }

    // ==========================================
    // INFINITE CANVAS OPERATIONS
    // ==========================================
    fun createCanvas(title: String = "Untitled", bgColorHex: String = "#FAF5FF", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val canvas = com.example.data.model.CanvasEntity(
                id = newId,
                title = title.trim().ifBlank { "Untitled" },
                backgroundColorHex = bgColorHex,
                createdAtUtc = System.currentTimeMillis(),
                updatedAtUtc = System.currentTimeMillis()
            )
            val result = repository.saveCanvasWithElements(canvas, emptyList())
            if (result.isSuccess) {
                onCreated(newId)
            }
        }
    }

    fun createMindMapProject(title: String = "Untitled Mind Map", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val canvas = com.example.data.model.CanvasEntity(
                id = newId,
                title = title.trim().ifBlank { "Untitled Mind Map" },
                backgroundColorHex = "#FFFFFF",
                createdAtUtc = now,
                updatedAtUtc = now
            )
            val config = com.example.ui.screens.mindmap.MindMapStyleConfig()
            val result = repository.saveCanvasWithElements(canvas, listOf(config.toEntity(newId)))
            if (result.isSuccess) {
                onCreated(newId)
            }
        }
    }

    fun convertNoteToMindMap(noteId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val note = repository.getNoteByIdDirect(noteId) ?: return@launch
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val canvas = com.example.data.model.CanvasEntity(
                id = newId,
                title = note.title.ifBlank { "Mind Map from Note" },
                backgroundColorHex = "#FFFFFF",
                createdAtUtc = now,
                updatedAtUtc = now
            )
            
            val lines = note.content.lines().filter { it.isNotBlank() }
            val config = com.example.ui.screens.mindmap.MindMapStyleConfig()
            
            val elements = mutableListOf<com.example.data.model.CanvasElementEntity>()
            elements.add(config.toEntity(newId))
            
            // Create root node
            val rootNode = com.example.ui.screens.mindmap.MindMapNode(
                title = note.title.ifBlank { "Root" },
                level = 0,
                x = 0f,
                y = 0f,
                colorArgb = config.themePreset.rootStart.toArgb()
            )
            
            elements.add(rootNode.toEntity(newId, elements.size))
            
            var currentY = -((lines.size - 1) * 60f) / 2f
            
            for (line in lines) {
                val cleanLine = line.trimStart('-', '*', ' ', '\t')
                val childNode = com.example.ui.screens.mindmap.MindMapNode(
                    parentId = rootNode.id,
                    title = cleanLine,
                    level = 1,
                    x = 200f,
                    y = currentY,
                    colorArgb = config.themePreset.canvasTint.toArgb()
                )
                elements.add(childNode.toEntity(newId, elements.size))
                currentY += 60f
            }
            
            val result = repository.saveCanvasWithElements(canvas, elements)
            if (result.isSuccess) {
                onCreated(newId)
            }
        }
    }

    fun createFreeformCanvasProject(
        title: String = "Untitled",
        bgColorHex: String = "#FFFFFF",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val canvas = com.example.data.model.CanvasEntity(
                id = newId,
                title = title.trim().ifBlank { "Untitled" },
                backgroundColorHex = bgColorHex,
                createdAtUtc = now,
                updatedAtUtc = now
            )
            val config = com.example.ui.screens.mindmap.MindMapStyleConfig(
                showGrid = true,
                showMintWaves = false
            )
            val result = repository.saveCanvasWithElements(canvas, listOf(config.toEntity(newId)))
            if (result.isSuccess) {
                onCreated(newId)
            }
        }
    }

    fun deleteCanvas(id: String) {
        viewModelScope.launch {
            repository.deleteCanvas(id)
        }
    }

    fun duplicateCanvas(id: String, onDuplicated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val originalCanvas = repository.getCanvasByIdDirect(id) ?: return@launch
            val originalElements = repository.getElementsForCanvasDirect(id)
            val newId = UUID.randomUUID().toString()
            val newCanvas = originalCanvas.copy(
                id = newId,
                title = originalCanvas.title + " (Copy)",
                createdAtUtc = System.currentTimeMillis(),
                updatedAtUtc = System.currentTimeMillis()
            )
            
            val idMapping = originalElements.associate { it.id to UUID.randomUUID().toString() }
            val newElements = originalElements.map { el ->
                var dataJson = el.elementDataJson
                if (el.elementType == "MINDMAP_NODE") {
                    try {
                        val obj = org.json.JSONObject(dataJson)
                        val pId = obj.optString("parentId", "")
                        if (pId.isNotBlank() && idMapping.containsKey(pId)) {
                            obj.put("parentId", idMapping[pId])
                        }
                        dataJson = obj.toString()
                    } catch (e: Exception) {}
                }
                
                el.copy(
                    id = idMapping[el.id] ?: UUID.randomUUID().toString(),
                    canvasId = newId,
                    elementDataJson = dataJson,
                    createdAtUtc = System.currentTimeMillis()
                )
            }
            
            val result = repository.saveCanvasWithElements(newCanvas, newElements)
            if (result.isSuccess) {
                onDuplicated(newId)
            }
        }
    }

    suspend fun getCanvasByIdDirect(canvasId: String): com.example.data.model.CanvasEntity? {
        return withContext(Dispatchers.IO) {
            repository.getCanvasByIdDirect(canvasId)
        }
    }

    fun loadCanvasElements(canvasId: String, onLoaded: (List<com.example.data.model.CanvasElementEntity>) -> Unit) {
        viewModelScope.launch {
            val elements = repository.getElementsForCanvasDirect(canvasId)
            onLoaded(elements)
        }
    }

    fun saveCanvasWithElements(
        canvas: com.example.data.model.CanvasEntity,
        elements: List<com.example.data.model.CanvasElementEntity>,
        onResult: (Result<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveCanvasWithElements(canvas, elements)
            onResult(result)
        }
    }

    fun saveAttachmentEntityDirect(attachment: com.example.data.model.AttachmentEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveAttachment(attachment)
            }
        }
    }

    // ==========================================
    // PHASE 5: IMAGE & PDF WORKSPACE OPERATIONS
    // ==========================================
    val bookmarks: StateFlow<List<com.example.data.model.BookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importMediaFile(
        uri: android.net.Uri,
        fileName: String,
        isPdf: Boolean,
        noteId: String? = null,
        folderId: String? = null,
        onDeduplicated: (Boolean) -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onSaved: (com.example.data.model.AttachmentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val saveResult = withContext(Dispatchers.IO) {
                val resolvedDisplayName = attachmentStorageManager.resolveDisplayName(uri, fileName)
                attachmentStorageManager.saveFromUriWithDedupSuspend(
                    uri = uri,
                    fileName = resolvedDisplayName,
                    noteId = noteId,
                    folderId = folderId,
                    expectPdf = isPdf,
                    existingByHashLookup = { hash -> repository.getAttachmentByHash(hash) }
                ).also { result ->
                    result.onSuccess { outcome ->
                        val attachment = outcome.attachment
                        repository.saveAttachment(attachment)
                        if (noteId == null && !isPdf) {
                            val newNoteId = UUID.randomUUID().toString()
                            val note = NoteEntity(
                                id = newNoteId,
                                title = attachment.fileName.substringBeforeLast(".").ifBlank { "Imported Image" },
                                content = "Imported photo attachment: ${attachment.fileName}",
                                noteType = NoteType.STANDARD.name,
                                folderId = folderId,
                                imageUri = attachmentStorageManager.getFileFromRelativePath(attachment.relativePath).absolutePath,
                                colorHex = "#EFF6FF",
                                createdAtUtc = System.currentTimeMillis(),
                                updatedAtUtc = System.currentTimeMillis()
                            )
                            repository.insertNote(note)
                        }
                    }
                }
            }
            saveResult.onSuccess { outcome ->
                onDeduplicated(outcome.deduplicatedFromExisting)
                onSaved(outcome.attachment)
            }.onFailure { error ->
                onError(error)
            }
        }
    }

    fun getAttachmentsForNote(noteId: String): Flow<List<com.example.data.model.AttachmentEntity>> {
        return repository.getAttachmentsForNote(noteId)
    }

    fun attachDocumentToNote(
        noteSnapshot: NoteEntity,
        uri: android.net.Uri,
        fileName: String,
        onError: (Throwable) -> Unit = {},
        onSaved: (com.example.data.model.AttachmentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Ensure parent NoteEntity exists in Room before inserting AttachmentEntity (FK safety)
                    val existing = repository.getNoteByIdDirect(noteSnapshot.id)
                    if (existing == null) {
                        repository.saveOrUpdateNote(noteSnapshot).getOrThrow()
                    }
                    val resolvedDisplayName = attachmentStorageManager.resolveDisplayName(uri, fileName)
                    val outcome = attachmentStorageManager.saveFromUriWithDedupSuspend(
                        uri = uri,
                        fileName = resolvedDisplayName,
                        noteId = noteSnapshot.id,
                        folderId = noteSnapshot.folderId,
                        expectPdf = resolvedDisplayName.endsWith(".pdf", ignoreCase = true),
                        existingByHashLookup = { hash -> repository.getAttachmentByHash(hash) }
                    ).getOrThrow()
                    repository.saveAttachment(outcome.attachment).getOrThrow()
                    outcome.attachment
                }
            }
            result.onSuccess(onSaved).onFailure(onError)
        }
    }

    fun renameAttachment(
        attachmentId: String,
        newName: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.renameAttachment(attachmentId, newName)
            onResult(res)
        }
    }

    fun moveAttachmentToFolder(
        attachmentId: String,
        folderId: String?,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.moveAttachmentToFolder(attachmentId, folderId)
            onResult(res)
        }
    }

    fun toggleAttachmentFavorite(attachment: AttachmentEntity) {
        viewModelScope.launch {
            repository.toggleAttachmentFavorite(attachment)
        }
    }

    fun toggleAttachmentArchive(attachment: AttachmentEntity) {
        viewModelScope.launch {
            repository.toggleAttachmentArchive(attachment)
        }
    }

    fun softDeleteAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            repository.softDeleteAttachment(attachment)
        }
    }

    fun restoreAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            repository.restoreAttachment(attachment)
        }
    }

    fun deleteAttachmentPermanently(attachment: AttachmentEntity) {
        deleteAttachment(attachment)
    }

    data class ViewerFileResolution(
        val file: File?,
        val isTempDecrypted: Boolean,
        val isLockedOut: Boolean,
        val isEncrypted: Boolean
    )

    suspend fun getAttachmentByIdDirect(attachmentId: String): AttachmentEntity? {
        if (attachmentId.isBlank()) return null
        return repository.getAttachmentById(attachmentId)
    }

    suspend fun resolveAttachmentForViewer(
        attachmentId: String,
        rawFilePath: String,
        defaultExtension: String = "pdf"
    ): ViewerFileResolution = withContext(Dispatchers.IO) {
        val attachment = if (attachmentId.isNotBlank() && !attachmentId.startsWith("demo")) {
            repository.getAttachmentById(attachmentId)
        } else null
        val storedFile = when {
            attachment != null -> attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
            rawFilePath.isNotBlank() -> attachmentStorageManager.getFileFromRelativePath(rawFilePath)
            else -> null
        }
        val isEncrypted = (attachment?.isLocked == true) ||
            rawFilePath.contains("vault_files/") ||
            (storedFile != null && securityManager.isEncryptedVaultFile(storedFile))
        if (isEncrypted && !isVaultUnlocked.value) {
            return@withContext ViewerFileResolution(
                file = null,
                isTempDecrypted = false,
                isLockedOut = true,
                isEncrypted = true
            )
        }
        if (isEncrypted) {
            val ext = attachment?.fileExtension?.ifBlank { defaultExtension }
                ?: storedFile?.extension?.ifBlank { defaultExtension }
                ?: defaultExtension
            val decFile = attachment?.let { attachmentStorageManager.createTempDecryptedFileForViewer(it) }
                ?: storedFile?.let { attachmentStorageManager.createTempDecryptedFileForViewerByPath(it.absolutePath, ext) }
            return@withContext ViewerFileResolution(
                file = decFile,
                isTempDecrypted = decFile != null && storedFile != null && decFile.absolutePath != storedFile.absolutePath,
                isLockedOut = false,
                isEncrypted = true
            )
        }
        ViewerFileResolution(
            file = storedFile,
            isTempDecrypted = false,
            isLockedOut = false,
            isEncrypted = false
        )
    }

    fun toggleAttachmentLock(
        attachment: AttachmentEntity,
        onResult: (Result<AttachmentEntity>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.setAttachmentLocked(
                attachment = attachment,
                locked = !attachment.isLocked,
                storageManager = attachmentStorageManager
            )
            onResult(res)
        }
    }

    fun createNoteFromPdfSelection(
        title: String,
        selectedText: String,
        pdfAttachmentId: String?,
        pdfFileName: String,
        pageNumber: Int,
        folderId: String? = null,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank {
                val base = pdfFileName.substringBeforeLast('.').ifBlank { "PDF Extract" }
                "$base (Page $pageNumber)"
            }
            val cleanBody = buildString {
                append(selectedText.trim())
                if (pdfFileName.isNotBlank()) {
                    append("\n\n— Source: $pdfFileName • Page $pageNumber")
                }
                if (!pdfAttachmentId.isNullOrBlank()) {
                    append("\n[pdf_ref:$pdfAttachmentId#p$pageNumber]")
                }
            }
            val foldersList = folders.value
            val folderName = folderId?.let { id -> foldersList.firstOrNull { it.id == id }?.name }.orEmpty()
            val now = System.currentTimeMillis()
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                content = cleanBody,
                noteType = NoteType.STANDARD.name,
                folderId = folderId,
                folderName = folderName,
                tagsCsv = "pdf-extract",
                createdAtUtc = now,
                updatedAtUtc = now
            )
            val res = repository.saveOrUpdateNote(note)
            res.onSuccess { newId -> onCreated(newId) }
        }
    }

    fun exportFileToUri(
        sourceFile: File,
        targetUri: android.net.Uri,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (!sourceFile.exists()) {
                        throw java.io.FileNotFoundException("Export source file not found")
                    }
                    val outStream = getApplication<android.app.Application>().contentResolver.openOutputStream(targetUri)
                        ?: throw java.io.IOException("Cannot open destination URI")
                    outStream.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    Unit
                }
            }
            onComplete(result)
        }
    }

    fun exportAttachmentToUri(
        attachment: com.example.data.model.AttachmentEntity,
        targetUri: android.net.Uri,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val sourceFile = attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
                    if (!sourceFile.exists()) {
                        throw java.io.FileNotFoundException("Attachment file not found")
                    }
                    val outStream = getApplication<android.app.Application>().contentResolver.openOutputStream(targetUri)
                        ?: throw java.io.IOException("Cannot open destination URI")
                    outStream.use { output ->
                        if (attachment.isLocked && securityManager.isEncryptedVaultFile(sourceFile)) {
                            securityManager.decryptFileToStream(sourceFile, output)
                        } else {
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                    Unit
                }
            }
            onComplete(result)
        }
    }

    fun saveBackgroundImageFromUri(
        uri: android.net.Uri,
        onError: (Throwable) -> Unit = {},
        onSaved: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                attachmentStorageManager.saveBackgroundImageFromUri(uri)
            }
            result.onSuccess { path ->
                onSaved(path)
            }.onFailure { error ->
                onError(error)
            }
        }
    }

    fun savePastedImageUri(
        uri: android.net.Uri,
        onError: (Throwable) -> Unit = {},
        onSaved: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                attachmentStorageManager.saveImageAttachmentFromUri(uri)
            }
            result.onSuccess { path ->
                onSaved(path)
            }.onFailure { error ->
                onError(error)
            }
        }
    }

    fun deleteAttachment(attachment: com.example.data.model.AttachmentEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val targetFile = attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
                val targetKey = try {
                    targetFile.canonicalPath
                } catch (_: Exception) {
                    targetFile.absolutePath
                }

                // Clear imageUri reference on the linked note if it points to this deleted media item
                val linkedNoteId = attachment.noteId ?: if (attachment.id.isNotBlank()) attachment.id else null
                if (!linkedNoteId.isNullOrBlank()) {
                    val linkedNote = repository.getNoteByIdDirect(linkedNoteId)
                    if (linkedNote != null && linkedNote.imageUri.isNotBlank()) {
                        val remainingParts = linkedNote.imageUri
                            .split("|")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .filterNot { part ->
                                val partFile = attachmentStorageManager.getFileFromRelativePath(part)
                                val partKey = try {
                                    partFile.canonicalPath
                                } catch (_: Exception) {
                                    partFile.absolutePath
                                }
                                partKey == targetKey || part == attachment.relativePath
                            }
                        val updatedImageUri = remainingParts.joinToString("|")
                        if (updatedImageUri != linkedNote.imageUri) {
                            repository.updateNote(linkedNote.copy(imageUri = updatedImageUri))
                        }
                    }
                }

                val existingDbAttachment = repository.getAttachmentById(attachment.id)
                if (existingDbAttachment != null) {
                    repository.deleteAttachment(attachment.id)
                } else if (attachment.relativePath.isNotBlank()) {
                    // Synthetic note-only media item in MediaScreen: delete file only when no DB attachment/note still references it
                    if (!repository.isFileReferenced(attachment.relativePath)) {
                        attachmentStorageManager.deleteFile(attachment.relativePath)
                    }
                }
            }
        }
    }

    val pdfAutoSaveEnabled = MutableStateFlow(settingsPrefs.getBoolean("pdf_autosave_enabled", true))

    fun setPdfAutoSaveEnabled(enabled: Boolean) {
        pdfAutoSaveEnabled.value = enabled
        settingsPrefs.edit { putBoolean("pdf_autosave_enabled", enabled) }
    }

    fun hasSeededPdfDemo(attachmentId: String): Boolean {
        val safeId = attachmentId.ifBlank { "demo_pdf" }
        return settingsPrefs.getBoolean("pdf_demo_seeded_$safeId", false)
    }

    fun markSeededPdfDemo(attachmentId: String) {
        val safeId = attachmentId.ifBlank { "demo_pdf" }
        settingsPrefs.edit { putBoolean("pdf_demo_seeded_$safeId", true) }
    }

    private val pdfAnnotationSaveMutex = Mutex()

    fun loadAnnotations(attachmentId: String, onLoaded: (List<com.example.data.model.AnnotationEntity>) -> Unit) {
        val safeId = attachmentId.ifBlank { "demo_pdf" }
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                repository.getAnnotationsForAttachmentDirect(safeId)
            }
            onLoaded(list)
        }
    }

    suspend fun saveAnnotationsDirect(
        attachmentId: String,
        annotations: List<com.example.data.model.AnnotationEntity>,
        filePath: String = "",
        fileName: String = ""
    ): Result<Unit> {
        val safeId = attachmentId.ifBlank { "demo_pdf" }
        return withContext(Dispatchers.IO) {
            pdfAnnotationSaveMutex.withLock {
                val existingAttachment = repository.getAttachmentById(safeId)
                val targetAttachment = existingAttachment ?: run {
                    val filesRoot = getApplication<Application>().filesDir.absolutePath
                    val relPath = when {
                        filePath.startsWith(filesRoot) -> filePath.removePrefix(filesRoot).trimStart('/')
                        filePath.isNotBlank() -> filePath
                        else -> "attachments/$safeId.pdf"
                    }
                    val resolvedFile = if (filePath.startsWith("/")) File(filePath) else File(getApplication<Application>().filesDir, relPath)
                    val resolvedName = fileName.ifBlank { resolvedFile.name.ifBlank { "Document.pdf" } }
                    val ext = resolvedName.substringAfterLast('.', "pdf").lowercase().ifBlank { "pdf" }
                    val mime = when (ext) {
                        "png", "jpg", "jpeg", "webp" -> "image/$ext"
                        else -> "application/pdf"
                    }
                    com.example.data.model.AttachmentEntity(
                        id = safeId,
                        fileName = resolvedName,
                        fileExtension = ext,
                        relativePath = relPath,
                        mimeType = mime,
                        sizeBytes = if (resolvedFile.exists()) resolvedFile.length() else 0L
                    )
                }
                val normalized = annotations.map {
                    if (it.attachmentId == safeId) it else it.copy(attachmentId = safeId)
                }
                repository.saveAttachmentWithAnnotations(targetAttachment, normalized)
            }
        }
    }

    fun saveAnnotations(
        attachmentId: String,
        annotations: List<com.example.data.model.AnnotationEntity>,
        filePath: String = "",
        fileName: String = "",
        onSaved: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = saveAnnotationsDirect(attachmentId, annotations, filePath, fileName)
            onSaved(result)
        }
    }

    fun flushAnnotationsDirect(
        attachmentId: String,
        annotations: List<com.example.data.model.AnnotationEntity>,
        filePath: String = "",
        fileName: String = ""
    ): Result<Unit> {
        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500L) {
                    saveAnnotationsDirect(attachmentId, annotations, filePath, fileName)
                }
            } ?: run {
                viewModelScope.launch(Dispatchers.IO) {
                    saveAnnotationsDirect(attachmentId, annotations, filePath, fileName)
                }
                Result.success(Unit)
            }
        } catch (e: Throwable) {
            Log.e("NoteViewModel", "flushAnnotationsDirect failed", e)
            viewModelScope.launch(Dispatchers.IO) {
                saveAnnotationsDirect(attachmentId, annotations, filePath, fileName)
            }
            Result.failure(e)
        }
    }

    fun toggleBookmark(targetType: String, targetId: String, label: String) {
        viewModelScope.launch {
            val existing = bookmarks.value.firstOrNull { it.targetType == targetType && it.targetId == targetId }
            if (existing != null) {
                repository.deleteBookmark(existing)
            } else {
                repository.insertBookmark(
                    com.example.data.model.BookmarkEntity(
                        targetType = targetType,
                        targetId = targetId,
                        label = label
                    )
                )
            }
        }
    }

    fun updateBookmarkLabel(bookmark: com.example.data.model.BookmarkEntity, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateBookmark(bookmark.copy(label = trimmed))
        }
    }

    fun deleteBookmark(bookmark: com.example.data.model.BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    // ==========================================
    // PHASE 6: REMINDERS & NOTIFICATIONS
    // ==========================================
    private fun safeNotificationTitle(note: NoteEntity?, fallbackTitle: String): String {
        return when {
            note == null -> fallbackTitle.ifBlank { "Reminder" }
            note.isLocked -> "Locked Note Reminder"
            else -> note.title.ifBlank { fallbackTitle.ifBlank { "Reminder" } }
        }
    }

    fun scheduleNoteReminder(
        noteId: String,
        noteTitle: String,
        timeUtc: Long,
        repeatInterval: String? = null
    ) {
        viewModelScope.launch {
            val newReminder = ReminderEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                scheduledTimeUtc = timeUtc,
                repeatInterval = repeatInterval,
                isCompleted = false
            )
            repository.saveReminder(newReminder)
            val note = repository.getNoteByIdDirect(noteId)
            if (note != null) {
                repository.updateNote(note.copy(reminderTimestamp = timeUtc))
            }
            ReminderNotificationManager.scheduleReminder(
                context = getApplication(),
                reminder = newReminder,
                noteTitle = safeNotificationTitle(note, noteTitle)
            )
        }
    }

    private suspend fun syncNoteReminderTimestamp(noteId: String) {
        if (noteId.isBlank()) return
        val note = repository.getNoteByIdDirect(noteId) ?: return
        val nextTimestamp = repository.getAllRemindersDirect()
            .filter { it.noteId == noteId && !it.isCompleted }
            .minByOrNull { it.scheduledTimeUtc }
            ?.scheduledTimeUtc
        if (note.reminderTimestamp != nextTimestamp) {
            repository.updateNote(note.copy(reminderTimestamp = nextTimestamp))
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.updateReminder(updated)
            repository.syncReminderCompletionFromReminder(reminder.id, updated.isCompleted)
            syncNoteReminderTimestamp(reminder.noteId)
            if (updated.isCompleted) {
                ReminderNotificationManager.cancelReminder(getApplication(), reminder.id)
            } else if (updated.scheduledTimeUtc > System.currentTimeMillis()) {
                val note = repository.getNoteByIdDirect(reminder.noteId)
                ReminderNotificationManager.scheduleReminder(
                    context = getApplication(),
                    reminder = updated,
                    noteTitle = safeNotificationTitle(note, "Reminder")
                )
            }
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, additionalMillis: Long) {
        viewModelScope.launch {
            val newTime = System.currentTimeMillis() + additionalMillis
            val updated = reminder.copy(scheduledTimeUtc = newTime, isCompleted = false)
            repository.updateReminder(updated)
            repository.getTaskByLinkedReminderIdDirect(reminder.id)?.let { linked ->
                val duration = if (linked.scheduledStartUtc != null && linked.scheduledEndUtc != null) {
                    (linked.scheduledEndUtc - linked.scheduledStartUtc).coerceAtLeast(15 * 60_000L)
                } else {
                    linked.estimatedMinutes * 60_000L
                }
                repository.saveTask(
                    linked.copy(
                        isCompleted = false,
                        completedAtUtc = null,
                        scheduledStartUtc = newTime,
                        scheduledEndUtc = newTime + duration,
                        dueAtUtc = newTime
                    )
                )
            }
            syncNoteReminderTimestamp(reminder.noteId)
            val note = repository.getNoteByIdDirect(reminder.noteId)
            ReminderNotificationManager.scheduleReminder(
                context = getApplication(),
                reminder = updated,
                noteTitle = safeNotificationTitle(note, "Reminder")
            )
        }
    }

    fun rescheduleReminder(reminder: ReminderEntity, newTimeUtc: Long) {
        viewModelScope.launch {
            val updated = reminder.copy(scheduledTimeUtc = newTimeUtc, isCompleted = false)
            repository.updateReminder(updated)
            repository.getTaskByLinkedReminderIdDirect(reminder.id)?.let { linked ->
                val duration = if (linked.scheduledStartUtc != null && linked.scheduledEndUtc != null) {
                    (linked.scheduledEndUtc - linked.scheduledStartUtc).coerceAtLeast(15 * 60_000L)
                } else {
                    linked.estimatedMinutes * 60_000L
                }
                repository.saveTask(
                    linked.copy(
                        isCompleted = false,
                        completedAtUtc = null,
                        scheduledStartUtc = newTimeUtc,
                        scheduledEndUtc = newTimeUtc + duration,
                        dueAtUtc = newTimeUtc
                    )
                )
            }
            syncNoteReminderTimestamp(reminder.noteId)
            val note = repository.getNoteByIdDirect(reminder.noteId)
            ReminderNotificationManager.scheduleReminder(
                context = getApplication(),
                reminder = updated,
                noteTitle = safeNotificationTitle(note, "Reminder")
            )
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            val existing = repository.getReminderById(reminderId)
            val linkedTask = repository.getTaskByLinkedReminderIdDirect(reminderId)
            repository.deleteReminder(reminderId)
            ReminderNotificationManager.cancelReminder(getApplication(), reminderId)
            if (linkedTask != null) {
                if (linkedTask.id.startsWith("task_rem_") || linkedTask.id.startsWith("task_cal_")) {
                    repository.deleteTask(linkedTask.id)
                } else {
                    repository.saveTask(linkedTask.copy(linkedReminderId = null))
                }
            }
            if (existing != null) {
                syncNoteReminderTimestamp(existing.noteId)
            }
        }
    }

    fun createCalendarEvent(
        title: String,
        description: String,
        startUtc: Long,
        endUtc: Long,
        category: String,
        colorHex: String,
        repeatInterval: String? = null,
        checklistItems: List<ChecklistItem> = emptyList(),
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "Scheduled Task" }
            val noteId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val note = NoteEntity(
                id = noteId,
                title = cleanTitle,
                content = description.trim(),
                noteType = NoteType.STANDARD.name,
                colorHex = colorHex,
                tagsCsv = listOf("Calendar", category).distinct().joinToString(","),
                reminderTimestamp = startUtc,
                canvasDataJson = SmartNoteUtils.encodeCalendarMeta(startUtc, endUtc, category),
                checklistJson = if (checklistItems.isNotEmpty()) {
                    SmartNoteUtils.encodeChecklistJson(checklistItems)
                } else {
                    ""
                },
                createdAtUtc = now,
                updatedAtUtc = now
            )
            repository.saveOrUpdateNote(note)

            val newReminder = ReminderEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                scheduledTimeUtc = startUtc,
                repeatInterval = repeatInterval,
                isCompleted = false
            )
            repository.saveReminder(newReminder)

            val durationMins = (((endUtc - startUtc) / 60_000L).toInt()).coerceIn(15, 480)
            val cols = withContext(Dispatchers.IO) { repository.getAllKanbanColumnsDirect() }
            val defaultColId = cols.firstOrNull { !it.isDoneColumn }?.id ?: "col_todo"
            repository.saveTask(
                TaskEntity(
                    id = "task_cal_${newReminder.id}",
                    title = cleanTitle,
                    description = description.trim(),
                    columnId = defaultColId,
                    priority = if (category.equals("High Priority", ignoreCase = true)) {
                        TaskPriority.HIGH.name
                    } else {
                        TaskPriority.MEDIUM.name
                    },
                    dueAtUtc = startUtc,
                    dueTimeZoneId = TimeZone.getDefault().id,
                    scheduledStartUtc = startUtc,
                    scheduledEndUtc = endUtc.coerceAtLeast(startUtc + 15 * 60_000L),
                    estimatedMinutes = durationMins,
                    recurrenceRule = TaskRecurrenceRule.fromName(repeatInterval).name,
                    subtasksJson = if (checklistItems.isNotEmpty()) {
                        SmartNoteUtils.encodeChecklistJson(checklistItems)
                    } else {
                        ""
                    },
                    linkedNoteId = noteId,
                    linkedReminderId = newReminder.id,
                    category = category,
                    colorHex = colorHex,
                    createdAtUtc = now,
                    updatedAtUtc = now
                )
            )

            ReminderNotificationManager.scheduleReminder(
                context = getApplication(),
                reminder = newReminder,
                noteTitle = cleanTitle
            )
            onComplete()
        }
    }

    fun updateCalendarEvent(
        reminderId: String?,
        taskId: String?,
        noteId: String?,
        title: String,
        description: String,
        startUtc: Long,
        endUtc: Long,
        category: String,
        colorHex: String,
        repeatInterval: String? = null,
        checklistItems: List<ChecklistItem> = emptyList(),
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "Scheduled Task" }
            val safeEnd = endUtc.coerceAtLeast(startUtc + 15 * 60_000L)
            val now = System.currentTimeMillis()

            if (!noteId.isNullOrBlank()) {
                val existingNote = withContext(Dispatchers.IO) { repository.getNoteByIdDirect(noteId) }
                if (existingNote != null && !existingNote.isLocked) {
                    repository.saveOrUpdateNote(
                        existingNote.copy(
                            title = cleanTitle,
                            content = description.trim(),
                            colorHex = colorHex,
                            reminderTimestamp = startUtc,
                            canvasDataJson = SmartNoteUtils.encodeCalendarMeta(startUtc, safeEnd, category),
                            checklistJson = if (checklistItems.isNotEmpty()) {
                                SmartNoteUtils.encodeChecklistJson(checklistItems)
                            } else {
                                existingNote.checklistJson
                            },
                            updatedAtUtc = now
                        )
                    )
                }
            }

            if (!reminderId.isNullOrBlank()) {
                val existingRem = withContext(Dispatchers.IO) { repository.getReminderById(reminderId) }
                if (existingRem != null) {
                    val updatedRem = existingRem.copy(
                        scheduledTimeUtc = startUtc,
                        repeatInterval = repeatInterval
                    )
                    repository.updateReminder(updatedRem)
                    if (!updatedRem.isCompleted) {
                        ReminderNotificationManager.scheduleReminder(
                            context = getApplication(),
                            reminder = updatedRem,
                            noteTitle = cleanTitle
                        )
                    }
                }
            }

            if (!taskId.isNullOrBlank()) {
                val existingTask = withContext(Dispatchers.IO) { repository.getTaskByIdDirect(taskId) }
                if (existingTask != null) {
                    val durationMins = (((safeEnd - startUtc) / 60_000L).toInt()).coerceIn(15, 480)
                    repository.saveTask(
                        existingTask.copy(
                            title = cleanTitle,
                            description = description.trim(),
                            dueAtUtc = startUtc,
                            scheduledStartUtc = startUtc,
                            scheduledEndUtc = safeEnd,
                            estimatedMinutes = durationMins,
                            recurrenceRule = TaskRecurrenceRule.fromName(repeatInterval).name,
                            subtasksJson = if (checklistItems.isNotEmpty()) {
                                SmartNoteUtils.encodeChecklistJson(checklistItems)
                            } else {
                                existingTask.subtasksJson
                            },
                            category = category,
                            colorHex = colorHex,
                            updatedAtUtc = now
                        )
                    )
                }
            }

            onComplete()
        }
    }

    fun toggleNoteChecklistItem(note: NoteEntity, itemIndex: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                editorSaveMutex.withLock {
                    val latest = repository.getNoteByIdDirect(note.id) ?: note
                    if (latest.isLocked) return@withLock
                    val items = SmartNoteUtils.parseChecklistJson(latest.checklistJson).toMutableList()
                    if (itemIndex !in items.indices) return@withLock
                    items[itemIndex] = items[itemIndex].copy(isDone = !items[itemIndex].isDone)
                    val rev = System.currentTimeMillis()
                    val updated = latest.copy(
                        checklistJson = SmartNoteUtils.encodeChecklistJson(items),
                        updatedAtUtc = rev
                    )
                    val res = repository.saveOrUpdateNote(updated)
                    if (res.isSuccess) {
                        latestSavedRevisions[updated.id] = rev
                    }
                }
            }
        }
    }

    // ==========================================
    // PHASE 2: KANBAN, TASKS, DEPENDENCIES, HABITS, POMODORO & DASHBOARD WIDGETS
    // ==========================================
    fun saveKanbanColumn(
        column: KanbanColumnEntity,
        onResult: (Result<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.saveKanbanColumn(column)
            onResult(res)
        }
    }

    fun reorderKanbanColumn(columnId: String, delta: Int) {
        viewModelScope.launch {
            val current = repository.getAllKanbanColumnsDirect().sortedBy { it.position }
            val idx = current.indexOfFirst { it.id == columnId }
            if (idx == -1) return@launch
            val newIdx = (idx + delta).coerceIn(0, current.lastIndex)
            if (newIdx == idx) return@launch
            val mutable = current.map { it.id }.toMutableList()
            val moved = mutable.removeAt(idx)
            mutable.add(newIdx, moved)
            repository.reorderKanbanColumns(mutable)
        }
    }

    fun deleteKanbanColumn(columnId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteKanbanColumn(columnId)
            onResult(res)
        }
    }

    fun saveTask(task: TaskEntity, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveTask(task)
            onResult(res)
        }
    }

    fun saveTaskWithReminder(
        task: TaskEntity,
        selectedDependencyIds: Set<String> = emptySet(),
        reminderEnabled: Boolean = false,
        reminderTimeUtc: Long? = null,
        onResult: (Result<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            var finalTask = task
            val existingRemId = task.linkedReminderId?.takeIf { it.isNotBlank() }

            if (reminderEnabled) {
                val triggerUtc = reminderTimeUtc ?: task.scheduledStartUtc ?: task.dueAtUtc
                    ?: (System.currentTimeMillis() + 3600_000L)
                val repeatStr = task.recurrenceRule.takeIf { it != TaskRecurrenceRule.NONE.name }
                if (existingRemId != null) {
                    val existingRem = withContext(Dispatchers.IO) { repository.getReminderById(existingRemId) }
                    if (existingRem != null) {
                        val updatedRem = existingRem.copy(
                            scheduledTimeUtc = triggerUtc,
                            repeatInterval = repeatStr,
                            isCompleted = task.isCompleted
                        )
                        repository.updateReminder(updatedRem)
                        if (!task.isCompleted) {
                            ReminderNotificationManager.scheduleReminder(
                                context = getApplication(),
                                reminder = updatedRem,
                                noteTitle = task.title.ifBlank { "Task Reminder" }
                            )
                        }
                    } else {
                        val targetNoteId = task.linkedNoteId ?: ""
                        val newRem = ReminderEntity(
                            id = existingRemId,
                            noteId = targetNoteId,
                            scheduledTimeUtc = triggerUtc,
                            repeatInterval = repeatStr,
                            isCompleted = task.isCompleted
                        )
                        repository.saveReminder(newRem)
                        if (!task.isCompleted) {
                            ReminderNotificationManager.scheduleReminder(
                                context = getApplication(),
                                reminder = newRem,
                                noteTitle = task.title.ifBlank { "Task Reminder" }
                            )
                        }
                    }
                } else {
                    val newRemId = UUID.randomUUID().toString()
                    val newRem = ReminderEntity(
                        id = newRemId,
                        noteId = task.linkedNoteId ?: "",
                        scheduledTimeUtc = triggerUtc,
                        repeatInterval = repeatStr,
                        isCompleted = task.isCompleted
                    )
                    repository.saveReminder(newRem)
                    if (!task.isCompleted) {
                        ReminderNotificationManager.scheduleReminder(
                            context = getApplication(),
                            reminder = newRem,
                            noteTitle = task.title.ifBlank { "Task Reminder" }
                        )
                    }
                    finalTask = finalTask.copy(
                        linkedReminderId = newRemId,
                        dueAtUtc = finalTask.dueAtUtc ?: triggerUtc
                    )
                }
            } else if (existingRemId != null) {
                repository.deleteReminder(existingRemId)
                ReminderNotificationManager.cancelReminder(getApplication(), existingRemId)
                finalTask = finalTask.copy(linkedReminderId = null)
            }

            val res = repository.saveTask(finalTask)
            val savedId = res.getOrNull()
            if (savedId != null) {
                val existingDeps = withContext(Dispatchers.IO) {
                    repository.getAllTaskDependenciesDirect()
                        .filter { it.taskId == savedId }
                        .map { it.dependsOnTaskId }
                        .toSet()
                }
                (existingDeps - selectedDependencyIds).forEach { removed ->
                    repository.removeTaskDependency(savedId, removed)
                }
                (selectedDependencyIds - existingDeps).forEach { added ->
                    repository.addTaskDependency(savedId, added)
                }
            }
            onResult(res)
        }
    }

    fun moveTaskToColumn(
        taskId: String,
        targetColumnId: String,
        targetOrder: Int? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.moveTaskToColumn(taskId, targetColumnId, targetOrder)
            onResult(res)
        }
    }

    fun reorderTaskInColumn(taskId: String, delta: Int) {
        viewModelScope.launch {
            repository.reorderTaskInColumn(taskId, delta)
        }
    }

    fun toggleTaskCompleted(taskId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.toggleTaskCompleted(taskId)
            onResult(res)
        }
    }

    fun toggleTaskSubtask(taskId: String, index: Int) {
        viewModelScope.launch {
            val current = withContext(Dispatchers.IO) { repository.getTaskByIdDirect(taskId) } ?: return@launch
            val items = SmartNoteUtils.parseChecklistJson(current.subtasksJson).toMutableList()
            if (index !in items.indices) return@launch
            items[index] = items[index].copy(isDone = !items[index].isDone)
            repository.saveTask(current.copy(subtasksJson = SmartNoteUtils.encodeChecklistJson(items)))
        }
    }

    fun deleteTask(taskId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { repository.getTaskByIdDirect(taskId) }
            val remId = existing?.linkedReminderId
            if (!remId.isNullOrBlank()) {
                repository.deleteReminder(remId)
                ReminderNotificationManager.cancelReminder(getApplication(), remId)
            }
            val res = repository.deleteTask(taskId)
            onResult(res)
        }
    }

    fun addTaskDependency(
        taskId: String,
        dependsOnTaskId: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.addTaskDependency(taskId, dependsOnTaskId)
            onResult(res)
        }
    }

    fun removeTaskDependency(taskId: String, dependsOnTaskId: String) {
        viewModelScope.launch {
            repository.removeTaskDependency(taskId, dependsOnTaskId)
        }
    }

    fun assignTaskTimeBlock(
        taskId: String,
        startUtc: Long?,
        endUtc: Long?,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.assignTaskTimeBlock(taskId, startUtc, endUtc)
            onResult(res)
        }
    }

    fun saveHabit(habit: HabitEntity, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveHabit(habit)
            onResult(res)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun checkInHabit(
        habitId: String,
        dateKey: String = ProductivityEngine.localDateKeyFromMillis(System.currentTimeMillis()),
        increment: Int = 1
    ) {
        viewModelScope.launch {
            repository.checkInHabit(habitId, dateKey, increment)
        }
    }

    fun setHabitCompletedForDate(
        habitId: String,
        dateKey: String,
        completed: Boolean
    ) {
        viewModelScope.launch {
            repository.setHabitCompletedForDate(habitId, dateKey, completed)
        }
    }

    private fun persistPomodoroState(state: PomodoroRuntimeState) {
        pomodoroState.value = state
        pomodoroRemainingSeconds.value = ProductivityEngine.computeRemainingSeconds(state)
        pomodoroPrefs.edit {
            putString(PomodoroCompletionWorker.KEY_POMODORO_STATE, ProductivityEngine.serializePomodoroState(state))
        }
    }

    private fun startPomodoroTicker() {
        pomodoroTickerJob?.cancel()
        pomodoroTickerJob = viewModelScope.launch {
            while (true) {
                val cur = pomodoroState.value
                if (!cur.isActiveOrPaused || cur.status != FocusSessionStatus.RUNNING) break
                val remaining = ProductivityEngine.computeRemainingSeconds(cur)
                pomodoroRemainingSeconds.value = remaining
                if (remaining <= 0) {
                    finalizeCompletedPomodoroSession(cur, cur.plannedDurationSec)
                    break
                }
                delay(1000L)
            }
        }
    }

    private suspend fun finalizeCompletedPomodoroSession(state: PomodoroRuntimeState, focusedSec: Int) {
        val now = System.currentTimeMillis()
        val tzId = TimeZone.getDefault().id
        val sessionId = state.activeSessionId ?: UUID.randomUUID().toString()
        val existing = withContext(Dispatchers.IO) {
            repository.getAllFocusSessionsDirect().firstOrNull { it.id == sessionId }
        }
        val entity = existing?.copy(
            actualFocusedSec = focusedSec.coerceAtLeast(1),
            status = FocusSessionStatus.COMPLETED.name,
            endedAtUtc = now
        ) ?: FocusSessionEntity(
            id = sessionId,
            sessionType = state.sessionType.name,
            linkedTaskId = state.linkedTaskId,
            linkedNoteId = state.linkedNoteId,
            plannedDurationSec = state.plannedDurationSec,
            actualFocusedSec = focusedSec.coerceAtLeast(1),
            status = FocusSessionStatus.COMPLETED.name,
            startedAtUtc = (now - focusedSec * 1000L).coerceAtLeast(0L),
            endedAtUtc = now,
            localDateKey = ProductivityEngine.localDateKeyFromMillis(now, tzId),
            timeZoneId = tzId
        )
        repository.saveFocusSession(entity)
        ProductivityWorkScheduler.cancelPomodoroCompletion(getApplication())

        val nextCycles = if (state.sessionType == FocusSessionType.WORK) {
            state.completedWorkCycles + 1
        } else {
            state.completedWorkCycles
        }
        val cleared = state.copy(
            activeSessionId = null,
            status = FocusSessionStatus.COMPLETED,
            isActiveOrPaused = false,
            accumulatedElapsedMs = 0L,
            lastResumedWallClockMs = 0L,
            completedWorkCycles = nextCycles
        )
        persistPomodoroState(cleared)
    }

    fun restorePomodoroFromPrefs() {
        val raw = pomodoroPrefs.getString(PomodoroCompletionWorker.KEY_POMODORO_STATE, null)
        val restored = ProductivityEngine.deserializePomodoroState(raw)
        pomodoroTickerJob?.cancel()
        if (!restored.isActiveOrPaused) {
            pomodoroState.value = restored
            pomodoroRemainingSeconds.value = restored.plannedDurationSec
            return
        }
        val now = System.currentTimeMillis()
        val remaining = ProductivityEngine.computeRemainingSeconds(restored, now)
        if (restored.status == FocusSessionStatus.RUNNING && remaining <= 0) {
            viewModelScope.launch {
                finalizeCompletedPomodoroSession(restored, restored.plannedDurationSec)
            }
        } else if (restored.status == FocusSessionStatus.RUNNING) {
            pomodoroState.value = restored
            pomodoroRemainingSeconds.value = remaining
            startPomodoroTicker()
            ProductivityWorkScheduler.schedulePomodoroCompletion(
                context = getApplication(),
                delayMs = remaining * 1000L,
                sessionTitle = "${restored.sessionType.label} Complete"
            )
        } else {
            pomodoroState.value = restored
            pomodoroRemainingSeconds.value = remaining
        }
    }

    fun startPomodoro(
        type: FocusSessionType = pomodoroState.value.sessionType,
        durationSec: Int = type.defaultDurationSec,
        linkedTaskId: String? = pomodoroState.value.linkedTaskId,
        linkedNoteId: String? = pomodoroState.value.linkedNoteId
    ) {
        viewModelScope.launch {
            val safeDuration = durationSec.coerceAtLeast(60)
            val now = System.currentTimeMillis()
            val tzId = TimeZone.getDefault().id
            val sessionId = UUID.randomUUID().toString()
            val safeTaskId = linkedTaskId?.takeIf { it.isNotBlank() }
            val safeNoteId = linkedNoteId?.takeIf { it.isNotBlank() }

            repository.saveFocusSession(
                FocusSessionEntity(
                    id = sessionId,
                    sessionType = type.name,
                    linkedTaskId = safeTaskId,
                    linkedNoteId = safeNoteId,
                    plannedDurationSec = safeDuration,
                    actualFocusedSec = 0,
                    status = FocusSessionStatus.RUNNING.name,
                    startedAtUtc = now,
                    endedAtUtc = null,
                    localDateKey = ProductivityEngine.localDateKeyFromMillis(now, tzId),
                    timeZoneId = tzId
                )
            )

            val newState = PomodoroRuntimeState(
                activeSessionId = sessionId,
                sessionType = type,
                status = FocusSessionStatus.RUNNING,
                isActiveOrPaused = true,
                plannedDurationSec = safeDuration,
                accumulatedElapsedMs = 0L,
                lastResumedWallClockMs = now,
                linkedTaskId = safeTaskId,
                linkedNoteId = safeNoteId,
                completedWorkCycles = pomodoroState.value.completedWorkCycles
            )
            persistPomodoroState(newState)
            startPomodoroTicker()
            ProductivityWorkScheduler.schedulePomodoroCompletion(
                context = getApplication(),
                delayMs = safeDuration * 1000L,
                sessionTitle = "${type.label} Complete"
            )
        }
    }

    fun selectPomodoroPreset(type: FocusSessionType, durationSec: Int = type.defaultDurationSec) {
        val current = pomodoroState.value
        if (current.isActiveOrPaused) return
        val updated = current.copy(
            sessionType = type,
            plannedDurationSec = durationSec.coerceAtLeast(60),
            accumulatedElapsedMs = 0L,
            lastResumedWallClockMs = 0L
        )
        persistPomodoroState(updated)
    }

    fun setPomodoroLinks(taskId: String?, noteId: String?) {
        val updated = pomodoroState.value.copy(
            linkedTaskId = taskId?.takeIf { it.isNotBlank() },
            linkedNoteId = noteId?.takeIf { it.isNotBlank() }
        )
        persistPomodoroState(updated)
    }

    fun pausePomodoro() {
        val cur = pomodoroState.value
        if (!cur.isActiveOrPaused || cur.status != FocusSessionStatus.RUNNING) return
        pomodoroTickerJob?.cancel()
        val now = System.currentTimeMillis()
        val segment = if (cur.lastResumedWallClockMs > 0L) (now - cur.lastResumedWallClockMs).coerceAtLeast(0L) else 0L
        val updated = cur.copy(
            status = FocusSessionStatus.PAUSED,
            accumulatedElapsedMs = cur.accumulatedElapsedMs + segment,
            lastResumedWallClockMs = 0L
        )
        persistPomodoroState(updated)
        ProductivityWorkScheduler.cancelPomodoroCompletion(getApplication())

        val sid = cur.activeSessionId
        if (!sid.isNullOrBlank()) {
            viewModelScope.launch {
                val existing = withContext(Dispatchers.IO) {
                    repository.getAllFocusSessionsDirect().firstOrNull { it.id == sid }
                }
                if (existing != null) {
                    repository.saveFocusSession(
                        existing.copy(
                            actualFocusedSec = (updated.accumulatedElapsedMs / 1000L).toInt(),
                            status = FocusSessionStatus.PAUSED.name
                        )
                    )
                }
            }
        }
    }

    fun resumePomodoro() {
        val cur = pomodoroState.value
        if (!cur.isActiveOrPaused || cur.status != FocusSessionStatus.PAUSED) return
        val now = System.currentTimeMillis()
        val remaining = ProductivityEngine.computeRemainingSeconds(cur, now)
        if (remaining <= 0) {
            viewModelScope.launch {
                finalizeCompletedPomodoroSession(cur, cur.plannedDurationSec)
            }
            return
        }
        val updated = cur.copy(
            status = FocusSessionStatus.RUNNING,
            lastResumedWallClockMs = now
        )
        persistPomodoroState(updated)
        startPomodoroTicker()
        ProductivityWorkScheduler.schedulePomodoroCompletion(
            context = getApplication(),
            delayMs = remaining * 1000L,
            sessionTitle = "${cur.sessionType.label} Complete"
        )
    }

    fun completePomodoroEarly() {
        val cur = pomodoroState.value
        if (!cur.isActiveOrPaused) return
        pomodoroTickerJob?.cancel()
        val elapsed = ProductivityEngine.computeElapsedSeconds(cur).coerceAtLeast(1)
        viewModelScope.launch {
            finalizeCompletedPomodoroSession(cur, elapsed)
        }
    }

    fun resetPomodoro(recordCancelled: Boolean = true) {
        val cur = pomodoroState.value
        pomodoroTickerJob?.cancel()
        ProductivityWorkScheduler.cancelPomodoroCompletion(getApplication())
        val elapsed = ProductivityEngine.computeElapsedSeconds(cur)
        val sid = cur.activeSessionId
        if (cur.isActiveOrPaused && !sid.isNullOrBlank()) {
            viewModelScope.launch {
                val existing = withContext(Dispatchers.IO) {
                    repository.getAllFocusSessionsDirect().firstOrNull { it.id == sid }
                }
                if (existing != null && recordCancelled) {
                    repository.saveFocusSession(
                        existing.copy(
                            actualFocusedSec = elapsed,
                            status = FocusSessionStatus.CANCELLED.name,
                            endedAtUtc = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        persistPomodoroState(
            cur.copy(
                activeSessionId = null,
                status = FocusSessionStatus.CANCELLED,
                isActiveOrPaused = false,
                accumulatedElapsedMs = 0L,
                lastResumedWallClockMs = 0L
            )
        )
    }

    fun refreshDashboardWidgetsFromPrefs() {
        dashboardWidgets.value = ProductivityEngine.parseDashboardWidgets(
            settingsPrefs.getString("dashboard_widgets_config", null)
        )
    }

    private fun saveDashboardWidgets(items: List<DashboardWidgetItem>) {
        val normalized = items.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
        dashboardWidgets.value = normalized
        settingsPrefs.edit {
            putString("dashboard_widgets_config", ProductivityEngine.encodeDashboardWidgets(normalized))
        }
    }

    fun toggleDashboardWidget(type: DashboardWidgetType) {
        val updated = dashboardWidgets.value.map { item ->
            if (item.type == type) item.copy(enabled = !item.enabled) else item
        }
        saveDashboardWidgets(updated)
    }

    fun moveDashboardWidget(type: DashboardWidgetType, delta: Int) {
        val mutable = dashboardWidgets.value.toMutableList()
        val idx = mutable.indexOfFirst { it.type == type }
        if (idx == -1) return
        val targetIdx = (idx + delta).coerceIn(0, mutable.lastIndex)
        if (targetIdx == idx) return
        val moved = mutable.removeAt(idx)
        mutable.add(targetIdx, moved)
        saveDashboardWidgets(mutable)
    }

    fun resetDashboardWidgets() {
        saveDashboardWidgets(ProductivityEngine.defaultDashboardWidgets())
    }

    private fun loadGoals(): List<GoalItem> {
        val raw = settingsPrefs.getString("productivity_goals_v1", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                GoalItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Goal"),
                    targetValue = obj.optInt("targetValue", 1).coerceAtLeast(1),
                    currentValue = obj.optInt("currentValue", 0).coerceAtLeast(0),
                    unit = obj.optString("unit", ""),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistGoals(list: List<GoalItem>) {
        goals.value = list
        try {
            val arr = org.json.JSONArray()
            list.forEach { g ->
                arr.put(
                    org.json.JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("targetValue", g.targetValue)
                        put("currentValue", g.currentValue)
                        put("unit", g.unit)
                        put("isCompleted", g.isCompleted)
                    }
                )
            }
            settingsPrefs.edit { putString("productivity_goals_v1", arr.toString()) }
        } catch (e: Exception) {
            Log.w("NoteViewModel", "Failed to persist goals", e)
        }
    }

    fun addGoal(
        title: String, 
        targetValue: Int = 1, 
        unit: String = "",
        deadlineUtc: Long? = null,
        milestones: List<String> = emptyList()
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val newGoal = GoalItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            targetValue = targetValue.coerceAtLeast(1),
            currentValue = 0,
            unit = unit.trim(),
            isCompleted = false,
            deadlineUtc = deadlineUtc,
            milestones = milestones
        )
        persistGoals(listOf(newGoal) + goals.value)
    }

    fun incrementGoal(goalId: String, delta: Int = 1) {
        val updated = goals.value.map { g ->
            if (g.id == goalId) {
                val next = (g.currentValue + delta).coerceIn(0, g.targetValue)
                g.copy(currentValue = next, isCompleted = next >= g.targetValue)
            } else g
        }
        persistGoals(updated)
    }

    fun toggleGoalCompleted(goalId: String) {
        val updated = goals.value.map { g ->
            if (g.id == goalId) {
                val nextDone = !g.isCompleted
                g.copy(
                    isCompleted = nextDone,
                    currentValue = if (nextDone) g.targetValue else (g.currentValue.coerceAtMost((g.targetValue - 1).coerceAtLeast(0)))
                )
            } else g
        }
        persistGoals(updated)
    }

    fun deleteGoal(goalId: String) {
        persistGoals(goals.value.filterNot { it.id == goalId })
    }

    init {
        AppDatabase.seedInitialData(database)
        ProductivityWorkScheduler.ensurePeriodicMaintenanceScheduled(getApplication())
        restorePomodoroFromPrefs()
    }

    companion object {
        const val KEY_QUICK_EDIT_TOOLBAR_CONFIG = "quick_edit_toolbar_config_v1"
        const val KEY_TRASH_RETENTION_DAYS = "trash_retention_days"
    }
}

data class StickyLayout(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float = DEFAULT_WIDTH,
    val height: Float = DEFAULT_HEIGHT,
    val rotationDeg: Float = 0f,
    val zIndex: Int = 0
) {
    companion object {
        const val MIN_WIDTH = 120f
        const val MAX_WIDTH = 560f
        const val MIN_HEIGHT = 110f
        const val MAX_HEIGHT = 600f
        const val DEFAULT_WIDTH = 170f
        const val DEFAULT_HEIGHT = 180f

        fun clampWidth(width: Float): Float = width.coerceIn(MIN_WIDTH, MAX_WIDTH)
        fun clampHeight(height: Float): Float = height.coerceIn(MIN_HEIGHT, MAX_HEIGHT)

        fun normalizeAngle(angleDeg: Float): Float {
            if (!angleDeg.isFinite()) return 0f
            var norm = ((angleDeg % 360f) + 360f) % 360f
            if (norm > 180f) norm -= 360f
            return if (kotlin.math.abs(norm) < 0.05f) 0f else norm
        }
    }
}
