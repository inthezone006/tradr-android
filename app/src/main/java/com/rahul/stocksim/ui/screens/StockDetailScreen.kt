package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.firebase.analytics.FirebaseAnalytics
import com.rahul.stocksim.data.*
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
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
    val coroutineScope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val ownedQuantity by viewModel.ownedQuantity.collectAsState()
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val priceAlerts by viewModel.priceAlerts.collectAsState()
    val activeContracts by viewModel.activeContracts.collectAsState()
    
    val reviewHelper = remember { ReviewHelper(context as Activity) }

    var showAlertDialog by remember { mutableStateOf(false) }
    var showContractsSheet by remember { mutableStateOf(false) }
    var showActiveContractsListSheet by remember { mutableStateOf(false) }
    
    var selectedContract by remember { mutableStateOf<TradeContract?>(null) }
    var showContractActionSheet by remember { mutableStateOf(false) }
    
    val scrollState = rememberLazyListState()
    
    val isCollapsed by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset > 200)
        }
    }

    LaunchedEffect(stockSymbol) {
        if (stockSymbol != null) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, stockSymbol)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, stockSymbol)
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "stock")
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isCollapsed,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        val state = uiState
                        if (state is StockDetailUiState.Success) {
                            Column {
                                Text(
                                    text = state.stock.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", state.stock.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.stock.change >= 0) Color.Green else Color.Red
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Watchlist",
                            tint = if (isInWatchlist) Color.Red else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCollapsed) MaterialTheme.colorScheme.background else Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is StockDetailUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                }
                is StockDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error", color = Color.Red, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
                is StockDetailUiState.Success -> {
                    val stock = state.stock
                    val newsArticles = state.newsArticles
                    val peers = state.peers
                    val esgScores = state.esgScores
                    val recommendation = state.aiRecommendation

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding() + 100.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                    ) {
                        // --- HEADER SECTION ---
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (stock.logoUrl != null) {
                                            AsyncImage(
                                                model = stock.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Text(
                                            text = stock.symbol,
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = stock.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", stock.price)}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val color = if (stock.change >= 0) Color.Green else Color.Red
                                        val icon = if (stock.change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                                        val prefix = if (stock.change >= 0) "+" else ""
                                        
                                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$prefix${String.format(Locale.US, "%,.2f", stock.change)} ($prefix${String.format(Locale.US, "%,.2f", stock.percentChange)}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
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
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    periods.forEach { period ->
                                                        FilterChip(
                                                            modifier = Modifier.weight(1f),
                                                            selected = selectedPeriod == period,
                                                            onClick = { viewModel.refreshGraph(period) },
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

                        // --- MARKET INSIGHTS PAGER (Stats, About, Insider, AI) ---
                        item {
                            val pagerState = rememberPagerState(pageCount = { 4 })
                            val nestedScrollConnection = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource
                                    ): Offset {
                                        // Consume all unconsumed scroll to prevent parent window scrolling
                                        return available
                                    }
                                }
                            }
                            
                            LaunchedEffect(pagerState.currentPage) {
                                if (pagerState.currentPage == 3 && state.aiAnalysis == null && isPro) {
                                    viewModel.triggerAiAnalysis()
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.height(260.dp)
                                    ) { page ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(nestedScrollConnection)
                                                .padding(16.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            when (page) {
                                                0 -> StatsPage(state)
                                                1 -> AboutPage(state)
                                                2 -> InsidersPage(state)
                                                3 -> {
                                                    if (isPro) {
                                                        DeepAiPage(state)
                                                    } else {
                                                        StockAiLockedContent {
                                                            navController.navigate("upgrade_screen")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                HorizontalPagerIndicator(
                                    pagerState = pagerState,
                                    pageCount = 4,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    activeColor = Color.White,
                                    inactiveColor = Color.Gray.copy(alpha = 0.3f),
                                    indicatorWidth = 6.dp,
                                    spacing = 6.dp
                                )
                            }
                        }

                        if (ownedQuantity > 0 || recommendation != null) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your Position & Analysis",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        if (ownedQuantity > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    val unitLabel = when {
                                                        stock.isCrypto -> "Units Owned"
                                                        stock.isForex -> "Lots Owned"
                                                        else -> "Shares Owned"
                                                    }
                                                    Text(unitLabel, color = Color.Gray, fontSize = 11.sp)
                                                    Text(
                                                        text = ownedQuantity.toString(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Equity Value", color = Color.Gray, fontSize = 11.sp)
                                                    Text(
                                                        text = "$${String.format(Locale.US, "%,.2f", ownedQuantity * stock.price)}",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp
                                                    )
                                                }
                                            }
                                            
                                            if (recommendation != null) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                        
                                        if (recommendation != null) {
                                            AIRecommendationContent(recommendation)
                                        }
                                    }
                                }
                            }
                        }

                        // --- OPTIONS VIEW ---
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SimulatedOptionsView(stock, viewModel)
                        }

                        // --- TRADR CONTRACTS BUTTON ---
                        if (activeContracts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                PillButton(
                                    text = "View ${activeContracts.size} Active Contracts",
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = Color(0xFFFFD700).copy(alpha = 0.1f),
                                    contentColor = Color(0xFFFFD700),
                                    onClick = { showActiveContractsListSheet = true }
                                )
                            }
                        }

                        if (isPro) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                TechnicalIndicatorsSection(state)
                            }
                        }

                        // ESG Scores
                        if (esgScores != null) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                    Text("ESG Sustainability", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                            StatItem("Environment", esgScores.environmentScore.toString(), alignment = Alignment.CenterHorizontally)
                                            StatItem("Social", esgScores.socialScore.toString(), alignment = Alignment.CenterHorizontally)
                                            StatItem("Governance", esgScores.governanceScore.toString(), alignment = Alignment.CenterHorizontally)
                                        }
                                    }
                                }
                            }
                        }

                        // Price Alerts Section
                        if (priceAlerts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Your Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(priceAlerts) { alert ->
                                ListItem(
                                    headlineContent = { Text("Target: $${alert.targetPrice}", color = Color.White) },
                                    supportingContent = { Text(if (alert.isAbove) "Notify when above" else "Notify when below", color = Color.Gray) },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.deletePriceAlert(alert) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }

                        // News Section
                        if (newsArticles.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text("Latest News", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            items(newsArticles.take(5)) { article ->
                                NewsArticleItem(article) { }
                            }
                        }
                        
                        // Peers Section
                        if (peers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text("Similar Stocks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    peers.forEach { peer ->
                                        Surface(
                                            modifier = Modifier.clickable { navController.navigate(Screen.Details.createRoute(peer)) },
                                            color = Color.DarkGray.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = peer,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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

            // Bottom Floating Bar
            val state = uiState
            if (state is StockDetailUiState.Success) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showContractsSheet = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRADE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        
                        IconButton(
                            onClick = { showAlertDialog = true },
                            modifier = Modifier.size(56.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showAlertDialog) {
        PriceAlertSheet(
            symbol = stockSymbol ?: "",
            existingAlerts = priceAlerts,
            onDismiss = { showAlertDialog = false },
            onAddAlert = { price, above -> viewModel.addPriceAlert(price, above) },
            onDeleteAlert = { viewModel.deletePriceAlert(it) }
        )
    }
    
    if (showContractsSheet) {
        val state = uiState
        if (state is StockDetailUiState.Success) {
            TradeContractSheet(
                stock = state.stock,
                activeContracts = activeContracts,
                viewModel = viewModel,
                onDismiss = { showContractsSheet = false },
                onCreateContract = { type, target, qty ->
                    coroutineScope.launch {
                        viewModel.createContract(type, target, qty)
                    }
                },
                onCancelContract = { viewModel.cancelContract(it) }
            )
        }
    }

    if (showActiveContractsListSheet) {
        ActiveContractsListSheet(
            activeContracts = activeContracts,
            onDismiss = { showActiveContractsListSheet = false },
            onContractClick = { contract ->
                selectedContract = contract
                showContractActionSheet = true
            },
            onCancelContract = { viewModel.cancelContract(it) }
        )
    }

    if (showContractActionSheet && selectedContract != null) {
        ContractDetailSheet(
            contract = selectedContract!!,
            onDismiss = { showContractActionSheet = false },
            onCancel = {
                viewModel.cancelContract(it.id)
                showContractActionSheet = false
            },
            onExecute = {
                showContractActionSheet = false
            }
        )
    }
}

@Composable
fun StatsPage(state: StockDetailUiState.Success) {
    val financials = state.financials
    val mktCap = financials?.metric?.get("marketCapitalization") as? Double
    val pe = financials?.metric?.get("peBasicExclExtraTTM") as? Double
    val weekHigh = financials?.metric?.get("52WeekHigh") as? Double
    val weekLow = financials?.metric?.get("52WeekLow") as? Double
    val eps = financials?.metric?.get("epsExclExtraItemsTTM") as? Double
    val divYield = financials?.metric?.get("dividendYieldIndicatedAnnual") as? Double
    val currentRsi = state.rsiData?.rsi?.lastOrNull() ?: 0.0
    val currentSma50 = state.sma50Data?.sma?.lastOrNull() ?: 0.0

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                StatItem("RSI (14)", String.format(Locale.US, "%.2f", currentRsi))
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("Market Cap", mktCap?.let { String.format(Locale.US, "$%.2fB", it / 1000) } ?: "N/A")
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("52W High", weekHigh?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A")
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("EPS (TTM)", eps?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                StatItem("50D SMA", String.format(Locale.US, "$%.2f", currentSma50), alignment = Alignment.End)
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("P/E Ratio", pe?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A", alignment = Alignment.End)
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("52W Low", weekLow?.let { String.format(Locale.US, "$%.2f", it) } ?: "N/A", alignment = Alignment.End)
                Spacer(modifier = Modifier.height(12.dp))
                StatItem("Div Yield", divYield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "0.00%", alignment = Alignment.End)
            }
        }
    }
}

@Composable
fun AboutPage(state: StockDetailUiState.Success) {
    val profile = state.profile
    val financials = state.financials
    Text(
        text = "About ${state.stock.name}",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = profile?.name?.let { "A major player in the ${profile.finnhubIndustry} sector, trading on the ${profile.exchange}." } 
              ?: financials?.metric?.get("marketCapitalization")?.let { "A leading company in the ${state.profile?.finnhubIndustry} sector with a market cap of $${String.format(Locale.US, "%,.2fB", (it as Double) / 1000)}." } 
              ?: "Information about this company is currently limited.",
        color = Color.LightGray,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatItem("Industry", profile?.finnhubIndustry ?: "N/A", alignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f))
        StatItem("Country", profile?.country ?: "N/A", alignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StockAiLockedContent(onUpgradeClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Deep AI Locked", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upgrade to Pro to unlock comprehensive AI-powered stock analysis.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onUpgradeClick,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("UPGRADE NOW", fontSize = 12.sp)
        }
    }
}

@Composable
fun InsidersPage(state: StockDetailUiState.Success) {
    val insiders = state.insiderTransactions
    Text("Insider Trading", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(12.dp))
    if (insiders.isEmpty()) {
        Text("No recent insider data.", color = Color.Gray, fontSize = 14.sp)
    } else {
        insiders.take(4).forEach { transaction ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (transaction.share > 0) "Buy" else "Sell", color = Color.Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val color = if (transaction.share > 0) Color.Green else Color.Red
                    Text(
                        text = String.format(Locale.US, "%,d", Math.abs(transaction.share)),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DeepAiPage(state: StockDetailUiState.Success) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Deep AI Analysis", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        if (state.aiAnalysis == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    if (state.aiAnalysis != null) {
        MarkdownText(markdown = state.aiAnalysis, modifier = Modifier.fillMaxWidth())
    } else {
        Text("Wait while Gemini creates a comprehensive analysis of the company's current position and outlook.", color = Color.Gray, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailSheet(
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
                    
                    DetailItem("Execution Target", "$${String.format(Locale.US, "%.2f", contract.targetPrice)}")
                    DetailItem("Quantity", "${contract.quantity} ${if(contract.quantity == 1L) "Unit" else "Units"}")
                    
                    if (contract.premium > 0) {
                        DetailItem("Premium Paid", "$${String.format(Locale.US, "%.2f", contract.premium)}")
                        val totalCost = contract.premium * 100 * contract.quantity
                        DetailItem("Total Investment", "$${String.format(Locale.US, "%,.2f", totalCost)}")
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
fun TechnicalIndicatorsSection(state: StockDetailUiState.Success) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QueryStats, contentDescription = null, tint = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Pro Technical Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                IndicatorItem("RSI (14)", state.tdRsi.firstOrNull()?.rsi ?: "N/A")
                IndicatorItem("MACD", state.tdMacd.firstOrNull()?.macd ?: "N/A")
            }
            Column(modifier = Modifier.weight(1f)) {
                IndicatorItem("EMA (20)", state.tdEma20?.toString() ?: "N/A")
                IndicatorItem("Bollinger Upper", state.tdBbands?.upper_band ?: "N/A")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val rsi = state.tdRsi.firstOrNull()?.rsi?.toDoubleOrNull()
        if (rsi != null) {
            val (signal, color) = when {
                rsi < 30 -> "OVERSOLD (Bullish)" to Color.Green
                rsi > 70 -> "OVERBOUGHT (Bearish)" to Color.Red
                else -> "NEUTRAL" to Color.Gray
            }
            
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "RSI Signal: $signal",
                    color = color,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun IndicatorItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertSheet(
    symbol: String,
    existingAlerts: List<PriceAlertEntity>,
    onDismiss: () -> Unit,
    onAddAlert: (Double, Boolean) -> Unit,
    onDeleteAlert: (PriceAlertEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var targetPrice by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Price Alert", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            TextField(
                value = targetPrice,
                onValueChange = { targetPrice = it },
                label = { Text("Target Price ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notify when price is:")
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(
                    selected = isAbove,
                    onClick = { isAbove = true },
                    label = { Text("Above") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = !isAbove,
                    onClick = { isAbove = false },
                    label = { Text("Below") }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val price = targetPrice.toDoubleOrNull()
                    if (price != null) {
                        onAddAlert(price, isAbove)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Set Alert", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveContractsListSheet(
    activeContracts: List<TradeContract>,
    onDismiss: () -> Unit,
    onContractClick: (TradeContract) -> Unit,
    onCancelContract: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Active Contracts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            if (activeContracts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No active contracts", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(activeContracts) { contract ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    text = "${contract.type.name.replace("_", " ")} @ $${String.format(Locale.US, "%,.2f", contract.targetPrice)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            supportingContent = { Text("Qty: ${contract.quantity} | ${contract.status}", color = Color.Gray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { 
                                onContractClick(contract)
                            }
                        )
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val ownedQuantity by viewModel.ownedQuantity.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } 
    var quantityInput by remember { mutableStateOf("1") }
    val quantity = quantityInput.toLongOrNull() ?: 0L
    
    var selectedLimitType by remember { mutableStateOf(ContractType.BUY_AT) }
    var targetPriceInput by remember { mutableStateOf(stock.price.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Trade ${stock.symbol}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Market") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Limit") })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Shared Balance & Quantity UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Buying Power: $${String.format(Locale.US, "%,.2f", balance)}", color = Color.Gray, fontSize = 14.sp)
                    if (ownedQuantity > 0) {
                        Text("Owned: $ownedQuantity", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { 
                            val current = quantityInput.toLongOrNull() ?: 0L
                            if (current > 1) quantityInput = (current - 1).toString()
                        },
                        modifier = Modifier.size(48.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }

                    TextField(
                        value = quantityInput,
                        onValueChange = { if (it.length <= 6) quantityInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
                        ),
                        trailingIcon = {
                            TextButton(onClick = { 
                                val max = if (selectedTab == 0) (balance / stock.price).toInt() else 999
                                quantityInput = max.toString() 
                            }) {
                                Text("MAX")
                            }
                        }
                    )

                    IconButton(
                        onClick = { 
                            val current = quantityInput.toLongOrNull() ?: 0L
                            quantityInput = (current + 1).toString()
                        },
                        modifier = Modifier.size(48.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }
                
                if (selectedTab == 1) { 
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ContractType.BUY_AT, ContractType.SELL_AT).forEach { type ->
                            FilterChip(
                                selected = selectedLimitType == type,
                                onClick = { selectedLimitType = type },
                                label = { Text(type.name.replace("_", " ")) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        label = { Text("Target Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (selectedTab == 0) { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.buyStock(quantity.toInt(), stock.price)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Order Executed!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            enabled = quantity > 0 && balance >= quantity * stock.price
                        ) {
                            Text("BUY", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.buyStock(-quantity.toInt(), stock.price)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Order Executed!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            enabled = quantity > 0 && ownedQuantity >= quantity
                        ) {
                            Text("SELL", fontWeight = FontWeight.Bold)
                        }
                    }
                } else { 
                    Button(
                        onClick = {
                            val target = targetPriceInput.toDoubleOrNull()
                            if (target != null && quantity > 0) {
                                onCreateContract(selectedLimitType, target, quantity)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = quantity > 0
                    ) {
                        Text("Create ${selectedLimitType.name.replace("_", " ")} Contract", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedOptionsView(
    stock: Stock, 
    detailViewModel: StockDetailViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var isCallSelected by remember { mutableStateOf(true) }
    val selectedContracts by remember { mutableIntStateOf(1) }
    
    val basePremium = stock.price * 0.05 
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Options Trading", color = Color.White, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            PillButton(
                text = "CALLS",
                modifier = Modifier.weight(1f),
                containerColor = if (isCallSelected) Color.Green.copy(alpha = 0.2f) else Color.Transparent,
                contentColor = if (isCallSelected) Color.Green else Color.Gray,
                onClick = { isCallSelected = true }
            )
            Spacer(modifier = Modifier.width(8.dp))
            PillButton(
                text = "PUTS",
                modifier = Modifier.weight(1f),
                containerColor = if (!isCallSelected) Color.Red.copy(alpha = 0.2f) else Color.Transparent,
                contentColor = if (!isCallSelected) Color.Red else Color.Gray,
                onClick = { isCallSelected = false }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val strikes = if (isCallSelected) {
            listOf(stock.price * 1.05, stock.price * 1.10, stock.price * 1.15)
        } else {
            listOf(stock.price * 0.95, stock.price * 0.90, stock.price * 0.85)
        }
        
        strikes.forEach { strike ->
            val dist = Math.abs(strike - stock.price) / stock.price
            val adjustedPremium = basePremium / (1 + dist * 5)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Strike: $${String.format(Locale.US, "%,.2f", strike)}", color = Color.White, fontWeight = FontWeight.Medium)
                    Text("Premium: $${String.format(Locale.US, "%,.2f", adjustedPremium)}", color = Color.Gray, fontSize = 12.sp)
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = detailViewModel.buyOption(isCallSelected, strike, adjustedPremium, selectedContracts)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Option contract purchased!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCallSelected) Color(0xFF4CAF50) else Color(0xFFF44336),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("BUY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Each contract is for 100 shares. Expiration 30 days.", color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun AIRecommendationSection(recommendation: AIRecommendation) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Investment Thesis",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        AIRecommendationContent(recommendation)
    }
}

@Composable
fun AIRecommendationContent(recommendation: AIRecommendation) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (color, icon) = when (recommendation.advice) {
                "BUY", "STRONG_BUY" -> Color.Green to Icons.Default.TrendingUp
                "SELL", "STRONG_SELL" -> Color.Red to Icons.Default.TrendingDown
                else -> Color.Yellow to Icons.Default.HorizontalRule
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recommendation.advice,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
            
            Text(
                text = "${recommendation.confidence}%",
                color = Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        recommendation.reasons.take(3).forEach { reason ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = reason,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun AIAnalysisSection(analysis: String?, onExpand: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Deep AI Analysis",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            if (analysis == null) {
                TextButton(onClick = onExpand) {
                    Text("Analyze Now", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (analysis != null) {
                    MarkdownText(
                        markdown = analysis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Get a comprehensive analysis of the company's current position, risks, and outlook using Gemini AI.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
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
        modifier = modifier,
        color = Color.White,
        lineHeight = 22.sp,
        fontSize = 14.sp
    )
}

fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            val headerMatch = Regex("^(#{1,4})\\s+(.*)").find(trimmedLine)
            
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val content = headerMatch.groupValues[2]
                val fontSize = when(level) {
                    1 -> 22.sp
                    2 -> 19.sp
                    3 -> 17.sp
                    else -> 15.sp
                }
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Black, 
                    fontSize = fontSize, 
                    color = Color.White
                )) {
                    appendFormatted(content)
                }
            } else if (trimmedLine == "---" || trimmedLine == "***") {
                // Horizontal rule
                withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.2f))) {
                    append("────────────────────────────────")
                }
            } else if (trimmedLine.startsWith("**") && trimmedLine.endsWith("**") && trimmedLine.length > 4) {
                // Treat entire bold lines as section titles
                val content = trimmedLine.substring(2, trimmedLine.length - 2).trim()
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color.White
                )) {
                    append(content)
                }
            } else if ((trimmedLine.startsWith("*") || trimmedLine.startsWith("-")) && trimmedLine.length > 1) {
                // Bullet points
                val content = if (trimmedLine.get(1) == ' ') trimmedLine.substring(2) else trimmedLine.substring(1)
                append("  • ")
                appendFormatted(content.trim())
            } else {
                appendFormatted(line)
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendFormatted(text: String) {
    var currentIndex = 0
    // Order matters here to catch longer matches first
    // *** for bold italic
    // ** for bold
    // * for italic
    val combinedRegex = Regex("(\\*\\*\\*|\\*\\*|\\*)(.*?)\\1")
    val matches = combinedRegex.findAll(text)
    
    for (match in matches) {
        append(text.substring(currentIndex, match.range.first))
        val marker = match.groupValues[1]
        val content = match.groupValues[2]
        
        val style = when (marker) {
            "***" -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = Color.White)
            "**" -> SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)
            "*" -> SpanStyle(fontStyle = FontStyle.Italic, color = Color.White)
            else -> SpanStyle()
        }
        
        withStyle(style = style) {
            append(content)
        }
        currentIndex = match.range.last + 1
    }
    
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

@Composable
fun InsiderTradingSection(insiders: List<FinnhubInsiderTransaction>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Insider Trading", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            insiders.take(5).forEach { transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(transaction.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(if (transaction.share > 0) "Buy" else "Sell", color = Color.Gray, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val color = if (transaction.share > 0) Color.Green else Color.Red
                        Text(
                            text = if (transaction.share > 0) "BUY" else "SELL",
                            color = color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%,d shares", Math.abs(transaction.share)),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isValueBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = if (isValueBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun RecItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment, modifier = modifier) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun NewsArticleItem(article: FinnhubNewsArticle, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { 
            article.url?.let { uriHandler.openUri(it) }
            onClick() 
        }.padding(vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = article.source, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = article.headline, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(text = formatDate(article.datetime.toString()), color = Color.Gray, fontSize = 11.sp)
            }
            if (!article.image.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

fun formatDate(timestamp: String): String {
    return try {
        val date = Date(timestamp.toLong() * 1000)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        timestamp
    }
}
