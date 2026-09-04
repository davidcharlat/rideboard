package com.example.rideboard.utils

import android.content.Context
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import java.io.File
import java.lang.Double.isNaN
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

const val MIN_SPEED = 0.1
const val STEP_FOR_ELEVATION_GAIN = 0.1

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


    val deltaTimeInSecond = /*if (previousIsMoving) */ (newGpsPoint.timestamp - previousTime).toDouble() / 1000.0
                            //else (newGpsPoint.timestamp - previousTime).toDouble() / 500.0 + 4.0
    val newCumulatedGpsPrecision = calculateNewCumulatedGpsPrecision (newGpsPoint, previousCumulatedGpsPrecision)

    if (!newIsStarted && newCumulatedGpsPrecision >1) {
        // startNewScreenValues (bufferSnapshot)
        val pt1 = bufferSnapshot[bufferSnapshot.size-3]
        val pt2 = bufferSnapshot[bufferSnapshot.size-2]
        val pt3 = bufferSnapshot[bufferSnapshot.size-1]

        val newAltitude = simplifySampleAltitudeMnt(pt2, bufferSnapshot) ?: (simplifySampleAltitudeMnt(pt1, bufferSnapshot) ?: calculateAltitudeGps(pt1, pt2, pt3?:pt2, previousIsAltitudeGot, previousSpreadAltitudeAndAltitudeGps, previousUncorrectedGpsAltitude))
        val newSpread = if (pt2.altitudeGps != null && newAltitude != null)
            pt2.altitudeGps!! - newAltitude else null
        //sample.gpsPointSpeed = 0.0
        //sample.gpsPointAcceleration :Double = 0.0
        //sample.gpsPointVerticalSpeed :Double = 0.0,
        sample.gpsPointDirection = calculateDirectionBetweenTwoPoints(pt1.latitude, pt1.longitude, pt2.latitude, pt2.longitude)
        sample.gpsPointAltitude = newAltitude
        sample.gpsPointLatitude = pt2.latitude
        sample.gpsPointLongitude = pt2.longitude
        sample.gpsPointGpsAltitudeAccuracy = min((pt2.altitudeAccuracy ?: 299.9f).toDouble(), (pt1.altitudeAccuracy ?: 299.9f).toDouble())
        sample.gpsPointAltitudeSourceMntMnsOrGps = if (newSpread != null) "MNT" else if (newAltitude != 0.0) "GPS" else ""
        sample.gpsPointSpreadAltitudeAndAltitudeGps = newSpread
        sample.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps = if (newSpread!= null && pt2.altitudeAccuracy != null && pt2.altitudeAccuracy!!.toDouble() != 0.0)
            1.0/(((pt2.altitudeAccuracy)?: 999.9f)).toDouble() else 0.0
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

        sample.gpsPointScreenValueString = previousGpsPointScreenValueString
        //sample.gpsPointUncorrectedGpsAltitude: Double? = null,
        //sample.gpsPointDisplayedVerticalSpeed: Double = 0.0,
        //sample.gpsPointDisplayedAltitude:Double? = null,
        //sample.gpsPointDisplayedSpeed:Double = 0.0,
        //sample.gpsPointDurationHighSpreadAltitude: Int = 0,

        //sample.gpsPointScreenValueDouble1: Double? = 0.0,
        //sample.gpsPointScreenValueDouble2: Double? = 0.0,
        //sample.gpsPointScreenValueDouble3: Double? = 0.0,
        //sample.gpsPointScreenValue: Boolean? = false,
        //sample.gpsPointScreenValueString2: String? = "",
        //sample.gpsPointScreenValueString3: String? = "",
        //sample.gpsPointScreenValueString4: String? = "",
        //sample.gpsPointScreenValueBoolean: Boolean? = false,
        //sample.gpsPointScreenValueLong: Long? = 0,
        sample.gpsPointScreenValueInt = buffer.size

        return
    }
    if (!newIsStarted) //return previousValues
    {
        sample.gpsPointIsStarted = false
        sample.gpsPointCumulatedGpsPrecision = calculateNewCumulatedGpsPrecision(newGpsPoint,previousCumulatedGpsPrecision)
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
    val time2 = System.currentTimeMillis()

    val altitudeMntMnsAccuracy = altitudeMntNewGpsPtAndAccuracy.y

    val time3 = System.currentTimeMillis()

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

        //sample.gpsPointScreenValueDouble1: Double? = 0.0,
        //sample.gpsPointScreenValueDouble2: Double? = 0.0,
        //sample.gpsPointScreenValueDouble3: Double? = 0.0,
        //sample.gpsPointScreenValue: Boolean? = false,
        //sample.gpsPointScreenValueString2: String? = "",
        //sample.gpsPointScreenValueString3: String? = "",
        //sample.gpsPointScreenValueString4: String? = "",
        //sample.gpsPointScreenValueBoolean: Boolean? = false,
        //sample.gpsPointScreenValueLong: Long? = 0,
        sample.gpsPointScreenValueInt = buffer.size
        return
    }
    // from here isMoving must be true because newGpsPoint is too far, (otherwise calculateScreenValues already returned)
    if ((newGpsPoint.latitude == previousGpsPoint.latitude && previousGpsPoint.latitude == latestGpsPoint.latitude
                && newGpsPoint.longitude == previousGpsPoint.longitude && latestGpsPoint.longitude == previousGpsPoint.longitude)
        || (previousSpeed < 0.2 && previousIsMoving)) { //return screenValuesWhenIsMovingBecomeWrong
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

        //sample.gpsPointScreenValueDouble1: Double? = 0.0,
        //sample.gpsPointScreenValueDouble2: Double? = 0.0,
        //sample.gpsPointScreenValueDouble3: Double? = 0.0,
        //sample.gpsPointScreenValue: Boolean? = false,

        //sample.gpsPointScreenValueString2: String? = "",
        //sample.gpsPointScreenValueString3: String? = "",
        //sample.gpsPointScreenValueString4: String? = "",
        //sample.gpsPointScreenValueBoolean: Boolean? = false,
        //sample.gpsPointScreenValueLong: Long? = 0,
        sample.gpsPointScreenValueInt = buffer.size
        return
    }

    val newIsMoving = true
    val newDirection = calculateNewDirection (previousLatitude, previousLongitude, latestGpsPoint, newGpsPoint, previousGpsPoint, previousDirection)
    val speedCorrectionRatio = calculateSpeedCorrectionRatio(
        calculateAngleBetweenTwoDirection(previousDirection, newDirection))

    val deltaAltitudeBetweenPreviousAndNewGpsPoint = try {
        val delta = if (previousAltitude == null || (previousAltitudeSourceMntMnsOrGps == "GPS" && altitudeGpsNewGpsPt == null)
            || (previousAltitudeSourceMntMnsOrGps == "MNS" && altitudeMnsNewGpsPt == null)
            || (previousAltitudeSourceMntMnsOrGps == "MNT" && altitudeMntNewGpsPt == null)) 0.0
        else if (previousAltitudeSourceMntMnsOrGps == "GPS" ) (altitudeGpsNewGpsPt!! - previousAltitude)
        else if (previousAltitudeSourceMntMnsOrGps  == "MNS" ) (altitudeMnsNewGpsPt!! - previousAltitude)
        else if (previousAltitudeSourceMntMnsOrGps  == "MNT" ) (altitudeMntNewGpsPt!! - previousAltitude)
        else 0.0
        max(min(1.0,delta),-3.5,)} catch (e: Exception) {0.0}
    /*
        val deltaAltitudeBetweenTwoLastPointAccordingToSource = try {
            if (previousValues.altitudeSourceMntMnsOrGps == "GPS" && altitudeGpsNewGpsPt != null)
                altitudeGpsNewGpsPt - (calculateAltitudeGps(previousValues,(buffer.getNthBeforeLast(4))?:previousGpsPoint ,previousGpsPoint, newGpsPoint)?:altitudeGpsNewGpsPt)
            else if (altitudeMnsNewGpsPt != null && altitudeMntNewGpsPt != null) listOf (
                ((simplifySampleAltitudeMnt(previousGpsPoint, buffer))?:altitudeMntNewGpsPt) - altitudeMntNewGpsPt,
                ((simplifySampleAltitudeMns(previousGpsPoint, buffer))?:altitudeMnsNewGpsPt) - altitudeMnsNewGpsPt,
                ((simplifySampleAltitudeMnt(previousGpsPoint, buffer))?:altitudeMntNewGpsPt) - altitudeMnsNewGpsPt,
                ((simplifySampleAltitudeMns(previousGpsPoint, buffer))?:altitudeMnsNewGpsPt) - altitudeMntNewGpsPt,
            ).minByOrNull { abs(it) }
            else 0.0
        } catch (e: Exception) {0.0}
    */
    /*
    val distanceToNewPointProjectedOnNewDir =
        calculateDistanceToNewPointProjectedOnNewDir(previousValues, newGpsPoint, newDirection)

     */
    val expectedSpeed = run {
        val speedGps1 = (latestGpsPoint.speedGps?: newGpsPoint.speedGps?: previousSpeed).toDouble()
        val speedGps2 = (newGpsPoint.speedGps?: latestGpsPoint.speedGps?: previousSpeed).toDouble()
        val speedGps0 = (if(speedGps1 == 0.0 && speedGps2 == 0.0)previousGpsPoint.speedGps?:previousSpeed else speedGps1).toDouble()
        val speedPoint = (calculateDistanceBetweenTwoGpsPoints(previousLatitude?:newGpsPoint.latitude, previousLongitude?:newGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude))/deltaTimeInSecond
        val speedPoint2 = (calculateDistanceBetweenTwoGpsPoints(previousLatitude?:latestGpsPoint.latitude, previousLongitude?:latestGpsPoint.longitude, latestGpsPoint.latitude, latestGpsPoint.longitude))/((latestGpsPoint.timestamp-previousGpsPoint.timestamp).toDouble()/1000.0)
        val speed = if ((speedGps1 > previousSpeed && speedGps2 < previousSpeed) || (speedGps1 < previousSpeed && speedGps2 > previousSpeed)) previousSpeed
        else listOf(speedGps1, speedGps2, speedPoint, speedPoint2, speedGps0).minByOrNull { abs(it - previousSpeed) } ?:previousSpeed
        speed
    }
        /*maxOf(
        0.0 ,
        (previousGpsPoint.speedGps?:previousSpeed).toDouble()/2.0 - 1.1,
        max(min(previousAcceleration * min(deltaTimeInSecond, 4.0)/4.0,3.5),-3.5)
            + (min(deltaTimeInSecond,4.0) * previousVerticalSpeed * 10.0 / (max(previousSpeed,(previousGpsPoint.speedGps?:previousSpeed).toDouble()) + 3.0))/2.0
            + previousSpeed
            - (min(max(deltaAltitudeBetweenPreviousAndNewGpsPoint,-10.0),1.1) * 10.0 / (max(previousSpeed,(previousGpsPoint.speedGps?:previousSpeed).toDouble()) + 3.0))/2.0
    )*/
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
    sample.screenVerticalSpeed4 = run { val kept = (3.0/4.0).pow(deltaTimeInSecond)
        previousVerticalSpeed4 * kept + averagedVerticalSpeed2 * (1.0 - kept)
    }
    sample.gpsPointMaxVerticalSpeed = max(sample.screenVerticalSpeed4, previousMaxVerticalSpeed)
    sample.gpsPointMinVerticalSpeed = min(sample.screenVerticalSpeed4, previousMinVerticalSpeed)
    sample.screenVerticalSpeed15 = run { val kept = (14.0/15.0).pow(deltaTimeInSecond)
        previousVerticalSpeed15 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.screenVerticalSpeed125 = run { val kept = (124.0/125.0).pow(deltaTimeInSecond)
        previousVerticalSpeed125 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.screenVerticalSpeed1000 = run { val kept = (999.0/1000.0).pow(deltaTimeInSecond)
        previousVerticalSpeed1000 * kept + max(0.0 , averagedVerticalSpeed2) * (1.0 - kept)
    }
    sample.maxVerticalSpeed15 = max(previousMaxVerticalSpeed15, sample.screenVerticalSpeed15)
    sample.maxVerticalSpeed125 = max(previousMaxVerticalSpeed125, sample.screenVerticalSpeed125)
    sample.maxVerticalSpeed1000 = max(previousMaxVerticalSpeed1000, sample.screenVerticalSpeed1000)

    sample.gpsPointScreenValueDouble1=latestGpsPoint.altitudeGps
    sample.gpsPointScreenValueDouble2 = expectedDistance
    sample.gpsPointScreenValueDouble3 = (latestGpsPoint.altitudeAccuracy?:0.0).toDouble()
    //sample.gpsPointScreenValue: Boolean? = false,
    sample.gpsPointScreenValueString2 = "alt mnt brut/fin" + (((altitudeMntNewGpsPt ?: 0.0) * 100).toInt() / 100.0).toString() + " / " + ((((coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.x?:0.0)*100).toInt())/100.0).toString()
    sample.gpsPointScreenValueString3 = "alt gps brut/fin" + (((altitudeGpsNewGpsPt ?: 0.0) * 100).toInt() / 100.0).toString() + " / " + ((((coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.x?:0.0)*100).toInt())/100.0).toString()
    sample.gpsPointScreenValueString4 = "i gps/acc scr " + ((36.0*newSpeed).toInt()/10.0).toString() + " " + ((36.0*(newGpsPoint.speedGps?:0.0).toDouble()).toInt()/10.0).toString() + "/" + "${newGpsPoint.speedAccuracy}" + " " + ((36.0*newGpsPoint.gpsPointDisplayedSpeed).toInt()/10.0).toString()
    //sample.gpsPointScreenValueBoolean: Boolean? = false,
    //sample.gpsPointScreenValueLong: Long? = 0,
    sample.gpsPointScreenValueInt = buffer.size
   /*
    AppConfig.gpsPointIsAltitudeGot = newAltitude!=null
    AppConfig.gpsPointIsGpsAltitudeGot = newIsGpsAltitudeGot
    AppConfig.gpsPointIsMoving = newIsMoving
    AppConfig.gpsPointUncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (previousGpsPoint, newGpsPoint, latestGpsPoint, previousUncorrectedGpsAltitude)
    AppConfig.gpsPointDurationHighSpreadAltitude = newDurationHighSpreadAltitude
    AppConfig.gpsPointDisplayedAltitude = newAltitude
    AppConfig.gpsPointDisplayedVerticalSpeed = newVerticalSpeed/2.0 + previousVerticalSpeed/2.0
    AppConfig.gpsPointDisplayedSpeed = max(0.3*(previousGpsPoint.speedGps?:0.0).toDouble() + 0.7*(newGpsPoint.speedGps?:0.0).toDouble(), 0.4*previousSpeed + 0.6 * newSpeed)

*/
    FitExporter.fileTsvFile(context,File(context.filesDir, "ride.tsv"),newGpsPoint.timestamp,
        listOf (
            newGpsPoint.gpsPointLatitude,
            newGpsPoint.gpsPointLongitude,
            newGpsPoint.gpsPointDisplayedAltitude,
            newGpsPoint.gpsPointElevationGain,
            (newGpsPoint.gpsPointDurationTime).toDouble()/1000.0,
            newGpsPoint.gpsPointDisplayedSpeed,
            newGpsPoint.gpsPointTotalDistance,
            newGpsPoint.gpsPointDisplayedVerticalSpeed2,) as List<Double?>
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
/*
    appendDebugLog(
        context,
        "${newGpsPoint.timestamp}\t" +
                "${newLatitude}\t" +
                "${newLongitude}\t" +
                "${newAltitude}\t" +
                "${newDisplayedAltitude}\t" +
                "${newSlope}\t" +
                "${bufferSnapshot[bufferSnapshot.size-2].speedGps}\t" +
                "${bufferSnapshot[bufferSnapshot.size-2].speedAccuracy}\t" +
                "${newSpeed}\t" +
                "${newDisplayedSpeed}\t" +
                "${newVerticalSpeed}\t" +
                "${newDisplayedVerticalSpeed}\t" +
                "${newDisplayedVerticalSpeed2}\t" +
                "${newAltitudeSourceMntMnsOrGps}\t" +
                "${newDurationHighSpreadAltitude}\t" +
                "${altitudeMntMnsAccuracy}\t" +
                "${altitudeGpsAccuracy}\t" +
                "${altitudeMntNewGpsPt}\t" +
                "${altitudeMnsNewGpsPt}\t" +
                "$altitudeGpsNewGpsPt"
    )
*/


    return
}

fun calculateAccuracyAltitudeIgn(sample: GpsSample, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): Double {
    fun calculateAccuracy (
        vertSpeed: Double,
        supplementVertSpeed: Double,
        gpsAccuracy: Double,
        supplementGpsAccuracy: Double,
        coefGpsAccuracy: Double,
        speed: Double,
        supplementSpeed: Double,
        generalCoef: Double,
        uncertaintyToAdd: Double,
        previousDurationAltitudeNotMnt: Int
    ):Double {
        return try { previousDurationAltitudeNotMnt * 0.05 + generalCoef*((abs(vertSpeed)+supplementVertSpeed)*(gpsAccuracy+supplementGpsAccuracy)
                /(speed+supplementSpeed)+coefGpsAccuracy*gpsAccuracy+uncertaintyToAdd)
            //lidar:sVS:0.02;sGA:0.5;cGA:0.02;sS:3.0;gC:0.8;uTA:0.0) ; //ign:0.04;5.0;0.02;3.0;1.0;0.1; //srtm:0.05;50.0;0.02;2.0;1.0;2.0)
        } catch (e: Exception) {
            999.9
        }
    }
    if (sample.altitudeIgn != null && sample.altitudeIgn!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.04, sample.accuracy.toDouble(), 5.0,
        0.02, previousSpeed, 3.0, 1.0, 0.1, previousDurationAltitudeNotMnt)
    if (sample.altitudeSrtm != null && sample.altitudeSrtm!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.05, sample.accuracy.toDouble(),  50.0,
        0.02, previousSpeed, 2.0, 1.0, 2.0, previousDurationAltitudeNotMnt)
    return 999.9
}

fun calculateAccuracyAltitudeMntMns(sample: GpsSample, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): Double {
    fun calculateAccuracy (
        vertSpeed: Double,
        supplementVertSpeed: Double,
        gpsAccuracy: Double,
        supplementGpsAccuracy: Double,
        coefGpsAccuracy: Double,
        speed: Double,
        supplementSpeed: Double,
        generalCoef: Double,
        uncertaintyToAdd: Double,
        previousDurationAltitudeNotMnt: Int
    ):Double {
        return try { previousDurationAltitudeNotMnt * 0.05 + generalCoef*((abs(vertSpeed)+supplementVertSpeed)*(gpsAccuracy+supplementGpsAccuracy)
                /(speed+supplementSpeed)+coefGpsAccuracy*gpsAccuracy+uncertaintyToAdd)
            //lidar:sVS:0.02;sGA:0.5;cGA:0.02;sS:3.0;gC:0.8;uTA:0.0) ; //ign:0.04;5.0;0.02;3.0;1.0;0.1; //srtm:0.05;50.0;0.02;2.0;1.0;2.0)
        } catch (e: Exception) {
            999.9
        }
    }
    if (sample.altitudeLidarHdMnt != null && sample.altitudeLidarHdMnt!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.02, sample.accuracy.toDouble(), 0.5,
        0.02, previousSpeed, 3.0, 0.8, 0.0, previousDurationAltitudeNotMnt)
    if (sample.altitudeIgn != null && sample.altitudeIgn!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.04, sample.accuracy.toDouble(), 5.0,
        0.02, previousSpeed, 3.0, 1.0, 0.1, previousDurationAltitudeNotMnt)
    if (sample.altitudeSrtm != null && sample.altitudeSrtm!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.05, sample.accuracy.toDouble(),  50.0,
        0.02, previousSpeed, 2.0, 1.0, 2.0, previousDurationAltitudeNotMnt)
    return 999.9
}

fun calculateAccuracyAltitudeSrtm(sample: GpsSample, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): Double {
    fun calculateAccuracy (
        vertSpeed: Double,
        supplementVertSpeed: Double,
        gpsAccuracy: Double,
        supplementGpsAccuracy: Double,
        coefGpsAccuracy: Double,
        speed: Double,
        supplementSpeed: Double,
        generalCoef: Double,
        uncertaintyToAdd: Double,
        previousDurationAltitudeNotMnt: Int
    ):Double {
        return try { previousDurationAltitudeNotMnt * 0.05 + generalCoef*((abs(vertSpeed)+supplementVertSpeed)*(gpsAccuracy+supplementGpsAccuracy)
                /(speed+supplementSpeed)+coefGpsAccuracy*gpsAccuracy+uncertaintyToAdd)
            //lidar:sVS:0.02;sGA:0.5;cGA:0.02;sS:3.0;gC:0.8;uTA:0.0) ; //ign:0.04;5.0;0.02;3.0;1.0;0.1; //srtm:0.05;50.0;0.02;2.0;1.0;2.0)
        } catch (e: Exception) {
            999.9
        }
    }
    if (sample.altitudeSrtm != null && sample.altitudeSrtm!! > -500.0) return calculateAccuracy (previousVerticalSpeed, 0.05, sample.accuracy.toDouble(),  50.0,
        0.02, previousSpeed, 2.0, 1.0, 2.0, previousDurationAltitudeNotMnt)
    return 999.9
}

fun calculateAltitudeGps ( oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample, previousIsAltitudeGot: Boolean, previousSpreadAltitudeAndAltitudeGps: Double?, previousUncorrectedGpsAltitude: Double?): Double? {
    if (!previousIsAltitudeGot) return null
    val newUncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (oldPoint, newPoint, latestPoint, previousUncorrectedGpsAltitude)
    if (newUncorrectedGpsAltitude == null || previousSpreadAltitudeAndAltitudeGps == null) return null
    return newUncorrectedGpsAltitude - previousSpreadAltitudeAndAltitudeGps
}

fun calculateAltitudeProbability (newHorizontalDistanceDone: Double,
                                  altitudeOfTheSource: Double?,
                                  accuracyOfTheSource: Double,
                                  deltaTimeInSecond: Double,
                                  powerForSource: Double,
                                  prePreviousAltitude: Double?, previousSpeed: Double, previousVerticalSpeed: Double, previousAcceleration: Double, previousDurationHighSpreadAltitude: Int):
        CoordinatesOrXNull {
    if (altitudeOfTheSource == null) return CoordinatesOrXNull(prePreviousAltitude, 0.0)
    val functionOfProbabilityOfAltitudeOfTheSource = { x: Double ->
        val unit = ((1.0 / sqrt((x - altitudeOfTheSource) * (x - altitudeOfTheSource)/(accuracyOfTheSource*accuracyOfTheSource) + 1.0))/(PI*accuracyOfTheSource))
        try {
            val power = min(powerForSource, 8.0)
            unit.pow(power)

        } catch (e: Exception) {unit}
    }
    val uncorrectedVerticalSpeed = previousVerticalSpeed
    val previousVerticalSpeed = if (uncorrectedVerticalSpeed < -9) (uncorrectedVerticalSpeed*0.1 - 5.1)
    else if (uncorrectedVerticalSpeed < -3) (uncorrectedVerticalSpeed*0.5 + 1.5)
    else if (uncorrectedVerticalSpeed < 2) uncorrectedVerticalSpeed
    else if (uncorrectedVerticalSpeed < 4) (uncorrectedVerticalSpeed*0.5 + 1.0)
    else (uncorrectedVerticalSpeed*0.1 + 3.6)
    val previousAltitude = prePreviousAltitude?:altitudeOfTheSource
    val previousHorizontalSpeed = sqrt(max(0.0,previousSpeed*previousSpeed-previousVerticalSpeed-previousVerticalSpeed))
    val deltaAltitude = if (altitudeOfTheSource - previousAltitude < 0) min (0.0,max(altitudeOfTheSource - previousAltitude, previousVerticalSpeed * deltaTimeInSecond))
    else max(0.0, min(previousAltitude - altitudeOfTheSource, previousVerticalSpeed * deltaTimeInSecond))
    val previousPower = max(0.0,previousHorizontalSpeed * previousHorizontalSpeed + 4.0 * previousHorizontalSpeed + previousVerticalSpeed * 300.0 + 10.0 * previousHorizontalSpeed * previousAcceleration)
    // retrouver l'altitude en fonction de previous power et de la vitesse, pas la vitesse fct de l'altitude
    /*val a = 1-(30/deltaTime)
    val b = 4-(30*previousSpeed/deltaTime)
    val c = 300*deltaAltitude/deltaTime - previousPower
    val nSpeed=(-b + sqrt(b * b - 4 * a * c)) / (2 * a)*/
    val horizontalSpeed = newHorizontalDistanceDone/deltaTimeInSecond
    val newSpeed= sqrt(newHorizontalDistanceDone*newHorizontalDistanceDone+deltaAltitude*deltaAltitude)/deltaTimeInSecond
    val expectedAltitude = previousAltitude + ((deltaTimeInSecond/300.0)*(previousPower-horizontalSpeed*horizontalSpeed-4.0*horizontalSpeed-10.0*horizontalSpeed*(horizontalSpeed-previousSpeed)
            /deltaTimeInSecond))/3.0 + deltaTimeInSecond*previousVerticalSpeed/1.5
    val expectedMaxAltitude = previousAltitude + ((deltaTimeInSecond/300.0)*(previousPower+300.0-horizontalSpeed*horizontalSpeed-4.0*horizontalSpeed-10.0*horizontalSpeed*(horizontalSpeed-previousSpeed)
            /deltaTimeInSecond))/3.0 + deltaTimeInSecond*max(-0.1,previousVerticalSpeed)/1.5 + 0.03*deltaTimeInSecond*min(20.0,deltaTimeInSecond)

    //cette zone pour ajouter une fonction qui inhibe les gros eccarts d'alt
    val expectedAltitudeDependingOnVerticalSpeed = previousAltitude + previousVerticalSpeed * deltaTimeInSecond
    val expectedMaxAltitudeDependingOnVerticalSpeed = previousAltitude + max (0.0,previousVerticalSpeed + 0.15)*deltaTimeInSecond
    val expectedMinAltitudeDependingOnVerticalSpeed = previousAltitude + min (0.0, previousVerticalSpeed - 0.33)*deltaTimeInSecond
    val functionOfProbabilityDependingOnVerticalSpeed = {x: Double ->
        try {
            if (x > expectedAltitudeDependingOnVerticalSpeed) {
                val spread = expectedMaxAltitudeDependingOnVerticalSpeed-expectedAltitudeDependingOnVerticalSpeed
                val xSpread = x-expectedAltitudeDependingOnVerticalSpeed
                val unit = 1.0/((xSpread*xSpread*xSpread*xSpread)/(spread*spread*spread*spread)+1.0)
                unit
            }
            else {
                val spread = expectedAltitudeDependingOnVerticalSpeed-expectedMinAltitudeDependingOnVerticalSpeed
                val xSpread = expectedAltitudeDependingOnVerticalSpeed - x
                val unit = 1.0/((xSpread*xSpread*xSpread*xSpread)/(spread*spread*spread*spread)+1.0)
                unit
            }
        }
        catch (e: Exception) {
            1.0
        }
    }

    //fin de cette zone

    val functionOfProbabilityDependingOnSpeed = { x: Double ->
        try {
            if (x > expectedAltitude) {
                val unit =
                    1.0 / ((x - expectedAltitude) * (x - expectedAltitude)
                            / ((expectedMaxAltitude - expectedAltitude) * (expectedMaxAltitude - expectedAltitude)) + 1.0)
                unit
            }
            else {
                val unit =
                    1.0 / ((expectedAltitude - x)
                            / (expectedMaxAltitude - expectedAltitude) + 1.0)
                unit
            }
        }
        catch (e: Exception) {
            1 / ((expectedAltitude - x) * (expectedAltitude - x)/(0.01 + (expectedAltitude - previousAltitude)*(expectedAltitude - previousAltitude)) + 1)
        }
    }
    val start = expectedAltitudeDependingOnVerticalSpeed - 4.0 * maxOf(abs(expectedAltitude-altitudeOfTheSource),expectedMaxAltitudeDependingOnVerticalSpeed-expectedAltitudeDependingOnVerticalSpeed,expectedMaxAltitude-expectedAltitude)
    val end = expectedAltitudeDependingOnVerticalSpeed + 2.0 * maxOf(abs(expectedAltitude-altitudeOfTheSource),expectedAltitudeDependingOnVerticalSpeed-expectedMinAltitudeDependingOnVerticalSpeed,expectedMaxAltitude-expectedAltitude)
    val functionOfProbability = { x: Double -> functionOfProbabilityOfAltitudeOfTheSource(x) * functionOfProbabilityDependingOnSpeed(x) * functionOfProbabilityDependingOnVerticalSpeed(x)}
    val coordinates = findMaxOfIncreasingThenDecreasingFunction(
        functionOfProbability,
        start,
        end,
        min(150,((1+(end-start)/min(accuracyOfTheSource,0.2)).toInt())*3),
        0.01)
    if (abs(altitudeOfTheSource - (coordinates.x)) > accuracyOfTheSource) {
        val nb = max (0.0,(previousDurationHighSpreadAltitude) - 0.5)
        if (coordinates.x > altitudeOfTheSource)
            coordinates.x -= (coordinates.x-(altitudeOfTheSource+accuracyOfTheSource)) * nb / (nb + 1.0) //nb * coordinates.x / (nb+1.0) + altitudeOfTheSource / (nb + 1.0)
        else coordinates.x += ((altitudeOfTheSource-accuracyOfTheSource)-coordinates.x) * nb / (nb + 1.0)
    }
    return CoordinatesOrXNull(coordinates.x, coordinates.y)
}

fun calculateGpsAltitudeAccuracy(pt1: GpsSample?, previousGpsAltitudeAccuracy: Double): Double{
    //=MIN(9*CS4/10+(P4+0,5)/10;CS4+1)
    //cs = previousValues.gpsAltitudeAccuracy
    if (pt1 == null) return 999.9
    val p = ((pt1.altitudeAccuracy?:999.9).toDouble())/1.0
    return try{ min(9.0 * previousGpsAltitudeAccuracy /10.0+(p+0.15)/10.0,
        previousGpsAltitudeAccuracy + 0.5)}
    catch (e: Exception) {
        previousGpsAltitudeAccuracy
    }
}

fun calculateAngleBetweenTwoDirection (direction1: Double?, direction2: Double?): Double {
    if (direction1 == null || direction2 == null) return 0.0
    return minOf(abs(direction1-direction2), abs(direction1-direction2-2*PI), abs(direction1-direction2+2*PI))
}

fun calculateCoefficientOfSpreadAltitudeAndAltitudeGps ( oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample, previousCoefficientOfSpreadAltitudeAndAltitudeGps: Double): Double {
    // la formule ci dessous est ok, mais remplacer newpoint par le choix du point comme dans calculatealtgps
    val acc = newPoint.altitudeAccuracy?: 9999.0F
    return try {
        if (previousCoefficientOfSpreadAltitudeAndAltitudeGps < 100) {
            previousCoefficientOfSpreadAltitudeAndAltitudeGps + 1 / (acc + 0.1)} else {100.0}
    } catch (e: Exception) {
        previousCoefficientOfSpreadAltitudeAndAltitudeGps
    }
}

fun calculateCorrectedAltitudeMns(lidar: AccuracyOfCoordinates, ign: AccuracyOfCoordinates, srtm: AccuracyOfCoordinates, buffer: List<GpsSample>, lastDifferentLidar: SpatialCoordinates?, lastDifferentIgn: SpatialCoordinates?, lastDifferentSrtm: SpatialCoordinates?): CoordinatesOrXNull {
    if(lidar.coordinates.alt != null)  {
        if (lastDifferentLidar == null || lastDifferentLidar.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeLidarHdMns, lidar.accuracy)
        }
        else {
            val pt0L = Coordinates(
                (lastDifferentLidar.lat),
                (lastDifferentLidar.lon)
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 40000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 40000)
            )
            val ptL = Coordinates(lidar.coordinates.lat, lidar.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (lidar.coordinates.alt!! - lastDifferentLidar.alt!!) + lastDifferentLidar.alt!!,
                lidar.accuracy
            )
        }
    }
    else if(ign.coordinates.alt != null)  {
        if (lastDifferentIgn == null || lastDifferentIgn.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeIgn, ign.accuracy)
        }
        else {
            val pt0L = Coordinates(
                lastDifferentIgn.lat,
                lastDifferentIgn.lon
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 20000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 20000)
            )
            val ptL = Coordinates(ign.coordinates.lat, ign.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (ign.coordinates.alt!! - lastDifferentIgn.alt!!) + lastDifferentIgn.alt!!,
                ign.accuracy
            )
        }
    }
    else if(srtm.coordinates.alt != null)  {
        if (lastDifferentSrtm == null || lastDifferentSrtm.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeSrtm, srtm.accuracy)
        }
        else {
            val pt0L = Coordinates(
                (lastDifferentSrtm.lat),
                (lastDifferentSrtm.lon)
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 5000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 5000)
            )
            val ptL = Coordinates(srtm.coordinates.lat, srtm.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (srtm.coordinates.alt!! - lastDifferentSrtm.alt!!) + lastDifferentSrtm.alt!!,
                srtm.accuracy
            )
        }
    }
    return CoordinatesOrXNull(
        null,
        999.9
    )
}

