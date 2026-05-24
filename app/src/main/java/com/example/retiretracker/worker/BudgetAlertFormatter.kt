package com.example.retiretracker.worker

object BudgetAlertFormatter {
    fun overBudgetMessage(overAmount: Double): String = "已超支 $${"%.2f".format(overAmount)}"
}
