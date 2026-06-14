# AGENTS.md

## UI task priority rules

For any home screen / layout / theme / style / visual refactor task:

- Read `DESIGN.md` first.
- Treat `DESIGN.md` as the visual source of truth.
- Use the attached reference image as the primary visual target when one is provided.
- For complex UI tasks, plan first before editing code.
- Fix layout skeleton before applying color or cosmetic styling.
- Do not stop at minor cosmetic changes.
- The result must be visibly different in screenshots.

### Home screen target structure

Unless explicitly told otherwise, the LrcForge home screen should follow this structure:

1. plain top header:
   - app title
   - subtitle
   - not a hero card

2. first large card:
   - format / capability summary

3. second large card:
   - storage location and related settings

4. third large card:
   - file list / empty state / primary actions

### Hard UI constraints

- Do not keep a floating bottom action card over the page content.
- Do not rely on oversized scroll bottom padding to compensate for floating overlays.
- Do not use deep card-inside-card-inside-card nesting for the main home screen.
- Do not add new feature sections just to make the screen look fuller.
- Keep existing behavior, workflow, and strings unless explicitly requested.
- Do not migrate to Compose unless explicitly requested.

## UI done definition

A UI task is not done unless:

- the screenshot clearly reads as a plain header plus a few large cards
- the screen no longer looks like a legacy Android settings form
- primary actions are visually obvious and live inside the correct content card
- empty state looks intentional
- there are no overlaps, clipping, or compressed controls
- the screen is visibly closer to the provided reference image

## UI rules

For any UI / layout / theme / style task:

- Read `DESIGN.md` first.
- Treat `DESIGN.md` as the visual source of truth.
- Do not stop at minor cosmetic changes.
- The result must be visibly different in screenshots.
- Prefer Material You / Material Design 3 visual language:
  - tonal containers
  - clear hierarchy
  - large rounded corners
  - spacious layout rhythm
  - strong primary CTA
- Avoid old Android utility-form aesthetics.
- Keep existing functionality and workflow unchanged unless explicitly requested.

## Done definition for UI tasks

A UI task is not done unless:

- the screenshot looks clearly more polished than before
- the screen no longer reads like a legacy settings form
- primary actions are visually obvious
- sections are grouped by container hierarchy, not hard borders
- the empty state looks intentional

## 用途

- 本文件提供 `LrcForge` 專案專用的 agent 工作指引。
- 若本文件與一般 Android 慣例衝突，以本文件為優先。
- 作用範圍：整個 repository。

## 專案概況

- 平台：使用 Kotlin 與 Gradle Groovy DSL 的 Android App。
- App module：`app`。
- 套件根路徑：`com.example.lrcforge`。
- UI 技術：Android Views + Material Components + RecyclerView。
- 非同步工作：Kotlin coroutines 搭配 `lifecycleScope`。
- 測試：JUnit4 單元測試與 AndroidX instrumentation tests。

## 已檢查的規則來源

- 既有 `AGENTS.md`：建立本檔時不存在。
- `.cursor/rules/` 中的 Cursor 規則：不存在。
- `.cursorrules`：不存在。
- `.github/copilot-instructions.md` 中的 Copilot 規則：不存在。
- 若日後新增上述規則檔，請將其中的 repo 專屬規則合併到本文件。

## 重要路徑

- App 原始碼：`app/src/main/java/com/example/lrcforge`
- 單元測試：`app/src/test/java/com/example/lrcforge`
- Instrumentation 測試：`app/src/androidTest/java/com/example/lrcforge`
- 資源檔：`app/src/main/res`
- Manifest：`app/src/main/AndroidManifest.xml`
- Module build 檔：`app/build.gradle`
- Root build 檔：`build.gradle`
- Gradle wrapper 設定：`gradle/wrapper/gradle-wrapper.properties`
- Android Studio code style：`.idea/codeStyles/Project.xml`

## 環境注意事項

