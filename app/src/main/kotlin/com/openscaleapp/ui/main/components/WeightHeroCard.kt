package com.openscaleapp.ui.main.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openscaleapp.R
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.WeighInRecord
import com.openscaleapp.model.WeightUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeightHeroCard(
    measurement: ScaleMeasurement?,
    isScanning: Boolean,
    scanRemainingSeconds: Int,
    selectedHistoryRecord: WeighInRecord?,
    onStartWeighIn: () -> Unit,
    onCancelWeighIn: () -> Unit,
    onClearHistorySelection: () -> Unit,
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Chip at top
            StatusBadge(
                measurement = measurement,
                isScanning = isScanning,
                scanRemainingSeconds = scanRemainingSeconds,
                selectedHistoryRecord = selectedHistoryRecord,
                pulseAlpha = pulseAlpha
            )

            if (selectedHistoryRecord != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onClearHistorySelection) {
                    Text(
                        text = stringResource(R.string.return_to_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Weight Display
            val weightText = measurement?.let { String.format(Locale.US, "%.2f", it.displayWeight) } ?: "--.--"
            val unitText = when (measurement?.unit) {
                WeightUnit.LB -> stringResource(R.string.unit_lb)
                WeightUnit.ST -> stringResource(R.string.unit_st)
                else -> stringResource(R.string.unit_kg)
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
                    color = when {
                        measurement?.isStabilized == true -> MaterialTheme.colorScheme.primary
                        isScanning -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.outline
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

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle guidance or scale info
            if (isScanning) {
                Text(
                    text = if (measurement != null) stringResource(R.string.hint_measuring) else stringResource(R.string.hint_step_on),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (scanRemainingSeconds / 30f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                )
            } else if (selectedHistoryRecord != null) {
                Text(
                    text = stringResource(R.string.record_from_date, formatRecordDate(selectedHistoryRecord.timestampMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else if (measurement != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mac_label, measurement.macAddress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    val typeLabel = if (measurement.deviceType == DeviceType.BODY_FAT_SCALE) {
                        stringResource(R.string.scale_type_body_fat)
                    } else {
                        stringResource(R.string.scale_type_standard)
                    }

                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.hint_start),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Weigh-In Action Button
            if (isScanning) {
                OutlinedButton(
                    onClick = onCancelWeighIn,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel_search),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cancel_search),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onStartWeighIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.weigh_in_button),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.weigh_in_button),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    measurement: ScaleMeasurement?,
    isScanning: Boolean,
    scanRemainingSeconds: Int,
    selectedHistoryRecord: WeighInRecord?,
    pulseAlpha: Float
) {
    val (text, dotColor, showDot) = when {
        selectedHistoryRecord != null -> Triple(
            stringResource(R.string.archived_record),
            MaterialTheme.colorScheme.tertiary,
            false
        )
        isScanning && measurement == null -> Triple(
            stringResource(R.string.status_searching, scanRemainingSeconds),
            MaterialTheme.colorScheme.primary,
            true
        )
        isScanning && measurement?.isStabilized == true -> Triple(
            stringResource(R.string.status_measuring),
            Color(0xFF4CAF50),
            true
        )
        isScanning && measurement?.isStabilized == false -> Triple(
            stringResource(R.string.status_measuring),
            MaterialTheme.colorScheme.primary,
            true
        )
        measurement?.isStabilized == true -> Triple(
            stringResource(R.string.status_completed),
            Color(0xFF4CAF50),
            false
        )
        else -> Triple(
            stringResource(R.string.status_ready),
            MaterialTheme.colorScheme.outline,
            false
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(pulseAlpha)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (measurement?.isStabilized == true && !isScanning && selectedHistoryRecord == null) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatRecordDate(timestampMs: Long): String {
    val date = Date(timestampMs)
    val now = Calendar.getInstance()
    val recordCal = Calendar.getInstance().apply { time = date }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(date)

    return if (now.get(Calendar.YEAR) == recordCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == recordCal.get(Calendar.DAY_OF_YEAR)
    ) {
        "Сегодня в $formattedTime"
    } else {
        val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())
        dateFormat.format(date)
    }
}
