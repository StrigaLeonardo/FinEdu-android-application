package com.example.finedu.model

data class Asset(
    val id: String = "",
    val symbol: String = "",
    val type: String = "", // "stock" ili "crypto"
    val quantity: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val state: String = ""
)
