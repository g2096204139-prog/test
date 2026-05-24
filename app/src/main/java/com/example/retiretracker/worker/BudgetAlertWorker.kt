package com.example.retiretracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retiretracker.data.AppDatabase
import com.example.retiretracker.data.Prefs
import java.time.LocalDate

class BudgetAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val prefs = Prefs(applicationContext)
        val today = LocalDate.now().toString()
        val budget = prefs.getDailyBudget()
        if (budget <= 0 || prefs.isBudgetAlertSent(today)) return Result.success()

        val spent = db.expenseDao().totalByDate(today)
        if (spent <= budget) return Result.success()

        createChannelIfNeeded()
        val over = spent - budget
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("今日預算超支")
            .setContentText("已超支 $$over")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notif)
        prefs.markBudgetAlertSent(today)
        return Result.success()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "budget_alerts"
    }
}
