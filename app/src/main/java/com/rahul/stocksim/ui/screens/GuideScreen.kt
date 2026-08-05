package com.rahul.stocksim.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.ui.viewmodels.AuthViewModel
import com.rahul.stocksim.ui.theme.DarkGrayBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GuideScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val authRepository = viewModel.repository
    var isTutorialCompleted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isTutorialCompleted = authRepository.isTutorialCompleted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Guide",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            TutorialCard(
                    isCompleted = isTutorialCompleted,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(Screen.MarketTutorial.route) 
                    }
                )
            }

            item {
                GuideSection(
                    title = "The Life of a Stock",
                    icon = Icons.Default.Factory,
                    content = "How a private idea becomes a public asset traded by millions.",
                    detailedContent = "It starts with a Private Company. To grow, they seek 'Seeding' from Angel Investors. As they scale, 'Venture Capitalists' provide series A/B/C funding. Finally, the company goes through an IPO (Initial Public Offering) to list on exchanges like NASDAQ. At this point, the public (you!) can buy shares, which represent partial ownership of that company's future."
                )
            }

            item {
                GuideSection(
                    title = "How the Market Works",
                    icon = Icons.Default.Public,
                    content = "An invisible auction that never stops during business hours.",
                    detailedContent = "The Stock Market is essentially a giant auction house. For every buyer, there must be a seller. The 'Price' you see is the last agreed-upon value. If more people want to buy (Demand) than sell (Supply), the price goes up. News, earnings, and global events shift this balance every second."
                )
            }

            item {
                GuideSection(
                    title = "Portfolio Analytics",
                    icon = Icons.Default.QueryStats,
                    content = "Understanding your top performers and investment concentration.",
                    detailedContent = "Your Portfolio card now shows 'Concentration' (how much of your wealth is in one stock) and 'Diversification'. A 'High Diversification' means you've spread your risk across many companies. 'Day Return' shows your total profit or loss specifically for the current trading day."
                )
            }

            item {
                GuideSection(
                    title = "Analyst Sentiment",
                    icon = Icons.Default.Groups,
                    content = "Learn what professional analysts think about a stock's future performance.",
                    detailedContent = "The sentiment numbers come from major financial institutions (like JP Morgan, Goldman Sachs). They represent an aggregate of ratings: 'Strong Buy' and 'Buy' suggest a positive outlook, 'Hold' suggests neutral, and 'Sell' suggests analysts expect the price to drop. These are updated monthly."
                )
            }

            item {
                GuideSection(
                    title = "Technical Indicators (RSI)",
                    icon = Icons.Default.LegendToggle,
                    content = "Use the RSI indicator to identify if a stock is overvalued or undervalued.",
                    detailedContent = "RSI (Relative Strength Index) is a momentum oscillator measured on a scale of 0 to 100. Generally, an RSI above 70 indicates a stock is 'Overbought' (may be due for a price drop), and an RSI below 30 indicates it is 'Oversold' (may be due for a price bounce)."
                )
            }

            item {
                GuideSection(
                    title = "Moving Averages (SMA)",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    content = "Identifying long-term trends using 50-day and 200-day averages.",
                    detailedContent = "The SMA (Simple Moving Average) smooths out price spikes. A 'Bullish Cross' occurs when the short-term 50-day average moves above the long-term 200-day average, suggesting a strong upward trend. The opposite is a 'Bearish Cross'."
                )
            }

            item {
                GuideSection(
                    title = "ESG & Sustainability",
                    icon = Icons.Default.Eco,
                    content = "Invest ethically by checking a company's ESG scores.",
                    detailedContent = "ESG stands for Environmental, Social, and Governance. It measures a company's impact on the planet and its ethical standards. Higher scores suggest the company is well-managed and prepared for a sustainable future."
                )
            }

            item {
                GuideSection(
                    title = "Trade Contracts & Limit Orders",
                    icon = Icons.Default.Description,
                    content = "Automate your strategy with price-triggered buy and sell orders.",
                    detailedContent = "A Limit Order (BUY_AT or SELL_AT) allows you to set a specific price to enter or exit a position. For example, if Apple is at $190, you can set a 'BUY_AT' contract at $180. If the price drops to $180, tradr will automatically buy the shares for you. This helps you trade without constantly watching the screen."
                )
            }

            item {
                GuideSection(
                    title = "Simulated Options (Calls & Puts)",
                    icon = Icons.Default.SwapCalls,
                    content = "Speculate on price movements with leveraged option contracts.",
                    detailedContent = "Options give you the right to benefit from price changes without owning the stock. A 'CALL' is a bet that the price will go UP. A 'PUT' is a bet that the price will go DOWN. In tradr, options expire in 30 days. You pay a 'Premium' (small fee) to open the contract. If your prediction is right (In The Money) at expiration, you keep the profit! 1 contract controls 100 shares."
                )
            }

            item {
                GuideSection(
                    title = "Economic Calendar",
                    icon = Icons.Default.CalendarMonth,
                    content = "Track global economic events that move the markets.",
                    detailedContent = "The Macro screen shows major events like Inflation data (CPI), Interest Rate decisions, and Employment reports. These events often cause high volatility. Check the 'Impact' level and compare 'Actual' vs 'Forecast' values to see if the news was better or worse than expected for the economy."
                )
            }

            item {
                GuideSection(
                    title = "Gemini AI Insights",
                    icon = Icons.Default.AutoAwesome,
                    content = "Get AI-powered analysis for any stock in your watchlist.",
                    detailedContent = "tradr integrates Google Gemini to provide deep insights. It analyzes current market conditions, recent news, and technical data to give you a concise summary and outlook for specific stocks. Look for the ✨ icon in the stock detail screen."
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Knowledge is the best asset! 📈",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

@Composable
fun TutorialCard(isCompleted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) DarkGrayBlack else Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterStart)
            ) {
                Text(
                    text = "Interactive Tutorial",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isCompleted) Color.Gray else Color.Black,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Master the basics of trading",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCompleted) Color.DarkGray else Color.Black.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Completed", color = Color.Gray, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Training", color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = if (isCompleted) Color.DarkGray else Color.Black.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp)
            )
        }
    }
}

