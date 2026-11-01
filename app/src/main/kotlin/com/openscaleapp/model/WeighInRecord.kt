package com.openscaleapp.model

import org.json.JSONObject
import java.util.UUID

data class WeighInRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val displayWeight: Float = weightKg,
    val unit: WeightUnit = WeightUnit.KG,
    val impedanceOhm: Float = 0f,
    val deviceType: DeviceType = DeviceType.BODY_FAT_SCALE,
    val macAddress: String = "",
    val bodyComposition: BodyComposition
) {
    fun toScaleMeasurement(): ScaleMeasurement {
        return ScaleMeasurement(
            weightKg = weightKg,
            displayWeight = displayWeight,
            impedanceOhm = impedanceOhm,
            isStabilized = true,
            unit = unit,
            deviceType = deviceType,
            sequenceIndex = 0,
            productId = 0,
            macAddress = macAddress,
            timestampMs = timestampMs
        )
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("timestampMs", timestampMs)
        json.put("weightKg", weightKg.toDouble())
        json.put("displayWeight", displayWeight.toDouble())
        json.put("unit", unit.name)
        json.put("impedanceOhm", impedanceOhm.toDouble())
        json.put("deviceType", deviceType.name)
        json.put("macAddress", macAddress)

        val compJson = JSONObject()
        compJson.put("bmi", bodyComposition.bmi.toDouble())
        compJson.put("bmiCategory", bodyComposition.bmiCategory.name)
        compJson.put("bmiStatus", bodyComposition.bmiStatus.name)
        bodyComposition.bodyFatRate?.let { compJson.put("bodyFatRate", it.toDouble()) }
        bodyComposition.bodyFatStatus?.let { compJson.put("bodyFatStatus", it.name) }
        bodyComposition.fatMassKg?.let { compJson.put("fatMassKg", it.toDouble()) }
        bodyComposition.fatMassStatus?.let { compJson.put("fatMassStatus", it.name) }
        bodyComposition.skeletalMuscleRate?.let { compJson.put("skeletalMuscleRate", it.toDouble()) }
        bodyComposition.skeletalMuscleKg?.let { compJson.put("skeletalMuscleKg", it.toDouble()) }
        bodyComposition.skeletalMuscleStatus?.let { compJson.put("skeletalMuscleStatus", it.name) }
        bodyComposition.muscleRate?.let { compJson.put("muscleRate", it.toDouble()) }
        bodyComposition.muscleMassKg?.let { compJson.put("muscleMassKg", it.toDouble()) }
        bodyComposition.muscleStatus?.let { compJson.put("muscleStatus", it.name) }
        bodyComposition.waterRate?.let { compJson.put("waterRate", it.toDouble()) }
        bodyComposition.waterMassKg?.let { compJson.put("waterMassKg", it.toDouble()) }
        bodyComposition.waterStatus?.let { compJson.put("waterStatus", it.name) }
        bodyComposition.visceralFat?.let { compJson.put("visceralFat", it.toDouble()) }
        bodyComposition.visceralFatStatus?.let { compJson.put("visceralFatStatus", it.name) }
        bodyComposition.boneMassKg?.let { compJson.put("boneMassKg", it.toDouble()) }
        bodyComposition.boneMassStatus?.let { compJson.put("boneMassStatus", it.name) }
        bodyComposition.bmrKcal?.let { compJson.put("bmrKcal", it.toDouble()) }
        bodyComposition.bmrStatus?.let { compJson.put("bmrStatus", it.name) }
        bodyComposition.proteinRate?.let { compJson.put("proteinRate", it.toDouble()) }
        bodyComposition.proteinStatus?.let { compJson.put("proteinStatus", it.name) }
        bodyComposition.obesityDegree?.let { compJson.put("obesityDegree", it.toDouble()) }
        bodyComposition.obesityDegreeStatus?.let { compJson.put("obesityDegreeStatus", it.name) }
        bodyComposition.bodyAge?.let { compJson.put("bodyAge", it) }
        bodyComposition.leanMassKg?.let { compJson.put("leanMassKg", it.toDouble()) }

        json.put("bodyComposition", compJson)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): WeighInRecord {
            val id = json.optString("id", UUID.randomUUID().toString())
            val timestampMs = json.optLong("timestampMs", System.currentTimeMillis())
            val weightKg = json.optDouble("weightKg", 0.0).toFloat()
            val displayWeight = json.optDouble("displayWeight", weightKg.toDouble()).toFloat()
            val unit = try {
                WeightUnit.valueOf(json.optString("unit", WeightUnit.KG.name))
            } catch (_: Exception) {
                WeightUnit.KG
            }
            val impedanceOhm = json.optDouble("impedanceOhm", 0.0).toFloat()
            val deviceType = try {
                DeviceType.valueOf(json.optString("deviceType", DeviceType.BODY_FAT_SCALE.name))
            } catch (_: Exception) {
                DeviceType.BODY_FAT_SCALE
            }
            val macAddress = json.optString("macAddress", "")

            val compJson = json.optJSONObject("bodyComposition")
            val comp = if (compJson != null) {
                val bmi = compJson.optDouble("bmi", 0.0).toFloat()
                val bmiCategory = try {
                    BmiCategory.valueOf(compJson.optString("bmiCategory", BmiCategory.NORMAL.name))
                } catch (_: Exception) {
                    BmiCategory.NORMAL
                }
                val bmiStatus = try {
                    MetricStatus.valueOf(compJson.optString("bmiStatus", MetricStatus.HEALTHY.name))
                } catch (_: Exception) {
                    MetricStatus.HEALTHY
                }
                fun optFloat(key: String): Float? = if (compJson.has(key)) compJson.optDouble(key).toFloat() else null
                fun optStatus(key: String): MetricStatus? = if (compJson.has(key)) {
                    try { MetricStatus.valueOf(compJson.getString(key)) } catch (_: Exception) { null }
                } else null

                BodyComposition(
                    bmi = bmi,
                    bmiCategory = bmiCategory,
                    bmiStatus = bmiStatus,
                    bodyFatRate = optFloat("bodyFatRate"),
                    bodyFatStatus = optStatus("bodyFatStatus"),
                    fatMassKg = optFloat("fatMassKg"),
                    fatMassStatus = optStatus("fatMassStatus"),
                    skeletalMuscleRate = optFloat("skeletalMuscleRate"),
                    skeletalMuscleKg = optFloat("skeletalMuscleKg"),
                    skeletalMuscleStatus = optStatus("skeletalMuscleStatus"),
                    muscleRate = optFloat("muscleRate"),
                    muscleMassKg = optFloat("muscleMassKg"),
                    muscleStatus = optStatus("muscleStatus"),
                    waterRate = optFloat("waterRate"),
                    waterMassKg = optFloat("waterMassKg"),
                    waterStatus = optStatus("waterStatus"),
                    visceralFat = optFloat("visceralFat"),
                    visceralFatStatus = optStatus("visceralFatStatus"),
                    boneMassKg = optFloat("boneMassKg"),
                    boneMassStatus = optStatus("boneMassStatus"),
                    bmrKcal = optFloat("bmrKcal"),
                    bmrStatus = optStatus("bmrStatus"),
                    proteinRate = optFloat("proteinRate"),
                    proteinStatus = optStatus("proteinStatus"),
                    obesityDegree = optFloat("obesityDegree"),
                    obesityDegreeStatus = optStatus("obesityDegreeStatus"),
                    bodyAge = if (compJson.has("bodyAge")) compJson.getInt("bodyAge") else null,
                    leanMassKg = optFloat("leanMassKg")
                )
            } else {
                BodyComposition(bmi = 0f)
            }

            return WeighInRecord(
                id = id,
                timestampMs = timestampMs,
                weightKg = weightKg,
                displayWeight = displayWeight,
                unit = unit,
                impedanceOhm = impedanceOhm,
                deviceType = deviceType,
                macAddress = macAddress,
                bodyComposition = comp
            )
        }
    }
}
