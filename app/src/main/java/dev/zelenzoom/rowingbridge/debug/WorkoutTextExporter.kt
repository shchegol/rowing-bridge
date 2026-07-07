package dev.zelenzoom.rowingbridge.debug

import dev.zelenzoom.rowingbridge.recording.WorkoutRecording

/**
 * Plain-text dump of a finished recording (summary + every tick) - a
 * stopgap for inspecting exactly what was captured before the real FIT
 * export exists (see the planned FitFileWriter).
 */
object WorkoutTextExporter {

    fun export(recording: WorkoutRecording): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("=== Workout summary ===")
        sb.appendLine("elapsedSeconds=${recording.elapsedSeconds}")
        sb.appendLine("totalDistanceMeters=${recording.totalDistanceMeters}")
        sb.appendLine("totalStrokeCount=${recording.totalStrokeCount}")
        sb.appendLine("totalCalories=${recording.totalCalories}")
        sb.appendLine("avgStrokeRate=${recording.avgStrokeRate}")
        sb.appendLine("avgPaceSecondsPer500m=${recording.avgPaceSecondsPer500m}")
        sb.appendLine("avgPowerWatts=${recording.avgPowerWatts}")
        sb.appendLine("avgHeartRateBpm=${recording.avgHeartRateBpm}")
        sb.appendLine("maxStrokeRate=${recording.maxStrokeRate}")
        sb.appendLine("maxPowerWatts=${recording.maxPowerWatts}")
        sb.appendLine("maxHeartRateBpm=${recording.maxHeartRateBpm}")
        sb.appendLine("deviceElapsedTimeSeconds=${recording.deviceElapsedTimeSeconds}")
        sb.appendLine()
        sb.appendLine("=== Records (${recording.records.size} ticks) ===")
        sb.appendLine(
            "elapsedMillis,heartRateBpm,strokeRate,distanceMeters,powerWatts," +
                "paceSecondsPer500m,resistanceLevel,met,strokeCount",
        )
        recording.records.forEach { r ->
            sb.appendLine(
                "${r.elapsedMillis},${r.heartRateBpm ?: ""},${r.strokeRate ?: ""},${r.distanceMeters ?: ""}," +
                    "${r.powerWatts ?: ""},${r.paceSecondsPer500m ?: ""},${r.resistanceLevel ?: ""}," +
                    "${r.met ?: ""},${r.strokeCount ?: ""}",
            )
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }
}
