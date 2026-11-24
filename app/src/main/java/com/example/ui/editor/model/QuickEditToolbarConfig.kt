package com.example.ui.editor.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * All tools available in Noteland's customizable Quick Edit Toolbar.
 * Stable [id] strings are persisted so future app updates never break or reset user preferences.
 */
enum class QuickEditTool(
    val id: String,
    val label: String,
    val shortLabel: String,
    val category: QuickEditToolCategory
) {
    BOLD("bold", "Bold", "B", QuickEditToolCategory.TEXT_STYLE),
    ITALIC("italic", "Italic", "I", QuickEditToolCategory.TEXT_STYLE),
    UNDERLINE("underline", "Underline", "U", QuickEditToolCategory.TEXT_STYLE),
    WAVY_UNDERLINE("wavy_underline", "Wavy Underline", "U~", QuickEditToolCategory.TEXT_STYLE),
    STRIKETHROUGH("strikethrough", "Strikethrough", "S", QuickEditToolCategory.TEXT_STYLE),
    TEXT_COLOR("text_color", "Text Colour", "Color", QuickEditToolCategory.COLORS),
    TEXT_BG_COLOR("text_bg_color", "Background Colour", "Bg", QuickEditToolCategory.COLORS),
    HIGHLIGHT("highlight", "Highlight", "Mark", QuickEditToolCategory.COLORS),
    ALIGNMENT("alignment", "Alignment", "Align", QuickEditToolCategory.PARAGRAPH),
    BULLET_LIST("bullet_list", "Bulleted List", "Bullets", QuickEditToolCategory.LISTS),
    NUMBERED_LIST("numbered_list", "Numbered List", "1. List", QuickEditToolCategory.LISTS),
    CHECKLIST("checklist", "Checklist / To-Do", "Tasks", QuickEditToolCategory.LISTS),
    INDENT("indent", "Increase Indent", "Indent", QuickEditToolCategory.PARAGRAPH),
    OUTDENT("outdent", "Decrease Indent", "Outdent", QuickEditToolCategory.PARAGRAPH),
    QUOTE("quote", "Blockquote", "Quote", QuickEditToolCategory.BLOCKS),
    CALLOUT("callout", "Note / Callout", "Callout", QuickEditToolCategory.BLOCKS),
    DIVIDER("divider", "Horizontal Divider", "Divider", QuickEditToolCategory.BLOCKS),
    TABLE("table", "Editable Table", "Table", QuickEditToolCategory.BLOCKS),
    SPREADSHEET("spreadsheet", "Spreadsheet", "Sheet", QuickEditToolCategory.BLOCKS),
    ATTACHMENT("attachment", "File Attachments", "Attach", QuickEditToolCategory.MEDIA),
    INLINE_CODE("inline_code", "Inline Code", "Code", QuickEditToolCategory.TEXT_STYLE),
    CLEAR_FORMAT("clear_format", "Clear Formatting", "Clear", QuickEditToolCategory.TEXT_STYLE);

    companion object {
        fun fromId(id: String): QuickEditTool? = entries.firstOrNull { it.id == id }
    }
}

enum class QuickEditToolCategory(val title: String) {
    TEXT_STYLE("Text Styling"),
    COLORS("Colours & Highlight"),
    PARAGRAPH("Alignment & Indentation"),
    LISTS("Lists & Checklists"),
    BLOCKS("Content Blocks & Tables"),
    MEDIA("Files & Attachments")
}

