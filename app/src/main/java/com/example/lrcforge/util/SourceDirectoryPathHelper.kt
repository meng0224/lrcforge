package com.example.lrcforge.util

object SourceDirectoryPathHelper {

    data class DirectoryInfo(
        val key: String,
        val label: String
    )

    fun buildDirectoryKey(authority: String, documentId: String): String {
        return "$authority|$documentId"
    }

    fun extractParentDocumentId(documentId: String): String? {
        val colonIndex = documentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = documentId.substring(0, colonIndex)
            val path = documentId.substring(colonIndex + 1)
            val parentPath = path.substringBeforeLast('/', "")
            return "$root:$parentPath"
        }

        return documentId.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }
    }

    fun extractSourceDirectoryLabel(parentDocumentId: String): String {
        val colonIndex = parentDocumentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = parentDocumentId.substring(0, colonIndex)
            val path = parentDocumentId.substring(colonIndex + 1)
            return path.substringAfterLast('/', root).ifEmpty { root }
        }
        return parentDocumentId.substringAfterLast('/').ifEmpty { parentDocumentId }
    }

    fun describeRelativeFilePath(fileName: String, relativeDirectoryPath: String?): String {
        return if (relativeDirectoryPath.isNullOrBlank()) {
            fileName
        } else {
            "$relativeDirectoryPath/$fileName"
        }
    }
}
