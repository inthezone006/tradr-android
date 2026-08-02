package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

    var selectedContract by remember { mutableStateOf<TradeContract?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }

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
                                onClick = {
                                    selectedContract = contract
                                    showActionSheet = true
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
                            ContractRow(contract)
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

                if (showActionSheet && selectedContract != null) {
                    ContractActionSheet(
                        contract = selectedContract!!,
                        onDismiss = { showActionSheet = false },
                        onCancel = {
                            viewModel.cancelContract(it.id)
                            showActionSheet = false
                        },
                        onExecute = {
                            viewModel.closeOptionPosition(it)
                            showActionSheet = false
                        }
                    )
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
fun ContractRow(contract: TradeContract, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
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
                // No button here as per user request
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractActionSheet(
    contract: TradeContract,
    onDismiss: () -> Unit,
    onCancel: (TradeContract) -> Unit,
    onExecute: (TradeContract) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val label = when (contract.type) {
                        ContractType.BUY_AT -> "Limit Buy"
                        ContractType.SELL_AT -> "Limit Sell"
                        ContractType.CALL_OPTION -> "Call Option"
                        ContractType.PUT_OPTION -> "Put Option"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contract.symbol} Contract",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                if (!contract.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = contract.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Position Details", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DetailItem("Execution Target", "$${String.format("%.2f", contract.targetPrice)}")
                    DetailItem("Quantity", "${contract.quantity} ${if(contract.quantity == 1L) "Unit" else "Units"}")
                    
                    if (contract.premium > 0) {
                        DetailItem("Premium Paid", "$${String.format("%.2f", contract.premium)}")
                        val totalCost = contract.premium * 100 * contract.quantity
                        DetailItem("Total Investment", "$${String.format("%,.2f", totalCost)}")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val isOption = contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION
            
            if (isOption) {
                Button(
                    onClick = { onExecute(contract) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Position & Settle", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            OutlinedButton(
                onClick = { onCancel(contract) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF44336)
                ),
                border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isOption) "Cancel Active Order" else "Cancel Contract", 
                    fontWeight = FontWeight.Bold
                )
            }
            
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
