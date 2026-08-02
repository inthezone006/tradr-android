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

    init {
        loadInsights()
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
}

sealed class InsightsUiState {
    object Loading : InsightsUiState()
    data class Success(val insights: MarketInsights) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
}
