package ru.rofleksey.sunnyway.rest.types

data class NavigationEdge (
    val fromPoint: GeoPoint,
    val toPoint: GeoPoint,
    val edgeId: Int,
    val toVertexId: Int,
    val direction: Double,
    val leftShadow: Double,
    val rightShadow: Double,
    val distance: Double,
)