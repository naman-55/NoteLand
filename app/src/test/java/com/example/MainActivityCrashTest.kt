package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityCrashTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun dismissSplashIfPresent() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("starting_animation_overlay").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("starting_animation_overlay").performClick()
        }
        composeRule.mainClock.advanceTimeBy(1200)
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun scrollHomeUntilTagVisible(tag: String, maxSwipes: Int = 8) {
        repeat(maxSwipes) {
            if (composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            composeRule.onNodeWithTag("home_screen_root").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun scrollHomeUntilTextVisible(text: String, maxSwipes: Int = 8) {
        repeat(maxSwipes) {
            if (composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            composeRule.onNodeWithTag("home_screen_root").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun scrollHomeToTop() {
        repeat(6) {
            if (composeRule.onAllNodesWithTag("home_greeting_text").fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            composeRule.onNodeWithTag("home_screen_root").performTouchInput { swipeDown() }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `check splash and icon drawable dimensions within safe canvas memory bounds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val optsSplash = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.noteland_splash, optsSplash)
        val splashBytes = optsSplash.outWidth.toLong() * optsSplash.outHeight.toLong() * 4L

        val optsIcon = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.noteland_icon, optsIcon)
        val iconBytes = optsIcon.outWidth.toLong() * optsIcon.outHeight.toLong() * 4L

        assertTrue("Splash bitmap decoded width > 0", optsSplash.outWidth > 0)
        assertTrue("Icon bitmap decoded width > 0", optsIcon.outWidth > 0)
        assertTrue("Splash bitmap too large ($splashBytes bytes)", splashBytes < 40_000_000L)
        assertTrue("Icon bitmap too large ($iconBytes bytes)", iconBytes < 40_000_000L)
    }

    @Test
    fun `launch MainActivity and exercise primary tabs, editor, canvas and workspace screens`() {
        dismissSplashIfPresent()

        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("home_greeting_text").assertIsDisplayed()

        // Toggle Grid <-> List mode while scrolling Recent Notes
        scrollHomeUntilTagVisible("grid_list_toggle_button")
        if (composeRule.onAllNodesWithTag("grid_list_toggle_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("grid_list_toggle_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("home_screen_root").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            repeat(8) {
                if (composeRule.onAllNodesWithTag("grid_list_toggle_button").fetchSemanticsNodes().isNotEmpty()) return@repeat
                composeRule.onNodeWithTag("home_screen_root").performTouchInput { swipeDown() }
                composeRule.waitForIdle()
            }
            if (composeRule.onAllNodesWithTag("grid_list_toggle_button").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("grid_list_toggle_button").performClick()
                composeRule.waitForIdle()
            }
        }

        // Open seeded notes from Home
        scrollHomeUntilTextVisible("Welcome to NoteLand")
        if (composeRule.onAllNodesWithText("Welcome to NoteLand", substring = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodesWithText("Welcome to NoteLand", substring = true)[0].performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("note_editor_back_button").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("note_editor_back_button").performClick()
                composeRule.waitForIdle()
            } else {
                pressBack()
            }
        }

        scrollHomeUntilTextVisible("Mindmap Sketch")
        if (composeRule.onAllNodesWithText("Mindmap Sketch", substring = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodesWithText("Mindmap Sketch", substring = true)[0].performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("canvas_back_btn").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("canvas_back_btn").performClick()
                composeRule.waitForIdle()
            } else {
                pressBack()
            }
        }

        scrollHomeToTop()

        // Verify Favourites and Tasks are removed from Home's Workspace only (Favourites moved into Folders Quick Access, 5 primary bottom tabs active)
        composeRule.onNodeWithTag("hero_tasks_card").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_studio").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_productivity").assertIsDisplayed()
        assertTrue(
            "Favourites card should not appear in Home Workspace",
            composeRule.onAllNodesWithTag("feature_card_favourites").fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            "Tasks card should not appear in Home Workspace",
            composeRule.onAllNodesWithTag("feature_card_tasks").fetchSemanticsNodes().isEmpty()
        )

        // Bottom navigation tabs & returning Home from each destination
        composeRule.onNodeWithTag("nav_folders").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_studio").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_productivity").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("productivity_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()

        // Multi-hop Folders -> Favourites -> Back -> Studio -> Productivity -> Home -> Folders keeps navigation uncorrupted
        composeRule.onNodeWithTag("nav_folders").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("quick_access_favourites").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("quick_access_favourites").performScrollTo().performClick()
            composeRule.waitForIdle()
            pressBack()
        }
        composeRule.onNodeWithTag("nav_studio").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_productivity").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_folders").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()
        scrollHomeToTop()

        // Create New Note + exercise editor modes, style sheet, smart utilities & drawing
        composeRule.onNodeWithTag("home_new_note_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_title_input").performTextInput("Crash Check Note")
        composeRule.onNodeWithTag("note_content_input").performTextInput("Hello **bold** world *italic*")
        composeRule.waitForIdle()

        if (composeRule.onAllNodesWithTag("markdown_mode_split").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("markdown_mode_split").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("markdown_mode_preview").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("markdown_mode_raw").performClick()
            composeRule.waitForIdle()
        }

        if (composeRule.onAllNodesWithTag("preview_toggle_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("preview_toggle_button").performClick()
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule.onAllNodesWithText("Edit").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("preview_toggle_button").performClick()
            composeRule.waitForIdle()
        }

        if (composeRule.onAllNodesWithTag("note_style_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("note_style_button").performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("style_sheet_done_button").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("style_sheet_done_button").performClick()
                composeRule.waitForIdle()
            } else {
                pressBack()
            }
        }

        if (composeRule.onAllNodesWithTag("note_draw_overlay_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("note_draw_overlay_button").performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("ink_done_btn").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("ink_done_btn").performClick()
                composeRule.waitForIdle()
            }
        }

        composeRule.onNodeWithTag("note_editor_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()

        // Open Calendar & Tasks hero cards + schedule dialog
        scrollHomeToTop()
        composeRule.onNodeWithTag("hero_calendar_card").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("calendar_tab_tasks").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("calendar_tab_tasks").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("calendar_tab_schedule").performClick()
            composeRule.waitForIdle()
        }
        if (composeRule.onAllNodesWithTag("calendar_add_fab").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("calendar_add_fab").performClick()
            composeRule.waitForIdle()
            pressBack()
        }
        pressBack()

        composeRule.onNodeWithTag("hero_tasks_card").performClick()
        composeRule.waitForIdle()
        pressBack()

        // Open feature cards on Home
        val featureTags = listOf(
            "feature_card_diary",
            "feature_card_board",
            "feature_card_sticky wall",
            "feature_card_pinned notes"
        )
        featureTags.forEach { tag ->
            scrollHomeUntilTagVisible(tag)
            composeRule.onNodeWithTag(tag).performClick()
            composeRule.waitForIdle()
            pressBack()
        }

        // Open Search from Home
        scrollHomeToTop()
        if (composeRule.onAllNodesWithTag("search_text_input").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("search_text_input").performClick()
            composeRule.waitForIdle()
            pressBack()
        }

        scrollHomeUntilTagVisible("view_all_button")
        if (composeRule.onAllNodesWithTag("view_all_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("view_all_button").performClick()
            composeRule.waitForIdle()
            pressBack()
        }
    }

    @Test
    fun `exercise secondary tools screens via Folders Quick Access and Settings`() {
        dismissSplashIfPresent()

        // Navigate to Folders tab where Tree view, folders, and Quick Access tools live
        composeRule.onNodeWithTag("nav_folders").performClick()
        composeRule.waitForIdle()

        // Toggle Tree view and expand/collapse
        if (composeRule.onAllNodesWithTag("folders_tree_toggle_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("folders_tree_toggle_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("folders_tree_toggle_button").performClick()
            composeRule.waitForIdle()
        }

        // Open Personal folder and navigate back
        if (composeRule.onAllNodesWithTag("folder_card_Personal").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("folder_card_Personal").performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("inside_folder_back_button").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("inside_folder_back_button").performClick()
                composeRule.waitForIdle()
            } else {
                pressBack()
            }
        }

        // Open PDF Reader from Quick Access
        if (composeRule.onAllNodesWithTag("quick_access_pdf_reader").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("quick_access_pdf_reader").performScrollTo().performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithText("Quick Start Guide.pdf").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithText("Quick Start Guide.pdf").performClick()
                composeRule.waitForIdle()
                pressBack()
            }
            pressBack()
        }

        // Open Locked Folder (Vault) and unlock with default PIN 1234
        if (composeRule.onAllNodesWithTag("quick_access_locked_folder").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("quick_access_locked_folder").performScrollTo().performClick()
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("pin_input_field").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("pin_input_field").performTextInput("1234")
                if (composeRule.onAllNodesWithTag("pin_submit_button").fetchSemanticsNodes().isNotEmpty()) {
                    composeRule.onNodeWithTag("pin_submit_button").performClick()
                }
                composeRule.waitForIdle()
            }
            if (composeRule.onAllNodesWithTag("vault_back_button").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("vault_back_button").performClick()
            } else {
                pressBack()
            }
            composeRule.waitForIdle()
        }

        // Open Archive & Recycle Bin Quick Access tiles
        listOf("Archive", "Recycle Bin", "Reminders", "Media").forEach { label ->
            val nodes = composeRule.onAllNodesWithText(label, substring = false).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                composeRule.onNodeWithText(label, substring = false).performScrollTo().performClick()
                composeRule.waitForIdle()
                pressBack()
            }
        }

        // Settings theme switching + Activity recreation
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        listOf("theme_chip_dark", "theme_chip_ivory", "theme_chip_light").forEach { chipTag ->
            if (composeRule.onAllNodesWithTag(chipTag).fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag(chipTag).performScrollTo().performClick()
                composeRule.waitForIdle()
            }
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w720dp-h1000dp")
    fun `exercise wide screen NavigationRail layout and rotation without crash`() {
        dismissSplashIfPresent()
        composeRule.onNodeWithTag("rail_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("rail_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_editor_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen_root").assertIsDisplayed()
    }
}
