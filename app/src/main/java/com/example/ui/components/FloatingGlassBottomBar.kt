package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavItem
import com.example.ui.navigation.Screen
import com.example.ui.theme.DarkFoldersAccent
import com.example.ui.theme.DarkHomeAccent
import com.example.ui.theme.DarkProductivityAccent
import com.example.ui.theme.DarkSettingsAccent
import com.example.ui.theme.DarkStudioAccent
import com.example.ui.theme.EditorialDarkGlassBarBottom
import com.example.ui.theme.EditorialDarkGlassBarTop
import com.example.ui.theme.NoteOutlineDark
import com.example.ui.theme.NotelandPurple
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun FloatingGlassBottomBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val isIvory = MaterialTheme.colorScheme.background == com.example.ui.theme.NoteBackgroundIvory

    val glassFillBrush = when {
        isDark -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF161B22).copy(alpha = 0.88f),
                    Color(0xFF0D1117).copy(alpha = 0.94f)
                )
            )
        }
        isIvory -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFDF9F0).copy(alpha = 0.90f),
                    Color(0xFFEFE8D8).copy(alpha = 0.88f)
                )
            )
        }
        else -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.84f),
                    Color(0xFFF1F5F4).copy(alpha = 0.80f)
                )
            )
        }
    }

    val glassRimBrush = when {
        isDark -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    NoteOutlineDark.copy(alpha = 0.70f)
                )
            )
        }
        isIvory -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFCBBFA8).copy(alpha = 0.85f)
                )
            )
        }
        else -> {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFDCE5E0).copy(alpha = 0.85f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            .testTag("bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (isDark) 14.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.28f else 0.10f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.34f else 0.12f)
                )
                .clip(CircleShape)
        ) {
            // Frosted translucent glass backdrop layer (real RenderEffect blur on Android 12+)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Modifier.blur(radius = 18.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle)
                        } else {
                            Modifier
                        }
                    )
                    .background(glassFillBrush)
            )

            // Subtle translucent outer rim + razor-sharp navigation tabs on top
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 1.dp, brush = glassRimBrush, shape = CircleShape)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.screen.route
                    val (tabAccent, tabAccentDeep) = when (item.screen) {
                        Screen.Home -> if (isDark) {
                            DarkHomeAccent to Color(0xFF1F7A42)
                        } else {
                            com.example.ui.theme.LightHomeAccent to com.example.ui.theme.LightHomeAccent
                        }
                        Screen.Folders -> if (isDark) {
                            DarkFoldersAccent to Color(0xFF2563EB)
                        } else {
                            com.example.ui.theme.LightFoldersAccent to com.example.ui.theme.LightFoldersAccent
                        }
                        Screen.Studio -> if (isDark) {
                            DarkStudioAccent to Color(0xFF6D28D9)
                        } else {
                            NotelandPurple to NotelandPurple
                        }
                        Screen.Productivity -> if (isDark) {
                            DarkProductivityAccent to Color(0xFF1E7E40)
                        } else {
                            com.example.ui.theme.LightProductivityAccent to com.example.ui.theme.LightProductivityAccent
                        }
                        Screen.Settings -> if (isDark) {
                            DarkSettingsAccent to Color(0xFF0E8875)
                        } else {
                            com.example.ui.theme.LightSettingsAccent to com.example.ui.theme.LightSettingsAccent
                        }
                        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary
                    }

                    val pillBgColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isDark -> tabAccentDeep.copy(alpha = 0.94f)
                            isSelected && !isDark -> tabAccent.copy(alpha = 0.16f)
                            else -> Color.Transparent
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "pillBg"
                    )
                    val pillTopGlow by animateColorAsState(
                        targetValue = when {
                            isSelected && isDark -> tabAccent.copy(alpha = 0.88f)
                            isSelected && !isDark -> tabAccent.copy(alpha = 0.16f)
                            else -> Color.Transparent
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "pillTopGlow"
                    )

                    val inactiveTint = if (isDark) {
                        Color(0xFFB5C1BA)
                    } else {
                        Color(0xFF65766F)
                    }

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isDark -> Color.White
                            isSelected -> tabAccent
                            else -> inactiveTint
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "contentTint"
                    )

                    val interactionSource = remember { MutableInteractionSource() }
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tabIconScale"
                    )
                    val iconLift by animateDpAsState(
                        targetValue = if (isSelected) (-1).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabIconLift"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .springPressScale(interactionSource, pressedScale = 0.93f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected && isDark) {
                                    Brush.verticalGradient(colors = listOf(pillTopGlow, pillBgColor))
                                } else {
                                    Brush.verticalGradient(colors = listOf(pillBgColor, pillBgColor))
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = if (isDark) 0.9.dp else 0.85.dp,
                                        color = if (isDark) Color.White.copy(alpha = 0.22f) else tabAccent.copy(alpha = 0.28f),
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true),
                                role = Role.Tab,
                                onClick = { onItemSelected(item) }
                            )
                            .testTag("nav_${item.label.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        translationY = iconLift.toPx()
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.sp
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