fun calculateCorrectedAltitudeMnt(lidar: AccuracyOfCoordinates, ign: AccuracyOfCoordinates, srtm: AccuracyOfCoordinates, buffer: List<GpsSample>, lastDifferentLidar: SpatialCoordinates?, lastDifferentIgn: SpatialCoordinates?, lastDifferentSrtm: SpatialCoordinates?): CoordinatesOrXNull {
    if(lidar.coordinates.alt != null)  {
        if (lastDifferentLidar == null || lastDifferentLidar.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeLidarHdMnt, lidar.accuracy)
        }
        else {
            val pt0L = Coordinates(
                (lastDifferentLidar.lat),
                (lastDifferentLidar.lon)
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 40000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 40000)
            )
            val ptL = Coordinates(lidar.coordinates.lat, lidar.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (lidar.coordinates.alt!! - lastDifferentLidar.alt!!) + lastDifferentLidar.alt!!,
                lidar.accuracy
            )
        }
    }
    else if(ign.coordinates.alt != null)  {
        if (lastDifferentIgn == null || lastDifferentIgn.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeIgn, ign.accuracy)
        }
        else {
            val pt0L = Coordinates(
                lastDifferentIgn.lat,
                lastDifferentIgn.lon
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 20000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 20000)
            )
            val ptL = Coordinates(ign.coordinates.lat, ign.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (ign.coordinates.alt!! - lastDifferentIgn.alt!!) + lastDifferentIgn.alt!!,
                ign.accuracy
            )
        }
    }
    else if(srtm.coordinates.alt != null)  {
        if (lastDifferentSrtm == null || lastDifferentSrtm.alt == null) {
            return CoordinatesOrXNull(buffer[buffer.size-2].altitudeSrtm, srtm.accuracy)
        }
        else {
            val pt0L = Coordinates(
                (lastDifferentSrtm.lat),
                (lastDifferentSrtm.lon)
            )
            val pt1L = Coordinates(
                roundToNearestStep(buffer[buffer.size - 2].latitude, 5000),
                roundToNearestStep(buffer[buffer.size - 2].longitude, 5000)
            )
            val ptL = Coordinates(srtm.coordinates.lat, srtm.coordinates.lon)
            return CoordinatesOrXNull(
                calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    pt0L,
                    pt1L,
                    ptL
                ) * (srtm.coordinates.alt!! - lastDifferentSrtm.alt!!) + lastDifferentSrtm.alt!!,
                srtm.accuracy
            )
        }
    }
    return CoordinatesOrXNull(
        null,
        999.9
    )
}


