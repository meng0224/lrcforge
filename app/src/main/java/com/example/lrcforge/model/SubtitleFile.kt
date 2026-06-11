package com.example.lrcforge.model

import android.net.Uri

data class SubtitleFile(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    var status: FileStatus = FileStatus.PENDING,
    var errorMessage: String? = null,
    var outputFileName: String? = null,
    var lrcContent: String? = null,
    var sourceDirectoryKey: String? = null,
    var sourceDirectoryLabel: String? = null,
    var importRootDirectoryKey: String? = null,
    var importRootDirectoryLabel: String? = null,
    var relativeDirectoryPath: String? = null
)

enum class FileStatus {
    PENDING,
    INVALID,
    PROCESSING,
    SUCCESS,
    ERROR
}

fun FileStatus.isEligibleForConversion(): Boolean {
    return this == FileStatus.PENDING || this == FileStatus.ERROR
}
