package dev.zelenzoom.rowingbridge.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.zelenzoom.rowingbridge.R
import dev.zelenzoom.rowingbridge.ui.StravaUploadState
import dev.zelenzoom.rowingbridge.ble.BleConnectionState
import dev.zelenzoom.rowingbridge.ble.RowerSample
import dev.zelenzoom.rowingbridge.recording.WorkoutRecording
import dev.zelenzoom.rowingbridge.recording.WorkoutState

/**
 * Live metrics + recording controls, mirroring garmin-rowing/source/
 * RowingView.mc + RowingDelegate.mc + WorkoutMenu.mc: a single primary
 * button starts recording or (while recording) pauses and opens a
 * Resume/Save/Discard menu dialog. Status (top) and the primary action
 * button (bottom) are pinned via Scaffold so a long field list can't push
 * them off-screen; everything else scrolls. Colors come from the app
 * theme's ColorScheme (see ui/theme/Theme.kt), not ad-hoc constants:
 * primary=green (good/active), secondary=orange (recording/paused),
 * error=red (discard/lost connection).
 */
@Composable
fun WorkoutScreen(
    connectionState: BleConnectionState,
    heartRateConnectionState: BleConnectionState,
    workoutState: WorkoutState,
    sample: RowerSample?,
    elapsedSeconds: Long,
    lastSavedRecording: WorkoutRecording?,
    onPrimaryButton: () -> Unit,
    onResume: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismissSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    stravaConnected: Boolean,
    stravaUploadState: StravaUploadState,
    onUploadToStrava: () -> Unit,
) {
    // Computed here (a real @Composable context) rather than inside the
    // LazyColumn content block below, which is a plain LazyListScope lambda
    // - not composable - so it can't call stringResource()-using functions.
    val liveFields = rawFields(sample)

    Scaffold(
        topBar = { StatusBar(connectionState, heartRateConnectionState, workoutState, sample?.heartRateBpm, onOpenSettings) },
        bottomBar = { PrimaryActionBar(workoutState, onPrimaryButton) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricTile(Icons.Filled.FitnessCenter, stringResource(R.string.tile_spm), sample?.strokeRate?.let { "%.1f".format(it) } ?: "--", Modifier.weight(1f))
                            MetricTile(Icons.Filled.Timer, stringResource(R.string.tile_time), formatElapsed(elapsedSeconds), Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                            MetricTile(Icons.Filled.Speed, stringResource(R.string.tile_pace), formatPace(sample?.paceSecondsPer500m), Modifier.weight(1f))
                            MetricTile(Icons.Filled.Straighten, stringResource(R.string.tile_distance), sample?.distanceMeters?.toString() ?: "--", Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.section_other_live_data),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                )
            }

            items(liveFields) { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Icon(field.icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = field.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = field.value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (workoutState == WorkoutState.Paused) {
        var showDiscardConfirm by remember { mutableStateOf(false) }

        if (showDiscardConfirm) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirm = false },
                title = { Text(stringResource(R.string.dialog_discard_title)) },
                confirmButton = {
                    TextButton(onClick = { showDiscardConfirm = false; onDiscard() }) {
                        Text(stringResource(R.string.button_discard), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardConfirm = false }) { Text(stringResource(R.string.button_cancel)) }
                },
            )
        } else {
            Dialog(onDismissRequest = onResume) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.dialog_workout_paused_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResume,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.button_resume))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSave,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.button_save))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showDiscardConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.button_discard))
                        }
                    }
                }
            }
        }
    }

    lastSavedRecording?.let { recording ->
        AlertDialog(
            onDismissRequest = onDismissSaved,
            title = { Text(stringResource(R.string.dialog_workout_saved_title)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                ) {
                    savedFields(recording).forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(text = value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (stravaConnected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        StravaUploadRow(stravaUploadState, onUploadToStrava)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissSaved) { Text(stringResource(R.string.button_ok)) }
            },
        )
    }
}

@Composable
private fun StatusBar(
    rowerState: BleConnectionState,
    heartRateState: BleConnectionState,
    workoutState: WorkoutState,
    heartRateBpm: Int?,
    onOpenSettings: () -> Unit,
) {
    Surface(shadowElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mainStatusText(rowerState, workoutState),
                    style = MaterialTheme.typography.titleMedium,
                    color = mainStatusColor(rowerState, workoutState),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                ConnectionBadge(stringResource(R.string.badge_rower), rowerState)
                Spacer(modifier = Modifier.width(16.dp))
                ConnectionBadge(stringResource(R.string.badge_hr), heartRateState, extra = heartRateBpm?.let { stringResource(R.string.badge_bpm_format, it) })
            }
        }
    }
}

