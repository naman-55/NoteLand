package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CanvasEntity
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.mindmap.MindMapLayoutType
import com.example.ui.screens.mindmap.MindMapNode
import com.example.ui.screens.mindmap.MindMapStyleConfig
import com.example.ui.screens.mindmap.MindMapThemePreset
import com.example.ui.screens.mindmap.applyMindMapLayout
import com.example.ui.screens.mindmap.applyThemeToNodes
import com.example.ui.screens.mindmap.createStarterMindMap
import com.example.ui.screens.mindmap.exportMindMap
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.NoteLandTheme
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MindMapStudioTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun awaitMainLooperCondition(timeoutMs: Long = 5000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            ShadowLooper.idleMainLooper()
            if (condition()) return
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Condition was not met within ${timeoutMs}ms")
            }
            Thread.sleep(25)
        }
    }

    @Test
    fun `starter mind map creates Main Topic with 4 primary branches and supports all 6 layouts`() {
        val starter = createStarterMindMap()
        assertEquals(15, starter.size)
        assertEquals("Main Topic", starter.first().title)
        assertEquals(4, starter.count { it.level == 1 })
        assertEquals(10, starter.count { it.level == 2 })

        MindMapLayoutType.entries.forEach { layout ->
            val laidOut = applyMindMapLayout(starter, layout)
            assertEquals("Layout $layout preserved node count", starter.size, laidOut.size)
            assertTrue("Layout $layout spread nodes cleanly", laidOut.map { it.x to it.y }.toSet().size >= 12)
        }

        val ocean = applyThemeToNodes(starter, MindMapThemePreset.OCEAN_FLOW)
        assertEquals(starter.size, ocean.size)
    }

    @Test
    fun `mind map png export creates non-empty image file`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val starter = createStarterMindMap()
        val file = exportMindMap(app, "Test Mind Map", starter, MindMapStyleConfig(), "png")
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 100L)
        file.delete()
    }

    @Test
    fun `TC1 Light Theme - blank canvas opens immediately, manual node creation and inline rename persist`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        var createdId: String? = null
        vm.createMindMapProject { createdId = it }
        awaitMainLooperCondition { createdId != null }
        val docId = createdId!!

        var observedDark: Boolean? = null
        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                observedDark = isAppInDarkTheme
                CanvasScreen(
                    noteId = docId,
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        assertEquals(false, observedDark)
        composeRule.onNodeWithText("Untitled Mind Map").assertIsDisplayed()

        // Blank initially -> 0 mind map nodes in Room
        val initialNodes = runBlocking {
            vm.repository.getElementsForCanvasDirect(docId).mapNotNull { MindMapNode.fromEntity(it) }
        }
        assertEquals(0, initialNodes.size)

        // Manually add Central Topic + 1 child Topic
        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()

        // Inline rename
        composeRule.onNodeWithTag("canvas_title_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_title_input").performTextClearance()
        composeRule.onNodeWithTag("canvas_title_input").performTextInput("Sprint Plan")
        composeRule.onNodeWithTag("canvas_title_input").performImeAction()
        composeRule.waitForIdle()

        // Trigger save and verify persistence
        composeRule.onNodeWithTag("canvas_save_btn").performClick()
        composeRule.waitForIdle()
        val persistedCanvas = runBlocking {
            delay(250)
            vm.getCanvasByIdDirect(docId)
        }
        val persistedNodes = runBlocking {
            vm.repository.getElementsForCanvasDirect(docId).mapNotNull { MindMapNode.fromEntity(it) }
        }
        assertNotNull(persistedCanvas)
        assertEquals("Sprint Plan", persistedCanvas!!.title)
        assertEquals(2, persistedNodes.size)
        assertEquals("Central Topic", persistedNodes.first().title)
    }

    @Test
    fun `TC2 Dark Theme - blank canvas opens with identical tools, nodes and rename persist`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        var createdId: String? = null
        vm.createMindMapProject { createdId = it }
        awaitMainLooperCondition { createdId != null }
        val docId = createdId!!

        var observedDark: Boolean? = null
        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.DARK) {
                observedDark = isAppInDarkTheme
                CanvasScreen(
                    noteId = docId,
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        assertEquals(true, observedDark)
        composeRule.onNodeWithText("Untitled Mind Map").assertIsDisplayed()
        composeRule.onNodeWithTag("mindmap_panel_insert").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_floating_layers_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_export_btn").assertIsDisplayed()

        // Create Central Topic manually & verify layers shows 1 node
        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_floating_layers_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_layers_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Layers (1 nodes · 0 strokes/items)").assertIsDisplayed()
    }

    @Test
    fun `TC3 Live Theme Switching - switching Light to Dark and back preserves canvas state without reload`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        vm.setThemeMode(AppThemeMode.LIGHT.key)
        var observedDark by mutableStateOf(false)

        composeRule.setContent {
            val modeKey by vm.themeMode.collectAsStateWithLifecycle()
            NoteLandTheme(themeMode = AppThemeMode.fromKey(modeKey)) {
                observedDark = isAppInDarkTheme
                CanvasScreen(
                    noteId = "0",
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        assertFalse(observedDark)
        // Add a single central topic and inline-rename
        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_title_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_title_input").performTextClearance()
        composeRule.onNodeWithTag("canvas_title_input").performTextInput("Live Theme Map")
        composeRule.onNodeWithTag("canvas_title_input").performImeAction()
        composeRule.waitForIdle()

        // Switch to DARK live without recreating CanvasScreen
        vm.setThemeMode(AppThemeMode.DARK.key)
        composeRule.waitForIdle()
        assertTrue(observedDark)
        composeRule.onNodeWithText("Live Theme Map").assertIsDisplayed()
        composeRule.onNodeWithTag("mindmap_canvas").assertIsDisplayed()

        // Switch back to LIGHT live
        vm.setThemeMode(AppThemeMode.LIGHT.key)
        composeRule.waitForIdle()
        assertFalse(observedDark)
        composeRule.onNodeWithText("Live Theme Map").assertIsDisplayed()

        // Verify central node remained intact via Layers sheet
        composeRule.onNodeWithTag("canvas_floating_layers_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Layers (1 nodes · 0 strokes/items)").assertIsDisplayed()
    }

    @Test
    fun `TC4 Blank Canvas Validation - StudioScreen creates Mind Map directly with zero nodes and no naming modal`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        var navigatedCanvasId: String? = null

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                StudioScreen(
                    viewModel = vm,
                    onNavigateToCanvas = { id -> navigatedCanvasId = id },
                    onNavigateToDestination = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_card_mindmap").performClick()
        composeRule.waitForIdle()

        // No naming modal should be displayed
        composeRule.onAllNodesWithTag("studio_project_title_input").assertCountEquals(0)
        awaitMainLooperCondition { navigatedCanvasId != null }

        val newId = navigatedCanvasId!!
        val savedCanvas = runBlocking { vm.getCanvasByIdDirect(newId) }
        val savedNodes = runBlocking {
            vm.repository.getElementsForCanvasDirect(newId).mapNotNull { MindMapNode.fromEntity(it) }
        }
        val savedOtherItems = runBlocking {
            vm.repository.getElementsForCanvasDirect(newId).filter {
                it.elementType != MindMapNode.ELEMENT_TYPE_MINDMAP_NODE &&
                    it.elementType != com.example.ui.screens.mindmap.MindMapStyleConfig.ELEMENT_TYPE_MINDMAP_CONFIG
            }
        }
        assertNotNull(savedCanvas)
        assertEquals("Untitled Mind Map", savedCanvas!!.title)
        assertEquals(0, savedNodes.size)
        assertEquals(0, savedOtherItems.size)
    }

    @Test
    fun `Freeform Canvas opens as Untitled without naming modal and supports inline renaming`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        var navigatedCanvasId by mutableStateOf<String?>(null)

        composeRule.setContent {
            val activeId = navigatedCanvasId
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                if (activeId == null) {
                    StudioScreen(
                        viewModel = vm,
                        onNavigateToCanvas = { id -> navigatedCanvasId = id },
                        onNavigateToDestination = {}
                    )
                } else {
                    CanvasScreen(
                        noteId = activeId,
                        viewModel = vm,
                        onNavigateBack = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_card_canvas").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("studio_project_title_input").assertCountEquals(0)
        awaitMainLooperCondition { navigatedCanvasId != null }

        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        val docId = navigatedCanvasId!!
        composeRule.onNodeWithText("Untitled").assertIsDisplayed()

        // Rename inline from "Untitled" to "Architecture Sketch"
        composeRule.onNodeWithTag("canvas_title_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_title_input").performTextClearance()
        composeRule.onNodeWithTag("canvas_title_input").performTextInput("Architecture Sketch")
        composeRule.onNodeWithTag("canvas_title_input").performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_save_btn").performClick()
        composeRule.waitForIdle()

        val canvases = runBlocking {
            delay(250)
            vm.repository.getAllCanvasesDirect()
        }
        assertEquals(1, canvases.count { it.id == docId })
        assertEquals("Architecture Sketch", canvases.first { it.id == docId }.title)
    }

    @Test
    fun `TC5 Naming Workflow - inline rename updates same document without duplicates and empty title does not block tools`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        var createdId: String? = null
        vm.createMindMapProject { createdId = it }
        awaitMainLooperCondition { createdId != null }
        val docId = createdId!!

        composeRule.setContent {
            NoteLandTheme(themeMode = AppThemeMode.LIGHT) {
                CanvasScreen(
                    noteId = docId,
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        // Clear title to empty and verify canvas tool still works immediately (not blocked)
        composeRule.onNodeWithTag("canvas_title_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("canvas_title_input").performTextClearance()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()

        // Verify canvas tool added Central Topic while title remained editable (not blocked)
        composeRule.onNodeWithTag("canvas_title_input").performTextInput("Product Strategy")
        composeRule.onNodeWithTag("canvas_title_input").performImeAction()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("canvas_save_btn").performClick()
        composeRule.waitForIdle()

        val allCanvases = runBlocking {
            delay(250)
            vm.repository.getAllCanvasesDirect()
        }
        val savedNodes = runBlocking {
            vm.repository.getElementsForCanvasDirect(docId).mapNotNull { MindMapNode.fromEntity(it) }
        }
        assertEquals(1, savedNodes.size)
        assertEquals(1, allCanvases.count { it.id == docId })
        assertEquals("Product Strategy", allCanvases.first { it.id == docId }.title)
    }

    @Test
    fun `TC6 Existing Documents - pre-existing nodes and positions remain intact across Light and Dark themes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)
        val docId = "existing-mm-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val rootNode = MindMapNode(id = "n_root", parentId = null, title = "Architecture", level = 0, x = 15f, y = -20f)
        val childNode = MindMapNode(id = "n_child", parentId = "n_root", title = "Storage", level = 1, x = 225f, y = 40f)
        val cfg = MindMapStyleConfig()

        runBlocking {
            vm.repository.saveCanvasWithElements(
                CanvasEntity(
                    id = docId,
                    title = "Roadmap Q4",
                    createdAtUtc = now,
                    updatedAtUtc = now
                ),
                listOf(cfg.toEntity(docId), rootNode.toEntity(docId, 0), childNode.toEntity(docId, 1))
            )
        }

        vm.setThemeMode(AppThemeMode.LIGHT.key)
        composeRule.setContent {
            val modeKey by vm.themeMode.collectAsStateWithLifecycle()
            NoteLandTheme(themeMode = AppThemeMode.fromKey(modeKey)) {
                CanvasScreen(
                    noteId = docId,
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Roadmap Q4").assertIsDisplayed()

        // Switch to Dark theme and verify document wasn't overwritten or reset
        vm.setThemeMode(AppThemeMode.DARK.key)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Roadmap Q4").assertIsDisplayed()

        val reloadedNodes = runBlocking {
            vm.repository.getElementsForCanvasDirect(docId).mapNotNull { MindMapNode.fromEntity(it) }
        }
        assertEquals(2, reloadedNodes.size)
        val loadedRoot = reloadedNodes.first { it.id == "n_root" }
        val loadedChild = reloadedNodes.first { it.id == "n_child" }
        assertEquals("Architecture", loadedRoot.title)
        assertEquals(15f, loadedRoot.x, 0.01f)
        assertEquals(-20f, loadedRoot.y, 0.01f)
        assertEquals("Storage", loadedChild.title)
        assertEquals(225f, loadedChild.x, 0.01f)
        assertEquals(40f, loadedChild.y, 0.01f)
    }

    @Test
    fun `CanvasScreen renders Mind Map Studio header and all 5 bottom drawer tabs smoothly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = NoteViewModel(app)

        composeRule.setContent {
            NoteLandTheme(darkTheme = false) {
                CanvasScreen(
                    noteId = "0",
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        // Top header bar elements
        composeRule.onNodeWithText("Untitled Mind Map").assertIsDisplayed()
        composeRule.onNodeWithText("Saved").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_export_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("canvas_floating_layers_btn").assertIsDisplayed()

        // Default Insert tab open on blank canvas -> clicking Topic creates Central Topic
        composeRule.onNodeWithTag("mindmap_panel_insert").assertIsDisplayed()
        composeRule.onNodeWithTag("insert_tool_select").assertIsDisplayed()
        composeRule.onNodeWithTag("insert_tool_topic_box").performClick()
        composeRule.waitForIdle()

        // Switch to Layout tab & trigger layout + quick action
        composeRule.onNodeWithTag("mindmap_tab_layout").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_panel_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_tile_hierarchical").performClick()
        composeRule.onNodeWithTag("quick_action_add_subtopic").performClick()
        composeRule.waitForIdle()

        // Switch to Style tab & pick theme
        composeRule.onNodeWithTag("mindmap_tab_style").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_panel_style").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_card_ocean_flow").performClick()
        composeRule.onNodeWithTag("style_subtab_node").performClick()
        composeRule.waitForIdle()

        // Switch to Connect tab & adjust connector type + arrowhead
        composeRule.onNodeWithTag("mindmap_tab_connect").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_panel_connect").assertIsDisplayed()
        composeRule.onNodeWithTag("connector_type_orthogonal").performClick()
        composeRule.onNodeWithTag("arrowhead_filled_arrow").performClick()
        composeRule.waitForIdle()

        // Switch to Tools tab & open Layers sheet (1 Central Topic + 1 Subtopic = 2 nodes)
        composeRule.onNodeWithTag("mindmap_tab_tools").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_panel_tools").assertIsDisplayed()
        composeRule.onNodeWithTag("tools_layers").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mindmap_layers_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Layers (2 nodes · 0 strokes/items)").assertIsDisplayed()
    }
}