- Gradle wrapper 目前設定使用 `gradle-9.0-milestone-1-bin.zip`。
- Repo 文件提到，某些環境無法下載這個 distribution。
- Windows 環境下，專案文件另外建議設定：
- `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `GRADLE_USER_HOME=D:\Git\lrcforge\.gradle-user`
- 在此 workspace 中，`JAVA_HOME` 可能不存在；若未設定，Gradle 指令預期會失敗。

## 建置指令

- Windows 完整建置：`./gradlew.bat build`
- macOS/Linux 完整建置：`./gradlew build`
- 只建置 debug APK：`./gradlew.bat assembleDebug`
- 安裝 debug APK：`./gradlew.bat installDebug`
- 清理：`./gradlew.bat clean`
- 從乾淨狀態重建：`./gradlew.bat clean build`

## Lint 指令

- 所有 variant 的 Android lint：`./gradlew.bat lint`
- 只跑 debug lint：`./gradlew.bat lintDebug`
- 只跑 release lint：`./gradlew.bat lintRelease`
- 專案內沒有額外的 ktlint / detekt / spotless 設定。
- 風格檢查以 Android lint 搭配 IDE 的 reformat / optimize imports 為基準。

## 測試指令

- 所有本機 JVM 單元測試：`./gradlew.bat testDebugUnitTest`
- 所有已連線裝置或模擬器的 instrumentation tests：`./gradlew.bat connectedDebugAndroidTest`
- 只建置 instrumentation APK、不執行測試：`./gradlew.bat assembleDebug assembleDebugAndroidTest`
- `connectedDebugAndroidTest` 需要可用的實機或模擬器。

## 單一測試指令

- 單一 unit test 類別：
- `./gradlew.bat testDebugUnitTest --tests "com.example.lrcforge.converter.SubtitleConverterTest"`
- 單一 unit test 方法：
- `./gradlew.bat testDebugUnitTest --tests "com.example.lrcforge.converter.SubtitleConverterTest.assDialogueWithCommaKeepsFullText"`
- 另一個 unit test 類別範例：
- `./gradlew.bat testDebugUnitTest --tests "com.example.lrcforge.util.FileValidatorTest"`
- 單一 instrumentation test 類別：
- `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.lrcforge.MainActivityInstrumentationTest`
- 單一 instrumentation test 方法：
- `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.lrcforge.MainActivityInstrumentationTest#coldLaunchShowsOptimizedEmptyState`
- 在 macOS/Linux 上，請把 `gradlew.bat` 改成 `./gradlew`。

## 建議的 Agent 工作流程

- 修改使用者流程前，先讀 `README.md` 確認產品行為。
- `converter`、`model`、`util` 底下的純邏輯，優先用本機單元測試驗證。
- 只有在需要 Android runtime 的 UI / 狀態整合情境時，才使用 instrumentation tests。
- 若變更 storage、SAF 或權限流程，請同步參考 `docs/validation/PHASE2_VALIDATION_REPORT.md` 與 `docs/validation/PHASE3_TEST_HARDENING.md`。
- 若 Gradle 無法執行，仍應做靜態分析，並清楚說明哪些項目尚未驗證。

## 程式風格基準

- 遵循 Kotlin 官方風格；專案 code style 明確設定為 `KOTLIN_OFFICIAL`。
- 使用 4 個空白縮排。
- 預設保持 ASCII；只有在檔案已包含中文 UI 文案或有明確理由時才加入非 ASCII 字元。
- Git 透過 `.gitattributes` 統一正規化行尾。
- import 保持明確，不要加入 wildcard import。
- import 排序交給 IDE 最佳化；現有檔案大致依 Android、AndroidX、專案、第三方、Java/Kotlin 分組。
- package 依功能維持在 `adapter`、`converter`、`model`、`util` 等結構下。

## Kotlin 慣例

