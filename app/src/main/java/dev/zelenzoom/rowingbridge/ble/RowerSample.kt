package dev.zelenzoom.rowingbridge.ble

/**
 * One parsed FTMS Rower Data (0x2AD1) packet. Fields are null when their
 * flag bit wasn't set in the packet (i.e. the machine didn't report them).
 */
data class RowerSample(
    val strokeRate: Float? = null,       // strokes/min, 0.5 resolution
    val strokeCount: Int? = null,
    val avgStrokeRate: Float? = null,
    val distanceMeters: Int? = null,     // cumulative, uint24
    val paceSecondsPer500m: Int? = null,
    val avgPaceSecondsPer500m: Int? = null,
    val powerWatts: Int? = null,
    val avgPowerWatts: Int? = null,
    val resistanceLevel: Int? = null,
    val totalEnergyKcal: Float? = null,  // see RowerDataParser: scaled /100 for this device
    val energyPerHourKcal: Int? = null,
    val energyPerMinuteKcal: Int? = null,
    val heartRateBpm: Int? = null,
    val met: Float? = null,
    val elapsedTimeSeconds: Int? = null,
    val remainingTimeSeconds: Int? = null,
)
