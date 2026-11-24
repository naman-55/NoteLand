package com.example.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.model.DividerStyle
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCellSelection

@Composable
fun RichDocumentEditorPane(
    document: RichNoteDocument,
    activeBlockId: String?,
    blockSelections: Map<String, TextRange>,
    pendingFocusBlockId: String?,
    pendingFocusOffset: Int?,
    selectedTableCell: TableCellSelection?,
    isReadOnly: Boolean,
    baseFontSizeSp: Float,
    accentColor: Color,
    isDark: Boolean,
    onBlockFocused: (String) -> Unit,
    onPendingFocusConsumed: () -> Unit,
    onTextBlockChange: (blockId: String, newValue: TextFieldValue) -> Unit,
    onEnterSplit: (blockId: String, cursorOffset: Int) -> Unit,
    onBackspaceAtStart: (blockId: String) -> Unit,
    onToggleChecklist: (blockId: String) -> Unit,
    onCycleCalloutTone: (blockId: String) -> Unit,
    onUpdateDocument: (RichNoteDocument) -> Unit,
    onSelectTableCell: (TableCellSelection?) -> Unit,
    onWikiLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstTextBlockId = document.blocks.firstOrNull { it is RichBlock.TextBlock }?.id
    val resolvedActiveId = activeBlockId ?: firstTextBlockId

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isReadOnly) "rich_document_view_pane" else "rich_document_edit_pane"),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        document.blocks.forEachIndexed { index, block ->
            key(block.id) {
                when (block) {
                    is RichBlock.TextBlock -> {
                        val currentSelection = blockSelections[block.id] ?: TextRange(block.text.length)
                        val isTargetFocus = pendingFocusBlockId == block.id
                        RichTextBlockView(
                            block = block,
                            blockIndex = index,
                            allBlocks = document.blocks,
                            isActive = block.id == resolvedActiveId,
                            isReadOnly = isReadOnly,
                            selection = currentSelection,
                            pendingFocusOffset = if (isTargetFocus) pendingFocusOffset else null,
                            baseFontSizeSp = baseFontSizeSp,
                            accentColor = accentColor,
                            isDark = isDark,
                            showPlaceholder = index == 0 || block.id == resolvedActiveId,
                            onFocused = {
                                onSelectTableCell(null)
                                onBlockFocused(block.id)
                            },
                            onPendingFocusConsumed = onPendingFocusConsumed,
                            onValueChange = { tfv -> onTextBlockChange(block.id, tfv) },
                            onEnterSplit = { offset -> onEnterSplit(block.id, offset) },
                            onBackspaceAtStart = { onBackspaceAtStart(block.id) },
                            onToggleChecklist = { onToggleChecklist(block.id) },
                            onCycleCalloutTone = { onCycleCalloutTone(block.id) },
                            onWikiLinkClick = onWikiLinkClick
                        )
                    }

                    is RichBlock.DividerBlock -> {
                        RichDividerBlockView(
                            block = block,
                            isReadOnly = isReadOnly,
                            accentColor = accentColor,
                            onCycleStyle = {
                                val styles = DividerStyle.entries
                                val nextStyle = styles[(styles.indexOf(block.style) + 1) % styles.size]
                                val updated = document.copy(
                                    blocks = document.blocks.map {
                                        if (it is RichBlock.DividerBlock && it.id == block.id) {
                                            it.copy(style = nextStyle)
                                        } else {
                                            it
                                        }
                                    }
                                )
                                onUpdateDocument(updated)
                            },
                            onDelete = {
                                onUpdateDocument(RichDocumentReducer.deleteBlock(document, block.id))
                            }
                        )
                    }

                    is RichBlock.TableBlock -> {
                        RichTableBlockView(
                            block = block,
                            selectedCell = selectedTableCell,
                            isReadOnly = isReadOnly,
                            accentColor = accentColor,
                            isDark = isDark,
                            onSelectCell = { r, c ->
                                onSelectTableCell(TableCellSelection(block.id, r, c))
                            },
                            onCellTextChange = { r, c, text ->
                                onUpdateDocument(
                                    RichDocumentReducer.updateTableCell(document, block.id, r, c) {
                                        it.copy(text = text)
                                    }
                                )
                            },
                            onToggleCellBold = { r, c ->
                                onUpdateDocument(
                                    RichDocumentReducer.updateTableCell(document, block.id, r, c) {
                                        it.copy(isBold = !it.isBold)
                                    }
                                )
                            },
                            onCycleCellAlignment = { r, c ->
                                val aligns = RichTextAlignment.entries
                                onUpdateDocument(
                                    RichDocumentReducer.updateTableCell(document, block.id, r, c) {
                                        val next = aligns[(aligns.indexOf(it.alignment) + 1) % aligns.size]
                                        it.copy(alignment = next)
                                    }
                                )
                            },
                            onInsertRow = { atIdx ->
                                onUpdateDocument(RichDocumentReducer.insertTableRow(document, block.id, atIdx))
                            },
                            onDeleteRow = { rIdx ->
                                val nextDoc = RichDocumentReducer.deleteTableRow(document, block.id, rIdx)
                                onUpdateDocument(nextDoc)
                                if (selectedTableCell?.blockId == block.id) {
                                    val maxR = (block.rowCount - 2).coerceAtLeast(0)
                                    onSelectTableCell(selectedTableCell.copy(rowIndex = selectedTableCell.rowIndex.coerceAtMost(maxR)))
                                }
                            },
                            onInsertColumn = { atIdx ->
                                onUpdateDocument(RichDocumentReducer.insertTableColumn(document, block.id, atIdx))
                            },
                            onDeleteColumn = { cIdx ->
                                val nextDoc = RichDocumentReducer.deleteTableColumn(document, block.id, cIdx)
                                onUpdateDocument(nextDoc)
                                if (selectedTableCell?.blockId == block.id) {
                                    val maxC = (block.colCount - 2).coerceAtLeast(0)
                                    onSelectTableCell(selectedTableCell.copy(colIndex = selectedTableCell.colIndex.coerceAtMost(maxC)))
                                }
                            },
                            onToggleSpreadsheetMode = {
                                onUpdateDocument(RichDocumentReducer.toggleSpreadsheetMode(document, block.id))
                            },
                            onDeleteTable = {
                                if (selectedTableCell?.blockId == block.id) onSelectTableCell(null)
                                onUpdateDocument(RichDocumentReducer.deleteBlock(document, block.id))
                            }
                        )
                    }
                }
            }
        }
    }
}
