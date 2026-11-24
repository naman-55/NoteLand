package com.example.util

import com.example.data.model.NoteEntity
import com.example.data.model.NoteLinkEntity
import com.example.data.model.NoteTemplateEntity
import com.example.data.model.NoteType
import com.example.data.model.TagEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ParsedWikiLink(
    val rawMatch: String,
    val targetTitle: String,
    val normalizedTitle: String,
    val anchorHeading: String = "",
    val displayAlias: String = "",
    val startIndex: Int,
    val endIndex: Int,
    val contextSnippet: String = ""
)

data class RelatedNoteRecommendation(
    val note: NoteEntity,
    val score: Int,
    val reasons: List<String>,
    val sharedTags: List<String> = emptyList()
)

data class GraphNode(
    val noteId: String,
    val label: String,
    val subtitle: String,
    val tags: List<String>,
    val folderName: String,
    val colorHex: String,
    val noteType: String,
    val isJournal: Boolean,
    val isPinned: Boolean,
    val incomingCount: Int,
    val outgoingCount: Int,
    val degree: Int,
    val x: Float,
    val y: Float
)

data class GraphEdge(
    val id: String,
    val sourceNoteId: String,
    val targetNoteId: String,
    val label: String
)

data class KnowledgeGraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val unresolvedTargetsCount: Int,
    val orphanCount: Int
)

data class UniversalNoteMatch(
    val note: NoteEntity,
    val score: Int,
    val matchReasons: List<String>
)

data class UniversalTagMatch(
    val name: String,
    val colorHex: String,
    val noteCount: Int
)

object KnowledgeBrainEngine {

    private val wikiLinkRegex = Regex("\\[\\[([^\\[\\]\\r\\n]+?)\\]\\]")
    private val fencedCodeRegex = Regex("```[\\s\\S]*?```")
    private val wsCollapseRegex = Regex("\\s+")
    private val symbolStripRegex = Regex("[^\\p{L}\\p{N}\\s]")

    /**
     * Central privacy and visibility rule for NoteLand Brain:
     * Locked, deleted, hidden, and archived notes must NEVER leak into the graph,
     * backlinks, related recommendations, or universal search.
     */
    fun isNoteKnowledgeEligible(note: NoteEntity): Boolean {
        return !note.isDeleted && !note.isLocked && !note.isHidden && !note.isArchived
    }

    fun normalizeTitleKey(rawTitle: String): String {
        return rawTitle.trim().replace(wsCollapseRegex, " ").lowercase(Locale.US)
    }

    private fun plainAlphaNumKey(rawTitle: String): String {
        return rawTitle.replace(symbolStripRegex, " ").trim().replace(wsCollapseRegex, " ").lowercase(Locale.US)
    }

    /**
     * Parses `[[Note Name]]`, `[[Note Name|Alias]]`, `[[Note Name#Section]]`, and
     * `[[Note Name#Section|Alias]]` spans from raw markdown/note text while ignoring fenced code blocks.
     */
    fun extractWikiLinkSpans(rawText: String): List<ParsedWikiLink> {
        if (rawText.isBlank() || !rawText.contains("[[")) return emptyList()

        val excludedRanges = fencedCodeRegex.findAll(rawText).map { it.range }.toList()
        val matches = mutableListOf<ParsedWikiLink>()

        wikiLinkRegex.findAll(rawText).forEach { match ->
            val range = match.range
            if (excludedRanges.any { range.first >= it.first && range.last <= it.last }) {
                return@forEach
            }
            val inner = match.groupValues[1].trim()
            if (inner.isBlank()) return@forEach

            val pipeIdx = inner.indexOf('|')
            val leftPart = (if (pipeIdx >= 0) inner.substring(0, pipeIdx) else inner).trim()
            val aliasPart = (if (pipeIdx >= 0) inner.substring(pipeIdx + 1) else "").trim()

            val hashIdx = leftPart.indexOf('#')
            val targetTitle = (if (hashIdx >= 0) leftPart.substring(0, hashIdx) else leftPart).trim()
            val anchorHeading = (if (hashIdx >= 0) leftPart.substring(hashIdx + 1) else "").trim()
            if (targetTitle.isBlank()) return@forEach

            val lineStart = rawText.lastIndexOf('\n', (range.first - 1).coerceAtLeast(0))
                .let { if (range.first == 0 || it == -1) 0 else it + 1 }
            val lineEnd = rawText.indexOf('\n', range.last + 1)
                .let { if (it == -1) rawText.length else it }
            val snippet = rawText.substring(lineStart, lineEnd).trim().take(160)

            matches.add(
                ParsedWikiLink(
                    rawMatch = match.value,
                    targetTitle = targetTitle,
                    normalizedTitle = normalizeTitleKey(targetTitle),
                    anchorHeading = anchorHeading,
                    displayAlias = aliasPart.ifBlank { targetTitle },
                    startIndex = range.first,
                    endIndex = range.last + 1,
                    contextSnippet = snippet
                )
            )
        }
        return matches
    }

