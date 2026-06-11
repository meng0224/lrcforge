package com.example.lrcforge.util

import android.net.Uri
import androidx.documentfile.provider.FakeDocumentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageHelperTest {

    @Test
    fun existingDocumentIsReusedWithoutCreatingReplacement() {
        val dir = FakeDocumentFile("dir", true)
        val existing = FakeDocumentFile("song.lrc", false, length = 16L)
        dir.children.add(existing)

        val resolved = StorageHelper.getOrCreateOutputDocument(dir, "song.lrc", "application/octet-stream")

        assertSame(existing, resolved)
        assertEquals(0, dir.createFileCalls)
        assertEquals(0, existing.deleteCalls)
    }

    @Test
    fun missingDocumentIsCreated() {
        val dir = FakeDocumentFile("dir", true)

        val created = StorageHelper.getOrCreateOutputDocument(dir, "song.lrc", "application/octet-stream")

        assertTrue(created != null)
        assertEquals(1, dir.createFileCalls)
        assertEquals("song.lrc", created?.name)
    }

    @Test
    fun resolveTargetDirectoryReusesExistingNestedDirectories() {
        val root = FakeDocumentFile("root", true)
        val season = FakeDocumentFile("season", true)
        val episode = FakeDocumentFile("episode", true)
        root.children.add(season)
        season.children.add(episode)

        val resolved = StorageHelper.resolveTargetDirectory(root, "season/episode")

        assertSame(episode, resolved)
        assertEquals(0, root.createDirectoryCalls)
        assertEquals(0, season.createDirectoryCalls)
    }

    @Test
    fun resolveTargetDirectoryCreatesMissingNestedDirectories() {
        val root = FakeDocumentFile("root", true)

        val resolved = StorageHelper.resolveTargetDirectory(root, "season/episode")

        assertTrue(resolved != null)
        assertEquals(1, root.createDirectoryCalls)
        assertEquals(1, root.children[0].createDirectoryCalls)
        assertEquals("season", root.children[0].name)
        assertEquals("episode", resolved?.name)
    }

    @Test
    fun verifySavedDocumentRejectsMissingFile() {
        val file = FakeDocumentFile("song.lrc", false, exists = false, length = 16L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertFalse(result)
    }

    @Test
    fun verifySavedDocumentRejectsZeroLengthFile() {
        val file = FakeDocumentFile("song.lrc", false, length = 0L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertFalse(result)
    }

    @Test
    fun verifySavedDocumentAcceptsExpectedBytes() {
        val file = FakeDocumentFile("song.lrc", false, length = 32L)

        val result = StorageHelper.verifySavedDocument(file, 16L)

        assertTrue(result)
    }

    @Test
    fun verifySavedFileNameRejectsProviderAppendedTxt() {
        val result = StorageHelper.verifySavedFileName("song.lrc", "song.lrc.txt")

        assertFalse(result)
    }

    @Test
    fun verifySavedFileNameAcceptsExpectedName() {
        val result = StorageHelper.verifySavedFileName("song.lrc", "song.lrc")

        assertTrue(result)
    }

    @Test
    fun countSuccessfulResultsOnlyCountsNonNullEntries() {
        val savedCount = StorageHelper.countSuccessfulResults(listOf("a.lrc", null, "b.lrc", null))

        assertEquals(2, savedCount)
    }

    @Test
    fun countSuccessfulOutputResultsOnlyCountsVerifiedTargets() {
        val successTarget = StorageHelper.OutputTarget(
            directoryUri = Uri.parse("content://tree/one"),
            fileName = "a.lrc",
            content = "[00:00.00]A",
            fileIndex = 0,
            relativeDirectoryPath = "album/disc1"
        )
        val failedTarget = StorageHelper.OutputTarget(
            directoryUri = Uri.parse("content://tree/two"),
            fileName = "b.lrc",
            content = "[00:00.00]B",
            fileIndex = 1
        )

        val count = StorageHelper.countSuccessfulOutputResults(
            listOf(
                StorageHelper.OutputResult(successTarget, Uri.parse("content://tree/one/a"), "a.lrc", 16L),
                StorageHelper.OutputResult(failedTarget, null, "b.lrc.txt", 0L)
            )
        )

        assertEquals(1, count)
    }

}
