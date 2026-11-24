package com.example.util

import com.example.data.model.AttachmentEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity

data class FolderTreeNode(
    val folder: FolderEntity,
    val depth: Int,
    val fullPath: String,
    val directSubfolderCount: Int,
    val directNoteCount: Int,
    val totalNoteCount: Int,
    val directDocumentCount: Int = 0,
    val totalDocumentCount: Int = 0
)

object FolderHierarchyUtils {

    /**
     * Resolves a folder's effective parentId: returns null if the folder has no parent
     * or if its parentId references a missing folder (orphan protection) or itself.
     */
    fun resolveEffectiveParentId(
        folder: FolderEntity,
        foldersById: Map<String, FolderEntity>
    ): String? {
        val pid = folder.parentId?.takeIf { it.isNotBlank() } ?: return null
        if (pid == folder.id) return null
        return if (foldersById.containsKey(pid)) pid else null
    }

    /**
     * Returns direct child folders under [parentId] (null for root-level folders),
     * sorted alphabetically by name.
     */
    fun getSubfolders(
        folders: List<FolderEntity>,
        parentId: String?
    ): List<FolderEntity> {
        val byId = folders.associateBy { it.id }
        return folders.filter { folder ->
            resolveEffectiveParentId(folder, byId) == parentId
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Returns the top-down ancestor chain ending with [folderId]:
     * e.g., [Root, Parent, Current]. Includes cycle detection.
     */
    fun getBreadcrumbTrail(
        folders: List<FolderEntity>,
        folderId: String?
    ): List<FolderEntity> {
        if (folderId.isNullOrBlank()) return emptyList()
        val byId = folders.associateBy { it.id }
        val trail = mutableListOf<FolderEntity>()
        val visited = mutableSetOf<String>()

        var cursor: FolderEntity? = byId[folderId]
        while (cursor != null && visited.add(cursor.id)) {
            trail.add(cursor)
            val parentId = resolveEffectiveParentId(cursor, byId)
            cursor = if (parentId != null) byId[parentId] else null
        }
        return trail.asReversed()
    }

    /**
     * Formats the full hierarchical path for [folderId], e.g. "Projects / Android / NoteLand".
     */
    fun getFolderPathString(
        folders: List<FolderEntity>,
        folderId: String?,
        separator: String = " / "
    ): String {
        val trail = getBreadcrumbTrail(folders, folderId)
        return trail.joinToString(separator) { it.name }
    }

    /**
     * Returns the parent path prefix excluding the folder itself, e.g. "Projects / Android"
     * or "" for root-level folders.
     */
    fun getParentPathString(
        folders: List<FolderEntity>,
        folderId: String?,
        separator: String = " / "
    ): String {
        val trail = getBreadcrumbTrail(folders, folderId)
        if (trail.size <= 1) return ""
        return trail.dropLast(1).joinToString(separator) { it.name }
    }

    /**
     * Returns all folder IDs in the subtree rooted at [rootFolderId], including [rootFolderId] itself.
     */
    fun getSubtreeFolderIds(
        folders: List<FolderEntity>,
        rootFolderId: String
    ): Set<String> {
        val byId = folders.associateBy { it.id }
        val childrenMap = folders.groupBy { resolveEffectiveParentId(it, byId) }
        val result = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(rootFolderId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (!result.add(currentId)) continue
            childrenMap[currentId].orEmpty().forEach { child ->
                if (child.id !in result) {
                    queue.add(child.id)
                }
            }
        }
        return result
    }

    /**
     * Returns true if setting [folderId]'s parentId to [candidateParentId] would form a cycle
     * (self-parenting or moving a folder inside one of its own descendants).
     */
    fun wouldCreateCycle(
        folders: List<FolderEntity>,
        folderId: String,
        candidateParentId: String?
    ): Boolean {
        if (candidateParentId.isNullOrBlank()) return false
        if (candidateParentId == folderId) return true
        val subtreeIds = getSubtreeFolderIds(folders, folderId)
        return candidateParentId in subtreeIds
    }

    fun countDirectNotes(
        notes: List<NoteEntity>,
        folderId: String
    ): Int {
        return notes.count { it.folderId == folderId && !it.isLocked && !it.isDeleted }
    }

    fun countSubtreeNotes(
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        folderId: String
    ): Int {
        val subtreeIds = getSubtreeFolderIds(folders, folderId)
        return notes.count { it.folderId != null && it.folderId in subtreeIds && !it.isLocked && !it.isDeleted }
    }

    fun countDirectAttachments(
        attachments: List<AttachmentEntity>,
        folderId: String
    ): Int {
        return attachments.count {
            it.folderId == folderId &&
                !it.id.startsWith("demo-") && it.id != "demo_pdf" &&
                !it.isLocked && !it.isDeleted && !it.isArchived && !it.isHidden
        }
    }

    fun countSubtreeAttachments(
        folders: List<FolderEntity>,
        attachments: List<AttachmentEntity>,
        folderId: String
    ): Int {
        val subtreeIds = getSubtreeFolderIds(folders, folderId)
        return attachments.count {
            it.folderId != null && it.folderId in subtreeIds &&
                !it.id.startsWith("demo-") && it.id != "demo_pdf" &&
                !it.isLocked && !it.isDeleted && !it.isArchived && !it.isHidden
        }
    }

    fun isFolderOrSubtreeActive(
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        folderId: String
    ): Boolean {
        return countSubtreeNotes(folders, notes, folderId) > 0
    }

    fun isFolderOrSubtreeActive(
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        attachments: List<AttachmentEntity>,
        folderId: String
    ): Boolean {
        return countSubtreeNotes(folders, notes, folderId) > 0 ||
            countSubtreeAttachments(folders, attachments, folderId) > 0
    }

    /**
     * Produces a pre-order flattened tree list with indentation depth, path strings, and recursive counts.
     * Passing [excludeSubtreeOfId] hides a folder and all of its descendants (useful when reparenting a folder).
     */
    fun flattenFolderTree(
        folders: List<FolderEntity>,
        notes: List<NoteEntity> = emptyList(),
        excludeSubtreeOfId: String? = null,
        attachments: List<AttachmentEntity> = emptyList()
    ): List<FolderTreeNode> {
        val byId = folders.associateBy { it.id }
        val excludedIds = if (excludeSubtreeOfId != null) {
            getSubtreeFolderIds(folders, excludeSubtreeOfId)
        } else {
            emptySet()
        }

        val childrenMap = folders
            .filter { it.id !in excludedIds }
            .groupBy { resolveEffectiveParentId(it, byId) }
            .mapValues { (_, list) -> list.sortedBy { it.name.lowercase() } }

        val output = mutableListOf<FolderTreeNode>()
        val visited = mutableSetOf<String>()

        fun dfs(folder: FolderEntity, depth: Int, pathSegments: List<String>) {
            if (!visited.add(folder.id)) return
            val currentPath = pathSegments + folder.name
            val directChildren = childrenMap[folder.id].orEmpty()
            val directNotes = countDirectNotes(notes, folder.id)
            val totalNotes = countSubtreeNotes(folders, notes, folder.id)
            val directDocs = countDirectAttachments(attachments, folder.id)
            val totalDocs = countSubtreeAttachments(folders, attachments, folder.id)

            output.add(
                FolderTreeNode(
                    folder = folder,
                    depth = depth,
                    fullPath = currentPath.joinToString(" / "),
                    directSubfolderCount = directChildren.size,
                    directNoteCount = directNotes,
                    totalNoteCount = totalNotes,
                    directDocumentCount = directDocs,
                    totalDocumentCount = totalDocs
                )
            )
            directChildren.forEach { child ->
                dfs(child, depth + 1, currentPath)
            }
        }

        childrenMap[null].orEmpty().forEach { root ->
            dfs(root, 0, emptyList())
        }
        return output
    }
}
