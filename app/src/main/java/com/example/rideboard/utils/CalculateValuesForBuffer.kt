package com.example.rideboard.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.example.rideboard.AppSettings
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import java.io.File
import java.lang.Double.isNaN
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

const val MIN_SPEED = 0.1
const val STEP_FOR_ELEVATION_GAIN = 0.1
const val SPEED_FOR_AVERAGE_POWER = 1.2
val POWER_ZONE_2 = AppSettings.powerZone2
val POWER_ZONE_3 = AppSettings.powerZone3
val POWER_ZONE_4 = AppSettings.powerZone4
val POWER_ZONE_5 = AppSettings.powerZone5
val HRZ2 = AppSettings.hrZone2
val HRZ3 = AppSettings.hrZone3
val HRZ4 = AppSettings.hrZone4
val HRZ5 = AppSettings.hrZone5
val IS_RINGING = AppSettings.isRinging


data class Coordinates(
    var x: Double,
    val y: Double
)

data class CoordinatesOrXNull(
    var x: Double?,
    var y: Double
)

data class SpatialCoordinates(
    var lon: Double,
    var lat: Double,
    var alt: Double?
)

data class AccuracyOfCoordinates(
    val coordinates: SpatialCoordinates,
    val accuracy: Double
)

    fun calculateValuesForBuffer(
        context: Context,
        buffer: GpsBuffer,
    )  {

        if (buffer.size < 3) return

        val sample = buffer.getNthBeforeLast(2)?:return
        val previousSample = buffer.getNthBeforeLast(3)?:return
        val bufferSnapshot = buffer.getAll()
        val newGpsPoint = bufferSnapshot[bufferSnapshot.size - 2]
        val previousGpsPoint = bufferSnapshot[bufferSnapshot.size - 3]
        val latestGpsPoint = bufferSnapshot[bufferSnapshot.size - 1]

        val newIsStarted = previousGpsPoint.gpsPointIsStarted
        val previousLatitude = previousGpsPoint.gpsPointLatitude
        val previousLongitude = previousGpsPoint.gpsPointLongitude
        val previousAltitude = previousGpsPoint.gpsPointAltitude
        val previousDirection = previousGpsPoint.gpsPointDirection
        val previousSpeed = previousGpsPoint.gpsPointSpeed
        val previousIsMoving = previousGpsPoint.gpsPointIsMoving
        val previousIsAltitudeGot = previousGpsPoint.gpsPointIsAltitudeGot
        val previousIsGpsAltitudeGot = previousGpsPoint.gpsPointIsGpsAltitudeGot
        val previousCumulatedGpsPrecision = previousGpsPoint.gpsPointCumulatedGpsPrecision
        val previousSpreadAltitudeAndAltitudeGps = previousGpsPoint.gpsPointSpreadAltitudeAndAltitudeGps
        val previousCoefficientOfSpreadAltitudeAndAltitudeGps = previousGpsPoint.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps
        val previousDurationAltitudeNotMnt = previousGpsPoint.gpsPointDurationAltitudeNotMnt
        val previousGpsAltitudeAccuracy = previousGpsPoint.gpsPointGpsAltitudeAccuracy
        val previousAltitudeSourceMntMnsOrGps = previousGpsPoint.gpsPointAltitudeSourceMntMnsOrGps
        val previousDurationHighSpreadAltitude = previousGpsPoint.gpsPointDurationHighSpreadAltitude
        val previousDisplayedAltitude = previousGpsPoint.gpsPointDisplayedAltitude
        val previousDisplayedVerticalSpeed = previousGpsPoint.gpsPointDisplayedVerticalSpeed
        val previousDisplayedVerticalSpeed2 = previousGpsPoint.gpsPointDisplayedVerticalSpeed2
        val previousDisplayedSpeed = previousGpsPoint.gpsPointDisplayedSpeed
        val previousUncorrectedGpsAltitude = previousGpsPoint.gpsPointUncorrectedGpsAltitude
        val previousTime = previousGpsPoint.timestamp
        val previousAcceleration = previousGpsPoint.gpsPointAcceleration
        val previousVerticalSpeed = previousGpsPoint.gpsPointVerticalSpeed
        val previousGpsPointLastDifferentStrm = previousGpsPoint.gpsPointLastDifferentSrtm
        val previousGpsPointLastDifferentLidarMnt = previousGpsPoint.gpsPointLastDifferentLidarMnt
        val previousGpsPointLastDifferentLidarMns = previousGpsPoint.gpsPointLastDifferentLidarMns
        val previousGpsPointLastDifferentIgn = previousGpsPoint.gpsPointLastDifferentIgn
        var previousGpsPointDurationTime = previousGpsPoint.gpsPointDurationTime
        var previousGpsPointTotalDistance = previousGpsPoint.gpsPointTotalDistance
        val previousGpsPointMaxSpeed = if (latestGpsPoint.gpsPointStringToReset == "maxSpeed" || newGpsPoint.gpsPointStringToReset == "maxSpeed") 0.0 else previousGpsPoint.gpsPointMaxSpeed
        val previousMinSlope = if (latestGpsPoint.gpsPointStringToReset == "minSlope" || newGpsPoint.gpsPointStringToReset == "minSlope") 0.0 else previousGpsPoint.gpsPointMinSlope
        val previousMaxSlope = if (latestGpsPoint.gpsPointStringToReset == "maxSlope" || newGpsPoint.gpsPointStringToReset == "maxSlope") 0.0 else previousGpsPoint.gpsPointMaxSlope
        val previousSlope = if (isNaN(previousGpsPoint.gpsPointSlope)) 0.0 else previousGpsPoint.gpsPointSlope
        val previousMaxAltitude = if (latestGpsPoint.gpsPointStringToReset == "maxAltitude" || newGpsPoint.gpsPointStringToReset == "maxAltitude") null else previousGpsPoint.gpsPointMaxAltitude
        val previousMinAltitude = if (latestGpsPoint.gpsPointStringToReset == "minAltitude" || newGpsPoint.gpsPointStringToReset == "minAltitude") null else previousGpsPoint.gpsPointMinAltitude
        val previousMaxVerticalSpeed = if (latestGpsPoint.gpsPointStringToReset == "maxVerticalSpeed" || newGpsPoint.gpsPointStringToReset == "maxVerticalSpeed") 0.0 else previousGpsPoint.gpsPointMaxVerticalSpeed
        val previousMinVerticalSpeed = if (latestGpsPoint.gpsPointStringToReset == "minVerticalSpeed" || newGpsPoint.gpsPointStringToReset == "minVerticalSpeed") 0.0 else previousGpsPoint.gpsPointMinVerticalSpeed
        val previousAltForElevationGain = previousGpsPoint.gpsPointAltForElevationGain
        var previousElevationGain = previousGpsPoint.gpsPointElevationGain
        var previousPartialElevationGain = if (latestGpsPoint.gpsPointStringToReset == "ElevationGain" || newGpsPoint.gpsPointStringToReset == "ElevationGain") 0.0
        else if (latestGpsPoint.gpsPointStringToReset == "ElevationGainRenewed" || newGpsPoint.gpsPointStringToReset == "ElevationGainRenewed") previousElevationGain
        else previousGpsPoint.partialElevationGain
        var previousPartialDurationTime = if (latestGpsPoint.gpsPointStringToReset == "Duration" || newGpsPoint.gpsPointStringToReset == "Duration") 0
        else if (latestGpsPoint.gpsPointStringToReset == "DurationRenewed" || newGpsPoint.gpsPointStringToReset == "DurationRenewed") previousGpsPointDurationTime
        else previousGpsPoint.partialDurationTime
        var previousPartialDistance = if (latestGpsPoint.gpsPointStringToReset == "Distance" || newGpsPoint.gpsPointStringToReset == "Distance") 0.0
        else if (latestGpsPoint.gpsPointStringToReset == "DistanceRenewed" || newGpsPoint.gpsPointStringToReset == "DistanceRenewed") previousGpsPointTotalDistance
        else previousGpsPoint.partialDistance
        var previousPartialDurationTimeForAverageSpeed = if (latestGpsPoint.gpsPointStringToReset == "averageSpeed" || newGpsPoint.gpsPointStringToReset == "averageSpeed") 0
        else if (latestGpsPoint.gpsPointStringToReset == "averageSpeedRenewed" || newGpsPoint.gpsPointStringToReset == "averageSpeedRenewed") previousPartialDurationTime
        else previousGpsPoint.partialDurationTimeForAverageSpeed
        var previousPartialDistanceForAverageSpeed = if (latestGpsPoint.gpsPointStringToReset == "averageSpeed" || newGpsPoint.gpsPointStringToReset == "averageSpeed") 0.0
        else if (latestGpsPoint.gpsPointStringToReset == "averageSpeedRenewed" || newGpsPoint.gpsPointStringToReset == "averageSpeedRenewed") previousPartialDistance
        else previousGpsPoint.partialDistanceForAverageSpeed
        val previousVerticalSpeed4 = if (latestGpsPoint.gpsPointStringToReset == "VerticalSpeed4" || newGpsPoint.gpsPointStringToReset == "VerticalSpeed4") 0.0 else previousGpsPoint.screenVerticalSpeed4
        val previousVerticalSpeed15 = if (latestGpsPoint.gpsPointStringToReset == "VerticalSpeed15" || newGpsPoint.gpsPointStringToReset == "VerticalSpeed15") 0.0 else previousGpsPoint.screenVerticalSpeed15
        val previousVerticalSpeed125 = if (latestGpsPoint.gpsPointStringToReset == "VerticalSpeed125" || newGpsPoint.gpsPointStringToReset == "VerticalSpeed125") 0.0 else previousGpsPoint.screenVerticalSpeed125
        val previousVerticalSpeed1000 = if (latestGpsPoint.gpsPointStringToReset == "VerticalSpeed1000" || newGpsPoint.gpsPointStringToReset == "VerticalSpeed1000") 0.0 else previousGpsPoint.screenVerticalSpeed1000
        val previousMaxVerticalSpeed15 = if (latestGpsPoint.gpsPointStringToReset == "maxVerticalSpeed15" || newGpsPoint.gpsPointStringToReset == "maxVerticalSpeed15") 0.0 else previousGpsPoint.maxVerticalSpeed15
        val previousMaxVerticalSpeed125 = if (latestGpsPoint.gpsPointStringToReset == "maxVerticalSpeed125" || newGpsPoint.gpsPointStringToReset == "maxVerticalSpeed125") 0.0 else previousGpsPoint.maxVerticalSpeed125
        val previousMaxVerticalSpeed1000 = if (latestGpsPoint.gpsPointStringToReset == "maxVerticalSpeed1000" || newGpsPoint.gpsPointStringToReset == "maxVerticalSpeed1000") 0.0 else previousGpsPoint.maxVerticalSpeed1000
        val previousDurationHRZ2 = if (latestGpsPoint.gpsPointStringToReset == "DurationHRZ2" || newGpsPoint.gpsPointStringToReset == "DurationHRZ2") 0 else previousGpsPoint.durationHRZ2
        val previousDurationHRZ3 = if (latestGpsPoint.gpsPointStringToReset == "DurationHRZ3" || newGpsPoint.gpsPointStringToReset == "DurationHRZ3") 0 else previousGpsPoint.durationHRZ3
        val previousDurationHRZ4 = if (latestGpsPoint.gpsPointStringToReset == "DurationHRZ4" || newGpsPoint.gpsPointStringToReset == "DurationHRZ4") 0 else previousGpsPoint.durationHRZ4
        val previousDurationHRZ5 = if (latestGpsPoint.gpsPointStringToReset == "DurationHRZ5" || newGpsPoint.gpsPointStringToReset == "DurationHRZ5") 0 else previousGpsPoint.durationHRZ5
        val previousDurationHR = if (latestGpsPoint.gpsPointStringToReset == "AvgHR" || newGpsPoint.gpsPointStringToReset == "AvgHR") 0 else previousGpsPoint.durationHR
        val previousMinHR = if (latestGpsPoint.gpsPointStringToReset == "MinHR" || newGpsPoint.gpsPointStringToReset == "MinHR") null else previousGpsPoint.minHR
        val previousMaxHR = if (latestGpsPoint.gpsPointStringToReset == "MaxHR" || newGpsPoint.gpsPointStringToReset == "MaxHR") null else previousGpsPoint.maxHR
        val previousTotHRForAvg = if (latestGpsPoint.gpsPointStringToReset == "AvgHR" || newGpsPoint.gpsPointStringToReset == "AvgHR") 0 else previousGpsPoint.totHRForAvg
        val previousMaxScreenPower4 = if (latestGpsPoint.gpsPointStringToReset == "MaxPower4" || newGpsPoint.gpsPointStringToReset == "MaxPower4") 0.0 else previousGpsPoint.maxScreenPower4
        val previousMaxScreenPower15 = if (latestGpsPoint.gpsPointStringToReset == "MaxPower15" || newGpsPoint.gpsPointStringToReset == "MaxPower15") 0.0 else previousGpsPoint.maxScreenPower15
        val previousMaxScreenPower125 = if (latestGpsPoint.gpsPointStringToReset == "MaxPower125" || newGpsPoint.gpsPointStringToReset == "MaxPower125") 0.0 else previousGpsPoint.maxScreenPower125
        val previousMaxScreenPower1000 = if (latestGpsPoint.gpsPointStringToReset == "MaxPower1000" || newGpsPoint.gpsPointStringToReset == "MaxPower1000") 0.0 else previousGpsPoint.maxScreenPower1000
        val previousScreenPower4 =  previousGpsPoint.screenPower4
        val previousScreenPower15 =  previousGpsPoint.screenPower15
        val previousScreenPower125 =  previousGpsPoint.screenPower125
        val previousScreenPower1000 =  previousGpsPoint.screenPower1000
        val previousTotalPowerForAveragePower =  if (latestGpsPoint.gpsPointStringToReset == "AvgPower" || newGpsPoint.gpsPointStringToReset == "AvgPower") 0 else previousGpsPoint.totalPowerForAveragePower
        val previousDurationForAveragePower =  if (latestGpsPoint.gpsPointStringToReset == "AvgPower" || newGpsPoint.gpsPointStringToReset == "AvgPower") 0 else previousGpsPoint.durationForAveragePower
        val previousDurationPowerZ2 = if (latestGpsPoint.gpsPointStringToReset == "DurationPowerZ2" || newGpsPoint.gpsPointStringToReset == "DurationPowerZ2") 0 else previousGpsPoint.durationPowerZ2
        val previousDurationPowerZ3 = if (latestGpsPoint.gpsPointStringToReset == "DurationPowerZ3" || newGpsPoint.gpsPointStringToReset == "DurationPowerZ3") 0 else previousGpsPoint.durationPowerZ3
        val previousDurationPowerZ4 = if (latestGpsPoint.gpsPointStringToReset == "DurationPowerZ4" || newGpsPoint.gpsPointStringToReset == "DurationPowerZ4") 0 else previousGpsPoint.durationPowerZ4
        val previousDurationPowerZ5 = if (latestGpsPoint.gpsPointStringToReset == "DurationPowerZ5" || newGpsPoint.gpsPointStringToReset == "DurationPowerZ5") 0 else previousGpsPoint.durationPowerZ5

        if (buffer.size == 3 || (previousGpsPointDurationTime < 100 && previousGpsPointTotalDistance == 0.0)) {
            val rideFile = File(context.filesDir, "ride.tsv")
            if (rideFile.exists()) {
                try {
                    val lines = rideFile.readLines()
                    if (lines.size > 2) {
                        val lastLine = lines.last()
                        val tokens = lastLine.split("\t")
                        if (tokens.size >= 8) {
                            previousElevationGain = tokens[4].toDoubleOrNull() ?: previousElevationGain
                            previousGpsPointDurationTime = tokens[5].toDoubleOrNull()?.let { (it * 1000.0).toLong() } ?: previousGpsPointDurationTime
                            previousGpsPointTotalDistance = tokens[7].toDoubleOrNull() ?: previousGpsPointTotalDistance
                            previousPartialElevationGain = previousElevationGain
                            previousPartialDurationTime = previousGpsPointDurationTime
                            previousPartialDistance = previousGpsPointTotalDistance
                            previousPartialDurationTimeForAverageSpeed = previousGpsPointDurationTime
                            previousPartialDistanceForAverageSpeed = previousGpsPointTotalDistance
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        val previousGpsPointScreenValueString = previousGpsPoint.gpsPointScreenValueString?:"      "
        val actualPower =if (newGpsPoint.power != null || previousGpsPoint.power != null || latestGpsPoint.power != null) {
            ((newGpsPoint.power?:0) + (latestGpsPoint.power?:0) + (previousGpsPoint.power?:0))/((if(newGpsPoint.power != null) 1 else 0) + (if(latestGpsPoint.power != null) 1 else 0) + (if(previousGpsPoint.power != null) 1 else 0))
        } else null
        val newCorrectedHeartRate = calculateCorrectedHeartRate(latestGpsPoint, newGpsPoint, previousGpsPoint, bufferSnapshot[max(0,bufferSnapshot.size - 4)])

        val deltaTimeInSecond = if (previousIsMoving)  (newGpsPoint.timestamp - previousTime).toDouble() / 1000.0
            else if (bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointIsMoving) (newGpsPoint.timestamp - bufferSnapshot[max(0,bufferSnapshot.size - 4)].timestamp).toDouble() / 1000.0
            else if (bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointIsMoving) (newGpsPoint.timestamp - bufferSnapshot[max(0,bufferSnapshot.size - 5)].timestamp).toDouble() / 1000.0
            //else if (bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointIsMoving) (newGpsPoint.timestamp - bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp).toDouble() / 1000.0
            else (newGpsPoint.timestamp - bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp).toDouble() / 1000.0 //7 si if -6

        val durationTimeIntInSec = min(4,(deltaTimeInSecond + 0.5).toLong())
        val newCumulatedGpsPrecision = calculateNewCumulatedGpsPrecision (newGpsPoint, previousCumulatedGpsPrecision)

        if (!newIsStarted && newCumulatedGpsPrecision >1) {
            // startNewScreenValues (bufferSnapshot)
            val pt1 = previousGpsPoint

            val newAltitude = simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot) ?: (simplifySampleAltitudeMnt(pt1, bufferSnapshot) ?: calculateAltitudeGps(pt1,
                newGpsPoint,
                latestGpsPoint ?: newGpsPoint, previousIsAltitudeGot, previousSpreadAltitudeAndAltitudeGps, previousUncorrectedGpsAltitude))
            val newSpread = if (newGpsPoint.altitudeGps != null && newAltitude != null)
                newGpsPoint.altitudeGps!! - newAltitude else null
            sample.gpsPointDirection = calculateDirectionBetweenTwoPoints(pt1.latitude, pt1.longitude,
                newGpsPoint.latitude,
                newGpsPoint.longitude)
            sample.gpsPointAltitude = newAltitude
            sample.gpsPointLatitude = newGpsPoint.latitude
            sample.gpsPointLongitude = newGpsPoint.longitude
            sample.gpsPointGpsAltitudeAccuracy = min((newGpsPoint.altitudeAccuracy ?: 299.9f).toDouble(), (pt1.altitudeAccuracy ?: 299.9f).toDouble())
            sample.gpsPointAltitudeSourceMntMnsOrGps = if (newSpread != null) "MNT" else if (newAltitude != 0.0) "GPS" else ""
            sample.gpsPointSpreadAltitudeAndAltitudeGps = newSpread
            sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = if (newSpread!= null && newGpsPoint.altitudeAccuracy != null && newGpsPoint.altitudeAccuracy!!.toDouble() != 0.0)
                1.0/(((newGpsPoint.altitudeAccuracy)?: 999.9f)).toDouble() else 0.0
            sample.gpsPointDurationAltitudeNotMnt = if (newSpread != null) 0 else 1
            //sample.gpsPointIsMoving: Boolean = false,
            sample.gpsPointIsStarted = true
            sample.gpsPointCumulatedGpsPrecision = 1.0
            sample.gpsPointIsAltitudeGot = (newSpread?:0.0) != 0.0
            sample.gpsPointIsGpsAltitudeGot = newSpread != null
            sample.gpsPointLastDifferentSrtm  = SpatialCoordinates (
                lon = roundToNearestStep(pt1.gpsPointLastDifferentSrtm?.lon ?:0.0, 5000),
                lat = roundToNearestStep(pt1.gpsPointLastDifferentSrtm?.lat?:0.0, 5000),
                alt = pt1.altitudeSrtm
            )
            sample.gpsPointLastDifferentLidarMnt  = SpatialCoordinates (
                lon = roundToNearestStep(pt1.gpsPointLastDifferentLidarMnt?.lon ?:0.0, 40000),
                lat = roundToNearestStep(pt1.gpsPointLastDifferentLidarMnt?.lat?:0.0, 40000),
                alt = pt1.altitudeLidarHdMnt
            )
            sample.gpsPointLastDifferentLidarMns = SpatialCoordinates (
                lon = roundToNearestStep(pt1.gpsPointLastDifferentLidarMns?.lon ?:0.0, 40000),
                lat = roundToNearestStep(pt1.gpsPointLastDifferentLidarMns?.lat?:0.0,40000),
                alt = pt1.altitudeLidarHdMns
            )
            sample.gpsPointLastDifferentIgn = SpatialCoordinates (
                lon = roundToNearestStep(pt1.gpsPointLastDifferentIgn?.lon ?:0.0, 20000),
                lat = roundToNearestStep(pt1.gpsPointLastDifferentIgn?.lat?:0.0,20000),
                alt = pt1.altitudeIgn
            )
            sample.gpsPointAltForElevationGain = newAltitude
            sample.gpsPointMaxAltitude = (previousMaxAltitude?: newAltitude)?:previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointMinAltitude = (previousMaxAltitude?: newAltitude)?: previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointDisplayedAltitude = (previousDisplayedAltitude?: newAltitude)?: previousGpsPoint.altitudeLidarHdMnt
            sample.correctedHeartRate = newCorrectedHeartRate

            return
        }
        if (!newIsStarted) //return screenValues when gpspoint is not started
        {

            val newAltitude = simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot) ?: (simplifySampleAltitudeMnt(
                previousGpsPoint, bufferSnapshot) ?: calculateAltitudeGps(
                previousGpsPoint,
                newGpsPoint,
                latestGpsPoint ?: newGpsPoint, previousIsAltitudeGot, previousSpreadAltitudeAndAltitudeGps, previousUncorrectedGpsAltitude))

            sample.gpsPointIsStarted = false
            sample.gpsPointCumulatedGpsPrecision = calculateNewCumulatedGpsPrecision(newGpsPoint,previousCumulatedGpsPrecision)
            sample.gpsPointLatitude = newGpsPoint.latitude
            sample.gpsPointLongitude = newGpsPoint.longitude
            sample.gpsPointAltitude = newAltitude
            return
        }

        if (!AppConfig.isRecording) {
            sample.gpsPointSpeed = 0.0
            sample.gpsPointAcceleration = 0.0
            sample.gpsPointVerticalSpeed = 0.0
            sample.gpsPointDirection = previousDirection
            sample.gpsPointAltitude = (previousAltitude?: previousGpsPoint.altitudeLidarHdMnt)
            sample.gpsPointLatitude = previousLatitude
            sample.gpsPointLongitude = previousLongitude
            sample.gpsPointGpsAltitudeAccuracy = previousGpsAltitudeAccuracy
            sample.gpsPointAltitudeSourceMntMnsOrGps = previousAltitudeSourceMntMnsOrGps
            sample.gpsPointSpreadAltitudeAndAltitudeGps = previousSpreadAltitudeAndAltitudeGps
            sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = previousCoefficientOfSpreadAltitudeAndAltitudeGps
            sample.gpsPointDurationAltitudeNotMnt = previousDurationAltitudeNotMnt
            sample.gpsPointIsMoving = false
            sample.gpsPointIsStarted = previousGpsPoint.gpsPointIsStarted
            sample.gpsPointCumulatedGpsPrecision  = previousGpsPoint.gpsPointCumulatedGpsPrecision
            sample.gpsPointIsAltitudeGot = previousIsAltitudeGot
            sample.gpsPointIsGpsAltitudeGot = previousIsGpsAltitudeGot || newGpsPoint.altitudeGps != null
            sample.gpsPointUncorrectedGpsAltitude = previousUncorrectedGpsAltitude
            sample.gpsPointDisplayedVerticalSpeed = 0.0
            sample.gpsPointDisplayedVerticalSpeed2 = 0.0
            sample.gpsPointDisplayedAltitude = previousDisplayedAltitude?: previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointDisplayedSpeed = 0.0
            sample.gpsPointDurationHighSpreadAltitude = previousDurationHighSpreadAltitude
            sample.gpsPointLastDifferentSrtm = previousGpsPoint.gpsPointLastDifferentSrtm
            sample.gpsPointLastDifferentLidarMnt = previousGpsPoint.gpsPointLastDifferentLidarMnt
            sample.gpsPointLastDifferentLidarMns = previousGpsPoint.gpsPointLastDifferentLidarMns
            sample.gpsPointLastDifferentIgn =previousGpsPoint.gpsPointLastDifferentIgn
            sample.gpsPointDurationTime = previousGpsPointDurationTime
            sample.gpsPointTotalDistance = previousGpsPointTotalDistance
            sample.gpsPointMaxSpeed = previousGpsPointMaxSpeed
            sample.gpsPointMinSlope = previousMinSlope
            sample.gpsPointMaxSlope = previousMaxSlope
            sample.gpsPointSlope = previousSlope
            sample.gpsPointMaxAltitude = previousMaxAltitude?: previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointMinAltitude = previousMinAltitude?: previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointAltForElevationGain = previousAltForElevationGain
            sample.gpsPointElevationGain = previousElevationGain
            sample.gpsPointMinVerticalSpeed = previousMinVerticalSpeed
            sample.gpsPointMaxVerticalSpeed = previousMaxVerticalSpeed
            sample.gpsPointScreenValueString = previousGpsPointScreenValueString
            sample.partialDistance = previousPartialDistance
            sample.partialDurationTime = previousPartialDurationTime
            sample.partialDistanceForAverageSpeed = previousPartialDistanceForAverageSpeed
            sample.partialDurationTimeForAverageSpeed = previousPartialDurationTimeForAverageSpeed
            sample.partialElevationGain = previousPartialElevationGain
            sample.screenVerticalSpeed4 = previousVerticalSpeed4 * (3.0/4.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed15 = previousVerticalSpeed15 * (14.0/15.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed125 = previousVerticalSpeed125 * (124.0/125.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed1000 = previousVerticalSpeed1000 * (999.0/1000.0).pow(deltaTimeInSecond)
            sample.maxVerticalSpeed15 = previousMaxVerticalSpeed15
            sample.maxVerticalSpeed125 = previousMaxVerticalSpeed125
            sample.maxVerticalSpeed1000 = previousMaxVerticalSpeed1000
            sample.durationHRZ2 = previousDurationHRZ2
            sample.durationHRZ3 = previousDurationHRZ3
            sample.durationHRZ4 = previousDurationHRZ4
            sample.durationHRZ5 = previousDurationHRZ5
            sample.durationHR = previousDurationHR
            sample.minHR = previousMinHR
            sample.maxHR = previousMaxHR
            sample.totHRForAvg = previousTotHRForAvg
            sample.correctedHeartRate = newCorrectedHeartRate
            sample.maxScreenPower4 = previousMaxScreenPower4
            sample.maxScreenPower15 = previousMaxScreenPower15
            sample.maxScreenPower125 = previousMaxScreenPower125
            sample.maxScreenPower1000 = previousMaxScreenPower1000
            sample.screenPower4 = previousScreenPower4 * (3.0/4.0).pow(deltaTimeInSecond)
            sample.screenPower15 = previousScreenPower15 * (14.0/15.0).pow(deltaTimeInSecond)
            sample.screenPower125 = previousScreenPower125 * (124.0/125.0).pow(deltaTimeInSecond)
            sample.screenPower1000 = previousScreenPower1000 * (999.0/1000.0).pow(deltaTimeInSecond)
            sample.totalPowerForAveragePower = previousTotalPowerForAveragePower
            sample.durationForAveragePower = previousDurationForAveragePower
            sample.durationPowerZ2 = previousDurationPowerZ2
            sample.durationPowerZ3 = previousDurationPowerZ3
            sample.durationPowerZ4 = previousDurationPowerZ4
            sample.durationPowerZ5 = previousDurationPowerZ5

            return
        }

        val newGpsPointLastDifferentSrtm = run {
            val roundedLat = roundToNearestStep(sample.latitude, 5000)
            val roundedLon = roundToNearestStep(sample.longitude, 5000)
            if (previousGpsPointLastDifferentStrm?.alt == null) (
                    if (sample.altitudeSrtm != null) SpatialCoordinates(
                        lat = roundedLat,
                        lon = roundedLon,
                        alt = sample.altitudeSrtm
                    )
                    else SpatialCoordinates(
                        lat = roundToNearestStep(previousSample.latitude, 5000),
                        lon = roundToNearestStep(previousSample.longitude, 5000),
                        alt = previousSample.altitudeSrtm
                    )
                    )
            else if ((roundedLat == previousGpsPointLastDifferentStrm.lat && roundedLon == previousGpsPointLastDifferentStrm.lon) || (sample.altitudeSrtm == null && previousSample.altitudeSrtm == null)) previousGpsPointLastDifferentStrm
            else if (sample.altitudeSrtm != null) SpatialCoordinates(
                lat = roundedLat,
                lon = roundedLon,
                alt = sample.altitudeSrtm
            )
            else SpatialCoordinates(
                lat = roundToNearestStep(previousSample.latitude, 5000),
                lon = roundToNearestStep(previousSample.longitude, 5000),
                alt = previousSample.altitudeSrtm
            )
        }
        val newGpsPointLastDifferentLidarMnt = run {
            val roundedLat = roundToNearestStep(sample.latitude, 40000)
            val roundedLon = roundToNearestStep(sample.longitude, 40000)
            if (previousGpsPointLastDifferentLidarMnt?.alt == null) (
                    if (sample.altitudeLidarHdMnt != null) SpatialCoordinates(
                        lat = roundedLat,
                        lon = roundedLon,
                        alt = sample.altitudeLidarHdMnt
                    )
                    else SpatialCoordinates(
                        lat = roundToNearestStep(previousSample.latitude, 40000),
                        lon = roundToNearestStep(previousSample.longitude, 40000),
                        alt = previousSample.altitudeLidarHdMnt
                    )
                    )
            else if ((roundedLat == previousGpsPointLastDifferentLidarMnt.lat && roundedLon == previousGpsPointLastDifferentLidarMnt.lon) || (sample.altitudeLidarHdMnt == null && previousSample.altitudeLidarHdMnt == null)) previousGpsPointLastDifferentLidarMnt
            else if (sample.altitudeLidarHdMnt != null) SpatialCoordinates(
                lat = roundedLat,
                lon = roundedLon,
                alt = sample.altitudeLidarHdMnt
            )
            else SpatialCoordinates(
                lat = roundToNearestStep(previousSample.latitude, 40000),
                lon = roundToNearestStep(previousSample.longitude, 40000),
                alt = previousSample.altitudeLidarHdMnt
            )
        }
        val newGpsPointLastDifferentLidarMns = run {
            val roundedLat = roundToNearestStep(sample.latitude, 40000)
            val roundedLon = roundToNearestStep(sample.longitude, 40000)
            if (previousGpsPointLastDifferentLidarMns?.alt == null) (
                    if (sample.altitudeLidarHdMns != null) SpatialCoordinates(
                        lat = roundedLat,
                        lon = roundedLon,
                        alt = sample.altitudeLidarHdMns
                    )
                    else SpatialCoordinates(
                        lat = roundToNearestStep(previousSample.latitude, 40000),
                        lon = roundToNearestStep(previousSample.longitude, 40000),
                        alt = previousSample.altitudeLidarHdMns
                    )
                    )
            else if ((roundedLat == previousGpsPointLastDifferentLidarMns.lat && roundedLon == previousGpsPointLastDifferentLidarMns.lon) || (sample.altitudeLidarHdMns == null && previousSample.altitudeLidarHdMns == null)) previousGpsPointLastDifferentLidarMns
            else if (sample.altitudeLidarHdMns != null) SpatialCoordinates(
                lat = roundedLat,
                lon = roundedLon,
                alt = sample.altitudeLidarHdMns
            )
            else SpatialCoordinates(
                lat = roundToNearestStep(previousSample.latitude, 40000),
                lon = roundToNearestStep(previousSample.longitude, 40000),
                alt = previousSample.altitudeLidarHdMns
            )
        }
        val newGpsPointLastDifferentIgn = run {
            val roundedLat = roundToNearestStep(sample.latitude, 20000)
            val roundedLon = roundToNearestStep(sample.longitude, 20000)
            if (previousGpsPointLastDifferentIgn?.alt == null) (
                    if (sample.altitudeIgn != null) SpatialCoordinates(
                        lat = roundedLat,
                        lon = roundedLon,
                        alt = sample.altitudeIgn
                    )
                    else SpatialCoordinates(
                        lat = roundToNearestStep(previousSample.latitude, 20000),
                        lon = roundToNearestStep(previousSample.longitude, 20000),
                        alt = previousSample.altitudeIgn
                    )
                    )
            else if ((roundedLat == previousGpsPointLastDifferentIgn.lat && roundedLon == previousGpsPointLastDifferentIgn.lon) || (sample.altitudeIgn == null && previousSample.altitudeIgn == null)) previousGpsPointLastDifferentLidarMnt
            else if (sample.altitudeIgn != null) SpatialCoordinates(
                lat = roundedLat,
                lon = roundedLon,
                alt = sample.altitudeIgn
            )
            else SpatialCoordinates(
                lat = roundToNearestStep(previousSample.latitude, 20000),
                lon = roundToNearestStep(previousSample.longitude, 20000),
                alt = previousSample.altitudeIgn
            )
        }

        val foundLastAltitudeLidarMnt = findLastAltitudeMnt(bufferSnapshot, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed)
        val foundLastAltitudeLidarMns = findLastAltitudeMns(bufferSnapshot, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed)
        val foundLastAltitudeIgn = findLastAltitudeIgn(bufferSnapshot, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed)
        val foundLastAltitudeSrtm = findLastAltitudeSrtm(bufferSnapshot, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed)
        // val altitudeMntNewGpsPt = simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot) est remplacée par le suivant:
        val altitudeMntNewGpsPtAndAccuracy = calculateCorrectedAltitudeMnt(
            foundLastAltitudeLidarMnt, foundLastAltitudeIgn, foundLastAltitudeSrtm,
            bufferSnapshot,
            newGpsPointLastDifferentLidarMnt, newGpsPointLastDifferentIgn, newGpsPointLastDifferentSrtm)
        val altitudeMntNewGpsPt = altitudeMntNewGpsPtAndAccuracy.x
        val altitudeMnsNewGpsPt = calculateCorrectedAltitudeMns(foundLastAltitudeLidarMns, foundLastAltitudeIgn, foundLastAltitudeSrtm,
            bufferSnapshot,
            newGpsPointLastDifferentLidarMns, newGpsPointLastDifferentIgn, newGpsPointLastDifferentSrtm).x
        //val time2 = System.currentTimeMillis()

        val altitudeMntMnsAccuracy = altitudeMntNewGpsPtAndAccuracy.y

        //val time3 = System.currentTimeMillis()

        val altitudeGpsNewGpsPt = calculateAltitudeGps(previousGpsPoint, newGpsPoint, latestGpsPoint, previousIsAltitudeGot, previousSpreadAltitudeAndAltitudeGps, previousUncorrectedGpsAltitude)
        val altitudeGpsAccuracy = calculateGpsAltitudeAccuracy(newGpsPoint,previousGpsAltitudeAccuracy)


        if (!previousIsMoving
            && (calculateDistanceBetweenTwoGpsPoints(previousLatitude?:newGpsPoint.latitude, previousLongitude?:newGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude)
                    < 3.0 * newGpsPoint.accuracy)
        ) {        //return screenValuesWhenIsMovingIsWrong (previousValues, newGpsPoint, bufferSnapshot)
            sample.gpsPointSpeed = 0.0
            sample.gpsPointAcceleration = 0.0
            sample.gpsPointVerticalSpeed = 0.0
            sample.gpsPointDirection = previousDirection
            sample.gpsPointAltitude = previousAltitude?: altitudeMntNewGpsPt
            sample.gpsPointLatitude = previousLatitude
            sample.gpsPointLongitude = previousLongitude
            sample.gpsPointGpsAltitudeAccuracy  = calculateGpsAltitudeAccuracy(newGpsPoint, previousGpsAltitudeAccuracy)
            sample.gpsPointAltitudeSourceMntMnsOrGps = previousAltitudeSourceMntMnsOrGps
            sample.gpsPointSpreadAltitudeAndAltitudeGps = previousSpreadAltitudeAndAltitudeGps
            sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = previousCoefficientOfSpreadAltitudeAndAltitudeGps
            sample.gpsPointDurationAltitudeNotMnt = previousDurationAltitudeNotMnt
            sample.gpsPointIsMoving = false
            sample.gpsPointIsStarted = true
            sample.gpsPointCumulatedGpsPrecision  = calculateNewCumulatedGpsPrecision(newGpsPoint, previousCumulatedGpsPrecision)
            sample.gpsPointIsAltitudeGot = previousIsAltitudeGot || simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot) != null
            sample.gpsPointIsGpsAltitudeGot = previousIsGpsAltitudeGot || newGpsPoint.altitudeGps != null
            sample.gpsPointUncorrectedGpsAltitude = previousUncorrectedGpsAltitude
            sample.gpsPointDisplayedVerticalSpeed = 0.0
            sample.gpsPointDisplayedVerticalSpeed2 = 0.0
            sample.gpsPointDisplayedAltitude = (previousDisplayedAltitude?: altitudeMntNewGpsPt)?: previousGpsPoint.altitudeLidarHdMnt
            sample.gpsPointDisplayedSpeed = 0.0
            sample.gpsPointDurationHighSpreadAltitude = previousDurationHighSpreadAltitude
            sample.gpsPointLastDifferentSrtm = newGpsPointLastDifferentSrtm
            sample.gpsPointLastDifferentLidarMnt = newGpsPointLastDifferentLidarMnt
            sample.gpsPointLastDifferentLidarMns = newGpsPointLastDifferentLidarMns
            sample.gpsPointLastDifferentIgn = newGpsPointLastDifferentIgn
            sample.gpsPointDurationTime = previousGpsPointDurationTime
            sample.gpsPointTotalDistance = previousGpsPointTotalDistance
            sample.gpsPointMaxSpeed = previousGpsPointMaxSpeed
            sample.gpsPointMinSlope = previousMinSlope
            sample.gpsPointMaxSlope = previousMaxSlope
            sample.gpsPointSlope = previousSlope
            sample.gpsPointMaxAltitude = previousMaxAltitude
            sample.gpsPointMinAltitude = previousMinAltitude
            sample.gpsPointAltForElevationGain = previousAltForElevationGain
            sample.gpsPointElevationGain = previousElevationGain
            sample.gpsPointMinVerticalSpeed = previousMinVerticalSpeed
            sample.gpsPointMaxVerticalSpeed = previousMaxVerticalSpeed
            sample.gpsPointScreenValueString = previousGpsPointScreenValueString
            sample.partialDistance = previousPartialDistance
            sample.partialDurationTime = previousPartialDurationTime
            sample.partialDistanceForAverageSpeed = previousPartialDistanceForAverageSpeed
            sample.partialDurationTimeForAverageSpeed = previousPartialDurationTimeForAverageSpeed
            sample.partialElevationGain = previousPartialElevationGain
            sample.screenVerticalSpeed4 = previousVerticalSpeed4 * (3.0/4.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed15 = previousVerticalSpeed15 * (14.0/15.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed125 = previousVerticalSpeed125 * (124.0/125.0).pow(deltaTimeInSecond)
            sample.screenVerticalSpeed1000 = previousVerticalSpeed1000 * (999.0/1000.0).pow(deltaTimeInSecond)
            sample.maxVerticalSpeed15 = previousMaxVerticalSpeed15
            sample.maxVerticalSpeed125 = previousMaxVerticalSpeed125
            sample.maxVerticalSpeed1000 = previousMaxVerticalSpeed1000
            sample.durationHRZ2 = if ((newCorrectedHeartRate?:0) in HRZ2..<HRZ3) previousDurationHRZ2 + durationTimeIntInSec else previousDurationHRZ2
            sample.durationHRZ3 = if ((newCorrectedHeartRate?:0) in HRZ3..<HRZ4) previousDurationHRZ3 + durationTimeIntInSec else previousDurationHRZ3
            sample.durationHRZ4 = if ((newCorrectedHeartRate?:0) in HRZ4..<HRZ5) previousDurationHRZ4 + durationTimeIntInSec else previousDurationHRZ4
            sample.durationHRZ5 = if ((newCorrectedHeartRate?:0) >= HRZ5) previousDurationHRZ5 + durationTimeIntInSec else previousDurationHRZ5
            sample.durationHR = if ((newCorrectedHeartRate?:0) > 0) previousDurationHR + durationTimeIntInSec else previousDurationHR
            sample.minHR = if ((newCorrectedHeartRate?:0) > 0 && previousMinHR != null) min(newCorrectedHeartRate!!, previousMinHR) else if(previousMinHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMinHR
            sample.maxHR = if ((newCorrectedHeartRate?:0) > 0 && previousMaxHR != null) max(newCorrectedHeartRate!!, previousMaxHR) else if(previousMaxHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMaxHR
            sample.totHRForAvg = if ((newCorrectedHeartRate?:0) > 0) previousTotHRForAvg + newCorrectedHeartRate!!*durationTimeIntInSec else previousTotHRForAvg
            sample.correctedHeartRate = newCorrectedHeartRate
            sample.maxScreenPower4 = previousMaxScreenPower4
            sample.maxScreenPower15 = previousMaxScreenPower15
            sample.maxScreenPower125 = previousMaxScreenPower125
            sample.maxScreenPower1000 = previousMaxScreenPower1000
            sample.screenPower4 = run{
                val kept = (3.0/4.0).pow(deltaTimeInSecond)
                if (actualPower != null) previousScreenPower4 * kept + actualPower.toDouble() * (1.0-kept)
            else previousScreenPower4 * kept.pow(1.0/3.0) }
            sample.screenPower15 = run{
                val kept = (14.0/15.0).pow(deltaTimeInSecond)
                if (actualPower != null) previousScreenPower15 * kept + actualPower.toDouble() * (1.0-kept)
                else previousScreenPower15 * kept.pow(1.0/3.0) }
            sample.screenPower125 = run{
                val kept = (124.0/125.0).pow(deltaTimeInSecond)
                if (actualPower != null) previousScreenPower125 * kept + actualPower.toDouble() * (1.0-kept)
                else previousScreenPower125 * kept.pow(1.0/3.0) }
            sample.screenPower1000 = run{
                val kept = (999.0/1000.0).pow(deltaTimeInSecond)
                if (actualPower != null) previousScreenPower1000 * kept + actualPower.toDouble() * (1.0-kept)
                else previousScreenPower1000 * kept.pow(1.0/3.0) }
        sample.totalPowerForAveragePower = previousTotalPowerForAveragePower
        sample.durationForAveragePower = previousDurationForAveragePower
        sample.durationPowerZ2 = previousDurationPowerZ2
        sample.durationPowerZ3 = previousDurationPowerZ3
        sample.durationPowerZ4 = previousDurationPowerZ4
        sample.durationPowerZ5 = previousDurationPowerZ5

        return
    }
if ((newGpsPoint.latitude == previousGpsPoint.latitude && previousGpsPoint.latitude == latestGpsPoint.latitude
&& newGpsPoint.longitude == previousGpsPoint.longitude && latestGpsPoint.longitude == previousGpsPoint.longitude)
        || (previousSpeed < 0.2 && previousIsMoving)) { //return screenValuesWhenIsMovingBecomeWrong
    if (IS_RINGING) {playDoubleBeep()}//sonnerie
        sample.gpsPointSpeed = 0.0
        sample.gpsPointAcceleration = 0.0
        sample.gpsPointVerticalSpeed = 0.0
        sample.gpsPointDirection = previousDirection
        sample.gpsPointAltitude = previousAltitude?: altitudeMntNewGpsPt
        sample.gpsPointLatitude = previousLatitude
        sample.gpsPointLongitude = previousLongitude
        sample.gpsPointGpsAltitudeAccuracy  = calculateGpsAltitudeAccuracy(newGpsPoint, previousGpsAltitudeAccuracy)
        sample.gpsPointAltitudeSourceMntMnsOrGps = previousAltitudeSourceMntMnsOrGps
        sample.gpsPointSpreadAltitudeAndAltitudeGps = previousSpreadAltitudeAndAltitudeGps
        sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = previousCoefficientOfSpreadAltitudeAndAltitudeGps
        sample.gpsPointDurationAltitudeNotMnt = previousDurationAltitudeNotMnt
        sample.gpsPointIsMoving = false
        sample.gpsPointIsStarted = true
        sample.gpsPointCumulatedGpsPrecision  = calculateNewCumulatedGpsPrecision(newGpsPoint, previousCumulatedGpsPrecision)
        sample.gpsPointIsAltitudeGot = previousIsAltitudeGot || simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot) != null
        sample.gpsPointIsGpsAltitudeGot = previousIsGpsAltitudeGot || newGpsPoint.altitudeGps != null
        sample.gpsPointUncorrectedGpsAltitude = previousUncorrectedGpsAltitude
        sample.gpsPointDisplayedVerticalSpeed = 0.0
        sample.gpsPointDisplayedVerticalSpeed2 = 0.0
        sample.gpsPointDisplayedAltitude = previousDisplayedAltitude?: altitudeMntNewGpsPt
        sample.gpsPointDisplayedSpeed = 0.0
        sample.gpsPointDurationHighSpreadAltitude = previousDurationHighSpreadAltitude
        sample.gpsPointLastDifferentSrtm = newGpsPointLastDifferentSrtm
        sample.gpsPointLastDifferentLidarMnt = newGpsPointLastDifferentLidarMnt
        sample.gpsPointLastDifferentLidarMns = newGpsPointLastDifferentLidarMns
        sample.gpsPointLastDifferentIgn = newGpsPointLastDifferentIgn
        sample.gpsPointDurationTime = previousGpsPointDurationTime
        sample.gpsPointTotalDistance = previousGpsPointTotalDistance
        sample.gpsPointMaxSpeed = previousGpsPointMaxSpeed
        sample.gpsPointMinSlope = previousMinSlope
        sample.gpsPointMaxSlope = previousMaxSlope
        sample.gpsPointSlope = previousSlope
        sample.gpsPointMaxAltitude = previousMaxAltitude
        sample.gpsPointMinAltitude = previousMinAltitude
        sample.gpsPointAltForElevationGain = previousAltForElevationGain
        sample.gpsPointElevationGain = previousElevationGain
        sample.gpsPointMinVerticalSpeed = previousMinVerticalSpeed
        sample.gpsPointMaxVerticalSpeed = previousMaxVerticalSpeed
        sample.gpsPointScreenValueString = previousGpsPointScreenValueString
        sample.partialDistance = previousPartialDistance
        sample.partialDurationTime = previousPartialDurationTime
        sample.partialDistanceForAverageSpeed = previousPartialDistanceForAverageSpeed
        sample.partialDurationTimeForAverageSpeed = previousPartialDurationTimeForAverageSpeed
        sample.partialElevationGain = previousPartialElevationGain
        sample.screenVerticalSpeed4 = previousVerticalSpeed4 * (3.0/4.0).pow(deltaTimeInSecond)
        sample.screenVerticalSpeed15 = previousVerticalSpeed15 * (14.0/15.0).pow(deltaTimeInSecond)
        sample.screenVerticalSpeed125 = previousVerticalSpeed125 * (124.0/125.0).pow(deltaTimeInSecond)
        sample.screenVerticalSpeed1000 = previousVerticalSpeed1000 * (999.0/1000.0).pow(deltaTimeInSecond)
        sample.maxVerticalSpeed15 = previousMaxVerticalSpeed15
        sample.maxVerticalSpeed125 = previousMaxVerticalSpeed125
        sample.maxVerticalSpeed1000 = previousMaxVerticalSpeed1000
        sample.durationHRZ2 = if ((newCorrectedHeartRate?:0) in HRZ2..<HRZ3) previousDurationHRZ2 + durationTimeIntInSec else previousDurationHRZ2
        sample.durationHRZ3 = if ((newCorrectedHeartRate?:0) in HRZ3..<HRZ4) previousDurationHRZ3 + durationTimeIntInSec else previousDurationHRZ3
        sample.durationHRZ4 = if ((newCorrectedHeartRate?:0) in HRZ4..<HRZ5) previousDurationHRZ4 + durationTimeIntInSec else previousDurationHRZ4
        sample.durationHRZ5 = if ((newCorrectedHeartRate?:0) >= HRZ5) previousDurationHRZ5 + durationTimeIntInSec else previousDurationHRZ5
        sample.durationHR = if ((newCorrectedHeartRate?:0) > 0) previousDurationHR + durationTimeIntInSec else previousDurationHR
        sample.minHR = if ((newCorrectedHeartRate?:0) > 0 && previousMinHR != null) min(newCorrectedHeartRate!!, previousMinHR) else if(previousMinHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMinHR
        sample.maxHR = if ((newCorrectedHeartRate?:0) > 0 && previousMaxHR != null) max(newCorrectedHeartRate!!, previousMaxHR) else if(previousMaxHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMaxHR
        sample.totHRForAvg = if ((newCorrectedHeartRate?:0) > 0) previousTotHRForAvg + newCorrectedHeartRate!!*durationTimeIntInSec else previousTotHRForAvg
        sample.correctedHeartRate = newCorrectedHeartRate
        sample.maxScreenPower4 = previousMaxScreenPower4
        sample.maxScreenPower15 = previousMaxScreenPower15
        sample.maxScreenPower125 = previousMaxScreenPower125
        sample.maxScreenPower1000 = previousMaxScreenPower1000
    sample.screenPower4 = run{
        val kept = (3.0/4.0).pow(deltaTimeInSecond)
        if (actualPower != null) previousScreenPower4 * kept + actualPower.toDouble() * (1.0-kept)
        else previousScreenPower4 * kept.pow(1.0/3.0) }
    sample.screenPower15 = run{
        val kept = (14.0/15.0).pow(deltaTimeInSecond)
        if (actualPower != null) previousScreenPower15 * kept + actualPower.toDouble() * (1.0-kept)
        else previousScreenPower15 * kept.pow(1.0/3.0) }
    sample.screenPower125 = run{
        val kept = (124.0/125.0).pow(deltaTimeInSecond)
        if (actualPower != null) previousScreenPower125 * kept + actualPower.toDouble() * (1.0-kept)
        else previousScreenPower125 * kept.pow(1.0/3.0) }
    sample.screenPower1000 = run{
        val kept = (999.0/1000.0).pow(deltaTimeInSecond)
        if (actualPower != null) previousScreenPower1000 * kept + actualPower.toDouble() * (1.0-kept)
        else previousScreenPower1000 * kept.pow(1.0/3.0) }
        sample.totalPowerForAveragePower = previousTotalPowerForAveragePower
        sample.durationForAveragePower = previousDurationForAveragePower
        sample.durationPowerZ2 = previousDurationPowerZ2
        sample.durationPowerZ3 = previousDurationPowerZ3
        sample.durationPowerZ4 = previousDurationPowerZ4
        sample.durationPowerZ5 = previousDurationPowerZ5

        return
    }

    //val newIsMoving = true
    val newDirection = calculateNewDirection (previousLatitude, previousLongitude, latestGpsPoint, newGpsPoint, previousGpsPoint, previousDirection)
    val speedCorrectionRatio = calculateSpeedCorrectionRatio(
        calculateAngleBetweenTwoDirection(previousDirection, newDirection))*1.002 // corection empirique minimale (= tendance de l'algorythme à couper les virages?)

    val deltaAltitudeBetweenPreviousAndNewGpsPoint = try {
        val delta = if (previousAltitude == null || (previousAltitudeSourceMntMnsOrGps == "GPS" && altitudeGpsNewGpsPt == null)
            || (previousAltitudeSourceMntMnsOrGps == "MNS" && altitudeMnsNewGpsPt == null)
            || (previousAltitudeSourceMntMnsOrGps == "MNT" && altitudeMntNewGpsPt == null)) 0.0
        else if (previousAltitudeSourceMntMnsOrGps == "GPS" ) (altitudeGpsNewGpsPt!! - previousAltitude)
        else if (previousAltitudeSourceMntMnsOrGps  == "MNS" ) (altitudeMnsNewGpsPt!! - previousAltitude)
        else if (previousAltitudeSourceMntMnsOrGps  == "MNT" ) (altitudeMntNewGpsPt!! - previousAltitude)
        else 0.0
        max(min(1.0,delta),-3.5,)} catch (e: Exception) {0.0}

    val expectedSpeed = run {
        val speedGps1 = (latestGpsPoint.speedGps?: newGpsPoint.speedGps?: previousSpeed).toDouble()
        val speedGps2 = (newGpsPoint.speedGps?: latestGpsPoint.speedGps?: previousSpeed).toDouble()
        val speedGps0 = (if(speedGps1 == 0.0 && speedGps2 == 0.0)previousGpsPoint.speedGps?:previousSpeed else speedGps1).toDouble()
        val speedPoints = (calculateDistanceBetweenTwoGpsPoints(previousGpsPoint.latitude, previousGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude))/((newGpsPoint.timestamp-previousGpsPoint.timestamp).toDouble()/1000.0)
        val speedPoint = (calculateDistanceBetweenTwoGpsPoints(previousLatitude?:newGpsPoint.latitude, previousLongitude?:newGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude))/((newGpsPoint.timestamp-previousGpsPoint.timestamp).toDouble()/1000.0)
        val speedPoint2 = (calculateDistanceBetweenTwoGpsPoints(previousLatitude?:latestGpsPoint.latitude, previousLongitude?:latestGpsPoint.longitude, latestGpsPoint.latitude, latestGpsPoint.longitude))/((latestGpsPoint.timestamp-previousGpsPoint.timestamp).toDouble()/1000.0)
        var speed = if ((speedGps1 > previousSpeed && speedGps2 < previousSpeed) || (speedGps1 < previousSpeed && speedGps2 > previousSpeed)) previousSpeed
        else listOf(speedGps1, speedGps2, speedPoint, speedPoints, speedGps0).minByOrNull { abs(it - previousSpeed) } ?:previousSpeed
        if (speedPoint > 0.2 + 1.9 * speedPoints || speedPoint2 > 0.2 + 1.9 * speedPoints) speed = 1.1 * speed + 0.2
        if (speedPoint > 0.4 + 3.9 * speedPoints) speed = 1.05 * speed + 0.2
        speed
    }
    val maxExpectedSpeed = sqrt (35.0 * min(deltaTimeInSecond,4.0)+ expectedSpeed * expectedSpeed)

    val expectedDistance = deltaTimeInSecond * expectedSpeed
    val minExpectedDistance = deltaTimeInSecond * max((expectedSpeed - 3.5*deltaTimeInSecond),0.0)
    val maxExpectedDistance = deltaTimeInSecond * maxExpectedSpeed

    val newHorizontalDistanceDone = calculateNewHorizontalDistanceDone (previousLatitude,previousLongitude,
        previousGpsPoint, newGpsPoint, latestGpsPoint,
        deltaTimeInSecond,
        newDirection,
        speedCorrectionRatio,
        minExpectedDistance, expectedDistance, maxExpectedDistance,
        deltaAltitudeBetweenPreviousAndNewGpsPoint,
        previousDirection, previousSpeed
    )
    val newLatitude = calculateNewLatitude(previousLatitude, previousLongitude, newDirection, newHorizontalDistanceDone,newGpsPoint, previousAltitude)
    val newLongitude = calculateNewLongitude(previousLatitude, previousLongitude, newDirection, newHorizontalDistanceDone, newGpsPoint, previousAltitude)

    val verticalSpeedForCalculateAltitude = (bufferSnapshot[bufferSnapshot.size - 3].gpsPointVerticalSpeed +
            bufferSnapshot[bufferSnapshot.size - 4].gpsPointVerticalSpeed +
            bufferSnapshot[bufferSnapshot.size - 5].gpsPointVerticalSpeed +
            bufferSnapshot[bufferSnapshot.size - 6].gpsPointVerticalSpeed +
            2 * previousDisplayedVerticalSpeed) / 6.0

    val coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone, altitudeMntNewGpsPt,altitudeMntMnsAccuracy, deltaTimeInSecond, 1.5, previousAltitude, previousSpeed, verticalSpeedForCalculateAltitude, previousAcceleration, previousDurationHighSpreadAltitude)
    val coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone,altitudeMnsNewGpsPt,altitudeMntMnsAccuracy, deltaTimeInSecond, 1.5, previousAltitude, previousSpeed, verticalSpeedForCalculateAltitude, previousAcceleration, previousDurationHighSpreadAltitude)
    val coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone,altitudeGpsNewGpsPt,altitudeGpsAccuracy, deltaTimeInSecond,(previousDurationAltitudeNotMnt + 1).toDouble(), previousAltitude, previousSpeed, verticalSpeedForCalculateAltitude, previousAcceleration, previousDurationHighSpreadAltitude)
    // correction des probabilités
    coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y *= 1 + max(1,previousDurationAltitudeNotMnt-2)*previousDurationAltitudeNotMnt/(1.0*altitudeMntMnsAccuracy)
    coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y *= 1 + max(1,previousDurationAltitudeNotMnt-2)*previousDurationAltitudeNotMnt/(1.1*altitudeMntMnsAccuracy)

    val newAltitudeSourceMntMnsOrGps = if (maxOf(
            coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y) == coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y) "MNT"
    else if (maxOf(
            coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y) == coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y) "MNS"
    else "GPS"

    val newDurationAltitudeNotMnt = if (newAltitudeSourceMntMnsOrGps == "MNT") max(previousDurationAltitudeNotMnt - 1,0)
    else min (10,previousDurationAltitudeNotMnt + 1)
    val newAltitude = calculateNewAltitude(
        coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability,
        coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability,
        coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability,
        previousAltitude
    )
    //mise à jour durationHighSpreadAltitude
    var newDurationHighSpreadAltitude = previousDurationHighSpreadAltitude
    if ( newAltitudeSourceMntMnsOrGps == "MNT"
        && abs((newAltitude?:0.0) - (altitudeMntNewGpsPt?:(newAltitude?:0.0)))>altitudeMntMnsAccuracy) {
        newDurationHighSpreadAltitude++
    }
    else if ( newAltitudeSourceMntMnsOrGps == "MNS"
        && abs((newAltitude?:0.0) - (altitudeMnsNewGpsPt?:(newAltitude?:0.0)))>altitudeMntMnsAccuracy) {
        newDurationHighSpreadAltitude++
    }
    else if ( newAltitudeSourceMntMnsOrGps == "GPS"
        && abs((newAltitude?:0.0) - (altitudeGpsNewGpsPt?:(newAltitude?:0.0)))>altitudeGpsAccuracy) {
        newDurationHighSpreadAltitude++
    }
    else newDurationHighSpreadAltitude = max(0,previousDurationHighSpreadAltitude - 1)
    //fin

    val newSpeed = calculateNewSpeed(newHorizontalDistanceDone, speedCorrectionRatio, newAltitude, deltaTimeInSecond, previousAltitude, previousSpeed)
    val newAcceleration = try {(newSpeed - previousSpeed)/(2 * deltaTimeInSecond)+ previousAcceleration / 2.0} catch (e: Exception) {previousAcceleration}
    val newVerticalSpeed = 0.6*previousVerticalSpeed +0.4*(try{((newAltitude?: (previousAltitude?: 0.0)) - (previousAltitude?: (newAltitude?: 0.0)))/deltaTimeInSecond}
    catch (e: Exception) {previousVerticalSpeed})
    val newGpsAltitudeAccuracy = altitudeGpsAccuracy//min (5.0 * previousValues.gpsAltitudeAccuracy / 6.0 + newGpsPoint.accuracy.toDouble() / 6.0,previousValues.gpsAltitudeAccuracy + 1.0)
    val newIsGpsAltitudeGot = previousIsGpsAltitudeGot || altitudeGpsNewGpsPt!= null

    val altitudeProb = when (newAltitudeSourceMntMnsOrGps) {
        "MNT" -> coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y
        "MNS" -> coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y
        else -> coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y
    }
    val newDisplayedVerticalSpeed = run{
        try{
            val correctionSpeed = 6.0
            val speed6 = 1000.5*(((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude)?:0.0) - ((bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointAltitude)?:((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude)?:0.0)))/((bufferSnapshot[max(0,bufferSnapshot.size - 7)].timestamp) + 1 - (bufferSnapshot[max(0,bufferSnapshot.size - 8)].timestamp)).toDouble()
            val speed5 = 1000.5*(((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude)?:0.0) - ((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude)?:((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude)?:0.0)))/((bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp) + 1 - (bufferSnapshot[max(0,bufferSnapshot.size - 7)].timestamp)).toDouble()
            val speed4 = 1000.5*(((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude)?:0.0) - ((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude)?:((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude)?:0.0)))/((bufferSnapshot[max(0,bufferSnapshot.size - 5)].timestamp) + 1 - (bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp)).toDouble()
            val speed3 = 1000.5*(((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude)?:0.0) - ((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude)?:((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude)?:0.0)))/((bufferSnapshot[max(0,bufferSnapshot.size - 4)].timestamp) + 1 - (bufferSnapshot[max(0,bufferSnapshot.size - 5)].timestamp)).toDouble()
            val speed2 = 1000.5*(((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude)?:0.0) - ((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude)?:((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude)?:0.0)))/((bufferSnapshot[max(0,bufferSnapshot.size - 3)].timestamp) + 1 - (bufferSnapshot[max(0,bufferSnapshot.size - 4)].timestamp)).toDouble()
            val speed1 = ((newAltitude?: (previousAltitude?: 0.0))-((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude)?:(newAltitude?: (previousAltitude?: 0.0))))/deltaTimeInSecond

            val medianVerticalSpeed4 = listOf(
                speed1, speed2, speed3, speed4, (speed1 + speed2)/2.0, (speed2 + speed3)/2.0, (speed3 + speed4)/2.0).sortedDescending()[3]
            val medianVerticalSpeed6 = listOf(
                speed1, speed2, speed3, speed4, speed5, speed6, (speed1 + speed2)/2.0, (speed2 + speed3)/2.0, (speed3 + speed4)/2.0, (speed4 + speed5)/2.0, (speed5 + speed6)/2.0).sortedDescending()[5]
            val factorForHighSpread = (1 + newDurationHighSpreadAltitude + 2 * previousDurationHighSpreadAltitude).toDouble() + (if (newDurationHighSpreadAltitude < previousDurationHighSpreadAltitude) 1.0 else 0.0)
           // correction speed peut etre remplacé per une variable majuscule, par: 1.0 pour changement très lents, plus pourchgt rapides
            val percentToChange = (min(0.25,altitudeProb.pow(1.0 / correctionSpeed)))/factorForHighSpread
            previousDisplayedVerticalSpeed * (1.0 - percentToChange) + medianVerticalSpeed6 * percentToChange

        }
        catch (e: Exception) {previousDisplayedVerticalSpeed}
    }
        //=AJ5298*MIN(0,9;PUISSANCE(MIN(AM5296;AM5297;AM5298);0,2)/(1+S5298+S5297))+(1-MIN(0,9;PUISSANCE(MIN(AM5296;AM5297;AM5298);0,2)/(1+S5298+S5297)))*AP5297
    val newDisplayedAltitude = if (newAltitude == null || previousDisplayedAltitude == null) newAltitude
            else {newAltitude * 0.08 + previousDisplayedAltitude * 0.92 + 0.92 * newDisplayedVerticalSpeed * deltaTimeInSecond}
    val newDisplayedSpeed = calculateDisplayedSpeed (bufferSnapshot[max(0,bufferSnapshot.size - 5)], bufferSnapshot[max(0,bufferSnapshot.size - 4)],previousGpsPoint, newGpsPoint, latestGpsPoint, previousDisplayedSpeed,newSpeed)

    val newSlope = calculateNewSlope(bufferSnapshot, newVerticalSpeed, newAltitude, newDisplayedSpeed,altitudeProb)
    val newAltForElevationGain = if (newDisplayedAltitude == null) null
        else if (previousAltForElevationGain == null) (newDisplayedAltitude)
        else if (newDisplayedAltitude < previousAltForElevationGain - STEP_FOR_ELEVATION_GAIN ) newDisplayedAltitude + STEP_FOR_ELEVATION_GAIN
        else   max(newDisplayedAltitude, previousAltForElevationGain)
    val newElevationGain = if (newDisplayedAltitude == null || previousAltForElevationGain == null) previousElevationGain
        else previousElevationGain + max(0.0,newDisplayedAltitude - previousAltForElevationGain)

    val newDisplayedVerticalSpeed2 = run{
        val deltaAlt3 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointDisplayedAltitude)?:0.0))
        val deltaAlt4 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointDisplayedAltitude)?:0.0))
        val deltaAlt5 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointDisplayedAltitude)?:0.0))
        val deltaAlt6 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointDisplayedAltitude)?:0.0))
        val deltaAlt7 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointDisplayedAltitude)?:0.0))
        val deltaAlt8 = - ((bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointDisplayedAltitude)?:0.0) + (newDisplayedAltitude?:((bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointDisplayedAltitude)?:0.0))
        val deltaT3 = (bufferSnapshot[max(0,bufferSnapshot.size - 3)].timestamp) - (newGpsPoint.timestamp)
        val deltaT4 = (bufferSnapshot[max(0,bufferSnapshot.size - 4)].timestamp) - (newGpsPoint.timestamp)
        val deltaT5 = (bufferSnapshot[max(0,bufferSnapshot.size - 5)].timestamp) - (newGpsPoint.timestamp)
        val deltaT6 = (bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp) - (newGpsPoint.timestamp)
        val deltaT7 = (bufferSnapshot[max(0,bufferSnapshot.size - 7)].timestamp) - (newGpsPoint.timestamp)
        val deltaT8 = (bufferSnapshot[max(0,bufferSnapshot.size - 8)].timestamp) - (newGpsPoint.timestamp)
        val precise = (bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitudeSourceMntMnsOrGps != "GPS"
                && bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitudeSourceMntMnsOrGps != "GPS"
                && bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitudeSourceMntMnsOrGps != "GPS"
                && bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitudeSourceMntMnsOrGps != "GPS"
                && bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitudeSourceMntMnsOrGps != "GPS"
                && bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointAltitudeSourceMntMnsOrGps != "GPS")
        val vSpeed3 = -1000.0*deltaAlt3/deltaT3
        val vSpeed4 = -1000.0*deltaAlt4/deltaT4
        val vSpeed5 = -1000.0*deltaAlt5/deltaT5
        val vSpeed6 = -1000.0*deltaAlt6/deltaT6
        val vSpeed7 = -1000.0*deltaAlt7/deltaT7
        val vSpeed8 = -1000.0*deltaAlt8/deltaT8
        if (precise) (listOf(vSpeed3,vSpeed4,vSpeed5).minByOrNull { abs((it - previousDisplayedVerticalSpeed2)/1.1) }?:0.0)*0.25 + previousDisplayedVerticalSpeed2*0.75
            else (listOf(vSpeed3,vSpeed4,vSpeed5, vSpeed6, vSpeed7, vSpeed8).minByOrNull { abs((it - previousDisplayedVerticalSpeed2)/1.1) }?:0.0)*0.3 + previousDisplayedVerticalSpeed2*0.7
    }
        val averagedVerticalSpeed2 = bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointDisplayedVerticalSpeed2 / 7.0 +
    bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointDisplayedVerticalSpeed2 / 7.0 +
    bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointDisplayedVerticalSpeed2 / 7.0 +
    bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointDisplayedVerticalSpeed2 / 7.0 +
    bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointDisplayedVerticalSpeed2 / 7.0 +
    previousDisplayedVerticalSpeed2/7.0 + newDisplayedVerticalSpeed2 /7.0

        if (IS_RINGING && !previousIsMoving) {playSimpleBeep()}//sonnerie

    sample.gpsPointSpeed = newSpeed
    sample.gpsPointAcceleration = newAcceleration
    sample.gpsPointVerticalSpeed = newVerticalSpeed
    sample.gpsPointDirection = newDirection
    sample.gpsPointAltitude = newAltitude
    sample.gpsPointLatitude = newLatitude
    sample.gpsPointLongitude = newLongitude
    sample.gpsPointGpsAltitudeAccuracy = newGpsAltitudeAccuracy
    sample.gpsPointAltitudeSourceMntMnsOrGps = newAltitudeSourceMntMnsOrGps
    sample.gpsPointSpreadAltitudeAndAltitudeGps = calculateSpreadAltitudeAndAltitudeGps ( previousGpsPoint, bufferSnapshot, previousAltitudeSourceMntMnsOrGps, previousAltitude, previousSpreadAltitudeAndAltitudeGps, previousCoefficientOfSpreadAltitudeAndAltitudeGps)
    sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = calculateCoefficientOfSpreadAltitudeAndAltitudeGps ( previousGpsPoint, newGpsPoint, latestGpsPoint, previousCoefficientOfSpreadAltitudeAndAltitudeGps)
    sample.gpsPointDurationAltitudeNotMnt = newDurationAltitudeNotMnt
    sample.gpsPointIsMoving = true
    sample.gpsPointIsStarted = true
    sample.gpsPointCumulatedGpsPrecision = newCumulatedGpsPrecision
    sample.gpsPointIsAltitudeGot = newAltitude!=null
    sample.gpsPointIsGpsAltitudeGot = newIsGpsAltitudeGot
    sample.gpsPointUncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (previousGpsPoint, newGpsPoint, latestGpsPoint, previousUncorrectedGpsAltitude)
    sample.gpsPointDisplayedVerticalSpeed = newDisplayedVerticalSpeed
    sample.gpsPointDisplayedVerticalSpeed2 = newDisplayedVerticalSpeed2
    sample.gpsPointDisplayedAltitude = newDisplayedAltitude
    sample.gpsPointDisplayedSpeed = newDisplayedSpeed * 0.6 + previousDisplayedSpeed * 0.4
    sample.gpsPointDurationHighSpreadAltitude = newDurationHighSpreadAltitude
    sample.gpsPointLastDifferentSrtm = newGpsPointLastDifferentSrtm
    sample.gpsPointLastDifferentLidarMnt = newGpsPointLastDifferentLidarMnt
    sample.gpsPointLastDifferentLidarMns = newGpsPointLastDifferentLidarMns
    sample.gpsPointLastDifferentIgn = newGpsPointLastDifferentIgn
    sample.gpsPointDurationTime = previousGpsPointDurationTime + min((deltaTimeInSecond*1000).toLong(),(newHorizontalDistanceDone*1000.0/MIN_SPEED).toLong())
    sample.gpsPointTotalDistance = previousGpsPointTotalDistance + newSpeed * deltaTimeInSecond
    sample.gpsPointMaxSpeed = max(previousGpsPointMaxSpeed,newDisplayedSpeed * 0.8 + previousDisplayedSpeed * 0.2)
    sample.gpsPointMinSlope = min(previousMinSlope,newSlope?:previousMinSlope)
    sample.gpsPointMaxSlope = max(previousMaxSlope,newSlope?:previousMaxSlope)
    sample.gpsPointSlope = newSlope?:previousSlope
    sample.gpsPointMaxAltitude = if (previousMaxAltitude == null || newDisplayedAltitude == null) newDisplayedAltitude
        else max(previousMaxAltitude,newDisplayedAltitude)
    sample.gpsPointMinAltitude = if (previousMinAltitude == null || newDisplayedAltitude == null) newDisplayedAltitude
        else min(previousMinAltitude,newDisplayedAltitude)
    sample.gpsPointElevationGain = newElevationGain
    sample.gpsPointAltForElevationGain = newAltForElevationGain
    sample.gpsPointScreenValueString = if (altitudeProb > 1.0 ) "||||||"
        else if (altitudeProb > 0.6) "||||| "
        else if (altitudeProb > 0.1) "||||  "
        else if (altitudeProb > 0.01) "|||   "
        else if (altitudeProb > 0.0001) "||    "
        else if (altitudeProb > 0.0000001) "|     "
        else "      "
    sample.partialDistance = previousPartialDistance + newSpeed * deltaTimeInSecond
    sample.partialDurationTime = previousPartialDurationTime + min((deltaTimeInSecond*1000).toLong(),(newHorizontalDistanceDone*1000.0/MIN_SPEED).toLong())
    sample.partialDistanceForAverageSpeed = previousPartialDistanceForAverageSpeed + newSpeed * deltaTimeInSecond
    sample.partialDurationTimeForAverageSpeed = previousPartialDurationTimeForAverageSpeed + min((deltaTimeInSecond*1000).toLong(),(newHorizontalDistanceDone*1000.0/MIN_SPEED).toLong())
    sample.partialElevationGain = previousPartialElevationGain + newElevationGain - previousElevationGain
    sample.screenVerticalSpeed4 = run {
        val kept = (3.0/4.0).pow(deltaTimeInSecond)
        previousVerticalSpeed4 * kept + averagedVerticalSpeed2 * (1.0 - kept)
    }
    sample.gpsPointMaxVerticalSpeed = max(sample.screenVerticalSpeed4, previousMaxVerticalSpeed)
    sample.gpsPointMinVerticalSpeed = min(sample.screenVerticalSpeed4, previousMinVerticalSpeed)
    sample.screenVerticalSpeed15 = run { val percent = if (averagedVerticalSpeed2 > previousVerticalSpeed15) 14.0/15.0 else 29.0/30.0
        val kept = (percent).pow(deltaTimeInSecond)
        previousVerticalSpeed15 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.screenVerticalSpeed125 = run { val percent = if (averagedVerticalSpeed2 > previousVerticalSpeed125) 124.0/125.0 else 249.0/250.0
        val kept = (percent).pow(deltaTimeInSecond)
        previousVerticalSpeed125 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.screenVerticalSpeed1000 = run { val percent = if (averagedVerticalSpeed2 > previousVerticalSpeed1000) 999.0/1000.0 else 1999.0/2000.0
        val kept = (percent).pow(deltaTimeInSecond)
        previousVerticalSpeed1000 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.maxVerticalSpeed15 = max(previousMaxVerticalSpeed15, sample.screenVerticalSpeed15)
    sample.maxVerticalSpeed125 = max(previousMaxVerticalSpeed125, sample.screenVerticalSpeed125)
    sample.maxVerticalSpeed1000 = max(previousMaxVerticalSpeed1000, sample.screenVerticalSpeed1000)
    sample.durationHRZ2 = if ((newCorrectedHeartRate?:0) in HRZ2..<HRZ3) previousDurationHRZ2 + durationTimeIntInSec else previousDurationHRZ2
    sample.durationHRZ3 = if ((newCorrectedHeartRate?:0) in HRZ3..<HRZ4) previousDurationHRZ3 + durationTimeIntInSec else previousDurationHRZ3
    sample.durationHRZ4 = if ((newCorrectedHeartRate?:0) in HRZ4..<HRZ5) previousDurationHRZ4 + durationTimeIntInSec else previousDurationHRZ4
    sample.durationHRZ5 = if ((newCorrectedHeartRate?:0) >= HRZ5) previousDurationHRZ5 + durationTimeIntInSec else previousDurationHRZ5
    sample.durationHR = if ((newCorrectedHeartRate?:0) > 0) previousDurationHR + durationTimeIntInSec else previousDurationHR
    sample.minHR = if ((newCorrectedHeartRate?:0) > 0 && previousMinHR != null) min(newCorrectedHeartRate!!, previousMinHR) else if(previousMinHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMinHR
    sample.maxHR = if ((newCorrectedHeartRate?:0) > 0 && previousMaxHR != null) max(newCorrectedHeartRate!!, previousMaxHR) else if(previousMaxHR == null && newCorrectedHeartRate != null) newCorrectedHeartRate else previousMaxHR
    sample.totHRForAvg = if ((newCorrectedHeartRate?:0) > 0) previousTotHRForAvg + newCorrectedHeartRate!!*durationTimeIntInSec else previousTotHRForAvg
    sample.correctedHeartRate = newCorrectedHeartRate
        sample.screenPower4 = run{
            val kept = (3.0/4.0).pow(deltaTimeInSecond)
            if (actualPower != null) previousScreenPower4 * kept + actualPower.toDouble() * (1.0-kept)
            else previousScreenPower4 * kept.pow(1.0/3.0) }
    sample.maxScreenPower4 = max(previousMaxScreenPower4,sample.screenPower4)
        sample.screenPower15 = run{
            val kept = (14.0/15.0).pow(deltaTimeInSecond)
            if (actualPower != null) previousScreenPower15 * kept + actualPower.toDouble() * (1.0-kept)
            else previousScreenPower15 * kept.pow(1.0/3.0) }
        sample.maxScreenPower15 = max(previousMaxScreenPower15,sample.screenPower15)
        sample.screenPower125 = run{
            val kept = (124.0/125.0).pow(deltaTimeInSecond)
            if (actualPower != null) previousScreenPower125 * kept + actualPower.toDouble() * (1.0-kept)
            else previousScreenPower125 * kept.pow(1.0/3.0) }
        sample.maxScreenPower125 = max(previousMaxScreenPower125,sample.screenPower125)
        sample.screenPower1000 = run{
            val kept = (999.0/1000.0).pow(deltaTimeInSecond)
            if (actualPower != null) previousScreenPower1000 * kept + actualPower.toDouble() * (1.0-kept)
            else previousScreenPower1000 * kept.pow(1.0/3.0) }
        sample.maxScreenPower1000 = max(previousMaxScreenPower1000,sample.screenPower1000)
        sample.totalPowerForAveragePower = if (newSpeed>SPEED_FOR_AVERAGE_POWER) previousTotalPowerForAveragePower + durationTimeIntInSec * (actualPower?:0)
        else previousTotalPowerForAveragePower
        sample.durationForAveragePower = if (newSpeed>SPEED_FOR_AVERAGE_POWER) previousDurationForAveragePower + durationTimeIntInSec else previousDurationForAveragePower
        sample.durationPowerZ2 = if (actualPower != null && actualPower >= POWER_ZONE_2 && actualPower < POWER_ZONE_3) previousDurationPowerZ2 + durationTimeIntInSec else previousDurationPowerZ2
        sample.durationPowerZ3 = if (actualPower != null && actualPower >= POWER_ZONE_3 && actualPower < POWER_ZONE_4) previousDurationPowerZ3 + durationTimeIntInSec else previousDurationPowerZ3
        sample.durationPowerZ4 = if (actualPower != null && actualPower >= POWER_ZONE_4 && actualPower < POWER_ZONE_5) previousDurationPowerZ4 + durationTimeIntInSec else previousDurationPowerZ4
        sample.durationPowerZ5 = if (actualPower != null && actualPower >= POWER_ZONE_5) previousDurationPowerZ5 + durationTimeIntInSec else previousDurationPowerZ5


    FitExporter.fileTsvFile(context,File(context.filesDir, "ride.tsv"),newGpsPoint.timestamp,
        listOf (
            newGpsPoint.gpsPointLatitude,
            newGpsPoint.gpsPointLongitude,
            newGpsPoint.gpsPointDisplayedAltitude,
            newGpsPoint.gpsPointElevationGain,
            (newGpsPoint.gpsPointDurationTime).toDouble()/1000.0,
            newGpsPoint.gpsPointDisplayedSpeed,
            newGpsPoint.gpsPointTotalDistance,
            if (newGpsPoint.correctedHeartRate != null) (newGpsPoint.correctedHeartRate!!.toDouble()) else null,
            if (newGpsPoint.power != null) (newGpsPoint.power!!.toDouble()) else null,
            if (newGpsPoint.cadence != null) (newGpsPoint.cadence!!.toDouble()) else null,
            if (newGpsPoint.outTemperature != null) (newGpsPoint.outTemperature!!.toDouble()) else null) as List<Double?>
    )
    //cr    eation du fichier gps_debug.txt

    appendDebugLog(
        context,
        "\t${newGpsPoint.accuracy}" +
                "\t${newAltitudeSourceMntMnsOrGps}" +
                "\t${if (newAltitudeSourceMntMnsOrGps == "MNT" || newAltitudeSourceMntMnsOrGps == "MNS") altitudeMntMnsAccuracy else altitudeGpsAccuracy}" +
                "\t${if (newAltitudeSourceMntMnsOrGps == "GPS")coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y else if (newAltitudeSourceMntMnsOrGps == "MNT")coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y else coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y}" +
                "\t${newAltitude}" +
                "\t${newDisplayedAltitude}" +
                "\t${newVerticalSpeed}" +
                "\t${newDisplayedVerticalSpeed}" +
                "\t${newDisplayedVerticalSpeed2}" +
                "\t${newDisplayedSpeed}" +
                "\t${foundLastAltitudeLidarMnt}" +
                "\t${foundLastAltitudeLidarMns}" +
                "\t${foundLastAltitudeIgn}" +
                "\t${foundLastAltitudeSrtm}" +
                "\t${altitudeMntNewGpsPt}" +
                "\t${altitudeMnsNewGpsPt}" +
                "\t${altitudeGpsNewGpsPt}" +
                "\t${newDurationHighSpreadAltitude}" +
                "\t${newSlope}"
    )
    return
}


fun playDoubleBeep() {
    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    val beepDuration = 150
    val gapBetweenBeeps = 100

    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, beepDuration)
    Handler(Looper.getMainLooper()).postDelayed({
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, beepDuration)
        Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, beepDuration + 50L)
    }, (beepDuration + gapBetweenBeeps).toLong())
}

fun playSimpleBeep() {
    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    val beepDuration = 150

    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, beepDuration)
    Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, beepDuration + 100L)
}