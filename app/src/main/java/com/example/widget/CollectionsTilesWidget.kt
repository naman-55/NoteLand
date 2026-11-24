package com.example.widget

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.ui.navigation.Screen

private data class CollectionTileSpec(
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val accentColor: ColorProvider,
    val badgeBgColor: ColorProvider,
    val isHighlighted: Boolean = false,
    val action: Action
)

class CollectionsTilesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.loadCollectionsSnapshot(context)
        provideContent {
            GlanceTheme {
                CollectionsTilesContent(snapshot)
            }
        }
    }

    @Composable
    private fun CollectionsTilesContent(snapshot: CollectionsWidgetSnapshot) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isThreeCols = size.width >= 240.dp
        val showThirdRow = size.height >= 200.dp
        val showFolderBar = size.height >= 260.dp && snapshot.topFolders.isNotEmpty()

        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetThemeTokens.WidgetSurface)
            .let { mod ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mod.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                } else {
                    mod.cornerRadius(22.dp)
                }
            }
            .padding(12.dp)

        val tiles = listOf(
            CollectionTileSpec(
                title = "New Note",
                subtitle = "${snapshot.activeNotesCount} notes",
                iconRes = R.drawable.ic_widget_note,
                accentColor = WidgetThemeTokens.OnPrimaryAccent,
                badgeBgColor = WidgetThemeTokens.PrimaryAccent,
                isHighlighted = true,
                action = actionStartActivity(
                    WidgetNavigation.actionIntent(context, WidgetNavigation.ACTION_NEW_NOTE)
                )
            ),
            CollectionTileSpec(
                title = "Folders",
                subtitle = "${snapshot.folderCount} collections",
                iconRes = R.drawable.ic_widget_folder,
                accentColor = WidgetThemeTokens.EmeraldAccent,
                badgeBgColor = WidgetThemeTokens.EmeraldContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.Folders.route)
                )
            ),
            CollectionTileSpec(
                title = "Daily Diary",
                subtitle = "Today's page",
                iconRes = R.drawable.ic_widget_diary,
                accentColor = WidgetThemeTokens.TerracottaAccent,
                badgeBgColor = WidgetThemeTokens.TerracottaContainer,
                action = actionStartActivity(
                    WidgetNavigation.actionIntent(context, WidgetNavigation.ACTION_OPEN_DAILY_DIARY)
                )
            ),
            CollectionTileSpec(
                title = "Tasks",
                subtitle = "${snapshot.pendingTasksCount} pending",
                iconRes = R.drawable.ic_widget_tasks,
                accentColor = WidgetThemeTokens.AmberAccent,
                badgeBgColor = WidgetThemeTokens.AmberContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.Tasks.route)
                )
            ),
            CollectionTileSpec(
                title = "Reminders",
                subtitle = "${snapshot.pendingRemindersCount} active",
                iconRes = R.drawable.ic_widget_alarm,
                accentColor = WidgetThemeTokens.VioletAccent,
                badgeBgColor = WidgetThemeTokens.VioletContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.Reminders.route)
                )
            ),
            CollectionTileSpec(
                title = "Favourites",
                subtitle = "${snapshot.favoritesCount} starred",
                iconRes = R.drawable.ic_widget_favorite,
                accentColor = WidgetThemeTokens.CoralAccent,
                badgeBgColor = WidgetThemeTokens.CoralContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.Favorites.route)
                )
            ),
            CollectionTileSpec(
                title = "Sticky Wall",
                subtitle = "${snapshot.stickyCount} stickies",
                iconRes = R.drawable.ic_widget_sticky,
                accentColor = WidgetThemeTokens.VioletAccent,
                badgeBgColor = WidgetThemeTokens.VioletContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.StickyBoard.route)
                )
            ),
            CollectionTileSpec(
                title = "Board",
                subtitle = "${snapshot.canvasCount} boards",
                iconRes = R.drawable.ic_widget_canvas,
                accentColor = WidgetThemeTokens.SkyAccent,
                badgeBgColor = WidgetThemeTokens.SkyContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.Canvas.route)
                )
            ),
            CollectionTileSpec(
                title = "PDF Reader",
                subtitle = "${snapshot.pdfCount} docs",
                iconRes = R.drawable.ic_widget_pdf,
                accentColor = WidgetThemeTokens.CoralAccent,
                badgeBgColor = WidgetThemeTokens.CoralContainer,
                action = actionStartActivity(
                    WidgetNavigation.routeIntent(context, Screen.PdfTools.route)
                )
            )
        )

        val cols = if (isThreeCols) 3 else 2
        val maxRows = when {
            showThirdRow && !isThreeCols -> 3
            showThirdRow -> 3
            else -> 2
        }
        val visibleTiles = tiles.take(cols * maxRows)
        val tileRows = visibleTiles.chunked(cols)

        Column(modifier = containerModifier) {
            // Top bar: Title + Search + New Folder Collection button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Folders.route))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collections",
                        style = TextStyle(
                            color = WidgetThemeTokens.TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .background(WidgetThemeTokens.CardSurfaceAlt)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${snapshot.activeNotesCount}",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Search button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(14.dp)
                        .background(WidgetThemeTokens.CardSurface)
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Search.route))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_search),
                        contentDescription = "Search notes",
                        colorFilter = ColorFilter.tint(WidgetThemeTokens.TextPrimary),
                        modifier = GlanceModifier.size(16.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Create New Folder / Collection button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(14.dp)
                        .background(WidgetThemeTokens.EmeraldContainer)
                        .clickable(
                            actionStartActivity(
                                WidgetNavigation.actionIntent(context, WidgetNavigation.ACTION_NEW_FOLDER)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_new_folder),
                        contentDescription = "New Folder Collection",
                        colorFilter = ColorFilter.tint(WidgetThemeTokens.EmeraldAccent),
                        modifier = GlanceModifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Multi-tile Grid
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                tileRows.forEachIndexed { rowIndex, rowTiles ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                    ) {
                        rowTiles.forEachIndexed { colIndex, tile ->
                            CollectionTileCard(
                                tile = tile,
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxSize()
                            )
                            if (colIndex < rowTiles.lastIndex) {
                                Spacer(modifier = GlanceModifier.width(7.dp))
                            }
                        }
                    }
                    if (rowIndex < tileRows.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(7.dp))
                    }
                }
            }

            // Optional Quick Folder Chips Row on Tall Widget Sizes
            if (showFolderBar) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    snapshot.topFolders.take(3).forEachIndexed { index, folder ->
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .cornerRadius(10.dp)
                                .background(WidgetThemeTokens.CardSurface)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(
                                    actionStartActivity(
                                        WidgetNavigation.actionIntent(
                                            context = context,
                                            widgetAction = WidgetNavigation.ACTION_OPEN_FOLDER,
                                            folderId = folder.id
                                        )
                                    )
                                )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_folder),
                                    contentDescription = folder.name,
                                    colorFilter = ColorFilter.tint(WidgetThemeTokens.PrimaryAccent),
                                    modifier = GlanceModifier.size(13.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(5.dp))
                                Text(
                                    text = "${folder.name} (${folder.noteCount})",
                                    style = TextStyle(
                                        color = WidgetThemeTokens.TextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                        if (index < snapshot.topFolders.take(3).lastIndex) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectionTileCard(
        tile: CollectionTileSpec,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val cardBg = if (tile.isHighlighted) WidgetThemeTokens.PrimaryAccent else WidgetThemeTokens.CardSurface
        val titleColor = if (tile.isHighlighted) WidgetThemeTokens.OnPrimaryAccent else WidgetThemeTokens.TextPrimary
        val subtitleColor = if (tile.isHighlighted) WidgetThemeTokens.OnPrimaryAccent else WidgetThemeTokens.TextSecondary

        Column(
            modifier = modifier
                .cornerRadius(14.dp)
                .background(cardBg)
                .padding(horizontal = 9.dp, vertical = 8.dp)
                .clickable(tile.action),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(26.dp)
                    .cornerRadius(8.dp)
                    .background(
                        if (tile.isHighlighted) WidgetThemeTokens.OnPrimaryAccent else tile.badgeBgColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(tile.iconRes),
                    contentDescription = tile.title,
                    colorFilter = ColorFilter.tint(
                        if (tile.isHighlighted) WidgetThemeTokens.PrimaryAccent else tile.accentColor
                    ),
                    modifier = GlanceModifier.size(15.dp)
                )
            }
            Spacer(modifier = GlanceModifier.height(5.dp))
            Text(
                text = tile.title,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = tile.subtitle,
                style = TextStyle(
                    color = subtitleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

class CollectionsTilesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CollectionsTilesWidget()
}
