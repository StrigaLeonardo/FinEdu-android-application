package com.example.finedu.model
import java.util.Date

data class Quiz(
    var id: String = "",
    val questions: List<Question> = emptyList(),
    val timestamp: Date = Date()
)

data class Question(
    val text: String = "",
    val answers: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0
): java.io.Serializable