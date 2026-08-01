package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.ui.screens.InsightsScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rahul.stocksim.data.AssetFilter
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.MarketRepository
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rahul.stocksim.ui.viewmodels.PortfolioViewModel
import com.rahul.stocksim.ui.viewmodels.MarketViewModel
import com.rahul.stocksim.ui.viewmodels.InsightsViewModel
import com.rahul.stocksim.ui.viewmodels.PortfolioUiState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.util.NetworkObserver
import com.rahul.stocksim.util.NetworkStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavController, 
    onStockClick: (Stock) -> Unit,
    portfolioViewModel: PortfolioViewModel = hiltViewModel(),
    marketViewModel: MarketViewModel = hiltViewModel()
) {
    val bottomNavController = rememberNavController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.networkStatus.collectAsState(initial = NetworkStatus.Available)
    val snackbarHostState = remember { SnackbarHostState() }
    
    val navItems = listOf(
        BottomNavItem.Portfolio,
        BottomNavItem.Market,
        BottomNavItem.Insights,
        BottomNavItem.Contracts,
        BottomNavItem.Leaderboard,
        BottomNavItem.Guide
    )
    
    val authRepository = remember { AuthRepository() }
    val user = authRepository.currentUser
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    var isTutorialCompleted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isTutorialCompleted = authRepository.isTutorialCompleted()
    }
    
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val performSearch = { query: String ->
        searchJob?.cancel()
        if (query.isNotEmpty()) {
            searchJob = coroutineScope.launch {
                delay(500)
                isSearching = true
                marketViewModel.searchStocks(query).collect { results ->
                    searchResults = results
                    isSearching = false
                }
            }
        } else {
            searchResults = emptyList()
        }
    }

    LaunchedEffect(networkStatus) {
        if (networkStatus == NetworkStatus.Unavailable) {
            snackbarHostState.showSnackbar(
                message = "No internet connection. Data may be outdated.",
                duration = SnackbarDuration.Indefinite,
                actionLabel = "Dismiss"
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { 
                        searchQuery = it
                        performSearch(it)
                    },
                    onSearch = { 
                        focusManager.clearFocus()
                    },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Search stocks...") },
                    leadingIcon = { 
                        if (searchActive) {
                            IconButton(onClick = { searchActive = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchActive && searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                            if (!searchActive) {
                                IconButton(onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    mainNavController.navigate(Screen.Achievements.route) 
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Achievements",
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                        .semantics { contentDescription = "Profile" }
                                        .clickable { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            mainNavController.navigate(Screen.Settings.route) 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val photoUrl = user?.photoUrl
                                    if (photoUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(photoUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = user?.email?.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!searchActive) Modifier.padding(horizontal = 16.dp, vertical = 8.dp) else Modifier),
                    colors = SearchBarDefaults.colors(
                        containerColor = if (searchActive) Color.Transparent else MaterialTheme.colorScheme.surface,
                        inputFieldColors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    ),
                    tonalElevation = 0.dp
                ) {
                    if (searchActive) {
                        if (isSearching) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.White)
                        }
                        
                        searchResults.forEach { stock ->
                            ListItem(
                                headlineContent = { Text(stock.symbol, color = Color.White) },
                                supportingContent = { Text(stock.name, color = Color.Gray) },
                                trailingContent = { Text("$${String.format("%,.2f", stock.price)}", color = Color.White) },
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    searchActive = false
                                    onStockClick(stock)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!searchActive) {
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 48.dp),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0)
                    ) {
                        val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    bottomNavController.navigate(item.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item == BottomNavItem.Guide && !isTutorialCompleted) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(8.dp)
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(28.dp),
                                            tint = if (selected) Color.White else Color.Gray
                                        )
                                    }
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color.Gray,
                                    disabledIconColor = Color.Gray,
                                    disabledTextColor = Color.Gray
                                ),
                                interactionSource = object : androidx.compose.foundation.interaction.MutableInteractionSource {
                                    override val interactions = kotlinx.coroutines.flow.emptyFlow<androidx.compose.foundation.interaction.Interaction>()
                                    override suspend fun emit(interaction: androidx.compose.foundation.interaction.Interaction) {}
                                    override fun tryEmit(interaction: androidx.compose.foundation.interaction.Interaction) = true
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Portfolio.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Portfolio.route) {
                    PortfolioScreen(mainNavController)
                }
                composable(BottomNavItem.Market.route) {
                    MarketScreen(
                        navController = mainNavController,
                        onStockClick = onStockClick,
                        onSettingsClick = { mainNavController.navigate(Screen.Settings.route) }
                    )
                }
                composable(BottomNavItem.Insights.route) {
                    InsightsScreen(onStockClick = onStockClick)
                }
                composable(BottomNavItem.Contracts.route) {
                    ContractsScreen(mainNavController)
                }
                composable(BottomNavItem.Leaderboard.route) {
                    LeaderboardScreen(mainNavController)
                }
                composable(BottomNavItem.Guide.route) {
                    GuideScreen(mainNavController)
                }
            }
        }
    }
}
