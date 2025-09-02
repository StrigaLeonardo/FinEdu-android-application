package com.example.finedu

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finedu.model.Asset
import com.example.finedu.model.Transaction
import com.example.finedu.api.FinnhubService
import com.example.finedu.trading.helpers.ApiUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PortfolioActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AssetAdapter
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val firestore = FirebaseFirestore.getInstance()
    private val finnhubService = FinnhubService()
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portfolio)

        recyclerView = findViewById(R.id.recyclerViewAssets)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAssetsAndPricesAndTransactions()

        refreshJob = mainScope.launch {
            while (isActive) {
                delay(60000)
                refreshAssetsAndPricesAndTransactions()
            }
        }
    }

    private fun loadAssetsAndPricesAndTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("portfolio")
            .get()
            .addOnSuccessListener { snapshot ->
                val assets = mutableListOf<Asset>()
                for (doc in snapshot.documents) {
                    val asset = doc.toObject(Asset::class.java)
                    if (asset != null) {
                        val assetWithId = asset.copy(id = doc.id)
                        assets.add(assetWithId)
                    }
                }
                mainScope.launch {
                    val transactionsMap = fetchAllTransactionsMap(assets)
                    val filteredAssets = assets.filter { asset ->
                        val txs = transactionsMap[asset.id] ?: emptyList()
                        val totalBuy = txs.filter { it.type == "buy" }.sumOf { it.quantity }
                        val totalSell = txs.filter { it.type == "sell" }.sumOf { it.quantity }
                        val netQuantity = totalBuy - totalSell
                        netQuantity > 0.0
                    }
                    val priceMap = fetchCurrentPrices(filteredAssets)
                    adapter = AssetAdapter(filteredAssets, transactionsMap, priceMap)
                    recyclerView.adapter = adapter
                }
            }
    }

    private fun refreshAssetsAndPricesAndTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("portfolio")
            .get()
            .addOnSuccessListener { snapshot ->
                val assets = mutableListOf<Asset>()
                for (doc in snapshot.documents) {
                    val asset = doc.toObject(Asset::class.java)
                    if (asset != null) {
                        val assetWithId = asset.copy(id = doc.id)
                        assets.add(assetWithId)
                    }
                }
                mainScope.launch {
                    val transactionsMap = fetchAllTransactionsMap(assets)
                    val filteredAssets = assets.filter { asset ->
                        val txs = transactionsMap[asset.id] ?: emptyList()
                        val totalBuy = txs.filter { it.type == "buy" }.sumOf { it.quantity }
                        val totalSell = txs.filter { it.type == "sell" }.sumOf { it.quantity }
                        val netQuantity = totalBuy - totalSell
                        netQuantity > 0.0
                    }
                    val priceMap = fetchCurrentPrices(filteredAssets)
                    if (::adapter.isInitialized) {
                        adapter.updateData(filteredAssets, transactionsMap, priceMap)
                    }
                }
            }
    }

    private suspend fun fetchAllTransactionsMap(assets: List<Asset>): Map<String, List<Transaction>> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyMap()
        return coroutineScope {
            assets.associate { asset ->
                asset.id to async {
                    try {
                        val snapshot = firestore
                            .collection("users")
                            .document(userId)
                            .collection("portfolio")
                            .document(asset.id)
                            .collection("transactions")
                            .get()
                            .await()
                        snapshot.toObjects(Transaction::class.java)
                    } catch (e: Exception) {
                        Log.e("PortfolioActivity", "Error fetching transactions for ${asset.symbol}: ${e.message}")
                        emptyList()
                    }
                }
            }.mapValues { it.value.await() }
        }
    }

    private suspend fun fetchCurrentPrices(assets: List<Asset>): Map<String, Double> =
        suspendCoroutine { continuation ->
            ApiUtils.fetchAllBinancePrices { binancePrices ->
                mainScope.launch {
                    val priceMap = mutableMapOf<String, Double>()
                    for (asset in assets) {
                        when (asset.type) {
                            "crypto" -> {
                                val symbol = asset.symbol + "USDT"
                                priceMap[symbol] = binancePrices[symbol] ?: 0.0
                            }
                            "stock" -> {
                                val (currentPrice, _) = withContext(Dispatchers.IO) {
                                    finnhubService.getStockQuote(asset.symbol)
                                }
                                priceMap[asset.symbol] = currentPrice
                            }
                        }
                    }
                    continuation.resume(priceMap)
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        refreshJob?.cancel()
    }
}
