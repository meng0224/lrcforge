# 專案功能邏輯總覽

本文件依據目前專案中的實際程式碼整理，重點是描述「功能如何被觸發、如何流經主要元件、如何落地到儲存層，以及結果如何回到 UI」。  
主要依據如下：

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
- `app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`
- `app/src/main/java/com/example/lrcforge/util/*`
- `app/src/main/java/com/example/lrcforge/adapter/SubtitleFileAdapter.kt`
- `app/src/main/java/com/example/lrcforge/model/*`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/item_subtitle_file.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/example/lrcforge/*` 中與邏輯行為直接相關的測試

同步狀態：

- 本文件已針對 2026-06 的第一段 Phase 4 低風險抽離做局部同步。
- 匯入列表合併與回饋文案已由 `ImportSelectionCoordinator` 承接。
- 來源目錄 key / label / 相對路徑摘要等純邏輯已由 `SourceDirectoryPathHelper` 承接。
- 原文件目錄輸出時的待授權與待保存暫存狀態已由 `SourceSaveAuthorizationState` 承接。
- 詳細逐步流程仍保留原本結構，後續若繼續拆 ViewModel / UseCase / Repository，需再次同步。

本文件明確區分：

- 已由主 UI 流程實際使用的功能
- 已存在但目前未由主 UI 流程呼叫的 helper
- 只能靠靜態閱讀推測、尚未經實機驗證的流程

---

## 1. 專案架構摘要

### 1.1 主要架構模式

此專案目前是單一 `app` 模組的 Android 應用，採用「單 Activity 集中協調」的實作方式，而不是典型的 MVVM / Clean Architecture 分層。

目前實際存在的架構特徵：

- UI 入口只有 `MainActivity`
- 畫面採傳統 XML View，不是 Compose UI
- 狀態主要保存在 `MainActivity` 內的記憶體欄位、`SourceSaveAuthorizationState` 與 `SharedPreferences`
- 業務邏輯分散在 `converter` 與 `util` 下的 helper / policy 類別
- 儲存層使用兩條路徑：
  - SAF `DocumentFile` / `contentResolver`
  - app-specific downloads 或舊版 public downloads

目前**未使用**的典型 Android 架構元件：

- Fragment
- ViewModel
- LiveData / StateFlow / SharedFlow
- Compose UI
- Repository
- UseCase
- Service
- BroadcastReceiver
- WorkManager
- Retrofit / OkHttp / API 呼叫
- Room / SQLite / Database

### 1.2 核心模組與責任分工

| 模組/層級 | 主要元件 | 責任 |
| --- | --- | --- |
| UI 協調層 | `MainActivity` | 綁定 UI、接收使用者事件、啟動檔案選擇/目錄授權、驅動轉換、更新列表狀態與 Toast |
| UI 呈現層 | `activity_main.xml`、`item_subtitle_file.xml`、`SubtitleFileAdapter` | 顯示儲存模式、按鈕、進度列、檔案列表與每筆狀態 |
| 狀態模型 | `SubtitleFile`、`FileStatus`、`AppSettings` | 保存目前選入檔案、轉換狀態、輸出設定、來源目錄資訊 |
| 轉換邏輯 | `SubtitleConverter` | 依副檔名分派字幕解析流程，將字幕文本轉成 LRC |
| 輸入驗證/策略 | `FileValidator`、`FileSelectionPolicy`、`ImportSelectionCoordinator`、`OutputSettingsPolicy`、`FileListUiPolicy`、`FileNameHelper` | 驗證格式/大小、合併檔案列表與回饋文案、限制輸出模式互斥、控制 UI 可操作性、產生輸出檔名 |
| 來源目錄輔助 | `SourceDirectoryPathHelper`、`SourceSaveAuthorizationState` | 解析來源目錄 key / label / 相對路徑摘要，管理原文件目錄輸出的待授權與待保存狀態 |
| 儲存與設定 | `StorageHelper`、`SettingsManager` | 寫出 LRC、管理 SAF tree URI、保存使用者設定與來源目錄授權 |

### 1.3 UI、狀態管理、資料層、網路層、儲存層之間的關係

目前沒有獨立資料層或網路層。實際關係如下：

1. UI 事件由 `MainActivity` 接收。
2. `MainActivity` 直接操作記憶體中的 `files: MutableList<SubtitleFile>` 與 `settings: AppSettings`。
3. `MainActivity` 視情境呼叫：
   - `FileValidator` 驗證選入檔案
   - `ImportSelectionCoordinator` / `FileSelectionPolicy` 合併列表並產生回饋文案
   - `SubtitleConverter` 執行格式轉換
   - `FileNameHelper` 生成輸出檔名
   - `SourceDirectoryPathHelper` / `SourceSaveAuthorizationState` 處理來源目錄資訊與待授權狀態
   - `StorageHelper` 保存轉換結果
   - `SettingsManager` 讀寫設定與來源目錄授權
4. UI 更新主要透過：
   - `SubtitleFileAdapter.updateFile()`
   - `adapter.notifyDataSetChanged()`
   - `TextView` / `ProgressBar` 可見性與文字
   - `Toast`

資料實際流向大致為：

`UI 事件 -> MainActivity -> policy/helper/converter/storage -> MainActivity 更新 SubtitleFile/AppSettings -> RecyclerView / TextView / Toast`

---

## 2. 功能清單總表

| 功能名稱 | 入口頁/觸發點 | 主要 UI 元件 | 主要邏輯類別 | 主要資料來源 | 相關檔案路徑 |
| --- | --- | --- | --- | --- | --- |
| App 啟動與設定載入 | App 冷啟動 | `MainActivity`、`toolbar`、`tvOutputDir` | `MainActivity`、`SettingsManager` | `SharedPreferences` | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt` |
| 輸出模式切換與互斥控制 | 「輸出到原文件目錄」開關 | `switchOutputToSourceDirectory`、`btnSelectOutputDir`、`btnClearOutputDir` | `MainActivity`、`OutputSettingsPolicy` | `AppSettings`、`SharedPreferences` | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/OutputSettingsPolicy.kt` |
| 自訂輸出目錄選擇與清除 | 點「選擇目錄」/「清除目錄」 | `btnSelectOutputDir`、`btnClearOutputDir`、`tvOutputDir` | `MainActivity`、`SettingsManager` | SAF tree URI、`SharedPreferences` | `app/src/main/java/com/example/lrcforge/MainActivity.kt` |
| 字幕檔選取與權限分流 | 點「選擇文件」 | `btnSelectFiles` | `MainActivity` | `ActivityResultContracts.OpenMultipleDocuments`、Android 權限系統 | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/AndroidManifest.xml` |
| 檔案驗證與列表建立/合併 | 選檔回傳後 | `RecyclerView` | `MainActivity`、`FileValidator`、`ImportSelectionCoordinator`、`FileSelectionPolicy` | `Uri`、`OpenableColumns`、`DocumentsContract` | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/FileValidator.kt`、`app/src/main/java/com/example/lrcforge/util/ImportSelectionCoordinator.kt`、`app/src/main/java/com/example/lrcforge/util/FileSelectionPolicy.kt` |
| 批次字幕轉 LRC | 點「開始轉換」 | `btnConvert`、`progressBar`、`tvProgress` | `MainActivity`、`SubtitleConverter`、`FileNameHelper` | `SubtitleFile` 列表、檔案內容流 | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`、`app/src/main/java/com/example/lrcforge/util/FileNameHelper.kt` |
| 一般輸出流程 | 轉換成功後自動保存 | `Toast`、`tvOutputDir` | `MainActivity`、`StorageHelper` | `lrcContent`、`outputDirUri` 或預設下載目錄 | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/StorageHelper.kt` |
| 原文件目錄輸出流程 | 開啟原目錄模式後轉換成功 | `switchOutputToSourceDirectory`、目錄授權 picker、`Toast` | `MainActivity`、`SourceDirectoryPathHelper`、`SourceSaveAuthorizationState`、`StorageHelper`、`SettingsManager` | `sourceDirectoryKey`、SAF tree URI、`SharedPreferences` | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/SourceDirectoryPathHelper.kt`、`app/src/main/java/com/example/lrcforge/util/SourceSaveAuthorizationState.kt`、`app/src/main/java/com/example/lrcforge/util/StorageHelper.kt`、`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt` |
| 文件列表清除與 UI 重置 | 點「清除文件列表」 | `btnClearFileList`、`RecyclerView`、`progressBar`、`tvProgress` | `MainActivity`、`FileListUiPolicy`、`SourceSaveAuthorizationState` | `files`、來源目錄授權暫存狀態 | `app/src/main/java/com/example/lrcforge/MainActivity.kt`、`app/src/main/java/com/example/lrcforge/util/FileListUiPolicy.kt`、`app/src/main/java/com/example/lrcforge/util/SourceSaveAuthorizationState.kt` |
| 列表項狀態顯示規則 | RecyclerView 綁定 | `item_subtitle_file.xml` | `SubtitleFileAdapter`、`FileStatus` | `SubtitleFile.status`、`errorMessage`、`outputFileName` | `app/src/main/java/com/example/lrcforge/adapter/SubtitleFileAdapter.kt`、`app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt` |

---

## 3. 各功能詳細邏輯拆解

### App 啟動與設定載入

#### 3.1.1 功能用途

初始化主畫面、載入儲存設定、建立檔案列表 UI、綁定按鈕事件，讓 App 進入可操作狀態。

#### 3.1.2 UI 入口與觸發方式

- 使用者從 Launcher 啟動 App
- `AndroidManifest.xml` 中 `.MainActivity` 被宣告為 `MAIN` / `LAUNCHER`

#### 3.1.3 執行流程（一步一步）

1. 系統啟動 `MainActivity.onCreate()`
2. `setContentView(R.layout.activity_main)` 載入主畫面 XML
3. `initViews()` 綁定 `toolbar`、按鈕、進度條、RecyclerView、開關
4. `loadSettings()` 從 `SettingsManager.loadSettings()` 讀取 `SharedPreferences`
5. 若發現 `outputDirUri != null` 且 `outputToSourceDirectory == true`，會強制關閉原文件目錄模式並回存設定
6. `syncSourceDirectorySwitch()` 讓 switch 狀態與設定同步，並掛上切換事件
7. `updateOutputDirDisplay()` 顯示目前儲存位置
8. `setupRecyclerView()` 建立 `SubtitleFileAdapter`
9. `setupClickListeners()` 綁定選檔、轉換、選目錄、清除目錄、清除列表等按鈕
10. `updateClearFileListButtonState()` 根據目前列表是否為空控制按鈕可用性

流程箭頭：

`App 啟動 -> MainActivity.onCreate() -> 載入 View -> 載入 AppSettings -> 初始化列表與按鈕 -> UI 進入待命`

#### 3.1.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`onCreate()`、`initViews()`、`loadSettings()`、`syncSourceDirectorySwitch()`、`setupRecyclerView()`、`setupClickListeners()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：整個 App 的初始化與流程中樞
- 類別名：`SettingsManager`
  - 方法名：`loadSettings()`、`saveSettings()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
  - 角色：從 `SharedPreferences` 載入/保存輸出設定
- 類別名：`AppSettings`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/model/AppSettings.kt`
  - 角色：保存 `smartNaming`、`timePrecision`、輸出目錄與原目錄模式

#### 3.1.5 關鍵條件判斷與例外處理

- `loadSettings()` 會主動修正互斥衝突：
  - 若同時有 `outputDirUri` 與 `outputToSourceDirectory=true`
  - 會改成 `outputToSourceDirectory=false`
- 這表示程式層面只允許一種輸出策略為主，不允許自訂目錄與原目錄模式同時生效

#### 3.1.6 資料流與狀態流

- `SharedPreferences -> AppSettings -> MainActivity.settings -> UI switch / output label`
- `files` 初始為空列表
- `pendingSourceAuthorizationKeys` 等暫存佇列初始為空，用於之後原目錄輸出授權流程

#### 3.1.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `onCreate`
  - `loadSettings`
  - `syncSourceDirectorySwitch`
  - `setupRecyclerView`
  - `setupClickListeners`
- `app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
  - `loadSettings`
  - `fromStoredValues`
- `app/src/main/AndroidManifest.xml`

---

### 輸出模式切換與互斥控制

#### 3.2.1 功能用途

控制目前輸出策略是：

- 預設應用下載目錄
- 自訂 SAF 目錄
- 原文件目錄

並確保「自訂輸出目錄」與「原文件目錄模式」不能同時啟用。

#### 3.2.2 UI 入口與觸發方式

- UI 元件：`switchOutputToSourceDirectory`
- UI 位置：`activity_main.xml`
- 使用者手動切換「輸出到原文件目錄」

#### 3.2.3 執行流程（一步一步）

1. `loadSettings()` 或 `syncSourceDirectorySwitch()` 先把 switch 與目前設定同步
2. 使用者切換 switch
3. `setOnCheckedChangeListener` 觸發 `handleSourceDirectoryToggle(isChecked)`
4. `handleSourceDirectoryToggle()` 呼叫 `OutputSettingsPolicy.canEnableSourceDirectoryOutput(settings)`
5. 如果目前已有 `outputDirUri`，則阻止切換、顯示 Toast，並重新同步 switch
6. 若可啟用，更新 `settings.outputToSourceDirectory`
7. 呼叫 `SettingsManager.saveSettings()`
8. 再次 `syncSourceDirectorySwitch()` 與 `updateOutputDirDisplay()`
9. UI 顯示新的儲存模式文字

流程箭頭：

`Switch 事件 -> MainActivity.handleSourceDirectoryToggle() -> OutputSettingsPolicy -> SettingsManager.saveSettings() -> updateOutputDirDisplay() -> UI 顯示新模式`

#### 3.2.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`syncSourceDirectorySwitch()`、`handleSourceDirectoryToggle()`、`updateOutputDirDisplay()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：接收 UI 事件並更新設定與顯示
- 類別名：`OutputSettingsPolicy`
  - 方法名：`canEnableSourceDirectoryOutput()`、`canSelectCustomOutputDirectory()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/OutputSettingsPolicy.kt`
  - 角色：封裝輸出模式互斥規則
- 類別名：`SettingsManager`
  - 方法名：`saveSettings()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
  - 角色：將切換結果持久化

#### 3.2.5 關鍵條件判斷與例外處理

- 不能在已有自訂輸出目錄時再啟用原文件目錄模式
- 對應規則：
  - `canEnableSourceDirectoryOutput(settings)` 要求 `settings.outputDirUri == null`
  - `canSelectCustomOutputDirectory(settings)` 要求 `!settings.outputToSourceDirectory`

#### 3.2.6 資料流與狀態流

- `Switch checked state -> settings.outputToSourceDirectory -> SharedPreferences -> tvOutputDir`
- 此流程不直接處理檔案資料，只影響後續轉換結果的輸出路徑策略

#### 3.2.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `syncSourceDirectorySwitch`
  - `handleSourceDirectoryToggle`
  - `updateOutputDirDisplay`
- `app/src/main/java/com/example/lrcforge/util/OutputSettingsPolicy.kt`

---

### 自訂輸出目錄選擇與清除

#### 3.3.1 功能用途

允許使用者透過 SAF 選擇一個固定輸出目錄，讓轉換成功的 `.lrc` 結果直接寫入該目錄。

#### 3.3.2 UI 入口與觸發方式

- 點擊 `btnSelectOutputDir`
- 點擊 `btnClearOutputDir`

#### 3.3.3 執行流程（一步一步）

選擇目錄流程：

1. 使用者點 `btnSelectOutputDir`
2. `setupClickListeners()` 中檢查 `OutputSettingsPolicy.canSelectCustomOutputDirectory(settings)`
3. 若原文件目錄模式已開啟，顯示 Toast 並中止
4. 否則呼叫 `directoryPickerLauncher.launch(null)`
5. `ActivityResultContracts.OpenDocumentTree()` 回傳 `uri`
6. 若目前不是來源目錄授權流程，則進入 `handleDirectorySelection(uri)`
7. `takePersistableUriPermission()` 保存讀寫授權
8. `settings.outputDirUri = uri.toString()`
9. `SettingsManager.saveSettings()` 回存
10. `updateOutputDirDisplay()` 更新畫面
11. 顯示「已設定自訂輸出資料夾」

清除目錄流程：

1. 使用者點 `btnClearOutputDir`
2. `clearCustomOutputDirectory()` 檢查 `settings.outputDirUri`
3. 若目前沒有自訂目錄，顯示 Toast
4. 否則清空 `settings.outputDirUri`
5. `SettingsManager.saveSettings()`
6. `updateOutputDirDisplay()`
7. 顯示「已清除自訂輸出資料夾」

#### 3.3.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`handleDirectorySelection()`、`clearCustomOutputDirectory()`、`updateOutputDirDisplay()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：處理 SAF 回傳與 UI 顯示
- 類別名：`SettingsManager`
  - 方法名：`saveSettings()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
  - 角色：保存輸出目錄 URI

#### 3.3.5 關鍵條件判斷與例外處理

- 若原文件目錄模式啟用中，禁止選擇自訂輸出目錄
- 若 `uri == null`，目前不進入 `handleDirectorySelection()`，等同使用者取消
- `takePersistableUriPermission()` 是之後跨次啟動重用目錄授權的關鍵

#### 3.3.6 資料流與狀態流

- `OpenDocumentTree result Uri -> settings.outputDirUri -> SharedPreferences`
- `settings.outputDirUri -> updateOutputDirDisplay() -> tvOutputDir`

#### 3.3.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `setupClickListeners`
  - `handleDirectorySelection`
  - `clearCustomOutputDirectory`
  - `updateOutputDirDisplay`

---

### 字幕檔選取與 Android 版本權限分流

#### 3.4.1 功能用途

讓使用者從系統文件選擇器選入多個字幕檔，並處理 Android 版本差異下的讀取權限行為。

#### 3.4.2 UI 入口與觸發方式

- 點擊 `btnSelectFiles`

#### 3.4.3 執行流程（一步一步）

1. 使用者點 `btnSelectFiles`
2. 執行 `checkAndRequestImportPermission()`
3. 若 `SDK >= Android 11 (R)`，直接呼叫 `openFilePicker()`
4. 若 `SDK < Android 11`，檢查 `READ_EXTERNAL_STORAGE`
5. 若尚未授權，透過 `permissionLauncher` 發出權限請求
6. 權限 callback 中：
   - 若全部授權成功 -> `openFilePicker()`
   - 否則顯示「未授予讀取外部儲存的權限」
7. `openFilePicker()` 呼叫 `filePickerLauncher.launch(arrayOf("*/*"))`
8. `OpenMultipleDocuments()` 回傳 `uris`
9. 若 `uris` 非空，進入 `handleSelectedFiles(uris)`

