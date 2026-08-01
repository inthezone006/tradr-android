package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.ui.components.ModernTextField
import com.rahul.stocksim.ui.components.PillButton
import com.rahul.stocksim.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun LoginScreen(navController: NavController, initialError: String? = null) {
    val authRepository = AuthRepository()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(initialError) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val notificationHelper = remember { NotificationHelper(context) }

    // Staggered animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val handlePostLogin = {
        coroutineScope.launch {
            val settings = authRepository.getNotificationSettings()
            if (settings.masterEnabled && settings.notifyNewSignIn) {
                notificationHelper.showNotification(
                    "Security Alert", 
                    "A new sign-in was detected on your account from a ${Build.MODEL}."
                )
            }
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Section with Staggered Animation
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tradr_logo_new),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, 100)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(600, 100))
            ) {
                Column {
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Sign in to continue your trading journey",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Inputs Section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, 200)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600, 200))
            ) {
                Column {
                    ModernTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ModernTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!, 
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(48.dp))

            // Buttons Section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, 300)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, 300))
            ) {
                Column {
                    PillButton(
                        text = "Sign in",
                        onClick = {
                            isLoading = true
                            authRepository.login(email, password) { success, error ->
                                isLoading = false
                                if (success) {
                                    handlePostLogin()
                                } else {
                                    errorMessage = error ?: "Login failed"
                                }
                            }
                        },
                        enabled = email.isNotEmpty() && password.isNotEmpty(),
                        isLoading = isLoading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PillButton(
                        text = "Continue with Google",
                        onClick = {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setServerClientId(WEB_CLIENT_ID)
                                .setFilterByAuthorizedAccounts(false)
                                .setAutoSelectEnabled(false)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            coroutineScope.launch {
                                try {
                                    val activity = context.findActivity() ?: return@launch
                                    val result = credentialManager.getCredential(request = request, context = activity)
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    val signInResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                                    signInResult.onSuccess { isNewUser ->
                                        val user = authRepository.currentUser
                                        if (isNewUser) {
                                            navController.navigate(Screen.PasswordSetup.createRoute(false, user?.displayName, user?.email))
                                        } else {
                                            if (authRepository.isProfileCreated()) handlePostLogin()
                                            else navController.navigate(Screen.BalanceSelection.createRoute(user?.displayName, user?.email))
                                        }
                                    }.onFailure { e ->
                                        errorMessage = "Google Auth Failed: ${e.message}"
                                    }
                                } catch (e: Exception) {
                                    Log.e("Auth", "Google Sign-in failed", e)
                                    errorMessage = (e as? GetCredentialException)?.message ?: "Google Sign-in failed"
                                }
                            }
                        },
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.android_light_rd_na),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Register.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Don't have an account? Sign up", 
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
