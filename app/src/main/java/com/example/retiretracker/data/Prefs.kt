package com.example.retiretracker.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("retire_prefs", Context.MODE_PRIVATE)

    fun getDailyBudget(): Double = sp.getFloat(KEY_DAILY_BUDGET, 0f).toDouble()

    fun setDailyBudget(value: Double) {
        sp.edit().putFloat(KEY_DAILY_BUDGET, value.toFloat()).apply()
    }

    fun setLastUpdateEpochMs(value: Long) {
        sp.edit().putLong(KEY_LAST_UPDATE_EPOCH_MS, value).apply()
    }

    fun getLastUpdateEpochMs(): Long = sp.getLong(KEY_LAST_UPDATE_EPOCH_MS, 0L)

    fun markBudgetAlertSent(date: String) {
        sp.edit().putString(KEY_BUDGET_ALERT_DATE, date).apply()
    }

    fun isBudgetAlertSent(date: String): Boolean = sp.getString(KEY_BUDGET_ALERT_DATE, "") == date

    companion object {
        private const val KEY_DAILY_BUDGET = "daily_budget"
        private const val KEY_LAST_UPDATE_EPOCH_MS = "last_update_epoch_ms"
        private const val KEY_BUDGET_ALERT_DATE = "budget_alert_date"
    }
}
