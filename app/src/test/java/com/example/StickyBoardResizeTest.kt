package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StickyNoteEntity
import com.example.ui.screens.StickyNoteCanvasItem
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.StickyLayout
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StickyBoardResizeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NoteViewModel stretches shrinks clamps and persists sticky note sizes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("noteland_settings", Context.MODE_PRIVATE).edit().clear().commit()

        val viewModel = NoteViewModel(app)
        val stickyId = "sticky-resize-vm-1"

        // Stretch beyond default
        viewModel.updateStickySize(stickyId, width = 340f, height = 410f)
        var saved = viewModel.stickyLayouts.value[stickyId]!!
        assertEquals(340f, saved.width, 0.01f)
        assertEquals(410f, saved.height, 0.01f)

        // Shrink below default
        viewModel.updateStickySize(stickyId, width = 135f, height = 125f)
        saved = viewModel.stickyLayouts.value[stickyId]!!
        assertEquals(135f, saved.width, 0.01f)
        assertEquals(125f, saved.height, 0.01f)

        // Clamp upper & lower boundaries
        viewModel.updateStickySize(stickyId, width = 999f, height = 999f)
        saved = viewModel.stickyLayouts.value[stickyId]!!
        assertEquals(StickyLayout.MAX_WIDTH, saved.width, 0.01f)
        assertEquals(StickyLayout.MAX_HEIGHT, saved.height, 0.01f)

        viewModel.updateStickySize(stickyId, width = 20f, height = 20f)
        saved = viewModel.stickyLayouts.value[stickyId]!!
        assertEquals(StickyLayout.MIN_WIDTH, saved.width, 0.01f)
        assertEquals(StickyLayout.MIN_HEIGHT, saved.height, 0.01f)

        // Reload persisted size in a fresh ViewModel instance
        viewModel.updateStickySize(stickyId, width = 285f, height = 315f)
        viewModel.updateStickyRotation(stickyId, rotationDeg = -32.5f)
        val reloadedVm = NoteViewModel(app)
        val reloaded = reloadedVm.stickyLayouts.value[stickyId]!!
        assertEquals(285f, reloaded.width, 0.01f)
        assertEquals(315f, reloaded.height, 0.01f)
        assertEquals(-32.5f, reloaded.rotationDeg, 0.01f)
    }

    @Test
    fun `StickyNoteCanvasItem corner and edge drag handles stretch and shrink note dimensions smoothly`() {
        val note = StickyNoteEntity(
            id = "sticky-ui-1",
            title = "Design Review",
            content = "Check corner and edge resize handles for stretching and shrinking smoothly."
        )
        var latestLayout = StickyLayout(
            id = note.id,
            x = 24f,
            y = 24f,
            width = 170f,
            height = 180f
        )

        composeTestRule.setContent {
            NoteLandTheme(darkTheme = false) {
                var layout by remember { mutableStateOf(latestLayout) }
                var selected by remember { mutableStateOf(false) }

                Box(modifier = Modifier.size(800.dp, 800.dp)) {
                    StickyNoteCanvasItem(
                        note = note,
                        layout = layout,
                        isSelected = selected,
                        onSelect = { selected = true },
                        onPositionCommit = { x, y ->
                            layout = layout.copy(x = x, y = y)
                            latestLayout = layout
                        },
                        onSizeCommit = { w, h ->
                            layout = layout.copy(
                                width = StickyLayout.clampWidth(w),
                                height = StickyLayout.clampHeight(h)
                            )
                            latestLayout = layout
                        },
                        onTogglePin = {},
                        onDuplicate = {},
                        onEdit = {},
                        onDelete = {}
                    )
                }
            }
        }

        // 1. Drag bottom-right corner handle outward -> stretches both width & height
        composeTestRule.onNodeWithTag("sticky_resize_corner_${note.id}")
            .assertIsDisplayed()
            .performTouchInput {
                swipe(
                    start = center,
                    end = center + Offset(120f, 100f),
                    durationMillis = 250
                )
            }
        composeTestRule.waitForIdle()
        val stretchedWidth = latestLayout.width
        val stretchedHeight = latestLayout.height
        assertTrue("Expected corner drag out to stretch width > 170, got $stretchedWidth", stretchedWidth > 220f)
        assertTrue("Expected corner drag out to stretch height > 180, got $stretchedHeight", stretchedHeight > 220f)

        // 2. Selecting via drag revealed independent width & height edge handles; drag right edge inward -> shrinks width only
        composeTestRule.onNodeWithTag("sticky_resize_width_${note.id}")
            .assertIsDisplayed()
            .performTouchInput {
                swipe(
                    start = center,
                    end = center + Offset(-90f, 0f),
                    durationMillis = 200
                )
            }
        composeTestRule.waitForIdle()
        assertTrue("Expected inward right-edge drag to shrink width below $stretchedWidth, got ${latestLayout.width}", latestLayout.width < stretchedWidth - 40f)
        assertEquals("Height should stay unchanged on horizontal edge resize", stretchedHeight, latestLayout.height, 0.5f)

        val widthAfterHorizontalShrink = latestLayout.width

        // 3. Drag bottom edge inward -> shrinks height only
        composeTestRule.onNodeWithTag("sticky_resize_height_${note.id}")
            .assertIsDisplayed()
            .performTouchInput {
                swipe(
                    start = center,
                    end = center + Offset(0f, -80f),
                    durationMillis = 200
                )
            }
        composeTestRule.waitForIdle()
        assertTrue("Expected inward bottom-edge drag to shrink height below $stretchedHeight, got ${latestLayout.height}", latestLayout.height < stretchedHeight - 30f)
        assertEquals("Width should stay unchanged on vertical edge resize", widthAfterHorizontalShrink, latestLayout.width, 0.5f)
    }

    @Test
    fun `StickyNoteCanvasItem rotate handle updates and commits rotation angle`() {
        val note = StickyNoteEntity(
            id = "sticky-rot-1",
            title = "Rotate Test",
            content = "Drag bottom-left handle to rotate."
        )
        var latestRotation = 0f

        composeTestRule.setContent {
            NoteLandTheme(darkTheme = false) {
                var layout by remember {
                    mutableStateOf(StickyLayout(id = note.id, x = 120f, y = 120f, width = 200f, height = 200f))
                }
                Box(modifier = Modifier.size(800.dp, 800.dp)) {
                    StickyNoteCanvasItem(
                        note = note,
                        layout = layout,
                        isSelected = true,
                        onSelect = {},
                        onPositionCommit = { _, _ -> },
                        onSizeCommit = { _, _ -> },
                        onRotationCommit = { deg ->
                            layout = layout.copy(rotationDeg = deg)
                            latestRotation = deg
                        },
                        onTogglePin = {},
                        onDuplicate = {},
                        onEdit = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("sticky_rotate_handle_${note.id}")
            .assertIsDisplayed()
            .performTouchInput {
                swipe(
                    start = center,
                    end = center + Offset(70f, 0f),
                    durationMillis = 250
                )
            }
        composeTestRule.waitForIdle()
        assertTrue("Expected rotation drag to produce non-zero angle, got $latestRotation", abs(latestRotation) > 5f)
        composeTestRule.onNodeWithTag("sticky_angle_badge_${note.id}").assertIsDisplayed()
    }
}
