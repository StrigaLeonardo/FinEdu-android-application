package com.example.finedu.trading.helpers

import android.os.Handler
import android.os.Looper
import com.github.mikephil.charting.data.CandleEntry
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ApiUtils {

    fun fetchAllBinancePrices(onResult: (Map<String, Double>) -> Unit) {
        Thread {
            val urlStr = "https://api.binance.com/api/v3/ticker/price"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection
            try {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val map = mutableMapOf<String, Double>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val symbol = obj.getString("symbol")
                    val price = obj.getString("price").toDouble()
                    map[symbol] = price
                }
                Handler(Looper.getMainLooper()).post { onResult(map) }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { onResult(emptyMap()) }
            } finally {
                connection.disconnect()
            }
        }.start()
    }

    fun fetchBinanceCandles(
        symbol: String,
        interval: String,
        limit: Int = 50,
        onResult: (List<CandleEntry>, List<Long>) -> Unit
    ) {
        Thread {
            val urlStr = "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection
            try {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val entries = mutableListOf<CandleEntry>()
                val openTimes = mutableListOf<Long>()
                for (i in 0 until jsonArray.length()) {
                    val kline = jsonArray.getJSONArray(i)
                    openTimes.add(kline.getLong(0))
                    val entry = CandleEntry(
                        i.toFloat(),
                        kline.getString(2).toDouble().toFloat(),
                        kline.getString(3).toDouble().toFloat(),
                        kline.getString(1).toDouble().toFloat(),
                        kline.getString(4).toDouble().toFloat()
                    )
                    entries.add(entry)
                }
                Handler(Looper.getMainLooper()).post { onResult(entries, openTimes) }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { onResult(emptyList(), emptyList()) }
            } finally {
                connection.disconnect()
            }
        }.start()
    }
}