fun calculateDirectionBetweenTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return atan2(
        lat2 - lat1,
        (lon2 - lon1)*cos(lat1*PI/180)
    )
}

fun calculateDisplayedSpeed (oldestPoint: GpsSample, olderPoint: GpsSample,oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample, previousDisplayedSpeed: Double, newSpeed: Double): Double {
    val gps1 = oldPoint.speedGps
    val gps2 = newPoint.speedGps
    val gps3 = latestPoint.speedGps
    val s1 = if (gps1 == null && gps2 == null) {(gps3?:0.0).toDouble()}
        else if ((gps1?:0.0) == 0.0) max(0.0,min ((gps2?:0.0).toDouble(),(gps2?:0.0).toDouble()*1.5-(gps3?:0.0).toDouble()*0.5))
        else (gps1?:0.0).toDouble()
    val s2 = if (gps1 == null && gps3 == null) {(gps2?:0.0).toDouble()}
        else if ((gps2?:0.0) == 0.0) max(0.0,(gps1?:0.0).toDouble()/2.0+(gps3?:0.0).toDouble()/2.0)
        else (gps2?:0.0).toDouble()
    val s3 = if (gps3 == null && gps2 == null) {(gps1?:0.0).toDouble()}
        else if ((gps3?:0.0) == 0.0) max(0.0,min ((gps2?:0.0).toDouble(),(gps2?:0.0).toDouble()*1.5-(gps1?:0.0).toDouble()*0.5))
        else (gps1?:0.0).toDouble()
    val speedGps = max (s2,3.0*s2/5.0+s1/5.0+s3/5.0)
    val speedPoints = minOf(
        oldestPoint.gpsPointSpeed/4.0+olderPoint.gpsPointSpeed/4.0+oldPoint.gpsPointSpeed/4.0+newSpeed/4.0,
        olderPoint.gpsPointSpeed/3.0+oldPoint.gpsPointSpeed/3.0+newSpeed/2.0,
        oldPoint.gpsPointSpeed/2.0+newSpeed/2.0)
    return max (speedGps,speedPoints)
}

