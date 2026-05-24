package com.example.retiretracker.data

import retrofit2.http.GET
import retrofit2.http.Query

interface PriceService {
    @GET("v7/finance/quote")
    suspend fun quote(@Query("symbols") symbols: String): QuoteResponse
}

data class QuoteResponse(
    val quoteResponse: QuoteResult,
)

data class QuoteResult(
    val result: List<QuoteItem>,
)

data class QuoteItem(
    val symbol: String,
    val regularMarketPrice: Double?,
)