流程箭頭：

`按下選擇文件 -> 權限判斷 -> OpenMultipleDocuments -> 回傳 Uri 列表 -> handleSelectedFiles()`

#### 3.4.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`checkAndRequestImportPermission()`、`openFilePicker()`、`handleSelectedFiles()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：處理版本分流、權限與檔案選擇 callback
- 元件名：`ActivityResultContracts.OpenMultipleDocuments`
  - 角色：系統文件選擇器，多選檔案
- 元件名：`ActivityResultContracts.RequestMultiplePermissions`
  - 角色：Android 10 以下讀取權限請求

#### 3.4.5 關鍵條件判斷與例外處理

- Android 11+ 不走舊式儲存權限流程，直接透過文件選擇器取檔
- Android 10 以下若沒授權，會中止選檔
- `openFilePicker()` 包 `try/catch`，若系統文件選擇器打不開會顯示錯誤訊息
- MIME type 目前傳 `*/*`，不會在 picker 端預先只顯示字幕副檔名

#### 3.4.6 資料流與狀態流

- `btnSelectFiles click -> permission state -> filePickerLauncher result -> List<Uri>`
- 真正的檔案 metadata 讀取與狀態建立在 `handleSelectedFiles()` 中才發生

#### 3.4.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `filePickerLauncher`
  - `permissionLauncher`
  - `checkAndRequestImportPermission`
  - `openFilePicker`
