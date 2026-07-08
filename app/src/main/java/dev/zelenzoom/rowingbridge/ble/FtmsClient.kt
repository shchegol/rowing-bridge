package dev.zelenzoom.rowingbridge.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "FtmsClient"

private val FTMS_SERVICE_UUID: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
private val ROWER_DATA_UUID: UUID = UUID.fromString("00002ad1-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * Scans for, connects to, and subscribes to a BLE FTMS rowing machine.
 * Kotlin analogue of garmin-rowing/source/FtmsManager.mc: same service/
 * characteristic UUIDs, same scan -> connect -> enable-notifications flow,
 * same auto-rescan-on-disconnect behavior.
 *
 * Callers must ensure BlePermissions.allGranted(context) before calling
 * start() - the BLE APIs below throw SecurityException otherwise.
 */
@SuppressLint("MissingPermission")
class FtmsClient(private val context: Context) {

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _samples = MutableSharedFlow<RowerSample>(replay = 1, extraBufferCapacity = 16)
    val samples: SharedFlow<RowerSample> = _samples.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private var wantsConnection = false
    private var quirks: RowerQuirks = RowerQuirks()

    private val bluetoothManager: BluetoothManager?
        get() = context.getSystemService(BluetoothManager::class.java)

    fun start() {
        // Idempotent: a caller (e.g. a recomposed LaunchedEffect after a
        // config change) may call start() again while already scanning/
        // connected - starting a second overlapping scan/connect attempt on
        // top of a live one is what caused the infinite-reconnect bug.
        if (wantsConnection) return
        wantsConnection = true
        startScan()
    }

    fun stop() {
        wantsConnection = false
        bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    private fun startScan() {
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "No BLE scanner available (adapter off?)")
            return
        }
        _connectionState.value = BleConnectionState.Scanning
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(FTMS_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(this)
            _connectionState.value = BleConnectionState.Connecting
            quirks = RowerQuirksRegistry.forDeviceName(result.device.name)
            Log.d(TAG, "Connecting to '${result.device.name}', quirks: $quirks")
            gatt = result.device.connectGatt(context, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    g.close()
                    gatt = null
                    _connectionState.value = BleConnectionState.Disconnected
                    if (wantsConnection) startScan()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val characteristic = g.getService(FTMS_SERVICE_UUID)
                ?.getCharacteristic(ROWER_DATA_UUID)
            if (characteristic == null) {
                Log.w(TAG, "Rower Data characteristic not found")
                return
            }
            g.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(cccd)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            _connectionState.value = BleConnectionState.Connected
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val bytes = characteristic.value ?: return
            if (characteristic.uuid == ROWER_DATA_UUID) {
                _samples.tryEmit(RowerDataParser.parse(bytes, quirks))
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == ROWER_DATA_UUID) {
                _samples.tryEmit(RowerDataParser.parse(value, quirks))
            }
        }
    }
}
