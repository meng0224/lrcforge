# Development Roadmap

本文件是 LrcForge 的唯一主路線圖，負責回答三件事：
- 做了什麼
- 現在在哪
- 接下來做什麼

其餘開發文件分工如下：
- 歷史 / 決策背景：[REVIEW_FINDINGS_PLAN.md](./REVIEW_FINDINGS_PLAN.md)
- Phase 2 驗證執行：[PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)
- Phase 3 測試策略與執行鏈路：[PHASE3_TEST_HARDENING.md](../validation/PHASE3_TEST_HARDENING.md)

## 1. Current Status

### Phase 1: 邏輯正確性與資料安全

- 狀態：`DONE`
- 目標：修正資料正確性與 SAF 覆寫風險
- 已完成：
  - 驗證失敗檔案改為 `INVALID`
  - ASS/SSA 含逗號文本不再截斷
  - SAF 同名輸出不再先刪舊檔
  - 第一批單元測試已補上
- 未完成：
  - `testDebugUnitTest` 尚未跑通；目前可執行到 JUnit runtime，但所有測試類別出現 `ClassNotFoundException`
- 下一步：
  - 排查 Gradle / JUnit test runtime classpath 後重跑 `testDebugUnitTest`

### Phase 2: Android 11+ 權限與儲存策略整理

- 狀態：`VALIDATION PENDING`
- 目標：移除 `MANAGE_EXTERNAL_STORAGE` 依賴，改以 SAF 為主
- 已完成：
  - Android 11+ 改為直接走系統文件選擇器
  - 冷啟動不再主動跳全域權限流程
  - Manifest 移除高風險權限與過時設定
  - README / guide / UI 文案已對齊
  - 遞迴資料夾匯入開關與匯入根目錄授權模型已落地
- 未完成：
  - Android 11+ / Android 7~10 的實機或模擬器驗證結果尚未回填
- 下一步：
  - 依 [PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md) 完成驗證與回填

### Phase 3: 測試補強與回歸保護

- 狀態：`IN PROGRESS`
- 目標：把已修好的核心邏輯變成可持續回歸的測試資產
- 已完成：
  - `FileValidatorTest`
  - `FileNameHelperTest`
  - 擴充 `SubtitleConverterTest`
  - `StorageHelperTest`
  - `SettingsManagerTest`
  - `OutputSettingsPolicyTest`
  - `FileSelectionPolicyTest`
  - `FileListUiPolicyTest`
  - `ImportSelectionCoordinatorTest`
  - `SourceDirectoryPathHelperTest`
  - `SourceSaveAuthorizationStateTest`
  - 最小 instrumentation 測試 `MainActivityInstrumentationTest`
- 未完成：
  - Unit tests 尚未在此環境跑通；`compileDebugUnitTestKotlin` 已通過，但 `testDebugUnitTest` 目前在 JUnit runtime 找不到測試類別
  - Instrumentation tests 尚未執行
  - Phase 2 驗證結果尚未併入測試執行結論
- 下一步：
  - 先排查 `testDebugUnitTest` 的 `ClassNotFoundException`
  - 再跑 `connectedDebugAndroidTest`

### Phase 4: 結構重整與可維護性提升

- 狀態：`IN PROGRESS`
- 目標：降低 `MainActivity` 的耦合度，提升 parser / storage / flow 的可維護性
- 已完成：
  - 已完成第一段低風險抽離，保留 Android Views 與既有流程
  - `ImportSelectionCoordinator` 接手匯入列表合併與回饋文案
  - `SourceDirectoryPathHelper` 接手來源目錄 key / label / 相對路徑摘要等純邏輯
  - `SourceSaveAuthorizationState` 接手來源目錄輸出時的待授權與待保存狀態
- 未完成：
  - ViewModel / UseCase / Repository 層尚未引入
  - parser 類別拆分
  - 更清楚的錯誤模型
- 下一步：
  - 先排查 unit test runtime，再視風險繼續拆 `MainActivity` 的 conversion / storage flow

## 2. Feature Track

### 輸出到原文件目錄與列表交互

- 狀態：`IN PROGRESS`
- 目標：讓轉換結果可依來源資料夾或匯入根目錄寫回原文件目錄，並支援多次跨資料夾累積匯入
- 已完成：
  - `AppSettings` / `SettingsManager` 新增 `outputToSourceDirectory`
  - 主畫面新增「輸出到原文件目錄」開關
  - 自訂輸出資料夾與原文件目錄模式互斥
  - 一般單檔匯入可保存來源資料夾的 SAF tree URI 授權
  - 已完成遞迴資料夾匯入開關
  - 已完成匯入根目錄授權保存
  - 已完成子資料夾結構還原輸出
  - 原文件目錄模式與遞迴匯入模式支援多次追加
  - 重複檔案以 `Uri` 去重
  - 主畫面新增「清除文件列表」按鈕
  - `StorageHelper` 新增多目標輸出模型、保存驗證與檔名改寫防呆
  - 已補 `FileSelectionPolicyTest`、`FileListUiPolicyTest`、`StorageHelperTest`
  - 已將匯入合併、來源目錄路徑解析、來源目錄授權暫存狀態抽到 helper 並補單元測試
- 未完成：
  - `assembleDebug` 與 `compileDebugUnitTestKotlin` 已通過，但 unit tests 尚未跑通
  - 實機驗證單一來源 / 多來源 / 遞迴匯入 / 重用授權流程
  - 驗證清除列表後不殘留舊的待授權/待保存狀態
- 下一步：
  - 在 Android 11+ 裝置驗證遞迴匯入、匯入根目錄一次授權與子資料夾回寫流程
  - 驗證 `.lrc` 檔名不會被 provider 改寫成 `.lrc.txt`
  - 驗證一般模式覆蓋、原文件目錄模式追加、清除列表回到乾淨狀態

## 3. Current Blockers

- `assembleDebug` 已通過
- `compileDebugUnitTestKotlin` 已通過
- `gradlew.bat testDebugUnitTest` 可執行到 JUnit runtime，但所有測試類別目前出現 `ClassNotFoundException`
- instrumentation 需要可用 emulator / device
- Phase 2 雖已實作，但尚未完成裝置驗證回填
- 原文件目錄輸出、遞迴匯入與保存驗證功能尚未在實機上完成完整驗證

## 4. Immediate Next Steps

1. 排查 `testDebugUnitTest` 的測試 classpath / runner 問題
2. 在可用 emulator / device 執行 instrumentation tests
3. 依 validation report 完成 Android 11+ 與 Android 7~10 驗證
4. 驗證遞迴匯入、匯入根目錄授權重用、檔名維持 `.lrc` 與清除列表流程
5. 更新各附屬文件中的 `status / passed / blocked` 結果

## 5. Acceptance Snapshot

- Phase 1 完成條件：功能修復已上線且 unit tests 在正式環境跑通
- Phase 2 完成條件：Android 11+ 與 Android 7~10 驗證結果已回填且通過
- Phase 3 完成條件：unit tests 與最小 instrumentation 至少各跑通一次
- Phase 4 下一段條件：unit test runtime 問題釐清，並確認第一段 helper 抽離沒有回歸
- 新功能完成條件：來源目錄輸出在單一來源、多來源、遞迴匯入、匯入根目錄一次授權、`.lrc` 檔名正確與清除列表場景都通過手動驗證
