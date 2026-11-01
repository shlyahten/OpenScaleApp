package com.openscaleapp.model

enum class WeightUnit {
    KG,
    LB,
    ST,
    UNKNOWN
}

enum class DeviceType {
    SIMPLE_SCALE,
    BODY_FAT_SCALE,
    UNKNOWN
}

data class ScaleMeasurement(
    val weightKg: Float,
    val displayWeight: Float = weightKg,
    val impedanceOhm: Float,
    val isStabilized: Boolean,
    val unit: WeightUnit,
    val deviceType: DeviceType,
    val sequenceIndex: Int,
    val productId: Int,
    val macAddress: String,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val hasImpedance: Boolean
        get() = impedanceOhm > 0f
}
