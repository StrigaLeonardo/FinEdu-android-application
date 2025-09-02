package com.example.finedu.trading

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.finedu.R
import com.example.finedu.trading.helpers.ApiUtils
import com.example.finedu.trading.helpers.AutoRefreshManager
import com.example.finedu.trading.helpers.OrderTotalCalculator
import com.example.finedu.trading.repository.DatabaseRepository
import com.example.finedu.trading.ui.*
import com.example.finedu.trading.utils.LimitOrderManager
import com.example.finedu.trading.utils.MarketOrderManager
import com.github.mikephil.charting.charts.CandleStickChart

class TradingActivity : AppCompatActivity() {

    private lateinit var chartManager: TradingChartManager
    private lateinit var orderbookManager: OrderbookManager
    private lateinit var positionsManager: PositionsManager
    private lateinit var ordersManager: OrdersManager
    private lateinit var orderEntryManager: OrderEntryManager
    private lateinit var marketOrderManager: MarketOrderManager
    private lateinit var limitOrderManager: LimitOrderManager
    private lateinit var autoRefreshManager: AutoRefreshManager

    private lateinit var spinnerSymbol: Spinner
    private lateinit var spinnerTimeframe: Spinner
    private lateinit var orderTypeGroup: RadioGroup
    private lateinit var orderSideGroup: RadioGroup
    private lateinit var etOrderAmount: EditText
    private lateinit var etOrderPrice: EditText
    private lateinit var tvTradeTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trading)

        marketOrderManager = MarketOrderManager(this)
        limitOrderManager = LimitOrderManager(this)

        spinnerSymbol = findViewById(R.id.spinnerSymbol)
        spinnerTimeframe = findViewById(R.id.spinnerTimeframe)
        orderTypeGroup = findViewById(R.id.orderTypeGroup)
        orderSideGroup = findViewById(R.id.orderSideGroup)
        etOrderAmount = findViewById(R.id.etOrderAmount)
        etOrderPrice = findViewById(R.id.etOrderPrice)
        tvTradeTotal = findViewById(R.id.tvTradeTotal)
        val chart = findViewById<CandleStickChart>(R.id.chartCandle)
        val tvCurrentPrice = findViewById<TextView>(R.id.tvCurrentPrice)
        val btnSubmitOrder = findViewById<Button>(R.id.btnSubmitOrder)
        val orderbookContainer = findViewById<LinearLayout>(R.id.orderbookContainer)
        val positionsContainer = findViewById<LinearLayout>(R.id.positionsContainer)
        val ordersContainer = findViewById<LinearLayout>(R.id.ordersContainer)

        chartManager = TradingChartManager(this, chart, tvCurrentPrice)
        orderbookManager = OrderbookManager(this, orderbookContainer)
        positionsManager = PositionsManager(this, positionsContainer)
        ordersManager = OrdersManager(this, ordersContainer) { order ->
            limitOrderManager.cancelLimitOrder(
                orderId = order.orderId,
                assetSymbol = order.symbol,
                isBuy = order.isBuy,
                amount = order.quantity,
                price = order.price
            ) { success, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (success) updateOrders()
            }
        }

        orderEntryManager = OrderEntryManager(
            this,
            orderTypeGroup,
            orderSideGroup,
            etOrderAmount,
            etOrderPrice,
            tvTradeTotal,
            btnSubmitOrder
        ) {
            tvCurrentPrice.text.toString()
                .replace("[^0-9.,]".toRegex(), "")
                .replace(",", ".")
                .toDoubleOrNull() ?: 0.0
        }

        val symbols = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "DOGEUSDT")
        val intervals = listOf("1m", "5m", "15m", "1h", "4h", "1d")
        val symbolAdapter = ArrayAdapter(this, R.layout.spinner_item_white, symbols)
        symbolAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerSymbol.adapter = symbolAdapter
        val intervalAdapter = ArrayAdapter(this, R.layout.spinner_item_white, intervals)
        intervalAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerTimeframe.adapter = intervalAdapter

        etOrderAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateOrderTotal()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        etOrderPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val amount = etOrderAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val price = etOrderPrice.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                OrderTotalCalculator.updateLimitOrderTotal(amount, price, tvTradeTotal)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        orderTypeGroup.setOnCheckedChangeListener { _, _ -> updateOrderTotal() }

        btnSubmitOrder.setOnClickListener {
            val selectedSideId = orderSideGroup.checkedRadioButtonId
            val isBuy = selectedSideId == R.id.rbBuy
            val symbol = (spinnerSymbol.selectedItem as String).replace("USDT", "")
            val amount = etOrderAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val limitPrice = etOrderPrice.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val isLimitOrder = etOrderPrice.visibility == View.VISIBLE && limitPrice > 0.0

            ApiUtils.fetchAllBinancePrices { priceMap ->
                val marketPrice = priceMap[symbol + "USDT"]?.toDouble() ?: 0.0
                if (amount <= 0.0) {
                    Toast.makeText(this, "Neispravan unos količine!", Toast.LENGTH_SHORT).show()
                    return@fetchAllBinancePrices
                }
                if (isLimitOrder) {
                    limitOrderManager.placeLimitOrder(
                        symbol = symbol,
                        amount = amount,
                        limitPrice = limitPrice,
                        isBuy = isBuy
                    ) { success, message ->
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        if (success) updateOrders()
                    }
                } else {
                    if (marketPrice <= 0.0) {
                        Toast.makeText(this, "Trenutna cijena nije dostupna!", Toast.LENGTH_SHORT).show()
                        return@fetchAllBinancePrices
                    }
                    marketOrderManager.executeMarketOrder(
                        symbol, amount, marketPrice, isBuy
                    ) { success, message ->
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        if (success) updatePositions()
                    }
                }
            }
        }

        spinnerSymbol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                chartManager.resetPrice()
                loadChart()
                loadOrderbook()
                updateOrderTotal()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinnerTimeframe.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                chartManager.resetPrice()
                loadChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadChart()
        loadOrderbook()
        updatePositions()
        updateOrders()

        autoRefreshManager = AutoRefreshManager(1000L) {
            loadChart()
            loadOrderbook()
            updatePositions()
            updateOrders()
            ApiUtils.fetchAllBinancePrices { priceMap ->
                limitOrderManager.processWaitingLimitOrders(
                    priceMap.mapValues { it.value.toDouble() }
                ) {
                    updatePositions()
                    updateOrders()
                }
            }
        }
        autoRefreshManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoRefreshManager.stop()
    }

    private fun updateOrderTotal() {
        val isLimit = orderTypeGroup.checkedRadioButtonId == R.id.rbLimit
        val symbol = (spinnerSymbol.selectedItem as String).replace("USDT", "")
        val amount = etOrderAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        val price = etOrderPrice.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

        if (isLimit) {
            etOrderPrice.visibility = View.VISIBLE
            OrderTotalCalculator.updateLimitOrderTotal(amount, price, tvTradeTotal)
        } else {
            etOrderPrice.visibility = View.GONE
            OrderTotalCalculator.updateMarketOrderTotal(amount, symbol, tvTradeTotal) { priceMap ->
                ApiUtils.fetchAllBinancePrices { prices ->
                    priceMap(prices.mapValues { it.value.toDouble() })
                }
            }
        }
    }

    private fun loadChart() {
        val symbol = spinnerSymbol.selectedItem as String
        val interval = spinnerTimeframe.selectedItem as String
        ApiUtils.fetchBinanceCandles(symbol, interval) { candleEntries, openTimes ->
            chartManager.updateChart(candleEntries, openTimes, interval)
        }
    }

    private fun loadOrderbook() {
        val symbol = spinnerSymbol.selectedItem as String
        orderbookManager.updateOrderbook(symbol)
    }

    private fun updatePositions() {
        DatabaseRepository.getAssetsWithExecutedTransactions { assetList ->
            ApiUtils.fetchAllBinancePrices { priceMap ->
                positionsManager.showPositions(assetList, priceMap)
            }
        }
    }


    private fun updateOrders() {
        DatabaseRepository.getWaitingOrders { orderList ->
            ordersManager.showOrders(orderList)
        }
    }
}
