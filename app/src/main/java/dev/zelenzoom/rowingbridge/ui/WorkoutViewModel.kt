package dev.zelenzoom.rowingbridge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.zelenzoom.rowingbridge.ble.BleConnectionState
import dev.zelenzoom.rowingbridge.ble.FtmsClient
import dev.zelenzoom.rowingbridge.ble.HeartRateClient
import dev.zelenzoom.rowingbridge.ble.RowerSample
import dev.zelenzoom.rowingbridge.debug.WorkoutTextExporter
import dev.zelenzoom.rowingbridge.fit.FitFileWriter
import dev.zelenzoom.rowingbridge.recording.WorkoutRecorder
import dev.zelenzoom.rowingbridge.recording.WorkoutRecording
import dev.zelenzoom.rowingbridge.recording.WorkoutState
import dev.zelenzoom.rowingbridge.storage.DownloadsFileStore
import dev.zelenzoom.rowingbridge.strava.StravaApi
import dev.zelenzoom.rowingbridge.strava.StravaAuthManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StravaUploadState { IDLE, UPLOADING, SUCCESS, ERROR }

/**
 * Owns the BLE connections (rower + watch heart rate) and the workout
 * recording state machine, and exposes both to the UI. The watch's bpm is
 * overlaid onto each RowerSample's heartRateBpm before it reaches the
 * recorder or the live display, since the rower itself never reports heart
 * rate. `saveWorkout()` writes a real FIT file (via FitFileWriter) plus a
 * plain-text debug dump, both landing in Downloads. Strava upload is a
 * separate, manual step (`uploadLastSavedToStrava()`) triggered from the
 * "Workout saved" dialog, not automatic.
 */
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val ftmsClient = FtmsClient(application)
    private val heartRateClient = HeartRateClient(application)
    private val recorder = WorkoutRecorder()
    private val stravaAuthManager = StravaAuthManager(application)
    private val stravaApi = StravaApi(stravaAuthManager)

    private var lastFitFile: File? = null

    private val _stravaUploadState = MutableStateFlow(StravaUploadState.IDLE)
    val stravaUploadState: StateFlow<StravaUploadState> = _stravaUploadState.asStateFlow()

    val connectionState: StateFlow<BleConnectionState> = ftmsClient.connectionState
    val heartRateConnectionState: StateFlow<BleConnectionState> = heartRateClient.connectionState
    val workoutState: StateFlow<WorkoutState> = recorder.state
    val elapsedSeconds: StateFlow<Long> = recorder.elapsedSeconds

    private val _latestSample = MutableStateFlow<RowerSample?>(null)
    val latestSample: StateFlow<RowerSample?> = _latestSample.asStateFlow()

    private val _lastSavedRecording = MutableStateFlow<WorkoutRecording?>(null)
    val lastSavedRecording: StateFlow<WorkoutRecording?> = _lastSavedRecording.asStateFlow()

    init {
        viewModelScope.launch {
            ftmsClient.samples.collect { sample ->
                val withHeartRate = sample.copy(
                    heartRateBpm = heartRateClient.heartRateBpm.value ?: sample.heartRateBpm,
                )
                _latestSample.value = withHeartRate
                recorder.onSample(withHeartRate)
            }
        }
    }

    fun startScanning() {
        ftmsClient.start()
        heartRateClient.start()
    }

    fun stopScanning() {
        ftmsClient.stop()
        heartRateClient.stop()
    }

    fun startWorkout() = recorder.start()

    fun pauseWorkout() = recorder.pause()

    fun resumeWorkout() = recorder.resume()

    fun saveWorkout() {
        val recording = recorder.save()
        _lastSavedRecording.value = recording
        if (recording != null) {
            val application = getApplication<Application>()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            DownloadsFileStore.save(
                context = application,
                fileName = "rowing_debug_$timestamp.txt",
                mimeType = "text/plain",
                bytes = WorkoutTextExporter.export(recording),
            )

            val fitCacheFile = File(application.cacheDir, "rowing_$timestamp.fit")
            FitFileWriter.write(recording, fitCacheFile)
            DownloadsFileStore.save(
                context = application,
                fileName = "rowing_$timestamp.fit",
                mimeType = "application/octet-stream",
                bytes = fitCacheFile.readBytes(),
            )
            lastFitFile = fitCacheFile
        }
    }

    fun isStravaConnected(): Boolean = stravaAuthManager.isConnected()

    fun uploadLastSavedToStrava() {
        val file = lastFitFile ?: return
        viewModelScope.launch {
            _stravaUploadState.value = StravaUploadState.UPLOADING
            val result = stravaApi.upload(file.readBytes(), file.name)
            _stravaUploadState.value = if (result.isSuccess) StravaUploadState.SUCCESS else StravaUploadState.ERROR
        }
    }

    fun discardWorkout() = recorder.discard()

    fun dismissSavedRecording() {
        _lastSavedRecording.value = null
        _stravaUploadState.value = StravaUploadState.IDLE
    }

    override fun onCleared() {
        ftmsClient.stop()
        heartRateClient.stop()
    }
}
