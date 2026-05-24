package com.example.retiretracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retiretracker.data.AppDatabase
import com.example.retiretracker.data.AssetSnapshot
import com.example.retiretracker.data.PriceRepository
import com.example.retiretracker.data.Prefs
import java.time.LocalDate

class DailyUpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)

        val existing = db.etfHoldingDao().all()
        if (existing.isEmpty()) {
            return Result.success()
        }

        val prices = try {
            PriceRepository().fetchPrices(existing.map { it.symbol })
        } catch (_: Exception) {
            return Result.retry()
        }

        if (prices.isEmpty()) {
            return Result.retry()
        }

        val updated = existing.map { h ->
            h.copy(lastPrice = prices[h.symbol] ?: h.lastPrice)
        }

        db.etfHoldingDao().upsertAll(updated)
        val total = updated.sumOf { it.shares * it.lastPrice }
        db.assetSnapshotDao().insert(AssetSnapshot(date = LocalDate.now().toString(), totalAsset = total))
        Prefs(applicationContext).setLastUpdateEpochMs(System.currentTimeMillis())
        return Result.success()
    }
}
