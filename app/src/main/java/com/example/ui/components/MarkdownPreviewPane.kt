package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EditorMarkdownMode(val label: String) {
    RAW("Raw"),
    SPLIT("Split"),
    PREVIEW("Preview")
}

@Composable
fun MarkdownModeSegmentedBar(
    currentMode: EditorMarkdownMode,
    accentColor: Color,
    isDark: Boolean,
    onSelectMode: (EditorMarkdownMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier.testTag("markdown_mode_segmented_bar")
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorMarkdownMode.entries.forEach { mode ->
                val selected = mode == currentMode
                val icon = when (mode) {
                    EditorMarkdownMode.RAW -> Icons.Default.Edit
                    EditorMarkdownMode.SPLIT -> Icons.Default.VerticalSplit
                    EditorMarkdownMode.PREVIEW -> Icons.Default.Visibility
                }
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (selected) accentColor else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelectMode(mode) }
                        .testTag("markdown_mode_${mode.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Checklist(val checked: Boolean, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Numbered(val marker: String, val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data object Blank : MarkdownBlock
}

internal fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    if (raw.isEmpty()) return emptyList()
    val lines = raw.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val numberedRegex = Regex("^(\\d+\\.)\\s+(.*)$")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            if (i < lines.size) i++
            continue
        }
        val numberedMatch = numberedRegex.matchEntire(line)
        when {
            line.startsWith("# ") -> blocks.add(MarkdownBlock.Heading(1, line.removePrefix("# ")))
            line.startsWith("## ") -> blocks.add(MarkdownBlock.Heading(2, line.removePrefix("## ")))
            line.startsWith("### ") -> blocks.add(MarkdownBlock.Heading(3, line.removePrefix("### ")))
            line.startsWith("> ") -> blocks.add(MarkdownBlock.Quote(line.removePrefix("> ")))
            line.startsWith("- [x] ", ignoreCase = true) -> blocks.add(MarkdownBlock.Checklist(true, line.drop(6)))
            line.startsWith("- [ ] ") -> blocks.add(MarkdownBlock.Checklist(false, line.drop(6)))
            line.startsWith("• ") -> blocks.add(MarkdownBlock.Bullet(line.removePrefix("• ")))
            line.startsWith("- ") -> blocks.add(MarkdownBlock.Bullet(line.removePrefix("- ")))
            line.startsWith("* ") -> blocks.add(MarkdownBlock.Bullet(line.removePrefix("* ")))
            numberedMatch != null -> blocks.add(
                MarkdownBlock.Numbered(
                    marker = numberedMatch.groupValues[1],
                    text = numberedMatch.groupValues[2]
                )
            )
            line.isBlank() -> blocks.add(MarkdownBlock.Blank)
            else -> blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }
    return blocks
}

@Composable
fun RenderedMarkdownPreview(
    markdownText: String,
    baseFontSizeSp: Float,
    accentColor: Color,
    isDark: Boolean,
    emptyPlaceholder: String = "Nothing to preview yet. Switch to Raw or Split mode to start typing Markdown.",
    onWikiLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val highlightBg = if (isDark) accentColor.copy(alpha = 0.28f) else Color(0xFFFDE047).copy(alpha = 0.45f)
    val codeBg = if (isDark) Color(0xFF162026) else Color(0xFFF1F5F9)
    val linkColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    if (markdownText.isBlank()) {
        Text(
            text = emptyPlaceholder,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = baseFontSizeSp.sp,
                lineHeight = (baseFontSizeSp * 1.5f).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = modifier
        )
        return
    }

    val blocks = parseMarkdownBlocks(markdownText)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = (baseFontSizeSp * 1.55f).sp,
                            lineHeight = (baseFontSizeSp * 1.9f).sp
                        )
                        2 -> MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (baseFontSizeSp * 1.32f).sp,
                            lineHeight = (baseFontSizeSp * 1.65f).sp
                        )
                        else -> MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = (baseFontSizeSp * 1.15f).sp,
                            lineHeight = (baseFontSizeSp * 1.45f).sp
                        )
                    }
                    Text(
                        text = parseRichMarkdownInline(
                            raw = block.text,
                            linkColor = linkColor,
                            highlightBg = highlightBg,
                            codeBg = codeBg,
                            onWikiLinkClick = onWikiLinkClick
                        ),
                        style = headingStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is MarkdownBlock.Quote -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(accentColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseRichMarkdownInline(
                                    raw = block.text,
                                    linkColor = linkColor,
                                    highlightBg = highlightBg,
                                    codeBg = codeBg,
                                    onWikiLinkClick = onWikiLinkClick
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = baseFontSizeSp.sp,
                                    lineHeight = (baseFontSizeSp * 1.48f).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                is MarkdownBlock.Checklist -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (block.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (block.checked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseRichMarkdownInline(
                                raw = block.text,
                                linkColor = linkColor,
                                highlightBg = highlightBg,
                                codeBg = codeBg,
                                onWikiLinkClick = onWikiLinkClick
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = baseFontSizeSp.sp,
                                textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                is MarkdownBlock.Bullet -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "•  ",
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = baseFontSizeSp.sp
                        )
                        Text(
                            text = parseRichMarkdownInline(
                                raw = block.text,
                                linkColor = linkColor,
                                highlightBg = highlightBg,
                                codeBg = codeBg
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = baseFontSizeSp.sp,
                                lineHeight = (baseFontSizeSp * 1.5f).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                is MarkdownBlock.Numbered -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "${block.marker} ",
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = baseFontSizeSp.sp
                        )
                        Text(
                            text = parseRichMarkdownInline(
                                raw = block.text,
                                linkColor = linkColor,
                                highlightBg = highlightBg,
                                codeBg = codeBg
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = baseFontSizeSp.sp,
                                lineHeight = (baseFontSizeSp * 1.5f).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = codeBg,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = (baseFontSizeSp * 0.88f).coerceAtLeast(12f).sp,
                                lineHeight = (baseFontSizeSp * 1.35f).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseRichMarkdownInline(
                            raw = block.text,
                            linkColor = linkColor,
                            highlightBg = highlightBg,
                            codeBg = codeBg
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = baseFontSizeSp.sp,
                            lineHeight = (baseFontSizeSp * 1.52f).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                MarkdownBlock.Blank -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