fun calculateDistanceBetweenTwoGpsPoints (lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return try {
        val x = (lon2 - lon1) * cos(lat1 * PI / 180) * 10000000.0 / 90
        val y = (lat2 - lat1) * 10000000.0 / 90
        sqrt(x * x + y * y)
        //sqrt((lat2 - lat1) * (lat2 - lat1) + ((lon2 - lon1) * (lon2 - lon1))*cos(lat1*PI/180)*cos(lat1*PI/180))*10000000.0/90.0
    } catch (e: Exception) {
        0.0
    }
}

fun calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1 (pt0: Coordinates, pt1: Coordinates, pt: Coordinates) :Double {
    fun deg2rad(d: Double) = d * PI / 180.0
    fun toLocalXY(lat: Double, lon: Double, lat0: Double, lon0: Double): Pair<Double, Double> {
        val latR = deg2rad(lat)
        val lonR = deg2rad(lon)
        val lat0R = deg2rad(lat0)
        val lon0R = deg2rad(lon0)

        val x = (lonR - lon0R) * cos((latR + lat0R) / 2.0)   // vers l’est
        val y = (latR - lat0R)                            // vers le nord
        return x to y
    }
    val (x2, y2) = toLocalXY(pt1.x, pt1.y, pt0.x, pt0.y)
    val (xp, yp) = toLocalXY(pt.x, pt.y, pt0.x, pt0.y)

    val vDotV = x2 * x2 + y2 * y2
    if (vDotV == 0.0) return 0.0 // p1 == p2 → éviter division par zéro

    // paramètre t du projeté ortho : P_proj = P1 + t * (P2 - P1)
    val t = (xp * x2 + yp * y2)/ vDotV

    return t
}


fun calculateDistanceToNewPointProjectedOnNewDir (newPoint: GpsSample, newDirection: Double, previousDirection: Double?, previousLatitude: Double?, previousLongitude: Double?): Double {
    val angle = calculateAngleBetweenTwoDirection(previousDirection, newDirection)
    val distance = calculateDistanceBetweenTwoGpsPoints(
        previousLatitude ?: newPoint.latitude,
        previousLongitude ?: newPoint.longitude,
        newPoint.latitude,
        newPoint.longitude
    )
    return distance * cos(angle)
}

/*
fun calculateExpectedSpeedDependingOnPreviousValuesAndNewAltitude(oldPoint: GpsSample, newPoint: GpsSample, latestGpsPoint: GpsSample, deltaTimeInSecond: Double, powerCorrection: Double, buffer: List<GpsSample>,
                                                                  previousSpeed: Double, previousVerticalSpeed: Double, previousAcceleration: Double, previousAltitude: Double?): Double {
    val deltaTime = if (deltaTimeInSecond < 1.0) deltaTimeInSecond else min(deltaTimeInSecond/2.0+0.5,2.0)
    val newAltitude = when (previousValues.altitudeSourceMntMnsOrGps) {
        "GPS" -> calculateAltitudeGps(oldPoint, newPoint, latestGpsPoint, previousIsAltitudeGot, previousSpreadAltitudeAndAltitudeGps))?: previousAltitude
        "MNS" -> simplifySampleAltitudeMns(newPoint, buffer)?: previousAltitude
        else -> simplifySampleAltitudeMnt(newPoint, buffer)?: previousAltitude
    }
    val deltaAltitude = if (previousAltitude == null || newAltitude == null) 0.0 else newAltitude - previousAltitude
    var previousPower = powerCorrection + previousSpeed * previousSpeed + 2.0 * previousSpeed + previousVerticalSpeed * 300.0 + 30.0 * previousSpeed * previousAcceleration
    if (previousPower < 0.0) previousPower = previousPower * 0.9
    if (previousPower > 1000.0) previousPower = 1000.0 + (previousPower - 1000.0) * 0.1
    val a = 1+(30/deltaTime)
    val b = 4-(30*previousSpeed/deltaTime)
    val c = 300*deltaAltitude/deltaTime - previousPower
    return try {
        val nSpeed=(-b + sqrt(b * b - 4 * a * c)) / (2 * a)
        if (nSpeed < previousSpeed/3) previousSpeed/3 else nSpeed
    } catch (e: Exception) {
        previousSpeed
    }
}
*/

fun calculateMiddleDirection (angle12: Double, angle13: Double, angle23: Double, direction1: Double, direction2: Double, direction3: Double): Double {
    if (maxOf(angle12, angle13, angle23)==angle12) return direction3
    if (maxOf(angle12, angle13, angle23)==angle13) return direction2
    return direction1
}

fun calculateNewAltitude (coordinates1: CoordinatesOrXNull, coordinates2: CoordinatesOrXNull, coordinates3: CoordinatesOrXNull, previousAltitude: Double?): Double? {
    val alt = if (coordinates1.x == null && coordinates2.x == null && coordinates3.x == null) previousAltitude
    else if (maxOf(coordinates1.y, coordinates2.y, coordinates3.y) == coordinates1.y) coordinates1.x
    else if (maxOf(coordinates1.y, coordinates2.y, coordinates3.y) == coordinates2.y) coordinates2.x
    else coordinates3.x

    if (alt==null) return alt
    return alt

}

