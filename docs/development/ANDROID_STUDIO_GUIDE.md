# Android Studio 開發與執行手冊

本文件說明如何在 Android Studio 中開啟、執行、驗證與排查 LrcForge。它是本專案的 Android Studio 專用手冊，不是通用 IDE 百科。

相關文件：
- 產品入口與最小建置命令：[README.md](../../README.md)
- 開發狀態與下一步：[DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)
- Android 版本差異驗證：[PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)
- 測試資產與阻塞：[PHASE3_TEST_HARDENING.md](../validation/PHASE3_TEST_HARDENING.md)

## 適用對象

這份文件同時寫給兩種人：
- Android 新手：需要從安裝 Android Studio、建立模擬器開始
- 本專案新進維護者：想快速把 `LrcForge` 跑起來、看 Logcat、驗證功能與定位問題

## 快速開始

如果你只想用最短路徑把專案跑起來，照下面做：

1. 安裝 Android Studio。
2. 用 Android Studio 開啟專案根目錄 `D:\06_開發與工具\Git\lrcapp`。
3. 確認已安裝 Android SDK Platform 34 與 Build Tools。
4. 等待 Gradle Sync 完成。
5. 選擇模擬器或實機。
6. 點擊工具列的 `Run` 執行 `app`。

如果你卡在任何一步，往下看對應章節。

## 1. 安裝 Android Studio 與必要元件

### Android Studio

請到官方網站下載並安裝 Android Studio：
- https://developer.android.com/studio

建議版本：
- Hedgehog (2023.1.1) 或更新版本

### 本專案需要的 SDK

開啟 Android Studio 後，請確認至少安裝：
- Android SDK Platform 34
- Android SDK Build-Tools 34.x
- Android SDK Platform-Tools
- Android Emulator

檢查方式：
1. 開啟 Android Studio。
2. 進入 `File > Settings > Android SDK`。
3. 確認 `SDK Platforms` 和 `SDK Tools` 內上述元件已安裝。

## 2. 開啟專案

1. 啟動 Android Studio。
2. 選擇 `Open`。
3. 指向專案根目錄：`D:\06_開發與工具\Git\lrcapp`。
4. 確認你選的是包含 `settings.gradle` 的那一層目錄。

開啟後，Android Studio 會自動開始 Gradle Sync。

## 3. Gradle Sync

### 正常情況

打開專案後，底部會顯示 Gradle Sync 狀態。第一次同步可能需要幾分鐘。

### 這個專案目前已知的限制

本專案目前的 Gradle wrapper 版本會嘗試下載：
- `gradle-9.0-milestone-1-bin.zip`

如果你的環境無法連外，Gradle Sync、建置或測試都可能失敗。若使用命令列，建議明確設定 Android Studio 內建 JBR。當前 workspace 已確認 `assembleDebug` 與 `compileDebugUnitTestKotlin` 可通過，但 `testDebugUnitTest` 目前卡在 JUnit runtime 對所有測試類別的 `ClassNotFoundException`。

### 如果 Sync 失敗，先檢查

1. 網路是否可下載 Gradle 發行版。
2. Android SDK 是否完整。
3. Android Studio 使用的 JDK 是否為 17 或內建 JBR。
4. `Build` 視窗或 `Sync` 訊息中是否明確指出缺少哪個元件。

## 4. 準備裝置

你可以用實機或模擬器。

### 方法 A：實機

1. 在手機啟用開發者選項。
2. 開啟 `USB 偵錯`。
3. 用 USB 連接電腦。
4. 在手機上允許偵錯授權。
5. 回到 Android Studio，確認裝置出現在裝置選擇器中。

### 方法 B：模擬器

1. 開啟 `Device Manager`。
2. 建立一台 Android 14 / API 34 模擬器。
3. 啟動模擬器。
4. 等待系統進入桌面後再執行 app。

## 5. 執行 LrcForge

### Android Studio 內執行

1. 確認上方 Run Configuration 是 `app`。
2. 選擇裝置。
3. 點擊 `Run`。

### 命令列執行

Windows:

```bash
.\gradlew.bat build
.\gradlew.bat installDebug
```

macOS / Linux:

```bash
./gradlew build
./gradlew installDebug
```

## 6. 本專案執行重點

這一段不是 Android 通用知識，而是 LrcForge 的實際行為。

### 權限與平台差異

- Android 7~10：第一次選檔可能要求 `READ_EXTERNAL_STORAGE`
- Android 11+：檔案匯入與輸出依賴 SAF，不要求 `MANAGE_EXTERNAL_STORAGE`

### 輸出模式

#### 預設輸出

- 未選擇其他輸出方式時，輸出到 app-specific downloads

#### 自訂輸出資料夾

- 點 `選擇目錄`
- 用 SAF 選一個授權目錄
- 之後輸出寫到這個資料夾

#### 原文件目錄

- 開啟 `輸出到原文件目錄`
- 第一次寫入某個來源資料夾時，會要求使用者授權該資料夾
- 同一來源資料夾之後會重用已保存授權
- 與自訂輸出資料夾互斥

### 文件列表行為

