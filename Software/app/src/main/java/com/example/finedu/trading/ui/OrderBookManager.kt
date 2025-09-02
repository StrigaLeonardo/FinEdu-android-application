package com.example.finedu.trading.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OrderbookManager(
    private val context: Context,
    private val orderbookContainer: LinearLayout
) {
    fun updateOrderbook(symbol: String, limit: Int = 10) {
        fetchOrderbook(symbol, limit) { bids, asks ->
            orderbookContainer.removeAllViews()

            // Header s dvije kolone: BUY lijevo, SELL desno
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, 8)
            }
            header.addView(makeCell("BUY", true, Color.rgb(0, 200, 0)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            header.addView(makeCell("SELL", true, Color.RED), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            orderbookContainer.addView(header)

            // Prikazuj redove orderbooka: svaka linija ima bid lijevo i ask desno
            val maxRows = maxOf(bids.size, asks.size)
            for (i in 0 until maxRows) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                // BUY (bid) s lijeve strane
                if (i < bids.size) {
                    val (bidPrice, bidQty) = bids[i]
                    val buyText = " %.2f | %.6f".format(bidPrice, bidQty)
                    row.addView(makeCell(buyText, false, Color.rgb(0, 200, 0)), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                } else {
                    row.addView(makeCell("", false), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }

                // SELL (ask) s desne strane (najniža cijena gore)
                if (i < asks.size) {
                    val (askPrice, askQty) = asks[asks.size - 1 - i]
                    val sellText = "%.2f | %.6f ".format(askPrice, askQty)
                    row.addView(makeCell(sellText, false, Color.RED), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                } else {
                    row.addView(makeCell("", false), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }

                orderbookContainer.addView(row)
            }
        }
    }


    private fun makeCell(text: String, isHeader: Boolean, color: Int = Color.WHITE): TextView {
        return TextView(context).apply {
            this.text = text
            setPadding(8, 4, 8, 4)
            setTextColor(color)
            gravity = Gravity.CENTER
            textSize = if (isHeader) 16f else 14f
            setTypeface(null, if (isHeader) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun fetchOrderbook(
        symbol: String,
        limit: Int,
        onResult: (List<Pair<Float, Float>>, List<Pair<Float, Float>>) -> Unit
    ) {
        Thread {
            val urlStr = "https://api.binance.com/api/v3/depth?symbol=$symbol&limit=$limit"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection
            try {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val bids = json.getJSONArray("bids")
                val asks = json.getJSONArray("asks")
                val bidList = mutableListOf<Pair<Float, Float>>()
                val askList = mutableListOf<Pair<Float, Float>>()
                for (i in 0 until bids.length()) {
                    val item = bids.getJSONArray(i)
                    bidList.add(Pair(item.getString(0).toFloat(), item.getString(1).toFloat()))
                }
                for (i in 0 until asks.length()) {
                    val item = asks.getJSONArray(i)
                    askList.add(Pair(item.getString(0).toFloat(), item.getString(1).toFloat()))
                }
                (context as? android.app.Activity)?.runOnUiThread {
                    onResult(bidList, askList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? android.app.Activity)?.runOnUiThread {
                    onResult(emptyList(), emptyList())
                }
            } finally {
                connection.disconnect()
            }
        }.start()
    }
}
