package com.example.lrcforge.converter

import android.content.Context
import android.net.Uri
import com.example.lrcforge.model.AppSettings
import com.example.lrcforge.util.FileValidator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * 字幕轉換器核心類
 */
class SubtitleConverter(private val context: Context, private val settings: AppSettings) {

    /**
     * 轉換字幕文件為 LRC 格式
     */
    fun convertToLrc(uri: Uri, fileName: String): String? {
        return try {
            val content = readFileContent(uri)
            convertContentToLrc(content, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    internal fun convertContentToLrc(content: String, fileName: String): String? {
        val extension = FileValidator.getExtension(fileName)

        return when (extension) {
            "vtt" -> convertVttToLrc(content)
            "srt" -> convertSrtToLrc(content)
            "ass", "ssa" -> convertAssToLrc(content)
            "smi" -> convertSmiToLrc(content)
            "sub", "str" -> convertSubToLrc(content)
            else -> null
        }
    }

    /**
     * 讀取文件內容（UTF-8）
     */
    private fun readFileContent(uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                return reader.readText()
            }
        } ?: throw Exception("無法讀取文件")
    }

    /**
     * 轉換 VTT 格式
     */
    internal fun convertVttToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()

        val timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})")

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.contains("-->")) {
                val timeMatch = timePattern.matcher(line)
                if (timeMatch.find()) {
                    val startTime = formatTimeToLrc(
                        timeMatch.group(1).toInt(),
                        timeMatch.group(2).toInt(),
                        timeMatch.group(3).toInt(),
                        timeMatch.group(4).toInt()
                    )

                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }

                    val text = cleanText(textLines.joinToString(" "))
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$startTime]$text")
                    }
                    continue
                }
            }
            i++
        }

        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SRT 格式
     */
    internal fun convertSrtToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()

        val timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{3})")

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.contains("-->")) {
                val timeMatch = timePattern.matcher(line)
                if (timeMatch.find()) {
                    val startTime = formatTimeToLrc(
                        timeMatch.group(1).toInt(),
                        timeMatch.group(2).toInt(),
                        timeMatch.group(3).toInt(),
                        timeMatch.group(4).toInt()
                    )

                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }

                    val text = cleanText(textLines.joinToString(" "))
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$startTime]$text")
                    }
                    continue
                }
            }
            i++
        }

        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 ASS/SSA 格式
     */
    internal fun convertAssToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()
        var inEventsSection = false
        var textColumnIndex: Int? = null

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith(";")) {
                continue
            }

            if (trimmedLine.startsWith("[")) {
                inEventsSection = trimmedLine.equals("[Events]", ignoreCase = true)
                continue
            }

            if (!inEventsSection) {
                continue
            }

            if (trimmedLine.startsWith("Format:", ignoreCase = true)) {
                textColumnIndex = parseAssTextColumnIndex(trimmedLine)
                continue
            }

            if (!trimmedLine.startsWith("Dialogue:", ignoreCase = true)) {
                continue
            }

            val dialogue = parseAssDialogue(trimmedLine, textColumnIndex)
            if (dialogue != null) {
                val startTime = formatTimeToLrc(
                    dialogue.hours,
                    dialogue.minutes,
                    dialogue.seconds,
                    dialogue.centiseconds * 10
                )
                val text = cleanAssText(dialogue.text)
                if (text.isNotEmpty()) {
                    lrcLines.add("[$startTime]$text")
                }
            }
        }

        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SMI 格式
     */
    internal fun convertSmiToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()

        val syncPattern = Pattern.compile("<SYNC\\s+Start=(\\d+)>", Pattern.CASE_INSENSITIVE)

        var currentTime = 0L
        var currentText = StringBuilder()

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine.startsWith("<!--") || trimmedLine.startsWith("<HEAD>") ||
                trimmedLine.startsWith("</HEAD>") || trimmedLine.startsWith("<BODY>") ||
                trimmedLine.startsWith("</BODY>")) {
                continue
            }

            val syncMatch = syncPattern.matcher(trimmedLine)
            if (syncMatch.find()) {
                if (currentTime > 0 && currentText.isNotEmpty()) {
                    val timeStr = formatTimeFromMilliseconds(currentTime)
                    val text = cleanText(currentText.toString())
                    if (text.isNotEmpty()) {
                        lrcLines.add("[$timeStr]$text")
                    }
                }

                currentTime = syncMatch.group(1).toLong()
                currentText.clear()
            } else if (trimmedLine.startsWith("<P") || trimmedLine.startsWith("</P>")) {
                // ignore paragraph tags
            } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("<")) {
                if (currentText.isNotEmpty()) {
                    currentText.append(" ")
                }
                currentText.append(cleanText(trimmedLine))
            }
        }

        if (currentTime > 0 && currentText.isNotEmpty()) {
            val timeStr = formatTimeFromMilliseconds(currentTime)
            val text = cleanText(currentText.toString())
            if (text.isNotEmpty()) {
                lrcLines.add("[$timeStr]$text")
            }
        }

        return lrcLines.joinToString("\n")
    }

    /**
     * 轉換 SUB 格式
     */
    internal fun convertSubToLrc(content: String): String {
        val lines = content.lines()
        val lrcLines = mutableListOf<String>()

        val subPattern = Pattern.compile("\\{(\\d+)\\}\\{(\\d+)\\}(.*)")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val match = subPattern.matcher(trimmedLine)
            if (match.find()) {
                val startTimeMs = match.group(1).toLong()
                val text = cleanText(match.group(3))

                if (text.isNotEmpty()) {
                    val timeStr = formatTimeFromMilliseconds(startTimeMs)
                    lrcLines.add("[$timeStr]$text")
                }
            }
        }

        return lrcLines.joinToString("\n")
    }

    internal fun parseAssTextColumnIndex(formatLine: String): Int? {
        val columns = formatLine.substringAfter(':', "")
            .split(',')
            .map { it.trim().lowercase() }
        val index = columns.indexOf("text")
        return if (index >= 0) index else null
    }

    internal fun parseAssDialogue(dialogueLine: String, textColumnIndex: Int?): AssDialogue? {
        val body = dialogueLine.substringAfter(':', "").trim()
        if (body.isEmpty()) {
            return null
        }

        val resolvedTextColumnIndex = textColumnIndex ?: ASS_FALLBACK_TEXT_COLUMN_INDEX
        val fields = body.split(',', limit = resolvedTextColumnIndex + 1)
        if (fields.size <= resolvedTextColumnIndex || fields.size <= ASS_START_COLUMN_INDEX) {
            return null
        }

        val timeMatch = ASS_TIME_PATTERN.matcher(fields[ASS_START_COLUMN_INDEX].trim())
        if (!timeMatch.matches()) {
            return null
        }

        return AssDialogue(
            hours = timeMatch.group(1).toInt(),
            minutes = timeMatch.group(2).toInt(),
            seconds = timeMatch.group(3).toInt(),
            centiseconds = timeMatch.group(4).toInt(),
            text = fields[resolvedTextColumnIndex].trim()
        )
    }

    private fun cleanText(text: String): String {
        return text
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanAssText(text: String): String {
        return text
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\\\[Nn]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatTimeToLrc(hours: Int, minutes: Int, seconds: Int, milliseconds: Int): String {
        val totalMinutes = hours * 60 + minutes
        val totalSeconds = seconds

        return if (settings.timePrecision) {
            val centiseconds = milliseconds / 10
            String.format("%02d:%02d.%02d", totalMinutes, totalSeconds, centiseconds)
        } else {
            String.format("%02d:%02d.00", totalMinutes, totalSeconds)
        }
    }

    private fun formatTimeFromMilliseconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val centiseconds = ((milliseconds % 1000) / 10).toInt()

        return if (settings.timePrecision) {
            String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
        } else {
            String.format("%02d:%02d.00", minutes, seconds)
        }
    }

    internal data class AssDialogue(
        val hours: Int,
        val minutes: Int,
        val seconds: Int,
        val centiseconds: Int,
        val text: String
    )

    companion object {
        private const val ASS_START_COLUMN_INDEX = 1
        private const val ASS_FALLBACK_TEXT_COLUMN_INDEX = 9
        private val ASS_TIME_PATTERN = Pattern.compile("^(\\d+):(\\d{2}):(\\d{2})[.,](\\d{2})$")
    }
}
