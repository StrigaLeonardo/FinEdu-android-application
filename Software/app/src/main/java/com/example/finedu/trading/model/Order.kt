package com.example.finedu.trading.model

data class Order(
    val orderId: String,
    val symbol: String,
    val price: Double,
    val quantity: Double,
    val isBuy: Boolean
)


