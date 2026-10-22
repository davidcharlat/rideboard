package com.example.rideboard.utils

import com.example.rideboard.buffer.GpsSample
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
    val expectedMaxAltitudeDependingOnVerticalSpeed = if ((previousSpeed*previousSpeed) < ((newHorizontalDistanceDone / deltaTimeInSecond).pow(2) + 3.0))
        previousAltitude + max (0.0,previousVerticalSpeed + 0.15)*deltaTimeInSecond
    else if ((previousSpeed*previousSpeed) < ((newHorizontalDistanceDone / deltaTimeInSecond).pow(2) + 5.0))
        previousAltitude + max (0.0,previousVerticalSpeed + 0.25)*deltaTimeInSecond
    else previousAltitude + max (0.0,previousVerticalSpeed + 0.35)*deltaTimeInSecond
    val expectedMinAltitudeDependingOnVerticalSpeed = if (previousSpeed*previousSpeed < ((newHorizontalDistanceDone / deltaTimeInSecond).pow(2) - 6.5))
        previousAltitude + min (0.0, previousVerticalSpeed - 0.33)*deltaTimeInSecond
    else if (previousSpeed*previousSpeed < ((newHorizontalDistanceDone / deltaTimeInSecond).pow(2) - 10))
        previousAltitude + min (0.0, previousVerticalSpeed - 0.50)*deltaTimeInSecond
    else previousAltitude + min (0.0, previousVerticalSpeed - 0.66)*deltaTimeInSecond
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

fun calculateCorrectedHeartRate (pt1: GpsSample, pt2: GpsSample, pt3: GpsSample, pt4: GpsSample?): Int? {
    val HR1 = pt1.heartRate
    val HR2 = pt2.heartRate
    val HR3 = pt3.heartRate
    val HR4 = pt4?.heartRate
    val correctedHR3 = pt3.correctedHeartRate
    val correctedHR4 = pt4?.correctedHeartRate
    val lastVariation = (correctedHR3?:0) - ((correctedHR4?:correctedHR3)?:0)
    var highestPossibleGrowth = if ((correctedHR3?:0) > HRZ5) 1 + max(1,lastVariation)
    else if ((correctedHR3?:0) > HRZ4) 1 + max(2,lastVariation)
    else if ((correctedHR3?:0) > HRZ3) 1 + max(3,lastVariation)
    else if ((correctedHR3?:0) > HRZ2) 1 + max(4,lastVariation)
    else  1 + max(5,lastVariation)
    var lowestPossibleGrowth = if ((correctedHR3?:0) < 90) -2 + min(-1,lastVariation)
    else if ((correctedHR3?:0) < HRZ2) -2 + min(-2,lastVariation)
    else if ((correctedHR3?:0) < HRZ3) -2 + min(-3,lastVariation)
    else -2 + min(-4,lastVariation)
    val uncorrectedAbsVariation = minOf(abs(((HR4?:HR1)?:0)-(HR1?:0)),abs(((HR3?:HR1)?:0)-(HR1?:0)),abs(((HR2?:HR1)?:0)-(HR1?:0)))
    highestPossibleGrowth = max(1,min(highestPossibleGrowth,uncorrectedAbsVariation))
    lowestPossibleGrowth = min(-1,max(lowestPossibleGrowth,-uncorrectedAbsVariation))
    val deltaToCorrect = if(HR2 == null || correctedHR3 == null) 0 else correctedHR3 - HR2
    return if (deltaToCorrect in lowestPossibleGrowth..highestPossibleGrowth) HR2
    else if (deltaToCorrect < lowestPossibleGrowth) (if (HR2 == null) null else (correctedHR3?:HR2) + highestPossibleGrowth)
    else (if (HR2 == null) null else (correctedHR3?:HR2) + lowestPossibleGrowth)
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
