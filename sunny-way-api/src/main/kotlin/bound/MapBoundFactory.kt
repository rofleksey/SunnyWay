package bound

import rest.GeoPoint

interface MapBoundFactory {
    fun create(pointA: GeoPoint, pointB: GeoPoint): MapBound
}