- `app/src/main/AndroidManifest.xml`

---

### 檔案驗證與列表建立/合併

#### 3.5.1 功能用途

把系統文件選擇器回傳的 `Uri` 轉成 App 內部的 `SubtitleFile` 物件，標記是否合法、是否可轉換，以及在原文件目錄模式下決定要追加還是覆蓋清單。

#### 3.5.2 UI 入口與觸發方式

- 由「選擇文件」流程的 callback 觸發
- 使用者不直接點擊此功能，但它是選檔後的主處理流程

#### 3.5.3 執行流程（一步一步）

1. `handleSelectedFiles(uris)` 在 `Dispatchers.IO` 執行
2. 對每個 `uri`：
   - `getFileName(uri)` 透過 `OpenableColumns.DISPLAY_NAME` 取檔名
   - `getFileSize(uri)` 透過 `OpenableColumns.SIZE` 取大小
   - `resolveSourceDirectoryInfo(uri)` 嘗試解析來源目錄 key/label
   - `FileValidator.validateFile(fileName, fileSize)` 驗證格式與大小
   - 建立 `SubtitleFile`
3. 驗證成功則狀態為 `PENDING`
4. 驗證失敗則狀態為 `INVALID`，並附上 `errorMessage`
5. 切回 `Dispatchers.Main`
6. 呼叫 `FileSelectionPolicy.mergeSelections()`
   - 若 `settings.outputToSourceDirectory = false`，直接覆蓋列表
   - 若 `true`，採追加並用 `Uri` 去重
