package com.rahul.stocksim.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = AuthRepository()
    val user = authRepository.currentUser
    
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var isUpdatingName by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White, style = MaterialTheme.typography.titleLarge) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Edit Name Section
            Text(
                "Identity",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (name.isNotEmpty() && name != user?.displayName) {
                                if (isUpdatingName) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    IconButton(onClick = {
                                        isUpdatingName = true
                                        coroutineScope.launch {
                                            val result = authRepository.updateDisplayName(name)
                                            isUpdatingName = false
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    "Name updated successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Update failed: ${result.exceptionOrNull()?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = Color.Green
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Danger Zone Section
            Text(
                "Danger Zone",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Delete Account",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "This will permanently remove all your progress, balance, and account data. This action cannot be undone.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DELETE ACCOUNT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Permanently Delete Account?", color = Color.White) },
            text = { 
                Text(
                    "Are you sure you want to delete your account? All your simulator data will be lost forever.",
                    color = Color.Gray
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            val result = authRepository.deleteCurrentUser()
                            isDeleting = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.Login.createRoute()) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                if (error.contains("recent-login")) {
                                    Toast.makeText(context, "Please log in again before deleting your account for security purposes.", Toast.LENGTH_LONG).show()
                                    authRepository.logout()
                                    navController.navigate(Screen.Login.createRoute()) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Deletion failed: $error", Toast.LENGTH_LONG).show()
                                    showDeleteDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        LoadingIndicator(modifier = Modifier.size(18.dp), color = Color.Red)
                    } else {
                        Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}
