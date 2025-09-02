package com.example.finedu.model

// In Module.kt
data class Module(
    val id: String = "",
    val naziv: String = "",
    val slides: List<String> = emptyList() // Now expects strings, not Slide objects
)

