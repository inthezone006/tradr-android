package com.rahul.stocksim.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.*
import com.rahul.stocksim.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _portfolios = MutableStateFlow<List<Portfolio>>(listOf(Portfolio(id = "default", name = "Main Portfolio")))
    val portfolios: StateFlow<List<Portfolio>> = _portfolios.asStateFlow()

    val selectedPortfolioId: StateFlow<String> = marketRepository.currentPortfolioId

    val userBalance: Flow<Double> = selectedPortfolioId.flatMapLatest { id ->
        marketRepository.getUserBalance(id)
    }

    private val _contracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val contracts: StateFlow<List<TradeContract>> = _contracts.asStateFlow()

    private val _executedContracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val executedContracts: StateFlow<List<TradeContract>> = _executedContracts.asStateFlow()

    private val _portfolioHistory = MutableStateFlow<List<StockPricePoint>>(emptyList())
    val portfolioHistory: StateFlow<List<StockPricePoint>> = _portfolioHistory.asStateFlow()
    
    val isPro = marketRepository.isPro

    init {
        loadPortfolios()
        loadContracts()
        loadExecutedContracts()
        
        // Reactively load data whenever the selected portfolio changes
        viewModelScope.launch {
            selectedPortfolioId.collectLatest { id ->
                loadData(false)
                loadHistory()
            }
        }
    }

    fun loadPortfolios() {
        viewModelScope.launch {
            marketRepository.getPortfolios().collect {
                _portfolios.value = it
            }
        }
    }

    fun selectPortfolio(portfolioId: String) {
        marketRepository.selectPortfolio(portfolioId)
        // loadData(true) is now handled automatically by the selectedPortfolioId observer in init
    }

    suspend fun createPortfolio(name: String, initialBalance: Double): Result<Portfolio> {
        val result = marketRepository.createPortfolio(name, initialBalance)
        if (result.isSuccess) {
            marketRepository.selectPortfolio(result.getOrThrow().id)
        }
        return result
    }

    fun deletePortfolio(portfolioId: String) {
        viewModelScope.launch {
            val result = marketRepository.deletePortfolio(portfolioId)
            if (result.isSuccess) {
                loadPortfolios()
            }
        }
    }

    fun renamePortfolio(portfolioId: String, newName: String) {
        viewModelScope.launch {
            marketRepository.renamePortfolio(portfolioId, newName)
        }
    }

    fun loadContracts() {
        viewModelScope.launch {
            marketRepository.getTradeContracts(ContractStatus.PENDING).collect {
                _contracts.value = it
            }
        }
    }

    fun loadExecutedContracts() {
        viewModelScope.launch {
            marketRepository.getTradeContracts(listOf(ContractStatus.EXECUTED, ContractStatus.CANCELLED, ContractStatus.EXPIRED)).collect {
                _executedContracts.value = it
            }
        }
    }

    fun healContracts(contracts: List<TradeContract>) {
        viewModelScope.launch {
            contracts.forEach { contract ->
                if (contract.status == ContractStatus.PENDING && contract.expirationDate != null) {
                    if (System.currentTimeMillis() > contract.expirationDate.toDate().time) {
                        // Settle if it's expired but still pending
                        marketRepository.getStockQuote(contract.symbol)?.let { quote ->
                            marketRepository.settleOption(contract, quote.price)
                        }
                    }
                }
            }
        }
    }

    fun cancelContract(contractId: String) {
        viewModelScope.launch {
            marketRepository.cancelTradeContract(contractId)
            loadContracts()
        }
    }

    fun closeOptionPosition(contract: TradeContract) {
        viewModelScope.launch {
            marketRepository.getStockQuote(contract.symbol)?.let { quote ->
                marketRepository.settleOption(contract, quote.price)
                loadContracts()
                loadExecutedContracts()
            }
        }
    }

    fun loadHistory() {
        val portfolioId = selectedPortfolioId.value
        viewModelScope.launch {
            marketRepository.getPortfolioHistory(portfolioId).collect {
                _portfolioHistory.value = it
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData(true)
            loadContracts()
            loadExecutedContracts()
            loadHistory()
            _isRefreshing.value = false
        }
    }

    private fun loadData(forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val portfolioId = marketRepository.currentPortfolioId.value
                val portfolioItems = marketRepository.getPortfolioWithQuotes(forceRefresh, portfolioId)
                
                // 1. Calculate current real-time values
                val balance = marketRepository.getUserBalance(portfolioId).first()
                val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
                val totalAccountValue = balance + totalStockValue
                
                _uiState.value = PortfolioUiState.Success(portfolioItems)
                
                // 2. Sync to Firestore if valid and it's the main portfolio
                if (totalAccountValue > 0 && portfolioId == "default") {
                    marketRepository.syncTotalAccountValue(totalAccountValue)
                    
                    // 3. Update the local graph state immediately so it ends at the current value
                    val currentHistory = _portfolioHistory.value.toMutableList()
                    val now = System.currentTimeMillis() / 1000
                    
                    // Remove any existing point for "today" (seconds within last hour approx) 
                    if (currentHistory.isNotEmpty() && now - currentHistory.last().timestamp < 3600) {
                        currentHistory.removeAt(currentHistory.size - 1)
                    }
                    currentHistory.add(StockPricePoint(now, totalAccountValue))
                    _portfolioHistory.value = currentHistory
                }
            } catch (e: Exception) {
                _uiState.value = PortfolioUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun triggerAiPortfolioAnalysis() {
        val currentState = _uiState.value
        if (currentState !is PortfolioUiState.Success || currentState.aiAnalysis != null) return

        viewModelScope.launch {
            try {
                val balance = userBalance.first()
                val analysis = geminiService.analyzePortfolio(currentState.portfolioItems, balance)
                val latestState = _uiState.value
                if (latestState is PortfolioUiState.Success) {
                    _uiState.value = latestState.copy(aiAnalysis = analysis)
                }
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "AI Portfolio Analysis failed", e)
            }
        }
    }
}

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(
        val portfolioItems: List<Pair<Stock, Long>>, 
        val contracts: List<TradeContract> = emptyList(),
        val aiAnalysis: PortfolioAnalysis? = null
    ) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}
