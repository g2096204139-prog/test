package com.example.retiretracker.worker

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertFormatterTest {
    @Test
    fun overBudgetMessage_formatsAmountToTwoDecimals() {
        assertEquals("已超支 $25.50", BudgetAlertFormatter.overBudgetMessage(25.5))
    }

    @Test
    fun overBudgetMessage_roundsFractionalCents() {
        assertEquals("已超支 $10.13", BudgetAlertFormatter.overBudgetMessage(10.126))
    }
}
