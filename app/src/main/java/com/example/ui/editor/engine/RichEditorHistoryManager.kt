package com.example.ui.editor.engine

import androidx.compose.ui.text.TextRange
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.TableCellSelection

/**
 * Immutable snapshot of editor state captured before any text, formatting, block, or table mutation.
 * Preserves active block, per-block cursor/selection ranges, and selected table cell safely.
 */
data class RichEditorHistorySnapshot(
    val title: String,
    val document: RichNoteDocument,
    val activeBlockId: String?,
    val blockSelections: Map<String, TextRange>,
    val selectedTableCell: TableCellSelection?
)

/**
 * Deterministic Undo/Redo history stack for the rich-text editor.
 * Supports text edits, inline formatting, block insertion/deletion, and table/spreadsheet edits
 * while clamping restored selection ranges and table coordinates to preserve document integrity.
 */
class RichEditorHistoryManager(
    private val maxEntries: Int = 80
) {
    private val undoStack = ArrayDeque<RichEditorHistorySnapshot>()
    private val redoStack = ArrayDeque<RichEditorHistorySnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoDepth: Int get() = undoStack.size
    val redoDepth: Int get() = redoStack.size

    fun recordBeforeChange(snapshot: RichEditorHistorySnapshot) {
        val safeSnapshot = sanitizeSnapshot(snapshot)
        if (undoStack.lastOrNull() == safeSnapshot) return
        if (undoStack.size >= maxEntries) {
            undoStack.removeFirst()
        }
        undoStack.addLast(safeSnapshot)
        redoStack.clear()
    }

    fun undo(current: RichEditorHistorySnapshot): RichEditorHistorySnapshot? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeLast()
        val safeCurrent = sanitizeSnapshot(current)
        if (redoStack.size >= maxEntries) {
            redoStack.removeFirst()
        }
        redoStack.addLast(safeCurrent)
        return sanitizeSnapshot(previous)
    }

    fun redo(current: RichEditorHistorySnapshot): RichEditorHistorySnapshot? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeLast()
        val safeCurrent = sanitizeSnapshot(current)
        if (undoStack.size >= maxEntries) {
            undoStack.removeFirst()
        }
        undoStack.addLast(safeCurrent)
        return sanitizeSnapshot(next)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Ensures every restored snapshot maintains strict document integrity:
     * - Non-empty block list
     * - All inline spans normalized within block text bounds
     * - All selection TextRanges clamped within [0, block.text.length]
     * - Active block ID and selected table cell verified against existing blocks/dimensions
     */
    fun sanitizeSnapshot(snapshot: RichEditorHistorySnapshot): RichEditorHistorySnapshot {
        val safeDoc = snapshot.document.ensureNonEmpty().let { doc ->
            doc.copy(
                blocks = doc.blocks.map { block ->
                    when (block) {
                        is RichBlock.TextBlock -> block.copy(
                            spans = RichSpanEngine.normalizeSpans(block.spans, block.text.length),
                            indentLevel = block.indentLevel.coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL)
                        )
                        is RichBlock.DividerBlock -> block
                        is RichBlock.TableBlock -> block
                    }
                }
            )
        }

        val textBlocksById = safeDoc.blocks
            .filterIsInstance<RichBlock.TextBlock>()
            .associateBy { it.id }

        val clampedSelections = buildMap {
            snapshot.blockSelections.forEach { (id, range) ->
                val block = textBlocksById[id] ?: return@forEach
                val len = block.text.length
                put(id, TextRange(range.start.coerceIn(0, len), range.end.coerceIn(0, len)))
            }
        }

        val validActiveId = snapshot.activeBlockId
            ?.takeIf { id -> safeDoc.blocks.any { it.id == id } }
            ?: safeDoc.blocks.firstOrNull { it is RichBlock.TextBlock }?.id

        val validTableCell = snapshot.selectedTableCell?.let { sel ->
            val tbl = safeDoc.blocks.firstOrNull { it.id == sel.blockId } as? RichBlock.TableBlock
            if (tbl != null && tbl.rowCount > 0 && tbl.colCount > 0) {
                sel.copy(
                    rowIndex = sel.rowIndex.coerceIn(0, tbl.rowCount - 1),
                    colIndex = sel.colIndex.coerceIn(0, tbl.colCount - 1)
                )
            } else {
                null
            }
        }

        return RichEditorHistorySnapshot(
            title = snapshot.title,
            document = safeDoc,
            activeBlockId = validActiveId,
            blockSelections = clampedSelections,
            selectedTableCell = validTableCell
        )
    }
}
