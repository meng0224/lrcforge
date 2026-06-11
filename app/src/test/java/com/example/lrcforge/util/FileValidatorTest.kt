package com.example.lrcforge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileValidatorTest {

    @Test
    fun supportedExtensionsAreAcceptedCaseInsensitively() {
        assertTrue(FileValidator.validateFormat("song.SRT"))
        assertTrue(FileValidator.validateFormat("lyric.ass"))
        assertTrue(FileValidator.validateFormat("subtitle.vtt"))
    }

    @Test
    fun unsupportedExtensionsAreRejected() {
        assertFalse(FileValidator.validateFormat("song.txt"))
        assertFalse(FileValidator.validateFormat("song"))
    }

    @Test
    fun validateFileAcceptsFilesAtTenMegabytes() {
        val result = FileValidator.validateFile("song.srt", 10L * 1024 * 1024)

        assertEquals(Pair(true, null), result)
    }

    @Test
    fun validateFileRejectsFilesLargerThanTenMegabytes() {
        val result = FileValidator.validateFile("song.srt", 10L * 1024 * 1024 + 1)

        assertEquals(Pair(false, "文件過大（超過 10MB）"), result)
    }
}
