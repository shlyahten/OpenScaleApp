package com.example.chipseascaleapp.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.WeightUnit
import java.util.Locale

@Composable
fun WeightHeroCard(
    measurement: ScaleMeasurement?,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Chip at top
            StatusBadge(measurement = measurement, isScanning = isScanning, pulseAlpha = pulseAlpha)

            Spacer(modifier = Modifier.height(16.dp))

            // Main Weight Display
            val weightText = measurement?.let { String.format(Locale.US, "%.2f", it.weightKg) } ?: "--.--"
            val unitText = when (measurement?.unit) {
                WeightUnit.LB -> "фунт"
                else -> "кг"
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = weightText,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (measurement?.isStabilized == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = unitText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scale metadata line
            if (measurement != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAC: ${measurement.macAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    val typeLabel = if (measurement.deviceType == DeviceType.BODY_FAT_SCALE) {
                        "Весы-жироанализатор"
                    } else {
                        "Обычные весы"
                    }

                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Text(
                    text = if (isScanning) "Встаньте на весы для измерения" else "Нажмите «Старт» для поиска весов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    measurement: ScaleMeasurement?,
    isScanning: Boolean,
    pulseAlpha: Float
) {
    val (bgColor, textColor, text, icon) = when {
        measurement == null && isScanning -> {
            Quad(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                "Поиск сигнала весов...",
                null
            )
        }
        measurement == null -> {
            Quad(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.outline,
                "Сканирование остановлено",
                null
            )
        }
        measurement.isStabilized -> {
            Quad(
                Color(0xFFE8F5E9),
                Color(0xFF1B5E20),
                "Вес зафиксирован",
                Icons.Default.Check
            )
        }
        else -> {
            Quad(
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                "Идет взвешивание...",
                null
            )
        }
    }

    val alphaModifier = if (measurement != null && !measurement.isStabilized) {
        Modifier.alpha(pulseAlpha)
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = alphaModifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (measurement != null && !measurement.isStabilized) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(textColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
