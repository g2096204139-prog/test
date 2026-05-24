package com.example.retiretracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amount: Double,
    val category: String,
    val note: String,
)

@Entity(primaryKeys = ["symbol"])
data class EtfHolding(
    val symbol: String,
    val shares: Double,
    val avgCost: Double,
    val lastPrice: Double,
)

@Entity
data class AssetSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val totalAsset: Double,
)