- 類別、object、enum、測試類別使用 `PascalCase`。
- 方法、屬性、區域變數、測試方法使用 `camelCase`。
- 測試方法名稱偏向描述式 camelCase，例如 `srtContentConvertsToLrc`。
- 預設優先使用 `val`；只有真正需要可變狀態時才用 `var`。
- 小型狀態載體使用 `data class`，如 `AppSettings`、`SubtitleFile`。
- 無狀態工具型別使用 `object`，如 `SettingsManager`、`FileValidator`。
- 優先拆成小型 helper method，避免過深巢狀邏輯。
- 若 extension function 能提升領域可讀性，可沿用此模式，例如 `FileStatus.isEligibleForConversion()`。

## 型別與 API 準則

- nullability 要明確且有意義。
- 只有在「缺少值」屬於合理且可預期結果時，才回傳 nullable。
- 優先使用最小可見性：先考慮 `private`，可測 helper 用 `internal`，真的需要才用 `public`。
- 現有程式對非 trivial function 常補明確回傳型別；新增程式時應延續此習慣。
- 保持資料模型穩定，不要把 UI 專用狀態混進工具或純資料類別。

## Coroutines 與執行緒

- UI 觸發的非同步工作目前使用 `lifecycleScope.launch(Dispatchers.IO)`。
- 操作 view 或 adapter 前，要先用 `withContext(Dispatchers.Main)` 切回主執行緒。
- 阻塞型 I/O、SAF 存取、檔案解析都要放在主執行緒外。
- 不要引入全域 coroutine scope。

## Android / UI 慣例

- 除非任務明確要求，否則保留既有 View-based 架構，不要自行遷移到 Compose。
- 優先重用現有 layout 中已使用的 Material 元件。
- 使用者可見行為需與目前中文 UI 文案保持一致。
- 可重用文字優先放進 `strings.xml`。
- 目前 codebase 的 Kotlin/XML 仍混有部分 inline 中文字串；新增修改時不要讓這種不一致更擴大。
- 若啟動畫面狀態或可見文案有變更，請同步更新 instrumentation tests。

## XML 慣例

- 專案 XML style 會依固定順序重排 attributes。
- 順序上先放 `xmlns:android`，再放其他 namespace，接著 `android:id`，最後其他 Android attributes。
- 非 trivial view 請維持一行一個 attribute，保持 layout 可讀性。
- 若非明確要求 redesign，請保留現有 Material card 間距、chip 樣式與整體視覺語言。

## 錯誤處理

- 對檔案 I/O、解析、SAF、權限流程要採安全失敗策略。
- 在 app 能恢復或能回報使用者的邊界層攔截例外。
- 可行時優先捕捉具體例外型別；除非是在保護 framework / 檔案存取邊界，否則避免過度寬泛的 catch。
- 除非呼叫端明確把它當 best-effort，否則不要靜默吞掉錯誤。
- 操作失敗時要提供簡潔的使用者提示，並保持狀態一致。
- 現有程式常用 `null`、`Pair<Boolean, String?>`、`FileStatus.ERROR` 表達失敗；新增行為時要與周邊模式一致。

## 測試慣例

- 單元測試保持快速、聚焦邏輯。
- `util` 與 `converter` helper 優先補純函式測試。
- 沿用目前測試框架的 JUnit4 imports，例如 `org.junit.Test`、`org.junit.Assert.*`。
- Android 專屬流程、啟動行為、view 顯示狀態、持久化設定等，適合用 instrumentation tests。
- Instrumentation tests 要重置共享狀態，參考 `MainActivityInstrumentationTest` 對 settings 的處理方式。

## Agent 通常應避免編輯的檔案

- 不要編輯 `build/` 產物。
- 不要編輯 `.gradle/` 快取。
- 不要提交 `local.properties` 的變更。
- 除非任務明確要求，否則不要調整 Gradle wrapper 版本或 Android SDK level。
- 除非變更本身就是專案工具或 code style 設定，否則不要動 `.idea/` 檔案。

## 變更衛生

- 變更保持最小且聚焦在當前功能或修正。
- 保留既有 package 結構與命名模式。
- 新增邏輯 helper 時，放到最接近的既有 feature package。
- 若修改字串或行為，應在同一個變更中更新受影響測試。
- 若因 Gradle、網路或模擬器限制而無法驗證，請在回覆中明確說明阻塞點。


