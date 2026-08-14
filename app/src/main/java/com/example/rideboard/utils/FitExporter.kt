package com.example.rideboard.utils

import android.content.Context
import android.util.Log
import java.io.File
import com.garmin.fit.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max


object FitExporter {

    fun fileTsvFile(
        context: Context,
        file: File,
        timeStampMs: Long,
        values: List<Double?>
    ) {

        try {
            //val file = File(context.filesDir, "ride.tsv")
            file.appendText("${timeStampMs}\t")
            for (value in values) {
                file.appendText("${value}\t")
            }
            file.appendText("\n")
        } catch (e: Exception) {
        }
    }

    fun export(context: Context, rideFile: File) {
        val outputDir = context.filesDir
        val fitOutputFile = File(outputDir, "ride.fit")
        val encode = FileEncoder(fitOutputFile, Fit.ProtocolVersion.V2_0)

        try {
            // 1. File ID (Obligatoire)
            val deviceInfoMesg = DeviceInfoMesg().apply {
                timestamp = DateTime(Date())
                deviceIndex = 0 // 0 représente l'appareil principal
                manufacturer = Manufacturer.GARMIN // Manufacturer.DEVELOPMENT // Ou votre ID officiel si vous en avez un
                product = 3570 // 1
                productName = "l'app de David Charlat" // Le nom de votre application
                serialNumber = 30178L // 12345L
                softwareVersion = 1.0f
            }
            encode.write(deviceInfoMesg)
            /*val fileIdMesg = FileIdMesg().apply {
                setType(com.garmin.fit.File.ACTIVITY)
                setManufacturer(Manufacturer.DEVELOPMENT)
                setProduct(1)
                setSerialNumber(12345L)
                setTimeCreated(DateTime(Date()))
            }
            encode.write(fileIdMesg)*/

            // Variables pour suivre les temps de début et de fin (nécessaires pour le résumé)
            var firstTimestamp: Long? = null
            var lastTimestamp: Long? = null
            var firstAltitude: Float? = null
            var lastAltitude: Float? = null
            var elevationGain: Float? = null
            var elapsedTime = 0F
            var totalDist = 0F


            // 2. Lecture et écriture des Records
            BufferedReader(FileReader(rideFile)).use { reader ->
                var line: String?
                // Décommenter si présence d'un header :
                // reader.readLine()

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split("\t")
                    if (tokens.size < 4) continue

                    try {
                        val timestampLong = tokens[0].toLong()
                        val latitude = tokens[1].toDouble()
                        val longitude = tokens[2].toDouble()
                        val altitude = tokens[3].toFloat()
                        val presentElevationGain = tokens[4].toFloat()
                        val presentElapsedTime = tokens[5].toFloat()
                        val presentSpeed = tokens[6].toFloat()
                        val presentTotalDistance = tokens[7].toFloat()
                        val presentVerticalSpeed = tokens[8].toFloat()

                        // Ajustement automatique MS vs Secondes
                        // Si le timestamp est plus petit que 1000000000000, il est probablement déjà en secondes.
                        val timeInMs =
                            if (timestampLong < 1000000000000L) timestampLong * 1000 else timestampLong

                        if (firstTimestamp == null) firstTimestamp = timeInMs
                        lastTimestamp = timeInMs
                        if (firstAltitude == null) firstAltitude = altitude
                        lastAltitude = altitude
                        elevationGain = presentElevationGain
                        elapsedTime = presentElapsedTime
                        totalDist = presentTotalDistance



                        val recordMesg = RecordMesg().apply {
                            timestamp = DateTime(Date(timeInMs))
                            positionLat = (latitude * (2147483648.0 / 180.0)).toInt()
                            positionLong = (longitude * (2147483648.0 / 180.0)).toInt()
                            enhancedAltitude = altitude
                            enhancedSpeed = presentSpeed
                            verticalSpeed = presentVerticalSpeed
                            distance = totalDist
                        }
                        encode.write(recordMesg)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 3. Écriture des messages de fin (Ce que Strava exige !)
            if (firstTimestamp != null && lastTimestamp != null) {
                val startingTime = DateTime(Date(firstTimestamp))
                val endTime = DateTime(Date(lastTimestamp))
                val totalDurationSeconds = ((lastTimestamp - firstTimestamp) / 1000.0).toFloat()

                // Message Lap (Le circuit / tour)
                val lapMesg = LapMesg().apply {
                    timestamp = endTime
                    startTime = startingTime
                    totalTimerTime = totalDurationSeconds
                    totalElapsedTime = elapsedTime
                    totalDistance = totalDist
                    sport = Sport.CYCLING
                    // AJOUTS :
                    totalAscent = (elevationGain?:0f).toInt() // En mètres
                    totalDescent = max(0,((elevationGain ?:0f) + (firstAltitude ?: 0f) - (lastAltitude ?: 0f)).toInt())
                }
                encode.write(lapMesg)
                /*val lapMesg = LapMesg().apply {
                    timestamp = endTime
                    startTime = startingTime
                    totalTimerTime = totalDurationSeconds
                    totalElapsedTime = totalDurationSeconds
                    sport = Sport.CYCLING // Ajoute explicitement le sport pour Strava
                }
                encode.write(lapMesg)*/

                // Message Session (La session globale)
                val sessionMesg = SessionMesg().apply {
                    timestamp = endTime
                    startTime = startingTime
                    totalTimerTime = totalDurationSeconds
                    totalElapsedTime = elapsedTime
                    totalDistance = totalDist
                    sport = Sport.CYCLING
                    subSport = SubSport.GENERIC
                    firstLapIndex = 0
                    numLaps = 1
                    // AJOUTS :
                    totalAscent = (elevationGain?:0f).toInt() // En mètres
                    totalDescent = max(0,((elevationGain ?:0f) + (firstAltitude ?: 0f) - (lastAltitude ?: 0f)).toInt())
                }
                encode.write(sessionMesg)

                // Message Activity (Le point final)
                val activityMesg = ActivityMesg().apply {
                    timestamp = endTime
                    numSessions = 1
                    type = Activity.MANUAL
                    totalTimerTime = totalDurationSeconds
                }
                encode.write(activityMesg)
            }

            // 4. Finaliser proprement
            encode.close()
            println("Fichier FIT créé avec succès et compatible Strava : ${fitOutputFile.absolutePath}")

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                encode.close()
            } catch (_: Exception) {
            }
        }
    }

    fun exportInGpx(context: Context, rideFile: File) {
        val outputDir = context.filesDir
        val gpxOutputFile = File(outputDir, "ride.gpx")

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            Log.i("GpxExport", "rideFile: ${rideFile.absolutePath}, exists=${rideFile.exists()}, length=${rideFile.length()}")
            val lineCount = rideFile.readLines().size
            Log.i("GpxExport", "Nombre de lignes lues: $lineCount, première ligne: ${rideFile.readLines().firstOrNull()}")

            BufferedWriter(FileWriter(gpxOutputFile)).use { writer ->

                var firstTimestamp: Long? = null

                // --- 1. En-tête GPX ---
                // Le "with Barometer" dans creator est ce qui indique à Strava
                // de garder l'altitude du fichier au lieu de la recalculer via GPS.
                writer.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="l'app de David Charlat with Barometer"
     xmlns="http://www.topografix.com/GPX/1/1"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
     xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
"""
                )

                // --- 2. Lecture du fichier source (même format que ton export FIT) ---
                val points = mutableListOf<String>()

                BufferedReader(FileReader(rideFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line!!.split("\t")
                        Log.i("GpxExport", "ligne lue: $line")
                        if (tokens.size < 4) continue

                        try {
                            val timestampLong = tokens[0].toLong()
                            val latitude = tokens[1].toDouble()
                            val longitude = tokens[2].toDouble()
                            val altitude = tokens[3].toFloat()
                            // tokens[4] = elevationGain, tokens[5] = elapsedTime,
                            // tokens[6] = speed, tokens[7] = totalDistance, tokens[8] = verticalSpeed
                            // -> pas repris ici : la vitesse/distance en GPX est dérivée
                            //    par Strava à partir du temps et des positions successives.

                            val timeInMs =
                                if (timestampLong < 1000000000000L) timestampLong * 1000 else timestampLong

                            if (firstTimestamp == null) firstTimestamp = timeInMs

                            val isoTime = dateFormatter.format(Date(timeInMs))

                            // Un <trkpt> par point. lat/lon en degrés décimaux (pas de conversion
                            // semicircles nécessaire, contrairement au FIT).
                            points.add(
                                """    <trkpt lat="$latitude" lon="$longitude">
      <ele>$altitude</ele>
      <time>$isoTime</time>
    </trkpt>
"""
                            )

                        } catch (e: Exception) {
                            Log.e("GpxExport", "Erreur parsing ligne: $line", e)
                        }
                    }
                }

                // --- 3. Métadonnées + track ---
                val metaTime = firstTimestamp?.let { dateFormatter.format(Date(it)) }
                    ?: dateFormatter.format(Date())

                writer.write(
                    """  <metadata>
    <time>$metaTime</time>
  </metadata>
  <trk>
    <name>Sortie vélo</name>
    <type>cycling</type>
    <trkseg>
"""
                )

                points.forEach { writer.write(it) }

                writer.write(
                    """    </trkseg>
  </trk>
</gpx>
"""
                )
            }

            println("Fichier GPX créé avec succès : ${gpxOutputFile.absolutePath}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportInTcx(context: Context, rideFile: File) {
        val outputDir = context.filesDir
        val tcxOutputFile = File(outputDir, "ride.tcx")

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            Log.i("TcxExport", "rideFile: ${rideFile.absolutePath}, exists=${rideFile.exists()}, length=${rideFile.length()}")
            val lineCount = rideFile.readLines().size
            Log.i("TcxExport", "Nombre de lignes lues: $lineCount, première ligne: ${rideFile.readLines().firstOrNull()}")

            BufferedWriter(FileWriter(tcxOutputFile)).use { writer ->

                var firstTimestamp: Long? = null
                var lastTimestamp: Long? = null
                var lastTotalDistance: Float = 0f

                val trackpoints = mutableListOf<String>()

                // --- 1. Lecture du fichier source (même format que tes autres exports) ---
                BufferedReader(FileReader(rideFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line!!.split("\t")
                        if (tokens.size < 4) continue

                        try {
                            val timestampLong = tokens[0].toLong()
                            val latitude = tokens[1].toDouble()
                            val longitude = tokens[2].toDouble()
                            val altitude = tokens[3].toFloat()
                            // tokens[4] = elevationGain, tokens[5] = elapsedTime,
                            // tokens[6] = speed, tokens[7] = totalDistance, tokens[8] = verticalSpeed
                            val totalDistance = tokens[7].toFloat()

                            val timeInMs =
                                if (timestampLong < 1000000000000L) timestampLong * 1000 else timestampLong

                            if (firstTimestamp == null) firstTimestamp = timeInMs
                            lastTimestamp = timeInMs
                            lastTotalDistance = totalDistance

                            val isoTime = dateFormatter.format(Date(timeInMs))

                            // DistanceMeters est un champ standard TCX par Trackpoint,
                            // contrairement au GPX où la distance doit être recalculée.
                            trackpoints.add(
                                """          <Trackpoint>
            <Time>$isoTime</Time>
            <Position>
              <LatitudeDegrees>$latitude</LatitudeDegrees>
              <LongitudeDegrees>$longitude</LongitudeDegrees>
            </Position>
            <AltitudeMeters>$altitude</AltitudeMeters>
            <DistanceMeters>$totalDistance</DistanceMeters>
          </Trackpoint>
"""
                            )

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (firstTimestamp == null || lastTimestamp == null) {
                    println("Aucun point valide, export TCX annulé.")
                    return
                }

                val startTimeIso = dateFormatter.format(Date(firstTimestamp!!))
                val totalTimeSeconds = (lastTimestamp!! - firstTimestamp!!) / 1000f

                // --- 2. Écriture du fichier TCX ---
                writer.write(
                    """<?xml version="1.0" encoding="UTF-8"?>
<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd">
  <Activities>
    <Activity Sport="Biking">
      <Id>$startTimeIso</Id>
      <Lap StartTime="$startTimeIso">
        <TotalTimeSeconds>$totalTimeSeconds</TotalTimeSeconds>
        <DistanceMeters>$lastTotalDistance</DistanceMeters>
        <Calories>0</Calories>
        <Intensity>Active</Intensity>
        <TriggerMethod>Manual</TriggerMethod>
        <Track>
"""
                )

                trackpoints.forEach { writer.write(it) }

                writer.write(
                    """        </Track>
      </Lap>
      <Creator xsi:type="Device_t">
        <Name>l'app de David Charlat with Barometer</Name>
        <UnitId>0</UnitId>
        <ProductID>0</ProductID>
        <Version>
          <VersionMajor>1</VersionMajor>
          <VersionMinor>0</VersionMinor>
          <BuildMajor>0</BuildMajor>
          <BuildMinor>0</BuildMinor>
        </Version>
      </Creator>
    </Activity>
  </Activities>
</TrainingCenterDatabase>
"""
                )
            }

            println("Fichier TCX créé avec succès : ${tcxOutputFile.absolutePath}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

    /*
    fun export(context: Context, rideFile: File) {
        // 1. Définir le fichier de sortie dans le dossier "files" de ton app
        val outputDir = context.filesDir // Pointe vers /data/data/com.example.RideBoard/files/
        val fitOutputFile = File(outputDir, "ride.fit")

        // 2. Initialiser l'encodeur Garmin FIT
        val encode = FileEncoder(fitOutputFile, Fit.ProtocolVersion.V2_0)

        try {
            // 3. Écrire le message d'en-tête obligatoire (File ID)
            val fileIdMesg = FileIdMesg().apply {
                setType(com.garmin.fit.File.ACTIVITY)
                setManufacturer(Manufacturer.DEVELOPMENT) // Tu pourras changer plus tard
                setProduct(1)
                setSerialNumber(12345L)
                setTimeCreated(DateTime(Date()))
            }
            encode.write(fileIdMesg)

            // Optionnel mais recommandé : Message d'initialisation de l'activité
            val activityMesg = ActivityMesg().apply {
                numSessions = 1
                type = Activity.MANUAL// Ou RUNNING, etc.
            }
            encode.write(activityMesg)

            // 4. Lire le fichier TSV et encoder les points
            BufferedReader(FileReader(rideFile)).use { reader ->
                var line: String?

                // Si ton fichier TSV a une ligne d'en-tête (header), décomment la ligne suivante :
                // reader.readLine()

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split("\t")
                    if (tokens.size < 4) continue // Sécurité si ligne incomplète

                    try {
                        // Supposons l'ordre : timestamp, latitude, longitude, altitude
                        val timestampLong = tokens[0].toLong() // Timestamp en millisecondes ou secondes
                        val latitude = tokens[1].toDouble()
                        val longitude = tokens[2].toDouble()
                        val altitude = tokens[3].toFloat()

                        // Création d'un point d'enregistrement (Record Message)
                        val recordMesg = RecordMesg()

                        // Le SDK Garmin attend le temps en secondes (convertir si c'est du ms)
                        // Et Garmin utilise une époque custom (1er Janvier 1989).
                        // La classe DateTime(Date(ms)) gère la conversion automatiquement.
                        recordMesg.timestamp = DateTime(Date(timestampLong))

                        // Attention : Garmin stocke la position en "Semicircles" et non en degrés !
                        // Formule : Semicircles = Degrés * (2^31 / 180)
                        val latSemi = (latitude * (2147483648.0 / 180.0)).toInt()
                        val lonSemi = (longitude * (2147483648.0 / 180.0)).toInt()

                        recordMesg.positionLat = latSemi
                        recordMesg.positionLong = lonSemi
                        recordMesg.altitude = altitude // En mètres

                        // Écriture du point dans le fichier
                        encode.write(recordMesg)

                    } catch (e: Exception) {
                        // Gestion d'une ligne mal formatée pour éviter de faire crash l'export
                        e.printStackTrace()
                    }
                }
            }

            // 5. Finaliser et fermer le fichier FIT
            encode.close()

            // Log ou notification de succès
            println("Fichier FIT créé avec succès : ${fitOutputFile.absolutePath}")

            //delete tsv file:
            //(File(context.filesDir, "ride.tsv")).writeText("")

        } catch (e: Exception) {
            e.printStackTrace()
            // Penser à fermer l'encodeur même en cas d'erreur si initialisé
            try { encode.close() } catch (_: Exception) {}
        }
    }

}*/