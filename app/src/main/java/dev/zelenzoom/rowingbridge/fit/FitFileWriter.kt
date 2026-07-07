package dev.zelenzoom.rowingbridge.fit

import com.garmin.fit.Activity
import com.garmin.fit.ActivityMesg
import com.garmin.fit.DateTime
import com.garmin.fit.Event
import com.garmin.fit.EventType
import com.garmin.fit.File as FitFile
import com.garmin.fit.Fit
import com.garmin.fit.FileEncoder
import com.garmin.fit.FileIdMesg
import com.garmin.fit.LapMesg
import com.garmin.fit.Manufacturer
import com.garmin.fit.RecordMesg
import com.garmin.fit.SessionMesg
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import dev.zelenzoom.rowingbridge.recording.WorkoutRecording
import java.io.File
import java.util.Date

/**
 * Encodes a finished WorkoutRecording as a real, independently-crafted FIT
 * file via the official Garmin FIT SDK (com.garmin:fit) - a different code
 * path from Connect IQ's ActivityRecording.Session/FitContributor, which is
 * confirmed to have Garmin Connect silently discard app-supplied values for
 * standard metrics (see project memory: rowing-machine-hardware.md).
 *
 * The typed setters below (setDistance, setTotalDistance, etc.) take values
 * in real units (meters, watts, seconds) - the SDK applies the FIT profile's
 * scale/offset internally, so unlike the Connect IQ FitContributor code this
 * does NOT need manual scaling (e.g. no *100 for distance).
 */
object FitFileWriter {

    fun write(recording: WorkoutRecording, outputFile: File) {
        val encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)

        val startTime = DateTime(Date(recording.startEpochMillis))
        val endTime = DateTime(Date(recording.endEpochMillis))
        val elapsedSecondsFloat = recording.elapsedSeconds.toFloat()

        val fileId = FileIdMesg()
        fileId.setType(FitFile.ACTIVITY)
        fileId.setManufacturer(Manufacturer.DEVELOPMENT)
        fileId.setTimeCreated(startTime)
        encoder.write(fileId)

        recording.records.forEach { r ->
            val record = RecordMesg()
            record.setTimestamp(DateTime(Date(r.epochMillis)))
            r.heartRateBpm?.let { record.setHeartRate(it.toShort()) }
            r.strokeRate?.let { record.setCadence(it.toShort()) }
            r.distanceMeters?.let { record.setDistance(it.toFloat()) }
            r.powerWatts?.let { record.setPower(it) }
            r.resistanceLevel?.let { record.setResistance(it.toShort()) }
            r.strokeCount?.let { record.setTotalCycles(it.toLong()) }
            encoder.write(record)
        }

        val lap = LapMesg()
        lap.setTimestamp(endTime)
        lap.setStartTime(startTime)
        lap.setTotalElapsedTime(elapsedSecondsFloat)
        lap.setTotalTimerTime(elapsedSecondsFloat)
        lap.setTotalDistance(recording.totalDistanceMeters.toFloat())
        lap.setSport(Sport.ROWING)
        lap.setSubSport(SubSport.INDOOR_ROWING)
        encoder.write(lap)

        val session = SessionMesg()
        session.setTimestamp(endTime)
        session.setStartTime(startTime)
        session.setSport(Sport.ROWING)
        session.setSubSport(SubSport.INDOOR_ROWING)
        session.setTotalElapsedTime(elapsedSecondsFloat)
        session.setTotalTimerTime(elapsedSecondsFloat)
        session.setTotalDistance(recording.totalDistanceMeters.toFloat())
        session.setTotalCycles(recording.totalStrokeCount.toLong())
        session.setTotalCalories(Math.round(recording.totalCalories))
        session.setAvgCadence(Math.round(recording.avgStrokeRate).toShort())
        session.setMaxCadence(Math.round(recording.maxStrokeRate).toShort())
        session.setAvgPower(recording.avgPowerWatts)
        session.setMaxPower(recording.maxPowerWatts)
        if (recording.avgHeartRateBpm > 0) session.setAvgHeartRate(recording.avgHeartRateBpm.toShort())
        if (recording.maxHeartRateBpm > 0) session.setMaxHeartRate(recording.maxHeartRateBpm.toShort())
        encoder.write(session)

        val activity = ActivityMesg()
        activity.setTimestamp(endTime)
        activity.setTotalTimerTime(elapsedSecondsFloat)
        activity.setNumSessions(1)
        activity.setType(Activity.MANUAL)
        activity.setEvent(Event.ACTIVITY)
        activity.setEventType(EventType.STOP)
        encoder.write(activity)

        encoder.close()
    }
}
