package com.rahul.stocksim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rahul.stocksim.model.Stock

@Composable
fun StockRow(
    stock: Stock,
    modifier: Modifier = Modifier,
    onRowClick: ((Stock) -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onRowClick != null) {
                    Modifier.clickable { onRowClick(stock) }
                } else {
                    Modifier
                }
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
              // Logo Box
              Box(
                  modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(MaterialTheme.colorScheme.surface),
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

              Column(
                  modifier = Modifier.weight(1f)
              ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = stock.symbol,
                          style = MaterialTheme.typography.titleMedium,
                          color = Color.White
                      )
                      if (stock.isCrypto) {
                          Spacer(modifier = Modifier.width(6.dp))
                          Box(
                              modifier = Modifier
                                  .clip(RoundedCornerShape(4.dp))
                                  .background(Color(0xFFFFA726).copy(alpha = 0.2f))
                                  .padding(horizontal = 4.dp, vertical = 1.dp)
                          ) {
                              Text(
                                  text = "CRYPTO",
                                  color = Color(0xFFFFA726),
                                  fontSize = 9.sp,
                                  fontWeight = FontWeight.Bold
                              )
                          }
                      }
                  }
                  Text(
                      text = stock.name,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.Gray,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
              }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${"%,.2f".format(stock.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${if (stock.change >= 0) "+" else ""}${"%,.2f".format(stock.change)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFCF6679)
                )
            }
        }
    }
}
