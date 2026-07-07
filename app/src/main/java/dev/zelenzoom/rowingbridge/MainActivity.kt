package dev.zelenzoom.rowingbridge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.zelenzoom.rowingbridge.ble.BlePermissions
import dev.zelenzoom.rowingbridge.recording.WorkoutState
import dev.zelenzoom.rowingbridge.ui.WorkoutViewModel
import dev.zelenzoom.rowingbridge.ui.connect.ConnectScreen
import dev.zelenzoom.rowingbridge.ui.settings.SettingsScreen
import dev.zelenzoom.rowingbridge.ui.settings.SettingsViewModel
import dev.zelenzoom.rowingbridge.ui.settings.ThemeMode
import dev.zelenzoom.rowingbridge.ui.theme.RowingBridgeTheme
import dev.zelenzoom.rowingbridge.ui.workout.WorkoutScreen

/**
 * AppCompatActivity (not plain ComponentActivity) is required for in-app
 * language switching via AppCompatDelegate (see SettingsViewModel) - it
 * needs an AppCompat-descended theme (see res/values/themes.xml) and this
 * base class to intercept locale changes and recreate itself correctly on
 * API levels before Android 13's native per-app language support.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: WorkoutViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStravaRedirectIfAny(intent)
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            RowingBridgeTheme(darkTheme = darkTheme) {
                var permissionGranted by remember { mutableStateOf(BlePermissions.allGranted(this)) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    permissionGranted = result.values.all { it }
                }

                if (permissionGranted) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "workout") {
                        composable("workout") {
                            val connectionState by viewModel.connectionState.collectAsState()
                            val heartRateConnectionState by viewModel.heartRateConnectionState.collectAsState()
                            val workoutState by viewModel.workoutState.collectAsState()
                            val sample by viewModel.latestSample.collectAsState()
                            val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                            val lastSavedRecording by viewModel.lastSavedRecording.collectAsState()
                            val stravaUploadState by viewModel.stravaUploadState.collectAsState()

                            LaunchedEffect(Unit) { viewModel.startScanning() }

                            WorkoutScreen(
                                connectionState = connectionState,
                                heartRateConnectionState = heartRateConnectionState,
                                workoutState = workoutState,
                                sample = sample,
                                elapsedSeconds = elapsedSeconds,
                                lastSavedRecording = lastSavedRecording,
                                onPrimaryButton = {
                                    if (workoutState == WorkoutState.Recording) {
                                        viewModel.pauseWorkout()
                                    } else {
                                        viewModel.startWorkout()
                                    }
                                },
                                onResume = viewModel::resumeWorkout,
                                onSave = viewModel::saveWorkout,
                                onDiscard = viewModel::discardWorkout,
                                onDismissSaved = viewModel::dismissSavedRecording,
                                onOpenSettings = { navController.navigate("settings") },
                                stravaConnected = viewModel.isStravaConnected(),
                                stravaUploadState = stravaUploadState,
                                onUploadToStrava = viewModel::uploadLastSavedToStrava,
                            )
                        }
                        composable("settings") {
                            val stravaConnected by settingsViewModel.stravaConnected.collectAsState()
                            SettingsScreen(
                                themeMode = themeMode,
                                onThemeModeChange = settingsViewModel::setThemeMode,
                                currentLanguageTag = settingsViewModel.currentLanguageTag(),
                                onLanguageChange = settingsViewModel::setLanguageTag,
                                stravaAvailable = settingsViewModel.stravaAvailable,
                                stravaConnected = stravaConnected,
                                onConnectStrava = settingsViewModel::connectStrava,
                                onDisconnectStrava = settingsViewModel::disconnectStrava,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                } else {
                    ConnectScreen(onRequestPermission = { permissionLauncher.launch(BlePermissions.required()) })
                }
            }
        }
    }

    // launchMode="singleTop" means an already-running instance gets the
    // Strava OAuth redirect here instead of a fresh onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStravaRedirectIfAny(intent)
    }

    private fun handleStravaRedirectIfAny(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "rowingbridge" && uri.host == "oauth-callback") {
                settingsViewModel.handleStravaRedirect(uri)
            }
        }
    }
}
