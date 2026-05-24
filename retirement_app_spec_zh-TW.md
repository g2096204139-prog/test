# Android 手機 App 規劃：退休後的日常花費、ETF 與資產變化（每日自動更新）

## 1. 產品目標
打造一個給退休族使用的 Android App，重點是：
- **快速記帳**：記錄每天生活花費。
- **自動追蹤**：每日更新 ETF、市值與總資產變化。
- **看得懂的報表**：用簡單圖表看每月花費、現金流、資產趨勢。

---

## 2. 核心功能（MVP）

### A. 日常花費紀錄
- 快速新增支出（金額、分類、日期、備註）。
- 常用分類：飲食、交通、醫療、保險、娛樂、孝親、其他。
- 一鍵重複（例如固定月費）。

### B. 資產總覽
- 現金、活存、定存、ETF、市值合計。
- 顯示：
  - 今日總資產
  - 與昨日差額
  - 與月初差額

### C. ETF 自動更新
- 每日固定時間背景更新（例如 08:00 與 18:00）。
- 更新內容：
  - ETF 最新價格
  - 持有股數／成本
  - 損益金額與報酬率
- 若更新失敗，顯示最近一次成功時間。

### D. 報表與提醒
- 週／月花費統計（分類圓餅圖、折線圖）。
- 預算提醒：當月某分類接近上限時推播。
- 每日摘要通知：
  - 今日支出
  - 今日資產變化

---

## 3. 資料模型（建議）

### Expense
- id
- date
- amount
- category
- note
- createdAt

### AssetSnapshot
- id
- date
- cashAmount
- depositAmount
- etfMarketValue
- totalAsset

### EtfHolding
- id
- symbol
- name
- shares
- avgCost
- lastPrice
- marketValue
- pnlAmount
- pnlPercent
- updatedAt

---

## 4. 技術實作建議（Android）
- **語言**：Kotlin
- **UI**：Jetpack Compose
- **本地資料庫**：Room
- **背景排程**：WorkManager（每日自動更新）
- **圖表**：MPAndroidChart 或 Compose chart library
- **通知**：Notification + Alarm/WorkManager
- **同步/API**：Retrofit + OkHttp

---

## 5. 每日自動更新流程
1. WorkManager 在排程時間觸發。
2. 呼叫 ETF 報價 API。
3. 更新 EtfHolding 與 AssetSnapshot。
4. 重新計算總資產與日變化。
5. 若啟用通知，推送「今日資產變動摘要」。

> 建議加上電池最佳化例外提示，避免背景工作被系統延遲。

---

## 6. 首版畫面清單
1. **首頁 Dashboard**：今日花費、總資產、ETF 損益。
2. **記帳頁**：快速新增／編輯支出。
3. **ETF 持倉頁**：各檔成本、市值、報酬率。
4. **報表頁**：週月支出、資產走勢。
5. **設定頁**：更新時間、通知、幣別、備份。

---

## 7. 後續進階功能（V2）
- 銀行簡訊/通知自動辨識記帳。
- 多帳本（個人/夫妻/家庭）。
- Google Drive 自動備份與還原。
- 退休提領模擬（4% rule、自訂提領率）。

---

## 8. 開發里程碑（建議）
- **第 1 週**：資料庫 + 記帳功能。
- **第 2 週**：ETF 持倉與手動更新。
- **第 3 週**：WorkManager 每日自動更新 + 通知。
- **第 4 週**：報表、優化 UI、封測。

