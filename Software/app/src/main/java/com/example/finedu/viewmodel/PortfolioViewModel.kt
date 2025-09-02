package com.example.finedu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.finedu.repository.PriceRepository
import com.example.finedu.model.Asset


class PortfolioViewModel(
    private val priceRepository: PriceRepository
) : ViewModel() {
    private val _portfolioValue = MutableStateFlow(0.0)
    val portfolioValue = _portfolioValue.asStateFlow()

    fun updatePortfolio(assets: List<Asset>, userBalance: Double) {
        viewModelScope.launch {
            var total = userBalance
            assets.forEach { asset ->
                val (currentPrice, _) = priceRepository.getAssetPrices(asset)
                total += asset.quantity * currentPrice

            }
            _portfolioValue.value = total
        }
    }
}