package com.example.lrcforge

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lrcforge.adapter.SubtitleFileAdapter
import com.example.lrcforge.converter.SubtitleConverter
import com.example.lrcforge.model.AppSettings
import com.example.lrcforge.model.FileStatus
import com.example.lrcforge.model.SubtitleFile
import com.example.lrcforge.model.isEligibleForConversion
import com.example.lrcforge.util.FileListUiPolicy
import com.example.lrcforge.util.FileNameHelper
import com.example.lrcforge.util.FileValidator
import com.example.lrcforge.util.ImportSelectionCoordinator
import com.example.lrcforge.util.OutputSettingsPolicy
import com.example.lrcforge.util.SettingsManager
import com.example.lrcforge.util.SourceDirectoryPathHelper
import com.example.lrcforge.util.SourceSaveAuthorizationState
import com.example.lrcforge.util.StorageHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubtitleFileAdapter
    private lateinit var rootLayout: View
    private lateinit var authBannerCard: MaterialCardView
    private lateinit var progressCard: MaterialCardView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgress: TextView
    private lateinit var tvProgressCount: TextView
    private lateinit var btnSelectFiles: MaterialButton
    private lateinit var btnConvert: MaterialButton
    private lateinit var btnAuthorizeSourceDir: MaterialButton
    private lateinit var tvOutputDir: TextView
    private lateinit var tvOutputDirHint: TextView
    private lateinit var tvStorageModeChip: Chip
    private lateinit var tvAuthBannerMessage: TextView
    private lateinit var tvFileListTitle: TextView
    private lateinit var tvFileSummary: TextView
    private lateinit var btnSelectOutputDir: MaterialButton
    private lateinit var btnClearOutputDir: MaterialButton
    private lateinit var btnClearFileList: MaterialButton
    private lateinit var switchOutputToSourceDirectory: MaterialSwitch
    private lateinit var switchRecursiveImport: MaterialSwitch
    private lateinit var toolbar: MaterialToolbar
    private lateinit var emptyStateContainer: View
    private lateinit var fileListCard: View
    private lateinit var secondaryActionsRow: View
    private lateinit var layoutCustomOutputActions: View

    private val files = mutableListOf<SubtitleFile>()
    private var settings = AppSettings()
    private var isConversionInProgress = false
    private var conversionProcessedCount = 0
    private var conversionTotalCount = 0

    private val sourceSaveAuthorizationState = SourceSaveAuthorizationState()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            openImportPicker()
        } else {
            showSystemToast("未授予讀取外部儲存的權限")
        }
    }

    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val authorizationKey = sourceSaveAuthorizationState.currentAuthorizationKey
        if (authorizationKey != null) {
            handleAuthorizationResult(authorizationKey, uri)
        } else if (uri != null) {
            handleDirectorySelection(uri)
        }
    }

    private val importDirectoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            handleSelectedDirectory(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSettings()
        setupRecyclerView()
        setupClickListeners()
        updateUiState()
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.rootLayout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = ""
        toolbar.subtitle = ""

        recyclerView = findViewById(R.id.recyclerView)
        authBannerCard = findViewById(R.id.authBannerCard)
        progressCard = findViewById(R.id.progressCard)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        tvProgressCount = findViewById(R.id.tvProgressCount)
        btnSelectFiles = findViewById(R.id.btnSelectFiles)
        btnConvert = findViewById(R.id.btnConvert)
        btnAuthorizeSourceDir = findViewById(R.id.btnAuthorizeSourceDir)
        tvOutputDir = findViewById(R.id.tvOutputDir)
        tvOutputDirHint = findViewById(R.id.tvOutputDirHint)
        tvStorageModeChip = findViewById(R.id.tvStorageModeChip)
        tvAuthBannerMessage = findViewById(R.id.tvAuthBannerMessage)
        tvFileListTitle = findViewById(R.id.tvFileListTitle)
        tvFileSummary = findViewById(R.id.tvFileSummary)
        btnSelectOutputDir = findViewById(R.id.btnSelectOutputDir)
        btnClearOutputDir = findViewById(R.id.btnClearOutputDir)
        btnClearFileList = findViewById(R.id.btnClearFileList)
        switchOutputToSourceDirectory = findViewById(R.id.switchOutputToSourceDirectory)
        switchRecursiveImport = findViewById(R.id.switchRecursiveImport)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        fileListCard = findViewById(R.id.fileListCard)
        secondaryActionsRow = findViewById(R.id.secondaryActionsRow)
        layoutCustomOutputActions = findViewById(R.id.layoutCustomOutputActions)
    }

    private fun loadSettings() {
        settings = SettingsManager.loadSettings(this)
        if (settings.outputDirUri != null && settings.outputToSourceDirectory) {
            settings.outputToSourceDirectory = false
            SettingsManager.saveSettings(this, settings)
        }
        syncSourceDirectorySwitch()
        syncRecursiveImportSwitch()
    }

    private fun syncSourceDirectorySwitch() {
        switchOutputToSourceDirectory.setOnCheckedChangeListener(null)
        switchOutputToSourceDirectory.isChecked = settings.outputToSourceDirectory
        switchOutputToSourceDirectory.setOnCheckedChangeListener { _, isChecked ->
            handleSourceDirectoryToggle(isChecked)
        }
    }

    private fun syncRecursiveImportSwitch() {
        switchRecursiveImport.setOnCheckedChangeListener(null)
        switchRecursiveImport.isChecked = settings.recursiveImportEnabled
        switchRecursiveImport.setOnCheckedChangeListener { _, isChecked ->
            handleRecursiveImportToggle(isChecked)
        }
    }

    private fun setupRecyclerView() {
        adapter = SubtitleFileAdapter(files)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        btnConvert.setOnClickListener {
            if (files.isEmpty()) {
                checkAndRequestImportPermission()
            } else {
                startConversion()
            }
        }

        btnSelectFiles.setOnClickListener {
            checkAndRequestImportPermission()
        }

        btnSelectOutputDir.setOnClickListener {
            if (!OutputSettingsPolicy.canSelectCustomOutputDirectory(settings)) {
                showSystemToast("已開啟輸出到原文件目錄，請先關閉其中一項")
                return@setOnClickListener
            }
            directoryPickerLauncher.launch(null)
        }

        btnClearOutputDir.setOnClickListener {
            clearCustomOutputDirectory()
        }

        btnClearFileList.setOnClickListener {
            clearFileList()
        }

        btnAuthorizeSourceDir.setOnClickListener {
            if (sourceSaveAuthorizationState.currentAuthorizationKey != null) {
                directoryPickerLauncher.launch(null)
            }
        }
    }

    private fun handleSourceDirectoryToggle(enable: Boolean) {
        if (enable && !OutputSettingsPolicy.canEnableSourceDirectoryOutput(settings)) {
            showSystemToast("已設定自訂輸出資料夾，請先清除或關閉其中一項")
            syncSourceDirectorySwitch()
            return
        }

        settings.outputToSourceDirectory = enable
        SettingsManager.saveSettings(this, settings)
        syncSourceDirectorySwitch()
        updateUiState()
    }

    private fun handleRecursiveImportToggle(enable: Boolean) {
        settings.recursiveImportEnabled = enable
        SettingsManager.saveSettings(this, settings)
        syncRecursiveImportSwitch()
        updateUiState()
    }

    private fun clearCustomOutputDirectory() {
        if (settings.outputDirUri == null) {
            showSystemToast("目前沒有自訂輸出資料夾")
            return
        }

        settings.outputDirUri = null
        SettingsManager.saveSettings(this, settings)
        updateUiState()
        showFeedback("已清除自訂輸出資料夾")
    }

    private fun clearFileList() {
        if (files.isEmpty()) {
            updateUiState()
            return
        }

        files.clear()
        adapter.notifyDataSetChanged()
        resetPendingSourceSaveState()
        isConversionInProgress = false
        conversionProcessedCount = 0
        conversionTotalCount = 0
        updateUiState()
        showFeedback("已清除文件列表")
    }

    private fun checkAndRequestImportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openImportPicker()
            return
        }

        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(permission))
        } else {
            openImportPicker()
        }
    }

    private fun openImportPicker() {
        if (settings.recursiveImportEnabled) {
            openImportDirectoryPicker()
        } else {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val mimeTypes = arrayOf("*/*")
        try {
            filePickerLauncher.launch(mimeTypes)
        } catch (e: Exception) {
            showSystemToast("無法打開文件選擇器: ${e.message}")
        }
    }

    private fun openImportDirectoryPicker() {
        try {
            importDirectoryPickerLauncher.launch(null)
        } catch (e: Exception) {
            showSystemToast("無法打開資料夾選擇器: ${e.message}")
        }
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newFiles = uris.mapNotNull { buildSubtitleFile(it, includeInvalidFiles = true) }
            withContext(Dispatchers.Main) {
                applyImportedFiles(
                    importedFiles = newFiles,
                    appendToExisting = settings.outputToSourceDirectory,
                    skippedInvalidCount = 0,
                    isRecursiveImport = false
                )
            }
        }
    }

    private fun handleSelectedDirectory(treeUri: Uri) {
        tryTakePersistableTreePermission(treeUri)
        val importRootInfo = resolveImportRootInfo(treeUri) ?: run {
            showSystemToast("無法取得匯入資料夾資訊")
            return
        }
        SettingsManager.saveImportRootDirectoryUri(this, importRootInfo.key, treeUri.toString())

        lifecycleScope.launch(Dispatchers.IO) {
            val scanResult = scanSubtitleFilesFromDirectory(treeUri, importRootInfo)
            withContext(Dispatchers.Main) {
                applyImportedFiles(
                    importedFiles = scanResult.files,
                    appendToExisting = true,
                    skippedInvalidCount = scanResult.skippedInvalidCount,
                    isRecursiveImport = true
                )
            }
        }
    }

    private fun applyImportedFiles(
        importedFiles: List<SubtitleFile>,
        appendToExisting: Boolean,
        skippedInvalidCount: Int,
        isRecursiveImport: Boolean
    ) {
        val importResult = ImportSelectionCoordinator.mergeAndDescribe(
            existingFiles = files,
            importedFiles = importedFiles,
            appendToExisting = appendToExisting,
            skippedInvalidCount = skippedInvalidCount,
            isRecursiveImport = isRecursiveImport
        )

        files.clear()
        files.addAll(importResult.files)
        adapter.notifyDataSetChanged()
        updateUiState()
        showFeedback(importResult.message)
    }

    private fun scanSubtitleFilesFromDirectory(treeUri: Uri, importRootInfo: ImportRootInfo): DirectoryImportResult {
        val root = DocumentFile.fromTreeUri(this, treeUri) ?: return DirectoryImportResult(emptyList(), 0)
        val pendingDirectories = ArrayDeque<DirectoryQueueEntry>()
        val collectedFiles = mutableListOf<SubtitleFile>()
        var skippedInvalidCount = 0

        pendingDirectories.add(DirectoryQueueEntry(root, ""))
        while (pendingDirectories.isNotEmpty()) {
            val entry = pendingDirectories.removeFirst()
            entry.directory.listFiles().forEach { child ->
                when {
                    child.isDirectory -> {
                        val childName = child.name.orEmpty()
                        val nextPath = if (entry.relativePath.isBlank()) {
                            childName
                        } else {
                            "${entry.relativePath}/$childName"
                        }
                        pendingDirectories.add(DirectoryQueueEntry(child, nextPath))
                    }
                    child.isFile -> {
                        val subtitleFile = buildSubtitleFile(
                            uri = child.uri,
                            includeInvalidFiles = false,
                            importRootInfo = importRootInfo,
                            relativeDirectoryPath = entry.relativePath.ifBlank { null }
                        )
                        if (subtitleFile != null) {
                            collectedFiles.add(subtitleFile)
                        } else {
                            skippedInvalidCount++
                        }
                    }
                }
            }
        }

        return DirectoryImportResult(collectedFiles, skippedInvalidCount)
    }

    private fun buildSubtitleFile(
        uri: Uri,
        includeInvalidFiles: Boolean,
        importRootInfo: ImportRootInfo? = null,
        relativeDirectoryPath: String? = null
    ): SubtitleFile? {
        return try {
            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)
            val sourceDirectoryInfo = resolveSourceDirectoryInfo(uri)
            val (isValid, errorMessage) = FileValidator.validateFile(fileName, fileSize)

            if (!isValid && !includeInvalidFiles) {
                return null
            }

            SubtitleFile(
                uri = uri,
                fileName = fileName,
                fileSize = fileSize,
                status = if (isValid) FileStatus.PENDING else FileStatus.INVALID,
                errorMessage = errorMessage,
                sourceDirectoryKey = sourceDirectoryInfo?.key,
                sourceDirectoryLabel = sourceDirectoryInfo?.label,
                importRootDirectoryKey = importRootInfo?.key,
                importRootDirectoryLabel = importRootInfo?.label,
                relativeDirectoryPath = relativeDirectoryPath
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun tryTakePersistableTreePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName.ifEmpty { "未知文件" }
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    private fun startConversion() {
        settings = SettingsManager.loadSettings(this)

        val filesToProcess = files.filter { it.status.isEligibleForConversion() }
        if (filesToProcess.isEmpty()) {
            showFeedback("沒有可轉換的文件")
            return
        }

        isConversionInProgress = true
        conversionProcessedCount = 0
        conversionTotalCount = filesToProcess.size
        progressBar.progress = 0
        tvProgress.text = "正在處理文件..."
        updateUiState()

        lifecycleScope.launch(Dispatchers.IO) {
            val converter = SubtitleConverter(this@MainActivity, settings)
            var processedCount = 0

            for (file in filesToProcess) {
                val fileIndex = files.indexOf(file)
                if (fileIndex < 0) continue

                withContext(Dispatchers.Main) {
                    files[fileIndex].status = FileStatus.PROCESSING
                    adapter.updateFile(fileIndex, files[fileIndex])
                    updateUiState()
                }

                try {
                    val lrcContent = converter.convertToLrc(file.uri, file.fileName)
                    if (lrcContent?.isNotEmpty() == true) {
                        val outputFileName = FileNameHelper.smartNaming(file.fileName, settings.smartNaming)
                        withContext(Dispatchers.Main) {
                            files[fileIndex].status = FileStatus.SUCCESS
                            files[fileIndex].outputFileName = outputFileName
                            files[fileIndex].lrcContent = lrcContent
                            files[fileIndex].errorMessage = null
                            adapter.updateFile(fileIndex, files[fileIndex])
                            updateUiState()
                        }
                    } else {
                        throw IllegalStateException("解析錯誤或內容為空")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        files[fileIndex].status = FileStatus.ERROR
                        files[fileIndex].errorMessage = "轉換失敗: ${e.message}"
                        adapter.updateFile(fileIndex, files[fileIndex])
                        updateUiState()
                    }
                }

                processedCount++
                val progress = (processedCount * 100) / filesToProcess.size
                withContext(Dispatchers.Main) {
                    conversionProcessedCount = processedCount
                    progressBar.progress = progress
                    tvProgress.text = "正在處理文件... $progress%"
                    updateUiState()
                }
            }

            withContext(Dispatchers.Main) {
                isConversionInProgress = false
                conversionProcessedCount = processedCount
                updateUiState()

                val successCount = files.count { it.status == FileStatus.SUCCESS }
                if (successCount > 0) {
                    downloadAllFiles()
                } else {
                    showFeedback("轉換完成，未產生可保存文件")
                }
            }
        }
    }

    private fun downloadAllFiles() {
        val successFiles = files.filter {
            it.status == FileStatus.SUCCESS && it.lrcContent != null && it.outputFileName != null
        }
        if (successFiles.isEmpty()) {
            showFeedback("沒有可導出的文件")
            return
        }

        if (settings.outputToSourceDirectory) {
            downloadAllFilesToSourceDirectories(successFiles)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val filesToSave = successFiles.map { it.outputFileName!! to it.lrcContent!! }
            val outputDirUri = settings.outputDirUri?.let(Uri::parse)
            val savedFiles = StorageHelper.saveMultipleFiles(this@MainActivity, outputDirUri, filesToSave)
            withContext(Dispatchers.Main) {
                showFeedback("已保存 $savedFiles 個文件")
            }
        }
    }

    private fun downloadAllFilesToSourceDirectories(successFiles: List<SubtitleFile>) {
        resetPendingSourceSaveState()

        successFiles.forEach { file ->
            val fileIndex = files.indexOf(file)
            if (fileIndex < 0) {
                return@forEach
            }

            val importRootKey = file.importRootDirectoryKey
            if (importRootKey != null) {
                val savedTreeUri = SettingsManager.getImportRootDirectoryUri(this, importRootKey)?.let(Uri::parse)
                if (savedTreeUri != null && matchesDirectoryKey(savedTreeUri, importRootKey)) {
                    sourceSaveAuthorizationState.addReadyTarget(
                        StorageHelper.OutputTarget(
                            directoryUri = savedTreeUri,
                            fileName = file.outputFileName!!,
                            content = file.lrcContent!!,
                            fileIndex = fileIndex,
                            sourceDirectoryKey = importRootKey,
                            relativeDirectoryPath = file.relativeDirectoryPath
                        )
                    )
                } else {
                    sourceSaveAuthorizationState.addPendingOutput(
                        SourceSaveAuthorizationState.PendingSourceOutput(
                            fileIndex = fileIndex,
                            authorizationKey = importRootKey,
                            authorizationLabel = file.importRootDirectoryLabel ?: "匯入根目錄",
                            authorizationMode = SourceSaveAuthorizationState.AuthorizationMode.IMPORT_ROOT,
                            fileName = file.outputFileName!!,
                            content = file.lrcContent!!,
                            relativeDirectoryPath = file.relativeDirectoryPath
                        )
                    )
                }
                return@forEach
            }

            val sourceDirectoryKey = file.sourceDirectoryKey
            if (sourceDirectoryKey == null) {
                sourceSaveAuthorizationState.addFailure(fileIndex, "保存失敗: 無法判定來源目錄")
                return@forEach
            }

            val savedTreeUri = SettingsManager.getSourceDirectoryUri(this, sourceDirectoryKey)?.let(Uri::parse)
            if (savedTreeUri != null && matchesDirectoryKey(savedTreeUri, sourceDirectoryKey)) {
                sourceSaveAuthorizationState.addReadyTarget(
                    StorageHelper.OutputTarget(
                        directoryUri = savedTreeUri,
                        fileName = file.outputFileName!!,
                        content = file.lrcContent!!,
                        fileIndex = fileIndex,
                        sourceDirectoryKey = sourceDirectoryKey,
                        relativeDirectoryPath = file.relativeDirectoryPath
                    )
                )
            } else {
                sourceSaveAuthorizationState.addPendingOutput(
                    SourceSaveAuthorizationState.PendingSourceOutput(
                        fileIndex = fileIndex,
                        authorizationKey = sourceDirectoryKey,
                        authorizationLabel = file.sourceDirectoryLabel ?: "來源目錄",
                        authorizationMode = SourceSaveAuthorizationState.AuthorizationMode.SOURCE_DIRECTORY,
                        fileName = file.outputFileName!!,
                        content = file.lrcContent!!,
                        relativeDirectoryPath = file.relativeDirectoryPath
                    )
                )
            }
        }

        if (!sourceSaveAuthorizationState.hasPendingOutputs()) {
            savePendingSourceTargets()
            return
        }

        sourceSaveAuthorizationState.enqueuePendingAuthorizationKeys()
        requestNextAuthorization()
    }

    private fun requestNextAuthorization() {
        val request = sourceSaveAuthorizationState.requestNextAuthorization()
        if (request == null) {
            updateUiState()
            savePendingSourceTargets()
            return
        }

        updateUiState()
        showFeedback(
            if (request.mode == SourceSaveAuthorizationState.AuthorizationMode.IMPORT_ROOT) {
                "請重新授權匯入根目錄：${request.label}"
            } else {
                "請授權來源目錄：${request.label}"
            }
        )
    }

    private fun handleAuthorizationResult(authorizationKey: String, treeUri: Uri?) {
        val authorizationMode = sourceSaveAuthorizationState.modeFor(authorizationKey)
        if (treeUri == null) {
            markPendingOutputsForAuthorization(authorizationKey, "保存失敗: 未授權${authorizationMode.displayName}")
            sourceSaveAuthorizationState.clearCurrentAuthorization()
            updateUiState()
            requestNextAuthorization()
            return
        }

        if (!matchesDirectoryKey(treeUri, authorizationKey)) {
            showSystemToast("選取的目錄與${authorizationMode.displayName}不符，請重新選擇")
            directoryPickerLauncher.launch(null)
            return
        }

        tryTakePersistableTreePermission(treeUri)
        when (authorizationMode) {
            SourceSaveAuthorizationState.AuthorizationMode.SOURCE_DIRECTORY -> {
                SettingsManager.saveSourceDirectoryUri(this, authorizationKey, treeUri.toString())
            }
            SourceSaveAuthorizationState.AuthorizationMode.IMPORT_ROOT -> {
                SettingsManager.saveImportRootDirectoryUri(this, authorizationKey, treeUri.toString())
            }
        }
        movePendingOutputsToReadyTargets(authorizationKey, treeUri)
        sourceSaveAuthorizationState.clearCurrentAuthorization()
        updateUiState()
        requestNextAuthorization()
    }

    private fun movePendingOutputsToReadyTargets(authorizationKey: String, treeUri: Uri) {
        sourceSaveAuthorizationState.movePendingOutputsToReadyTargets(authorizationKey, treeUri)
    }

    private fun markPendingOutputsForAuthorization(authorizationKey: String, errorMessage: String) {
        sourceSaveAuthorizationState.markPendingOutputsFailed(authorizationKey, errorMessage)
    }

    private fun savePendingSourceTargets() {
        val readyTargets = sourceSaveAuthorizationState.readyTargets()
        val initialFailures = sourceSaveAuthorizationState.failures()

        if (readyTargets.isEmpty()) {
            applySaveResults(emptyList(), initialFailures)
            resetPendingSourceSaveState()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val results = StorageHelper.saveOutputTargets(this@MainActivity, readyTargets)
            withContext(Dispatchers.Main) {
                applySaveResults(results, initialFailures)
                resetPendingSourceSaveState()
            }
        }
    }

    private fun applySaveResults(
        results: List<StorageHelper.OutputResult>,
        initialFailures: Map<Int, String>
    ) {
        initialFailures.forEach { (fileIndex, errorMessage) ->
            if (fileIndex in files.indices) {
                files[fileIndex].status = FileStatus.ERROR
                files[fileIndex].errorMessage = errorMessage
                adapter.updateFile(fileIndex, files[fileIndex])
            }
        }

        results.forEach { result ->
            if (!result.isSuccess) {
                val fileIndex = result.target.fileIndex
                if (fileIndex in files.indices) {
                    files[fileIndex].status = FileStatus.ERROR
                    files[fileIndex].errorMessage = "保存失敗: ${describeTargetPath(result.target)}"
                    adapter.updateFile(fileIndex, files[fileIndex])
                }
            }
        }

        val successCount = StorageHelper.countSuccessfulOutputResults(results)
        val failureCount = initialFailures.size + results.count { !it.isSuccess }
        val firstSuccess = results.firstOrNull { it.isSuccess }
        val firstFailure = results.firstOrNull { !it.isSuccess }
        val message = when {
            successCount == 0 && failureCount > 0 -> {
                "保存失敗 ($failureCount)：${describeTargetPath(firstFailure?.target)}"
            }
            failureCount > 0 -> {
                "已保存 $successCount 個文件，失敗 $failureCount 個（${describeTargetPath(firstFailure?.target)}）"
            }
            successCount == 1 -> {
                "已保存 1 個文件：${describeSavedPath(firstSuccess)}"
            }
            else -> {
                "已保存 $successCount 個文件"
            }
        }
        showFeedback(message)
        updateUiState()
    }

    private fun describeSavedPath(result: StorageHelper.OutputResult?): String {
        val fileName = result?.savedFileName ?: result?.target?.fileName ?: "未知文件"
        return SourceDirectoryPathHelper.describeRelativeFilePath(
            fileName = fileName,
            relativeDirectoryPath = result?.target?.relativeDirectoryPath
        )
    }

    private fun describeTargetPath(target: StorageHelper.OutputTarget?): String {
        if (target == null) {
            return "未知文件"
        }
        return SourceDirectoryPathHelper.describeRelativeFilePath(
            fileName = target.fileName,
            relativeDirectoryPath = target.relativeDirectoryPath
        )
    }

    private fun resetPendingSourceSaveState() {
        sourceSaveAuthorizationState.reset()
    }

    private fun handleDirectorySelection(uri: Uri) {
        tryTakePersistableTreePermission(uri)
        settings.outputDirUri = uri.toString()
        SettingsManager.saveSettings(this, settings)
        updateUiState()
        showFeedback("已設定自訂輸出資料夾")
    }

    private fun updateUiState() {
        updateOutputDirDisplay()
        updateAuthorizationBanner()
        updateProgressSection()
        updateFileListSection()
        updateActionButtons()
    }

    private fun updateOutputDirDisplay() {
        val uriString = settings.outputDirUri
        val customDirectoryName = uriString?.let(::resolveDirectoryName)

        when {
            settings.outputToSourceDirectory -> {
                setStorageModeChip(
                    label = "原目錄",
                    backgroundColorRes = R.color.md_theme_light_primaryContainer,
                    textColorRes = R.color.md_theme_light_onPrimaryContainer
                )
                tvOutputDir.text = getString(R.string.storage_mode_source)
                tvOutputDirHint.text = if (settings.recursiveImportEnabled) {
                    "遞迴匯入時會先授權匯入根目錄，保存時沿用同一授權並重建子資料夾。"
                } else {
                    "每個文件會保存在其來源資料夾，首次寫入需要逐目錄授權。"
                }
                layoutCustomOutputActions.visibility = View.GONE
            }
            uriString != null -> {
                setStorageModeChip(
                    label = "自訂",
                    backgroundColorRes = R.color.md_theme_light_secondaryContainer,
                    textColorRes = R.color.md_theme_light_onSecondaryContainer
                )
                tvOutputDir.text = getString(R.string.storage_mode_custom)
                tvOutputDirHint.text = customDirectoryName ?: "已授權目錄"
                layoutCustomOutputActions.visibility = View.VISIBLE
                btnSelectOutputDir.text = "變更目錄"
            }
            else -> {
                setStorageModeChip(
                    label = "預設",
                    backgroundColorRes = R.color.status_pending_container,
                    textColorRes = R.color.status_pending_onContainer
                )
                tvOutputDir.text = getString(R.string.storage_mode_default)
                tvOutputDirHint.text = "/storage/emulated/0/Download"
                layoutCustomOutputActions.visibility = View.VISIBLE
                btnSelectOutputDir.text = "選擇目錄"
            }
        }

        btnClearOutputDir.isEnabled = settings.outputDirUri != null
    }

    private fun setStorageModeChip(label: String, backgroundColorRes: Int, textColorRes: Int) {
        tvStorageModeChip.text = label
        tvStorageModeChip.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(this, backgroundColorRes)
        )
        tvStorageModeChip.setTextColor(ContextCompat.getColor(this, textColorRes))
    }

    private fun updateAuthorizationBanner() {
        val request = sourceSaveAuthorizationState.currentAuthorizationRequest()
        if (request == null) {
            authBannerCard.visibility = View.GONE
            return
        }

        tvAuthBannerMessage.text = if (request.mode == SourceSaveAuthorizationState.AuthorizationMode.IMPORT_ROOT) {
            "匯入根目錄「${request.label}」的授權已失效，重新授權後會繼續把輸出寫回對應子資料夾。"
        } else {
            "首次寫入「${request.label}」時需要授權，授權後會重用此來源目錄權限。"
        }
        authBannerCard.visibility = View.VISIBLE
    }

    private fun updateProgressSection() {
        if (!isConversionInProgress || conversionTotalCount <= 0) {
            progressCard.visibility = View.GONE
            return
        }

        progressCard.visibility = View.VISIBLE
        tvProgressCount.text = "$conversionProcessedCount/$conversionTotalCount"
    }

    private fun updateFileListSection() {
        val eligibleCount = files.count { it.status.isEligibleForConversion() }
        tvFileListTitle.text = if (files.isEmpty()) {
            "文件列表"
        } else {
            "文件列表 (${files.size})"
        }
        tvFileSummary.text = if (files.isEmpty()) {
            ""
        } else {
            "$eligibleCount 個可轉換"
        }

        emptyStateContainer.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        fileListCard.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        recyclerView.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        updateRecyclerViewHeight()
    }

    private fun updateRecyclerViewHeight() {
        val layoutParams = recyclerView.layoutParams
        layoutParams.height = if (files.size >= 4) {
            resources.getDimensionPixelSize(R.dimen.file_list_max_height)
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        recyclerView.layoutParams = layoutParams
        recyclerView.isNestedScrollingEnabled = files.size >= 4
    }

    private fun updateActionButtons() {
        val eligibleCount = files.count { it.status.isEligibleForConversion() }
        btnClearFileList.isEnabled = FileListUiPolicy.canClearFileList(files.size)

        if (isConversionInProgress) {
            btnConvert.text = "轉換中..."
            btnConvert.isEnabled = false
            secondaryActionsRow.visibility = View.GONE
            return
        }

        btnConvert.isEnabled = true
        if (files.isEmpty()) {
            btnConvert.text = if (settings.recursiveImportEnabled) "選擇資料夾" else "選擇文件"
            secondaryActionsRow.visibility = View.GONE
            return
        }

        btnConvert.text = if (eligibleCount > 0) {
            "開始轉換 ($eligibleCount)"
        } else {
            "沒有可轉換的文件"
        }
        btnConvert.isEnabled = eligibleCount > 0
        btnSelectFiles.text = when {
            settings.recursiveImportEnabled -> "新增資料夾"
            settings.outputToSourceDirectory -> "新增文件"
            else -> "重新選擇"
        }
        secondaryActionsRow.visibility = View.VISIBLE
    }

    private fun resolveDirectoryName(uriString: String): String? {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromTreeUri(this, uri)
        return docFile?.name
    }

    private fun showFeedback(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(btnConvert)
            .show()
    }

    private fun showSystemToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun resolveSourceDirectoryInfo(uri: Uri): SourceDirectoryInfo? {
        if (!DocumentsContract.isDocumentUri(this, uri)) {
            return null
        }

        val authority = uri.authority ?: return null
        val documentId = DocumentsContract.getDocumentId(uri)
        val parentDocumentId = SourceDirectoryPathHelper.extractParentDocumentId(documentId) ?: return null
        return SourceDirectoryInfo(
            key = SourceDirectoryPathHelper.buildDirectoryKey(authority, parentDocumentId),
            label = SourceDirectoryPathHelper.extractSourceDirectoryLabel(parentDocumentId)
        )
    }

    private fun resolveImportRootInfo(treeUri: Uri): ImportRootInfo? {
        val authority = treeUri.authority ?: return null
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val label = DocumentFile.fromTreeUri(this, treeUri)?.name
            ?: SourceDirectoryPathHelper.extractSourceDirectoryLabel(treeDocumentId)
        return ImportRootInfo(
            key = SourceDirectoryPathHelper.buildDirectoryKey(authority, treeDocumentId),
            label = label
        )
    }

    private fun matchesDirectoryKey(treeUri: Uri, directoryKey: String): Boolean {
        val separatorIndex = directoryKey.indexOf('|')
        if (separatorIndex <= 0) {
            return false
        }

        val expectedAuthority = directoryKey.substring(0, separatorIndex)
        val expectedTreeDocumentId = directoryKey.substring(separatorIndex + 1)
        return try {
            treeUri.authority == expectedAuthority &&
                DocumentsContract.getTreeDocumentId(treeUri) == expectedTreeDocumentId
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private data class SourceDirectoryInfo(
        val key: String,
        val label: String
    )

    private data class ImportRootInfo(
        val key: String,
        val label: String
    )

    private data class DirectoryImportResult(
        val files: List<SubtitleFile>,
        val skippedInvalidCount: Int
    )

    private data class DirectoryQueueEntry(
        val directory: DocumentFile,
        val relativePath: String
    )

}

