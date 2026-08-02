package com.rahul.stocksim.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingRepository.isPro
    val purchaseStatus: StateFlow<BillingRepository.PurchaseStatus> = billingRepository.purchaseStatus

    fun purchasePro(activity: Activity) {
        billingRepository.launchPurchaseFlow(activity)
    }
    
    fun refreshStatus() {
        viewModelScope.launch {
            billingRepository.refreshProStatus()
        }
    }
}
