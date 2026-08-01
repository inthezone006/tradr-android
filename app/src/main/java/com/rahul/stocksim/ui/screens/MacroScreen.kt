package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rahul.stocksim.data.FinnhubEconomicEntry
import com.rahul.stocksim.ui.viewmodels.MacroUiState
import com.rahul.stocksim.ui.viewmodels.MacroViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MacroScreen(
    viewModel: MacroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is MacroUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(color = Color.White)
                }
            }
            is MacroUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Economic Calendar",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    if (state.economicCalendar.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxHeight(0.8f), contentAlignment = Alignment.Center) {
                                Text("No economic events found for this week.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(state.economicCalendar) { entry ->
                            EconomicEventItem(entry)
                        }
                    }
                }
            }
            is MacroUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun EconomicEventItem(entry: FinnhubEconomicEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                ImpactBadge(impact = entry.impact)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = entry.event,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueColumn("Actual", entry.actual?.toString() ?: "--", entry.unit)
                ValueColumn("Estimate", entry.estimate?.toString() ?: "--", entry.unit)
                ValueColumn("Previous", entry.prev?.toString() ?: "--", entry.unit)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.time,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ValueColumn(label: String, value: String, unit: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(
            text = if (value != "--") "$value $unit" else value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ImpactBadge(impact: String) {
    val color = when (impact.lowercase()) {
        "high" -> Color(0xFFE57373)
        "medium" -> Color(0xFFFFB74D)
        "low" -> Color(0xFF81C784)
        else -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = impact.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
