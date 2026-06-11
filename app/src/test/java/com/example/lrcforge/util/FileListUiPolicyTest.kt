package com.example.lrcforge.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileListUiPolicyTest {

    @Test
    fun clearFileListDisabledWhenListIsEmpty() {
        assertFalse(FileListUiPolicy.canClearFileList(0))
    }

    @Test
    fun clearFileListEnabledWhenListHasItems() {
        assertTrue(FileListUiPolicy.canClearFileList(1))
        assertTrue(FileListUiPolicy.canClearFileList(5))
    }
}
