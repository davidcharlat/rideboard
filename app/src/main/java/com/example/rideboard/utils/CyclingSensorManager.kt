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
import com.example.rideboard.buffer.GpsBuffer
import java.util.*

enum class SensorType { HEART_RATE, POWER, CADENCE }

class CyclingSensorManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val activeGatts = mutableMapOf<SensorType, BluetoothGatt>()
    private val sensorValues = mutableMapOf<SensorType, Int>()

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
        if (isScanning) stopScan()
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        currentScanningType = type
        isScanning = true
        Log.d("CyclingSensor", "Démarrage scan pour $type...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)

        handler.postDelayed({
            if (isScanning) stopScan()
        }, 20000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d("CyclingSensor", "Scan arrêté")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = (try { device.name } catch (e: Exception) { null })?.lowercase() ?: ""
            val serviceUuids = result.scanRecord?.serviceUuids ?: emptyList()

            val match = when (currentScanningType) {
                SensorType.HEART_RATE -> serviceUuids.contains(ParcelUuid(HEART_RATE_SERVICE)) || name.contains("hr") || name.contains("heart")
                SensorType.POWER -> serviceUuids.contains(ParcelUuid(CYCLING_POWER_SERVICE)) || name.contains("power")
                SensorType.CADENCE -> serviceUuids.contains(ParcelUuid(CSC_SERVICE)) || name.contains("cadence") || name.contains("cad")
                null -> false
            }

            if (match) {
                Log.i("CyclingSensor", "Capteur trouvé pour $currentScanningType: ${device.name} (${device.address})")
                stopScan()
                connectToDevice(device, currentScanningType!!)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice, type: SensorType) {
        activeGatts[type]?.let {
            it.disconnect()
            it.close()
        }
        val gatt = device.connectGatt(context, false, createGattCallback(type), BluetoothDevice.TRANSPORT_LE)
        activeGatts[type] = gatt
    }

    private fun createGattCallback(type: SensorType) = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("CyclingSensor", "Connecté au capteur $type")
                handler.postDelayed({ gatt.discoverServices() }, 600)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("CyclingSensor", "Déconnecté du capteur $type")
                gatt.close()
                activeGatts.remove(type)
                sensorValues.remove(type)
                
                if (GpsBuffer.getLast()?.gpsPointIsMoving == true) {
                    handler.postDelayed({ startScan(type) }, 5000)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
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
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            when (characteristic.uuid) {
                HEART_RATE_MEASUREMENT -> {
                    val flag = value[0].toInt()
                    val format = if (flag and 0x01 != 0) 1 else 0
                    sensorValues[SensorType.HEART_RATE] = if (format == 0) value[1].toInt() and 0xFF else ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                }
                CYCLING_POWER_MEASUREMENT -> {
                    // Format: Flags (16bit), Instantaneous Power (sint16)
                    sensorValues[SensorType.POWER] = ((value[3].toInt() and 0xFF) shl 8) or (value[2].toInt() and 0xFF)
                    
                    // Si le capteur de puissance envoie aussi la cadence (Wheel/Crank Revolution Data)
                    val flags = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
                    if (flags and 0x20 != 0) { // Crank Revolution Data present
                        // Cadence calculée ailleurs ou via CSC, ici on se concentre sur Power
                    }
                }
                CSC_MEASUREMENT -> {
                    val flags = value[0].toInt()
                    if (flags and 0x02 != 0) { // Crank Revolution Data Present
                        // Note: Pour une vraie cadence, il faut calculer le delta entre deux révolutions
                        // Ici on stocke une valeur brute ou on ignore si pas de calcul de temps
                    }
                }
            }
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }
    }
}
