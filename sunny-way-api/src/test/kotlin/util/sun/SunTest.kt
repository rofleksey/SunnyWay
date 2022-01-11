package util.sun

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.sin

class SunTest {
    @Test
    fun test1() {
        val sun = Sun(59.9, 30.3609, 3)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = 1641036600L * 1000L
        }
        sun.setTime(calendar)
        val result = sun.calculate()
        assertEquals(-23.011636727869238, result.declination)
        assertEquals(-3.705178323396069, result.eot)
        assertEquals(45.0, result.lstm)
        assertEquals(-62.261578323396066, result.timeCorrection)
        assertEquals(2.8592852841553498, result.localSolarTime)
        assertEquals(10.17840768790125, result.sunrise)
        assertEquals(15.89697825621195, result.sunset)
        assertEquals(3.494069804950795, result.azimuth)
        assertEquals(1.4806895981190114, result.zenith)
    }
}