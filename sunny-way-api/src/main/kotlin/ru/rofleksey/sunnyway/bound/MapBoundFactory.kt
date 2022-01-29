package ru.rofleksey.sunnyway.bound

import ru.rofleksey.sunnyway.rest.types.GeoPoint

interface MapBoundFactory {
    fun create(pointA: GeoPoint, pointB: GeoPoint): MapBound
}