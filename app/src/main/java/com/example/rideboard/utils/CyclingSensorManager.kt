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

enum class SensorType { HEART_RATE, POWER, CADENCE }
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
    
    // Suivi de si un capteur a déjà été connecté au moins une fois
    private val everConnected = mutableSetOf<SensorType>()

    val sensorStatuses = mutableStateMapOf<SensorType, SensorConnectionStatus>().apply {
        SensorType.values().forEach { put(it, SensorConnectionStatus.DISCONNECTED) }
    }

    private val HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    private val CYCLING_POWER_SERVICE = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    private val CYCLING_POWER_MEASUREMENT = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")

    private val CSC_SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    private val CSC_MEASUREMENT = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")

    private val CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var currentScanningType: SensorType? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    val heartRate: Int? get() = sensorValues[SensorType.HEART_RATE]
    val power: Int? get() = sensorValues[SensorType.POWER]
    val cadence: Int? get() = sensorValues[SensorType.CADENCE]

    @SuppressLint("MissingPermission")
    fun startScan(type: SensorType) {
        try {
            if (isScanning) stopScan()
            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

            currentScanningType = type
            isScanning = true
            sensorStatuses[type] = SensorConnectionStatus.SCANNING
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
                // Si on scanne et qu'on n'a rien trouvé, on regarde si on doit retenter (LOST) ou abandonner (DISCONNECTED)
                if (AppConfig.isRecording && everConnected.contains(type) && activeGatts[type] == null) {
                    sensorStatuses[type] = SensorConnectionStatus.LOST
                    Log.d("CyclingSensor", "Scan $type infructueux, passage en LOST (re-scan dans 5s)")
                    handler.postDelayed({ startScan(type) }, 5000)
                } else {
                    sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED
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

                val match = when (currentScanningType) {
                    SensorType.HEART_RATE -> serviceUuids.contains(ParcelUuid(HEART_RATE_SERVICE)) || name.contains("hr") || name.contains("heart")
                    SensorType.POWER -> serviceUuids.contains(ParcelUuid(CYCLING_POWER_SERVICE)) || name.contains("power")
                    SensorType.CADENCE -> serviceUuids.contains(ParcelUuid(CSC_SERVICE)) || name.contains("cadence") || name.contains("cad")
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
            sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED
        }
    }

    private fun createGattCallback(type: SensorType) = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("CyclingSensor", "Connecté au capteur $type")
                    everConnected.add(type) // On marque comme "déjà connecté une fois"
                    sensorStatuses[type] = SensorConnectionStatus.CONNECTED
                    handler.postDelayed({ gatt.discoverServices() }, 600)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("CyclingSensor", "Déconnecté du capteur $type (status: $status)")
                    gatt.close()
                    activeGatts.remove(type)
                    sensorValues.remove(type)
                    
                    if (AppConfig.isRecording && everConnected.contains(type)) {
                        sensorStatuses[type] = SensorConnectionStatus.LOST
                        Log.i("CyclingSensor", "Perte de connexion $type pendant la sortie, re-scan dans 5s...")
                        handler.postDelayed({ startScan(type) }, 5000)
                    } else {
                        sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED
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

                val serviceUuid = when (type) {
                    SensorType.HEART_RATE -> HEART_RATE_SERVICE
                    SensorType.POWER -> CYCLING_POWER_SERVICE
                    SensorType.CADENCE -> CSC_SERVICE
                }
                val charUuid = when (type) {
                    SensorType.HEART_RATE -> HEART_RATE_MEASUREMENT
                    SensorType.POWER -> CYCLING_POWER_MEASUREMENT
                    SensorType.CADENCE -> CSC_MEASUREMENT
                }

                val service = gatt.getService(serviceUuid)
                val characteristic = service?.getCharacteristic(charUuid)
                
                if (characteristic != null) {
                    Log.d("CyclingSensor", "Service et Caractéristique trouvés pour $type")
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CONFIG_DESCRIPTOR)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CyclingSensor", "Erreur onServicesDiscovered", e)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            try {
                // On s'assure que le statut est bien CONNECTED (règle le bug de la couleur ératique)
                if (sensorStatuses[type] != SensorConnectionStatus.CONNECTED) {
                    sensorStatuses[type] = SensorConnectionStatus.CONNECTED
                }

                when (characteristic.uuid) {
                    HEART_RATE_MEASUREMENT -> {
                        val flag = value[0].toInt()
                        val format = if (flag and 0x01 != 0) 1 else 0
                        sensorValues[SensorType.HEART_RATE] = if (format == 0) value[1].toInt() and 0xFF else ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                    }
                    CYCLING_POWER_MEASUREMENT -> {
                        sensorValues[SensorType.POWER] = ((value[3].toInt() and 0xFF) shl 8) or (value[2].toInt() and 0xFF)
                    }
                    CSC_MEASUREMENT -> {
                        // Cadence brute ignorée ici (nécessite calcul de temps)
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
}