fun calculateNewCumulatedGpsPrecision (newPoint: GpsSample, previousCumulatedGpsPrecision: Double): Double {
    return try {
        previousCumulatedGpsPrecision + 1 / (newPoint.accuracy)
    } catch (e: Exception) {
        previousCumulatedGpsPrecision
    }
}

fun calculateNewDirection (previousLatitude: Double?, previousLongitude: Double?, latestGpsPoint: GpsSample, newGpsPoint: GpsSample, previousGpsPoint: GpsSample, previousDirection: Double?): Double {
    val previousDirection = previousDirection
    if (previousDirection == null || previousLatitude == 0.0 || previousLongitude == 0.0 || previousLatitude == null || previousLongitude == null)
        return calculateDirectionBetweenTwoPoints(previousGpsPoint.latitude, previousGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude)
    val direction2 = calculateDirectionBetweenTwoPoints(previousLatitude, previousLongitude, latestGpsPoint.latitude, latestGpsPoint.longitude)
    val direction1 = calculateDirectionBetweenTwoPoints(previousLatitude, previousLongitude, newGpsPoint.latitude, newGpsPoint.longitude)
    if ( (calculateAngleBetweenTwoDirection (previousDirection, direction1)
                + calculateAngleBetweenTwoDirection (previousDirection, direction2)
                + calculateAngleBetweenTwoDirection (direction1, direction2)) < 6.28)
        return calculateMiddleDirection ( calculateAngleBetweenTwoDirection (previousDirection, direction1),
            calculateAngleBetweenTwoDirection (previousDirection, direction2),
            calculateAngleBetweenTwoDirection (direction1, direction2),
            previousDirection, direction1, direction2)
    return direction1
}

fun calculateNewHorizontalDistanceDone (
    previousLatitude: Double?, previousLongitude: Double?,
    oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample,
    deltaTimeInSecond: Double,
    newDirection: Double,
    speedCorrectionRatio: Double,
    minExpectedDistance: Double, expectedDistance: Double, maxExpectedDistance: Double,
    deltaAltitude: Double,
    previousDirection: Double?, previousSpeed: Double
): Double {
    /* fun calculateDistanceToNewPointProjectedOnNewDir (previousValues: ScreenValues, newPoint: GpsSample, newDirection: Double): Double {
         val angle = calculateAngleBetweenTwoDirection(previousValues.direction, newDirection)
         val distance = calculateDistanceBetweenTwoGpsPoints(
             previousValues.latitude ?: newPoint.latitude,
             previousValues.longitude ?: newPoint.longitude,
             newPoint.latitude,
             newPoint.longitude
         )
         return distance * cos(angle)
     }*/

    fun calculateDistanceDone (expectedDistance: Double, maxExpectedDistance: Double, minExpectedDistance: Double,
                               distanceToNewPointProjectedOnNewDir: Double,
                               newPoint: GpsSample,
                               speedCorrectionRatio: Double,
                               deltaAltitude: Double,
                               previousSpeed: Double
    ): Double {
        //val deltaAltitude = if (possibleNewAltitude == null || previousValues.altitude == null) 0.0 else possibleNewAltitude - previousValues.altitude
        val expectedMaxHorizontalDistance = max(
            sqrt(maxExpectedDistance * maxExpectedDistance - (deltaAltitude)*(deltaAltitude)),
            maxExpectedDistance/1.15
        )/speedCorrectionRatio
        val expectedHorizontalDistance = max(
            sqrt(expectedDistance * expectedDistance - (deltaAltitude)*(deltaAltitude)),
            expectedDistance/1.15,
        )/speedCorrectionRatio
        val expectedMinHorizontalDistance = max(
            sqrt(minExpectedDistance * minExpectedDistance - (deltaAltitude)*(deltaAltitude)),
            minExpectedDistance/1.15
        )/speedCorrectionRatio
        val gpsUncertainty = newPoint.accuracy.toDouble() + 0.05
        val functionForDistanceAccordingToGpsAccuracy = { x: Double ->
            1 / ((distanceToNewPointProjectedOnNewDir - x) * (distanceToNewPointProjectedOnNewDir - x) / (3.0 * gpsUncertainty * gpsUncertainty) + 1)
        }

        val functionForDistanceAccordingToGpsPoints = { x: Double ->
            val speed01 = calculateDistanceBetweenTwoGpsPoints(previousLatitude?: newPoint.latitude,
                previousLongitude?: newPoint.longitude,
                newPoint.latitude,
                newPoint.longitude)/deltaTimeInSecond
            val speed02 = calculateDistanceBetweenTwoGpsPoints(previousLatitude?: newPoint.latitude,
                previousLongitude?: newPoint.longitude,
                latestPoint.latitude,
                latestPoint.longitude)/(deltaTimeInSecond+(newPoint.timestamp-latestPoint.timestamp)/1000.0)
            val speed12 = calculateDistanceBetweenTwoGpsPoints (newPoint.latitude,
                latestPoint.latitude,
                newPoint.longitude,
                latestPoint.longitude)/((newPoint.timestamp-latestPoint.timestamp)/1000.0)
            val speed = (listOf(speed01,speed02,speed12).minByOrNull { abs(it-previousSpeed)  })?:previousSpeed
            val distance = speed * deltaTimeInSecond
            try { val unit = 1 / ((x - distance) * (x - distance) / ((expectedMaxHorizontalDistance * expectedMaxHorizontalDistance ) + 1))
                unit} catch (e: Exception) {previousSpeed*deltaTimeInSecond}
        }

        val functionForDistanceAccordingToExpectedDistance = { x: Double ->
            val unit = 1 / ((x - expectedHorizontalDistance)  * (x - expectedHorizontalDistance) / (expectedMaxHorizontalDistance  * expectedMaxHorizontalDistance ) + 1)
            unit
        }

        val functionForDistanceAccordingToGpsSpeed = { x: Double ->
            val speed = ((newPoint.speedGps)?:previousSpeed).toDouble()
            val uncertainty = ((newPoint.speedAccuracy)?:3.0).toDouble() + 0.03
            val unit = 1 / (((x-speed)*(x-speed)/(uncertainty*uncertainty))+1)
            unit*unit
        }

        val functionOfProbabilityAccordingToDistance = { x: Double -> functionForDistanceAccordingToGpsSpeed(x)*functionForDistanceAccordingToExpectedDistance(x) * functionForDistanceAccordingToGpsPoints (x) * functionForDistanceAccordingToGpsAccuracy (x) }

        val distanceDone = try {findMaxOfIncreasingThenDecreasingFunction(
            functionOfProbabilityAccordingToDistance,
            minExpectedDistance,maxExpectedDistance+1.0,50,0.01).x }
        catch (e: Exception) {
            if (previousLatitude!=null && previousLongitude!=null) {calculateDistanceBetweenTwoGpsPoints(previousLatitude, previousLongitude, newPoint.latitude, newPoint.longitude)}
            else 0.01
        }
        return distanceDone  // calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?: newPoint.latitude, previousValues.longitude?: newPoint.longitude, newPoint.latitude, newPoint.longitude)

    }

    val distanceToNewPointProjectedOnNewDir =
        calculateDistanceToNewPointProjectedOnNewDir(newPoint, newDirection, previousDirection, previousLatitude, previousLongitude)
    return calculateDistanceDone(expectedDistance, maxExpectedDistance, minExpectedDistance,
        distanceToNewPointProjectedOnNewDir,
        newPoint,
        speedCorrectionRatio,
        deltaAltitude,
        previousSpeed)
}

fun calculateNewLatitude ( previousLatitude: Double?,previousLongitude: Double?, newDirection: Double, newHorizontalDistanceDone: Double, newPoint: GpsSample, previousAltitude: Double?): Double {
    if (previousLatitude == null || previousLongitude == null) return newPoint.latitude
    return previousLatitude + newHorizontalDistanceDone * sin(newDirection) * 90.0 / (10000000.0 + ((previousAltitude)?:0.0))
}

fun calculateNewLongitude (previousLatitude: Double?, previousLongitude: Double?, newDirection: Double, newHorizontalDistanceDone: Double, newPoint: GpsSample, previousAltitude: Double?): Double {
    if (previousLatitude == null || previousLongitude == null) return newPoint.longitude
    return try {previousLongitude + newHorizontalDistanceDone * cos(newDirection) * 90.0 / ((10000000.0 + ((previousAltitude)?:0.0)) * cos(previousLatitude*PI/180))}
    catch (e: Exception) {
        newPoint.longitude
    }
}