@Composable
fun MarketTutorialScreen(onComplete: () -> Unit, onDismiss: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) }
    var previewActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val steps = listOf(
        TutorialStep(
            title = "Welcome Trader!",
            description = "Welcome to tradr!",
            icon = Icons.Default.WavingHand
        ),
        TutorialStep(
            title = "Researching Stocks",
            description = "Use the search bar to find companies. Tap the icon to see it in action!",
            icon = Icons.Default.Search
        ),
        TutorialStep(
            title = "Executing a Trade",
            description = "Buying low and selling high is the goal. Tap the icon to simulate a purchase!",
            icon = Icons.Default.SwapHoriz
        ),
        TutorialStep(
            title = "Monitoring Performance",
            description = "Track your assets in the Portfolio. Tap the icon to see your holdings!",
            icon = Icons.Default.DonutLarge
        ),
        TutorialStep(
            title = "The Leaderboard",
            description = "Compete with others globally. Tap the icon to see the winners!",
            icon = Icons.Default.EmojiEvents
        ),
        TutorialStep(
            title = "Contracts & Options",
            description = "Master advanced trading. Tap to see how contracts and options work!",
            icon = Icons.Default.Description
        ),
        TutorialStep(
            title = "Market Intelligence",
            description = "Stay informed with Economic Calendar and Gemini AI. Tap to explore!",
            icon = Icons.Default.AutoAwesome
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Interactive Graphic with Animation
                    AnimatedGraphic(
                        icon = steps[currentStep].icon, 
                        stepIndex = currentStep,
                        onInteraction = { previewActive = true }
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "TutorialContent"
                    ) { stepIndex ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = steps[stepIndex].title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = steps[stepIndex].description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    // Interactive Progress Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(steps.size) { index ->
                            val size by animateDpAsState(if (index == currentStep) 12.dp else 8.dp, label = "dotSize")
                            val color by animateColorAsState(if (index == currentStep) Color.White else Color.DarkGray, label = "dotColor")
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(size + 8.dp) // Larger touch target
                                    .clip(CircleShape)
                                    .clickable { 
                                        currentStep = index 
                                        previewActive = false
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(size)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }

                // Bottom Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            currentStep--
                            previewActive = false
                        },
                        enabled = currentStep > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            tint = if (currentStep > 0) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    if (currentStep < steps.size - 1) {
                        IconButton(
                            onClick = { 
                                currentStep++ 
                                previewActive = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Continue",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onComplete
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Finish",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF00C853)
                            )
                        }
                    }
                }
            }

            // Preview Overlay
            AnimatedVisibility(
                visible = previewActive,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(expandFrom = Alignment.CenterVertically),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable { previewActive = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Hint text to dismiss
                        Text(
                            text = "Tap anywhere to dismiss preview",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        when (currentStep) {
                            1 -> TutorialSearchBarPreview()
                            2 -> TradePreview(onConfirm = { 
                                Toast.makeText(context, "Purchase Successful", Toast.LENGTH_SHORT).show()
                                previewActive = false 
                            })
                            3 -> PortfolioPreview()
                            4 -> LeaderboardPreview()
                            5 -> ContractsPreview()
                            6 -> AIIntelligencePreview()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialSearchBarPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        TextField(
            value = "NVIDIA (NVDA)",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search stocks...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            trailingIcon = { Icon(Icons.Default.Close, null, tint = Color.White) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            readOnly = true
        )
    }
}

@Composable
fun TradePreview(onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Buy AAPL", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shares:", color = Color.Gray)
                Text("10", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Market Price:", color = Color.Gray)
                Text("$190.20", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Purchase", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PortfolioPreview() {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Asset Allocation", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            PortfolioItemPreview("NVIDIA (NVDA)", "+12.4%", Color(0xFF00C853))
            Spacer(modifier = Modifier.height(12.dp))
            PortfolioItemPreview("Tesla (TSLA)", "-2.1%", Color.Red)
            Spacer(modifier = Modifier.height(12.dp))
            PortfolioItemPreview("Bitcoin (BTC)", "+0.8%", Color(0xFF00C853))
        }
    }
}

@Composable
fun PortfolioItemPreview(symbol: String, returns: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(symbol, color = Color.White)
        Text(returns, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LeaderboardPreview() {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Global Ranking", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LeaderboardItemPreview(1, "Warren Buffet", "$1.2M", Color.Yellow)
            Spacer(modifier = Modifier.height(12.dp))
            LeaderboardItemPreview(2, "Nancy Pelosi", "$840K", Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            LeaderboardItemPreview(3, "Retail Trader", "$120K", Color(0xFFCD7F32))
        }
    }
}

@Composable
fun LeaderboardItemPreview(rank: Int, name: String, value: String, rankColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("$rank.", color = rankColor, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Text(name, color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ContractsPreview() {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Active Contracts", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("AAPL CALL $200", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Expires in 28d", color = Color.Gray, fontSize = 12.sp)
                }
                Text("+$450.00", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TSLA BUY_AT $150", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Target: $150.00", color = Color.Gray, fontSize = 12.sp)
                }
                Text("Pending", color = Color.Yellow, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AIIntelligencePreview() {
    Card(
        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Market Intelligence", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("CPI Inflation Data", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Impact: High", color = Color.Red, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Gemini: Bullish outlook for NVDA based on recent earnings...", color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun AnimatedGraphic(icon: ImageVector, stepIndex: Int, onInteraction: () -> Unit) {
    var isTapped by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isTapped) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "graphicScale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer { translationY = floatOffset }
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { 
                onInteraction()
                coroutineScope.launch {
                    isTapped = true
                    delay(400)
                    isTapped = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        
        // Circular rotating background effect
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
                    .align(Alignment.TopCenter)
            )
        }
    }
}

data class TutorialStep(val title: String, val description: String, val icon: ImageVector)

@Composable
fun GuideSection(title: String, icon: ImageVector, content: String, detailedContent: String) {
    var showDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDialog = true 
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to learn more →",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = detailedContent,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDialog = false 
                }) {
                    Text("Got it!", color = Color.White)
                }
            }
        )
    }
}
