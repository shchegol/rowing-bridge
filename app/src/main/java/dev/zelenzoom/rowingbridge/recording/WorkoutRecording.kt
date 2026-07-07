package dev.zelenzoom.rowingbridge.recording

/** A finished (saved) recording, ready to be handed to a FIT file writer. */
data class WorkoutRecording(
    val records: List<RecordSample>,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val elapsedSeconds: Long,
    val totalDistanceMeters: Int,
    val totalStrokeCount: Int,
    val totalCalories: Float,
    val avgStrokeRate: Float,
    val avgPaceSecondsPer500m: Int,
    val avgPowerWatts: Int,
    val avgHeartRateBpm: Int,
    val maxStrokeRate: Float,
    val maxPowerWatts: Int,
    val maxHeartRateBpm: Int,
    val deviceElapsedTimeSeconds: Int?,
)
