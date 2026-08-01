package com.rahul.stocksim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VicoPieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val chartColors = listOf(
        Color.White,
        Color(0xFF03DAC6),
        Color(0xFFCF6679),
        Color(0xFF00C853),
        Color(0xFFFFA726),
        Color(0xFF2196F3),
        Color(0xFF9C27B0)
    )

    Column(modifier = modifier) {
        data.forEach { (label, value) ->
            val total = data.values.sum()
            val percent = (value / total).toFloat()
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.width(100.dp),
                    maxLines = 1
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent)
                            .fillMaxHeight()
                            .background(
                                chartColors[data.keys.toList().indexOf(label) % chartColors.size],
                                CircleShape
                            )
                    )
                }
                
                Text(
                    text = String.format("%.1f%%", percent * 100),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp)
                )
            }
        }
    }
}
