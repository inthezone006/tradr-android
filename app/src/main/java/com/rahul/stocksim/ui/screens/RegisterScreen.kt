package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

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
fun RegisterScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Simplified Validation for initial step
    val isFormValid = name.isNotEmpty() && email.contains("@") && email.contains(".")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.tradr_logo_new),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter your details to get started",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Full Name", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.lowercase() }, // Force lowercase to prevent unwanted capitalization
                label = { Text(text = "Email Address", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Email
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            val exists = authRepository.checkEmailExists(email)
                            isLoading = false
                            if (exists) {
                                snackbarHostState.showSnackbar("This email is already in use.")
                            } else {
                                navController.navigate(Screen.PasswordSetup.createRoute(isChangePassword = false, name = name, email = email))
                            }
                        }
                    },
                    enabled = !isLoading && isFormValid,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    if (isLoading) {
                        LoadingIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                OutlinedButton(
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
                                        navController.navigate(Screen.PasswordSetup.createRoute(
                                            isChangePassword = false,
                                            name = user?.displayName,
                                            email = user?.email
                                        )) {
                                            popUpTo(Screen.Register.route) { inclusive = true }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            if (authRepository.isProfileCreated()) {
                                                navController.navigate(Screen.Main.route) {
                                                    popUpTo(Screen.Register.route) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(Screen.BalanceSelection.createRoute(
                                                    name = user?.displayName,
                                                    email = user?.email
                                                )) {
                                                    popUpTo(Screen.Register.route) { inclusive = true }
                                                }
                                            }
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
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.android_light_rd_na),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { navController.navigate(Screen.Login.createRoute()) }) {
                Text(text = "Already have an account? Sign in", color = Color.Gray)
            }
        }
    }
}
