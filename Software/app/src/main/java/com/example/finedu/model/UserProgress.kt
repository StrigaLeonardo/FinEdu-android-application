package com.example.finedu.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProgress(
    val userId: String = "",
    val modulesProgress: Map<String, Int> = emptyMap(),
    val quizProgress: Map<String, QuizProgressEntry> = emptyMap()
)

data class QuizProgressEntry(
    val score: Int = 0,
    @ServerTimestamp val date: Timestamp? = null,
    val answers: Map<String, Int> = emptyMap()
)
