package androidx.documentfile.provider

import android.net.Uri

class FakeDocumentFile(
    private val displayName: String,
    private val directory: Boolean,
    private val fileType: String? = null,
    private val exists: Boolean = true,
    private val length: Long = 0L
) : DocumentFile(null) {
    val children = mutableListOf<FakeDocumentFile>()
    var createFileCalls = 0
    var createDirectoryCalls = 0
    var deleteCalls = 0

    override fun createFile(mimeType: String, displayName: String): DocumentFile {
        createFileCalls++
        return FakeDocumentFile(displayName, false, mimeType, length = 16L).also { children.add(it) }
    }

    override fun createDirectory(displayName: String): DocumentFile {
        createDirectoryCalls++
        return FakeDocumentFile(displayName, true).also { children.add(it) }
    }

    override fun getUri(): Uri = Uri.parse("content://test/$displayName")

    override fun getName(): String = displayName

    override fun getType(): String? = fileType

    override fun isDirectory(): Boolean = directory

    override fun isFile(): Boolean = !directory

    override fun isVirtual(): Boolean = false

    override fun lastModified(): Long = 0L

    override fun length(): Long = length

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = true

    override fun delete(): Boolean {
        deleteCalls++
        return true
    }

    override fun exists(): Boolean = exists

    override fun listFiles(): Array<DocumentFile> = children.toTypedArray()

    override fun findFile(displayName: String): DocumentFile? {
        return children.firstOrNull { it.name == displayName }
    }

    override fun renameTo(displayName: String): Boolean = false
}
