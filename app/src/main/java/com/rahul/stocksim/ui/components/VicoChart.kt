package com.rahul.stocksim.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.rahul.stocksim.data.StockPricePoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter

@Composable
fun VicoLineChart(
    history: List<StockPricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    // Calculate trend-based color if not provided
    val chartColor = remember(history, lineColor) {
        lineColor ?: if (history.size >= 2) {
            if (history.last().price >= history.first().price) Color.Green else Color.Red
        } else Color.Green
    }
    
    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = history.map { it.timestamp },
                        y = history.map { it.price }
                    )
                }
            }
        }
    }

    if (history.isEmpty()) return

    val marker = rememberCartesianMarker(history)

    val rangeProvider = remember(history) {
        object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val tempMin = if (minY.isNaN() || minY.isInfinite()) 0.0 else minY
                val tempMax = if (maxY.isNaN() || maxY.isInfinite()) tempMin + 1.0 else maxY
                val delta = tempMax - tempMin
                return if (delta > 0) tempMin - delta * 0.1 else tempMin * 0.9
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val tempMin = if (minY.isNaN() || minY.isInfinite()) 0.0 else minY
                val tempMax = if (maxY.isNaN() || maxY.isInfinite()) tempMin + 1.0 else maxY
                val delta = tempMax - tempMin
                return if (delta > 0) tempMax + delta * 0.1 else tempMax * 1.1
            }
        }
    }

    // Keying the chart by history ensuring axes and color update correctly
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(chartColor)),
                    areaFill = LineCartesianLayer.AreaFill.single(
                        fill(
                            DynamicShader.verticalGradient(
                                arrayOf(chartColor.copy(alpha = 0.5f), chartColor.copy(alpha = 0f))
                            )
                        )
                    ),
                    thickness = 3.dp,
                    pointConnector = LineCartesianLayer.PointConnector.cubic()
                )
            ),
            rangeProvider = rangeProvider
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = rememberTextComponent(
                color = Color.Gray,
                textSize = 10.sp,
                padding = Dimensions(8f, 2f, 8f, 2f)
            ),
            line = null,
            tick = null,
            guideline = null,
            valueFormatter = remember(history) {
                CartesianValueFormatter { _, value, _ ->
                    val date = Date(value.toLong() * 1000)
                    val first = history.firstOrNull()?.timestamp ?: 0L
                    val last = history.lastOrNull()?.timestamp ?: 0L
                    val diff = abs(last - first)
                    val pattern = when {
                        diff <= 0 -> "h:mm a"
                        diff <= 90000 -> "h:mm a"
                        diff <= 86400 * 7 -> "EEE"
                        diff <= 86400 * 35 -> "MMM dd"
                        else -> "MMM yyyy"
                    }
                    SimpleDateFormat(pattern, Locale.getDefault()).format(date)
                }
            },
            itemPlacer = HorizontalAxis.ItemPlacer.aligned()
        ),
        endAxis = VerticalAxis.rememberEnd(
            label = rememberTextComponent(
                color = Color.Gray, 
                textSize = 10.sp,
                padding = Dimensions(8f, 2f, 8f, 2f)
            ),
            itemPlacer = VerticalAxis.ItemPlacer.count(count = { 6 }),
            line = null,
            tick = null,
            guideline = rememberLineComponent(
                fill = fill(Color.White.copy(alpha = 0.1f)),
                thickness = 1.dp
            ),
            valueFormatter = { _, value, _ ->
                if (value >= 1000) {
                    String.format(Locale.US, "$%.2fK", value / 1000.0)
                } else {
                    String.format(Locale.US, "$%.2f", value)
                }
            }
        ),
        marker = marker
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false)
    )
}

@Composable
fun rememberCartesianMarker(history: List<StockPricePoint> = emptyList()): DefaultCartesianMarker {
    val labelBackground = rememberShapeComponent(
        fill = fill(Color.Black.copy(alpha = 0.8f)),
        shape = CorneredShape.rounded(8f),
        strokeFill = fill(Color.White.copy(alpha = 0.1f)),
        strokeThickness = 1.dp
    )
    val label = rememberTextComponent(
        color = Color.White,
        background = labelBackground,
        padding = Dimensions(8f, 4f, 8f, 4f),
        textSize = 12.sp,
        lineCount = 2 // Increased to show date and price
    )
    val indicator = rememberShapeComponent(fill = fill(Color.White), shape = CorneredShape.Pill)
    val guideline = rememberLineComponent(
        fill = fill(Color.White.copy(alpha = 0.5f)),
        thickness = 1.dp
    )
    
    return remember(label, indicator, guideline, history) {
        DefaultCartesianMarker(
            label = label,
            indicator = { indicator },
            indicatorSizeDp = 6f,
            guideline = guideline,
            valueFormatter = CartesianMarkerValueFormatter { _, targets ->
                val target = targets.filterIsInstance<LineCartesianLayerMarkerTarget>()
                    .firstOrNull()?.points?.firstOrNull()
                val price = target?.entry?.y ?: 0.0
                val x = target?.entry?.x ?: 0.0
                
                val date = Date(x.toLong() * 1000)
                val dateStr = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(date)
                
                "$dateStr\n$${String.format(Locale.US, "%,.2f", price)}"
            }
        )
    }
}
