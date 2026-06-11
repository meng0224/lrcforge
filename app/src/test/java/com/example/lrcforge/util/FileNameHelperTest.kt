package com.example.lrcforge.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameHelperTest {

    @Test
    fun disabledSmartNamingOnlyReplacesExtension() {
        assertEquals("music.mp3.lrc", FileNameHelper.smartNaming("music.mp3.ass", false))
    }

    @Test
    fun smartNamingRemovesEmbeddedMediaExtension() {
        assertEquals("music.lrc", FileNameHelper.smartNaming("music.mp3.ass", true))
    }

    @Test
    fun smartNamingKeepsRegularCompoundNames() {
        assertEquals("artist.live.lrc", FileNameHelper.smartNaming("artist.live.srt", true))
    }
}
