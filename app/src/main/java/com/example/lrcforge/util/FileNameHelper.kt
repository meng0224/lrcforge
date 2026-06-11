package com.example.lrcforge.util

object FileNameHelper {
    // 媒體文件擴展名列表
    private val MEDIA_EXTENSIONS = setOf(
        "mp3", "mp4", "avi", "mkv", "flac", "wav", "aac", "m4a",
        "wmv", "mov", "wma", "ogg", "webm", "flv", "3gp"
    )

    /**
     * 智能命名：移除文件名中的媒體擴展名
     */
    fun smartNaming(fileName: String, enable: Boolean): String {
        if (!enable) {
            return fileName.substringBeforeLast('.') + ".lrc"
        }

        val nameWithoutExt = fileName.substringBeforeLast('.')
        val parts = nameWithoutExt.split('.')
        
        // 檢查是否有媒體擴展名
        val filteredParts = parts.filter { part ->
            part.lowercase() !in MEDIA_EXTENSIONS
        }
        
        return if (filteredParts.isEmpty()) {
            nameWithoutExt + ".lrc"
        } else {
            filteredParts.joinToString(".") + ".lrc"
        }
    }
}
