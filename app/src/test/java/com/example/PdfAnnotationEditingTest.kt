package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.ui.screens.PdfBottomStudioDock
import com.example.ui.screens.PdfColorPickerDialog
import com.example.ui.screens.PdfHighlightSegment
import com.example.ui.screens.PdfPageAnnotation
import com.example.ui.screens.PdfStickyColors
import com.example.ui.screens.PdfTool
import com.example.ui.screens.STICKY_BADGE_MAX_SIZE
import com.example.ui.screens.STICKY_CARD_MAX_WIDTH
import com.example.ui.screens.STICKY_CARD_MIN_HEIGHT
import com.example.ui.screens.STICKY_CARD_MIN_WIDTH
import com.example.ui.screens.STICKY_CARD_WIDTH
import com.example.ui.screens.STICKY_STYLE_BADGE
import com.example.ui.screens.STICKY_STYLE_CARD
import com.example.ui.screens.SelectionDragHandle
import com.example.ui.screens.ShapeType
import com.example.ui.screens.boundingBox
import com.example.ui.screens.defaultCurveControlPoint
import com.example.ui.screens.findStickyPalette
import com.example.ui.screens.flippedCurve
import com.example.ui.screens.generateHueGrades
import com.example.ui.screens.hitTestSelectionHandle
import com.example.ui.screens.interpolateGradientColor
import com.example.ui.screens.normalizeAngleDeg
import com.example.ui.screens.parsePdfAnnotation
import com.example.ui.screens.resizedFromHandle
import com.example.ui.screens.rgbToHsv
import com.example.ui.screens.rotateHandlePosition
import com.example.ui.screens.rotatedAround
import com.example.ui.screens.rotatedByDelta
import com.example.ui.screens.rotationPivot
import com.example.ui.screens.snapAngleDeg
import com.example.ui.screens.stickyHeight
import com.example.ui.screens.stickyWidth
import com.example.ui.screens.translatedBy
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfAnnotationEditingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shape dock shows two non-scrolling lines graphic previews dot weights and gradient picker`() {
        var currentTool by mutableStateOf(PdfTool.SHAPE)
        var currentShape by mutableStateOf(ShapeType.RECT)
        var strokeWidth by mutableFloatStateOf(5f)
        var colorPickerOpens = 0
        var selectedAnn by mutableStateOf<PdfPageAnnotation?>(null)

        composeTestRule.setContent {
            MaterialTheme {
                PdfBottomStudioDock(
                    toolMode = currentTool,
                    selectedShape = currentShape,
                    highlightColor = Color(0xFFFEF08A),
                    inkColor = Color(0xFF2563EB),
                    textColor = Color(0xFF1E293B),
                    activeColor = Color(0xFFDC2626),
                    penStrokeWidth = strokeWidth,
                    textFontSize = 36f,
                    selectedStickyPalette = PdfStickyColors.first(),
                    stickyAsCard = true,
                    selectedAnnotation = selectedAnn,
                    currentPageIndex = 0,
                    totalPages = 5,
                    isBookmarked = false,
                    canUndo = false,
                    canRedo = false,
                    zoomScale = 1f,
                    hasPanOffset = false,
                    onSelectTool = { currentTool = it },
                    onSelectStickyPalette = {},
                    onToggleStickyStyle = {},
                    onOpenStickyWall = {},
                    onSelectHighlightColor = {},
                    onSelectPenWidth = { strokeWidth = it },
                    onSelectInkColor = {},
                    onSelectTextSize = {},
                    onSelectTextColor = {},
                    onSelectShape = { currentShape = it },
                    onEditSelectedText = {},
                    onUpdateSelectedColor = {},
                    onAdjustSelectedSize = {},
                    onFlipSelectedCurve = {},
                    onDuplicateSelected = {},
                    onBringSelectedToFront = {},
                    onDeleteSelected = {},
                    onClearCurrentPage = {},
                    onPrevPage = {},
                    onNextPage = {},
                    onJumpPage = {},
                    onOpenThumbnails = {},
                    onToggleBookmark = {},
                    onUndo = {},
                    onRedo = {},
                    onResetZoom = {},
                    onOpenColorPicker = { colorPickerOpens++ },
                    onRotateSelected = { delta ->
                        selectedAnn = selectedAnn?.rotatedByDelta(delta)
                    },
                    onResetRotationSelected = {
                        selectedAnn = selectedAnn?.copy(rotationDeg = 0f)
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Done chip has been removed from PDF tool panels
        composeTestRule.onNodeWithTag("dock_done_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").assertDoesNotExist()

        // Both shape panel rows are simultaneously displayed without horizontal scrolling
        composeTestRule.onNodeWithTag("shape_panel_row_shapes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shape_panel_row_style").assertIsDisplayed()

        // Basic shapes remain text pills
        composeTestRule.onNodeWithText("Rect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Star").assertIsDisplayed()

        // Non-basic arrow/curve tools render graphic buttons (no old text pills)
        composeTestRule.onNodeWithText("Curve ↷").assertDoesNotExist()
        composeTestRule.onNodeWithText("S-Curve").assertDoesNotExist()
        composeTestRule.onNodeWithText("Double ↔").assertDoesNotExist()
        composeTestRule.onNodeWithText("Elbow ↱").assertDoesNotExist()

        composeTestRule.onNodeWithTag("shape_arrow_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shape_curve_arrow_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ShapeType.CURVE_ARROW, currentShape)
        composeTestRule.onNodeWithContentDescription("Curve Arrow").assertIsSelected()

        composeTestRule.onNodeWithTag("shape_s_curve_arrow_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ShapeType.S_CURVE_ARROW, currentShape)

        composeTestRule.onNodeWithTag("shape_double_arrow_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ShapeType.DOUBLE_ARROW, currentShape)

        composeTestRule.onNodeWithTag("shape_elbow_arrow_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ShapeType.ELBOW_ARROW, currentShape)

        // Shape stroke weight buttons are visual dots (no Thin/Med/Bold text) and fit on line 2
        composeTestRule.onNodeWithText("Thin").assertDoesNotExist()
        composeTestRule.onNodeWithText("Med").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bold").assertDoesNotExist()
        composeTestRule.onNodeWithTag("shape_weight_bold").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(9f, strokeWidth, 0.01f)

        composeTestRule.onNodeWithTag("shape_color_gradient_picker").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, colorPickerOpens)

        // Verify SELECT 2-line panel when a sticky note is selected + rotate buttons & no Done button
        selectedAnn = PdfPageAnnotation(
            id = "sel-sticky-1",
            type = "COMMENT",
            pageNumber = 1,
            start = Offset(100f, 100f),
            end = Offset(260f, 180f),
            content = "Selected note",
            strokeWidth = STICKY_STYLE_CARD
        )
        currentTool = PdfTool.SELECT
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("select_panel_row_actions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_panel_row_colors").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dock_done_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sticky Card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_rotate_right").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(15f, selectedAnn!!.rotationDeg, 0.01f)
        composeTestRule.onNodeWithTag("select_rotate_reset").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(0f, selectedAnn!!.rotationDeg, 0.01f)
        composeTestRule.onNodeWithTag("select_color_gradient_picker").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, colorPickerOpens)

        // Verify Done button does not exist across remaining annotation tools
        listOf(
            PdfTool.PEN,
            PdfTool.NOTE,
            PdfTool.TEXT_BOX,
            PdfTool.HIGHLIGHT,
            PdfTool.UNDERLINE,
            PdfTool.STRIKETHROUGH,
            PdfTool.ERASER
        ).forEach { tool ->
            currentTool = tool
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("dock_done_button").assertDoesNotExist()
            composeTestRule.onNodeWithText("Done").assertDoesNotExist()
        }

        // Switch to PEN tool and verify proportional dot pickers replace Fine/Med/Bold/Marker text
        currentTool = PdfTool.PEN
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Fine").assertDoesNotExist()
        composeTestRule.onNodeWithText("Marker").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pen_weight_marker").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(18f, strokeWidth, 0.01f)
        composeTestRule.onNodeWithContentDescription("Marker stroke").assertIsSelected()
    }

    @Test
    fun `color grades and gradient picker selects grades gradients and synthesizes sticky palettes`() {
        // Pure color helpers
        val hsv = rgbToHsv(Color(0xFF2563EB))
        assertTrue(hsv[0] in 210f..230f)
        val grades = generateHueGrades(hsv[0])
        assertEquals(7, grades.size)
        // First grade is lighter than last grade
        assertTrue(grades.first().red > grades.last().red || grades.first().green > grades.last().green)

        val gradStart = Color(0xFFFEF08A)
        val gradEnd = Color(0xFF9333EA)
        val midGrad = interpolateGradientColor(listOf(gradStart, gradEnd), 0.5f)
        assertNotEquals(gradStart, midGrad)
        assertNotEquals(gradEnd, midGrad)

        // Custom sticky color synthesizes valid high-contrast ink & accent palette
        val customStickyPalette = findStickyPalette(Color(0xFF1E3A8A).toArgb())
        assertEquals("Custom", customStickyPalette.name)
        assertEquals(Color.White, customStickyPalette.ink)

        // UI dialog test: choosing a tonal grade swatch & applying
        var picked: Color? = null
        composeTestRule.setContent {
            MaterialTheme {
                PdfColorPickerDialog(
                    title = "Pick Color",
                    initialColor = Color(0xFFDC2626),
                    onSelectColor = { picked = it },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pdf_color_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color_grade_pad").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hue_spectrum_bar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("gradient_bar_ocean").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("grade_swatch_4").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("apply_picked_color_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(picked)
        assertNotEquals(Color(0xFFDC2626).toArgb(), picked!!.toArgb())
    }

    @Test
    fun `sticky note resizes via corner and edge handles clamps bounds moves cleanly and persists`() {
        val sticky = PdfPageAnnotation(
            id = "sticky-resize-1",
            type = "COMMENT",
            pageNumber = 1,
            start = Offset(200f, 150f),
            content = "Important theorem proof summary for exam revision",
            colorArgb = -256,
            strokeWidth = STICKY_STYLE_CARD
        )

        // Default size when end == Offset.Zero
        assertEquals(STICKY_CARD_WIDTH, sticky.stickyWidth(), 0.1f)
        val initialHeight = sticky.stickyHeight()
        assertTrue(initialHeight >= STICKY_CARD_MIN_HEIGHT)

        // Corner drag stretches both width and height
        val stretched = sticky.resizedFromHandle(
            SelectionDragHandle.RESIZE_BR,
            Offset(200f + 410f, 150f + 290f)
        )
        assertEquals(410f, stretched.stickyWidth(), 0.1f)
        assertEquals(290f, stretched.stickyHeight(), 0.1f)

        // Right-edge drag shrinks width only while preserving height
        val narrower = stretched.resizedFromHandle(
            SelectionDragHandle.RESIZE_R,
            Offset(200f + 190f, 150f + 999f)
        )
        assertEquals(190f, narrower.stickyWidth(), 0.1f)
        assertEquals(290f, narrower.stickyHeight(), 0.1f)

        // Bottom-edge drag shrinks height only while preserving width
        val shorter = narrower.resizedFromHandle(
            SelectionDragHandle.RESIZE_B,
            Offset(200f + 500f, 150f + 140f)
        )
        assertEquals(190f, shorter.stickyWidth(), 0.1f)
        assertEquals(140f, shorter.stickyHeight(), 0.1f)

        // Clamp bounds at extreme drags
        val tooSmall = shorter.resizedFromHandle(SelectionDragHandle.RESIZE_BR, Offset(210f, 160f))
        assertEquals(STICKY_CARD_MIN_WIDTH, tooSmall.stickyWidth(), 0.1f)
        assertEquals(STICKY_CARD_MIN_HEIGHT, tooSmall.stickyHeight(), 0.1f)

        val tooBig = shorter.resizedFromHandle(SelectionDragHandle.RESIZE_BR, Offset(1500f, 1500f))
        assertEquals(STICKY_CARD_MAX_WIDTH, tooBig.stickyWidth(), 0.1f)

        // Moving sticky note shifts position without altering stored width/height in `end`
        val moved = shorter.translatedBy(80f, 55f)
        assertEquals(280f, moved.start.x, 0.1f)
        assertEquals(205f, moved.start.y, 0.1f)
        assertEquals(190f, moved.stickyWidth(), 0.1f)
        assertEquals(140f, moved.stickyHeight(), 0.1f)

        // Round-trip serialization preserves resized size
        val parsed = parsePdfAnnotation(moved.toEntity("pdf-att-sticky"))
        assertNotNull(parsed)
        assertEquals(190f, parsed!!.stickyWidth(), 0.1f)
        assertEquals(140f, parsed.stickyHeight(), 0.1f)

        // Hit-testing honors resized bounds
        assertTrue(parsed.containsPoint(Offset(450f, 330f), tolerance = 0f))
        assertFalse(parsed.containsPoint(Offset(600f, 330f), tolerance = 0f))

        // Mini pin badge resize
        val badge = sticky.copy(strokeWidth = STICKY_STYLE_BADGE, end = Offset.Zero)
        val largerBadge = badge.resizedFromHandle(SelectionDragHandle.RESIZE_BR, Offset(200f + 180f, 150f + 90f))
        assertEquals(STICKY_BADGE_MAX_SIZE, largerBadge.stickyWidth(), 0.1f)
    }

    @Test
    fun `select tool moves highlights strokes and text boxes accurately`() {
        val highlight = PdfPageAnnotation(
            id = "hl-move-1",
            type = "HIGHLIGHT",
            pageNumber = 1,
            start = Offset(100f, 200f),
            end = Offset(400f, 270f),
            segments = listOf(
                PdfHighlightSegment(150f, 200f, 400f, 228f),
                PdfHighlightSegment(100f, 240f, 320f, 270f)
            )
        )
        val movedHighlight = highlight.translatedBy(40f, -20f)
        assertEquals(140f, movedHighlight.start.x, 0.1f)
        assertEquals(180f, movedHighlight.start.y, 0.1f)
        assertEquals(190f, movedHighlight.segments[0].left, 0.1f)
        assertEquals(208f, movedHighlight.segments[0].bottom, 0.1f)
        assertEquals(360f, movedHighlight.segments[1].right, 0.1f)

        val drawing = PdfPageAnnotation(
            id = "draw-1",
            type = "DRAWING",
            pageNumber = 1,
            points = listOf(Offset(100f, 100f), Offset(200f, 150f), Offset(250f, 200f)),
            strokeWidth = 6f
        )
        val movedDraw = drawing.translatedBy(25f, 30f)
        assertEquals(Offset(125f, 130f), movedDraw.points.first())
        assertEquals(Offset(275f, 230f), movedDraw.points.last())

        val bbox = movedDraw.boundingBox()
        val resizedDraw = movedDraw.resizedFromHandle(
            SelectionDragHandle.RESIZE_BR,
            Offset(bbox.left + bbox.width * 2f, bbox.top + bbox.height * 2f)
        )
        assertTrue(resizedDraw.boundingBox().width > bbox.width * 1.8f)

        val textBox = PdfPageAnnotation(
            id = "txt-1",
            type = "TEXT_BOX",
            pageNumber = 1,
            start = Offset(300f, 400f),
            content = "Key Formula",
            strokeWidth = 32f
        )
        val movedText = textBox.translatedBy(15f, -10f)
        assertEquals(Offset(315f, 390f), movedText.start)
        val textBbox = movedText.boundingBox()
        val largerText = movedText.resizedFromHandle(
            SelectionDragHandle.RESIZE_BR,
            Offset(textBbox.left + textBbox.width * 1.5f, textBbox.bottom)
        )
        assertTrue(largerText.strokeWidth > 40f)
    }

    @Test
    fun `curve double and elbow arrow tools hit test bend flip and serialize roundtrip`() {
        val start = Offset(100f, 300f)
        val end = Offset(500f, 300f)
        val ctrl = defaultCurveControlPoint(start, end)
        assertTrue("Default curve control point should arch away from midpoint Y", ctrl.y < 260f)

        val curveArrow = PdfPageAnnotation(
            id = "curve-1",
            type = "SHAPE_CURVE_ARROW",
            pageNumber = 1,
            points = listOf(ctrl),
            start = start,
            end = end,
            colorArgb = -65536,
            strokeWidth = 5f
        )

        // Midpoint of quadratic Bezier at t=0.5 is 0.25*P0 + 0.5*P1 + 0.25*P2
        val curveMidpoint = Offset(
            0.25f * start.x + 0.5f * ctrl.x + 0.25f * end.x,
            0.25f * start.y + 0.5f * ctrl.y + 0.25f * end.y
        )
        assertTrue(curveArrow.containsPoint(curveMidpoint, tolerance = 12f))
        assertFalse(curveArrow.containsPoint(Offset(300f, 520f), tolerance = 12f))

        // Selection handles hit curve control, start & end endpoints cleanly
        assertEquals(
            SelectionDragHandle.CURVE_CONTROL,
            hitTestSelectionHandle(curveArrow, ctrl + Offset(3f, -2f), zoomScale = 1f)
        )
        assertEquals(
            SelectionDragHandle.ENDPOINT_END,
            hitTestSelectionHandle(curveArrow, end, zoomScale = 1f)
        )

        // Dragging curve control handle bends the curve
        val bent = curveArrow.resizedFromHandle(SelectionDragHandle.CURVE_CONTROL, Offset(300f, 120f))
        assertEquals(Offset(300f, 120f), bent.points.first())

        // Flipping curve reflects control point across start-end chord
        val flipped = bent.flippedCurve()
        assertEquals(480f, flipped.points.first().y, 0.1f)

        // Serialization roundtrip preserves curve type and control point
        val parsedCurve = parsePdfAnnotation(bent.toEntity("pdf-att-arrows"))
        assertNotNull(parsedCurve)
        assertEquals("SHAPE_CURVE_ARROW", parsedCurve!!.type)
        assertEquals(Offset(300f, 120f), parsedCurve.points.first())

        // S-Curve, Double, and Elbow arrows hit test cleanly
        val sCurve = curveArrow.copy(id = "s-1", type = "SHAPE_S_CURVE_ARROW", points = emptyList())
        assertTrue(sCurve.containsPoint(Offset(300f, 300f), tolerance = 16f))

        val doubleArrow = curveArrow.copy(id = "dbl-1", type = "SHAPE_DOUBLE_ARROW", points = emptyList())
        assertTrue(doubleArrow.containsPoint(Offset(300f, 300f), tolerance = 12f))

        val elbowArrow = PdfPageAnnotation(
            id = "elb-1",
            type = "SHAPE_ELBOW_ARROW",
            pageNumber = 1,
            start = Offset(100f, 100f),
            end = Offset(400f, 250f),
            colorArgb = -16776961,
            strokeWidth = 5f
        )
        // Corner is at (400, 100) since horizontal span (300) > vertical span (150)
        assertTrue(elbowArrow.containsPoint(Offset(400f, 180f), tolerance = 12f))
        assertFalse(elbowArrow.containsPoint(Offset(150f, 220f), tolerance = 12f))
    }

    @Test
    fun `rotation supports lines curves shapes and sticky notes with hit testing resize and roundtrip`() {
        // Angle normalization & magnetic snap
        assertEquals(90f, snapAngleDeg(91.8f), 0.01f)
        assertEquals(-45f, normalizeAngleDeg(315f), 0.01f)
        assertEquals(0f, snapAngleDeg(358.2f), 0.01f)

        // 1. Horizontal line/stroke rotated 90° becomes vertical around its pivot
        val line = PdfPageAnnotation(
            id = "rot-line-1",
            type = "UNDERLINE",
            pageNumber = 1,
            start = Offset(100f, 300f),
            end = Offset(300f, 300f),
            strokeWidth = 4f
        ).rotatedByDelta(90f)
        // Pivot is (200, 300); rotated 90° maps local (300, 300) -> world (200, 400)
        assertTrue(line.containsPoint(Offset(200f, 390f), tolerance = 14f))
        assertFalse(line.containsPoint(Offset(295f, 300f), tolerance = 6f))
        val lineRotHandleWorld = line.rotateHandlePosition(1f).rotatedAround(line.rotationPivot(), line.rotationDeg)
        assertEquals(SelectionDragHandle.ROTATE, hitTestSelectionHandle(line, lineRotHandleWorld, zoomScale = 1f))

        // 2. Curve arrow rotated 90° hits rotated curve midpoint & curve control handle in world space
        val curveStart = Offset(100f, 300f)
        val curveEnd = Offset(300f, 300f)
        val curveCtrl = defaultCurveControlPoint(curveStart, curveEnd)
        val rotatedCurve = PdfPageAnnotation(
            id = "rot-curve-1",
            type = "SHAPE_CURVE_ARROW",
            pageNumber = 1,
            points = listOf(curveCtrl),
            start = curveStart,
            end = curveEnd,
            strokeWidth = 5f
        ).rotatedByDelta(90f)
        val curvePivot = rotatedCurve.rotationPivot()
        val worldCtrl = curveCtrl.rotatedAround(curvePivot, 90f)
        assertEquals(SelectionDragHandle.CURVE_CONTROL, hitTestSelectionHandle(rotatedCurve, worldCtrl, zoomScale = 1f))

        // 3. Rectangle shape rotated 45° resizes without drifting its opposite top-left world corner
        val shape = PdfPageAnnotation(
            id = "rot-rect-1",
            type = "SHAPE_RECT",
            pageNumber = 1,
            start = Offset(200f, 200f),
            end = Offset(400f, 300f),
            strokeWidth = 4f,
            rotationDeg = 45f
        )
        val oldPivot = shape.rotationPivot()
        val oldWorldTopLeft = Offset(200f, 200f).rotatedAround(oldPivot, 45f)
        val targetWorldBR = Offset(480f, 360f).rotatedAround(oldPivot, 45f)
        val resizedRotatedShape = shape.resizedFromHandle(SelectionDragHandle.RESIZE_BR, targetWorldBR)
        val newPivot = resizedRotatedShape.rotationPivot()
        val newWorldTopLeft = resizedRotatedShape.start.rotatedAround(newPivot, 45f)
        assertTrue(
            "Top-left anchor drifted by ${hypot(oldWorldTopLeft.x - newWorldTopLeft.x, oldWorldTopLeft.y - newWorldTopLeft.y)}",
            hypot(oldWorldTopLeft.x - newWorldTopLeft.x, oldWorldTopLeft.y - newWorldTopLeft.y) < 1.5f
        )

        // 4. Sticky note rotation + JSON persistence round-trip
        val rotatedSticky = PdfPageAnnotation(
            id = "rot-sticky-1",
            type = "COMMENT",
            pageNumber = 2,
            start = Offset(300f, 400f),
            end = Offset(260f, 180f),
            content = "Rotated sticky card",
            strokeWidth = STICKY_STYLE_CARD
        ).rotatedByDelta(-30f)
        val roundTripped = parsePdfAnnotation(rotatedSticky.toEntity("pdf-rot-test"))
        assertNotNull(roundTripped)
        assertEquals(-30f, roundTripped!!.rotationDeg, 0.01f)
    }

    @Test
    fun `shape colors and gradient picker fit completely inside shapes box on compact screens`() {
        var selectedColor by mutableStateOf(Color(0xFFDC2626))
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    PdfBottomStudioDock(
                        toolMode = PdfTool.SHAPE,
                        selectedShape = ShapeType.CIRCLE,
                        highlightColor = Color(0xFFFEF08A),
                        inkColor = selectedColor,
                        textColor = Color(0xFF1E293B),
                        activeColor = selectedColor,
                        penStrokeWidth = 5f,
                        textFontSize = 36f,
                        selectedStickyPalette = PdfStickyColors.first(),
                        stickyAsCard = true,
                        selectedAnnotation = null,
                        currentPageIndex = 0,
                        totalPages = 3,
                        isBookmarked = false,
                        canUndo = false,
                        canRedo = false,
                        zoomScale = 1f,
                        hasPanOffset = false,
                        onSelectTool = {},
                        onSelectStickyPalette = {},
                        onToggleStickyStyle = {},
                        onOpenStickyWall = {},
                        onSelectHighlightColor = {},
                        onSelectPenWidth = {},
                        onSelectInkColor = { selectedColor = it },
                        onSelectTextSize = {},
                        onSelectTextColor = {},
                        onSelectShape = {},
                        onEditSelectedText = {},
                        onUpdateSelectedColor = {},
                        onAdjustSelectedSize = {},
                        onFlipSelectedCurve = {},
                        onDuplicateSelected = {},
                        onBringSelectedToFront = {},
                        onDeleteSelected = {},
                        onClearCurrentPage = {},
                        onPrevPage = {},
                        onNextPage = {},
                        onJumpPage = {},
                        onOpenThumbnails = {},
                        onToggleBookmark = {},
                        onUndo = {},
                        onRedo = {},
                        onResetZoom = {},
                        onOpenColorPicker = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val rowBounds = composeTestRule.onNodeWithTag("shape_panel_row_style").getBoundsInRoot()
        (0..5).forEach { idx ->
            val swatchNode = composeTestRule.onNodeWithTag("shape_color_swatch_$idx")
            swatchNode.assertIsDisplayed()
            val swatchBounds = swatchNode.getBoundsInRoot()
            assertTrue("Swatch $idx left (${swatchBounds.left}) clipped outside box left (${rowBounds.left})", swatchBounds.left >= rowBounds.left)
            assertTrue("Swatch $idx right (${swatchBounds.right}) clipped outside box right (${rowBounds.right})", swatchBounds.right <= rowBounds.right)
        }
        val gradNode = composeTestRule.onNodeWithTag("shape_color_gradient_picker")
        gradNode.assertIsDisplayed()
        val gradBounds = gradNode.getBoundsInRoot()
        assertTrue("Gradient picker right (${gradBounds.right}) clipped outside box right (${rowBounds.right})", gradBounds.right <= rowBounds.right)
    }

    @Test
    fun `all annotation tools expose modern rotate bar and rotate selected or latest item cleanly`() {
        var currentTool by mutableStateOf(PdfTool.PAN_ZOOM)
        val latestAnnotation = PdfPageAnnotation(
            id = "ann-any-1",
            type = "SHAPE_RECT",
            pageNumber = 1,
            start = Offset(120f, 160f),
            end = Offset(320f, 260f),
            strokeWidth = 5f
        )
        var pageItem by mutableStateOf(latestAnnotation)
        var selectedAnn by mutableStateOf<PdfPageAnnotation?>(null)

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    PdfBottomStudioDock(
                        toolMode = currentTool,
                        selectedShape = ShapeType.RECT,
                        highlightColor = Color(0xFFFEF08A),
                        inkColor = Color(0xFF2563EB),
                        textColor = Color(0xFF1E293B),
                        activeColor = Color(0xFF2563EB),
                        penStrokeWidth = 5f,
                        textFontSize = 36f,
                        selectedStickyPalette = PdfStickyColors.first(),
                        stickyAsCard = true,
                        selectedAnnotation = selectedAnn,
                        currentPageIndex = 0,
                        totalPages = 3,
                        isBookmarked = false,
                        canUndo = false,
                        canRedo = false,
                        zoomScale = 1f,
                        hasPanOffset = false,
                        onSelectTool = { currentTool = it },
                        onSelectStickyPalette = {},
                        onToggleStickyStyle = {},
                        onOpenStickyWall = {},
                        onSelectHighlightColor = {},
                        onSelectPenWidth = {},
                        onSelectInkColor = {},
                        onSelectTextSize = {},
                        onSelectTextColor = {},
                        onSelectShape = {},
                        onEditSelectedText = {},
                        onUpdateSelectedColor = {},
                        onAdjustSelectedSize = {},
                        onFlipSelectedCurve = {},
                        onDuplicateSelected = {},
                        onBringSelectedToFront = {},
                        onDeleteSelected = {},
                        onClearCurrentPage = {},
                        onPrevPage = {},
                        onNextPage = {},
                        onJumpPage = {},
                        onOpenThumbnails = {},
                        onToggleBookmark = {},
                        onUndo = {},
                        onRedo = {},
                        onResetZoom = {},
                        onRotateSelected = { delta ->
                            val updated = (selectedAnn ?: pageItem).rotatedByDelta(delta)
                            pageItem = updated
                            selectedAnn = updated
                        },
                        onRotateSelectedTo = { targetDeg ->
                            val updated = (selectedAnn ?: pageItem).copy(rotationDeg = snapAngleDeg(targetDeg, 2f))
                            pageItem = updated
                            selectedAnn = updated
                        },
                        onResetRotationSelected = {
                            val updated = pageItem.copy(rotationDeg = 0f)
                            pageItem = updated
                            selectedAnn = updated
                        },
                        hasSelectableOnPage = true,
                        onSelectLatestOnPage = { selectedAnn = pageItem },
                        onClearSelection = { selectedAnn = null }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // PAN_ZOOM and ERASER do not display the annotation rotate bar
        composeTestRule.onNodeWithTag("tool_rotate_bar").assertDoesNotExist()
        currentTool = PdfTool.ERASER
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tool_rotate_bar").assertDoesNotExist()

        // Every annotation creation/editing tool exposes ModernToolRotateStrip
        listOf(
            PdfTool.PEN,
            PdfTool.SHAPE,
            PdfTool.HIGHLIGHT,
            PdfTool.UNDERLINE,
            PdfTool.STRIKETHROUGH,
            PdfTool.TEXT_BOX,
            PdfTool.NOTE
        ).forEach { tool ->
            selectedAnn = null
            pageItem = pageItem.copy(rotationDeg = 0f)
            currentTool = tool
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("tool_rotate_bar").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tool_rotate_select_last").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tool_rotate_right").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()

            // Rotating auto-selects target and reveals precision slider + live reset badge
            assertNotNull("Expected rotation to select item in $tool", selectedAnn)
            assertEquals(15f, selectedAnn!!.rotationDeg, 0.01f)
            composeTestRule.onNodeWithTag("tool_rotate_slider").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tool_rotate_reset").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()
            assertEquals(0f, selectedAnn!!.rotationDeg, 0.01f)

            composeTestRule.onNodeWithTag("tool_rotate_left").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()
            assertEquals(-15f, selectedAnn!!.rotationDeg, 0.01f)

            composeTestRule.onNodeWithTag("tool_rotate_deselect").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()
            assertEquals(null, selectedAnn)

            composeTestRule.onNodeWithTag("tool_rotate_select_last").performClick()
            composeTestRule.waitForIdle()
            assertNotNull(selectedAnn)
        }

        // In SELECT mode, tool_rotate_bar yields to integrated select_rotate_slider_row + preset chips
        currentTool = PdfTool.SELECT
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tool_rotate_bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("select_rotate_slider_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_rotate_slider").assertIsDisplayed()

        composeTestRule.onNodeWithText("-45°").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(-45f, selectedAnn!!.rotationDeg, 0.01f)

        composeTestRule.onNodeWithText("+45°").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(45f, selectedAnn!!.rotationDeg, 0.01f)
    }

    @Test
    fun `all annotation types rotate around pivot with handle priority and persistence`() {
        val typesToVerify = listOf(
            PdfPageAnnotation(
                id = "a-pen",
                type = "DRAWING",
                pageNumber = 1,
                points = listOf(Offset(100f, 100f), Offset(220f, 130f), Offset(280f, 180f)),
                strokeWidth = 6f
            ),
            PdfPageAnnotation(
                id = "a-hl",
                type = "HIGHLIGHT",
                pageNumber = 1,
                start = Offset(100f, 200f),
                end = Offset(340f, 232f),
                segments = listOf(PdfHighlightSegment(100f, 200f, 340f, 232f))
            ),
            PdfPageAnnotation(
                id = "a-text",
                type = "TEXT_BOX",
                pageNumber = 1,
                start = Offset(180f, 320f),
                content = "Rotated Note",
                strokeWidth = 36f
            ),
            PdfPageAnnotation(
                id = "a-strike",
                type = "STRIKETHROUGH",
                pageNumber = 1,
                start = Offset(100f, 400f),
                end = Offset(320f, 400f),
                strokeWidth = 4f
            ),
            PdfPageAnnotation(
                id = "a-star",
                type = "SHAPE_STAR",
                pageNumber = 1,
                start = Offset(150f, 500f),
                end = Offset(290f, 640f),
                strokeWidth = 5f
            ),
            PdfPageAnnotation(
                id = "a-scurve",
                type = "SHAPE_S_CURVE_ARROW",
                pageNumber = 1,
                start = Offset(100f, 700f),
                end = Offset(360f, 780f),
                strokeWidth = 5f
            )
        )

        typesToVerify.forEach { base ->
            val rotated = base.rotatedByDelta(30f)
            val rotHandleWorld = rotated.rotateHandlePosition(1f).rotatedAround(
                pivot = rotated.rotationPivot(),
                angleDeg = rotated.rotationDeg
            )
            assertEquals(
                "Rotate handle missed for ${base.type}",
                SelectionDragHandle.ROTATE,
                hitTestSelectionHandle(rotated, rotHandleWorld, zoomScale = 1f)
            )
            val parsed = parsePdfAnnotation(rotated.toEntity("pdf-all-rot"))
            assertNotNull(parsed)
            assertEquals(30f, parsed!!.rotationDeg, 0.01f)
        }
    }
}