enum class QuickEditToolbarPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val tools: List<QuickEditTool>,
    val frequent: Set<QuickEditTool>
) {
    SIMPLE(
        id = "simple",
        title = "Simple",
        subtitle = "7 essentials",
        tools = listOf(
            QuickEditTool.BOLD,
            QuickEditTool.ITALIC,
            QuickEditTool.UNDERLINE,
            QuickEditTool.HIGHLIGHT,
            QuickEditTool.CHECKLIST,
            QuickEditTool.TABLE,
            QuickEditTool.ATTACHMENT
        ),
        frequent = setOf(QuickEditTool.BOLD, QuickEditTool.HIGHLIGHT, QuickEditTool.CHECKLIST)
    ),
    WRITING(
        id = "writing",
        title = "Writing",
        subtitle = "11 tools",
        tools = listOf(
            QuickEditTool.BOLD,
            QuickEditTool.ITALIC,
            QuickEditTool.UNDERLINE,
            QuickEditTool.TEXT_COLOR,
            QuickEditTool.HIGHLIGHT,
            QuickEditTool.ALIGNMENT,
            QuickEditTool.BULLET_LIST,
            QuickEditTool.NUMBERED_LIST,
            QuickEditTool.CHECKLIST,
            QuickEditTool.QUOTE,
            QuickEditTool.ATTACHMENT
        ),
        frequent = setOf(QuickEditTool.BOLD, QuickEditTool.HIGHLIGHT, QuickEditTool.QUOTE)
    ),
    FULL(
        id = "full",
        title = "Full",
        subtitle = "All defaults",
        tools = emptyList(), // resolved dynamically to QuickEditToolbarConfig.DEFAULT_TOOLS
        frequent = emptySet()
    )
}

/**
 * User-customizable Quick Edit Toolbar configuration.
 * Persisted as versioned JSON so updates never reset user preferences.
 */
data class QuickEditToolbarConfig(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val enabledTools: List<QuickEditTool> = DEFAULT_TOOLS,
    val frequentTools: Set<QuickEditTool> = DEFAULT_FREQUENT_TOOLS,
    val defaultTextColorHex: String? = DEFAULT_TEXT_COLOR_HEX,
    val defaultBackgroundColorHex: String? = DEFAULT_BG_COLOR_HEX,
    val defaultHighlightColorHex: String? = DEFAULT_HIGHLIGHT_COLOR_HEX,
    val preferredUnderlineStyle: UnderlineStyle = UnderlineStyle.STANDARD
) {
    /**
     * Ordered tools for rendering: preserves user's exact custom arrangement,
     * and ensures no duplicates exist.
     */
    fun orderedEnabledTools(): List<QuickEditTool> = enabledTools.distinct()

    /**
     * Tools not currently in [enabledTools], available to add.
     */
    fun availableToAddTools(): List<QuickEditTool> {
        val activeSet = enabledTools.toSet()
        return QuickEditTool.entries.filter { it !in activeSet }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        val DEFAULT_TEXT_COLOR_HEX: String = EditorColorPalette.textColors[1].hex ?: "#DC2626"
        val DEFAULT_BG_COLOR_HEX: String = EditorColorPalette.textBackgroundColors[4].hex ?: "#DCFCE7"
        val DEFAULT_HIGHLIGHT_COLOR_HEX: String = EditorColorPalette.highlightColors[1].hex ?: "#FEF08A"

        val DEFAULT_TOOLS: List<QuickEditTool> = listOf(
            QuickEditTool.BOLD,
            QuickEditTool.ITALIC,
            QuickEditTool.UNDERLINE,
            QuickEditTool.WAVY_UNDERLINE,
            QuickEditTool.STRIKETHROUGH,
            QuickEditTool.TEXT_COLOR,
            QuickEditTool.TEXT_BG_COLOR,
            QuickEditTool.HIGHLIGHT,
            QuickEditTool.ALIGNMENT,
            QuickEditTool.BULLET_LIST,
            QuickEditTool.NUMBERED_LIST,
            QuickEditTool.CHECKLIST,
            QuickEditTool.INDENT,
            QuickEditTool.OUTDENT,
            QuickEditTool.QUOTE,
            QuickEditTool.CALLOUT,
            QuickEditTool.DIVIDER,
            QuickEditTool.TABLE,
            QuickEditTool.ATTACHMENT
        )

        val DEFAULT_FREQUENT_TOOLS: Set<QuickEditTool> = setOf(
            QuickEditTool.BOLD,
            QuickEditTool.ITALIC,
            QuickEditTool.HIGHLIGHT,
            QuickEditTool.CHECKLIST
        )

        fun default(): QuickEditToolbarConfig = QuickEditToolbarConfig()
    }
}