    /**
     * Extracts unique wiki-link targets per note across body content and checklist items.
     */
    fun extractUniqueWikiLinks(content: String, checklistJson: String = ""): List<ParsedWikiLink> {
        val combinedText = buildString {
            append(content)
            if (checklistJson.isNotBlank() && checklistJson != "[]") {
                val items = SmartNoteUtils.parseChecklistJson(checklistJson)
                if (items.isNotEmpty()) {
                    append('\n')
                    items.forEach { item ->
                        append(item.text)
                        append('\n')
                    }
                }
            }
        }
        val allSpans = extractWikiLinkSpans(combinedText)
        if (allSpans.isEmpty()) return emptyList()
        val seen = LinkedHashMap<String, ParsedWikiLink>()
        for (span in allSpans) {
            if (!seen.containsKey(span.normalizedTitle)) {
                seen[span.normalizedTitle] = span
            }
        }
        return seen.values.toList()
    }

    fun resolveTargetNote(
        targetTitle: String,
        eligibleNotes: List<NoteEntity>,
        excludeNoteId: String? = null
    ): NoteEntity? {
        val exactKey = normalizeTitleKey(targetTitle)
        if (exactKey.isBlank()) return null

        val exactMatch = eligibleNotes.firstOrNull { candidate ->
            candidate.id != excludeNoteId &&
                isNoteKnowledgeEligible(candidate) &&
                normalizeTitleKey(candidate.title) == exactKey
        }
        if (exactMatch != null) return exactMatch

        val plainKey = plainAlphaNumKey(targetTitle)
        if (plainKey.isBlank()) return null
        val plainMatches = eligibleNotes.filter { candidate ->
            candidate.id != excludeNoteId &&
                isNoteKnowledgeEligible(candidate) &&
                plainAlphaNumKey(candidate.title) == plainKey
        }
        return if (plainMatches.size == 1) plainMatches.first() else null
    }

    fun buildOutgoingLinkEntities(
        sourceNote: NoteEntity,
        eligibleNotes: List<NoteEntity>,
        nowUtc: Long = System.currentTimeMillis()
    ): List<NoteLinkEntity> {
        if (!isNoteKnowledgeEligible(sourceNote)) return emptyList()
        val parsed = extractUniqueWikiLinks(sourceNote.content, sourceNote.checklistJson)
        if (parsed.isEmpty()) return emptyList()

        return parsed.mapNotNull { link ->
            val target = resolveTargetNote(link.targetTitle, eligibleNotes, excludeNoteId = sourceNote.id)
            if (target?.id == sourceNote.id) return@mapNotNull null
            NoteLinkEntity(
                id = UUID.randomUUID().toString(),
                sourceNoteId = sourceNote.id,
                targetNoteId = target?.id,
                targetTitle = link.targetTitle,
                normalizedTargetTitle = link.normalizedTitle,
                anchorHeading = link.anchorHeading,
                displayAlias = if (link.displayAlias != link.targetTitle) link.displayAlias else "",
                contextSnippet = link.contextSnippet,
                createdAtUtc = nowUtc,
                updatedAtUtc = nowUtc
            )
        }
    }

