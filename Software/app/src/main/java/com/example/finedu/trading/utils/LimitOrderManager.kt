package com.example.finedu.trading.utils

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LimitOrderManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun placeLimitOrder(
        symbol: String,
        amount: Double,
        limitPrice: Double,
        isBuy: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        if (amount < 0.01) {
            onResult(false, "Minimalna količina za limit nalog je 0.01.")
            return
        }

        val uid = userId ?: return onResult(false, "Korisnik nije prijavljen.")
        val orderValue = amount * limitPrice

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            if (isBuy) {
                val userBalance = userDoc.getDouble("userBalance") ?: 0.0
                if (userBalance < orderValue) {
                    onResult(false, "Nedovoljno sredstava za limit kupnju.")
                    return@addOnSuccessListener
                }
            } else {
                val assetRef = db.collection("users").document(uid)
                    .collection("portfolio").document(symbol)
                assetRef.get().addOnSuccessListener { assetDoc ->
                    val oldQty = assetDoc.getDouble("quantity") ?: 0.0
                    if (oldQty < amount) {
                        onResult(false, "Nedovoljno količine za limit prodaju.")
                        return@addOnSuccessListener
                    }
                }
            }

            val assetRef = db.collection("users").document(uid)
                .collection("portfolio").document(symbol)

            assetRef.get().addOnSuccessListener { assetDoc ->
                if (!assetDoc.exists()) {
                    val assetData = mapOf(
                        "symbol" to symbol,
                        "type" to "crypto"
                    )
                    assetRef.set(assetData)
                }
                val orderData = mapOf(
                    "type" to if (isBuy) "buy" else "sell",
                    "state" to "waiting",
                    "price" to limitPrice,
                    "quantity" to amount,
                    "date" to Timestamp.now()
                )
                assetRef.collection("transactions").add(orderData)
                    .addOnSuccessListener {
                        onResult(true, "Limit order postavljen!")
                    }
                    .addOnFailureListener {
                        onResult(false, "Greška pri postavljanju limit ordera.")
                    }
            }
        }
    }

    fun processWaitingLimitOrders(
        currentPrices: Map<String, Double>,
        onOrderExecuted: (() -> Unit)? = null
    ) {
        val uid = userId ?: return
        val usersRef = db.collection("users").document(uid).collection("portfolio")
        usersRef.get().addOnSuccessListener { portfolioSnapshot ->
            for (assetDoc in portfolioSnapshot.documents) {
                val symbol = assetDoc.getString("symbol") ?: continue
                val transactionsRef = assetDoc.reference.collection("transactions")
                transactionsRef.whereEqualTo("state", "waiting").get()
                    .addOnSuccessListener { transSnapshot ->
                        for (transDoc in transSnapshot.documents) {
                            val type = transDoc.getString("type") ?: continue
                            val price = transDoc.getDouble("price") ?: continue
                            val quantity = transDoc.getDouble("quantity") ?: continue
                            val currentPrice = currentPrices[symbol + "USDT"] ?: continue

                            val shouldExecute = when (type) {
                                "buy" -> currentPrice <= price
                                "sell" -> currentPrice >= price
                                else -> false
                            }

                            if (shouldExecute) {
                                val marketOrderManager = MarketOrderManager(context)
                                when (type) {
                                    "buy", "sell" -> {
                                        marketOrderManager.executeMarketOrder(
                                            symbol,
                                            quantity,
                                            price,
                                            type == "buy"
                                        ) { success, _ ->
                                            if (success) {
                                                transDoc.reference.update("state", "executed")
                                                onOrderExecuted?.invoke()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    fun cancelLimitOrder(
        orderId: String,
        assetSymbol: String,
        isBuy: Boolean,
        amount: Double,
        price: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = userId ?: return onResult(false, "Korisnik nije prijavljen.")
        val assetRef = db.collection("users").document(uid).collection("portfolio").document(assetSymbol)
        val orderRef = assetRef.collection("transactions").document(orderId)

        orderRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onResult(false, "Nalog ne postoji.")
                return@addOnSuccessListener
            }

            orderRef.delete().addOnSuccessListener {
                if (isBuy) {
                    db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                        val oldBalance = userDoc.getDouble("userBalance") ?: 0.0
                        val refund = amount * price
                        db.collection("users").document(uid)
                            .update("userBalance", oldBalance + refund)
                            .addOnSuccessListener {
                                onResult(true, "Nalog je otkazan, sredstva vraćena.")
                            }
                            .addOnFailureListener {
                                onResult(false, "Sredstva nisu vraćena.")
                            }
                    }
                } else {
                    onResult(true, "Nalog je otkazan.")
                }
            }.addOnFailureListener {
                onResult(false, "Greška pri otkazivanju naloga.")
            }
        }
    }
}
