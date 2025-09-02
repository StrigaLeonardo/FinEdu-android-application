package com.example.finedu.model

import com.google.firebase.Timestamp

data class Transaction(
    val date: Timestamp? = null,
    val price: Double = 0.0,
    val quantity: Double = 0.0,
    val type: String = ""
)
