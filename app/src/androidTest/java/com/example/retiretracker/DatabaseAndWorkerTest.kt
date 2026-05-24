package com.example.retiretracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.retiretracker.data.AppDatabase
import com.example.retiretracker.data.Expense
import com.example.retiretracker.worker.DailyUpdateWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseAndWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun expense_insert_and_query() = runBlocking {
        db.expenseDao().insert(Expense(date = "2026-05-24", amount = 123.0, category = "飲食", note = "午餐"))
        val all = db.expenseDao().all()
        assertEquals(1, all.size)
        assertEquals(123.0, all.first().amount, 0.0)
    }

    @Test
    fun worker_runs_successfully() = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyUpdateWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success()::class, result::class)
    }
}
