package com.rahul.stocksim.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.ui.components.ModernTextField
import com.rahul.stocksim.ui.components.PillButton
import com.rahul.stocksim.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupScreen(
    navController: NavController, 
    isChangePassword: Boolean = false,
    initialName: String? = null,
    initialEmail: String? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authRepository = viewModel.repository
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var oldPassword by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    
    val isPasswordValid = hasMinLength && hasUppercase && hasDigit && hasSpecial && passwordsMatch && (!isChangePassword || oldPassword.isNotEmpty())

    val handleBack: () -> Unit = {
        if (!isChangePassword && authRepository.currentUser != null) {
            isLoading = true
            coroutineScope.launch {
                authRepository.deleteCurrentUser()
                isLoading = false
                navController.navigate(Screen.Login.createRoute()) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isChangePassword) "Change Password" else "Security Setup", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = handleBack, enabled = !isLoading) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isChangePassword) 
                        "Confirm your current identity and choose a new secure password." 
                        else "Create a secure password for your account. You can use this to sign in later with your email.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (isChangePassword) {
                    ModernTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = "Current Password",
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = if (isChangePassword) "New Password" else "Choose Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                ModernTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text("Password Requirements", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                RequirementItem("At least 8 characters", hasMinLength)
                RequirementItem("At least one uppercase letter", hasUppercase)
                RequirementItem("At least one digit", hasDigit)
                RequirementItem("At least one special character", hasSpecial)
                RequirementItem("Passwords must match", passwordsMatch)
                if (isChangePassword) {
                    RequirementItem("Current password required", oldPassword.isNotEmpty())
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(48.dp))

                PillButton(
                    text = if (isChangePassword) "Change Password" else "Complete Setup",
                    onClick = {
                        if (isPasswordValid) {
                            if (isChangePassword) {
                                isLoading = true
                                coroutineScope.launch {
                                    val result = authRepository.updatePassword(password)
                                    isLoading = false
                                    if (result.isSuccess) navController.popBackStack()
                                    else snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.localizedMessage}")
                                }
                            } else {
                                val currentUser = authRepository.currentUser
                                if (currentUser != null) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = authRepository.updatePassword(password)
                                        isLoading = false
                                        if (result.isSuccess) {
                                            navController.navigate(Screen.BalanceSelection.createRoute(initialName ?: currentUser.displayName, initialEmail ?: currentUser.email, null))
                                        } else {
                                            snackbarHostState.showSnackbar("Error linking password: ${result.exceptionOrNull()?.localizedMessage}")
                                        }
                                    }
                                } else {
                                    navController.navigate(Screen.BalanceSelection.createRoute(initialName, initialEmail, password))
                                }
                            }
                        }
                    },
                    enabled = isPasswordValid,
                    isLoading = isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isMet) Color.Green else Color.DarkGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) Color.White else Color.Gray,
            fontSize = 13.sp
        )
    }
}
