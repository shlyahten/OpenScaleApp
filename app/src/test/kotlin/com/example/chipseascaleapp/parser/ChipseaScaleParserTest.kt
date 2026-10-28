package com.example.chipseascaleapp.parser

import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChipseaScaleParserTest {

    @Test
    fun parsePayload_validStabilizedWeightAndImpedance() {
        // C0 01 1E 1E 17 70 0A 11 18 80 F4 16 01 47 50
        // Weight: 0x1E1E = 7710 -> 77.10 kg
        // Impedance: 0x1770 = 6000 -> 600.0 Ohm
        // PID: 0x0A11 = 2577
        // Props: 0x18 -> bit 4 = 1 (stabilized), bits 2-3 = 2 (body fat), bits 0-1 = 0 (KG)
        // MAC: 80:F4:16:01:47:50
        val payload = byteArrayOf(
            0xC0.toByte(), 0x01.toByte(),
            0x1E.toByte(), 0x1E.toByte(),
            0x17.toByte(), 0x70.toByte(),
            0x0A.toByte(), 0x11.toByte(),
            0x18.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.parsePayload(payload)
        assertNotNull(measurement)
        assertEquals(77.10f, measurement!!.weightKg, 0.001f)
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
    fun parsePayload_liveMeasuringNotStabilized() {
        // Props: 0x08 -> bit 4 = 0 (measuring / live)
        val payload = byteArrayOf(
            0xC0.toByte(), 0x02.toByte(),
            0x19.toByte(), 0x64.toByte(), // 0x1964 = 6500 -> 65.00 kg
            0x00.toByte(), 0x00.toByte(), // Z = 0
            0x0A.toByte(), 0x11.toByte(),
            0x08.toByte(),
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
            0x18.toByte(),
            0x80.toByte(), 0xF4.toByte(), 0x16.toByte(), 0x01.toByte(), 0x47.toByte(), 0x50.toByte()
        )

        val measurement = ChipseaScaleParser.findAndParseFromRawBytes(packet)
        assertNotNull(measurement)
        assertEquals(77.10f, measurement!!.weightKg, 0.001f)
        assertEquals("80:F4:16:01:47:50", measurement.macAddress)
    }
}
