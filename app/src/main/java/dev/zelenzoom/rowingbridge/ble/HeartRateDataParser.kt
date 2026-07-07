package dev.zelenzoom.rowingbridge.ble

/**
 * Parses the standard Bluetooth SIG Heart Rate Measurement characteristic
 * (0x2A37, part of the Heart Rate Service 0x180D) - what a Garmin watch
 * sends when "Broadcast Heart Rate" is enabled. Only the bpm value is
 * needed here; sensor-contact/energy-expended/RR-interval fields are
 * ignored.
 */
object HeartRateDataParser {

    fun parse(data: ByteArray): Int? {
        if (data.isEmpty()) return null
        val flags = data[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        return if (is16Bit) {
            if (data.size < 3) return null
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else {
            if (data.size < 2) return null
            data[1].toInt() and 0xFF
        }
    }
}
