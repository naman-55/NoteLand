package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.springPressScale
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DarkSettingsAccent
import com.example.ui.theme.LightSettingsAccent
import com.example.ui.theme.NoteBackgroundDark
import com.example.ui.theme.NoteOutlineDark
import com.example.ui.theme.NoteSurfaceDark
import com.example.ui.theme.NoteSurfaceVariantDark
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isDark = isAppInDarkTheme
    val sectionTint = if (isDark) DarkSettingsAccent else LightSettingsAccent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(start = 6.dp, bottom = 2.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sectionTint,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.85.sp,
                fontSize = 11.sp
            ),
            color = sectionTint.copy(alpha = 0.92f)
        )
    }
}

@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isAppInDarkTheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = editorialCardContainerColor(isDark)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xFF15865D).copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                } else Modifier
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 16.dp),
        thickness = 0.65.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
    )
}

@Composable
fun SettingsProfileHeroCard(
    userName: String,
    userEmail: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val settingsAccent = if (isDark) DarkSettingsAccent else LightSettingsAccent
    val cardGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(NoteSurfaceVariantDark, NoteSurfaceDark)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF0FBF8), Color(0xFFFFFFFF))
        )
    }

    val heroInteraction = remember { MutableInteractionSource() }
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = settingsAccent.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                } else Modifier
            )
            .springPressScale(heroInteraction, pressedScale = 0.985f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = heroInteraction,
                indication = LocalIndication.current,
                onClick = onEditClick
            )
            .testTag("settings_profile_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                val hasName = userName.isNotBlank()
                val hasEmail = userEmail.isNotBlank()
                val avatarContent = if (isDark) Color(0xFF05241F) else Color.White
                Surface(
                    shape = CircleShape,
                    color = settingsAccent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hasName) {
                            Text(
                                text = userName.trim().take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = avatarContent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = avatarContent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasName) userName else "Your Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (hasName) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            hasEmail -> userEmail
                            hasName -> "Tap to add your email"
                            else -> "Tap to add your name & email"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Surface(
                    shape = CircleShape,
                    color = settingsAccent.copy(alpha = 0.14f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = settingsAccent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectorRow(
    selectedThemeKey: String,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    val isDark = isAppInDarkTheme
    val resolvedMode = AppThemeMode.fromKey(selectedThemeKey)
    val accent = if (isDark) DarkSettingsAccent else LightSettingsAccent
    val options = listOf(
        Triple(AppThemeMode.LIGHT, Color(0xFFFFFFFF), Color(0xFFCBD5E1)),
        Triple(AppThemeMode.DARK, NoteBackgroundDark, DarkSettingsAccent),
        Triple(AppThemeMode.IVORY, Color(0xFFF7F2E7), Color(0xFFCBBFA8))
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) NoteSurfaceVariantDark else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            options.forEach { (mode, swatchColor, borderColor) ->
                val isSelected = resolvedMode == mode
                val chipInteraction = remember { MutableInteractionSource() }
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isDark) Color(0xFF173630) else MaterialTheme.colorScheme.surface
                    } else Color.Transparent,
                    animationSpec = tween(200),
                    label = "themeChipBg_${mode.key}"
                )
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = chipBg,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    border = if (isSelected) {
                        BorderStroke(1.2.dp, accent.copy(alpha = 0.88f))
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .springPressScale(chipInteraction, pressedScale = 0.95f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(
                            interactionSource = chipInteraction,
                            indication = LocalIndication.current
                        ) { onSelectTheme(mode) }
                        .testTag("theme_chip_${mode.key}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(1.dp, borderColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsInlineSegmentedRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    options: List<SegmentedPillOption>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            SettingsRowIcon(icon = icon, iconColor = iconColor, iconBg = iconBg)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val isDark = isAppInDarkTheme
        val settingsAccent = if (isDark) DarkSettingsAccent else LightSettingsAccent
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) NoteSurfaceVariantDark else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.0.dp, editorialCardBorderColor(isDark))
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    val segInteraction = remember { MutableInteractionSource() }
                    val segBg by animateColorAsState(
                        targetValue = if (option.selected) {
                            if (isDark) Color(0xFF173630) else settingsAccent.copy(alpha = 0.14f)
                        } else Color.Transparent,
                        animationSpec = tween(200),
                        label = "segBg_${option.testTag}"
                    )
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = segBg,
                        border = if (option.selected) {
                            BorderStroke(1.dp, settingsAccent.copy(alpha = 0.85f))
                        } else null,
                        modifier = Modifier
                            .widthIn(min = 44.dp)
                            .heightIn(min = 44.dp)
                            .springPressScale(segInteraction, pressedScale = 0.94f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable(
                                interactionSource = segInteraction,
                                indication = LocalIndication.current,
                                onClick = option.onClick
                            )
                            .testTag(option.testTag)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (option.selected) {
                                    settingsAccent
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SegmentedPillOption(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    trailingBadge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .springPressScale(rowInteraction, pressedScale = 0.985f)
            .clickable(
                interactionSource = rowInteraction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            SettingsRowIcon(icon = icon, iconColor = iconColor, iconBg = iconBg)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            SettingsRowIcon(icon = icon, iconColor = iconColor, iconBg = iconBg)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val isDark = isAppInDarkTheme
        val switchAccent = if (isDark) DarkSettingsAccent else LightSettingsAccent
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = switchAccent,
                checkedBorderColor = switchAccent,
                uncheckedTrackColor = if (isDark) NoteSurfaceVariantDark else MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = if (isDark) NoteOutlineDark else MaterialTheme.colorScheme.outline
            ),
            modifier = if (switchTestTag.isNotBlank()) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
fun SettingsRowIcon(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = iconBg,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
    }
}
