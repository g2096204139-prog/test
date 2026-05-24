package com.example.retiretracker.data

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

class PriceRepository {
    private val stooqApi: StooqPriceService = Retrofit.Builder()
        .baseUrl("https://stooq.com/")
        .build()
        .create(StooqPriceService::class.java)

    private val twseApi: TwsePriceService = Retrofit.Builder()
        .baseUrl("https://mis.twse.com.tw/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TwsePriceService::class.java)

    suspend fun fetchPrices(symbols: List<String>): Map<String, Double> {
        if (symbols.isEmpty()) return emptyMap()

        return symbols.mapNotNull { rawSymbol ->
            val symbol = normalizeSymbol(rawSymbol)
            val price = if (isTaiwanSymbol(symbol)) {
                fetchTaiwanPrice(symbol)
            } else {
                fetchUsPrice(symbol)
            }
            price?.let { rawSymbol to it }
        }.toMap()
    }

    private fun normalizeSymbol(symbol: String): String {
        val trimmed = symbol.trim().uppercase()
        return if (trimmed.matches(Regex("""\d{4,6}[A-Z]?"""))) "$trimmed.TW" else trimmed
    }

    private fun isTaiwanSymbol(symbol: String): Boolean = symbol.endsWith(".TW")

    private suspend fun fetchUsPrice(symbol: String): Double? {
        val stooqSymbol = if (symbol.endsWith(".US")) symbol else "$symbol.US"
        val csv = stooqApi.quote(symbol = stooqSymbol.lowercase()).string()
        val row = csv.lineSequence().drop(1).firstOrNull() ?: return null
        val columns = row.split(",")
        return columns.getOrNull(6)?.takeIf { it != "N/D" }?.toDoubleOrNull()
    }

    private suspend fun fetchTaiwanPrice(symbol: String): Double? {
        val code = symbol.substringBefore(".TW")
        return fetchTwsePrice("tse_$code.tw") ?: fetchTwsePrice("otc_$code.tw")
    }

    private suspend fun fetchTwsePrice(exchangeSymbol: String): Double? {
        val response = twseApi.stockInfo(exchangeSymbol = exchangeSymbol)
        val item = response.msgArray.firstOrNull() ?: return null
        return item.lastPrice.toDoubleOrNull() ?: item.referencePrice.toDoubleOrNull()
    }
}

interface StooqPriceService {
    @GET("q/l/")
    suspend fun quote(
        @Query("s") symbol: String,
        @Query("f") fields: String = "sd2t2ohlcv",
        @Query("h") header: String = "",
        @Query("e") format: String = "csv",
    ): ResponseBody
}

interface TwsePriceService {
    @Headers("Referer: https://mis.twse.com.tw/stock/fibest.jsp")
    @GET("stock/api/getStockInfo.jsp")
    suspend fun stockInfo(
        @Query("ex_ch") exchangeSymbol: String,
        @Query("json") json: Int = 1,
        @Query("delay") delay: Int = 0,
    ): TwseQuoteResponse
}

data class TwseQuoteResponse(
    val msgArray: List<TwseQuoteItem> = emptyList(),
)

data class TwseQuoteItem(
    @SerializedName("z") val lastPrice: String = "",
    @SerializedName("y") val referencePrice: String = "",
)
