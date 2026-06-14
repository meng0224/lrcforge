# Review Findings Plan

本文件是早期 code review findings 的原始修復規劃，主要用途是保存「為什麼要這樣修」的背景與已採納方案，不再作為當前執行看板。

目前狀態：
- Finding 1：已實作，待驗證
- Finding 2：已實作
- Finding 3：已實作
- Finding 4：已實作
- 當前執行狀態請以 [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md) 為主

## 1. Findings Background

### Finding 1

- 主題：Android 11+ 被不必要地強制要求 `MANAGE_EXTERNAL_STORAGE`
- 原始風險：使用者拒絕 all-files-access 後無法走本來可由 SAF 完成的匯入流程，且增加 Play policy 風險
- 已採納方案：
  - Android 11+ 匯入與自訂輸出目錄改為依賴 SAF
  - `MANAGE_EXTERNAL_STORAGE` 與 `requestLegacyExternalStorage` 從產品路徑移除
- 驗證結果另記錄於 [PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)

### Finding 2

- 主題：驗證失敗檔案仍被送進轉換流程
- 原始風險：無效檔案被重試、錯誤訊息被覆寫、統計失真
- 已採納方案：
  - 新增 `FileStatus.INVALID`
  - 僅允許合法可轉換項目進入轉換流程

### Finding 3

- 主題：ASS/SSA 文字欄位遇到逗號會被截斷
- 原始風險：歌詞文本被靜默破壞
- 已採納方案：
  - 根據 `Format:` 與 `Text` 欄位位置做受控切分
  - 保留沒有 `Format:` 時的保守 fallback

### Finding 4

- 主題：SAF 覆蓋寫入先刪舊檔，存在資料遺失風險
- 原始風險：建立新檔或寫入失敗時，使用者原檔已先被刪除
- 已採納方案：
  - 優先重用既有 document
  - 不再先刪除舊檔
  - 成功統計改為依據實際寫入結果計算

## 2. Original Prioritization

### 第一優先

- Finding 2
- Finding 3
- Finding 4

原因：直接影響輸出正確性與資料安全。

### 第二優先

- Finding 1

原因：屬於平台權限與產品可用性風險，但不如前三項直接破壞輸出內容與已有資料。

## 3. Original Validation Themes

當時定義的驗證方向如下，現已分流到專門文件：

- Android 版本差異驗證：見 [PHASE2_VALIDATION_REPORT.md](../validation/PHASE2_VALIDATION_REPORT.md)
- 測試補強與執行鏈路：見 [PHASE3_TEST_HARDENING.md](../validation/PHASE3_TEST_HARDENING.md)
- 當前 phase 狀態與下一步：見 [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)
