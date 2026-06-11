package com.example.lrcforge.util

import com.example.lrcforge.model.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputSettingsPolicyTest {

    @Test
    fun cannotEnableSourceDirectoryOutputWhenCustomDirectoryExists() {
        val settings = AppSettings(outputDirUri = "content://tree/downloads")

        assertFalse(OutputSettingsPolicy.canEnableSourceDirectoryOutput(settings))
    }

    @Test
    fun canEnableSourceDirectoryOutputWhenNoCustomDirectoryExists() {
        val settings = AppSettings(outputDirUri = null)

        assertTrue(OutputSettingsPolicy.canEnableSourceDirectoryOutput(settings))
    }

    @Test
    fun cannotSelectCustomDirectoryWhenSourceDirectoryOutputEnabled() {
        val settings = AppSettings(outputToSourceDirectory = true)

        assertFalse(OutputSettingsPolicy.canSelectCustomOutputDirectory(settings))
    }

    @Test
    fun canSelectCustomDirectoryWhenSourceDirectoryOutputDisabled() {
        val settings = AppSettings(outputToSourceDirectory = false)

        assertTrue(OutputSettingsPolicy.canSelectCustomOutputDirectory(settings))
    }
}
