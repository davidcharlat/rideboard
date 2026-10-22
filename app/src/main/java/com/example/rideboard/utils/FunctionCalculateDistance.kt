package com.example.rideboard.utils

import com.example.rideboard.buffer.GpsSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


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
    fun calculateDistanceDone (expectedDistance: Double, maxExpectedDistance: Double, minExpectedDistance: Double,
                               distanceToNewPointProjectedOnNewDir: Double,
                               newPoint: GpsSample,
                               speedCorrectionRatio: Double,
                               deltaAltitude: Double,
                               previousSpeed: Double
    ): Double {
        //val deltaAltitude = if (possibleNewAltitude == null || previousValues.altitude == null) 0.0 else possibleNewAltitude - previousValues.altitude
        val expectedMaxHorizontalDistance = max(
            sqrt(max(0.0,maxExpectedDistance * maxExpectedDistance - (deltaAltitude)*(deltaAltitude))),
            maxExpectedDistance/1.15
        )/speedCorrectionRatio
        val expectedHorizontalDistance = max(
            sqrt(max(0.0,expectedDistance * expectedDistance - (deltaAltitude)*(deltaAltitude))),
            expectedDistance/1.15,
        )/speedCorrectionRatio
        val expectedMinHorizontalDistance = max(
            sqrt(max(0.0,minExpectedDistance * minExpectedDistance - (deltaAltitude)*(deltaAltitude))),
            minExpectedDistance/1.15
        )/speedCorrectionRatio
        val gpsUncertainty = newPoint.accuracy.toDouble() + 0.05
        val functionForDistanceAccordingToGpsAccuracy = { x: Double ->
            val unit = 1 / ((distanceToNewPointProjectedOnNewDir - x) * (distanceToNewPointProjectedOnNewDir - x) / (3.0 * gpsUncertainty * gpsUncertainty) + 1)
            unit * unit
        }

        val speed01 = calculateDistanceBetweenTwoGpsPoints(previousLatitude?: newPoint.latitude,
            previousLongitude?: newPoint.longitude,
            newPoint.latitude,
            newPoint.longitude)/((newPoint.timestamp-oldPoint.timestamp).toDouble()/1000.0)
        val speed02 = calculateDistanceBetweenTwoGpsPoints(previousLatitude?: newPoint.latitude,
            previousLongitude?: newPoint.longitude,
            latestPoint.latitude,
            latestPoint.longitude)/((latestPoint.timestamp-oldPoint.timestamp)/1000.0)
        val speed12 = (calculateDistanceBetweenTwoGpsPoints (newPoint.latitude,
            newPoint.longitude,
            latestPoint.latitude,
            latestPoint.longitude)/((latestPoint.timestamp-newPoint.timestamp)/1000.0))*0.85 + 0.05 * speed01 + 0.1 * speed02
        val functionForDistanceAccordingToGpsPoints = { x: Double ->
            val speed = (listOf(speed01,speed02,speed12).minByOrNull { abs(it-previousSpeed)  })?:previousSpeed
            val distance = speed * deltaTimeInSecond
            try { val unit = 1 / ((x - distance) * (x - distance) / ((expectedMaxHorizontalDistance * expectedMaxHorizontalDistance ) + 1))
                unit} catch (e: Exception) {previousSpeed*deltaTimeInSecond}
        }

        val functionForDistanceAccordingToExpectedDistance = { x: Double ->
            val unit = 1 / ((x - expectedHorizontalDistance)  * (x - expectedHorizontalDistance) / (expectedHorizontalDistance  * expectedHorizontalDistance ) + 1)
            unit
        }

        val functionForDistanceAccordingToGpsSpeed = { x: Double ->
            val speed = ((newPoint.speedGps)?:previousSpeed).toDouble()
            val uncertainty= ((newPoint.speedAccuracy)?:3.0).toDouble() + 0.03
            val unit = 1 / (((x-speed)*(x-speed)/(uncertainty*uncertainty))+1)
            unit
        }

        val functionOfProbabilityAccordingToDistance = { x: Double -> functionForDistanceAccordingToGpsSpeed(x)*functionForDistanceAccordingToExpectedDistance(x) * functionForDistanceAccordingToGpsPoints (x) * functionForDistanceAccordingToGpsAccuracy (x) }

        val distanceDone = try {findMaxOfIncreasingThenDecreasingFunction(
            functionOfProbabilityAccordingToDistance,
            minExpectedDistance,maxExpectedDistance+1.0,50,0.01).x }
        catch (e: Exception) {
            if (previousLatitude!=null && previousLongitude!=null) {calculateDistanceBetweenTwoGpsPoints(previousLatitude, previousLongitude, newPoint.latitude, newPoint.longitude)}
            else 0.01
        }
        return try {
            val ratioOfSpeed = max(0.0,(speed01+0.1)/(speed12+0.1)-2)
            var correctedDist = distanceDone * (1.0 + 0.25 * (ratioOfSpeed/(ratioOfSpeed+18.0)))//cette petite correction pour que les points calculés rattrapent le point gps brut
            if (speed01 < speed12/1.5 && speed01 < previousSpeed/1.5) correctedDist *= 0.95
            correctedDist
        }catch (e: Exception) {distanceDone}  // calculateDistanceBetweenTwoGpsPoints(previousValues.latitude?: newPoint.latitude, previousValues.longitude?: newPoint.longitude, newPoint.latitude, newPoint.longitude)

    }
    val timeInterval = minOf (4.0,(newPoint.timestamp-oldPoint.timestamp).toDouble()/1000.0,(latestPoint.timestamp-newPoint.timestamp).toDouble()/1000.0)
    val distanceToNewPointProjectedOnNewDir =
        calculateDistanceToNewPointProjectedOnNewDir(newPoint, newDirection, previousDirection, previousLatitude, previousLongitude)
    return min (calculateDistanceDone(expectedDistance, maxExpectedDistance, minExpectedDistance,
        distanceToNewPointProjectedOnNewDir,
        newPoint,
        speedCorrectionRatio,
        deltaAltitude,
        previousSpeed),
        timeInterval*(previousSpeed+3.0-min(1.5,previousSpeed)))
}
