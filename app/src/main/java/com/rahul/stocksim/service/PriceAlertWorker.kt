package com.rahul.stocksim.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.local.StockDao
import com.rahul.stocksim.model.ContractStatus
import com.rahul.stocksim.model.ContractType
import com.rahul.stocksim.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class PriceAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MarketRepository,
    private val stockDao: StockDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val alerts = stockDao.getActivePriceAlerts()
            val contracts = repository.getPendingTradeContractsForCurrentUser()
            
            if (alerts.isEmpty() && contracts.isEmpty()) {
                return@withContext Result.success()
            }

            val notificationHelper = NotificationHelper(applicationContext)

            // Handle Price Alerts
            for (alert in alerts) {
                val stock = repository.getStockQuote(alert.symbol)
                if (stock != null) {
                    val triggered = if (alert.isAbove) {
                        stock.price >= alert.targetPrice
                    } else {
                        stock.price <= alert.targetPrice
                    }

                    if (triggered) {
                        val direction = if (alert.isAbove) "above" else "below"
                        notificationHelper.showNotification(
                            title = "Price Alert: ${alert.symbol}",
                            message = "${stock.name} has reached $${stock.price}, which is $direction your target of $${alert.targetPrice}.",
                            notificationId = alert.id
                        )
                        // Disable alert after it triggers to avoid spamming
                        stockDao.updatePriceAlert(alert.copy(isEnabled = false))
                    }
                }
            }

            // Handle Trade Contracts (Limit Orders \u0026 Options)
            for (contract in contracts) {
                val stock = repository.getStockQuote(contract.symbol)
                if (stock != null) {
                    var isTriggered = false
                    var message = ""
                    var title = ""

                    when (contract.type) {
                        ContractType.BUY_AT -> {
                            if (stock.price <= contract.targetPrice) {
                                isTriggered = true
                                val result = repository.buyStock(contract.symbol, contract.quantity.toInt(), stock.price, contract.userId)
                                if (result.isSuccess) {
                                    title = "Limit Order Executed: ${contract.symbol}"
                                    message = "Bought ${contract.quantity} shares at $${stock.price} (Target: $${contract.targetPrice})"
                                    repository.updateTradeContract(contract.copy(status = ContractStatus.EXECUTED))
                                }
                            }
                        }
                        ContractType.SELL_AT -> {
                            if (stock.price >= contract.targetPrice) {
                                isTriggered = true
                                val result = repository.sellStock(contract.symbol, contract.quantity.toInt(), stock.price, contract.userId)
                                if (result.isSuccess) {
                                    title = "Limit Order Executed: ${contract.symbol}"
                                    message = "Sold ${contract.quantity} shares at $${stock.price} (Target: $${contract.targetPrice})"
                                    repository.updateTradeContract(contract.copy(status = ContractStatus.EXECUTED))
                                }
                            }
                        }
                        ContractType.CALL_OPTION, ContractType.PUT_OPTION -> {
                            val now = Timestamp.now()
                            if (contract.expirationDate != null && now.seconds >= contract.expirationDate.seconds) {
                                isTriggered = true
                                val result = repository.settleOption(contract, stock.price, contract.userId)
                                if (result.isSuccess) {
                                    val isCall = contract.type == ContractType.CALL_OPTION
                                    val diff = if (isCall) stock.price - contract.targetPrice else contract.targetPrice - stock.price
                                    val profit = if (diff > 0) diff * 100 * contract.quantity else 0.0
                                    
                                    if (profit > 0) {
                                        title = "${if (isCall) "Call" else "Put"} Option Expired ITM: ${contract.symbol}"
                                        message = "Your ${contract.quantity} contract(s) expired in the money! Profit: $${String.format(Locale.US, "%.2f", profit)}"
                                    } else {
                                        title = "Option Expired: ${contract.symbol}"
                                        message = "Your ${contract.quantity} contract(s) expired worthless."
                                    }
                                }
                            }
                        }
                    }

                    if (isTriggered && title.isNotEmpty()) {
                        if (message.contains("worthless")) {
                            repository.updateTradeContract(contract.copy(status = ContractStatus.EXPIRED))
                        }
                        notificationHelper.showNotification(
                            title = title,
                            message = message,
                            notificationId = contract.id.hashCode()
                        )
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Error in background work", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "PriceAlertWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(
                15, TimeUnit.MINUTES // Minimum interval for periodic work
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
