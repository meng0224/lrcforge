package com.example.lrcforge.converter

import android.content.ContextWrapper
import com.example.lrcforge.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleConverterTest {

    private val converter = SubtitleConverter(ContextWrapper(null), AppSettings())

    @Test
    fun assDialogueWithCommaKeepsFullText() {
        val content = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,Hello, world
        """.trimIndent()

        assertEquals("[00:01.23]Hello, world", converter.convertAssToLrc(content))
    }

    @Test
    fun assDialogueTextStillCleansStyleMarkup() {
        val content = """
            [Events]
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,{\\i1}Hello{\\i0}\\Nworld
        """.trimIndent()

        assertEquals("[00:01.23]Hello world", converter.convertAssToLrc(content))
    }

    @Test
    fun srtContentConvertsToLrc() {
        val content = """
            1
            00:00:01,500 --> 00:00:03,000
            Hello SRT
        """.trimIndent()

        assertEquals("[00:01.50]Hello SRT", converter.convertSrtToLrc(content))
    }

    @Test
    fun vttContentMergesMultipleLines() {
        val content = """
            WEBVTT

            00:00:01.000 --> 00:00:02.500
            Hello
            VTT
        """.trimIndent()

        assertEquals("[00:01.00]Hello VTT", converter.convertVttToLrc(content))
    }

    @Test
    fun smiContentUsesSyncStartAsTimestamp() {
        val content = """
            <SYNC Start=12345>
            lyric line
            <SYNC Start=15000>
        """.trimIndent()

        assertEquals("[00:12.34]lyric line", converter.convertSmiToLrc(content))
    }

    @Test
    fun subContentConvertsFrameStyleEntry() {
        val content = "{12345}{12350}subtitle line"

        assertEquals("[00:12.34]subtitle line", converter.convertSubToLrc(content))
    }

    @Test
    fun unsupportedExtensionReturnsNull() {
        assertNull(converter.convertContentToLrc("hello", "note.txt"))
    }

    @Test
    fun emptySupportedContentReturnsEmptyString() {
        assertEquals("", converter.convertContentToLrc("", "empty.srt"))
    }
}
