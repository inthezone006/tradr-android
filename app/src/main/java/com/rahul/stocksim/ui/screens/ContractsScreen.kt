package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rahul.stocksim.model.*
import com.rahul.stocksim.ui.components.StockRow
import com.rahul.stocksim.ui.viewmodels.PortfolioViewModel
import com.rahul.stocksim.ui.viewmodels.PortfolioUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContractsScreen(
    mainNavController: NavController,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val uiState by viewModel.uiState.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val executedContracts by viewModel.executedContracts.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is PortfolioUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = Color.White)
                }
            }
            is PortfolioUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Contracts",
                            color = Color.White,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Buying Power Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = "Buying Power", color = Color.Gray, fontSize = 14.sp)
                                Text(
                                    text = "$${String.format("%,.2f", balance)}",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (contracts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Pending Contracts",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    if (contracts.isNotEmpty()) {
                        items(contracts) { contract ->
                            ContractRow(
                                contract = contract,
                                onCancel = { viewModel.cancelContract(contract.id) },
                                onExecute = {
                                    // Simulated execution/close position
                                    viewModel.closeOptionPosition(contract)
                                }
                            )
                        }
                    }

                    item {
                        if (executedContracts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Contract History",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    if (executedContracts.isNotEmpty()) {
                        items(executedContracts) { contract ->
                            ContractRow(contract, onCancel = null)
                        }
                    }

                    if (contracts.isEmpty() && executedContracts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.7f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "You don't have any contract history yet.", color = Color.Gray)
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
            is PortfolioUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun ContractRow(contract: TradeContract, onCancel: (() -> Unit)? = null, onExecute: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            if (!contract.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = contract.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contract.symbol.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = contract.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    when (contract.status) {
                        ContractStatus.EXECUTED -> {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "EXECUTED",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ContractStatus.CANCELLED -> {
                            Surface(
                                color = Color.Red.copy(alpha = 0.2f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CANCELLED",
                                    color = Color.Red,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ContractStatus.EXPIRED -> {
                            Surface(
                                color = Color.Gray.copy(alpha = 0.2f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "EXPIRED",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
                Text(
                    text = "${contract.type.name.replace("_", " ")} @ $${String.format("%.2f", contract.targetPrice)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${contract.quantity} ${
                        if (contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION) {
                            if (contract.quantity == 1L) "contract" else "contracts"
                        } else {
                            if (contract.quantity == 1L) "unit" else "units"
                        }
                    }", 
                    color = Color.Gray, 
                    fontSize = 12.sp
                )
            }
            if (contract.status == ContractStatus.PENDING) {
                var showCloseConfirm by remember { mutableStateOf(false) }
                
                if (showCloseConfirm) {
                    AlertDialog(
                        onDismissRequest = { showCloseConfirm = false },
                        title = { Text("Close Position?") },
                        text = { Text("Are you sure you want to close this option position? If it's currently profitable, you'll secure your gains.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                onExecute?.invoke()
                                showCloseConfirm = false
                            }) {
                                Text("Close Position", color = Color(0xFF03DAC5))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCloseConfirm = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = Color.White,
                        textContentColor = Color.LightGray
                    )
                }

                Row {
                    if (onExecute != null && (contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION)) {
                        TextButton(onClick = { showCloseConfirm = true }) {
                            Text("Close", color = Color(0xFF03DAC5))
                        }
                    }
                    if (onCancel != null) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                        }
                    }
                }
            } else {
                // Show date
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(contract.createdAt.toDate()),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PositionRow(stock: Stock, quantity: Long, mainNavController: NavController, isOld: Boolean = false) {
    Column {
        StockRow(
            stock = stock,
            onRowClick = { mainNavController.navigate(Screen.Details.createRoute(stock.symbol)) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isOld) "Sold All" else "$quantity Shares", 
                color = if (isOld) Color.Gray else Color.White, 
                fontSize = 14.sp
            )
            if (!isOld) {
                Text(
                    text = "Value: $${String.format("%,.2f", stock.price * quantity)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    }
}
