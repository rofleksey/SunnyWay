package ru.rofleksey.sunnyway.util.sun

import java.util.*
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class Sun(private val latitude: Double, private val longitude: Double, private val timeZone: Int) {
    private var minutesSinceMidnight: Double = 0.0
    private var dayOfTheYear: Double = 1.0

    fun setTime(calendar: Calendar) {
        minutesSinceMidnight = calendar.get(Calendar.HOUR_OF_DAY).toDouble() * 60
        minutesSinceMidnight += calendar.get(Calendar.MINUTE)
        dayOfTheYear = calendar.get(Calendar.DAY_OF_YEAR).toDouble()
    }

    fun calculate(): SunResult {
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfTheYear - 81.0)))
        val declinationRad = Math.toRadians(declination)
        val latitudeRad = Math.toRadians(latitude)
        val b = 360.0 / 365.0 * (dayOfTheYear - 81.0) * Math.PI / 180.0
        val equationOfTime = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val localSolarTimeMeridian = 15.0 * timeZone.toDouble()
        val timeCorrection = equationOfTime + 4.0 * (longitude - localSolarTimeMeridian)
        val localSolarTime = minutesSinceMidnight / 60.0 + timeCorrection / 60.0
        val hourAngle = Math.toRadians(15.0 * (localSolarTime - 12.0))
        val elevation =
            asin(sin(declinationRad) * sin(latitudeRad) + cos(declinationRad) * cos(latitudeRad) * cos(hourAngle))
        var azimuth = acos(
            (cos(latitudeRad) * sin(declinationRad) - cos(declinationRad) * sin(latitudeRad) * cos(hourAngle))
                    / cos(elevation)
        )
        if (hourAngle > 0) {
            azimuth = 2 * Math.PI - azimuth
        }
        return SunResult(Math.toDegrees(elevation), azimuth)
    }
}