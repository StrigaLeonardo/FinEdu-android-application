package com.example.finedu.portfolio

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.example.finedu.api.FinnhubService
import com.example.finedu.api.CoinMarketCapService
import com.example.finedu.model.Asset
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import java.util.*

class PortfolioManager(
    private val context: Context,
    private val pieChart: PieChart,
    private val txtTotalValue: TextView,
    private val onDailyChangeUpdated: (Double, Double) -> Unit = { _, _ -> },
    private val finnhubService: FinnhubService = FinnhubService(),
    private val coinMarketCapService: CoinMarketCapService = CoinMarketCapService()
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var portfolioListener: ListenerRegistration? = null
    private var userBalanceListener: ListenerRegistration? = null
    private var mainScope = CoroutineScope(Dispatchers.Main + Job())

    private var latestAssets: List<Asset> = emptyList()
    private var latestUserBalance: Double = 0.0

    private var assetsLoaded = false
    private var balanceLoaded = false

    fun startPortfolioListeners() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        portfolioListener = firestore.collection("users")
            .document(userId)
            .collection("portfolio")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PortfolioManager", "Error fetching portfolio", error)
                    return@addSnapshotListener
                }
                latestAssets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                assetsLoaded = true
                triggerInitialCalculationIfReady()
            }

        userBalanceListener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                latestUserBalance = snapshot?.getDouble("userBalance") ?: 0.0
                balanceLoaded = true
                triggerInitialCalculationIfReady()
            }

        // Periodično ažuriranje svakih 12 sekundi
        mainScope.launch {
            while (true) {
                delay(12000)
                calculateAndDisplayPortfolio(latestAssets, latestUserBalance)
            }
        }
    }

    private fun triggerInitialCalculationIfReady() {
        if (assetsLoaded && balanceLoaded) {
            calculateAndDisplayPortfolio(latestAssets, latestUserBalance)
        }
    }

    fun stopPortfolioListeners() {
        mainScope.cancel()
        portfolioListener?.remove()
        userBalanceListener?.remove()
    }

    private fun calculateAndDisplayPortfolio(assets: List<Asset>, userBalance: Double) {
        mainScope.launch {
            var totalPortfolioValue = userBalance
            var stocksValue = 0.0
            var cryptoValue = 0.0

            // Za daily change
            var dailyChangeMoney = 0.0
            var previousMarketValue = 0.0
            var currentMarketValue = 0.0

            val prices = assets.map { asset ->
                async(Dispatchers.IO) {
                    try {
                        when (asset.type) {
                            "stock" -> {
                                val (currentPrice, previousClose) = finnhubService.getStockQuote(asset.symbol)
                                Log.d("PortfolioManager", "STOCK ${asset.symbol}: current=$currentPrice, prevClose=$previousClose, time=${System.currentTimeMillis()}")
                                asset to Triple(currentPrice, previousClose, asset.quantity)
                            }
                            "crypto" -> {
                                val (currentPrice, previousClose) = coinMarketCapService.getCryptoQuote(asset.symbol)
                                Log.d("PortfolioManager", "CRYPTO ${asset.symbol}: current=$currentPrice, prevClose=$previousClose, time=${System.currentTimeMillis()}")
                                asset to Triple(currentPrice, previousClose, asset.quantity)
                            }
                            else -> {
                                Log.d("PortfolioManager", "UNKNOWN TYPE ${asset.symbol}")
                                asset to Triple(0.0, 0.0, asset.quantity)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PortfolioManager", "Error fetching ${asset.symbol}: ${e.message}")
                        asset to Triple(0.0, 0.0, asset.quantity)
                    }
                }
            }.awaitAll()

            prices.forEach { (asset, triple) ->
                val (currentPrice, previousClose, quantity) = triple
                val assetValue = quantity * currentPrice
                totalPortfolioValue += assetValue
                when (asset.type) {
                    "stock" -> stocksValue += assetValue
                    "crypto" -> cryptoValue += assetValue
                }

                // Daily change calculation
                dailyChangeMoney += (currentPrice - previousClose) * quantity
                previousMarketValue += previousClose * quantity
                currentMarketValue += currentPrice * quantity
            }

            Log.d("PortfolioManager", "TOTAL portfolio: $totalPortfolioValue at ${System.currentTimeMillis()}")

            updatePieChart(userBalance, stocksValue, cryptoValue)
            updateTotalDisplay(totalPortfolioValue)

            val dailyChangePercent = if (previousMarketValue != 0.0) {
                ((currentMarketValue - previousMarketValue) / previousMarketValue) * 100
            } else {
                0.0
            }

            onDailyChangeUpdated(dailyChangeMoney, dailyChangePercent)
        }
    }

    private fun updateTotalDisplay(total: Double) {
        txtTotalValue.text = String.format("$%,.2f", total)
    }

    private fun updatePieChart(userBalance: Double, stocksValue: Double, cryptoValue: Double) {
        val entries = mutableListOf<PieEntry>()
        if (userBalance > 0) entries.add(PieEntry(userBalance.toFloat(), "Cash"))
        if (stocksValue > 0) entries.add(PieEntry(stocksValue.toFloat(), "Stocks"))
        if (cryptoValue > 0) entries.add(PieEntry(cryptoValue.toFloat(), "Crypto"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                android.graphics.Color.parseColor("#FFD600"),
                android.graphics.Color.parseColor("#4CAF50"),
                android.graphics.Color.parseColor("#2196F3")
            )
            valueTextColor = android.graphics.Color.BLACK
            valueTextSize = 16f
        }

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.invalidate()
    }

    fun refreshPortfolio() {
        calculateAndDisplayPortfolio(latestAssets, latestUserBalance)
    }
}
