package com.example.finedu.model

data class UserPortfolio(
    val userId: String = "",
    val cash: Double = 10000.0,
    val portfolioValue: Double = 0.0,
    val holdings: Map<String, Int> = mapOf(),
    val totalValue: Double = cash + portfolioValue
)