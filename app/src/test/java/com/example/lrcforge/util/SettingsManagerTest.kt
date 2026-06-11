package com.example.lrcforge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsManagerTest {

    @Test
    fun fromStoredValuesLoadsOutputToSourceDirectory() {
        val settings = SettingsManager.fromStoredValues(
            outputDirUri = null,
            outputToSourceDirectory = true,
            recursiveImportEnabled = false
        )

        assertTrue(settings.outputToSourceDirectory)
        assertNull(settings.outputDirUri)
        assertFalse(settings.recursiveImportEnabled)
        assertTrue(settings.smartNaming)
        assertTrue(settings.timePrecision)
    }

    @Test
    fun fromStoredValuesKeepsCustomOutputDirectoryWhenSourceModeDisabled() {
        val settings = SettingsManager.fromStoredValues(
            outputDirUri = "content://tree/downloads",
            outputToSourceDirectory = false,
            recursiveImportEnabled = false
        )

        assertEquals("content://tree/downloads", settings.outputDirUri)
        assertFalse(settings.outputToSourceDirectory)
    }

    @Test
    fun fromStoredValuesLoadsRecursiveImportFlag() {
        val settings = SettingsManager.fromStoredValues(
            outputDirUri = null,
            outputToSourceDirectory = false,
            recursiveImportEnabled = true
        )

        assertTrue(settings.recursiveImportEnabled)
        assertFalse(settings.outputToSourceDirectory)
    }

    @Test
    fun sourceDirectoryPreferenceKeyIsStableAndEncoded() {
        val key = SettingsManager.sourceDirectoryPreferenceKey("com.android.externalstorage.documents|primary:Music/Lyrics")

        assertEquals(
            "source_directory_uri_com.android.externalstorage.documents%7Cprimary%3AMusic%2FLyrics",
            key
        )
    }

    @Test
    fun importRootDirectoryPreferenceKeyIsStableAndEncoded() {
        val key = SettingsManager.importRootDirectoryPreferenceKey("com.android.externalstorage.documents|primary:Anime/Subtitles")

        assertEquals(
            "import_root_directory_uri_com.android.externalstorage.documents%7Cprimary%3AAnime%2FSubtitles",
            key
        )
    }
}
