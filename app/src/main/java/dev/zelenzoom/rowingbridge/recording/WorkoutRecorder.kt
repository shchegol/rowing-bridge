package dev.zelenzoom.rowingbridge.recording

import android.os.SystemClock
import dev.zelenzoom.rowingbridge.ble.RowerSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aggregates BLE samples into a workout recording: avg/max tracking and an
 * elapsed-time-excluding-pause clock. Kotlin analogue of
 * garmin-rowing/source/RowerModel.mc, minus the FIT-writing half (that's
 * FitFileWriter's job, fed by the WorkoutRecording save() returns).
 *
 * This specific rowing machine never sets the FTMS "average" flag bits
 * (Average Stroke Rate, Average Pace, Average Power) - only instantaneous
 * values. So all averages here are computed from the instantaneous stream
 * ourselves rather than trusting the device to report them. Heart rate
 * doesn't come from the rower at all - WorkoutViewModel overlays it from a
 * separate BLE Heart Rate Service connection (see HeartRateClient) before
 * samples reach onSample().
 */
class WorkoutRecorder {

    private val _state = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var elapsedBeforePauseMs = 0L
    private var segmentStartMs = 0L
    private val records = mutableListOf<RecordSample>()

    private var latestDistance = 0
    private var latestStrokeCount = 0
    private var latestCalories = 0f
    private var strokeRateSum = 0.0
    private var strokeRateSampleCount = 0
    private var powerSum = 0L
    private var powerSampleCount = 0
    private var maxStrokeRate = 0f
    private var maxPower = 0
    private var heartRateSum = 0L
    private var heartRateSampleCount = 0
    private var maxHeartRate = 0
    private var lastDeviceElapsedTimeSeconds: Int? = null
    private var startEpochMillis = 0L

    fun start() {
        if (_state.value != WorkoutState.Idle) return
        resetAccumulators()
        segmentStartMs = SystemClock.elapsedRealtime()
        startEpochMillis = System.currentTimeMillis()
        _state.value = WorkoutState.Recording
    }

    fun pause() {
        if (_state.value != WorkoutState.Recording) return
        elapsedBeforePauseMs += SystemClock.elapsedRealtime() - segmentStartMs
        _state.value = WorkoutState.Paused
    }

    fun resume() {
        if (_state.value != WorkoutState.Paused) return
        segmentStartMs = SystemClock.elapsedRealtime()
        _state.value = WorkoutState.Recording
    }

    /** Finalizes the recording and returns it for saving to a FIT file. Null if nothing was recorded. */
    fun save(): WorkoutRecording? {
        if (_state.value == WorkoutState.Idle) return null
        val result = if (records.isEmpty()) {
            null
        } else {
            // Avg pace from total time/distance rather than averaging noisy
            // instantaneous readings - this is also how avg pace is normally
            // defined (total time over total distance), not just a mean of ticks.
            val avgPace = if (latestDistance > 0) {
                (_elapsedSeconds.value * 500 / latestDistance).toInt()
            } else {
                0
            }
            WorkoutRecording(
                records = records.toList(),
                startEpochMillis = startEpochMillis,
                endEpochMillis = System.currentTimeMillis(),
                elapsedSeconds = _elapsedSeconds.value,
                totalDistanceMeters = latestDistance,
                totalStrokeCount = latestStrokeCount,
                totalCalories = latestCalories,
                avgStrokeRate = if (strokeRateSampleCount > 0) (strokeRateSum / strokeRateSampleCount).toFloat() else 0f,
                avgPaceSecondsPer500m = avgPace,
                avgPowerWatts = if (powerSampleCount > 0) (powerSum / powerSampleCount).toInt() else 0,
                avgHeartRateBpm = if (heartRateSampleCount > 0) (heartRateSum / heartRateSampleCount).toInt() else 0,
                maxStrokeRate = maxStrokeRate,
                maxPowerWatts = maxPower,
                maxHeartRateBpm = maxHeartRate,
                deviceElapsedTimeSeconds = lastDeviceElapsedTimeSeconds,
            )
        }
        resetAccumulators()
        _state.value = WorkoutState.Idle
        return result
    }

    fun discard() {
        resetAccumulators()
        _state.value = WorkoutState.Idle
    }

    /** Feed every BLE sample here regardless of state; it's a no-op unless currently recording. */
    fun onSample(sample: RowerSample) {
        if (_state.value != WorkoutState.Recording) return

        sample.distanceMeters?.let { latestDistance = it }
        sample.strokeCount?.let { latestStrokeCount = it }
        sample.totalEnergyKcal?.let { latestCalories = it }
        sample.strokeRate?.let {
            if (it > maxStrokeRate) maxStrokeRate = it
            strokeRateSum += it
            strokeRateSampleCount += 1
        }
        sample.powerWatts?.let {
            if (it > maxPower) maxPower = it
            powerSum += it
            powerSampleCount += 1
        }
        sample.heartRateBpm?.let {
            if (it > maxHeartRate) maxHeartRate = it
            heartRateSum += it
            heartRateSampleCount += 1
        }
        sample.elapsedTimeSeconds?.let { lastDeviceElapsedTimeSeconds = it }

        val elapsedMs = elapsedBeforePauseMs + (SystemClock.elapsedRealtime() - segmentStartMs)
        _elapsedSeconds.value = elapsedMs / 1000

        records += RecordSample(
            elapsedMillis = elapsedMs,
            epochMillis = System.currentTimeMillis(),
            heartRateBpm = sample.heartRateBpm,
            strokeRate = sample.strokeRate?.let { Math.round(it) },
            distanceMeters = sample.distanceMeters,
            powerWatts = sample.powerWatts,
            paceSecondsPer500m = sample.paceSecondsPer500m,
            resistanceLevel = sample.resistanceLevel,
            met = sample.met,
            strokeCount = sample.strokeCount,
        )
    }

    private fun resetAccumulators() {
        records.clear()
        elapsedBeforePauseMs = 0
        _elapsedSeconds.value = 0
        latestDistance = 0
        latestStrokeCount = 0
        latestCalories = 0f
        strokeRateSum = 0.0
        strokeRateSampleCount = 0
        powerSum = 0L
        powerSampleCount = 0
        maxStrokeRate = 0f
        maxPower = 0
        heartRateSum = 0L
        heartRateSampleCount = 0
        maxHeartRate = 0
        lastDeviceElapsedTimeSeconds = null
    }
}
