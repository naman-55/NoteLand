package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AttachmentEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.security.BiometricAuthHelper
import com.example.ui.components.DictationTarget
import com.example.ui.components.EditorMarkdownMode
import com.example.ui.components.HandwritingOverlayLayer
import com.example.ui.components.HandwritingOverlayToolbar
import com.example.ui.components.HierarchicalFolderPickerSheet
import com.example.ui.components.INK_ADAPTIVE_ARGB
import com.example.ui.components.MarkdownModeSegmentedBar
import com.example.ui.components.MarkdownVisualTransformation
import com.example.ui.components.NotelandEraserIcon
import com.example.ui.components.NoteStyleStudioSheet
import com.example.ui.components.PinDialog
import com.example.ui.components.ReminderDialog
import com.example.ui.components.RenderedMarkdownPreview
import com.example.ui.components.RichFormatBar
import com.example.ui.components.SmartUtilitiesSheet
import com.example.ui.components.VoiceClipsSection
import com.example.ui.components.VoiceDictationBar
import com.example.ui.components.VoiceDictationController
import com.example.ui.components.VoicePlayerController
import com.example.ui.components.VoiceRecorderController
import com.example.ui.components.VoiceRecorderDockCard
import com.example.ui.components.applyEditorialFormatting
import com.example.ui.components.buildRecognizerIntent
import com.example.ui.components.drawNoteTexture
import com.example.ui.components.inkStrokeContainsPoint
import com.example.ui.components.mergeDictatedSegment
import com.example.ui.components.parseRichMarkdownInline
import com.example.ui.editor.components.NoteAttachmentsSection

import com.example.ui.editor.components.RichDocumentEditorPane
import com.example.ui.editor.components.RichEditorToolbar
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.engine.RichEditorHistoryManager
import com.example.ui.editor.engine.RichEditorHistorySnapshot
import com.example.ui.editor.engine.RichSpanEngine
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCellSelection
import com.example.ui.editor.model.UnderlineStyle
import com.example.ui.editor.serialization.RichNoteDocumentCodec
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalAppTextScaled
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.theme.resolveNoteBackgroundBrush
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.ChecklistItem
import com.example.util.FolderHierarchyUtils
import com.example.util.InkStroke
import com.example.util.KnowledgeBrainEngine
import com.example.util.NoteBlockType
import com.example.util.NoteInkTool
import com.example.util.SmartNoteUtils
import com.example.util.VoiceRecordingClip
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MicPermissionPurpose {
    DICTATION,
    AUDIO_RECORDING
}

private enum class EditorHistoryKind {
    TEXT,
    INK
}

sealed class EditorSaveState {
    object Idle : EditorSaveState()
    object Saving : EditorSaveState()
    object Saved : EditorSaveState()
    data class Error(val message: String) : EditorSaveState()
}

private val TerracottaAccent: Color
    @Composable
    get() = if (isAppInDarkTheme) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)

private val SagePillAccent: Color
    @Composable
    get() = if (isAppInDarkTheme) Color(0xFF5EEAD4) else Color(0xFF5D7F62)

private val SlateBluePillAccent: Color
    @Composable
    get() = if (isAppInDarkTheme) Color(0xFF60A5FA) else Color(0xFF5C82A6)

private fun parseImageUris(raw: String): List<String> =
    raw.split("|").map { it.trim() }.filter { it.isNotBlank() }

