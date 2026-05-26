package com.example.lrcapp

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
import com.example.lrcapp.adapter.SubtitleFileAdapter
import com.example.lrcapp.converter.SubtitleConverter
import com.example.lrcapp.model.AppSettings
import com.example.lrcapp.model.FileStatus
import com.example.lrcapp.model.SubtitleFile
import com.example.lrcapp.model.isEligibleForConversion
import com.example.lrcapp.util.FileListUiPolicy
import com.example.lrcapp.util.FileNameHelper
import com.example.lrcapp.util.FileSelectionPolicy
import com.example.lrcapp.util.FileValidator
import com.example.lrcapp.util.OutputSettingsPolicy
import com.example.lrcapp.util.SettingsManager
import com.example.lrcapp.util.StorageHelper
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

    private val pendingAuthorizationKeys = ArrayDeque<String>()
    private val pendingAuthorizationLabels = mutableMapOf<String, String>()
    private val pendingAuthorizationModes = mutableMapOf<String, AuthorizationMode>()
    private val pendingSourceReadyTargets = mutableListOf<StorageHelper.OutputTarget>()
    private val pendingSourceOutputs = mutableListOf<PendingSourceOutput>()
    private val pendingSourceSaveFailures = mutableMapOf<Int, String>()
    private var currentAuthorizationKey: String? = null

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
        val authorizationKey = currentAuthorizationKey
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
            if (currentAuthorizationKey != null) {
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
        val mergeResult = FileSelectionPolicy.mergeSelections(
            existingFiles = files,
            newFiles = importedFiles,
            appendToExisting = appendToExisting
        )

        files.clear()
        files.addAll(mergeResult.files)
        adapter.notifyDataSetChanged()
        updateUiState()

        val message = if (isRecursiveImport) {
            buildRecursiveImportMessage(
                addedCount = mergeResult.addedCount,
                skippedDuplicateCount = mergeResult.skippedDuplicateCount,
                skippedInvalidCount = skippedInvalidCount
            )
        } else if (appendToExisting) {
            "已新增 ${mergeResult.addedCount} 個文件，略過 ${mergeResult.skippedDuplicateCount} 個重複文件"
        } else {
            "已選擇 ${importedFiles.size} 個文件"
        }
        showFeedback(message)
    }

    private fun buildRecursiveImportMessage(
        addedCount: Int,
        skippedDuplicateCount: Int,
        skippedInvalidCount: Int
    ): String {
        val skippedSummary = mutableListOf<String>()
        if (skippedDuplicateCount > 0) {
            skippedSummary.add("$skippedDuplicateCount 個重複文件")
        }
        if (skippedInvalidCount > 0) {
            skippedSummary.add("$skippedInvalidCount 個無效文件")
        }

        if (addedCount == 0) {
            return if (skippedSummary.isEmpty()) {
                "所選資料夾中沒有可新增的字幕文件"
            } else {
                "未新增任何文件，略過 ${skippedSummary.joinToString("、")}"
            }
        }

        return if (skippedSummary.isEmpty()) {
            "已新增 $addedCount 個文件"
        } else {
            "已新增 $addedCount 個文件，略過 ${skippedSummary.joinToString("、")}"
        }
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
                    pendingSourceReadyTargets.add(
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
                    pendingSourceOutputs.add(
                        PendingSourceOutput(
                            fileIndex = fileIndex,
                            authorizationKey = importRootKey,
                            authorizationLabel = file.importRootDirectoryLabel ?: "匯入根目錄",
                            authorizationMode = AuthorizationMode.IMPORT_ROOT,
                            fileName = file.outputFileName!!,
                            content = file.lrcContent!!,
                            relativeDirectoryPath = file.relativeDirectoryPath
                        )
                    )
                    pendingAuthorizationLabels[importRootKey] = file.importRootDirectoryLabel ?: "匯入根目錄"
                    pendingAuthorizationModes[importRootKey] = AuthorizationMode.IMPORT_ROOT
                }
                return@forEach
            }

            val sourceDirectoryKey = file.sourceDirectoryKey
            if (sourceDirectoryKey == null) {
                pendingSourceSaveFailures[fileIndex] = "保存失敗: 無法判定來源目錄"
                return@forEach
            }

            val savedTreeUri = SettingsManager.getSourceDirectoryUri(this, sourceDirectoryKey)?.let(Uri::parse)
            if (savedTreeUri != null && matchesDirectoryKey(savedTreeUri, sourceDirectoryKey)) {
                pendingSourceReadyTargets.add(
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
                pendingSourceOutputs.add(
                    PendingSourceOutput(
                        fileIndex = fileIndex,
                        authorizationKey = sourceDirectoryKey,
                        authorizationLabel = file.sourceDirectoryLabel ?: "來源目錄",
                        authorizationMode = AuthorizationMode.SOURCE_DIRECTORY,
                        fileName = file.outputFileName!!,
                        content = file.lrcContent!!,
                        relativeDirectoryPath = file.relativeDirectoryPath
                    )
                )
                pendingAuthorizationLabels[sourceDirectoryKey] = file.sourceDirectoryLabel ?: "來源目錄"
                pendingAuthorizationModes[sourceDirectoryKey] = AuthorizationMode.SOURCE_DIRECTORY
            }
        }

        if (pendingSourceOutputs.isEmpty()) {
            savePendingSourceTargets()
            return
        }

        pendingSourceOutputs.map { it.authorizationKey }
            .distinct()
            .forEach { pendingAuthorizationKeys.add(it) }

        requestNextAuthorization()
    }

    private fun requestNextAuthorization() {
        if (pendingAuthorizationKeys.isEmpty()) {
            currentAuthorizationKey = null
            updateUiState()
            savePendingSourceTargets()
            return
        }

        currentAuthorizationKey = pendingAuthorizationKeys.removeFirst()
        val label = pendingAuthorizationLabels[currentAuthorizationKey] ?: "來源目錄"
        val mode = pendingAuthorizationModes[currentAuthorizationKey] ?: AuthorizationMode.SOURCE_DIRECTORY
        updateUiState()
        showFeedback(
            if (mode == AuthorizationMode.IMPORT_ROOT) {
                "請重新授權匯入根目錄：$label"
            } else {
                "請授權來源目錄：$label"
            }
        )
    }

    private fun handleAuthorizationResult(authorizationKey: String, treeUri: Uri?) {
        val authorizationMode = pendingAuthorizationModes[authorizationKey] ?: AuthorizationMode.SOURCE_DIRECTORY
        if (treeUri == null) {
            markPendingOutputsForAuthorization(authorizationKey, "保存失敗: 未授權${authorizationMode.displayName}")
            currentAuthorizationKey = null
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
            AuthorizationMode.SOURCE_DIRECTORY -> SettingsManager.saveSourceDirectoryUri(this, authorizationKey, treeUri.toString())
            AuthorizationMode.IMPORT_ROOT -> SettingsManager.saveImportRootDirectoryUri(this, authorizationKey, treeUri.toString())
        }
        movePendingOutputsToReadyTargets(authorizationKey, treeUri)
        currentAuthorizationKey = null
        updateUiState()
        requestNextAuthorization()
    }

    private fun movePendingOutputsToReadyTargets(authorizationKey: String, treeUri: Uri) {
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

    private fun markPendingOutputsForAuthorization(authorizationKey: String, errorMessage: String) {
        val iterator = pendingSourceOutputs.iterator()
        while (iterator.hasNext()) {
            val pendingOutput = iterator.next()
            if (pendingOutput.authorizationKey == authorizationKey) {
                pendingSourceSaveFailures[pendingOutput.fileIndex] = errorMessage
                iterator.remove()
            }
        }
    }

    private fun savePendingSourceTargets() {
        val readyTargets = pendingSourceReadyTargets.toList()
        val initialFailures = pendingSourceSaveFailures.toMap()

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
        val relativePath = result?.target?.relativeDirectoryPath
        return if (relativePath.isNullOrBlank()) fileName else "$relativePath/$fileName"
    }

    private fun describeTargetPath(target: StorageHelper.OutputTarget?): String {
        if (target == null) {
            return "未知文件"
        }
        return if (target.relativeDirectoryPath.isNullOrBlank()) {
            target.fileName
        } else {
            "${target.relativeDirectoryPath}/${target.fileName}"
        }
    }

    private fun resetPendingSourceSaveState() {
        pendingAuthorizationKeys.clear()
        pendingAuthorizationLabels.clear()
        pendingAuthorizationModes.clear()
        pendingSourceReadyTargets.clear()
        pendingSourceOutputs.clear()
        pendingSourceSaveFailures.clear()
        currentAuthorizationKey = null
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
        val key = currentAuthorizationKey
        if (key == null) {
            authBannerCard.visibility = View.GONE
            return
        }

        val label = pendingAuthorizationLabels[key] ?: "來源目錄"
        val mode = pendingAuthorizationModes[key] ?: AuthorizationMode.SOURCE_DIRECTORY
        tvAuthBannerMessage.text = if (mode == AuthorizationMode.IMPORT_ROOT) {
            "匯入根目錄「$label」的授權已失效，重新授權後會繼續把輸出寫回對應子資料夾。"
        } else {
            "首次寫入「$label」時需要授權，授權後會重用此來源目錄權限。"
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
        val parentDocumentId = extractParentDocumentId(documentId) ?: return null
        return SourceDirectoryInfo(
            key = "$authority|$parentDocumentId",
            label = extractSourceDirectoryLabel(parentDocumentId)
        )
    }

    private fun resolveImportRootInfo(treeUri: Uri): ImportRootInfo? {
        val authority = treeUri.authority ?: return null
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val label = DocumentFile.fromTreeUri(this, treeUri)?.name ?: extractSourceDirectoryLabel(treeDocumentId)
        return ImportRootInfo(
            key = "$authority|$treeDocumentId",
            label = label
        )
    }

    private fun extractParentDocumentId(documentId: String): String? {
        val colonIndex = documentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = documentId.substring(0, colonIndex)
            val path = documentId.substring(colonIndex + 1)
            val parentPath = path.substringBeforeLast('/', "")
            return "$root:$parentPath"
        }

        return documentId.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }
    }

    private fun extractSourceDirectoryLabel(parentDocumentId: String): String {
        val colonIndex = parentDocumentId.indexOf(':')
        if (colonIndex >= 0) {
            val root = parentDocumentId.substring(0, colonIndex)
            val path = parentDocumentId.substring(colonIndex + 1)
            return path.substringAfterLast('/', root).ifEmpty { root }
        }
        return parentDocumentId.substringAfterLast('/').ifEmpty { parentDocumentId }
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

    private data class PendingSourceOutput(
        val fileIndex: Int,
        val authorizationKey: String,
        val authorizationLabel: String,
        val authorizationMode: AuthorizationMode,
        val fileName: String,
        val content: String,
        val relativeDirectoryPath: String?
    )

    private data class DirectoryImportResult(
        val files: List<SubtitleFile>,
        val skippedInvalidCount: Int
    )

    private data class DirectoryQueueEntry(
        val directory: DocumentFile,
        val relativePath: String
    )

    private enum class AuthorizationMode(val displayName: String) {
        SOURCE_DIRECTORY("來源目錄"),
        IMPORT_ROOT("匯入根目錄")
    }
}

