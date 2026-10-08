# Phase 3 Test Hardening

本文件只負責記錄 Phase 3 的測試資產、執行方式、阻塞與後續 instrumentation 範圍。當前 phase 狀態請看 [DEVELOPMENT_ROADMAP.md](../development/DEVELOPMENT_ROADMAP.md)。Phase 2 的手動驗證與裝置結果請看 [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md)。

## 1. Test Assets Added

### Unit tests already added

- `FileStatusTest`
  - `INVALID` 不可進入轉換
  - `PENDING` / `ERROR` 可進入轉換
- `FileValidatorTest`
  - 支援格式大小寫驗證
  - 不支援格式與無副檔名
  - `10MB` 邊界與 `10MB + 1`
- `FileNameHelperTest`
  - 停用 smart naming 時只替換副檔名
  - `music.mp3.ass -> music.lrc`
  - 一般 compound name 不被誤傷
- `SubtitleConverterTest`
  - ASS/SSA 逗號保留
  - ASS/SSA 樣式碼與 `\N` 清理
  - SRT 基本轉換
  - VTT 多行合併
  - SMI 基本轉換
  - SUB 基本轉換
  - unsupported extension 回傳 `null`
  - 空的 supported content 回傳空字串
- `StorageHelperTest`
  - 既有 document 重用
  - 缺少 document 時建立新檔
  - 巢狀子資料夾建立與重用
  - 保存驗證只在檔案存在且長度正確時視為成功
  - provider 改寫檔名為 `.lrc.txt` 時視為失敗
  - 多目標輸出成功統計
- `SettingsManagerTest`
  - `outputToSourceDirectory` 讀值
  - source directory preference key 編碼
  - 匯入根目錄授權 key 編碼
- `OutputSettingsPolicyTest`
  - 自訂輸出目錄與原文件目錄模式互斥
- `FileSelectionPolicyTest`
  - 原文件目錄模式多次選檔追加
  - 同 `Uri` 去重
  - 一般模式覆蓋舊列表
  - 同檔名不同 `Uri` 可共存
  - 遞迴匯入模式可追加且保留去重邏輯
- `FileListUiPolicyTest`
  - 空列表時禁用「清除文件列表」按鈕
  - 有列表時啟用「清除文件列表」按鈕
- `ImportSelectionCoordinatorTest`
  - 一般匯入覆蓋列表時回報選取數
  - 追加匯入時回報新增與重複數
  - 遞迴匯入時回報重複與無效略過數
- `SourceDirectoryPathHelperTest`
  - 來源目錄 parent document id 與 label 解析
  - 相對子資料夾輸出路徑摘要
- `SourceSaveAuthorizationStateTest`
  - 待授權 key 去重排隊
  - 授權後移入 ready targets
  - 拒絕授權後轉為保存失敗

### Instrumentation tests already added

- `MainActivityInstrumentationTest`
  - 冷啟動可顯示主畫面主要 UI，不受全域權限流程阻塞
  - 遞迴匯入開關狀態可在冷啟動後恢復

## 2. Execution Commands

### Unit tests

- `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `set GRADLE_USER_HOME=D:\06_開發與工具\Git\lrcapp\.gradle-user`
- `gradlew.bat testDebugUnitTest`

### Instrumentation tests

- `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `set GRADLE_USER_HOME=D:\06_開發與工具\Git\lrcapp\.gradle-user`
- `gradlew.bat connectedDebugAndroidTest`

## 3. Current Blockers

### Unit test execution

- Status: `PASSED` (2026-10-09, Asia/Taipei)
- Fresh Codex Cloud task restored JDK 17.0.18, Android SDK 34 and Build Tools 34.0.0.
  Tool paths and writable Gradle / Android user directories were verified.
- The Linux wrapper lacked execute permission; `gradlew` now has Git mode `100755`.
- Reproduced the baseline: 56 tests, 43 passed, 13 failed. No `ClassNotFoundException`.
- Root causes and fixes:
  - 12 tests called Android stub `Uri.parse` / `Uri.encode`. The five affected test classes
    now use Robolectric 4.12.2 with SDK 34 and no application manifest.
  - Robolectric's default download lock targeted a read-only user home. Gradle resolves
    `android-all-instrumented:14-robolectric-10818077-i6`; Robolectric loads it offline
    from the Gradle cache, using the existing Gradle proxy / TLS configuration for downloads.
  - The ASS fixture used doubled backslashes in a Kotlin raw string. It now contains
    standard ASS single-backslash style / newline sequences. The expected output is unchanged.
- `./gradlew testDebugUnitTest --no-daemon --console=plain`: 56 passed,
  0 failed, 0 errors, 0 skipped. All existing assertions remain in place.
- `./gradlew assembleDebug --no-daemon --console=plain`: passed before and after the fixes.
- Gradle 9.0-milestone-1, AGP 8.2.0 and Kotlin 1.9.22 remain unchanged.

### Instrumentation execution

- Status: `BLOCKED`
- Blocking reason:
  - No available device / emulator in this environment
  - JVM tests now pass; device / emulator validation remains separate.

## 4. Next Test Gaps

### Unit test gaps

- `SubtitleConverter`
  - `STR` 與 `SUB` 共用邏輯的額外案例
  - `timePrecision = false` 的輸出格式
  - 含 HTML tag 的 SRT/VTT 清理
- `StorageHelper`
  - 非 SAF 路徑的保存成功數
  - ZIP 輸出的成功與失敗回報
  - 保存結果與實際 `savedFileName` / 相對路徑摘要一致
- `MainActivity` related logic
  - 清除文件列表後的進度狀態重置
  - 遞迴匯入根目錄授權失效後重新授權
  - 非遞迴單檔匯入與遞迴匯入共存回歸

### Instrumentation next target

1. `MainActivity` 冷啟動不跳額外權限流程
2. 遞迴匯入開關狀態恢復與主按鈕文案更新
3. 選擇資料夾後匯入列表與保存回饋更新
4. 原文件目錄模式下遞迴匯入只授權匯入根目錄一次
5. 成功轉換後寫出一個檔案到對應子資料夾
6. 清除文件列表後畫面回到乾淨狀態

## 5. Notes

- 本文件不再重複手動 QA checklist；裝置驗證與回歸項目統一記錄於 [PHASE2_VALIDATION_REPORT.md](./PHASE2_VALIDATION_REPORT.md)
- 若後續要補更大範圍的 UI 自動化，請另開新文件，不把本文件擴張成總路線圖

