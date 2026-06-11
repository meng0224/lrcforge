package com.example.lrcforge.util

import android.net.Uri
import android.content.Context
import com.example.lrcforge.model.SubtitleFile
import java.io.File

object FileValidator {
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    private val SUPPORTED_EXTENSIONS = setOf(
        ".vtt", ".ass", ".ssa", ".srt", ".str", ".smi", ".sub"
    )

    /**
     * 驗證文件格式
     */
    fun validateFormat(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return SUPPORTED_EXTENSIONS.contains(".$extension")
    }

    /**
     * 驗證文件大小
     */
    fun validateSize(fileSize: Long): Boolean {
        return fileSize <= MAX_FILE_SIZE
    }

    /**
     * 獲取文件擴展名
     */
    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /**
     * 檢查文件是否有效
     */
    fun validateFile(fileName: String, fileSize: Long): Pair<Boolean, String?> {
        if (!validateFormat(fileName)) {
            return Pair(false, "不支持的格式")
        }
        if (!validateSize(fileSize)) {
            return Pair(false, "文件過大（超過 10MB）")
        }
        return Pair(true, null)
    }
}
