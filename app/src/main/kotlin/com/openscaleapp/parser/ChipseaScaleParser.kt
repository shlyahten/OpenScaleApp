package com.openscaleapp.parser

import android.bluetooth.le.ScanRecord
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.WeightUnit
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

        // 2. Fallback to raw advertisement bytes inside Manufacturer Data (type 0xFF)
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

        // Byte 8: Scale properties (decoded from OKOK decompiled BytesUtil / chipseaBroadcastFrame)
        // Bit 0: cmdId / Stabilization flag (0 = real-time measuring, 1 = locked measurement)
        // Bits 1..2: Weight digit precision (1 = 0 dec, 2 = 2 dec / 100, else = 1 dec / 10)
        // Bits 3..4: Weight unit (0 = KG, 1 = JIN, 2 = LB, 3 = ST)
        // Bit 5: Device type (1 = Body fat scale, 0 = Simple scale)
        val props = payload[8].toInt() and 0xFF

        // Bit 0: Locked/stabilized
        val isStabilized = (props and 0x01) == 1

        // Bits 3..4: Unit
        val unitBits = (props shr 3) and 0x03
        val unit = when (unitBits) {
            0 -> WeightUnit.KG
            1 -> WeightUnit.KG // JIN in OKOK (can be treated as KG or JIN)
            2 -> WeightUnit.LB
            3 -> WeightUnit.ST
            else -> WeightUnit.KG
        }

        // Bits 1..2: Decimal precision
        val digitBits = (props shr 1) and 0x03
        val rawDisplayWeight = when (digitBits) {
            1 -> rawWeight.toFloat()
            2 -> rawWeight / 100.0f
            else -> rawWeight / 10.0f
        }

        // Bit 5: Device type
        val isBodyFat = (props and 0x20) != 0 || ((props shr 2) and 0x03) == 2 || (props and 0x08) != 0
        val deviceType = if (isBodyFat) {
            DeviceType.BODY_FAT_SCALE
        } else {
            DeviceType.SIMPLE_SCALE
        }

        // Weight value in KG
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
