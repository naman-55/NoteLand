package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.NoteType
import com.example.ui.components.FloatingGlassBottomBar
import com.example.ui.components.StartingAnimationOverlay
import com.example.ui.navigation.PrimaryNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.PinnedNotesScreen
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaScreen
import com.example.ui.screens.ImageAnnotatorScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.ProductivityScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StickyBoardScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel

class MainActivity : FragmentActivity() {
    private val pendingNavigateNoteId = mutableStateOf<String?>(null)
    private val pendingWidgetRoute = mutableStateOf<String?>(null)
    private val pendingWidgetAction = mutableStateOf<String?>(null)
    private val pendingWidgetFolderId = mutableStateOf<String?>(null)
    
    private val pendingExternalUris = mutableStateOf<List<android.net.Uri>>(emptyList())
    private val pendingExternalAction = mutableStateOf<String?>(null)

    private fun extractLaunchExtras(launchIntent: Intent?) {
        if (launchIntent == null) return
        
        // Handle Action View / Action Send (External Documents)
        val action = launchIntent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = mutableListOf<android.net.Uri>()
            if (action == Intent.ACTION_VIEW) {
                launchIntent.data?.let { uris.add(it) }
            } else if (action == Intent.ACTION_SEND) {
                (launchIntent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))?.let { uris.add(it) }
            } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                launchIntent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
            if (uris.isNotEmpty()) {
                pendingExternalAction.value = action
                pendingExternalUris.value = uris.distinct()
            }
        }

        val noteId = launchIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_NOTE_ID)
        val route = launchIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ROUTE)
        val widgetAction = launchIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ACTION)
        val folderId = launchIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_FOLDER_ID)
        if (!noteId.isNullOrBlank()) pendingNavigateNoteId.value = noteId
        if (!route.isNullOrBlank()) pendingWidgetRoute.value = route
        if (!widgetAction.isNullOrBlank()) pendingWidgetAction.value = widgetAction
        if (!folderId.isNullOrBlank()) pendingWidgetFolderId.value = folderId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractLaunchExtras(intent)
        setContent {
            NoteLandApp(
                pendingNavigateNoteId = pendingNavigateNoteId.value,
                onConsumeNavigateNoteId = { pendingNavigateNoteId.value = null },
                pendingWidgetRoute = pendingWidgetRoute.value,
                pendingWidgetAction = pendingWidgetAction.value,
                pendingWidgetFolderId = pendingWidgetFolderId.value,
                onConsumeWidgetLaunch = {
                    pendingWidgetRoute.value = null
                    pendingWidgetAction.value = null
                    pendingWidgetFolderId.value = null
                },
                pendingExternalUris = pendingExternalUris.value,
                pendingExternalAction = pendingExternalAction.value,
                onConsumeExternalUris = {
                    pendingExternalUris.value = emptyList()
                    pendingExternalAction.value = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractLaunchExtras(intent)
    }
}

@Composable
fun NoteLandApp(
    viewModel: NoteViewModel = viewModel(),
    pendingNavigateNoteId: String? = null,
    onConsumeNavigateNoteId: () -> Unit = {},
    pendingWidgetRoute: String? = null,
    pendingWidgetAction: String? = null,
    pendingWidgetFolderId: String? = null,
    onConsumeWidgetLaunch: () -> Unit = {},
    pendingExternalUris: List<android.net.Uri> = emptyList(),
    pendingExternalAction: String? = null,
    onConsumeExternalUris: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val editorFontSizeSp by viewModel.editorFontSizeSp.collectAsStateWithLifecycle()
    val resolvedTheme = AppThemeMode.fromKey(themeMode)
    val activity = LocalActivity.current as? ComponentActivity

    LaunchedEffect(resolvedTheme, activity) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (resolvedTheme == AppThemeMode.DARK) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
            navigationBarStyle = if (resolvedTheme == AppThemeMode.DARK) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.handleVaultExitOrBackground()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkAutoLockExpiration()
                viewModel.restorePomodoroFromPrefs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val autoLockTimeoutMs by viewModel.autoLockTimeoutMs.collectAsStateWithLifecycle()
    val vaultSessionEpoch by viewModel.vaultSessionEpoch.collectAsStateWithLifecycle()
    LaunchedEffect(isVaultUnlocked, autoLockTimeoutMs, vaultSessionEpoch, lifecycleOwner) {
        if (isVaultUnlocked && autoLockTimeoutMs > 0L) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val remainingMs = viewModel.getRemainingAutoLockDelayMs()
                if (remainingMs == 0L) {
                    viewModel.checkAutoLockExpiration()
                } else if (remainingMs < Long.MAX_VALUE) {
                    kotlinx.coroutines.delay(remainingMs)
                    viewModel.checkAutoLockExpiration()
                }
            }
        }
    }

    val hostView = LocalView.current
    val safeTextToolbar = remember(hostView) {
        com.example.ui.components.SafeAndroidTextToolbar(hostView)
    }
    DisposableEffect(safeTextToolbar) {
        onDispose { safeTextToolbar.hide() }
    }

    NoteLandTheme(
        themeMode = resolvedTheme,
        dynamicColor = dynamicColor,
        textSizeSp = editorFontSizeSp
    ) {
        CompositionLocalProvider(LocalTextToolbar provides safeTextToolbar) {
            Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LaunchedEffect(currentRoute) {
                viewModel.checkAutoLockExpiration()
                val isEditorOrViewer = currentRoute != null && (
                    currentRoute.startsWith("note_editor") ||
                        currentRoute.startsWith("canvas") ||
                        currentRoute.startsWith("pdf_reader") ||
                        currentRoute.startsWith("image_annotator")
                    )
                if (currentRoute != null && currentRoute != Screen.Vault.route && !isEditorOrViewer) {
                    viewModel.handleVaultExitOrBackground()
                }
            }

            LaunchedEffect(pendingNavigateNoteId) {
                val targetId = pendingNavigateNoteId
                if (!targetId.isNullOrBlank()) {
                    val note = viewModel.getNoteByIdDirect(targetId)
                    onConsumeNavigateNoteId()
                    if (note != null && !note.isDeleted) {
                        if (note.noteType == NoteType.CANVAS.name) {
                            navController.navigate("canvas?noteId=${note.id}")
                        } else {
                            navController.navigate("note_editor/${note.id}?type=${note.noteType}")
                        }
                    }
                }
            }

            LaunchedEffect(pendingWidgetRoute, pendingWidgetAction, pendingWidgetFolderId) {
                val route = pendingWidgetRoute
                val action = pendingWidgetAction
                val folderId = pendingWidgetFolderId
                if (!route.isNullOrBlank() || !action.isNullOrBlank()) {
                    onConsumeWidgetLaunch()
                    if (!route.isNullOrBlank()) {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    } else if (!action.isNullOrBlank()) {
                        when (action) {
                            com.example.widget.WidgetNavigation.ACTION_NEW_NOTE -> {
                                navController.navigate("note_editor/0?type=${NoteType.STANDARD.name}")
                            }
                            com.example.widget.WidgetNavigation.ACTION_NEW_CHECKLIST -> {
                                navController.navigate("note_editor/0?type=CHECKLIST")
                            }
                            com.example.widget.WidgetNavigation.ACTION_OPEN_DAILY_DIARY -> {
                                viewModel.openOrCreateDailyDiary { noteId, noteType ->
                                    navController.navigate("note_editor/$noteId?type=$noteType")
                                }
                            }
                            com.example.widget.WidgetNavigation.ACTION_NEW_FOLDER -> {
                                viewModel.requestNewFolderFromWidget()
                                navController.navigate(Screen.Folders.route) {
                                    launchSingleTop = true
                                }
                            }
                            com.example.widget.WidgetNavigation.ACTION_OPEN_FOLDER -> {
                                if (!folderId.isNullOrBlank()) {
                                    viewModel.requestOpenFolderFromWidget(folderId)
                                }
                                navController.navigate(Screen.Folders.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }

            var showStartingAnimation by rememberSaveable {
                mutableStateOf(
                    pendingNavigateNoteId.isNullOrBlank() &&
                        pendingWidgetRoute.isNullOrBlank() &&
                        pendingWidgetAction.isNullOrBlank()
                )
            }

            val isPrimaryDestination = currentRoute in listOf(
                Screen.Home.route,
                Screen.Notes.route,
                Screen.Folders.route,
                Screen.Studio.route,
                Screen.Productivity.route,
                Screen.Settings.route
            )
            val activePrimaryRoute = when {
                currentRoute == Screen.Notes.route -> Screen.Home.route
                currentRoute?.startsWith("productivity") == true -> Screen.Productivity.route
                else -> currentRoute
            }

            if (pendingExternalUris.isNotEmpty()) {
                com.example.ui.components.ExternalDocumentHandler(
                    uris = pendingExternalUris,
                    action = pendingExternalAction,
                    viewModel = viewModel,
                    onDismiss = onConsumeExternalUris,
                    onNavigateToMedia = { attId ->
                        navController.navigate("image_annotator/$attId")
                    },
                    onNavigateToPdf = { attId ->
                        navController.navigate("pdf_reader/$attId")
                    }
                )
            }

            val navigateToPrimaryTab: (Screen) -> Unit = { target ->
                val activeRoute = navController.currentBackStackEntry?.destination?.route
                val targetNavRoute = if (target == Screen.Productivity) {
                    Screen.Productivity.createRoute(0)
                } else {
                    target.route
                }
                if (target == Screen.Home) {
                    if (activeRoute != Screen.Home.route) {
                        val poppedToHome = navController.popBackStack(Screen.Home.route, inclusive = false)
                        if (!poppedToHome) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                } else if (activeRoute != target.route) {
                    navController.navigate(targetNavRoute) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen) {
                        val isDark = com.example.ui.theme.isAppInDarkTheme
                        // Adaptive Navigation Rail Layout for Tablets, Foldables & Landscape
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = 0.dp),
                            containerColor = com.example.ui.theme.editorialCardContainerColor(isDark),
                            header = {
                                FloatingActionButton(
                                    onClick = {
                                        navController.navigate("note_editor/0?type=${NoteType.STANDARD.name}")
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = if (isDark) com.example.ui.theme.DarkHomeAccent else com.example.ui.theme.LightHomeAccent,
                                    contentColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF072110) else androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier
                                        .padding(top = 8.dp, bottom = 16.dp)
                                        .testTag("rail_fab")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Note")
                                }
                            }
                        ) {
                            PrimaryNavItems.forEach { item ->
                                val isSelected = activePrimaryRoute == item.screen.route
                                val section = when (item.screen) {
                                    Screen.Home -> com.example.ui.theme.AppSection.HOME
                                    Screen.Folders -> com.example.ui.theme.AppSection.FOLDERS
                                    Screen.Studio -> com.example.ui.theme.AppSection.STUDIO
                                    Screen.Productivity -> com.example.ui.theme.AppSection.PRODUCTIVITY
                                    Screen.Settings -> com.example.ui.theme.AppSection.SETTINGS
                                    else -> com.example.ui.theme.AppSection.HOME
                                }
                                val sectionPalette = com.example.ui.theme.SectionColorPalette.forSection(section, isDark)
                                NavigationRailItem(
                                    icon = {
                                        Icon(
                                            if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    selected = isSelected,
                                    colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                        selectedIconColor = if (isDark) sectionPalette.onTonalContainer else sectionPalette.accent,
                                        selectedTextColor = sectionPalette.accent,
                                        indicatorColor = sectionPalette.tonalContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = { navigateToPrimaryTab(item.screen) }
                                )
                            }
                        }

                        androidx.compose.material3.VerticalDivider(
                            thickness = 0.8.dp,
                            color = com.example.ui.theme.editorialCardBorderColor(isDark)
                        )
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (!isWideScreen && isPrimaryDestination) {
                                FloatingGlassBottomBar(
                                    items = PrimaryNavItems,
                                    currentRoute = activePrimaryRoute,
                                    onItemSelected = { item -> navigateToPrimaryTab(item.screen) }
                                )
                            }
                        }
                    ) { _ ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppNavHost(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            if (showStartingAnimation) {
                StartingAnimationOverlay(
                    onFinished = { showStartingAnimation = false }
                )
            }
        }
        }
    }
}

private fun primaryTabOrder(route: String?): Int = when {
    route == Screen.Home.route || route == Screen.Notes.route -> 0
    route == Screen.Folders.route -> 1
    route == Screen.Studio.route -> 2
    route?.startsWith("productivity") == true -> 3
    route == Screen.Settings.route -> 4
    else -> -1
}

@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: NoteViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            val fromIdx = primaryTabOrder(initialState.destination.route)
            val toIdx = primaryTabOrder(targetState.destination.route)
            if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                val dir = if (toIdx > fromIdx) 1 else -1
                slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> dir * (fullWidth / 5) }
                ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.975f, animationSpec = tween(240, easing = FastOutSlowInEasing))
            } else {
                slideInHorizontally(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> fullWidth / 10 }
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        exitTransition = {
            val fromIdx = primaryTabOrder(initialState.destination.route)
            val toIdx = primaryTabOrder(targetState.destination.route)
            if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                val dir = if (toIdx > fromIdx) -1 else 1
                slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> dir * (fullWidth / 6) }
                ) + fadeOut(animationSpec = tween(170))
            } else {
                fadeOut(animationSpec = tween(160))
            }
        },
        popEnterTransition = {
            val fromIdx = primaryTabOrder(initialState.destination.route)
            val toIdx = primaryTabOrder(targetState.destination.route)
            if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                val dir = if (toIdx > fromIdx) 1 else -1
                slideInHorizontally(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> dir * (fullWidth / 5) }
                ) + fadeIn(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> -fullWidth / 12 }
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> fullWidth / 10 }
            ) + fadeOut(animationSpec = tween(160))
        }
    ) {
        // Destination 1: Home (Clean, Focused Notes Explorer)
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToScreen = { route ->
                    navController.navigate(route)
                }
            )
        }

        // Destination 2: Folders
        composable(Screen.Folders.route) {
            FoldersScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                },
                onCreateNoteInFolder = { folderId ->
                    navController.navigate(Screen.NoteEditor.createRoute("0", NoteType.STANDARD.name, folderId))
                }
            )
        }

        // Destination 3: Studio (Canvas, Mind Map & Sticky Wall)
        composable(Screen.Studio.route) {
            StudioScreen(
                viewModel = viewModel,
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                }
            )
        }

        // Sub-route: Favorites (accessible from Folders Quick Access & Search)
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                }
            )
        }

        // Destination 4: Settings & Appearance
        composable(Screen.Settings.route) {
            BackupSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                }
            )
        }

        // Global Search
        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Destination: Notes Explorer
        composable(Screen.Notes.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToScreen = { route ->
                    navController.navigate(route)
                }
            )
        }

        // Studio: Canvas / Whiteboard
        composable(Screen.Canvas.route) {
            CanvasScreen(
                noteId = "0",
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Media & Documents
        composable(Screen.Media.route) {
            MediaScreen(
                viewModel = viewModel,
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Image Viewer & Annotator
        composable(
            route = Screen.ImageAnnotator.route,
            arguments = listOf(
                navArgument("attachmentId") { type = NavType.StringType; defaultValue = "" },
                navArgument("filePath") { type = NavType.StringType; defaultValue = "" },
                navArgument("fileName") { type = NavType.StringType; defaultValue = "Image" }
            )
        ) { backStackEntry ->
            val attachmentId = backStackEntry.arguments?.getString("attachmentId") ?: ""
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val rawName = backStackEntry.arguments?.getString("fileName") ?: "Image"
            val filePath = try { java.net.URLDecoder.decode(rawPath, "UTF-8") } catch (_: Exception) { rawPath }
            val fileName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }

            ImageAnnotatorScreen(
                attachmentId = attachmentId,
                filePath = filePath,
                fileName = fileName,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // PDF Reader & Annotation
        composable(
            route = Screen.PdfReader.route,
            arguments = listOf(
                navArgument("attachmentId") { type = NavType.StringType; defaultValue = "" },
                navArgument("filePath") { type = NavType.StringType; defaultValue = "" },
                navArgument("fileName") { type = NavType.StringType; defaultValue = "Document.pdf" },
                navArgument("initialPage") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val attachmentId = backStackEntry.arguments?.getString("attachmentId") ?: ""
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val rawName = backStackEntry.arguments?.getString("fileName") ?: "Document.pdf"
            val initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 1
            val filePath = try { java.net.URLDecoder.decode(rawPath, "UTF-8") } catch (_: Exception) { rawPath }
            val fileName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }

            PdfReaderScreen(
                attachmentId = attachmentId,
                filePath = filePath,
                fileName = fileName,
                viewModel = viewModel,
                initialPage = initialPage,
                onNavigateToNote = { targetId, targetType ->
                    if (targetType == NoteType.CANVAS.name) {
                        navController.navigate("canvas?noteId=$targetId")
                    } else {
                        navController.navigate("note_editor/$targetId?type=$targetType")
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // PDF Hub & Office Tools Suite (Matching image.png left screen)
        composable(Screen.PdfTools.route) {
            PdfToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenPdf = { attachmentId, filePath, fileName ->
                    navController.navigate(Screen.PdfReader.createRoute(attachmentId, filePath, fileName))
                }
            )
        }

        // Sub-routes
        composable(
            route = "note_editor/{noteId}?type={noteType}&folderId={folderId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    defaultValue = "0"
                },
                navArgument("noteType") {
                    type = NavType.StringType
                    defaultValue = "STANDARD"
                },
                navArgument("folderId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: "0"
            val noteType = backStackEntry.arguments?.getString("noteType") ?: "STANDARD"
            val rawFolderId = backStackEntry.arguments?.getString("folderId") ?: ""
            val folderId = try { java.net.URLDecoder.decode(rawFolderId, "UTF-8") } catch (_: Exception) { rawFolderId }
            NoteEditorScreen(
                noteId = noteId,
                initialNoteType = noteType,
                initialFolderId = folderId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNote = { targetId, targetType ->
                    if (targetType == NoteType.CANVAS.name) {
                        navController.navigate("canvas?noteId=$targetId")
                    } else {
                        navController.navigate("note_editor/$targetId?type=$targetType")
                    }
                },
                onOpenPdfAttachment = { attachment ->
                    val file = viewModel.attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
                    navController.navigate(
                        Screen.PdfReader.createRoute(
                            attachmentId = attachment.id,
                            filePath = file.absolutePath,
                            fileName = attachment.fileName
                        )
                    )
                },
                onOpenPdfAtPage = { attachment, pageNumber ->
                    val file = viewModel.attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
                    navController.navigate(
                        Screen.PdfReader.createRoute(
                            attachmentId = attachment.id,
                            filePath = file.absolutePath,
                            fileName = attachment.fileName,
                            initialPage = pageNumber
                        )
                    )
                }
            )
        }

        composable(
            route = "canvas?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    defaultValue = "0"
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: "0"
            CanvasScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StickyBoard.route) {
            StickyBoardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Vault.route) {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Archive.route) {
            ArchiveScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecycleBin.route) {
            RecycleBinScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Utilities.route) {
            BackupSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                initialTab = 0,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToProductivity = { tab ->
                    navController.navigate(Screen.Productivity.createRoute(tab))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tasks.route) {
            CalendarScreen(
                viewModel = viewModel,
                initialTab = 1,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToProductivity = { tab ->
                    navController.navigate(Screen.Productivity.createRoute(tab))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Productivity.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
            ProductivityScreen(
                viewModel = viewModel,
                initialTab = initialTab,
                isRootTab = true,
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PinnedNotes.route) {
            PinnedNotesScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId, type ->
                    navController.navigate("note_editor/$noteId?type=$type")
                },
                onNavigateToCanvas = { noteId ->
                    navController.navigate("canvas?noteId=$noteId")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
