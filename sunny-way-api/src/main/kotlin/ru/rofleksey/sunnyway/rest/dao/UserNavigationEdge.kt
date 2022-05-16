package ru.rofleksey.sunnyway.rest.dao

import ru.rofleksey.sunnyway.rest.types.GeoPoint

data class UserNavigationEdge(
    val fromPoint: GeoPoint,
    val toPoint: GeoPoint,
    val edgeId: Int,
    val toVertexId: Int,
    val distance: Double,
    val time: Long,
    val factor: Double,
    val leftShadow: Double,
    val rightShadow: Double,
)