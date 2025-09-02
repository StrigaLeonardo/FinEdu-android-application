interface PriceService {
    suspend fun getStockPrice(symbol: String): Double
    suspend fun getCryptoPrice(symbol: String): Double
}
