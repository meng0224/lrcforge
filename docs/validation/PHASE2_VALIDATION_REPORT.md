# Phase 2 Validation Report

本文件只負責記錄 Phase 2 的驗證執行結果。當前整體狀態請看 [DEVELOPMENT_ROADMAP.md](../development/DEVELOPMENT_ROADMAP.md)。

## Current Status

- Status: `NOT STARTED`
- Owner:
- Last Updated: 2026-03-15

## 1. Scope

- Android 11+
  - 冷啟動不跳 all-files-access
  - 遞迴匯入關閉時可直接選檔
  - 遞迴匯入開啟時主入口改為選擇資料夾
  - 可選擇 SAF 目錄並保存
  - 遞迴匯入搭配原文件目錄輸出時，匯入根目錄只授權一次
  - 保存後會在對應子資料夾生成 `.lrc`
  - 不會輸出成 `.lrc.txt`
- Android 7~10
  - 第一次選檔正確要求 `READ_EXTERNAL_STORAGE`
  - 授權後可正常選檔與保存
  - 遞迴匯入開啟時可正常選擇資料夾並保存
- 共通回歸
  - 無效檔案維持 `INVALID` 或在遞迴匯入時被略過
  - 同名輸出不先刪舊檔
  - 保存成功訊息需和實際檔案一致
  - 不影響既有 SAF 匯入與輸出流程

## 2. Environment

- Repo: `D:\06_開發與工具\Git\lrcapp`
- Date:
- Tester:
- Device / Emulator:
- Android Version:
- Build Variant:

## 3. Automated Validation Status

### Unit tests

- Command:
  - `set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
  - `set GRADLE_USER_HOME=D:\06_開發與工具\Git\lrcapp\.gradle-user`
  - `gradlew.bat testDebugUnitTest`
- Current status: `BLOCKED`
- Blocking reason:
  - With Android Studio JBR, Gradle can build and compile tests
  - `testDebugUnitTest` currently reaches JUnit runtime but every test class fails with `ClassNotFoundException`
  - `assembleDebug` and `compileDebugUnitTestKotlin` pass

### Instrumentation tests

- Candidate command:
  - `gradlew.bat connectedDebugAndroidTest`
- Current status: `NOT RUN`
- Minimum test candidate:
  - `MainActivityInstrumentationTest.coldLaunchShowsMainUiWithoutPermissionGate`

## 4. Manual Validation Checklist

### Android 11+

- [ ] Cold launch does not open all-files-access settings
- [ ] 遞迴匯入關閉時，點 `選擇文件` 會直接打開系統文件選擇器
- [ ] 遞迴匯入關閉時，選擇合法字幕檔後項目進入 `待處理`
- [ ] 遞迴匯入開啟時，點 `選擇資料夾` 會打開系統資料夾選擇器
- [ ] 遞迴匯入開啟時，匯入根目錄只授權一次
- [ ] 遞迴匯入時會加入子資料夾中的合法字幕檔，並略過無效檔
- [ ] Tap `選擇目錄` and select SAF directory
- [ ] Output directory text shows `SAF 授權目錄`
- [ ] Tap `開始轉換` and file saves successfully
- [ ] 原文件目錄輸出下，保存結果會出現在對應子資料夾
- [ ] 保存檔名維持 `.lrc`，不會變成 `.lrc.txt`
- [ ] Re-run same output name without deleting previous file first
- [ ] 保存成功 Snackbar 與實際產生的檔案數一致

### Android 7~10

- [ ] Cold launch does not request storage permission automatically
- [ ] First tap on `選擇文件` requests `READ_EXTERNAL_STORAGE`
- [ ] Grant permission and document picker opens normally
- [ ] Valid subtitle converts and saves successfully
- [ ] 遞迴匯入開啟時可正常選擇資料夾、掃描子資料夾與保存

### Shared regression

- [ ] Unsupported extension becomes `無效` in general import mode
- [ ] File larger than `10MB` becomes `無效` in general import mode
- [ ] 遞迴匯入時無效檔會被略過，不加入列表
- [ ] ASS/SSA text with commas remains intact
- [ ] SRT/VTT multiline text merges correctly
- [ ] 原文件目錄模式下保存成功訊息需對應實際檔案
- [ ] 新增原文件目錄、遞迴匯入與清除文件列表功能後，不影響原本 SAF 權限與輸出流程

## 5. Results

### Passed

- 

### Failed

- 

### Blocked

- Unit test execution blocked by JUnit runtime `ClassNotFoundException` for all test classes
- Instrumentation execution pending device/emulator availability

## 6. Follow-up

- Investigate the Gradle / JUnit runtime classpath issue, then rerun unit tests and record result here
- Run `connectedDebugAndroidTest` on available emulator/device
- Update `Status` and checklist after each validation round

