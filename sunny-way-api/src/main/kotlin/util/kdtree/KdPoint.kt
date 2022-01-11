package util.kdtree

import kotlin.math.abs

class KdPoint(val x: Double, val y: Double): Comparable<KdPoint> {
    companion object {
        val comparator = compareBy<KdPoint> ({ it.y }, { it.x })
    }

    fun dx(p: KdPoint): Double = abs(x - p.x)
    fun dy(p: KdPoint): Double = abs(y - p.y)

    override fun compareTo(other: KdPoint): Int {
        return comparator.compare(this, other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KdPoint

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}