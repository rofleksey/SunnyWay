package ru.rofleksey.sunnyway.util

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class Util {
    companion object {
        const val EARTH_RADIUS_M = 6371000
        const val EARTH_RADIUS = 6371
        const val PEDESTRIAN_SPEED = 0.9
        // accepts [-180..180], returns [0..180)
        fun findAngleDelta(main: Double, secondary: Double): Double {
            var temp = secondary - main
            if (temp >= 180) {
                temp -= 360
            }
            if (temp <= -180) {
                temp += 360
            }
            if (temp == 180.0 || temp == -180.0) {
                return 0.0
            }
            return temp
        }
        fun findAngleDeltaAbs(a: Double, b: Double) = abs(findAngleDelta(a, b))
        // accepts [-180..180], returns -1 if coming from the right, 1 - from the left, 0 - if parallel
        fun getRaySide(main: Double, rayAngle: Double): Double = sign(findAngleDelta(main, rayAngle))
        // accepts -180..180, returns 0..90
        fun findOrtAngle(curAngle: Double, sunAngle: Double): Double {
            var angleDelta = findAngleDeltaAbs(sunAngle, curAngle)
            var angleDelta1 = findAngleDeltaAbs(sunAngle, invertAngle180(curAngle))
            if (angleDelta > 90.0) {
                angleDelta = 180 - angleDelta
            }
            if (angleDelta1 > 90.0) {
                angleDelta1 = 180 - angleDelta1
            }
            return min(angleDelta, angleDelta1)
        }
        fun invertAngle180(a: Double) = if (a < 0) a + 180 else a - 180
        // from [0..360] to [-180..180]
        fun convert360to180(a: Double) = if (a < 180) a else a - 360
    }
}