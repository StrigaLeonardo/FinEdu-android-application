package com.example.finedu.trading.helpers

import android.os.Handler
import android.os.Looper

class AutoRefreshManager(
    private val intervalMillis: Long = 1000L,
    private val onTick: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, intervalMillis)
        }
    }

    fun start() {
        handler.post(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
    }
}
