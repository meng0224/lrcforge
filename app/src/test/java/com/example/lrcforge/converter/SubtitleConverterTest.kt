package com.example.lrcforge.converter

import android.content.ContextWrapper
import android.net.Uri
import com.example.lrcforge.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.CharacterCodingException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,{\i1}Hello{\i0}\Nworld
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
        val content = "{1}{1}25.0\n{250}{300}subtitle line"

        assertEquals("[00:10.00]subtitle line", converter.convertSubToLrc(content))
    }

    @Test
    fun unsupportedExtensionReturnsNull() {
        assertNull(converter.convertContentToLrc("hello", "note.txt"))
    }

    @Test
    fun emptySupportedContentReturnsEmptyString() {
        assertEquals("", converter.convertContentToLrc("", "empty.srt"))
    }

    @Test
    fun vttAcceptsShortTimestampsAndCueSettings() {
        val content = "WEBVTT\n\ncue-id\n00:01.250 --> 00:02.000 align:start\nHello"
        assertEquals("[00:01.25]Hello", converter.convertVttToLrc(content))
    }

    @Test
    fun vttCrossingHourUsesStartTime() {
        val content = "WEBVTT\n\n59:59.000 --> 01:00:01.000\nHello"
        assertEquals("[59:59.00]Hello", converter.convertVttToLrc(content))
    }

    @Test
    fun vttKeepsHoursOverTwoDigits() {
        assertEquals("[6000:01.00]Hello", converter.convertVttToLrc(
            "WEBVTT\n\n100:00:01.000 --> 100:00:02.000\nHello"
        ))
    }

    @Test
    fun vttSkipsMetadataBlocksAndKeepsCueIdentifiers() {
        val content = "WEBVTT\n\nNOTE comment\nignored\n\nSTYLE\n::cue { color: lime; }\n\n" +
            "id\n00:01.000 --> 00:02.000\n<v Speaker>Hello</v>"
        assertEquals("[00:01.00]Hello", converter.convertVttToLrc(content))
    }

    @Test
    fun timedFormatsRejectBrokenStartInsteadOfUsingEnd() {
        assertParseFailure("SRT", 2) {
            converter.convertSrtToLrc("1\ninvalid --> 00:00:03,000\nHello")
        }
        assertParseFailure("VTT", 3) {
            converter.convertVttToLrc("WEBVTT\n\ninvalid --> 00:00:03.000\nHello")
        }
    }

    @Test
    fun timedFormatsRejectInvalidRangesAndMissingSeparators() {
        listOf("00:60:01,000", "00:00:61500", "00:00:01,00").forEach { start ->
            assertParseFailure("SRT", 2) {
                converter.convertSrtToLrc("1\n$start --> 01:01:02,000\nHello")
            }
        }
        assertParseFailure("VTT", 3) {
            converter.convertVttToLrc("WEBVTT\n\n00:02.000 --> 00:01.000\nHello")
        }
    }

    @Test
    fun malformedLaterCueRejectsWholeResult() {
        assertParseFailure("SRT", 6) {
            converter.convertSrtToLrc(
                "1\n00:00:01,000 --> 00:00:02,000\nGood\n\n2\nbroken\nLost"
            )
        }
    }

    @Test
    fun smiReadsInlineAndParagraphTextWithoutLineDependence() {
        val content = "<SYNC Start=1000><P Class=ENCC>Hello<br>world</P>" +
            "<SYNC Start=2000>\n<P Class=ENCC>Next</P>\n<SYNC Start=3000><P>&nbsp;"
        assertEquals("[00:01.00]Hello world\n[00:02.00]Next", converter.convertSmiToLrc(content))
    }

    @Test
    fun smiAcceptsZeroQuotesAndCaseInsensitiveAttributes() {
        val content = "<sami><head><title>Not a caption</title></head><body>" +
            "<sync class='cue' start = \"0\"><p>Zero</p></sync>" +
            "<SYNC Start='1234'><P>Next</P></SYNC></body></sami>"
        assertEquals("[00:00.00]Zero\n[00:01.23]Next", converter.convertSmiToLrc(content))
    }

    @Test
    fun smiRejectsMalformedSyncWithOriginalLineNumber() {
        assertParseFailure("SMI", 3) {
            converter.convertSmiToLrc("<!-- comment\nmore -->\n<SYNC Start=bad><P>Lost")
        }
    }

    @Test
    fun subRequiresExplicitFrameRate() {
        assertParseFailure("SUB", 1) { converter.convertSubToLrc("{250}{300}Hello") }
    }

    @Test
    fun subSupportsFractionalFrameRateAndLineBreaks() {
        assertEquals("[00:10.01]Hello world", converter.convertSubToLrc(
            "{0}{}23.976\n{240}{300}Hello|world"
        ))
    }

    @Test
    fun subRejectsInvalidOrRepeatedFrameRate() {
        listOf("0", "-25", "NaN", "Infinity").forEach { rate ->
            assertParseFailure("SUB", 1) { converter.convertSubToLrc("{1}{1}$rate\n{25}{50}Hello") }
        }
        assertParseFailure("SUB", 3) {
            converter.convertSubToLrc("{1}{1}25\n{25}{50}Good\n{1}{1}30")
        }
    }

    @Test
    fun subRejectsBrokenLaterEntry() {
        assertParseFailure("SUB", 3) {
            converter.convertSubToLrc("{1}{1}25\n{25}{50}Good\nbroken")
        }
    }

    @Test
    fun assUsesDeclaredStartColumnAndPreservesCommas() {
        val content = "[Events]\n" +
            "Format: Start, End, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n" +
            "Dialogue: 0:00:01.00,0:00:03.00,0,Default,,0,0,0,,Hello, world"
        assertEquals("[00:01.00]Hello, world", converter.convertAssToLrc(content))
    }

    @Test
    fun assMalformedDialogueReportsOriginalLineAndRejectsPartialResult() {
        assertParseFailure("ASS/SSA", 3) {
            converter.convertAssToLrc("[Events]\n" +
                "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Good\n" +
                "Dialogue: 0,broken,0:00:04.00,Default,,0,0,0,,Lost")
        }
    }

    @Test
    fun assRejectsInvalidDeclaredFormatInsteadOfDefaulting() {
        listOf("Layer, End, Text", "Start, Text, End", "Start, Start, End, Text").forEach { columns ->
            assertParseFailure("ASS/SSA", 2) {
                converter.convertAssToLrc("[Events]\nFormat: $columns")
            }
        }
    }

    @Test
    fun assConvertsHardSpacesAndKeepsLiteralAngleBrackets() {
        assertEquals("[00:01.00]Hello world <literal>", converter.convertAssToLrc(
            "[Events]\nDialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello\\hworld <literal>"
        ))
    }

    @Test
    fun textEntitiesDecodeOnceAfterMarkupRemoval() {
        assertEquals("[00:01.00]A & B <tag> &#65; A", converter.convertVttToLrc(
            "WEBVTT\n\n00:00:01.000 --> 00:00:02.000\n" +
                "<b>A &amp; B</b> &lt;tag&gt; &amp;#65; &#x41;"
        ))
    }

    @Test
    fun allTimestampWritersUseAsciiRegardlessOfLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("[00:01.50]Hello", converter.convertSrtToLrc(
                "1\n00:00:01,500 --> 00:00:03,000\nHello"
            ))
            assertEquals("[00:01.50]Hello", converter.convertSmiToLrc("<SYNC Start=1500>\nHello"))
            val secondsOnly = SubtitleConverter(ContextWrapper(null), AppSettings(timePrecision = false))
            assertEquals("[00:01.00]Hello", secondsOnly.convertSrtToLrc(
                "1\n00:00:01,500 --> 00:00:03,000\nHello"
            ))
            assertEquals("[00:01.00]Hello", secondsOnly.convertSubToLrc("{1}{1}25\n{49}{50}Hello"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun fileEntryPointReadsUtf8BomAndWindowsLineEndings() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://conversion/utf8.ass")
        val text = "\uFEFF[Events]\r\nDialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,你好"
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
        assertEquals("[00:01.00]你好", SubtitleConverter(context, AppSettings()).convertToLrc(uri, "utf8.ass"))
    }

    @Test
    fun fileEntryPointPropagatesLineDiagnostics() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://conversion/broken.srt")
        val text = "1\ninvalid --> 00:00:03,000\nHello"
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(text.toByteArray()))
        assertParseFailure("SRT", 2) { SubtitleConverter(context, AppSettings()).convertToLrc(uri, "broken.srt") }
    }

    @Test
    fun fileEntryPointPropagatesReadFailureAndClosesStream() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://conversion/unreadable.srt")
        val cause = IOException("test read failure")
        var closed = false
        shadowOf(context.contentResolver).registerInputStream(uri, object : InputStream() {
            override fun read(): Int = throw cause
            override fun close() { closed = true }
        })
        assertSame(cause, assertThrows(IOException::class.java) {
            SubtitleConverter(context, AppSettings()).convertToLrc(uri, "unreadable.srt")
        })
        assertTrue(closed)
    }

    @Test
    fun vttHeaderWordsUsedAsIdentifiersKeepTheirCues() {
        listOf("WEBVTT", "WEBVTT caption", "STYLE", "REGION").forEach { identifier ->
            val content = "WEBVTT\n\n$identifier\n00:01.000 --> 00:02.000\nFirst\n\n" +
                "00:03.000 --> 00:04.000\nSecond"
            assertEquals("[00:01.00]First\n[00:03.00]Second", converter.convertVttToLrc(content))
        }
    }

    @Test
    fun vttMetadataAfterCueIsRejectedButNotesRemainAllowed() {
        val cue = "WEBVTT\n\n00:01.000 --> 00:02.000\nFirst\n\n"
        assertParseFailure("VTT", 7) { converter.convertVttToLrc(cue + "STYLE\n::cue {color: red}") }
        assertEquals("[00:01.00]First", converter.convertVttToLrc(cue + "NOTE comment\nignored"))
    }

    @Test
    fun vttHeaderWithoutBlankSeparatorIsRejected() {
        assertParseFailure("VTT", 2) {
            converter.convertVttToLrc("WEBVTT\n00:01.000 --> 00:02.000\nLost")
        }
    }

    @Test
    fun timedTextRejectsMissingCueSeparatorsAtOriginalLine() {
        assertParseFailure("VTT", 5) {
            converter.convertVttToLrc("WEBVTT\n\n00:01.000 --> 00:02.000\nFirst\n00:03.000 --> 00:04.000\nSecond")
        }
        assertParseFailure("SRT", 5) {
            converter.convertSrtToLrc("1\n00:00:01,000 --> 00:00:02,000\nFirst\n2\n00:00:03,000 --> 00:00:04,000\nSecond")
        }
    }

    @Test
    fun srtPreservesOrdinaryArrowsInText() {
        assertEquals("[00:01.00]Go --> home", converter.convertSrtToLrc(
            "1\n00:00:01,000 --> 00:00:02,000\nGo --> home"
        ))
    }

    @Test
    fun subEarlyCaptionsWithoutEndAreNotFrameRateHeaders() {
        assertEquals("[00:00.00]Zero\n[00:00.04]One\n[00:00.04]123", converter.convertSubToLrc(
            "{1}{1}25\n{0}{}Zero\n{1}{}One\n{1}{}123"
        ))
    }

    @Test
    fun subPreservesLiteralAnglesAndEntities() {
        assertEquals("[00:01.00]A < B > C &amp; D", converter.convertSubToLrc(
            "{1}{1}25\n{25}{50}A < B > C &amp; D"
        ))
    }

    @Test
    fun smiOnlyReadsRealStartAttribute() {
        assertEquals("[00:01.00]Hello", converter.convertSmiToLrc(
            "<SYNC title=\"example start=2000 \" Start=1000><P>Hello"
        ))
        assertParseFailure("SMI", 1) {
            converter.convertSmiToLrc("<SYNC title=\"start=2000\"><P>Lost")
        }
    }

    @Test
    fun smiQuotedAnglesDoNotEndTagsOrCreateSyncs() {
        assertEquals("[00:01.00]Hello", converter.convertSmiToLrc(
            "<P title=\"<SYNC Start=9999>\">ignored</P>" +
                "<SYNC title='a > b' Start=1000><P title=\"c > d\">Hello"
        ))
    }

    @Test
    fun smiRejectsDuplicateStartAndBrokenAttributes() {
        listOf(" Start=1000 Start=2000", " Start=1000 broken==value").forEach { attributes ->
            assertParseFailure("SMI", 1) { converter.convertSmiToLrc("<SYNC$attributes><P>Lost") }
        }
    }

    @Test
    fun smiRejectsUnterminatedTagsInsteadOfReturningPartialText() {
        listOf("<SYNC Start=2000 title='unfinished>", "<SYNC Start=2000\n<P>Lost").forEach { broken ->
            assertParseFailure("SMI", 2) {
                converter.convertSmiToLrc("<SYNC Start=1000><P>Good\n$broken")
            }
        }
    }

    @Test
    fun smiLongQuotedAttributesKeepTheirBoundary() {
        val title = "a".repeat(32768) + " > start=2000"
        assertEquals("[00:01.00]Hello", converter.convertSmiToLrc(
            "<SYNC title=\"$title\" Start=1000><P>Hello"
        ))
    }

    @Test
    fun assMalformedEventPrefixRejectsPartialOutput() {
        assertParseFailure("ASS/SSA", 3) {
            converter.convertAssToLrc("[Events]\n" +
                "Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Good\n" +
                "Dialogue 0,0:00:03.00,0:00:04.00,Default,,0,0,0,,Lost")
        }
    }

    @Test
    fun assEmptyZeroDurationEventDoesNotBlockCaptions() {
        assertEquals("[00:01.00]Good", converter.convertAssToLrc("[Events]\n" +
            "Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Good\n" +
            "Dialogue: 0,0:00:00.00,0:00:00.00,Default,,0,0,0,,"))
    }

    @Test
    fun assInvalidNonemptyOrReversedEventsStillFail() {
        listOf("0:00:01.00,0:00:01.00,Hello", "0:00:02.00,0:00:01.00,").forEach { event ->
            assertParseFailure("ASS/SSA", 3) {
                converter.convertAssToLrc("[Events]\nFormat: Start, End, Text\nDialogue: $event")
            }
        }
    }

    @Test
    fun assCommentsDoNotBecomeMalformedDialogue() {
        assertEquals("[00:01.00]Good", converter.convertAssToLrc("[Events]\n" +
            "Comment: 0,0:00:00.00,0:00:00.00,Default,,0,0,0,,Dialogue without colon\n" +
            "Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Good"))
    }

    @Test
    fun invalidUtf8FailsExplicitlyAndClosesStream() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://conversion/invalid-utf8.srt")
        listOf(byteArrayOf(0xC3.toByte(), 0x28), byteArrayOf(0xE4.toByte(), 0xBD.toByte())).forEach { invalid ->
            var closed = false
            val bytes = "1\n00:00:01,000 --> 00:00:02,000\nA ".toByteArray() + invalid
            shadowOf(context.contentResolver).registerInputStream(uri, object : ByteArrayInputStream(bytes) {
                override fun close() { closed = true; super.close() }
            })
            val error = assertThrows(IOException::class.java) {
                SubtitleConverter(context, AppSettings()).convertToLrc(uri, "invalid-utf8.srt")
            }
            assertTrue(error.message, error.message!!.contains("UTF-8"))
            assertTrue(error.cause is CharacterCodingException)
            assertTrue(closed)
        }
    }

    private fun assertParseFailure(format: String, line: Int, action: () -> Unit) {
        val error = assertThrows(SubtitleParseException::class.java) { action() }
        assertEquals(format, error.format)
        assertEquals(line, error.lineNumber)
        assertTrue(error.message, error.message!!.contains(format))
        assertTrue(error.message, error.message!!.contains("line $line:"))
    }
}
