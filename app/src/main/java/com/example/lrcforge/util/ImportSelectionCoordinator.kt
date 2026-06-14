package com.example.lrcforge.util

import com.example.lrcforge.model.SubtitleFile

object ImportSelectionCoordinator {

    data class ImportResult(
        val files: List<SubtitleFile>,
        val message: String
    )

    fun mergeAndDescribe(
        existingFiles: List<SubtitleFile>,
        importedFiles: List<SubtitleFile>,
        appendToExisting: Boolean,
        skippedInvalidCount: Int,
        isRecursiveImport: Boolean
    ): ImportResult {
        val mergeResult = FileSelectionPolicy.mergeSelections(
            existingFiles = existingFiles,
            newFiles = importedFiles,
            appendToExisting = appendToExisting
        )

        val message = when {
            isRecursiveImport -> buildRecursiveImportMessage(
                addedCount = mergeResult.addedCount,
                skippedDuplicateCount = mergeResult.skippedDuplicateCount,
                skippedInvalidCount = skippedInvalidCount
            )
            appendToExisting -> {
                "已新增 ${mergeResult.addedCount} 個文件，略過 ${mergeResult.skippedDuplicateCount} 個重複文件"
            }
            else -> {
                "已選擇 ${importedFiles.size} 個文件"
            }
        }

        return ImportResult(
            files = mergeResult.files,
            message = message
        )
    }

    internal fun buildRecursiveImportMessage(
        addedCount: Int,
        skippedDuplicateCount: Int,
        skippedInvalidCount: Int
    ): String {
        val skippedSummary = mutableListOf<String>()
        if (skippedDuplicateCount > 0) {
            skippedSummary.add("$skippedDuplicateCount 個重複文件")
        }
        if (skippedInvalidCount > 0) {
            skippedSummary.add("$skippedInvalidCount 個無效文件")
        }

        if (addedCount == 0) {
            return if (skippedSummary.isEmpty()) {
                "所選資料夾中沒有可新增的字幕文件"
            } else {
                "未新增任何文件，略過 ${skippedSummary.joinToString("、")}"
            }
        }

        return if (skippedSummary.isEmpty()) {
            "已新增 $addedCount 個文件"
        } else {
            "已新增 $addedCount 個文件，略過 ${skippedSummary.joinToString("、")}"
        }
    }
}
