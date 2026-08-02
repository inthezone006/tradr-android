package com.rahul.stocksim.util

import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.BillingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProFeatureManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingRepository: BillingRepository
) {
    val isPro = billingRepository.isPro
    
    fun canAccessTechnicalIndicators(): Boolean = billingRepository.isPro.value
    
    fun canAccessMultiplePortfolios(): Boolean = billingRepository.isPro.value
    
    fun getMaxWatchlistSize(): Int = if (billingRepository.isPro.value) Int.MAX_VALUE else 10
    
    fun canAccessDeepDiveAI(): Boolean = billingRepository.isPro.value

    sealed class ProFeature {
        object TechnicalIndicators : ProFeature()
        object MultiplePortfolios : ProFeature()
        object UnlimitedWatchlist : ProFeature()
        object DeepDiveAI : ProFeature()
        object ProBadge : ProFeature()
    }

    fun isFeatureLocked(feature: ProFeature): Boolean {
        return !billingRepository.isPro.value
    }
}
