package com.example.lrcforge.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileStatusTest {

    @Test
    fun invalidFilesAreNotEligibleForConversion() {
        assertFalse(FileStatus.INVALID.isEligibleForConversion())
    }

    @Test
    fun pendingAndRetryableErrorsRemainEligibleForConversion() {
        assertTrue(FileStatus.PENDING.isEligibleForConversion())
        assertTrue(FileStatus.ERROR.isEligibleForConversion())
    }
}