7. `files.clear()` 後重新加入結果
8. `adapter.notifyDataSetChanged()`
9. `updateClearFileListButtonState()`
10. 顯示已選擇/已新增/略過重複檔案的 Toast

流程箭頭：

`Uri 清單 -> 讀 metadata -> FileValidator -> 建立 SubtitleFile -> FileSelectionPolicy.mergeSelections() -> 更新 files -> RecyclerView 刷新`

#### 3.5.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`handleSelectedFiles()`、`getFileName()`、`getFileSize()`、`resolveSourceDirectoryInfo()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：將外部文件描述轉成內部模型，並回寫 UI
- 類別名：`FileValidator`
  - 方法名：`validateFile()`、`validateFormat()`、`validateSize()`、`getExtension()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/FileValidator.kt`
  - 角色：驗證副檔名與大小上限 10MB
- 類別名：`FileSelectionPolicy`
  - 方法名：`mergeSelections()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/FileSelectionPolicy.kt`
  - 角色：控制覆蓋或追加清單，以及重複 `Uri` 去重
- 類別名：`SubtitleFile`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`
  - 角色：列表內每筆檔案的狀態與中繼資料

#### 3.5.5 關鍵條件判斷與例外處理

- 支援格式：
  - `.vtt`
  - `.ass`
  - `.ssa`
  - `.srt`
  - `.str`
  - `.smi`
  - `.sub`
- 大小限制：`10MB`
- 原文件目錄模式下：
  - 第二次選檔會追加，不覆蓋
  - 重複判斷依據是 `Uri`
  - 相同檔名但不同 `Uri` 仍可同時存在
- `handleSelectedFiles()` 中若單一檔案處理丟出例外，目前會直接略過該檔案，不額外顯示錯誤

#### 3.5.6 資料流與狀態流

- `Uri -> fileName/fileSize/sourceDirectoryInfo -> SubtitleFile`
- 狀態流：
  - 驗證通過：`PENDING`
  - 驗證失敗：`INVALID`
- `sourceDirectoryKey` / `sourceDirectoryLabel` 只在能從 `DocumentsContract` 推導來源目錄時才有值

#### 3.5.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `handleSelectedFiles`
  - `getFileName`
  - `getFileSize`
  - `resolveSourceDirectoryInfo`
  - `extractParentDocumentId`
  - `extractSourceDirectoryLabel`
- `app/src/main/java/com/example/lrcforge/util/FileValidator.kt`
- `app/src/main/java/com/example/lrcforge/util/FileSelectionPolicy.kt`
- `app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`

---

### 批次轉換字幕為 LRC

#### 3.6.1 功能用途

把列表中可轉換的字幕檔逐筆讀取、解析並轉成 LRC，更新每筆狀態與輸出檔名，最後觸發保存流程。

#### 3.6.2 UI 入口與觸發方式

- 點擊 `btnConvert`

#### 3.6.3 執行流程（一步一步）

1. 使用者點 `btnConvert`
2. 若 `files.isEmpty()`，顯示「請先選擇文件」並中止
3. 執行 `startConversion()`
4. `SettingsManager.loadSettings()` 重新讀入設定，避免使用舊值
5. 顯示進度條與進度文字
6. 篩出 `files.filter { it.status.isEligibleForConversion() }`
7. 若沒有可轉換檔案，顯示「沒有可轉換的文件」，隱藏進度 UI 並中止
8. 在 `Dispatchers.IO` 中建立 `SubtitleConverter`
9. 逐檔處理：
   - 找出 `fileIndex`
   - 主執行緒把狀態改成 `PROCESSING`
   - 呼叫 `converter.convertToLrc(file.uri, file.fileName)`
   - 若回傳內容非空：
     - 用 `FileNameHelper.smartNaming()` 產出輸出檔名
     - 把狀態改成 `SUCCESS`
     - 寫入 `outputFileName`、`lrcContent`
   - 若內容為空或發生例外：
     - 狀態改成 `ERROR`
     - `errorMessage = "轉換失敗: ..."`
   - 更新進度百分比
10. 全部處理後，隱藏進度 UI
11. 若至少一筆 `SUCCESS`，呼叫 `downloadAllFiles()`
12. 顯示總結 Toast

流程箭頭：

`開始轉換 -> 篩選可處理檔案 -> SubtitleConverter.convertToLrc() -> FileNameHelper.smartNaming() -> 更新 SubtitleFile 狀態 -> downloadAllFiles()`

#### 3.6.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`startConversion()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：批次處理總控、進度更新、結果彙整
- 類別名：`SubtitleConverter`
  - 方法名：`convertToLrc()`、`convertContentToLrc()`、各格式轉換方法
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`
  - 角色：依副檔名解析字幕文本並產生 LRC
- 類別名：`FileNameHelper`
  - 方法名：`smartNaming()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/FileNameHelper.kt`
  - 角色：將原始檔名轉成輸出 `.lrc` 名稱
- 類別名：`FileStatus`
  - 方法名：`isEligibleForConversion()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`
  - 角色：決定哪些狀態可再次進入轉換

#### 3.6.5 關鍵條件判斷與例外處理

- 只有 `PENDING` 與 `ERROR` 會進入 `isEligibleForConversion()`
- `INVALID` 不會轉換
- `SUCCESS` 也不會再次轉換
- `convertToLrc()` 若發生例外會回 `null`
- `MainActivity` 進一步把 `null` 或空字串視為失敗
- `smartNaming` 與 `timePrecision` 目前由 `AppSettings` 控制，但實際上 `SettingsManager.fromStoredValues()` 固定回傳 `true`

#### 3.6.6 資料流與狀態流

- `SubtitleFile(uri,fileName) -> SubtitleConverter -> lrcContent`
- `FileStatus` 流轉：
  - `PENDING -> PROCESSING -> SUCCESS`
  - `PENDING -> PROCESSING -> ERROR`
  - `ERROR -> PROCESSING -> SUCCESS/ERROR`
- 成功後資料暫存在記憶體：
  - `outputFileName`
  - `lrcContent`

#### 3.6.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `startConversion`
- `app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`
  - `convertToLrc`
  - `convertContentToLrc`
  - `convertVttToLrc`
  - `convertSrtToLrc`
  - `convertAssToLrc`
  - `convertSmiToLrc`
  - `convertSubToLrc`
- `app/src/main/java/com/example/lrcforge/util/FileNameHelper.kt`
- `app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`

---

### 字幕格式解析與 LRC 生成

#### 3.7.1 功能用途

依字幕檔副檔名選擇對應 parser，將不同字幕格式整理成 `"[mm:ss.xx]歌詞內容"` 的 LRC 行格式。

#### 3.7.2 UI 入口與觸發方式

- 不直接由 UI 單獨觸發
- 由 `startConversion()` 內部呼叫 `SubtitleConverter.convertToLrc()`

#### 3.7.3 執行流程（一步一步）

1. `convertToLrc(uri, fileName)` 先讀入整個檔案內容
2. `convertContentToLrc(content, fileName)` 用 `FileValidator.getExtension(fileName)` 判斷副檔名
3. 依副檔名分派：
   - `vtt -> convertVttToLrc`
   - `srt -> convertSrtToLrc`
   - `ass` / `ssa -> convertAssToLrc`
   - `smi -> convertSmiToLrc`
   - `sub` / `str -> convertSubToLrc`
4. 每種 parser 會抽取起始時間與文字
5. 文字會透過 `cleanText()` 或 `cleanAssText()` 去標記與壓縮空白
6. 時間會透過 `formatTimeToLrc()` 或 `formatTimeFromMilliseconds()` 轉成 LRC 時間格式
7. 回傳整份 LRC 字串給 `MainActivity`

流程箭頭：

`Uri -> readFileContent() -> convertContentToLrc() -> 格式專用 parser -> cleanText / formatTime -> LRC 字串`

#### 3.7.4 涉及的核心元件

- 類別名：`SubtitleConverter`
  - 方法名：`readFileContent()`、`convertContentToLrc()`、各 parser、`cleanText()`、`cleanAssText()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`
  - 角色：字幕解析核心
- 類別名：`FileValidator`
  - 方法名：`getExtension()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/FileValidator.kt`
  - 角色：提供副檔名判斷

#### 3.7.5 關鍵條件判斷與例外處理

- `ASS/SSA`
  - 會先找 `[Events]`
  - 讀取 `Format:` 解析 `Text` 欄位 index
  - 若沒有 `Format:`，使用 fallback index `9`
  - `Dialogue:` 以 `limit = resolvedTextColumnIndex + 1` split，避免含逗號文本被截斷
- `VTT/SRT`
  - 找含 `"-->"` 的時間行
  - 收集後續直到空白行的文本並合併
- `SMI`
  - 以 `<SYNC Start=...>` 當時間點
- `SUB/STR`
  - 目前以 `\{start\}\{end\}text` 規則解析
  - 依程式行為直接把第一個數字當毫秒值處理
  - 是否完全符合所有 `.sub/.str` 變體格式，靜態閱讀下無法完全確認
- `timePrecision = false` 時會把小數統一輸出為 `.00`

#### 3.7.6 資料流與狀態流

- 輸入：原始字幕全文字串
- 輸出：LRC 全文字串
- 中間不使用資料庫或遠端 API

#### 3.7.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/converter/SubtitleConverter.kt`
- `app/src/test/java/com/example/lrcforge/converter/SubtitleConverterTest.kt`

