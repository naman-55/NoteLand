package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalDocumentHandler(
    uris: List<Uri>,
    action: String?,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onNavigateToMedia: (String) -> Unit,
    onNavigateToPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    val folders by viewModel.folders.collectAsState(initial = emptyList())

    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var saveToFavorites by remember { mutableStateOf(false) }

    if (uris.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isImporting) onDismiss() },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (uris.size > 1) "Import ${uris.size} Files" else "Open External File",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (isImporting) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Importing files...")
                }
            } else {
                Text(
                    text = "Choose how you want to handle these external files inside NoteLand.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Folder Selection
                if (folders.isNotEmpty()) {
                    Text("Destination Folder", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        item {
                            FolderSelectionRow(
                                name = "Workspace Root",
                                isSelected = selectedFolderId == null,
                                onClick = { selectedFolderId = null }
                            )
                        }
                        items(folders) { folder ->
                            FolderSelectionRow(
                                name = folder.name,
                                isSelected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = saveToFavorites,
                        onCheckedChange = { saveToFavorites = it }
                    )
                    Text("Add to Favorites")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uris.size == 1) {
                        OutlinedButton(
                            onClick = {
                                isImporting = true
                                scope.launch {
                                    handleSingleUri(
                                        context, viewModel, uris.first(),
                                        selectedFolderId, saveToFavorites,
                                        onNavigateToMedia, onNavigateToPdf,
                                        onComplete = { onDismiss() },
                                        openTemporarily = true
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Temporarily")
                        }
                    }

                    Button(
                        onClick = {
                                isImporting = true
                            scope.launch {
                                var successes = 0
                                for (uri in uris) {
                                    val name = getFileName(context, uri) ?: "Imported File"
                                    val mime = context.contentResolver.getType(uri)
                                    val isPdf = name.endsWith(".pdf", ignoreCase = true) || mime == "application/pdf"
                                    
                                    val job = kotlinx.coroutines.CompletableDeferred<Unit>()
                                    viewModel.importMediaFile(
                                        uri = uri,
                                        fileName = name,
                                        isPdf = isPdf,
                                        folderId = selectedFolderId,
                                        onSaved = { att ->
                                            if (saveToFavorites) {
                                                viewModel.toggleAttachmentFavorite(att)
                                            }
                                            successes++
                                            job.complete(Unit)
                                        },
                                        onError = {
                                            job.complete(Unit)
                                        }
                                    )
                                    job.await()
                                }
                                Toast.makeText(context, "Saved $successes file(s) to NoteLand", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save to NoteLand")
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSelectionRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private suspend fun handleSingleUri(
    context: Context,
    viewModel: NoteViewModel,
    uri: Uri,
    folderId: String?,
    saveToFavorites: Boolean,
    onNavigateToMedia: (String) -> Unit,
    onNavigateToPdf: (String) -> Unit,
    onComplete: () -> Unit,
    openTemporarily: Boolean = false
) {
    val name = getFileName(context, uri) ?: "External File"
    val mimeType = context.contentResolver.getType(uri)
    val isPdf = name.endsWith(".pdf", ignoreCase = true) || mimeType == "application/pdf"

    if (openTemporarily) {
        // Copy to cache and open
        try {
            val cacheFile = java.io.File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (isPdf) {
                // Navigate to PDF reader with file path
                val encodedPath = java.net.URLEncoder.encode(cacheFile.absolutePath, "UTF-8")
                val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                onNavigateToPdf("temp_ext?filePath=$encodedPath&fileName=$encodedName")
            } else if (mimeType?.startsWith("image/") == true) {
                val encodedPath = java.net.URLEncoder.encode(cacheFile.absolutePath, "UTF-8")
                val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                onNavigateToMedia("temp_ext?filePath=$encodedPath&fileName=$encodedName")
            } else if (mimeType?.startsWith("text/") == true || name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv")) {
                // E.g. Text file temporarily
                val content = cacheFile.readText()
                val noteId = java.util.UUID.randomUUID().toString()
                viewModel.saveNote(
                    com.example.data.model.NoteEntity(
                        id = noteId,
                        title = name.substringBeforeLast("."),
                        content = content,
                        folderId = null,
                        isFavorite = false,
                        createdAtUtc = System.currentTimeMillis(),
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
                Toast.makeText(context, "Opened as temporary Note", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "NoteLand cannot view this format. Please 'Save to NoteLand'.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not read temporary file", Toast.LENGTH_SHORT).show()
        }
        onComplete()
        return
    }

    // Check if it's text/markdown
    if (mimeType?.startsWith("text/") == true || name.endsWith(".txt") || name.endsWith(".md")) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val noteId = java.util.UUID.randomUUID().toString()
            viewModel.saveNote(
                com.example.data.model.NoteEntity(
                    id = noteId,
                    title = name.substringBeforeLast("."),
                    content = content,
                    folderId = folderId,
                    isFavorite = saveToFavorites,
                    createdAtUtc = System.currentTimeMillis(),
                    updatedAtUtc = System.currentTimeMillis()
                )
            )
            Toast.makeText(context, "Text document imported as Note", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not read text file", Toast.LENGTH_SHORT).show()
        }
        onComplete()
        return
    }

    viewModel.importMediaFile(
        uri = uri,
        fileName = name,
        isPdf = isPdf,
        folderId = folderId,
        onSaved = { att ->
            if (saveToFavorites) {
                viewModel.toggleAttachmentFavorite(att)
            }
            if (isPdf) {
                onNavigateToPdf(att.id)
            } else {
                onNavigateToMedia(att.id)
            }
            onComplete()
        },
        onError = {
            Toast.makeText(context, "Failed to open file", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    )
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.let { java.io.File(it).name }
    }
    return result
}
