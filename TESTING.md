# RetireTracker 測試指南

## 先決條件
- Android Studio（最新版）
- Android SDK Platform 34
- Build-Tools 34.x
- Java 17

## 在 Android Studio 測試
1. 開啟專案根目錄。
2. 等待 Gradle Sync。
3. 建立 Emulator（API 26+）。
4. 執行 app。

## 指令測試
```bash
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
gradle :app:lint
```

## 已加入的自動化測試
- `DatabaseAndWorkerTest.expense_insert_and_query`
  - 驗證 Room in-memory DB 可新增並讀出 Expense。
- `DatabaseAndWorkerTest.worker_runs_successfully`
  - 驗證 `DailyUpdateWorker` 可執行並回傳 success。


## CI（GitHub Actions）
- 已新增 `.github/workflows/android-ci.yml`。
- CI 會固定使用 Java 17，並安裝 Android 34 所需 SDK 元件後執行：
  - `gradle :app:testDebugUnitTest`
  - `gradle :app:lint`
