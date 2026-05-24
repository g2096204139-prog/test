package com.example.retiretracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert suspend fun insert(expense: Expense)

    @Query("SELECT * FROM Expense ORDER BY id DESC")
    suspend fun all(): List<Expense>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM Expense WHERE date = :date")
    suspend fun totalByDate(date: String): Double

    @Query(
        "SELECT category, IFNULL(SUM(amount),0) AS total " +
            "FROM Expense WHERE substr(date, 1, 7) = :yearMonth " +
            "GROUP BY category ORDER BY total DESC"
    )
    suspend fun totalsByCategoryInMonth(yearMonth: String): List<CategoryTotal>
}

data class CategoryTotal(
    val category: String,
    val total: Double,
)

@Dao
interface EtfHoldingDao {
    @Query("SELECT * FROM EtfHolding ORDER BY symbol")
    suspend fun all(): List<EtfHolding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EtfHolding>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EtfHolding)
}

@Dao
interface AssetSnapshotDao {
    @Insert suspend fun insert(snapshot: AssetSnapshot)

    @Query("SELECT * FROM AssetSnapshot ORDER BY id DESC LIMIT 1")
    suspend fun latest(): AssetSnapshot?

    @Query("SELECT * FROM AssetSnapshot WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): AssetSnapshot?

    @Query("SELECT * FROM AssetSnapshot ORDER BY id DESC LIMIT :limit")
    suspend fun latestN(limit: Int): List<AssetSnapshot>
}