fun calculateNewSlope (bufferSnapshot: List<GpsSample>, newVerticalSpeed: Double, newAltitude: Double?, newDisplayedSpeed:Double, altitudeProbability: Double): Double { try {
    val newTimestamp = bufferSnapshot[bufferSnapshot.size - 2].timestamp
    val previousSlope = bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointSlope
    val vSpeed23 = ((bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointVerticalSpeed)+ newVerticalSpeed)/2.0
    val vSpeed24 = ((bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointVerticalSpeed) + 2.0 * vSpeed23)/3.0
    val vSpeed25 = ((bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointVerticalSpeed) + 3.0 * vSpeed24)/4.0
    val vSpeed26 = ((bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointVerticalSpeed) + 4.0 * vSpeed25)/5.0
    val vSpeed27 = ((bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointVerticalSpeed) + 5.0 * vSpeed26)/6.0
    val vSpeed28 = ((bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointVerticalSpeed) + 6.0 * vSpeed27)/7.0
    val deltaAlt23 = (bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude?: (newAltitude?:0.0)) - (newAltitude?:0.0)
    val deltaAlt24 = deltaAlt23 + (bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude?: (bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude?:0.0)) - (bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitude?:0.0)
    val deltaAlt25 = deltaAlt24 + (bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude?: (bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude?:0.0)) - (bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitude?:0.0)
    val deltaAlt26 = deltaAlt25 + (bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude?: (bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude?:0.0)) - (bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitude?:0.0)
    val deltaAlt27 = deltaAlt26 + (bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude?: (bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude?:0.0)) - (bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitude?:0.0)
    val deltaAlt28 = deltaAlt27 + (bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointAltitude?: (bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude?:0.0)) - (bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitude?:0.0)
    val deltaT23 = ((bufferSnapshot[max(0,bufferSnapshot.size - 3)].timestamp) - (newTimestamp))/1000.0
    val deltaT24 = ((bufferSnapshot[max(0,bufferSnapshot.size - 4)].timestamp) - (newTimestamp))/1000.0
    val deltaT25 = ((bufferSnapshot[max(0,bufferSnapshot.size - 5)].timestamp) - (newTimestamp))/1000.0
    val deltaT26 = ((bufferSnapshot[max(0,bufferSnapshot.size - 6)].timestamp) - (newTimestamp))/1000.0
    val deltaT27 = ((bufferSnapshot[max(0,bufferSnapshot.size - 7)].timestamp) - (newTimestamp))/1000.0
    val deltaT28 = ((bufferSnapshot[max(0,bufferSnapshot.size - 8)].timestamp) - (newTimestamp))/1000.0
    val speed23 = ((bufferSnapshot[max(0, bufferSnapshot.size - 3)].gpsPointDisplayedSpeed) + newDisplayedSpeed) / 2.0
    val speed24 = ((bufferSnapshot[max(0, bufferSnapshot.size - 4)].gpsPointDisplayedSpeed) + 2.0 * speed23) / 3.0
    val speed25 = ((bufferSnapshot[max(0, bufferSnapshot.size - 5)].gpsPointDisplayedSpeed) + 3.0 * speed24) / 4.0
    val speed26 = ((bufferSnapshot[max(0, bufferSnapshot.size - 6)].gpsPointDisplayedSpeed) + 4.0 * speed25) / 5.0
    val speed27 = ((bufferSnapshot[max(0, bufferSnapshot.size - 7)].gpsPointDisplayedSpeed) + 5.0 * speed26) / 6.0
    val speed28 = ((bufferSnapshot[max(0, bufferSnapshot.size - 8)].gpsPointDisplayedSpeed) + 6.0 * speed27) / 7.0
    val hSpeed22 = try {sqrt(newDisplayedSpeed*newDisplayedSpeed-newVerticalSpeed*newVerticalSpeed)} catch (e: Exception) {newDisplayedSpeed}
    val hSpeed23 = sqrt(speed23 * speed23 - vSpeed23 * vSpeed23)
    val hSpeed24 = sqrt(speed24 * speed24 - vSpeed24 * vSpeed24)
    val hSpeed25 = sqrt(speed25 * speed25 - vSpeed25 * vSpeed25)
    val hSpeed26 = sqrt(speed26 * speed26 - vSpeed26 * vSpeed26)
    val hSpeed27 = sqrt(speed27 * speed27 - vSpeed27 * vSpeed27)
    val hSpeed28 = sqrt(speed28 * speed28 - vSpeed28 * vSpeed28)
    val slopeS22 = 100.0 * newVerticalSpeed / hSpeed22
    val slopeS23 = 100.0 * vSpeed23 / hSpeed23
    val slopeS24 = 100.0 * vSpeed24 / hSpeed24
    val slopeS25 = 100.0 * vSpeed25 / hSpeed25
    val slopeS26 = 100.0 * vSpeed26 / hSpeed26
    val slopeS27 = 100.0 * vSpeed27 / hSpeed27
    val slopeS28 = 100.0 * vSpeed28 / hSpeed28
    val slopeA23 = 100.0 * deltaAlt23 / (deltaT23 * hSpeed23)
    val slopeA24 = 100.0 * deltaAlt24 / (deltaT24 * hSpeed24)
    val slopeA25 = 100.0 * deltaAlt25 / (deltaT25 * hSpeed25)
    val slopeA26 = 100.0 * deltaAlt26 / (deltaT26 * hSpeed26)
    val slopeA27 = 100.0 * deltaAlt27 / (deltaT27 * hSpeed27)
    val slopeA28 = 100.0 * deltaAlt28 / (deltaT28 * hSpeed28)
    val minSpeed = minOf(hSpeed22,hSpeed23,hSpeed24,hSpeed25,hSpeed26,hSpeed27,hSpeed28)

    val hasGpsSource = bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointAltitudeSourceMntMnsOrGps == "GPS" ||
                        bufferSnapshot[max(0,bufferSnapshot.size - 4)].gpsPointAltitudeSourceMntMnsOrGps == "GPS" ||
                        bufferSnapshot[max(0,bufferSnapshot.size - 5)].gpsPointAltitudeSourceMntMnsOrGps == "GPS" ||
                        bufferSnapshot[max(0,bufferSnapshot.size - 6)].gpsPointAltitudeSourceMntMnsOrGps == "GPS" ||
                        bufferSnapshot[max(0,bufferSnapshot.size - 7)].gpsPointAltitudeSourceMntMnsOrGps == "GPS" ||
                        bufferSnapshot[max(0,bufferSnapshot.size - 8)].gpsPointAltitudeSourceMntMnsOrGps == "GPS"

    val percentToChange = if (altitudeProbability > 1.0 && minSpeed > 10.0) 0.5
                        else if (altitudeProbability > 0.6 && minSpeed > 6.0) 0.3
                        else if (altitudeProbability > 0.1 && minSpeed > 4.0) 0.2
                        else if (altitudeProbability > 0.01 && minSpeed > 2.0) 0.1
                        else if (altitudeProbability > 0.0001 && minSpeed > 1.0) 0.05
                        else if (altitudeProbability > 0.0000001 && minSpeed > 0.6) 0.03
                        else 0.02
    if (newDisplayedSpeed < 0.2 || hSpeed22 < 0.2) return previousSlope
    else if (hasGpsSource || newDisplayedSpeed < 0.4) {
        val bestSlope = ((listOf(
            slopeS22, slopeS23, slopeS24, slopeS25, slopeS26, slopeS27, slopeS28, slopeA23, slopeA24, slopeA25, slopeA26, slopeA27, slopeA28,
                    100 * vSpeed23/hSpeed24, 100 * vSpeed23/hSpeed25, 100 * vSpeed23/hSpeed26, 100 * vSpeed23/hSpeed27, 100 * vSpeed23/hSpeed28,
                    100 * vSpeed24/hSpeed25, 100 * vSpeed24/hSpeed26, 100 * vSpeed24/hSpeed27, 100 * vSpeed24/hSpeed28,
                    100 * vSpeed25/hSpeed26, 100 * vSpeed25/hSpeed27, 100 * vSpeed25/hSpeed28,
                    100 * vSpeed26/hSpeed27, 100 * vSpeed26/hSpeed28,
                    100 * vSpeed27/hSpeed28
        ).minByOrNull { abs(it - previousSlope / 1.01) }) ?: 0.0)
        return percentToChange * bestSlope + (1.0 - percentToChange) * previousSlope
    }
    else if (altitudeProbability > 2.0 && newDisplayedSpeed > 3.0) {
        val bestSlope = ((listOf(slopeS22, slopeS23, slopeS24, slopeS25, slopeA23, slopeA24, slopeA25,
                    100 * vSpeed23/hSpeed24, 100 * vSpeed23/hSpeed25,
                    100 * vSpeed24/hSpeed25
        ).minByOrNull { abs(it - previousSlope / 1.04) }) ?: 0.0)
        return percentToChange * bestSlope + (1.0 - percentToChange) * previousSlope
    }
    else if (altitudeProbability < 0.8 || newDisplayedSpeed < 2.0) {
        val bestSlope = ((listOf(slopeS22, slopeS23, slopeS24, slopeS25, slopeS26, slopeS27, slopeA23, slopeA24, slopeA25, slopeA26, slopeA27,
            100 * vSpeed23/hSpeed24, 100 * vSpeed23/hSpeed25, 100 * vSpeed23/hSpeed26, 100 * vSpeed23/hSpeed27,
            100 * vSpeed24/hSpeed25, 100 * vSpeed24/hSpeed26, 100 * vSpeed24/hSpeed27,
            100 * vSpeed25/hSpeed26, 100 * vSpeed25/hSpeed27,
            100 * vSpeed26/hSpeed27
        ).minByOrNull { abs(it - previousSlope / 1.02) }) ?: 0.0)
        return percentToChange * bestSlope + (1.0 - percentToChange) * previousSlope
    }
    else  {
        val bestSlope = ((listOf(slopeS22, slopeS23, slopeS24, slopeS25, slopeS26, slopeA23, slopeA24, slopeA25, slopeA26,
            100 * vSpeed23/hSpeed24, 100 * vSpeed23/hSpeed25, 100 * vSpeed23/hSpeed26,
            100 * vSpeed24/hSpeed25, 100 * vSpeed24/hSpeed26,
            100 * vSpeed25/hSpeed26
        ).minByOrNull { abs(it - previousSlope / 1.03) }) ?: 0.0)
        return percentToChange * bestSlope + (1.0 - percentToChange) * previousSlope
    }
    } catch (e: Exception) { return bufferSnapshot[max(0,bufferSnapshot.size - 3)].gpsPointSlope
         }
}

fun calculateNewSpeed(
    newHorizontalDistanceDone: Double,
    speedCorrectionRatio:Double,
    newAltitude: Double?,
    deltaTimeInSecond: Double,
    previousAltitude: Double?, previousSpeed: Double):Double {
    val deltaAltitude =
        if (newAltitude == null || previousAltitude == null) 0.0 else newAltitude - previousAltitude
    val distance =
        min(sqrt(newHorizontalDistanceDone * newHorizontalDistanceDone + deltaAltitude * deltaAltitude),1.5 * newHorizontalDistanceDone)
    return if (deltaTimeInSecond > 0.0)
        (distance / deltaTimeInSecond) * speedCorrectionRatio
    else previousSpeed
}

fun calculateSpeedCorrectionRatio(angleOldDirectionNewDirection: Double): Double {
    if (angleOldDirectionNewDirection == 0.0) return 1.0
    val angle = abs (min (angleOldDirectionNewDirection, PI))
    try {
        val a = (angle / (2*sin(angle/2)))
        return a*a*a
    } catch (e: Exception) {
        return 1.0
    }
}

fun calculateSpreadAltitudeAndAltitudeGps (oldPoint: GpsSample, buffer: List<GpsSample>, previousAltitudeSourceMntMnsOrGps: String, previousAltitude: Double?, previousSpreadAltitudeAndAltitudeGps: Double?, previousCoefficientOfSpreadAltitudeAndAltitudeGps: Double): Double? {
    val previousAltitude = if (previousAltitudeSourceMntMnsOrGps == "MNT")
        simplifySampleAltitudeMnt(oldPoint, buffer)?: previousAltitude
    else simplifySampleAltitudeMns(oldPoint, buffer)?: previousAltitude
    if (previousAltitudeSourceMntMnsOrGps == "GPS"
        || previousAltitude == null || oldPoint.altitudeGps == null) return previousSpreadAltitudeAndAltitudeGps
    val previousSpread = previousSpreadAltitudeAndAltitudeGps?:0.0
    val altGps = oldPoint.altitudeGps!!
    if ((oldPoint.altitudeAccuracy == null || oldPoint.altitudeAccuracy == 0.0f ))
    {
        return previousSpreadAltitudeAndAltitudeGps?:(altGps-previousAltitude)
    }
    val accGps = (oldPoint.altitudeAccuracy!!).toDouble()
    return try {
        (previousSpread * previousCoefficientOfSpreadAltitudeAndAltitudeGps + (altGps - previousAltitude) / (accGps + 0.1)) / (previousCoefficientOfSpreadAltitudeAndAltitudeGps + 1.0 / (accGps + 0.1))
    } catch (e: Exception) {
        previousSpread
    }
}

