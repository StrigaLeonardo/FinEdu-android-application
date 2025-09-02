package com.example.finedu.model

data class User(
    val userId: String = "",
    val email: String = "",
    var userBalance: Double = 1000.0,
)