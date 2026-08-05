package com.rahul.stocksim.data.local

import androidx.room.*
import com.rahul.stocksim.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStock(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>): List<Long>

    @Query("DELETE FROM stocks")
    suspend fun clearAllStocks(): Int

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements(): Int

    @Query("DELETE FROM price_alerts")
    suspend fun clearPriceAlerts(): Int

    @Transaction
    suspend fun clearUserData() {
        clearAchievements()
        clearPriceAlerts()
    }

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1")
    suspend fun getActivePriceAlerts(): List<PriceAlertEntity>

    @Query("SELECT * FROM price_alerts WHERE symbol = :symbol")
    fun getAlertsForStock(symbol: String): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updatePriceAlert(alert: PriceAlertEntity): Int

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlertEntity): Int

    // News Caching
    @Query("SELECT * FROM news WHERE symbol IS NULL ORDER BY datetime DESC LIMIT 20")
    suspend fun getGeneralNews(): List<NewsEntity>

    @Query("SELECT * FROM news WHERE symbol = :symbol ORDER BY datetime DESC LIMIT 10")
    suspend fun getCompanyNews(symbol: String): List<NewsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    // History Caching
    @Query("SELECT * FROM stock_history WHERE symbol = :symbol AND period = :period")
    suspend fun getStockHistory(symbol: String, period: String): StockHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockHistory(history: StockHistoryEntity)

    // Company Details Caching
    @Query("SELECT * FROM company_details WHERE symbol = :symbol")
    suspend fun getCompanyDetails(symbol: String): CompanyDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyDetails(details: CompanyDetailsEntity)
}
