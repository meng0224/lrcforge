package com.example.lrcforge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceDirectoryPathHelperTest {

    @Test
    fun extractParentDocumentIdKeepsStorageRootForNestedPrimaryPath() {
        val parent = SourceDirectoryPathHelper.extractParentDocumentId("primary:Music/Lyrics/song.srt")

        assertEquals("primary:Music/Lyrics", parent)
    }

    @Test
    fun extractParentDocumentIdReturnsRootForPrimaryRootFile() {
        val parent = SourceDirectoryPathHelper.extractParentDocumentId("primary:song.srt")

        assertEquals("primary:", parent)
    }

    @Test
    fun extractParentDocumentIdReturnsNullForFlatDocumentId() {
        val parent = SourceDirectoryPathHelper.extractParentDocumentId("song.srt")

        assertNull(parent)
    }

    @Test
    fun extractSourceDirectoryLabelUsesLastPathSegment() {
        val label = SourceDirectoryPathHelper.extractSourceDirectoryLabel("primary:Music/Lyrics")

        assertEquals("Lyrics", label)
    }

    @Test
    fun describeRelativeFilePathIncludesNestedDirectory() {
        val path = SourceDirectoryPathHelper.describeRelativeFilePath("song.lrc", "album/disc1")

        assertEquals("album/disc1/song.lrc", path)
    }
}
