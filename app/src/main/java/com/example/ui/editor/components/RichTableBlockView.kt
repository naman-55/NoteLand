package com.example.ui.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.TableCell
import com.example.ui.editor.model.TableCellSelection

@Composable
fun RichTableBlockView(
    block: RichBlock.TableBlock,
    selectedCell: TableCellSelection?,
    isReadOnly: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onSelectCell: (rowIndex: Int, colIndex: Int) -> Unit,
    onCellTextChange: (rowIndex: Int, colIndex: Int, newText: String) -> Unit,
    onToggleCellBold: (rowIndex: Int, colIndex: Int) -> Unit,
    onCycleCellAlignment: (rowIndex: Int, colIndex: Int) -> Unit,
    onInsertRow: (atIndex: Int) -> Unit,
    onDeleteRow: (rowIndex: Int) -> Unit,
    onInsertColumn: (atIndex: Int) -> Unit,
    onDeleteColumn: (colIndex: Int) -> Unit,
    onToggleSpreadsheetMode: () -> Unit,
    onDeleteTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isThisTableSelected = selectedCell?.blockId == block.id
    val activeRow = if (isThisTableSelected) selectedCell?.rowIndex?.coerceIn(0, (block.rowCount - 1).coerceAtLeast(0)) ?: 0 else 0
    val activeCol = if (isThisTableSelected) selectedCell?.colIndex?.coerceIn(0, (block.colCount - 1).coerceAtLeast(0)) ?: 0 else 0

    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val gridBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.72f)
    val headerBg = if (block.isSpreadsheet) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        accentColor.copy(alpha = if (isDark) 0.20f else 0.11f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(
            width = if (isThisTableSelected && !isReadOnly) 1.3.dp else 0.9.dp,
            color = if (isThisTableSelected && !isReadOnly) accentColor.copy(alpha = 0.7f) else gridBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("rich_table_${block.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact Table / Spreadsheet Header Bar (shown in Edit Mode or when titled)
            if (!isReadOnly || block.isSpreadsheet || block.title.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.6f else 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (block.isSpreadsheet) Icons.Default.GridOn else Icons.Default.TableChart,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (block.isSpreadsheet) {
                                "Spreadsheet · ${block.rowCount}×${block.colCount}"
                            } else {
                                "Table · ${block.rowCount}×${block.colCount}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isThisTableSelected && !isReadOnly) {
                            val cellRef = "${RichDocumentReducer.columnIndexToLetter(activeCol)}${activeRow + 1}"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = cellRef,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isReadOnly) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SmallTableActionChip(
                                label = if (block.isSpreadsheet) "Table" else "Sheet",
                                onClick = onToggleSpreadsheetMode,
                                accentColor = accentColor,
                                testTag = "table_toggle_mode_${block.id}"
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDeleteTable() }
                                    .testTag("table_delete_${block.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Table",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Spreadsheet Formula / Active Cell Bar
            if (!isReadOnly && block.isSpreadsheet && isThisTableSelected) {
                val activeCell = block.rows.getOrNull(activeRow)?.getOrNull(activeCol) ?: TableCell()
                val cellRef = "${RichDocumentReducer.columnIndexToLetter(activeCol)}${activeRow + 1}"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "fx",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = accentColor
                    )
                    BasicTextField(
                        value = activeCell.text,
                        onValueChange = { onCellTextChange(activeRow, activeCol, it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("spreadsheet_formula_bar_${block.id}"),
                        decorationBox = { inner ->
                            if (activeCell.text.isEmpty()) {
                                Text(
                                    text = "$cellRef value or formula (e.g. =SUM(A1:A3))",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            // Horizontally Scrollable Table / Spreadsheet Grid
            val hScroll = rememberScrollState()
            val cellWidth = if (block.isSpreadsheet) 112.dp else 124.dp
            val rowHeaderWidth = 34.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Spreadsheet Column Letters Row (A, B, C...)
                if (block.isSpreadsheet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(rowHeaderWidth)
                                .height(26.dp)
                                .background(headerBg)
                                .border(0.6.dp, gridBorder)
                        )
                        for (c in 0 until block.colCount) {
                            val isColSelected = isThisTableSelected && activeCol == c
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(26.dp)
                                    .background(if (isColSelected) accentColor.copy(alpha = 0.22f) else headerBg)
                                    .border(0.6.dp, gridBorder)
                                    .clickable(enabled = !isReadOnly) { onSelectCell(activeRow, c) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = RichDocumentReducer.columnIndexToLetter(c),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isColSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Data Rows
                block.rows.forEachIndexed { rIdx, rowCells ->
                    val isHeaderRow = !block.isSpreadsheet && block.hasHeaderRow && rIdx == 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (block.isSpreadsheet) {
                            val isRowSelected = isThisTableSelected && activeRow == rIdx
                            Box(
                                modifier = Modifier
                                    .width(rowHeaderWidth)
                                    .height(40.dp)
                                    .background(if (isRowSelected) accentColor.copy(alpha = 0.22f) else headerBg)
                                    .border(0.6.dp, gridBorder)
                                    .clickable(enabled = !isReadOnly) { onSelectCell(rIdx, activeCol) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rIdx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isRowSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        rowCells.forEachIndexed { cIdx, cell ->
                            val isSelected = isThisTableSelected && activeRow == rIdx && activeCol == cIdx
                            val cellBg = EditorColorPalette.resolveBackgroundColor(cell.bgColorHex, isDark)
                                ?: if (isHeaderRow) headerBg else Color.Transparent
                            val cellAlign = when (cell.alignment) {
                                RichTextAlignment.LEFT -> TextAlign.Start
                                RichTextAlignment.CENTER -> TextAlign.Center
                                RichTextAlignment.RIGHT -> TextAlign.End
                            }
                            val cellTextStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.5.sp,
                                fontWeight = if (cell.isBold || isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = cellAlign
                            )

                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(40.dp)
                                    .background(cellBg)
                                    .border(
                                        width = if (isSelected && !isReadOnly) 1.8.dp else 0.6.dp,
                                        color = if (isSelected && !isReadOnly) accentColor else gridBorder
                                    )
                                    .clickable(enabled = !isReadOnly) { onSelectCell(rIdx, cIdx) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("table_cell_${block.id}_${rIdx}_${cIdx}"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (isReadOnly || (block.isSpreadsheet && !isSelected && cell.text.trim().startsWith("="))) {
                                    val displayVal = if (block.isSpreadsheet) {
                                        RichDocumentReducer.evaluateSpreadsheetDisplay(block.rows, cell.text)
                                    } else {
                                        cell.text
                                    }
                                    Text(
                                        text = displayVal,
                                        style = cellTextStyle,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    BasicTextField(
                                        value = cell.text,
                                        onValueChange = { onCellTextChange(rIdx, cIdx, it) },
                                        textStyle = cellTextStyle,
                                        cursorBrush = SolidColor(accentColor),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) onSelectCell(rIdx, cIdx)
                                            }
                                            .testTag("table_cell_input_${block.id}_${rIdx}_${cIdx}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Row / Column Management Strip (visible in Edit Mode)
            if (!isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.45f else 0.35f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallTableIconChip(
                        icon = Icons.Default.Add,
                        label = "Row",
                        onClick = { onInsertRow(if (isThisTableSelected) activeRow + 1 else block.rowCount) },
                        accentColor = accentColor,
                        testTag = "table_add_row_${block.id}"
                    )
                    SmallTableIconChip(
                        icon = Icons.Default.Remove,
                        label = "Row",
                        enabled = block.rowCount > 1,
                        onClick = { onDeleteRow(if (isThisTableSelected) activeRow else block.rowCount - 1) },
                        accentColor = MaterialTheme.colorScheme.error,
                        testTag = "table_del_row_${block.id}"
                    )
                    SmallTableIconChip(
                        icon = Icons.Default.Add,
                        label = "Col",
                        onClick = { onInsertColumn(if (isThisTableSelected) activeCol + 1 else block.colCount) },
                        accentColor = accentColor,
                        testTag = "table_add_col_${block.id}"
                    )
                    SmallTableIconChip(
                        icon = Icons.Default.Remove,
                        label = "Col",
                        enabled = block.colCount > 1,
                        onClick = { onDeleteColumn(if (isThisTableSelected) activeCol else block.colCount - 1) },
                        accentColor = MaterialTheme.colorScheme.error,
                        testTag = "table_del_col_${block.id}"
                    )
                    if (isThisTableSelected) {
                        val activeCell = block.rows.getOrNull(activeRow)?.getOrNull(activeCol)
                        val alignIcon = when (activeCell?.alignment) {
                            RichTextAlignment.CENTER -> Icons.Default.FormatAlignCenter
                            RichTextAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                            else -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        }
                        SmallTableIconChip(
                            icon = Icons.Default.FormatBold,
                            label = "Bold",
                            highlighted = activeCell?.isBold == true,
                            onClick = { onToggleCellBold(activeRow, activeCol) },
                            accentColor = accentColor,
                            testTag = "table_cell_bold_${block.id}"
                        )
                        SmallTableIconChip(
                            icon = alignIcon,
                            label = "Align",
                            onClick = { onCycleCellAlignment(activeRow, activeCol) },
                            accentColor = accentColor,
                            testTag = "table_cell_align_${block.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallTableActionChip(
    label: String,
    onClick: () -> Unit,
    accentColor: Color,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accentColor.copy(alpha = 0.14f),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SmallTableIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = when {
        highlighted -> accentColor
        else -> MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.9f else 0.45f)
    }
    val fg = when {
        highlighted -> Color.White
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) Color.White else if (enabled) accentColor else fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
    }
}
