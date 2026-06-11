package com.example.lrcforge.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.lrcforge.model.AppSettings

object SettingsManager {
    private const val PREFS_NAME = "lrc_app_settings"
    private const val KEY_OUTPUT_DIR_URI = "output_dir_uri"
    private const val KEY_OUTPUT_TO_SOURCE_DIRECTORY = "output_to_source_directory"
    private const val KEY_RECURSIVE_IMPORT_ENABLED = "recursive_import_enabled"
    private const val KEY_SOURCE_DIRECTORY_URI_PREFIX = "source_directory_uri_"
    private const val KEY_IMPORT_ROOT_DIRECTORY_URI_PREFIX = "import_root_directory_uri_"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = getSharedPreferences(context)
        return fromStoredValues(
            outputDirUri = prefs.getString(KEY_OUTPUT_DIR_URI, null),
            outputToSourceDirectory = prefs.getBoolean(KEY_OUTPUT_TO_SOURCE_DIRECTORY, false),
            recursiveImportEnabled = prefs.getBoolean(KEY_RECURSIVE_IMPORT_ENABLED, false)
        )
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_OUTPUT_DIR_URI, settings.outputDirUri)
            putBoolean(KEY_OUTPUT_TO_SOURCE_DIRECTORY, settings.outputToSourceDirectory)
            putBoolean(KEY_RECURSIVE_IMPORT_ENABLED, settings.recursiveImportEnabled)
            apply()
        }
    }

    fun getSourceDirectoryUri(context: Context, sourceDirectoryKey: String): String? {
        return getSharedPreferences(context).getString(sourceDirectoryPreferenceKey(sourceDirectoryKey), null)
    }

    fun saveSourceDirectoryUri(context: Context, sourceDirectoryKey: String, treeUri: String) {
        getSharedPreferences(context).edit()
            .putString(sourceDirectoryPreferenceKey(sourceDirectoryKey), treeUri)
            .apply()
    }

    fun getImportRootDirectoryUri(context: Context, importRootDirectoryKey: String): String? {
        return getSharedPreferences(context).getString(importRootDirectoryPreferenceKey(importRootDirectoryKey), null)
    }

    fun saveImportRootDirectoryUri(context: Context, importRootDirectoryKey: String, treeUri: String) {
        getSharedPreferences(context).edit()
            .putString(importRootDirectoryPreferenceKey(importRootDirectoryKey), treeUri)
            .apply()
    }

    internal fun fromStoredValues(
        outputDirUri: String?,
        outputToSourceDirectory: Boolean,
        recursiveImportEnabled: Boolean
    ): AppSettings {
        return AppSettings(
            smartNaming = true,
            timePrecision = true,
            outputDirUri = outputDirUri,
            outputToSourceDirectory = outputToSourceDirectory,
            recursiveImportEnabled = recursiveImportEnabled
        )
    }

    internal fun sourceDirectoryPreferenceKey(sourceDirectoryKey: String): String {
        return KEY_SOURCE_DIRECTORY_URI_PREFIX + Uri.encode(sourceDirectoryKey)
    }

    internal fun importRootDirectoryPreferenceKey(importRootDirectoryKey: String): String {
        return KEY_IMPORT_ROOT_DIRECTORY_URI_PREFIX + Uri.encode(importRootDirectoryKey)
    }
}