---

### 一般輸出流程（預設下載目錄 / 自訂 SAF 目錄）

#### 3.8.1 功能用途

將成功轉換的 LRC 結果保存到：

- 自訂 SAF 輸出目錄，或
- 預設下載目錄

#### 3.8.2 UI 入口與觸發方式

- 不直接由額外按鈕觸發
- 在 `startConversion()` 完成且有成功檔案後，自動進入 `downloadAllFiles()`

#### 3.8.3 執行流程（一步一步）

1. `downloadAllFiles()` 篩出：
   - `status == SUCCESS`
   - `lrcContent != null`
   - `outputFileName != null`
2. 若沒有可輸出檔案，顯示 Toast 並中止
3. 若 `settings.outputToSourceDirectory == true`，改走原文件目錄流程
4. 否則：
   - 建立 `(outputFileName, lrcContent)` 列表
   - `settings.outputDirUri?.let(Uri::parse)` 取得自訂目錄 URI
   - 呼叫 `StorageHelper.saveMultipleFiles()`
5. `StorageHelper.saveMultipleFiles()`：
   - 若有 `outputDirUri`，走 `saveContentToUri()`
   - 若沒有，寫入 `getDownloadDirectory(context)`
6. 回主執行緒顯示「已保存 X 個文件」

流程箭頭：

