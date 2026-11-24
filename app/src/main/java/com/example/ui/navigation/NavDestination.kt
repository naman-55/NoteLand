package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String) {
    // Primary Destinations (5-section NoteLand architecture)
    object Home : Screen("home", "Home")
    object Folders : Screen("folders", "Folders")
    object Studio : Screen("studio", "Studio")
    object Favorites : Screen("favorites", "Favourites")
    object Settings : Screen("settings", "Settings")

    // Full Notes Explorer & Search
    object Notes : Screen("notes", "Notes")
    object Search : Screen("search", "Search")

    // Studio & Utility destinations
    object Canvas : Screen("canvas", "Board")
    object Media : Screen("media", "Media")
    object StickyBoard : Screen("sticky_board", "Sticky Wall")
    object Vault : Screen("vault", "Secure Vault")
    object Archive : Screen("archive", "Archive")
    object RecycleBin : Screen("recycle_bin", "Recycle Bin")
    object Utilities : Screen("utilities", "Smart Utilities")
    object Reminders : Screen("reminders", "Reminders")
    object Calendar : Screen("calendar", "My Calendar")
    object Tasks : Screen("tasks", "Tasks")
    object PinnedNotes : Screen("pinned_notes", "Pinned Notes")
    object Productivity : Screen("productivity?tab={tab}", "Productivity") {
        fun createRoute(tab: Int = 0): String = "productivity?tab=$tab"
    }
    object PdfTools : Screen("pdf_tools", "PDF Reader")
    object NoteEditor : Screen("note_editor/{noteId}?type={noteType}&folderId={folderId}", "Editor") {
        fun createRoute(noteId: String = "0", type: String = "STANDARD", folderId: String = ""): String {
            val encFolder = java.net.URLEncoder.encode(folderId, "UTF-8")
            return "note_editor/$noteId?type=$type&folderId=$encFolder"
        }
    }
    object ImageAnnotator : Screen("image_annotator?attachmentId={attachmentId}&filePath={filePath}&fileName={fileName}", "Image Annotator") {
        fun createRoute(attachmentId: String, filePath: String, fileName: String): String {
            val encPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val encName = java.net.URLEncoder.encode(fileName, "UTF-8")
            return "image_annotator?attachmentId=$attachmentId&filePath=$encPath&fileName=$encName"
        }
    }
    object PdfReader : Screen("pdf_reader?attachmentId={attachmentId}&filePath={filePath}&fileName={fileName}&initialPage={initialPage}", "PDF Reader") {
        fun createRoute(attachmentId: String, filePath: String, fileName: String, initialPage: Int = 1): String {
            val encPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val encName = java.net.URLEncoder.encode(fileName, "UTF-8")
            return "pdf_reader?attachmentId=$attachmentId&filePath=$encPath&fileName=$encName&initialPage=${initialPage.coerceAtLeast(1)}"
        }
    }
}

data class NavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

val PrimaryNavItems = listOf(
    NavItem(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavItem(Screen.Folders, Icons.Filled.Folder, Icons.Outlined.Folder, "Folders"),
    NavItem(Screen.Studio, Icons.Filled.Brush, Icons.Outlined.Brush, "Studio"),
    NavItem(Screen.Productivity, Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges, "Productivity"),
    NavItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
)
