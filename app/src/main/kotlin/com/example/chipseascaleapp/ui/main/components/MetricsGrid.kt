package com.example.chipseascaleapp.ui.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chipseascaleapp.model.BmiCategory
import com.example.chipseascaleapp.model.BodyComposition
import com.example.chipseascaleapp.model.ScaleMeasurement
import java.util.Locale

@Composable
fun MetricsGrid(
    measurement: ScaleMeasurement?,
    bodyComposition: BodyComposition?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Состав тела",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Row 1: BMI & Body Fat %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bmiValue = bodyComposition?.let { String.format(Locale.US, "%.1f", it.bmi) } ?: "--"
            val bmiBadge = bodyComposition?.bmiCategory

            MetricCard(
                title = "Индекс массы (ИМТ)",
                value = bmiValue,
                unit = "кг/м²",
                badgeText = bmiBadge?.label,
                badgeColor = when (bmiBadge) {
                    BmiCategory.NORMAL -> Color(0xFF1B5E20)
                    BmiCategory.OVERWEIGHT -> Color(0xFFE65100)
                    BmiCategory.OBESE -> Color(0xFFC62828)
                    BmiCategory.UNDERWEIGHT -> Color(0xFF0277BD)
                    null -> MaterialTheme.colorScheme.outline
                },
                badgeBg = when (bmiBadge) {
                    BmiCategory.NORMAL -> Color(0xFFE8F5E9)
                    BmiCategory.OVERWEIGHT -> Color(0xFFFFF3E0)
                    BmiCategory.OBESE -> Color(0xFFFFEBEE)
                    BmiCategory.UNDERWEIGHT -> Color(0xFFE1F5FE)
                    null -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.weight(1f)
            )

            val fatRateValue = bodyComposition?.bodyFatRate?.let {
                String.format(Locale.US, "%.1f", it)
            } ?: "--"

            MetricCard(
                title = "Процент жира",
                value = fatRateValue,
                unit = "%",
                badgeText = if (bodyComposition?.bodyFatRate != null) "Биоимпеданс" else "Нет данных Z",
                badgeColor = MaterialTheme.colorScheme.primary,
                badgeBg = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Fat Mass & Lean Mass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fatMassValue = bodyComposition?.fatMassKg?.let {
                String.format(Locale.US, "%.1f", it)
            } ?: "--"

            MetricCard(
                title = "Масса жира",
                value = fatMassValue,
                unit = "кг",
                badgeText = null,
                modifier = Modifier.weight(1f)
            )

            val leanMassValue = bodyComposition?.leanMassKg?.let {
                String.format(Locale.US, "%.1f", it)
            } ?: "--"

            MetricCard(
                title = "Сухая масса тела",
                value = leanMassValue,
                unit = "кг",
                badgeText = null,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Impedance
        val impedanceValue = measurement?.let {
            if (it.hasImpedance) String.format(Locale.US, "%.1f", it.impedanceOhm) else "0.0"
        } ?: "--"

        MetricCard(
            title = "Электрическое сопротивление (Импеданс Z)",
            value = impedanceValue,
            unit = "Ом (Ω)",
            badgeText = if (measurement?.hasImpedance == true) "Контакт обнаружен" else "Встаньте босыми ногами",
            badgeColor = if (measurement?.hasImpedance == true) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline,
            badgeBg = if (measurement?.hasImpedance == true) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    badgeBg: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                Text(
                    text = unit,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (badgeText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
