package com.rahul.stocksim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.ui.components.ModernTextField
import com.rahul.stocksim.ui.theme.RichBlack
import com.rahul.stocksim.ui.viewmodels.PortfolioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    
    val levels = listOf(
        BalanceLevel(1, 100000.0, "$100,000 (Beginner)"),
        BalanceLevel(2, 50000.0, "$50,000 (Casual)"),
        BalanceLevel(3, 25000.0, "$25,000 (Standard)"),
        BalanceLevel(4, 10000.0, "$10,000 (Intermediate)"),
        BalanceLevel(5, 5000.0, "$5,000 (Hard)"),
        BalanceLevel(6, 1000.0, "$1,000 (Difficult)"),
        BalanceLevel(7, 100.0, "$100 (Impossible)")
    )

    var selectedLevel by remember { mutableStateOf(levels[3]) }
    var portfolioName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Portfolio", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> InitialCapitalPage(levels, selectedLevel) { selectedLevel = it }
                    1 -> PortfolioNamePage(portfolioName) { portfolioName = it }
                }
            }

            // Bottom Navigation & Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Arrow or Placeholder
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Page Indicators (Dots)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) Color.White else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                // Next/Finish Button
                FloatingActionButton(
                    onClick = {
                        if (pagerState.currentPage == 0) {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        } else {
                            if (portfolioName.isNotBlank() && !isLoading) {
                                isLoading = true
                                scope.launch {
                                    val result = viewModel.createPortfolio(portfolioName, selectedLevel.amount)
                                    if (result.isSuccess) {
                                        navController.popBackStack()
                                    } else {
                                        isLoading = false
                                        // Could show a snackbar here if we had host state
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(
                            imageVector = if (pagerState.currentPage == 0) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InitialCapitalPage(
    levels: List<BalanceLevel>,
    selectedLevel: BalanceLevel,
    onLevelSelected: (BalanceLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Select Starting Balance",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose the initial capital for this strategy.",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            levels.forEach { level ->
                val isSelected = level == selectedLevel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) RichBlack else MaterialTheme.colorScheme.surface)
                        .selectable(
                            selected = isSelected,
                            onClick = { onLevelSelected(level) },
                            role = Role.RadioButton
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = level.label,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioNamePage(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Name your Portfolio",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Give this trading strategy a recognizable name.",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ModernTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Portfolio Name (e.g. Aggressive Growth)"
        )
    }
}
