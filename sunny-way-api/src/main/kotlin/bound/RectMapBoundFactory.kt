package bound

import rest.GeoPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RectMapBoundFactory(private val minOffset: Double, private val offsetFactor: Double): MapBoundFactory {
    override fun create(pointA: GeoPoint, pointB: GeoPoint): MapBound {
        val deltaX = abs(pointA.lat - pointB.lat)
        val deltaY = abs(pointA.lon - pointB.lon)
        val minX = min(pointA.lat, pointB.lat)
        val maxX = max(pointA.lat, pointB.lat)
        val minY = min(pointA.lon, pointB.lon)
        val maxY = max(pointA.lon, pointB.lon)
        val offsetX = max(deltaX * offsetFactor, minOffset)
        val offsetY = max(deltaY * offsetFactor, minOffset)
        return RectMapBound(minX - offsetX, minY - offsetY, maxX + offsetX, maxY + offsetY)
    }
}