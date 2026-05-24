package com.example.retiretracker.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PriceRepository {
    private val api: PriceService = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PriceService::class.java)

    suspend fun fetchPrices(symbols: List<String>): Map<String, Double> {
        if (symbols.isEmpty()) return emptyMap()
        val response = api.quote(symbols.joinToString(","))
        return response.quoteResponse.result
            .mapNotNull { item -> item.regularMarketPrice?.let { p -> item.symbol to p } }
            .toMap()
    }
}
