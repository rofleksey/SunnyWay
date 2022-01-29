package ru.rofleksey.sunnyway.bound

import ru.rofleksey.sunnyway.rest.types.GeoPoint

interface MapBound {
    fun isInside(point: GeoPoint): Boolean
}