@Composable
private fun ConnectionBadge(label: String, state: BleConnectionState, extra: String? = null) {
    val (icon, color) = when (state) {
        BleConnectionState.Connected -> Icons.Filled.BluetoothConnected to MaterialTheme.colorScheme.primary
        BleConnectionState.Connecting -> Icons.Filled.Bluetooth to MaterialTheme.colorScheme.secondary
        BleConnectionState.Scanning -> Icons.AutoMirrored.Filled.BluetoothSearching to MaterialTheme.colorScheme.outline
        BleConnectionState.Disconnected -> Icons.Filled.Bluetooth to MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.width(16.dp))
        Text(
            text = "$label: ${extra ?: connectionStateLabel(state)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun connectionStateLabel(state: BleConnectionState): String = stringResource(
    when (state) {
        BleConnectionState.Connected -> R.string.connection_state_connected
        BleConnectionState.Connecting -> R.string.connection_state_connecting
        BleConnectionState.Scanning -> R.string.connection_state_scanning
        BleConnectionState.Disconnected -> R.string.connection_state_disconnected
    },
)

@Composable
private fun PrimaryActionBar(workoutState: WorkoutState, onPrimaryButton: () -> Unit) {
    Surface(shadowElevation = 4.dp) {
        Button(
            onClick = onPrimaryButton,
            enabled = workoutState != WorkoutState.Paused,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (workoutState == WorkoutState.Recording) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
                disabledContentColor = Color.White,
            ),
        ) {
            Icon(
                if (workoutState == WorkoutState.Recording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (workoutState) {
                    WorkoutState.Recording -> stringResource(R.string.button_stop)
                    WorkoutState.Paused -> stringResource(R.string.button_paused)
                    WorkoutState.Idle -> stringResource(R.string.button_start)
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StravaUploadRow(state: StravaUploadState, onUpload: () -> Unit) {
    when (state) {
        StravaUploadState.IDLE -> {
            Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.strava_upload_button))
            }
        }
        StravaUploadState.UPLOADING -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.strava_upload_uploading))
            }
        }
        StravaUploadState.SUCCESS -> {
            Text(stringResource(R.string.strava_upload_success), color = MaterialTheme.colorScheme.primary)
        }
        StravaUploadState.ERROR -> {
            Column {
                Text(stringResource(R.string.strava_upload_failed), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.strava_upload_button))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.height(18.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.headlineMedium)
    }
}

private data class RawField(val icon: ImageVector, val label: String, val value: String)

/**
 * Everything worth showing beyond the 4 main tiles (which already cover
 * stroke rate, distance, and instant pace) - heart rate now comes from the
 * watch overlay in WorkoutViewModel, not the rower itself. Resistance/MET/
 * Energy-per-hour/Energy-per-min/Remaining-time are omitted entirely: this
 * rowing machine never sends them (confirmed by testing).
 */
