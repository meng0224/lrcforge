package com.example.lrcforge.converter

import android.content.Context
import android.net.Uri
import androidx.core.text.HtmlCompat
import com.example.lrcforge.R
import com.example.lrcforge.converter.SubtitleParseException.Reason
import com.example.lrcforge.model.AppSettings
import com.example.lrcforge.util.FileValidator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.regex.Pattern

/** 字幕轉換器；解析失敗由呼叫端回報，不回傳不完整的歌詞。 */
class SubtitleConverter(private val context: Context, private val settings: AppSettings) {

    fun convertToLrc(uri: Uri, fileName: String): String? {
        return convertContentToLrc(readFileContent(uri), fileName)
    }

    internal fun convertContentToLrc(content: String, fileName: String): String? {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        return when (FileValidator.getExtension(fileName)) {
            "vtt" -> convertVttToLrc(normalized)
            "srt" -> convertSrtToLrc(normalized)
            "ass", "ssa" -> convertAssToLrc(normalized)
            "smi" -> convertSmiToLrc(normalized)
            "sub", "str" -> convertSubToLrc(normalized)
            else -> null
        }
    }

    private fun readFileContent(uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            try {
                BufferedReader(InputStreamReader(inputStream, decoder)).use { reader ->
                    return reader.readText()
                }
            } catch (error: CharacterCodingException) {
                throw IOException(context.getString(R.string.conversion_invalid_utf8), error)
            }
        } ?: throw IOException(context.getString(R.string.conversion_read_failed))
    }

    internal fun convertVttToLrc(content: String): String = convertTimedText(content, "VTT", VTT_TIME_PATTERN)

    internal fun convertSrtToLrc(content: String): String = convertTimedText(content, "SRT", SRT_TIME_PATTERN)

    private fun convertTimedText(content: String, format: String, timePattern: Pattern): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        var seenCue = false
        var i = 0
        while (i < lines.size) {
            if (lines[i].isBlank()) {
                i++
                continue
            }
            val blockStart = i
            while (i < lines.size && lines[i].isNotBlank()) i++
            val blockEnd = i
            val first = lines[blockStart].trim()
            if (format == "VTT") {
                if (blockStart == 0 && (first == "WEBVTT" || first.startsWith("WEBVTT ") || first.startsWith("WEBVTT\t"))) {
                    val unexpectedTiming = (blockStart + 1 until blockEnd).firstOrNull { lines[it].contains("-->") }
                    if (unexpectedTiming != null) throw SubtitleParseException(format, unexpectedTiming + 1, Reason.INVALID_CUE)
                    continue
                }
                if (first == "NOTE" || first.startsWith("NOTE ") || first.startsWith("NOTE\t")) continue
                val hasTimingLine = blockStart + 1 < blockEnd && lines[blockStart + 1].contains("-->")
                if (!seenCue && !hasTimingLine && (first == "STYLE" || first == "REGION")) continue
            }

            val timingIndex = if (first.contains("-->")) blockStart else blockStart + 1
            if (timingIndex >= blockEnd || !lines[timingIndex].contains("-->") ||
                (format == "SRT" && timingIndex != blockStart && !first.matches(Regex("[0-9]+")))) {
                throw SubtitleParseException(format, (timingIndex + 1).coerceAtMost(blockEnd), Reason.INVALID_CUE)
            }
            val timing = lines[timingIndex].split("-->")
            if (timing.size != 2) throw SubtitleParseException(format, timingIndex + 1, Reason.INVALID_CUE)
            val start = parseTime(timing[0].trim(), timePattern, format, timingIndex + 1)
            // Cue settings follow the end timestamp; they are not part of the time.
            val endToken = timing[1].trim().substringBefore(' ').substringBefore('\t')
            val end = parseTime(endToken, timePattern, format, timingIndex + 1)
            if (end <= start) throw SubtitleParseException(format, timingIndex + 1, Reason.INVALID_TIME_RANGE)
            seenCue = true
            for (textIndex in timingIndex + 1 until blockEnd) {
                val textLine = lines[textIndex]
                if ((format == "VTT" && textLine.contains("-->")) ||
                    (format == "SRT" && SRT_TIMING_LINE.containsMatchIn(textLine))) {
                    throw SubtitleParseException(format, textIndex + 1, Reason.INVALID_CUE)
                }
            }
            val text = cleanText(lines.subList(timingIndex + 1, blockEnd).joinToString(" "))
            if (text.isNotEmpty()) lrcLines.add("[${formatTimeFromMilliseconds(start)}]$text")
        }
        return lrcLines.joinToString("\n")
    }

    internal fun convertAssToLrc(content: String): String {
        val lrcLines = mutableListOf<String>()
        var inEventsSection = false
        // The conventional layout is supported only when Format is absent.
        var columns = AssColumns(1, 2, 9)
        for ((index, line) in content.lines().withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith(";")) continue
            if (trimmedLine.startsWith("[")) {
                inEventsSection = trimmedLine.equals("[Events]", ignoreCase = true)
                columns = AssColumns(1, 2, 9)
                continue
            }
            if (!inEventsSection) continue
            if (trimmedLine.startsWith("Format:", ignoreCase = true)) {
                columns = parseAssColumns(trimmedLine, index + 1)
                continue
            }
            if (!trimmedLine.startsWith("Dialogue:", ignoreCase = true)) {
                // Non-dialogue ASS/SSA events do not produce lyrics; unknown lines are errors.
                if (ASS_NON_DIALOGUE_EVENT.containsMatchIn(trimmedLine)) continue
                throw SubtitleParseException("ASS/SSA", index + 1, Reason.INVALID_ASS_DIALOGUE)
            }
            val fields = trimmedLine.substringAfter(':').trim().split(',', limit = columns.text + 1)
            if (fields.size != columns.text + 1) {
                throw SubtitleParseException("ASS/SSA", index + 1, Reason.INVALID_ASS_DIALOGUE)
            }
            val start = parseTime(fields[columns.start].trim(), ASS_TIME_PATTERN, "ASS/SSA", index + 1, 10)
            val end = parseTime(fields[columns.end].trim(), ASS_TIME_PATTERN, "ASS/SSA", index + 1, 10)
            val text = cleanAssText(fields[columns.text])
            if (end < start || (end == start && text.isNotEmpty())) {
                throw SubtitleParseException("ASS/SSA", index + 1, Reason.INVALID_TIME_RANGE)
            }
            if (text.isNotEmpty()) lrcLines.add("[${formatTimeFromMilliseconds(start)}]$text")
        }
        return lrcLines.joinToString("\n")
    }

    private fun parseAssColumns(formatLine: String, lineNumber: Int): AssColumns {
        val fields = formatLine.substringAfter(':').split(',').map { it.trim().lowercase(Locale.ROOT) }
        if (fields.any { it.isEmpty() } || fields.distinct().size != fields.size ||
            fields.indexOf("start") < 0 || fields.indexOf("end") < 0 || fields.last() != "text") {
            throw SubtitleParseException("ASS/SSA", lineNumber, Reason.INVALID_ASS_FORMAT)
        }
        return AssColumns(fields.indexOf("start"), fields.indexOf("end"), fields.lastIndex)
    }

    internal fun convertSmiToLrc(content: String): String {
        // Keep offsets/newlines intact so diagnostics refer to the original file.
        val body = Regex("<!--.*?-->|<head\\b[^>]*>.*?</head\\s*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(content) { match ->
            match.value.map { if (it == '\n' || it == '\r') it else ' ' }.joinToString("")
        }
        val syncs = findSmiSyncTags(body)
        val lrcLines = mutableListOf<String>()
        var previousOffset = 0
        var lineNumber = 1
        for ((index, sync) in syncs.withIndex()) {
            lineNumber += body.substring(previousOffset, sync.range.first).count { it == '\n' }
            previousOffset = sync.range.first
            val timestamp = parseSmiStart(sync.value, lineNumber)
            val end = syncs.getOrNull(index + 1)?.range?.first ?: body.length
            val text = cleanText(body.substring(sync.range.last + 1, end))
            if (text.isNotEmpty()) lrcLines.add("[${formatTimeFromMilliseconds(timestamp)}]$text")
        }
        return lrcLines.joinToString("\n")
    }

    private fun findSmiSyncTags(body: String): List<MatchResult> {
        val syncs = mutableListOf<MatchResult>()
        var cursor = 0
        var lineNumber = 1
        while (true) {
            val start = HTML_TAG_START.find(body, cursor)?.range?.first ?: break
            lineNumber += body.substring(cursor, start).count { it == '\n' }
            val tag = HTML_TAG_PATTERN.matchAt(body, start)
                ?: throw SubtitleParseException("SMI", lineNumber, Reason.INVALID_SYNC)
            if (SMI_SYNC_TAG.containsMatchIn(tag.value)) syncs.add(tag)
            lineNumber += tag.value.count { it == '\n' }
            cursor = tag.range.last + 1
        }
        return syncs
    }

    private fun parseSmiStart(tag: String, lineNumber: Int): Long {
        val attributes = tag.substring(5, tag.length - 1).trimEnd().removeSuffix("/").trimEnd()
        var cursor = 0
        var start: Long? = null
        while (cursor < attributes.length) {
            val attribute = SMI_ATTRIBUTE.find(attributes, cursor)
            if (attribute == null || attribute.range.first != cursor) {
                throw SubtitleParseException("SMI", lineNumber, Reason.INVALID_SYNC)
            }
            if (attribute.groupValues[1].equals("start", ignoreCase = true)) {
                val value = attribute.groups[2]?.value ?: attribute.groups[3]?.value ?: attribute.groups[4]?.value
                if (start != null || value == null || !value.matches(Regex("[0-9]+"))) {
                    throw SubtitleParseException("SMI", lineNumber, Reason.INVALID_SYNC)
                }
                start = value.toLongOrNull() ?: throw SubtitleParseException("SMI", lineNumber, Reason.INVALID_SYNC)
            }
            cursor = attribute.range.last + 1
        }
        return start ?: throw SubtitleParseException("SMI", lineNumber, Reason.INVALID_SYNC)
    }

    internal fun convertSubToLrc(content: String): String {
        val lrcLines = mutableListOf<String>()
        var frameRate: BigDecimal? = null
        for ((index, line) in content.lines().withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            val match = SUB_PATTERN.matchEntire(trimmedLine)
                ?: throw SubtitleParseException("SUB", index + 1, Reason.INVALID_SUB_ENTRY)
            val start = match.groupValues[1].toLongOrNull()
                ?: throw SubtitleParseException("SUB", index + 1, Reason.INVALID_SUB_ENTRY)
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toLongOrNull()
            if (match.groupValues[2].isNotEmpty() && end == null) {
                throw SubtitleParseException("SUB", index + 1, Reason.INVALID_SUB_ENTRY)
            }
            val payload = match.groupValues[3].trim()
            if (frameRate == null && start <= 1 && (end == null || end == start)) {
                val parsedRate = payload.takeIf { it.matches(Regex("[0-9]+(?:\\.[0-9]+)?")) }?.toBigDecimalOrNull()
                if (parsedRate == null || parsedRate <= BigDecimal.ZERO) {
                    throw SubtitleParseException("SUB", index + 1, Reason.INVALID_FRAME_RATE)
                }
                frameRate = parsedRate
                continue
            }
            val rate = frameRate ?: throw SubtitleParseException("SUB", index + 1, Reason.MISSING_FRAME_RATE)
            if (start <= 1 && end == start) throw SubtitleParseException("SUB", index + 1, Reason.INVALID_FRAME_RATE)
            if (end != null && end <= start) {
                throw SubtitleParseException("SUB", index + 1, Reason.INVALID_SUB_ENTRY)
            }
            val milliseconds = try {
                start.toBigDecimal().multiply(BigDecimal(1000)).divide(rate, 0, RoundingMode.DOWN).longValueExact()
            } catch (error: ArithmeticException) {
                throw SubtitleParseException("SUB", index + 1, Reason.INVALID_TIMESTAMP)
            }
            val text = payload.replace('|', ' ').replace(Regex("\\s+"), " ").trim()
            if (text.isNotEmpty()) lrcLines.add("[${formatTimeFromMilliseconds(milliseconds)}]$text")
        }
        return lrcLines.joinToString("\n")
    }

    private fun parseTime(
        value: String,
        pattern: Pattern,
        format: String,
        lineNumber: Int,
        fractionMultiplier: Int = 1
    ): Long {
        val match = pattern.matcher(value)
        if (!match.matches()) throw SubtitleParseException(format, lineNumber, Reason.INVALID_TIMESTAMP)
        val hoursToken = match.group(1)
        val hours = if (hoursToken == null) 0 else hoursToken.toIntOrNull()
        val minutes = checkNotNull(match.group(2)).toInt()
        val seconds = checkNotNull(match.group(3)).toInt()
        val milliseconds = checkNotNull(match.group(4)).toInt() * fractionMultiplier
        if (hours == null || minutes !in 0..59 || seconds !in 0..59) {
            throw SubtitleParseException(format, lineNumber, Reason.INVALID_TIMESTAMP)
        }
        return hours.toLong() * 3_600_000 + minutes * 60_000L + seconds * 1000L + milliseconds
    }

    private fun cleanText(text: String): String {
        val withoutComments = text.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val withoutMarkup = HTML_TAG_PATTERN.replace(withoutComments) {
            if (HTML_LINE_BREAK_TAG.containsMatchIn(it.value)) " " else ""
        }
        // Decode each entity once, after removing actual tags, preserving escaped literal markup.
        val decoded = Regex("&(?:#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]+);").replace(withoutMarkup) {
            HtmlCompat.fromHtml(it.value, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        }
        return decoded.replace(Regex("[\\s\\u00A0]+"), " ").trim()
    }

    private fun cleanAssText(text: String): String {
        return text
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace(Regex("\\\\[Nnh]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatTimeFromMilliseconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = if (settings.timePrecision) (milliseconds % 1000) / 10 else 0L
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, centiseconds)
    }

    private data class AssColumns(val start: Int, val end: Int, val text: Int)

    companion object {
        private val VTT_TIME_PATTERN = Pattern.compile("(?:(\\d{2,}):)?(\\d{2}):(\\d{2})\\.(\\d{3})")
        private val SRT_TIME_PATTERN = Pattern.compile("(\\d{2,}):(\\d{2}):(\\d{2})[,.](\\d{3})")
        private val ASS_TIME_PATTERN = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})[.,](\\d{2})")
        private val SUB_PATTERN = Regex("\\{([0-9]+)\\}\\{([0-9]*)\\}(.*)")
        private val SRT_TIMING_LINE = Regex("^\\s*[0-9]+:[0-9:.,]+\\s*-->")
        private val ASS_NON_DIALOGUE_EVENT = Regex("^(?:Comment|Picture|Sound|Movie|Command):", RegexOption.IGNORE_CASE)
        private val HTML_TAG_START = Regex("<[A-Za-z/!]")
        private val HTML_TAG_PATTERN = Regex("<[^'\"<>]*(?:\"[^\"]*\"[^'\"<>]*|'[^']*'[^'\"<>]*)*>")
        private val HTML_LINE_BREAK_TAG = Regex("^</?(?:br|p)(?=\\s|/?>)", RegexOption.IGNORE_CASE)
        private val SMI_SYNC_TAG = Regex("^<sync(?=\\s|/?>)", RegexOption.IGNORE_CASE)
        private val SMI_ATTRIBUTE = Regex(
            "\\s+([A-Za-z_:][A-Za-z0-9_:.-]*)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+)))?"
        )
    }
}
