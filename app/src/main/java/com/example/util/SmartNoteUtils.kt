package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

data class CalendarEventMeta(
    val startUtc: Long,
    val endUtc: Long,
    val category: String = "Focus"
)

enum class NoteInkTool {
    FINE_PEN,
    PRESSURE_PEN,
    HIGHLIGHTER,
    ERASER
}

data class InkSample(
    val xDp: Float,
    val yDp: Float,
    val pressure: Float = 0.5f
)

data class InkStroke(
    val id: String = UUID.randomUUID().toString(),
    val tool: String = NoteInkTool.PRESSURE_PEN.name,
    val colorArgb: Int,
    val baseWidthDp: Float = 2.6f,
    val points: List<InkSample>
)

data class VoiceRecordingClip(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val title: String = "",
    val durationMs: Long = 0L,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val waveform: List<Float> = emptyList()
)

enum class NoteBlockType {
    IMAGES,
    VOICE,
    TEXT,
    CHECKLIST
}

object SmartNoteUtils {

    val DEFAULT_NOTE_BLOCK_ORDER: List<NoteBlockType> = listOf(
        NoteBlockType.IMAGES,
        NoteBlockType.VOICE,
        NoteBlockType.TEXT,
        NoteBlockType.CHECKLIST
    )

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun countCharacters(text: String, includeSpaces: Boolean = true): Int {
        return if (includeSpaces) text.length else text.replace("\\s".toRegex(), "").length
    }

    fun calculateReadingTimeMinutes(text: String, wordsPerMinute: Int = 200): Int {
        val words = countWords(text)
        return (words / wordsPerMinute).coerceAtLeast(1)
    }

