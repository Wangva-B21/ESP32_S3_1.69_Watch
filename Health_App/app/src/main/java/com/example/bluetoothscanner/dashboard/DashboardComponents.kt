package com.example.bluetoothscanner.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import android.graphics.Paint
import android.graphics.Typeface

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HealthCard(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    color: Color,
    isSelected: Boolean = false,
    isWarning: Boolean = false, // <=== QUAN TRỌNG: Đã thêm tham số này
    onClick: () -> Unit = {}
) {
    // 1. ANIMATION NHẤP NHÁY KHI CÓ CẢNH BÁO
    val infiniteTransition = rememberInfiniteTransition(label = "WarningPulse")

    // Nếu Warning: Màu sẽ chạy từ Đỏ -> Đỏ nhạt -> Đỏ liên tục
    val animatedColor by if (isWarning) {
        infiniteTransition.animateColor(
            initialValue = Color.Red,
            targetValue = Color.Red.copy(alpha = 0.2f),
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing), // Chớp tắt mỗi 0.8s
                repeatMode = RepeatMode.Reverse
            ),
            label = "ColorPulse"
        )
    } else {
        rememberUpdatedState(color) // Giữ màu gốc nếu bình thường
    }

    // Viền thẻ cũng đổi màu theo cảnh báo
    val finalBorderColor = if (isWarning) Color.Red else (if (isSelected) color else Color.Transparent)
    val borderWidth = if (isSelected || isWarning) 2.dp else 0.dp

    // Nền thẻ: Nếu cảnh báo thì nền hơi đỏ nhẹ
    val containerColor = if (isWarning) Color(0xFFFFEBEE) else (if (isSelected) color.copy(alpha = 0.05f) else Color(0xFFFAFAFA))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .border(borderWidth, finalBorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(if (isSelected || isWarning) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon tròn bên trái
            Surface(
                shape = CircleShape,
                color = animatedColor.copy(alpha = 0.1f), // Màu nền icon nhấp nháy
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = animatedColor, // Icon nhấp nháy
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column {
                Text(
                    text = if (isWarning) "$label (NGUY HIỂM!)" else label, // Đổi text cảnh báo
                    fontSize = 14.sp,
                    color = if (isWarning) Color.Red else Color.Gray,
                    fontWeight = if (isWarning) FontWeight.Bold else FontWeight.Normal
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "NumberSlide"
                    ) { targetCount ->
                        Text(
                            text = "$targetCount",
                            fontSize = 22.sp,
                            color = if (isWarning) Color.Red else Color.Black, // Số đỏ lòm
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        color = if (isWarning) Color.Red.copy(alpha = 0.7f) else Color.Gray,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

// Hàm vẽ biểu đồ (Giữ nguyên để không bị mất)
@Composable
fun LiveLineChart(
    dataPoints: List<Int>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Đang đo...", color = Color.Gray)
        }
        return
    }

    Canvas(modifier = modifier.padding(start = 0.dp, end = 12.dp, top = 10.dp, bottom = 20.dp)) {

        // --- 1. TÍNH TOÁN KÍCH THƯỚC VÀ TỈ LỆ ---
        val yAxisPadding = 70f
        val xAxisPadding = 40f

        val chartAreaWidth = size.width - yAxisPadding
        val chartAreaHeight = size.height - xAxisPadding

        val maxVal = (dataPoints.maxOrNull() ?: 100).toFloat()
        val minVal = (dataPoints.minOrNull() ?: 0).toFloat()

        val range = max(maxVal - minVal, 10f) * 1.5f
        val centerData = (maxVal + minVal) / 2
        val baseLine = centerData - (range / 2)
        val topLine = centerData + (range / 2)

        // --- 2. VẼ TRỤC VÀ LƯỚI (GRID) ---
        val textPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 32f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }

        val gridValues = listOf(topLine, centerData, baseLine)
        gridValues.forEach { value ->
            val y = chartAreaHeight - ((value - baseLine) / range) * chartAreaHeight

            drawContext.canvas.nativeCanvas.drawText(
                "${value.toInt()}",
                yAxisPadding - 15f,
                y + 10f,
                textPaint
            )

            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(yAxisPadding, y),
                end = Offset(size.width, y),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // --- 3. VẼ NHÃN TRỤC X ---
        val timeLabels = listOf("60s", "30s", "Now")
        val textPaintCenter = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }

        timeLabels.forEachIndexed { index, label ->
            val x = yAxisPadding + (chartAreaWidth / 2) * index
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height + 10f,
                textPaintCenter
            )
        }

        // --- 4. VẼ ĐƯỜNG CONG ---
        val path = Path()
        val maxPointsVisible = 50
        val stepX = chartAreaWidth / (maxPointsVisible - 1)
        val startIndex = max(0, maxPointsVisible - dataPoints.size)

        dataPoints.forEachIndexed { index, value ->
            val x = yAxisPadding + (startIndex + index) * stepX
            val y = chartAreaHeight - ((value - baseLine) / range) * chartAreaHeight

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = yAxisPadding + (startIndex + (index - 1)) * stepX
                val prevVal = dataPoints[index - 1]
                val prevY = chartAreaHeight - ((prevVal - baseLine) / range) * chartAreaHeight

                val cx1 = (prevX + x) / 2
                val cy1 = prevY
                val cx2 = (prevX + x) / 2
                val cy2 = y

                path.cubicTo(cx1, cy1, cx2, cy2, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(size.width, chartAreaHeight)
        fillPath.lineTo(yAxisPadding, chartAreaHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)
            )
        )

        if (dataPoints.isNotEmpty()) {
            val lastX = yAxisPadding + (startIndex + dataPoints.lastIndex) * stepX
            val lastVal = dataPoints.last()
            val lastY = chartAreaHeight - ((lastVal - baseLine) / range) * chartAreaHeight

            drawCircle(color = lineColor.copy(alpha = 0.2f), radius = 18f, center = Offset(lastX, lastY))
            drawCircle(color = Color.White, radius = 8f, center = Offset(lastX, lastY))
            drawCircle(color = lineColor, radius = 6f, center = Offset(lastX, lastY))
        }
    }
}