`SUCCESS 檔案 -> downloadAllFiles() -> StorageHelper.saveMultipleFiles() -> SAF 或下載目錄 -> Toast`

#### 3.8.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`downloadAllFiles()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：彙整成功檔案並決定保存策略
- 類別名：`StorageHelper`
  - 方法名：`saveMultipleFiles()`、`saveContentToUri()`、`getDownloadDirectory()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/StorageHelper.kt`
  - 角色：實際寫檔

#### 3.8.5 關鍵條件判斷與例外處理

- 若有自訂輸出目錄 URI，走 SAF
- 若沒有，走本地下載目錄
- Android Q 以上用 `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`
- Android Q 以下用 `Environment.getExternalStoragePublicDirectory()`
- 單一檔案寫入失敗時：
  - SAF 路徑會以 `null` 表示該檔案失敗
  - 預設下載目錄則在每筆 `try/catch` 中略過失敗檔案並繼續

#### 3.8.6 資料流與狀態流

- `SubtitleFile.lrcContent + outputFileName -> Pair<String, String> -> StorageHelper`
- 這條流程不回寫每筆狀態為「已保存成功」，只在結尾以 Toast 告知保存數量

#### 3.8.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `downloadAllFiles`
- `app/src/main/java/com/example/lrcforge/util/StorageHelper.kt`
  - `saveMultipleFiles`
  - `saveContentToUri`
  - `getDownloadDirectory`

---

### 原文件目錄輸出流程（逐來源目錄授權、授權重用、失敗回寫）

#### 3.9.1 功能用途

在「輸出到原文件目錄」模式下，把每筆成功轉換的結果寫回來源字幕檔所在資料夾。若對該來源資料夾還沒有授權，會要求使用者逐個來源目錄授權，並把授權保存以供下次重用。

#### 3.9.2 UI 入口與觸發方式

- 先由使用者啟用 `switchOutputToSourceDirectory`
- 再選檔並開始轉換
- 若某來源目錄尚未授權，系統會跳出 `OpenDocumentTree` 讓使用者選目錄

#### 3.9.3 執行流程（一步一步）

1. `downloadAllFiles()` 發現 `settings.outputToSourceDirectory == true`
2. 呼叫 `downloadAllFilesToSourceDirectories(successFiles)`
3. `resetPendingSourceSaveState()` 清空上一輪暫存
4. 逐筆成功檔案：
   - 以 `files.indexOf(file)` 找到原列表位置
   - 讀取 `file.sourceDirectoryKey`
   - 若 `sourceDirectoryKey == null`：
     - 記錄到 `pendingSourceSaveFailures[fileIndex]`
   - 否則從 `SettingsManager.getSourceDirectoryUri()` 讀取既有授權
   - 若已保存的 `treeUri` 存在且 `matchesSourceDirectoryKey()` 通過：
     - 直接放入 `pendingSourceReadyTargets`
   - 否則放入 `pendingSourceOutputs`
     - 同時記錄 `pendingSourceAuthorizationLabels`
5. 若沒有任何待授權輸出，直接 `savePendingSourceTargets()`
6. 若有待授權輸出：
   - 取出所有不同 `sourceDirectoryKey`
   - 放入 `pendingSourceAuthorizationKeys` 佇列
   - 呼叫 `requestNextSourceDirectoryAuthorization()`
7. `requestNextSourceDirectoryAuthorization()`：
   - 取下一個 key
   - 顯示「請授權來源目錄：...」
   - 啟動 `directoryPickerLauncher.launch(null)`
8. `OpenDocumentTree` 回傳後進入 `handleSourceDirectoryAuthorizationResult()`
9. 若 `treeUri == null`：
   - 將該來源的所有 pending output 標成失敗
   - 繼續下一個來源目錄授權
10. 若 `matchesSourceDirectoryKey(treeUri, sourceDirectoryKey)` 不成立：
   - 顯示「選取的目錄與來源目錄不符，請重新選擇」
   - 再次要求選目錄
11. 若匹配成功：
   - `takePersistableUriPermission()`
   - `SettingsManager.saveSourceDirectoryUri()`
   - `movePendingOutputsToReadyTargets()`
   - 繼續下一個授權
12. 所有授權處理完成後，`savePendingSourceTargets()`
13. `StorageHelper.saveOutputTargets()` 實際寫檔
14. `applySaveResults()`：
   - 將初始失敗項目更新為 `ERROR`
   - 將寫入失敗項目更新為 `ERROR`
   - 顯示「已保存 X 個文件，失敗 Y 個」或「已保存 X 個文件」
15. `resetPendingSourceSaveState()` 清空本輪暫存

流程箭頭：

`SUCCESS 檔案 -> 依 sourceDirectoryKey 分組 -> 檢查既有授權 -> 逐來源請求 SAF 授權 -> StorageHelper.saveOutputTargets() -> applySaveResults() -> RecyclerView/Toast`

#### 3.9.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：
    - `downloadAllFilesToSourceDirectories()`
    - `requestNextSourceDirectoryAuthorization()`
    - `handleSourceDirectoryAuthorizationResult()`
    - `movePendingOutputsToReadyTargets()`
    - `markPendingOutputsForSourceDirectory()`
    - `savePendingSourceTargets()`
    - `applySaveResults()`
    - `resetPendingSourceSaveState()`
    - `matchesSourceDirectoryKey()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：原目錄模式的整體授權佇列與保存協調器
- 類別名：`SettingsManager`
  - 方法名：`getSourceDirectoryUri()`、`saveSourceDirectoryUri()`、`sourceDirectoryPreferenceKey()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
  - 角色：保存每個來源目錄對應的 tree URI
- 類別名：`StorageHelper`
  - 方法名：`saveOutputTargets()`、`countSuccessfulOutputResults()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/StorageHelper.kt`
  - 角色：對多個不同 SAF 目錄執行批次保存
- 資料結構：`OutputTarget`、`OutputResult`、`PendingSourceOutput`
  - 角色：描述待保存任務與保存結果