fun calculateUncorrectedGpsAltitude (oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample, previousUncorrectedGpsAltitude: Double?): Double? {
    val previous = previousUncorrectedGpsAltitude ?: return if (newPoint.altitudeGps != null) newPoint.altitudeGps else latestPoint.altitudeGps
    if (newPoint.altitudeGps == null && latestPoint.altitudeGps == null && oldPoint.altitudeGps == null) return previous
    val smallestGap = minOf(
        abs((newPoint.altitudeGps ?: 9999.9) - previous),
        abs((oldPoint.altitudeGps ?: 9999.9) - previous),
        abs((latestPoint.altitudeGps ?: 9999.9) - previous)
    )
    val nearestPoint = when (smallestGap) {
        abs((newPoint.altitudeGps ?: 9999.9) - previous) -> newPoint
        abs((oldPoint.altitudeGps ?: 9999.9) - previous) -> oldPoint
        else -> latestPoint
    }
    val higherPrecision = (minOf(
        newPoint.altitudeAccuracy ?: 9999.9f,
        oldPoint.altitudeAccuracy ?: 9999.9f,
        latestPoint.altitudeAccuracy ?: 9999.9f
    )).toDouble()
    return try {
        previous + ((nearestPoint.altitudeGps
            ?: previous) - previous) / (1.0 * higherPrecision + 0.00005 * (nearestPoint.altitudeAccuracy
            ?: 9999.9f) + 0.1) //on peut diviser par un diviseur (1.0 et 0.5 et 0.1) encore plus grand poir faire changer moins vite

    } catch (e: Exception) {
        previous
    }
}

fun findLastAltitudeIgn (buffer: List<GpsSample>, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): AccuracyOfCoordinates {
    var n = 2
    try {
        while (n < buffer.size) {
            val it = buffer[buffer.size - n]
            if (it.altitudeIgn != null) return AccuracyOfCoordinates(
                SpatialCoordinates(it.longitude, it.latitude,it.altitudeIgn!! + previousVerticalSpeed * (buffer[buffer.size-2].timestamp - it.timestamp) / 1000.0),
                calculateAccuracyAltitudeIgn(it, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed) + (n-2) * (buffer[buffer.size-2].timestamp - it.timestamp) / 10000.0,
            )
            n++
        }
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    } catch (e: Exception) {
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    }
}

fun findLastAltitudeMnt (buffer: List<GpsSample>, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): AccuracyOfCoordinates {
    var n = 2
    try {
            while (n < buffer.size) {
                val it = buffer[buffer.size - n]
                if (it.altitudeLidarHdMnt != null) return AccuracyOfCoordinates(
                    SpatialCoordinates(it.longitude, it.latitude,it.altitudeLidarHdMnt!! + (previousVerticalSpeed * (buffer[buffer.size-2].timestamp - it.timestamp) / 1000.0)),
                    calculateAccuracyAltitudeMntMns(it, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed) + (n-2) * (buffer[buffer.size-2].timestamp - it.timestamp) / 10000.0,
                )
                n++
            }
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    } catch (e: Exception) {
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    }
}

fun findLastAltitudeMns (buffer: List<GpsSample>, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): AccuracyOfCoordinates {
    var n = 2
    try {
        while (n < buffer.size) {
            val it = buffer[buffer.size - n]
            if (it.altitudeLidarHdMns != null) return AccuracyOfCoordinates(
                SpatialCoordinates(it.longitude, it.latitude,it.altitudeLidarHdMns!! + previousVerticalSpeed * (buffer[buffer.size-2].timestamp - it.timestamp) / 1000.0),
                calculateAccuracyAltitudeMntMns(it, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed) + (n-2) * (buffer[buffer.size-2].timestamp - it.timestamp) / 10000.0,
            )
            n++
        }
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    } catch (e: Exception) {
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    }
}

fun findLastAltitudeSrtm (buffer: List<GpsSample>, previousDurationAltitudeNotMnt: Int, previousVerticalSpeed: Double, previousSpeed: Double): AccuracyOfCoordinates {
    var n = 2
    try {
        while (n < buffer.size) {
            val it = buffer[buffer.size - n]
            if (it.altitudeSrtm != null) return AccuracyOfCoordinates(
                SpatialCoordinates(it.longitude, it.latitude,it.altitudeSrtm!! + previousVerticalSpeed * (buffer[buffer.size-2].timestamp - it.timestamp) / 1000.0),
                calculateAccuracyAltitudeSrtm(it, previousDurationAltitudeNotMnt, previousVerticalSpeed, previousSpeed) + (n-2) * (buffer[buffer.size-2].timestamp - it.timestamp) / 10000.0,
            )
            n++
        }
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    } catch (e: Exception) {
        return AccuracyOfCoordinates(SpatialCoordinates(buffer[buffer.size - 2].longitude, buffer[buffer.size - 2].latitude, null), 999.9)
    }
}

fun findMaxOfIncreasingThenDecreasingFunction(
    f: (Double) -> Double,
    start: Double,
    end: Double,
    numberOfStage: Int,
    precision: Double
): Coordinates {

    require(end > start)
    require(numberOfStage >= 3)
    require(precision > 0)

    var left = start
    var right = end

    while (right - left > precision) {

        val step = (right - left) / numberOfStage

        val values = DoubleArray(numberOfStage + 1)

        for (i in 0..numberOfStage) {
            val x = left + i * step
            values[i] = f(x)
        }

        var bestIndex = 0

        for (i in 1..numberOfStage) {
            if (values[i] > values[bestIndex]) {
                bestIndex = i
            }
        }

        val newLeftIndex = maxOf(0, bestIndex - 1)
        val newRightIndex = minOf(numberOfStage, bestIndex + 1)

        left += newLeftIndex * step
        right = left + (newRightIndex - newLeftIndex) * step
    }

    val xMax = (left + right) / 2

    return Coordinates(
        x = xMax,
        y = f(xMax)
    )
}

/*   f: (Double) -> Double,
start: Double,
end: Double,
precision: Double
): Coordinates {
require(precision > 0) { "precision must be positive" }
require(end > start) { "end must be greater than start" }

var left = start
var right = end

// boucle de recherche ternaire
while (right - left > precision) {
    val m1 = left + (right - left) / 3
    val m2 = right - (right - left) / 3
    val f1 = f(m1)
    val f2 = f(m2)

    if (f1 < f2) {
        // maximum est à droite
        left = m1
    } else {
        // maximum est à gauche
        right = m2
    }
}

val xMax = (left + right) / 2
val yMax = f(xMax)
return Coordinates(xMax, yMax)
}
*/
/*
fun screenValuesWhenIsMovingBecomeWrong (previousValues: ScreenValues, gpsPt: GpsSample, buffer: List<GpsSample>) : ScreenValues {
    return screenValuesWhenIsMovingIsWrong (previousValues, gpsPt, buffer)
    // à remplacer notament en changeant latitude et longitude et altitude
}*/
/*
fun screenValuesWhenIsMovingIsWrong (previousValues: ScreenValues, gpsPt: GpsSample, buffer: List<GpsSample>) : ScreenValues {
    val pt1 = buffer[buffer.size - 3]
    val pt2 = buffer[buffer.size - 2]
    val pt3 = buffer[buffer.size - 1]
    return ScreenValues(
        screenValueDouble1 = pt3.altitudeGps,
        screenValueDouble2 = pt3.altitudeAccuracy?.toDouble(),
        screenValueDouble3 = previousValues.cumulatedGpsPrecision,
        screenValueLong = 0,
        screenValueString = "moving wrong",
        screenValueString2 = "alt mnt brute",
        screenValueString3 = "alt gps brute",
        screenValueString4 = "alt mns brute ",
        screenValueInt = buffer.size,

        screenValueBoolean = previousValues.isStarted,
        isStarted = true,
        cumulatedGpsPrecision = calculateNewCumulatedGpsPrecision(gpsPt, previousCumulatedGpsPrecision),// = previousValues.cumulatedGpsPrecision,
        acceleration = 0.0,
        verticalSpeed = 0.0,
        direction = previousValues.direction,
        speed = 0.0,
        altitude = previousValues.altitude,
        latitude = previousValues.latitude,
        longitude = previousValues.longitude,
        time = gpsPt.timestamp,
        spreadAltitudeAndAltitudeGps = previousValues.spreadAltitudeAndAltitudeGps,
        coefficientOfSpreadAltitudeAndAltitudeGps = previousValues.coefficientOfSpreadAltitudeAndAltitudeGps,
        durationAltitudeNotMnt = previousValues.durationAltitudeNotMnt,
        gpsAltitudeAccuracy = calculateGpsAltitudeAccuracy(pt3),
        altitudeSourceMntMnsOrGps = previousValues.altitudeSourceMntMnsOrGps,
        isMoving = false,
        isAltitudeGot = previousValues.isAltitudeGot || simplifySampleAltitudeMnt(gpsPt, buffer) != null,
        uncorrectedGpsAltitude = previousValues.uncorrectedGpsAltitude,
        isGpsAltitudeGot = previousValues.isGpsAltitudeGot || gpsPt.altitudeGps != null,
        displayedVerticalSpeed = 0.0,
        displayedAltitude = previousValues.displayedAltitude

    )
}
*/
fun simplifySampleAltitudeMnt(sample: GpsSample, buffer: List<GpsSample>):Double? {

    fun calculateAltitudeForSample(sample: GpsSample, buffer: List<GpsSample>, precisionForNearestStepSrtm: Int, precisionForNearestStepLidar: Int, precisionForNearestStepIgn: Int): Double? {
        val sampleIgnAltitude = sample.altitudeIgn
        val sampleSrtmAltitude = sample.altitudeSrtm
        val sampleMntAltitude = sample.altitudeLidarHdMnt

        fun findLastPointFarFromSampleForIgn(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n <= buffer.size) {

                    val it = buffer[buffer.size - n]

                    val roundedLon = roundToNearestStep(
                        it.longitude,
                        precisionForNearestStep
                    )

                    val roundedLat = roundToNearestStep(
                        it.latitude,
                        precisionForNearestStep
                    )

                    if (
                        abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1)
                        > 0.5 / precisionForNearestStep
                        &&
                        (it.altitudeIgn ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat
                        coord.lon = roundedLon
                        coord.alt = it.altitudeIgn
                        return coord
                    }
                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        fun findLastPointFarFromSampleForLidar(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n < buffer.size) {
                    val it = buffer[buffer.size - n]

                    val roundedLon = roundToNearestStep(
                        it.longitude,
                        precisionForNearestStep
                    )

                    val roundedLat = roundToNearestStep(
                        it.latitude,
                        precisionForNearestStep
                    )

                    if (abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1) > 0.5 / precisionForNearestStep && (it.altitudeLidarHdMnt
                            ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat; coord.lon = roundedLon; coord.alt =
                            it.altitudeLidarHdMnt; return coord
                    }

                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        fun findLastPointFarFromSampleForSrtm(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n < buffer.size) {
                    val it = buffer[buffer.size - n]

                    val roundedLon = roundToNearestStep(it.longitude, precisionForNearestStep)
                    val roundedLat = roundToNearestStep(it.latitude, precisionForNearestStep)
                    if (abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1) > 0.5 / precisionForNearestStep && (it.altitudeSrtm
                            ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat; coord.lon = roundedLon; coord.alt =
                            it.altitudeSrtm; return coord
                    }

                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        val latPt1Ign = roundToNearestStep(sample.latitude, precisionForNearestStepIgn)
        val lonPt1Ign = roundToNearestStep(sample.longitude, precisionForNearestStepIgn)
        val pt0Ign = findLastPointFarFromSampleForIgn(
            buffer,
            latPt1Ign,
            lonPt1Ign,
            sampleIgnAltitude,
            precisionForNearestStepIgn
        )
        val latPt0Ign = pt0Ign.lat
        val lonPt0Ign = pt0Ign.lon
        val altPt0Ign = pt0Ign.alt
        val latPt1Srtm = roundToNearestStep(sample.latitude, precisionForNearestStepSrtm)
        val lonPt1Srtm = roundToNearestStep(sample.longitude, precisionForNearestStepSrtm)
        val pt0Srtm = findLastPointFarFromSampleForSrtm(
            buffer,
            latPt1Srtm,
            lonPt1Srtm,
            sampleSrtmAltitude,
            precisionForNearestStepSrtm
        )
        val latPt0Srtm = pt0Srtm.lat
        val lonPt0Srtm = pt0Srtm.lon
        val altPt0Srtm = pt0Srtm.alt
        val latPt1Lidar = roundToNearestStep(sample.latitude, precisionForNearestStepLidar)
        val lonPt1Lidar = roundToNearestStep(sample.longitude, precisionForNearestStepLidar)
        val pt0Lidar = findLastPointFarFromSampleForLidar(
            buffer,
            latPt1Lidar,
            lonPt1Lidar,
            sampleMntAltitude,
            precisionForNearestStepLidar
        )
        val latPt0Lidar = pt0Lidar.lat
        val lonPt0Lidar = pt0Lidar.lon
        val altPt0Lidar = pt0Lidar.alt
        val ign =
            if ((latPt0Ign == latPt1Ign && lonPt0Ign == lonPt1Ign) || altPt0Ign == null || sampleIgnAltitude == null) sampleIgnAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Ign, lonPt0Ign),
                    Coordinates(latPt1Ign, lonPt1Ign), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Ign + (sampleIgnAltitude - altPt0Ign) * ratio
            } catch (e: Exception) {
                null
            }
        val srtm =
            if ((latPt0Srtm == latPt1Srtm && lonPt0Srtm == lonPt1Srtm) || altPt0Srtm == null || sampleSrtmAltitude == null) sampleSrtmAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Srtm, lonPt0Srtm),
                    Coordinates(latPt1Srtm, lonPt1Srtm), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Srtm + (sampleSrtmAltitude - altPt0Srtm) * ratio
            } catch (e: Exception) {
                null
            }
        val mnt =
            if ((latPt0Lidar == latPt1Lidar && lonPt0Lidar == lonPt1Lidar) || altPt0Lidar == null || sampleMntAltitude == null) sampleMntAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Lidar, lonPt0Lidar),
                    Coordinates(latPt1Lidar, lonPt1Lidar), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Lidar + (sampleMntAltitude - altPt0Lidar) * ratio
            } catch (e: Exception) {
                null
            }
        try {
            if (mnt == null && ign == null && srtm == null) {
                return null
            }
            val nbMnt = if (mnt == null) 0.0 else 1.0
            val nbIgn = if (ign == null) 0.0 else 1.0
            val nbSrtm = if (srtm == null) 0.0 else 1.0
            return ((mnt ?: 0.0) * 1000000 + (ign ?: 0.0) * 1000 + (srtm
                ?: 0.0)) / (nbMnt * 1000000 + nbIgn * 1000 + nbSrtm)
        } catch (e: Exception) {
            return null
        }
    }
    return calculateAltitudeForSample(sample, buffer, 5000, 40000, 20000)
}

