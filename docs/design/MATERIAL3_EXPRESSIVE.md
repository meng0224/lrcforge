# Material 3 Expressive 介面重寫

## 設計與範圍

使用 Android Views 官方 `Theme.Material3Expressive.DayNight.NoActionBar`，搭配 Material Components 1.14.0，保留字幕解析、批次轉換、自動儲存、SAF 授權及設定持久化流程。

- 首頁保留 App 名稱、簡短功能說明及薄荷綠色系；依使用者回饋移除宣傳大標題，字幕空狀態縮為兩行說明，移除大型 LRC 標記、色塊與多餘留白。
- 字幕列表放在輸出設定之前，移除重複的格式說明卡與重複副標。
- 主要操作與次要操作放在捲動內容之外。列表、輸出設定捲動時，主要按鈕仍可使用。
- 官方 Expressive 按鈕提供按壓形狀變化與狀態顏色；主要按鈕採 Medium 尺寸，依匯入／轉換狀態切換圖示與文字。
- 淺色／深色分別提供完整配色及系統列圖示亮度。平板增加左右留白。
- 輸出檔名、錯誤與字幕檔名不截斷；狀態保留文字，避免只靠顏色辨識。
- 開關有可讀標籤，進度與列表摘要提供無障礙狀態通知。
- 預設輸出位置顯示 `StorageHelper.getDownloadDirectory()` 的實際路徑，避免把 App 專用目錄顯示成公共 Download。

## 建置相依

- Material Components：1.11.0 → 1.14.0。
- Android Gradle Plugin：8.2.0 → 8.7.3。
- compileSdk：34 → 35。minSdk 24、targetSdk 34 及 Gradle wrapper 保持原設定。
- Java 17 以上、SDK Platform 35、Build Tools 34.0.0。
- 明確宣告 `androidx.documentfile:documentfile:1.0.1`。原本依賴 Material 間接帶入的類別，在升級後不再可用；SAF 功能本身沒有重寫。

這些調整是使用官方 Expressive Views 主題所需的建置基準，不涉及 Compose 遷移。

## 官方依據

- [Material Components 1.14.0 入門與 Expressive 主題需求](https://github.com/material-components/material-components-android/blob/1.14.0/docs/getting-started.md)
- [Expressive 按鈕及尺寸、形狀狀態定義](https://github.com/material-components/material-components-android/blob/1.14.0/lib/java/com/google/android/material/button/res/values/styles.xml)

## 驗證

2026-10-20 自動驗證完成：

- `assembleDebug`、`assembleDebugAndroidTest`：通過。
- `testDebugUnitTest`：13 個測試類別、59 項測試，0 failures / 0 errors。包含新增的 3 項 `MainActivityUiTest`，驗證固定操作區、匯入設定還原與深色資源。
- `lintDebug`：通過，0 errors / 51 warnings。仍有未使用資源、舊版相依、版面巢狀層級、舊 Android 儲存權限與既有文字格式等提醒，未宣稱全數清除。
- 淺色與深色共 14 組主要文字／背景色對比均達 4.5:1，最低為 5.54:1。此檢查不代表所有系統與互動狀態都已實機驗證。
- XML 解析、字串參照、首頁 view ID 唯一性與 `git diff --check` 通過。
- Instrumentation tests 已同步更新首頁文案、儲存區捲動與固定按鈕檢查，測試 APK 建置通過，尚未執行。

導覽列的 `windowLightNavigationBar` 限於 `values-v27` 主題，Android 7.0–8.0 使用黑色導覽列，避免將 API 27 屬性放入所有版本的主題。

本次使用 Java 21 及專案快取內 `.gradle-user/expressive-sdk` 的 SDK。原有 `local.properties` 指向不存在的 SDK，驗證時以 `ANDROID_HOME` 指定有效位置，沒有修改或提交本機設定。可在有效的 Java／SDK 環境中重現：

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user'
$env:ANDROID_HOME = Join-Path (Get-Location) '.gradle-user/expressive-sdk'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --console=plain
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。

SHA-256：`2CCED7600937D81455855B6D4E7A2F85DE89C3C913C891723625AC3329DCD767`。

目前 `adb devices` 沒有連線裝置。實機／模擬器的視覺效果、按壓動態、TalkBack、200% 字體、橫向畫面及文件選擇器完整操作尚未驗證。
