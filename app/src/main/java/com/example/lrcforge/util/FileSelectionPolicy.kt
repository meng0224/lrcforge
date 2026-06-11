package com.example.lrcforge.util

import com.example.lrcforge.model.SubtitleFile

object FileSelectionPolicy {

    data class MergeResult(
        val files: List<SubtitleFile>,
        val addedCount: Int,
        val skippedDuplicateCount: Int
    )

    fun mergeSelections(
        existingFiles: List<SubtitleFile>,
        newFiles: List<SubtitleFile>,
        appendToExisting: Boolean
    ): MergeResult {
        if (!appendToExisting) {
            return MergeResult(
                files = newFiles,
                addedCount = newFiles.size,
                skippedDuplicateCount = 0
            )
        }

        val mergedFiles = existingFiles.toMutableList()
        val existingUris = existingFiles.map { it.uri }.toHashSet()
        var addedCount = 0
        var skippedDuplicateCount = 0

        newFiles.forEach { file ->
            if (existingUris.add(file.uri)) {
                mergedFiles.add(file)
                addedCount++
            } else {
                skippedDuplicateCount++
            }
        }

        return MergeResult(
            files = mergedFiles,
            addedCount = addedCount,
            skippedDuplicateCount = skippedDuplicateCount
        )
    }
}
