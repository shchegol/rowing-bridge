package dev.zelenzoom.rowingbridge.recording

/** One recorded tick. */
data class RecordSample(
    /** Active recording time (pauses excluded) - for the on-screen TIME clock. */
    val elapsedMillis: Long,
    /** Real wall-clock time this tick was captured (System.currentTimeMillis) - for FIT record timestamps. */
    val epochMillis: Long,
    val heartRateBpm: Int?,
    val strokeRate: Int?,
    val distanceMeters: Int?,
    val powerWatts: Int?,
    val paceSecondsPer500m: Int?,
    val resistanceLevel: Int?,
    val met: Float?,
    val strokeCount: Int?,
)