#### 3.9.5 關鍵條件判斷與例外處理

- 必須先能從選入文件推導出 `sourceDirectoryKey`
- 來源目錄比對規則不是比檔名，也不是比完整 `Uri`
- 比對核心是：
  - `authority`
  - `DocumentsContract.getTreeDocumentId(treeUri)`
- 若使用者取消某來源目錄授權：
  - 該來源底下所有 pending output 會被標為失敗
  - 其他來源目錄仍繼續處理
- 若使用者選錯目錄：
  - 會重複要求選擇，不直接當作失敗
- 儲存失敗後：
  - 相關檔案狀態會從 `SUCCESS` 改成 `ERROR`
  - `errorMessage` 會寫成保存失敗訊息

#### 3.9.6 資料流與狀態流

- `SubtitleFile.sourceDirectoryKey/sourceDirectoryLabel`
  -> `pendingSourceOutputs`
  -> 取得或請求授權
  -> `pendingSourceReadyTargets`
  -> `StorageHelper.saveOutputTargets()`
  -> `OutputResult`
  -> `applySaveResults()` 回寫 `files[fileIndex].status`

狀態補充：

- 檔案可能先轉換成功成為 `SUCCESS`
- 之後若保存到原目錄失敗，會被回寫為 `ERROR`
- 因此「轉換成功」不等於「最終寫入成功」

#### 3.9.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `downloadAllFilesToSourceDirectories`
  - `requestNextSourceDirectoryAuthorization`
  - `handleSourceDirectoryAuthorizationResult`
  - `movePendingOutputsToReadyTargets`
  - `markPendingOutputsForSourceDirectory`
  - `savePendingSourceTargets`
  - `applySaveResults`
  - `resetPendingSourceSaveState`
  - `matchesSourceDirectoryKey`
- `app/src/main/java/com/example/lrcforge/util/SettingsManager.kt`
- `app/src/main/java/com/example/lrcforge/util/StorageHelper.kt`

---

### 文件列表清除與 UI 狀態重置

#### 3.10.1 功能用途

清空目前已選檔案、重設原目錄保存相關暫存、清除進度顯示，讓畫面回到乾淨狀態。

#### 3.10.2 UI 入口與觸發方式

- 點擊 `btnClearFileList`

#### 3.10.3 執行流程（一步一步）

1. 使用者點 `btnClearFileList`
2. 執行 `clearFileList()`
3. 若 `files.isEmpty()`：
   - 只更新按鈕狀態後返回
4. 若有資料：
   - `files.clear()`
   - `adapter.notifyDataSetChanged()`
   - `resetPendingSourceSaveState()`
   - `progressBar.progress = 0`
   - 隱藏 `progressBar`
   - `tvProgress.text = "進度: 0%"`
   - 隱藏 `tvProgress`
   - `updateClearFileListButtonState()`
   - 顯示「已清除文件列表」

#### 3.10.4 涉及的核心元件

- 類別名：`MainActivity`
  - 方法名：`clearFileList()`、`resetPendingSourceSaveState()`、`updateClearFileListButtonState()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - 角色：重置畫面與暫存狀態
- 類別名：`FileListUiPolicy`
  - 方法名：`canClearFileList()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/util/FileListUiPolicy.kt`
  - 角色：控制清除按鈕是否可用

#### 3.10.5 關鍵條件判斷與例外處理

- 空列表時不做清除動作，只同步 UI 按鈕狀態
- 除了清空 `files`，還會同步清除原目錄模式的 pending 授權/待保存狀態

#### 3.10.6 資料流與狀態流

- `files -> empty`
- `pendingSourceAuthorizationKeys/pendingSourceOutputs/... -> empty`
- `progress UI -> 歸零並隱藏`

#### 3.10.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/MainActivity.kt`
  - `clearFileList`
  - `updateClearFileListButtonState`
  - `resetPendingSourceSaveState`
- `app/src/main/java/com/example/lrcforge/util/FileListUiPolicy.kt`

---

### 列表項狀態顯示規則

#### 3.11.1 功能用途

依每筆 `SubtitleFile` 的狀態，決定 RecyclerView 卡片中應顯示的標籤、背景、輸出檔名與錯誤訊息。

#### 3.11.2 UI 入口與觸發方式

- RecyclerView 綁定資料時自動觸發
- 每次 `notifyDataSetChanged()` 或 `updateFile()` 後會重新綁定

#### 3.11.3 執行流程（一步一步）

1. `RecyclerView` 請求綁定列表項目
2. `SubtitleFileAdapter.onBindViewHolder()` 讀取 `files[position]`
3. 顯示 `tvFileName`
4. 依 `FileStatus` 分支：
   - `PENDING`
   - `INVALID`
   - `PROCESSING`
   - `SUCCESS`
   - `ERROR`
5. 設定：
   - 狀態文字
   - 狀態背景資源
   - 狀態文字顏色
   - 是否顯示 `tvOutputFileName`
   - 是否顯示 `tvErrorMessage`
6. `updateFile(position, file)` 會直接替換列表資料後 `notifyItemChanged(position)`

#### 3.11.4 涉及的核心元件

