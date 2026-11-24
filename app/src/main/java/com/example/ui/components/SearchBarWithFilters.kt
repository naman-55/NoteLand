package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteType
import com.example.data.model.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    folders: List<FolderEntity>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    tags: List<TagEntity>,
    selectedType: NoteType?,
    onSelectType: (NoteType?) -> Unit,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search notes, tags, checklists...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input")
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onToggleGridView,
                modifier = Modifier.testTag("view_toggle_button")
            ) {
                Icon(
                    if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (isGridView) "Switch to List" else "Switch to Grid",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Horizontal filter chips: Type, Folders & Tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedType == null && selectedFolderId == null && selectedTag == null,
                onClick = {
                    onSelectType(null)
                    onSelectFolder(null)
                    onSelectTag(null)
                },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_all")
            )

            FilterChip(
                selected = selectedType == NoteType.STANDARD,
                onClick = {
                    onSelectType(if (selectedType == NoteType.STANDARD) null else NoteType.STANDARD)
                },
                label = { Text("Notes") }
            )

            FilterChip(
                selected = selectedType == NoteType.STICKY,
                onClick = {
                    onSelectType(if (selectedType == NoteType.STICKY) null else NoteType.STICKY)
                },
                label = { Text("Stickies") }
            )

            FilterChip(
                selected = selectedType == NoteType.CANVAS,
                onClick = {
                    onSelectType(if (selectedType == NoteType.CANVAS) null else NoteType.CANVAS)
                },
                label = { Text("Canvas") }
            )

            folders.forEach { folder ->
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = {
                        onSelectFolder(if (selectedFolderId == folder.id) null else folder.id)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(folder.name) }
                )
            }

            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag.name,
                    onClick = {
                        onSelectTag(if (selectedTag == tag.name) null else tag.name)
                    },
                    label = { Text("#${tag.name}") }
                )
            }
        }
    }
}
