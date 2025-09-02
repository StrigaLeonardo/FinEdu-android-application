package com.example.finedu.trading.helpers

import android.widget.TextView

object OrderTotalCalculator {

    fun updateLimitOrderTotal(amount: Double, price: Double, tvTradeTotal: TextView) {
        if (amount > 0.0 && price > 0.0) {
            val total = amount * price
            tvTradeTotal.visibility = TextView.VISIBLE
            tvTradeTotal.text = String.format("Ukupno: %.2f $", total)
        } else {
            tvTradeTotal.visibility = TextView.GONE
        }
    }

    fun updateMarketOrderTotal(
        amount: Double,
        symbol: String,
        tvTradeTotal: TextView,
        fetchPriceMap: (onResult: (Map<String, Double>) -> Unit) -> Unit
    ) {
        if (amount <= 0.0) {
            tvTradeTotal.visibility = TextView.GONE
            return
        }

        fetchPriceMap { priceMap ->
            val price = priceMap[symbol + "USDT"] ?: return@fetchPriceMap
            val total = amount * price
            tvTradeTotal.post {
                tvTradeTotal.visibility = TextView.VISIBLE
                tvTradeTotal.text = String.format("Ukupno: %.2f $", total)
            }
        }
    }
}
