package com.example.lrcforge.util

import android.net.Uri
import com.example.lrcforge.model.FileStatus
import com.example.lrcforge.model.SubtitleFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSelectionPolicyTest {

    @Test
    fun sourceDirectoryModeAppendsNewUris() {
        val existing = listOf(file("content://docs/a/song1.srt", "song1.srt"))
        val incoming = listOf(file("content://docs/b/song2.srt", "song2.srt"))

        val result = FileSelectionPolicy.mergeSelections(existing, incoming, appendToExisting = true)

        assertEquals(2, result.files.size)
        assertEquals(1, result.addedCount)
        assertEquals(0, result.skippedDuplicateCount)
        assertEquals(Uri.parse("content://docs/a/song1.srt"), result.files[0].uri)
        assertEquals(Uri.parse("content://docs/b/song2.srt"), result.files[1].uri)
    }

    @Test
    fun sourceDirectoryModeSkipsDuplicateUriAndKeepsExistingEntry() {
        val existing = listOf(
            file(
                uri = "content://docs/a/song1.srt",
                fileName = "song1.srt",
                status = FileStatus.ERROR,
                errorMessage = "轉換失敗"
            )
        )
        val incoming = listOf(file("content://docs/a/song1.srt", "song1.srt"))

        val result = FileSelectionPolicy.mergeSelections(existing, incoming, appendToExisting = true)

        assertEquals(1, result.files.size)
        assertEquals(0, result.addedCount)
        assertEquals(1, result.skippedDuplicateCount)
        assertSame(existing[0], result.files[0])
        assertEquals(FileStatus.ERROR, result.files[0].status)
    }

    @Test
    fun nonSourceDirectoryModeReplacesExistingList() {
        val existing = listOf(file("content://docs/a/song1.srt", "song1.srt"))
        val incoming = listOf(file("content://docs/b/song2.srt", "song2.srt"))

        val result = FileSelectionPolicy.mergeSelections(existing, incoming, appendToExisting = false)

        assertEquals(1, result.files.size)
        assertEquals(1, result.addedCount)
        assertEquals(0, result.skippedDuplicateCount)
        assertEquals(Uri.parse("content://docs/b/song2.srt"), result.files[0].uri)
    }

    @Test
    fun sourceDirectoryModeKeepsSameFileNameFromDifferentUris() {
        val existing = listOf(file("content://docs/a/shared.srt", "shared.srt"))
        val incoming = listOf(file("content://docs/b/shared.srt", "shared.srt"))

        val result = FileSelectionPolicy.mergeSelections(existing, incoming, appendToExisting = true)

        assertEquals(2, result.files.size)
        assertTrue(result.files.all { it.fileName == "shared.srt" })
        assertEquals(1, result.addedCount)
    }

    @Test
    fun recursiveImportModeCanAppendEvenWhenNotUsingSourceDirectoryOutput() {
        val existing = listOf(file("content://docs/a/song1.srt", "song1.srt"))
        val incoming = listOf(
            file("content://docs/a/song1.srt", "song1.srt"),
            file("content://docs/b/song2.ass", "song2.ass")
        )

        val result = FileSelectionPolicy.mergeSelections(existing, incoming, appendToExisting = true)

        assertEquals(2, result.files.size)
        assertEquals(1, result.addedCount)
        assertEquals(1, result.skippedDuplicateCount)
        assertEquals(Uri.parse("content://docs/b/song2.ass"), result.files[1].uri)
    }

    private fun file(
        uri: String,
        fileName: String,
        status: FileStatus = FileStatus.PENDING,
        errorMessage: String? = null
    ): SubtitleFile {
        return SubtitleFile(
            uri = Uri.parse(uri),
            fileName = fileName,
            fileSize = 128L,
            status = status,
            errorMessage = errorMessage
        )
    }
}
