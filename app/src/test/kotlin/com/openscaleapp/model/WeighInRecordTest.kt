package com.openscaleapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeighInRecordTest {

    @Test
    fun toJson_and_fromJson_preservesAllFields() {
        val original = WeighInRecord(
            id = "test-uuid-1234",
            timestampMs = 1730000000000L,
            weightKg = 76.65f,
            displayWeight = 76.65f,
            unit = WeightUnit.KG,
            impedanceOhm = 600.0f,
            deviceType = DeviceType.BODY_FAT_SCALE,
            macAddress = "80:F4:16:01:47:50",
            bodyComposition = BodyComposition(
                bmi = 24.2f,
                bmiCategory = BmiCategory.NORMAL,
                bmiStatus = MetricStatus.HEALTHY,
                bodyFatRate = 22.0f,
                bodyFatStatus = MetricStatus.HIGH,
                fatMassKg = 16.9f,
                fatMassStatus = MetricStatus.HIGH,
                skeletalMuscleRate = 40.0f,
                skeletalMuscleKg = 30.7f,
                skeletalMuscleStatus = MetricStatus.PERFECT,
                muscleRate = 73.6f,
                muscleMassKg = 56.4f,
                muscleStatus = MetricStatus.HEALTHY,
                waterRate = 54.0f,
                waterMassKg = 41.4f,
                waterStatus = MetricStatus.HEALTHY,
                visceralFat = 10.0f,
                visceralFatStatus = MetricStatus.HIGH,
                boneMassKg = 3.3f,
                boneMassStatus = MetricStatus.PERFECT,
                bmrKcal = 1688.0f,
                bmrStatus = MetricStatus.HIGH,
                proteinRate = 19.7f,
                proteinStatus = MetricStatus.HEALTHY,
                obesityDegree = 11.7f,
                obesityDegreeStatus = MetricStatus.OVERWEIGHT,
                bodyAge = 35,
                leanMassKg = 59.79f
            )
        )

        val json = original.toJson()
        val restored = WeighInRecord.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.timestampMs, restored.timestampMs)
        assertEquals(original.weightKg, restored.weightKg, 0.01f)
        assertEquals(original.impedanceOhm, restored.impedanceOhm, 0.01f)
        assertEquals(original.unit, restored.unit)
        assertEquals(original.deviceType, restored.deviceType)
        assertEquals(original.macAddress, restored.macAddress)

        val comp = restored.bodyComposition
        assertEquals(24.2f, comp.bmi, 0.01f)
        assertEquals(22.0f, comp.bodyFatRate!!, 0.01f)
        assertEquals(16.9f, comp.fatMassKg!!, 0.01f)
        assertEquals(40.0f, comp.skeletalMuscleRate!!, 0.01f)
        assertEquals(30.7f, comp.skeletalMuscleKg!!, 0.01f)
        assertEquals(73.6f, comp.muscleRate!!, 0.01f)
        assertEquals(56.4f, comp.muscleMassKg!!, 0.01f)
        assertEquals(54.0f, comp.waterRate!!, 0.01f)
        assertEquals(41.4f, comp.waterMassKg!!, 0.01f)
        assertEquals(10.0f, comp.visceralFat!!, 0.01f)
        assertEquals(3.3f, comp.boneMassKg!!, 0.01f)
        assertEquals(1688.0f, comp.bmrKcal!!, 0.5f)
        assertEquals(19.7f, comp.proteinRate!!, 0.01f)
        assertEquals(11.7f, comp.obesityDegree!!, 0.01f)
        assertEquals(35, comp.bodyAge)
        assertEquals(59.79f, comp.leanMassKg!!, 0.01f)
    }
}
