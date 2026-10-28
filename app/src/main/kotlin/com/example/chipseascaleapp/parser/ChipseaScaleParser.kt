package com.example.chipseascaleapp.parser

import android.bluetooth.le.ScanRecord
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.WeightUnit
import java.util.Locale

object ChipseaScaleParser {
    const val PROTOCOL_ID = 0xC0.toByte()
    const val PACKET_LENGTH = 15

    fun parse(record: ScanRecord?, deviceAddress: String? = null): ScaleMeasurement? {
        if (record == null) return null

        // 1. Check Manufacturer Specific Data
        val msd = record.manufacturerSpecificData
        if (msd != null) {
            for (i in 0 until msd.size()) {
                val companyId = msd.keyAt(i)
                val data = msd.valueAt(i) ?: continue

                // Direct 15-byte manufacturer data starting with 0xC0
                if (data.size >= PACKET_LENGTH && data[0] == PROTOCOL_ID) {
                    val parsed = parsePayload(data.copyOfRange(0, PACKET_LENGTH), deviceAddress)
                    if (parsed != null) return parsed
                }

                // If Android treated byte 0 (0xC0) and byte 1 (seq) as 16-bit Company ID (little-endian):
                val low = (companyId and 0xFF).toByte()
                val high = ((companyId shr 8) and 0xFF).toByte()
                if (low == PROTOCOL_ID && data.size >= (PACKET_LENGTH - 2)) {
                    val reconstructed = ByteArray(PACKET_LENGTH)
                    reconstructed[0] = low
                    reconstructed[1] = high
                    System.arraycopy(data, 0, reconstructed, 2, PACKET_LENGTH - 2)
                    val parsed = parsePayload(reconstructed, deviceAddress)
                    if (parsed != null) return parsed
                }
            }
        }

        // 2. Fallback to raw advertisement bytes
        val rawBytes = record.bytes
        if (rawBytes != null) {
            val parsed = findAndParseFromRawBytes(rawBytes, deviceAddress)
            if (parsed != null) return parsed
        }

        return null
    }

    fun findAndParseFromRawBytes(bytes: ByteArray, deviceAddress: String? = null): ScaleMeasurement? {
        // Walk standard BLE TLV structures: [length] [type] [payload]
        var index = 0
        while (index < bytes.size) {
            val length = bytes[index].toInt() and 0xFF
            if (length == 0) break
            if (index + 1 + length > bytes.size) break

            val type = bytes[index + 1].toInt() and 0xFF
            val dataStart = index + 2
            val dataLength = length - 1

            if (type == 0xFF) {
                // If payload starts directly with 0xC0
                if (dataLength >= PACKET_LENGTH && bytes[dataStart] == PROTOCOL_ID) {
                    val payload = bytes.copyOfRange(dataStart, dataStart + PACKET_LENGTH)
                    val parsed = parsePayload(payload, deviceAddress)
                    if (parsed != null) return parsed
                }
                // If payload has 2-byte company ID then 0xC0
                if (dataLength >= PACKET_LENGTH + 2 && bytes[dataStart + 2] == PROTOCOL_ID) {
                    val payload = bytes.copyOfRange(dataStart + 2, dataStart + 2 + PACKET_LENGTH)
                    val parsed = parsePayload(payload, deviceAddress)
                    if (parsed != null) return parsed
                }
            }
            index += 1 + length
        }

        // Sliding window fallback across the entire buffer
        for (i in 0..(bytes.size - PACKET_LENGTH)) {
            if (bytes[i] == PROTOCOL_ID) {
                val candidate = bytes.copyOfRange(i, i + PACKET_LENGTH)
                val parsed = parsePayload(candidate, deviceAddress)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    fun parsePayload(payload: ByteArray, fallbackMac: String? = null): ScaleMeasurement? {
        if (payload.size < PACKET_LENGTH) return null
        if (payload[0] != PROTOCOL_ID) return null

        val sequenceIndex = payload[1].toInt() and 0xFF

        // Bytes 2..3: Weight big-endian
        val rawWeight = ((payload[2].toInt() and 0xFF) shl 8) or (payload[3].toInt() and 0xFF)

        // Bytes 4..5: Bioelectrical impedance big-endian (e.g. 0x1770 = 6000 -> 600.0 Ohm)
        val rawImpedance = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        val impedanceOhm = rawImpedance / 10.0f

        // Bytes 6..7: Product ID (e.g. 0x0A11)
        val productId = ((payload[6].toInt() and 0xFF) shl 8) or (payload[7].toInt() and 0xFF)

        // Byte 8: Scale properties
        // Bit 0-1: Unit (0 = KG, 1 = LB, 2 = ST)
        // Bit 2-3 / Bit 5: Device type (Body fat scale vs Simple scale)
        // Bit 4: Weight stabilized / locked flag
        val props = payload[8].toInt() and 0xFF
        val unitBits = props and 0x03
        val unit = when (unitBits) {
            0 -> WeightUnit.KG
            1 -> WeightUnit.LB
            2 -> WeightUnit.ST
            else -> WeightUnit.KG
        }

        // In Chipsea protocol (and OKOK decompiled BytesUtil):
        // Bit 5 is body fat scale flag (as in 0x24, 0x25, 0x34, 0x35)
        // Bits 2-3 are also checked in some variants (0x08, 0x18)
        val isBodyFat = (props and 0x20) != 0 || ((props shr 2) and 0x03) == 2 || (props and 0x08) != 0
        val deviceType = if (isBodyFat) {
            DeviceType.BODY_FAT_SCALE
        } else {
            DeviceType.SIMPLE_SCALE
        }

        // Bit 4: Weight stabilized/locked flag
        val isStabilized = ((props shr 4) and 0x01) == 1

        // Weight value calculation with unit normalization
        val rawDisplayWeight = rawWeight / 100.0f
        val weightKg = when (unit) {
            WeightUnit.LB -> rawDisplayWeight * 0.45359237f
            WeightUnit.ST -> rawDisplayWeight * 6.35029318f
            else -> rawDisplayWeight
        }

        // Sanity filter to prevent false positives from random non-scale BLE data
        if (weightKg > 350.0f) {
            return null
        }

        // Bytes 9..14: MAC address (6 bytes)
        val payloadMac = String.format(
            Locale.US,
            "%02X:%02X:%02X:%02X:%02X:%02X",
            payload[9].toInt() and 0xFF,
            payload[10].toInt() and 0xFF,
            payload[11].toInt() and 0xFF,
            payload[12].toInt() and 0xFF,
            payload[13].toInt() and 0xFF,
            payload[14].toInt() and 0xFF
        )

        val macAddress = if (payloadMac == "00:00:00:00:00:00" && !fallbackMac.isNullOrBlank()) {
            fallbackMac.uppercase(Locale.US)
        } else {
            payloadMac
        }

        return ScaleMeasurement(
            weightKg = weightKg,
            displayWeight = rawDisplayWeight,
            impedanceOhm = impedanceOhm,
            isStabilized = isStabilized,
            unit = unit,
            deviceType = deviceType,
            sequenceIndex = sequenceIndex,
            productId = productId,
            macAddress = macAddress
        )
    }
}
