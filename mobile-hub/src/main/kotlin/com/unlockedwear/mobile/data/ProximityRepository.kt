package com.unlockedwear.mobile.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

@Singleton
class ProximityRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val scanner get() = bluetoothAdapter?.bluetoothLeScanner
    private val scope = CoroutineScope(Dispatchers.Default)
    private val preferences = context.getSharedPreferences("proximity_lab", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var scanJob: Job? = null
    private var scanCallback: ScanCallback? = null
    private val devices = linkedMapOf<String, ProximityDevice>()

    private val _state = MutableStateFlow(
        ProximityState(calibrationProfiles = loadProfiles())
    )
    val state: StateFlow<ProximityState> = _state.asStateFlow()

    fun setCondition(condition: WallCondition) {
        _state.value = _state.value.copy(selectedCondition = condition, errorMessage = null)
        devices.replaceAll { _, device -> device.copy(estimatedDistanceMeters = estimateDistance(device.rssi)) }
        publishDevices()
    }

    fun hasRequiredPermissions(): Boolean {
        val bluetoothReady = Build.VERSION.SDK_INT < 31 ||
            (hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
        val wifiReady = Build.VERSION.SDK_INT < 33 || hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
        val locationReady = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return bluetoothReady && wifiReady && locationReady
    }

    @SuppressLint("MissingPermission")
    fun scan() {
        if (!hasRequiredPermissions()) {
            _state.value = _state.value.copy(
                hasRequiredPermissions = false,
                errorMessage = "Grant Nearby devices and location access before scanning."
            )
            return
        }

        stopScan()
        devices.clear()
        _state.value = _state.value.copy(
            devices = emptyList(),
            isScanning = true,
            hasRequiredPermissions = true,
            errorMessage = null
        )

        scanner?.let { leScanner ->
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val id = device.address
                    val name = result.scanRecord?.deviceName
                        ?: runCatching { device.name }.getOrNull()
                        ?: "Unnamed BLE device"
                    upsert(
                        id = "ble:$id",
                        name = name,
                        radio = ProximityRadio.BLUETOOTH,
                        rssi = result.rssi
                    )
                }

                override fun onScanFailed(errorCode: Int) {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        errorMessage = "Bluetooth scan failed ($errorCode)."
                    )
                }
            }
            leScanner.startScan(null, settings, scanCallback)
        }

        readWifiResults()
        runCatching { wifiManager?.startScan() }
        scanJob = scope.launch {
            delay(10_000)
            readWifiResults()
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        scanCallback?.let { callback -> runCatching { scanner?.stopScan(callback) } }
        scanCallback = null
        _state.value = _state.value.copy(isScanning = false, lastScanAt = System.currentTimeMillis())
    }

    fun addCalibrationSample(distanceMeters: Float) {
        if (distanceMeters <= 0f) return
        val current = devices.values.minByOrNull { it.estimatedDistanceMeters } ?: return
        val condition = _state.value.selectedCondition
        val old = _state.value.calibrationProfiles[condition] ?: CalibrationProfile(condition)
        val points = (old.points + CalibrationPoint(distanceMeters, current.rssi)).takeLast(12)
        val profile = fitProfile(condition, points)
        val profiles = _state.value.calibrationProfiles + (condition to profile)
        saveProfiles(profiles)
        _state.value = _state.value.copy(calibrationProfiles = profiles, errorMessage = null)
        devices.replaceAll { _, device -> device.copy(estimatedDistanceMeters = estimateDistance(device.rssi)) }
        publishDevices()
    }

    private fun readWifiResults() {
        if (!hasRequiredPermissions()) return
        val results: List<WifiScanResult> = runCatching { wifiManager?.scanResults.orEmpty() }.getOrDefault(emptyList())
        results.forEach { result ->
            val ssid = result.SSID.takeIf { it.isNotBlank() } ?: "Hidden Wi-Fi network"
            upsert(
                id = "wifi:${result.BSSID}",
                name = ssid,
                radio = ProximityRadio.WIFI,
                rssi = result.level
            )
        }
    }

    private fun upsert(id: String, name: String, radio: ProximityRadio, rssi: Int) {
        val previous = devices[id]
        val smoothedRssi = if (previous == null) rssi else (previous.rssi * 0.7f + rssi * 0.3f).toInt()
        val normalized = name.lowercase(Locale.US)
        devices[id] = ProximityDevice(
            id = id,
            name = name,
            radio = radio,
            rssi = smoothedRssi,
            estimatedDistanceMeters = estimateDistance(smoothedRssi),
            isEsp32 = normalized.contains("esp32"),
            isEsp8266 = normalized.contains("esp8266") || normalized.contains("nodemcu"),
            lastSeenMs = System.currentTimeMillis()
        )
        publishDevices()
    }

    private fun publishDevices() {
        _state.value = _state.value.copy(devices = devices.values.sortedBy { it.estimatedDistanceMeters })
    }

    private fun estimateDistance(rssi: Int): Float {
        val profile = _state.value.calibrationProfiles[_state.value.selectedCondition]
            ?: CalibrationProfile(_state.value.selectedCondition)
        val exponent = profile.pathLossExponent.coerceIn(1.2f, 5f)
        return 10f.pow((profile.referenceRssiAtOneMeter - rssi) / (10f * exponent))
            .coerceIn(0.1f, 100f)
    }

    private fun fitProfile(condition: WallCondition, points: List<CalibrationPoint>): CalibrationProfile {
        if (points.size < 2) {
            val point = points.firstOrNull()
            return CalibrationProfile(
                condition = condition,
                points = points,
                referenceRssiAtOneMeter = point?.let {
                    it.rssi + (20f * log10(it.distanceMeters.coerceAtLeast(0.1f).toDouble())).toFloat()
                } ?: -59f
            )
        }
        val xs = points.map { log10(it.distanceMeters.coerceAtLeast(0.1f).toDouble()) }
        val ys = points.map { (-59 - it.rssi).toDouble() }
        val meanX = xs.average()
        val meanY = ys.average()
        val denominator = xs.sumOf { (it - meanX) * (it - meanX) }
        val slope = if (denominator == 0.0) 2.0 else xs.indices.sumOf {
            (xs[it] - meanX) * (ys[it] - meanY)
        } / denominator
        val exponent = slope.coerceIn(1.2, 5.0)
        val reference = points.map {
            it.rssi + (10.0 * exponent * log10(it.distanceMeters.coerceAtLeast(0.1f).toDouble()))
        }.average()
        return CalibrationProfile(condition, points, exponent.toFloat(), reference.toFloat())
    }

    private fun loadProfiles(): Map<WallCondition, CalibrationProfile> {
        val raw = preferences.getString("profiles", null) ?: return emptyMap()
        return runCatching {
            json.decodeFromString<List<CalibrationProfile>>(raw).associateBy { it.condition }
        }.getOrDefault(emptyMap())
    }

    private fun saveProfiles(profiles: Map<WallCondition, CalibrationProfile>) {
        preferences.edit().putString("profiles", json.encodeToString(profiles.values.toList())).apply()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