    fun parseNoteTags(tagsCsv: String): List<String> {
        if (tagsCsv.isBlank()) return emptyList()
        return tagsCsv.split(',')
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    /**
     * Recommends related unlocked, visible notes using direct wiki links, 2-hop graph connections,
     * shared tags, and shared folder membership.
     */
    fun findRelatedNotes(
        anchorNote: NoteEntity,
        allNotes: List<NoteEntity>,
        allLinks: List<NoteLinkEntity>,
        maxResults: Int = 8
    ): List<RelatedNoteRecommendation> {
        if (!isNoteKnowledgeEligible(anchorNote)) return emptyList()

        val eligibleMap = allNotes
            .filter { isNoteKnowledgeEligible(it) }
            .associateBy { it.id }

        val validLinks = allLinks.filter { link ->
            link.targetNoteId != null &&
                eligibleMap.containsKey(link.sourceNoteId) &&
                eligibleMap.containsKey(link.targetNoteId) &&
                link.sourceNoteId != link.targetNoteId
        }

        val anchorOutIds = validLinks
            .filter { it.sourceNoteId == anchorNote.id }
            .mapNotNull { it.targetNoteId }
            .toSet()
        val anchorInIds = validLinks
            .filter { it.targetNoteId == anchorNote.id }
            .map { it.sourceNoteId }
            .toSet()
        val anchorDirectNeighbors = anchorOutIds + anchorInIds

        val neighborsByNote = mutableMapOf<String, MutableSet<String>>()
        validLinks.forEach { link ->
            val targetId = link.targetNoteId ?: return@forEach
            neighborsByNote.getOrPut(link.sourceNoteId) { mutableSetOf() }.add(targetId)
            neighborsByNote.getOrPut(targetId) { mutableSetOf() }.add(link.sourceNoteId)
        }

        val anchorTags = parseNoteTags(anchorNote.tagsCsv).associateBy { it.lowercase(Locale.US) }
        val recommendations = mutableListOf<RelatedNoteRecommendation>()

        for ((candidateId, candidate) in eligibleMap) {
            if (candidateId == anchorNote.id) continue

            var score = 0
            val reasons = mutableListOf<String>()
            val sharedTagNames = mutableListOf<String>()

            val isOutgoing = candidateId in anchorOutIds
            val isIncoming = candidateId in anchorInIds
            when {
                isOutgoing && isIncoming -> {
                    score += 8
                    reasons.add("Bidirectional wiki link")
                }
                isOutgoing -> {
                    score += 6
                    reasons.add("Linked from this note")
                }
                isIncoming -> {
                    score += 6
                    reasons.add("Links to this note")
                }
            }

            if (candidateId !in anchorDirectNeighbors && anchorDirectNeighbors.isNotEmpty()) {
                val candidateNeighbors = neighborsByNote[candidateId].orEmpty()
                val bridgeIds = anchorDirectNeighbors.intersect(candidateNeighbors)
                if (bridgeIds.isNotEmpty()) {
                    score += 3 * bridgeIds.size.coerceAtMost(2)
                    val bridgeTitle = eligibleMap[bridgeIds.first()]?.title?.ifBlank { "Untitled" } ?: "note"
                    reasons.add("Connected via [[$bridgeTitle]]")
                }
            }

            val candidateTags = parseNoteTags(candidate.tagsCsv)
            candidateTags.forEach { tag ->
                val lower = tag.lowercase(Locale.US)
                if (anchorTags.containsKey(lower)) {
                    score += 2
                    sharedTagNames.add(anchorTags.getValue(lower))
                }
            }
            if (sharedTagNames.isNotEmpty()) {
                reasons.add("Shared tags: ${sharedTagNames.joinToString(", ") { "#$it" }}")
            }

            if (!anchorNote.folderId.isNullOrBlank() && anchorNote.folderId == candidate.folderId) {
                score += 1
                val fName = candidate.folderName.ifBlank { "same folder" }
                reasons.add("Folder: $fName")
            }

            if (score > 0) {
                recommendations.add(
                    RelatedNoteRecommendation(
                        note = candidate,
                        score = score,
                        reasons = reasons,
                        sharedTags = sharedTagNames
                    )
                )
            }
        }

        return recommendations
            .sortedWith(
                compareByDescending<RelatedNoteRecommendation> { it.score }
                    .thenByDescending { it.note.updatedAtUtc }
            )
            .take(maxResults)
    }

    /**
     * Builds the Knowledge Graph geometry in normalized world coordinates `[0..1000]`
     * using deterministic seeding + force-directed stabilization on unlocked, visible notes.
     */
    fun buildKnowledgeGraph(
        notes: List<NoteEntity>,
        links: List<NoteLinkEntity>,
        filterTag: String? = null,
        includeOrphans: Boolean = true
    ): KnowledgeGraphData {
        val eligibleNotes = notes.filter { isNoteKnowledgeEligible(it) }
        val eligibleById = eligibleNotes.associateBy { it.id }

        val validEdgesRaw = links.mapNotNull { link ->
            val targetId = link.targetNoteId ?: return@mapNotNull null
            if (link.sourceNoteId == targetId) return@mapNotNull null
            if (!eligibleById.containsKey(link.sourceNoteId) || !eligibleById.containsKey(targetId)) {
                return@mapNotNull null
            }
            GraphEdge(
                id = "${link.sourceNoteId}->${targetId}",
                sourceNoteId = link.sourceNoteId,
                targetNoteId = targetId,
                label = link.targetTitle
            )
        }.distinctBy { it.id }

        val unresolvedCount = links.count {
            it.targetNoteId == null && eligibleById.containsKey(it.sourceNoteId)
        }

        val initialNodeNotes = if (!filterTag.isNullOrBlank()) {
            val taggedIds = eligibleNotes.filter { noteHasTag(it.tagsCsv, filterTag) }.map { it.id }.toSet()
            val neighborIds = validEdgesRaw
                .filter { it.sourceNoteId in taggedIds || it.targetNoteId in taggedIds }
                .flatMap { listOf(it.sourceNoteId, it.targetNoteId) }
                .toSet()
            eligibleNotes.filter { it.id in taggedIds || it.id in neighborIds }
        } else {
            eligibleNotes
        }

        val filteredEdges = validEdgesRaw.filter { edge ->
            initialNodeNotes.any { it.id == edge.sourceNoteId } &&
                initialNodeNotes.any { it.id == edge.targetNoteId }
        }

        val inDegree = mutableMapOf<String, Int>()
        val outDegree = mutableMapOf<String, Int>()
        filteredEdges.forEach { edge ->
            outDegree[edge.sourceNoteId] = (outDegree[edge.sourceNoteId] ?: 0) + 1
            inDegree[edge.targetNoteId] = (inDegree[edge.targetNoteId] ?: 0) + 1
        }

        val finalNotes = if (includeOrphans) {
            initialNodeNotes
        } else {
            val connectedIds = filteredEdges.flatMap { listOf(it.sourceNoteId, it.targetNoteId) }.toSet()
            initialNodeNotes.filter { it.id in connectedIds }
        }

        val orphanCount = initialNodeNotes.count { (inDegree[it.id] ?: 0) + (outDegree[it.id] ?: 0) == 0 }

        if (finalNotes.isEmpty()) {
            return KnowledgeGraphData(
                nodes = emptyList(),
                edges = emptyList(),
                unresolvedTargetsCount = unresolvedCount,
                orphanCount = 0
            )
        }

        val count = finalNotes.size
        val worldSize = 1000f
        val center = worldSize / 2f
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val indexById = HashMap<String, Int>(count)

        val goldenAngle = PI * (3.0 - sqrt(5.0))
        finalNotes.forEachIndexed { idx, note ->
            indexById[note.id] = idx
            val degree = (inDegree[note.id] ?: 0) + (outDegree[note.id] ?: 0)
            val baseRadius = if (count == 1) 0f else {
                val rankRatio = sqrt((idx + 1).toDouble() / count.toDouble()).toFloat()
                val ringBias = if (degree == 0) 0.92f else 0.72f
                40f + rankRatio * 340f * ringBias
            }
            val angle = idx * goldenAngle + ((note.id.hashCode() and 0xFF) / 255.0) * 0.25
            xs[idx] = center + (baseRadius * cos(angle)).toFloat()
            ys[idx] = center + (baseRadius * sin(angle)).toFloat()
        }

        if (count > 1) {
            val area = worldSize * worldSize
            val k = sqrt(area / count.toDouble()).toFloat() * 0.62f
            val dispX = FloatArray(count)
            val dispY = FloatArray(count)
            var temperature = 135f
            val iterations = if (count <= 45) 85 else 55

            for (iter in 0 until iterations) {
                dispX.fill(0f)
                dispY.fill(0f)

                // Repulsive forces between all node pairs
                for (i in 0 until count) {
                    for (j in i + 1 until count) {
                        var dx = xs[i] - xs[j]
                        var dy = ys[i] - ys[j]
                        var dist = hypot(dx, dy)
                        if (dist < 1f) {
                            dx = 1.5f
                            dy = 1.5f
                            dist = hypot(dx, dy)
                        }
                        val force = (k * k) / dist
                        val fx = (dx / dist) * force
                        val fy = (dy / dist) * force
                        dispX[i] += fx
                        dispY[i] += fy
                        dispX[j] -= fx
                        dispY[j] -= fy
                    }
                }

                // Attractive forces along resolved edges
                for (edge in filteredEdges) {
                    val u = indexById[edge.sourceNoteId] ?: continue
                    val v = indexById[edge.targetNoteId] ?: continue
                    val dx = xs[u] - xs[v]
                    val dy = ys[u] - ys[v]
                    val dist = max(1f, hypot(dx, dy))
                    val force = (dist * dist) / (k * 1.15f)
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    dispX[u] -= fx
                    dispY[u] -= fy
                    dispX[v] += fx
                    dispY[v] += fy
                }

                // Gentle centering gravity so disconnected nodes stay compact in view
                for (i in 0 until count) {
                    val gravityStrength = if ((inDegree[finalNotes[i].id] ?: 0) + (outDegree[finalNotes[i].id] ?: 0) == 0) 0.14f else 0.08f
                    dispX[i] += (center - xs[i]) * gravityStrength
                    dispY[i] += (center - ys[i]) * gravityStrength
                }

                for (i in 0 until count) {
                    val dispLen = max(0.01f, hypot(dispX[i], dispY[i]))
                    val step = min(dispLen, temperature)
                    xs[i] = (xs[i] + (dispX[i] / dispLen) * step).coerceIn(70f, worldSize - 70f)
                    ys[i] = (ys[i] + (dispY[i] / dispLen) * step).coerceIn(70f, worldSize - 70f)
                }

                temperature *= 0.94f
            }
        }

        val graphNodes = finalNotes.mapIndexed { idx, note ->
            val inDeg = inDegree[note.id] ?: 0
            val outDeg = outDegree[note.id] ?: 0
            val parsedTags = parseNoteTags(note.tagsCsv)
            GraphNode(
                noteId = note.id,
                label = note.title.ifBlank { "Untitled" },
                subtitle = note.content.replace("\n", " ").trim().take(72),
                tags = parsedTags,
                folderName = note.folderName,
                colorHex = note.colorHex,
                noteType = note.noteType,
                isJournal = isDailyJournalNote(note),
                isPinned = note.isPinned,
                incomingCount = inDeg,
                outgoingCount = outDeg,
                degree = inDeg + outDeg,
                x = xs[idx],
                y = ys[idx]
            )
        }

        return KnowledgeGraphData(
            nodes = graphNodes,
            edges = filteredEdges,
            unresolvedTargetsCount = unresolvedCount,
            orphanCount = orphanCount
        )
    }

    // ==========================================
    // DAILY JOURNAL DATE-BASED HELPERS
    // ==========================================

    private val isoDateRegex = Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b")

    fun journalDateKeyFromMillis(utcMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(utcMillis))
    }

    fun millisFromJournalDateKey(dateKey: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val parsed = sdf.parse(dateKey.trim()) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    fun journalDisplayTitleForDateKey(dateKey: String): String {
        val millis = millisFromJournalDateKey(dateKey) ?: System.currentTimeMillis()
        val display = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
        return "Diary - $display"
    }

    fun journalHeaderSubtitleForDateKey(dateKey: String): String {
        val millis = millisFromJournalDateKey(dateKey) ?: return dateKey
        return SimpleDateFormat("EEEE · MMMM d, yyyy", Locale.getDefault()).format(Date(millis))
    }

    fun isDailyJournalNote(note: NoteEntity): Boolean {
        if (note.isDeleted || note.isLocked) return false
        if (SmartNoteUtils.parseJournalDateKey(note.canvasDataJson).isNotBlank()) return true
        if (noteHasTag(note.tagsCsv, "Diary") || noteHasTag(note.tagsCsv, "Journal")) return true
        return note.title.startsWith("Diary - ", ignoreCase = true)
    }

    fun extractJournalDateKey(note: NoteEntity): String? {
        if (note.isDeleted || note.isLocked) return null
        val explicitKey = SmartNoteUtils.parseJournalDateKey(note.canvasDataJson)
        if (explicitKey.isNotBlank()) return explicitKey

        val isoMatch = isoDateRegex.find(note.title)
        if (isoMatch != null) return isoMatch.groupValues[1]

        if (note.title.startsWith("Diary - ", ignoreCase = true)) {
            val datePart = note.title.substringAfter("Diary - ", "").trim()
            if (datePart.isNotBlank()) {
                val patterns = listOf("MMM d, yyyy", "MMMM d, yyyy", "yyyy-MM-dd")
                for (pattern in patterns) {
                    val parsed = runCatching {
                        SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }.parse(datePart)
                    }.getOrNull() ?: runCatching {
                        SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(datePart)
                    }.getOrNull()
                    if (parsed != null) {
                        return journalDateKeyFromMillis(parsed.time)
                    }
                }
            }
        }

        return if (noteHasTag(note.tagsCsv, "Diary") || noteHasTag(note.tagsCsv, "Journal")) {
            journalDateKeyFromMillis(note.createdAtUtc)
        } else {
            null
        }
    }

    // ==========================================
    // REUSABLE NOTE TEMPLATES & PLACEHOLDERS
    // ==========================================

    fun builtInTemplates(seedTimeUtc: Long = 1_700_000_000_000L): List<NoteTemplateEntity> {
        return listOf(
            NoteTemplateEntity(
                id = "template_builtin_daily_journal",
                name = "Daily Reflection",
                description = "Structured date-based check-in with intentions, wins, and backlinks.",
                category = "Journal",
                titlePattern = "Diary - {{date}}",
                contentTemplate = """
# Diary - {{date}}
*{{day}} · {{time}}*

### Morning Intentions
- 

### Key Wins & Observations
- 

### Connected Notes
- [[Welcome to NoteLand]]

### Evening Reflection
- 
                """.trimIndent(),
                defaultTagsCsv = "Diary",
                defaultColorHex = "#FFEDD5",
                checklistTemplateJson = """[{"id":"tpl_dj_1","text":"Set top 3 priorities","done":false},{"id":"tpl_dj_2","text":"Evening gratitude check-in","done":false}]""",
                isBuiltIn = true,
                createdAtUtc = seedTimeUtc,
                updatedAtUtc = seedTimeUtc
            ),
            NoteTemplateEntity(
                id = "template_builtin_zettelkasten",
                name = "Zettelkasten Concept",
                description = "Atomic evergreen note linking core ideas, sources, and related thoughts.",
                category = "Knowledge",
                titlePattern = "{{title}}",
                contentTemplate = """
# {{title}}

### Core Idea
> Summarize this concept in your own words.

### Key Arguments
- 

### Related Concepts
- [[]]

### Source & References
- 
                """.trimIndent(),
                defaultTagsCsv = "Idea,Knowledge",
                defaultColorHex = "#EFF6FF",
                checklistTemplateJson = "",
                isBuiltIn = true,
                createdAtUtc = seedTimeUtc,
                updatedAtUtc = seedTimeUtc
            ),
            NoteTemplateEntity(
                id = "template_builtin_meeting_notes",
                name = "Meeting Brief",
                description = "Agenda, decisions, action items, and follow-up links.",
                category = "Work",
                titlePattern = "Meeting: {{title}} ({{date}})",
                contentTemplate = """
# Meeting Notes · {{date}}

**Topic:** {{title}}
**Related Project:** [[]]

### Agenda
1. 

### Decisions Made
- 

### Next Steps & Follow-Ups
- 
                """.trimIndent(),
                defaultTagsCsv = "Work,Checklist",
                defaultColorHex = "#F0FDF4",
                checklistTemplateJson = """[{"id":"tpl_mt_1","text":"Share decision summary","done":false},{"id":"tpl_mt_2","text":"Schedule follow-up review","done":false}]""",
                isBuiltIn = true,
                createdAtUtc = seedTimeUtc,
                updatedAtUtc = seedTimeUtc
            ),
            NoteTemplateEntity(
                id = "template_builtin_book_notes",
                name = "Book & Reading Notes",
                description = "Capture thesis, memorable quotes, and synthesize takeaways.",
                category = "Knowledge",
                titlePattern = "Reading: {{title}}",
                contentTemplate = """
# {{title}}

### Main Thesis
- 

### Memorable Quotes
> 

### Connections to Other Ideas
- [[]]
                """.trimIndent(),
                defaultTagsCsv = "Reading",
                defaultColorHex = "#FAF5FF",
                checklistTemplateJson = "",
                isBuiltIn = true,
                createdAtUtc = seedTimeUtc,
                updatedAtUtc = seedTimeUtc
            ),
            NoteTemplateEntity(
                id = "template_builtin_project_plan",
                name = "Project Blueprint",
                description = "Scope, milestones, risks, and linked architecture specs.",
                category = "Work",
                titlePattern = "{{title}} Blueprint",
                contentTemplate = """
# {{title}} Blueprint

### Objective
- 

### Architecture & Dependencies
- [[Weekly Project Roadmap]]

### Milestones
- [ ] Define core requirements
- [ ] Build & verify prototype
                """.trimIndent(),
                defaultTagsCsv = "Project,Important",
                defaultColorHex = "#FEF3C7",
                checklistTemplateJson = """[{"id":"tpl_pj_1","text":"Define core requirements","done":false},{"id":"tpl_pj_2","text":"Review security & edge cases","done":false}]""",
                isBuiltIn = true,
                createdAtUtc = seedTimeUtc,
                updatedAtUtc = seedTimeUtc
            )
        )
    }

    fun expandTemplatePattern(
        pattern: String,
        titleInput: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val dateDisplay = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(nowMillis))
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(nowMillis))
        val timeDisplay = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nowMillis))
        val safeTitle = titleInput.trim().ifBlank { "Untitled Note" }

        return pattern
            .replace("{{date}}", dateDisplay)
            .replace("{{iso_date}}", isoDate)
            .replace("{{day}}", dayName)
            .replace("{{time}}", timeDisplay)
            .replace("{{title}}", safeTitle)
    }

    // ==========================================
    // UNIVERSAL SEARCH ENGINE
    // ==========================================

    fun universalSearchNotes(
        query: String,
        allNotes: List<NoteEntity>,
        allLinks: List<NoteLinkEntity>,
        typeFilter: NoteType? = null,
        pinnedOnly: Boolean = false,
        hasChecklistOnly: Boolean = false,
        favoritesOnly: Boolean = false,
        sortOrder: String = "UPDATED_DESC"
    ): List<UniversalNoteMatch> {
        val eligibleNotes = allNotes.filter { isNoteKnowledgeEligible(it) }
        val eligibleById = eligibleNotes.associateBy { it.id }

        val normalizedQuery = query.trim()
        val hasQuery = normalizedQuery.isNotEmpty()
        val tagFilterToken = if (normalizedQuery.startsWith("#") && normalizedQuery.length > 1) {
            normalizedQuery.removePrefix("#").trim()
        } else null

        val outgoingTargetsBySource = mutableMapOf<String, MutableList<String>>()
        val incomingSourcesByTarget = mutableMapOf<String, MutableList<String>>()
        allLinks.forEach { link ->
            if (!eligibleById.containsKey(link.sourceNoteId)) return@forEach
            outgoingTargetsBySource.getOrPut(link.sourceNoteId) { mutableListOf() }.add(link.targetTitle)
            val tId = link.targetNoteId
            if (tId != null && eligibleById.containsKey(tId)) {
                val srcTitle = eligibleById[link.sourceNoteId]?.title?.ifBlank { "Untitled" } ?: return@forEach
                incomingSourcesByTarget.getOrPut(tId) { mutableListOf() }.add(srcTitle)
            }
        }

        val matches = mutableListOf<UniversalNoteMatch>()
        for (note in eligibleNotes) {
            if (typeFilter == null && note.noteType == NoteType.PDF_DOC.name) continue
            if (typeFilter != null && note.noteType != typeFilter.name) continue
            if (pinnedOnly && !note.isPinned) continue
            if (favoritesOnly && !note.isFavorite) continue
            if (hasChecklistOnly && (note.checklistJson.isBlank() || note.checklistJson == "[]")) continue

            if (!hasQuery) {
                matches.add(UniversalNoteMatch(note = note, score = 1, matchReasons = emptyList()))
                continue
            }

            var score = 0
            val reasons = mutableListOf<String>()

            if (note.title.contains(normalizedQuery, ignoreCase = true)) {
                score += 10
                reasons.add("Title match")
            }

            val parsedTags = parseNoteTags(note.tagsCsv)
            val tagMatch = parsedTags.firstOrNull { tag ->
                if (tagFilterToken != null) {
                    tag.contains(tagFilterToken, ignoreCase = true)
                } else {
                    tag.contains(normalizedQuery, ignoreCase = true)
                }
            }
            if (tagMatch != null) {
                score += 7
                reasons.add("Tag #$tagMatch")
            }

            if (tagFilterToken == null && note.folderName.contains(normalizedQuery, ignoreCase = true)) {
                score += 5
                reasons.add("Folder ${note.folderName}")
            }

            if (tagFilterToken == null && note.content.contains(normalizedQuery, ignoreCase = true)) {
                score += 5
                reasons.add("Note content")
            }

            if (tagFilterToken == null && note.checklistJson.isNotBlank()) {
                val checklistHit = SmartNoteUtils.parseChecklistJson(note.checklistJson)
                    .any { it.text.contains(normalizedQuery, ignoreCase = true) }
                if (checklistHit) {
                    score += 4
                    reasons.add("Checklist item")
                }
            }

            val outHit = outgoingTargetsBySource[note.id].orEmpty()
                .firstOrNull { it.contains(normalizedQuery, ignoreCase = true) }
            if (outHit != null) {
                score += 6
                reasons.add("Links to [[$outHit]]")
            }

            val inHit = incomingSourcesByTarget[note.id].orEmpty()
                .firstOrNull { it.contains(normalizedQuery, ignoreCase = true) }
            if (inHit != null) {
                score += 6
                reasons.add("Linked from [[$inHit]]")
            }

            if (score > 0) {
                matches.add(
                    UniversalNoteMatch(
                        note = note,
                        score = score,
                        matchReasons = reasons.distinct()
                    )
                )
            }
        }

        return when (sortOrder) {
            "CREATED_DESC" -> matches.sortedByDescending { it.note.createdAtUtc }
            "TITLE_ASC" -> matches.sortedBy { it.note.title.lowercase(Locale.US) }
            "TITLE_DESC" -> matches.sortedByDescending { it.note.title.lowercase(Locale.US) }
            else -> if (hasQuery) {
                matches.sortedWith(
                    compareByDescending<UniversalNoteMatch> { it.score }
                        .thenByDescending { it.note.updatedAtUtc }
                )
            } else {
                matches.sortedByDescending { it.note.updatedAtUtc }
            }
        }
    }

    fun universalSearchTags(
        query: String,
        allNotes: List<NoteEntity>,
        tagEntities: List<TagEntity>
    ): List<UniversalTagMatch> {
        val eligibleNotes = allNotes.filter { isNoteKnowledgeEligible(it) }
        val countsByLower = mutableMapOf<String, Pair<String, Int>>()
        val colorsByLower = tagEntities.associate { it.name.lowercase(Locale.US) to it.colorHex }

        eligibleNotes.forEach { note ->
            parseNoteTags(note.tagsCsv).forEach { tag ->
                val lower = tag.lowercase(Locale.US)
                val existing = countsByLower[lower]
                countsByLower[lower] = (existing?.first ?: tag) to ((existing?.second ?: 0) + 1)
            }
        }
        tagEntities.forEach { entity ->
            val lower = entity.name.trim().lowercase(Locale.US)
            if (lower.isNotBlank() && !countsByLower.containsKey(lower)) {
                countsByLower[lower] = entity.name.trim() to 0
            }
        }

        val cleanQuery = query.trim().removePrefix("#")
        return countsByLower.values
            .filter { (name, count) ->
                if (cleanQuery.isBlank()) count > 0 else name.contains(cleanQuery, ignoreCase = true)
            }
            .map { (name, count) ->
                UniversalTagMatch(
                    name = name,
                    colorHex = colorsByLower[name.lowercase(Locale.US)] ?: "#6366F1",
                    noteCount = count
                )
            }
            .sortedWith(compareByDescending<UniversalTagMatch> { it.noteCount }.thenBy { it.name.lowercase(Locale.US) })
    }
}
