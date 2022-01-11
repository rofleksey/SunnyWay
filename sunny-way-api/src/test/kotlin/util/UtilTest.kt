package util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class UtilTest {
    @Test
    fun findAngleDeltaAbsTest() {
        assertEquals(3.0, Util.findAngleDeltaAbs(148.0, 151.0))
        assertEquals(3.0, Util.findAngleDeltaAbs(151.0, 148.0))
        assertEquals(2.0, Util.findAngleDeltaAbs(-179.0, 179.0))
        assertEquals(2.0, Util.findAngleDeltaAbs(179.0, -179.0))
        assertEquals(64.0, Util.findAngleDeltaAbs(32.0, 96.0))
        assertEquals(64.0, Util.findAngleDeltaAbs(96.0, 32.0))
        assertEquals(64.0, Util.findAngleDeltaAbs(-32.0, -96.0))
        assertEquals(64.0, Util.findAngleDeltaAbs(-96.0, -32.0))
        assertEquals(0.0, Util.findAngleDeltaAbs(-1.0, 179.0))
        assertEquals(91.0, Util.findAngleDeltaAbs(-46.0, 45.0))
        assertEquals(91.0, Util.findAngleDeltaAbs(45.0, -46.0))
    }

    @Test
    fun findAngleDeltaTest() {
        assertEquals(3.0, Util.findAngleDelta(148.0, 151.0))
        assertEquals(-3.0, Util.findAngleDelta(151.0, 148.0))
        assertEquals(-2.0, Util.findAngleDelta(-179.0, 179.0))
        assertEquals(2.0, Util.findAngleDelta(179.0, -179.0))
        assertEquals(64.0, Util.findAngleDelta(32.0, 96.0))
        assertEquals(-64.0, Util.findAngleDelta(96.0, 32.0))
        assertEquals(-64.0, Util.findAngleDelta(-32.0, -96.0))
        assertEquals(64.0, Util.findAngleDelta(-96.0, -32.0))
        assertEquals(0.0, Util.findAngleDelta(-1.0, 179.0))
        assertEquals(91.0, Util.findAngleDelta(-46.0, 45.0))
        assertEquals(-91.0, Util.findAngleDelta(45.0, -46.0))
    }

    @Test
    fun sunAngleTest() {
        assertEquals(1.0, Util.findOrtAngle(44.0, 45.0))
        assertEquals(1.0, Util.findOrtAngle(46.0, 45.0))
        assertEquals(45.0, Util.findOrtAngle(0.0, 45.0))
        assertEquals(0.0, Util.findOrtAngle(45.0, 45.0))
        assertEquals(45.0, Util.findOrtAngle(90.0, 45.0))
        assertEquals(89.0, Util.findOrtAngle(134.0, 45.0))
        assertEquals(90.0, Util.findOrtAngle(135.0, 45.0))
        assertEquals(89.0, Util.findOrtAngle(136.0, 45.0))
    }
}