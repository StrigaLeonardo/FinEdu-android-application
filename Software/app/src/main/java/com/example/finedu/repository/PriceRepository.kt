package com.example.finedu.repository

import com.example.finedu.api.FinnhubService
import com.example.finedu.api.CoinMarketCapService
import com.example.finedu.model.Asset
import javax.inject.Inject

class PriceRepository @Inject constructor(
    private val finnhubService: FinnhubService,
    private val CoinMarketCapService: CoinMarketCapService
) {
    suspend fun getAssetPrices(asset: Asset): Pair<Double, Double> {
        return when (asset.type) {
            "stock" -> finnhubService.getStockQuote(asset.symbol)
            "crypto" -> {
                val currentPrice = CoinMarketCapService.getCryptoPrice(asset.symbol)
                currentPrice to currentPrice
            }
            else -> 0.0 to 0.0
        }
    }
}