fun simplifySampleAltitudeMns(sample: GpsSample, buffer: List<GpsSample>):Double? {

    fun calculateAltitudeForSample(sample: GpsSample, buffer:List<GpsSample>, precisionForNearestStepSrtm: Int, precisionForNearestStepLidar: Int, precisionForNearestStepIgn: Int): Double? {
        val sampleIgnAltitude = sample.altitudeIgn
        val sampleSrtmAltitude = sample.altitudeSrtm
        val sampleMnsAltitude = sample.altitudeLidarHdMns

        fun findLastPointFarFromSampleForIgn(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n < buffer.size) {
                    val it = buffer[buffer.size - n]
                    val roundedLon = roundToNearestStep(it.longitude, precisionForNearestStep)
                    val roundedLat = roundToNearestStep(it.latitude, precisionForNearestStep)
                    if (abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1) > 0.5 / precisionForNearestStep && (it.altitudeIgn
                            ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat; coord.lon = roundedLon; coord.alt =
                            it.altitudeIgn; return coord
                    }

                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        fun findLastPointFarFromSampleForLidar(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n < buffer.size) {
                    val it = buffer[buffer.size - n]
                    val roundedLon = roundToNearestStep(it.longitude, precisionForNearestStep)
                    val roundedLat = roundToNearestStep(it.latitude, precisionForNearestStep)
                    if (abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1) > 0.5 / precisionForNearestStep && (it.altitudeLidarHdMns
                            ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat; coord.lon = roundedLon; coord.alt =
                            it.altitudeLidarHdMns; return coord
                    }

                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        fun findLastPointFarFromSampleForSrtm(
            buffer: List<GpsSample>,
            latPt1: Double,
            lonPt1: Double,
            altPt1: Double?,
            precisionForNearestStep: Int
        ): SpatialCoordinates {
            var n = 2
            val coord = SpatialCoordinates(lonPt1, latPt1, altPt1)
            try {
                while (n < buffer.size) {
                    val it = buffer[buffer.size - n]
                    val roundedLon = roundToNearestStep(it.longitude, precisionForNearestStep)
                    val roundedLat = roundToNearestStep(it.latitude, precisionForNearestStep)
                    if (abs(roundedLon - lonPt1)
                        + abs(roundedLat - latPt1) > 0.5 / precisionForNearestStep && (it.altitudeSrtm
                            ?: -999.9) > -99
                    ) {
                        coord.lat = roundedLat; coord.lon = roundedLon; coord.alt =
                            it.altitudeSrtm; return coord
                    }

                    n++
                }
                return coord
            } catch (e: Exception) {
                return coord
            }
        }

        val latPt1Ign = roundToNearestStep(sample.latitude, precisionForNearestStepIgn)
        val lonPt1Ign = roundToNearestStep(sample.longitude, precisionForNearestStepIgn)
        val pt0Ign = findLastPointFarFromSampleForIgn(
            buffer,
            latPt1Ign,
            lonPt1Ign,
            sampleIgnAltitude,
            precisionForNearestStepIgn
        )
        val latPt0Ign = pt0Ign.lat
        val lonPt0Ign = pt0Ign.lon
        val altPt0Ign = pt0Ign.alt
        val latPt1Srtm = roundToNearestStep(sample.latitude, precisionForNearestStepSrtm)
        val lonPt1Srtm = roundToNearestStep(sample.longitude, precisionForNearestStepSrtm)
        val pt0Srtm = findLastPointFarFromSampleForSrtm(
            buffer,
            latPt1Srtm,
            lonPt1Srtm,
            sampleSrtmAltitude,
            precisionForNearestStepSrtm
        )
        val latPt0Srtm = pt0Srtm.lat
        val lonPt0Srtm = pt0Srtm.lon
        val altPt0Srtm = pt0Srtm.alt
        val latPt1Lidar = roundToNearestStep(sample.latitude, precisionForNearestStepLidar)
        val lonPt1Lidar = roundToNearestStep(sample.longitude, precisionForNearestStepLidar)
        val pt0Lidar = findLastPointFarFromSampleForLidar(
            buffer,
            latPt1Lidar,
            lonPt1Lidar,
            sampleMnsAltitude,
            precisionForNearestStepLidar
        )
        val latPt0Lidar = pt0Lidar.lat
        val lonPt0Lidar = pt0Lidar.lon
        val altPt0Lidar = pt0Lidar.alt
        val ign =
            if ((latPt0Ign == latPt1Ign && lonPt0Ign == lonPt1Ign) || altPt0Ign == null || sampleIgnAltitude == null) sampleIgnAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Ign, lonPt0Ign),
                    Coordinates(latPt1Ign, lonPt1Ign), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Ign + (sampleIgnAltitude - altPt0Ign) * ratio
            } catch (e: Exception) {
                null
            }
        val srtm =
            if ((latPt0Srtm == latPt1Srtm && lonPt0Srtm == lonPt1Srtm) || altPt0Srtm == null || sampleSrtmAltitude == null) sampleSrtmAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Srtm, lonPt0Srtm),
                    Coordinates(latPt1Srtm, lonPt1Srtm), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Srtm + (sampleSrtmAltitude - altPt0Srtm) * ratio
            } catch (e: Exception) {
                null
            }
        val mns =
            if ((latPt0Lidar == latPt1Lidar && lonPt0Lidar == lonPt1Lidar) || altPt0Lidar == null || sampleMnsAltitude == null) sampleMnsAltitude
            else try {
                val ratio = calculateDistanceRatioBetweenPt0andProjectedPtAndPt0andPt1(
                    Coordinates(latPt0Lidar, lonPt0Lidar),
                    Coordinates(latPt1Lidar, lonPt1Lidar), Coordinates(
                        sample.latitude,
                        sample.longitude
                    )
                )
                altPt0Lidar + (sampleMnsAltitude - altPt0Lidar) * ratio
            } catch (e: Exception) {
                null
            }
        try {
            if (mns == null && ign == null && srtm == null) {
                return null
            }
            val nbMnt = if (mns == null) 0.0 else 1.0
            val nbIgn = if (ign == null) 0.0 else 1.0
            val nbSrtm = if (srtm == null) 0.0 else 1.0
            return ((mns ?: 0.0) * 1000000 + (ign ?: 0.0) * 1000 + (srtm
                ?: 0.0)) / (nbMnt * 1000000 + nbIgn * 1000 + nbSrtm)
        } catch (e: Exception) {
            return null
        }
    }
    return calculateAltitudeForSample(sample, buffer, 5000, 40000, 20000)
}

fun startNewScreenValues (buffer: List<GpsSample>){

}