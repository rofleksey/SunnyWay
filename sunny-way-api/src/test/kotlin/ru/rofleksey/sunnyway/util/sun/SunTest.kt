package ru.rofleksey.sunnyway.util.sun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class SunTest {
    @Test
    fun test1() {
        val sun = Sun(59.9, 30.3609, 3)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = 1641036600L * 1000L
        }
        sun.setTime(calendar)
        val result = sun.calculate()
        assertEquals(3.494069804950795, result.azimuth, 0.001)
        assertEquals(5.162735258858645, result.elevation, 0.001)
    }
}