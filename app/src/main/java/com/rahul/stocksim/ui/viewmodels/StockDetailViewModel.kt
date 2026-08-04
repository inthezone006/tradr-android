package com.rahul.stocksim.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.rahul.stocksim.data.*
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
import com.rahul.stocksim.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    private val billingRepository: BillingRepository,
    private val geminiService: GeminiService,
    private val webSocket: TwelveDataWebSocket,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val symbol: String? = savedStateHandle["symbol"]
    private val application = marketRepository.getApplicationContext()

    fun getApplicationContext() = application

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<StockPricePoint>>(emptyList())
    val history: StateFlow<List<StockPricePoint>> = _history.asStateFlow()

    private val _isGraphLoading = MutableStateFlow(false)
    val isGraphLoading: StateFlow<Boolean> = _isGraphLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("1D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist.asStateFlow()

    private val _ownedQuantity = MutableStateFlow(0L)
    val ownedQuantity: StateFlow<Long> = _ownedQuantity.asStateFlow()

    private val _priceAlerts = MutableStateFlow<List<PriceAlertEntity>>(emptyList())
    val priceAlerts: StateFlow<List<PriceAlertEntity>> = _priceAlerts.asStateFlow()

    private val _activeContracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val activeContracts: StateFlow<List<TradeContract>> = _activeContracts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    val isPro = billingRepository.isPro

    val userBalance: Flow<Double> = marketRepository.currentPortfolioId.flatMapLatest { id ->
        marketRepository.getUserBalance(id)
    }

    init {
        symbol?.let { stockSymbol ->
            viewModelScope.launch {
                marketRepository.currentPortfolioId.collect { portfolioId ->
                    _ownedQuantity.value = marketRepository.getPortfolio(portfolioId).find { it.first == stockSymbol }?.second ?: 0L
                }
            }
        }
    }

    private val _technicalIndicators = MutableStateFlow<TechnicalIndicators?>(null)
    val technicalIndicators: StateFlow<TechnicalIndicators?> = _technicalIndicators.asStateFlow()

    data class TechnicalIndicators(
        val rsi: Double?,
        val macd: TwelveDataMACDValue?,
        val bbands: TwelveDataBBandsValue?
    )

    init {
        val stockSymbol = symbol
        if (stockSymbol != null) {
            observePersistentData(stockSymbol)
            refreshGraph(_selectedPeriod.value)
            
            // Connect WebSocket for real-time prices
            webSocket.connect(marketRepository.twelveDataApiKey)
            webSocket.subscribe(listOf(stockSymbol))
            
            viewModelScope.launch {
                webSocket.priceUpdates
                    .filter { it.symbol == stockSymbol }
                    .collect { update ->
                        val currentState = _uiState.value
                        if (currentState is StockDetailUiState.Success) {
                            val updatedStock = currentState.stock.copy(
                                price = update.price,
                                // We might need to recalculate change/percentChange if available from WS
                            )
                            _uiState.value = currentState.copy(stock = updatedStock)
                        }
                    }
            }
        }
        refreshData()
    }

    override fun onCleared() {
        super.onCleared()
        symbol?.let { webSocket.unsubscribe(listOf(it)) }
    }

    private fun observePersistentData(stockSymbol: String) {
        viewModelScope.launch {
            marketRepository.getPriceAlerts(stockSymbol).collect {
                _priceAlerts.value = it
            }
        }

        viewModelScope.launch {
            marketRepository.getTradeContracts(ContractStatus.PENDING)
                .map { contracts -> contracts.filter { it.symbol == stockSymbol } }
                .collect { _activeContracts.value = it }
        }
    }

    fun refreshData() {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            // 1. Instant load from memory cache (Full Detail)
            val cached = marketRepository.getCachedFullDetail(stockSymbol)
            if (cached is StockDetailUiState.Success) {
                _uiState.value = cached
                _isRefreshing.value = true 
            } else {
                // 2. Fast load from Quote Cache (Just price/name)
                val quickQuote = marketRepository.getStockQuote(stockSymbol, skipTwelveData = true)
                if (quickQuote != null) {
                    _uiState.value = StockDetailUiState.Success(stock = quickQuote)
                    _isRefreshing.value = true
                } else {
                    _uiState.value = StockDetailUiState.Loading
                }
            }

            // Start graph loading immediately in parallel
            refreshGraph("1D")

            try {
                // 3. Fetch/Refresh the core Stock Quote (TwelveData/Finnhub)
                val stockResult = if (_uiState.value is StockDetailUiState.Success) {
                    val currentStock = (_uiState.value as StockDetailUiState.Success).stock
                    marketRepository.getStockQuote(stockSymbol) ?: currentStock
                } else {
                    marketRepository.getStockQuote(stockSymbol)
                }

                if (stockResult == null) {
                    if (_uiState.value is StockDetailUiState.Loading) {
                        _uiState.value = StockDetailUiState.Error("Stock not found")
                    }
                    _isRefreshing.value = false
                    return@launch
                }

                // Update UI with at least the stock quote if it changed or if we were loading
                if (_uiState.value !is StockDetailUiState.Success || (_uiState.value as StockDetailUiState.Success).stock.price != stockResult.price) {
                     val currentState = _uiState.value as? StockDetailUiState.Success
                     _uiState.value = currentState?.copy(stock = stockResult) ?: StockDetailUiState.Success(stock = stockResult)
                }

                // 4. Fetch all other data in parallel using async
                val profileDeferred = async { marketRepository.getCompanyProfile(stockSymbol) }
                val financialsDeferred = async { marketRepository.getBasicFinancials(stockSymbol) }
                val newsDeferred = async { marketRepository.getCompanyNews(stockSymbol) }
                val recsDeferred = async { marketRepository.getRecommendations(stockSymbol) }
                val peersDeferred = async { marketRepository.getPeers(stockSymbol) }
                val earningsDeferred = async { marketRepository.getEarningsCalendar(stockSymbol) }
                val rsiDeferred = async { marketRepository.getTechnicalIndicator(stockSymbol, "rsi") }
                val sma50Deferred = async { marketRepository.getTechnicalIndicator(stockSymbol, "sma", 50) }
                val sma200Deferred = async { marketRepository.getTechnicalIndicator(stockSymbol, "sma", 200) }
                val dividendsDeferred = async { marketRepository.getDividends(stockSymbol) }
                val marketStatusDeferred = async { marketRepository.getMarketStatus() }
                val priceTargetDeferred = async { marketRepository.getPriceTarget(stockSymbol) }
                val esgDeferred = async { 
                    if (!stockResult.isCrypto && !stockResult.isForex) 
                        marketRepository.getEsgScores(stockSymbol)
                    else null
                }
                val insidersDeferred = async { marketRepository.getInsiderTransactions(stockSymbol) }
                
                // Twelve Data Advanced
                val tdRsiDeferred = async { marketRepository.getTwelveDataRSI(stockSymbol) }
                val tdMacdDeferred = async { marketRepository.getTwelveDataMACD(stockSymbol) }
                val tdEmaDeferred = async { marketRepository.getTwelveDataEMA(stockSymbol) }
                val tdBbandsDeferred = async { marketRepository.getTwelveDataBBands(stockSymbol) }
                val tdHistoryDeferred = async { marketRepository.getTwelveDataTimeSeries(stockSymbol, "1day") }
                val socialSentimentDeferred = async { marketRepository.getSocialSentiment(stockSymbol) }

                // Wait for market data (but NOT AI Analysis yet)
                val profile = profileDeferred.await()
                val financials = financialsDeferred.await()
                val news = newsDeferred.await()
                val recs = recsDeferred.await()
                val peers = peersDeferred.await()
                val earnings = earningsDeferred.await()
                val rsiRes = rsiDeferred.await()
                val sma50Res = sma50Deferred.await()
                val sma200Res = sma200Deferred.await()
                val dividends = dividendsDeferred.await()
                val marketStatus = marketStatusDeferred.await()
                val priceTarget = priceTargetDeferred.await()
                val esg = esgDeferred.await()
                val insiders = insidersDeferred.await()
                
                val tdRsi = tdRsiDeferred.await()
                val tdMacd = tdMacdDeferred.await()
                val tdEma = tdEmaDeferred.await()
                val tdBbands = tdBbandsDeferred.await()
                val tdHistory = tdHistoryDeferred.await()
                val socialSentiment = socialSentimentDeferred.await()

                val aiRec = marketRepository.analyzeStock(
                    stock = stockResult,
                    financials = financials,
                    priceTarget = priceTarget,
                    rsi = rsiRes?.rsi?.lastOrNull(),
                    sma50 = sma50Res?.sma?.lastOrNull(),
                    sma200 = sma200Res?.sma?.lastOrNull(),
                    sentiment = null,
                    analystRecs = recs.firstOrNull(),
                    news = news
                )

                // 5. Update UI with all market data
                val mainSuccessState = StockDetailUiState.Success(
                    stock = stockResult,
                    profile = profile,
                    financials = financials,
                    newsArticles = news,
                    recommendations = recs,
                    peers = peers,
                    earnings = earnings,
                    rsiData = rsiRes,
                    sma50Data = sma50Res,
                    sma200Data = sma200Res,
                    dividends = dividends,
                    marketStatus = marketStatus,
                    esgScores = esg,
                    priceTarget = priceTarget,
                    aiRecommendation = aiRec,
                    insiderTransactions = insiders,
                    aiAnalysis = (cached as? StockDetailUiState.Success)?.aiAnalysis,
                    tdRsi = tdRsi,
                    tdMacd = tdMacd,
                    tdEma20 = tdEma.firstOrNull()?.rsi?.toDoubleOrNull(),
                    tdBbands = tdBbands.firstOrNull(),
                    candleHistory = tdHistory,
                    socialSentiment = socialSentiment
                )
                
                _uiState.value = mainSuccessState
                marketRepository.cacheFullDetail(stockSymbol, mainSuccessState)
                _isRefreshing.value = false

                // 6. Background load for persistent user data
                val portfolioId = marketRepository.currentPortfolioId.value
                _isInWatchlist.value = marketRepository.getWatchlist().any { it.symbol == stockSymbol }
                _ownedQuantity.value = marketRepository.getPortfolio(portfolioId).find { it.first == stockSymbol }?.second ?: 0L

            } catch (e: Exception) {
                _uiState.value = StockDetailUiState.Error(e.message ?: "Unknown error")
                _isRefreshing.value = false
            }
        }
    }

    fun triggerAiAnalysis() {
        val currentState = _uiState.value
        if (currentState !is StockDetailUiState.Success || currentState.aiAnalysis != null) return

        viewModelScope.launch {
            try {
                val aiAnalysis = geminiService.generateStockAnalysis(
                    currentState.stock, 
                    currentState.newsArticles, 
                    currentState.financials
                )
                marketRepository.trackAIUsage()
                val latestState = _uiState.value
                if (latestState is StockDetailUiState.Success) {
                    val updatedState = latestState.copy(aiAnalysis = aiAnalysis)
                    _uiState.value = updatedState
                    marketRepository.cacheFullDetail(currentState.stock.symbol, updatedState)
                }
            } catch (e: Exception) {
                Log.e("StockDetailViewModel", "AI Analysis failed", e)
            }
        }
    }

    fun refreshGraph(period: String) {
        val stockSymbol = symbol ?: return
        _selectedPeriod.value = period
        viewModelScope.launch {
            _isGraphLoading.value = true
            _history.value = marketRepository.getStockHistory(stockSymbol, period)
            _isGraphLoading.value = false
        }
    }

    fun toggleWatchlist() {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                marketRepository.removeFromWatchlist(stockSymbol)
                _isInWatchlist.value = false
            } else {
                marketRepository.addToWatchlist(stockSymbol)
                _isInWatchlist.value = true
            }
        }
    }

    suspend fun buyStock(quantity: Int, price: Double): Result<Double> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val portfolioId = marketRepository.currentPortfolioId.value
        val result = marketRepository.buyStock(stockSymbol, quantity, price, portfolioId)
        if (result.isSuccess) {
            _ownedQuantity.value = marketRepository.getPortfolio(portfolioId).find { it.first == stockSymbol }?.second ?: 0L
        }
        return result
    }

    suspend fun sellStock(quantity: Int, price: Double): Result<Double> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val portfolioId = marketRepository.currentPortfolioId.value
        val result = marketRepository.sellStock(stockSymbol, quantity, price, portfolioId)
        if (result.isSuccess) {
            _ownedQuantity.value = marketRepository.getPortfolio(portfolioId).find { it.first == stockSymbol }?.second ?: 0L
        }
        return result
    }

    suspend fun createContract(type: ContractType, targetPrice: Double, quantity: Long): Result<Unit> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val currentStock = (uiState.value as? StockDetailUiState.Success)?.stock
        val contract = TradeContract(
            symbol = stockSymbol,
            type = type,
            targetPrice = targetPrice,
            quantity = quantity,
            status = ContractStatus.PENDING,
            createdAt = Timestamp.now(),
            logoUrl = currentStock?.logoUrl
        )
        val res = marketRepository.createTradeContract(contract)
        return res
    }

    suspend fun buyOption(isCall: Boolean, strikePrice: Double, premium: Double, contracts: Int): Result<Unit> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val portfolioId = marketRepository.currentPortfolioId.value
        
        // Option contracts are usually 100 shares each
        val totalCost = premium * 100 * contracts
        
        // Check balance first (Wait for first value from flow)
        val balance = marketRepository.getUserBalance(portfolioId).first()
        if (balance < totalCost) return Result.failure(Exception("Insufficient balance for premium"))

        // Create the contract
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 30) // Default 30-day expiration
        
        val currentStock = (uiState.value as? StockDetailUiState.Success)?.stock
        val contract = TradeContract(
            symbol = stockSymbol,
            type = if (isCall) ContractType.CALL_OPTION else ContractType.PUT_OPTION,
            targetPrice = strikePrice,
            quantity = contracts.toLong(), // Represents number of contracts (each 100 shares)
            status = ContractStatus.PENDING,
            createdAt = Timestamp.now(),
            premium = premium,
            expirationDate = Timestamp(calendar.time),
            logoUrl = currentStock?.logoUrl
        )
        
        return marketRepository.createTradeContract(contract)
    }

    suspend fun settleOption(contract: TradeContract, currentPrice: Double): Result<Unit> {
        val result = marketRepository.settleOption(contract, currentPrice)
        if (result.isSuccess) {
            // No direct field to update like _ownedQuantity for options yet, 
            // but the balance flow will update automatically
        }
        return result
    }

    fun cancelContract(contractId: String) {
        viewModelScope.launch {
            marketRepository.cancelTradeContract(contractId)
        }
    }

    fun addPriceAlert(targetPrice: Double, isAbove: Boolean) {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            marketRepository.addPriceAlert(
                PriceAlertEntity(
                    symbol = stockSymbol,
                    targetPrice = targetPrice,
                    isAbove = isAbove
                )
            )
        }
    }

    fun deletePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            marketRepository.deletePriceAlert(alert)
        }
    }
}

