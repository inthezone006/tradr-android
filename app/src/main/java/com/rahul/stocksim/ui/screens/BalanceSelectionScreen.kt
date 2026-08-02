package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.ui.components.PillButton
import com.rahul.stocksim.ui.theme.RichBlack
import kotlinx.coroutines.launch

data class BalanceLevel(val level: Int, val amount: Double, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSelectionScreen(
    navController: NavController,
    name: String? = null,
    email: String? = null,
    password: String? = null,
    wantsPro: Boolean = false
) {
    val authRepository = AuthRepository()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val levels = listOf(
        BalanceLevel(1, 100000.0, "$100,000 (Beginner)"),
        BalanceLevel(2, 50000.0, "$50,000 (Casual)"),
        BalanceLevel(3, 25000.0, "$25,000 (Standard)"),
        BalanceLevel(4, 10000.0, "$10,000 (Intermediate)"),
        BalanceLevel(5, 5000.0, "$5,000 (Hard)"),
        BalanceLevel(6, 1000.0, "$1,000 (Difficult)"),
        BalanceLevel(7, 100.0, "$100 (Impossible)")
    )

    var selectedLevel by remember { mutableStateOf(levels[3]) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Set Difficulty", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Select a starting balance. This determines your initial trading power and account level.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = level == selectedLevel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) RichBlack else Color.Transparent)
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedLevel = level },
                                role = Role.RadioButton
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.White,
                                    unselectedColor = Color.DarkGray
                                )
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = level.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(48.dp))

            PillButton(
                text = "Start Trading",
                onClick = {
                    if (email != null && password != null) {
                        isLoading = true
                        coroutineScope.launch {
                            authRepository.register(email, password) { success, error ->
                                if (success) {
                                    coroutineScope.launch {
                                        if (name != null) authRepository.updateDisplayName(name)
                                        val result = authRepository.setUserBalance(selectedLevel.amount, selectedLevel.level)
                                        isLoading = false
                                        if (result.isSuccess) {
                                            if (wantsPro) {
                                                navController.navigate(Screen.Upgrade.route) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(Screen.Main.route) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.localizedMessage}")
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar(error ?: "Registration failed") }
                                }
                            }
                        }
                    } else {
                        isLoading = true
                        coroutineScope.launch {
                            val result = authRepository.setUserBalance(selectedLevel.amount, selectedLevel.level)
                            isLoading = false
                            if (result.isSuccess) {
                                if (wantsPro) {
                                    navController.navigate(Screen.Upgrade.route) {
                                        popUpTo(Screen.BalanceSelection.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.BalanceSelection.route) { inclusive = true }
                                    }
                                }
                            } else {
                                snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.localizedMessage}")
                            }
                        }
                    }
                },
                isLoading = isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
