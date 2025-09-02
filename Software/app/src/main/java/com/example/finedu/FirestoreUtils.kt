package com.example.finedu

import com.example.finedu.model.UserPortfolio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun saveUserPortfolio(
    portfolio: UserPortfolio,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        onFailure(Exception("User not logged in"))
        return
    }
    firestore.collection("portfolios")
        .document(userId)
        .set(portfolio)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}
