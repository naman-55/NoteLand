package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.navigation.PrimaryNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductivityScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.widget.WidgetNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationRestructureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `primary navigation exposes 5 sections and preserves existing widget routes`() {
        assertEquals(5, PrimaryNavItems.size)
        assertEquals(
            listOf(Screen.Home, Screen.Folders, Screen.Studio, Screen.Productivity, Screen.Settings),
            PrimaryNavItems.map { it.screen }
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            Screen.Tasks.route,
            Screen.Reminders.route,
            Screen.Favorites.route,
            Screen.StickyBoard.route,
            Screen.Canvas.route,
            Screen.PdfTools.route,
            Screen.Search.route,
            Screen.Productivity.createRoute(0)
        ).forEach { route ->
            val intent = WidgetNavigation.routeIntent(context, route)
            assertNotNull("Intent for $route should not be null", intent)
        }
    }

    @Test
    fun `HomeScreen Quick Create FAB opens sheet with all 6 creation targets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                HomeScreen(
                    viewModel = vm,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateToCanvas = {},
                    onNavigateToScreen = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_global_search_button").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_fab").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_create_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_note").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_task").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_event").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_mindmap").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_create_sticky").assertIsDisplayed()
    }

    @Test
    fun `StudioScreen displays Canvas, Mind Map, and Sticky Wall hubs without Brain`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                StudioScreen(
                    viewModel = vm,
                    onNavigateToCanvas = {},
                    onNavigateToDestination = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("studio_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_card_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_card_mindmap").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_card_sticky_wall").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("studio_card_brain").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `CalendarScreen switches Daily Weekly and Monthly views cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                CalendarScreen(
                    viewModel = vm,
                    initialTab = 0,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateToProductivity = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calendar_view_daily").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_daily_header").assertIsDisplayed()

        composeRule.onNodeWithTag("calendar_view_monthly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_monthly_grid").assertIsDisplayed()

        composeRule.onNodeWithTag("calendar_view_weekly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_timeblock_picker_btn").assertIsDisplayed()
    }

    @Test
    fun `ProductivityScreen exposes Planner and Goals tabs and ViewModel goal CRUD works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        val initialCount = vm.goals.value.size
        vm.addGoal(title = "Read 10 books", targetValue = 10, unit = "books")
        val added = vm.goals.value.firstOrNull { it.title == "Read 10 books" }
        assertNotNull(added)
        assertEquals(initialCount + 1, vm.goals.value.size)

        vm.incrementGoal(added!!.id, 2)
        val incremented = vm.goals.value.first { it.id == added.id }
        assertEquals(2, incremented.currentValue)
        assertFalse(incremented.isCompleted)

        vm.toggleGoalCompleted(added.id)
        assertTrue(vm.goals.value.first { it.id == added.id }.isCompleted)

        composeRule.setContent {
            NoteLandTheme {
                ProductivityScreen(
                    viewModel = vm,
                    initialTab = 0,
                    isRootTab = true,
                    onNavigateToCalendar = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("productivity_tab_planner").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("planner_tab_root").assertIsDisplayed()

        composeRule.onNodeWithTag("productivity_tab_goals").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("goals_tab_root").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_tab_root").performScrollToNode(hasText("Read 10 books"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Read 10 books").assertIsDisplayed()

        vm.deleteGoal(added.id)
    }

    @Test
    fun `BackupSettingsScreen organizes Editor Customization, Backup & Sync, and Storage sections`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                BackupSettingsScreen(
                    viewModel = vm,
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        listOf(
            "settings_quick_edit_row",
            "settings_backup_export_row",
            "settings_backup_restore_row",
            "settings_storage_summary_row",
            "settings_empty_trash_row"
        ).forEach { tag ->
            composeRule.onNodeWithTag("settings_lazy_column").performScrollToNode(hasTestTag(tag))
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun `SearchScreen searches across Tasks and Studio filters`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        vm.saveTask(
            TaskEntity(
                id = "search_test_task_1",
                title = "Prepare Quarterly Budget",
                priority = TaskPriority.HIGH.name,
                category = "Work"
            )
        )

        composeRule.setContent {
            NoteLandTheme {
                SearchScreen(
                    viewModel = vm,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateToCanvas = {},
                    onNavigateToDestination = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("filter_chip_TASKS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_chip_STUDIO").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_chip_ALL").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("global_search_input").performTextInput("Quarterly")
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Prepare Quarterly Budget", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithText("Prepare Quarterly Budget", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }
}
