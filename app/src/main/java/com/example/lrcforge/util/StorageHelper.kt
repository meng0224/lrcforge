package com.example.lrcforge.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StorageHelper {
    private const val LRC_MIME_TYPE = "application/octet-stream"

    data class OutputTarget(
        val directoryUri: Uri,
        val fileName: String,
        val content: String,
        val fileIndex: Int,
        val sourceDirectoryKey: String? = null,
        val relativeDirectoryPath: String? = null
    )

    data class OutputResult(
        val target: OutputTarget,
        val savedUri: Uri?,
        val savedFileName: String?,
        val bytesWritten: Long
    ) {
        val isSuccess: Boolean
            get() = savedUri != null && savedFileName != null && bytesWritten > 0
    }

    internal data class SavedDocumentResult(
        val savedUri: Uri?,
        val savedFileName: String?,
        val bytesWritten: Long
    ) {
        val isSuccess: Boolean
            get() = savedUri != null && savedFileName != null && bytesWritten > 0
    }

    fun getDownloadDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    private fun saveContentToUri(
        context: Context,
        dirUri: Uri,
        fileName: String,
        mimeType: String,
        contentBytes: ByteArray,
        relativeDirectoryPath: String? = null
    ): SavedDocumentResult {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return SavedDocumentResult(null, null, 0)
            val targetDir = resolveTargetDirectory(dir, relativeDirectoryPath) ?: return SavedDocumentResult(null, null, 0)
            val targetFile = getOrCreateOutputDocument(targetDir, fileName, mimeType) ?: return SavedDocumentResult(null, null, 0)
            if (!writeBytesToDocument(context, targetFile, contentBytes)) {
                return SavedDocumentResult(null, null, 0)
            }

            val actualFileName = targetFile.name
            if (!verifySavedDocument(targetFile, contentBytes.size.toLong()) || !verifySavedFileName(fileName, actualFileName)) {
                return SavedDocumentResult(null, actualFileName, 0)
            }

            SavedDocumentResult(
                savedUri = targetFile.uri,
                savedFileName = actualFileName,
                bytesWritten = contentBytes.size.toLong()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SavedDocumentResult(null, null, 0)
        }
    }

    internal fun resolveTargetDirectory(rootDir: DocumentFile, relativeDirectoryPath: String?): DocumentFile? {
        if (relativeDirectoryPath.isNullOrBlank()) {
            return rootDir
        }

        var currentDir = rootDir
        relativeDirectoryPath.split('/')
            .filter { it.isNotBlank() }
            .forEach { segment ->
                currentDir = getOrCreateChildDirectory(currentDir, segment) ?: return null
            }
        return currentDir
    }

    internal fun getOrCreateChildDirectory(parentDir: DocumentFile, directoryName: String): DocumentFile? {
        val existing = parentDir.findFile(directoryName)
        if (existing != null && existing.isDirectory) {
            return existing
        }
        return parentDir.createDirectory(directoryName)
    }

    internal fun getOrCreateOutputDocument(dir: DocumentFile, fileName: String, mimeType: String): DocumentFile? {
        return dir.findFile(fileName) ?: dir.createFile(mimeType, fileName)
    }

    internal fun verifySavedDocument(file: DocumentFile, expectedBytes: Long): Boolean {
        return file.exists() && file.length() >= expectedBytes && expectedBytes > 0
    }

    internal fun verifySavedFileName(expectedFileName: String, actualFileName: String?): Boolean {
        return actualFileName == expectedFileName
    }

    internal fun countSuccessfulResults(results: List<String?>): Int {
        return results.count { it != null }
    }

    internal fun countSuccessfulOutputResults(results: List<OutputResult>): Int {
        return results.count { it.isSuccess }
    }

    private fun writeBytesToDocument(context: Context, file: DocumentFile, contentBytes: ByteArray): Boolean {
        return context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
            outputStream.write(contentBytes)
            outputStream.flush()
            true
        } ?: false
    }

    fun saveLrcFile(context: Context, outputDirUri: Uri?, fileName: String, content: String): String? {
        return if (outputDirUri != null) {
            saveContentToUri(
                context,
                outputDirUri,
                fileName,
                LRC_MIME_TYPE,
                content.toByteArray(Charsets.UTF_8)
            ).savedFileName?.takeIf { verifySavedFileName(fileName, it) }
        } else {
            try {
                val downloadDir = getDownloadDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                file.name
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun generateZipFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "字幕轉換_$timestamp.zip"
    }

    fun saveAsZip(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>, zipFileName: String): String? {
        if (outputDirUri != null) {
            return try {
                val dir = DocumentFile.fromTreeUri(context, outputDirUri) ?: return null
                val zipFile = getOrCreateOutputDocument(dir, zipFileName, "application/zip") ?: return null
                context.contentResolver.openOutputStream(zipFile.uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        for ((fileName, content) in files) {
                            zos.putNextEntry(ZipEntry(fileName))
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                    }
                } ?: return null
                zipFile.name
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        return try {
            val downloadDir = getDownloadDirectory(context)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val zipFile = File(downloadDir, zipFileName)
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for ((fileName, content) in files) {
                    zos.putNextEntry(ZipEntry(fileName))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            zipFile.name
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun saveOutputTargets(context: Context, targets: List<OutputTarget>): List<OutputResult> {
        return targets.map { target ->
            val saveResult = saveContentToUri(
                context = context,
                dirUri = target.directoryUri,
                fileName = target.fileName,
                mimeType = LRC_MIME_TYPE,
                contentBytes = target.content.toByteArray(Charsets.UTF_8),
                relativeDirectoryPath = target.relativeDirectoryPath
            )
            OutputResult(
                target = target,
                savedUri = saveResult.savedUri,
                savedFileName = saveResult.savedFileName,
                bytesWritten = saveResult.bytesWritten
            )
        }
    }

    fun saveMultipleFiles(context: Context, outputDirUri: Uri?, files: List<Pair<String, String>>): Int {
        if (outputDirUri != null) {
            val results = files.map { (fileName, content) ->
                saveContentToUri(
                    context,
                    outputDirUri,
                    fileName,
                    LRC_MIME_TYPE,
                    content.toByteArray(Charsets.UTF_8)
                ).savedFileName?.takeIf { verifySavedFileName(fileName, it) }
            }
            return countSuccessfulResults(results)
        }

        var savedCount = 0
        val downloadDir = getDownloadDirectory(context)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        for ((fileName, content) in files) {
            try {
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                savedCount++
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return savedCount
    }
}
