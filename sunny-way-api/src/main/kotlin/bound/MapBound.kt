package bound

import rest.GeoPoint

interface MapBound {
    fun isInside(point: GeoPoint): Boolean
}