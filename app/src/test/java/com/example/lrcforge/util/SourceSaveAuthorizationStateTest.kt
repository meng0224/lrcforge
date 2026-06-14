package com.example.lrcforge.util

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSaveAuthorizationStateTest {

    @Test
    fun pendingOutputsAreGroupedIntoDistinctAuthorizationRequests() {
        val state = SourceSaveAuthorizationState()
        state.addPendingOutput(pendingOutput(fileIndex = 0, authorizationKey = "source-a"))
        state.addPendingOutput(pendingOutput(fileIndex = 1, authorizationKey = "source-a"))
        state.addPendingOutput(pendingOutput(fileIndex = 2, authorizationKey = "source-b"))

        state.enqueuePendingAuthorizationKeys()

        assertEquals("source-a", state.requestNextAuthorization()?.key)
        assertEquals("source-b", state.requestNextAuthorization()?.key)
        assertNull(state.requestNextAuthorization())
    }

    @Test
    fun authorizedPendingOutputsMoveToReadyTargets() {
        val state = SourceSaveAuthorizationState()
        val treeUri = Uri.parse("content://tree/source-a")
        state.addPendingOutput(pendingOutput(fileIndex = 3, authorizationKey = "source-a"))

        state.movePendingOutputsToReadyTargets("source-a", treeUri)

        val target = state.readyTargets().single()
        assertEquals(treeUri, target.directoryUri)
        assertEquals("song.lrc", target.fileName)
        assertEquals(3, target.fileIndex)
    }

    @Test
    fun deniedPendingOutputsBecomeFailures() {
        val state = SourceSaveAuthorizationState()
        state.addPendingOutput(pendingOutput(fileIndex = 4, authorizationKey = "source-a"))

        state.markPendingOutputsFailed("source-a", "保存失敗: 未授權來源目錄")

        assertEquals("保存失敗: 未授權來源目錄", state.failures()[4])
        assertTrue(state.readyTargets().isEmpty())
    }

    private fun pendingOutput(
        fileIndex: Int,
        authorizationKey: String
    ): SourceSaveAuthorizationState.PendingSourceOutput {
        return SourceSaveAuthorizationState.PendingSourceOutput(
            fileIndex = fileIndex,
            authorizationKey = authorizationKey,
            authorizationLabel = "來源目錄",
            authorizationMode = SourceSaveAuthorizationState.AuthorizationMode.SOURCE_DIRECTORY,
            fileName = "song.lrc",
            content = "[00:00.00]Song",
            relativeDirectoryPath = null
        )
    }
}
