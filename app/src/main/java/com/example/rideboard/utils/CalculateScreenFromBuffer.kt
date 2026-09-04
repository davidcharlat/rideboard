package com.example.rideboard.utils

import android.annotation.SuppressLint
import android.content.Context
import com.example.rideboard.buffer.GpsBuffer
import com.example.rideboard.buffer.GpsSample
import com.example.rideboard.config.AppConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun calculateScreenValues(buffer: GpsBuffer,
): ScreenValues {
    val point = buffer.getNthBeforeLast(2) ?: return ScreenValues()
    return ScreenValues(
        screenValueDouble1 = point.gpsPointScreenValueDouble1,
        screenValueDouble2 = point.gpsPointScreenValueDouble2,
        screenValueDouble3 = point.gpsPointScreenValueDouble3,
        screenValueLong = point.gpsPointScreenValueLong,
        screenValueString = point.gpsPointScreenValueString,
        screenValueString2 = point.gpsPointScreenValueString2,
        screenValueString3 = point.gpsPointScreenValueString3,
        screenValueString4 = point.gpsPointScreenValueString4,
        screenValueBoolean = point.gpsPointScreenValueBoolean,
        screenValueInt = point.gpsPointScreenValueInt,
        isStarted = point.gpsPointIsStarted,
        cumulatedGpsPrecision = point.gpsPointCumulatedGpsPrecision,
        acceleration = point.gpsPointAcceleration,
        verticalSpeed = point.gpsPointDisplayedVerticalSpeed2,
        maxVerticalSpeed = point.gpsPointMaxVerticalSpeed,
        minVerticalSpeed = point.gpsPointMinVerticalSpeed,
        direction = point.gpsPointDirection,
        directionString = if (point.gpsPointDirection == null) " "
                else if(point.gpsPointDirection!! < -3.04) " W    "
                else if(point.gpsPointDirection!! < -2.845) "W-wsw"
                else if(point.gpsPointDirection!! < -2.65) "WSW   "
                else if(point.gpsPointDirection!! < -2.453) "SW-wsw"
                else if(point.gpsPointDirection!! < -2.256) "SW   "
                else if(point.gpsPointDirection!! < -2.059) "SW-ssw"
                else if(point.gpsPointDirection!! < -1.864) "SSW   "
                else if(point.gpsPointDirection!! < -1.668) "S-ssw"
                else if(point.gpsPointDirection!! < -1.472) "S     "
                else if(point.gpsPointDirection!! < -1.276) "S-sse"
                else if(point.gpsPointDirection!! < -1.081) "SSE   "
                else if(point.gpsPointDirection!! < -0.883) "SE-sse"
                else if(point.gpsPointDirection!! < -0.687) "SE   "
                else if(point.gpsPointDirection!! < -0.491) "SE-ese"
                else if(point.gpsPointDirection!! < -0.295) "ESE   "
                else if(point.gpsPointDirection!! < -0.099) "E-ese"
                else if(point.gpsPointDirection!! < 0.099) " E    "
                else if(point.gpsPointDirection!! < 0.295) "E-ene"
                else if(point.gpsPointDirection!! < 0.491) "ENE   "
                else if(point.gpsPointDirection!! < 0.687) "NE-ene"
                else if(point.gpsPointDirection!! < 0.883) "NE   "
                else if(point.gpsPointDirection!! < 1.081) "NE-nne"
                else if(point.gpsPointDirection!! < 1.276) "NNE   "
                else if(point.gpsPointDirection!! < 1.472) "N-nne"
                else if(point.gpsPointDirection!! < 1.668) " N    "
                else if(point.gpsPointDirection!! < 1.864) "N-nnw"
                else if(point.gpsPointDirection!! < 2.059) "NNW   "
                else if(point.gpsPointDirection!! < 2.256) "NW-nnw"
                else if(point.gpsPointDirection!! < 2.453) "NW   "
                else if(point.gpsPointDirection!! < 2.65) "NW-wnw"
                else if(point.gpsPointDirection!! < 2.845) "WNW   "
                else if(point.gpsPointDirection!! < 3.04) "W-wnw"
                else " W    ",
        speed = point.gpsPointDisplayedSpeed,
        altitude = point.gpsPointDisplayedAltitude,
        latitude = point.gpsPointLatitude,
        longitude = point.gpsPointLongitude,
        time = point.timestamp,
        spreadAltitudeAndAltitudeGps = point.gpsPointSpreadAltitudeAndAltitudeGps,
        coefficientOfSpreadAltitudeAndAltitudeGps = point.gpsPointCoefficientOfSpreadAltitudeAndAltitudeGps,
        durationAltitudeNotMnt = point.gpsPointDurationAltitudeNotMnt,
        gpsAltitudeAccuracy = point.gpsPointGpsAltitudeAccuracy,
        altitudeSourceMntMnsOrGps = point.gpsPointAltitudeSourceMntMnsOrGps,
        isMoving = point.gpsPointIsMoving,
        isAltitudeGot = point.gpsPointIsAltitudeGot,
        isGpsAltitudeGot = point.gpsPointIsGpsAltitudeGot,
        uncorrectedGpsAltitude = point.gpsPointUncorrectedGpsAltitude,
        displayedAltitude = point.gpsPointDisplayedAltitude,
        displayedVerticalSpeed = point.gpsPointDisplayedVerticalSpeed,
        durationHighSpreadAltitude = point.gpsPointDurationHighSpreadAltitude,
        verticalSpeed4 = point.screenVerticalSpeed4,
        verticalSpeed15 = point.screenVerticalSpeed15,
        verticalSpeed125 = point.screenVerticalSpeed125,
        verticalSpeed1000 = point.screenVerticalSpeed1000,
        maxVerticalSpeed15 = point.maxVerticalSpeed15,
        maxVerticalSpeed125 = point.maxVerticalSpeed125,
        maxVerticalSpeed1000 = point.maxVerticalSpeed1000,

        maxSpeed = point.gpsPointMaxSpeed,
        averageSpeed = if (point.partialDurationTimeForAverageSpeed > 2000L) {
            1000.0*point.partialDistanceForAverageSpeed/(point.partialDurationTimeForAverageSpeed).toDouble()
            }
            else 0.0,
        durationSeconds = (point.partialDurationTime + 300)/1000,
        distance = point.partialDistance,
        elevationGain = point.partialElevationGain,
        slope = point.gpsPointSlope,
        minSlope = point.gpsPointMinSlope,
        maxSlope = point.gpsPointMaxSlope,
        minAltitude = point.gpsPointMinAltitude,
        maxAltitude = point.gpsPointMaxAltitude,
    )


    /*
        return ScreenValues(

        altitude = newAltitude,
        latitude = newLatitude,
        longitude = newLongitude,
        time = newGpsPoint.timestamp,
        spreadAltitudeAndAltitudeGps = calculateSpreadAltitudeAndAltitudeGps (previousValues, previousGpsPoint, bufferSnapshot),
        coefficientOfSpreadAltitudeAndAltitudeGps = calculateCoefficientOfSpreadAltitudeAndAltitudeGps (previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint),
        durationAltitudeNotMnt = newDurationAltitudeNotMnt,
        gpsAltitudeAccuracy = newGpsAltitudeAccuracy,
        altitudeSourceMntMnsOrGps = newAltitudeSourceMntMnsOrGps,
        isMoving = newIsMoving,
        isAltitudeGot = newAltitude!=null,
        isGpsAltitudeGot = newIsGpsAltitudeGot,
        uncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint),
        displayedAltitude = newAltitude,
        displayedVerticalSpeed = newVerticalSpeed/2.0 + previousValues.verticalSpeed/2.0,
        durationHighSpreadAltitude = newDurationHighSpreadAltitude
    )
     */

}
/*
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

fun calculateScreenValues(
    context: Context,
    buffer: GpsBuffer,
    previousValues: ScreenValues = ScreenValues()
): ScreenValues {
    if (buffer.size < 3) return previousValues

    //pour voir le temps de calcul:
    val start = System.currentTimeMillis()

    //cree un header dans gps_debug.txt
    if (buffer.size == 3) (appendDebugLog(
        context,
        "time\taltitude\taltbruteMNT\tAccuracyMNT\taltoptMNT\tprobaMNT\talt source\taltbruteGPS\tprobaGPS\tprobamnt\trequetes Lidar\trequetes Srtm\ttemps de calcul\t\t\t\tspeed"
    ))
    //fin de création du header
    val bufferSnapshot = buffer.getAll()
    val newGpsPoint = bufferSnapshot[bufferSnapshot.size - 2]
    val previousGpsPoint = bufferSnapshot[bufferSnapshot.size - 3]
    val latestGpsPoint = bufferSnapshot[bufferSnapshot.size - 1]
    val deltaTimeInSecond = (newGpsPoint.timestamp - previousValues.time).toDouble() / 1000.0
    val newCumulatedGpsPrecision = calculateNewCumulatedGpsPrecision (newGpsPoint, previousValues)
    val newIsStarted = previousValues.isStarted
    if (!newIsStarted && newCumulatedGpsPrecision >1) return startNewScreenValues (bufferSnapshot, previousValues)
    if (!newIsStarted) //return previousValues
        return ScreenValues(
            screenValueDouble1 = 1.0,
            screenValueDouble2 = (bufferSnapshot.size).toDouble(),
            screenValueDouble3 = previousValues.cumulatedGpsPrecision,
            screenValueLong = 0,
            screenValueString = "not enough gps points",
            screenValueString2 = "alt mnt brute",
            screenValueString3 = "alt gps brute",
            screenValueString4 = "alt mns brute",
            screenValueBoolean = false,
            screenValueInt = bufferSnapshot.size,

            isStarted = false,
            cumulatedGpsPrecision = calculateNewCumulatedGpsPrecision(newGpsPoint, previousValues),
            acceleration = 0.0,
            verticalSpeed = 0.0,
            direction = 0.0,
            speed = 0.0,
            altitude = 0.0,
            latitude = 0.0,
            longitude = 0.0,
            time = 0,
            spreadAltitudeAndAltitudeGps = 0.0,
            coefficientOfSpreadAltitudeAndAltitudeGps = 0.0,
            durationAltitudeNotMnt = 0,
            gpsAltitudeAccuracy = 0.0,
            altitudeSourceMntMnsOrGps = "",
            isMoving = false,
            isAltitudeGot = false,
            isGpsAltitudeGot = false,
            uncorrectedGpsAltitude = 0.0,
            displayedAltitude = previousValues.displayedAltitude,
            displayedVerticalSpeed = 0.0
        )
    // from here isStarted must be true, (otherwise calculateScreenValues already returned)
    // newIsStarted = true
    val altitudeMntNewGpsPt = simplifySampleAltitudeMnt(newGpsPoint, bufferSnapshot)
    val altitudeMntMnsAccuracy = calculateAccuracyAltitudeMntMns(newGpsPoint, previousValues)
    val altitudeMnsNewGpsPt = simplifySampleAltitudeMns(newGpsPoint, bufferSnapshot)
    val altitudeGpsNewGpsPt = calculateAltitudeGps(previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint)
    val altitudeGpsAccuracy = calculateGpsAltitudeAccuracy(newGpsPoint, previousValues)

    val time1=System.currentTimeMillis()

    if (!previousValues.isMoving
        && (calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?:newGpsPoint.latitude, previousValues.longitude?:newGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude)
                < 3.0 * newGpsPoint.accuracy)
    ) return screenValuesWhenIsMovingIsWrong (previousValues, newGpsPoint, bufferSnapshot)
    // from here isMoving must be true because newGpsPoint is too far, (otherwise calculateScreenValues already returned)
    if ((newGpsPoint.latitude == previousGpsPoint.latitude && previousGpsPoint.latitude == latestGpsPoint.latitude
                && newGpsPoint.longitude == previousGpsPoint.longitude && latestGpsPoint.longitude == previousGpsPoint.longitude)
        || (previousValues.speed < 0.2 && previousValues.isMoving))
        return screenValuesWhenIsMovingBecomeWrong (previousValues, newGpsPoint, bufferSnapshot)
    val newIsMoving = true
    val newDirection = calculateNewDirection (previousValues, latestGpsPoint, newGpsPoint, previousGpsPoint)
    val speedCorrectionRatio = calculateSpeedCorrectionRatio(
        calculateAngleBetweenTwoDirection(previousValues.direction, newDirection))

    val deltaAltitudeBetweenPreviousAndNewGpsPoint = try {
        if (previousValues.altitude == null || (previousValues.altitudeSourceMntMnsOrGps == "GPS" && altitudeGpsNewGpsPt == null)
            || (previousValues.altitudeSourceMntMnsOrGps == "MNS" && altitudeMnsNewGpsPt == null)
            || (previousValues.altitudeSourceMntMnsOrGps == "MNT" && altitudeMntNewGpsPt == null)) 0.0
        else if (previousValues.altitudeSourceMntMnsOrGps == "GPS" ) (altitudeGpsNewGpsPt!! - previousValues.altitude)
        else if (previousValues.altitudeSourceMntMnsOrGps == "MNS" ) (altitudeMnsNewGpsPt!! - previousValues.altitude)
        else if (previousValues.altitudeSourceMntMnsOrGps == "MNT" ) (altitudeMntNewGpsPt!! - previousValues.altitude)
        else 0.0 } catch (e: Exception) {0.0}
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
    val expectedSpeed = maxOf(0.0 ,previousValues.speed/2.0 - 1.1,previousValues.acceleration * max(deltaTimeInSecond,4.0)/2.0
        + max(deltaTimeInSecond,4.0) * previousValues.verticalSpeed * 10.0 / previousValues.speed
        + previousValues.speed
        - min(deltaAltitudeBetweenPreviousAndNewGpsPoint,1.1) * 10.0 / previousValues.speed
    )
    val maxExpectedSpeed = sqrt (20*max(deltaTimeInSecond,4.0)+ expectedSpeed * expectedSpeed)

    val expectedDistance = deltaTimeInSecond * expectedSpeed
    val minExpectedDistance = deltaTimeInSecond * max((expectedSpeed - 3.0),0.0)
    val maxExpectedDistance = deltaTimeInSecond * maxExpectedSpeed

    val newHorizontalDistanceDone = calculateNewHorizontalDistanceDone (
        previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint,
        deltaTimeInSecond,
        newDirection,
        speedCorrectionRatio,
        minExpectedDistance, expectedDistance, maxExpectedDistance,
        deltaAltitudeBetweenPreviousAndNewGpsPoint
    )
    val newLatitude = calculateNewLatitude(previousValues, newDirection, newHorizontalDistanceDone, newGpsPoint)
    val newLongitude = calculateNewLongitude(previousValues, newDirection, newHorizontalDistanceDone, newGpsPoint)

    val time2 = System.currentTimeMillis()

    val coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone, altitudeMntNewGpsPt,altitudeMntMnsAccuracy,previousValues, deltaTimeInSecond, 1)
    val coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone,altitudeMnsNewGpsPt,altitudeMntMnsAccuracy,previousValues, deltaTimeInSecond, 1)

    val time3 = System.currentTimeMillis()

    val coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability = calculateAltitudeProbability(newHorizontalDistanceDone,altitudeGpsNewGpsPt,altitudeGpsAccuracy,previousValues, deltaTimeInSecond,previousValues.durationAltitudeNotMnt/2 + 1)
    // correction des probabilités
    coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y *= 1 + max(1,previousValues.durationAltitudeNotMnt-2)*previousValues.durationAltitudeNotMnt/1.0
    coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y *= 1 + max(1,previousValues.durationAltitudeNotMnt-2)*previousValues.durationAltitudeNotMnt/1.1

    val newAltitudeSourceMntMnsOrGps = if (maxOf(
            coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y) == coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y) "MNT"
        else if (maxOf(
            coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y,
            coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y) == coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y) "MNS"
        else "GPS"

    val newDurationAltitudeNotMnt = if (newAltitudeSourceMntMnsOrGps == "MNT") max(previousValues.durationAltitudeNotMnt - 1,0)
        else min (10,previousValues.durationAltitudeNotMnt + 1)
    val newAltitude = calculateNewAltitude(
        coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability,
        coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability,
        coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability,
        previousValues
    )
    //mise à jour durationHighSpreadAltitude
    var newDurationHighSpreadAltitude = previousValues.durationHighSpreadAltitude
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
    else newDurationHighSpreadAltitude = max(0,previousValues.durationHighSpreadAltitude - 1)
    //fin

    val newSpeed = calculateNewSpeed(previousValues, newHorizontalDistanceDone, speedCorrectionRatio, newAltitude, deltaTimeInSecond)
    val newAcceleration = try {(newSpeed - previousValues.speed)/(2 * deltaTimeInSecond)+ previousValues.acceleration / 2.0} catch (e: Exception) {previousValues.acceleration}
    val newVerticalSpeed = try{((newAltitude?: (previousValues.altitude?: 0.0)) - (previousValues.altitude?: (newAltitude?: 0.0)))/deltaTimeInSecond}
        catch (e: Exception) {previousValues.verticalSpeed}
    val newGpsAltitudeAccuracy = altitudeGpsAccuracy//min (5.0 * previousValues.gpsAltitudeAccuracy / 6.0 + newGpsPoint.accuracy.toDouble() / 6.0,previousValues.gpsAltitudeAccuracy + 1.0)
    val newIsGpsAltitudeGot = previousValues.isGpsAltitudeGot || altitudeGpsNewGpsPt!= null



    //creation du fichier gps_debug.txt
    appendDebugLog(
        context,
        "${newGpsPoint.timestamp}\t" +
                "${newAltitude}\t" +
                "${(((altitudeMntNewGpsPt ?: 0.0) * 1000000).toInt() / 1000000.0)}\t" +
                "${(((altitudeMntMnsAccuracy) * 1000000).toInt() / 1000000.0)}\t" +
                "${((((coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.x?:0.0)*1000).toInt())/1000.0)}"
                + "\t" + (coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y)
                + "\t" + newAltitudeSourceMntMnsOrGps
                + "\t" + (((altitudeGpsNewGpsPt ?: 0.0) * 100).toInt() / 100.0)
                + "\t" + (coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y)
                + "\t" + (AppConfig.activeAltitudeLidarRequests).toString()
                + "\t" + (AppConfig.activeAltitudeSrtmRequests).toString()
                + "\t" + (time1-start).toString() + "\t" + (time2-time1).toString() + "\t" + (time3-time2).toString() + "\t" + (System.currentTimeMillis()-time3).toString()
                + "\t" + (previousValues.durationHighSpreadAltitude)
                + "\t" + "$newSpeed"
    )




    return ScreenValues(
        altGps = (((coordinatesOfMostProbableGpsAltitudeAsAltitudeAndProbability.y)*100000).toInt())/100000.0 ,//- (((altitudeGpsNewGpsPt ?: 0.0) * 100).toInt() / 100.0),
        altMnt = (((coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.y)*100000).toInt())/100000.0 ,//- (((altitudeMntNewGpsPt ?: 0.0) * 100).toInt() / 100.0),
        altMns = altitudeMntMnsAccuracy, //(((coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.y)*100000).toInt())/100000.0 ,//- (((altitudeMnsNewGpsPt ?: 0.0) * 100).toInt() / 100.0),
        screenValueDouble1 = latestGpsPoint.altitudeGps,
        screenValueDouble2 = expectedDistance,
        screenValueDouble3 = (latestGpsPoint.altitudeAccuracy?:0.0).toDouble(),
        screenValueLong = newGpsPoint.timestamp,
        screenValueString = "tps calc "+(time1-start).toString() + " + " + (time2-time1).toString() + " + " + (time3-time2).toString() + " + " + (System.currentTimeMillis()-time3).toString(),
        screenValueString2 = "alt mnt brut/fin" + (((altitudeMntNewGpsPt ?: 0.0) * 100).toInt() / 100.0).toString() + " / "
                + ((((coordinatesOfMostProbableMntAltitudeAsAltitudeAndProbability.x?:0.0)*100).toInt())/100.0).toString(),
        screenValueString3 = "requetes lidhd / srtm  " + (AppConfig.activeAltitudeLidarRequests).toString() + " / "
                + (AppConfig.activeAltitudeSrtmRequests).toString(),
        screenValueString4 = "alt mns brut/fin" + (((altitudeMnsNewGpsPt ?: 0.0) * 100).toInt() / 100.0).toString() + " / "
                + ((((coordinatesOfMostProbableMnsAltitudeAsAltitudeAndProbability.x?:0.0)*100).toInt())/100.0).toString(),
        screenValueBoolean = false,
        screenValueInt = buffer.size,
        isStarted = true,
        cumulatedGpsPrecision = newCumulatedGpsPrecision,
        acceleration = newAcceleration,
        verticalSpeed = newVerticalSpeed,
        direction = newDirection,
        speed = newSpeed,
        altitude = newAltitude,
        latitude = newLatitude,
        longitude = newLongitude,
        time = newGpsPoint.timestamp,
        spreadAltitudeAndAltitudeGps = calculateSpreadAltitudeAndAltitudeGps (previousValues, previousGpsPoint, bufferSnapshot),
        coefficientOfSpreadAltitudeAndAltitudeGps = calculateCoefficientOfSpreadAltitudeAndAltitudeGps (previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint),
        durationAltitudeNotMnt = newDurationAltitudeNotMnt,
        gpsAltitudeAccuracy = newGpsAltitudeAccuracy,
        altitudeSourceMntMnsOrGps = newAltitudeSourceMntMnsOrGps,
        isMoving = newIsMoving,
        isAltitudeGot = newAltitude!=null,
        isGpsAltitudeGot = newIsGpsAltitudeGot,
        uncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (previousValues, previousGpsPoint, newGpsPoint, latestGpsPoint),
        displayedAltitude = newAltitude,
        displayedVerticalSpeed = newVerticalSpeed/2.0 + previousValues.verticalSpeed/2.0,
        durationHighSpreadAltitude = newDurationHighSpreadAltitude
    )
}

fun calculateAccuracyAltitudeMntMns(sample: GpsSample, previousValues: ScreenValues): Double {
    fun calculateAccuracy (vertSpeed:Double,
                           supplementVertSpeed:Double,
                           gpsAccuracy:Double,
                           supplementGpsAccuracy:Double,
                           coefGpsAccuracy:Double,
                           speed:Double,
                           supplementSpeed:Double,
                           generalCoef:Double,
                           uncertaintyToAdd:Double):Double {
        return try { previousValues.durationAltitudeNotMnt * 0.05 + generalCoef*((abs(vertSpeed)+supplementVertSpeed)*(gpsAccuracy+supplementGpsAccuracy)
                /(speed+supplementSpeed)+coefGpsAccuracy*gpsAccuracy+uncertaintyToAdd)
            //lidar:sVS:0.02;sGA:0.5;cGA:0.02;sS:3.0;gC:0.8;uTA:0.0) ; //ign:0.04;5.0;0.02;3.0;1.0;0.1; //srtm:0.05;50.0;0.02;2.0;1.0;2.0)
        } catch (e: Exception) {
            999.9
        }
    }
    if (sample.altitudeLidarHdMnt != null && sample.altitudeLidarHdMnt!! > -500.0) return calculateAccuracy (previousValues.verticalSpeed, 0.02, sample.accuracy.toDouble(), 0.5,
        0.02, previousValues.speed, 3.0, 0.8, 0.0)
    if (sample.altitudeIgn != null && sample.altitudeIgn!! > -500.0) return calculateAccuracy (previousValues.verticalSpeed, 0.04, sample.accuracy.toDouble(), 5.0,
        0.02, previousValues.speed, 3.0, 1.0, 0.1)
    if (sample.altitudeSrtm != null && sample.altitudeSrtm!! > -500.0) return calculateAccuracy (previousValues.verticalSpeed, 0.05, sample.accuracy.toDouble(),  50.0,
        0.02, previousValues.speed, 2.0, 1.0, 2.0)
    return 999.9
}

fun calculateAltitudeGps (previousValues: ScreenValues, oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample): Double? {
    if (!previousValues.isAltitudeGot) return null
    val newUncorrectedGpsAltitude = calculateUncorrectedGpsAltitude (previousValues, oldPoint, newPoint, latestPoint)
    val previousSpread = previousValues.spreadAltitudeAndAltitudeGps
    if (newUncorrectedGpsAltitude == null || previousSpread == null) return null
    return newUncorrectedGpsAltitude - previousValues.spreadAltitudeAndAltitudeGps
}

fun calculateAltitudeProbability (newHorizontalDistanceDone: Double,
                                  altitudeOfTheSource: Double?,
                                  accuracyOfTheSource: Double,
                                  previousValues: ScreenValues,
                                  deltaTimeInSecond: Double,
                                  powerForSource: Int):
        CoordinatesOrXNull {
    if (altitudeOfTheSource == null) return CoordinatesOrXNull(previousValues.altitude, 0.0)
    val functionOfProbabilityOfAltitudeOfTheSource = { x: Double ->
        val unit = ((1.0 / ((x - altitudeOfTheSource) * (x - altitudeOfTheSource)/(accuracyOfTheSource*accuracyOfTheSource) + 1.0))/(PI*accuracyOfTheSource))
        when (powerForSource) {
            1 -> unit
            2 -> unit * unit
            3 -> unit * unit * unit
            else -> unit * unit * unit * unit
        }
    }
    val previousSpeed = previousValues.speed
    val uncorrectedVerticalSpeed = previousValues.verticalSpeed
    val previousVerticalSpeed = if (uncorrectedVerticalSpeed < -9) (uncorrectedVerticalSpeed*0.1 - 5.1)
        else if (uncorrectedVerticalSpeed < -3) (uncorrectedVerticalSpeed*0.5 + 1.5)
        else if (uncorrectedVerticalSpeed < 2) uncorrectedVerticalSpeed
        else if (uncorrectedVerticalSpeed < 4) (uncorrectedVerticalSpeed*0.5 + 1.0)
        else (uncorrectedVerticalSpeed*0.1 + 3.6)
    val previousAcceleration = previousValues.acceleration
    val previousAltitude = previousValues.altitude?:altitudeOfTheSource
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
                    1.0 / ((x - expectedAltitude) * (x - expectedAltitude)  * (x - expectedAltitude)
                            / ((expectedMaxAltitude - expectedAltitude) * (expectedMaxAltitude - expectedAltitude) * (expectedMaxAltitude - expectedAltitude)) + 1.0)
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
        val nb = max (0.0,(previousValues.durationHighSpreadAltitude) - 0.5)
        if (coordinates.x > altitudeOfTheSource)
            coordinates.x -= (coordinates.x-(altitudeOfTheSource+accuracyOfTheSource)) * nb / (nb + 1.0) //nb * coordinates.x / (nb+1.0) + altitudeOfTheSource / (nb + 1.0)
        else coordinates.x += ((altitudeOfTheSource-accuracyOfTheSource)-coordinates.x) * nb / (nb + 1.0)
    }
    return CoordinatesOrXNull(coordinates.x, coordinates.y)
}

fun calculateGpsAltitudeAccuracy(pt1: GpsSample?, previousValues: ScreenValues): Double{
    //=MIN(9*CS4/10+(P4+0,5)/10;CS4+1)
    //cs = previousValues.gpsAltitudeAccuracy
    if (pt1 == null) return 999.9
    val cs = previousValues.gpsAltitudeAccuracy
    val p = ((pt1.altitudeAccuracy?:999.9).toDouble())/1.0
    return try{ min(9.0*cs/10.0+(p+0.15)/10.0,cs+0.5)}
    catch (e: Exception) {
        cs
    }
}

fun calculateAngleBetweenTwoDirection (direction1: Double?, direction2: Double?): Double {
    if (direction1 == null || direction2 == null) return 0.0
    return minOf(abs(direction1-direction2), abs(direction1-direction2-2*PI), abs(direction1-direction2+2*PI))
}

fun calculateCoefficientOfSpreadAltitudeAndAltitudeGps (previousValues: ScreenValues, oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample): Double {
    val previous = previousValues.coefficientOfSpreadAltitudeAndAltitudeGps
    // la formule ci dessous est ok, mais remplacer newpoint par le choix du point comme dans calculatealtgps
    val acc = newPoint.altitudeAccuracy?: 9999.0F
    return try {
        if (previous<100) {previous+1/(acc+0.1)} else {100.0}
    } catch (e: Exception) {
        previous
    }
}

fun calculateDirectionBetweenTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return atan2(
        lat2 - lat1,
        (lon2 - lon1)*cos(lat1*PI/180)
    )
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


fun calculateDistanceToNewPointProjectedOnNewDir (previousValues: ScreenValues, newPoint: GpsSample, newDirection: Double): Double {
    val angle = calculateAngleBetweenTwoDirection(previousValues.direction, newDirection)
    val distance = calculateDistanceBetweenTwoGpsPoints(
        previousValues.latitude ?: newPoint.latitude,
        previousValues.longitude ?: newPoint.longitude,
        newPoint.latitude,
        newPoint.longitude
    )
    return distance * cos(angle)
}

fun calculateExpectedSpeedDependingOnPreviousValuesAndNewAltitude(previousValues: ScreenValues, oldPoint: GpsSample, newPoint: GpsSample, latestGpsPoint: GpsSample, deltaTimeInSecond: Double, powerCorrection: Double, buffer: List<GpsSample>): Double {
    val deltaTime = if (deltaTimeInSecond < 1.0) deltaTimeInSecond else min(deltaTimeInSecond/2.0+0.5,2.0)
    val previousSpeed = previousValues.speed
    val previousVerticalSpeed = previousValues.verticalSpeed
    val previousAcceleration = previousValues.acceleration
    val previousAltitude = previousValues.altitude
    val newAltitude = when (previousValues.altitudeSourceMntMnsOrGps) {
        "GPS" -> calculateAltitudeGps(previousValues, oldPoint, newPoint, latestGpsPoint)?: previousValues.altitude
        "MNS" -> simplifySampleAltitudeMns(newPoint, buffer)?: previousValues.altitude
        else -> simplifySampleAltitudeMnt(newPoint, buffer)?: previousValues.altitude
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

fun calculateMiddleDirection (angle12: Double, angle13: Double, angle23: Double, direction1: Double, direction2: Double, direction3: Double): Double {
    if (maxOf(angle12, angle13, angle23)==angle12) return direction3
    if (maxOf(angle12, angle13, angle23)==angle13) return direction2
    return direction1
}

fun calculateNewAltitude (coordinates1: CoordinatesOrXNull, coordinates2: CoordinatesOrXNull, coordinates3: CoordinatesOrXNull, previousValues: ScreenValues): Double? {
    var alt: Double? = previousValues.altitude
    alt =
        if (coordinates1.x == null && coordinates2.x == null && coordinates3.x == null) previousValues.altitude
    else if (maxOf(coordinates1.y, coordinates2.y, coordinates3.y) == coordinates1.y) coordinates1.x
    else if (maxOf(coordinates1.y, coordinates2.y, coordinates3.y) == coordinates2.y) coordinates2.x
    else coordinates3.x

    if (alt==null) return alt
    return alt

}

fun calculateNewCumulatedGpsPrecision (newPoint: GpsSample, previousValues: ScreenValues): Double {
    val previous = previousValues.cumulatedGpsPrecision
    return try {
        previous + 1/(newPoint.accuracy)
    } catch (e: Exception) {
        previous
    }
}

fun calculateNewDirection (previousValues: ScreenValues, latestGpsPoint: GpsSample, newGpsPoint: GpsSample, previousGpsPoint: GpsSample): Double {
val previousDirection = previousValues.direction
    if (previousDirection == null || previousValues.latitude == 0.0 || previousValues.longitude == 0.0 || previousValues.latitude == null || previousValues.longitude == null)
        return calculateDirectionBetweenTwoPoints(previousGpsPoint.latitude, previousGpsPoint.longitude, newGpsPoint.latitude, newGpsPoint.longitude)
val direction2 = calculateDirectionBetweenTwoPoints(previousValues.latitude, previousValues.longitude, latestGpsPoint.latitude, latestGpsPoint.longitude)
val direction1 = calculateDirectionBetweenTwoPoints(previousValues.latitude, previousValues.longitude, newGpsPoint.latitude, newGpsPoint.longitude)
    if ( (calculateAngleBetweenTwoDirection (previousDirection, direction1)
         + calculateAngleBetweenTwoDirection (previousDirection, direction2)
         + calculateAngleBetweenTwoDirection (direction1, direction2)) < 6.28)
         return calculateMiddleDirection ( calculateAngleBetweenTwoDirection (previousDirection, direction1),
             calculateAngleBetweenTwoDirection (previousDirection, direction2),
             calculateAngleBetweenTwoDirection (direction1, direction2),
             previousDirection, direction1, direction2)
return previousDirection
}

fun calculateNewHorizontalDistanceDone (
    previousValues: ScreenValues,
    oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample,
    deltaTimeInSecond: Double,
    newDirection: Double,
    speedCorrectionRatio: Double,
    minExpectedDistance: Double, expectedDistance: Double, maxExpectedDistance: Double,
    deltaAltitude: Double
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
                               previousValues: ScreenValues,
                               speedCorrectionRatio: Double,
                               deltaAltitude: Double
                            ): Double {
        //val deltaAltitude = if (possibleNewAltitude == null || previousValues.altitude == null) 0.0 else possibleNewAltitude - previousValues.altitude
        val expectedMaxHorizontalDistance = min(
            sqrt(maxExpectedDistance * maxExpectedDistance + (deltaAltitude)*(deltaAltitude)),
            1.15 * maxExpectedDistance
        )/speedCorrectionRatio
        val expectedHorizontalDistance = min(
            sqrt(expectedDistance * expectedDistance + (deltaAltitude)*(deltaAltitude)),
            1.15 * expectedDistance,
        )/speedCorrectionRatio
        val expectedMinHorizontalDistance = min(
            sqrt(minExpectedDistance * minExpectedDistance + (deltaAltitude)*(deltaAltitude)),
            1.15 * minExpectedDistance
        )/speedCorrectionRatio
        val gpsUncertainty = newPoint.accuracy.toDouble()
        val functionForDistanceAccordingToGpsAccuracy = { x: Double ->
            1 / ((distanceToNewPointProjectedOnNewDir - x) * (distanceToNewPointProjectedOnNewDir - x) / (gpsUncertainty * gpsUncertainty) + 1)
        }

        val functionForDistanceAccordingToGpsPoints = { x: Double ->
            val previousSpeed = previousValues.speed
            val speed01 = calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?: newPoint.latitude,
                previousValues.longitude?: newPoint.longitude,
                newPoint.latitude,
                newPoint.longitude)/deltaTimeInSecond
            val speed02 = calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?: newPoint.latitude,
                previousValues.longitude?: newPoint.longitude,
                latestPoint.latitude,
                latestPoint.longitude)/(deltaTimeInSecond+(newPoint.timestamp-latestPoint.timestamp)/1000.0)
            val speed12 = calculateDistanceBetweenTwoGpsPoints (newPoint.latitude,
                latestPoint.latitude,
                newPoint.longitude,
                latestPoint.longitude)/((newPoint.timestamp-latestPoint.timestamp)/1000.0)
            val speed = (listOf(speed01,speed02,speed12).minByOrNull { abs(it-previousSpeed)  })?:previousSpeed
            val distance = speed * deltaTimeInSecond
            try { val unit = 1 / ((x - distance) * (x - distance) / ((expectedMaxHorizontalDistance * expectedMaxHorizontalDistance ) + 1))
            unit * unit * unit} catch (e: Exception) {previousSpeed*deltaTimeInSecond}
        }

        val functionForDistanceAccordingToExpectedDistance = { x: Double -> 1.0
            /*if (x > expectedHorizontalDistance) 1 / ((x - expectedHorizontalDistance) * (x - expectedHorizontalDistance) * (x - expectedHorizontalDistance) * (x - expectedHorizontalDistance) / (expectedMaxHorizontalDistance * expectedMaxHorizontalDistance  * expectedMaxHorizontalDistance  * expectedMaxHorizontalDistance ) + 1)
            else
                1 / ((expectedHorizontalDistance - x) * (expectedHorizontalDistance - x) * (expectedHorizontalDistance - x) / (expectedMinHorizontalDistance * expectedMinHorizontalDistance * expectedMinHorizontalDistance) + 1)
        */}
        val functionOfProbabilityAccordingToDistance = { x: Double -> functionForDistanceAccordingToGpsPoints (x) * functionForDistanceAccordingToGpsAccuracy (x) }
        val distanceDone = try {findMaxOfIncreasingThenDecreasingFunction(
            functionOfProbabilityAccordingToDistance,
            0.0,maxExpectedDistance+10.0,10,0.01).x }
        catch (e: Exception) {
            if (previousValues.latitude!=null && previousValues.longitude!=null) {calculateDistanceBetweenTwoGpsPoints(previousValues.latitude, previousValues.longitude, newPoint.latitude, newPoint.longitude)}
            else 0.01
        }
        return distanceDone  // calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?: newPoint.latitude, previousValues.longitude?: newPoint.longitude, newPoint.latitude, newPoint.longitude)

    }

    val distanceToNewPointProjectedOnNewDir =
        calculateDistanceToNewPointProjectedOnNewDir(previousValues, newPoint, newDirection)
    return calculateDistanceDone(expectedDistance, maxExpectedDistance, minExpectedDistance,
        distanceToNewPointProjectedOnNewDir,
        newPoint,
        previousValues,
        speedCorrectionRatio,
        deltaAltitude)
}

