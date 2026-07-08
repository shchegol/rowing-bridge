package dev.zelenzoom.rowingbridge.ble

/**
 * Parses the Bluetooth SIG FTMS "Rower Data" characteristic (0x2AD1).
 * Field layout/flags ported from the Connect IQ reference implementation
 * (garmin-rowing/source/FtmsManager.mc), which was cross-checked against
 * the FTMS spec. All multi-byte fields are little-endian.
 *
 * Kotlin's ByteArray elements are signed, unlike Monkey C's implicit
 * unsigned byte access — every byte is masked with 0xFF before combining.
 */
object RowerDataParser {

    fun parse(data: ByteArray, quirks: RowerQuirks = RowerQuirks()): RowerSample {
        if (data.size < 2) return RowerSample()

        fun u8(i: Int) = data[i].toInt() and 0xFF
        fun u16(i: Int) = u8(i) or (u8(i + 1) shl 8)
        fun s16(i: Int): Int {
            val v = u16(i)
            return if (v > 0x7FFF) v - 0x10000 else v
        }
        fun u24(i: Int) = u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16)

        val flags = u16(0)
        var i = 2

        var strokeRate: Float? = null
        var strokeCount: Int? = null
        var avgStrokeRate: Float? = null
        var distance: Int? = null
        var pace: Int? = null
        var avgPace: Int? = null
        var power: Int? = null
        var avgPower: Int? = null
        var resistance: Int? = null
        var totalEnergy: Float? = null
        var energyPerHour: Int? = null
        var energyPerMinute: Int? = null
        var heartRate: Int? = null
        var met: Float? = null
        var elapsedTime: Int? = null
        var remainingTime: Int? = null

        // bit0 == 0 -> Stroke Rate (uint8, step 0.5) + Stroke Count (uint16)
        if ((flags and 0x0001) == 0) {
            if (i < data.size) {
                strokeRate = u8(i) / 2.0f
                i += 1
            }
            if (i + 1 < data.size) {
                strokeCount = u16(i)
                i += 2
            }
        }

        // bit1: Average Stroke Rate (uint8, step 0.5)
        if ((flags and 0x0002) != 0) {
            if (i < data.size) {
                avgStrokeRate = u8(i) / 2.0f
                i += 1
            }
        }

        // bit2: Total Distance (uint24, meters)
        if ((flags and 0x0004) != 0) {
            if (i + 2 < data.size) {
                distance = u24(i)
                i += 3
            }
        }

        // bit3: Instantaneous Pace (uint16, s/500m). 0xFFFF = not available.
        if ((flags and 0x0008) != 0) {
            if (i + 1 < data.size) {
                val v = u16(i)
                if (v != 0xFFFF) pace = v
                i += 2
            }
        }

        // bit4: Average Pace (uint16, s/500m). 0xFFFF = not available.
        if ((flags and 0x0010) != 0) {
            if (i + 1 < data.size) {
                val v = u16(i)
                if (v != 0xFFFF) avgPace = v
                i += 2
            }
        }

        // bit5: Instantaneous Power (sint16, W)
        if ((flags and 0x0020) != 0) {
            if (i + 1 < data.size) {
                power = s16(i)
                i += 2
            }
        }

        // bit6: Average Power (sint16, W)
        if ((flags and 0x0040) != 0) {
            if (i + 1 < data.size) {
                avgPower = s16(i)
                i += 2
            }
        }

        // bit7: Resistance Level (sint16)
        if ((flags and 0x0080) != 0) {
            if (i + 1 < data.size) {
                resistance = s16(i)
                i += 2
            }
        }

        // bit8: Expended Energy - Total(uint16 kcal), PerHour(uint16), PerMin(uint8).
        // Total Energy is scaled per-device via `quirks.totalEnergyScale` (default
        // 1.0, i.e. trust the spec's whole-kcal definition) - see RowerQuirks.kt
        // for known deviations confirmed on real hardware.
        if ((flags and 0x0100) != 0) {
            if (i + 1 < data.size) {
                val v = u16(i)
                if (v != 0xFFFF) totalEnergy = v * quirks.totalEnergyScale
            }
            i += 2
            if (i + 1 < data.size) {
                val v = u16(i)
                if (v != 0xFFFF) energyPerHour = v
            }
            i += 2
            if (i < data.size) {
                if (u8(i) != 0xFF) energyPerMinute = u8(i)
            }
            i += 1
        }

        // bit9: Heart Rate (uint8, bpm)
        if ((flags and 0x0200) != 0) {
            if (i < data.size) {
                if (u8(i) != 0) heartRate = u8(i)
                i += 1
            }
        }

        // bit10: Metabolic Equivalent (uint8, step 0.1)
        if ((flags and 0x0400) != 0) {
            if (i < data.size) {
                met = u8(i) / 10.0f
                i += 1
            }
        }

        // bit11: Elapsed Time (uint16, s)
        if ((flags and 0x0800) != 0) {
            if (i + 1 < data.size) {
                elapsedTime = u16(i)
                i += 2
            }
        }

        // bit12: Remaining Time (uint16, s)
        if ((flags and 0x1000) != 0) {
            if (i + 1 < data.size) {
                remainingTime = u16(i)
                i += 2
            }
        }

        return RowerSample(
            strokeRate = strokeRate,
            strokeCount = strokeCount,
            avgStrokeRate = avgStrokeRate,
            distanceMeters = distance,
            paceSecondsPer500m = pace,
            avgPaceSecondsPer500m = avgPace,
            powerWatts = power,
            avgPowerWatts = avgPower,
            resistanceLevel = resistance,
            totalEnergyKcal = totalEnergy,
            energyPerHourKcal = energyPerHour,
            energyPerMinuteKcal = energyPerMinute,
            heartRateBpm = heartRate,
            met = met,
            elapsedTimeSeconds = elapsedTime,
            remainingTimeSeconds = remainingTime,
        )
    }
}
