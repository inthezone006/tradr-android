package com.rahul.stocksim.data

import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.rahul.stocksim.BuildConfig
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val viaPush: Boolean = true,
    val notifyLargeDrop: Boolean = true,
    val notifyLowBalance: Boolean = true,
    val notifyNewSignIn: Boolean = true
)

@Singleton
class AuthRepository @Inject constructor() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics

    private fun recordError(e: Exception) {
        if (e is CancellationException || 
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException ||
            (e is com.google.firebase.firestore.FirebaseFirestoreException && 
             e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE)) {
            return
        }
        crashlytics.recordException(e)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    private val defaultWatchlistSymbols = listOf(
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
        "META", "NVDA", "NFLX", "AMD", "PYPL",
        "INTC", "CSCO", "ADBE", "CRM", "QCOM"
    )

    suspend fun saveFcmToken(token: String) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
            Log.d("AUTH_REPO", "FCM Token saved successfully")
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Error saving FCM Token", e)
            recordError(e)
        }
    }

    fun logEventWithUser(eventName: String, bundle: Bundle = Bundle()) {
        val user = auth.currentUser
        
        user?.let {
            crashlytics.setUserId(it.uid)
            analytics.setUserId(it.uid)
        }

        bundle.apply {
            putString("user_id", user?.uid ?: "anonymous")
            putString("user_name", user?.displayName ?: "anonymous")
            putString("user_email", user?.email ?: "anonymous")
            putString("device_model", android.os.Build.MODEL)
            putString("android_version", android.os.Build.VERSION.RELEASE)
            putBoolean("is_pro", BuildConfig.DEBUG || (user != null && user.email == "rahul.menon85280@gmail.com")) // Example check, ideally synced from Firestore
        }
        
        analytics.logEvent(eventName, bundle)
        
        val params = bundle.keySet().joinToString(", ") { "$it=${bundle.get(it)}" }
        Log.d("APP_EVENT", "Event: $eventName | Params: $params")
    }

    suspend fun setUserProperties(isPro: Boolean, level: Int) {
        analytics.setUserProperty("is_pro_user", isPro.toString())
        analytics.setUserProperty("user_difficulty_level", level.toString())
        crashlytics.setCustomKey("is_pro", isPro)
        crashlytics.setCustomKey("user_level", level)
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logEventWithUser(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "email")
                    })
                    onResult(true, null)
                } else {
                    onResult(false, "Invalid credentials.")
                }
            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logEventWithUser(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "email")
                    })
                    onResult(true, null)
                } else {
                    val error = task.exception?.localizedMessage ?: "Registration failed."
                    onResult(false, error)
                }
            }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false

            user?.let { firebaseUser ->
                val event = if (isNewUser) FirebaseAnalytics.Event.SIGN_UP else FirebaseAnalytics.Event.LOGIN
                logEventWithUser(event, Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "google")
                })

                firebaseUser.photoUrl?.let { photoUri ->
                    if (firebaseUser.photoUrl.toString() != photoUri.toString()) {
                        val profileUpdates = UserProfileChangeRequest.Builder().setPhotoUri(photoUri).build()
                        firebaseUser.updateProfile(profileUpdates).await()

                        firestore.collection("users").document(firebaseUser.uid)
                            .set(mapOf("photoUrl" to photoUri.toString()), SetOptions.merge()).await()
                        logEventWithUser("set_google_profile_picture")
                    }
                }
            }
            Result.success(isNewUser)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            recordError(e)
            false
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            logEventWithUser("send_email_verification")
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<FirebaseUser?> {
        return try {
            currentUser?.reload()?.await()
            Result.success(currentUser)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
            user.updateProfile(profileUpdates).await()
            firestore.collection("users").document(user.uid)
                .set(mapOf("displayName" to newName), SetOptions.merge()).await()
            logEventWithUser("update_display_name", Bundle().apply {
                putString("new_name", newName)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            user.updatePassword(newPassword).await()
            logEventWithUser("update_password")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                Log.w("AUTH_REPO", "Login required to update password")
            }
            recordError(e)
            Result.failure(e)
        }
    }

    fun isGoogleUser(): Boolean {
        return currentUser?.providerData?.any { it.providerId == "google.com" } ?: false
    }

    suspend fun deleteAccount(password: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val email = user.email ?: return Result.failure(Exception("User email not found"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            
            performAccountCleanup(user.uid)
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Error deleting account with password", e)
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountWithGoogle(idToken: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            
            performAccountCleanup(user.uid)
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Error deleting account with Google", e)
            recordError(e)
            Result.failure(e)
        }
    }

    private suspend fun performAccountCleanup(uid: String) {
        logEventWithUser("delete_account")
        firestore.collection("users").document(uid).delete().await()
        try {
            storage.reference.child("profile_pictures/$uid").delete().await()
        } catch (e: Exception) {
            Log.w("AUTH_REPO", "Could not delete profile picture during account cleanup", e)
        }
    }

    suspend fun deleteCurrentUser(): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val uid = user.uid
            logEventWithUser("delete_account_immediate")
            
            // Try to delete firestore data first
            try {
                firestore.collection("users").document(uid).delete().await()
                storage.reference.child("profile_pictures/$uid").delete().await()
            } catch (e: Exception) {
                Log.w("AUTH_REPO", "Cleanup failed during immediate delete", e)
            }
            
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AUTH_REPO", "Error in deleteCurrentUser", e)
            recordError(e)
            // If delete fails due to recent login required, at least sign out
            auth.signOut()
            Result.failure(e)
        }
    }

    suspend fun setUserBalance(balance: Double, level: Int): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val userRef = firestore.collection("users").document(user.uid)
            val userData = hashMapOf(
                "balance" to balance,
                "totalAccountValue" to balance, // Initialize for leaderboard
                "level" to level,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "isPro" to false // Initialize Pro status
            )
            userRef.set(userData, SetOptions.merge()).await()

            val batch = firestore.batch()
            defaultWatchlistSymbols.forEach { symbol ->
                val watchlistRef = userRef.collection("watchlist").document(symbol)
                batch.set(watchlistRef, mapOf("symbol" to symbol))
            }
            batch.commit().await()

            // Initialize 30-day history with the starting balance
            val historyRef = userRef.collection("account_history")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val historyBatch = firestore.batch()
            
            for (i in 0..29) {
                val dateStr = sdf.format(calendar.time)
                val docRef = historyRef.document(dateStr)
                historyBatch.set(docRef, mapOf(
                    "value" to balance,
                    "timestamp" to com.google.firebase.Timestamp(calendar.time)
                ))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            historyBatch.commit().await()

            setUserProperties(BuildConfig.DEBUG, level)
            
            logEventWithUser("select_difficulty_level", Bundle().apply {
                putInt("level", level)
                putDouble("initial_balance", balance)
            })

            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun isProfileCreated(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            snapshot.exists() && snapshot.contains("balance")
        } catch (e: Exception) {
            recordError(e)
            false
        }
    }

    suspend fun isTutorialCompleted(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            snapshot.getBoolean("tutorialCompleted") ?: false
        } catch (e: Exception) {
            recordError(e)
            false
        }
    }

    suspend fun isProUser(): Boolean {
        if (BuildConfig.DEBUG) return true
        val user = auth.currentUser ?: return false
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            val hasPro = snapshot.getBoolean("isPro") ?: false
            val level = (snapshot.get("level") as? Number)?.toInt() ?: 4
            setUserProperties(hasPro, level)
            hasPro
        } catch (e: Exception) {
            recordError(e)
            false
        }
    }

    suspend fun setTutorialCompleted(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestore.collection("users").document(user.uid)
                .set(mapOf("tutorialCompleted" to true), SetOptions.merge()).await()
            logEventWithUser("complete_tutorial")
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun getNotificationSettings(): NotificationSettings {
        val user = auth.currentUser ?: return NotificationSettings()
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            val data = snapshot.data ?: return NotificationSettings()
            NotificationSettings(
                masterEnabled = data["notif_master"] as? Boolean ?: true,
                viaPush = data["notif_push"] as? Boolean ?: true,
                notifyLargeDrop = data["notif_large_drop"] as? Boolean ?: true,
                notifyLowBalance = data["notif_low_balance"] as? Boolean ?: true,
                notifyNewSignIn = data["notif_new_signin"] as? Boolean ?: true
            )
        } catch (e: Exception) {
            recordError(e)
            NotificationSettings()
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val data = hashMapOf(
                "notif_master" to settings.masterEnabled,
                "notif_push" to settings.viaPush,
                "notif_large_drop" to settings.notifyLargeDrop,
                "notif_low_balance" to settings.notifyLowBalance,
                "notif_new_signin" to settings.notifyNewSignIn
            )
            firestore.collection("users").document(user.uid).update(data as Map<String, Any>).await()
            logEventWithUser("update_notification_settings")
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    fun logout() {
        logEventWithUser("logout")
        auth.signOut()
    }
}
