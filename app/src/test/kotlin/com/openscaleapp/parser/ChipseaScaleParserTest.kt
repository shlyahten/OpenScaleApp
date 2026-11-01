package com.openscaleapp.parser

import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChipseaScaleParserTest {

    @Test
    fun parsePayload_validStabilizedWeightAndImpedance() {
        // C0 01 1E 1E 17 70 0A 11 25 80 F4 16 01 47 50
        // Weight: 0x1E1E = 7710 -> 77.10 kg
        // Impedance: 0x1770 = 6000 -> 600.0 Ohm
        // PID: 0x0A11 = 2577
        // Props: 0x25 -> bit 0 = 1 (stabilized), bit 5 = 1 (body fat), bits 3-4 = 0 (KG), bits 1-2 = 2 (/100)
        // MAC: 80:F4:16:01:47:50
        val payload = byteArrayOf(
            0xC0.toByte(), 0x01.toByte(),
            0x1E.toByte(), 0x1E.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x25.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload)
        assertNotNull(measurement)
        assertEquals(77.10f, measurement!!.weightKg, 0.001f)
        assertEquals(77.10f, measurement.displayWeight, 0.001f)
        assertEquals(600.0f, measurement.impedanceOhm, 0.001f)
        assertTrue(measurement.isStabilized)
        assertEquals(WeightUnit.KG, measurement.unit)
        assertEquals(DeviceType.BODY_FAT_SCALE, measurement.deviceType)
        assertEquals(1, measurement.sequenceIndex)
        assertEquals(0x0A11, measurement.productId)
        assertEquals("80:F4:16:01:47:50", measurement.macAddress)
        assertTrue(measurement.hasImpedance)
    }

    @Test
    fun parsePayload_realScaleProperty0x25_bodyFatScaleInKgStabilized() {
        // Props: 0x25 = 0b00100101 -> Bit 5 = 1 (body fat scale), Bits 3..4 = 0 (KG), Bits 1..2 = 2 (/100), Bit 0 = 1 (stabilized/locked)
        // Raw weight: 0x1B3F = 6975 -> 69.75 kg
        // Impedance: 0x1770 = 6000 -> 600.0 Ohm
        val payload = byteArrayOf(
            0xC0.toByte(), 0x1D.toByte(),
            0x1B.toByte(), 0x3F.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x25.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload)
        assertNotNull(measurement)
        assertTrue(measurement!!.isStabilized)
        assertEquals(WeightUnit.KG, measurement.unit)
        assertEquals(DeviceType.BODY_FAT_SCALE, measurement.deviceType)
        assertEquals(69.75f, measurement.displayWeight, 0.01f)
        assertEquals(69.75f, measurement.weightKg, 0.01f)
        assertEquals(600.0f, measurement.impedanceOhm, 0.01f)
    }

    @Test
    fun parsePayload_realScaleProperty0x35_bodyFatScaleInPoundsStabilized() {
        // Props: 0x35 = 0b00110101 -> Bit 5 = 1 (body fat), Bits 3..4 = 2 (LB), Bits 1..2 = 2 (/100), Bit 0 = 1 (stabilized)
        val payload = byteArrayOf(
            0xC0.toByte(), 0x08.toByte(),
            0x3A.toByte(), 0x98.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x35.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload)
        assertNotNull(measurement)
        assertTrue(measurement!!.isStabilized)
        assertEquals(WeightUnit.LB, measurement.unit)
        assertEquals(DeviceType.BODY_FAT_SCALE, measurement.deviceType)
        assertEquals(150.00f, measurement.displayWeight, 0.01f)
        assertEquals(600.0f, measurement.impedanceOhm, 0.001f)
    }

    @Test
    fun parsePayload_realScaleProperty0x24_bodyFatScaleInKgMeasuring() {
        // Props: 0x24 = 0b00100100 -> Bit 5 = 1 (body fat), Bits 3..4 = 0 (KG), Bits 1..2 = 2 (/100), Bit 0 = 0 (measuring)
        val payloadLive = byteArrayOf(
            0xC0.toByte(), 0x03.toByte(),
            0x1E.toByte(), 0x1E.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x24.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val mLive = ChipseaScaleParser.parsePayload(payloadLive)
        assertNotNull(mLive)
        assertFalse(mLive!!.isStabilized)
        assertEquals(DeviceType.BODY_FAT_SCALE, mLive.deviceType)
        assertEquals(WeightUnit.KG, mLive.unit)
        assertEquals(77.10f, mLive.weightKg, 0.01f)
    }

    @Test
    fun parsePayload_macAddressFallbackWhenZero() {
        val payload = byteArrayOf(
            0xC0.toByte(), 0x01.toByte(),
            0x1E.toByte(), 0x1E.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x25.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload, fallbackMac = "80:F4:16:01:47:50")
        assertNotNull(measurement)
        assertEquals("80:F4:16:01:47:50", measurement!!.macAddress)
    }

    @Test
    fun parsePayload_liveMeasuringNotStabilized() {
        // Props: 0x24 -> bit 0 = 0 (measuring / live), bits 3..4 = 0 (KG), bits 1..2 = 2 (/100)
        val payload = byteArrayOf(
            0xC0.toByte(), 0x02.toByte(),
            0x19.toByte(), 0x64.toByte(), // 0x1964 = 6500 -> 65.00 kg
            0x00.toByte(), 0x00.toByte(), // Z = 0
            0x0A.toByte(), 0x11.toByte(),
            0x24.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload)
        assertNotNull(measurement)
        assertEquals(65.00f, measurement!!.weightKg, 0.001f)
        assertEquals(0.0f, measurement.impedanceOhm, 0.001f)
        assertFalse(measurement.isStabilized)
        assertFalse(measurement.hasImpedance)
    }

    @Test
    fun parsePayload_invalidPackets() {
        // Null or empty
        assertNull(ChipseaScaleParser.parsePayload(byteArrayOf()))

        // Too short (< 15 bytes)
        assertNull(ChipseaScaleParser.parsePayload(byteArrayOf(0xC0.toByte(), 0x01.toByte(), 0x02.toByte())))

        // Wrong protocol identifier (not 0xC0)
        val wrongId = ByteArray(15) { 0x00 }
        wrongId[0] = 0xAA.toByte()
        assertNull(ChipseaScaleParser.parsePayload(wrongId))

        // Extreme impossible weight (e.g. 500 kg)
        val extremeWeight = ByteArray(15) { 0x00 }
        extremeWeight[0] = 0xC0.toByte()
        extremeWeight[2] = 0xFF.toByte()
        extremeWeight[3] = 0xFF.toByte()
        assertNull(ChipseaScaleParser.parsePayload(extremeWeight))
    }

    @Test
    fun findAndParseFromRawBytes_extractsFromBleAdPacket() {
        // An advertisement with standard AD structure:
        // [length] [0xFF] [payload...]
        val packet = byteArrayOf(
            0x02, 0x01, 0x06, // Flags AD structure
            0x10, 0xFF.toByte(), // Length 16 (0x10), Type 0xFF
            0xC0.toByte(), 0x05.toByte(),
            0x1E.toByte(), 0x1E.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x25.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.findAndParseFromRawBytes(packet)
        assertNotNull(measurement)
        assertEquals(77.10f, measurement!!.weightKg, 0.001f)
        assertEquals("80:F4:16:01:47:50", measurement.macAddress)
    }
}
