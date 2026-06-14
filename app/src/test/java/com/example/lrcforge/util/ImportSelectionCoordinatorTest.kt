package com.example.lrcforge.util

import android.net.Uri
import com.example.lrcforge.model.SubtitleFile
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportSelectionCoordinatorTest {

    @Test
    fun nonAppendImportReportsSelectedFileCount() {
        val result = ImportSelectionCoordinator.mergeAndDescribe(
            existingFiles = listOf(file("content://old/song.srt")),
            importedFiles = listOf(file("content://new/a.srt"), file("content://new/b.srt")),
            appendToExisting = false,
            skippedInvalidCount = 0,
            isRecursiveImport = false
        )

        assertEquals(2, result.files.size)
        assertEquals("已選擇 2 個文件", result.message)
    }

    @Test
    fun appendImportReportsAddedAndDuplicateCounts() {
        val existing = listOf(file("content://docs/a.srt"))
        val result = ImportSelectionCoordinator.mergeAndDescribe(
            existingFiles = existing,
            importedFiles = listOf(file("content://docs/a.srt"), file("content://docs/b.srt")),
            appendToExisting = true,
            skippedInvalidCount = 0,
            isRecursiveImport = false
        )

        assertEquals(2, result.files.size)
        assertEquals("已新增 1 個文件，略過 1 個重複文件", result.message)
    }

    @Test
    fun recursiveImportReportsDuplicateAndInvalidSkippedFiles() {
        val existing = listOf(file("content://docs/a.srt"))
        val result = ImportSelectionCoordinator.mergeAndDescribe(
            existingFiles = existing,
            importedFiles = listOf(file("content://docs/a.srt"), file("content://docs/b.srt")),
            appendToExisting = true,
            skippedInvalidCount = 3,
            isRecursiveImport = true
        )

        assertEquals(2, result.files.size)
        assertEquals("已新增 1 個文件，略過 1 個重複文件、3 個無效文件", result.message)
    }

    @Test
    fun recursiveImportWithNoNewFilesReportsEmptyFolder() {
        val result = ImportSelectionCoordinator.mergeAndDescribe(
            existingFiles = emptyList(),
            importedFiles = emptyList(),
            appendToExisting = true,
            skippedInvalidCount = 0,
            isRecursiveImport = true
        )

        assertEquals("所選資料夾中沒有可新增的字幕文件", result.message)
    }

    private fun file(uri: String): SubtitleFile {
        return SubtitleFile(
            uri = Uri.parse(uri),
            fileName = uri.substringAfterLast('/'),
            fileSize = 128L
        )
    }
}
