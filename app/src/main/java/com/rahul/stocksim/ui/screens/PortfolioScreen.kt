package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.StockPricePoint
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow
import com.rahul.stocksim.ui.components.VicoLineChart
import com.rahul.stocksim.ui.components.VicoPieChart
import com.rahul.stocksim.ui.viewmodels.PortfolioUiState
import com.rahul.stocksim.ui.viewmodels.PortfolioViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val uiState by viewModel.uiState.collectAsState()
    val portfolioHistory by viewModel.portfolioHistory.collectAsState()
    
    var showAiSheet by remember { mutableStateOf(false) }

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
                val portfolioItems = state.portfolioItems
                val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
                val totalAccountValue = balance + totalStockValue
                
                // Portfolio Analytics
                val dayChange = portfolioItems.sumOf { it.first.change * it.second }
                val dayChangePercent = if (totalStockValue > 0) (dayChange / (totalStockValue - dayChange)) * 100 else 0.0
                
                val bestPerformer = portfolioItems.maxByOrNull { it.first.percentChange }
                val largestHolding = portfolioItems.maxByOrNull { it.first.price * it.second }
                val stockConcentration = if (totalAccountValue > 0) (totalStockValue / totalAccountValue) * 100 else 0.0
                val singleStockConcentration = if (totalAccountValue > 0) ((largestHolding?.let { it.first.price * it.second } ?: 0.0) / totalAccountValue) * 100 else 0.0

                // Entrance animation state
                var listVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    listVisible = true
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Portfolio",
                            color = Color.White,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Enhanced Fintech Header with Deep Insights
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            val pagerState = rememberPagerState(pageCount = { 4 })

                            // Trigger AI Analysis when swiping to the 4th page
                            LaunchedEffect(pagerState.currentPage) {
                                if (pagerState.currentPage == 3 && state.aiAnalysis == null) {
                                    viewModel.triggerAiPortfolioAnalysis()
                                }
                            }

                            val industryData = remember(portfolioItems) {
                                portfolioItems.groupBy { it.first.industry ?: "Unknown" }
                                    .mapValues { entry -> entry.value.sumOf { it.first.price * it.second } }
                            }
                            
                            Column {
                                val cardHeight by animateDpAsState(
                                    targetValue = when (pagerState.currentPage) {
                                        0 -> 290.dp // Overview with chart
                                        1 -> (40 + (industryData.size * 35)).dp.coerceIn(160.dp, 450.dp) // Dynamic height for industries
                                        2 -> 250.dp // Insights
                                        3 -> if (state.aiAnalysis != null) 500.dp else 250.dp // AI Analysis (Taller for bigger text)
                                        else -> 250.dp
                                    },
                                    label = "CardHeightAnimation"
                                )
                                
                                HorizontalPager(state = pagerState) {
                                    page ->
                                    val isTopAligned = page == 0 || page == 1 || page == 3
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 0.dp)
                                            .height(cardHeight),
                                        verticalArrangement = if (isTopAligned) Arrangement.Top else Arrangement.Center
                                    ) {
                                        when (page) {
                                            0 -> { // Overview Card
                                                Text(text = "Total Account Value", color = Color.Gray, fontSize = 14.sp)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    AnimatedContent(
                                                        targetState = totalAccountValue,
                                                        transitionSpec = {
                                                            if (targetState > initialState) {
                                                                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                                            } else {
                                                                (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                                                            }.using(SizeTransform(clip = false))
                                                        },
                                                        label = "TotalValueTicker"
                                                    ) { value ->
                                                        Text(
                                                            text = "$${String.format("%,.2f", value)}",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.displayLarge,
                                                            fontSize = 32.sp // Override size but keep font family
                                                        )
                                                    }
                                                    
                                                    Surface(
                                                        color = (if (dayChange >= 0) Color.Green else Color.Red).copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = if (dayChange >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                                contentDescription = null,
                                                                tint = if (dayChange >= 0) Color.Green else Color.Red,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "${if (dayChange >= 0) "+" else ""}${String.format("%,.2f%%", dayChangePercent)}",
                                                                color = if (dayChange >= 0) Color.Green else Color.Red,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                if (portfolioHistory.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(24.dp))
                                                    VicoLineChart(
                                                        history = portfolioHistory,
                                                        lineColor = if (dayChange >= 0) Color.Green else Color.Red,
                                                        modifier = Modifier.fillMaxWidth().height(180.dp)
                                                    )
                                                }
                                            }
                                            1 -> { // Diversification Card
                                                if (portfolioItems.isNotEmpty()) {
                                                    Text(
                                                        text = "Industry Diversification",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                    VicoPieChart(
                                                        data = industryData,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                            2 -> { // Insights/Breakdown Card
                                                // Invested vs Available breakdown
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    InsightSummaryItem(
                                                        modifier = Modifier.weight(1f),
                                                        label = "Stocks Value",
                                                        value = "$${String.format("%,.2f", totalStockValue)}",
                                                        subValue = "${String.format("%.1f%%", stockConcentration)} of total",
                                                        icon = Icons.Default.BarChart,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    InsightSummaryItem(
                                                        modifier = Modifier.weight(1f),
                                                        label = "Buying Power",
                                                        value = "$${String.format("%,.2f", balance)}",
                                                        subValue = "Available to trade",
                                                        icon = Icons.Default.AccountBalanceWallet,
                                                        color = Color.Green
                                                    )
                                                }
                
                                                Spacer(modifier = Modifier.height(20.dp))
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                Spacer(modifier = Modifier.height(20.dp))
                
                                                // Deep Insights Row
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    InsightItem(
                                                        label = "Portfolio Risk",
                                                        value = if (singleStockConcentration > 50) "High" else if (singleStockConcentration > 25 || portfolioItems.size < 3) "Medium" else "Low",
                                                        icon = Icons.Default.Warning,
                                                        color = if (singleStockConcentration > 50) Color.Red else if (singleStockConcentration > 25 || portfolioItems.size < 3) Color(0xFFFFA726) else Color.Green
                                                    )
                                                    InsightItem(
                                                        label = "Top Performer",
                                                        value = bestPerformer?.first?.symbol ?: "N/A",
                                                        icon = Icons.Default.Star,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                    InsightItem(
                                                        label = "Largest Asset",
                                                        value = largestHolding?.first?.symbol ?: "N/A",
                                                        icon = Icons.Default.PieChart,
                                                        color = Color.Cyan
                                                    )
                                                }
                                            }
                                            3 -> { // AI Analysis Card
                                                val analysis = state.aiAnalysis
                                                if (analysis != null) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBB86FC))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("AI Portfolio Analysis", color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column {
                                                            Text("Risk Level", color = Color.Gray, fontSize = 12.sp)
                                                            Text(analysis.riskLevel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text("Diversification", color = Color.Gray, fontSize = 12.sp)
                                                            Text("${analysis.diversificationScore}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = analysis.outlook, 
                                                        color = Color.White.copy(alpha = 0.9f), 
                                                        fontSize = 13.sp, 
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                                        lineHeight = 18.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(14.dp))
                                                    Text("Strategic Recommendations:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Column(modifier = Modifier.fillMaxHeight()) {
                                                        analysis.recommendations.forEach { rec ->
                                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 6.dp)) {
                                                                Text("•", color = Color(0xFFBB86FC), fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                                                                Text(rec, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        LoadingIndicator(color = Color(0xFFBB86FC), modifier = Modifier.size(32.dp))
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text("Gemini is analyzing your portfolio...", color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalPagerIndicator(
                                    pagerState = pagerState,
                                    pageCount = pagerState.pageCount,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 12.dp),
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    inactiveColor = Color.Gray.copy(alpha = 0.5f),
                                    indicatorWidth = 8.dp,
                                    indicatorHeight = 8.dp,
                                    spacing = 6.dp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your Assets",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (portfolioItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(text = "No stocks owned yet", color = Color.Gray)
                            }
                        }
                    } else {
                        items(portfolioItems.size) { index ->
                            val (stock, quantity) = portfolioItems[index]
                            AnimatedVisibility(
                                visible = listVisible,
                                enter = fadeIn(animationSpec = tween(600, index * 100)) + 
                                        slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(600, index * 100))
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            navController.navigate(Screen.Details.createRoute(stock.symbol)) 
                                        },
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        StockRow(
                                            stock = stock
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$quantity ${if (stock.isCrypto) "units" else "shares"}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Value: $${String.format("%,.2f", stock.price * quantity)}",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
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
        
        // AI Portfolio Deep Dive Sheet
        if (showAiSheet) {
            val analysis = (uiState as? PortfolioUiState.Success)?.aiAnalysis
            if (analysis != null) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBB86FC))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Full AI Portfolio Analysis", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Risk Level", color = Color.Gray, fontSize = 12.sp)
                                    Text(analysis.riskLevel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Diversification", color = Color.Gray, fontSize = 12.sp)
                                    Text("${analysis.diversificationScore}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Market Outlook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(analysis.outlook, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Strategic Recommendations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        analysis.recommendations.forEach { rec ->
                            Row(
                                modifier = Modifier.padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                                Text(rec, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAiSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) {
                            Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightSummaryItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(text = subValue, color = color.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InsightItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.Gray) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}
