package com.example.finedu.trading.utils

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class MarketOrderManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun executeMarketOrder(
        symbol: String,
        amount: Double,
        marketPrice: Double,
        isBuy: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        if (amount < 0.01) {
            onResult(false, "Minimalna količina za transakciju je 0.01.")
            return
        }

        val uid = userId ?: return onResult(false, "Korisnik nije prijavljen.")
        val assetRef = db.collection("users").document(uid).collection("portfolio").document(symbol)

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userBalance = userDoc.getDouble("userBalance") ?: 0.0

            if (isBuy) {
                val cost = amount * marketPrice
                if (userBalance < cost) {
                    onResult(false, "Nedovoljno sredstava na računu.")
                    return@addOnSuccessListener
                }

                db.collection("users").document(uid).update("userBalance", userBalance - cost)

                assetRef.get().addOnSuccessListener { assetDoc ->
                    val oldQty = assetDoc.getDouble("quantity") ?: 0.0
                    val oldPrice = assetDoc.getDouble("purchasePrice") ?: marketPrice
                    val newQty = oldQty + amount
                    val newPrice = if (oldQty > 0.0)
                        ((oldPrice * oldQty) + (marketPrice * amount)) / newQty
                    else marketPrice

                    assetRef.set(
                        mapOf(
                            "symbol" to symbol,
                            "quantity" to newQty,
                            "purchasePrice" to newPrice,
                            "type" to "crypto"
                        )
                    )

                    recordTransaction(assetRef, isBuy, amount, marketPrice)
                    onResult(true, "Market BUY nalog izvršen!")
                }
            } else {
                assetRef.get().addOnSuccessListener { assetDoc ->
                    val availableQty = assetDoc.getDouble("quantity") ?: 0.0
                    if (availableQty < amount) {
                        onResult(false, "Nedovoljno količine za prodaju.")
                        return@addOnSuccessListener
                    }

                    val newQty = availableQty - amount
                    assetRef.update("quantity", newQty)
                    db.collection("users").document(uid)
                        .update("userBalance", userBalance + (amount * marketPrice))

                    recordTransaction(assetRef, isBuy, amount, marketPrice)
                    onResult(true, "Market SELL nalog izvršen!")
                }
            }
        }
    }

    private fun recordTransaction(
        assetRef: DocumentReference,
        isBuy: Boolean,
        amount: Double,
        price: Double
    ) {
        val transData = mapOf(
            "type" to if (isBuy) "buy" else "sell",
            "state" to "executed",
            "price" to price,
            "quantity" to amount,
            "date" to Timestamp.now()
        )
        assetRef.collection("transactions").add(transData)
    }
}
