package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val marketRepository: MarketRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val repository = authRepository
    val currentUser get() = authRepository.currentUser

    suspend fun reloadUser() = authRepository.reloadUser()

    fun clearState() {
        viewModelScope.launch {
            marketRepository.clearCaches()
            billingRepository.clear()
        }
    }

    fun logout() {
        viewModelScope.launch {
            marketRepository.clearCaches()
            billingRepository.clear()
            authRepository.logout()
        }
    }
}
