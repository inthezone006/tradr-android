package com.rahul.stocksim.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.rahul.stocksim.data.MarketRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyHistoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MarketRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = repository.getCurrentUserId() ?: return@withContext Result.success()
            
            // Calculate total account value
            val balance = repository.getUserBalance().first()
            val totalStockValue = repository.getTotalPortfoliosValue()
            val totalAccountValue = balance + totalStockValue

            if (totalAccountValue > 0) {
                repository.saveAccountValueHistory(userId, totalAccountValue)
                Log.d("DailyHistoryWorker", "Saved daily snapshot: $totalAccountValue")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyHistoryWorker", "Error saving daily history", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyHistoryWork"

        fun schedule(context: Context) {
            // Schedule to run once every 24 hours
            val request = PeriodicWorkRequestBuilder<DailyHistoryWorker>(
                24, TimeUnit.HOURS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