@Composable
private fun rawFields(sample: RowerSample?): List<RawField> {
    val notSent = stringResource(R.string.field_not_sent)
    return listOf(
        RawField(Icons.Filled.Favorite, stringResource(R.string.field_heart_rate), sample?.heartRateBpm?.toString() ?: notSent),
        RawField(Icons.Filled.Repeat, stringResource(R.string.field_stroke_count), sample?.strokeCount?.toString() ?: notSent),
        RawField(Icons.Filled.Bolt, stringResource(R.string.field_instant_power), sample?.powerWatts?.toString() ?: notSent),
        RawField(Icons.Filled.LocalFireDepartment, stringResource(R.string.field_total_energy), sample?.totalEnergyKcal?.let { "%.2f".format(it) } ?: notSent),
        RawField(Icons.Filled.History, stringResource(R.string.field_device_elapsed_time), sample?.elapsedTimeSeconds?.toString() ?: notSent),
    )
}

/** Every value collected for a finished recording, label + formatted value, for the "Workout saved" dialog. */
@Composable
private fun savedFields(r: WorkoutRecording): List<Pair<String, String>> {
    val notSent = stringResource(R.string.field_not_sent)
    return listOf(
        stringResource(R.string.saved_time) to formatElapsed(r.elapsedSeconds),
        stringResource(R.string.saved_distance) to r.totalDistanceMeters.toString(),
        stringResource(R.string.saved_strokes) to r.totalStrokeCount.toString(),
        stringResource(R.string.saved_calories) to "%.2f".format(r.totalCalories),
        stringResource(R.string.saved_avg_stroke_rate) to "%.1f".format(r.avgStrokeRate),
        stringResource(R.string.saved_avg_pace) to formatPace(r.avgPaceSecondsPer500m),
        stringResource(R.string.saved_avg_power) to r.avgPowerWatts.toString(),
        stringResource(R.string.saved_avg_heart_rate) to r.avgHeartRateBpm.toString(),
        stringResource(R.string.saved_max_stroke_rate) to "%.1f".format(r.maxStrokeRate),
        stringResource(R.string.saved_max_power) to r.maxPowerWatts.toString(),
        stringResource(R.string.saved_max_heart_rate) to r.maxHeartRateBpm.toString(),
        stringResource(R.string.saved_device_elapsed_time) to (r.deviceElapsedTimeSeconds?.toString() ?: notSent),
    )
}

@Composable
private fun mainStatusText(rowerState: BleConnectionState, workout: WorkoutState): String {
    val rowerLost = workout != WorkoutState.Idle && rowerState != BleConnectionState.Connected
    return when {
        rowerLost && workout == WorkoutState.Paused -> stringResource(R.string.status_paused) + stringResource(R.string.status_rower_lost_suffix)
        rowerLost -> stringResource(R.string.status_recording) + stringResource(R.string.status_rower_lost_suffix)
        workout == WorkoutState.Paused -> stringResource(R.string.status_paused)
        workout == WorkoutState.Recording -> stringResource(R.string.status_recording)
        rowerState == BleConnectionState.Connected -> stringResource(R.string.status_connected)
        rowerState == BleConnectionState.Connecting -> stringResource(R.string.status_connecting)
        else -> stringResource(R.string.status_searching)
    }
}

@Composable
private fun mainStatusColor(rowerState: BleConnectionState, workout: WorkoutState): Color = when {
    workout != WorkoutState.Idle && rowerState != BleConnectionState.Connected -> MaterialTheme.colorScheme.error
    workout == WorkoutState.Paused -> MaterialTheme.colorScheme.secondary
    workout == WorkoutState.Recording -> MaterialTheme.colorScheme.error
    rowerState == BleConnectionState.Connected -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.outline
}

/** Pace in seconds/500m -> "m:ss". Mirrors formatPace() in RowingView.mc. */
private fun formatPace(paceSeconds: Int?): String {
    if (paceSeconds == null || paceSeconds <= 0 || paceSeconds >= 3600) return "--:--"
    val minutes = paceSeconds / 60
    val seconds = paceSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Elapsed workout time in seconds -> "m:ss" (or "h:mm:ss" past an hour). Mirrors formatElapsed() in RowingView.mc. */
private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
