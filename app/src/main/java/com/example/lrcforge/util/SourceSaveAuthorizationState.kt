package com.example.lrcforge.util

import android.net.Uri
import java.util.ArrayDeque

class SourceSaveAuthorizationState {

    private val pendingAuthorizationKeys = ArrayDeque<String>()
    private val pendingAuthorizationLabels = mutableMapOf<String, String>()
    private val pendingAuthorizationModes = mutableMapOf<String, AuthorizationMode>()
    private val pendingSourceReadyTargets = mutableListOf<StorageHelper.OutputTarget>()
    private val pendingSourceOutputs = mutableListOf<PendingSourceOutput>()
    private val pendingSourceSaveFailures = mutableMapOf<Int, String>()

    var currentAuthorizationKey: String? = null
        private set

    fun reset() {
        pendingAuthorizationKeys.clear()
        pendingAuthorizationLabels.clear()
        pendingAuthorizationModes.clear()
        pendingSourceReadyTargets.clear()
        pendingSourceOutputs.clear()
        pendingSourceSaveFailures.clear()
        currentAuthorizationKey = null
    }

    fun addReadyTarget(target: StorageHelper.OutputTarget) {
        pendingSourceReadyTargets.add(target)
    }

    fun addPendingOutput(output: PendingSourceOutput) {
        pendingSourceOutputs.add(output)
        pendingAuthorizationLabels[output.authorizationKey] = output.authorizationLabel
        pendingAuthorizationModes[output.authorizationKey] = output.authorizationMode
    }

    fun addFailure(fileIndex: Int, errorMessage: String) {
        pendingSourceSaveFailures[fileIndex] = errorMessage
    }

    fun hasPendingOutputs(): Boolean {
        return pendingSourceOutputs.isNotEmpty()
    }

    fun enqueuePendingAuthorizationKeys() {
        pendingSourceOutputs.map { it.authorizationKey }
            .distinct()
            .forEach { pendingAuthorizationKeys.add(it) }
    }

    fun requestNextAuthorization(): AuthorizationRequest? {
        if (pendingAuthorizationKeys.isEmpty()) {
            currentAuthorizationKey = null
            return null
        }

        currentAuthorizationKey = pendingAuthorizationKeys.removeFirst()
        return currentAuthorizationRequest()
    }

    fun currentAuthorizationRequest(): AuthorizationRequest? {
        val key = currentAuthorizationKey ?: return null
        return AuthorizationRequest(
            key = key,
            label = labelFor(key),
            mode = modeFor(key)
        )
    }

    fun modeFor(authorizationKey: String): AuthorizationMode {
        return pendingAuthorizationModes[authorizationKey] ?: AuthorizationMode.SOURCE_DIRECTORY
    }

    fun movePendingOutputsToReadyTargets(authorizationKey: String, treeUri: Uri) {
        val iterator = pendingSourceOutputs.iterator()
        while (iterator.hasNext()) {
            val pendingOutput = iterator.next()
            if (pendingOutput.authorizationKey == authorizationKey) {
                pendingSourceReadyTargets.add(
                    StorageHelper.OutputTarget(
                        directoryUri = treeUri,
                        fileName = pendingOutput.fileName,
                        content = pendingOutput.content,
                        fileIndex = pendingOutput.fileIndex,
                        sourceDirectoryKey = authorizationKey,
                        relativeDirectoryPath = pendingOutput.relativeDirectoryPath
                    )
                )
                iterator.remove()
            }
        }
    }

    fun markPendingOutputsFailed(authorizationKey: String, errorMessage: String) {
        val iterator = pendingSourceOutputs.iterator()
        while (iterator.hasNext()) {
            val pendingOutput = iterator.next()
            if (pendingOutput.authorizationKey == authorizationKey) {
                pendingSourceSaveFailures[pendingOutput.fileIndex] = errorMessage
                iterator.remove()
            }
        }
    }

    fun readyTargets(): List<StorageHelper.OutputTarget> {
        return pendingSourceReadyTargets.toList()
    }

    fun failures(): Map<Int, String> {
        return pendingSourceSaveFailures.toMap()
    }

    fun clearCurrentAuthorization() {
        currentAuthorizationKey = null
    }

    private fun labelFor(authorizationKey: String): String {
        return pendingAuthorizationLabels[authorizationKey] ?: "來源目錄"
    }

    data class PendingSourceOutput(
        val fileIndex: Int,
        val authorizationKey: String,
        val authorizationLabel: String,
        val authorizationMode: AuthorizationMode,
        val fileName: String,
        val content: String,
        val relativeDirectoryPath: String?
    )

    data class AuthorizationRequest(
        val key: String,
        val label: String,
        val mode: AuthorizationMode
    )

    enum class AuthorizationMode(val displayName: String) {
        SOURCE_DIRECTORY("來源目錄"),
        IMPORT_ROOT("匯入根目錄")
    }
}