private fun encodeImageUris(uris: List<String>): String =
    uris.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString("|")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(
    noteId: String,
    initialNoteType: String,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit,
    initialFolderId: String = "",
    onNavigateToNote: (String, String) -> Unit = { _, _ -> },
    onOpenPdfAttachment: (AttachmentEntity) -> Unit = {},
    onOpenPdfAtPage: (AttachmentEntity, Int) -> Unit = { att, _ -> onOpenPdfAttachment(att) }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val allLibraryAttachments by viewModel.attachments.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsStateWithLifecycle()
    val quickEditConfig by viewModel.quickEditConfig.collectAsStateWithLifecycle()
    val themeModeKey by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentAppThemeMode = remember(themeModeKey) { AppThemeMode.fromKey(themeModeKey) }
    val rawEditorFontSizeSp by viewModel.editorFontSizeSp.collectAsStateWithLifecycle()
    val editorFontSizeSp = if (LocalAppTextScaled.current) 16.5f else rawEditorFontSizeSp
    val canPromptBiometrics = remember(context, biometricsEnabled) {
        biometricsEnabled && BiometricAuthHelper.checkAvailability(context) !is BiometricAuthHelper.Availability.Unavailable
    }

    // State preserved across configuration changes and process recreations via rememberSaveable
    var title by rememberSaveable { mutableStateOf("") }
    var contentState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val content = contentState.text
    var selectedColorHex by rememberSaveable { mutableStateOf("") }
    var selectedTextureKey by rememberSaveable { mutableStateOf("") }
    var selectedBgImagePath by rememberSaveable { mutableStateOf("") }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var isHidden by rememberSaveable { mutableStateOf(false) }
    var isArchived by rememberSaveable { mutableStateOf(false) }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(initialFolderId.takeIf { it.isNotBlank() }) }
    var selectedFolderName by rememberSaveable { mutableStateOf("") }
    var currentReminder by rememberSaveable { mutableStateOf<Long?>(null) }
    var noteType by rememberSaveable { mutableStateOf(initialNoteType) }
    var imageUri by rememberSaveable { mutableStateOf("") }
    val tagsList = remember { mutableStateListOf<String>() }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }
    var newChecklistText by rememberSaveable { mutableStateOf("") }
    var savedTagsCsv by rememberSaveable(noteId) { mutableStateOf("") }
    var savedChecklistJson by rememberSaveable(noteId) { mutableStateOf("") }
    var savedBlockOrderCsv by rememberSaveable(noteId) { mutableStateOf("") }
    var hasHydratedFromDb by rememberSaveable(noteId) { mutableStateOf(false) }

    // Undo / Redo history stacks (stores title to content string and unified history order)
    val undoStack = remember { mutableStateListOf<Pair<String, String>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, String>>() }
    val historyUndoKinds = remember { mutableStateListOf<EditorHistoryKind>() }
    val historyRedoKinds = remember { mutableStateListOf<EditorHistoryKind>() }

    // Stable Note ID: generated once and kept stable to prevent duplicate note creation
    var isNewDraft by rememberSaveable(noteId) {
        mutableStateOf(noteId.isBlank() || noteId == "0")
    }
    var currentNoteId by rememberSaveable(noteId) {
        mutableStateOf(if (noteId.isNotBlank() && noteId != "0") noteId else UUID.randomUUID().toString())
    }
    var currentRevision by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var saveState by remember { mutableStateOf<EditorSaveState>(EditorSaveState.Idle) }
    var hasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var isDiscardedOrDeleted by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    val isExistingNote = noteId.isNotBlank() && noteId != "0"
    var selectedTab by rememberSaveable(noteId) {
        mutableIntStateOf(0)
    }
    var markdownEditorMode by rememberSaveable(noteId) {
        mutableStateOf(EditorMarkdownMode.RAW)
    }
    var useRichEditorMode by rememberSaveable(noteId) { mutableStateOf(true) }
    var richDocument by remember { mutableStateOf(RichNoteDocument.empty()) }
    var richDocRevision by remember { mutableIntStateOf(0) }
    var activeRichBlockId by remember { mutableStateOf<String?>(null) }
    val richBlockSelections = remember { mutableStateMapOf<String, TextRange>() }
    var pendingRichFocusBlockId by remember { mutableStateOf<String?>(null) }
    var pendingRichFocusOffset by remember { mutableStateOf<Int?>(null) }
    var selectedTableCell by remember { mutableStateOf<TableCellSelection?>(null) }
    var pendingTypingStyle by remember { mutableStateOf<InlineFormatStyle?>(null) }
    val richHistory = remember(noteId) { RichEditorHistoryManager(maxEntries = 80) }
    var richHistoryVersion by remember { mutableIntStateOf(0) }
    var pendingDownloadAttachment by remember { mutableStateOf<AttachmentEntity?>(null) }
    val noteAttachments by remember(currentNoteId) {
        viewModel.getAttachmentsForNote(currentNoteId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var showColorSheet by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showNoteTypeMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by rememberSaveable { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTitleInput by rememberSaveable { mutableStateOf("") }
    var linkUrlInput by rememberSaveable { mutableStateOf("") }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSmartUtilitiesSheet by remember { mutableStateOf(false) }
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var expandedVersionId by remember { mutableStateOf<String?>(null) }
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }
    var originalNote by remember { mutableStateOf<NoteEntity?>(null) }
    var blockOrder by remember { mutableStateOf(SmartNoteUtils.DEFAULT_NOTE_BLOCK_ORDER) }
    val coroutineScope = rememberCoroutineScope()
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val checklistFocusRequester = remember { FocusRequester() }
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    val checklistBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isTitleFocused by remember { mutableStateOf(false) }
    var isBodyFocused by remember { mutableStateOf(false) }
    var isChecklistFocused by remember { mutableStateOf(false) }
    var userInteractedWithField by remember { mutableStateOf(false) }
    var bodyTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val editScrollState = rememberScrollState()
    val viewScrollState = rememberScrollState()

    // Stylus / Free-Form Handwriting Overlay State
    val inkStrokes = remember { mutableStateListOf<InkStroke>() }
    val inkUndoStack = remember { mutableStateListOf<List<InkStroke>>() }
    val inkRedoStack = remember { mutableStateListOf<List<InkStroke>>() }
    var inkRevision by remember { mutableIntStateOf(0) }
    var isDrawingMode by rememberSaveable { mutableStateOf(false) }

    // Voice Recording Clips State
    val voiceClips = remember { mutableStateListOf<VoiceRecordingClip>() }
    var voiceClipsRevision by remember { mutableIntStateOf(0) }
    var renamingVoiceClip by remember { mutableStateOf<VoiceRecordingClip?>(null) }
    var renameClipInput by rememberSaveable { mutableStateOf("") }
    var isInkVisible by rememberSaveable { mutableStateOf(true) }
    var activeInkTool by remember { mutableStateOf(NoteInkTool.PRESSURE_PEN) }
    var activeInkColorArgb by rememberSaveable { mutableIntStateOf(INK_ADAPTIVE_ARGB) }
    var activeInkWidthDp by rememberSaveable { mutableFloatStateOf(2.8f) }
    var stylusOnlyMode by rememberSaveable { mutableStateOf(false) }
    var stylusDetected by remember { mutableStateOf(false) }
    var preEraseInkSnapshot by remember { mutableStateOf<List<InkStroke>?>(null) }
    var didEraseInCurrentSweep by remember { mutableStateOf(false) }

    fun pushInkUndo(snapshot: List<InkStroke>) {
        if (inkUndoStack.size >= 40) inkUndoStack.removeAt(0)
        inkUndoStack.add(snapshot)
        inkRedoStack.clear()
        if (historyUndoKinds.size >= 80) historyUndoKinds.removeAt(0)
        historyUndoKinds.add(EditorHistoryKind.INK)
        historyRedoKinds.clear()
    }

    fun handleStrokeCompleted(stroke: InkStroke) {
        pushInkUndo(inkStrokes.toList())
        inkStrokes.add(stroke)
        inkRevision++
        currentRevision = System.currentTimeMillis()
        hasUnsavedChanges = true
        saveState = EditorSaveState.Saving
    }

    fun handleInkEraseAt(pointDp: androidx.compose.ui.geometry.Offset, radiusDp: Float, startSweep: Boolean) {
        if (startSweep) {
            preEraseInkSnapshot = inkStrokes.toList()
            didEraseInCurrentSweep = false
        }
        val toRemove = inkStrokes.filter { inkStrokeContainsPoint(it, pointDp, radiusDp) }
        if (toRemove.isNotEmpty()) {
            if (!didEraseInCurrentSweep) {
                val snap = preEraseInkSnapshot ?: inkStrokes.toList()
                pushInkUndo(snap)
                didEraseInCurrentSweep = true
            }
            inkStrokes.removeAll(toRemove)
            inkRevision++
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    fun handleCancelInkEraseSweep() {
        if (didEraseInCurrentSweep) {
            val snap = preEraseInkSnapshot
            if (snap != null) {
                if (inkUndoStack.isNotEmpty()) inkUndoStack.removeAt(inkUndoStack.size - 1)
                inkStrokes.clear()
                inkStrokes.addAll(snap)
                inkRevision++
            }
        }
        preEraseInkSnapshot = null
        didEraseInCurrentSweep = false
    }

    fun undoInkStroke() {
        if (inkUndoStack.isNotEmpty()) {
            val prev = inkUndoStack.removeAt(inkUndoStack.size - 1)
            inkRedoStack.add(inkStrokes.toList())
            inkStrokes.clear()
            inkStrokes.addAll(prev)
            inkRevision++
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    fun redoInkStroke() {
        if (inkRedoStack.isNotEmpty()) {
            val next = inkRedoStack.removeAt(inkRedoStack.size - 1)
            inkUndoStack.add(inkStrokes.toList())
            inkStrokes.clear()
            inkStrokes.addAll(next)
            inkRevision++
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    fun clearAllInkStrokes() {
        if (inkStrokes.isNotEmpty()) {
            pushInkUndo(inkStrokes.toList())
            inkStrokes.clear()
            inkRevision++
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    // Resolve pre-selected folder name when navigated from inside a folder
    LaunchedEffect(folders, initialFolderId) {
        if (isNewDraft && initialFolderId.isNotBlank() && selectedFolderName.isBlank()) {
            folders.firstOrNull { it.id == initialFolderId }?.let { folder ->
                selectedFolderId = folder.id
                selectedFolderName = folder.name
            }
        }
    }

    // Multi-photo picker launcher (zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val currentCount = parseImageUris(imageUri).size
            val availableSlots = (12 - currentCount).coerceAtLeast(0)
            uris.take(availableSlots).forEach { pickedUri ->
                viewModel.savePastedImageUri(
                    uri = pickedUri,
                    onError = { err ->
                        Toast.makeText(
                            context,
                            "Could not import photo: ${err.localizedMessage ?: "Unsupported format"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSaved = { savedLocalUri ->
                        val current = parseImageUris(imageUri)
                        val combined = (current + savedLocalUri).distinct().take(12)
                        imageUri = encodeImageUris(combined)
                        currentRevision = System.currentTimeMillis()
                        hasUnsavedChanges = true
                        saveState = EditorSaveState.Saving
                    }
                )
            }
        }
    }

    fun launchPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun attachSavedImageUri(uri: Uri, silentIfInvalid: Boolean = false) {
        viewModel.savePastedImageUri(
            uri = uri,
            onError = { err ->
                if (!silentIfInvalid) {
                    Toast.makeText(
                        context,
                        "Could not paste image: ${err.localizedMessage ?: "Unsupported format"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSaved = { savedFileUri ->
                val current = parseImageUris(imageUri)
                val combined = (current + savedFileUri).distinct().take(12)
                imageUri = encodeImageUris(combined)
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
                Toast.makeText(context, "Image pasted into note", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun pasteImageFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val imageUris = buildList {
            for (i in 0 until clip.itemCount) {
                val uri = clip.getItemAt(i).uri ?: continue
                val mime = context.contentResolver.getType(uri)
                if ((mime != null && mime.startsWith("image/")) ||
                    clip.description.hasMimeType("image/*")
                ) {
                    add(uri)
                }
            }
        }
        if (imageUris.isEmpty()) {
            Toast.makeText(
                context,
                "No image found in clipboard. Copy an image first or paste directly from keyboard.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        imageUris.forEach { attachSavedImageUri(it) }
    }

    val bodyReceiveContentListener = remember {
        ReceiveContentListener { transferableContent ->
            if (transferableContent.hasMediaType(MediaType.Image)) {
                transferableContent.consume { item ->
                    val uri = item.uri
                    val mime = uri?.let { context.contentResolver.getType(it) }
                    if (uri != null && (mime == null || mime.startsWith("image/"))) {
                        attachSavedImageUri(uri, silentIfInvalid = true)
                        true
                    } else {
                        false
                    }
                }
            } else {
                transferableContent
            }
        }
    }

    val bgPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveBackgroundImageFromUri(
                uri = uri,
                onError = { err ->
                    Toast.makeText(
                        context,
                        "Could not load background photo: ${err.localizedMessage ?: "Error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSaved = { savedPath ->
                    selectedBgImagePath = savedPath
                    currentRevision = System.currentTimeMillis()
                    hasUnsavedChanges = true
                    saveState = EditorSaveState.Saving
                }
            )
        }
    }

    fun launchBackgroundPhotoPicker() {
        bgPhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun resolveDisplayName(uri: Uri, fallback: String): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && col >= 0) {
                    cursor.getString(col)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    var attachFileHelper by remember { mutableStateOf<(Uri, String) -> Unit>({ _, _ -> }) }

    val pdfAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveDisplayName(uri, "Document_${System.currentTimeMillis()}.pdf")
            attachFileHelper(uri, fileName)
        }
    }

    val documentAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = resolveDisplayName(uri, "Attachment_${System.currentTimeMillis()}.bin")
            attachFileHelper(uri, fileName)
        }
    }

    val downloadAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri: Uri? ->
        val att = pendingDownloadAttachment
        pendingDownloadAttachment = null
        if (targetUri != null && att != null) {
            viewModel.exportAttachmentToUri(att, targetUri) { res ->
                res.onSuccess {
                    Toast.makeText(context, "Saved ${att.fileName}", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Download failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openNoteAttachment(attachment: AttachmentEntity) {
        val ext = attachment.fileExtension.lowercase()
        val isPdf = attachment.mimeType.contains("pdf", ignoreCase = true) || ext == "pdf"
        val isImage = attachment.mimeType.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp", "gif")
        val file = viewModel.attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
        when {
            isPdf -> onOpenPdfAttachment(attachment)
            isImage && file.exists() -> {
                val existing = parseImageUris(imageUri)
                val idx = existing.indexOfFirst { it == file.absolutePath }
                if (idx >= 0) {
                    previewImageIndex = idx
                } else {
                    try {
                        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, attachment.mimeType.ifBlank { "image/*" })
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, attachment.fileName))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to open image preview", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            file.exists() -> {
                try {
                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, attachment.mimeType.ifBlank { "*/*" })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, attachment.fileName))
                } catch (_: Exception) {
                    Toast.makeText(context, "No app available to open ${attachment.fileName}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Toast.makeText(context, "Attachment file is missing", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(noteId) {
        if (noteId.isNotBlank() && noteId != "0") {
            currentNoteId = noteId
            isNewDraft = false
            val note = viewModel.getNoteByIdDirect(noteId)
                ?: allNotes.firstOrNull { it.id == noteId }
                ?: viewModel.archivedNotes.value.firstOrNull { it.id == noteId }
                ?: viewModel.lockedNotes.value.firstOrNull { it.id == noteId }

            if (hasHydratedFromDb) {
                // Recomposition after configuration change or process recreation: preserve active draft state
                note?.let {
                    originalNote = it
                    if (inkStrokes.isEmpty()) {
                        inkStrokes.addAll(SmartNoteUtils.parseHandwritingStrokes(it.canvasDataJson))
                    }
                    if (voiceClips.isEmpty()) {
                        voiceClips.addAll(SmartNoteUtils.parseVoiceRecordings(it.canvasDataJson))
                    }
                }
                if (tagsList.isEmpty() && savedTagsCsv.isNotBlank()) {
                    savedTagsCsv.split(",").filter { t -> t.isNotBlank() }.forEach { tagsList.add(it) }
                }
                if (checklistItems.isEmpty() && savedChecklistJson.isNotBlank() && savedChecklistJson != "[]") {
                    checklistItems.addAll(SmartNoteUtils.parseChecklistJson(savedChecklistJson))
                }
                if (savedBlockOrderCsv.isNotBlank()) {
                    val restoredOrder = savedBlockOrderCsv.split(",")
                        .mapNotNull { name -> runCatching { NoteBlockType.valueOf(name) }.getOrNull() }
                    if (restoredOrder.size == NoteBlockType.entries.size) {
                        blockOrder = restoredOrder
                    }
                }
                return@LaunchedEffect
            }

            note?.let {
                originalNote = it
                title = if (it.isLocked) viewModel.securityManager.decrypt(it.title) else it.title
                val decryptedBody = if (it.isLocked) viewModel.securityManager.decrypt(it.content) else it.content
                contentState = TextFieldValue(text = decryptedBody, selection = TextRange(decryptedBody.length))
                selectedColorHex = it.colorHex
                selectedTextureKey = SmartNoteUtils.parseNoteTexture(it.canvasDataJson)
                selectedBgImagePath = SmartNoteUtils.parseNoteBackgroundImage(it.canvasDataJson)
                blockOrder = SmartNoteUtils.parseBlockOrder(it.canvasDataJson)
                isPinned = it.isPinned
                isFavorite = it.isFavorite
                isLocked = it.isLocked
                isHidden = it.isHidden
                isArchived = it.isArchived
                selectedFolderId = it.folderId
                selectedFolderName = it.folderName
                currentReminder = it.reminderTimestamp
                noteType = it.noteType
                imageUri = it.imageUri

                tagsList.clear()
                it.tagsCsv.split(",").filter { t -> t.isNotBlank() }.forEach { t -> tagsList.add(t) }
                savedTagsCsv = tagsList.joinToString(",")

                checklistItems.clear()
                val rawChecklist = if (it.isLocked && it.checklistJson.isNotBlank()) viewModel.securityManager.decrypt(it.checklistJson) else it.checklistJson
                checklistItems.addAll(SmartNoteUtils.parseChecklistJson(rawChecklist))
                savedChecklistJson = SmartNoteUtils.encodeChecklistJson(checklistItems)
                savedBlockOrderCsv = blockOrder.joinToString(",") { b -> b.name }

                inkStrokes.clear()
                inkUndoStack.clear()
                inkRedoStack.clear()
                undoStack.clear()
                redoStack.clear()
                historyUndoKinds.clear()
                historyRedoKinds.clear()
                inkStrokes.addAll(SmartNoteUtils.parseHandwritingStrokes(it.canvasDataJson))
                voiceClips.clear()
                voiceClips.addAll(SmartNoteUtils.parseVoiceRecordings(it.canvasDataJson))
                val rawRichJson = SmartNoteUtils.parseRichDocumentJson(it.canvasDataJson)
                val decryptedRichJson = if (it.isLocked && rawRichJson.isNotBlank() && !rawRichJson.trimStart().startsWith("{")) {
                    viewModel.securityManager.decrypt(rawRichJson)
                } else {
                    rawRichJson
                }
                val loadedRichDoc = RichNoteDocumentCodec.loadForNote(decryptedRichJson, decryptedBody)
                richDocument = loadedRichDoc
                activeRichBlockId = loadedRichDoc.blocks.firstOrNull { b -> b is RichBlock.TextBlock }?.id
                richBlockSelections.clear()
                loadedRichDoc.blocks.filterIsInstance<RichBlock.TextBlock>().forEach { tb ->
                    richBlockSelections[tb.id] = TextRange(tb.text.length)
                }
                richHistory.clear()
                richHistoryVersion++
                pendingTypingStyle = null
                selectedTableCell = null
                saveState = EditorSaveState.Saved
                hasUnsavedChanges = false
                hasHydratedFromDb = true
            }
        } else {
            if (hasHydratedFromDb) {
                if (tagsList.isEmpty() && savedTagsCsv.isNotBlank()) {
                    savedTagsCsv.split(",").filter { t -> t.isNotBlank() }.forEach { tagsList.add(it) }
                }
                if (checklistItems.isEmpty() && savedChecklistJson.isNotBlank() && savedChecklistJson != "[]") {
                    checklistItems.addAll(SmartNoteUtils.parseChecklistJson(savedChecklistJson))
                }
                if (savedBlockOrderCsv.isNotBlank()) {
                    val restoredOrder = savedBlockOrderCsv.split(",")
                        .mapNotNull { name -> runCatching { NoteBlockType.valueOf(name) }.getOrNull() }
                    if (restoredOrder.size == NoteBlockType.entries.size) {
                        blockOrder = restoredOrder
                    }
                }
                return@LaunchedEffect
            }
            blockOrder = SmartNoteUtils.DEFAULT_NOTE_BLOCK_ORDER
            savedBlockOrderCsv = blockOrder.joinToString(",") { it.name }
            hasHydratedFromDb = true
            delay(150)
            if (!userInteractedWithField && !isBodyFocused && !isChecklistFocused && !isTitleFocused) {
                try {
                    titleFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    fun isEmptyDraft(): Boolean {
        return isNewDraft &&
            title.isBlank() &&
            contentState.text.isBlank() &&
            richDocument.isEffectivelyEmpty() &&
            checklistItems.isEmpty() &&
            imageUri.isBlank() &&
            noteAttachments.isEmpty() &&
            selectedBgImagePath.isBlank() &&
            inkStrokes.isEmpty() &&
            voiceClips.isEmpty() &&
            tagsList.isEmpty()
    }

    fun buildCurrentNote(effectiveId: String): NoteEntity {
        val finalTitle = if (isLocked) viewModel.securityManager.encrypt(title.trim()) else title.trim()
        val finalContent = if (isLocked) viewModel.securityManager.encrypt(contentState.text) else contentState.text
        val rawChecklistJson = SmartNoteUtils.encodeChecklistJson(checklistItems)
        val finalChecklist = if (isLocked && rawChecklistJson.isNotBlank() && rawChecklistJson != "[]") {
            viewModel.securityManager.encrypt(rawChecklistJson)
        } else {
            rawChecklistJson
        }
        val styledJson = SmartNoteUtils.withNoteStyleMeta(
            existingJson = originalNote?.canvasDataJson.orEmpty(),
            textureKey = selectedTextureKey,
            backgroundImagePath = selectedBgImagePath
        )
        val mergedInkJson = SmartNoteUtils.withHandwritingStrokes(
            existingJson = styledJson,
            strokes = inkStrokes.toList()
        )
        val mergedVoiceJson = SmartNoteUtils.withVoiceRecordings(
            existingJson = mergedInkJson,
            clips = voiceClips.toList()
        )
        val mergedOrderJson = SmartNoteUtils.withBlockOrder(
            existingJson = mergedVoiceJson,
            order = blockOrder
        )
        val encodedRichDoc = RichNoteDocumentCodec.encode(
            document = richDocument,
            syncedPlainContent = contentState.text
        )
        val finalRichDocPayload = if (isLocked) {
            viewModel.securityManager.encrypt(encodedRichDoc)
        } else {
            encodedRichDoc
        }
        val mergedCanvasJson = SmartNoteUtils.withRichDocumentJson(
            existingJson = mergedOrderJson,
            richDocumentJson = finalRichDocPayload
        )
        return NoteEntity(
            id = effectiveId,
            title = finalTitle,
            content = finalContent,
            noteType = noteType,
            colorHex = selectedColorHex,
            folderId = selectedFolderId,
            folderName = selectedFolderName,
            tagsCsv = tagsList.joinToString(","),
            isPinned = isPinned,
            isArchived = isArchived,
            isLocked = isLocked,
            isHidden = isHidden,
            isFavorite = isFavorite,
            isDeleted = originalNote?.isDeleted ?: false,
            deletedAtUtc = originalNote?.deletedAtUtc,
            reminderTimestamp = currentReminder,
            imageUri = imageUri,
            canvasDataJson = mergedCanvasJson,
            checklistJson = finalChecklist,
            createdAtUtc = originalNote?.createdAtUtc ?: System.currentTimeMillis(),
            updatedAtUtc = System.currentTimeMillis()
        )
    }

    attachFileHelper = { uri, fileName ->
        val snapshot = buildCurrentNote(currentNoteId)
        viewModel.attachDocumentToNote(
            noteSnapshot = snapshot,
            uri = uri,
            fileName = fileName,
            onError = { err ->
                Toast.makeText(
                    context,
                    "Could not attach file: ${err.localizedMessage ?: "Error"}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSaved = { savedAtt ->
                isNewDraft = false
                if (originalNote == null) originalNote = snapshot
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                Toast.makeText(context, "Attached ${savedAtt.fileName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun saveCurrentNote(onDone: () -> Unit = {}) {
        if (isDiscardedOrDeleted) {
            onDone()
            return
        }
        if (isEmptyDraft()) {
            hasUnsavedChanges = false
            saveState = EditorSaveState.Idle
            onDone()
            return
        }

        val snapshotRevision = currentRevision
        val noteToSave = buildCurrentNote(currentNoteId)

        saveState = EditorSaveState.Saving
        viewModel.saveNote(noteToSave, revision = snapshotRevision) { result ->
            result.onSuccess { savedId ->
                currentNoteId = savedId
                isNewDraft = false
                originalNote = noteToSave.copy(id = savedId)
                if (currentRevision == snapshotRevision) {
                    hasUnsavedChanges = false
                    saveState = EditorSaveState.Saved
                }
                onDone()
            }.onFailure { error ->
                saveState = EditorSaveState.Error(error.localizedMessage ?: "Failed to save note")
                onDone()
            }
        }
    }

    fun flushPendingSave() {
        if (!isDiscardedOrDeleted && hasUnsavedChanges && !isEmptyDraft()) {
            val snapshotRevision = currentRevision
            val noteToSave = buildCurrentNote(currentNoteId)
            val result = viewModel.flushSaveDirect(noteToSave, snapshotRevision)
            result.onSuccess { savedId ->
                currentNoteId = savedId
                isNewDraft = false
                originalNote = noteToSave.copy(id = savedId)
                if (currentRevision == snapshotRevision) {
                    hasUnsavedChanges = false
                    saveState = EditorSaveState.Saved
                }
            }.onFailure { error ->
                saveState = EditorSaveState.Error(error.localizedMessage ?: "Save failed")
            }
        }
    }

    fun moveCurrentNoteToTrash() {
        isDiscardedOrDeleted = true
        hasUnsavedChanges = false
        val existing = originalNote
        if (existing != null) {
            viewModel.softDeleteNote(existing)
        } else if (!isEmptyDraft()) {
            val trashedDraft = buildCurrentNote(currentNoteId).copy(
                isDeleted = true,
                deletedAtUtc = System.currentTimeMillis()
            )
            viewModel.saveNote(trashedDraft, revision = System.currentTimeMillis()) { result ->
                result.onSuccess { savedId ->
                    viewModel.softDeleteNote(trashedDraft.copy(id = savedId))
                }
            }
        }
        Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    val editorContentSignature = listOf(
        title,
        contentState.text,
        imageUri,
        checklistItems.joinToString("|") { "${it.id}:${it.text}:${it.isDone}" },
        tagsList.joinToString(","),
        selectedFolderId ?: "",
        selectedFolderName,
        selectedColorHex,
        selectedTextureKey,
        selectedBgImagePath,
        inkRevision.toString(),
        voiceClipsRevision.toString(),
        richDocRevision.toString(),
        isPinned.toString(),
        isFavorite.toString(),
        isArchived.toString(),
        isLocked.toString(),
        currentReminder?.toString() ?: "",
        noteType,
        blockOrder.joinToString(",") { it.name }
    ).joinToString(";;")

    val voiceRecorderController = remember(context) {
        VoiceRecorderController(context, viewModel.attachmentStorageManager)
    }
    val voicePlayerController = remember(context) {
        VoicePlayerController(viewModel.attachmentStorageManager)
    }

    fun finishAndSaveActiveRecording() {
        if (voiceRecorderController.isRecording) {
            val savedClip = voiceRecorderController.stopAndSaveRecording()
            if (savedClip != null) {
                voiceClips.add(savedClip)
                voiceClipsRevision++
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
            }
        }
    }

    DisposableEffect(voiceRecorderController, voicePlayerController) {
        onDispose {
            voiceRecorderController.destroy()
            voicePlayerController.destroy()
        }
    }

    LaunchedEffect(editorContentSignature) {
        savedTagsCsv = tagsList.joinToString(",")
        savedChecklistJson = SmartNoteUtils.encodeChecklistJson(checklistItems)
        savedBlockOrderCsv = blockOrder.joinToString(",") { it.name }
        if (!isDiscardedOrDeleted && hasUnsavedChanges) {
            saveState = EditorSaveState.Saving
            delay(1000)
            saveCurrentNote()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                finishAndSaveActiveRecording()
                voicePlayerController.stopPlayback()
                flushPendingSave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            finishAndSaveActiveRecording()
            voicePlayerController.stopPlayback()
            flushPendingSave()
        }
    }

    fun applyContentChange(newVal: TextFieldValue) {
        if (newVal.text != contentState.text) {
            undoStack.add(title to contentState.text)
            redoStack.clear()
            historyUndoKinds.add(EditorHistoryKind.TEXT)
            historyRedoKinds.clear()
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
        contentState = newVal
    }

    fun captureRichSnapshot(): RichEditorHistorySnapshot {
        return RichEditorHistorySnapshot(
            title = title,
            document = richDocument,
            activeBlockId = activeRichBlockId,
            blockSelections = richBlockSelections.toMap(),
            selectedTableCell = selectedTableCell
        )
    }

    fun restoreRichSnapshot(snapshot: RichEditorHistorySnapshot) {
        val safe = richHistory.sanitizeSnapshot(snapshot)
        title = safe.title
        val plainMd = RichNoteDocumentCodec.toPlainMarkdown(safe.document)
        val syncedDoc = safe.document.copy(
            sourceContentHash = RichNoteDocumentCodec.computeContentHash(plainMd)
        ).ensureNonEmpty()
        richDocument = syncedDoc
        contentState = TextFieldValue(text = plainMd, selection = TextRange(plainMd.length))
        richBlockSelections.clear()
        richBlockSelections.putAll(safe.blockSelections)
        activeRichBlockId = safe.activeBlockId
        selectedTableCell = safe.selectedTableCell
        pendingTypingStyle = null
        val focusId = safe.activeBlockId
        if (focusId != null) {
            val sel = safe.blockSelections[focusId]
            pendingRichFocusBlockId = focusId
            pendingRichFocusOffset = sel?.end
        }
        richDocRevision++
        richHistoryVersion++
        currentRevision = System.currentTimeMillis()
        hasUnsavedChanges = true
        saveState = EditorSaveState.Saving
    }

    fun commitRichDocument(
        updatedDoc: RichNoteDocument,
        pushUndo: Boolean = true,
        focusBlockId: String? = null,
        focusOffset: Int? = null
    ) {
        if (pushUndo) {
            richHistory.recordBeforeChange(captureRichSnapshot())
            richHistoryVersion++
            undoStack.add(title to contentState.text)
            redoStack.clear()
            historyUndoKinds.add(EditorHistoryKind.TEXT)
            historyRedoKinds.clear()
        }
        val plainMd = RichNoteDocumentCodec.toPlainMarkdown(updatedDoc)
        val syncedDoc = updatedDoc.copy(
            sourceContentHash = RichNoteDocumentCodec.computeContentHash(plainMd)
        ).ensureNonEmpty()
        richDocument = syncedDoc
        contentState = TextFieldValue(text = plainMd, selection = TextRange(plainMd.length))
        if (focusBlockId != null) {
            activeRichBlockId = focusBlockId
            val targetBlock = syncedDoc.blocks.firstOrNull { it.id == focusBlockId } as? RichBlock.TextBlock
            val safeOffset = (focusOffset ?: targetBlock?.text?.length ?: 0).coerceIn(0, targetBlock?.text?.length ?: 0)
            richBlockSelections[focusBlockId] = TextRange(safeOffset)
            pendingRichFocusBlockId = focusBlockId
            pendingRichFocusOffset = safeOffset
        }
        richDocRevision++
        currentRevision = System.currentTimeMillis()
        hasUnsavedChanges = true
        saveState = EditorSaveState.Saving
    }

    fun resolveActiveRichTextBlock(): RichBlock.TextBlock {
        val current = richDocument.blocks.firstOrNull { it.id == activeRichBlockId } as? RichBlock.TextBlock
        if (current != null) return current
        val first = richDocument.blocks.firstOrNull { it is RichBlock.TextBlock } as? RichBlock.TextBlock
        if (first != null) {
            activeRichBlockId = first.id
            return first
        }
        val fallback = RichBlock.TextBlock()
        commitRichDocument(richDocument.copy(blocks = richDocument.blocks + fallback), pushUndo = false)
        activeRichBlockId = fallback.id
        return fallback
    }

    fun currentActiveInlineStyle(): InlineFormatStyle {
        val block = richDocument.blocks.firstOrNull { it.id == activeRichBlockId } as? RichBlock.TextBlock
            ?: richDocument.blocks.firstOrNull { it is RichBlock.TextBlock } as? RichBlock.TextBlock
            ?: return InlineFormatStyle()
        val sel = richBlockSelections[block.id] ?: TextRange(block.text.length)
        if (!sel.collapsed) {
            return RichSpanEngine.resolveActiveStyleAt(block.spans, block.text.length, sel.min, sel.max)
        }
        return pendingTypingStyle ?: RichSpanEngine.resolveActiveStyleAt(block.spans, block.text.length, sel.min, sel.max)
    }

    fun applyRichInlineAction(
        updateTypingStyle: (InlineFormatStyle) -> InlineFormatStyle,
        updateRangeSpans: (spans: List<com.example.ui.editor.model.InlineSpan>, len: Int, start: Int, end: Int) -> List<com.example.ui.editor.model.InlineSpan>
    ) {
        val block = resolveActiveRichTextBlock()
        val sel = richBlockSelections[block.id] ?: TextRange(block.text.length)
        val selStart = sel.min.coerceIn(0, block.text.length)
        val selEnd = sel.max.coerceIn(0, block.text.length)
        if (selStart != selEnd) {
            val newSpans = updateRangeSpans(block.spans, block.text.length, selStart, selEnd)
            val updatedDoc = RichDocumentReducer.updateTextBlock(richDocument, block.id) {
                it.copy(spans = newSpans)
            }
            commitRichDocument(updatedDoc)
            // Keep exact selection intact so users can stack multiple styles seamlessly
            richBlockSelections[block.id] = TextRange(selStart, selEnd)
            pendingTypingStyle = null
        } else {
            val baseStyle = pendingTypingStyle ?: RichSpanEngine.resolveActiveStyleAt(block.spans, block.text.length, selStart, selEnd)
            pendingTypingStyle = updateTypingStyle(baseStyle).normalized()
        }
    }

    fun insertFormatting(prefix: String, suffix: String) {
        if (useRichEditorMode) {
            val block = resolveActiveRichTextBlock()
            val sel = richBlockSelections[block.id] ?: TextRange(block.text.length)
            val start = sel.min.coerceIn(0, block.text.length)
            val end = sel.max.coerceIn(0, block.text.length)
            val insertion = "$prefix$suffix"
            val newText = block.text.replaceRange(start, end, insertion)
            val newSpans = RichSpanEngine.applyTextDiff(block.text, newText, block.spans, pendingTypingStyle)
            val updated = RichDocumentReducer.updateTextBlock(richDocument, block.id) {
                it.copy(text = newText, spans = newSpans)
            }
            commitRichDocument(updated, focusBlockId = block.id, focusOffset = start + prefix.length)
            return
        }
        if (markdownEditorMode == EditorMarkdownMode.PREVIEW) {
            markdownEditorMode = EditorMarkdownMode.SPLIT
        }
        val updated = applyEditorialFormatting(contentState, prefix, suffix)
        undoStack.add(title to contentState.text)
        redoStack.clear()
        historyUndoKinds.add(EditorHistoryKind.TEXT)
        historyRedoKinds.clear()
        contentState = updated
        currentRevision = System.currentTimeMillis()
        hasUnsavedChanges = true
        saveState = EditorSaveState.Saving
        try {
            bodyFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun applyUndo() {
        if (useRichEditorMode && richHistory.canUndo) {
            val prev = richHistory.undo(captureRichSnapshot())
            if (prev != null) {
                if (undoStack.isNotEmpty()) {
                    undoStack.removeAt(undoStack.size - 1)
                    redoStack.add(title to contentState.text)
                }
                restoreRichSnapshot(prev)
                return
            }
        }
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(title to contentState.text)
            title = previous.first
            contentState = TextFieldValue(
                text = previous.second,
                selection = TextRange(previous.second.length)
            )
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    fun applyRedo() {
        if (useRichEditorMode && richHistory.canRedo) {
            val next = richHistory.redo(captureRichSnapshot())
            if (next != null) {
                if (redoStack.isNotEmpty()) {
                    redoStack.removeAt(redoStack.size - 1)
                    undoStack.add(title to contentState.text)
                }
                restoreRichSnapshot(next)
                return
            }
        }
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(title to contentState.text)
            title = next.first
            contentState = TextFieldValue(
                text = next.second,
                selection = TextRange(next.second.length)
            )
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        }
    }

    fun performUnifiedUndo() {
        if (isDrawingMode) {
            if (inkUndoStack.isNotEmpty()) {
                val inkIdx = historyUndoKinds.lastIndexOf(EditorHistoryKind.INK)
                if (inkIdx >= 0) {
                    historyUndoKinds.removeAt(inkIdx)
                    historyRedoKinds.add(EditorHistoryKind.INK)
                }
                undoInkStroke()
            }
            return
        }
        val canTextUndo = richHistory.canUndo || undoStack.isNotEmpty()
        when (historyUndoKinds.lastOrNull()) {
            EditorHistoryKind.INK -> {
                if (inkUndoStack.isNotEmpty()) {
                    historyUndoKinds.removeAt(historyUndoKinds.lastIndex)
                    historyRedoKinds.add(EditorHistoryKind.INK)
                    undoInkStroke()
                } else if (canTextUndo) {
                    applyUndo()
                }
            }
            EditorHistoryKind.TEXT -> {
                if (canTextUndo) {
                    historyUndoKinds.removeAt(historyUndoKinds.lastIndex)
                    historyRedoKinds.add(EditorHistoryKind.TEXT)
                    applyUndo()
                } else if (inkUndoStack.isNotEmpty()) {
                    undoInkStroke()
                }
            }
            null -> {
                when {
                    canTextUndo -> applyUndo()
                    inkUndoStack.isNotEmpty() -> undoInkStroke()
                }
            }
        }
    }

    fun performUnifiedRedo() {
        if (isDrawingMode) {
            if (inkRedoStack.isNotEmpty()) {
                val inkIdx = historyRedoKinds.lastIndexOf(EditorHistoryKind.INK)
                if (inkIdx >= 0) {
                    historyRedoKinds.removeAt(inkIdx)
                    historyUndoKinds.add(EditorHistoryKind.INK)
                }
                redoInkStroke()
            }
            return
        }
        val canTextRedo = richHistory.canRedo || redoStack.isNotEmpty()
        when (historyRedoKinds.lastOrNull()) {
            EditorHistoryKind.INK -> {
                if (inkRedoStack.isNotEmpty()) {
                    historyRedoKinds.removeAt(historyRedoKinds.lastIndex)
                    historyUndoKinds.add(EditorHistoryKind.INK)
                    redoInkStroke()
                } else if (canTextRedo) {
                    applyRedo()
                }
            }
            EditorHistoryKind.TEXT -> {
                if (canTextRedo) {
                    historyRedoKinds.removeAt(historyRedoKinds.lastIndex)
                    historyUndoKinds.add(EditorHistoryKind.TEXT)
                    applyRedo()
                } else if (inkRedoStack.isNotEmpty()) {
                    redoInkStroke()
                }
            }
            null -> {
                when {
                    canTextRedo -> applyRedo()
                    inkRedoStack.isNotEmpty() -> redoInkStroke()
                }
            }
        }
    }

    // Voice-to-Text Microphone Dictation State & Permission Flow
    val latestDictationInsert by rememberUpdatedState<(String, DictationTarget) -> Unit> { spoken, targetField ->
        if (targetField == DictationTarget.TITLE) {
            if (useRichEditorMode) {
                richHistory.recordBeforeChange(captureRichSnapshot())
                richHistoryVersion++
            }
            undoStack.add(title to contentState.text)
            redoStack.clear()
            historyUndoKinds.add(EditorHistoryKind.TEXT)
            historyRedoKinds.clear()
            title = mergeDictatedSegment(
                TextFieldValue(title, TextRange(title.length)),
                spoken
            ).text
            currentRevision = System.currentTimeMillis()
            hasUnsavedChanges = true
            saveState = EditorSaveState.Saving
        } else if (useRichEditorMode) {
            val block = resolveActiveRichTextBlock()
            val sel = richBlockSelections[block.id] ?: TextRange(block.text.length)
            val merged = mergeDictatedSegment(TextFieldValue(block.text, sel), spoken)
            val updatedSpans = RichSpanEngine.applyTextDiff(
                oldText = block.text,
                newText = merged.text,
                oldSpans = block.spans,
                typingStyle = pendingTypingStyle
            )
            val updatedDoc = RichDocumentReducer.updateTextBlock(richDocument, block.id) {
                it.copy(text = merged.text, spans = updatedSpans)
            }
            commitRichDocument(
                updatedDoc = updatedDoc,
                focusBlockId = block.id,
                focusOffset = merged.selection.end
            )
        } else {
            applyContentChange(mergeDictatedSegment(contentState, spoken))
        }
    }

    var pendingFallbackTarget by remember { mutableStateOf(DictationTarget.BODY) }
    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                latestDictationInsert(spoken, pendingFallbackTarget)
            }
        }
    }

    var pendingSpeechUnavailableMsg by remember { mutableStateOf<String?>(null) }

    val latestFallbackLaunch by rememberUpdatedState<(DictationTarget) -> Unit> { targetField ->
        pendingFallbackTarget = targetField
        val intent = buildRecognizerIntent(context)
        if (intent.resolveActivity(context.packageManager) != null) {
            speechFallbackLauncher.launch(intent)
        } else {
            pendingSpeechUnavailableMsg = "Speech recognition is unavailable on this device"
            Toast.makeText(
                context,
                "No speech recognition service available on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val dictationController = remember(context) {
        VoiceDictationController(
            context = context,
            onFinalTranscript = { spoken, targetField ->
                latestDictationInsert(spoken, targetField)
            },
            onFallbackIntentRequested = { targetField ->
                latestFallbackLaunch(targetField)
            }
        )
    }

    LaunchedEffect(pendingSpeechUnavailableMsg) {
        val msg = pendingSpeechUnavailableMsg
        if (!msg.isNullOrBlank()) {
            dictationController.reportError(msg)
            pendingSpeechUnavailableMsg = null
        }
    }

    // Ensure mic never auto-starts when opening or switching notes
    LaunchedEffect(noteId) {
        dictationController.stopListening()
    }

    DisposableEffect(lifecycleOwner, dictationController) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                dictationController.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            dictationController.destroy()
        }
    }

    LaunchedEffect(selectedTab, isDrawingMode) {
        if ((selectedTab != 0 || isDrawingMode) && dictationController.isListening) {
            dictationController.stopListening()
        }
    }

    var pendingMicPurpose by remember { mutableStateOf(MicPermissionPurpose.DICTATION) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isDrawingMode = false
            if (pendingMicPurpose == MicPermissionPurpose.AUDIO_RECORDING) {
                dictationController.stopListening()
                voicePlayerController.stopPlayback()
                voiceRecorderController.startRecording()
            } else {
                finishAndSaveActiveRecording()
                selectedTab = 0
                dictationController.startListening()
            }
        } else {
            if (pendingMicPurpose == MicPermissionPurpose.DICTATION) {
                dictationController.reportError("Microphone permission denied — allow mic access to dictate")
            }
            Toast.makeText(
                context,
                "Microphone permission is required for voice features",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun toggleVoiceDictation() {
        if (dictationController.isListening) {
            dictationController.stopListening()
            return
        }
        finishAndSaveActiveRecording()
        dictationController.clearError()
        selectedTab = 0
        isDrawingMode = false
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            dictationController.startListening()
        } else {
            pendingMicPurpose = MicPermissionPurpose.DICTATION
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun toggleVoiceRecording() {
        if (voiceRecorderController.isRecording) {
            finishAndSaveActiveRecording()
            return
        }
        dictationController.stopListening()
        voicePlayerController.stopPlayback()
        voiceRecorderController.clearError()
        isDrawingMode = false
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            voiceRecorderController.startRecording()
        } else {
            pendingMicPurpose = MicPermissionPurpose.AUDIO_RECORDING
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun shareVoiceClip(clip: VoiceRecordingClip) {
        val file = viewModel.attachmentStorageManager.resolveVoiceRecordingFile(clip.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Audio file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, clip.title.ifBlank { title.ifBlank { "Voice Note" } })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Voice Recording"))
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to share recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun performSafeNavigateBack() {
        if (isNavigatingBack) return
        isNavigatingBack = true
        finishAndSaveActiveRecording()
        dictationController.stopListening()
        if (hasUnsavedChanges && !isEmptyDraft()) {
            saveCurrentNote { onNavigateBack() }
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        if (voiceRecorderController.isRecording) {
            finishAndSaveActiveRecording()
        } else if (dictationController.isListening) {
            dictationController.stopListening()
        } else if (isDrawingMode) {
            isDrawingMode = false
        } else {
            performSafeNavigateBack()
        }
    }

    val isDark = isAppInDarkTheme
    val defaultBackdrop = editorialBackdropBrush(isDark)
    val defaultEditorBg = if (isDark) {
        MaterialTheme.colorScheme.background
    } else {
        Color(0xFFFAF9F6)
    }
    val editorBgBrush = if (selectedColorHex.isBlank()) {
        defaultBackdrop
    } else {
        resolveNoteBackgroundBrush(
            colorKey = selectedColorHex,
            isDark = isDark,
            fallbackColor = defaultEditorBg
        )
    }

    val attachedImages = remember(imageUri) { parseImageUris(imageUri) }
    val noteTypeDisplay = when (noteType) {
        NoteType.STICKY.name -> "Sticky Wall"
        NoteType.CANVAS.name -> "Board"
        NoteType.PDF_DOC.name -> "PDF"
        else -> "Notes"
    }

    val needsVaultReauth = isLocked && !isNewDraft && !isVaultUnlocked

    val noteBacklinks by remember(currentNoteId, isLocked) {
        if (!isLocked) viewModel.observeBacklinks(currentNoteId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val noteRelated by remember(currentNoteId, isLocked) {
        if (!isLocked) viewModel.observeRelatedNotes(currentNoteId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val noteVersions by remember(currentNoteId, isLocked) {
        if (!isLocked) viewModel.observeNoteVersions(currentNoteId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val handleWikiLinkNavigation: (String) -> Unit = { targetTitle ->
        if (!isLocked) {
            saveCurrentNote {
                viewModel.openOrCreateWikiLink(targetTitle) { targetId, targetType ->
                    onNavigateToNote(targetId, targetType)
                }
            }
        }
    }

    val activeRichBlockTextForWiki = if (useRichEditorMode) {
        (richDocument.blocks.firstOrNull { it.id == activeRichBlockId } as? RichBlock.TextBlock)?.text
    } else null
    val activeRichBlockSelForWiki = if (useRichEditorMode && activeRichBlockId != null) {
        richBlockSelections[activeRichBlockId]
    } else null

    val wikiAutocompleteMatch = remember(
        useRichEditorMode,
        activeRichBlockId,
        activeRichBlockTextForWiki,
        activeRichBlockSelForWiki,
        contentState,
        allNotes,
        currentNoteId,
        isLocked
    ) {
        if (isLocked) {
            null
        } else if (useRichEditorMode) {
            val blockText = activeRichBlockTextForWiki ?: return@remember null
            val sel = activeRichBlockSelForWiki ?: TextRange(blockText.length)
            if (!sel.collapsed) {
                null
            } else {
                val cursor = sel.end.coerceIn(0, blockText.length)
                val beforeCursor = blockText.substring(0, cursor)
                val openRel = beforeCursor.lastIndexOf("[[")
                val closeRel = beforeCursor.lastIndexOf("]]")
                if (openRel >= 0 && openRel > closeRel) {
                    val query = beforeCursor.substring(openRel + 2)
                    val suggestions = allNotes
                        .filter {
                            it.id != currentNoteId &&
                                KnowledgeBrainEngine.isNoteKnowledgeEligible(it) &&
                                it.title.isNotBlank() &&
                                (query.isBlank() || it.title.contains(query.trim(), ignoreCase = true))
                        }
                        .take(6)
                    Triple(openRel, cursor, suggestions)
                } else {
                    null
                }
            }
        } else if (!contentState.selection.collapsed) {
            null
        } else {
            val text = contentState.text
            val cursor = contentState.selection.end.coerceIn(0, text.length)
            val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
                .let { if (cursor == 0 || it == -1) 0 else it + 1 }
            val beforeCursor = text.substring(lineStart, cursor)
            val openRel = beforeCursor.lastIndexOf("[[")
            val closeRel = beforeCursor.lastIndexOf("]]")
            if (openRel >= 0 && openRel > closeRel) {
                val query = beforeCursor.substring(openRel + 2)
                val suggestions = allNotes
                    .filter {
                        it.id != currentNoteId &&
                            KnowledgeBrainEngine.isNoteKnowledgeEligible(it) &&
                            it.title.isNotBlank() &&
                            (query.isBlank() || it.title.contains(query.trim(), ignoreCase = true))
                    }
                    .take(6)
                Triple(lineStart + openRel, cursor, suggestions)
            } else {
                null
            }
        }
    }

    val onAccentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
    val resolvedAccentColor = TerracottaAccent
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val markdownTransformation = remember(
        editorFontSizeSp,
        resolvedAccentColor,
        isDark
    ) {
        MarkdownVisualTransformation(
            baseFontSizeSp = editorFontSizeSp,
            accentColor = resolvedAccentColor,
            markerColor = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.62f) else Color(0xFF64748B).copy(alpha = 0.58f),
            highlightBg = if (isDark) resolvedAccentColor.copy(alpha = 0.28f) else Color(0xFFFDE047).copy(alpha = 0.45f),
            codeBg = if (isDark) Color(0xFF162026) else Color(0xFFF1F5F9),
            linkColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        )
    }

    LaunchedEffect(isBodyFocused, imeBottomPx, contentState.selection) {
        if (selectedTab == 0 && isBodyFocused && contentState.selection.collapsed) {
            if (imeBottomPx > 0) delay(50)
            bodyTextLayout?.let { layout ->
                val offset = contentState.selection.end.coerceIn(0, layout.layoutInput.text.length)
                val cursorRect = layout.getCursorRect(offset)
                val extraBottomPx = with(density) { 44.dp.toPx() }
                val targetRect = Rect(
                    left = cursorRect.left,
                    top = (cursorRect.top - 12f).coerceAtLeast(0f),
                    right = cursorRect.right.coerceAtLeast(cursorRect.left + 4f),
                    bottom = cursorRect.bottom + extraBottomPx
                )
                try {
                    bodyBringIntoViewRequester.bringIntoView(targetRect)
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorBgBrush)
    ) {
        if (selectedBgImagePath.isNotBlank()) {
            AsyncImage(
                model = if (selectedBgImagePath.startsWith("/")) File(selectedBgImagePath) else selectedBgImagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isDark) Color(0xFF040708).copy(alpha = 0.62f)
                        else Color.White.copy(alpha = 0.66f)
                    )
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawNoteTexture(selectedTextureKey, isDark = isDark, subtle = false)
        )
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .fillMaxSize()
                .then(if (showPinDialog || needsVaultReauth) Modifier.blur(28.dp) else Modifier),
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                                .union(WindowInsets.ime)
                                .only(WindowInsetsSides.Bottom)
                        )
                ) {
                    when {
                        isDrawingMode -> {
                            HandwritingOverlayToolbar(
                                activeTool = activeInkTool,
                                activeColorArgb = activeInkColorArgb,
                                activeWidthDp = activeInkWidthDp,
                                stylusOnlyMode = stylusOnlyMode,
                                stylusDetected = stylusDetected,
                                hasStrokes = inkStrokes.isNotEmpty(),
                                onSelectTool = { tool ->
                                    activeInkTool = tool
                                    if (tool == NoteInkTool.HIGHLIGHTER && activeInkWidthDp < 8f) {
                                        activeInkWidthDp = 12f
                                    } else if (tool != NoteInkTool.HIGHLIGHTER && tool != NoteInkTool.ERASER && activeInkWidthDp >= 10f) {
                                        activeInkWidthDp = 2.8f
                                    }
                                },
                                onSelectColorArgb = { argb -> activeInkColorArgb = argb },
                                onSelectWidthDp = { wDp -> activeInkWidthDp = wDp },
                                onToggleStylusOnly = { stylusOnlyMode = !stylusOnlyMode },
                                onClearAll = { clearAllInkStrokes() },
                                onDone = { isDrawingMode = false },
                                applyNavigationBarPadding = false
                            )
                        }
                        selectedTab == 0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                VoiceRecorderDockCard(
                                    isRecording = voiceRecorderController.isRecording,
                                    isPaused = voiceRecorderController.isPaused,
                                    elapsedMs = voiceRecorderController.elapsedMs,
                                    currentLevel = voiceRecorderController.currentLevel,
                                    liveWaveform = voiceRecorderController.liveWaveform,
                                    errorMessage = voiceRecorderController.errorMessage,
                                    onTogglePauseResume = { voiceRecorderController.togglePauseResume() },
                                    onDiscard = { voiceRecorderController.cancelRecording() },
                                    onSave = { finishAndSaveActiveRecording() },
                                    onDismissError = { voiceRecorderController.clearError() },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 4.dp)
                                )
                                VoiceDictationBar(
                                    isListening = dictationController.isListening,
                                    isReady = dictationController.isReady,
                                    isProcessing = dictationController.isProcessing,
                                    isContinuous = dictationController.isContinuous,
                                    target = dictationController.target,
                                    partialTranscript = dictationController.partialTranscript,
                                    normalizedLevel = dictationController.normalizedLevel,
                                    errorMessage = dictationController.errorMessage,
                                    onToggleContinuous = { dictationController.toggleContinuous() },
                                    onToggleTarget = { dictationController.toggleTarget() },
                                    onRetry = {
                                        dictationController.clearError()
                                        toggleVoiceDictation()
                                    },
                                    onStop = {
                                        dictationController.stopListening()
                                        dictationController.clearError()
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 2.dp)
                                )
                                wikiAutocompleteMatch?.let { (replaceStart, replaceEnd, suggestions) ->
                                    if (suggestions.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White.copy(alpha = 0.96f),
                                            border = BorderStroke(0.9.dp, TerracottaAccent.copy(alpha = 0.45f)),
                                            shadowElevation = 3.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                                .testTag("wiki_link_autocomplete_bar")
                                        ) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                itemsIndexed(suggestions, key = { _, item -> item.id }) { _, suggestion ->
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = TerracottaAccent.copy(alpha = 0.14f),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                val insertion = "[[${suggestion.title.trim()}]] "
                                                                if (useRichEditorMode) {
                                                                    val targetBlock = richDocument.blocks
                                                                        .firstOrNull { it.id == activeRichBlockId } as? RichBlock.TextBlock
                                                                    if (targetBlock != null) {
                                                                        val safeStart = replaceStart.coerceIn(0, targetBlock.text.length)
                                                                        val safeEnd = replaceEnd.coerceIn(safeStart, targetBlock.text.length)
                                                                        val updatedText = targetBlock.text.replaceRange(safeStart, safeEnd, insertion)
                                                                        val updatedSpans = RichSpanEngine.applyTextDiff(
                                                                            oldText = targetBlock.text,
                                                                            newText = updatedText,
                                                                            oldSpans = targetBlock.spans,
                                                                            typingStyle = null
                                                                        )
                                                                        val updatedDoc = RichDocumentReducer.updateTextBlock(richDocument, targetBlock.id) {
                                                                            it.copy(text = updatedText, spans = updatedSpans)
                                                                        }
                                                                        commitRichDocument(
                                                                            updatedDoc = updatedDoc,
                                                                            focusBlockId = targetBlock.id,
                                                                            focusOffset = safeStart + insertion.length
                                                                        )
                                                                    }
                                                                } else {
                                                                    val updatedText = contentState.text.replaceRange(replaceStart, replaceEnd, insertion)
                                                                    applyContentChange(
                                                                        TextFieldValue(
                                                                            text = updatedText,
                                                                            selection = TextRange(replaceStart + insertion.length)
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                            .testTag("wiki_suggestion_${suggestion.id}")
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Link,
                                                                contentDescription = null,
                                                                tint = TerracottaAccent,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = suggestion.title,
                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                                color = TerracottaAccent
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (useRichEditorMode) {
                                    val activeTextBlock = richDocument.blocks.firstOrNull { it.id == activeRichBlockId } as? RichBlock.TextBlock
                                        ?: richDocument.blocks.firstOrNull { it is RichBlock.TextBlock } as? RichBlock.TextBlock
                                    RichEditorToolbar(
                                        plainTextForStats = contentState.text,
                                        activeStyle = currentActiveInlineStyle(),
                                        activeBlockKind = activeTextBlock?.kind ?: RichBlockKind.PARAGRAPH,
                                        activeAlignment = activeTextBlock?.alignment ?: RichTextAlignment.LEFT,
                                        activeIndentLevel = activeTextBlock?.indentLevel ?: 0,
                                        onToggleBold = {
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(bold = !it.bold) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleBold(s, l, a, b) }
                                            )
                                        },
                                        onToggleItalic = {
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(italic = !it.italic) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleItalic(s, l, a, b) }
                                            )
                                        },
                                        onToggleUnderlineStandard = {
                                            applyRichInlineAction(
                                                updateTypingStyle = {
                                                    it.copy(underline = if (it.underline == UnderlineStyle.STANDARD) UnderlineStyle.NONE else UnderlineStyle.STANDARD)
                                                },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleUnderline(s, l, a, b, UnderlineStyle.STANDARD) }
                                            )
                                        },
                                        onToggleUnderlineWavy = {
                                            applyRichInlineAction(
                                                updateTypingStyle = {
                                                    it.copy(underline = if (it.underline == UnderlineStyle.WAVY) UnderlineStyle.NONE else UnderlineStyle.WAVY)
                                                },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleUnderline(s, l, a, b, UnderlineStyle.WAVY) }
                                            )
                                        },
                                        onSelectUnderlineStyle = { style ->
                                            applyRichInlineAction(
                                                updateTypingStyle = {
                                                    it.copy(underline = if (it.underline == style) UnderlineStyle.NONE else style)
                                                },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleUnderline(s, l, a, b, style) }
                                            )
                                        },
                                        onToggleStrikethrough = {
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(strikethrough = !it.strikethrough) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleStrikethrough(s, l, a, b) }
                                            )
                                        },
                                        onToggleInlineCode = {
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(inlineCode = !it.inlineCode) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleInlineCode(s, l, a, b) }
                                            )
                                        },
                                        onClearFormatting = {
                                            applyRichInlineAction(
                                                updateTypingStyle = { InlineFormatStyle() },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.clearInlineFormatting(s, l, a, b) }
                                            )
                                        },
                                        onSelectTextColor = { hex ->
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(textColorHex = hex) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.setTextColor(s, l, a, b, hex) }
                                            )
                                        },
                                        onSelectTextBackgroundColor = { hex ->
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(backgroundColorHex = hex) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.setBackgroundColor(s, l, a, b, hex) }
                                            )
                                        },
                                        onSelectHighlightColor = { hex ->
                                            applyRichInlineAction(
                                                updateTypingStyle = { it.copy(highlightColorHex = hex) },
                                                updateRangeSpans = { s, l, a, b -> RichSpanEngine.setHighlightColor(s, l, a, b, hex) }
                                            )
                                        },
                                        onSetAlignment = { align ->
                                            val target = resolveActiveRichTextBlock()
                                            commitRichDocument(RichDocumentReducer.setAlignment(richDocument, target.id, align))
                                        },
                                        onIncreaseIndent = {
                                            val target = resolveActiveRichTextBlock()
                                            commitRichDocument(RichDocumentReducer.increaseIndent(richDocument, target.id))
                                        },
                                        onDecreaseIndent = {
                                            val target = resolveActiveRichTextBlock()
                                            commitRichDocument(RichDocumentReducer.decreaseIndent(richDocument, target.id))
                                        },
                                        onSetBlockKind = { kind ->
                                            val target = resolveActiveRichTextBlock()
                                            commitRichDocument(RichDocumentReducer.setBlockKind(richDocument, target.id, kind))
                                        },
                                        onInsertDivider = {
                                            val res = RichDocumentReducer.insertBlockAfter(
                                                document = richDocument,
                                                afterBlockId = activeRichBlockId,
                                                newBlock = RichBlock.DividerBlock(),
                                                ensureTrailingParagraph = true
                                            )
                                            commitRichDocument(res.document, focusBlockId = res.focusBlockId, focusOffset = res.focusCursorOffset)
                                        },
                                        onInsertTable = {
                                            val table = RichDocumentReducer.createDefaultTable(isSpreadsheet = false, rows = 3, cols = 3)
                                            val res = RichDocumentReducer.insertBlockAfter(
                                                document = richDocument,
                                                afterBlockId = activeRichBlockId,
                                                newBlock = table,
                                                ensureTrailingParagraph = true
                                            )
                                            selectedTableCell = TableCellSelection(table.id, 0, 0)
                                            commitRichDocument(res.document)
                                        },
                                        onInsertSpreadsheet = {
                                            val sheet = RichDocumentReducer.createDefaultTable(isSpreadsheet = true, rows = 4, cols = 3)
                                            val res = RichDocumentReducer.insertBlockAfter(
                                                document = richDocument,
                                                afterBlockId = activeRichBlockId,
                                                newBlock = sheet,
                                                ensureTrailingParagraph = true
                                            )
                                            selectedTableCell = TableCellSelection(sheet.id, 0, 0)
                                            commitRichDocument(res.document)
                                        },
                                        onOpenLinkDialog = { showLinkDialog = true },
                                        onAttachImage = { launchPhotoPicker() },
                                        onAttachPdf = { pdfAttachmentLauncher.launch(arrayOf("application/pdf")) },
                                        onAttachDocument = { documentAttachmentLauncher.launch(arrayOf("*/*")) },
                                        onPasteImage = { pasteImageFromClipboard() },
                                        selectedNoteColorHex = selectedColorHex,
                                        onSelectNoteColorHex = { hex ->
                                            selectedColorHex = hex
                                            currentRevision = System.currentTimeMillis()
                                            hasUnsavedChanges = true
                                        },
                                        onOpenNoteColorSheet = { showColorSheet = true },
                                        onOpenUtilities = { showSmartUtilitiesSheet = true },
                                        onOpenDrawing = {
                                            finishAndSaveActiveRecording()
                                            dictationController.stopListening()
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            if (activeInkTool == NoteInkTool.ERASER) {
                                                activeInkTool = NoteInkTool.PRESSURE_PEN
                                            }
                                            isInkVisible = true
                                            isDrawingMode = true
                                        },
                                        onOpenEraser = {
                                            finishAndSaveActiveRecording()
                                            dictationController.stopListening()
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            isInkVisible = true
                                            activeInkTool = NoteInkTool.ERASER
                                            isDrawingMode = true
                                        },
                                        onStartVoiceDictation = { toggleVoiceDictation() },
                                        isDictating = dictationController.isListening,
                                        onToggleVoiceRecording = { toggleVoiceRecording() },
                                        isRecordingAudio = voiceRecorderController.isRecording,
                                    
                                        currentAppThemeMode = currentAppThemeMode,
                                        onSelectAppThemeMode = { mode -> viewModel.setThemeMode(mode.key) },
                                    
                                        hasInkStrokes = inkStrokes.isNotEmpty(),
                                        applyNavigationBarPadding = false
                                    )
                                } else {
                                    RichFormatBar(
                                        content = contentState.text,
                                        selection = contentState.selection,
                                        onInsertMarkdown = { prefix, suffix -> insertFormatting(prefix, suffix) },
                                        onOpenLinkDialog = { showLinkDialog = true },
                                        onAddImage = { launchPhotoPicker() },
                                        onPasteImage = { pasteImageFromClipboard() },
                                        onOpenUtilities = { showSmartUtilitiesSheet = true },
                                        selectedColorHex = selectedColorHex,
                                        onSelectColorHex = { hex ->
                                            selectedColorHex = hex
                                            currentRevision = System.currentTimeMillis()
                                            hasUnsavedChanges = true
                                        },
                                        onOpenColorSheet = { showColorSheet = true },
                                        onOpenDrawing = {
                                            finishAndSaveActiveRecording()
                                            dictationController.stopListening()
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            if (activeInkTool == NoteInkTool.ERASER) {
                                                activeInkTool = NoteInkTool.PRESSURE_PEN
                                            }
                                            isInkVisible = true
                                            isDrawingMode = true
                                        },
                                        onOpenEraser = {
                                            finishAndSaveActiveRecording()
                                            dictationController.stopListening()
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            isInkVisible = true
                                            activeInkTool = NoteInkTool.ERASER
                                            isDrawingMode = true
                                        },
                                        onStartVoiceDictation = { toggleVoiceDictation() },
                                        isDictating = dictationController.isListening,
                                        onToggleVoiceRecording = { toggleVoiceRecording() },
                                        isRecordingAudio = voiceRecorderController.isRecording,
                                        currentAppThemeMode = currentAppThemeMode,
                                        onSelectAppThemeMode = { mode -> viewModel.setThemeMode(mode.key) },
                                        applyNavigationBarPadding = false
                                    )
                                }
                            }
                        }
                        voiceRecorderController.isRecording || !voiceRecorderController.errorMessage.isNullOrBlank() -> {
                            VoiceRecorderDockCard(
                                isRecording = voiceRecorderController.isRecording,
                                isPaused = voiceRecorderController.isPaused,
                                elapsedMs = voiceRecorderController.elapsedMs,
                                currentLevel = voiceRecorderController.currentLevel,
                                liveWaveform = voiceRecorderController.liveWaveform,
                                errorMessage = voiceRecorderController.errorMessage,
                                onTogglePauseResume = { voiceRecorderController.togglePauseResume() },
                                onDiscard = { voiceRecorderController.cancelRecording() },
                                onSave = { finishAndSaveActiveRecording() },
                                onDismissError = { voiceRecorderController.clearError() },
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Navigation, Save Status Pill, Crisp Save/Edit CTA, More Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { performSafeNavigateBack() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("note_editor_back_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            val (statusText, statusColor) = when (saveState) {
                                EditorSaveState.Saving -> "Saving..." to Color(0xFFF59E0B)
                                EditorSaveState.Saved -> "Saved" to Color(0xFF10B981)
                                is EditorSaveState.Error -> "Error" to Color(0xFFEF4444)
                                EditorSaveState.Idle -> if (hasUnsavedChanges) "Saving..." to Color(0xFFF59E0B) else "Saved" to Color(0xFF10B981)
                            }

                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.11f),
                                border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = saveState is EditorSaveState.Error) {
                                        saveCurrentNote()
                                    }
                                    .testTag("save_status_indicator")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    when {
                                        saveState == EditorSaveState.Saving || (saveState == EditorSaveState.Idle && hasUnsavedChanges) -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(11.dp),
                                                strokeWidth = 1.8.dp,
                                                color = statusColor
                                            )
                                        }
                                        saveState is EditorSaveState.Error -> {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Error",
                                                tint = statusColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Saved",
                                                tint = statusColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = statusColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.testTag("save_status_text")
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val _historyTick = richHistoryVersion
                            val canHeaderUndo = (_historyTick >= 0 && richHistory.canUndo) || undoStack.isNotEmpty() || inkUndoStack.isNotEmpty()
                            val canHeaderRedo = (_historyTick >= 0 && richHistory.canRedo) || redoStack.isNotEmpty() || inkRedoStack.isNotEmpty()

                            IconButton(
                                onClick = { performUnifiedUndo() },
                                enabled = canHeaderUndo,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("undo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canHeaderUndo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { performUnifiedRedo() },
                                enabled = canHeaderRedo,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("redo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canHeaderRedo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                DropdownMenuItem(
                                    text = { Text(if (voiceRecorderController.isRecording) "Save Voice Recording" else "Record Voice Note") },
                                    leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TerracottaAccent) },
                                    modifier = Modifier.testTag("more_menu_record_voice"),
                                    onClick = {
                                        showMoreMenu = false
                                        toggleVoiceRecording()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Smart Utilities") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TerracottaAccent) },
                                    modifier = Modifier.testTag("smart_utilities_button"),
                                    onClick = {
                                        showMoreMenu = false
                                        showSmartUtilitiesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send to Sticky Wall") },
                                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    onClick = {
                                        showMoreMenu = false
                                        val now = System.currentTimeMillis()
                                        viewModel.saveStickyNote(
                                            com.example.data.model.StickyNoteEntity(
                                                id = java.util.UUID.randomUUID().toString(),
                                                boardName = "default",
                                                title = title.takeIf { it.isNotBlank() } ?: "From Note",
                                                content = contentState.text.take(500),
                                                colorHex = "#FEF3C7",
                                                createdAtUtc = now,
                                                updatedAtUtc = now
                                            )
                                        )
                                        Toast.makeText(context, "Sent to Sticky Wall", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isPinned) "Unpin Note" else "Pin Note") },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                    onClick = {
                                        isPinned = !isPinned
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) "Remove from Favourites" else "Add to Favourites") },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444)) },
                                    onClick = {
                                        isFavorite = !isFavorite
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate Note") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        if (isEmptyDraft()) {
                                            Toast.makeText(context, "Add content before duplicating", Toast.LENGTH_SHORT).show()
                                        } else {
                                            saveCurrentNote {
                                                val noteToDuplicate = originalNote ?: buildCurrentNote(currentNoteId)
                                                viewModel.duplicateNote(noteToDuplicate) { onNavigateBack() }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isLocked) "Unlock from Vault" else "Lock in Vault") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        if (!isLocked) showPinDialog = true else {
                                            isLocked = false
                                            currentRevision = System.currentTimeMillis()
                                            hasUnsavedChanges = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reminder") },
                                    leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showReminderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Colors & Textures") },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showColorSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Version History") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                    modifier = Modifier.testTag("more_menu_version_history"),
                                    onClick = {
                                        showMoreMenu = false
                                        if (isLocked) {
                                            Toast.makeText(context, "Version history is disabled for locked notes", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showVersionHistoryDialog = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Note Statistics") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showStatsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isArchived) "Unarchive Note" else "Archive Note") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        isArchived = !isArchived
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        moveCurrentNoteToTrash()
                                    }
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = TerracottaAccent,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    finishAndSaveActiveRecording()
                                    dictationController.stopListening()
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    isDrawingMode = false
                                    if (isEmptyDraft()) {
                                        Toast.makeText(context, "Add title, text, ink, or voice to save", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveCurrentNote {
                                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .testTag("preview_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save Note",
                                    tint = onAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = onAccentColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Row 2: Quick Tools Bar (Format, Voice, Image, Table, Code, More)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .testTag("below_header_utility_strip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        try { bodyFocusRequester.requestFocus() } catch (_: Exception) {}
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatSize,
                                    contentDescription = "Format",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Format",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { toggleVoiceDictation() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = if (dictationController.isListening) TerracottaAccent else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (dictationController.isListening) "Listening" else "Voice",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (dictationController.isListening) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { launchPhotoPicker() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Image",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Image",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val table = RichDocumentReducer.createDefaultTable(isSpreadsheet = false, rows = 3, cols = 3)
                                        val res = RichDocumentReducer.insertBlockAfter(
                                            document = richDocument,
                                            afterBlockId = activeRichBlockId,
                                            newBlock = table,
                                            ensureTrailingParagraph = true
                                        )
                                        selectedTableCell = TableCellSelection(table.id, 0, 0)
                                        commitRichDocument(res.document)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = "Table",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Table",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        applyRichInlineAction(
                                            updateTypingStyle = { it.copy(inlineCode = !it.inlineCode) },
                                            updateRangeSpans = { s, l, a, b -> RichSpanEngine.toggleInlineCode(s, l, a, b) }
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = "Code",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Code",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showMoreMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val activeScrollState = if (selectedTab == 0) editScrollState else viewScrollState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editScrollState)
                            .padding(horizontal = 22.dp, vertical = 6.dp)
                    ) {
                        // Category & Tags Row (Matches NoteLand Header Design)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_top_pills_row"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Notes, To-Do List, Ideas, Research
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val folderDisplayPath = remember(folders, selectedFolderId, selectedFolderName) {
                                    if (!selectedFolderId.isNullOrBlank()) {
                                        FolderHierarchyUtils.getFolderPathString(folders, selectedFolderId).ifBlank { selectedFolderName }
                                    } else {
                                        selectedFolderName
                                    }
                                }
                                OutlinedEditorialPill(
                                    label = folderDisplayPath.ifBlank { "Notes" },
                                    accentColor = Color(0xFFE65100),
                                    leadingIcon = Icons.Default.Folder,
                                    isDark = isDark,
                                    isHighlighted = true,
                                    onClick = { showFolderMenu = true }
                                )
                                OutlinedEditorialPill(
                                    label = "To-Do List",
                                    accentColor = Color(0xFF1976D2),
                                    leadingIcon = Icons.Default.CheckBox,
                                    isDark = isDark,
                                    isHighlighted = checklistItems.isNotEmpty(),
                                    onClick = {
                                        coroutineScope.launch {
                                            try { checklistBringIntoViewRequester.bringIntoView() } catch (_: Exception) {}
                                            try { checklistFocusRequester.requestFocus() } catch (_: Exception) {}
                                        }
                                    }
                                )
                                OutlinedEditorialPill(
                                    label = "Ideas",
                                    accentColor = Color(0xFF7B1FA2),
                                    leadingIcon = Icons.Default.Lightbulb,
                                    isDark = isDark,
                                    isHighlighted = "Ideas" in tagsList,
                                    onClick = {
                                        if ("Ideas" !in tagsList) tagsList.add("Ideas") else tagsList.remove("Ideas")
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                    }
                                )
                                OutlinedEditorialPill(
                                    label = "Research",
                                    accentColor = Color(0xFF2E7D32),
                                    leadingIcon = Icons.Default.School,
                                    isDark = isDark,
                                    isHighlighted = "Research" in tagsList,
                                    onClick = {
                                        if ("Research" !in tagsList) tagsList.add("Research") else tagsList.remove("Research")
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                    }
                                )
                            }

                            // Row 2: Meeting, Tag, + Add, and custom tags
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedEditorialPill(
                                    label = "Meeting",
                                    accentColor = Color(0xFF455A64),
                                    leadingIcon = Icons.Default.CalendarToday,
                                    isDark = isDark,
                                    isHighlighted = "Meeting" in tagsList,
                                    onClick = {
                                        if ("Meeting" !in tagsList) tagsList.add("Meeting") else tagsList.remove("Meeting")
                                        currentRevision = System.currentTimeMillis()
                                        hasUnsavedChanges = true
                                    }
                                )
                                OutlinedEditorialPill(
                                    label = "Tag",
                                    accentColor = Color(0xFF455A64),
                                    leadingIcon = Icons.Default.LocalOffer,
                                    isDark = isDark,
                                    onClick = { showAddTagDialog = true }
                                )
                                OutlinedEditorialPill(
                                    label = "+ Add",
                                    accentColor = Color(0xFF2E7D32),
                                    leadingIcon = Icons.Default.Add,
                                    isDark = isDark,
                                    onClick = { showAddTagDialog = true }
                                )
                                tagsList.filter { it !in listOf("Ideas", "Research", "Meeting") }.forEach { tag ->
                                    OutlinedEditorialPill(
                                        label = tag,
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        isDark = isDark,
                                        trailingClose = true,
                                        onClick = {
                                            tagsList.remove(tag)
                                            currentRevision = System.currentTimeMillis()
                                            hasUnsavedChanges = true
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        var lastTitleHistoryAtMs by remember(noteId) { mutableStateOf(0L) }
                        BasicTextField(
                            value = title,
                            onValueChange = { nextTitle ->
                                if (nextTitle != title) {
                                    val nowMs = System.currentTimeMillis()
                                    val crossedBoundary = (nowMs - lastTitleHistoryAtMs > 1200L) ||
                                        nextTitle.endsWith(" ") || nextTitle.endsWith("\n")
                                    if (crossedBoundary) {
                                        if (useRichEditorMode) {
                                            richHistory.recordBeforeChange(captureRichSnapshot())
                                            richHistoryVersion++
                                        }
                                        undoStack.add(title to contentState.text)
                                        redoStack.clear()
                                        historyUndoKinds.add(EditorHistoryKind.TEXT)
                                        historyRedoKinds.clear()
                                        lastTitleHistoryAtMs = nowMs
                                    }
                                    title = nextTitle
                                    currentRevision = nowMs
                                    hasUnsavedChanges = true
                                    saveState = EditorSaveState.Saving
                                }
                            },
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                letterSpacing = (-0.6).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(TerracottaAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester)
                                .onFocusChanged {
                                    isTitleFocused = it.isFocused
                                    if (it.isFocused) userInteractedWithField = true
                                }
                                .testTag("note_title_input"),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        "Title",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 34.sp,
                                            lineHeight = 40.sp,
                                            letterSpacing = (-0.6).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Movable Sections (Images, Voice Recordings, Note Text, To-Do List)
                        val visibleEditBlocks = remember(blockOrder, attachedImages.size, voiceClips.size) {
                            blockOrder.filter { block ->
                                when (block) {
                                    NoteBlockType.IMAGES -> attachedImages.isNotEmpty()
                                    NoteBlockType.VOICE -> voiceClips.isNotEmpty()
                                    NoteBlockType.TEXT -> true
                                    NoteBlockType.CHECKLIST -> true
                                }
                            }
                        }
                        val blockHeightsPx = remember { mutableStateMapOf<NoteBlockType, Int>() }
                        var draggingBlock by remember { mutableStateOf<NoteBlockType?>(null) }
                        var dragOffsetY by remember { mutableFloatStateOf(0f) }

                        fun moveVisibleBlock(block: NoteBlockType, direction: Int) {
                            val visIdx = visibleEditBlocks.indexOf(block)
                            val targetVisIdx = visIdx + direction
                            if (visIdx == -1 || targetVisIdx !in visibleEditBlocks.indices) return
                            val targetBlock = visibleEditBlocks[targetVisIdx]
                            val fullA = blockOrder.indexOf(block)
                            val fullB = blockOrder.indexOf(targetBlock)
                            if (fullA != -1 && fullB != -1) {
                                val mutable = blockOrder.toMutableList()
                                mutable[fullA] = targetBlock
                                mutable[fullB] = block
                                blockOrder = mutable
                                currentRevision = System.currentTimeMillis()
                                hasUnsavedChanges = true
                                saveState = EditorSaveState.Saving
                            }
                        }

                        visibleEditBlocks.forEachIndexed { visibleIndex, blockType ->
                            key(blockType) {
                                val canMoveUp = visibleIndex > 0
                                val canMoveDown = visibleIndex < visibleEditBlocks.lastIndex
                                val isBeingDragged = draggingBlock == blockType
                                val blockTitle = when (blockType) {
                                    NoteBlockType.IMAGES -> "Photos (${attachedImages.size})"
                                    NoteBlockType.VOICE -> "Voice Note (${voiceClips.size})"
                                    NoteBlockType.TEXT -> "Note Text"
                                    NoteBlockType.CHECKLIST -> "To - Do - List (${checklistItems.count { it.isDone }}/${checklistItems.size})"
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isBeingDragged) 2f else 0f)
                                        .graphicsLayer {
                                            if (isBeingDragged) {
                                                translationY = dragOffsetY
                                                scaleX = 1.015f
                                                scaleY = 1.015f
                                            }
                                        }
                                        .onSizeChanged { blockHeightsPx[blockType] = it.height }
                                        .padding(bottom = 16.dp)
                                ) {
                                    if (visibleEditBlocks.size > 1) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                        ) {
                                            val latestVisibleEditBlocks by rememberUpdatedState(visibleEditBlocks)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .pointerInput(blockType) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                draggingBlock = blockType
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragEnd = {
                                                                draggingBlock = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggingBlock = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetY += dragAmount.y
                                                                val blocksSnapshot = latestVisibleEditBlocks
                                                                val currentIdx = blocksSnapshot.indexOf(blockType)
                                                                if (currentIdx != -1) {
                                                                    if (dragOffsetY > 0f && currentIdx < blocksSnapshot.lastIndex) {
                                                                        val nextBlock = blocksSnapshot[currentIdx + 1]
                                                                        val nextH = (blockHeightsPx[nextBlock] ?: 200).toFloat()
                                                                        if (dragOffsetY > nextH * 0.5f) {
                                                                            moveVisibleBlock(blockType, +1)
                                                                            dragOffsetY -= nextH
                                                                        }
                                                                    } else if (dragOffsetY < 0f && currentIdx > 0) {
                                                                        val prevBlock = blocksSnapshot[currentIdx - 1]
                                                                        val prevH = (blockHeightsPx[prevBlock] ?: 200).toFloat()
                                                                        if (-dragOffsetY > prevH * 0.5f) {
                                                                            moveVisibleBlock(blockType, -1)
                                                                            dragOffsetY += prevH
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                                    .testTag("drag_handle_${blockType.name.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DragIndicator,
                                                    contentDescription = "Drag to move $blockTitle",
                                                    tint = TerracottaAccent.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = blockTitle,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        letterSpacing = 0.3.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { moveVisibleBlock(blockType, -1) },
                                                    enabled = canMoveUp,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("move_${blockType.name.lowercase()}_up")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowUp,
                                                        contentDescription = "Move $blockTitle up",
                                                        tint = if (canMoveUp) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { moveVisibleBlock(blockType, +1) },
                                                    enabled = canMoveDown,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("move_${blockType.name.lowercase()}_down")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Move $blockTitle down",
                                                        tint = if (canMoveDown) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    when (blockType) {
                                        NoteBlockType.IMAGES -> {
                                            EditorialPhotoCollage(
                                                images = attachedImages,
                                                onImageClick = { index -> previewImageIndex = index },
                                                onRemoveImage = { uriToRemove ->
                                                    val updated = attachedImages.filterNot { it == uriToRemove }
                                                    imageUri = encodeImageUris(updated)
                                                    currentRevision = System.currentTimeMillis()
                                                    hasUnsavedChanges = true
                                                    saveState = EditorSaveState.Saving
                                                },
                                                onAddMore = { launchPhotoPicker() },
                                                onPasteImage = { pasteImageFromClipboard() },
                                                accentColor = TerracottaAccent,
                                                onAccentColor = onAccentColor
                                            )
                                        }

                                        NoteBlockType.VOICE -> {
                                            VoiceClipsSection(
                                                clips = voiceClips,
                                                playerController = voicePlayerController,
                                                readOnly = false,
                                                onAddVoiceClip = { toggleVoiceRecording() },
                                                onRenameClip = { clip ->
                                                    renamingVoiceClip = clip
                                                    renameClipInput = clip.title
                                                },
                                                onShareClip = { clip -> shareVoiceClip(clip) },
                                                onDeleteClip = { clip ->
                                                    voicePlayerController.stopIfPlaying(clip.id)
                                                    voiceClips.removeAll { it.id == clip.id }
                                                    viewModel.attachmentStorageManager.deleteVoiceRecordingFile(clip.filePath)
                                                    voiceClipsRevision++
                                                    currentRevision = System.currentTimeMillis()
                                                    hasUnsavedChanges = true
                                                    saveState = EditorSaveState.Saving
                                                }
                                            )
                                        }

                                        NoteBlockType.TEXT -> {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {


                                                if (useRichEditorMode) {
                                                    RichDocumentEditorPane(
                                                        document = richDocument,
                                                        activeBlockId = activeRichBlockId,
                                                        blockSelections = richBlockSelections,
                                                        pendingFocusBlockId = pendingRichFocusBlockId,
                                                        pendingFocusOffset = pendingRichFocusOffset,
                                                        selectedTableCell = selectedTableCell,
                                                        isReadOnly = false,
                                                        baseFontSizeSp = editorFontSizeSp,
                                                        accentColor = TerracottaAccent,
                                                        isDark = isDark,
                                                        onBlockFocused = { blockId ->
                                                            activeRichBlockId = blockId
                                                            isBodyFocused = true
                                                            userInteractedWithField = true
                                                            pendingTypingStyle = null
                                                        },
                                                        onPendingFocusConsumed = {
                                                            pendingRichFocusBlockId = null
                                                            pendingRichFocusOffset = null
                                                        },
                                                        onTextBlockChange = { blockId, newVal ->
                                                            val prevBlock = richDocument.blocks.firstOrNull { it.id == blockId } as? RichBlock.TextBlock
                                                            val prevSel = richBlockSelections[blockId]
                                                            richBlockSelections[blockId] = newVal.selection
                                                            if (prevBlock != null && newVal.text != prevBlock.text) {
                                                                val updatedSpans = RichSpanEngine.applyTextDiff(
                                                                    oldText = prevBlock.text,
                                                                    newText = newVal.text,
                                                                    oldSpans = prevBlock.spans,
                                                                    typingStyle = pendingTypingStyle
                                                                )
                                                                val updatedDoc = RichDocumentReducer.updateTextBlock(richDocument, blockId) {
                                                                    it.copy(text = newVal.text, spans = updatedSpans)
                                                                }
                                                                commitRichDocument(updatedDoc)
                                                            } else if (prevSel != newVal.selection) {
                                                                pendingTypingStyle = null
                                                            }
                                                        },
                                                        onEnterSplit = { blockId, offset ->
                                                            val res = RichDocumentReducer.splitBlockOnEnter(richDocument, blockId, offset)
                                                            pendingTypingStyle = null
                                                            commitRichDocument(
                                                                updatedDoc = res.document,
                                                                focusBlockId = res.focusBlockId,
                                                                focusOffset = res.focusCursorOffset
                                                            )
                                                        },
                                                        onBackspaceAtStart = { blockId ->
                                                            val res = RichDocumentReducer.handleBackspaceAtStart(richDocument, blockId)
                                                            pendingTypingStyle = null
                                                            commitRichDocument(
                                                                updatedDoc = res.document,
                                                                focusBlockId = res.focusBlockId,
                                                                focusOffset = res.focusCursorOffset
                                                            )
                                                        },
                                                        onToggleChecklist = { blockId ->
                                                            commitRichDocument(RichDocumentReducer.toggleChecklist(richDocument, blockId))
                                                        },
                                                        onCycleCalloutTone = { blockId ->
                                                            commitRichDocument(RichDocumentReducer.cycleCalloutTone(richDocument, blockId))
                                                        },
                                                        onUpdateDocument = { nextDoc ->
                                                            commitRichDocument(nextDoc)
                                                        },
                                                        onSelectTableCell = { sel ->
                                                            selectedTableCell = sel
                                                        },
                                                        onWikiLinkClick = if (!isLocked) handleWikiLinkNavigation else null,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else when (markdownEditorMode) {
                                                    EditorMarkdownMode.RAW -> {
                                                        BasicTextField(
                                                            value = contentState,
                                                            onValueChange = { applyContentChange(it) },
                                                            onTextLayout = { bodyTextLayout = it },
                                                            visualTransformation = markdownTransformation,
                                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                                fontWeight = FontWeight.Normal,
                                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                                                                fontSize = editorFontSizeSp.sp,
                                                                lineHeight = (editorFontSizeSp * 1.56f).sp
                                                            ),
                                                            cursorBrush = SolidColor(TerracottaAccent),
                                                            minLines = 7,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .focusRequester(bodyFocusRequester)
                                                                .bringIntoViewRequester(bodyBringIntoViewRequester)
                                                                .contentReceiver(bodyReceiveContentListener)
                                                                .onFocusChanged {
                                                                    isBodyFocused = it.isFocused
                                                                    if (it.isFocused) userInteractedWithField = true
                                                                }
                                                                .testTag("note_content_input"),
                                                            decorationBox = { innerTextField ->
                                                                if (contentState.text.isEmpty() && checklistItems.isEmpty()) {
                                                                    Text(
                                                                        "Tap here to pause, reflect, and write your thoughts...",
                                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                                                            fontSize = editorFontSizeSp.sp,
                                                                            lineHeight = (editorFontSizeSp * 1.56f).sp
                                                                        )
                                                                    )
                                                                }
                                                                innerTextField()
                                                            }
                                                        )
                                                    }

                                                    EditorMarkdownMode.SPLIT -> {
                                                        val paneSurfaceBg = if (isDark) {
                                                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
                                                        } else {
                                                            Color.White.copy(alpha = 0.92f)
                                                        }
                                                        val paneBorder = BorderStroke(
                                                            0.9.dp,
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                                        )
                                                        val rawScrollState = rememberScrollState()
                                                        val previewScrollState = rememberScrollState()

                                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                            val sideBySide = maxWidth >= 560.dp

                                                            val rawEditorPane: @Composable (Modifier) -> Unit = { mod ->
                                                                Surface(
                                                                    shape = RoundedCornerShape(16.dp),
                                                                    color = paneSurfaceBg,
                                                                    border = paneBorder,
                                                                    modifier = mod
                                                                ) {
                                                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                                                        Text(
                                                                            text = "RAW MARKDOWN",
                                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                                fontWeight = FontWeight.Bold,
                                                                                letterSpacing = 0.8.sp
                                                                            ),
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                        Spacer(modifier = Modifier.height(6.dp))
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .verticalScroll(rawScrollState)
                                                                        ) {
                                                                            BasicTextField(
                                                                                value = contentState,
                                                                                onValueChange = { applyContentChange(it) },
                                                                                onTextLayout = { bodyTextLayout = it },
                                                                                visualTransformation = VisualTransformation.None,
                                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                                    fontFamily = FontFamily.Monospace,
                                                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.94f),
                                                                                    fontSize = (editorFontSizeSp - 0.5f).coerceAtLeast(13f).sp,
                                                                                    lineHeight = (editorFontSizeSp * 1.48f).sp
                                                                                ),
                                                                                cursorBrush = SolidColor(TerracottaAccent),
                                                                                minLines = 5,
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .focusRequester(bodyFocusRequester)
                                                                                    .bringIntoViewRequester(bodyBringIntoViewRequester)
                                                                                    .contentReceiver(bodyReceiveContentListener)
                                                                                    .onFocusChanged {
                                                                                        isBodyFocused = it.isFocused
                                                                                        if (it.isFocused) userInteractedWithField = true
                                                                                    }
                                                                                    .testTag("note_content_input"),
                                                                                decorationBox = { innerTextField ->
                                                                                    if (contentState.text.isEmpty()) {
                                                                                        Text(
                                                                                            "Type # heading, **bold**, *italic*, - list...",
                                                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                                                fontFamily = FontFamily.Monospace,
                                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                                                                                fontSize = (editorFontSizeSp - 0.5f).coerceAtLeast(13f).sp
                                                                                            )
                                                                                        )
                                                                                    }
                                                                                    innerTextField()
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            val livePreviewPane: @Composable (Modifier) -> Unit = { mod ->
                                                                Surface(
                                                                    shape = RoundedCornerShape(16.dp),
                                                                    color = paneSurfaceBg,
                                                                    border = BorderStroke(1.dp, TerracottaAccent.copy(alpha = 0.35f)),
                                                                    modifier = mod.testTag("note_split_preview_pane")
                                                                ) {
                                                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                                                        Text(
                                                                            text = "LIVE PREVIEW",
                                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                                fontWeight = FontWeight.Bold,
                                                                                letterSpacing = 0.8.sp
                                                                            ),
                                                                            color = TerracottaAccent
                                                                        )
                                                                        Spacer(modifier = Modifier.height(6.dp))
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .verticalScroll(previewScrollState)
                                                                        ) {
                                                                            RenderedMarkdownPreview(
                                                                                markdownText = contentState.text,
                                                                                baseFontSizeSp = editorFontSizeSp,
                                                                                accentColor = TerracottaAccent,
                                                                                isDark = isDark,
                                                                                emptyPlaceholder = "Rendered rich text appears here as you type...",
                                                                                onWikiLinkClick = if (!isLocked) handleWikiLinkNavigation else null,
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (sideBySide) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                ) {
                                                                    rawEditorPane(Modifier.weight(1f).heightIn(min = 240.dp, max = 420.dp))
                                                                    livePreviewPane(Modifier.weight(1f).heightIn(min = 240.dp, max = 420.dp))
                                                                }
                                                            } else {
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                                ) {
                                                                    rawEditorPane(Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 230.dp))
                                                                    livePreviewPane(Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 230.dp))
                                                                }
                                                            }
                                                        }
                                                    }

                                                    EditorMarkdownMode.PREVIEW -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (isDark) {
                                                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
                                                            } else {
                                                                Color.White.copy(alpha = 0.92f)
                                                            },
                                                            border = BorderStroke(1.dp, TerracottaAccent.copy(alpha = 0.35f)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .testTag("note_inline_markdown_preview")
                                                        ) {
                                                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "RENDERED MARKDOWN",
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontWeight = FontWeight.Bold,
                                                                            letterSpacing = 0.8.sp
                                                                        ),
                                                                        color = TerracottaAccent
                                                                    )
                                                                    Text(
                                                                        text = "Edit Raw",
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontWeight = FontWeight.SemiBold
                                                                        ),
                                                                        color = TerracottaAccent,
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .clickable { markdownEditorMode = EditorMarkdownMode.RAW }
                                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                RenderedMarkdownPreview(
                                                                    markdownText = contentState.text,
                                                                    baseFontSizeSp = editorFontSizeSp,
                                                                    accentColor = TerracottaAccent,
                                                                    isDark = isDark,
                                                                    onWikiLinkClick = if (!isLocked) handleWikiLinkNavigation else null,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                NoteAttachmentsSection(
                                                    attachments = noteAttachments,
                                                    resolveFile = { att -> viewModel.attachmentStorageManager.getFileFromRelativePath(att.relativePath) },
                                                    isReadOnly = false,
                                                    accentColor = TerracottaAccent,
                                                    isDark = isDark,
                                                    onOpenAttachment = { openNoteAttachment(it) },
                                                    onDownloadAttachment = { att ->
                                                        pendingDownloadAttachment = att
                                                        downloadAttachmentLauncher.launch(att.fileName)
                                                    },
                                                    onRemoveAttachment = { att ->
                                                        viewModel.deleteAttachment(att)
                                                        Toast.makeText(context, "Removed ${att.fileName}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }

                                        NoteBlockType.CHECKLIST -> {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isDark) {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                } else {
                                                    Color.White.copy(alpha = 0.90f)
                                                },
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bringIntoViewRequester(checklistBringIntoViewRequester)
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                val addChecklistTasksFromInput: () -> Unit = {
                                                    val normalizedLines = newChecklistText
                                                        .replace("\r\n", "\n")
                                                        .replace('\r', '\n')
                                                        .split("\n")
                                                        .map { rawLine ->
                                                            rawLine.trim()
                                                                .removePrefix("- [ ] ")
                                                                .removePrefix("- [x] ")
                                                                .removePrefix("- [X] ")
                                                                .removePrefix("• ")
                                                                .removePrefix("- ")
                                                                .removePrefix("* ")
                                                                .trim()
                                                        }
                                                        .filter { it.isNotBlank() }
                                                    if (normalizedLines.isNotEmpty()) {
                                                        normalizedLines.forEach { taskLabel ->
                                                            checklistItems.add(
                                                                ChecklistItem(
                                                                    id = UUID.randomUUID().toString(),
                                                                    text = taskLabel,
                                                                    isDone = false
                                                                )
                                                            )
                                                        }
                                                        newChecklistText = ""
                                                        currentRevision = System.currentTimeMillis()
                                                        hasUnsavedChanges = true
                                                    }
                                                }

                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .clip(CircleShape)
                                                                    .background(TerracottaAccent.copy(alpha = 0.14f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = TerracottaAccent,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                            Text(
                                                                "To - Do - List (${checklistItems.count { it.isDone }}/${checklistItems.size})",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }

                                                        if (!isEmptyDraft() || originalNote != null) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                                                modifier = Modifier
                                                                    .clip(CircleShape)
                                                                    .clickable { moveCurrentNoteToTrash() }
                                                                    .testTag("checklist_move_to_trash_btn")
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.DeleteOutline,
                                                                        contentDescription = "Move Task to Trash",
                                                                        tint = MaterialTheme.colorScheme.error,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Text(
                                                                        text = "Trash",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                        color = MaterialTheme.colorScheme.error,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (checklistItems.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                    }

                                                    checklistItems.forEach { item ->
                                                        key(item.id) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 2.dp)
                                                            ) {
                                                                Checkbox(
                                                                    checked = item.isDone,
                                                                    onCheckedChange = { checked ->
                                                                        val targetIdx = checklistItems.indexOfFirst { it.id == item.id }
                                                                        if (targetIdx >= 0) {
                                                                            checklistItems[targetIdx] = checklistItems[targetIdx].copy(isDone = checked)
                                                                            currentRevision = System.currentTimeMillis()
                                                                            hasUnsavedChanges = true
                                                                        }
                                                                    },
                                                                    colors = CheckboxDefaults.colors(
                                                                        checkedColor = TerracottaAccent,
                                                                        checkmarkColor = onAccentColor
                                                                    )
                                                                )
                                                                BasicTextField(
                                                                    value = item.text,
                                                                    onValueChange = { updatedText ->
                                                                        val targetIdx = checklistItems.indexOfFirst { it.id == item.id }
                                                                        if (targetIdx >= 0) {
                                                                            val cleaned = updatedText.replace("\r\n", " ").replace('\n', ' ')
                                                                            checklistItems[targetIdx] = checklistItems[targetIdx].copy(text = cleaned)
                                                                            currentRevision = System.currentTimeMillis()
                                                                            hasUnsavedChanges = true
                                                                        }
                                                                    },
                                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                                                        color = if (item.isDone) {
                                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                                        } else {
                                                                            MaterialTheme.colorScheme.onSurface
                                                                        }
                                                                    ),
                                                                    cursorBrush = SolidColor(TerracottaAccent),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .padding(vertical = 6.dp)
                                                                )
                                                                IconButton(
                                                                    onClick = {
                                                                        val targetIdx = checklistItems.indexOfFirst { it.id == item.id }
                                                                        if (targetIdx >= 0) {
                                                                            checklistItems.removeAt(targetIdx)
                                                                            currentRevision = System.currentTimeMillis()
                                                                            hasUnsavedChanges = true
                                                                        }
                                                                    },
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("delete_checklist_item_${item.id}")
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.DeleteOutline,
                                                                        contentDescription = "Delete task item",
                                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 8.dp)
                                                    ) {
                                                        OutlinedTextField(
                                                            value = newChecklistText,
                                                            onValueChange = { incoming ->
                                                                newChecklistText = incoming.replace("\r\n", "\n").replace('\r', '\n')
                                                            },
                                                            placeholder = {
                                                                Text(
                                                                    "Add or paste task item...",
                                                                    fontSize = 14.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                                )
                                                            },
                                                            singleLine = false,
                                                            minLines = 1,
                                                            maxLines = 4,
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            keyboardOptions = KeyboardOptions(
                                                                capitalization = KeyboardCapitalization.Sentences,
                                                                imeAction = ImeAction.Done
                                                            ),
                                                            keyboardActions = KeyboardActions(
                                                                onDone = { addChecklistTasksFromInput() }
                                                            ),
                                                            shape = RoundedCornerShape(14.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                                focusedContainerColor = if (isDark) {
                                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                                } else {
                                                                    Color.White
                                                                },
                                                                unfocusedContainerColor = if (isDark) {
                                                                    MaterialTheme.colorScheme.surfaceContainer
                                                                } else {
                                                                    Color(0xFFFAF9F7)
                                                                },
                                                                cursorColor = TerracottaAccent,
                                                                focusedBorderColor = TerracottaAccent,
                                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .focusRequester(checklistFocusRequester)
                                                                .onFocusChanged {
                                                                    isChecklistFocused = it.isFocused
                                                                    if (it.isFocused) userInteractedWithField = true
                                                                }
                                                                .testTag("new_checklist_input")
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { addChecklistTasksFromInput() },
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .testTag("add_checklist_btn")
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = TerracottaAccent,
                                                                modifier = Modifier.size(38.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        Icons.Default.Add,
                                                                        contentDescription = "Add",
                                                                        tint = onAccentColor,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tapping blank space below blocks focuses body editor cleanly
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    try {
                                        bodyFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    } catch (_: Exception) {}
                                }
                        )
                    }
                } else {
                    // Clean View Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(viewScrollState)
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                            .testTag("note_view_mode_container")
                    ) {
                        // Read-only metadata strip (Folder hierarchy path, Type, Tags, Reminder)
                        val viewFolderPath = remember(folders, selectedFolderId, selectedFolderName) {
                            if (!selectedFolderId.isNullOrBlank()) {
                                FolderHierarchyUtils.getFolderPathString(folders, selectedFolderId).ifBlank { selectedFolderName }
                            } else {
                                selectedFolderName
                            }
                        }
                        if (viewFolderPath.isNotBlank() || tagsList.isNotEmpty() || currentReminder != null || noteType != NoteType.STANDARD.name) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                if (viewFolderPath.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = TerracottaAccent.copy(alpha = 0.12f),
                                        border = BorderStroke(0.8.dp, TerracottaAccent.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = TerracottaAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = viewFolderPath,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = TerracottaAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = SagePillAccent.copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, SagePillAccent.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = noteTypeDisplay,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SagePillAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                tagsList.forEach { tag ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                currentReminder?.let { reminderTs ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Alarm,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = SmartNoteUtils.formatShortDate(reminderTs),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (title.isNotBlank()) {
                            Text(
                                title,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    lineHeight = 38.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        val viewWordCount = SmartNoteUtils.countWords(content)
                        val viewReadMin = SmartNoteUtils.calculateReadingTimeMinutes(content)
                        val updatedLabel = SmartNoteUtils.formatShortDate(originalNote?.updatedAtUtc ?: System.currentTimeMillis())
                        Text(
                            text = if (viewWordCount > 0) {
                                "$updatedLabel  ·  $viewWordCount words  ·  $viewReadMin min read"
                            } else {
                                updatedLabel
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val viewHighlightBg = if (isDark) TerracottaAccent.copy(alpha = 0.28f) else Color(0xFFFDE047).copy(alpha = 0.45f)
                        val viewCodeBg = if (isDark) Color(0xFF162026) else Color(0xFFF1F5F9)
                        val viewLinkColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

                        if (title.isBlank() && content.isBlank() && richDocument.isEffectivelyEmpty() && checklistItems.isEmpty() && attachedImages.isEmpty() && noteAttachments.isEmpty() && inkStrokes.isEmpty() && voiceClips.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTab = 0 }
                            ) {
                                Text(
                                    text = "This note is empty. Tap Edit at the top to start writing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        blockOrder.forEach { blockType ->
                            key(blockType) {
                                when (blockType) {
                                    NoteBlockType.IMAGES -> {
                                        if (attachedImages.isNotEmpty()) {
                                            EditorialPhotoCollage(
                                                images = attachedImages,
                                                onImageClick = { index -> previewImageIndex = index },
                                                onRemoveImage = {},
                                                onAddMore = {},
                                                readOnly = true,
                                                accentColor = TerracottaAccent,
                                                onAccentColor = onAccentColor
                                            )
                                            Spacer(modifier = Modifier.height(18.dp))
                                        }
                                    }

                                    NoteBlockType.VOICE -> {
                                        if (voiceClips.isNotEmpty()) {
                                            VoiceClipsSection(
                                                clips = voiceClips,
                                                playerController = voicePlayerController,
                                                readOnly = true,
                                                onShareClip = { clip -> shareVoiceClip(clip) }
                                            )
                                            Spacer(modifier = Modifier.height(18.dp))
                                        }
                                    }

                                    NoteBlockType.TEXT -> {
                                        if (!richDocument.isEffectivelyEmpty() || content.isNotBlank()) {
                                            RichDocumentEditorPane(
                                                document = richDocument,
                                                activeBlockId = null,
                                                blockSelections = emptyMap(),
                                                pendingFocusBlockId = null,
                                                pendingFocusOffset = null,
                                                selectedTableCell = null,
                                                isReadOnly = true,
                                                baseFontSizeSp = editorFontSizeSp,
                                                accentColor = TerracottaAccent,
                                                isDark = isDark,
                                                onBlockFocused = {},
                                                onPendingFocusConsumed = {},
                                                onTextBlockChange = { _, _ -> },
                                                onEnterSplit = { _, _ -> },
                                                onBackspaceAtStart = {},
                                                onToggleChecklist = { blockId ->
                                                    commitRichDocument(RichDocumentReducer.toggleChecklist(richDocument, blockId), pushUndo = false)
                                                },
                                                onCycleCalloutTone = {},
                                                onUpdateDocument = {},
                                                onSelectTableCell = {},
                                                onWikiLinkClick = if (!isLocked) handleWikiLinkNavigation else null,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        if (noteAttachments.isNotEmpty()) {
                                            NoteAttachmentsSection(
                                                attachments = noteAttachments,
                                                resolveFile = { att -> viewModel.attachmentStorageManager.getFileFromRelativePath(att.relativePath) },
                                                isReadOnly = true,
                                                accentColor = TerracottaAccent,
                                                isDark = isDark,
                                                onOpenAttachment = { openNoteAttachment(it) },
                                                onDownloadAttachment = { att ->
                                                    pendingDownloadAttachment = att
                                                    downloadAttachmentLauncher.launch(att.fileName)
                                                },
                                                onRemoveAttachment = {}
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }

                                    NoteBlockType.CHECKLIST -> {
                                        if (checklistItems.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isDark) {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                } else {
                                                    Color.White.copy(alpha = 0.85f)
                                                },
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        "To - Do - List (${checklistItems.count { it.isDone }}/${checklistItems.size})",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    checklistItems.forEach { item ->
                                                        key(item.id) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .clickable {
                                                                        val targetIdx = checklistItems.indexOfFirst { it.id == item.id }
                                                                        if (targetIdx >= 0) {
                                                                            checklistItems[targetIdx] = checklistItems[targetIdx].copy(
                                                                                isDone = !checklistItems[targetIdx].isDone
                                                                            )
                                                                            currentRevision = System.currentTimeMillis()
                                                                            hasUnsavedChanges = true
                                                                        }
                                                                    }
                                                                    .padding(vertical = 6.dp)
                                                            ) {
                                                                Icon(
                                                                    if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                                    contentDescription = if (item.isDone) "Completed" else "Pending",
                                                                    tint = if (item.isDone) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Text(
                                                                    item.text,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                                                    ),
                                                                    color = if (item.isDone) {
                                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                                    } else {
                                                                        MaterialTheme.colorScheme.onSurface
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (!isLocked) {
                            val pdfRefs = remember(contentState.text, allLibraryAttachments) {
                                val regex = Regex("""\[pdf_ref:([^\s#\]]+)#p(\d+)]""")
                                regex.findAll(contentState.text).mapNotNull { match ->
                                    val attId = match.groupValues[1]
                                    val page = match.groupValues[2].toIntOrNull() ?: 1
                                    val att = allLibraryAttachments.firstOrNull { it.id == attId && !it.isDeleted }
                                    if (att != null) att to page else null
                                }.distinctBy { (att, page) -> "${att.id}#$page" }.toList()
                            }
                            if (pdfRefs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("linked_pdf_references_panel"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Source PDFs (${pdfRefs.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    pdfRefs.forEach { (att, pageNum) ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White.copy(alpha = 0.9f),
                                            border = BorderStroke(0.8.dp, TerracottaAccent.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    if (att.isLocked && !isVaultUnlocked) {
                                                        Toast.makeText(context, "Unlock Vault to open this locked PDF.", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        onOpenPdfAtPage(att, pageNum)
                                                    }
                                                }
                                                .testTag("pdf_ref_chip_${att.id}_$pageNum")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Default.PictureAsPdf,
                                                        contentDescription = null,
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "${att.fileName} • Page $pageNum",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "Open",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TerracottaAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isLocked && !isNewDraft) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backlinks_panel"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Backlinks (${noteBacklinks.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (noteBacklinks.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No notes link here yet — reference [[${title.ifBlank { "Note Title" }}]] from any note.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    noteBacklinks.forEach { item ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White.copy(alpha = 0.85f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    onNavigateToNote(item.sourceNote.id, item.sourceNote.noteType)
                                                }
                                                .testTag("backlink_item_${item.sourceNote.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Link,
                                                        contentDescription = null,
                                                        tint = TerracottaAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = item.sourceNote.title.ifBlank { "Untitled" },
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (item.link.contextSnippet.isNotBlank()) {
                                                    Text(
                                                        text = item.link.contextSnippet,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (noteRelated.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("related_notes_panel"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Related Notes",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    noteRelated.take(5).forEach { rec ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White.copy(alpha = 0.80f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    onNavigateToNote(rec.note.id, rec.note.noteType)
                                                }
                                                .testTag("related_note_item_${rec.note.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = rec.note.title.ifBlank { "Untitled" },
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = rec.reasons.joinToString(" · "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // High-Accuracy Stylus & Handwriting Canvas Overlay synchronized to note scroll
                HandwritingOverlayLayer(
                    strokes = inkStrokes,
                    scrollState = activeScrollState,
                    isDrawingMode = isDrawingMode,
                    isInkVisible = isInkVisible,
                    isDark = isDark,
                    activeTool = activeInkTool,
                    activeColorArgb = activeInkColorArgb,
                    activeWidthDp = activeInkWidthDp,
                    stylusOnlyMode = stylusOnlyMode,
                    onStrokeCompleted = { stroke -> handleStrokeCompleted(stroke) },
                    onEraseAt = { ptDp, radiusDp, startSweep -> handleInkEraseAt(ptDp, radiusDp, startSweep) },
                    onCancelEraseSweep = { handleCancelInkEraseSweep() },
                    onStylusDetected = { stylusDetected = true },
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }

    // Full-screen Multi-Image Gallery Viewer Dialog (swipeable, zoomable, thumbnail strip)
    val activePreviewIdx = previewImageIndex
    if (activePreviewIdx != null && attachedImages.isNotEmpty()) {
        MultiImageViewerDialog(
            images = attachedImages,
            initialIndex = activePreviewIdx.coerceIn(0, attachedImages.lastIndex),
            readOnly = selectedTab != 0,
            accentColor = TerracottaAccent,
            onDismiss = { previewImageIndex = null },
            onDeleteImage = { uriToDelete ->
                val updated = attachedImages.filterNot { it == uriToDelete }
                imageUri = encodeImageUris(updated)
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
                if (updated.isEmpty()) {
                    previewImageIndex = null
                } else {
                    previewImageIndex = (activePreviewIdx).coerceIn(0, updated.lastIndex)
                }
            }
        )
    }

    if (needsVaultReauth) {
        PinDialog(
            title = "Unlock Locked Note",
            message = "Re-authenticate to continue viewing this locked note.",
            onDismiss = { onNavigateBack() },
            onConfirm = { pin -> viewModel.unlockVault(pin) },
            onBiometricClick = if (canPromptBiometrics) {
                {
                    BiometricAuthHelper.authenticate(
                        context = context,
                        title = "Unlock Locked Note",
                        subtitle = "Authenticate with biometrics to view this note",
                        negativeButtonText = "Use Vault PIN",
                        onSuccess = { viewModel.unlockVaultWithBiometrics() },
                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                    )
                }
            } else {
                null
            }
        )
    } else if (showPinDialog) {
        PinDialog(
            title = "Secure Vault PIN",
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                val verified = viewModel.unlockVault(pin)
                if (verified) {
                    isLocked = true
                    currentRevision = System.currentTimeMillis()
                    hasUnsavedChanges = true
                    showPinDialog = false
                }
                verified
            },
            onBiometricClick = if (canPromptBiometrics) {
                {
                    BiometricAuthHelper.authenticate(
                        context = context,
                        title = "Lock Note in Vault",
                        subtitle = "Authenticate with biometrics to lock this note",
                        negativeButtonText = "Use Vault PIN",
                        onSuccess = {
                            viewModel.unlockVaultWithBiometrics()
                            isLocked = true
                            currentRevision = System.currentTimeMillis()
                            hasUnsavedChanges = true
                            showPinDialog = false
                        },
                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                    )
                }
            } else {
                null
            }
        )
    }

    // Colors, Backgrounds & Textures Studio Sheet
    if (showColorSheet) {
        NoteStyleStudioSheet(
            selectedColorHex = selectedColorHex,
            selectedTextureKey = selectedTextureKey,
            selectedBgImagePath = selectedBgImagePath,
            isDark = isDark,
            accentColor = TerracottaAccent,
            onSelectColorHex = { hex ->
                selectedColorHex = hex
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
            },
            onSelectTextureKey = { texture ->
                selectedTextureKey = texture
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
            },
            onUploadGalleryBackground = { launchBackgroundPhotoPicker() },
            onClearGalleryBackground = {
                if (selectedBgImagePath.isNotBlank()) {
                    selectedBgImagePath = ""
                    currentRevision = System.currentTimeMillis()
                    hasUnsavedChanges = true
                }
            },
            onDismiss = { showColorSheet = false }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            currentReminder = currentReminder,
            onSetReminder = { timestamp ->
                currentReminder = timestamp
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Tag") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newTagInput.trim().removePrefix("#")
                        if (clean.isNotBlank() && !tagsList.contains(clean)) {
                            tagsList.add(clean)
                            viewModel.createTag(clean)
                            currentRevision = System.currentTimeMillis()
                            hasUnsavedChanges = true
                        }
                        newTagInput = ""
                        showAddTagDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Insert Hyperlink") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = linkTitleInput,
                        onValueChange = { linkTitleInput = it },
                        label = { Text("Link Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
                        label = { Text("URL (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (linkUrlInput.isNotBlank()) {
                            val text = linkTitleInput.ifBlank { "Link" }
                            insertFormatting("[$text]($linkUrlInput) ", "")
                        }
                        linkTitleInput = ""
                        linkUrlInput = ""
                        showLinkDialog = false
                    }
                ) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Note Statistics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val words = SmartNoteUtils.countWords(content)
                    val charsWithSpaces = SmartNoteUtils.countCharacters(content, includeSpaces = true)
                    val charsNoSpaces = SmartNoteUtils.countCharacters(content, includeSpaces = false)
                    val readTime = SmartNoteUtils.calculateReadingTimeMinutes(content)

                    Text("Words: $words", style = MaterialTheme.typography.bodyLarge)
                    Text("Characters (with spaces): $charsWithSpaces", style = MaterialTheme.typography.bodyMedium)
                    Text("Characters (no spaces): $charsNoSpaces", style = MaterialTheme.typography.bodyMedium)
                    Text("Estimated Reading Time: $readTime min", style = MaterialTheme.typography.bodyMedium)
                    Text("Checklist Progress: ${checklistItems.count { it.isDone }}/${checklistItems.size}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSmartUtilitiesSheet) {
        SmartUtilitiesSheet(
            noteContent = content,
            currentTitle = title,
            currentNoteId = noteId,
            availableFolders = folders,
            existingNotes = allNotes,
            onDismiss = { showSmartUtilitiesSheet = false },
            onApplyTitle = { newTitle ->
                title = newTitle
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
            },
            onInsertSummary = { summaryText ->
                if (useRichEditorMode) {
                    val appendedBlocks = RichNoteDocumentCodec.fromLegacyMarkdown(summaryText).blocks
                    val baseBlocks = if (richDocument.isEffectivelyEmpty()) emptyList() else richDocument.blocks
                    val merged = richDocument.copy(blocks = baseBlocks + appendedBlocks).ensureNonEmpty()
                    commitRichDocument(merged)
                } else {
                    val newText = contentState.text + "\n\n" + summaryText
                    applyContentChange(TextFieldValue(newText, TextRange(newText.length)))
                }
            },
            onAddChecklistItems = { items ->
                checklistItems.addAll(items)
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
            },
            onApplyFolder = { folder ->
                selectedFolderId = folder.id
                selectedFolderName = folder.name
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
            },
            onAddTag = { tag ->
                if (tag !in tagsList) {
                    tagsList.add(tag)
                    currentRevision = System.currentTimeMillis()
                    hasUnsavedChanges = true
                    saveState = EditorSaveState.Saving
                }
            }
        )
    }

    if (showFolderMenu) {
        HierarchicalFolderPickerSheet(
            title = "Select Folder",
            subtitle = "Organize this note inside any folder or subfolder",
            folders = folders,
            notes = allNotes,
            selectedFolderId = selectedFolderId,
            rootOptionLabel = "No Folder (Uncategorized)",
            allowCreateFolder = true,
            onCreateQuickFolder = { folderName, parentId ->
                viewModel.createFolder(
                    name = folderName,
                    parentId = parentId,
                    onCreated = { created ->
                        selectedFolderId = created.id
                        selectedFolderName = created.name
                        currentRevision = System.currentTimeMillis()
                        hasUnsavedChanges = true
                        saveState = EditorSaveState.Saving
                        showFolderMenu = false
                    }
                )
            },
            onSelectFolder = { chosen ->
                selectedFolderId = chosen?.id
                selectedFolderName = chosen?.name.orEmpty()
                currentRevision = System.currentTimeMillis()
                hasUnsavedChanges = true
                saveState = EditorSaveState.Saving
            },
            onDismiss = { showFolderMenu = false }
        )
    }

    renamingVoiceClip?.let { clipToRename ->
        AlertDialog(
            onDismissRequest = { renamingVoiceClip = null },
            title = { Text("Rename Voice Clip") },
            text = {
                OutlinedTextField(
                    value = renameClipInput,
                    onValueChange = { renameClipInput = it },
                    label = { Text("Clip title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_voice_clip_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = renameClipInput.trim()
                        val idx = voiceClips.indexOfFirst { it.id == clipToRename.id }
                        if (idx >= 0) {
                            voiceClips[idx] = voiceClips[idx].copy(title = cleaned)
                            voiceClipsRevision++
                            currentRevision = System.currentTimeMillis()
                            hasUnsavedChanges = true
                            saveState = EditorSaveState.Saving
                        }
                        renamingVoiceClip = null
                    },
                    modifier = Modifier.testTag("rename_voice_clip_confirm")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingVoiceClip = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVersionHistoryDialog && !isLocked) {
        AlertDialog(
            onDismissRequest = { showVersionHistoryDialog = false },
            title = { Text("Version History (${noteVersions.size})") },
            text = {
                if (noteVersions.isEmpty()) {
                    Text(
                        text = "No saved versions yet. Edits automatically record restorable snapshots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        noteVersions.forEach { version ->
                            val isExpanded = expandedVersionId == version.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedVersionId = if (isExpanded) null else version.id
                                    }
                                    .testTag("version_item_${version.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "v${version.versionNumber} · ${version.changeSummary}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${SmartNoteUtils.formatRelativeTime(version.createdAtUtc)} · ${version.wordCount} words",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.restoreNoteVersion(currentNoteId, version.id) { res ->
                                                    res.onSuccess { restored ->
                                                        originalNote = restored
                                                        title = restored.title
                                                        contentState = TextFieldValue(
                                                            text = restored.content,
                                                            selection = TextRange(restored.content.length)
                                                        )
                                                        val restoredRich = RichNoteDocumentCodec.loadForNote(
                                                            SmartNoteUtils.parseRichDocumentJson(restored.canvasDataJson),
                                                            restored.content
                                                        )
                                                        richDocument = restoredRich
                                                        activeRichBlockId = restoredRich.blocks.firstOrNull { b -> b is RichBlock.TextBlock }?.id
                                                        tagsList.clear()
                                                        tagsList.addAll(KnowledgeBrainEngine.parseNoteTags(restored.tagsCsv))
                                                        checklistItems.clear()
                                                        checklistItems.addAll(SmartNoteUtils.parseChecklistJson(restored.checklistJson))
                                                        hasUnsavedChanges = false
                                                        saveState = EditorSaveState.Saved
                                                        showVersionHistoryDialog = false
                                                        Toast.makeText(context, "Restored v${version.versionNumber}", Toast.LENGTH_SHORT).show()
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Restore failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("restore_version_${version.id}")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore")
                                        }
                                    }
                                    if (isExpanded) {
                                        Text(
                                            text = "${version.title}\n\n${version.content}".trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 8,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }


}

@Composable
private fun OutlinedEditorialPill(
    label: String,
    accentColor: Color,
    isDark: Boolean,
    leadingIcon: ImageVector? = null,
    isHighlighted: Boolean = false,
    trailingClose: Boolean = false,
    maxWidth: Dp = Dp.Unspecified,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isHighlighted) accentColor.copy(alpha = if (isDark) 0.18f else 0.08f) else Color.Transparent,
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.72f else 0.55f)),
        modifier = Modifier
            .then(if (maxWidth != Dp.Unspecified) Modifier.widthIn(max = maxWidth) else Modifier)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isDark) accentColor.copy(alpha = 0.96f) else accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $label",
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorialPhotoCollage(
    images: List<String>,
    onImageClick: (Int) -> Unit,
    onRemoveImage: (String) -> Unit,
    onAddMore: () -> Unit,
    onPasteImage: () -> Unit = {},
    readOnly: Boolean = false,
    accentColor: Color = Color(0xFFD85C37),
    onAccentColor: Color = Color.White
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!readOnly) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (images.size == 1) "1 Image Attached" else "${images.size} Images Attached",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.42f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onPasteImage)
                            .testTag("photo_block_paste_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste image from clipboard",
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Paste",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = accentColor
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAddMore)
                            .testTag("photo_block_add_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add more images",
                                tint = onAccentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = onAccentColor
                            )
                        }
                    }
                }
            }
        }

        when (images.size) {
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    CollagePhotoTile(
                        uri = images[0],
                        readOnly = readOnly,
                        onClick = { onImageClick(0) },
                        onRemove = { onRemoveImage(images[0]) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(194.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    images.forEachIndexed { idx, uri ->
                        CollagePhotoTile(
                            uri = uri,
                            readOnly = readOnly,
                            onClick = { onImageClick(idx) },
                            onRemove = { onRemoveImage(uri) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(206.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollagePhotoTile(
                        uri = images[0],
                        readOnly = readOnly,
                        onClick = { onImageClick(0) },
                        onRemove = { onRemoveImage(images[0]) },
                        modifier = Modifier
                            .weight(1.12f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.88f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CollagePhotoTile(
                            uri = images[1],
                            readOnly = readOnly,
                            onClick = { onImageClick(1) },
                            onRemove = { onRemoveImage(images[1]) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            CollagePhotoTile(
                                uri = images[2],
                                readOnly = readOnly,
                                onClick = { onImageClick(2) },
                                onRemove = { onRemoveImage(images[2]) },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (images.size > 3) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.42f))
                                        .clickable { onImageClick(3) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+${images.size - 3} View All",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-image interactive strip showing ALL attached images directly
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_image_strip")
                ) {
                    itemsIndexed(images) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onImageClick(index) }
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attachment ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            if (!readOnly) {
                                IconButton(
                                    onClick = { onRemoveImage(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove image ${index + 1}",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePhotoTile(
    uri: String,
    readOnly: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Note attachment",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (!readOnly) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.56f), CircleShape)
                    .size(26.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiImageViewerDialog(
    images: List<String>,
    initialIndex: Int,
    readOnly: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDeleteImage: (String) -> Unit
) {
    val safeInitial = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { images.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040708))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .statusBarsPadding()
                .testTag("multi_image_viewer_dialog")
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val uri = images[page]
                var scale by remember(uri) { mutableFloatStateOf(1f) }
                var offset by remember(uri) { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.4f
                                    }
                                }
                            )
                        }
                        .then(
                            if (scale > 1.02f) {
                                Modifier.pointerInput(uri) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = nextScale
                                        if (nextScale <= 1.02f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            val maxX = (size.width * (nextScale - 1f)) / 2f
                                            val maxY = (size.height * (nextScale - 1f)) / 2f
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Image ${page + 1} of ${images.size}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
            }

            // Top overlay bar: Close, Index Counter, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color(0xFF0F1417).copy(alpha = 0.85f), CircleShape)
                        .border(0.8.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .size(44.dp)
                        .testTag("image_viewer_close_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close viewer", tint = Color.White)
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F1417).copy(alpha = 0.85f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("image_viewer_counter")
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                if (!readOnly && pagerState.currentPage in images.indices) {
                    IconButton(
                        onClick = { onDeleteImage(images[pagerState.currentPage]) },
                        modifier = Modifier
                            .background(Color(0xFF0F1417).copy(alpha = 0.85f), CircleShape)
                            .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.45f), CircleShape)
                            .size(44.dp)
                            .testTag("image_viewer_delete_btn")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove current image",
                            tint = Color(0xFFFCA5A5)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // Bottom thumbnail selector when multiple images exist
            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F1417).copy(alpha = 0.88f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        itemsIndexed(images) { idx, uri ->
                            val isSelected = idx == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Thumbnail ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
