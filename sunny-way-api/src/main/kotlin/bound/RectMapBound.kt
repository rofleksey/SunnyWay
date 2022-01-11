package bound

import rest.GeoPoint

class RectMapBound(
    private val x1: Double,
    private val y1: Double,
    private val x2: Double,
    private val y2: Double,
): MapBound {
    override fun isInside(point: GeoPoint): Boolean {
        return point.lat in x1..x2 && point.lon in y1..y2
    }
}