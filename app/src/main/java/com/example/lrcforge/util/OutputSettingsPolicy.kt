package com.example.lrcforge.util

import com.example.lrcforge.model.AppSettings

object OutputSettingsPolicy {
    fun canEnableSourceDirectoryOutput(settings: AppSettings): Boolean {
        return settings.outputDirUri == null
    }

    fun canSelectCustomOutputDirectory(settings: AppSettings): Boolean {
        return !settings.outputToSourceDirectory
    }
}
