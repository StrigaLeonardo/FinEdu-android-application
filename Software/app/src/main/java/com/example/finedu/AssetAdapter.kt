package com.example.finedu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finedu.model.Asset
import com.example.finedu.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

sealed class PortfolioListItem {
    data class AssetItem(val asset: Asset, val isExpanded: Boolean = false) : PortfolioListItem()
    data class TransactionItem(val transaction: Transaction) : PortfolioListItem()
}

class AssetAdapter(
    assets: List<Asset>,
    private var transactionsMap: Map<String, List<Transaction>>,
    private var priceMap: Map<String, Double>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Prikazuj SAMO assete s netQuantity > 0 (filtrirano u PortfolioActivity)
    private var items: MutableList<PortfolioListItem> =
        assets.map { PortfolioListItem.AssetItem(it) }.toMutableList()
    private var expandedAssetId: String? = null

    companion object {
        private const val VIEW_TYPE_ASSET = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is PortfolioListItem.AssetItem -> VIEW_TYPE_ASSET
        is PortfolioListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_ASSET) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_asset, parent, false)
            AssetViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            TransactionViewHolder(view)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PortfolioListItem.AssetItem -> (holder as AssetViewHolder).bind(item, position)
            is PortfolioListItem.TransactionItem -> (holder as TransactionViewHolder).bind(item.transaction)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun calculateAvgBuyPrice(transactions: List<Transaction>): Double {
        val totalBuy = transactions.filter { it.type == "buy" }.sumOf { it.quantity }
        val totalSell = transactions.filter { it.type == "sell" }.sumOf { it.quantity }
        val qtyLeft = totalBuy - totalSell
        if (qtyLeft <= 0) return 0.0
        var qty = 0.0
        var total = 0.0
        for (tx in transactions.filter { it.type == "buy" }) {
            val takeQty = minOf(tx.quantity, qtyLeft - qty)
            total += takeQty * tx.price
            qty += takeQty
            if (qty >= qtyLeft) break
        }
        return if (qty > 0) total / qty else 0.0
    }

    inner class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAssetSymbol: TextView = itemView.findViewById(R.id.tvAssetSymbol)
        private val tvAssetQuantity: TextView = itemView.findViewById(R.id.tvAssetQuantity)
        private val tvCurrentPrice: TextView = itemView.findViewById(R.id.tvCurrentPrice)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val tvGainLoss: TextView = itemView.findViewById(R.id.tvGainLoss)
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)

        fun bind(item: PortfolioListItem.AssetItem, position: Int) {
            val asset = item.asset
            tvAssetSymbol.text = asset.symbol

            val currentPrice = when (asset.type) {
                "crypto" -> priceMap[asset.symbol + "USDT"] ?: 0.0
                "stock" -> priceMap[asset.symbol] ?: 0.0
                else -> 0.0
            }
            tvCurrentPrice.text = "$%.2f".format(currentPrice)

            val transactions = transactionsMap[asset.id] ?: emptyList()
            val totalBuy = transactions.filter { it.type == "buy" }.sumOf { it.quantity }
            val totalSell = transactions.filter { it.type == "sell" }.sumOf { it.quantity }
            val netQuantity = totalBuy - totalSell
            tvAssetQuantity.text = "%.2f".format(netQuantity)
            val currentValue = netQuantity * currentPrice
            tvCurrentValue.text = "$%.2f".format(currentValue)

            val avgBuyPrice = calculateAvgBuyPrice(transactions)
            val gainLoss = (currentPrice - avgBuyPrice) * netQuantity
            val gainLossPercent = if (avgBuyPrice > 0.0 && netQuantity > 0.0)
                (gainLoss / (avgBuyPrice * netQuantity)) * 100 else 0.0

            tvGainLoss.text = "%+.2f (%+.2f%%)".format(gainLoss, gainLossPercent)
            tvGainLoss.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (gainLoss >= 0) R.color.gain_green else R.color.loss_red
                )
            )

            ivMore.setOnClickListener {
                handleAssetClick(adapterPosition, asset)
            }
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)

        fun bind(transaction: Transaction) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            tvDate.text = transaction.date?.toDate()?.let { dateFormat.format(it) } ?: ""
            tvQuantity.text = "%.2f".format(transaction.quantity)
            tvPrice.text = "%.2f".format(transaction.price)
            tvType.text = when (transaction.type) {
                "buy" -> "Kupnja"
                "sell" -> "Prodaja"
                else -> "Nepoznato"
            }
            tvType.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    when (transaction.type) {
                        "buy" -> R.color.gain_green
                        "sell" -> R.color.loss_red
                        else -> R.color.primary_text
                    }
                )
            )
        }
    }

    private fun handleAssetClick(position: Int, asset: Asset) {
        var actualPosition = position
        expandedAssetId?.let { prevId ->
            val prevIndex = items.indexOfFirst {
                it is PortfolioListItem.AssetItem && it.asset.id == prevId
            }
            if (prevIndex != -1 && prevIndex != actualPosition) {
                collapseAsset(prevIndex)
                if (actualPosition > prevIndex) {
                    val prevTransactions = transactionsMap[prevId] ?: emptyList()
                    actualPosition -= prevTransactions.size
                }
            }
        }
        val isExpanded = (items[actualPosition] as PortfolioListItem.AssetItem).isExpanded
        if (isExpanded) {
            collapseAsset(actualPosition)
            expandedAssetId = null
        } else {
            expandAsset(actualPosition, asset)
            expandedAssetId = asset.id
        }
        notifyDataSetChanged()
    }

    private fun expandAsset(position: Int, asset: Asset) {
        val transactions = transactionsMap[asset.id] ?: emptyList()
        val transactionItems = transactions.map { PortfolioListItem.TransactionItem(it) }
        items[position] = (items[position] as PortfolioListItem.AssetItem).copy(isExpanded = true)
        items.addAll(position + 1, transactionItems)
    }

    private fun collapseAsset(position: Int) {
        items[position] = (items[position] as PortfolioListItem.AssetItem).copy(isExpanded = false)
        var removeCount = 0
        for (i in position + 1 until items.size) {
            if (items[i] is PortfolioListItem.TransactionItem) removeCount++ else break
        }
        repeat(removeCount) { items.removeAt(position + 1) }
    }

    fun updateData(
        newAssets: List<Asset>,
        newTransactionsMap: Map<String, List<Transaction>>,
        newPriceMap: Map<String, Double>
    ) {
        items = newAssets.map { PortfolioListItem.AssetItem(it) }.toMutableList()
        transactionsMap = newTransactionsMap
        priceMap = newPriceMap
        expandedAssetId = null
        notifyDataSetChanged()
    }
}
