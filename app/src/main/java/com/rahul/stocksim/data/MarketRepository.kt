package com.rahul.stocksim.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query as FirestoreQuery
import com.rahul.stocksim.data.local.StockDao
import com.rahul.stocksim.data.local.entity.StockEntity
import com.rahul.stocksim.data.local.entity.*
import com.rahul.stocksim.model.*
import com.rahul.stocksim.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin
import kotlin.math.abs

// Interface for Finnhub API
interface FinnhubApi {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubQuoteResponse

    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") apiKey: String
    ): FinnhubSearchResponse

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): List<FinnhubNewsArticle>

    @GET("stock/candle")
    suspend fun getStockCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse

    @GET("crypto/candle")
    suspend fun getCryptoCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse

    @GET("forex/candle")
    suspend fun getForexCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse

    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubProfileResponse

    @GET("stock/metric")
    suspend fun getBasicFinancials(
        @Query("symbol") symbol: String,
        @Query("metric") metric: String = "all",
        @Query("token") apiKey: String
    ): FinnhubFinancialsResponse

    @GET("news")
    suspend fun getMarketNews(
        @Query("category") category: String = "general",
        @Query("token") apiKey: String
    ): List<FinnhubNewsArticle>

    @GET("stock/recommendation")
    suspend fun getRecommendations(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<FinnhubRecommendationResponse>

    @GET("stock/peers")
    suspend fun getPeers(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<String>

    @GET("calendar/earnings")
    suspend fun getEarningsCalendar(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): FinnhubEarningsCalendarResponse

    @GET("indicator")
    suspend fun getTechnicalIndicator(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("indicator") indicator: String,
        @Query("token") apiKey: String,
        @Query("timeperiod") timeperiod: Int? = null
    ): FinnhubIndicatorResponse

    @GET("stock/dividend")
    suspend fun getDividends(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): List<FinnhubDividendResponse>

    @GET("calendar/ipo")
    suspend fun getIpoCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): FinnhubIpoCalendarResponse

    @GET("news-sentiment")
    suspend fun getNewsSentiment(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubNewsSentimentResponse

    @GET("forex/rates")
    suspend fun getForexRates(
        @Query("base") base: String = "USD",
        @Query("token") apiKey: String
    ): FinnhubForexRatesResponse

    @GET("stock/market-status")
    suspend fun getMarketStatus(
        @Query("exchange") exchange: String,
        @Query("token") apiKey: String
    ): FinnhubMarketStatusResponse

    @GET("stock/esg")
    suspend fun getEsgScores(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubEsgResponse

    @GET("stock/price-target")
    suspend fun getPriceTarget(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubPriceTargetResponse

    @GET("stock/earnings")
    suspend fun getEarningsSurprises(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<FinnhubEarningsSurpriseResponse>

    @GET("stock/insider-transactions")
    suspend fun getInsiderTransactions(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubInsiderResponse

    @GET("calendar/economic")
    suspend fun getEconomicCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): FinnhubEconomicCalendarResponse
}

data class FinnhubQuoteResponse(val c: Double, val d: Double, val dp: Double, val h: Double, val l: Double, val o: Double, val pc: Double)
data class FinnhubSearchResponse(val count: Int, val result: List<FinnhubSearchResult>)
data class FinnhubSearchResult(val description: String, val displaySymbol: String, val symbol: String, val type: String)
data class FinnhubSymbolResult(val description: String, val displaySymbol: String, val symbol: String)
data class FinnhubNewsArticle(val category: String, val datetime: Long, val headline: String, val id: Long, val image: String, val related: String, val source: String, val summary: String, val url: String)
data class FinnhubCandleResponse(val c: List<Double>?, val h: List<Double>?, val l: List<Double>?, val o: List<Double>?, val s: String, val t: List<Long>?, val v: List<Long>?)
data class FinnhubProfileResponse(val country: String?, val currency: String?, val exchange: String?, val name: String?, val ticker: String?, val logo: String?, val marketCapitalization: Double?, val finnhubIndustry: String?, val shareOutstanding: Double?)
data class FinnhubFinancialsResponse(val symbol: String, val metric: Map<String, Any?>?)
data class FinnhubRecommendationResponse(val buy: Int, val hold: Int, val period: String, val sell: Int, val strongBuy: Int, val strongSell: Int, val symbol: String)
data class FinnhubEarningsCalendarResponse(val earningsCalendar: List<FinnhubEarningsEntry>)
data class FinnhubEarningsEntry(val date: String, val epsActual: Double?, val epsEstimate: Double?, val hour: String, val quarter: Int, val symbol: String, val year: Int)
data class FinnhubIndicatorResponse(val rsi: List<Double>?, val macd: List<Double>?, val macdSignal: List<Double>?, val macdHist: List<Double>?, val sma: List<Double>?, val ema: List<Double>?, val s: String)
data class FinnhubDividendResponse(val symbol: String, val date: String, val amount: Double, val adjustedAmount: Double, val payDate: String, val recordDate: String, val declarationDate: String, val currency: String)
data class FinnhubIpoCalendarResponse(val ipoCalendar: List<FinnhubIpoEntry>)
data class FinnhubIpoEntry(val date: String, val exchange: String, val name: String, val numberOfShares: Long, val price: String, val status: String, val symbol: String, val totalSharesValue: Long)
data class FinnhubNewsSentimentResponse(val buzz: FinnhubBuzz?, val companyNewsScore: Double?, val sectorAverageBullishPercent: Double?, val sectorAverageNewsScore: Double?, val sentiment: FinnhubSentiment?, val symbol: String)
data class FinnhubBuzz(val articlesInLastWeek: Int?, val buzz: Double?, val weeklyAverage: Double?)
data class FinnhubSentiment(val bearishPercent: Double?, val bullishPercent: Double?)
data class FinnhubForexRatesResponse(val base: String, val code: String? = null, val quote: Map<String, Double>? = null)
data class FinnhubMarketStatusResponse(val exchange: String, val holiday: String?, val isOpen: Boolean, val session: String?, val timezone: String?)
data class FinnhubEsgResponse(val symbol: String, val totalScore: Double?, val environmentScore: Double?, val socialScore: Double?, val governanceScore: Double?, val data: Map<String, Any?>?)
data class FinnhubPriceTargetResponse(val symbol: String, val targetHigh: Double?, val targetLow: Double?, val targetMean: Double?, val targetMedian: Double?, val lastUpdate: String?)
data class FinnhubEarningsSurpriseResponse(val actual: Double?, val estimate: Double?, val period: String?, val symbol: String?, val surprise: Double?, val surprisePercent: Double?)
data class FinnhubInsiderResponse(val data: List<FinnhubInsiderTransaction>, val symbol: String)
data class FinnhubInsiderTransaction(val change: Long, val name: String, val share: Long, val transactionDate: String, val transactionPrice: Double)
data class FinnhubEconomicCalendarResponse(val economicCalendar: List<FinnhubEconomicEntry>)
data class FinnhubEconomicEntry(val actual: Double?, val country: String, val estimate: Double?, val event: String, val impact: String, val prev: Double?, val time: String, val unit: String)

data class StockPricePoint(
    val timestamp: Long,
    val price: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val volume: Long = 0
)
data class WatchlistItem(val symbol: String)
data class AIRecommendation(val advice: String, val confidence: Int, val reasons: List<String>)
enum class AssetFilter { STOCKS, CRYPTO, FOREX, OTHERS }

data class MarketInsights(
    val indices: List<Stock>,
    val sectors: List<Stock>,
    val gainers: List<Stock>,
    val losers: List<Stock>
)

@Singleton
class MarketRepository @Inject constructor(
    private val api: FinnhubApi,
    private val twelveDataApi: TwelveDataApi,
    private val stockDao: StockDao,
    private val billingRepository: BillingRepository,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics
    private val notificationHelper = NotificationHelper(context)
    private val gson = Gson()
    
    private val apiKey = "d38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0"
    val twelveDataApiKey = "e55badc51cfd4ba5b9ed060f2c048d57"
    
    val isPro = billingRepository.isPro

    private val _currentPortfolioId = MutableStateFlow("default")
    val currentPortfolioId: StateFlow<String> = _currentPortfolioId.asStateFlow()

    fun selectPortfolio(id: String) {
        _currentPortfolioId.value = id
        globalPortfolioCache = null
    }

    suspend fun getSocialSentiment(symbol: String): Pair<Int, Int> {
        return try {
            val watchlistCount = firestore.collectionGroup("watchlist")
                .whereEqualTo("symbol", symbol)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
                .count

            val portfolioCount = firestore.collectionGroup("portfolio")
                .whereEqualTo("symbol", symbol)
                .whereGreaterThan("quantity", 0)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
                .count

            Pair(watchlistCount.toInt(), portfolioCount.toInt())
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                preloadCaches()
            } catch (e: Exception) {
                Log.e("MarketRepository", "Error preloading caches", e)
            }
        }
    }

    private suspend fun preloadCaches() {
        stockDao.getAllStocks().take(1).collect { stocks ->
            stocks.forEach { stock ->
                companyNameMap[stock.symbol] = stock.name
                stock.industry?.let { industryCache[stock.symbol] = it }
                stock.logoUrl?.let { logoCache[stock.symbol] = it }
            }
        }
    }

    fun getApplicationContext() = context

    private val quoteCache = ConcurrentHashMap<String, Pair<Stock, Long>>()
    private val genericCache = ConcurrentHashMap<String, Pair<Any, Long>>()
    private val fullDetailCache = ConcurrentHashMap<String, Any>() // Fast UI-level cache
    
    fun cacheFullDetail(symbol: String, detail: Any) {
        fullDetailCache[symbol] = detail
    }

    fun getCachedFullDetail(symbol: String): Any? = fullDetailCache[symbol]

    private val CACHE_EXPIRATION_MS = 300_000L // 5 minutes
    private val LONG_CACHE_EXPIRATION_MS = 3600_000L // 1 hour
    
    private suspend fun <T : Any> getCachedOrFetch(
        key: String,
        expirationMs: Long = CACHE_EXPIRATION_MS,
        fetch: suspend () -> T?
    ): T? {
        val cached = genericCache[key]
        if (cached != null && System.currentTimeMillis() - cached.second < expirationMs) {
            @Suppress("UNCHECKED_CAST")
            return cached.first as? T
        }
        val result = fetch()
        if (result != null) {
            genericCache[key] = result to System.currentTimeMillis()
        }
        return result
    }
    
    private val apiMutex = Mutex()
    private var lastRequestTime = 0L
    private val MIN_DELAY_MS = 100L

    companion object {
        private var globalWatchlistCache: List<Stock>? = null
        private var globalPortfolioCache: List<Pair<Stock, Long>>? = null
        
        private val industryCache = ConcurrentHashMap<String, String>()
        private val logoCache = ConcurrentHashMap<String, String>()
        private val companyNameMap = ConcurrentHashMap<String, String>().apply {
            put("AAPL", "Apple Inc.")
            put("GOOGL", "Alphabet Inc.")
            put("MSFT", "Microsoft Corp.")
            put("AMZN", "Amazon.com Inc.")
            put("TSLA", "Tesla Inc.")
            put("META", "Meta Platforms Inc.")
            put("NVDA", "NVIDIA Corp.")
            put("NFLX", "Netflix Inc.")
            put("AMD", "Advanced Micro Devices Inc.")
            put("PYPL", "PayPal Holdings Inc.")
            put("INTC", "Intel Corp.")
            put("CSCO", "Cisco Systems Inc.")
            put("ADBE", "Adobe Inc.")
            put("CRM", "Salesforce Inc.")
            put("QCOM", "Qualcomm Inc.")
            put("SPY", "SPDR S&P 500 ETF Trust")
        }

        private val cryptoSymbols = setOf("BINANCE:BTCUSDT", "BINANCE:ETHUSDT", "BINANCE:XRPUSDT", "BINANCE:SOLUSDT")
        private val forexSymbols = setOf("OANDA:EUR_USD", "OANDA:GBP_USD", "OANDA:USD_JPY")
    }

    private fun recordError(e: Exception) {
        if (e is CancellationException ||
            e is java.net.SocketTimeoutException || 
            e is java.net.UnknownHostException ||
            (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE) ||
            e.message?.contains("Insufficient", ignoreCase = true) == true) {
            return
        }

        if (e is HttpException && e.code() == 401) {
            crashlytics.log("Finnhub API 401 Unauthorized: Check API Key validity. Current key: ${apiKey.take(5)}...")
            crashlytics.setCustomKey("api_auth_error", true)
        }
        crashlytics.recordException(e)
    }

    suspend fun getStockQuote(symbol: String, skipTwelveData: Boolean = false): Stock? {
        val cached = quoteCache[symbol]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRATION_MS) {
            return cached.first
        }
        
        // Try Room before network if memory is empty
        val dbStock = stockDao.getStock(symbol)?.toDomain()
        if (dbStock != null && cached == null) {
             // If we have nothing in memory, use DB stock temporarily
             quoteCache[symbol] = dbStock to System.currentTimeMillis() - (CACHE_EXPIRATION_MS / 2) // set as semi-old
             // We don't return here yet, we still want to refresh from network if it's been > 5 mins
        }

        // Try Twelve Data first for better quote detail + name
        if (!skipTwelveData) {
            val tdQuote = getTwelveDataQuote(symbol)
            if (tdQuote != null) {
                quoteCache[symbol] = tdQuote to System.currentTimeMillis()
                stockDao.insertStock(tdQuote.toEntity())
                return tdQuote
            }
        }

        // Try Finnhub as fallback
        return try {
            apiMutex.withLock {
                val reCached = quoteCache[symbol]
                if (reCached != null && System.currentTimeMillis() - reCached.second < CACHE_EXPIRATION_MS) {
                    return reCached.first
                }

                val timeSinceLast = System.currentTimeMillis() - lastRequestTime
                if (timeSinceLast < MIN_DELAY_MS) {
                    delay(MIN_DELAY_MS - timeSinceLast)
                }

                val response = api.getQuote(symbol, apiKey)
                lastRequestTime = System.currentTimeMillis()
                
                if (!companyNameMap.containsKey(symbol) || !industryCache.containsKey(symbol) || !logoCache.containsKey(symbol)) {
                    try {
                        val profileRes = api.getCompanyProfile(symbol, apiKey)
                        if (profileRes.name != null) companyNameMap[symbol] = profileRes.name
                        if (profileRes.finnhubIndustry != null) industryCache[symbol] = profileRes.finnhubIndustry!!
                        if (profileRes.logo != null) logoCache[symbol] = profileRes.logo!!
                    } catch (e: Exception) {}
                }

                val isCrypto = symbol.startsWith("BINANCE:") || cryptoSymbols.contains(symbol)
                val isForex = symbol.startsWith("OANDA:") || forexSymbols.contains(symbol)
                
                val stock = Stock(
                    symbol = symbol,
                    name = companyNameMap[symbol] ?: symbol,
                    price = response.c,
                    change = response.d,
                    percentChange = response.dp,
                    high = response.h,
                    low = response.l,
                    open = response.o,
                    prevClose = response.pc,
                    isCrypto = isCrypto,
                    isForex = isForex,
                    industry = industryCache[symbol],
                    logoUrl = logoCache[symbol]
                )
                
                // Update caches
                quoteCache[symbol] = stock to System.currentTimeMillis()
                stockDao.insertStock(stock.toEntity())
                
                stock
            }
        } catch (e: Exception) {
            recordError(e)
            // Offline fallback
            stockDao.getStock(symbol)?.toDomain() ?: cached?.first
        }
    }

    private fun Stock.toEntity() = StockEntity(
        symbol = symbol, name = name, price = price, change = change, percentChange = percentChange,
        high = high, low = low, open = open, prevClose = prevClose, isCrypto = isCrypto,
        isForex = isForex, industry = industry, logoUrl = logoUrl
    )

    private fun StockEntity.toDomain() = Stock(
        symbol = symbol, name = name, price = price, change = change, percentChange = percentChange,
        high = high, low = low, open = open, prevClose = prevClose, isCrypto = isCrypto,
        isForex = isForex, industry = industry, logoUrl = logoUrl
    )

    suspend fun getStocksQuotes(symbols: List<String>): List<Stock> = coroutineScope {
        if (symbols.isEmpty()) return@coroutineScope emptyList()
        
        // Eagerly fetch logos for symbols that don't have them in cache yet
        val missingLogos = symbols.filter { !logoCache.containsKey(it) }
        if (missingLogos.isNotEmpty()) {
            launch {
                missingLogos.forEach { symbol ->
                    try {
                        val profile = api.getCompanyProfile(symbol, apiKey)
                        profile.logo?.let { logoCache[symbol] = it }
                        profile.name?.let { companyNameMap[symbol] = it }
                        profile.finnhubIndustry?.let { industryCache[symbol] = it }
                        delay(200) // Small delay to avoid hitting rate limits too fast
                    } catch (e: Exception) {
                        Log.e("MarketRepository", "Error fetching profile for $symbol during eager load", e)
                    }
                }
            }
        }

        // Use Twelve Data Batching for efficiency
        val tdQuotes = getTwelveDataBatchQuotes(symbols)
        
        if (tdQuotes.size == symbols.size) {
            return@coroutineScope tdQuotes
        }

        // Fallback or fill gaps with Finnhub if Twelve Data misses some or fails
        val missingSymbols = symbols.filter { symbol -> tdQuotes.none { it.symbol == symbol } }
        
        if (missingSymbols.isEmpty()) return@coroutineScope tdQuotes

        val finnhubQuotes = missingSymbols.map { symbol ->
            async {
                try {
                    withTimeout(15000) {
                        getStockQuote(symbol, skipTwelveData = true)
                    }
                } catch (e: Exception) {
                    Log.e("MarketRepository", "Error fetching quote for $symbol", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
        
        tdQuotes + finnhubQuotes
    }

    suspend fun getWatchlistWithQuotes(forceRefresh: Boolean = false): List<Stock> {
        if (!forceRefresh && globalWatchlistCache != null) {
            return globalWatchlistCache!!
        }
        val symbols = getWatchlist().map { it.symbol }
        if (symbols.isEmpty()) return emptyList()
        return try {
            val stocks = getStocksQuotes(symbols)
            if (stocks.isNotEmpty()) globalWatchlistCache = stocks
            globalWatchlistCache ?: emptyList()
        } catch (e: Exception) {
            recordError(e)
            globalWatchlistCache ?: emptyList()
        }
    }

    suspend fun getPortfolioWithQuotes(forceRefresh: Boolean = false, portfolioId: String = "default"): List<Pair<Stock, Long>> {
        if (!forceRefresh && portfolioId == "default" && globalPortfolioCache != null) return globalPortfolioCache!!
        return try {
            val rawPortfolio = getPortfolio(portfolioId)
            if (rawPortfolio.isEmpty()) return emptyList()
            val stocks = getStocksQuotes(rawPortfolio.map { it.first })
            val portfolioWithQuotes = rawPortfolio.mapNotNull { (symbol, qty) ->
                val stock = stocks.find { it.symbol == symbol }
                if (stock != null) stock to qty else null
            }
            if (portfolioWithQuotes.isNotEmpty() && portfolioId == "default") {
                globalPortfolioCache = portfolioWithQuotes
                if (portfolioWithQuotes.size >= 5) {
                    updateAchievementProgress("diversified", 1f)
                }
                
                // Track Sector Specialist: Own stocks in 3+ industries
                val industries = portfolioWithQuotes.mapNotNull { it.first.industry }.distinct()
                if (industries.size >= 3) {
                    updateAchievementProgress("diversified_sector", 1f)
                }
            }
            portfolioWithQuotes
        } catch (e: Exception) {
            recordError(e)
            if (portfolioId == "default") globalPortfolioCache ?: emptyList() else emptyList()
        }
    }

    suspend fun getStockHistory(symbol: String, period: String): List<StockPricePoint> {
        val cached = stockDao.getStockHistory(symbol, period)
        val expiration = if (period == "1D") 300_000L else 3600_000L // 5 mins for 1D, 1 hour others
        if (cached != null && System.currentTimeMillis() - cached.lastUpdated < expiration) {
            val type = object : TypeToken<List<StockPricePoint>>() {}.type
            return gson.fromJson(cached.historyJson, type)
        }

        val to = Instant.now().epochSecond
        val from = when (period) {
            "1D" -> to - (24 * 3600)
            "5D" -> to - (5 * 24 * 3600)
            "1W" -> to - (7 * 24 * 3600)
            "1M" -> to - (30 * 24 * 3600)
            "6M" -> to - (180 * 24 * 3600)
            "1Y" -> to - (365 * 24 * 3600)
            "5Y" -> to - (5 * 365 * 24 * 3600)
            else -> to - (30 * 24 * 3600)
        }
        val resolution = when (period) {
            "1D" -> "15"
            "5D" -> "60"
            "1W" -> "60"
            "1M" -> "D"
            "6M" -> "D"
            "1Y" -> "W"
            "5Y" -> "M"
            else -> "D"
        }

        val isCrypto = symbol.startsWith("BINANCE:") || cryptoSymbols.contains(symbol)
        val isForex = symbol.startsWith("OANDA:") || forexSymbols.contains(symbol)

        val history = try {
            val response = when {
                isCrypto -> api.getCryptoCandles(symbol, resolution, from, to, apiKey)
                isForex -> api.getForexCandles(symbol, resolution, from, to, apiKey)
                else -> api.getStockCandles(symbol, resolution, from, to, apiKey)
            }
            if (response.s == "ok" && !response.c.isNullOrEmpty() && !response.t.isNullOrEmpty()) {
                response.t.indices.map { i ->
                    StockPricePoint(
                        timestamp = response.t[i],
                        price = response.c[i],
                        open = response.o?.getOrNull(i) ?: response.c[i],
                        high = response.h?.getOrNull(i) ?: response.c[i],
                        low = response.l?.getOrNull(i) ?: response.c[i],
                        volume = response.v?.getOrNull(i) ?: 0L
                    )
                }.sortedBy { it.timestamp }
            } else {
                // Fallback to simulation if no data or status not ok
                getStockQuote(symbol)?.let { generateSimulatedPoints(symbol, FinnhubQuoteResponse(it.price, it.change, it.percentChange, it.high, it.low, it.open, it.prevClose), to, period) } ?: emptyList()
            }
        } catch (e: Exception) {
            recordError(e)
            // Fallback to simulation on network error
            getStockQuote(symbol)?.let { generateSimulatedPoints(symbol, FinnhubQuoteResponse(it.price, it.change, it.percentChange, it.high, it.low, it.open, it.prevClose), to, period) } ?: emptyList()
        }

        if (history.isNotEmpty()) {
            stockDao.insertStockHistory(StockHistoryEntity(symbol, period, gson.toJson(history)))
        } else if (cached != null) {
            val type = object : TypeToken<List<StockPricePoint>>() {}.type
            return gson.fromJson(cached.historyJson, type)
        }
        
        return history
    }

    private fun generateSimulatedPoints(symbol: String, quote: FinnhubQuoteResponse, endTime: Long, period: String): List<StockPricePoint> {
        val points = mutableListOf<StockPricePoint>()
        
        // Use available quote data for variance
        val high = if (quote.h > 0) quote.h else quote.c * 1.05
        val low = if (quote.l > 0) quote.l else quote.c * 0.95
        val open = if (quote.o > 0) quote.o else quote.pc.takeIf { it > 0 } ?: (quote.c * 0.98)
        val close = quote.c
        
        val steps = when(period) {
            "1D" -> 24 
            "5D" -> 40 // 8 points per day for 5 days
            "1W" -> 35
            "1M" -> 30 
            "6M" -> 120 // More points for 6M
            "1Y" -> 52 
            "5Y" -> 60 
            else -> 30
        }

        val intervalSeconds = when(period) { 
            "1D" -> 3600L 
            "5D" -> (3600L * 24 * 5) / steps
            "1W" -> (3600L * 24 * 7) / steps
            "1M" -> (3600L * 24 * 30) / steps
            "6M" -> (3600L * 24 * 180) / steps
            "1Y" -> (3600L * 24 * 365) / steps
            "5Y" -> (3600L * 24 * 365 * 5) / steps
            else -> 3600L * 24
        }

        // Use a stable seed based on symbol and period to keep history consistent across refreshes
        val seed = symbol.hashCode().toLong() + period.hashCode().toLong()
        val random = Random(seed)

        if (period == "1D") {
            // For 1D charts, strictly follow Open -> (High/Low) -> Close path to touch all key stats
            val isHighFirst = random.nextBoolean()
            val step1 = steps / 3
            val step2 = 2 * steps / 3
            
            val keySteps = listOf(0, step1, step2, steps)
            val keyPrices = if (isHighFirst) listOf(open, high, low, close) else listOf(open, low, high, close)

            for (i in 0 until 3) {
                val sStart = keySteps[i]
                val sEnd = keySteps[i+1]
                val pStart = keyPrices[i]
                val pEnd = keyPrices[i+1]
                
                for (step in sStart until sEnd) {
                    val progress = (step - sStart).toDouble() / (sEnd - sStart)
                    val trend = pStart + (pEnd - pStart) * progress
                    // Add some jitter but keep within daily bounds
                    val noise = (random.nextDouble() - 0.5) * (high - low) * 0.15
                    val price = (trend + noise).coerceIn(low, high)
                    
                    points.add(StockPricePoint(
                        timestamp = endTime - ((steps - step) * intervalSeconds),
                        price = price,
                        open = price * (1 + (random.nextDouble() - 0.5) * 0.002),
                        high = price * (1 + random.nextDouble() * 0.002),
                        low = price * (1 - random.nextDouble() * 0.002),
                        volume = (1000..50000).random().toLong()
                    ))
                }
            }
            // Add final point exactly at current price
            points.add(StockPricePoint(
                timestamp = endTime,
                price = close,
                open = close,
                high = close,
                low = close,
                volume = (1000..50000).random().toLong()
            ))
        } else {
            // For other periods, use a trend-based simulation
            // For 5D/1M/6M/1Y, the daily 'open' is not the correct starting point for the whole period.
            // We'll estimate a starting price based on a random walk backwards.
            val periodVariance = when(period) {
                "5D" -> 0.05
                "1W" -> 0.07
                "1M" -> 0.12
                "6M" -> 0.25
                "1Y" -> 0.40
                "5Y" -> 0.80
                else -> 0.10
            }
            
            val startPrice = close * (1.0 + (random.nextDouble() - 0.5) * periodVariance)
            
            for (i in 0..steps) {
                val timestamp = endTime - ((steps - i) * intervalSeconds)
                val progress = i.toDouble() / steps
                
                // Add some sinusoidal movement for more realistic stock behavior
                val sineWave = sin(progress * Math.PI * 2) * (close * periodVariance * 0.2)
                val trend = startPrice + (close - startPrice) * progress
                val noise = (random.nextDouble() - 0.5) * close * (periodVariance * 0.1)
                val price = (trend + sineWave + noise).coerceAtLeast(0.01)
                
                points.add(StockPricePoint(
                    timestamp = timestamp,
                    price = if (i == steps) close else price,
                    open = price * (1 + (random.nextDouble() - 0.5) * 0.005),
                    high = price * (1 + random.nextDouble() * 0.005),
                    low = price * (1 - random.nextDouble() * 0.005),
                    volume = (1000..50000).random().toLong()
                ))
            }
        }

        return points
    }

    suspend fun getCompanyNews(symbol: String): List<FinnhubNewsArticle> = getCachedOrFetch("company_news_$symbol", 1800_000L) {
        try {
            val localNews = stockDao.getCompanyNews(symbol)
            if (localNews.isNotEmpty() && System.currentTimeMillis() - localNews.first().lastUpdated < 3600_000L) {
                localNews.map { it.toDomain() }
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val today = sdf.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val remoteNews = api.getCompanyNews(symbol, sdf.format(calendar.time), today, apiKey).take(5)
                
                if (remoteNews.isNotEmpty()) {
                    stockDao.insertNews(remoteNews.map { it.toEntity(symbol) })
                }
                remoteNews
            }
        } catch (e: Exception) {
            recordError(e)
            stockDao.getCompanyNews(symbol).map { it.toDomain() }
        }
    } ?: emptyList()

    suspend fun searchStocks(query: String, filter: AssetFilter = AssetFilter.STOCKS): List<Stock> = coroutineScope {
        try {
            val response = api.searchSymbol(query, apiKey)
            val filteredResults = response.result.filter { result ->
                when (filter) {
                    AssetFilter.STOCKS -> (result.type == "Common Stock" || result.type == "ADR" || result.type == "ETF") && result.symbol.all { it.isLetter() }
                    AssetFilter.CRYPTO -> result.symbol.startsWith("BINANCE:") || cryptoSymbols.contains(result.symbol)
                    AssetFilter.FOREX -> result.symbol.startsWith("OANDA:") || forexSymbols.contains(result.symbol)
                    AssetFilter.OTHERS -> true
                }
            }.take(10)
            filteredResults.map { result ->
                companyNameMap[result.symbol] = result.description
                getStockQuote(result.symbol)?.let { quote ->
                    val isCrypto = result.symbol.startsWith("BINANCE:") || cryptoSymbols.contains(result.symbol)
                    val isForex = result.symbol.startsWith("OANDA:") || forexSymbols.contains(result.symbol)
                    Stock(
                        symbol = result.symbol,
                        name = result.description,
                        price = quote.price,
                        change = quote.change,
                        percentChange = quote.percentChange,
                        high = quote.high,
                        low = quote.low,
                        open = quote.open,
                        prevClose = quote.prevClose,
                        isCrypto = isCrypto,
                        isForex = isForex,
                        industry = industryCache[result.symbol]
                    )
                }
            }.filterNotNull()
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    private val DEFAULT_WATCHLIST = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX", "SPY")

    suspend fun getWatchlist(): List<WatchlistItem> {
        val userId = auth.currentUser?.uid ?: return DEFAULT_WATCHLIST.map { WatchlistItem(it) }
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("watchlist").get().await()
            if (snapshot.isEmpty) {
                DEFAULT_WATCHLIST.map { WatchlistItem(it) }
            } else {
                snapshot.documents.map { WatchlistItem(it.getString("symbol") ?: "") }
            }
        } catch (e: Exception) {
            recordError(e)
            DEFAULT_WATCHLIST.map { WatchlistItem(it) }
        }
    }

    suspend fun addToWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        
        if (!billingRepository.isPro.value) {
            val currentWatchlist = getWatchlist()
            if (currentWatchlist.size >= 10) {
                return Result.failure(Exception("Watchlist limit reached for Free users. Upgrade to Pro for unlimited watchlist!"))
            }
        }

        return try {
            firestore.collection("users").document(userId).collection("watchlist").document(symbol).set(mapOf("symbol" to symbol)).await()
            analytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, Bundle().apply { putString(FirebaseAnalytics.Param.ITEM_ID, symbol) })
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).collection("watchlist").document(symbol).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun createTradeContract(contract: TradeContract): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val docRef = firestore.collection("users").document(userId).collection("contracts").document()
            val contractWithId = contract.copy(id = docRef.id, userId = userId)
            docRef.set(contractWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    fun getTradeContracts(statuses: List<ContractStatus>): Flow<List<TradeContract>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }
        val statusStrings = statuses.map { it.name }
        if (statusStrings.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val listener = firestore.collection("users").document(userId).collection("contracts")
            .whereIn("status", statusStrings)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(emptyList())
                    } else {
                        recordError(error)
                        close(error)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val contracts = snapshot.toObjects(TradeContract::class.java)
                        .sortedByDescending { it.createdAt }
                    trySend(contracts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getTradeContracts(status: ContractStatus = ContractStatus.PENDING): Flow<List<TradeContract>> = 
        getTradeContracts(listOf(status))

    suspend fun getPendingTradeContractsForCurrentUser(): List<TradeContract> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("contracts")
                .whereEqualTo("status", ContractStatus.PENDING.name)
                .get().await()
            snapshot.toObjects(TradeContract::class.java)
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    suspend fun updateTradeContract(contract: TradeContract): Result<Unit> {
        return try {
            firestore.collection("users").document(contract.userId).collection("contracts").document(contract.id)
                .set(contract).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun cancelTradeContract(contractId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).collection("contracts").document(contractId)
                .update("status", ContractStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun settleOption(contract: TradeContract, currentPrice: Double, userId: String? = null): Result<Unit> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        val isCall = contract.type == ContractType.CALL_OPTION
        val strike = contract.targetPrice
        val intrinsicValue = if (isCall) {
            (currentPrice - strike).coerceAtLeast(0.0)
        } else {
            (strike - currentPrice).coerceAtLeast(0.0)
        }
        
        val totalSettlement = intrinsicValue * 100 * contract.quantity
        val newStatus = if (totalSettlement > 0) ContractStatus.EXECUTED else ContractStatus.EXPIRED
        
        return try {
            val userRef = firestore.collection("users").document(targetUserId)
            val contractRef = userRef.collection("contracts").document(contract.id)
            
            firestore.runTransaction { transaction ->
                // Update balance if there's any value to settle
                if (totalSettlement > 0) {
                    val currentBalance = (transaction.get(userRef).get("balance") as? Number)?.toDouble() ?: 0.0
                    transaction.update(userRef, "balance", currentBalance + totalSettlement)
                }
                
                // Mark contract as executed or expired
                transaction.update(contractRef, "status", newStatus.name)
                null
            }.await()
            
            globalPortfolioCache = null
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun getPortfolios(): List<Portfolio> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("portfolios").get().await()
            val portfolios = snapshot.toObjects(Portfolio::class.java).toMutableList()
            if (portfolios.none { it.id == "default" }) {
                portfolios.add(Portfolio(id = "default", name = "Main Portfolio", isDefault = true))
            }
            portfolios.sortedByDescending { it.isDefault }
        } catch (e: Exception) {
            recordError(e)
            listOf(Portfolio(id = "default", name = "Main Portfolio", isDefault = true))
        }
    }

    suspend fun createPortfolio(name: String): Result<Portfolio> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        if (!billingRepository.isPro.value) {
            return Result.failure(Exception("Upgrade to Pro to create multiple portfolios!"))
        }
        
        return try {
            val docRef = firestore.collection("users").document(userId).collection("portfolios").document()
            val portfolio = Portfolio(id = docRef.id, name = name, isDefault = false)
            docRef.set(portfolio).await()
            Result.success(portfolio)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun buyStock(symbol: String, quantity: Int, pricePerShare: Double, portfolioId: String = "default", userId: String? = null): Result<Double> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalCost = quantity * pricePerShare
        return try {
            var newBalance = 0.0
            val userRef = firestore.collection("users").document(targetUserId)
            val portfolioRef = if (portfolioId == "default") {
                userRef.collection("portfolio").document(symbol)
            } else {
                userRef.collection("portfolios").document(portfolioId).collection("holdings").document(symbol)
            }

            firestore.runTransaction { transaction ->
                val currentBalance = (transaction.get(userRef).get("balance") as? Number)?.toDouble() ?: 0.0
                val portfolioDoc = transaction.get(portfolioRef)
                if (currentBalance >= totalCost) {
                    newBalance = currentBalance - totalCost
                    transaction.update(userRef, "balance", newBalance)
                    if (portfolioDoc.exists()) {
                        transaction.update(portfolioRef, "quantity", ((portfolioDoc.get("quantity") as? Number)?.toLong() ?: 0L) + quantity)
                    } else {
                        transaction.set(portfolioRef, mapOf(
                            "quantity" to quantity.toLong(),
                            "symbol" to symbol,
                            "purchaseDate" to FieldValue.serverTimestamp()
                        ))
                    }
                    null
                } else throw Exception("Insufficient balance")
            }.await()
            
            // Track Achievements
            updateAchievementProgress("first_trade", 1f)
            if (totalCost >= 10000.0) {
                updateAchievementProgress("big_spender", 1f)
            }
            
            // Track Risk Taker: Buying stock down > 10%
            getStockQuote(symbol)?.let { quote ->
                if (quote.percentChange <= -10.0) {
                    updateAchievementProgress("risk_taker", 1f)
                }
            }
            
            analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply { putString(FirebaseAnalytics.Param.CURRENCY, "USD"); putDouble(FirebaseAnalytics.Param.VALUE, totalCost); putString(FirebaseAnalytics.Param.TRANSACTION_ID, symbol) })
            globalPortfolioCache = null
            Result.success(newBalance)
        } catch (e: Exception) {
            recordError(e); Result.failure(e)
        }
    }

    suspend fun updateAchievementProgress(id: String, progress: Float) {
        val current = stockDao.getAchievement(id)
        if (current == null) {
            stockDao.insertAchievement(AchievementEntity(id = id, progress = progress, isUnlocked = progress >= 1f, unlockedAt = if (progress >= 1f) System.currentTimeMillis() else null))
        } else if (!current.isUnlocked) {
            val newProgress = (current.progress + progress).coerceAtMost(1f)
            val unlocked = newProgress >= 1f
            stockDao.updateAchievement(current.copy(
                progress = newProgress,
                isUnlocked = unlocked,
                unlockedAt = if (unlocked) System.currentTimeMillis() else null
            ))
            
            if (unlocked) {
                // Notify user
                val achievement = ALL_ACHIEVEMENTS.find { it.id == id }
                achievement?.let {
                    notificationHelper.showNotification(
                        "Achievement Unlocked!",
                        "${it.icon} ${it.title}: ${it.description}"
                    )
                }
            }
        }
    }

    fun getAchievements(): Flow<List<Achievement>> = stockDao.getAllAchievements().map { entities ->
        ALL_ACHIEVEMENTS.map { template ->
            val entity = entities.find { it.id == template.id }
            template.copy(
                isUnlocked = entity?.isUnlocked ?: false,
                progress = entity?.progress ?: 0f,
                unlockedAt = entity?.unlockedAt
            )
        }
    }

    suspend fun sellStock(symbol: String, quantity: Int, pricePerShare: Double, portfolioId: String = "default", userId: String? = null): Result<Double> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalGain = quantity * pricePerShare
        return try {
            val userRef = firestore.collection("users").document(targetUserId)
            val portfolioRef = if (portfolioId == "default") {
                userRef.collection("portfolio").document(symbol)
            } else {
                userRef.collection("portfolios").document(portfolioId).collection("holdings").document(symbol)
            }
            
            var resultDays = -1L
            var resultInstantSell = false
            var newBalance = 0.0

            firestore.runTransaction { transaction ->
                val portfolioDoc = transaction.get(portfolioRef)
                val currentQty = (portfolioDoc.get("quantity") as? Number)?.toLong() ?: 0L
                val purchaseDate = portfolioDoc.getTimestamp("purchaseDate")
                
                if (currentQty >= quantity) {
                    val currentBalance = (transaction.get(userRef).get("balance") as? Number)?.toDouble() ?: 0.0
                    newBalance = currentBalance + totalGain
                    transaction.update(userRef, "balance", newBalance)
                    transaction.update(portfolioRef, "quantity", currentQty - quantity)
                    
                    purchaseDate?.let { date ->
                        val holdTimeMillis = System.currentTimeMillis() - date.toDate().time
                        resultDays = holdTimeMillis / (1000 * 60 * 60 * 24)
                        resultInstantSell = holdTimeMillis < (24 * 60 * 60 * 1000)
                    }

                    null
                } else throw Exception("Insufficient quantity")
            }.await()
            
            if (resultDays >= 7) updateAchievementProgress("diamond_hands", 1f)
            else if (resultInstantSell) updateAchievementProgress("paper_hands", 1f)

            // Track Achievements: Bull Runner (Profit on sale)
            getStockQuote(symbol)?.let { quote ->
                if (quote.percentChange >= 10.0) {
                    updateAchievementProgress("bull_runner", 1f)
                }
            }

            globalPortfolioCache = null
            Result.success(newBalance)
        } catch (e: Exception) {
            recordError(e); Result.failure(e)
        }
    }

    suspend fun getTotalPortfoliosValue(): Double {
        val portfolios = getPortfolios()
        var total = 0.0
        portfolios.forEach { portfolio ->
            val holdings = getPortfolioWithQuotes(forceRefresh = false, portfolioId = portfolio.id)
            total += holdings.sumOf { it.first.price * it.second }
        }
        return total
    }

    suspend fun getTwelveDataQuote(symbol: String): Stock? {
        return try {
            val jsonElement = twelveDataApi.getQuote(symbol, twelveDataApiKey)
            val gson = Gson()
            val quote = if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("symbol")) {
                gson.fromJson(jsonElement, TwelveDataQuote::class.java)
            } else if (jsonElement.isJsonObject) {
                // It might be a map with one entry
                val mapType = object : TypeToken<HashMap<String, TwelveDataQuote>>() {}.type
                val map: HashMap<String, TwelveDataQuote> = gson.fromJson(jsonElement, mapType)
                map[symbol] ?: map.values.firstOrNull()
            } else null

            quote?.let { response ->
                Stock(
                    symbol = response.symbol,
                    name = response.name ?: symbol,
                    price = response.close?.toDoubleOrNull() ?: 0.0,
                    change = response.change?.toDoubleOrNull() ?: 0.0,
                    percentChange = response.percent_change?.toDoubleOrNull() ?: 0.0,
                    high = response.high?.toDoubleOrNull() ?: 0.0,
                    low = response.low?.toDoubleOrNull() ?: 0.0,
                    open = response.open?.toDoubleOrNull() ?: 0.0,
                    prevClose = response.previous_close?.toDoubleOrNull() ?: 0.0,
                    isCrypto = symbol.contains(":"),
                    isForex = symbol.contains("/"),
                    industry = industryCache[symbol]
                )
            }
        } catch (e: Exception) {
            recordError(e)
            null
        }
    }

    suspend fun getTwelveDataBatchQuotes(symbols: List<String>): List<Stock> {
        if (symbols.isEmpty()) return emptyList()
        return try {
            val symbolString = symbols.joinToString(",")
            val jsonElement = twelveDataApi.getQuote(symbolString, twelveDataApiKey)
            val gson = Gson()
            
            // Check for API error status first
            if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("status") && 
                jsonElement.asJsonObject.get("status").asString == "error") {
                val message = jsonElement.asJsonObject.get("message")?.asString ?: "Unknown Twelve Data Error"
                Log.e("MarketRepository", "Twelve Data API Error: $message")
                return emptyList()
            }

            val responseMap = if (jsonElement.isJsonObject && !jsonElement.asJsonObject.has("symbol")) {
                val mapType = object : TypeToken<HashMap<String, TwelveDataQuote>>() {}.type
                gson.fromJson<HashMap<String, TwelveDataQuote>>(jsonElement, mapType)
            } else if (jsonElement.isJsonObject) {
                val quote = gson.fromJson(jsonElement, TwelveDataQuote::class.java)
                mapOf(quote.symbol to quote)
            } else emptyMap()

            val stocks = responseMap.map { (symbol, response) ->
                response.name?.let { companyNameMap[symbol] = it }
                val isCrypto = symbol.contains(":") || symbol.contains("/") || cryptoSymbols.contains(symbol)
                val isForex = symbol.contains("/") && !isCrypto || forexSymbols.contains(symbol)
                Stock(
                    symbol = symbol,
                    name = response.name ?: symbol,
                    price = response.close?.toDoubleOrNull() ?: 0.0,
                    change = response.change?.toDoubleOrNull() ?: 0.0,
                    percentChange = response.percent_change?.toDoubleOrNull() ?: 0.0,
                    high = response.high?.toDoubleOrNull() ?: 0.0,
                    low = response.low?.toDoubleOrNull() ?: 0.0,
                    open = response.open?.toDoubleOrNull() ?: 0.0,
                    prevClose = response.previous_close?.toDoubleOrNull() ?: 0.0,
                    isCrypto = isCrypto,
                    isForex = isForex,
                    industry = industryCache[symbol],
                    logoUrl = logoCache[symbol]
                )
            }
            
            if (stocks.isNotEmpty()) {
                stockDao.insertStocks(stocks.map { it.toEntity() })
            }
            stocks
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    suspend fun getTwelveDataRSI(symbol: String, interval: String = "1day"): List<TwelveDataIndicatorValue> {
        return getCachedOrFetch("rsi_$symbol$interval", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getRSI(symbol, interval, apiKey = twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e)
                null
            }
        } ?: emptyList()
    }

    suspend fun getTwelveDataMACD(symbol: String, interval: String = "1day"): List<TwelveDataMACDValue> {
        return getCachedOrFetch("macd_$symbol$interval", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getMACD(symbol, interval, apiKey = twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e)
                null
            }
        } ?: emptyList()
    }

    suspend fun getTwelveDataEMA(symbol: String, interval: String = "1day", timePeriod: Int = 20): List<TwelveDataIndicatorValue> {
        return getCachedOrFetch("ema_$symbol$interval$timePeriod", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getEMA(symbol, interval, timePeriod, twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e); null
            }
        } ?: emptyList()
    }

    suspend fun getTwelveDataSMA(symbol: String, interval: String = "1day", timePeriod: Int = 50): List<TwelveDataIndicatorValue> {
        return getCachedOrFetch("sma_$symbol$interval$timePeriod", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getSMA(symbol, interval, timePeriod, twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e); null
            }
        } ?: emptyList()
    }

    suspend fun getTwelveDataBBands(symbol: String, interval: String = "1day"): List<TwelveDataBBandsValue> {
        return getCachedOrFetch("bbands_$symbol$interval", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getBollingerBands(symbol, interval, apiKey = twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e); null
            }
        } ?: emptyList()
    }

    suspend fun getTwelveDataTimeSeries(symbol: String, interval: String, outputSize: Int = 50): List<TwelveDataTimeSeriesValue> {
        return getCachedOrFetch("timeseries_$symbol$interval$outputSize", LONG_CACHE_EXPIRATION_MS) {
            try {
                val response = twelveDataApi.getTimeSeries(symbol, interval, outputSize, twelveDataApiKey)
                response.values
            } catch (e: Exception) {
                recordError(e); null
            }
        } ?: emptyList()
    }

    fun getUserBalance(): Flow<Double> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(0.0)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0.0)
                    recordError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val balance = (snapshot.get("balance") as? Number)?.toDouble() ?: 0.0
                    trySend(balance)
                } else {
                    trySend(0.0)
                }
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        if (e is Exception) recordError(e)
        emit(0.0)
    }

    suspend fun syncTotalAccountValue(value: Double): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).update(mapOf("totalAccountValue" to value, "lastSync" to FieldValue.serverTimestamp())).await()
            
            // Track Achievement: Whale
            if (value >= 250000.0) {
                updateAchievementProgress("whale", 1f)
            }

            Result.success(Unit)
        } catch (e: Exception) { recordError(e); Result.failure(e) }
    }

    suspend fun saveAccountValueHistory(userId: String, value: Double) {
        try {
            val historyRef = firestore.collection("users").document(userId).collection("account_history")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // 1. Get existing history to handle the backfilling logic
            val snapshot = historyRef.orderBy("timestamp", FirestoreQuery.Direction.DESCENDING).limit(10).get().await()
            val existingDocs = snapshot.documents
            
            if (existingDocs.isEmpty()) {
                // Initial Backfill: Create 30 days of history with the current value
                val batch = firestore.batch()
                val calendar = Calendar.getInstance()
                
                for (i in 0..29) {
                    val timestamp = Timestamp(calendar.time)
                    val dateStr = sdf.format(calendar.time)
                    val docRef = historyRef.document(dateStr)
                    batch.set(docRef, mapOf(
                        "value" to value,
                        "timestamp" to timestamp
                    ))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                batch.commit().await()
                Log.d("MarketRepository", "Initial 30-day backfill completed for user $userId")
            } else {
                // Regular Daily Update
                val today = sdf.format(Date())
                historyRef.document(today).set(
                    mapOf(
                        "value" to value,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Prune history to keep only the last 31 entries (today + 30 history days)
                if (existingDocs.size > 31) {
                    val oldestDocs = historyRef.orderBy("timestamp").limit((existingDocs.size - 31).toLong()).get().await()
                    for (doc in oldestDocs.documents) {
                        doc.reference.delete()
                    }
                }
            }
        } catch (e: Exception) {
            recordError(e)
        }
    }

    fun getAccountValueHistory(): Flow<List<Pair<Long, Double>>> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        var snapshot = firestore.collection("users").document(userId)
            .collection("account_history")
            .orderBy("timestamp")
            .get().await()
        
        if (snapshot.isEmpty) {
            val balance = getUserBalance().first()
            saveAccountValueHistory(userId, balance)
            snapshot = firestore.collection("users").document(userId)
                .collection("account_history")
                .orderBy("timestamp")
                .get().await()
        }
        
        val history = snapshot.documents.mapNotNull { doc ->
            val value = (doc.get("value") as? Number)?.toDouble() ?: return@mapNotNull null
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: return@mapNotNull null
            timestamp to value
        }
        emit(history)
    }.catch { e ->
        if (e is Exception) recordError(e)
        emit(emptyList())
    }

    fun getPortfolioHistory(): Flow<List<StockPricePoint>> = getAccountValueHistory().map { points ->
        points.map { StockPricePoint(it.first / 1000, it.second) }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getPortfolio(portfolioId: String = "default"): List<Pair<String, Long>> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val colRef = if (portfolioId == "default") {
            firestore.collection("users").document(userId).collection("portfolio")
        } else {
            firestore.collection("users").document(userId).collection("portfolios").document(portfolioId).collection("holdings")
        }
        
        return try {
            colRef.get().await().documents.map { 
                it.getString("symbol").orEmpty() to ((it.get("quantity") as? Number)?.toLong() ?: 0L) 
            }
        } catch (e: Exception) { recordError(e); emptyList() }
    }

    suspend fun getCompanyProfile(symbol: String): FinnhubProfileResponse? = try {
        val cached = stockDao.getCompanyDetails(symbol)
        if (cached?.profileJson != null && System.currentTimeMillis() - cached.lastUpdated < 86400000L) {
            val profile = gson.fromJson(cached.profileJson, FinnhubProfileResponse::class.java)
            profile.logo?.let { logoCache[symbol] = it }
            profile.name?.let { companyNameMap[symbol] = it }
            profile
        } else {
            val remote = api.getCompanyProfile(symbol, apiKey)
            if (remote != null) {
                remote.logo?.let { logoCache[symbol] = it }
                remote.name?.let { companyNameMap[symbol] = it }
                val current = cached ?: CompanyDetailsEntity(symbol)
                stockDao.insertCompanyDetails(current.copy(profileJson = gson.toJson(remote), lastUpdated = System.currentTimeMillis()))
            }
            remote
        }
    } catch (e: Exception) { recordError(e); null }

    suspend fun getBasicFinancials(symbol: String): FinnhubFinancialsResponse? = try {
        val cached = stockDao.getCompanyDetails(symbol)
        if (cached?.financialsJson != null && System.currentTimeMillis() - cached.lastUpdated < 86400000L) {
            gson.fromJson(cached.financialsJson, FinnhubFinancialsResponse::class.java)
        } else {
            val remote = api.getBasicFinancials(symbol, "all", apiKey)
            if (remote != null) {
                val current = cached ?: CompanyDetailsEntity(symbol)
                stockDao.insertCompanyDetails(current.copy(financialsJson = gson.toJson(remote), lastUpdated = System.currentTimeMillis()))
            }
            remote
        }
    } catch (e: Exception) { recordError(e); null }

    suspend fun getMarketNews(): List<FinnhubNewsArticle> = getCachedOrFetch("market_news", 600_000L) { // 10 min memory cache
        try { 
            val localNews = stockDao.getGeneralNews()
            if (localNews.isNotEmpty() && System.currentTimeMillis() - localNews.first().lastUpdated < 1800_000L) {
                localNews.map { it.toDomain() }
            } else {
                val remoteNews = api.getMarketNews("general", apiKey).take(10)
                if (remoteNews.isNotEmpty()) {
                    stockDao.insertNews(remoteNews.map { it.toEntity(null) })
                }
                remoteNews
            }
        } catch (e: Exception) { 
            recordError(e)
            stockDao.getGeneralNews().map { it.toDomain() }
        }
    } ?: emptyList()

    suspend fun getRecommendations(symbol: String): List<FinnhubRecommendationResponse> = try {
        val cached = stockDao.getCompanyDetails(symbol)
        if (cached?.recommendationsJson != null && System.currentTimeMillis() - cached.lastUpdated < 86400000L) {
            val type = object : TypeToken<List<FinnhubRecommendationResponse>>() {}.type
            gson.fromJson(cached.recommendationsJson, type)
        } else {
            val remote = api.getRecommendations(symbol, apiKey)
            val current = cached ?: CompanyDetailsEntity(symbol)
            stockDao.insertCompanyDetails(current.copy(recommendationsJson = gson.toJson(remote), lastUpdated = System.currentTimeMillis()))
            remote
        }
    } catch (e: Exception) { recordError(e); emptyList() }

    suspend fun getPeers(symbol: String): List<String> = try {
        val cached = stockDao.getCompanyDetails(symbol)
        if (cached?.peersJson != null && System.currentTimeMillis() - cached.lastUpdated < 86400000L) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(cached.peersJson, type)
        } else {
            val remote = api.getPeers(symbol, apiKey)
            val current = cached ?: CompanyDetailsEntity(symbol)
            stockDao.insertCompanyDetails(current.copy(peersJson = gson.toJson(remote), lastUpdated = System.currentTimeMillis()))
            remote
        }
    } catch (e: Exception) { recordError(e); emptyList() }

    suspend fun getInsiderTransactions(symbol: String): List<FinnhubInsiderTransaction> = try { 
        api.getInsiderTransactions(symbol, apiKey).data.take(20) 
    } catch (e: Exception) { recordError(e); emptyList() }

    private fun FinnhubNewsArticle.toEntity(symbol: String?) = NewsEntity(
        id = id, symbol = symbol, category = category, datetime = datetime, headline = headline,
        image = image, related = related, source = source, summary = summary, url = url
    )

    private fun NewsEntity.toDomain() = FinnhubNewsArticle(
        category = category, datetime = datetime, headline = headline, id = id,
        image = image, related = related, source = source, summary = summary, url = url
    )

    suspend fun trackAIUsage() {
        updateAchievementProgress("ai_enthusiast", 0.2f)
    }

    private var cachedMarketInsights: MarketInsights? = null
    private var lastInsightsFetch: Long = 0L

    suspend fun getMarketInsights(): MarketInsights {
        val now = System.currentTimeMillis()
        if (cachedMarketInsights != null && now - lastInsightsFetch < CACHE_EXPIRATION_MS) {
            return cachedMarketInsights!!
        }

        val indexSymbols = listOf("SPY", "QQQ", "DIA", "IWM")
        val sectorSymbols = listOf("XLK", "XLF", "XLV", "XLY", "XLP", "XLE", "XLI", "XLB", "XLRE", "XLU")
        val moverCandidates = listOf(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX", 
            "AMD", "PYPL", "INTC", "CSCO", "ADBE", "CRM", "QCOM", "JPM", "V", "MA", "WMT", "PG"
        )
        
        val allSymbols = (indexSymbols + sectorSymbols + moverCandidates).distinct()
        val allQuotes = getStocksQuotes(allSymbols)
        
        val indices = indexSymbols.mapNotNull { sym -> allQuotes.find { it.symbol == sym } }
        val sectors = sectorSymbols.mapNotNull { sym -> 
            allQuotes.find { it.symbol == sym }?.copy(name = getSectorName(sym))
        }.sortedByDescending { it.percentChange }
        
        val sortedMovers = allQuotes.filter { moverCandidates.contains(it.symbol) }
            .sortedByDescending { it.percentChange }
            
        val gainers = sortedMovers.take(10)
        val losers = sortedMovers.takeLast(10).reversed()
        
        val insights = MarketInsights(indices, sectors, gainers, losers)
        cachedMarketInsights = insights
        lastInsightsFetch = now
        return insights
    }
    
    private fun getSectorName(symbol: String): String = when(symbol) {
        "XLK" -> "Technology"
        "XLF" -> "Financials"
        "XLV" -> "Health Care"
        "XLY" -> "Consumer Discretionary"
        "XLP" -> "Consumer Staples"
        "XLE" -> "Energy"
        "XLI" -> "Industrials"
        "XLB" -> "Materials"
        "XLRE" -> "Real Estate"
        "XLU" -> "Utilities"
        else -> symbol
    }

    suspend fun getEconomicCalendar(): List<FinnhubEconomicEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val nextWeek = sdf.format(calendar.time)
        return try { 
            api.getEconomicCalendar(today, nextWeek, apiKey).economicCalendar 
        } catch (e: Exception) { recordError(e); emptyList() }
    }

    suspend fun getEarningsCalendar(symbol: String): FinnhubEarningsCalendarResponse? = getCachedOrFetch("earnings_$symbol", LONG_CACHE_EXPIRATION_MS) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.MONTH, 6)
        try { 
            // Try Twelve Data first
            val tdResponse = twelveDataApi.getEarningsCalendar(symbol, twelveDataApiKey)
            if (tdResponse.status == "ok" && tdResponse.data != null) {
                return@getCachedOrFetch FinnhubEarningsCalendarResponse(
                    earningsCalendar = tdResponse.data.map { entry ->
                        FinnhubEarningsEntry(
                            date = entry.date,
                            epsActual = entry.eps_actual?.toDoubleOrNull(),
                            epsEstimate = entry.eps_estimate?.toDoubleOrNull(),
                            hour = entry.time ?: "",
                            quarter = 0, // Not provided by TD in this endpoint easily
                            symbol = entry.symbol,
                            year = entry.date.take(4).toIntOrNull() ?: 0
                        )
                    }
                )
            }
            api.getEarningsCalendar(symbol, today, sdf.format(calendar.time), apiKey) 
        } catch (e: Exception) { 
            // Fallback to Finnhub on any TD failure (like 403)
            try { api.getEarningsCalendar(symbol, today, sdf.format(calendar.time), apiKey) } catch (e2: Exception) { recordError(e2); null }
        }
    }

    suspend fun getTechnicalIndicator(symbol: String, indicator: String, timeperiod: Int? = null): FinnhubIndicatorResponse? = getCachedOrFetch("indicator_${symbol}_${indicator}_$timeperiod", CACHE_EXPIRATION_MS) {
        val to = Instant.now().epochSecond
        try { api.getTechnicalIndicator(symbol, "D", to - (365 * 24 * 3600), to, indicator, apiKey, timeperiod) } catch (e: Exception) { recordError(e); null }
    }

    suspend fun getDividends(symbol: String): List<FinnhubDividendResponse> = getCachedOrFetch("dividends_$symbol", LONG_CACHE_EXPIRATION_MS) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.YEAR, -1)
        try { 
            // Try Twelve Data first
            val tdResponse = twelveDataApi.getDividendsCalendar(symbol, twelveDataApiKey)
            if (tdResponse.status == "ok" && tdResponse.data != null) {
                return@getCachedOrFetch tdResponse.data.map { entry ->
                    FinnhubDividendResponse(
                        symbol = entry.symbol,
                        date = entry.date,
                        amount = entry.amount?.toDoubleOrNull() ?: 0.0,
                        adjustedAmount = entry.amount?.toDoubleOrNull() ?: 0.0,
                        payDate = entry.payable_date ?: "",
                        recordDate = "",
                        declarationDate = "",
                        currency = entry.currency ?: "USD"
                    )
                }
            }
            api.getDividends(symbol, sdf.format(calendar.time), today, apiKey) 
        } catch (e: Exception) { 
            try { api.getDividends(symbol, sdf.format(calendar.time), today, apiKey) } catch (e2: Exception) { recordError(e2); emptyList() }
        }
    } ?: emptyList()

    suspend fun getIpoCalendar(): List<FinnhubIpoEntry> = getCachedOrFetch("ipo_calendar", LONG_CACHE_EXPIRATION_MS) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.MONTH, 1)
        try { 
            // Try Twelve Data first
            val tdResponse = twelveDataApi.getIpoCalendar(twelveDataApiKey)
            if (tdResponse.status == "ok" && tdResponse.data != null) {
                return@getCachedOrFetch tdResponse.data.map { entry ->
                    FinnhubIpoEntry(
                        date = entry.date,
                        exchange = entry.exchange ?: "",
                        name = entry.name ?: "",
                        numberOfShares = entry.shares_offered?.toLongOrNull() ?: 0L,
                        price = entry.price_range ?: "",
                        status = "expected",
                        symbol = entry.symbol,
                        totalSharesValue = 0L
                    )
                }
            }
            api.getIpoCalendar(today, sdf.format(calendar.time), apiKey).ipoCalendar 
        } catch (e: Exception) { 
            try { api.getIpoCalendar(today, sdf.format(calendar.time), apiKey).ipoCalendar } catch (e2: Exception) { recordError(e2); emptyList() }
        }
    } ?: emptyList()

    suspend fun getNewsSentiment(symbol: String): FinnhubNewsSentimentResponse? = getCachedOrFetch("sentiment_$symbol", CACHE_EXPIRATION_MS) {
        try { api.getNewsSentiment(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    }
    suspend fun getForexRates(base: String = "USD"): FinnhubForexRatesResponse? = getCachedOrFetch("forex_rates_$base", CACHE_EXPIRATION_MS) {
        try { api.getForexRates(base, apiKey) } catch (e: Exception) { recordError(e); null }
    }
    suspend fun getMarketStatus(exchange: String = "US"): FinnhubMarketStatusResponse? = getCachedOrFetch("market_status_$exchange", CACHE_EXPIRATION_MS) {
        try { api.getMarketStatus(exchange, apiKey) } catch (e: Exception) { recordError(e); null }
    }
    suspend fun getEsgScores(symbol: String): FinnhubEsgResponse? = getCachedOrFetch("esg_$symbol", LONG_CACHE_EXPIRATION_MS) {
        try { api.getEsgScores(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    }
    suspend fun getPriceTarget(symbol: String): FinnhubPriceTargetResponse? = getCachedOrFetch("price_target_$symbol", LONG_CACHE_EXPIRATION_MS) {
        try { api.getPriceTarget(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    }
    suspend fun getEarningsSurprises(symbol: String): List<FinnhubEarningsSurpriseResponse> = getCachedOrFetch("surprises_$symbol", LONG_CACHE_EXPIRATION_MS) {
        try { api.getEarningsSurprises(symbol, apiKey) } catch (e: Exception) { recordError(e); emptyList() }
    } ?: emptyList()

    fun getPriceAlerts(symbol: String): Flow<List<PriceAlertEntity>> {
        return stockDao.getAlertsForStock(symbol)
    }

    suspend fun addPriceAlert(alert: PriceAlertEntity) {
        stockDao.insertPriceAlert(alert)
    }

    suspend fun deletePriceAlert(alert: PriceAlertEntity) {
        stockDao.deletePriceAlert(alert)
    }

    fun analyzeStock(
        stock: Stock,
        financials: FinnhubFinancialsResponse?,
        priceTarget: FinnhubPriceTargetResponse?,
        rsi: Double?,
        sma50: Double?,
        sma200: Double?,
        sentiment: FinnhubNewsSentimentResponse?,
        analystRecs: FinnhubRecommendationResponse?,
        news: List<FinnhubNewsArticle>
    ): AIRecommendation {
        val reasons = mutableListOf<String>()
        var confidence = 50
        
        // Price vs Target
        priceTarget?.targetMean?.let { target ->
            if (stock.price < target) {
                reasons.add("Trading below analyst mean target of $${String.format(Locale.US, "%.2f", target)}.")
                confidence += 10
            } else {
                reasons.add("Trading above analyst mean target of $${String.format(Locale.US, "%.2f", target)}.")
                confidence -= 10
            }
        }

        // RSI
        rsi?.let {
            if (it > 70) {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, indicating overbought conditions.")
                confidence -= 15
            } else if (it < 30) {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, indicating oversold conditions.")
                confidence += 15
            } else {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, which is in a neutral range.")
            }
        }

        // SMA
        if (sma50 != null && sma200 != null) {
            if (sma50 > sma200) {
                reasons.add("Golden cross detected: 50-day SMA is above 200-day SMA.")
                confidence += 10
            } else {
                reasons.add("Death cross detected: 50-day SMA is below 200-day SMA.")
                confidence -= 10
            }
        }

        // Valuation
        val metrics = financials?.metric
        val pe = metrics?.get("peBasicExclExtraTTM") as? Double
        if (pe != null) {
            if (pe > 25) {
                reasons.add("P/E Ratio of ${String.format(Locale.US, "%.1f", pe)} suggests high valuation.")
                confidence -= 5
            } else if (pe < 15) {
                reasons.add("P/E Ratio of ${String.format(Locale.US, "%.1f", pe)} suggests potential undervaluation.")
                confidence += 5
            }
        }

        val advice = when {
            confidence >= 70 -> "Strong Buy"
            confidence >= 60 -> "Buy"
            confidence <= 30 -> "Strong Sell"
            confidence <= 40 -> "Sell"
            else -> "Hold"
        }

        return AIRecommendation(
            advice = advice,
            confidence = confidence.coerceIn(0, 100),
            reasons = if (reasons.isEmpty()) listOf("Insufficient data for detailed analysis.") else reasons
        )
    }
}
