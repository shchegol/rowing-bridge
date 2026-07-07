package dev.zelenzoom.rowingbridge.recording

sealed interface WorkoutState {
    data object Idle : WorkoutState
    data object Recording : WorkoutState
    data object Paused : WorkoutState
}
