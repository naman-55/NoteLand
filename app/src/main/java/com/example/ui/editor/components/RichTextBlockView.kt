package com.example.ui.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.engine.RichSpanEngine
import com.example.ui.editor.model.CalloutTone
import com.example.ui.editor.model.DividerStyle
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.InlineSpan
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.UnderlineStyle
import kotlin.math.PI
import kotlin.math.sin

private class RichSpansVisualTransformation(
    private val spans: List<InlineSpan>,
    private val isDark: Boolean,
    private val codeBg: Color,
    private val linkColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = RichSpanEngine.buildStyledAnnotatedString(
            text = text.text,
            spans = spans,
            isDark = isDark,
            codeBg = codeBg,
            linkColor = linkColor
        )
        return TransformedText(styled, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RichSpansVisualTransformation) return false
        return spans == other.spans && isDark == other.isDark && codeBg == other.codeBg && linkColor == other.linkColor
    }

    override fun hashCode(): Int {
        var r = spans.hashCode()
        r = 31 * r + isDark.hashCode()
        r = 31 * r + codeBg.hashCode()
        r = 31 * r + linkColor.hashCode()
        return r
    }
}

/**
 * Draws crisp wavy underlines beneath every UnderlineStyle.WAVY span using exact TextLayoutResult bounds.
 */
