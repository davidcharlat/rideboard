package com.example.rideboard.utils

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.config.AppConfig
import java.util.*

enum class SensorType { HEART_RATE, POWER, CADENCE, ENVIRONMENT }
enum class SensorConnectionStatus { DISCONNECTED, SCANNING, CONNECTED, LOST }

class CyclingSensorManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter
        } catch (e: Exception) {
            Log.e("CyclingSensor", "Erreur accès BluetoothAdapter", e)
            null
        }
    }

    private val activeGatts = mutableMapOf<SensorType, BluetoothGatt>()
    private val sensorValues = mutableMapOf<SensorType, Int>()

    // Cache des valeurs mesurées
    private var lastPower: Int? = null
    private var lastPowerBattery: Int? = null
    private var lastPowerPercentRightLeft: Int? = null
    private var lastPowerTorque: Double? = null
    private var lastPowerCadence: Float? = null
    private var lastPedalSmoothnessLeft: Float? = null
    private var lastPedalSmoothnessRight: Float? = null
    private var lastTorqueEffectivenessLeft: Float? = null
    private var lastTorqueEffectivenessRight: Float? = null

    var envTemperature: Float? = null
        private set
    var envHumidity: Float? = null
        private set

    private val everConnected = mutableSetOf<SensorType>()

    val sensorStatuses = mutableStateMapOf<SensorType, SensorConnectionStatus>().apply {
        SensorType.values().forEach { put(it, SensorConnectionStatus.DISCONNECTED) }
    }

    // File d'attente pour l'écriture séquentielle des descripteurs GATT
    private val descriptorWriteQueue = LinkedList<Pair<BluetoothGatt, BluetoothGattDescriptor>>()
    private var isWritingDescriptor = false

    // UUIDs GATT Standards
    private val HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    private val CYCLING_POWER_SERVICE = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    private val CYCLING_POWER_MEASUREMENT = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")
    private val TORQUE_EFFECTIVENESS_PEDAL_SMOOTHNESS = UUID.fromString("00002a69-0000-1000-8000-00805f9b34fb")

    private val CSC_SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    private val CSC_MEASUREMENT = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")

    private val ENVIRONMENTAL_SENSING_SERVICE = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
    private val TEMPERATURE_MEASUREMENT_CHAR = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")
    private val HUMIDITY_MEASUREMENT_CHAR = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")

    private val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_CHAR = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private val INKBIRD_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val INKBIRD_CHAR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private val CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var currentScanningType: SensorType? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    private var lastCrankRevs: Int = -1
    private var lastCrankTime: Int = -1

    val heartRate: Int? get() = sensorValues[SensorType.HEART_RATE]
    val power: Int? get() = sensorValues[SensorType.POWER]
    val cadence: Int? get() = sensorValues[SensorType.CADENCE]

    @SuppressLint("MissingPermission")
    fun startScan(type: SensorType) {
        try {
            if (sensorStatuses[type] == SensorConnectionStatus.CONNECTED && activeGatts[type] != null) {
                Log.d("CyclingSensor", "$type est déjà connecté. Scan ignoré.")
                return
            }

            if (isScanning) stopScan()
            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

            currentScanningType = type
            isScanning = true
            handler.post { sensorStatuses[type] = SensorConnectionStatus.SCANNING }
            Log.d("CyclingSensor", "Démarrage scan pour $type...")

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(null, settings, scanCallback)

            handler.postDelayed({
                if (isScanning && currentScanningType == type) stopScan()
            }, 20000)
        } catch (e: Exception) {
            Log.e("CyclingSensor", "Crash dans startScan", e)
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (!isScanning) return
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false

            val type = currentScanningType
            if (type != null && sensorStatuses[type] == SensorConnectionStatus.SCANNING) {
                if (AppConfig.isRecording && everConnected.contains(type) && activeGatts[type] == null) {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.LOST }
                    Log.d("CyclingSensor", "Scan $type infructueux, passage en LOST (re-scan dans 5s)")
                    handler.postDelayed({ startScan(type) }, 5000)
                } else {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
                    Log.d("CyclingSensor", "Scan $type terminé sans connexion, retour à DISCONNECTED")
                }
            }
            Log.d("CyclingSensor", "Scan arrêté")
        } catch (e: Exception) {
            Log.e("CyclingSensor", "Erreur dans stopScan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val deviceName = try { device.name } catch (e: Exception) { null }
                val name = deviceName?.lowercase() ?: ""
                val serviceUuids = result.scanRecord?.serviceUuids ?: emptyList()

                if (currentScanningType == SensorType.ENVIRONMENT) {
                    val manufacturerData = result.scanRecord?.manufacturerSpecificData
                    parseEnvironmentBroadcast(manufacturerData, name)
                }

                val match = when (currentScanningType) {
                    SensorType.HEART_RATE -> serviceUuids.contains(ParcelUuid(HEART_RATE_SERVICE)) || name.contains("hr") || name.contains("heart")
                    SensorType.POWER -> serviceUuids.contains(ParcelUuid(CYCLING_POWER_SERVICE)) || name.contains("power") || name.contains("assioma")
                    SensorType.CADENCE -> serviceUuids.contains(ParcelUuid(CSC_SERVICE)) || name.contains("cadence") || name.contains("cad")
                    SensorType.ENVIRONMENT -> serviceUuids.contains(ParcelUuid(ENVIRONMENTAL_SENSING_SERVICE)) || serviceUuids.contains(ParcelUuid(INKBIRD_SERVICE)) || name.contains("inkbird") || name.contains("ith") || name.contains("ibs") || name.contains("govee") || name.contains("h507") || name.contains("thermo") || name.contains("temp") || name.contains("hygro")
                    null -> false
                }

                if (match) {
                    Log.i("CyclingSensor", "Capteur trouvé pour $currentScanningType: ${deviceName ?: "Inconnu"} (${device.address})")
                    stopScan()
                    connectToDevice(device, currentScanningType!!)
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur onScanResult", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("CyclingSensor", "Scan failed: $errorCode")
            isScanning = false
        }
    }

    private fun parseEnvironmentBroadcast(manufacturerData: android.util.SparseArray<ByteArray>?, deviceName: String) {
        if (manufacturerData == null || manufacturerData.size() == 0) return
        try {
            for (i in 0 until manufacturerData.size()) {
                val key = manufacturerData.keyAt(i)
                val data = manufacturerData.valueAt(i) ?: continue

                if (key == 0xEC88 || key == 0x0001 || deviceName.contains("govee") || deviceName.contains("h5074")) {
                    if (data.size >= 4) {
                        val tempRaw = (data[1].toInt() shl 8) or (data[0].toInt() and 0xFF)
                        val humRaw = (data[3].toInt() shl 8) or (data[2].toInt() and 0xFF)
                        val tempFloat = tempRaw / 100.0f
                        val humFloat = humRaw / 100.0f
                        if (tempFloat in -40.0f..60.0f && humFloat in 0.0f..100.0f) {
                            updateEnvironmentData(tempFloat, humFloat)
                        }
                    }
                } else if (deviceName.contains("inkbird") || deviceName.contains("ith") || deviceName.contains("ibs")) {
                    if (data.size >= 4) {
                        val tempRaw = (data[1].toInt() shl 8) or (data[0].toInt() and 0xFF)
                        val humRaw = (data[3].toInt() shl 8) or (data[2].toInt() and 0xFF)
                        val tempFloat = tempRaw / 100.0f
                        val humFloat = humRaw / 100.0f
                        if (tempFloat in -40.0f..60.0f && humFloat in 0.0f..100.0f) {
                            updateEnvironmentData(tempFloat, humFloat)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CyclingSensor", "Erreur parseEnvironmentBroadcast", e)
        }
    }

    private fun updateEnvironmentData(temp: Float, hum: Float) {
        envTemperature = temp
        envHumidity = hum
        handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }

        GpsBuffer.getLast()?.let { sample ->
            sample.outTemperature = temp
            sample.outHumidity = hum
        }
    }

    private fun queueDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor) {
        synchronized(descriptorWriteQueue) {
            descriptorWriteQueue.add(Pair(gatt, descriptor))
            if (!isWritingDescriptor) {
                processNextDescriptorWrite()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun processNextDescriptorWrite() {
        synchronized(descriptorWriteQueue) {
            val next = descriptorWriteQueue.poll()
            if (next == null) {
                isWritingDescriptor = false
                // Une fois toutes les souscriptions aux notifications terminées, on peut lire la batterie
                readBatteryLevelIfAvailable()
                return
            }
            isWritingDescriptor = true
            val (gatt, descriptor) = next
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur processNextDescriptorWrite", e)
                isWritingDescriptor = false
                processNextDescriptorWrite()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readBatteryLevelIfAvailable() {
        handler.postDelayed({
            try {
                activeGatts.values.forEach { gatt ->
                    val batService = gatt.getService(BATTERY_SERVICE)
                    val batChar = batService?.getCharacteristic(BATTERY_LEVEL_CHAR)
                    if (batChar != null && (batChar.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        gatt.readCharacteristic(batChar)
                    }
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur readBatteryLevelIfAvailable", e)
            }
        }, 500)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice, type: SensorType) {
        try {
            activeGatts[type]?.let {
                it.disconnect()
                it.close()
            }
            Log.d("CyclingSensor", "Tentative connexion GATT $type")
            val gatt = device.connectGatt(context, false, createGattCallback(type), BluetoothDevice.TRANSPORT_LE)
            activeGatts[type] = gatt
        } catch (e: Exception) {
            Log.e("CyclingSensor", "Erreur connectToDevice", e)
            handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
        }
    }

    private fun createGattCallback(type: SensorType) = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("CyclingSensor", "Connecté au capteur $type")
                    everConnected.add(type)
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }
                    handler.postDelayed({ gatt.discoverServices() }, 600)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("CyclingSensor", "Déconnecté du capteur $type (status: $status)")
                    gatt.close()
                    activeGatts.remove(type)
                    sensorValues.remove(type)

                    if (AppConfig.isRecording && everConnected.contains(type)) {
                        handler.post { sensorStatuses[type] = SensorConnectionStatus.LOST }
                        Log.i("CyclingSensor", "Perte de connexion $type pendant la sortie, re-scan dans 5s...")
                        handler.postDelayed({ startScan(type) }, 5000)
                    } else {
                        handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
                    }
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur onConnectionStateChange", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) return

                handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }

                // Recherche uniquement le service principal du type de capteur actuel
                val serviceUuid = when (type) {
                    SensorType.HEART_RATE -> HEART_RATE_SERVICE
                    SensorType.POWER -> CYCLING_POWER_SERVICE
                    SensorType.CADENCE -> CSC_SERVICE
                    SensorType.ENVIRONMENT -> ENVIRONMENTAL_SENSING_SERVICE
                }

                val service = gatt.getService(serviceUuid) ?: gatt.services.firstOrNull {
                    type == SensorType.ENVIRONMENT && (it.uuid == INKBIRD_SERVICE || it.uuid == ENVIRONMENTAL_SENSING_SERVICE)
                }

                if (service != null) {
                    for (char in service.characteristics) {
                        val props = char.properties
                        val isNotifiable = (props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                        if (isNotifiable && (
                            char.uuid == HEART_RATE_MEASUREMENT ||
                            char.uuid == CYCLING_POWER_MEASUREMENT ||
                            char.uuid == CSC_MEASUREMENT ||
                            char.uuid == TEMPERATURE_MEASUREMENT_CHAR ||
                            char.uuid == HUMIDITY_MEASUREMENT_CHAR ||
                            char.uuid == INKBIRD_CHAR
                        )) {
                            gatt.setCharacteristicNotification(char, true)
                            val descriptor = char.getDescriptor(CONFIG_DESCRIPTOR)
                            if (descriptor != null) {
                                queueDescriptorWrite(gatt, descriptor)
                            }
                        }
                    }
                }

                // Vérifier si le service batterie est aussi présent
                val batteryService = gatt.getService(BATTERY_SERVICE)
                if (batteryService != null) {
                    val batChar = batteryService.getCharacteristic(BATTERY_LEVEL_CHAR)
                    if (batChar != null) {
                        val props = batChar.properties
                        if ((props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
                            gatt.setCharacteristicNotification(batChar, true)
                            val descriptor = batChar.getDescriptor(CONFIG_DESCRIPTOR)
                            if (descriptor != null) {
                                queueDescriptorWrite(gatt, descriptor)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur onServicesDiscovered", e)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d("CyclingSensor", "onDescriptorWrite status=$status pour ${descriptor.characteristic.uuid}")
            handler.post { processNextDescriptorWrite() }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR && value.isNotEmpty()) {
                val batteryVal = value[0].toInt() and 0xFF
                lastPowerBattery = batteryVal
                applyCachedSensorValuesToGpsBuffer()
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            onCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            try {
                if (sensorStatuses[type] != SensorConnectionStatus.CONNECTED) {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }
                }

                when (characteristic.uuid) {
                    HEART_RATE_MEASUREMENT -> {
                        val flag = value[0].toInt()
                        val format = if (flag and 0x01 != 0) 1 else 0
                        val hr = if (format == 0) value[1].toInt() and 0xFF else ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                        sensorValues[SensorType.HEART_RATE] = hr
                    }

                    CYCLING_POWER_MEASUREMENT -> {
                        val flags = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
                        val powerVal = ((value[3].toInt() and 0xFF) shl 8) or (value[2].toInt() and 0xFF)
                        sensorValues[SensorType.POWER] = powerVal
                        lastPower = powerVal

                        var offset = 4

                        var rightLeftVal: Int? = null
                        if ((flags and 0x01) != 0 && value.size > offset) {
                            rightLeftVal = value[offset].toInt() and 0xFF
                            lastPowerPercentRightLeft = rightLeftVal
                            offset += 1
                        }

                        var torqueVal: Double? = null
                        if ((flags and 0x04) != 0 && value.size >= offset + 2) {
                            val accTorque = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
                            torqueVal = accTorque / 32.0
                            lastPowerTorque = torqueVal
                            offset += 2
                        }

                        if ((flags and 0x10) != 0) {
                            offset += 6
                        }

                        var cadenceVal: Float? = null
                        if ((flags and 0x20) != 0 && value.size >= offset + 4) {
                            val crankRevs = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
                            val crankTime = ((value[offset + 3].toInt() and 0xFF) shl 8) or (value[offset + 2].toInt() and 0xFF)

                            if (lastCrankRevs != -1 && lastCrankTime != -1) {
                                val dCrank = (crankRevs - lastCrankRevs) and 0xFFFF
                                val dTime = ((crankTime - lastCrankTime) and 0xFFFF) / 1024.0f
                                if (dTime > 0) {
                                    cadenceVal = (dCrank / dTime) * 60.0f
                                    lastPowerCadence = cadenceVal
                                }
                            }
                            lastCrankRevs = crankRevs
                            lastCrankTime = crankTime
                        }

                        applyCachedSensorValuesToGpsBuffer()
                    }

                    TORQUE_EFFECTIVENESS_PEDAL_SMOOTHNESS -> {
                        if (value.size >= 4) {
                            lastTorqueEffectivenessLeft = (value[0].toInt() and 0xFF) / 2.0f
                            lastTorqueEffectivenessRight = (value[1].toInt() and 0xFF) / 2.0f
                            lastPedalSmoothnessLeft = (value[2].toInt() and 0xFF) / 2.0f
                            lastPedalSmoothnessRight = (value[3].toInt() and 0xFF) / 2.0f

                            applyCachedSensorValuesToGpsBuffer()
                        }
                    }

                    BATTERY_LEVEL_CHAR -> {
                        if (value.isNotEmpty()) {
                            val batteryVal = value[0].toInt() and 0xFF
                            lastPowerBattery = batteryVal
                            applyCachedSensorValuesToGpsBuffer()
                        }
                    }

                    CSC_MEASUREMENT -> {
                    }

                    TEMPERATURE_MEASUREMENT_CHAR, INKBIRD_CHAR -> {
                        if (value.size >= 2) {
                            val tempRaw = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
                            val tempFloat = tempRaw / 100.0f
                            if (tempFloat in -40.0f..60.0f) {
                                envTemperature = tempFloat
                                handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }
                                applyCachedSensorValuesToGpsBuffer()
                            }
                        }
                    }

                    HUMIDITY_MEASUREMENT_CHAR -> {
                        if (value.size >= 2) {
                            val humRaw = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
                            val humFloat = humRaw / 100.0f
                            if (humFloat in 0.0f..100.0f) {
                                envHumidity = humFloat
                                handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }
                                applyCachedSensorValuesToGpsBuffer()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur parsing données $type", e)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }
    }

    private fun applyCachedSensorValuesToGpsBuffer() {
        GpsBuffer.getLast()?.let { sample ->
            if (lastPower != null) sample.power = lastPower
            if (lastPowerBattery != null) sample.powerBattery = lastPowerBattery
            if (lastPowerPercentRightLeft != null) sample.powerPercentRightLeft = lastPowerPercentRightLeft
            if (lastPowerTorque != null) sample.powerTorque = lastPowerTorque
            if (lastPowerCadence != null) sample.powerCadence = lastPowerCadence
            if (lastPedalSmoothnessLeft != null) sample.pedalSmoothnessLeft = lastPedalSmoothnessLeft
            if (lastPedalSmoothnessRight != null) sample.pedalSmoothnessRight = lastPedalSmoothnessRight
            if (lastTorqueEffectivenessLeft != null) sample.torqueEffectivenessLeft = lastTorqueEffectivenessLeft
            if (lastTorqueEffectivenessRight != null) sample.torqueEffectivenessRight = lastTorqueEffectivenessRight
            if (envTemperature != null) sample.outTemperature = envTemperature
            if (envHumidity != null) sample.outHumidity = envHumidity
        }
    }
}
