package com.example.lrcforge.model

data class AppSettings(
    var smartNaming: Boolean = true,
    var timePrecision: Boolean = true,
    var outputDirUri: String? = null,
    var outputToSourceDirectory: Boolean = false,
    var recursiveImportEnabled: Boolean = false
)