private fun Modifier.drawWavyUnderlines(
    spans: List<InlineSpan>,
    textLength: Int,
    layoutResult: TextLayoutResult?,
    defaultWaveColor: Color,
    isDark: Boolean
): Modifier = this.drawWithContent {
    drawContent()
    val layout = layoutResult ?: return@drawWithContent
    if (textLength <= 0) return@drawWithContent
    val wavySpans = spans.filter { it.style.underline == UnderlineStyle.WAVY || it.style.underline == UnderlineStyle.DOUBLE_WAVY }
    if (wavySpans.isEmpty()) return@drawWithContent

    val amplitudePx = 1.9.dp.toPx()
    val wavelengthPx = 6.2.dp.toPx()
    val strokePx = 1.35.dp.toPx()

    wavySpans.forEach { span ->
        val start = span.start.coerceIn(0, textLength - 1)
        val end = (span.end - 1).coerceIn(start, textLength - 1)
        val waveColor = EditorColorPalette.resolveTextColor(span.style.textColorHex, isDark)
            ?: defaultWaveColor

        val startLine = layout.getLineForOffset(start)
        val endLine = layout.getLineForOffset(end)

        for (line in startLine..endLine) {
            val lineStartOffset = maxOf(start, layout.getLineStart(line))
            val lineEndOffset = minOf(end, (layout.getLineEnd(line, visibleEnd = true) - 1).coerceAtLeast(lineStartOffset))
            if (lineEndOffset < lineStartOffset) continue

            val startBox = layout.getBoundingBox(lineStartOffset)
            val endBox = layout.getBoundingBox(lineEndOffset)
            val xStart = minOf(startBox.left, endBox.left)
            val xEnd = maxOf(startBox.right, endBox.right)
            if (xEnd - xStart < 2f) continue

            val baselineY = (maxOf(startBox.bottom, endBox.bottom) - 1.dp.toPx())
                .coerceIn(0f, size.height)

            val path = Path()
            var x = xStart
            var first = true
            val step = 1.5f
            while (x <= xEnd) {
                val phase = ((x - xStart) / wavelengthPx) * (2.0 * PI)
                val y = baselineY + (sin(phase) * amplitudePx).toFloat()
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }
            drawPath(
                path = path,
                color = waveColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun RichTextBlockView(
    block: RichBlock.TextBlock,
    blockIndex: Int,
    allBlocks: List<RichBlock>,
    isActive: Boolean,
    isReadOnly: Boolean,
    selection: TextRange,
    pendingFocusOffset: Int?,
    baseFontSizeSp: Float,
    accentColor: Color,
    isDark: Boolean,
    showPlaceholder: Boolean,
    onFocused: () -> Unit,
    onPendingFocusConsumed: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onEnterSplit: (cursorOffset: Int) -> Unit,
    onBackspaceAtStart: () -> Unit,
    onToggleChecklist: () -> Unit,
    onCycleCalloutTone: () -> Unit,
    onWikiLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember(block.id) { FocusRequester() }
    var textLayoutResult by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }

    val codeBg = if (isDark) Color(0xFF1A252E) else Color(0xFFF1F5F9)
    val linkColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val textAlign = when (block.alignment) {
        RichTextAlignment.LEFT -> TextAlign.Start
        RichTextAlignment.CENTER -> TextAlign.Center
        RichTextAlignment.RIGHT -> TextAlign.End
    }

    val baseTextStyle = resolveBlockTextStyle(
        kind = block.kind,
        baseFontSizeSp = baseFontSizeSp,
        textAlign = textAlign,
        checked = block.checked,
        baseColor = MaterialTheme.colorScheme.onBackground.copy(
            alpha = if (block.kind == RichBlockKind.CHECKLIST && block.checked) 0.56f else 0.94f
        )
    )

    val indentStartDp = (block.indentLevel.coerceIn(0, RichSpanEngine.MAX_INDENT_LEVEL) * 22).dp

    // Trigger focus when requested by reducer (e.g., Enter split or Backspace merge)
    LaunchedEffect(pendingFocusOffset) {
        if (!isReadOnly && pendingFocusOffset != null) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            onPendingFocusConsumed()
        }
    }

    val containerModifier = modifier
        .fillMaxWidth()
        .padding(start = indentStartDp, top = 2.dp, bottom = 2.dp)

    val renderFieldContent: @Composable (Modifier) -> Unit = { innerMod ->
        if (isReadOnly) {
            val styled = remember(block.text, block.spans, isDark) {
                RichSpanEngine.buildStyledAnnotatedString(
                    text = block.text,
                    spans = block.spans,
                    isDark = isDark,
                    codeBg = codeBg,
                    linkColor = linkColor
                )
            }
            Text(
                text = styled,
                style = baseTextStyle,
                onTextLayout = { textLayoutResult = it },
                modifier = innerMod
                    .fillMaxWidth()
                    .drawWavyUnderlines(
                        spans = block.spans,
                        textLength = block.text.length,
                        layoutResult = textLayoutResult,
                        defaultWaveColor = accentColor,
                        isDark = isDark
                    )
                    .then(
                        if (onWikiLinkClick != null && block.text.contains("[[")) {
                            Modifier.clickable {
                                val m = Regex("\\[\\[([^\\[\\]|#]+)").find(block.text)
                                m?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let(onWikiLinkClick)
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            var compositionState by remember(block.id) { mutableStateOf<TextRange?>(null) }
            val safeSelection = TextRange(
                selection.start.coerceIn(0, block.text.length),
                selection.end.coerceIn(0, block.text.length)
            )
            val safeComposition = compositionState?.let { comp ->
                val s = comp.start.coerceIn(0, block.text.length)
                val e = comp.end.coerceIn(0, block.text.length)
                if (s < e) TextRange(s, e) else null
            }
            val tfv = TextFieldValue(
                text = block.text,
                selection = safeSelection,
                composition = safeComposition
            )
            val visualTransformation = remember(block.spans, isDark, codeBg, linkColor) {
                RichSpansVisualTransformation(
                    spans = block.spans,
                    isDark = isDark,
                    codeBg = codeBg,
                    linkColor = linkColor
                )
            }

            BasicTextField(
                value = tfv,
                onValueChange = { incoming ->
                    val newlineIdx = incoming.text.indexOf('\n')
                    val expectedLenAfterSingleEnter = block.text.length - safeSelection.length + 1
                    val isSingleEnterKeypress = newlineIdx >= 0 &&
                        incoming.text.count { it == '\n' } == 1 &&
                        incoming.text.length == expectedLenAfterSingleEnter
                    if (isSingleEnterKeypress) {
                        compositionState = null
                        if (!safeSelection.collapsed) {
                            val cleanWithoutNewline = incoming.text.removeRange(newlineIdx, newlineIdx + 1)
                            onValueChange(
                                TextFieldValue(
                                    text = cleanWithoutNewline,
                                    selection = TextRange(newlineIdx.coerceIn(0, cleanWithoutNewline.length)),
                                    composition = null
                                )
                            )
                        }
                        // Single Enter press splits cleanly into next rich block
                        onEnterSplit(newlineIdx)
                    } else {
                        val sanitized = if (incoming.text.contains('\n')) {
                            val clean = incoming.text.replace('\n', ' ')
                            compositionState = incoming.composition?.let { comp ->
                                val s = comp.start.coerceIn(0, clean.length)
                                val e = comp.end.coerceIn(0, clean.length)
                                if (s < e) TextRange(s, e) else null
                            }
                            incoming.copy(
                                text = clean,
                                selection = TextRange(
                                    incoming.selection.start.coerceIn(0, clean.length),
                                    incoming.selection.end.coerceIn(0, clean.length)
                                ),
                                composition = compositionState
                            )
                        } else {
                            compositionState = incoming.composition
                            incoming
                        }
                        onValueChange(sanitized)
                    }
                },
                onTextLayout = { textLayoutResult = it },
                visualTransformation = visualTransformation,
                textStyle = baseTextStyle,
                cursorBrush = SolidColor(accentColor),
                modifier = innerMod
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused() }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            keyEvent.key == Key.Backspace &&
                            safeSelection.collapsed &&
                            safeSelection.start == 0
                        ) {
                            onBackspaceAtStart()
                            true
                        } else {
                            false
                        }
                    }
                    .drawWavyUnderlines(
                        spans = block.spans,
                        textLength = block.text.length,
                        layoutResult = textLayoutResult,
                        defaultWaveColor = accentColor,
                        isDark = isDark
                    )
                    .testTag(if (isActive) "note_content_input" else "rich_block_input_${block.id}"),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (block.text.isEmpty() && showPlaceholder) {
                            val placeholder = when (block.kind) {
                                RichBlockKind.HEADING_1 -> "Heading 1"
                                RichBlockKind.HEADING_2 -> "Heading 2"
                                RichBlockKind.HEADING_3 -> "Heading 3"
                                RichBlockKind.BULLET_LIST -> "List item"
                                RichBlockKind.NUMBERED_LIST -> "Numbered item"
                                RichBlockKind.CHECKLIST -> "To-do item"
                                RichBlockKind.BLOCKQUOTE -> "Write a quote…"
                                RichBlockKind.CALLOUT -> "Add a callout note…"
                                else -> "Start writing…"
                            }
                            Text(
                                text = placeholder,
                                style = baseTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }

    when (block.kind) {
        RichBlockKind.BLOCKQUOTE -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.42f else 0.52f),
                modifier = containerModifier.padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    renderFieldContent(Modifier.weight(1f))
                }
            }
        }

        RichBlockKind.CALLOUT -> {
            val (toneBg, toneAccent, toneIcon) = resolveCalloutVisuals(block.calloutTone, isDark, accentColor)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = toneBg,
                border = BorderStroke(1.dp, toneAccent.copy(alpha = 0.35f)),
                modifier = containerModifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = toneAccent.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .then(
                                if (!isReadOnly) {
                                    Modifier.clickable { onCycleCalloutTone() }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("callout_tone_btn_${block.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = toneIcon,
                                contentDescription = block.calloutTone.label,
                                tint = toneAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = block.calloutTone.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = toneAccent,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        renderFieldContent(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        RichBlockKind.BULLET_LIST -> {
            val glyph = RichDocumentReducer.computeBulletGlyph(block.indentLevel)
            Row(
                modifier = containerModifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = glyph,
                    style = baseTextStyle.copy(fontWeight = FontWeight.Bold, color = accentColor),
                    modifier = Modifier.width(20.dp)
                )
                renderFieldContent(Modifier.weight(1f))
            }
        }

        RichBlockKind.NUMBERED_LIST -> {
            val label = remember(allBlocks, blockIndex) {
                RichDocumentReducer.computeNumberedListLabel(allBlocks, blockIndex)
            }
            Row(
                modifier = containerModifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    style = baseTextStyle.copy(fontWeight = FontWeight.SemiBold, color = accentColor),
                    modifier = Modifier.padding(end = 8.dp)
                )
                renderFieldContent(Modifier.weight(1f))
            }
        }

        RichBlockKind.CHECKLIST -> {
            Row(
                modifier = containerModifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleChecklist() }
                        .testTag("rich_checklist_toggle_${block.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (block.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (block.checked) "Checked" else "Unchecked",
                        tint = if (block.checked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                    renderFieldContent(Modifier.fillMaxWidth())
                }
            }
        }

        else -> {
            Box(modifier = containerModifier.padding(vertical = if (block.kind.name.startsWith("HEADING")) 4.dp else 1.dp)) {
                renderFieldContent(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun RichDividerBlockView(
    block: RichBlock.DividerBlock,
    isReadOnly: Boolean,
    accentColor: Color,
    onCycleStyle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(if (!isReadOnly) Modifier.clickable { onCycleStyle() } else Modifier)
            .testTag("rich_divider_${block.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            val y = size.height / 2f
            val pathEffect = when (block.style) {
                DividerStyle.SOLID -> null
                DividerStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                DividerStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
            }
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
        }
        if (!isReadOnly) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("delete_divider_${block.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Divider",
                    tint = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun resolveBlockTextStyle(
    kind: RichBlockKind,
    baseFontSizeSp: Float,
    textAlign: TextAlign,
    checked: Boolean,
    baseColor: Color
): TextStyle {
    return when (kind) {
        RichBlockKind.HEADING_1 -> MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = (baseFontSizeSp * 1.48f).sp,
            lineHeight = (baseFontSizeSp * 1.82f).sp,
            color = baseColor,
            textAlign = textAlign
        )
        RichBlockKind.HEADING_2 -> MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (baseFontSizeSp * 1.28f).sp,
            lineHeight = (baseFontSizeSp * 1.6f).sp,
            color = baseColor,
            textAlign = textAlign
        )
        RichBlockKind.HEADING_3 -> MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (baseFontSizeSp * 1.14f).sp,
            lineHeight = (baseFontSizeSp * 1.46f).sp,
            color = baseColor,
            textAlign = textAlign
        )
        RichBlockKind.BLOCKQUOTE -> MaterialTheme.typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic,
            fontSize = baseFontSizeSp.sp,
            lineHeight = (baseFontSizeSp * 1.54f).sp,
            color = baseColor.copy(alpha = 0.88f),
            textAlign = textAlign
        )
        RichBlockKind.CHECKLIST -> MaterialTheme.typography.bodyLarge.copy(
            fontSize = baseFontSizeSp.sp,
            lineHeight = (baseFontSizeSp * 1.52f).sp,
            color = baseColor,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            textAlign = textAlign
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            fontSize = baseFontSizeSp.sp,
            lineHeight = (baseFontSizeSp * 1.56f).sp,
            color = baseColor,
            textAlign = textAlign
        )
    }
}

private fun resolveCalloutVisuals(
    tone: CalloutTone,
    isDark: Boolean,
    defaultAccent: Color
): Triple<Color, Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (tone) {
        CalloutTone.NOTE -> Triple(
            if (isDark) Color(0xFF1F1C18) else Color(0xFFFFF7ED),
            defaultAccent,
            Icons.AutoMirrored.Filled.StickyNote2
        )
        CalloutTone.INFO -> Triple(
            if (isDark) Color(0xFF132232) else Color(0xFFEFF6FF),
            if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
            Icons.Default.Info
        )
        CalloutTone.TIP -> Triple(
            if (isDark) Color(0xFF12261E) else Color(0xFFECFDF5),
            if (isDark) Color(0xFF34D399) else Color(0xFF059669),
            Icons.Default.Lightbulb
        )
        CalloutTone.WARNING -> Triple(
            if (isDark) Color(0xFF2B1C16) else Color(0xFFFEF2F2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Icons.Default.Warning
        )
    }
}
