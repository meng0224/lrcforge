package com.example.lrcforge.util

object FileListUiPolicy {
    fun canClearFileList(fileCount: Int): Boolean {
        return fileCount > 0
    }
}
