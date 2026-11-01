package com.openscaleapp.ui.theme

import androidx.compose.ui.graphics.Color
import com.openscaleapp.model.MetricStatus

// Light Colors
val primaryLight = Color(0xFF0284C7)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFE0F2FE)
val onPrimaryContainerLight = Color(0xFF0369A1)

val secondaryLight = Color(0xFF475569)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFF1F5F9)
val onSecondaryContainerLight = Color(0xFF1E293B)

val tertiaryLight = Color(0xFF0D9488)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFCCFBF1)
val onTertiaryContainerLight = Color(0xFF115E59)

val errorLight = Color(0xFFDC2626)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFEE2E2)
val onErrorContainerLight = Color(0xFF7F1D1D)

val backgroundLight = Color(0xFFF8FAFC)
val onBackgroundLight = Color(0xFF0F172A)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF0F172A)
val surfaceVariantLight = Color(0xFFF1F5F9)
val onSurfaceVariantLight = Color(0xFF475569)
val outlineLight = Color(0xFF94A3B8)
val outlineVariantLight = Color(0xFFE2E8F0)

// Dark Colors
val primaryDark = Color(0xFF7DD3FC)
val onPrimaryDark = Color(0xFF082F49)
val primaryContainerDark = Color(0xFF0369A1)
val onPrimaryContainerDark = Color(0xFFE0F2FE)

val secondaryDark = Color(0xFF94A3B8)
val onSecondaryDark = Color(0xFF0F172A)
val secondaryContainerDark = Color(0xFF1E293B)
val onSecondaryContainerDark = Color(0xFFE2E8F0)

val tertiaryDark = Color(0xFF5EEAD4)
val onTertiaryDark = Color(0xFF042F2E)
val tertiaryContainerDark = Color(0xFF115E59)
val onTertiaryContainerDark = Color(0xFFCCFBF1)

val errorDark = Color(0xFFF87171)
val onErrorDark = Color(0xFF450A0A)
val errorContainerDark = Color(0xFF7F1D1D)
val onErrorContainerDark = Color(0xFFFECACA)

val backgroundDark = Color(0xFF0B111E)
val onBackgroundDark = Color(0xFFF1F5F9)
val surfaceDark = Color(0xFF151E2E)
val onSurfaceDark = Color(0xFFF1F5F9)
val surfaceVariantDark = Color(0xFF1E293B)
val onSurfaceVariantDark = Color(0xFFCBD5E1)
val outlineDark = Color(0xFF64748B)
val outlineVariantDark = Color(0xFF334155)

// Status Colors Helper (Adapts for Light vs Dark mode)
data class MetricStatusColors(
    val textColor: Color,
    val backgroundColor: Color
)

fun getMetricStatusColors(status: MetricStatus, isDark: Boolean): MetricStatusColors {
    return when (status) {
        MetricStatus.LOW -> if (isDark) {
            MetricStatusColors(Color(0xFF38BDF8), Color(0xFF0C4A6E))
        } else {
            MetricStatusColors(Color(0xFF0284C7), Color(0xFFE0F2FE))
        }
        MetricStatus.HEALTHY -> if (isDark) {
            MetricStatusColors(Color(0xFF4ADE80), Color(0xFF14532D))
        } else {
            MetricStatusColors(Color(0xFF16A34A), Color(0xFFDCFCE7))
        }
        MetricStatus.PERFECT -> if (isDark) {
            MetricStatusColors(Color(0xFF86EFAC), Color(0xFF052E16))
        } else {
            MetricStatusColors(Color(0xFF15803D), Color(0xFFBBF7D0))
        }
        MetricStatus.HIGH -> if (isDark) {
            MetricStatusColors(Color(0xFFFBBF24), Color(0xFF78350F))
        } else {
            MetricStatusColors(Color(0xFFD97706), Color(0xFFFEF3C7))
        }
        MetricStatus.OVERWEIGHT -> if (isDark) {
            MetricStatusColors(Color(0xFFFB923C), Color(0xFF7C2D12))
        } else {
            MetricStatusColors(Color(0xFFEA580C), Color(0xFFFFEDD5))
        }
        MetricStatus.ALERT -> if (isDark) {
            MetricStatusColors(Color(0xFFF87171), Color(0xFF7F1D1D))
        } else {
            MetricStatusColors(Color(0xFFDC2626), Color(0xFFFEE2E2))
        }
    }
}
