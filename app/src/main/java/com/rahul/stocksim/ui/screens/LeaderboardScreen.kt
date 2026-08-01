package com.rahul.stocksim.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rahul.stocksim.ui.viewmodels.LeaderboardViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class LeaderboardUser(
    val id: String,
    val name: String,
    val totalAccountValue: Double,
    val photoUrl: String? = null,
    val level: Int = 4
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LeaderboardScreen(
    mainNavController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    
    val levels = listOf(0, 1, 2, 3, 4, 5, 6, 7)
    val pagerState = rememberPagerState(pageCount = { levels.size })
    val coroutineScope = rememberCoroutineScope()

    val leadersCache by viewModel.leadersCache.collectAsState()
    val loadingStates by viewModel.loadingStates.collectAsState()
    val errorMessages by viewModel.errorMessages.collectAsState()
    
    val currentUserId = viewModel.currentUserId
    val selectedLevelFilter = levels[pagerState.currentPage]

    LaunchedEffect(selectedLevelFilter) {
        viewModel.fetchLeadersIfNeeded(selectedLevelFilter)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Static Header Area
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Level Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = Color.White
                    )
                }
            ) {
                levels.forEachIndexed { index, level ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        text = { 
                            Text(
                                text = if (level == 0) "All" else "Level $level",
                                fontSize = 14.sp
                            ) 
                        }
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val level = levels[pageIndex]
            val leaders = leadersCache[level] ?: emptyList()
            val isLoading = loadingStates[level] ?: false
            val errorMessage = errorMessages[level]

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    val currentUserLeader = leaders.find { it.id == currentUserId }
                    val currentUserRank = if (currentUserLeader != null) {
                         leaders.indexOfFirst { it.id == currentUserId }.let { if (it != -1) it + 1 else null }
                    } else null
                    
                    if (currentUserLeader != null) {
                        CurrentUserSummary(rank = currentUserRank, userValue = currentUserLeader.totalAccountValue)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (isLoading && leaders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = Color.White)
                        }
                    }
                } else if (leaders.isEmpty() && errorMessage == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (level == 0) "No traders found." else "No traders found for Level $level.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else if (errorMessage != null && leaders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(errorMessage, color = Color.Gray)
                        }
                    }
                } else {
                    itemsIndexed(leaders) { index, user ->
                        LeaderCard(
                            rank = index + 1,
                            user = user, 
                            isCurrentUser = user.id == currentUserId
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun CurrentUserSummary(rank: Int?, userValue: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Your Global Rank", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = if (rank != null) "#$rank" else "Processing...",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Portfolio Value", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "$${String.format("%,.2f", userValue)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}





@Composable
fun LeaderCard(rank: Int, user: LeaderboardUser, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                color = when(rank) {
                    1 -> Color(0xFFFFD700) // Gold
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.Gray
                },
                fontWeight = FontWeight.Bold,
                fontSize = if (rank >= 100) 14.sp else 18.sp,
                modifier = Modifier.width(44.dp),
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                if (user.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(user.name.firstOrNull()?.toString()?.uppercase() ?: "?", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name, 
                        color = Color.White, 
                        fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "👑", fontSize = 14.sp)
                    }
                }
                Text(text = "Level ${user.level}", color = Color.Gray, fontSize = 12.sp)
            }

            Text(
                text = "$${String.format("%,.0f", user.totalAccountValue)}",
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}



