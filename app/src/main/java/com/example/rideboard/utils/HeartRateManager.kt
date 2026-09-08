package com.example.rideboard.utils

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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

class HeartRateManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var lastHeartRate: Int? = null

    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    val currentHeartRate: Int? get() = lastHeartRate

    @SuppressLint("MissingPermission")
    fun startScan() {
        try {
            if (isScanning) return
            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
                Log.e("HRManager", "Bluetooth scanner non disponible (Bluetooth désactivé ?)")
                return
            }

            // On retire le filtre car beaucoup de capteurs ne diffusent pas leur UUID de service 
            // dans le paquet d'annonce initial pour économiser de la batterie.
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            isScanning = true
            Log.d("HRManager", "Démarrage du scan BLE (sans filtre)...")
            scanner.startScan(null, settings, scanCallback)

            handler.postDelayed({
                if (isScanning) {
                    Log.d("HRManager", "Timeout du scan atteint")
                    stopScan()
                }
            }, 30000)
        } catch (e: Exception) {
            Log.e("HRManager", "Erreur lors du démarrage du scan", e)
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (!isScanning) return
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d("HRManager", "Scan BLE arrêté")
        } catch (e: Exception) {
            Log.e("HRManager", "Erreur lors de l'arrêt du scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val deviceName = try { device.name } catch (e: Exception) { null }
                val rssi = result.rssi
                
                // Log de tous les appareils trouvés pour debug
                Log.v("HRManager", "Appareil détecté: ${deviceName ?: "Inconnu"} (${device.address}) RSSI: $rssi")

                // On vérifie si l'appareil ressemble à un capteur cardiaque
                // 1. Soit via l'UUID de service dans les données de scan
                val serviceUuids = result.scanRecord?.serviceUuids
                val isHRByUuid = serviceUuids?.contains(ParcelUuid(HEART_RATE_SERVICE_UUID)) == true
                
                // 2. Soit via le nom (beaucoup de ceintures ont "HRM", "Heart", "Polar", "Garmin", "Wahoo" dans le nom)
                val name = deviceName?.lowercase() ?: ""
                val isHRByName = name.contains("hr") || name.contains("heart") || 
                                 name.contains("polar") || name.contains("wahoo") || 
                                 name.contains("garmin") || name.contains("coospo") ||
                                 name.contains("decathlon") || name.contains("kalenji")

                if (isHRByUuid || isHRByName) {
                    Log.i("HRManager", "Cible potentielle trouvée: ${deviceName ?: "Inconnu"} (${device.address})")
                    stopScan()
                    connectToDevice(device)
                }
            } catch (e: Exception) {
                Log.e("HRManager", "Erreur dans onScanResult", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("HRManager", "Scan failed with error: $errorCode")
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothGatt != null) {
            closeConnection()
        }
        Log.d("HRManager", "Tentative de connexion à ${device.address}...")
        // Utilisation de transport LE pour être sûr
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun closeConnection() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            lastHeartRate = null
        } catch (e: Exception) {
            Log.e("HRManager", "Erreur lors de la fermeture de la connexion", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("HRManager", "Connecté! Découverte des services...")
                    // Petite pause pour laisser le temps à la pile BT de se stabiliser
                    handler.postDelayed({ gatt.discoverServices() }, 600)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("HRManager", "Déconnecté (status: $status)")
                    gatt.close()
                    if (gatt == bluetoothGatt) bluetoothGatt = null
                    lastHeartRate = null
                    
                    val lastSample = GpsBuffer.getLast()
                    if (lastSample?.gpsPointIsMoving == true) {
                        handler.postDelayed({ startScan() }, 5000)
                    }
                }
            } catch (e: Exception) {
                Log.e("HRManager", "Erreur dans onConnectionStateChange", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("HRManager", "Services découverts")
                    val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
                    
                    if (characteristic != null) {
                        Log.i("HRManager", "Caractéristique HR trouvée, activation...")
                        gatt.setCharacteristicNotification(characteristic, true)
                        
                        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
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
                    } else {
                        Log.e("HRManager", "Le service HR a été trouvé mais pas la caractéristique de mesure!")
                        // On déconnecte car ce n'est probablement pas le bon appareil
                        closeConnection()
                        startScan()
                    }
                }
            } catch (e: Exception) {
                Log.e("HRManager", "Erreur dans onServicesDiscovered", e)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            processHeartRate(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            processHeartRate(characteristic.value)
        }

        private fun processHeartRate(value: ByteArray?) {
            if (value == null || value.isEmpty()) return
            try {
                val flag = value[0].toInt()
                val format = if (flag and 0x01 != 0) 1 else 0
                val bpm = if (format == 0) {
                    value[1].toInt() and 0xFF
                } else {
                    ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                }
                lastHeartRate = bpm
            } catch (e: Exception) {
                Log.e("HRManager", "Erreur parsing BPM", e)
            }
        }
    }
}
