package com.example.rideboard.utils

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.util.isEmpty
import androidx.core.util.size
import com.example.rideboard.AppSettings
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.config.AppConfig
import java.util.Collections
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

enum class SensorType { HEART_RATE, POWER, CADENCE, ENVIRONMENT }
enum class SensorConnectionStatus { DISCONNECTED, SCANNING, CONNECTED, LOST }

/**
 * Gère les capteurs Bluetooth Low Energy (cardio, puissance, cadence, température/humidité).
 *
 * Plan du fichier :
 *   1. Constantes
 *   2. État public (valeurs et statuts lus par le reste de l'appli)
 *   3. État interne
 *   4. API publique : startScan, stopScan, forget, release
 *   4b. Supervision : reconnexions et scans périodiques (uniquement pendant l'enregistrement)
 *   5. Scan : détection des appareils
 *   6. Connexion GATT : callbacks, découverte des services
 *   7. Abonnements aux notifications (file d'écriture des descripteurs)
 *   8. Décodage des mesures
 *   9. Batterie
 *  10. Écriture dans le GpsBuffer et remise à zéro des caches
 */
class CyclingSensorManager(private val context: Context) {

    // =====================================================================
    // 1. CONSTANTES
    // =====================================================================

    private companion object {
        const val TAG = "CyclingSensor"

        // Durées (ms)
        const val SCAN_TIMEOUT_MS = 20_000L        // un scan s'arrête seul après ce délai
        const val CONNECT_TIMEOUT_MS = 25_000L     // abandon d'une connexion qui ne répond pas
        const val RESCAN_DELAY_MS = 5_000L         // re-scan après une perte de connexion
        const val DISCOVER_DELAY_MS = 600L         // pause entre la connexion et la découverte des services
        const val CCCD_TIMEOUT_MS = 3_000L         // abandon d'une écriture d'abonnement sans réponse
        const val BATTERY_POLL_MS = 10 * 60 * 1000L

        // Nombre maximal d'appareils écartés avant d'abandonner la recherche
        const val MAX_REJECTED_DEVICES = 6

        //constantes pour les calculs
        const val CADENCE_ZERO_AFTER_MS = 5_000L      // sans nouveau tour pendant ce délai : cadence = 0
        const val MAX_PLAUSIBLE_CADENCE = 250f        // au-delà, la mesure est ignorée (parasite)

        // UUID GATT standards
        val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

        val CYCLING_POWER_SERVICE: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val CYCLING_POWER_MEASUREMENT: UUID = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")

        val CSC_SERVICE: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CSC_MEASUREMENT: UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")

        val ENVIRONMENTAL_SENSING_SERVICE: UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
        val TEMPERATURE_MEASUREMENT_CHAR: UUID = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")
        val HUMIDITY_MEASUREMENT_CHAR: UUID = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")

        val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        val INKBIRD_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val INKBIRD_CHAR: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        val CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Caractéristiques de mesure auxquelles on s'abonne. */
        val MEASUREMENT_CHARACTERISTICS = setOf(
            HEART_RATE_MEASUREMENT,
            CYCLING_POWER_MEASUREMENT,
            CSC_MEASUREMENT,
            TEMPERATURE_MEASUREMENT_CHAR,
            HUMIDITY_MEASUREMENT_CHAR,
            INKBIRD_CHAR
        )
        // Indices par le nom, utilisés en secours quand l'appareil n'annonce pas son service.
        // Un nom donne seulement un candidat : l'appareil est vérifié après la connexion
        // (service attendu présent, sinon il est écarté et la recherche continue).
        val HEART_RATE_NAME_HINTS = listOf("hr", "heart")
        val POWER_NAME_HINTS = listOf("power")
        val CADENCE_NAME_HINTS = listOf("cadence", "cad")
        val ENVIRONMENT_NAME_HINTS = listOf("inkbird", "ith", "ibs", "govee", "h507", "thermo", "temp", "hygro")

        // Capteurs de température qui diffusent leurs mesures dans l'annonce (sans connexion)
        const val GOVEE_COMPANY_ID = 0xEC88
        const val ALT_COMPANY_ID = 0x0001
        // SwitchBot : identifiant constructeur 0x0969 ; le type d'appareil est le 1er octet des données de service FD3D
        const val SWITCHBOT_COMPANY_ID = 0x0969
        const val SWITCHBOT_TYPE_INDOOR_OUTDOOR = 0x77            // 'w'
        val SWITCHBOT_SERVICE_DATA_UUID: ParcelUuid = ParcelUuid.fromString("0000fd3d-0000-1000-8000-00805f9b34fb")
        val ENVIRONMENT_BROADCAST_NAME_HINTS = listOf("govee", "h5074", "inkbird", "ith", "ibs")

        const val ENV_SCAN_WINDOW_MS = 15_000L      // durée d'un scan périodique du capteur d'ambiance
        // La pause entre deux scans du capteur d'ambiance est réglable : AppSettings.envRescanSeconds
        const val ENV_STALE_MS = 5 * 60 * 1000L     // sans mesure valide au-delà (et d'au moins 3 pauses), les valeurs sont effacées
        const val SUPERVISOR_PERIOD_MS = 2_000L     // cadence de vérification des capteurs à reconnecter
    }

    // =====================================================================
    // 2. ÉTAT PUBLIC
    // =====================================================================

