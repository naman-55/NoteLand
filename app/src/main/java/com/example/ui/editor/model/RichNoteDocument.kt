package com.example.ui.editor.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class UnderlineStyle {
    NONE,
    STANDARD,
    DOUBLE,
    WAVY,
    DOUBLE_WAVY,
    DOTTED,
    DASHED,
    DASH_DOT
}

enum class RichTextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class RichBlockKind {
    PARAGRAPH,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKLIST,
    BLOCKQUOTE,
    CALLOUT,
    DIVIDER,
    TABLE,
    SPREADSHEET
}

enum class CalloutTone(val label: String) {
    NOTE("Note"),
    INFO("Info"),
    TIP("Tip"),
    WARNING("Important")
}

enum class DividerStyle {
    SOLID,
    DASHED,
    DOTTED
}

data class InlineFormatStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val underline: UnderlineStyle = UnderlineStyle.NONE,
    val textColorHex: String? = null,
    val backgroundColorHex: String? = null,
    val highlightColorHex: String? = null,
    val inlineCode: Boolean = false
) {
    fun isEmpty(): Boolean {
        return !bold &&
            !italic &&
            !strikethrough &&
            underline == UnderlineStyle.NONE &&
            textColorHex.isNullOrBlank() &&
            backgroundColorHex.isNullOrBlank() &&
            highlightColorHex.isNullOrBlank() &&
            !inlineCode
    }

    fun normalized(): InlineFormatStyle = copy(
        textColorHex = textColorHex?.trim()?.takeIf { it.isNotEmpty() },
        backgroundColorHex = backgroundColorHex?.trim()?.takeIf { it.isNotEmpty() },
        highlightColorHex = highlightColorHex?.trim()?.takeIf { it.isNotEmpty() }
    )
}

data class InlineSpan(
    val start: Int,
    val end: Int,
    val style: InlineFormatStyle
) {
    val length: Int get() = (end - start).coerceAtLeast(0)
}

data class TableCell(
    val text: String = "",
    val alignment: RichTextAlignment = RichTextAlignment.LEFT,
    val isBold: Boolean = false,
    val bgColorHex: String? = null
)

data class TableCellSelection(
    val blockId: String,
    val rowIndex: Int,
    val colIndex: Int
)

sealed interface RichBlock {
    val id: String
    val kind: RichBlockKind

    data class TextBlock(
        override val id: String = UUID.randomUUID().toString(),
        override val kind: RichBlockKind = RichBlockKind.PARAGRAPH,
        val text: String = "",
        val spans: List<InlineSpan> = emptyList(),
        val alignment: RichTextAlignment = RichTextAlignment.LEFT,
        val indentLevel: Int = 0,
        val checked: Boolean = false,
        val calloutTone: CalloutTone = CalloutTone.NOTE
    ) : RichBlock

    data class DividerBlock(
        override val id: String = UUID.randomUUID().toString(),
        val style: DividerStyle = DividerStyle.SOLID
    ) : RichBlock {
        override val kind: RichBlockKind = RichBlockKind.DIVIDER
    }

    data class TableBlock(
        override val id: String = UUID.randomUUID().toString(),
        val title: String = "",
        val isSpreadsheet: Boolean = false,
        val hasHeaderRow: Boolean = true,
        val rows: List<List<TableCell>> = listOf(
            listOf(TableCell("Header 1", isBold = true), TableCell("Header 2", isBold = true), TableCell("Header 3", isBold = true)),
            listOf(TableCell(""), TableCell(""), TableCell("")),
            listOf(TableCell(""), TableCell(""), TableCell(""))
        )
    ) : RichBlock {
        override val kind: RichBlockKind
            get() = if (isSpreadsheet) RichBlockKind.SPREADSHEET else RichBlockKind.TABLE

        val rowCount: Int get() = rows.size
        val colCount: Int get() = rows.maxOfOrNull { it.size } ?: 0
    }
}

data class RichNoteDocument(
    val version: Int = CURRENT_SCHEMA_VERSION,
    val sourceContentHash: String = "",
    val blocks: List<RichBlock> = listOf(RichBlock.TextBlock())
) {
    fun ensureNonEmpty(): RichNoteDocument {
        return if (blocks.isEmpty()) copy(blocks = listOf(RichBlock.TextBlock())) else this
    }

    fun isEffectivelyEmpty(): Boolean {
        return blocks.all { block ->
            when (block) {
                is RichBlock.TextBlock -> block.text.isBlank()
                is RichBlock.DividerBlock -> false
                is RichBlock.TableBlock -> false
            }
        }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun empty(): RichNoteDocument = RichNoteDocument(
            version = CURRENT_SCHEMA_VERSION,
            blocks = listOf(RichBlock.TextBlock())
        )
    }
}

