package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {
    val isPro: StateFlow<Boolean> = billingRepository.isPro
    val currentUser = authRepository.currentUser
    
    fun logout() {
        authRepository.logout()
    }
    
    suspend fun sendEmailVerification() {
        authRepository.sendEmailVerification()
    }
}
