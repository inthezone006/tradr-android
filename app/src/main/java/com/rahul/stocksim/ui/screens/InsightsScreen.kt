package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.viewmodels.InsightsUiState
import com.rahul.stocksim.ui.viewmodels.InsightsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InsightsScreen(
    onStockClick: (Stock) -> Unit,
    onUpgradeClick: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val tabs = remember(isPro) {
        mutableListOf(
            Triple("Top Gainers", Icons.AutoMirrored.Filled.TrendingUp, 0),
            Triple("Top Losers", Icons.AutoMirrored.Filled.TrendingDown, 1),
            Triple("Sectors", Icons.Default.PieChart, 2),
            Triple("Indices", Icons.Default.Public, 3)
        ).apply {
            add(Triple("Pro Picks", Icons.Default.Star, 4))
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Insights",
            color = Color.White,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(16.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Color.White
                )
            }
        ) {
            tabs.forEach { (label, icon, index) ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is InsightsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = Color.White)
                    }
                }
                is InsightsUiState.Success -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) { pageIndex ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (pageIndex) {
                                0 -> { // Top Gainers
                                    items(state.insights.gainers.size) { index ->
                                        val stock = state.insights.gainers[index]
                                        MoverItem(stock) { onStockClick(stock) }
                                    }
                                }
                                1 -> { // Top Losers
                                    items(state.insights.losers.size) { index ->
                                        val stock = state.insights.losers[index]
                                        MoverItem(stock) { onStockClick(stock) }
                                    }
                                }
                                2 -> { // Sector Performance
                                    items(state.insights.sectors.size) { index ->
                                        val sector = state.insights.sectors[index]
                                        SectorItem(sector, modifier = Modifier.fillMaxWidth()) { onStockClick(sector) }
                                    }
                                }
                                3 -> { // Global Indices
                                    items(state.insights.indices.size) { index ->
                                        val stockIndex = state.insights.indices[index]
                                        IndexCard(stockIndex, modifier = Modifier.fillMaxWidth()) { onStockClick(stockIndex) }
                                    }
                                }
                                4 -> { // Pro Picks
                                    item {
                                        if (isPro) {
                                            ProRecommendationsContent(recommendations)
                                        } else {
                                            ProLockedContent(onUpgradeClick)
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
                is InsightsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.Red, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun ProRecommendationsContent(recommendations: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Market Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Surface(
                color = Color(0xFFFFD700).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "PRO",
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (recommendations == null) {
            Column(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gemini is analyzing the market...", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            // Split the text by numbers if it follows a numbered list format from the prompt
            val sections = recommendations.split(Regex("(?=\\d\\.)"))
            
            sections.forEach { section ->
                if (section.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = section.trim(),
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 22.sp,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Powered by Google Gemini. Recommendations are for informational purposes only.",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProLockedContent(onUpgradeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Pro Picks is Locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Upgrade to Tradr Pro to get AI-powered investment recommendations and deep market analysis.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUpgradeClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Upgrade to PRO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IndexCard(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!stock.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = stock.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = getIndexIcon(stock.symbol),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stock.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stock.name, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("$${String.format(Locale.US, "%,.2f", stock.price)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                val color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val prefix = if (stock.change >= 0) "+" else ""
                Text(
                    text = "$prefix${String.format(Locale.US, "%.2f", stock.percentChange)}%",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SectorItem(stock: Stock, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!stock.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = stock.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = getSectorIcon(stock.symbol),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stock.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stock.name, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("$${String.format(Locale.US, "%,.2f", stock.price)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                val color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val prefix = if (stock.change >= 0) "+" else ""
                Text(
                    text = "$prefix${String.format(Locale.US, "%.2f", stock.percentChange)}%",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getSectorIcon(symbol: String): ImageVector = when(symbol) {
    "XLK" -> Icons.Default.Memory
    "XLF" -> Icons.Default.AccountBalance
    "XLV" -> Icons.Default.MedicalServices
    "XLY" -> Icons.Default.ShoppingBag
    "XLP" -> Icons.Default.ShoppingCart
    "XLE" -> Icons.Default.Bolt
    "XLI" -> Icons.Default.Business
    "XLB" -> Icons.Default.Construction
    "XLRE" -> Icons.Default.HomeWork
    "XLU" -> Icons.Default.Lightbulb
    else -> Icons.Default.Category
}

private fun getIndexIcon(symbol: String): ImageVector = when(symbol) {
    "SPY" -> Icons.Default.Leaderboard
    "QQQ" -> Icons.Default.QueryStats
    "DIA" -> Icons.AutoMirrored.Filled.ShowChart
    "IWM" -> Icons.Default.AutoGraph
    else -> Icons.Default.Public
}

@Composable
fun MoverItem(stock: Stock, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!stock.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = stock.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = stock.symbol.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stock.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stock.name, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("$${String.format(Locale.US, "%,.2f", stock.price)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                val color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val prefix = if (stock.change >= 0) "+" else ""
                Text(
                    text = "$prefix${String.format(Locale.US, "%.2f", stock.percentChange)}%",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