data class EditorColorSwatch(
    val name: String,
    val hex: String?,
    val lightColor: Color,
    val darkColor: Color
) {
    fun resolve(isDark: Boolean): Color = if (isDark) darkColor else lightColor
}

object EditorColorPalette {
    val textColors: List<EditorColorSwatch> = listOf(
        EditorColorSwatch("Default", null, Color(0xFF1F2937), Color(0xFFF3F4F6)),
        EditorColorSwatch("Crimson", "#DC2626", Color(0xFFDC2626), Color(0xFFF87171)),
        EditorColorSwatch("Terracotta", "#D85C37", Color(0xFFD85C37), Color(0xFFFB923C)),
        EditorColorSwatch("Amber", "#D97706", Color(0xFFB45309), Color(0xFFFBBF24)),
        EditorColorSwatch("Forest", "#15803D", Color(0xFF15803D), Color(0xFF4ADE80)),
        EditorColorSwatch("Teal", "#0F766E", Color(0xFF0F766E), Color(0xFF2DD4BF)),
        EditorColorSwatch("Ocean", "#2563EB", Color(0xFF2563EB), Color(0xFF60A5FA)),
        EditorColorSwatch("Indigo", "#4F46E5", Color(0xFF4F46E5), Color(0xFF818CF8)),
        EditorColorSwatch("Plum", "#9333EA", Color(0xFF9333EA), Color(0xFFC084FC)),
        EditorColorSwatch("Slate", "#64748B", Color(0xFF64748B), Color(0xFF94A3B8))
    )

    val textBackgroundColors: List<EditorColorSwatch> = listOf(
        EditorColorSwatch("None", null, Color.Transparent, Color.Transparent),
        EditorColorSwatch("Blush", "#FEE2E2", Color(0xFFFEE2E2), Color(0xFF5C1E22)),
        EditorColorSwatch("Peach", "#FFEDD5", Color(0xFFFFEDD5), Color(0xFF5A2E14)),
        EditorColorSwatch("Butter", "#FEF3C7", Color(0xFFFEF3C7), Color(0xFF523C10)),
        EditorColorSwatch("Mint", "#DCFCE7", Color(0xFFDCFCE7), Color(0xFF144329)),
        EditorColorSwatch("Sky", "#DBEAFE", Color(0xFFDBEAFE), Color(0xFF17365D)),
        EditorColorSwatch("Lavender", "#EDE9FE", Color(0xFFEDE9FE), Color(0xFF37235C)),
        EditorColorSwatch("Rose", "#FCE7F3", Color(0xFFFCE7F3), Color(0xFF591E3E)),
        EditorColorSwatch("Mist", "#E2E8F0", Color(0xFFE2E8F0), Color(0xFF2B3544))
    )

    val highlightColors: List<EditorColorSwatch> = listOf(
        EditorColorSwatch("None", null, Color.Transparent, Color.Transparent),
        EditorColorSwatch("Sunbeam", "#FEF08A", Color(0xB8FDE047), Color(0x66FACC15)),
        EditorColorSwatch("Lime", "#BBF7D0", Color(0xA686EFAC), Color(0x664ADE80)),
        EditorColorSwatch("Aqua", "#BAE6FD", Color(0xA67DD3FC), Color(0x6638BDF8)),
        EditorColorSwatch("Coral", "#FECACA", Color(0xA6FCA5A5), Color(0x66F87171)),
        EditorColorSwatch("Orchid", "#E9D5FF", Color(0xA6D8B4FE), Color(0x66C084FC)),
        EditorColorSwatch("Apricot", "#FED7AA", Color(0xA6FDBA74), Color(0x66FB923C))
    )

    fun resolveTextColor(hex: String?, isDark: Boolean): Color? {
        if (hex.isNullOrBlank()) return null
        textColors.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.let {
            return it.resolve(isDark)
        }
        return parseHexColorSafe(hex)
    }

    fun resolveBackgroundColor(hex: String?, isDark: Boolean): Color? {
        if (hex.isNullOrBlank()) return null
        textBackgroundColors.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.let {
            return it.resolve(isDark)
        }
        return parseHexColorSafe(hex)?.copy(alpha = if (isDark) 0.42f else 0.78f)
    }

    fun resolveHighlightColor(hex: String?, isDark: Boolean): Color? {
        if (hex.isNullOrBlank()) return null
        highlightColors.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.let {
            return it.resolve(isDark)
        }
        return parseHexColorSafe(hex)?.copy(alpha = if (isDark) 0.42f else 0.65f)
    }

    fun parseHexColorSafe(hex: String?): Color? {
        val clean = hex?.trim()?.removePrefix("#") ?: return null
        return try {
            when (clean.length) {
                6 -> Color(("FF$clean").toLong(16))
                8 -> Color(clean.toLong(16))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
