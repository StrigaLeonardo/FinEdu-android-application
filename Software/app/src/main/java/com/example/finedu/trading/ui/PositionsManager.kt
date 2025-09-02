package com.example.finedu.trading.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.finedu.R
import com.example.finedu.model.Asset

class PositionsManager(
    private val context: Context,
    private val positionsContainer: LinearLayout
) {

    fun showPositions(
        positions: List<Asset>,
        priceMap: Map<String, Double>
    ) {
        positionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)

        // Sortiraj pozicije po ukupnom PnL-u
        val sortedPositions = positions.sortedByDescending { asset ->
            priceMap[asset.symbol + "USDT"]?.let { price ->
                (price - asset.purchasePrice) * asset.quantity
            } ?: Double.NEGATIVE_INFINITY
        }

        for (asset in sortedPositions) {
            val binanceSymbol = asset.symbol + "USDT"
            val currentPrice = priceMap[binanceSymbol] ?: continue

            val pnl = (currentPrice - asset.purchasePrice) * asset.quantity
            val pnlPercent = if (asset.purchasePrice != 0.0)
                ((currentPrice - asset.purchasePrice) / asset.purchasePrice) * 100.0
            else 0.0
            val pnlText = "%+.2f $ | %+.2f%%".format(pnl, pnlPercent)

            val color = when {
                pnl > 0f -> Color.rgb(0, 200, 0)
                pnl < 0f -> Color.RED
                else -> Color.WHITE
            }

            val row = inflater.inflate(R.layout.item_position, positionsContainer, false)
            row.findViewById<TextView>(R.id.tvPositionSymbol).text = asset.symbol
            row.findViewById<TextView>(R.id.tvPositionEntry).text =
                "Entry: %.2f".format(asset.purchasePrice)
            row.findViewById<TextView?>(R.id.tvPositionQty)?.text =
                "Qty: %.4f".format(asset.quantity)
            row.findViewById<TextView>(R.id.tvPositionPnl).apply {
                text = pnlText
                setTextColor(color)
            }
            positionsContainer.addView(row)
        }
    }

}