sealed class StockDetailUiState {
    object Loading : StockDetailUiState()
    data class Success(
        val stock: Stock,
        val profile: FinnhubProfileResponse? = null,
        val financials: FinnhubFinancialsResponse? = null,
        val newsArticles: List<FinnhubNewsArticle> = emptyList(),
        val recommendations: List<FinnhubRecommendationResponse> = emptyList(),
        val peers: List<String> = emptyList(),
        val earnings: FinnhubEarningsCalendarResponse? = null,
        val rsiData: FinnhubIndicatorResponse? = null,
        val sma50Data: FinnhubIndicatorResponse? = null,
        val sma200Data: FinnhubIndicatorResponse? = null,
        val dividends: List<FinnhubDividendResponse> = emptyList(),
        val newsSentiment: FinnhubNewsSentimentResponse? = null,
        val marketStatus: FinnhubMarketStatusResponse? = null,
        val esgScores: FinnhubEsgResponse? = null,
        val priceTarget: FinnhubPriceTargetResponse? = null,
        val aiRecommendation: AIRecommendation? = null,
        val insiderTransactions: List<FinnhubInsiderTransaction> = emptyList(),
        val aiAnalysis: String? = null,
        val tdRsi: List<TwelveDataIndicatorValue> = emptyList(),
        val tdMacd: List<TwelveDataMACDValue> = emptyList(),
        val tdEma20: Double? = null,
        val tdBbands: TwelveDataBBandsValue? = null,
        val candleHistory: List<TwelveDataTimeSeriesValue> = emptyList(),
        val socialSentiment: Pair<Int, Int> = Pair(0, 0)
    ) : StockDetailUiState()
    data class Error(val message: String) : StockDetailUiState()
}
