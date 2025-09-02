package com.example.finedu.trading.ui

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class TradingChartManager(
    private val context: Context,
    private val chart: CandleStickChart,
    private val tvCurrentPrice: TextView
) {
    private var previousPrice: Float? = null

    fun updateChart(
        candleEntries: List<CandleEntry>,
        openTimes: List<Long>,
        interval: String
    ) {
        if (candleEntries.isNotEmpty()) {
            val dataSet = CandleDataSet(candleEntries, "")
            dataSet.decreasingColor = Color.RED
            dataSet.increasingColor = Color.rgb(0, 200, 0)
            dataSet.decreasingPaintStyle = android.graphics.Paint.Style.FILL
            dataSet.increasingPaintStyle = android.graphics.Paint.Style.FILL
            dataSet.shadowColor = Color.DKGRAY
            dataSet.neutralColor = Color.BLUE
            chart.xAxis.textColor = Color.WHITE
            chart.axisRight.textColor = Color.WHITE

            val candleData = CandleData(dataSet)
            chart.data = candleData

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    if (idx in openTimes.indices) {
                        val date = Date(openTimes[idx])
                        val sdf = when (interval) {
                            "1m", "5m", "15m" -> SimpleDateFormat("HH:mm", Locale.getDefault())
                            else -> SimpleDateFormat("dd.MM.", Locale.getDefault())
                        }
                        return sdf.format(date)
                    }
                    return ""
                }
            }
            chart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            chart.axisRight.isEnabled = true
            chart.axisLeft.isEnabled = false

            // Prikaz trenutne cijene s dinamičkom bojom i $ znakom
            val lastCandle = candleEntries.lastOrNull()
            val currentPrice = lastCandle?.close ?: 0f
            val priceText = "Trenutna cijena: %.2f $".format(currentPrice)

            val color = when {
                previousPrice == null -> Color.WHITE
                currentPrice > previousPrice!! -> Color.rgb(0, 200, 0)
                currentPrice < previousPrice!! -> Color.RED
                else -> Color.WHITE
            }
            tvCurrentPrice.text = priceText
            tvCurrentPrice.setTextColor(color)
            previousPrice = currentPrice

            chart.visibility = android.view.View.VISIBLE
            chart.invalidate()
        } else {
            tvCurrentPrice.text = "Trenutna cijena: -"
            tvCurrentPrice.setTextColor(Color.WHITE)
            chart.clear()
            chart.visibility = android.view.View.INVISIBLE
            previousPrice = null
        }
    }

    fun resetPrice() {
        previousPrice = null
    }
}
