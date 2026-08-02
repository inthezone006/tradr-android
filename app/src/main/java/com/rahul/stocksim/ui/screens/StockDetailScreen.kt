package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.ui.theme.DarkGrayBlack
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.rahul.stocksim.data.*
import com.rahul.stocksim.model.*
import com.rahul.stocksim.ui.components.PillButton
import com.rahul.stocksim.ui.components.TradingViewChart
import com.rahul.stocksim.ui.components.VicoLineChart
import com.rahul.stocksim.ui.viewmodels.StockDetailUiState
import com.rahul.stocksim.ui.viewmodels.StockDetailViewModel
import com.rahul.stocksim.util.ReviewHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StockDetailScreen(
    stockSymbol: String?,
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val ownedQuantity by viewModel.ownedQuantity.collectAsState()
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val priceAlerts by viewModel.priceAlerts.collectAsState()
    val activeContracts by viewModel.activeContracts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val reviewHelper = remember { ReviewHelper(context as Activity) }

    var quantityInput by remember { mutableStateOf("1") }
    val quantity = quantityInput.toIntOrNull() ?: 0

    var showAlertDialog by remember { mutableStateOf(false) }
    
    var showContractsSheet by remember { mutableStateOf(false) }
    
    val scrollState = rememberLazyListState()
    
    val isCollapsed by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset > 200)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    AnimatedContent(
                        targetState = isCollapsed,
                        transitionSpec = {
                            fadeIn() + slideInVertically { it / 2 } togetherWith fadeOut() + slideOutVertically { -it / 2 }
                        },
                        label = "HeaderTransition"
                    ) { collapsed ->
                        val state = uiState
                        if (collapsed && state is StockDetailUiState.Success) {
                            val stock = state.stock
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            ) {
                                if (state.profile?.logo?.isNotEmpty() == true) {
                                    AsyncImage(
                                        model = state.profile.logo,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        state.marketStatus?.let { status ->
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (status.isOpen) Color.Green else Color.Red)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = stock.symbol,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = stock.name,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Column {
                                Text(stockSymbol ?: "Stock Details", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                (state as? StockDetailUiState.Success)?.marketStatus?.let { status ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (status.isOpen) Color.Green else Color.Red)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (status.isOpen) "Market Open" else "Market Closed",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isRefreshing) {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (uiState is StockDetailUiState.Success) {

                        IconButton(onClick = { 
                            showAlertDialog = !showAlertDialog 
                        }) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Set Alert",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { 
                            viewModel.toggleWatchlist() 
                        }) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Watchlist",
                                tint = if (isInWatchlist) Color.Red else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is StockDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = Color.White)
                }
            }
            is StockDetailUiState.Error -> {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Market data unavailable for $stockSymbol. Please try again later.", Toast.LENGTH_LONG).show()
                    onBackClick()
                }
            }
            is StockDetailUiState.Success -> {
                val stock = state.stock
                val profile = state.profile
                val newsSentiment = state.newsSentiment
                val rsiData = state.rsiData
                val sma50Data = state.sma50Data
                val sma200Data = state.sma200Data
                val recommendations = state.recommendations
                val esgScores = state.esgScores
                val earnings = state.earnings
                val dividends = state.dividends
                val peers = state.peers
                val financials = state.financials
                val aiRecommendation = state.aiRecommendation
                val newsArticles = state.newsArticles
                val tdRsi = state.tdRsi
                val tdMacd = state.tdMacd
                val tdBbands = state.tdBbands
                val candleHistory = state.candleHistory

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                // Logo Box
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile?.logo?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = profile.logo,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = stock.symbol.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = stock.symbol,
                                        style = MaterialTheme.typography.displayLarge,
                                        fontSize = 28.sp, // Tweak size for this specific spot
                                        color = Color.White
                                    )
                                    Text(
                                        text = stock.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                val color = if (stock.change >= 0) Color.Green else Color.Red
                                
                                AnimatedContent(
                                    targetState = stock.price,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                        } else {
                                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                                        }.using(SizeTransform(clip = false))
                                    },
                                    label = "StockPriceTicker"
                                ) { price ->
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", price)}",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontSize = 24.sp,
                                        color = color
                                    )
                                }
                                
                                Text(
                                    text = "${if (stock.change >= 0) "+" else ""}${String.format(Locale.US, "%.2f", stock.change)} (${String.format(Locale.US, "%.2f", stock.percentChange)}%)",
                                    color = color,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Sentiment & Technical Indicators Row
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (rsiData?.rsi?.isNotEmpty() == true) {
                                    val currentRsi = rsiData.rsi.last()
                                    val rsiStatus = when {
                                        currentRsi > 70 -> "Overbought"
                                        currentRsi < 30 -> "Oversold"
                                        else -> "Neutral"
                                    }
                                    val rsiColor = when {
                                        currentRsi > 70 -> Color.Red
                                        currentRsi < 30 -> Color.Green
                                        else -> Color.Gray
                                    }
                                    Row {
                                        Text("RSI: ", color = Color.Gray, fontSize = 12.sp)
                                        Text("${String.format(Locale.US, "%.1f", currentRsi)} ($rsiStatus)", color = rsiColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            if (sma50Data?.sma?.isNotEmpty() == true && sma200Data?.sma?.isNotEmpty() == true) {
                                val currentSma50 = sma50Data.sma.last()
                                val currentSma200 = sma200Data.sma.last()
                                val trend = if (currentSma50 > currentSma200) "Bullish Cross" else "Bearish Cross"
                                val trendColor = if (currentSma50 > currentSma200) Color.Green else Color.Red
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "50D SMA: $${String.format(Locale.US, "%.2f", currentSma50)}", color = Color.Gray, fontSize = 11.sp)
                                    Text(text = trend, color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "200D SMA: $${String.format(Locale.US, "%.2f", currentSma200)}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // --- NATIVE CHART SECTION ---
                    item {
                        val history by viewModel.history.collectAsState()
                        val isGraphLoading by viewModel.isGraphLoading.collectAsState()
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Text(
                                text = "Market Chart",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                if (isGraphLoading) {
                                    LoadingIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                                } else if (history.isNotEmpty()) {
                                    Column {
                                        VicoLineChart(
                                            history = history,
                                            lineColor = if (stock.change >= 0) Color.Green else Color.Red,
                                            modifier = Modifier.weight(1f).fillMaxWidth()
                                        )
                                        
                                        val periods = listOf("1D", "5D", "1M", "6M", "1Y")
                                        val selectedPeriod by viewModel.selectedPeriod.collectAsState()
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            periods.forEach { period ->
                                                FilterChip(
                                                    modifier = Modifier.weight(1f),
                                                    selected = selectedPeriod == period,
                                                    onClick = { 
                                                        viewModel.refreshGraph(period) 
                                                    },
                                                    label = { 
                                                        Text(
                                                            text = period,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center
                                                        ) 
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        labelColor = Color.Gray,
                                                        selectedLabelColor = Color.Black,
                                                        selectedContainerColor = Color.White,
                                                        containerColor = Color.Transparent
                                                    ),
                                                    border = null
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No historical data available",
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    if (ownedQuantity > 0) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Position",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val unitLabel = when {
                                            stock.isCrypto -> "Units Owned"
                                            stock.isForex -> "Lots Owned"
                                            else -> "Shares Owned"
                                        }
                                        Text(unitLabel, color = Color.Gray, fontSize = 12.sp)
                                        Text(
                                            text = "$ownedQuantity",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Equity Value", color = Color.Gray, fontSize = 12.sp)
                                        Text(
                                            text = "$${String.format(Locale.US, "%,.2f", ownedQuantity * stock.price)}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- BUY/SELL SECTION ---
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Balance and Position Info
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(text = "Available to Spend", color = Color.Gray, fontSize = 12.sp)
                                        Text(
                                            text = "$${String.format(Locale.US, "%,.2f", balance)}",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (ownedQuantity > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "In $stockSymbol", color = Color.Gray, fontSize = 12.sp)
                                            Text(
                                                text = "$${String.format(Locale.US, "%,.2f", ownedQuantity * stock.price)}",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Quantity Selector
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val unitLabel = when {
                                        stock.isCrypto -> "Units"
                                        stock.isForex -> "Lots"
                                        else -> "Shares"
                                    }
                                    Text(unitLabel, color = Color.Gray, fontSize = 12.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(onClick = { 
                                            val current = quantityInput.toIntOrNull() ?: 0
                                            if (current > 1) quantityInput = (current - 1).toString()
                                        }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                .widthIn(min = 60.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BasicTextField(
                                                value = quantityInput,
                                                onValueChange = { newValue ->
                                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                        quantityInput = newValue
                                                    }
                                                },
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = { focusManager.clearFocus() }
                                                ),
                                                cursorBrush = SolidColor(Color.White),
                                                singleLine = true
                                            )
                                        }
                                        
                                        IconButton(onClick = { 
                                            val current = quantityInput.toIntOrNull() ?: 0
                                            quantityInput = (current + 1).toString()
                                        }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                                    }
                                }

                                val quantityValue = quantityInput.toIntOrNull() ?: 0
                                val totalCost = quantityValue * stock.price
                                val hasEnoughMoney = balance >= totalCost
                                val canAffordAnything = balance >= stock.price
                                val canSellQuantity = ownedQuantity > 0 && quantityValue > 0 && quantityValue <= ownedQuantity

                                // Action Buttons Section
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                OutlinedButton(
                                    onClick = { 
                                        showContractsSheet = true 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Limit Orders & Contracts", fontSize = 14.sp)
                                    if (activeContracts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color.White,
                                            shape = CircleShape,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${activeContracts.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Themed Warning (Show if they don't have enough money for CURRENT quantity)
                                if (quantityValue > 0 && !hasEnoughMoney) {
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (canAffordAnything) 
                                                    "Insufficient funds for purchasing $quantityValue shares."
                                                    else "Insufficient funds to purchase any shares.",
                                                color = Color.Red,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Buy/Sell Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    PillButton(
                                        text = "BUY",
                                        onClick = {
                                            coroutineScope.launch {
                                                val buyResult = viewModel.buyStock(quantityValue, stock.price)
                                                if (buyResult.isSuccess) {
                                                    Toast.makeText(context, "Purchase Successful", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, buyResult.exceptionOrNull()?.message ?: "Purchase failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = hasEnoughMoney && quantityValue > 0,
                                        containerColor = Color(0xFF00C853),
                                        contentColor = Color.White
                                    )
                                    
                                    if (ownedQuantity > 0) {
                                        PillButton(
                                            text = "SELL",
                                            onClick = {
                                                coroutineScope.launch {
                                                    val sellResult = viewModel.sellStock(quantityValue, stock.price)
                                                    if (sellResult.isSuccess) {
                                                        Toast.makeText(context, "Sale Successful", Toast.LENGTH_SHORT).show()
                                                        reviewHelper.launchReviewIfEligible()
                                                    } else {
                                                        Toast.makeText(context, sellResult.exceptionOrNull()?.message ?: "Sale failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = canSellQuantity,
                                            containerColor = Color(0xFFD50000),
                                            contentColor = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- KEY STATISTICS SECTION ---
                    financials?.let { stats ->
                        val isCrypto = stock.symbol.contains(":") || stock.symbol.endsWith("USD") 
                        val isForex = stock.symbol.contains("/")
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Key Statistics", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        val mktCap = (stats.metric?.get("marketCapitalization") as? Number)?.toDouble()
                                        val peRatio = (stats.metric?.get("peBasicExclExtraTTM") as? Number)?.toDouble()
                                        StatItem("Market Cap", mktCap?.let { String.format(Locale.US, "%.2fB", it / 1000) } ?: "N/A")
                                        if (!isCrypto && !isForex) {
                                            StatItem("P/E Ratio", peRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A", Alignment.End)
                                        } else {
                                            val high52 = (stats.metric?.get("52WeekHigh") as? Number)?.toDouble()
                                            StatItem("52W High", high52?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A", Alignment.End)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        if (!isCrypto && !isForex) {
                                            val high52 = (stats.metric?.get("52WeekHigh") as? Number)?.toDouble()
                                            val low52 = (stats.metric?.get("52WeekLow") as? Number)?.toDouble()
                                            StatItem("52W High", high52?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A")
                                            StatItem("52W Low", low52?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A", Alignment.End)
                                        } else {
                                            val low52 = (stats.metric?.get("52WeekLow") as? Number)?.toDouble()
                                            val beta = (stats.metric?.get("beta") as? Number)?.toDouble()
                                            StatItem("52W Low", low52?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A")
                                            StatItem("Beta", beta?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A", Alignment.End)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        if (!isCrypto && !isForex) {
                                            val beta = (stats.metric?.get("beta") as? Number)?.toDouble()
                                            val yield = (stats.metric?.get("dividendYieldIndicatedAnnual") as? Number)?.toDouble() 
                                                ?: (stats.metric?.get("dividendYield5Y") as? Number)?.toDouble()
                                            StatItem("Beta", beta?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A")
                                            StatItem("Yield", yield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "N/A", Alignment.End)
                                        } else {
                                            val yield = (stats.metric?.get("dividendYieldIndicatedAnnual") as? Number)?.toDouble()
                                            StatItem("Yield", yield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "N/A")
                                            StatItem("", "") // Placeholder for alignment
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- NEXT EARNINGS SECTION ---
                    if (earnings?.earningsCalendar?.isNotEmpty() == true) {
                        val nextEarnings = earnings.earningsCalendar.first()
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Next Earnings: ", color = Color.Gray, fontSize = 14.sp)
                                    Text(text = formatDate(nextEarnings.date), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // --- COMBINED ANALYST SENTIMENT & AI PREDICTION ---
                    if (recommendations.isNotEmpty() || aiRecommendation != null) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Market Analysis", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = DarkGrayBlack,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column {
                                    if (recommendations.isNotEmpty()) {
                                        val rec = recommendations.first()
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text("Analyst Sentiment", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                RecItem("Buy", rec.buy + rec.strongBuy, Color.Green)
                                                RecItem("Hold", rec.hold, Color.Gray)
                                                RecItem("Sell", rec.sell + rec.strongSell, Color.Red)
                                            }
                                        }
                                        if (aiRecommendation != null) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        }
                                    }

                                    aiRecommendation?.let { recommendation ->
                                        AIRecommendationContent(recommendation)
                                    }
                                }
                            }
                        }
                    }

                    // --- GEMINI AI ANALYSIS SECTION ---
                    item {
                        AIAnalysisSection(state.aiAnalysis) {
                            viewModel.triggerAiAnalysis()
                        }
                    }

                    // --- PEERS SECTION ---
                    if (peers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Similar Companies", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(peers) { peer ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { navController.navigate(Screen.Details.createRoute(peer)) },
                                        label = { Text(peer) },
                                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        }
                    }

                    // --- NEWS SECTION ---
                    if (newsArticles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Latest News", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        items(newsArticles.take(5)) { article ->
                            NewsArticleItem(article) {
                                val intent = Intent(Intent.ACTION_VIEW, article.url.toUri())
                                context.startActivity(intent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }

                // --- MODAL SHEETS ---
                if (showAlertDialog) {
                    PriceAlertSheet(
                        symbol = stock.symbol,
                        priceAlerts = priceAlerts,
                        onDismiss = { showAlertDialog = false },
                        onAddAlert = { price, above -> viewModel.addPriceAlert(price, above) },
                        onDeleteAlert = { viewModel.deletePriceAlert(it) }
                    )
                }

                if (showContractsSheet) {
                    TradeContractSheet(
                        stock = stock,
                        activeContracts = activeContracts,
                        viewModel = viewModel,
                        onDismiss = { showContractsSheet = false },
                        onCreateContract = { type, target, qty ->
                            coroutineScope.launch {
                                val res = viewModel.createContract(type, target, qty)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Contract created. This will execute when price targets are hit in the background.", Toast.LENGTH_LONG).show()
                                    showContractsSheet = false
                                } else {
                                    Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCancelContract = { viewModel.cancelContract(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertSheet(
    symbol: String,
    priceAlerts: List<com.rahul.stocksim.data.local.entity.PriceAlertEntity>,
    onDismiss: () -> Unit,
    onAddAlert: (Double, Boolean) -> Unit,
    onDeleteAlert: (com.rahul.stocksim.data.local.entity.PriceAlertEntity) -> Unit
) {
    var alertTargetPrice by remember { mutableStateOf("") }
    var alertIsAbove by remember { mutableStateOf(true) }

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Set Price Alert for $symbol",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = alertTargetPrice,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) alertTargetPrice = it },
                label = { Text("Target Price") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trigger when price is", modifier = Modifier.weight(1f))
                FilterChip(
                    selected = alertIsAbove,
                    onClick = { alertIsAbove = true },
                    label = { Text("Above") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = !alertIsAbove,
                    onClick = { alertIsAbove = false },
                    label = { Text("Below") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val target = alertTargetPrice.toDoubleOrNull()
                    if (target != null) {
                        onAddAlert(target, alertIsAbove)
                        alertTargetPrice = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = alertTargetPrice.isNotEmpty()
            ) {
                Text("Set Alert")
            }

            if (priceAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Active Alerts", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                priceAlerts.forEach { alert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            if (alert.isAbove) "Above $${String.format(Locale.US, "%.2f", alert.targetPrice)}"
                            else "Below $${String.format(Locale.US, "%.2f", alert.targetPrice)}",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteAlert(alert) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeContractSheet(
    stock: Stock,
    activeContracts: List<TradeContract>,
    viewModel: StockDetailViewModel,
    onDismiss: () -> Unit,
    onCreateContract: (ContractType, Double, Long) -> Unit,
    onCancelContract: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var contractPrice by remember { mutableStateOf(stock.price.toString()) }
    var contractQuantity by remember { mutableStateOf("1") }
    
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Trading Desk",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Limit Orders") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Options") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // --- LIMIT ORDERS TAB ---
                Text("Set automated buy/sell targets for ${stock.symbol}", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = contractPrice,
                        onValueChange = { contractPrice = it },
                        label = { Text("Execution Price") },
                        prefix = { Text("$") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = contractQuantity,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) contractQuantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val target = contractPrice.toDoubleOrNull()
                            val qty = contractQuantity.toLongOrNull() ?: 0L
                            if (target != null && qty > 0) {
                                onCreateContract(ContractType.BUY_AT, target, qty)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Buy At Target") }
                    
                    Button(
                        onClick = {
                            val target = contractPrice.toDoubleOrNull()
                            val qty = contractQuantity.toLongOrNull() ?: 0L
                            if (target != null && qty > 0) {
                                onCreateContract(ContractType.SELL_AT, target, qty)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Sell At Target") }
                }
            } else {
                // --- OPTIONS TAB ---
                val detailViewModel: StockDetailViewModel = hiltViewModel()
                SimulatedOptionsView(stock, detailViewModel)
            }

            // --- PENDING CONTRACTS (Always visible) ---
            if (activeContracts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Pending Contracts", fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                activeContracts.forEach { contract ->
                    Card(
                        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val label = when(contract.type) {
                                        ContractType.BUY_AT -> "Buy"
                                        ContractType.SELL_AT -> "Sell"
                                        ContractType.CALL_OPTION -> "Call"
                                        ContractType.PUT_OPTION -> "Put"
                                    }
                                    val color = if (contract.type == ContractType.BUY_AT || contract.type == ContractType.CALL_OPTION) 
                                        Color(0xFF4CAF50) else Color(0xFFF44336)
                                    
                                    Text(
                                        text = "$label ${contract.quantity} ${if(contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION) "contracts" else "shares"}", 
                                        color = color, 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Target: $${String.format(Locale.US, "%.2f", contract.targetPrice)}", 
                                        color = Color.White.copy(alpha = 0.7f), 
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = { 
                                    if (contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION) {
                                        coroutineScope.launch {
                                            val res = viewModel.settleOption(contract, stock.price)
                                            if (res.isSuccess) {
                                                val strike = contract.targetPrice
                                                val current = stock.price
                                                val isCall = contract.type == ContractType.CALL_OPTION
                                                val intrinsicValue = if (isCall) {
                                                    (current - strike).coerceAtLeast(0.0)
                                                } else {
                                                    (strike - current).coerceAtLeast(0.0)
                                                }
                                                val totalSettlement = intrinsicValue * 100 * contract.quantity
                                                
                                                if (totalSettlement > 0) {
                                                    Toast.makeText(context, "Option closed! Profit of $${String.format(Locale.US, "%.2f", totalSettlement)} added to balance.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Option closed at $0.00 value.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Failed to close option: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        onCancelContract(contract.id)
                                    }
                                }) {
                                    val icon = if (contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION) Icons.Default.CheckCircle else Icons.Default.Close
                                    Icon(icon, contentDescription = "Close", tint = if (icon == Icons.Default.CheckCircle) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color.Gray)
                                }
                            }
                            
                            // Show P/L for options
                            if (contract.type == ContractType.CALL_OPTION || contract.type == ContractType.PUT_OPTION) {
                                val strike = contract.targetPrice
                                val current = stock.price
                                val isCall = contract.type == ContractType.CALL_OPTION
                                val intrinsicValue = if (isCall) {
                                    (current - strike).coerceAtLeast(0.0)
                                } else {
                                    (strike - current).coerceAtLeast(0.0)
                                }
                                val currentTotalValue = intrinsicValue * 100 * contract.quantity
                                val costBasis = (contract.premium ?: 0.0) * 100 * contract.quantity
                                val profit = currentTotalValue - costBasis
                                
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                                
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Value: $${String.format(Locale.US, "%.2f", currentTotalValue)}", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = "P/L: ${if (profit >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", profit)}",
                                        color = if (profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedOptionsView(stock: Stock, viewModel: StockDetailViewModel) {
    var selectedCall by remember { mutableStateOf(true) }
    var strikePrice by remember { mutableStateOf(String.format(Locale.US, "%.0f", stock.price)) }
    var contractsQuantity by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()
    
    Column {
        Text("Create Simulated Option Contract", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = selectedCall,
                onClick = { selectedCall = true },
                label = { Text("Call Option") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF4CAF50),
                    labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCall,
                    borderColor = Color.Gray.copy(alpha = 0.3f),
                    selectedBorderColor = Color(0xFF4CAF50)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !selectedCall,
                onClick = { selectedCall = false },
                label = { Text("Put Option") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF44336).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFFF44336),
                    labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !selectedCall,
                    borderColor = Color.Gray.copy(alpha = 0.3f),
                    selectedBorderColor = Color(0xFFF44336)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = strikePrice,
            onValueChange = { strikePrice = it },
            label = { Text("Strike Price") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Contracts (100 shares each)", color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (contractsQuantity > 1) contractsQuantity-- }) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                }
                Text(
                    text = contractsQuantity.toString(),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { contractsQuantity++ }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Simulated Premium Calculation
        val strike = strikePrice.toDoubleOrNull() ?: stock.price
        val diff = kotlin.math.abs(strike - stock.price) / stock.price
        val basePremium = stock.price * 0.05 // 5% base premium
        val adjustedPremium = if (selectedCall) {
            if (strike < stock.price) basePremium + (stock.price - strike) else basePremium / (1 + diff * 10)
        } else {
            if (strike > stock.price) basePremium + (strike - stock.price) else basePremium / (1 + diff * 10)
        }
        
        val totalCost = adjustedPremium * 100 * contractsQuantity
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Option Details", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailRow("Est. Premium", "$${String.format(Locale.US, "%.2f", adjustedPremium)} / share")
                DetailRow("Break Even", "$${String.format(Locale.US, "%.2f", if(selectedCall) strike + adjustedPremium else strike - adjustedPremium)}")
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                DetailRow("Total Cost", "$${String.format(Locale.US, "%,.2f", totalCost)}", isBold = true)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                scope.launch {
                    val res = viewModel.buyOption(selectedCall, strike, adjustedPremium, contractsQuantity)
                    if (res.isSuccess) {
                        Toast.makeText(viewModel.getApplicationContext(), "Option purchased successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(viewModel.getApplicationContext(), res.exceptionOrNull()?.message ?: "Failed to buy", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Buy $contractsQuantity Contract${if (contractsQuantity > 1) "s" else ""}", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Note: Simulated options expire in 30 days. Profits are settled automatically on expiration.",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AIRecommendationSection(recommendation: AIRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        AIRecommendationContent(recommendation)
    }
}

@Composable
fun AIRecommendationContent(recommendation: AIRecommendation) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Prediction",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Surface(
                color = when (recommendation.advice) {
                    "BUY", "Strong Buy", "Buy" -> Color(0xFF00C853).copy(alpha = 0.1f)
                    "SELL", "Strong Sell", "Sell" -> Color.Red.copy(alpha = 0.1f)
                    else -> Color.Gray.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = recommendation.advice,
                    color = when (recommendation.advice) {
                        "BUY", "Strong Buy", "Buy" -> Color(0xFF00C853)
                        "SELL", "Strong Sell", "Sell" -> Color.Red
                        else -> Color.Gray
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Confidence Meter
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confidence Score", color = Color.Gray, fontSize = 12.sp)
                Text("${recommendation.confidence}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { recommendation.confidence / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    recommendation.confidence > 75 -> Color(0xFF00C853)
                    recommendation.confidence > 50 -> Color.Yellow
                    else -> Color.Red
                },
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text("Key Drivers", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
        
        recommendation.reasons.forEach { reason ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = reason, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AIAnalysisSection(analysis: String?, onExpand: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { 
                expanded = !expanded 
                if (expanded && analysis == null) onExpand()
            }
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .then(
                    if (expanded && analysis != null) {
                        Modifier.heightIn(max = 400.dp)
                    } else Modifier
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Gemini AI Insights",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                if (analysis != null) {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        MarkdownText(
                            markdown = analysis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(markdown) {
        parseMarkdown(markdown)
    }
    Text(
        text = annotatedString,
        color = Color.White.copy(alpha = 0.9f),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
        modifier = modifier
    )
}

private fun parseMarkdown(markdown: String): androidx.compose.ui.text.AnnotatedString {
    // 1. Replace bullet point asterisks (* ) at the start of lines with real bullet characters (• )
    val bulletRegex = Regex("(?m)^\\s*\\*\\s+")
    val textWithBullets = markdown.replace(bulletRegex, "• ")

    return androidx.compose.ui.text.buildAnnotatedString {
        val lines = textWithBullets.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isHeader = false
            
            // Handle Headers
            when {
                currentLine.startsWith("### ") -> {
                    isHeader = true
                    currentLine = currentLine.removePrefix("### ")
                }
                currentLine.startsWith("## ") -> {
                    isHeader = true
                    currentLine = currentLine.removePrefix("## ")
                }
                currentLine.startsWith("# ") -> {
                    isHeader = true
                    currentLine = currentLine.removePrefix("# ")
                }
            }

            if (isHeader) {
                pushStyle(androidx.compose.ui.text.SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFBB86FC),
                    fontSize = 17.sp
                ))
            }

            // Parse Bold and Italic inside the line
            var lineIndex = 0
            val mixedRegex = Regex("""(\*\*.*?\*\*)|(\*.*?\*)""")
            
            mixedRegex.findAll(currentLine).forEach { matchResult ->
                append(currentLine.substring(lineIndex, matchResult.range.first))
                val match = matchResult.value
                when {
                    match.startsWith("**") -> {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                        append(match.substring(2, match.length - 2))
                        pop()
                    }
                    match.startsWith("*") -> {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White.copy(alpha = 0.9f)))
                        append(match.substring(1, match.length - 1))
                        pop()
                    }
                }
                lineIndex = matchResult.range.last + 1
            }
            append(currentLine.substring(lineIndex))

            if (isHeader) pop()
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}


@Composable
fun InsiderTradingSection(transactions: List<FinnhubInsiderTransaction>) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = "Insider Transactions",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                transactions.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tx.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = tx.transactionDate, color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val color = if (tx.change > 0) Color.Green else Color.Red
                            Text(
                                text = "${if (tx.change > 0) "+" else ""}${String.format(Locale.US, "%,d", tx.change)}",
                                color = color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "at ${String.format(Locale.US, "$%,.2f", tx.transactionPrice)}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (tx != transactions.take(5).last()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun RecItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun StatItem(label: String, value: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun NewsArticleItem(article: FinnhubNewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = article.source.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = article.headline, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = article.summary, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (article.image.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(model = article.image, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        if (date != null) outputFormat.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}
