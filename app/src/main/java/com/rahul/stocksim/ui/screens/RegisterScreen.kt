package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.ui.components.ModernTextField
import com.rahul.stocksim.ui.components.PillButton
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
fun RegisterScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = name.isNotEmpty() && email.contains("@") && email.contains(".")

    // Staggered animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Header Section
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
                            text = "Sign up",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Start your simulated trading journey",
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
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        ModernTextField(
                            value = email,
                            onValueChange = { email = it.lowercase() },
                            label = "Email Address",
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Email
                            )
                        )
                    }
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
                            text = "Continue",
                            onClick = {
                                isLoading = true
                                coroutineScope.launch {
                                    val exists = authRepository.checkEmailExists(email)
                                    isLoading = false
                                    if (exists) {
                                        snackbarHostState.showSnackbar("This email is already in use.")
                                    } else {
                                        navController.navigate(Screen.PasswordSetup.createRoute(false, name, email))
                                    }
                                }
                            },
                            enabled = isFormValid,
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
                                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

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
                                                if (authRepository.isProfileCreated()) {
                                                    navController.navigate(Screen.Main.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                                                } else {
                                                    navController.navigate(Screen.BalanceSelection.createRoute(user?.displayName, user?.email))
                                                }
                                            }
                                        }.onFailure { e ->
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Google Sign-in failed: ${e.message}") }
                                        }
                                    } catch (e: Exception) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Error: ${e.localizedMessage}") }
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
                            onClick = { navController.navigate(Screen.Login.createRoute()) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Already have an account? Sign in", 
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
}
