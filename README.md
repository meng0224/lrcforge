# LrcForge

LrcForge 是一個 Android 字幕轉換工具，可將常見字幕格式轉成 LRC 歌詞格式。

## 介面

- 使用官方 Material 3 Expressive Views 主題與按鈕，支援淺色及深色模式。
- 首頁先顯示字幕匯入／轉換列表，再顯示輸出位置與匯入選項。
- 「加入字幕／開始轉換」固定在畫面底部，捲動設定或長列表時仍可操作。
- 檔案狀態保留文字與配色，錯誤訊息、輸出檔名可完整換行顯示。
- 建置需要 Java 17 以上、Android SDK 35 與 Build Tools 34.0.0；Material Components 1.14.0、AGP 8.7.3。Gradle wrapper 沿用專案版本。
- 設計依據與驗證記錄見 [MATERIAL3_EXPRESSIVE.md](./docs/design/MATERIAL3_EXPRESSIVE.md)。

## 開發文件索引

- [DEVELOPMENT_ROADMAP.md](./docs/development/DEVELOPMENT_ROADMAP.md) - 主開發路線圖與目前狀態
- [FEATURE_LOGIC_MAP.md](./docs/development/FEATURE_LOGIC_MAP.md) - 既有功能流程與邏輯地圖
- [REVIEW_FINDINGS_PLAN.md](./docs/development/REVIEW_FINDINGS_PLAN.md) - code review findings 的歷史決策背景
- [PHASE2_VALIDATION_REPORT.md](./docs/validation/PHASE2_VALIDATION_REPORT.md) - Android 版本差異與 Phase 2 驗證記錄
- [PHASE3_TEST_HARDENING.md](./docs/validation/PHASE3_TEST_HARDENING.md) - 測試補強、執行鏈路與阻塞
- [FIGMA_DESIGN_PROMPT_PACK.md](./docs/design/FIGMA_DESIGN_PROMPT_PACK.md) - 可直接交給設計師或設計型 AI 的 Figma 設計提示詞包
- [後續路線建議.md](./docs/planning/後續路線建議.md) - Phase 4 之後的策略發展方向與優先級建議

## 主要功能

- 支援 `VTT`、`ASS`、`SSA`、`SRT`、`STR`、`SMI`、`SUB` 轉成 `LRC`
- 一般模式可使用系統文件選擇器一次選擇多個字幕檔
- 開啟「遞迴匯入子資料夾字幕」後，選檔入口會改為選擇資料夾並遍歷子資料夾
- 自動驗證副檔名與 `10MB` 大小限制
- 轉換後自動保存，不需逐檔另存
- 支援原文件目錄輸出、自訂 SAF 目錄輸出、預設應用下載目錄輸出

## 輸出模式

### 1. 預設應用下載目錄

- 未設定其他輸出方式時，輸出到 app-specific downloads

### 2. 自訂輸出資料夾

- 可用「選擇目錄」指定 SAF 授權資料夾
- 之後的輸出會保存到該目錄

### 3. 原文件目錄

- 可開啟「輸出到原文件目錄」
- 一般單檔匯入時，第一次寫入某個來源資料夾時，會要求使用者授權該資料夾
- 遞迴匯入時，會先對匯入根目錄授權一次，後續保存沿用同一授權
- 遞迴匯入保存時，會在匯入根目錄下重建原始子資料夾結構並寫回對應位置
- 與自訂輸出資料夾互斥，不能同時啟用

## 文件列表行為

- 一般模式：第二次選檔會覆蓋舊列表
- 原文件目錄模式：第二次選檔會追加到現有列表
- 遞迴匯入模式：每次匯入都追加到現有列表，並以 `Uri` 去重
- 遞迴匯入時，不支援格式或超過 `10MB` 的檔案會直接略過，不加入列表
- 可使用「清除文件列表」按鈕手動清空目前列表與相關暫存狀態

## 使用流程

1. 啟動 App。
2. 視需要設定輸出方式：
   - 保持預設應用下載目錄
   - 點「選擇目錄」設定自訂輸出資料夾
   - 或開啟「輸出到原文件目錄」
3. 視需要開啟「遞迴匯入子資料夾字幕」：
   - 關閉時：點「選擇文件」加入字幕檔
   - 開啟時：點「選擇資料夾」授權匯入根目錄並掃描子資料夾
4. App 會先驗證格式與大小；一般模式的不合法檔案會標成 `無效`，遞迴匯入的不合法檔案會直接略過。
5. 點「開始轉換」。
6. 轉換成功的檔案會自動保存到目前選定的輸出位置。

## 平台與權限

- Android 7~10：第一次選檔可能要求 `READ_EXTERNAL_STORAGE`
- Android 11+：匯入與自訂/原目錄輸出都依賴 SAF，不要求 `MANAGE_EXTERNAL_STORAGE`

## 重要限制

- 單個文件不能超過 `10MB`
- 系統選擇器目前不會預先過濾副檔名，會在選取後驗證
- 「智能命名清理」與「時間精度優化」目前固定啟用，尚無 UI 開關
- 轉換完成後會自動保存所有成功檔案，不提供單檔下載或 ZIP 打包流程
- 若 SAF provider 改寫檔名，例如變成 `.lrc.txt`，App 會視為保存失敗，避免誤報成功
- 字幕以 UTF-8 嚴格解碼，支援 UTF-8 BOM 與 Windows 換行；損壞或截斷的編碼會明確報錯，不替換成亂碼後繼續。
- `.sub/.str` 支援文字型 MicroDVD：首筆非空行需包含 FPS，例如 `{1}{1}25.0`；`{250}{300}文字` 在 25 FPS 時從 10 秒開始。缺少或無效 FPS 會報錯，不猜測影格率；不支援影像型 VobSub 或其他同副檔名格式。
- VTT 支援 `mm:ss.mmm` 與 `hh:mm:ss.mmm`。SRT/VTT 僅取起始時間，並檢查時間值與起訖順序。
- ASS/SSA 依 `Format:` 定位 Start、End、Text；沒有宣告時保留標準欄位順序。SMI 支援同一行的 SYNC／P 文字、帶引號的 Start 與零秒起始。
- VTT 僅在檔案開頭辨認 `WEBVTT` 標頭，字幕識別碼可使用 `WEBVTT`／`STYLE`／`REGION`。相鄰字幕缺少空行時報錯，不把時間碼混入歌詞。
- MicroDVD 讀取 FPS 後，省略結束格數的第 0／1 格字幕按正文處理，保留角括號與實體文字。ASS/SSA 允許起訖相同且清理後無文字的空白事件；非空的零時長事件或倒置時間仍報錯。
- SMI 的標籤與屬性依引號邊界解析；其他屬性內的 `start=` 不視為時間，重複 Start 或損壞屬性會報錯。
- 遇到無效字幕項目時，該檔停止轉換並回報格式、原始行號與原因，不儲存部分成功的內容；其他檔案繼續處理。Logcat 的 `SubtitleConversion` 標籤提供開始、解析完成與失敗堆疊，不記錄字幕正文。

## 開發與建置

```bash
# Windows
.\gradlew.bat build
.\gradlew.bat installDebug

# macOS / Linux
./gradlew build
./gradlew installDebug
```

完整 IDE 操作與裝置執行請見 [ANDROID_STUDIO_GUIDE.md](./docs/development/ANDROID_STUDIO_GUIDE.md)。

## Android Studio 詳細教學

Android Studio 安裝、Gradle Sync、模擬器與 Logcat 操作請見 [ANDROID_STUDIO_GUIDE.md](./docs/development/ANDROID_STUDIO_GUIDE.md)。
