package com.rahul.stocksim.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isPro = MutableStateFlow(false)
    val isPro = _isPro.asStateFlow()

    private val _purchaseStatus = MutableStateFlow<PurchaseStatus>(PurchaseStatus.Idle)
    val purchaseStatus = _purchaseStatus.asStateFlow()

    sealed class PurchaseStatus {
        object Idle : PurchaseStatus()
        object Loading : PurchaseStatus()
        object Success : PurchaseStatus()
        data class Error(val message: String) : PurchaseStatus()
    }

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingRepository", "Billing setup finished")
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("BillingRepository", "Billing service disconnected")
                // Reconnect strategy can be added here
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPro = purchases.any { purchase ->
                    (purchase.products.contains("tradr_pro") || purchase.products.contains("tradr-pro")) 
                            && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPro.value = hasPro
                if (hasPro) {
                    syncProStatusWithFirebase()
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        _purchaseStatus.value = PurchaseStatus.Loading
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("tradr_pro")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("tradr-pro")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
                
                // Try to find either version of the ID
                val productDetails = productDetailsList.find { 
                    it.productId == "tradr_pro" || it.productId == "tradr-pro" 
                }
                
                if (productDetails != null) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )

                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    billingClient.launchBillingFlow(activity, flowParams)
                } else {
                    _purchaseStatus.value = PurchaseStatus.Error("Product not found (Check tradr_pro or tradr-pro)")
                }
            } else {
                _purchaseStatus.value = PurchaseStatus.Error("Error fetching product details: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseStatus.value = PurchaseStatus.Idle
        } else {
            _purchaseStatus.value = PurchaseStatus.Error("Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingRepository", "Purchase acknowledged")
                        _isPro.value = true
                        _purchaseStatus.value = PurchaseStatus.Success
                        syncProStatusWithFirebase()
                    }
                }
            } else {
                _isPro.value = true
                _purchaseStatus.value = PurchaseStatus.Success
                syncProStatusWithFirebase()
            }
        }
    }

    private fun syncProStatusWithFirebase() {
        val user = auth.currentUser ?: return
        scope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .set(mapOf("isPro" to true), SetOptions.merge()).await()
                Log.d("BillingRepository", "Pro status synced with Firebase")
            } catch (e: Exception) {
                Log.e("BillingRepository", "Error syncing Pro status", e)
            }
        }
    }
    
    suspend fun refreshProStatus(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            val pro = snapshot.getBoolean("isPro") ?: false
            _isPro.value = pro
            pro
        } catch (e: Exception) {
            false
        }
    }
}
