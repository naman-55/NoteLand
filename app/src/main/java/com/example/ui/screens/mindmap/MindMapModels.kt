package com.example.ui.screens.mindmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.CanvasElementEntity
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import org.json.JSONObject

enum class MindMapDrawerTab(val label: String) {
    INSERT("Insert"),
    LAYOUT("Layout"),
    STYLE("Style"),
    CONNECT("Connect"),
    TOOLS("Tools")
}

enum class MindMapLayoutType(val label: String) {
    RADIAL("Radial"),
    HIERARCHICAL("Hierarchical"),
    TREE("Tree"),
    CONCEPT("Concept"),
    TIMELINE("Timeline"),
    ORG_CHART("Org Chart")
}

enum class MindMapThemePreset(
    val label: String,
    val rootStart: Color,
    val rootEnd: Color,
    val branchPalette: List<Color>,
    val canvasTint: Color
) {
    EMERALD_MULTI(
        label = "Emerald Studio",
        rootStart = Color(0xFF16A34A),
        rootEnd = Color(0xFF047857),
        branchPalette = listOf(
            Color(0xFF9333EA), // Purple (Topic 1)
            Color(0xFFF59E0B), // Amber (Topic 2)
            Color(0xFF3B82F6), // Sky Blue (Topic 3)
            Color(0xFFEC4899), // Pink (Topic 4)
            Color(0xFF06B6D4), // Cyan
            Color(0xFF10B981)  // Emerald
        ),
        canvasTint = Color(0xFFF7FCFA)
    ),
    OCEAN_FLOW(
        label = "Ocean Blue",
        rootStart = Color(0xFF2563EB),
        rootEnd = Color(0xFF1D4ED8),
        branchPalette = listOf(
            Color(0xFF0284C7),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6),
            Color(0xFF6366F1),
            Color(0xFF0EA5E9),
            Color(0xFF14B8A6)
        ),
        canvasTint = Color(0xFFF5F9FF)
    ),
    ROYAL_VIOLET(
        label = "Royal Violet",
        rootStart = Color(0xFF7C3AED),
        rootEnd = Color(0xFF5B21B6),
        branchPalette = listOf(
            Color(0xFF9333EA),
            Color(0xFFA855F7),
            Color(0xFFEC4899),
            Color(0xFF6366F1),
            Color(0xFFD946EF),
            Color(0xFF8B5CF6)
        ),
        canvasTint = Color(0xFFFAF5FF)
    ),
    SUNSET_AMBER(
        label = "Warm Sunset",
        rootStart = Color(0xFFEA580C),
        rootEnd = Color(0xFFC2410C),
        branchPalette = listOf(
            Color(0xFFF59E0B),
            Color(0xFFEF4444),
            Color(0xFFEC4899),
            Color(0xFFF97316),
            Color(0xFFEAB308),
            Color(0xFFFB7185)
        ),
        canvasTint = Color(0xFFFFFBF5)
    ),
    SPECTRUM(
        label = "Vivid Prism",
        rootStart = Color(0xFF059669),
        rootEnd = Color(0xFF0D9488),
        branchPalette = listOf(
            Color(0xFF8B5CF6),
            Color(0xFF0EA5E9),
            Color(0xFFF59E0B),
            Color(0xFFF43F5E),
            Color(0xFF10B981),
            Color(0xFF6366F1)
        ),
        canvasTint = Color(0xFFF8FAFC)
    );

    companion object {
        fun fromName(name: String?): MindMapThemePreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EMERALD_MULTI
        }
    }
}

enum class MindMapNodeShape(val label: String) {
    PILL("Pill"),
    ROUNDED("Rounded"),
    DIAMOND("Diamond"),
    UNDERLINE("Minimal")
}

enum class MindMapFillStyle(val label: String) {
    GRADIENT("Soft Gradient"),
    SOLID("Vivid Solid"),
    PASTEL("Soft Pastel"),
    OUTLINE("Clean Outline")
}

