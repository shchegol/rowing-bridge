package dev.zelenzoom.rowingbridge.ble

/** Shared connection-state shape for any single BLE peripheral client (rower, heart rate strap/watch). */
sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data object Scanning : BleConnectionState
    data object Connecting : BleConnectionState
    data object Connected : BleConnectionState
}
