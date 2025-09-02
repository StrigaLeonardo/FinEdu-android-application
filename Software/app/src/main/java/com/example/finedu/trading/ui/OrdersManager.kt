package com.example.finedu.trading.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.finedu.R
import com.example.finedu.trading.model.Order



class OrdersManager(
    private val context: Context,
    private val ordersContainer: LinearLayout,
    private val onCancelOrder: (Order) -> Unit
) {
    fun showOrders(orders: List<Order>) {
        ordersContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (order in orders) {
            val row = inflater.inflate(R.layout.item_order, ordersContainer, false)
            row.findViewById<TextView>(R.id.tvOrderSymbol).text = order.symbol
            row.findViewById<TextView>(R.id.tvOrderPrice).text = "Cijena: %.2f".format(order.price)
            row.findViewById<TextView>(R.id.tvOrderQuantity).text = "Količina: %.4f".format(order.quantity)
            row.findViewById<ImageButton>(R.id.btnCancelOrder).setOnClickListener {
                onCancelOrder(order)
            }
            ordersContainer.addView(row)
        }
    }
}

