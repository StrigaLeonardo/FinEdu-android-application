package com.example.finedu.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubQuoteResponse
}

data class FinnhubQuoteResponse(
    val c: Double?, // current price
    val pc: Double? // previous close price
)

class FinnhubService(
    private val apiKey: String = "d1mkl79r01qlvnp31hlgd1mkl79r01qlvnp31hm0"
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://finnhub.io/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(FinnhubApi::class.java)

    // Returns Pair(currentPrice, previousClose)
    suspend fun getStockQuote(symbol: String): Pair<Double, Double> {
        return try {
            val response = api.getQuote(symbol, apiKey)
            val currentPrice = response.c ?: 0.0
            val previousClose = response.pc ?: 0.0
            currentPrice to previousClose
        } catch (e: Exception) {
            e.printStackTrace()
            0.0 to 0.0
        }
    }
}
