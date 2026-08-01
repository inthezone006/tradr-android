package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.util.NotificationHelper
import kotlinx.coroutines.launch

// Helper function to safely find Activity from Context
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.tradr_logo_new),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "Email", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password", color = Color.LightGray) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sign in button
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        authRepository.login(email, password) { success, error ->
                            isLoading = false
                            if (success) {
                                handlePostLogin()
                            } else {
                                errorMessage = error ?: "Registration failed"
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    if (isLoading) {
                        LoadingIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Google sign in button
                OutlinedButton(
                    onClick = {
                        // Use GetGoogleIdOption for a more flexible flow that can handle re-auth issues
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
                                val activity = context.findActivity()
                                if (activity == null) {
                                    errorMessage = "Internal Error: Activity not found"
                                    return@launch
                                }
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = activity
                                )
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val signInResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                                signInResult.onSuccess { isNewUser ->
                                    val user = authRepository.currentUser
                                    if (isNewUser) {
                                        navController.navigate(Screen.PasswordSetup.createRoute(
                                            isChangePassword = false,
                                            name = user?.displayName,
                                            email = user?.email
                                        )) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            if (authRepository.isProfileCreated()) {
                                                handlePostLogin()
                                            } else {
                                                navController.navigate(Screen.BalanceSelection.createRoute(
                                                    name = user?.displayName,
                                                    email = user?.email
                                                )) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }.onFailure { e ->
                                    errorMessage = "Firebase Google Auth Failed: ${e.message}"
                                }
                            } catch (e: GetCredentialException) {
                                Log.e("Auth", "Google Sign-in failed", e)
                                errorMessage = when (e) {
                                    is GetCredentialCancellationException -> "Sign-in cancelled"
                                    is NoCredentialException -> "No Google accounts found. Please check your device accounts or SHA-1 configuration in Firebase."
                                    else -> e.message ?: "Google Sign-in failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.android_light_rd_na),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                Text(text = "Don't have an account? Sign up", color = Color.Gray)
            }
        }
    }
}
