package com.openscaleapp.ui.main.components

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openscaleapp.R
import com.openscaleapp.model.BodyComposition
import com.openscaleapp.model.MetricStatus
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.ui.theme.getMetricStatusColors
import java.util.Locale

@Composable
fun MetricsGrid(
    measurement: ScaleMeasurement?,
    bodyComposition: BodyComposition?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Primary Metrics
        MetricSectionHeader(title = stringResource(R.string.section_primary_metrics))

        // Row 1: BMI & Obesity Degree
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bmiVal = bodyComposition?.let { String.format(Locale.US, "%.1f", it.bmi) } ?: "--"
            val bmiStatus = bodyComposition?.bmiStatus

            MetricCard(
                title = stringResource(R.string.metric_bmi),
                value = bmiVal,
                unit = stringResource(R.string.unit_bmi),
                status = bmiStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val odVal = bodyComposition?.obesityDegree?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val odStatus = bodyComposition?.obesityDegreeStatus

            MetricCard(
                title = stringResource(R.string.metric_obesity_degree),
                value = odVal,
                unit = stringResource(R.string.unit_percent),
                status = odStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Section: Fat
        MetricSectionHeader(title = stringResource(R.string.section_fat))

        // Row 2: Fat Rate & Fat Mass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fatRateVal = bodyComposition?.bodyFatRate?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val fatStatus = bodyComposition?.bodyFatStatus

            MetricCard(
                title = stringResource(R.string.metric_body_fat),
                value = fatRateVal,
                unit = stringResource(R.string.unit_percent),
                status = fatStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val fatMassVal = bodyComposition?.fatMassKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val fatMassStatus = bodyComposition?.fatMassStatus

            MetricCard(
                title = stringResource(R.string.metric_fat_mass),
                value = fatMassVal,
                unit = stringResource(R.string.unit_kg),
                status = fatMassStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Visceral Fat & Lean Mass (Weight without fat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val vfVal = bodyComposition?.visceralFat?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val vfStatus = bodyComposition?.visceralFatStatus

            MetricCard(
                title = stringResource(R.string.metric_visceral_fat),
                value = vfVal,
                unit = stringResource(R.string.unit_index),
                status = vfStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val leanVal = bodyComposition?.leanMassKg?.let { String.format(Locale.US, "%.2f", it) } ?: "--"

            MetricCard(
                title = stringResource(R.string.metric_weight_without_fat),
                value = leanVal,
                unit = stringResource(R.string.unit_kg),
                status = null,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Section: Muscle and Bone
        MetricSectionHeader(title = stringResource(R.string.section_muscle_bone))

        // Row 4: Skeletal Muscle % and Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val skmRateVal = bodyComposition?.skeletalMuscleRate?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val skmStatus = bodyComposition?.skeletalMuscleStatus

            MetricCard(
                title = stringResource(R.string.metric_skeletal_muscle_rate),
                value = skmRateVal,
                unit = stringResource(R.string.unit_percent),
                status = skmStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val skmKgVal = bodyComposition?.skeletalMuscleKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"

            MetricCard(
                title = stringResource(R.string.metric_skeletal_muscle_mass),
                value = skmKgVal,
                unit = stringResource(R.string.unit_kg),
                status = skmStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 5: Total Muscle % and Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val muscleRateVal = bodyComposition?.muscleRate?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val muscleStatus = bodyComposition?.muscleStatus

            MetricCard(
                title = stringResource(R.string.metric_muscle_rate),
                value = muscleRateVal,
                unit = stringResource(R.string.unit_percent),
                status = muscleStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val muscleKgVal = bodyComposition?.muscleMassKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"

            MetricCard(
                title = stringResource(R.string.metric_muscle_mass),
                value = muscleKgVal,
                unit = stringResource(R.string.unit_kg),
                status = muscleStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 6: Bone Mass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val boneVal = bodyComposition?.boneMassKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val boneStatus = bodyComposition?.boneMassStatus

            MetricCard(
                title = stringResource(R.string.metric_bone_mass),
                value = boneVal,
                unit = stringResource(R.string.unit_kg),
                status = boneStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Section: Water, Protein and Metabolism
        MetricSectionHeader(title = stringResource(R.string.section_water_protein))

        // Row 7: Water % and Mass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val waterRateVal = bodyComposition?.waterRate?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val waterStatus = bodyComposition?.waterStatus

            MetricCard(
                title = stringResource(R.string.metric_water_rate),
                value = waterRateVal,
                unit = stringResource(R.string.unit_percent),
                status = waterStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val waterKgVal = bodyComposition?.waterMassKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"

            MetricCard(
                title = stringResource(R.string.metric_water_mass),
                value = waterKgVal,
                unit = stringResource(R.string.unit_kg),
                status = waterStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 8: Protein % and BMR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val proteinVal = bodyComposition?.proteinRate?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val proteinStatus = bodyComposition?.proteinStatus

            MetricCard(
                title = stringResource(R.string.metric_protein_rate),
                value = proteinVal,
                unit = stringResource(R.string.unit_percent),
                status = proteinStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            val bmrVal = bodyComposition?.bmrKcal?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val bmrStatus = bodyComposition?.bmrStatus

            MetricCard(
                title = stringResource(R.string.metric_bmr),
                value = bmrVal,
                unit = stringResource(R.string.unit_kcal),
                status = bmrStatus,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 9: Body Age
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bodyAgeVal = bodyComposition?.bodyAge?.toString() ?: "--"

            MetricCard(
                title = stringResource(R.string.metric_body_age),
                value = bodyAgeVal,
                unit = stringResource(R.string.unit_years),
                status = if (bodyComposition?.bodyAge != null) MetricStatus.HEALTHY else null,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Section: Bioimpedance
        MetricSectionHeader(title = stringResource(R.string.section_bioimpedance))

        val impedanceValue = measurement?.let {
            if (it.hasImpedance) String.format(Locale.US, "%.1f", it.impedanceOhm) else "0.0"
        } ?: "--"

        val hasContact = measurement?.hasImpedance == true
        val contactBadgeText = if (hasContact) stringResource(R.string.impedance_contact_detected) else stringResource(R.string.impedance_step_barefoot)
        val contactBadgeColor = if (hasContact) {
            if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        } else {
            MaterialTheme.colorScheme.outline
        }
        val contactBadgeBg = if (hasContact) {
            if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

        MetricCard(
            title = stringResource(R.string.metric_impedance),
            value = impedanceValue,
            unit = stringResource(R.string.unit_ohm),
            badgeText = contactBadgeText,
            badgeColor = contactBadgeColor,
            badgeBg = contactBadgeBg,
            isDark = isDark,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MetricSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    status: MetricStatus? = null,
    badgeText: String? = status?.let { stringResource(it.stringResId) },
    isDark: Boolean = isSystemInDarkTheme(),
    badgeColor: Color = status?.let { getMetricStatusColors(it, isDark).textColor } ?: MaterialTheme.colorScheme.primary,
    badgeBg: Color = status?.let { getMetricStatusColors(it, isDark).backgroundColor } ?: MaterialTheme.colorScheme.primaryContainer
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
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
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