- 類別名：`SubtitleFileAdapter`
  - 方法名：`onBindViewHolder()`、`updateFile()`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/adapter/SubtitleFileAdapter.kt`
  - 角色：將資料模型映射到列表卡片 UI
- 類別名：`SubtitleFile`
  - 檔案路徑：`app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`
  - 角色：提供狀態、輸出檔名、錯誤內容
- XML：`item_subtitle_file.xml`
  - 角色：定義列表項 UI 結構

#### 3.11.5 關鍵條件判斷與例外處理

- `SUCCESS` 才顯示輸出檔名
- `INVALID` 與 `ERROR` 會顯示錯誤訊息
- `PENDING`、`PROCESSING` 會隱藏錯誤訊息與輸出檔名

#### 3.11.6 資料流與狀態流

- `SubtitleFile.status/errorMessage/outputFileName -> Adapter -> TextView visibility/text/style`

#### 3.11.7 相關代碼位置

- `app/src/main/java/com/example/lrcforge/adapter/SubtitleFileAdapter.kt`
- `app/src/main/java/com/example/lrcforge/model/SubtitleFile.kt`
- `app/src/main/res/layout/item_subtitle_file.xml`

---

## 4. 功能之間的串接關係

### 4.1 哪些功能會互相呼叫

- App 啟動流程會先決定輸出模式 UI 初始狀態，影響後續選目錄與選檔行為
- 選檔流程會產生 `SubtitleFile` 列表，這是轉換流程的輸入
- 轉換流程成功後必定進入保存流程
- 保存流程會依 `AppSettings` 決定是一般輸出還是原文件目錄輸出
- 原文件目錄輸出流程依賴選檔時是否成功解析 `sourceDirectoryKey`
- 清除文件列表會清掉原文件目錄輸出的 pending 狀態，因此與原目錄流程直接相關

### 4.2 哪些共用元件被多處使用

- `MainActivity`
  - 幾乎所有功能都由它協調
- `SettingsManager`
  - 啟動載入設定
  - 切換輸出模式
  - 選/清除自訂輸出目錄
  - 保存來源目錄授權
- `StorageHelper`
  - 一般輸出使用 `saveMultipleFiles()`
  - 原目錄輸出使用 `saveOutputTargets()`
- `SubtitleFile`
  - 選檔建立
  - 轉換更新狀態
  - 保存失敗回寫狀態
  - RecyclerView 顯示

### 4.3 哪些功能依賴相同資料源或狀態

- `AppSettings`
  - 被輸出模式控制、自訂目錄選擇、轉換後保存共同依賴
- `files`
  - 被選檔、清單顯示、轉換、保存失敗回寫、清除列表共同依賴
- `SharedPreferences`
  - 同時保存一般輸出設定與原目錄授權
- SAF `DocumentFile` / `treeUri`
  - 被自訂輸出目錄與原目錄輸出兩條流程共用

### 4.4 結構特徵補充

- 目前 `MainActivity` 同時扮演：
  - View controller
  - 狀態管理者
  - 流程協調器
  - 保存流程編排者
- 第一段 Phase 4 抽離後，匯入合併文案、來源目錄路徑解析、來源目錄待授權狀態已移到 util helper。
- 這代表文件中提到的多數主要功能仍不是經過 `ViewModel -> UseCase -> Repository`，而是由 `MainActivity -> util/helper` 協作完成。

---

## 5. 核心流程總結

### 5.1 App 啟動流程

`系統啟動 MainActivity -> onCreate() -> initViews() -> loadSettings() -> syncSourceDirectorySwitch() -> updateOutputDirDisplay() -> setupRecyclerView() -> setupClickListeners() -> 畫面待命`

### 5.2 選檔與驗證流程

`點選擇文件 -> checkAndRequestImportPermission() -> OpenMultipleDocuments -> handleSelectedFiles() -> getFileName/getFileSize -> FileValidator.validateFile() -> 建立 SubtitleFile -> ImportSelectionCoordinator / FileSelectionPolicy 合併列表與產生回饋文案 -> RecyclerView 更新`

### 5.3 批次轉換流程

`點開始轉換 -> startConversion() -> 篩選可處理檔案 -> SubtitleConverter.convertToLrc() -> FileNameHelper.smartNaming() -> 狀態更新為 SUCCESS/ERROR -> 更新進度 -> downloadAllFiles()`

### 5.4 一般輸出流程

`downloadAllFiles() -> successFiles -> StorageHelper.saveMultipleFiles() -> 自訂 SAF 目錄 或 預設下載目錄 -> Toast 顯示保存數`

### 5.5 原文件目錄輸出與授權重用流程

`downloadAllFilesToSourceDirectories() -> 檢查 sourceDirectoryKey / importRootDirectoryKey -> SourceSaveAuthorizationState 區分 ready targets 與待授權輸出 -> requestNextAuthorization() -> 使用者選對應目錄 -> saveSourceDirectoryUri() / saveImportRootDirectoryUri() -> savePendingSourceTargets() -> applySaveResults() -> 成功或錯誤回寫到列表`

### 5.6 實際產品主線

若把這個 App 當成完整產品流程，最核心的主線是：

`設定輸出策略 -> 選入字幕檔 -> 驗證檔案合法性 -> 批次轉換為 LRC -> 自動寫入目標位置 -> 回到列表/Toast 呈現結果`

---

## 6. 待確認區域

以下區域無法只靠靜態閱讀完全確認，或已知目前未被主流程使用：

### 6.1 原文件目錄授權與回寫流程未完全確認

- 程式碼已完整實作逐來源目錄授權與授權重用流程
- 但 `DEVELOPMENT_ROADMAP.md` 明確提到此功能尚未完成實機驗證
- 因此下列描述屬於「根據程式碼可推斷，但未完全確認」：
  - 多來源資料夾連續授權體驗
  - 清除列表後是否在實機上完全不殘留舊狀態
  - 各家文件提供者對 `DocumentsContract` / `treeDocumentId` 的相容性

### 6.2 `StorageHelper` 內有 helper 尚未接入目前主 UI 流程

以下方法存在，但目前主畫面流程未見直接呼叫：

- `saveLrcFile()`
- `generateZipFileName()`
- `saveAsZip()`

因此它們比較像保留的儲存 helper，不應視為目前正式功能的一部分。

### 6.3 `smartNaming` 與 `timePrecision` 目前屬固定啟用

- `AppSettings` 有這兩個欄位
- `SubtitleConverter` 與 `FileNameHelper` 也會讀取它們
- 但目前 UI 沒有對應開關
- `SettingsManager.fromStoredValues()` 也固定建立為 `true`

所以目前可確認的是：

- 這兩個能力有被實作
- 但不是使用者可在 UI 上直接控制的正式設定功能

### 6.4 `viewBinding` 已開啟但目前未使用

- `app/build.gradle` 中 `buildFeatures { viewBinding true }`
- 但 `MainActivity` 仍採 `findViewById`

因此不能把此專案描述為「已實際使用 ViewBinding」。

### 6.5 `.sub/.str` 解析規則的泛用性未完全確認

- `convertSubToLrc()` 目前把 `\{數字\}\{數字\}文字` 的第一個數字直接當成毫秒處理
- 測試案例也以此行為為準
- 但不同來源的 `.sub/.str` 檔可能存在 frame-based 或其他變體

因此對 `.sub/.str` 的支援應描述為：

- 目前程式已實作一種明確的解析規則
- 是否涵蓋所有常見變體，僅靠靜態閱讀無法完全確認

### 6.6 沒有遠端資料與資料庫流程

本次掃描未發現：

- 遠端 API
- Repository
- 資料庫
- Room
- Service / Receiver
- 背景同步工作

如果未來文件模板要求填寫這類流程，應標示為「目前未使用」，而不是推測存在。