enum class MindMapConnectorType(val label: String) {
    STRAIGHT("Straight"),
    ORTHOGONAL("Elbow"),
    CURVED("Curved"),
    SPLINE("Spline"),
    BIDIRECTIONAL("Double")
}

enum class MindMapLineStyle(val label: String) {
    SOLID("Solid"),
    DASHED("Dashed"),
    DOTTED("Dotted")
}

enum class MindMapArrowhead(val label: String) {
    NONE("None"),
    OPEN_ARROW("Arrow"),
    DOT("Dot"),
    FILLED_ARROW("Filled"),
    HOLLOW_TRIANGLE("Triangle"),
    DIAMOND("Diamond")
}

data class MindMapNode(
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    val title: String,
    val iconKey: String = "",
    val level: Int = 1, // 0 = Main Topic, 1 = Primary Topic, 2+ = Subtopic
    val x: Float = 0f,
    val y: Float = 0f,
    val colorArgb: Int = Color(0xFF9333EA).toArgb(),
    val shape: MindMapNodeShape = MindMapNodeShape.PILL,
    val isLocked: Boolean = false,
    val groupId: String? = null,
    val side: Int = 0, // -1 = left, 1 = right, 0 = auto/center
    val isCollapsed: Boolean = false
) {
    fun estimatedSize(): Pair<Float, Float> {
        return when (level) {
            0 -> {
                val w = max(188f, title.length * 12.5f + 64f)
                w to 76f
            }
            1 -> {
                val iconPad = if (iconKey.isNotBlank()) 30f else 0f
                val w = max(116f, title.length * 9.8f + 44f + iconPad)
                w to 42f
            }
            else -> {
                val iconPad = if (iconKey.isNotBlank()) 22f else 0f
                val w = max(86f, title.length * 8.4f + 32f + iconPad)
                w to 30f
            }
        }
    }

    fun bounds(): Rect {
        val (w, h) = estimatedSize()
        return Rect(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
    }

    fun containsPoint(worldPoint: Offset, padding: Float = 10f): Boolean {
        val b = bounds()
        return worldPoint.x in (b.left - padding)..(b.right + padding) &&
            worldPoint.y in (b.top - padding)..(b.bottom + padding)
    }

    fun toEntity(canvasId: String, zIndex: Int): CanvasElementEntity {
        val obj = JSONObject().apply {
            put("type", ELEMENT_TYPE_MINDMAP_NODE)
            put("parentId", parentId ?: "")
            put("title", title)
            put("iconKey", iconKey)
            put("level", level)
            put("x", x.toDouble())
            put("y", y.toDouble())
            put("colorArgb", colorArgb)
            put("shape", shape.name)
            put("isLocked", isLocked)
            put("groupId", groupId ?: "")
            put("side", side)
            put("isCollapsed", isCollapsed)
        }
        return CanvasElementEntity(
            id = id,
            canvasId = canvasId,
            elementType = ELEMENT_TYPE_MINDMAP_NODE,
            elementDataJson = obj.toString(),
            zIndex = zIndex,
            createdAtUtc = System.currentTimeMillis()
        )
    }

    companion object {
        const val ELEMENT_TYPE_MINDMAP_NODE = "MINDMAP_NODE"

        fun fromEntity(entity: CanvasElementEntity): MindMapNode? {
            if (entity.elementType != ELEMENT_TYPE_MINDMAP_NODE) return null
            return try {
                val obj = JSONObject(entity.elementDataJson)
                val parentRaw = obj.optString("parentId", "")
                val groupRaw = obj.optString("groupId", "")
                MindMapNode(
                    id = entity.id,
                    parentId = parentRaw.takeIf { it.isNotBlank() },
                    title = obj.optString("title", "Topic"),
                    iconKey = obj.optString("iconKey", ""),
                    level = obj.optInt("level", 1),
                    x = obj.optDouble("x", 0.0).toFloat(),
                    y = obj.optDouble("y", 0.0).toFloat(),
                    colorArgb = obj.optInt("colorArgb", Color(0xFF9333EA).toArgb()),
                    shape = MindMapNodeShape.entries.firstOrNull {
                        it.name.equals(obj.optString("shape"), ignoreCase = true)
                    } ?: MindMapNodeShape.PILL,
                    isLocked = obj.optBoolean("isLocked", false),
                    groupId = groupRaw.takeIf { it.isNotBlank() },
                    side = obj.optInt("side", 0),
                    isCollapsed = obj.optBoolean("isCollapsed", false)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class MindMapStyleConfig(
    val layoutType: MindMapLayoutType = MindMapLayoutType.RADIAL,
    val themePreset: MindMapThemePreset = MindMapThemePreset.EMERALD_MULTI,
    val fillStyle: MindMapFillStyle = MindMapFillStyle.GRADIENT,
    val borderWidthPx: Float = 2.2f,
    val textScale: Float = 1.0f,
    val boldText: Boolean = true,
    val connectorType: MindMapConnectorType = MindMapConnectorType.CURVED,
    val lineStyle: MindMapLineStyle = MindMapLineStyle.SOLID,
    val arrowhead: MindMapArrowhead = MindMapArrowhead.NONE,
    val lineWidthPx: Float = 3.2f,
    val overrideLineColorArgb: Int? = null,
    val showGrid: Boolean = true,
    val showMintWaves: Boolean = true
) {
    fun toEntity(canvasId: String): CanvasElementEntity {
        val obj = JSONObject().apply {
            put("type", ELEMENT_TYPE_MINDMAP_CONFIG)
            put("layoutType", layoutType.name)
            put("themePreset", themePreset.name)
            put("fillStyle", fillStyle.name)
            put("borderWidthPx", borderWidthPx.toDouble())
            put("textScale", textScale.toDouble())
            put("boldText", boldText)
            put("connectorType", connectorType.name)
            put("lineStyle", lineStyle.name)
            put("arrowhead", arrowhead.name)
            put("lineWidthPx", lineWidthPx.toDouble())
            if (overrideLineColorArgb != null) put("overrideLineColorArgb", overrideLineColorArgb)
            put("showGrid", showGrid)
            put("showMintWaves", showMintWaves)
        }
        return CanvasElementEntity(
            id = "mm_config_$canvasId",
            canvasId = canvasId,
            elementType = ELEMENT_TYPE_MINDMAP_CONFIG,
            elementDataJson = obj.toString(),
            zIndex = -100,
            createdAtUtc = System.currentTimeMillis()
        )
    }

    companion object {
        const val ELEMENT_TYPE_MINDMAP_CONFIG = "MINDMAP_CONFIG"

        fun fromEntity(entity: CanvasElementEntity): MindMapStyleConfig? {
            if (entity.elementType != ELEMENT_TYPE_MINDMAP_CONFIG) return null
            return try {
                val obj = JSONObject(entity.elementDataJson)
                MindMapStyleConfig(
                    layoutType = MindMapLayoutType.entries.firstOrNull {
                        it.name.equals(obj.optString("layoutType"), ignoreCase = true)
                    } ?: MindMapLayoutType.RADIAL,
                    themePreset = MindMapThemePreset.fromName(obj.optString("themePreset")),
                    fillStyle = MindMapFillStyle.entries.firstOrNull {
                        it.name.equals(obj.optString("fillStyle"), ignoreCase = true)
                    } ?: MindMapFillStyle.GRADIENT,
                    borderWidthPx = obj.optDouble("borderWidthPx", 2.2).toFloat(),
                    textScale = obj.optDouble("textScale", 1.0).toFloat(),
                    boldText = obj.optBoolean("boldText", true),
                    connectorType = MindMapConnectorType.entries.firstOrNull {
                        it.name.equals(obj.optString("connectorType"), ignoreCase = true)
                    } ?: MindMapConnectorType.CURVED,
                    lineStyle = MindMapLineStyle.entries.firstOrNull {
                        it.name.equals(obj.optString("lineStyle"), ignoreCase = true)
                    } ?: MindMapLineStyle.SOLID,
                    arrowhead = MindMapArrowhead.entries.firstOrNull {
                        it.name.equals(obj.optString("arrowhead"), ignoreCase = true)
                    } ?: MindMapArrowhead.NONE,
                    lineWidthPx = obj.optDouble("lineWidthPx", 3.2).toFloat(),
                    overrideLineColorArgb = if (obj.has("overrideLineColorArgb")) obj.getInt("overrideLineColorArgb") else null,
                    showGrid = obj.optBoolean("showGrid", true),
                    showMintWaves = obj.optBoolean("showMintWaves", true)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

val MindMapNodeIcons = listOf(
    "brain" to "🧠",
    "target" to "🎯",
    "bulb" to "💡",
    "book" to "📖",
    "chart" to "📊",
    "star" to "⭐",
    "rocket" to "🚀",
    "check" to "✅",
    "fire" to "🔥",
    "pin" to "📌",
    "note" to "📝",
    "spark" to "✨"
)

fun resolveMindMapIconGlyph(iconKey: String): String {
    if (iconKey.isBlank()) return ""
    return MindMapNodeIcons.firstOrNull { it.first == iconKey }?.second ?: iconKey
}

/**
 * Builds the default starter Mind Map matching the reference design:
 * Main Topic (Emerald) -> Topic 1 (Purple: Idea 1..3), Topic 2 (Amber: Note A..B),
 * Topic 3 (Blue: Point 1..2), Topic 4 (Pink: Step 1..3).
 */
fun createStarterMindMap(
    theme: MindMapThemePreset = MindMapThemePreset.EMERALD_MULTI
): List<MindMapNode> {
    val palette = theme.branchPalette
    val rootId = "mm_root"
    val t1Id = "mm_topic_1"
    val t2Id = "mm_topic_2"
    val t3Id = "mm_topic_3"
    val t4Id = "mm_topic_4"

    val raw = listOf(
        MindMapNode(
            id = rootId,
            parentId = null,
            title = "Main Topic",
            iconKey = "brain",
            level = 0,
            colorArgb = theme.rootStart.toArgb(),
            side = 0
        ),
        // Topic 1 (Top-Left, Purple)
        MindMapNode(id = t1Id, parentId = rootId, title = "Topic 1", iconKey = "target", level = 1, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_t1_1", parentId = t1Id, title = "Idea 1", level = 2, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_t1_2", parentId = t1Id, title = "Idea 2", level = 2, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_t1_3", parentId = t1Id, title = "Idea 3", level = 2, colorArgb = palette[0].toArgb(), side = -1),

        // Topic 2 (Top-Right, Amber)
        MindMapNode(id = t2Id, parentId = rootId, title = "Topic 2", iconKey = "bulb", level = 1, colorArgb = palette[1].toArgb(), side = 1),
        MindMapNode(id = "mm_t2_1", parentId = t2Id, title = "Note A", level = 2, colorArgb = palette[1].toArgb(), side = 1),
        MindMapNode(id = "mm_t2_2", parentId = t2Id, title = "Note B", level = 2, colorArgb = palette[1].toArgb(), side = 1),

        // Topic 3 (Bottom-Left, Blue)
        MindMapNode(id = t3Id, parentId = rootId, title = "Topic 3", iconKey = "book", level = 1, colorArgb = palette[2].toArgb(), side = -1),
        MindMapNode(id = "mm_t3_1", parentId = t3Id, title = "Point 1", level = 2, colorArgb = palette[2].toArgb(), side = -1),
        MindMapNode(id = "mm_t3_2", parentId = t3Id, title = "Point 2", level = 2, colorArgb = palette[2].toArgb(), side = -1),

        // Topic 4 (Bottom-Right, Pink)
        MindMapNode(id = t4Id, parentId = rootId, title = "Topic 4", iconKey = "chart", level = 1, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_t4_1", parentId = t4Id, title = "Step 1", level = 2, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_t4_2", parentId = t4Id, title = "Step 2", level = 2, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_t4_3", parentId = t4Id, title = "Step 3", level = 2, colorArgb = palette[3].toArgb(), side = 1)
    )
    return applyMindMapLayout(raw, MindMapLayoutType.RADIAL)
}

/**
 * Rich 5-branch sample Mind Map ("AI in Education") matching the secondary reference screenshots.
 */
fun createAiInEducationMindMap(
    theme: MindMapThemePreset = MindMapThemePreset.EMERALD_MULTI
): List<MindMapNode> {
    val palette = theme.branchPalette
    val rootId = "mm_ai_root"
    val t1 = "mm_ai_benefits"
    val t2 = "mm_ai_challenges"
    val t3 = "mm_ai_apps"
    val t4 = "mm_ai_future"
    val t5 = "mm_ai_conclusion"
    val raw = listOf(
        MindMapNode(id = rootId, parentId = null, title = "AI in Education", iconKey = "brain", level = 0, colorArgb = theme.rootStart.toArgb(), side = 0),
        MindMapNode(id = t1, parentId = rootId, title = "Benefits", iconKey = "target", level = 1, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_b1", parentId = t1, title = "Personalized Learning", level = 2, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_b2", parentId = t1, title = "Higher Engagement", level = 2, colorArgb = palette[0].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_b3", parentId = t1, title = "Better Outcomes", level = 2, colorArgb = palette[0].toArgb(), side = -1),

        MindMapNode(id = t2, parentId = rootId, title = "Challenges", iconKey = "bulb", level = 1, colorArgb = palette[1].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_c1", parentId = t2, title = "Data Privacy", level = 2, colorArgb = palette[1].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_c2", parentId = t2, title = "Implementation Cost", level = 2, colorArgb = palette[1].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_c3", parentId = t2, title = "Digital Divide", level = 2, colorArgb = palette[1].toArgb(), side = 1),

        MindMapNode(id = t3, parentId = rootId, title = "Applications", iconKey = "book", level = 1, colorArgb = palette[2].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_a1", parentId = t3, title = "Smart Content", level = 2, colorArgb = palette[2].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_a2", parentId = t3, title = "Virtual Tutors", level = 2, colorArgb = palette[2].toArgb(), side = -1),
        MindMapNode(id = "mm_ai_a3", parentId = t3, title = "Assessment", level = 2, colorArgb = palette[2].toArgb(), side = -1),

        MindMapNode(id = t4, parentId = rootId, title = "Future Scope", iconKey = "rocket", level = 1, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_f1", parentId = t4, title = "Adaptive Learning", level = 2, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_f2", parentId = t4, title = "AI Assistants", level = 2, colorArgb = palette[3].toArgb(), side = 1),
        MindMapNode(id = "mm_ai_f3", parentId = t4, title = "Lifelong Learning", level = 2, colorArgb = palette[3].toArgb(), side = 1),

        MindMapNode(id = t5, parentId = rootId, title = "Conclusion", iconKey = "check", level = 1, colorArgb = Color(0xFF16A34A).toArgb(), side = 2),
        MindMapNode(id = "mm_ai_e1", parentId = t5, title = "Transformative Potential", level = 2, colorArgb = Color(0xFF16A34A).toArgb(), side = 2),
        MindMapNode(id = "mm_ai_e2", parentId = t5, title = "Responsible & Ethical Use", level = 2, colorArgb = Color(0xFF16A34A).toArgb(), side = 2)
    )
    return applyMindMapLayout(raw, MindMapLayoutType.RADIAL)
}

/**
 * Computes clean, non-overlapping world positions for all 6 Mind Map layouts.
 */
fun applyMindMapLayout(
    nodes: List<MindMapNode>,
    layout: MindMapLayoutType,
    centerX: Float = 0f,
    centerY: Float = 0f
): List<MindMapNode> {
    if (nodes.isEmpty()) return nodes
    val root = nodes.firstOrNull { it.parentId == null } ?: nodes.first()
    val childrenByParent = nodes.filter { it.id != root.id }.groupBy { it.parentId ?: root.id }
    val positioned = mutableMapOf<String, MindMapNode>()

    when (layout) {
        MindMapLayoutType.RADIAL -> {
            positioned[root.id] = root.copy(x = centerX, y = centerY, level = 0, side = 0)
            val primaries = childrenByParent[root.id].orEmpty()
            val leftTopics = mutableListOf<MindMapNode>()
            val rightTopics = mutableListOf<MindMapNode>()
            val bottomTopics = mutableListOf<MindMapNode>()
            primaries.forEachIndexed { idx, node ->
                when {
                    node.side == 2 -> bottomTopics.add(node.copy(side = 2))
                    node.side < 0 -> leftTopics.add(node.copy(side = -1))
                    node.side > 0 -> rightTopics.add(node.copy(side = 1))
                    idx % 2 == 0 -> leftTopics.add(node.copy(side = -1))
                    else -> rightTopics.add(node.copy(side = 1))
                }
            }

            fun placeSide(sideTopics: List<MindMapNode>, dir: Int) {
                if (sideTopics.isEmpty()) return
                val topicHeights = sideTopics.map { topic ->
                    val subs = childrenByParent[topic.id].orEmpty()
                    max(76f, subs.size * 44f + 22f)
                }
                val totalH = topicHeights.sum() + max(0, sideTopics.size - 1) * 28f
                var cursorY = centerY - totalH / 2f
                sideTopics.forEachIndexed { idx, topic ->
                    val blockH = topicHeights[idx]
                    val topicY = cursorY + blockH / 2f
                    val topicX = centerX + dir * 195f
                    positioned[topic.id] = topic.copy(x = topicX, y = topicY, level = 1, side = dir)

                    val subs = childrenByParent[topic.id].orEmpty()
                    if (subs.isNotEmpty()) {
                        val subSpacing = 40f
                        val subStartY = topicY - ((subs.size - 1) * subSpacing) / 2f
                        subs.forEachIndexed { sIdx, sub ->
                            val subX = topicX + dir * 172f
                            val subY = subStartY + sIdx * subSpacing
                            positioned[sub.id] = sub.copy(x = subX, y = subY, level = 2, side = dir)
                            placeDeepDescendants(sub.id, subX, subY, dir, 3, childrenByParent, positioned)
                        }
                    }
                    cursorY += blockH + 28f
                }
            }

            placeSide(leftTopics, -1)
            placeSide(rightTopics, 1)

            // Bottom center topics (e.g., Conclusion in 5-branch maps)
            bottomTopics.forEachIndexed { idx, topic ->
                val tx = centerX + (idx - (bottomTopics.size - 1) / 2f) * 200f
                val ty = centerY + 138f
                positioned[topic.id] = topic.copy(x = tx, y = ty, level = 1, side = 2)
                val subs = childrenByParent[topic.id].orEmpty()
                subs.forEachIndexed { sIdx, sub ->
                    val sx = tx + (sIdx - (subs.size - 1) / 2f) * 210f
                    val sy = ty + 76f
                    positioned[sub.id] = sub.copy(x = sx, y = sy, level = 2, side = 2)
                }
            }
        }

        MindMapLayoutType.HIERARCHICAL, MindMapLayoutType.ORG_CHART -> {
            val rowGap = if (layout == MindMapLayoutType.ORG_CHART) 115f else 108f
            val leafGap = 112f
            fun subtreeWidth(nodeId: String): Float {
                val kids = childrenByParent[nodeId].orEmpty()
                if (kids.isEmpty()) return leafGap
                return max(leafGap, kids.sumOf { subtreeWidth(it.id).toDouble() }.toFloat())
            }
            fun placeTopDown(node: MindMapNode, x: Float, y: Float, lvl: Int) {
                positioned[node.id] = node.copy(x = x, y = y, level = lvl)
                val kids = childrenByParent[node.id].orEmpty()
                if (kids.isEmpty()) return
                val widths = kids.map { subtreeWidth(it.id) }
                val totalW = widths.sum()
                var curX = x - totalW / 2f
                kids.forEachIndexed { i, child ->
                    val w = widths[i]
                    placeTopDown(child, curX + w / 2f, y + rowGap, lvl + 1)
                    curX += w
                }
            }
            placeTopDown(root, centerX, centerY - 120f, 0)
        }

        MindMapLayoutType.TREE -> {
            fun subtreeHeight(nodeId: String): Float {
                val kids = childrenByParent[nodeId].orEmpty()
                if (kids.isEmpty()) return 48f
                return max(48f, kids.sumOf { subtreeHeight(it.id).toDouble() }.toFloat())
            }
            fun placeRightward(node: MindMapNode, x: Float, y: Float, lvl: Int) {
                positioned[node.id] = node.copy(x = x, y = y, level = lvl, side = if (lvl == 0) 0 else 1)
                val kids = childrenByParent[node.id].orEmpty()
                if (kids.isEmpty()) return
                val heights = kids.map { subtreeHeight(it.id) }
                val totalH = heights.sum()
                var curY = y - totalH / 2f
                val stepX = if (lvl == 0) 210f else 168f
                kids.forEachIndexed { i, child ->
                    val h = heights[i]
                    placeRightward(child, x + stepX, curY + h / 2f, lvl + 1)
                    curY += h
                }
            }
            placeRightward(root, centerX - 240f, centerY, 0)
        }

        MindMapLayoutType.CONCEPT -> {
            positioned[root.id] = root.copy(x = centerX, y = centerY, level = 0, side = 0)
            val primaries = childrenByParent[root.id].orEmpty()
            val radius1 = 195f
            primaries.forEachIndexed { idx, topic ->
                val angle = -PI / 2.0 + (2.0 * PI * idx / primaries.size.coerceAtLeast(1))
                val tx = centerX + (cos(angle) * radius1).toFloat()
                val ty = centerY + (sin(angle) * radius1).toFloat()
                val dir = if (tx >= centerX) 1 else -1
                positioned[topic.id] = topic.copy(x = tx, y = ty, level = 1, side = dir)
                val subs = childrenByParent[topic.id].orEmpty()
                val spread = 0.52
                subs.forEachIndexed { sIdx, sub ->
                    val subAngle = angle + (sIdx - (subs.size - 1) / 2.0) * spread
                    val sx = tx + (cos(subAngle) * 145f).toFloat()
                    val sy = ty + (sin(subAngle) * 145f).toFloat()
                    positioned[sub.id] = sub.copy(x = sx, y = sy, level = 2, side = dir)
                    placeDeepDescendants(sub.id, sx, sy, dir, 3, childrenByParent, positioned)
                }
            }
        }

        MindMapLayoutType.TIMELINE -> {
            val primaries = childrenByParent[root.id].orEmpty()
            val spacingX = 185f
            val startX = centerX - (primaries.size * spacingX) / 2f
            positioned[root.id] = root.copy(x = startX - 135f, y = centerY, level = 0, side = 0)
            primaries.forEachIndexed { idx, topic ->
                val above = idx % 2 == 0
                val dirY = if (above) -1 else 1
                val tx = startX + idx * spacingX + 40f
                val ty = centerY + dirY * 92f
                positioned[topic.id] = topic.copy(x = tx, y = ty, level = 1, side = 1)
                val subs = childrenByParent[topic.id].orEmpty()
                subs.forEachIndexed { sIdx, sub ->
                    val sx = tx + (sIdx - (subs.size - 1) / 2f) * 96f
                    val sy = ty + dirY * 68f
                    positioned[sub.id] = sub.copy(x = sx, y = sy, level = 2, side = 1)
                    placeDeepDescendants(sub.id, sx, sy, 1, 3, childrenByParent, positioned)
                }
            }
        }
    }

    // Preserve any orphan nodes safely
    return nodes.map { positioned[it.id] ?: it }
}

private fun placeDeepDescendants(
    parentId: String,
    parentX: Float,
    parentY: Float,
    dir: Int,
    level: Int,
    childrenByParent: Map<String, List<MindMapNode>>,
    positioned: MutableMap<String, MindMapNode>
) {
    val kids = childrenByParent[parentId].orEmpty()
    if (kids.isEmpty()) return
    val gap = 38f
    val startY = parentY - ((kids.size - 1) * gap) / 2f
    kids.forEachIndexed { idx, child ->
        val cx = parentX + dir * 135f
        val cy = startY + idx * gap
        positioned[child.id] = child.copy(x = cx, y = cy, level = level, side = dir)
        placeDeepDescendants(child.id, cx, cy, dir, level + 1, childrenByParent, positioned)
    }
}

fun applyThemeToNodes(
    nodes: List<MindMapNode>,
    theme: MindMapThemePreset
): List<MindMapNode> {
    if (nodes.isEmpty()) return nodes
    val root = nodes.firstOrNull { it.parentId == null } ?: nodes.first()
    val palette = theme.branchPalette
    val primaryColorById = mutableMapOf<String, Int>()
    val primaries = nodes.filter { it.parentId == root.id }
    primaries.forEachIndexed { idx, node ->
        primaryColorById[node.id] = palette[idx % palette.size].toArgb()
    }
    val nodeById = nodes.associateBy { it.id }

    fun branchColorFor(node: MindMapNode): Int {
        if (node.id == root.id) return theme.rootStart.toArgb()
        var cur: MindMapNode? = node
        var guard = 0
        while (cur != null && guard < 16) {
            primaryColorById[cur.id]?.let { return it }
            cur = cur.parentId?.let { nodeById[it] }
            guard++
        }
        return palette.first().toArgb()
    }

    return nodes.map { node ->
        node.copy(colorArgb = branchColorFor(node))
    }
}

fun computeNodesBoundingBox(nodes: List<MindMapNode>): Rect? {
    if (nodes.isEmpty()) return null
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    nodes.forEach { node ->
        val b = node.bounds()
        if (b.left < minX) minX = b.left
        if (b.top < minY) minY = b.top
        if (b.right > maxX) maxX = b.right
        if (b.bottom > maxY) maxY = b.bottom
    }
    return Rect(minX, minY, maxX, maxY)
}

fun visibleMindMapNodes(nodes: List<MindMapNode>): List<MindMapNode> {
    if (nodes.isEmpty()) return emptyList()
    val byId = nodes.associateBy { it.id }
    return nodes.filter { node ->
        var curParentId = node.parentId
        var guard = 0
        var hidden = false
        while (curParentId != null && guard < 64) {
            val parent = byId[curParentId] ?: break
            if (parent.isCollapsed) {
                hidden = true
                break
            }
            curParentId = parent.parentId
            guard++
        }
        !hidden
    }
}

fun hasChildNodes(nodeId: String, nodes: List<MindMapNode>): Boolean {
    return nodes.any { it.parentId == nodeId }
}

fun hiddenDescendantCount(nodeId: String, nodes: List<MindMapNode>): Int {
    val childrenByParent = nodes.groupBy { it.parentId }
    var count = 0
    val queue = ArrayDeque<String>()
    childrenByParent[nodeId]?.forEach { queue.add(it.id) }
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        count++
        childrenByParent[id]?.forEach { queue.add(it.id) }
    }
    return count
}

