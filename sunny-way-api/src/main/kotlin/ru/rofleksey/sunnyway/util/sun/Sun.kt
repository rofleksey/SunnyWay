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
        // Equation Of Time
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        // Local Solar Time Meridian
        val lstm = 15.0 * timeZone.toDouble()
        val timeCorrection = eot + 4.0 * (longitude - lstm)
        val lst = minutesSinceMidnight / 60.0 + timeCorrection / 60.0
        val a = -1.0 * (sin(latitudeRad) * sin(declinationRad)) / (cos(latitudeRad) * cos(declinationRad))
        val localSolarTime = acos(a) / 0.261799 // 0.261799 = Math.toRadians(15)
        val sunrise = 12.0 - localSolarTime - (timeCorrection / 60.0)
        val sunset = 12 + localSolarTime - (timeCorrection / 60.0)
        val hra = Math.toRadians(15.0 * (lst - 12.0))
        val alt = asin((sin(declinationRad) * sin(latitudeRad)) + (cos(declinationRad) * cos(latitudeRad) * cos(hra)))
        var azi = acos((cos(latitudeRad) * sin(declinationRad) - cos(declinationRad) * sin(latitudeRad) * cos(hra)) / cos(alt))
        val zen = Math.PI / 2 - alt
        if (hra > 0) {
            azi = 2 * Math.PI - azi
        }
        return SunResult(declination, eot, lstm, timeCorrection, localSolarTime, Math.toDegrees(alt), zen, azi, sunrise, sunset)
    }

}