/**
 * Pure reducer functions for customizing the Quick Edit Toolbar safely.
 */
object QuickEditConfigReducer {

    fun addTool(config: QuickEditToolbarConfig, tool: QuickEditTool): QuickEditToolbarConfig {
        if (tool in config.enabledTools) return config
        return config.copy(enabledTools = config.enabledTools + tool)
    }

    fun removeTool(config: QuickEditToolbarConfig, tool: QuickEditTool): QuickEditToolbarConfig {
        // Always keep at least 1 tool so the Quick Edit strip is never completely orphaned
        if (config.enabledTools.size <= 1) return config
        return config.copy(
            enabledTools = config.enabledTools.filterNot { it == tool },
            frequentTools = config.frequentTools - tool
        )
    }

    fun moveTool(config: QuickEditToolbarConfig, fromIndex: Int, toIndex: Int): QuickEditToolbarConfig {
        val current = config.enabledTools.distinct()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            return config
        }
        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return config.copy(enabledTools = mutable)
    }

    fun toggleFrequentTool(config: QuickEditToolbarConfig, tool: QuickEditTool): QuickEditToolbarConfig {
        val updatedEnabled = if (tool !in config.enabledTools) config.enabledTools + tool else config.enabledTools
        val isCurrentlyFrequent = tool in config.frequentTools
        val nextFrequent = if (isCurrentlyFrequent) {
            config.frequentTools - tool
        } else {
            config.frequentTools + tool
        }
        return config.copy(
            enabledTools = updatedEnabled,
            frequentTools = nextFrequent
        )
    }

    fun setDefaultTextColor(config: QuickEditToolbarConfig, hex: String?): QuickEditToolbarConfig {
        return config.copy(defaultTextColorHex = hex?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun setDefaultBackgroundColor(config: QuickEditToolbarConfig, hex: String?): QuickEditToolbarConfig {
        return config.copy(defaultBackgroundColorHex = hex?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun setDefaultHighlightColor(config: QuickEditToolbarConfig, hex: String?): QuickEditToolbarConfig {
        return config.copy(defaultHighlightColorHex = hex?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun setPreferredUnderlineStyle(config: QuickEditToolbarConfig, style: UnderlineStyle): QuickEditToolbarConfig {
        val safeStyle = if (style == UnderlineStyle.NONE) UnderlineStyle.STANDARD else style
        return config.copy(preferredUnderlineStyle = safeStyle)
    }

    fun applyPreset(config: QuickEditToolbarConfig, preset: QuickEditToolbarPreset): QuickEditToolbarConfig {
        return when (preset) {
            QuickEditToolbarPreset.FULL -> config.copy(
                enabledTools = QuickEditToolbarConfig.DEFAULT_TOOLS,
                frequentTools = QuickEditToolbarConfig.DEFAULT_FREQUENT_TOOLS
            )
            else -> config.copy(
                enabledTools = preset.tools,
                frequentTools = preset.frequent
            )
        }
    }

    fun restoreDefaults(): QuickEditToolbarConfig = QuickEditToolbarConfig.default()
}

/**
 * Forward- and backward-compatible JSON serializer for [QuickEditToolbarConfig].
 * Preserves user customizations across app updates and recovers gracefully from malformed payloads.
 */
object QuickEditToolbarConfigCodec {

    fun encode(config: QuickEditToolbarConfig): String {
        val root = JSONObject()
        root.put("v", config.schemaVersion)
        val enabledArr = JSONArray()
        config.orderedEnabledTools().forEach { enabledArr.put(it.id) }
        root.put("enabled", enabledArr)

        val freqArr = JSONArray()
        config.frequentTools.forEach { freqArr.put(it.id) }
        root.put("frequent", freqArr)

        if (!config.defaultTextColorHex.isNullOrBlank()) {
            root.put("defaultTextColor", config.defaultTextColorHex)
        } else {
            root.put("defaultTextColor", JSONObject.NULL)
        }
        if (!config.defaultBackgroundColorHex.isNullOrBlank()) {
            root.put("defaultBgColor", config.defaultBackgroundColorHex)
        } else {
            root.put("defaultBgColor", JSONObject.NULL)
        }
        if (!config.defaultHighlightColorHex.isNullOrBlank()) {
            root.put("defaultHighlightColor", config.defaultHighlightColorHex)
        } else {
            root.put("defaultHighlightColor", JSONObject.NULL)
        }
        root.put("underlineStyle", config.preferredUnderlineStyle.name)
        return root.toString()
    }

    fun decode(rawJson: String?): QuickEditToolbarConfig {
        if (rawJson.isNullOrBlank()) return QuickEditToolbarConfig.default()
        return try {
            val root = JSONObject(rawJson)
            val version = root.optInt("v", QuickEditToolbarConfig.CURRENT_SCHEMA_VERSION)

            val enabledList = mutableListOf<QuickEditTool>()
            val enabledArr = root.optJSONArray("enabled")
            if (enabledArr != null) {
                for (i in 0 until enabledArr.length()) {
                    val tool = QuickEditTool.fromId(enabledArr.optString(i, ""))
                    if (tool != null && tool !in enabledList) {
                        enabledList.add(tool)
                    }
                }
            }
            val finalEnabled = if (enabledList.isNotEmpty()) enabledList else QuickEditToolbarConfig.DEFAULT_TOOLS

            val freqSet = mutableSetOf<QuickEditTool>()
            val freqArr = root.optJSONArray("frequent")
            if (freqArr != null) {
                for (i in 0 until freqArr.length()) {
                    val tool = QuickEditTool.fromId(freqArr.optString(i, ""))
                    if (tool != null) {
                        freqSet.add(tool)
                    }
                }
            } else {
                freqSet.addAll(QuickEditToolbarConfig.DEFAULT_FREQUENT_TOOLS)
            }

            val defaultTextColor = if (root.has("defaultTextColor")) {
                if (root.isNull("defaultTextColor")) null
                else root.optString("defaultTextColor", "").trim().takeIf { it.isNotEmpty() }
            } else {
                QuickEditToolbarConfig.DEFAULT_TEXT_COLOR_HEX
            }

            val defaultBgColor = if (root.has("defaultBgColor")) {
                if (root.isNull("defaultBgColor")) null
                else root.optString("defaultBgColor", "").trim().takeIf { it.isNotEmpty() }
            } else {
                QuickEditToolbarConfig.DEFAULT_BG_COLOR_HEX
            }

            val defaultHighlightColor = if (root.has("defaultHighlightColor")) {
                if (root.isNull("defaultHighlightColor")) null
                else root.optString("defaultHighlightColor", "").trim().takeIf { it.isNotEmpty() }
            } else {
                QuickEditToolbarConfig.DEFAULT_HIGHLIGHT_COLOR_HEX
            }

            val underlineStyle = runCatching {
                UnderlineStyle.valueOf(root.optString("underlineStyle", UnderlineStyle.STANDARD.name))
            }.getOrDefault(UnderlineStyle.STANDARD).let {
                if (it == UnderlineStyle.NONE) UnderlineStyle.STANDARD else it
            }

            QuickEditToolbarConfig(
                schemaVersion = version.coerceAtLeast(QuickEditToolbarConfig.CURRENT_SCHEMA_VERSION),
                enabledTools = finalEnabled,
                frequentTools = freqSet.intersect(finalEnabled.toSet()),
                defaultTextColorHex = defaultTextColor,
                defaultBackgroundColorHex = defaultBgColor,
                defaultHighlightColorHex = defaultHighlightColor,
                preferredUnderlineStyle = underlineStyle
            )
        } catch (_: Exception) {
            QuickEditToolbarConfig.default()
        }
    }
}