    fun formatReadingTime(text: String): String {
        val words = countWords(text)
        if (words == 0) return "< 1 min read"
        val minutes = (words / 200).coerceAtLeast(1)
        return "$minutes min read ($words words)"
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            diff < 60_000 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> formatShortDate(timestamp)
        }
    }

    fun parseChecklistJson(jsonStr: String): List<ChecklistItem> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<ChecklistItem>()
            val seenIds = HashSet<String>()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = obj.optString("text", "")
                val done = obj.optBoolean("done", false)
                val rawId = obj.optString("id", "").trim()
                val stableId = if (rawId.isNotBlank() && seenIds.add(rawId)) {
                    rawId
                } else {
                    var candidate = "item_$i"
                    var suffix = 1
                    while (!seenIds.add(candidate)) {
                        candidate = "item_${i}_$suffix"
                        suffix++
                    }
                    candidate
                }
                list.add(ChecklistItem(id = stableId, text = text, isDone = done))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun encodeChecklistJson(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEachIndexed { idx, item ->
            val obj = JSONObject()
            val stableId = item.id.takeIf { it.isNotBlank() } ?: "item_$idx"
            obj.put("id", stableId)
            obj.put("text", item.text)
            obj.put("done", item.isDone)
            array.put(obj)
        }
        return array.toString()
    }

    fun calculateChecklistProgress(items: List<ChecklistItem>): Pair<Int, Int> {
        val total = items.size
        val done = items.count { it.isDone }
        return Pair(done, total)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    // ==========================================
    // OFFLINE DETERMINISTIC SMART UTILITIES
    // ==========================================

    fun countParagraphs(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("\n\n+".toRegex()).count { it.isNotBlank() }
    }

    fun countSentences(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("[.!?]+".toRegex()).count { it.isNotBlank() }
    }

    private val commonStopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
        "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't",
        "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have",
        "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
        "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into",
        "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our",
        "ours", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's",
        "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs",
        "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
        "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't",
        "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
        "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
        "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself"
    )

    /**
     * Deterministic Extractive Summarizer:
     * Scores sentences based on term frequencies and positional prominence,
     * returning the most significant 2-3 sentences.
     */
    fun generateExtractiveSummary(text: String, maxSentences: Int = 3): String {
        if (text.isBlank()) return ""
        val cleanText = text.replace("```[\\s\\S]*?```".toRegex(), "") // strip code blocks
            .replace("#+ ".toRegex(), "") // strip headers
        val rawSentences = cleanText.split("(?<=[.!?\\n])\\s+".toRegex()).map { it.trim() }.filter { it.length > 15 }

        if (rawSentences.size <= maxSentences) {
            return rawSentences.joinToString(" ")
        }

        // Frequency map of non-stop words
        val wordFreq = mutableMapOf<String, Int>()
        val words = cleanText.lowercase().split("[^a-zA-Z0-9]+".toRegex()).filter { it.length > 2 && it !in commonStopWords }
        for (w in words) {
            wordFreq[w] = (wordFreq[w] ?: 0) + 1
        }

        // Score sentences
        val scored = rawSentences.mapIndexed { index, sentence ->
            val sWords = sentence.lowercase().split("[^a-zA-Z0-9]+".toRegex()).filter { it in wordFreq }
            var score = sWords.sumOf { wordFreq[it] ?: 0 }.toFloat() / Math.sqrt(sentence.length.toDouble().coerceAtLeast(1.0)).toFloat()
            // Positional bonus for early intro sentences
            if (index == 0) score *= 1.4f
            else if (index == 1) score *= 1.2f
            Pair(index, score)
        }

        val topIndices = scored.sortedByDescending { it.second }
            .take(maxSentences)
            .map { it.first }
            .sorted()

        return topIndices.joinToString(" ") { rawSentences[it] }
    }

    /**
     * Automatic Checklist Item Extraction:
     * Detects markdown task items, bullet points, action verbs, and numbered lists.
     */
    fun extractChecklistItems(text: String): List<ChecklistItem> {
        val lines = text.lines()
        val items = mutableListOf<ChecklistItem>()

        val actionVerbs = setOf("buy", "review", "call", "send", "email", "complete", "finish", "submit", "prepare", "write", "clean", "fix", "update", "check", "verify", "organize", "read", "schedule", "test")

        lines.forEach { line ->
            val trimmed = line.trim()
            var extractedText: String? = null
            var isDone = false

            when {
                trimmed.startsWith("- [ ]") -> {
                    extractedText = trimmed.removePrefix("- [ ]").trim()
                }
                trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]") -> {
                    extractedText = trimmed.substring(5).trim()
                    isDone = true
                }
                trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ") -> {
                    extractedText = trimmed.substring(2).trim()
                }
                trimmed.matches("^\\d+\\.\\s+.*".toRegex()) -> {
                    extractedText = trimmed.replaceFirst("^\\d+\\.\\s+".toRegex(), "").trim()
                }
                trimmed.startsWith("TODO:", ignoreCase = true) -> {
                    extractedText = trimmed.substring(5).trim()
                }
                else -> {
                    val firstWord = trimmed.split("\\s+".toRegex()).firstOrNull()?.lowercase() ?: ""
                    if (firstWord in actionVerbs && trimmed.length in 8..80) {
                        extractedText = trimmed
                    }
                }
            }

            if (!extractedText.isNullOrBlank() && extractedText.length >= 3) {
                items.add(ChecklistItem(id = UUID.randomUUID().toString(), text = extractedText, isDone = isDone))
            }
        }
        return items
    }

    /**
     * Deterministic Title Suggestions based on heading, first sentence, or topical keywords.
     */
    fun suggestTitles(content: String, currentTitle: String = ""): List<String> {
        val suggestions = mutableListOf<String>()

        // 1. Heading extraction (# Heading)
        val headingMatch = "^#{1,3}\\s+(.*)".toRegex(RegexOption.MULTILINE).find(content)
        if (headingMatch != null) {
            val h = headingMatch.groupValues[1].trim()
            if (h.isNotBlank() && h != currentTitle) suggestions.add(h)
        }

        // 2. First significant sentence or phrase
        val firstLine = content.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim() ?: ""
        if (firstLine.isNotBlank()) {
            val sentence = firstLine.split("[.!?]".toRegex()).first().trim()
            val candidate = if (sentence.length > 40) sentence.take(38) + "…" else sentence
            if (candidate.isNotBlank() && candidate != currentTitle && candidate !in suggestions) {
                suggestions.add(candidate)
            }
        }

        // 3. Keyword-based topical title
        val words = content.lowercase().split("[^a-zA-Z0-9]+".toRegex())
            .filter { it.length > 3 && it !in commonStopWords }
        val topWords = words.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key.replaceFirstChar { c -> c.uppercase() } }

        if (topWords.isNotEmpty()) {
            val topical = "${topWords.joinToString(" & ")} Notes"
            if (topical != currentTitle && topical !in suggestions) {
                suggestions.add(topical)
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Quick Note")
        }

        return suggestions.distinct()
    }

    /**
     * Duplicate Content Detection:
     * Calculates Jaccard token similarity against existing notes to prevent duplicate efforts.
     */
    data class DuplicateCheckResult(
        val isDuplicate: Boolean,
        val similarityPercent: Int,
        val matchedNoteTitle: String? = null,
        val matchedNoteId: String? = null
    )

    fun detectDuplicateContent(
        content: String,
        currentNoteId: String,
        existingNotes: List<com.example.data.model.NoteEntity>,
        threshold: Float = 0.50f
    ): DuplicateCheckResult {
        if (content.length < 30) return DuplicateCheckResult(false, 0)

        val targetTokens = content.lowercase().split("[^a-zA-Z0-9]+".toRegex())
            .filter { it.length > 2 && it !in commonStopWords }
            .toSet()

        if (targetTokens.size < 5) return DuplicateCheckResult(false, 0)

        var maxSimilarity = 0f
        var bestMatch: com.example.data.model.NoteEntity? = null

        for (other in existingNotes) {
            if (other.id == currentNoteId || other.content.isBlank()) continue
            val otherTokens = other.content.lowercase().split("[^a-zA-Z0-9]+".toRegex())
                .filter { it.length > 2 && it !in commonStopWords }
                .toSet()

            if (otherTokens.isEmpty()) continue

            val intersection = targetTokens.intersect(otherTokens).size
            val union = targetTokens.union(otherTokens).size
            val jaccard = if (union > 0) intersection.toFloat() / union.toFloat() else 0f

            if (jaccard > maxSimilarity) {
                maxSimilarity = jaccard
                bestMatch = other
            }
        }

        val pct = (maxSimilarity * 100).toInt()
        val isDup = maxSimilarity >= threshold && bestMatch != null
        return DuplicateCheckResult(
            isDuplicate = isDup,
            similarityPercent = pct,
            matchedNoteTitle = bestMatch?.title,
            matchedNoteId = bestMatch?.id
        )
    }

    /**
     * Offline Organization Suggestions:
     * Suggests relevant folders and tags based on note keywords.
     */
    data class OrganizationSuggestion(
        val suggestedFolderId: String?,
        val suggestedFolderName: String?,
        val suggestedTags: List<String>
    )

    fun suggestOrganization(
        content: String,
        availableFolders: List<com.example.data.model.FolderEntity>
    ): OrganizationSuggestion {
        val lower = content.lowercase()

        // Folder matching
        var matchedFolder: com.example.data.model.FolderEntity? = null
        for (folder in availableFolders) {
            if (lower.contains(folder.name.lowercase())) {
                matchedFolder = folder
                break
            }
        }

        if (matchedFolder == null) {
            when {
                lower.contains("meeting") || lower.contains("client") || lower.contains("sprint") || lower.contains("roadmap") -> {
                    matchedFolder = availableFolders.firstOrNull { it.name.contains("Project", ignoreCase = true) || it.name.contains("Work", ignoreCase = true) }
                }
                lower.contains("study") || lower.contains("exam") || lower.contains("class") || lower.contains("lecture") || lower.contains("chapter") -> {
                    matchedFolder = availableFolders.firstOrNull { it.name.contains("Study", ignoreCase = true) || it.name.contains("Personal", ignoreCase = true) }
                }
                lower.contains("idea") || lower.contains("brainstorm") || lower.contains("sketch") || lower.contains("concept") -> {
                    matchedFolder = availableFolders.firstOrNull { it.name.contains("Creative", ignoreCase = true) || it.name.contains("Idea", ignoreCase = true) }
                }
            }
        }

        // Tag matching
        val suggestedTags = mutableListOf<String>()
        if (lower.contains("important") || lower.contains("urgent") || lower.contains("priority")) suggestedTags.add("Important")
        if (lower.contains("todo") || lower.contains("task") || lower.contains("checklist")) suggestedTags.add("Checklist")
        if (lower.contains("reading") || lower.contains("book") || lower.contains("article")) suggestedTags.add("Reading")
        if (lower.contains("idea") || lower.contains("vision")) suggestedTags.add("Idea")
        if (lower.contains("code") || lower.contains("api") || lower.contains("database")) suggestedTags.add("Dev")

        return OrganizationSuggestion(
            suggestedFolderId = matchedFolder?.id,
            suggestedFolderName = matchedFolder?.name,
            suggestedTags = suggestedTags.distinct()
        )
    }

    // ==========================================
    // TIME-BASED GREETINGS & CALENDAR HELPERS
    // ==========================================

    fun getTimeBasedGreeting(
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): String {
        return when (hourOfDay) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatFullDateHeader(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeRange(startUtc: Long, endUtc: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "${sdf.format(Date(startUtc))} - ${sdf.format(Date(endUtc))}"
    }

    fun formatTimeMarker(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm\na", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun startOfDayUtc(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun getWeekDaysStartingMonday(anchorUtc: Long): List<Long> {
        val noonCal = Calendar.getInstance().apply {
            timeInMillis = anchorUtc
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val offsetFromMonday = when (dayOfWeek) {
                Calendar.SUNDAY -> -6
                else -> Calendar.MONDAY - dayOfWeek
            }
            add(Calendar.DAY_OF_MONTH, offsetFromMonday)
        }
        return List(7) {
            val dayStart = startOfDayUtc(noonCal.timeInMillis)
            noonCal.add(Calendar.DAY_OF_MONTH, 1)
            dayStart
        }
    }

    fun encodeCalendarMeta(startUtc: Long, endUtc: Long, category: String): String {
        return JSONObject().apply {
            put("calendarStartUtc", startUtc)
            put("calendarEndUtc", endUtc)
            put("calendarCategory", category)
        }.toString()
    }

    fun parseCalendarMeta(jsonStr: String): CalendarEventMeta? {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return null
        return try {
            val obj = JSONObject(jsonStr)
            if (!obj.has("calendarStartUtc")) return null
            val start = obj.getLong("calendarStartUtc")
            val end = obj.optLong("calendarEndUtc", start + 3600_000L)
            val cat = obj.optString("calendarCategory", "Focus")
            CalendarEventMeta(startUtc = start, endUtc = end, category = cat)
        } catch (_: Exception) {
            null
        }
    }

    fun parseNoteTexture(jsonStr: String): String {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return ""
        return try {
            JSONObject(jsonStr).optString("noteTexture", "")
        } catch (_: Exception) {
            ""
        }
    }

    fun parseNoteBackgroundImage(jsonStr: String): String {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return ""
        return try {
            JSONObject(jsonStr).optString("noteBackgroundImage", "")
        } catch (_: Exception) {
            ""
        }
    }

    fun withNoteTexture(existingJson: String, textureKey: String): String {
        return withNoteStyleMeta(
            existingJson = existingJson,
            textureKey = textureKey,
            backgroundImagePath = parseNoteBackgroundImage(existingJson)
        )
    }

    fun withNoteStyleMeta(
        existingJson: String,
        textureKey: String,
        backgroundImagePath: String
    ): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            if (textureKey.isBlank() || textureKey == "none") {
                obj.remove("noteTexture")
            } else {
                obj.put("noteTexture", textureKey)
            }
            if (backgroundImagePath.isBlank()) {
                obj.remove("noteBackgroundImage")
            } else {
                obj.put("noteBackgroundImage", backgroundImagePath)
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            val obj = JSONObject()
            if (textureKey.isNotBlank() && textureKey != "none") obj.put("noteTexture", textureKey)
            if (backgroundImagePath.isNotBlank()) obj.put("noteBackgroundImage", backgroundImagePath)
            if (obj.length() == 0) "" else obj.toString()
        }
    }

    fun parseJournalDateKey(jsonStr: String): String {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return ""
        return try {
            JSONObject(jsonStr).optString("journalDateKey", "")
        } catch (_: Exception) {
            ""
        }
    }

    fun withJournalDateKey(existingJson: String, dateKey: String): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            if (dateKey.isBlank()) {
                obj.remove("journalDateKey")
            } else {
                obj.put("journalDateKey", dateKey.trim())
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            existingJson
        }
    }

    fun hasHandwritingStrokes(jsonStr: String): Boolean {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return false
        return try {
            val arr = JSONObject(jsonStr).optJSONArray("handwritingStrokes")
            arr != null && arr.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    fun parseHandwritingStrokes(jsonStr: String): List<InkStroke> {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return emptyList()
        return try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("handwritingStrokes") ?: return emptyList()
            val result = ArrayList<InkStroke>(arr.length())
            for (i in 0 until arr.length()) {
                val sObj = arr.optJSONObject(i) ?: continue
                val ptsArr = sObj.optJSONArray("pts") ?: continue
                val samples = ArrayList<InkSample>(ptsArr.length())
                for (j in 0 until ptsArr.length()) {
                    val tuple = ptsArr.optJSONArray(j) ?: continue
                    if (tuple.length() >= 2) {
                        val x = tuple.optDouble(0, 0.0).toFloat()
                        val y = tuple.optDouble(1, 0.0).toFloat()
                        val p = if (tuple.length() >= 3) tuple.optDouble(2, 0.5).toFloat() else 0.5f
                        samples.add(InkSample(xDp = x, yDp = y, pressure = p.coerceIn(0f, 1f)))
                    }
                }
                if (samples.isNotEmpty()) {
                    result.add(
                        InkStroke(
                            id = sObj.optString("id", UUID.randomUUID().toString()),
                            tool = sObj.optString("t", NoteInkTool.PRESSURE_PEN.name),
                            colorArgb = sObj.optInt("c", 0),
                            baseWidthDp = sObj.optDouble("w", 2.6).toFloat().coerceIn(0.6f, 36f),
                            points = samples
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun withHandwritingStrokes(existingJson: String, strokes: List<InkStroke>): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            if (strokes.isEmpty()) {
                obj.remove("handwritingStrokes")
            } else {
                val arr = JSONArray()
                strokes.forEach { stroke ->
                    if (stroke.points.isNotEmpty()) {
                        val sObj = JSONObject()
                        sObj.put("id", stroke.id)
                        sObj.put("t", stroke.tool)
                        sObj.put("c", stroke.colorArgb)
                        sObj.put("w", ((stroke.baseWidthDp * 100f).roundToInt() / 100.0))
                        val ptsArr = JSONArray()
                        stroke.points.forEach { pt ->
                            val tuple = JSONArray()
                            tuple.put((pt.xDp * 100f).roundToInt() / 100.0)
                            tuple.put((pt.yDp * 100f).roundToInt() / 100.0)
                            tuple.put((pt.pressure.coerceIn(0f, 1f) * 100f).roundToInt() / 100.0)
                            ptsArr.put(tuple)
                        }
                        sObj.put("pts", ptsArr)
                        arr.put(sObj)
                    }
                }
                if (arr.length() == 0) {
                    obj.remove("handwritingStrokes")
                } else {
                    obj.put("handwritingStrokes", arr)
                }
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            existingJson
        }
    }

    fun formatAudioDuration(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun hasVoiceRecordings(jsonStr: String): Boolean {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return false
        return try {
            val arr = JSONObject(jsonStr).optJSONArray("voiceRecordings")
            arr != null && arr.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    fun parseVoiceRecordings(jsonStr: String): List<VoiceRecordingClip> {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return emptyList()
        return try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("voiceRecordings") ?: return emptyList()
            val result = ArrayList<VoiceRecordingClip>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val filePath = item.optString("filePath", "").trim()
                if (filePath.isBlank()) continue
                val wfArr = item.optJSONArray("waveform")
                val waveform = if (wfArr != null) {
                    List(wfArr.length()) { idx ->
                        wfArr.optDouble(idx, 0.25).toFloat().coerceIn(0f, 1f)
                    }
                } else {
                    emptyList()
                }
                result.add(
                    VoiceRecordingClip(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        filePath = filePath,
                        title = item.optString("title", ""),
                        durationMs = item.optLong("durationMs", 0L).coerceAtLeast(0L),
                        createdAtUtc = item.optLong("createdAtUtc", System.currentTimeMillis()),
                        waveform = waveform
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun withVoiceRecordings(existingJson: String, clips: List<VoiceRecordingClip>): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            if (clips.isEmpty()) {
                obj.remove("voiceRecordings")
            } else {
                val arr = JSONArray()
                clips.forEach { clip ->
                    if (clip.filePath.isNotBlank()) {
                        val cObj = JSONObject()
                        cObj.put("id", clip.id)
                        cObj.put("filePath", clip.filePath)
                        cObj.put("title", clip.title)
                        cObj.put("durationMs", clip.durationMs.coerceAtLeast(0L))
                        cObj.put("createdAtUtc", clip.createdAtUtc)
                        val wfArr = JSONArray()
                        clip.waveform.forEach { sample ->
                            wfArr.put(((sample.coerceIn(0f, 1f) * 100f).roundToInt() / 100.0))
                        }
                        cObj.put("waveform", wfArr)
                        arr.put(cObj)
                    }
                }
                if (arr.length() == 0) {
                    obj.remove("voiceRecordings")
                } else {
                    obj.put("voiceRecordings", arr)
                }
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            existingJson
        }
    }

    fun parseBlockOrder(jsonStr: String): List<NoteBlockType> {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) {
            return DEFAULT_NOTE_BLOCK_ORDER
        }
        return try {
            val arr = JSONObject(jsonStr).optJSONArray("blockOrder") ?: return DEFAULT_NOTE_BLOCK_ORDER
            val seen = LinkedHashSet<NoteBlockType>()
            for (i in 0 until arr.length()) {
                val rawName = arr.optString(i, "")
                val parsed = NoteBlockType.entries.firstOrNull { it.name == rawName }
                if (parsed != null) {
                    seen.add(parsed)
                }
            }
            DEFAULT_NOTE_BLOCK_ORDER.forEach { block -> seen.add(block) }
            seen.toList()
        } catch (_: Exception) {
            DEFAULT_NOTE_BLOCK_ORDER
        }
    }

    fun withBlockOrder(existingJson: String, order: List<NoteBlockType>): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        val normalized = LinkedHashSet<NoteBlockType>().apply {
            addAll(order)
            addAll(DEFAULT_NOTE_BLOCK_ORDER)
        }.toList()
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            if (normalized == DEFAULT_NOTE_BLOCK_ORDER) {
                obj.remove("blockOrder")
            } else {
                val arr = JSONArray()
                normalized.forEach { arr.put(it.name) }
                obj.put("blockOrder", arr)
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            existingJson
        }
    }

    fun parseRichDocumentJson(jsonStr: String): String {
        if (jsonStr.isBlank() || !jsonStr.trimStart().startsWith("{")) return ""
        return try {
            val root = JSONObject(jsonStr)
            when {
                root.has("richDocument") -> {
                    val value = root.opt("richDocument")
                    when (value) {
                        is JSONObject -> value.toString()
                        is String -> value
                        else -> ""
                    }
                }
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun withRichDocumentJson(existingJson: String, richDocumentJson: String): String {
        val trimmed = existingJson.trim()
        if (trimmed.startsWith("[")) {
            return existingJson
        }
        return try {
            val obj = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            val cleanRich = richDocumentJson.trim()
            if (cleanRich.isBlank()) {
                obj.remove("richDocument")
            } else if (cleanRich.startsWith("{")) {
                obj.put("richDocument", JSONObject(cleanRich))
            } else {
                // Supports encrypted payload strings when a note is locked in Vault
                obj.put("richDocument", cleanRich)
            }
            if (obj.length() == 0) "" else obj.toString()
        } catch (_: Exception) {
            existingJson
        }
    }
}



