package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.BillingRepository
import com.rahul.stocksim.data.GeminiService
import com.rahul.stocksim.data.MarketInsights
import com.rahul.stocksim.data.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    private val billingRepository: BillingRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    val isPro = billingRepository.isPro

    private val _recommendations = MutableStateFlow<String?>(null)
    val recommendations: StateFlow<String?> = _recommendations.asStateFlow()

    private val _strategyPicks = MutableStateFlow<List<StrategyPick>>(emptyList())
    val strategyPicks: StateFlow<List<StrategyPick>> = _strategyPicks.asStateFlow()

    init {
        loadInsights()
        loadStrategyPicks()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadInsights()
            _isRefreshing.value = false
        }
    }

    private fun loadInsights() {
        viewModelScope.launch {
            try {
                val insights = marketRepository.getMarketInsights()
                _uiState.value = InsightsUiState.Success(insights)
                
                if (billingRepository.isPro.value) {
                    loadRecommendations(insights)
                }
            } catch (e: Exception) {
                _uiState.value = InsightsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadRecommendations(insights: MarketInsights) {
        viewModelScope.launch {
            val recs = geminiService.generateMarketRecommendations(
                gainers = insights.gainers,
                losers = insights.losers,
                indices = insights.indices
            )
            _recommendations.value = recs
        }
    }

    private fun loadStrategyPicks() {
        viewModelScope.launch {
            val growthSymbols = listOf("NVDA", "TSLA", "AMD")
            val dividendSymbols = listOf("KO", "JNJ", "PG")
            val defensiveSymbols = listOf("WMT", "MCD", "COST")
            val valueSymbols = listOf("BRK-B", "JPM", "V")

            val allSymbols = growthSymbols + dividendSymbols + defensiveSymbols + valueSymbols
            val quotes = marketRepository.getStocksQuotes(allSymbols)

            _strategyPicks.value = listOf(
                StrategyPick("Growth", quotes.filter { it.symbol in growthSymbols }),
                StrategyPick("Dividend", quotes.filter { it.symbol in dividendSymbols }),
                StrategyPick("Defensive", quotes.filter { it.symbol in defensiveSymbols }),
                StrategyPick("Value", quotes.filter { it.symbol in valueSymbols })
            )
        }
    }
}

data class StrategyPick(
    val strategy: String,
    val stocks: List<com.rahul.stocksim.model.Stock>
)

sealed class InsightsUiState {
    object Loading : InsightsUiState()
    data class Success(val insights: MarketInsights) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
}