    val sensorStatuses = mutableStateMapOf<SensorType, SensorConnectionStatus>().apply {
        SensorType.entries.forEach { put(it, SensorConnectionStatus.DISCONNECTED) }
    }

    val heartRate: Int? get() = sensorValues[SensorType.HEART_RATE]
    val power: Int? get() = sensorValues[SensorType.POWER]
    val cadence: Int? get() = sensorValues[SensorType.CADENCE]

    var envTemperature: Float? = null
        private set
    var envHumidity: Float? = null
        private set

    /** Température d'ambiance corrigée (réglage AppSettings.tempCorrection) si la mesure est récente, sinon null. */
    val freshEnvTemperature: Float?
        get() = if (isEnvFresh()) envTemperature?.let { it + AppSettings.tempCorrection } else null

    /** Humidité d'ambiance si la mesure est récente, sinon null. */
    val freshEnvHumidity: Float?
        get() = if (isEnvFresh()) envHumidity else null

    // =====================================================================
    // 3. ÉTAT INTERNE
    // =====================================================================

    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter
        } catch (e: Exception) {
            Log.e(TAG, "Erreur accès BluetoothAdapter", e)
            null
        }
    }

    // --- Scan
    @Volatile private var currentScanningType: SensorType? = null
    @Volatile private var isScanning = false
    @Volatile private var scanToken = 0                       // identifie le scan en cours (pour sa minuterie)
    private val rejectedAddresses = Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile private var envLockedAddress: String? = null    // capteur d'ambiance retenu (évite de mélanger plusieurs capteurs)

    // --- Supervision (reconnexions et scans périodiques, uniquement pendant l'enregistrement)
    private val wantedSensors = Collections.synchronizedSet(mutableSetOf<SensorType>())   // capteurs demandés par l'utilisateur
    private val scanEndedAt = ConcurrentHashMap<SensorType, Long>()                       // fin du dernier scan, par capteur
    private var supervisorStarted = false

    //peremption de données
    @Volatile private var envUpdatedAt = 0L

    //cadence
    @Volatile private var lastCadence: Int? = null
    @Volatile private var lastCscCrankRevs: Int = -1
    @Volatile private var lastCscCrankTime: Int = -1
    @Volatile private var lastCrankEventAt: Long = 0L

    private val cadenceTimeout = Runnable {
        if (lastCadence != 0 && SystemClock.elapsedRealtime() - lastCrankEventAt >= CADENCE_ZERO_AFTER_MS) {
            setCadence(0)
        }
    }

    // --- Connexions
    private val activeGatts = ConcurrentHashMap<SensorType, BluetoothGatt>()
    private val everConnected = Collections.synchronizedSet(mutableSetOf<SensorType>())
    private val firstNotification = Collections.synchronizedSet(mutableSetOf<UUID>())

    // --- File d'écriture des descripteurs (une seule écriture GATT à la fois)
    private val descriptorWriteQueue = LinkedList<Pair<BluetoothGatt, BluetoothGattDescriptor>>()
    private var isWritingDescriptor = false
    private var writeToken = 0

    // --- Valeurs mesurées
    private val sensorValues = ConcurrentHashMap<SensorType, Int>()
    private val batteryLevels = ConcurrentHashMap<SensorType, Int>()

    @Volatile private var lastPower: Int? = null
    @Volatile private var lastPowerPercentRightLeft: Int? = null
    @Volatile private var lastPowerCadence: Float? = null
    @Volatile private var lastCrankRevs: Int = -1
    @Volatile private var lastCrankTime: Int = -1

    private val batteryPoll = object : Runnable {
        override fun run() {
            activeGatts.forEach { (type, gatt) -> readBattery(type, gatt) }
            handler.postDelayed(this, BATTERY_POLL_MS)
        }
    }

    /** Vérifie régulièrement quels capteurs doivent être recherchés à nouveau (voir supervise). */
    private val supervisor = object : Runnable {
        override fun run() {
            try {
                supervise()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur supervise", e)
            }
            handler.postDelayed(this, SUPERVISOR_PERIOD_MS)
        }
    }

    // =====================================================================
    // 4. API PUBLIQUE
    // =====================================================================

    /**
     * Lance la recherche d'un capteur du type demandé.
     * @param userRequested true pour une demande de l'utilisateur (bouton) : la liste des appareils écartés est
     *        remise à zéro. false pour la suite automatique d'une recherche (appareil écarté : on cherche le bon).
     * @param background true pour un scan de fond (reconnexion, rafraîchissement du thermomètre) : il n'a jamais
     *        lieu hors enregistrement et ne coupe jamais un scan en cours.
     */
    @SuppressLint("MissingPermission")
    fun startScan(type: SensorType, userRequested: Boolean = true, background: Boolean = false) {
        try {
            if (background && !AppConfig.isRecording) return

            if (userRequested) {
                rejectedAddresses.clear()
                if (type == SensorType.ENVIRONMENT) envLockedAddress = null
                wantedSensors.add(type)
                ensureSupervisor()
            }
            if (activeGatts[type] != null) {
                Log.d(TAG, "$type : connexion déjà en cours ou établie. Scan ignoré.")
                return
            }

            if (isScanning) {
                if (background) return        // un scan de fond n'interrompt pas un scan en cours
                stopScan()
            }
            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

            currentScanningType = type
            isScanning = true

            // Un scan de rafraîchissement du thermomètre ne change pas le statut du bouton
            val quiet = background && type == SensorType.ENVIRONMENT
            if (!quiet) {
                handler.post { sensorStatuses[type] = SensorConnectionStatus.SCANNING }
            }
            Log.d(TAG, "Démarrage scan pour $type...")

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, scanCallback)

            // La minuterie ne concerne que CE scan : un scan plus récent n'est pas interrompu par elle
            val windowMs = if (quiet) ENV_SCAN_WINDOW_MS else SCAN_TIMEOUT_MS
            val token = ++scanToken
            handler.postDelayed({
                if (isScanning && scanToken == token) stopScan()
            }, windowMs)
        } catch (e: Exception) {
            Log.e(TAG, "Crash dans startScan", e)
            isScanning = false
        }
    }

    /**
     * Arrête le scan.
     * @param connecting true quand on s'arrête parce qu'un appareil a été trouvé : le statut reste alors
     * "en cours" (jaune) pendant la connexion, au lieu de repasser à "déconnecté".
     */
    @SuppressLint("MissingPermission")
    fun stopScan(connecting: Boolean = false) {
        try {
            if (!isScanning) return
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false

            val type = currentScanningType
            if (type != null) scanEndedAt[type] = SystemClock.elapsedRealtime()

            if (!connecting && type != null && sensorStatuses[type] == SensorConnectionStatus.SCANNING) {
                if (AppConfig.isRecording && everConnected.contains(type) && activeGatts[type] == null) {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.LOST }
                    Log.d(TAG, "Scan $type infructueux, passage en LOST (le superviseur réessaiera)")
                } else {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
                    Log.d(TAG, "Scan $type terminé sans connexion, retour à DISCONNECTED")
                }
            }
            Log.d(TAG, "Scan arrêté")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans stopScan", e)
        }
    }

    /** L'utilisateur retire un capteur de la barre : on le déconnecte et on ne le recherche plus. */
    @SuppressLint("MissingPermission")
    fun forget(type: SensorType) {
        wantedSensors.remove(type)
        everConnected.remove(type)
        if (isScanning && currentScanningType == type) stopScan(connecting = true)
        activeGatts.remove(type)?.let { try { it.disconnect(); it.close() } catch (_: Exception) {} }
        sensorValues.remove(type)
        batteryLevels.remove(type)
        when (type) {
            SensorType.POWER -> clearPowerCache()
            SensorType.CADENCE -> clearCadenceCache()
            SensorType.ENVIRONMENT -> {
                envLockedAddress = null
                envTemperature = null
                envHumidity = null
                envUpdatedAt = 0L
            }
            SensorType.HEART_RATE -> {}
        }
        handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
    }

    /** Ferme toutes les connexions et remet l'état à zéro (à appeler quand on quitte l'appli). */
    @SuppressLint("MissingPermission")
    fun release() {
        try { stopScan(connecting = true) } catch (_: Exception) {}
        isScanning = false
        handler.removeCallbacksAndMessages(null)
        supervisorStarted = false
        synchronized(descriptorWriteQueue) {
            descriptorWriteQueue.clear()
            isWritingDescriptor = false
        }
        activeGatts.values.toList().forEach { try { it.disconnect(); it.close() } catch (_: Exception) {} }
        activeGatts.clear()
        sensorValues.clear()
        batteryLevels.clear()
        everConnected.clear()
        wantedSensors.clear()
        scanEndedAt.clear()
        firstNotification.clear()
        rejectedAddresses.clear()
        envLockedAddress = null
        envTemperature = null
        envHumidity = null
        envUpdatedAt = 0L
        clearPowerCache()
        clearCadenceCache()
        SensorType.entries.forEach { sensorStatuses[it] = SensorConnectionStatus.DISCONNECTED }
    }

    // =====================================================================
    // 4b. SUPERVISION : RECONNEXIONS ET SCANS PÉRIODIQUES
    // =====================================================================

    private fun ensureSupervisor() {
        if (supervisorStarted) return
        supervisorStarted = true
        handler.postDelayed(supervisor, SUPERVISOR_PERIOD_MS)
    }

    /**
     * Appelé toutes les SUPERVISOR_PERIOD_MS, sur le thread principal.
     * Hors enregistrement, il ne lance AUCUN scan : la liste des capteurs demandés est conservée, et les
     * reconnexions reprennent dès que l'enregistrement démarre.
     */
    private fun supervise() {
        checkEnvironmentFreshness()

        if (!AppConfig.isRecording || isScanning) return
        val now = SystemClock.elapsedRealtime()

        for (type in wantedSensors.toList()) {
            if (activeGatts[type] != null) continue                       // connexion établie ou en cours

            val lastEnd = scanEndedAt[type] ?: 0L
            val broadcastSensor = type == SensorType.ENVIRONMENT && envLockedAddress != null
            val due = when {
                broadcastSensor -> now - lastEnd >= envRescanMs()         // capteur qui diffuse : lecture périodique
                everConnected.contains(type) -> now - lastEnd >= RESCAN_DELAY_MS   // capteur déjà connecté, perdu
                else -> false                                             // jamais trouvé : pas de recherche automatique
            }
            if (due) {
                Log.d(TAG, if (broadcastSensor) "Scan périodique du capteur d'ambiance" else "Nouvelle tentative de connexion $type")
                startScan(type, userRequested = false, background = true)
                return
            }
        }
    }

    /** Sans mesure valide depuis trop longtemps : on efface les valeurs (écran et buffer) et le bouton quitte le vert. */
    private fun checkEnvironmentFreshness() {
        if (envUpdatedAt == 0L || isEnvFresh()) return

        if (envTemperature != null || envHumidity != null) {
            Log.w(TAG, "Mesures d'ambiance périmées (plus de ${envStaleMs() / 1000} s) : effacées")
            envTemperature = null
            envHumidity = null
            applyCachedSensorValuesToGpsBuffer()                          // écrit null dans le dernier échantillon
        }
        val status = sensorStatuses[SensorType.ENVIRONMENT]
        if (status == SensorConnectionStatus.CONNECTED && activeGatts[SensorType.ENVIRONMENT] == null) {
            sensorStatuses[SensorType.ENVIRONMENT] =
                if (AppConfig.isRecording) SensorConnectionStatus.LOST else SensorConnectionStatus.DISCONNECTED
        }
    }

    private fun envRescanMs(): Long = AppSettings.envRescanSeconds.coerceAtLeast(5) * 1000L
    private fun envStaleMs(): Long = maxOf(ENV_STALE_MS, 3 * envRescanMs() + ENV_SCAN_WINDOW_MS)
    private fun isEnvFresh(): Boolean = envUpdatedAt != 0L && SystemClock.elapsedRealtime() - envUpdatedAt < envStaleMs()

    // =====================================================================
    // 5. SCAN : DÉTECTION DES APPAREILS
    // =====================================================================

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val scanType = currentScanningType ?: return
                val device = result.device
                val deviceName = try { device.name } catch (e: Exception) { null }
                val name = deviceName?.lowercase() ?: ""
                val serviceUuids = result.scanRecord?.serviceUuids ?: emptyList()

                // Les capteurs d'ambiance qui diffusent dans leur annonce sont lus sans connexion
                if (scanType == SensorType.ENVIRONMENT) {
                    parseEnvironmentBroadcast(device.address, result.scanRecord, name)
                    // Un capteur qui diffuse est déjà retenu : on ne se connecte à aucun autre thermomètre trouvé
                    if (envLockedAddress != null) return
                }

                if (!matchesSensor(scanType, serviceUuids, name)) return
                if (!isScanning || activeGatts[scanType] != null) return   // résultats en double après stopScan
                if (device.address in rejectedAddresses) return           // appareil déjà écarté

                Log.i(
                    TAG, "Capteur trouvé pour $scanType: ${deviceName ?: "Inconnu"} (${device.address}) " +
                            "services annoncés=" + serviceUuids.map { it.uuid.toString().substring(4, 8) }
                )
                stopScan(connecting = true)
                connectToDevice(device, scanType)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur onScanResult", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            isScanning = false
        }
    }

    /** Un appareil est un candidat s'il annonce le service attendu, ou (en secours) si son nom y ressemble. */
    private fun matchesSensor(type: SensorType, serviceUuids: List<ParcelUuid>, name: String): Boolean = when (type) {
        SensorType.HEART_RATE ->
            ParcelUuid(HEART_RATE_SERVICE) in serviceUuids || HEART_RATE_NAME_HINTS.any { it in name }
        SensorType.POWER ->
            ParcelUuid(CYCLING_POWER_SERVICE) in serviceUuids || POWER_NAME_HINTS.any { it in name }
        SensorType.CADENCE ->
            ParcelUuid(CSC_SERVICE) in serviceUuids || CADENCE_NAME_HINTS.any { it in name }
        SensorType.ENVIRONMENT ->
            ParcelUuid(ENVIRONMENTAL_SENSING_SERVICE) in serviceUuids ||
                    ParcelUuid(INKBIRD_SERVICE) in serviceUuids ||
                    ENVIRONMENT_NAME_HINTS.any { it in name }
    }

    /** Décode température et humidité diffusées dans l'annonce (sans connexion). */
    /** Lit température et humidité diffusées dans l'annonce (sans connexion). */
    private fun parseEnvironmentBroadcast(address: String, record: ScanRecord?, deviceName: String) {
        val manufacturerData = record?.manufacturerSpecificData ?: return
        if (manufacturerData.isEmpty()) return

        // Une fois un capteur retenu, on ignore les autres (sinon les valeurs de plusieurs capteurs se mélangent)
        val locked = envLockedAddress
        if (locked != null && locked != address) return

        try {
            // SwitchBot : reconnu par son identifiant constructeur, format propre
            val switchBot = manufacturerData.get(SWITCHBOT_COMPANY_ID)
            if (switchBot != null) {
                decodeSwitchBotIndoorOutdoor(address, switchBot, record?.getServiceData(SWITCHBOT_SERVICE_DATA_UUID))
                return
            }

            // Govee, Inkbird... (inchangé)
            for (i in 0 until manufacturerData.size) {
                val key = manufacturerData.keyAt(i)
                val data = manufacturerData.valueAt(i) ?: continue

                val looksLikeEnvironmentSensor = key == GOVEE_COMPANY_ID || key == ALT_COMPANY_ID ||
                        ENVIRONMENT_BROADCAST_NAME_HINTS.any { it in deviceName }
                if (!looksLikeEnvironmentSensor || data.size < 4) continue

                val tempFloat = ((data[1].toInt() shl 8) or (data[0].toInt() and 0xFF)) / 100.0f
                val humFloat = ((data[3].toInt() shl 8) or (data[2].toInt() and 0xFF)) / 100.0f
                if (tempFloat in -40.0f..60.0f && humFloat in 0.0f..100.0f) {
                    if (envLockedAddress == null) {
                        envLockedAddress = address
                        Log.i(TAG, "Capteur d'ambiance retenu : $address")
                    }
                    updateEnvironmentData(tempFloat, humFloat)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parseEnvironmentBroadcast", e)
        }
    }

    /**
     * SwitchBot Indoor/Outdoor Thermo-Hygrometer. Données constructeur (après l'identifiant) :
     * [adresse 6 octets] [...] température aux octets 8-9, humidité à l'octet 10.
     * Données de service FD3D : octet 0 = type d'appareil, octet 2 = batterie.
     */
    private fun decodeSwitchBotIndoorOutdoor(address: String, data: ByteArray, serviceData: ByteArray?) {
        // Les autres appareils SwitchBot (Bot, rideaux...) ont le même identifiant constructeur mais un autre format :
        // on ne décode que le type connu.
        if (serviceData == null || serviceData.isEmpty() ||
            (serviceData[0].toInt() and 0x7F) != SWITCHBOT_TYPE_INDOOR_OUTDOOR) return
        if (data.size < 11) return

        val magnitude = (data[8].toInt() and 0x0F) * 0.1f + (data[9].toInt() and 0x7F)
        val temperature = if ((data[9].toInt() and 0x80) != 0) magnitude else -magnitude
        val humidity = (data[10].toInt() and 0x7F).toFloat()
        if (temperature !in -40.0f..60.0f || humidity !in 0.0f..100.0f) return

        if (envLockedAddress == null) {
            envLockedAddress = address
            Log.i(TAG, "Capteur d'ambiance SwitchBot retenu : $address")
        }
        updateEnvironmentData(temperature, humidity)

        if (serviceData.size >= 3) {
            batteryLevels[SensorType.ENVIRONMENT] = serviceData[2].toInt() and 0x7F
            applyCachedSensorValuesToGpsBuffer()
        }
    }
    private var lastEnvLog = 0L

    private fun updateEnvironmentData(temp: Float, hum: Float) {
        envTemperature = temp
        envHumidity = hum
        envUpdatedAt = SystemClock.elapsedRealtime()      // c'était la ligne qui manquait
        handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }
        applyCachedSensorValuesToGpsBuffer()              // remplace l'écriture directe dans le dernier échantillon

        val now = SystemClock.elapsedRealtime()
        if (now - lastEnvLog > 5_000) {
            lastEnvLog = now
            Log.d(TAG, "Ambiance : %.1f° %.0f%% (GpsBuffer : %d échantillon(s))".format(temp, hum, GpsBuffer.size))
        }
    }

    // =====================================================================
    // 6. CONNEXION GATT
    // =====================================================================

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice, type: SensorType) {
        try {
            activeGatts.remove(type)?.let {
                it.disconnect()
                it.close()
            }
            Log.d(TAG, "Tentative connexion GATT $type")
            val gatt = device.connectGatt(context, false, createGattCallback(type), BluetoothDevice.TRANSPORT_LE)
            activeGatts[type] = gatt

            // Pendant la connexion, le statut reste "en cours" (jaune) au lieu de "déconnecté"
            handler.post { sensorStatuses[type] = SensorConnectionStatus.SCANNING }

            // Abandon si l'appareil ne répond pas
            handler.postDelayed({
                if (activeGatts[type] === gatt && sensorStatuses[type] != SensorConnectionStatus.CONNECTED) {
                    Log.w(TAG, "Connexion $type sans réponse après ${CONNECT_TIMEOUT_MS / 1000} s : abandon")
                    activeGatts.remove(type)
                    gatt.disconnect()
                    gatt.close()
                    sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED
                }
            }, CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur connectToDevice", e)
            handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
        }
    }

    private fun createGattCallback(type: SensorType) = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Connecté au capteur $type (status=$status)")
                        everConnected.add(type)
                        handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }
                        handler.postDelayed({ gatt.discoverServices() }, DISCOVER_DELAY_MS)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> onGattDisconnected(gatt, type, status)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur onConnectionStateChange", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                Log.d(TAG, "onServicesDiscovered $type status=$status services=" +
                        gatt.services.map { it.uuid.toString().substring(4, 8) })
                if (status != BluetoothGatt.GATT_SUCCESS) return

                val service = findMainService(gatt, type)
                if (service == null) {
                    rejectDevice(gatt, type)
                    return
                }
                handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }

                subscribeToMeasurements(gatt, type, service)
                subscribeToBattery(gatt)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur onServicesDiscovered", e)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(TAG, "onDescriptorWrite $type status=$status pour ${descriptor.characteristic.uuid}")
            handler.post { processNextDescriptorWrite() }
        }

        // --- Lecture ponctuelle (batterie)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR) {
                storeBattery(type, value)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            onCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
        }

        // --- Notifications
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            try {
                if (firstNotification.add(characteristic.uuid)) {
                    Log.d(TAG, "Première notification $type ${characteristic.uuid} (${value.size} octets)")
                }
                if (sensorStatuses[type] != SensorConnectionStatus.CONNECTED) {
                    handler.post { sensorStatuses[type] = SensorConnectionStatus.CONNECTED }
                }

                when (characteristic.uuid) {
                    HEART_RATE_MEASUREMENT -> decodeHeartRate(value)
                    CYCLING_POWER_MEASUREMENT -> decodePower(value)
                    CSC_MEASUREMENT -> decodeCadence(value)
                    TEMPERATURE_MEASUREMENT_CHAR, INKBIRD_CHAR -> decodeTemperature(value)
                    HUMIDITY_MEASUREMENT_CHAR -> decodeHumidity(value)
                    BATTERY_LEVEL_CHAR -> storeBattery(type, value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing données $type", e)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onGattDisconnected(gatt: BluetoothGatt, type: SensorType, status: Int) {
        Log.w(TAG, "Déconnecté du capteur $type (status: $status)")
        gatt.close()
        synchronized(descriptorWriteQueue) { descriptorWriteQueue.removeAll { it.first === gatt } }

        // Une ancienne connexion qui se ferme ne doit pas toucher à l'état de la nouvelle
        if (activeGatts[type] !== gatt) {
            Log.d(TAG, "Déconnexion d'une ancienne connexion $type ignorée")
            return
        }
        activeGatts.remove(type)
        sensorValues.remove(type)
        batteryLevels.remove(type)
        firstNotification.clear()
        if (type == SensorType.POWER) clearPowerCache()
        if (type == SensorType.CADENCE) clearCadenceCache()

        scanEndedAt[type] = SystemClock.elapsedRealtime()                 // la prochaine tentative aura lieu après RESCAN_DELAY_MS
        if (AppConfig.isRecording && everConnected.contains(type)) {
            handler.post { sensorStatuses[type] = SensorConnectionStatus.LOST }
            Log.i(TAG, "Perte de connexion $type pendant la sortie : le superviseur réessaiera")
        } else {
            handler.post { sensorStatuses[type] = SensorConnectionStatus.DISCONNECTED }
        }
    }

    /** Service principal attendu pour ce type de capteur, ou null s'il est absent. */
    private fun findMainService(gatt: BluetoothGatt, type: SensorType): BluetoothGattService? {
        val serviceUuid = when (type) {
            SensorType.HEART_RATE -> HEART_RATE_SERVICE
            SensorType.POWER -> CYCLING_POWER_SERVICE
            SensorType.CADENCE -> CSC_SERVICE
            SensorType.ENVIRONMENT -> ENVIRONMENTAL_SENSING_SERVICE
        }
        return gatt.getService(serviceUuid) ?: gatt.services.firstOrNull {
            type == SensorType.ENVIRONMENT && (it.uuid == INKBIRD_SERVICE || it.uuid == ENVIRONMENTAL_SENSING_SERVICE)
        }
    }

    /**
     * L'appareil est connecté mais n'offre pas le service attendu (par exemple la pédale droite d'une
     * paire Assioma) : on l'écarte et on relance la recherche pour trouver le bon.
     */
    @SuppressLint("MissingPermission")
    private fun rejectDevice(gatt: BluetoothGatt, type: SensorType) {
        val address = gatt.device.address
        Log.w(TAG, "Service attendu absent sur $address ($type) : appareil écarté")
        rejectedAddresses.add(address)

        handler.post {
            if (activeGatts[type] === gatt) activeGatts.remove(type)
            gatt.disconnect()
            gatt.close()

            if (rejectedAddresses.size >= MAX_REJECTED_DEVICES) {
                Log.w(TAG, "Trop d'appareils écartés pour $type : recherche abandonnée")
                sensorStatuses[type] =
                    if (type == SensorType.ENVIRONMENT && envTemperature != null) SensorConnectionStatus.CONNECTED
                    else SensorConnectionStatus.DISCONNECTED
            } else {
                Log.d(TAG, "Nouvelle recherche pour $type")
                sensorStatuses[type] = SensorConnectionStatus.SCANNING
                startScan(type, userRequested = false)
            }
        }
    }

    // =====================================================================
    // 7. ABONNEMENTS AUX NOTIFICATIONS
    // =====================================================================

    private fun isNotifiable(c: BluetoothGattCharacteristic): Boolean =
        (c.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

    private fun subscribeToMeasurements(gatt: BluetoothGatt, type: SensorType, service: BluetoothGattService) {
        for (char in service.characteristics) {
            if (char.uuid in MEASUREMENT_CHARACTERISTICS && isNotifiable(char)) {
                Log.d(TAG, "Souscription $type : ${char.uuid}")
                enableNotifications(gatt, char)
            }
        }
    }

    private fun subscribeToBattery(gatt: BluetoothGatt) {
        val batteryChar = gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL_CHAR) ?: return
        if (isNotifiable(batteryChar)) enableNotifications(gatt, batteryChar)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        characteristic.getDescriptor(CONFIG_DESCRIPTOR)?.let { queueDescriptorWrite(gatt, it) }
    }

    private fun queueDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor) {
        synchronized(descriptorWriteQueue) {
            descriptorWriteQueue.add(Pair(gatt, descriptor))
            if (!isWritingDescriptor) {
                processNextDescriptorWrite()
            }
        }
    }

    /**
     * Envoie l'écriture suivante de la file. Une écriture refusée est sautée, une écriture sans réponse
     * est abandonnée après CCCD_TIMEOUT_MS : la file ne peut donc pas rester bloquée.
     */
    private fun processNextDescriptorWrite() {
        synchronized(descriptorWriteQueue) {
            while (true) {
                val next = descriptorWriteQueue.poll()
                if (next == null) {
                    isWritingDescriptor = false
                    readBatteryLevelIfAvailable()
                    return
                }
                val (gatt, descriptor) = next
                if (activeGatts.values.none { it === gatt }) {
                    Log.w(TAG, "Écriture CCCD ignorée : connexion déjà fermée")
                    continue
                }

                val value =
                    if ((descriptor.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

                val started = try {
                    writeCccd(gatt, descriptor, value)
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur writeDescriptor", e)
                    false
                }

                if (started) {
                    isWritingDescriptor = true
                    val token = ++writeToken
                    handler.postDelayed({
                        synchronized(descriptorWriteQueue) {
                            if (isWritingDescriptor && token == writeToken) {
                                Log.w(TAG, "Pas de réponse à l'écriture CCCD, on continue")
                                processNextDescriptorWrite()
                            }
                        }
                    }, CCCD_TIMEOUT_MS)
                    return
                }
                Log.w(TAG, "writeDescriptor refusé pour ${descriptor.characteristic.uuid}, on continue")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun writeCccd(gatt: BluetoothGatt, d: BluetoothGattDescriptor, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            gatt.writeDescriptor(d, value) == BluetoothStatusCodes.SUCCESS
        else {
            d.value = value
            gatt.writeDescriptor(d)
        }

    // =====================================================================
    // 8. DÉCODAGE DES MESURES
    // =====================================================================

    private fun decodeHeartRate(value: ByteArray) {
        val flag = value[0].toInt()
        val is16Bit = (flag and 0x01) != 0
        val hr = if (!is16Bit) value[1].toInt() and 0xFF
        else ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        sensorValues[SensorType.HEART_RATE] = hr
    }

    /**
     * Cycling Power Measurement : [drapeaux 2 octets][puissance 2 octets] puis, selon les drapeaux,
     * équilibre G/D (1 octet), couple accumulé (2), roue (6), manivelle (4 : tours + temps).
     */
    private fun decodePower(value: ByteArray) {
        if (value.size < 4) return
        val flags = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
        val powerVal = ((value[3].toInt() and 0xFF) shl 8) or (value[2].toInt() and 0xFF)
        sensorValues[SensorType.POWER] = powerVal
        lastPower = powerVal

        var offset = 4

        if ((flags and 0x01) != 0 && value.size > offset) {          // équilibre G/D
            lastPowerPercentRightLeft = value[offset].toInt() and 0xFF
            offset += 1
        }
        if ((flags and 0x04) != 0) offset += 2                        // couple accumulé : non utilisé, mais à sauter
        if ((flags and 0x10) != 0) offset += 6                        // données de roue : non utilisées

        if ((flags and 0x20) != 0 && value.size >= offset + 4) {      // données de manivelle -> cadence
            val crankRevs = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            val crankTime = ((value[offset + 3].toInt() and 0xFF) shl 8) or (value[offset + 2].toInt() and 0xFF)

            if (lastCrankRevs != -1 && lastCrankTime != -1) {
                val dCrank = (crankRevs - lastCrankRevs) and 0xFFFF
                val dTime = ((crankTime - lastCrankTime) and 0xFFFF) / 1024.0f
                if (dTime > 0) {
                    lastPowerCadence = (dCrank / dTime) * 60.0f
                }
            }
            lastCrankRevs = crankRevs
            lastCrankTime = crankTime
        }

        applyCachedSensorValuesToGpsBuffer()
    }

    private fun decodeTemperature(value: ByteArray) {
        envUpdatedAt = SystemClock.elapsedRealtime()
        if (value.size < 2) return
        val tempRaw = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
        val tempFloat = tempRaw / 100.0f
        if (tempFloat in -40.0f..60.0f) {
            envTemperature = tempFloat
            handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }
            applyCachedSensorValuesToGpsBuffer()
        }
    }

    private fun decodeHumidity(value: ByteArray) {
        envUpdatedAt = SystemClock.elapsedRealtime()
        if (value.size < 2) return
        val humRaw = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
        val humFloat = humRaw / 100.0f
        if (humFloat in 0.0f..100.0f) {
            envHumidity = humFloat
            handler.post { sensorStatuses[SensorType.ENVIRONMENT] = SensorConnectionStatus.CONNECTED }
            applyCachedSensorValuesToGpsBuffer()
        }
    }

    //plusieurs fonctions pour la cadence
    private fun u16(v: ByteArray, offset: Int): Int =
        ((v[offset + 1].toInt() and 0xFF) shl 8) or (v[offset].toInt() and 0xFF)

    /**
     * CSC Measurement : [drapeaux 1 octet] puis, selon les drapeaux,
     * roue (bit 0) : tours cumulés (4 octets) + heure du dernier événement (2 octets) ;
     * manivelle (bit 1) : tours cumulés (2 octets) + heure du dernier événement (2 octets, en 1/1024 s).
     */
    private fun decodeCadence(value: ByteArray) {
        if (value.isEmpty()) return
        val flags = value[0].toInt() and 0xFF
        var offset = 1
        if ((flags and 0x01) != 0) offset += 6                        // données de roue (vitesse) : non utilisées
        if ((flags and 0x02) == 0 || value.size < offset + 4) return  // pas de données de manivelle : pas de cadence

        val crankRevs = u16(value, offset)
        val crankTime = u16(value, offset + 2)
        val now = SystemClock.elapsedRealtime()

        if (lastCscCrankRevs == -1) {                                 // premier paquet : sert de référence
            lastCscCrankRevs = crankRevs
            lastCscCrankTime = crankTime
            lastCrankEventAt = now
            scheduleCadenceTimeout()
            return
        }

        val dTime = (crankTime - lastCscCrankTime) and 0xFFFF
        val dRevs = (crankRevs - lastCscCrankRevs) and 0xFFFF

        if (dTime == 0) {
            // Pas de nouveau tour depuis le paquet précédent
            if (now - lastCrankEventAt >= CADENCE_ZERO_AFTER_MS && lastCadence != 0) setCadence(0)
        } else {
            lastCscCrankRevs = crankRevs
            lastCscCrankTime = crankTime
            lastCrankEventAt = now
            val rpm = dRevs * 60f * 1024f / dTime
            if (rpm <= MAX_PLAUSIBLE_CADENCE) setCadence(rpm.roundToInt())
        }
        scheduleCadenceTimeout()
    }

    private fun setCadence(rpm: Int) {
        sensorValues[SensorType.CADENCE] = rpm        // lu par le getter public `cadence`
        lastCadence = rpm
        applyCachedSensorValuesToGpsBuffer()          // écrit dans sample.cadence
    }

    /** Relance le délai de remise à zéro : il se déclenche aussi quand le capteur n'envoie plus rien à l'arrêt. */
    private fun scheduleCadenceTimeout() {
        handler.removeCallbacks(cadenceTimeout)
        handler.postDelayed(cadenceTimeout, CADENCE_ZERO_AFTER_MS + 200)
    }

    private fun clearCadenceCache() {
        handler.removeCallbacks(cadenceTimeout)
        lastCadence = null
        lastCscCrankRevs = -1
        lastCscCrankTime = -1
        lastCrankEventAt = 0L
    }

    // =====================================================================
    // 9. BATTERIE
    // =====================================================================

    private fun storeBattery(type: SensorType, value: ByteArray) {
        if (value.isEmpty()) return
        batteryLevels[type] = value[0].toInt() and 0xFF
        Log.d(TAG, "Batterie $type : ${batteryLevels[type]} %")
        applyCachedSensorValuesToGpsBuffer()
    }

    /** Lit la batterie de chaque capteur connecté, puis programme un rafraîchissement périodique. */
    private fun readBatteryLevelIfAvailable() {
        handler.postDelayed({
            activeGatts.forEach { (type, gatt) -> readBattery(type, gatt) }
            handler.removeCallbacks(batteryPoll)
            handler.postDelayed(batteryPoll, BATTERY_POLL_MS)
        }, 500)
    }

    @SuppressLint("MissingPermission")
    private fun readBattery(type: SensorType, gatt: BluetoothGatt) {
        try {
            val batteryChar = gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL_CHAR) ?: return
            if ((batteryChar.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                if (!gatt.readCharacteristic(batteryChar)) {
                    Log.w(TAG, "Lecture de la batterie $type refusée (opération en cours ?)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur readBattery $type", e)
        }
    }

    // =====================================================================
    // 10. GPSBUFFER ET CACHES
    // =====================================================================

    /**
     * Copie les dernières valeurs connues dans le dernier échantillon du GpsBuffer.
     * À appeler aussi à la création de chaque échantillon GPS, pour qu'il porte dès le départ les valeurs capteur.
     */
    fun applyCachedSensorValuesToGpsBuffer() {
        GpsBuffer.getLast()?.let { sample ->
            lastPower?.let { sample.power = it }
            lastPowerPercentRightLeft?.let { sample.powerPercentRightLeft = it }
            lastPowerCadence?.let { sample.powerCadence = it }
            lastCadence?.let { sample.cadence = it }

            batteryLevels[SensorType.POWER]?.let { sample.powerBattery = it }
            batteryLevels[SensorType.HEART_RATE]?.let { sample.heartRateBattery = it }
            batteryLevels[SensorType.CADENCE]?.let { sample.cadenceBattery = it }
            batteryLevels[SensorType.ENVIRONMENT]?.let { sample.environmentBattery = it }

            // Ambiance : mesures récentes (avec la correction de température du réglage) ; périmées : on efface
            if (isEnvFresh()) {
                envTemperature?.let { sample.outTemperature = it + AppSettings.tempCorrection }
                envHumidity?.let { sample.outHumidity = it }
            } else if (envUpdatedAt != 0L) {
                sample.outTemperature = null
                sample.outHumidity = null
            }
        }
    }

    private fun clearPowerCache() {
        lastPower = null
        lastPowerCadence = null
        lastPowerPercentRightLeft = null
        lastCrankRevs = -1
        lastCrankTime = -1
    }
}