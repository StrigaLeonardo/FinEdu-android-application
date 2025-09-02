package com.example.finedu.trading.repository

import com.example.finedu.model.Asset
import com.example.finedu.trading.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

object DatabaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun getAssetsWithExecutedTransactions(onResult: (List<Asset>) -> Unit) {
        val uid = userId ?: return onResult(emptyList())
        val assets = mutableListOf<Asset>()

        db.collection("users").document(uid).collection("portfolio")
            .get()
            .addOnSuccessListener { portfolioSnapshot ->
                val tasks = mutableListOf<Task<*>>()
                for (assetDoc in portfolioSnapshot.documents) {
                    val symbol = assetDoc.getString("symbol") ?: continue
                    val purchasePrice = assetDoc.getDouble("purchasePrice") ?: continue
                    val quantity = assetDoc.getDouble("quantity") ?: continue
                    if (quantity <= 0.0) continue

                    val transactionsRef = assetDoc.reference.collection("transactions")
                    val task = transactionsRef.whereEqualTo("state", "executed").get()
                        .addOnSuccessListener { transSnapshot ->
                            if (!transSnapshot.isEmpty) {
                                assets.add(
                                    Asset(
                                        symbol = symbol,
                                        purchasePrice = purchasePrice,
                                        quantity = quantity
                                    )
                                )
                            }
                        }
                    tasks.add(task)
                }
                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        onResult(assets)
                    }
            }
    }

    fun getWaitingOrders(onResult: (List<Order>) -> Unit) {
        val uid = userId ?: return onResult(emptyList())
        val orders = mutableListOf<Order>()

        db.collection("users").document(uid).collection("portfolio")
            .get()
            .addOnSuccessListener { portfolioSnapshot ->
                val tasks = mutableListOf<Task<*>>()
                for (assetDoc in portfolioSnapshot.documents) {
                    val symbol = assetDoc.getString("symbol") ?: continue
                    val transactionsRef = assetDoc.reference.collection("transactions")
                    val task = transactionsRef.whereEqualTo("state", "waiting").get()
                        .addOnSuccessListener { transSnapshot ->
                            for (transDoc in transSnapshot.documents) {
                                val orderId = transDoc.id
                                val price = transDoc.getDouble("price") ?: continue
                                val quantity = transDoc.getDouble("quantity") ?: continue
                                val type = transDoc.getString("type") ?: continue
                                val isBuy = type == "buy"

                                orders.add(
                                    Order(
                                        orderId = orderId,
                                        symbol = symbol,
                                        price = price,
                                        quantity = quantity,
                                        isBuy = isBuy
                                    )
                                )
                            }
                        }
                    tasks.add(task)
                }
                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        onResult(orders)
                    }
            }
    }
}
