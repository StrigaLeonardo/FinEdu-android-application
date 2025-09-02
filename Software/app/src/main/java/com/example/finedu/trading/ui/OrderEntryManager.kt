package com.example.finedu.trading.ui

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.example.finedu.R

class OrderEntryManager(
    private val context: Context,
    private val orderTypeGroup: RadioGroup,
    private val orderSideGroup: RadioGroup,
    private val etOrderAmount: EditText,
    private val etOrderPrice: EditText,
    private val tvTradeTotal: TextView,
    private val btnSubmitOrder: Button,
    private val getCurrentPrice: () -> Double
) {
    private var isMarketOrder = true
    private var isBuy = true

    init {
        etOrderPrice.visibility = View.GONE
        tvTradeTotal.visibility = View.VISIBLE
        btnSubmitOrder.text = context.getString(R.string.buy)
        btnSubmitOrder.setBackgroundColor(Color.rgb(0, 200, 0))

        // Tip naloga: Market / Limit
        orderTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            isMarketOrder = checkedId == R.id.rbMarket
            etOrderPrice.visibility = if (isMarketOrder) View.GONE else View.VISIBLE
            tvTradeTotal.visibility = if (isMarketOrder) View.VISIBLE else View.GONE
            updateTradeTotal()
        }

        // Strana naloga: Buy / Sell
        orderSideGroup.setOnCheckedChangeListener { _, checkedId ->
            isBuy = checkedId == R.id.rbBuy
            btnSubmitOrder.text = if (isBuy) context.getString(R.string.buy) else context.getString(R.string.sell)
            btnSubmitOrder.setBackgroundColor(if (isBuy) Color.rgb(0, 200, 0) else Color.RED)
        }

        etOrderAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateTradeTotal()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etOrderPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateTradeTotal()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSubmitOrder.setOnClickListener {
            val amount = etOrderAmount.text.toString().replace(",", ".").toDoubleOrNull()
            val price = if (isMarketOrder)
                getCurrentPrice()
            else
                etOrderPrice.text.toString().replace(",", ".").toDoubleOrNull()

            if (amount == null || amount <= 0.0) {
                Toast.makeText(context, "Unesi ispravnu količinu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isMarketOrder && (price == null || price <= 0.0)) {
                Toast.makeText(context, "Unesi ispravnu cijenu za limit nalog", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val orderType = if (isMarketOrder) "Market" else "Limit"
            val side = if (isBuy) "Buy" else "Sell"
            val priceDisplay = "%.2f $".format(price ?: 0.0)
            val msg = "$orderType $side\nKoličina: $amount\nCijena: $priceDisplay"

            Toast.makeText(context, "Nalog poslan:\n$msg", Toast.LENGTH_LONG).show()

        }
    }

    private fun updateTradeTotal() {
        if (isMarketOrder) {
            val amount = etOrderAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val price = getCurrentPrice()
            val total = amount * price
            tvTradeTotal.text = "Ukupno: %.2f $".format(total)
        } else {
            tvTradeTotal.text = ""
        }
    }
}