fun calculateNewLatitude (previousValues: ScreenValues, newDirection: Double, newHorizontalDistanceDone: Double, newPoint: GpsSample): Double {
    if (previousValues.latitude == null || previousValues.longitude == null) return newPoint.latitude
    return previousValues.latitude + newHorizontalDistanceDone * sin(newDirection) * 90.0 / (10000000.0 + ((previousValues.altitude)?:0.0))
}

fun calculateNewLongitude (previousValues: ScreenValues, newDirection: Double, newHorizontalDistanceDone: Double, newPoint: GpsSample): Double {
    if (previousValues.latitude == null || previousValues.longitude == null) return newPoint.longitude
    return try {previousValues.longitude + newHorizontalDistanceDone * cos(newDirection) * 90.0 / ((10000000.0 + ((previousValues.altitude)?:0.0)) * cos(previousValues.latitude*PI/180))}
    catch (e: Exception) {
        newPoint.longitude
    }
}

fun calculateNewSpeed(
    previousValues: ScreenValues,
    newHorizontalDistanceDone: Double,
    speedCorrectionRatio:Double,
    newAltitude: Double?,
    deltaTimeInSecond: Double):Double {
    val previousAltitude = previousValues.altitude
    val deltaAltitude =
        if (newAltitude == null || previousAltitude == null) 0.0 else newAltitude - previousAltitude
    val distance =
        min(sqrt(newHorizontalDistanceDone * newHorizontalDistanceDone + deltaAltitude * deltaAltitude),1.5 * newHorizontalDistanceDone)
    return if (deltaTimeInSecond > 0.0)
        (distance / deltaTimeInSecond) * speedCorrectionRatio
    else previousValues.speed
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

fun calculateSpreadAltitudeAndAltitudeGps (previousValues: ScreenValues, oldPoint: GpsSample, buffer: List<GpsSample>): Double? {
    val previousAltitude = if (previousValues.altitudeSourceMntMnsOrGps == "MNT")
        simplifySampleAltitudeMnt(oldPoint, buffer)?: previousValues.altitude
    else simplifySampleAltitudeMns(oldPoint, buffer)?: previousValues.altitude
    if (previousValues.altitudeSourceMntMnsOrGps == "GPS"
        || previousAltitude == null || oldPoint.altitudeGps == null) return previousValues.spreadAltitudeAndAltitudeGps
    val previousSpread = previousValues.spreadAltitudeAndAltitudeGps?:0.0
    val previousCoef = previousValues.coefficientOfSpreadAltitudeAndAltitudeGps
    val altGps = oldPoint.altitudeGps!!
    if ((oldPoint.altitudeAccuracy == null || oldPoint.altitudeAccuracy == 0.0f ))
        return if (previousValues.altitude != null) (previousValues.spreadAltitudeAndAltitudeGps?:(altGps-previousValues.altitude)) else null
    val accGps = (oldPoint.altitudeAccuracy!!).toDouble()
    return try {
        (previousSpread * previousCoef + (altGps - previousAltitude) / (accGps + 0.1)) / (previousCoef + 1.0 / (accGps + 0.1))
    } catch (e: Exception) {
        previousSpread
    }
}

fun calculateUncorrectedGpsAltitude (previousValues: ScreenValues, oldPoint: GpsSample, newPoint: GpsSample, latestPoint: GpsSample): Double? {
    val previous = previousValues.uncorrectedGpsAltitude
    if (previous == null) return if (newPoint.altitudeGps != null) newPoint.altitudeGps else latestPoint.altitudeGps
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
*/

