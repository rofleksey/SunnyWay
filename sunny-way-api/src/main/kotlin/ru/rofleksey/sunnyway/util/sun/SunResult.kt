package ru.rofleksey.sunnyway.util.sun

import ru.rofleksey.sunnyway.util.Util
import kotlin.math.min

data class SunResult(val elevation: Double, val azimuth: Double) {
    companion object {
        const val DEFAULT_MAX_FACTOR = 10.0
        private const val MAX_ANGLE = 50.0
        private const val MIN_ANGLE = 5.0
        private const val MIN_ELEVATION = 1.0
    }

    fun isSunUp() = elevation >= MIN_ELEVATION

    fun calculateFactor(
        direction: Double,
        leftShadow: Double,
        rightShadow: Double,
        preferShadow: Boolean,
        maxFactor: Double
    ): Double {
        val azimuthDegrees = Util.convert360to180(Math.toDegrees(azimuth))
        val sunAngle = Util.findOrtAngle(direction, azimuthDegrees)
        val raySide = Util.getRaySide(direction, Util.invertAngle180(azimuthDegrees))
        return if (raySide == 0.0 || sunAngle <= MIN_ANGLE) {
            if (preferShadow) {
                maxFactor
            } else {
                1.0
            }
        } else {
            val buildingsFactor = if (raySide > 0) leftShadow else rightShadow
            val sunMultiplier = if (preferShadow) {
                min((sunAngle - MIN_ANGLE) / (MAX_ANGLE - MIN_ANGLE), 1.0)
            } else {
                1 - min((sunAngle - MIN_ANGLE) / (MAX_ANGLE - MIN_ANGLE), 1.0)
            }
            val shadowFactor = maxFactor + (1.0 - maxFactor) * sunMultiplier
            if (preferShadow) {
                buildingsFactor * shadowFactor + (1 - buildingsFactor) * maxFactor
            } else {
                buildingsFactor * shadowFactor * maxFactor + (1 - buildingsFactor)
            }
        }
    }
}