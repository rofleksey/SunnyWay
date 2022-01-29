package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class UserPolygonResponse(val polygon: List<GeoPoint>)