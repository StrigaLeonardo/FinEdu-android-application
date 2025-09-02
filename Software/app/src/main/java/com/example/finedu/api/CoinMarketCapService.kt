package com.example.finedu.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// --- API INTERFACE ---

interface CoinMarketCapApi {
    @GET("v1/cryptocurrency/quotes/latest")
    suspend fun getQuote(
        @Query("symbol") symbol: String, // npr. BTC, ETH, SOL
        @Query("convert") convert: String = "USD",
        @Header("X-CMC_PRO_API_KEY") apiKey: String
    ): CoinMarketCapQuoteResponse
}

// --- DATA MODELI ---

data class CoinMarketCapQuoteResponse(
    val data: Map<String, CoinMarketCapCoinData>
)

data class CoinMarketCapCoinData(
    val quote: Map<String, CoinMarketCapQuote>
)

data class CoinMarketCapQuote(
    val price: Double,
    val percent_change_24h: Double
)

// --- SERVISNA KLASA ---

class CoinMarketCapService {
    // Tvoj API ključ (za produkciju ga čuvaj sigurnije!)
    private val apiKey = "9450ebeb-df84-42cf-9ffe-dae1998676c9"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pro-api.coinmarketcap.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CoinMarketCapApi::class.java)

    // Dohvati samo trenutnu cijenu (za prikaz vrijednosti)
    suspend fun getCryptoPrice(symbol: String): Double {
        val symbolUpper = symbol.uppercase()
        val response = api.getQuote(symbolUpper, "USD", apiKey)
        return response.data[symbolUpper]?.quote?.get("USD")?.price ?: 0.0
    }

    // Dohvati trenutnu i prethodnu cijenu (za daily change)
    suspend fun getCryptoQuote(symbol: String): Pair<Double, Double> {
        val symbolUpper = symbol.uppercase()
        val response = api.getQuote(symbolUpper, "USD", apiKey)
        val current = response.data[symbolUpper]?.quote?.get("USD")?.price ?: 0.0
        val percentChange = response.data[symbolUpper]?.quote?.get("USD")?.percent_change_24h ?: 0.0
        val previous = if (percentChange != 0.0) {
            current / (1 + percentChange / 100)
        } else {
            current
        }
        return current to previous
    }
}
