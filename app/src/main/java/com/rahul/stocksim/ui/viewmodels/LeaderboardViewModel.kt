package com.rahul.stocksim.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rahul.stocksim.ui.screens.LeaderboardUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _leadersCache = MutableStateFlow<Map<Int, List<LeaderboardUser>>>(emptyMap())
    private val _loadingStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    private val _errorMessages = MutableStateFlow<Map<Int, String?>>(emptyMap())
    private val _currentUserRankInfo = MutableStateFlow<Map<Int, Pair<Int?, Double>?>>(emptyMap())

    val leadersCache: StateFlow<Map<Int, List<LeaderboardUser>>> = _leadersCache.asStateFlow()
    val loadingStates: StateFlow<Map<Int, Boolean>> = _loadingStates.asStateFlow()
    val errorMessages: StateFlow<Map<Int, String?>> = _errorMessages.asStateFlow()
    val currentUserRankInfo: StateFlow<Map<Int, Pair<Int?, Double>?>> = _currentUserRankInfo.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    fun fetchLeadersIfNeeded(level: Int) {
        if (_leadersCache.value.containsKey(level)) return
        fetchLeaders(level)
    }

    fun fetchLeaders(level: Int) {
        viewModelScope.launch {
            updateError(level, null)
            updateLoading(level, true)
            try {
                // 1. Fetch current user info and calculate rank
                fetchCurrentUserRank(level)

                // 2. Fetch leaderboard users
                val baseQuery = if (level == 0) {
                    firestore.collection("users")
                        .orderBy("totalAccountValue", Query.Direction.DESCENDING)
                } else {
                    firestore.collection("users")
                        .whereEqualTo("level", level)
                        .orderBy("totalAccountValue", Query.Direction.DESCENDING)
                }

                // Limit to 99 as requested
                val snapshot = try {
                    baseQuery.limit(99).get().await()
                } catch (e: Exception) {
                    if (e.message?.contains("PERMISSION_DENIED") == true) {
                        updateError(level, "Access denied. Please check your internet or account.")
                        null
                    } else throw e
                }

                if (snapshot != null) {
                    val users = snapshot.documents.map { doc ->
                        LeaderboardUser(
                            id = doc.id,
                            name = doc.getString("displayName") ?: doc.getString("email")?.split("@")?.get(0) ?: "Trader",
                            totalAccountValue = (doc.get("totalAccountValue") as? Number)?.toDouble() 
                                ?: (doc.get("balance") as? Number)?.toDouble() ?: 0.0,
                            photoUrl = doc.getString("photoUrl"),
                            level = ((doc.get("level") as? Number)?.toLong() ?: 4L).toInt(),
                            isPro = doc.getBoolean("isPro") ?: false
                        )
                    }
                    updateCache(level, users)
                }
            } catch (e: Exception) {
                Log.e("LeaderboardVM", "Query failed for level $level, falling back to local filter", e)
                if (e !is kotlinx.coroutines.CancellationException && 
                    e !is java.net.UnknownHostException &&
                    e !is java.net.SocketTimeoutException) {
                    Firebase.crashlytics.recordException(e)
                }
                
                // Fallback: Fetch all users and filter locally if the index isn't ready
                try {
                    val fallbackSnapshot = firestore.collection("users")
                        .orderBy("totalAccountValue", Query.Direction.DESCENDING)
                        .limit(200)
                        .get().await()
                    
                    val allUsers = fallbackSnapshot.documents.map { doc ->
                        LeaderboardUser(
                            id = doc.id,
                            name = doc.getString("displayName") ?: doc.getString("email")?.split("@")?.get(0) ?: "Trader",
                            totalAccountValue = (doc.get("totalAccountValue") as? Number)?.toDouble() 
                                ?: (doc.get("balance") as? Number)?.toDouble() ?: 0.0,
                            photoUrl = doc.getString("photoUrl"),
                            level = ((doc.get("level") as? Number)?.toLong() ?: 4L).toInt(),
                            isPro = doc.getBoolean("isPro") ?: false
                        )
                    }
                    
                    val filteredUsers = if (level == 0) {
                        allUsers.take(99)
                    } else {
                        allUsers.filter { it.level == level }.take(99)
                    }
                    updateCache(level, filteredUsers)
                } catch (fallbackEx: Exception) {
                    if (fallbackEx !is kotlinx.coroutines.CancellationException && 
                        fallbackEx !is java.net.UnknownHostException &&
                        fallbackEx !is java.net.SocketTimeoutException) {
                        Firebase.crashlytics.recordException(fallbackEx)
                    }
                    updateError(level, "Leaderboard currently unavailable.")
                }
            } finally {
                updateLoading(level, false)
            }
        }
    }

    private suspend fun fetchCurrentUserRank(level: Int) {
        val uid = currentUserId ?: return
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val userValue = (userDoc.get("totalAccountValue") as? Number)?.toDouble() 
                ?: (userDoc.get("balance") as? Number)?.toDouble() ?: 0.0
            
            val rankQuery = if (level == 0) {
                firestore.collection("users")
                    .whereGreaterThan("totalAccountValue", userValue)
            } else {
                firestore.collection("users")
                    .whereEqualTo("level", level)
                    .whereGreaterThan("totalAccountValue", userValue)
            }
            
            val countSnapshot = rankQuery.count().get(com.google.firebase.firestore.AggregateSource.SERVER).await()
            val rank = (countSnapshot.count + 1).toInt()
            
            _currentUserRankInfo.value = _currentUserRankInfo.value.toMutableMap().apply {
                put(level, rank to userValue)
            }
        } catch (e: Exception) {
            Log.e("LeaderboardVM", "Failed to fetch current user rank", e)
        }
    }

    private fun updateCache(level: Int, users: List<LeaderboardUser>) {
        _leadersCache.value = _leadersCache.value.toMutableMap().apply { put(level, users) }
    }

    private fun updateLoading(level: Int, isLoading: Boolean) {
        _loadingStates.value = _loadingStates.value.toMutableMap().apply { put(level, isLoading) }
    }

    private fun updateError(level: Int, message: String?) {
        _errorMessages.value = _errorMessages.value.toMutableMap().apply { put(level, message) }
    }
}