- 一般模式：第二次選檔會覆蓋舊列表
- 原文件目錄模式：第二次選檔會追加到現有列表
- 重複檔案以 `Uri` 去重
- `清除文件列表` 會清空目前列表，並重置相關暫存狀態

## 7. 常見工作流程

### 修改程式碼後重新驗證

1. 修改 Kotlin 或 XML。
2. 重新 `Run`。
3. 觀察主畫面是否能正常進入。
4. 依需求驗證：
   - 一般模式選檔覆蓋
   - 原文件目錄模式跨資料夾追加
   - 自訂輸出資料夾
   - 清除文件列表

### 驗證 SAF 輸出

1. 啟動 app。
2. 點 `選擇目錄`。
3. 選一個 SAF 目錄。
4. 選檔並開始轉換。
5. 確認輸出寫入剛授權的目錄。

### 驗證原文件目錄輸出

1. 開啟 `輸出到原文件目錄`。
2. 先從 A 資料夾選一些檔案。
3. 再去 B 資料夾選一些檔案。
4. 確認列表保留 A+B。
5. 開始轉換。
6. 第一次寫入每個來源資料夾時，確認會要求授權對應目錄。
7. 再次執行時，確認已授權目錄可被重用。

### 驗證清除文件列表

1. 先選入一些檔案。
2. 確認 `清除文件列表` 按鈕可點。
3. 點擊後列表應清空。
4. 若原本有原文件目錄模式的待處理狀態，也應一併清掉。

## 8. 建置與測試

### Android Studio 內建流程

- Build: `Build > Make Project`
- Rebuild: `Build > Rebuild Project`
- Clean: `Build > Clean Project`

### 命令列測試

Windows:

```bash
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set GRADLE_USER_HOME=D:\06_開發與工具\Git\lrcapp\.gradle-user
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

### 目前已知阻塞

- `assembleDebug` 可通過
- `compileDebugUnitTestKotlin` 可通過
- `testDebugUnitTest` 目前會進入 JUnit runtime，但所有測試類別出現 `ClassNotFoundException`
- instrumentation 測試需要可用模擬器或實機

如果你要看詳細的測試資產與阻塞，請直接看：
- [PHASE3_TEST_HARDENING.md](../validation/PHASE3_TEST_HARDENING.md)

如果你要看裝置驗證清單，請看：
- [PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)

## 9. Logcat 與排錯

### 看 Logcat

1. 執行 app。
2. 打開 Android Studio 底部的 `Logcat`。
3. 過濾 `com.example.lrcforge`。
4. 關注：
   - `FATAL EXCEPTION`
   - `Process: com.example.lrcforge`
   - 第一段 exception 類型與 stack trace

### App 啟動閃退時怎麼抓問題

請優先找這幾段：
- `FATAL EXCEPTION`
- `Caused by:`
- 指向 `MainActivity.kt` 或 layout inflate 的那一行

如果你要讓其他人協助排查，最有價值的是貼出：
- exception 類型
- `Caused by`
- 前 10 到 20 行 stack trace

## 10. 常見問題

### 1. Gradle Sync 失敗

優先檢查：
- 網路是否能下載 Gradle distribution
- Android SDK 是否完整
- JDK / JBR 是否正確

### 2. SDK 未安裝或版本不對

請到：
- `File > Settings > Android SDK`

確認 API 34 與 Build Tools 已安裝。

### 3. 模擬器啟動很慢或無法啟動

先做：
- 降低同時開啟的 IDE / 瀏覽器數量
- 重新啟動模擬器
- 確認虛擬化功能與 Android Emulator 元件已安裝

### 4. 裝置未識別

先檢查：
- 手機是否開啟 USB 偵錯
- 是否已在手機上授權電腦
- USB 線與 USB 埠是否正常

### 5. 啟動後直接閃退

先看 Logcat，抓：
- `FATAL EXCEPTION`
- `Caused by`
- 指向專案檔案的行號

如果沒有 stack trace，只憑「有閃退」很難準確定位。

### 6. 測試跑不動

先區分是哪一種問題：
- Gradle / 網路下載問題
- 沒有裝置或模擬器
- 真正的測試失敗

不要把三種問題混在一起看。

### 7. SAF 輸出或原文件目錄授權有問題

先確認：
- 是否真的選了對的資料夾
- 是否是第一次授權該來源目錄
- 是否先前已有儲存的 tree URI
- 是否切到了互斥的輸出模式

## 11. 建議閱讀順序

如果你是第一次接手這個專案，建議順序如下：

1. [README.md](../../README.md)
2. 本文件
3. [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)
4. [PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)
5. [PHASE3_TEST_HARDENING.md](../validation/PHASE3_TEST_HARDENING.md)

## 12. 文件角色說明

- `README.md`
  - 產品入口、使用方式、最小建置命令
- `ANDROID_STUDIO_GUIDE.md`
  - Android Studio 開發、執行、排錯與裝置驗證手冊
- `DEVELOPMENT_ROADMAP.md`
  - 目前做到哪、下一步做什麼
- `PHASE2_VALIDATION_REPORT.md`
  - Android 版本差異與裝置驗證紀錄
- `PHASE3_TEST_HARDENING.md`
  - 測試資產、執行方式與阻塞

